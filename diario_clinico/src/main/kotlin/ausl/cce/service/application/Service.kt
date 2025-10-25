package ausl.cce.service.application

import ausl.cce.service.domain.DummyEntity
import ausl.cce.service.domain.DummyEntity.DummyId
import ausl.cce.service.domain.EncounterEntity
import ausl.cce.service.domain.EncounterId
import mf.cce.utils.EncounterConcluded
import mf.cce.utils.Service

/**
 * interface for Encounter entity services.
 */
interface EncounterService : Service {
    fun getEncounterById(id: EncounterId): EncounterEntity
    fun addEncounter(entity: EncounterEntity)
    fun updateEncounter(entity: EncounterEntity)
    fun deleteEncounter(id: EncounterId)
}

/**
 * Implementation of the EncounterService interface.
 */
class EncounterServiceImpl(
    private val encounterRepository: EncounterRepository,
    private val encounterEventProducer: EncounterEventProducerVerticle,
) : EncounterService {
    /**
     * Retrieves an EncounterEntity by its ID.
     *
     * @param id The ID of the EncounterEntity to retrieve.
     * @return The EncounterEntity with the specified ID.
     * @throws NoSuchElementException if no EncounterEntity with the specified ID is found.
     */
    override fun getEncounterById(id: EncounterId): EncounterEntity {
        return encounterRepository.findById(id) ?: throw NoSuchElementException("EncounterEntity with id '$id' not found")
    }

    /**
     * Adds a new EncounterEntity to the repository and publishes an event.
     *
     * @param entity The EncounterEntity to add.
     */
    override fun addEncounter(entity: EncounterEntity) {
        encounterRepository.save(entity)
        encounterEventProducer.publishEvent(EncounterConcluded.of(entity.encounter))
    }

    /**
     * Updates an existing EncounterEntity in the repository.
     *
     * @param entity The EncounterEntity to update.
     */
    override fun updateEncounter(entity: EncounterEntity) {
        encounterRepository.update(entity)
    }

    /**
     * Deletes an EncounterEntity from the repository by its ID.
     *
     * @param id The ID of the EncounterEntity to delete.
     */
    override fun deleteEncounter(id: EncounterId) {
        encounterRepository.deleteById(id)
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
    private val dummyRepository: DummyRepository
) : DummyService {
    /**
     * Retrieves a DummyEntity by its ID.
     *
     * @param id The ID of the DummyEntity to retrieve.
     * @return The DummyEntity with the specified ID.
     * @throws NoSuchElementException if no DummyEntity with the specified ID is found.
     */
    override fun getDummyEntityById(id: DummyId): DummyEntity {
        return dummyRepository.findById(id) ?: throw NoSuchElementException("DummyEntity with id '$id' not found")
    }

    /**
     * Adds a new DummyEntity to the repository.
     *
     * @param dummyEntity The DummyEntity to add.
     */
    override fun addDummyEntity(dummyEntity: DummyEntity) {
        dummyRepository.save(dummyEntity)
    }

    /**
     * Updates an existing DummyEntity in the repository.
     *
     * @param dummyEntity The DummyEntity to update.
     */
    override fun updateDummyEntity(dummyEntity: DummyEntity) {
        dummyRepository.update(dummyEntity)
    }

    /**
     * Deletes a DummyEntity from the repository by its ID.
     *
     * @param id The ID of the DummyEntity to delete.
     */
    override fun deleteDummyEntity(id: DummyId) {
        dummyRepository.deleteById(id)
    }
}