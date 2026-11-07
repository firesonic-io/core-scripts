package core

import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Patient
import spock.lang.Specification
import spock.lang.Unroll
import support.SemanticRuleTestKit

import static support.TestFixtures.bundleOf
import static support.TestFixtures.patient

class PatientRulesSpec extends Specification {

	private static final String IDENTIFIER_RULE = "SEMANTIC-CORE-PAT-001"
	private static final String NAME_RULE = "SEMANTIC-CORE-PAT-002"

	private SemanticRuleTestKit rules

	def setup() {
		rules = new SemanticRuleTestKit()
	}

	def cleanup() {
		rules.close()
	}

	def "patient with identifier and complete name passes all patient rules"() {
		given:
		Patient patient = patient()
				.setId("Patient/p1")
				.setName("Emma", "Miller")
				.build()

		when:
		def identifierIssues = rules.validate(IDENTIFIER_RULE, bundleOf(patient))
		def nameIssues = rules.validate(NAME_RULE, bundleOf(patient))

		then:
		identifierIssues.isEmpty()
		nameIssues.isEmpty()
	}

	def "patient without identifier creates one identifier issue"() {
		given:
		Patient patient = patient()
				.setId("Patient/p1")
				.setName("Emma", "Miller")
				.withoutIdentifier()
				.build()

		when:
		def issues = rules.validate(IDENTIFIER_RULE, bundleOf(patient))

		then:
		issues.size() == 1

		and:
		def issue = issues.first()
		issue.ruleId == IDENTIFIER_RULE
		issue.severity.toString() == "ERROR"
		issue.message == "Patient does not contain an identifier"
		issue.resourceType == "Patient"
		issue.resourceId == "Patient/p1"
	}

	def "patient without name creates one name issue"() {
		given:
		Patient patient = patient()
				.setId("Patient/p1")
				.setIdentifier("urn:test:patient", "123456")
				.withoutName()
				.build()

		when:
		def issues = rules.validate(NAME_RULE, bundleOf(patient))

		then:
		issues.size() == 1

		and:
		def issue = issues.first()
		issue.ruleId == NAME_RULE
		issue.severity.toString() == "ERROR"
		issue.message == "Patient does not contain both a first name and a last name"
		issue.resourceType == "Patient"
		issue.resourceId == "Patient/p1"
	}

	@Unroll
	def "patient name is invalid when #caseName"() {
		given:
		Patient patient = patient()
				.setId("Patient/p1")
				.setIdentifier("urn:test:patient", "123456")
				.withoutName()
				.build()

		def name = patient.addName()

		if (givenName != null) {
			name.addGiven(givenName)
		}

		if (familyName != null) {
			name.setFamily(familyName)
		}

		when:
		def issues = rules.validate(NAME_RULE, bundleOf(patient))

		then:
		issues.size() == 1
		issues.first().ruleId == NAME_RULE
		issues.first().resourceId == "Patient/p1"

		where:
		caseName | givenName | familyName
		"given name is missing" | null | "Miller"
		"given name is blank" | "   " | "Miller"
		"family name is missing"| "Emma" | null
		"family name is blank" | "Emma" | "   "
		"both names are missing"| null | null
	}

	def "patient passes name rule when at least one name is complete"() {
		given:
		Patient patient = patient()
				.setId("Patient/p1")
				.setIdentifier("urn:test:patient", "123456")
				.build()

		patient.addName()
				.setFamily("AliasOnly")

		patient.addName()
				.setFamily("Miller")
				.addGiven("Emma")

		when:
		def issues = rules.validate(NAME_RULE, bundleOf(patient))

		then:
		issues.isEmpty()
	}

	def "identifier rule reports only patients without identifiers"() {
		given:
		Patient valid = patient()
				.setId("Patient/valid")
				.setName("Emma", "Miller")
				.setIdentifier("urn:test:patient", "123456")
				.build()

		Patient invalid = patient()
				.setId("Patient/missing-id")
				.setName("Emma", "Miller")
				.withoutIdentifier()
				.build()

		when:
		def issues = rules.validate(
				IDENTIFIER_RULE,
				bundleOf(valid, invalid)
				)

		then:
		issues.size() == 1
		issues.first().resourceId == "Patient/missing-id"
	}

	def "name rule reports only patients without a complete name"() {
		given:
		Patient valid = patient()
				.setId("Patient/valid")
				.setName("Emma", "Miller")
				.build()


		Patient invalid = patient()
				.setId("Patient/missing-name")
				.setIdentifier("urn:test:patient", "123456")
				.withoutName()
				.build()

		when:
		def issues = rules.validate(
				NAME_RULE,
				bundleOf(valid, invalid)
				)

		then:
		issues.size() == 1
		issues.first().resourceId == "Patient/missing-name"
	}

	def "patient rules ignore a bundle without Patient resources"() {
		expect:
		rules.validate(IDENTIFIER_RULE, new Bundle()).isEmpty()
		rules.validate(NAME_RULE, new Bundle()).isEmpty()
	}
}
