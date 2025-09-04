package ausl.cce.service.infrastructure.persistence

import ausl.cce.service.domain.AllergyIntoleranceEntity
import ausl.cce.service.domain.AllergyIntoleranceId
import ausl.cce.service.domain.DummyEntity
import mf.cce.utils.Repository

interface DummyRepository : Repository<DummyEntity.DummyId, DummyEntity>, Closeable

interface AllergyIntoleranceRepository : Repository<AllergyIntoleranceId, AllergyIntoleranceEntity>, Closeable

interface Closeable {
    fun close()
}