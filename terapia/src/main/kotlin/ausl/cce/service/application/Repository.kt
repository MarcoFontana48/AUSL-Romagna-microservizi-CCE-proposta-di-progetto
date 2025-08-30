package ausl.cce.service.application

import ausl.cce.service.domain.DummyEntity
import ausl.cce.service.domain.DummyEntity.DummyId
import mf.cce.utils.Repository

interface DummyRepository : Repository<DummyId, DummyEntity>, Closeable

interface Closeable {
    fun close()
}
