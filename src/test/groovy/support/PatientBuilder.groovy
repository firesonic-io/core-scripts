package support

import org.hl7.fhir.r4.model.*
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender
import org.hl7.fhir.r4.model.Reference

/**
 * Test-data builder for FHIR Patient resources.
 *
 * Default result:
 * - id: Patient/p1
 * - identifier: urn:test:patient | 123456
 * - name: Emma Miller
 * - active: true
 */
final class PatientBuilder {

	private String id = 'Patient/p1'

	private boolean includeIdentifier = true
	private String identifierSystem = 'urn:test:patient'
	private String identifierValue = '123456'

	private boolean includeName = true
	private String givenName = 'Emma'
	private String familyName = 'Miller'

	private Boolean active = true
	private AdministrativeGender gender = AdministrativeGender.FEMALE
	private DateType birthDate

	private final List<Identifier> additionalIdentifiers = []
	private final List<HumanName> additionalNames = []
	private final List<Address> addresses = []
	private final List<ContactPoint> telecoms = []
	private final List<Reference> generalPractitioners = []
	private final List<Closure<?>> customizers = []

	PatientBuilder setId(String id) {
		this.id = id
		return this
	}

	PatientBuilder setIdentifier(String system, String value) {
		this.identifierSystem = system
		this.identifierValue = value
		this.includeIdentifier = true
		return this
	}

	PatientBuilder setIdentifierSystem(String system) {
		this.identifierSystem = system
		this.includeIdentifier = true
		return this
	}

	PatientBuilder setIdentifierValue(String value) {
		this.identifierValue = value
		this.includeIdentifier = true
		return this
	}

	PatientBuilder withoutIdentifier() {
		this.includeIdentifier = false
		return this
	}

	PatientBuilder addIdentifier(String system, String value) {
		additionalIdentifiers.add(
				new Identifier()
				.setSystem(system)
				.setValue(value)
				)
		return this
	}

	PatientBuilder setName(String given, String family) {
		this.givenName = given
		this.familyName = family
		this.includeName = true
		return this
	}

	PatientBuilder setGivenName(String given) {
		this.givenName = given
		this.includeName = true
		return this
	}

	PatientBuilder setFamilyName(String family) {
		this.familyName = family
		this.includeName = true
		return this
	}

	PatientBuilder withoutName() {
		this.includeName = false
		return this
	}

	PatientBuilder addName(String given, String family) {
		HumanName name = new HumanName()

		if (given != null) {
			name.addGiven(given)
		}

		if (family != null) {
			name.setFamily(family)
		}

		additionalNames.add(name)
		return this
	}

	PatientBuilder setActive(Boolean active) {
		this.active = active
		return this
	}

	PatientBuilder setGender(AdministrativeGender gender) {
		this.gender = gender
		return this
	}

	PatientBuilder setBirthDate(String value) {
		this.birthDate = value != null
				? new DateType(value)
				: null
		return this
	}

	PatientBuilder addAddress(Address address) {
		addresses.add(
				Objects.requireNonNull(address, 'address')
				)
		return this
	}

	PatientBuilder addTelecom(ContactPoint telecom) {
		telecoms.add(
				Objects.requireNonNull(telecom, 'telecom')
				)
		return this
	}

	PatientBuilder addGeneralPractitioner(String reference) {
		generalPractitioners.add(new Reference(reference))
		return this
	}

	/**
	 * Applies additional custom configuration to the Patient created by this
	 * builder.
	 *
	 * Use this method for Patient fields that do not yet have a dedicated builder
	 * method. The closure is executed during build() after the builder's standard
	 * fields have been populated.
	 *
	 * The Patient instance is available both:
	 * - as the closure argument
	 * - as the closure delegate
	 *
	 * Example:
	 *
	 * patient()
	 *     .customize { Patient p ->
	 *         p.setDeceased(new BooleanType(false))
	 *         p.addCommunication()
	 *             .getLanguage()
	 *             .setText('English')
	 *     }
	 *     .build()
	 *
	 * @param customizer closure that modifies the Patient before it is returned
	 * @return this builder
	 */
	PatientBuilder customize(
			@DelegatesTo(value = Patient, strategy = Closure.DELEGATE_FIRST)
			Closure<?> customizer) {

		customizers.add(
				Objects.requireNonNull(customizer, 'customizer')
				)

		return this
	}

	Patient build() {
		Patient patient = new Patient()

		if (id != null && !id.isBlank()) {
			patient.setId(id)
		}

		if (active != null) {
			patient.setActive(active)
		}

		if (includeIdentifier) {
			patient.addIdentifier()
					.setSystem(identifierSystem)
					.setValue(identifierValue)
		}

		for (Identifier identifier : additionalIdentifiers) {
			patient.addIdentifier(identifier.copy())
		}

		if (includeName) {
			HumanName name = patient.addName()

			if (givenName != null) {
				name.addGiven(givenName)
			}

			if (familyName != null) {
				name.setFamily(familyName)
			}
		}

		for (HumanName name : additionalNames) {
			patient.addName(name.copy())
		}

		if (gender != null) {
			patient.setGender(gender)
		}

		if (birthDate != null) {
			patient.setBirthDateElement(birthDate.copy())
		}

		for (Address address : addresses) {
			patient.addAddress(address.copy())
		}

		for (ContactPoint telecom : telecoms) {
			patient.addTelecom(telecom.copy())
		}

		for (Reference practitioner : generalPractitioners) {
			patient.addGeneralPractitioner(practitioner.copy())
		}

		for (Closure<?> customizer : customizers) {
			Closure<?> configured = customizer.rehydrate(
					patient,
					customizer.owner,
					customizer.thisObject
					)

			configured.resolveStrategy = Closure.DELEGATE_FIRST
			configured.call(patient)
		}

		return patient
	}
}