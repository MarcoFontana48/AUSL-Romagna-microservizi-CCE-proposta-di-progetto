package ausl.cce.service.application

import ausl.cce.service.domain.DummyEntity
import ausl.cce.service.domain.DummyEntity.DummyId
import mf.cce.utils.Service

interface DummyService : Service {
    fun getDummyEntityById(id: DummyId): DummyEntity
}

class DummyServiceImpl(
    private val dummyRepository: DummyRepository
) : DummyService {
    override fun getDummyEntityById(id: DummyId): DummyEntity {
        return dummyRepository.findById(id) ?: throw NoSuchElementException("DummyEntity with id '$id' not found")
    }
}