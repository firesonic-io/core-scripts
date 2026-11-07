package support

import io.firesonic.script.api.MessageEnvelope
import io.firesonic.script.api.validation.SemanticIssue
import io.firesonic.script.api.validation.dsl.SemanticIssueCollector
import io.firesonic.script.api.validation.dsl.SemanticRuleContext
import io.firesonic.script.api.validation.dsl.SemanticRuleDefinition
import io.firesonic.script.api.validation.dsl.SemanticRulesDsl
import org.codehaus.groovy.control.CompilerConfiguration
import org.hl7.fhir.r4.model.Bundle

import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Stream

/**
 * Test-only harness for loading and executing FireSonic semantic rules.
 *
 *
 * Default rule directory:
 *   ./rules/semantic
 *
 * This class intentionally uses only:
 * - Groovy libraries available to the SDK test project
 * - public FireSonic Script API / semantic DSL classes
 *
 */
final class SemanticRuleTestKit implements AutoCloseable {

	private static final Path DEFAULT_RULES_DIR =
	Path.of('.', 'rules', 'semantic').toAbsolutePath().normalize()

	private final Path rulesDirectory
	private final GroovyClassLoader classLoader

	SemanticRuleTestKit() {
		this(DEFAULT_RULES_DIR)
	}

	SemanticRuleTestKit(Path rulesDirectory) {
		if (rulesDirectory == null) {
			throw new IllegalArgumentException('rulesDirectory must not be null')
		}

		this.rulesDirectory = rulesDirectory.toAbsolutePath().normalize()

		CompilerConfiguration configuration = new CompilerConfiguration()
		this.classLoader = new GroovyClassLoader(
				SemanticRuleTestKit.class.classLoader,
				configuration
				)
	}

	/**
	 * Recursively loads every *.groovy rule file from ./rules/semantic.
	 */
	List<SemanticRuleDefinition> loadAll() {
		if (!Files.exists(rulesDirectory)) {
			throw new IllegalStateException(
			'Semantic rules directory does not exist: ' + rulesDirectory
			)
		}

		if (!Files.isDirectory(rulesDirectory)) {
			throw new IllegalStateException(
			'Semantic rules path is not a directory: ' + rulesDirectory
			)
		}

		List<Path> files

		Stream<Path> stream = Files.walk(rulesDirectory)
		try {
			files = stream
					.filter { Path path ->
						Files.isRegularFile(path)
					}
					.filter { Path path ->
						path.fileName.toString().toLowerCase(Locale.ROOT).endsWith('.groovy')
					}
					.sorted()
					.toList()
		} finally {
			stream.close()
		}

		List<SemanticRuleDefinition> rules = []

		for (Path file : files) {
			rules.addAll(loadFile(file))
		}

		assertUniqueRuleIds(rules)
		return List.copyOf(rules)
	}

	/**
	 * Loads one rule file. The supplied path may be absolute or relative to
	 * ./rules/semantic.
	 */
	List<SemanticRuleDefinition> loadFile(Path file) {
		Path resolved = resolveRuleFile(file)

		if (!Files.isRegularFile(resolved)) {
			throw new IllegalArgumentException(
			'Semantic rule file does not exist: ' + resolved
			)
		}

		Class<?> scriptClass

		try {
			scriptClass = classLoader.parseClass(resolved.toFile())
		} catch (Exception ex) {
			throw new IllegalStateException(
			'Failed to compile semantic rule file: ' + resolved,
			ex
			)
		}

		Object instance

		try {
			instance = scriptClass.getDeclaredConstructor().newInstance()
		} catch (Exception ex) {
			throw new IllegalStateException(
			'Failed to instantiate semantic rule script: ' + resolved,
			ex
			)
		}

		if (!(instance instanceof SemanticRulesDsl)) {
			throw new IllegalStateException(
			'Rule script must use @BaseScript SemanticRulesDsl: ' + resolved
			)
		}

		SemanticRulesDsl script = (SemanticRulesDsl) instance
		script.setSourceFile(resolved)

		try {
			script.run()
		} catch (Exception ex) {
			throw new IllegalStateException(
			'Failed to execute semantic rule script: ' + resolved,
			ex
			)
		}

		List<SemanticRuleDefinition> rules = script.getRules()

		if (rules.isEmpty()) {
			throw new IllegalStateException(
			'No semantic rules were declared in file: ' + resolved
			)
		}

		return List.copyOf(rules)
	}

	SemanticRuleDefinition requireRule(String ruleId) {
		if (ruleId == null || ruleId.isBlank()) {
			throw new IllegalArgumentException('ruleId must not be blank')
		}

		List<SemanticRuleDefinition> matches = loadAll().findAll { SemanticRuleDefinition rule ->
			rule.id() == ruleId
		}

		if (matches.isEmpty()) {
			throw new IllegalArgumentException(
			'Semantic rule not found: ' + ruleId
			+ '; directory=' + rulesDirectory
			)
		}

		if (matches.size() > 1) {
			throw new IllegalStateException(
			'Duplicate semantic rule id: ' + ruleId
			)
		}

		return matches.first()
	}

	/**
	 * Executes one already-loaded rule against an in-memory Bundle.
	 *
	 * The validation closure is executed only when at least one rule scope
	 * matches the supplied envelope messageCode and triggerEvent. Rules without
	 * an explicit appliesTo(...) declaration contain the global wildcard scope and
	 * therefore match every envelope.
	 */
	List<SemanticIssue> validate(
			SemanticRuleDefinition rule,
			Bundle bundle,
			MessageEnvelope envelope = envelope().build()) {

		if (rule == null) {
			throw new IllegalArgumentException('rule must not be null')
		}

		if (envelope == null) {
			throw new IllegalArgumentException('envelope must not be null')
		}

		if (!appliesTo(rule, envelope)) {
			return []
		}

		SemanticRuleContext context = new SemanticRuleContext(envelope, bundle)
		SemanticIssueCollector collector = new SemanticIssueCollector()

		Closure<?> validation = rule.validate()
		if (validation == null) {
			throw new IllegalStateException(
			'Semantic rule has no validation closure: ' + rule.id()
			)
		}

		validation.call(context, collector)
		return collector.issues()
	}

	List<SemanticIssue> validate(
			String ruleId,
			Bundle bundle,
			MessageEnvelope envelope = envelope().build()) {

		return validate(requireRule(ruleId), bundle, envelope)
	}

	/**
	 * Returns true when the rule applies to the supplied HL7 envelope.
	 */
	private static boolean appliesTo(
			SemanticRuleDefinition rule,
			MessageEnvelope envelope) {

		return rule.scopes().any { scope ->
			scope.matches(
			envelope.getMessageCode(),
			envelope.getTriggerEvent()
			)
		}
	}

	private Path resolveRuleFile(Path file) {
		if (file == null) {
			throw new IllegalArgumentException('file must not be null')
		}

		Path resolved = file.isAbsolute()
				? file
				: rulesDirectory.resolve(file)

		return resolved.toAbsolutePath().normalize()
	}

	private static void assertUniqueRuleIds(List<SemanticRuleDefinition> rules) {
		Map<String, List<SemanticRuleDefinition>> grouped =
				rules.groupBy { SemanticRuleDefinition rule ->
					rule.id()
				}

		List<String> duplicates = grouped
				.findAll { String id, List<SemanticRuleDefinition> values ->
					id != null && values.size() > 1
				}
				.keySet()
				.sort()

		if (!duplicates.isEmpty()) {
			throw new IllegalStateException(
			'Duplicate semantic rule IDs: ' + duplicates.join(', ')
			)
		}
	}

	@Override
	void close() {
		classLoader.close()
	}
}
