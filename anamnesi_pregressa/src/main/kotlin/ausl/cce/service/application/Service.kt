package ausl.cce.service.application

import ausl.cce.service.domain.AllergyIntoleranceEntity
import ausl.cce.service.domain.AllergyIntoleranceId
import ausl.cce.service.domain.DummyEntity
import ausl.cce.service.domain.DummyEntity.DummyId
import ausl.cce.service.infrastructure.persistence.AllergyIntoleranceRepository
import ausl.cce.service.infrastructure.persistence.DummyRepository
import mf.cce.utils.Service

interface AllergyIntoleranceService : Service {
    fun getAllergyIntoleranceById(id: AllergyIntoleranceId): AllergyIntoleranceEntity
    fun addAllergyIntolerance(entity: AllergyIntoleranceEntity)
    fun updateAllergyIntolerance(entity: AllergyIntoleranceEntity)
    fun deleteAllergyIntolerance(id: AllergyIntoleranceId)
}

class AllergyIntoleranceServiceImpl(
    private val allergyIntoleranceRepository: AllergyIntoleranceRepository,
) : AllergyIntoleranceService {
    override fun getAllergyIntoleranceById(id: AllergyIntoleranceId): AllergyIntoleranceEntity {
        return allergyIntoleranceRepository.findById(id) ?: throw NoSuchElementException("AllergyIntoleranceEntity with id '$id' not found")
    }

    override fun addAllergyIntolerance(entity: AllergyIntoleranceEntity) {
        allergyIntoleranceRepository.save(entity)
    }

    override fun updateAllergyIntolerance(entity: AllergyIntoleranceEntity) {
        allergyIntoleranceRepository.update(entity)
    }

    override fun deleteAllergyIntolerance(id: AllergyIntoleranceId) {
        allergyIntoleranceRepository.deleteById(id)
    }
}

interface DummyService : Service {
    fun getDummyEntityById(id: DummyId): DummyEntity
    fun addDummyEntity(dummyEntity: DummyEntity)
    fun updateDummyEntity(dummyEntity: DummyEntity)
    fun deleteDummyEntity(id: DummyId)
}

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