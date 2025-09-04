package ausl.cce.service.infrastructure.persistence

import ausl.cce.service.domain.CarePlanEntity
import ausl.cce.service.domain.CarePlanId
import ausl.cce.service.domain.DummyEntity
import mf.cce.utils.Repository

interface DummyRepository : Repository<DummyEntity.DummyId, DummyEntity>, Closeable

interface CarePlanRepository : Repository<CarePlanId, CarePlanEntity>, Closeable

interface Closeable {
    fun close()
}