package support

import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Period
import org.hl7.fhir.r4.model.Reference

/**
 * Test-data builder for FHIR Encounter resources.
 *
 * By default, build() returns a valid Encounter containing:
 * - id: Encounter/e1
 * - identifier: urn:test:encounter | VN-1001
 * - status: in-progress
 * - class: inpatient encounter
 *
 * Tests can override or remove individual fields.
 */
final class EncounterBuilder {

	private String id = 'Encounter/e1'

	private String identifierSystem = 'urn:test:encounter'
	private String identifierValue = 'VN-1001'
	private boolean includeIdentifier = true

	private Encounter.EncounterStatus status = Encounter.EncounterStatus.INPROGRESS

	private Coding encounterClass = new Coding()
	.setSystem('http://terminology.hl7.org/CodeSystem/v3-ActCode')
	.setCode('IMP')
	.setDisplay('inpatient encounter')

	private Reference subject

	private Instant periodStart
	private Instant periodEnd

	private final List<LocationDefinition> locations = []
	private final List<Closure<?>> customizers = []

	EncounterBuilder setId(String id) {
		this.id = id
		return this
	}

	EncounterBuilder setIdentifier(
			String system,
			String value) {

		this.identifierSystem = system
		this.identifierValue = value
		this.includeIdentifier = true
		return this
	}

	EncounterBuilder setIdentifierSystem(String system) {
		this.identifierSystem = system
		this.includeIdentifier = true
		return this
	}

	EncounterBuilder setIdentifierValue(String value) {
		this.identifierValue = value
		this.includeIdentifier = true
		return this
	}

	/**
	 * Creates an Encounter without any identifier.
	 *
	 * Useful for testing identifier validation rules.
	 */
	EncounterBuilder withoutIdentifier() {
		this.includeIdentifier = false
		return this
	}

	EncounterBuilder setStatus(Encounter.EncounterStatus status) {
		this.status = status
		return this
	}

	EncounterBuilder setClass(
			String system,
			String code,
			String display = null) {

		this.encounterClass = new Coding()
				.setSystem(system)
				.setCode(code)
				.setDisplay(display)

		return this
	}

	EncounterBuilder setSubject(String patientReference) {
		this.subject = patientReference != null
				? new Reference(patientReference)
				: null

		return this
	}

	EncounterBuilder setSubject(Reference subject) {
		this.subject = subject
		return this
	}

	EncounterBuilder setPeriod(
			Instant start,
			Instant end = null) {

		this.periodStart = start
		this.periodEnd = end
		return this
	}

	EncounterBuilder setStartTime(Instant start) {
		this.periodStart = start
		return this
	}

	EncounterBuilder setEndTime(Instant end) {
		this.periodEnd = end
		return this
	}

	EncounterBuilder addLocation(String locationReference) {
		return addLocation(
				locationReference,
				Encounter.EncounterLocationStatus.ACTIVE
				)
	}

	EncounterBuilder addLocation(
			String locationReference,
			Encounter.EncounterLocationStatus status) {

		locations.add(
				new LocationDefinition(locationReference, status)
				)

		return this
	}

	/**
	 * Applies additional custom configuration to the Encounter created by this
	 * builder.
	 *
	 * Use this method for Encounter fields that do not yet have a dedicated
	 * builder method. The closure is executed during build() after the builder's
	 * standard fields have been populated.
	 *
	 * The Encounter instance is available both:
	 * - as the closure argument
	 * - as the closure delegate
	 *
	 * Example:
	 *
	 * encounter()
	 *     .customize { Encounter e ->
	 *         e.addType().setText('Emergency')
	 *         e.setServiceProvider(
	 *             new Reference('Organization/hospital-1')
	 *         )
	 *     }
	 *     .build()
	 *
	 * @param customizer closure that modifies the Encounter before it is returned
	 * @return this builder
	 */
	EncounterBuilder customize(
			@DelegatesTo(value = Encounter, strategy = Closure.DELEGATE_FIRST)
			Closure<?> customizer) {

		customizers.add(
				Objects.requireNonNull(customizer, 'customizer')
				)

		return this
	}

	Encounter build() {
		Encounter encounter = new Encounter()

		if (id != null && !id.isBlank()) {
			encounter.setId(id)
		}

		if (includeIdentifier) {
			encounter.addIdentifier()
					.setSystem(identifierSystem)
					.setValue(identifierValue)
		}

		if (status != null) {
			encounter.setStatus(status)
		}

		if (encounterClass != null) {
			encounter.setClass_(encounterClass.copy())
		}

		if (subject != null) {
			encounter.setSubject(subject.copy())
		}

		if (periodStart != null || periodEnd != null) {
			Period period = new Period()

			if (periodStart != null) {
				period.setStart(Date.from(periodStart))
			}

			if (periodEnd != null) {
				period.setEnd(Date.from(periodEnd))
			}

			encounter.setPeriod(period)
		}

		for (LocationDefinition definition : locations) {
			Encounter.EncounterLocationComponent component =
					encounter.addLocation()

			component.setLocation(
					new Reference(definition.reference)
					)

			if (definition.status != null) {
				component.setStatus(definition.status)
			}
		}

		for (Closure<?> customizer : customizers) {
			Closure<?> configured = customizer.rehydrate(
					encounter,
					customizer.owner,
					customizer.thisObject
					)

			configured.resolveStrategy = Closure.DELEGATE_FIRST
			configured.call(encounter)
		}

		return encounter
	}

	private static final class LocationDefinition {

		final String reference
		final Encounter.EncounterLocationStatus status

		LocationDefinition(
		String reference,
		Encounter.EncounterLocationStatus status) {

			this.reference = Objects.requireNonNull(
					reference,
					'locationReference'
					)
			this.status = status
		}
	}
}