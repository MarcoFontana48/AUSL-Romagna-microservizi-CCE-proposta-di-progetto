package ausl.cce.service.domain

import mf.cce.utils.Entity
import mf.cce.utils.Factory
import mf.cce.utils.ID

class DummyEntity private constructor(id: DummyId) : Entity<DummyEntity.DummyId>(id) {

    class DummyId(value : String) : ID<String>(value)

    companion object : Factory<DummyEntity> {
        override fun of(id: String): DummyEntity {
            return DummyEntity(DummyId(id))
        }
    }
}