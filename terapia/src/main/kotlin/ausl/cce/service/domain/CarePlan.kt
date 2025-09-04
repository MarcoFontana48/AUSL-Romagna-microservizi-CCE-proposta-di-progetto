package ausl.cce.service.domain

import com.fasterxml.jackson.annotation.JsonCreator
import mf.cce.utils.Entity
import mf.cce.utils.Factory
import mf.cce.utils.ID
import org.hl7.fhir.r4.model.CarePlan
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import com.fasterxml.jackson.annotation.JsonProperty
import org.hl7.fhir.r4.model.*
import java.util.*

/**
 * Extension functions for CarePlan FHIR resources with JSON serialization support.
 * Uses HAPI FHIR's native JSON parser for proper FHIR-compliant serialization.
 *
 * Based on HAPI FHIR documentation: https://hapifhir.io/hapi-fhir/docs/getting_started/downloading_and_importing.html
 * Required dependencies: hapi-fhir-base + hapi-fhir-structures-r4
 */

// FHIR context and parser - shared across all extension functions
private val fhirContext: FhirContext = FhirContext.forR4()
private val jsonParser: IParser = fhirContext.newJsonParser().setPrettyPrint(true)

/**
 * Factory object containing static methods for CarePlan creation
 */
object CarePlanFactory {
    /**
     * Factory method to create a new CarePlan with basic information
     *
     * @param id Logical ID of the CarePlan resource (without "CarePlan/" prefix)
     * @param patientReference Reference to the patient (e.g., "Patient/123")
     * @param title Title of the care plan (e.g., "Diabetes Management Plan")
     * @param description Description of the care plan
     * @param status Status of the care plan (default: "active")
     * @param intent Intent of the care plan (default: "plan")
     * @return Configured CarePlan instance
     */
    fun of(
        id: String,
        patientReference: String,
        title: String,
        description: String? = null,
        status: String = "active",
        intent: String = "plan"
    ): CarePlan {
        return CarePlan().configure(
            id, patientReference, title, description, status, intent
        )
    }
}

/**
 * Extension function for CarePlan class to serialize CarePlan resource to JSON string
 */
fun CarePlan.toJson(): String {
    return jsonParser.encodeResourceToString(this)
}

/**
 * Extension function for String class to deserialize JSON string to CarePlan resource
 */
fun String.fromJsonToCarePlan(): CarePlan {
    return jsonParser.parseResource(CarePlan::class.java, this)
}

/**
 * Extension function to create a new CarePlan with basic information
 */
fun CarePlan.configure(
    id: String,
    patientReference: String,
    title: String,
    description: String? = null,
    status: String = "active",
    intent: String = "plan"
): CarePlan {

    // Set basic fields
    this.id = id    // here 'id' is the 'logical id', not the 'resource id', so no "CarePlan/" prefix! HAPI FHIR handles that automatically
    this.subject = Reference(patientReference)

    // Set title
    this.title = title

    // Set description if provided
    description?.let { this.description = it }

    // Set status
    this.status = CarePlan.CarePlanStatus.fromCode(status)

    // Set intent
    this.intent = CarePlan.CarePlanIntent.fromCode(intent)

    // Set creation period to now
    this.period = Period().apply {
        start = Date()
    }

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

/**
 * Extension function to add a category to the CarePlan
 */
fun CarePlan.addCategory(system: String, code: String, display: String): CarePlan {
    this.addCategory(createCodeableConcept(system, code, display))
    return this
}

/**
 * Extension function to add an activity to the CarePlan
 */
fun CarePlan.addActivity(
    activitySystem: String,
    activityCode: String,
    activityDisplay: String,
    description: String? = null,
    status: String = "not-started"
): CarePlan {
    val activity = CarePlan.CarePlanActivityComponent().apply {
        detail = CarePlan.CarePlanActivityDetailComponent().apply {
            code = createCodeableConcept(activitySystem, activityCode, activityDisplay)
            description?.let { this.description = it }
            this.status = CarePlan.CarePlanActivityStatus.fromCode(status)
        }
    }
    this.addActivity(activity)
    return this
}

/**
 * Extension function to add an activity with medication product reference to the CarePlan
 */
fun CarePlan.addMedicationActivity(
    activitySystem: String,
    activityCode: String,
    activityDisplay: String,
    medicationReference: String,  // e.g., "Medication/123"
    description: String? = null,
    status: String = "not-started",
    dailyAmount: String? = null,  // e.g., "1 tablet"
    quantity: String? = null      // e.g., "30 tablets"
): CarePlan {
    val activity = CarePlan.CarePlanActivityComponent().apply {
        detail = CarePlan.CarePlanActivityDetailComponent().apply {
            code = createCodeableConcept(activitySystem, activityCode, activityDisplay)
            description?.let { this.description = it }
            this.status = CarePlan.CarePlanActivityStatus.fromCode(status)

            // Set medication reference
            setProduct(Reference(medicationReference))

            // Set daily amount if provided
            dailyAmount?.let {
                this.dailyAmount = Quantity().apply {
                    // Parse the amount string - you might want more sophisticated parsing
                    val parts = it.split(" ", limit = 2)
                    if (parts.size >= 2) {
                        value = parts[0].toBigDecimalOrNull()?.let { bd -> bd }
                        unit = parts[1]
                    }
                }
            }

            // Set quantity if provided
            quantity?.let {
                this.quantity = Quantity().apply {
                    val parts = it.split(" ", limit = 2)
                    if (parts.size >= 2) {
                        value = parts[0].toBigDecimalOrNull()?.let { bd -> bd }
                        unit = parts[1]
                    }
                }
            }
        }
    }
    this.addActivity(activity)
    return this
}

/**
 * Extension function to add an activity with medication CodeableConcept to the CarePlan
 */
fun CarePlan.addMedicationActivity(
    activitySystem: String,
    activityCode: String,
    activityDisplay: String,
    medicationSystem: String,     // e.g., "http://www.nlm.nih.gov/research/umls/rxnorm"
    medicationCode: String,       // e.g., "161"
    medicationDisplay: String,    // e.g., "Aspirin"
    description: String? = null,
    status: String = "not-started",
    dailyAmount: String? = null,
    quantity: String? = null
): CarePlan {
    val activity = CarePlan.CarePlanActivityComponent().apply {
        detail = CarePlan.CarePlanActivityDetailComponent().apply {
            code = createCodeableConcept(activitySystem, activityCode, activityDisplay)
            description?.let { this.description = it }
            this.status = CarePlan.CarePlanActivityStatus.fromCode(status)

            // Set medication as CodeableConcept
            setProduct(createCodeableConcept(medicationSystem, medicationCode, medicationDisplay))

            // Set amounts as before
            dailyAmount?.let {
                this.dailyAmount = Quantity().apply {
                    val parts = it.split(" ", limit = 2)
                    if (parts.size >= 2) {
                        value = parts[0].toBigDecimalOrNull()?.let { bd -> bd }
                        unit = parts[1]
                    }
                }
            }

            quantity?.let {
                this.quantity = Quantity().apply {
                    val parts = it.split(" ", limit = 2)
                    if (parts.size >= 2) {
                        value = parts[0].toBigDecimalOrNull()?.let { bd -> bd }
                        unit = parts[1]
                    }
                }
            }
        }
    }
    this.addActivity(activity)
    return this
}

/**
 * Extension function to add an activity that references a MedicationRequest
 */
fun CarePlan.addMedicationRequestActivity(
    medicationRequestReference: String  // e.g., "MedicationRequest/456"
): CarePlan {
    val activity = CarePlan.CarePlanActivityComponent().apply {
        reference = Reference(medicationRequestReference)
    }
    this.addActivity(activity)
    return this
}

/**
 * Extension function to add a goal reference to the CarePlan
 */
fun CarePlan.addGoal(goalReference: String): CarePlan {
    this.addGoal(Reference(goalReference))
    return this
}

/**
 * Domain ID for CarePlan entities
 */
data class CarePlanId @JsonCreator constructor(
    override val value: String
) : ID<String>(value)

/**
 * Domain entity wrapper for FHIR CarePlan resource
 */
class CarePlanEntity private constructor(
    id: CarePlanId,
    val carePlan: CarePlan
) : Entity<CarePlanId>(id) {

    companion object : Factory<CarePlanEntity> {
        /**
         * Creates a CarePlanEntity from a FHIR CarePlan resource
         */
        fun of(carePlan: CarePlan): CarePlanEntity {
            val domainId = CarePlanId(carePlan.id)
            return CarePlanEntity(domainId, carePlan)
        }

        /**
         * Creates a CarePlanEntity with domain ID and FHIR resource
         */
        fun of(id: CarePlanId, carePlan: CarePlan): CarePlanEntity {
            // Ensure the FHIR resource ID matches the domain ID
            carePlan.id = id.value
            return CarePlanEntity(id, carePlan)
        }
    }

    /**
     * Convenience method to get the underlying FHIR resource as JSON
     */
    fun toJson(): String = carePlan.toJson()

    /**
     * Updates the underlying FHIR resource
     */
    fun updateCarePlan(updatedResource: CarePlan): CarePlanEntity {
        // Ensure ID consistency
        updatedResource.id = this.id.value
        return CarePlanEntity(this.id, updatedResource)
    }

    /**
     * Convenience method to add a category to the underlying CarePlan
     */
    fun addCategory(system: String, code: String, display: String): CarePlanEntity {
        carePlan.addCategory(system, code, display)
        return this
    }

    /**
     * Convenience method to add an activity to the underlying CarePlan
     */
    fun addActivity(
        activitySystem: String,
        activityCode: String,
        activityDisplay: String,
        description: String? = null,
        status: String = "not-started"
    ): CarePlanEntity {
        carePlan.addActivity(activitySystem, activityCode, activityDisplay, description, status)
        return this
    }

    /**
     * Convenience method to add a medication activity with product reference to the underlying CarePlan
     */
    fun addMedicationActivity(
        activitySystem: String,
        activityCode: String,
        activityDisplay: String,
        medicationReference: String,
        description: String? = null,
        status: String = "not-started",
        dailyAmount: String? = null,
        quantity: String? = null
    ): CarePlanEntity {
        carePlan.addMedicationActivity(
            activitySystem, activityCode, activityDisplay,
            medicationReference, description, status, dailyAmount, quantity
        )
        return this
    }

    /**
     * Convenience method to add a medication activity with CodeableConcept to the underlying CarePlan
     */
    fun addMedicationActivity(
        activitySystem: String,
        activityCode: String,
        activityDisplay: String,
        medicationSystem: String,
        medicationCode: String,
        medicationDisplay: String,
        description: String? = null,
        status: String = "not-started",
        dailyAmount: String? = null,
        quantity: String? = null
    ): CarePlanEntity {
        carePlan.addMedicationActivity(
            activitySystem, activityCode, activityDisplay,
            medicationSystem, medicationCode, medicationDisplay,
            description, status, dailyAmount, quantity
        )
        return this
    }

    /**
     * Convenience method to add a MedicationRequest activity to the underlying CarePlan
     */
    fun addMedicationRequestActivity(
        medicationRequestReference: String
    ): CarePlanEntity {
        carePlan.addMedicationRequestActivity(medicationRequestReference)
        return this
    }

    /**
     * Convenience method to add a goal reference to the underlying CarePlan
     */
    fun addGoal(goalReference: String): CarePlanEntity {
        carePlan.addGoal(goalReference)
        return this
    }
}


/**
 * A dummy entity for demonstration purposes, not to be used in production.
 */
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