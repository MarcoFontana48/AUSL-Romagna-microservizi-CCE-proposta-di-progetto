package ausl.cce.service.domain

import com.fasterxml.jackson.annotation.JsonCreator
import mf.cce.utils.Entity
import mf.cce.utils.Factory
import mf.cce.utils.ID
import org.hl7.fhir.r4.model.AllergyIntolerance
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import org.hl7.fhir.r4.model.*
import org.hl7.fhir.dstu3.model.AllergyIntolerance.AllergyIntoleranceClinicalStatusEnumFactory
import java.util.*

/**
 * Extension functions for AllergyIntolerance FHIR resources with JSON serialization support.
 * Uses HAPI FHIR's native JSON parser for proper FHIR-compliant serialization.
 *
 * Based on HAPI FHIR documentation: https://hapifhir.io/hapi-fhir/docs/getting_started/downloading_and_importing.html
 * Required dependencies: hapi-fhir-base + hapi-fhir-structures-r4
 */

// FHIR context and parser - shared across all extension functions
private val fhirContext: FhirContext = FhirContext.forR4()
private val jsonParser: IParser = fhirContext.newJsonParser().setPrettyPrint(true)

/**
 * Factory object containing static methods for AllergyIntolerance creation
 */
object AllergyIntoleranceFactory {
    /**
     * Factory method to create a new AllergyIntolerance with basic information
     *
     * @param id Logical ID of the AllergyIntolerance resource (without "AllergyIntolerance/" prefix)
     * @param patientReference Reference to the patient (e.g., "Patient/123")
     * @param allergenSystem Coding system for the allergen (e.g., "http://snomed.info/sct")
     * @param allergenCode Code for the allergen (e.g., "227493005")
     * @param allergenDisplay Display text for the allergen (e.g., "Cashew nuts")
     * @param clinicalStatus Clinical status of the allergy (default: "active")
     * @return Configured AllergyIntolerance instance
     */
    fun of(
        id: String,
        patientReference: String,
        allergenSystem: String,
        allergenCode: String,
        allergenDisplay: String,
        clinicalStatus: String = "active",
    ): AllergyIntolerance {

        return AllergyIntolerance().configure(
            id, patientReference, allergenSystem, allergenCode, allergenDisplay, clinicalStatus,
        )
    }
}

/**
 * Extension function for AllergyIntolerance class to serialize AllergyIntolerance resource to JSON string
 */
fun AllergyIntolerance.toJson(): String {
    return jsonParser.encodeResourceToString(this)
}

/**
 * Extension function for String class to deserialize JSON string to AllergyIntolerance resource
 */
fun String.fromJsonToAllergyIntolerance(): AllergyIntolerance {
    return jsonParser.parseResource(AllergyIntolerance::class.java, this)
}

/**
 * Extension function to create a new AllergyIntolerance with basic information
 */
fun AllergyIntolerance.configure(
    id: String,
    patientReference: String,
    allergenSystem: String,
    allergenCode: String,
    allergenDisplay: String,
    clinicalStatus: String = "active",
): AllergyIntolerance {

    // Set basic fields
    this.id = id    // here 'id' is the 'logical id', not the 'resource id', so no "AllergyIntolerance/" prefix! HAPI FHIR handles that automatically
    this.patient = Reference(patientReference)

    // Set allergen
    this.code = createCodeableConcept(allergenSystem, allergenCode, allergenDisplay)

    // Set status fields using the enum factories
    this.clinicalStatus = createClinicalStatusConcept(clinicalStatus)

    // Set onset date to now
    this.onset = DateTimeType(Date())

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

private fun createClinicalStatusConcept(statusCode: String): CodeableConcept {
    val concept = CodeableConcept()
    val enumValue = AllergyIntoleranceClinicalStatusEnumFactory().fromCode(statusCode)
    concept.addCoding().apply {
        system = "http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical"
        code = enumValue.toCode()
        display = enumValue.display
    }
    return concept
}

/**
 * Domain ID for AllergyIntolerance entities
 */
data class AllergyIntoleranceId @JsonCreator constructor(
    override val value: String
) : ID<String>(value)

/**
 * Domain entity wrapper for FHIR AllergyIntolerance resource
 */
class AllergyIntoleranceEntity private constructor(
    id: AllergyIntoleranceId,
    val allergyIntolerance: AllergyIntolerance
) : Entity<AllergyIntoleranceId>(id) {

    companion object : Factory<AllergyIntoleranceEntity> {
        /**
         * Creates an AllergyIntoleranceEntity from a FHIR AllergyIntolerance resource
         */
        fun of(allergyIntolerance: AllergyIntolerance): AllergyIntoleranceEntity {
            val domainId = AllergyIntoleranceId(allergyIntolerance.id)
            return AllergyIntoleranceEntity(domainId, allergyIntolerance)
        }

        /**
         * Creates an AllergyIntoleranceEntity with domain ID and FHIR resource
         */
        fun of(id: AllergyIntoleranceId, allergyIntolerance: AllergyIntolerance): AllergyIntoleranceEntity {
            // Ensure the FHIR resource ID matches the domain ID
            allergyIntolerance.id = id.value
            return AllergyIntoleranceEntity(id, allergyIntolerance)
        }
    }

    /**
     * Convenience method to get the underlying FHIR resource as JSON
     */
    fun toJson(): String = allergyIntolerance.toJson()

    /**
     * Updates the underlying FHIR resource
     */
    fun updateAllergyIntolerance(updatedResource: AllergyIntolerance): AllergyIntoleranceEntity {
        // Ensure ID consistency
        updatedResource.id = this.id.value
        return AllergyIntoleranceEntity(this.id, updatedResource)
    }
}
