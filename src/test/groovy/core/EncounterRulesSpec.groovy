package core

import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Patient
import spock.lang.Specification
import support.SemanticRuleTestKit

import static support.TestFixtures.*

class EncounterRulesSpec extends Specification {

	private static final String IDENTIFIER_RULE = "SEMANTIC-CORE-ENC-001"

	private SemanticRuleTestKit rules

	def setup() {
		rules = new SemanticRuleTestKit()
	}

	def cleanup() {
		rules.close()
	}

	def "encounter with identifier passes validation"() {
		given:
		Encounter encounter = encounter()
				.setId("Encounter/e1")
				.setIdentifier("urn:test:encounter", "VN-1001")
				.build()

		when:
		def issues = rules.validate(
				IDENTIFIER_RULE,
				bundleOf(encounter)
				)

		then:
		issues.isEmpty()
	}

	def "encounter without identifier creates one error issue"() {
		given:
		Encounter encounter = encounter()
				.setId("Encounter/e1")
				.withoutIdentifier()
				.build()

		when:
		def issues = rules.validate(
				IDENTIFIER_RULE,
				bundleOf(encounter)
				)

		then:
		issues.size() == 1

		and:
		def issue = issues.first()
		issue.ruleId == IDENTIFIER_RULE
		issue.severity.toString() == "ERROR"
		issue.message == "Encounter does not contain an identifier"
		issue.resourceType == "Encounter"
		issue.resourceId == "Encounter/e1"
	}

	def "rule reports every encounter without an identifier"() {
		given:
		Encounter valid = encounter()
				.setId("Encounter/valid")
				.setIdentifier("urn:test:encounter", "VN-1")
				.build()

		Encounter invalidOne = encounter()
				.setId("Encounter/missing-1")
				.withoutIdentifier()
				.build()

		Encounter invalidTwo = encounter()
				.setId("Encounter/missing-2")
				.withoutIdentifier()
				.build()

		when:
		def issues = rules.validate(
				IDENTIFIER_RULE,
				bundleOf(valid, invalidOne, invalidTwo)
				)

		then:
		issues.size() == 2
		issues*.resourceId as Set == [
			"Encounter/missing-1",
			"Encounter/missing-2"
		] as Set
		issues.every { it.ruleId == IDENTIFIER_RULE }
		issues.every { it.severity.toString() == "ERROR" }
	}

	def "encounter rule ignores non-Encounter resources"() {
		given:
		Patient patient = patient()
				.setId("Patient/p1")
				.withoutIdentifier()
				.build()

		when:
		def issues = rules.validate(
				IDENTIFIER_RULE,
				bundleOf(patient)
				)

		then:
		issues.isEmpty()
	}

	def "encounter rule accepts an empty bundle"() {
		expect:
		rules.validate(IDENTIFIER_RULE, new Bundle()).isEmpty()
	}
}
