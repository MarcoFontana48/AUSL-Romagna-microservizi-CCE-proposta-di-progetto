package ausl.cce.service.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import mf.cce.utils.Entity
import mf.cce.utils.Factory
import mf.cce.utils.ID
import org.hl7.fhir.r4.model.Encounter
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import org.hl7.fhir.r4.model.*
import java.util.*

/**
 * Extension functions for Encounter FHIR resources with JSON serialization support.
 * Uses HAPI FHIR's native JSON parser for proper FHIR-compliant serialization.
 *
 * Based on HAPI FHIR documentation: https://hapifhir.io/hapi-fhir/docs/getting_started/downloading_and_importing.html
 * Required dependencies: hapi-fhir-base + hapi-fhir-structures-r4
 */

// FHIR context and parser - shared across all extension functions
private val fhirContext: FhirContext = FhirContext.forR4()
private val jsonParser: IParser = fhirContext.newJsonParser().setPrettyPrint(true)

/**
 * Factory object containing static methods for Encounter creation
 */
object EncounterFactory {
    /**
     * Factory method to create a new Encounter with basic information
     *
     * @param id Logical ID of the Encounter resource (without "Encounter/" prefix)
     * @param patientReference Reference to the patient (e.g., "Patient/123")
     * @param encounterClass Class of the encounter (e.g., "AMB" for ambulatory, "IMP" for inpatient)
     * @param status Status of the encounter (default: "finished")
     * @param serviceTypeSystem Coding system for the service type (e.g., "http://terminology.hl7.org/CodeSystem/service-type")
     * @param serviceTypeCode Code for the service type (e.g., "408")
     * @param serviceTypeDisplay Display text for the service type (e.g., "General Medicine")
     * @return Configured Encounter instance
     * @see <a href="https://www.hl7.org/fhir/encounter.html">FHIR Encounter Resource</a>
     */
    fun of(
        id: String,
        patientReference: String,
        encounterClass: String,
        status: String = "finished",
        serviceTypeSystem: String? = null,
        serviceTypeCode: String? = null,
        serviceTypeDisplay: String? = null
    ): Encounter {
        return Encounter().configure(
            id, patientReference, encounterClass, status,
            serviceTypeSystem, serviceTypeCode, serviceTypeDisplay
        )
    }

    /**
     * Factory method to create a new Encounter with basic information and a CarePlan diagnosis/outcome
     *
     * @param id Logical ID of the Encounter resource (without "Encounter/" prefix)
     * @param patientReference Reference to the patient (e.g., "Patient/123")
     * @param encounterClass Class of the encounter (e.g., "AMB" for ambulatory, "IMP" for inpatient)
     * @param carePlanReference Reference to the CarePlan resource (e.g., "CarePlan/123")
     * @param carePlanUseType The role/use of this diagnosis in the encounter context (default: "CM" for comorbidity)
     * @param status Status of the encounter (default: "finished")
     * @param serviceTypeSystem Coding system for the service type (e.g., "http://terminology.hl7.org/CodeSystem/service-type")
     * @param serviceTypeCode Code for the service type (e.g., "408")
     * @param serviceTypeDisplay Display text for the service type (e.g., "General Medicine")
     * @return Configured Encounter instance
     * @see <a href="https://www.hl7.org/fhir/encounter.html">FHIR Encounter Resource</a>
     */
    fun of(
        id: String,
        patientReference: String,
        encounterClass: String,
        carePlanReference: String,
        carePlanUseType: String = "CM",
        status: String = "finished",
        serviceTypeSystem: String? = null,
        serviceTypeCode: String? = null,
        serviceTypeDisplay: String? = null
    ): Encounter {
        return this.of(
            id, patientReference, encounterClass, status,
            serviceTypeSystem, serviceTypeCode, serviceTypeDisplay
        ).addCarePlanDiagnosis(carePlanReference, carePlanUseType)
    }
}

/**
 * Extension function for Encounter class to serialize Encounter resource to JSON string
 */
fun Encounter.toJson(): String {
    return jsonParser.encodeResourceToString(this)
}

/**
 * Extension function for String class to deserialize JSON string to Encounter resource
 */
fun String.fromJsonToEncounter(): Encounter {
    return jsonParser.parseResource(Encounter::class.java, this)
}

/**
 * Extension function to configure an Encounter with basic information
 */
fun Encounter.configure(
    id: String,
    patientReference: String,
    encounterClass: String,
    status: String = "finished",
    serviceTypeSystem: String? = null,
    serviceTypeCode: String? = null,
    serviceTypeDisplay: String? = null
): Encounter {

    // Set basic fields
    this.id = id    // here 'id' is the 'logical id', not the 'resource id', so no "Encounter/" prefix! HAPI FHIR handles that automatically
    this.subject = Reference(patientReference)

    // Set encounter class
    this.class_ = createEncounterClass(encounterClass)

    // Set status
    this.status = Encounter.EncounterStatus.fromCode(status)

    // Set service type if provided
    if (serviceTypeSystem != null && serviceTypeCode != null && serviceTypeDisplay != null) {
        this.serviceType = createCodeableConcept(serviceTypeSystem, serviceTypeCode, serviceTypeDisplay)
    }

    // Set period - start time to now, end time to now for finished encounters
    val period = Period()
    val now = Date()
    period.start = now
    if (status == "finished") {
        period.end = now
    }
    this.period = period

    return this
}

/**
 * Extension function to add a CarePlan diagnosis/outcome to the encounter
 * This represents a CarePlan that was identified, created, or modified during this encounter
 *
 * @param carePlanReference Reference to the CarePlan resource (e.g., "CarePlan/123")
 * @param useType The role/use of this diagnosis in the encounter context
 * @param rank Ranking of the diagnosis (1 = primary, 2 = secondary, etc.)
 * @return The modified Encounter instance
 */
fun Encounter.addCarePlanDiagnosis(
    carePlanReference: String,
    useType: String = "CM", // Case management
    rank: Int? = null
): Encounter {
    val diagnosis = Encounter.DiagnosisComponent()
    diagnosis.condition = Reference(carePlanReference)

    // Set the use/type of this diagnosis
    val useCodeableConcept = CodeableConcept()
    useCodeableConcept.addCoding().apply {
        system = "http://terminology.hl7.org/CodeSystem/diagnosis-role"
        code = useType
        display = when (useType) {
            "AD" -> "Admission diagnosis"
            "DD" -> "Discharge diagnosis"
            "CC" -> "Chief complaint"
            "CM" -> "Comorbidity diagnosis"
            "pre-op" -> "Pre-operative diagnosis"
            "post-op" -> "Post-operative diagnosis"
            "billing" -> "Billing diagnosis"
            else -> useType
        }
    }
    diagnosis.use = useCodeableConcept

    // Set rank if provided
    rank?.let { diagnosis.rank = it }

    this.addDiagnosis(diagnosis)
    return this
}

/**
 * Extension function to add a participant to the encounter
 */
fun Encounter.addParticipant(
    practitionerReference: String,
    participantType: String = "PPRF", // Primary performer
    participantTypeSystem: String = "http://terminology.hl7.org/CodeSystem/v3-ParticipationType"
): Encounter {
    val participant = Encounter.EncounterParticipantComponent()
    participant.individual = Reference(practitionerReference)

    val typeCodeableConcept = CodeableConcept()
    typeCodeableConcept.addCoding().apply {
        system = participantTypeSystem
        code = participantType
        display = when (participantType) {
            "PPRF" -> "Primary Performer"
            "SPRF" -> "Secondary Performer"
            "ATND" -> "Attender"
            else -> participantType
        }
    }
    participant.addType(typeCodeableConcept)

    this.addParticipant(participant)
    return this
}

/**
 * Extension function to add a reason for the encounter
 */
fun Encounter.addReason(
    reasonSystem: String,
    reasonCode: String,
    reasonDisplay: String
): Encounter {
    val reasonReference = CodeableConcept()
    reasonReference.addCoding().apply {
        system = reasonSystem
        code = reasonCode
        display = reasonDisplay
    }
    reasonReference.text = reasonDisplay

    this.addReasonCode(reasonReference)
    return this
}

/**
 * Extension function to add a location to the encounter
 */
fun Encounter.addLocation(
    locationReference: String,
    status: String = "active"
): Encounter {
    val location = Encounter.EncounterLocationComponent()
    location.location = Reference(locationReference)
    location.status = Encounter.EncounterLocationStatus.fromCode(status)

    this.addLocation(location)
    return this
}

// === Helper functions for creating CodeableConcepts ===

private fun createCodeableConcept(system: String, code: String, display: String): CodeableConcept {
    val concept = CodeableConcept()
    concept.addCoding().apply {
        this.system = system
        this.code = code
        this.display = display
    }
    concept.text = display
    return concept
}

private fun createEncounterClass(classCode: String): Coding {
    val coding = Coding()
    coding.system = "http://terminology.hl7.org/CodeSystem/v3-ActCode"
    coding.code = classCode
    coding.display = when (classCode) {
        "AMB" -> "Ambulatory"
        "EMER" -> "Emergency"
        "FLD" -> "Field"
        "HH" -> "Home Health"
        "IMP" -> "Inpatient Encounter"
        "ACUTE" -> "Inpatient Acute"
        "NONAC" -> "Inpatient Non-Acute"
        "OBSENC" -> "Observation Encounter"
        "PRENC" -> "Pre-Admission"
        "SS" -> "Short Stay"
        "VR" -> "Virtual"
        else -> classCode
    }
    return coding
}

/**
 * Domain ID for Encounter entities
 */
data class EncounterId @JsonCreator constructor(
    override val value: String
) : ID<String>(value)

/**
 * Domain entity wrapper for FHIR Encounter resource
 */
class EncounterEntity private constructor(
    id: EncounterId,
    val encounter: Encounter
) : Entity<EncounterId>(id) {

    companion object : Factory<EncounterEntity> {
        /**
         * Creates an EncounterEntity from a FHIR Encounter resource
         */
        fun of(encounter: Encounter): EncounterEntity {
            val domainId = EncounterId(encounter.id)
            return EncounterEntity(domainId, encounter)
        }

        /**
         * Creates an EncounterEntity with domain ID and FHIR resource
         */
        fun of(id: EncounterId, encounter: Encounter): EncounterEntity {
            // Ensure the FHIR resource ID matches the domain ID
            encounter.id = id.value
            return EncounterEntity(id, encounter)
        }
    }

    /**
     * Convenience method to get the underlying FHIR resource as JSON
     */
    fun toJson(): String = encounter.toJson()

    /**
     * Updates the underlying FHIR resource
     */
    fun updateEncounter(updatedResource: Encounter): EncounterEntity {
        // Ensure ID consistency
        updatedResource.id = this.id.value
        return EncounterEntity(this.id, updatedResource)
    }
}

class DummyEntity private constructor(
    @JsonProperty("id") id: DummyId,
    @JsonProperty("field") val dummyField: String
) : Entity<DummyEntity.DummyId>(id) {

    data class DummyId @JsonCreator constructor(
        @JsonProperty("value") override val value: String
    ) : ID<String>(value)

    companion object : Factory<DummyEntity> {
        fun of(id: DummyId, dummyField: String): DummyEntity {
            return DummyEntity(id, dummyField)
        }
    }
}