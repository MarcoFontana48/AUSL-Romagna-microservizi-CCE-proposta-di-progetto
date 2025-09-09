package ausl.cce.service.application

import ausl.cce.service.domain.EncounterEntity
import ausl.cce.service.domain.EncounterId
import ausl.cce.service.domain.DummyEntity
import mf.cce.utils.Repository

// Updated repository interfaces to include Closeable
interface DummyRepository : Repository<DummyEntity.DummyId, DummyEntity>, Closeable

interface EncounterRepository : Repository<EncounterId, EncounterEntity>, Closeable

interface Closeable {
    fun close()
}