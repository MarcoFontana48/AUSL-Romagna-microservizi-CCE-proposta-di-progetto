package ausl.cce.service.application

import ausl.cce.service.domain.EncounterEntity
import ausl.cce.service.domain.EncounterId
import ausl.cce.service.domain.DummyEntity
import mf.cce.utils.Repository

/**
 * interface for Dummy entity repositories.
 */
interface DummyRepository : Repository<DummyEntity.DummyId, DummyEntity>, Closeable

/**
 * interface for Encounter entity repositories.
 */
interface EncounterRepository : Repository<EncounterId, EncounterEntity>, Closeable

/**
 * Interface for closeable resources.
 */
interface Closeable {
    fun close()
}