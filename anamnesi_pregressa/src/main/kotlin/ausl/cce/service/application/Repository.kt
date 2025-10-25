package ausl.cce.service.application

import ausl.cce.service.domain.AllergyIntoleranceEntity
import ausl.cce.service.domain.AllergyIntoleranceId
import ausl.cce.service.domain.DummyEntity
import mf.cce.utils.Repository

/**
 * interface for Dummy entity repositories.
 */
interface DummyRepository : Repository<DummyEntity.DummyId, DummyEntity>, Closeable

/**
 * interface for AllergyIntolerance entity repositories.
 */
interface AllergyIntoleranceRepository : Repository<AllergyIntoleranceId, AllergyIntoleranceEntity>, Closeable

/**
 * Interface for closeable resources.
 */
interface Closeable {
    fun close()
}