package support

import io.firesonic.script.api.MessageEnvelope
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Resource

/**
 * Central entry point for reusable SDK test-data builders and fixtures.
 *
 * <pre>
 * MessageEnvelope envelope = TestFixtures.envelope()
 *     .fromResource('hl7/adt-a01-with-multiple-al1.hl7')
 *     .build()
 *
 * Patient patient = TestFixtures.patient()
 *     .setId('Patient/p100')
 *     .build()
 *
 * Encounter encounter = TestFixtures.encounter()
 *     .setId('Encounter/e100')
 *     .build()
 *
 * Bundle bundle = TestFixtures.bundleOf(patient, encounter)
 * </pre>
 */
final class TestFixtures {

	private TestFixtures() {
		// Utility class
	}

	/**
	 * Creates a new MessageEnvelope builder with default test values.
	 */
	static MessageEnvelopeBuilder envelope() {
		return new MessageEnvelopeBuilder()
	}

	/**
	 * Loads, parses, and builds a MessageEnvelope directly from an HL7 file under
	 * src/test/resources.
	 *
	 * <p>
	 * Use {@link #envelope()} with {@code fromResource(...)} when additional
	 * builder customization is required.
	 * </p>
	 */
	static MessageEnvelope envelopeFromResource(String resourcePath) {
		return envelope()
				.fromResource(resourcePath)
				.build()
	}

	/**
	 * Creates a new Patient builder with default test values.
	 */
	static PatientBuilder patient() {
		return new PatientBuilder()
	}

	/**
	 * Creates a new Encounter builder with default test values.
	 */
	static EncounterBuilder encounter() {
		return new EncounterBuilder()
	}

	/**
	 * Creates a transaction Bundle containing the supplied resources.
	 * Null resources are ignored.
	 */
	static Bundle bundleOf(Resource... resources) {
		Bundle bundle = new Bundle()
		bundle.setType(Bundle.BundleType.TRANSACTION)

		if (resources == null) {
			return bundle
		}

		for (Resource resource : resources) {
			if (resource != null) {
				bundle.addEntry().setResource(resource)
			}
		}

		return bundle
	}
}
