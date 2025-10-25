package ausl.cce.service.application

import ausl.cce.service.domain.DummyEntity
import ausl.cce.service.domain.DummyEntity.DummyId
import mf.cce.utils.Service

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
     */
    override fun getDummyEntityById(id: DummyId): DummyEntity {
        return dummyRepository.findById(id) ?: throw NoSuchElementException("DummyEntity with id '$id' not found")
    }

    /**
     * Adds a new DummyEntity to the repository.
     */
    override fun addDummyEntity(dummyEntity: DummyEntity) {
        dummyRepository.save(dummyEntity)
    }

    /**
     * Updates an existing DummyEntity in the repository.
     */
    override fun updateDummyEntity(dummyEntity: DummyEntity) {
        dummyRepository.update(dummyEntity)
    }

    /**
     * Deletes a DummyEntity from the repository by its ID.
     */
    override fun deleteDummyEntity(id: DummyId) {
        dummyRepository.deleteById(id)
    }
}