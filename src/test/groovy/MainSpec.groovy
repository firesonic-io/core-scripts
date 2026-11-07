import io.firesonic.script.api.FilterResult
import io.firesonic.script.api.MessageEnvelope
import org.hl7.fhir.r4.model.Bundle
import spock.lang.Specification

import static support.TestFixtures.*

/**
 * Example Spock tests for the default SDK Main script.
 *
 * These tests intentionally focus on behavior that does not require:
 * - a running FHIR server
 * - a real Patient merge workflow
 * - a real Patient transfer workflow
 *
 * A02 and A40 branches should be covered by separate workflow/integration tests,
 * because Main delegates those branches to PatientTransferWorkflow and
 * PatientMergeWorkflow.
 */
class MainSpec extends Specification {

	private MessageEnvelope envelop
	private Bundle bundle

	def setup() {
		envelop = envelope().build()
		bundle = bundleOf(patient().build(), encounter().build())
	}

	def "shouldProcess accepts message by default"() {
		given:
		def script = new Main()

		when:
		FilterResult result = script.shouldProcess(envelop)

		then:
		result.isAccepted()
	}

	def "preConvert returns the same envelope instance"() {
		given:
		def script = new Main()

		expect:
		script.preConvert(envelop) == envelop
	}

	def "postConvert returns null bundle unchanged"() {
		given:
		def script = new Main()

		expect:
		script.postConvert(envelop, null) == null
	}

	def "postConvert returns bundle unchanged"() {
		given:
		def script = new Main()

		when:
		Bundle result = script.postConvert(envelop, bundle)

		then:
		result.is(bundle)
		!result.entry.isEmpty()
	}

	def "scriptVersion returns configured script version"() {
		given:
		def script = new Main()
		script.scriptVersion = "custom-script-v1.0.0"

		expect:
		script.scriptVersion() == "custom-script-v1.0.0"
	}

	def "lifecycle hooks are safe to call"() {
		given:
		def script = new Main()
		script.debugEnabled = false

		when:
		script.onStartup()
		script.onShutdown()

		then:
		noExceptionThrown()
	}
}
