package ausl.cce.service.application

import ausl.cce.service.domain.CarePlanEntity
import ausl.cce.service.domain.CarePlanId
import ausl.cce.service.domain.DummyEntity
import mf.cce.utils.Repository

/**
 * interface for Dummy entity repositories.
 */
interface DummyRepository : Repository<DummyEntity.DummyId, DummyEntity>, Closeable

/**
 * interface for CarePlan entity repositories.
 */
interface CarePlanRepository : Repository<CarePlanId, CarePlanEntity>, Closeable

/**
 * Interface for closeable resources.
 */
interface Closeable {
    fun close()
}