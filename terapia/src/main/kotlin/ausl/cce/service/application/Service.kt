package ausl.cce.service.application

import ausl.cce.service.domain.CarePlanEntity
import ausl.cce.service.domain.CarePlanId
import ausl.cce.service.domain.DummyEntity
import ausl.cce.service.domain.DummyEntity.DummyId
import io.micrometer.core.instrument.MeterRegistry
import mf.cce.utils.AllergyDiagnosed
import mf.cce.utils.MetricsProvider
import mf.cce.utils.Service
import mf.cce.utils.TherapyRevoked
import org.apache.logging.log4j.LogManager
import org.hl7.fhir.r4.model.AllergyIntolerance
import org.hl7.fhir.r4.model.CarePlan

/**
 * interface for CarePlan entity services.
 */
interface CarePlanService : Service {
    fun getCarePlanById(id: CarePlanId): CarePlanEntity
    fun addCarePlan(entity: CarePlanEntity)
    fun updateCarePlan(entity: CarePlanEntity)
    fun deleteCarePlan(id: CarePlanId)
    fun checkAndSuspendCarePlanIfConflict(allergyDiagnosed: AllergyDiagnosed)
    fun checkMedicationAllergy(
        carePlan: CarePlan,
        allergy: AllergyIntolerance
    ): Boolean
}

/**
 * Implementation of the CarePlanService interface.
 */
class CarePlanServiceImpl(
    private val carePlanRepository: CarePlanRepository,
    private val terapiaEventProducer: TerapiaProducerVerticle,
    override val meterRegistry: MeterRegistry,
    override val serviceName: String,
) : CarePlanService, MetricsProvider, CarePlanMetricsProvider {
    private val logger = LogManager.getLogger(this::class)

    /**
     * Check all CarePlans for medication-allergy conflicts based on a newly diagnosed allergy.
     * If a conflict is found, the CarePlan is suspended (status set to REVOKED).
     *
     * @param allergyDiagnosed The newly diagnosed allergy information.
     */
    override fun checkAndSuspendCarePlanIfConflict(allergyDiagnosed: AllergyDiagnosed) {
        try {
            val allCarePlans = carePlanRepository.findAll()
            logger.debug("Retrieved ${allCarePlans.count()} CarePlans from repository")

            // instead of checking each carePlan, a better algorithm could be used here to reduce time complexity
            allCarePlans.forEach { carePlanEntity ->
                val carePlan = carePlanEntity.carePlan
                val allergy = allergyDiagnosed.allergyIntolerance

                if (checkMedicationAllergy(carePlan, allergy)) {
                    logger.info("Conflict detected between CarePlan '${carePlan.id}' and AllergyIntolerance '${allergy.id}'. Suspending CarePlan.")
                    // suspend the CarePlan by updating its status
                    carePlan.status =
                        CarePlan.CarePlanStatus.REVOKED   // a 'SUSPENDED' status is not available in CarePlan resource, using 'REVOKED' as an alternative

                    metricsCounter.increment()
                    metricsWriteRequestsCounter.increment()
                    updateCarePlanCounter.increment()

                    carePlanRepository.update(CarePlanEntity.of(carePlan))
                    terapiaEventProducer.publishEvent(TherapyRevoked.of(carePlan))
                    logger.debug("Updated CarePlan '${carePlan.id}' status to REVOKED in repository")

                    metricsSuccessCounter.increment()
                    metricsSuccessWriteRequestsCounter.increment()
                    updateCarePlanSuccessCounter.increment()
                } else {
                    logger.debug("No conflict detected between CarePlan '${carePlan.id}' and AllergyIntolerance '${allergy.id}'")
                }
            }
        } catch (e: Exception) {
            logger.error("Error while checking and suspending CarePlans: ${e.message}")
            metricsFailureCounter.increment()
            metricsFailureWriteRequestsCounter.increment()
            updateCarePlanFailureCounter.increment()
            throw e
        }
    }

    /**
     * Retrieves a CarePlanEntity by its ID.
     */
    override fun getCarePlanById(id: CarePlanId): CarePlanEntity {
        return carePlanRepository.findById(id) ?: throw NoSuchElementException("CarePlanEntity with id '$id' not found")
    }

    /**
     * Adds a new CarePlanEntity to the repository.
     */
    override fun addCarePlan(entity: CarePlanEntity) {
        carePlanRepository.save(entity)
    }

    /**
     * Updates an existing CarePlanEntity in the repository.
     */
    override fun updateCarePlan(entity: CarePlanEntity) {
        carePlanRepository.update(entity)
    }

    /**
     * Deletes a CarePlanEntity from the repository by its ID.
     */
    override fun deleteCarePlan(id: CarePlanId) {
        carePlanRepository.deleteById(id)
    }

    /**
     * Check if a CarePlan contains medications that conflict with known allergies
     * @param carePlan The CarePlan to check
     * @param allergy known AllergyIntolerance resource for the patient
     * @return true if there are any medication-allergy conflicts, false otherwise
     */
    override fun checkMedicationAllergy(
        carePlan: CarePlan,
        allergy: AllergyIntolerance
    ): Boolean {
        carePlan.activity?.forEach { activity ->
            activity.detail?.let { detail ->
                // Check productCodeableConcept for medication codes that might conflict with allergies (same codes between medication and allergy)
                detail.productCodeableConcept?.coding?.forEach { medicationCoding ->
                    allergy.code?.coding?.forEach { allergyCoding ->
                        if (medicationCoding.system == allergyCoding.system &&
                            medicationCoding.code == allergyCoding.code) {
                            return true // Found a conflict, return immediately
                        }
                    }
                }
            }
        }

        return false // No conflicts found
    }
}

/**
 * interface for Dummy entity services.
 */
interface DummyService : Service {
    fun getDummyEntityById(id: DummyId): DummyEntity
    fun addDummyEntity(dummyEntity: DummyEntity)
    fun updateDummyEntity(dummyEntity: DummyEntity)
    fun deleteDummyEntity(id: DummyId)
}

/**
 * Implementation of the DummyService interface.
 */
class DummyServiceImpl(
    private val dummyRepository: DummyRepository,
) : DummyService {
    override fun getDummyEntityById(id: DummyId): DummyEntity {
        return dummyRepository.findById(id) ?: throw NoSuchElementException("DummyEntity with id '$id' not found")
    }

    override fun addDummyEntity(dummyEntity: DummyEntity) {
        dummyRepository.save(dummyEntity)
    }

    override fun updateDummyEntity(dummyEntity: DummyEntity) {
        dummyRepository.update(dummyEntity)
    }

    override fun deleteDummyEntity(id: DummyId) {
        dummyRepository.deleteById(id)
    }
}