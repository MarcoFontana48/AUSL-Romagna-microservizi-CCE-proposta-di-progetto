package ausl.cce.service.application

import ausl.cce.service.domain.DummyEntity
import ausl.cce.service.domain.DummyEntity.DummyId
import mf.cce.utils.Repository

/**
 * interface for Dummy entity repositories.
 */
interface DummyRepository : Repository<DummyId, DummyEntity>, Closeable

/**
 * Interface for closeable resources.
 */
interface Closeable {
    fun close()
}
