package ausl.cce.service.application

import ausl.cce.service.domain.DummyEntity
import ausl.cce.service.domain.DummyEntity.DummyId
import ausl.cce.service.domain.EncounterEntity
import ausl.cce.service.domain.EncounterId
import ausl.cce.service.infrastructure.controller.EncounterEventProducerVerticle
import ausl.cce.service.infrastructure.persistence.DummyRepository
import ausl.cce.service.infrastructure.persistence.EncounterRepository
import mf.cce.utils.EncounterConcluded
import mf.cce.utils.Service

interface EncounterService : Service {
    fun getEncounterById(id: EncounterId): EncounterEntity
    fun addEncounter(entity: EncounterEntity)
    fun updateEncounter(entity: EncounterEntity)
    fun deleteEncounter(id: EncounterId)
}

class EncounterServiceImpl(
    private val encounterRepository: EncounterRepository,
    private val encounterEventProducer: EncounterEventProducerVerticle,
) : EncounterService {
    override fun getEncounterById(id: EncounterId): EncounterEntity {
        return encounterRepository.findById(id) ?: throw NoSuchElementException("EncounterEntity with id '$id' not found")
    }

    override fun addEncounter(entity: EncounterEntity) {
        encounterRepository.save(entity)
        encounterEventProducer.publishEvent(EncounterConcluded.of(entity.encounter))
    }

    override fun updateEncounter(entity: EncounterEntity) {
        encounterRepository.update(entity)
    }

    override fun deleteEncounter(id: EncounterId) {
        encounterRepository.deleteById(id)
    }
}

interface DummyService : Service {
    fun getDummyEntityById(id: DummyId): DummyEntity
    fun addDummyEntity(dummyEntity: DummyEntity)
    fun updateDummyEntity(dummyEntity: DummyEntity)
    fun deleteDummyEntity(id: DummyId)
}

class DummyServiceImpl(
    private val dummyRepository: DummyRepository
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