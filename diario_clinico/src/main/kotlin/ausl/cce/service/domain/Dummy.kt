package ausl.cce.service.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import mf.cce.utils.Entity
import mf.cce.utils.Factory
import mf.cce.utils.ID

class DummyEntity private constructor(
    @JsonProperty("id") id: DummyId,
    @JsonProperty("field") val dummyField: String
) : Entity<DummyEntity.DummyId>(id) {

    data class DummyId @JsonCreator constructor(
        @JsonProperty("value") override val value: String
    ) : ID<String>(value)

    companion object : Factory<DummyEntity> {
        fun of(id: DummyId, dummyField: String): DummyEntity {
            return DummyEntity(id, dummyField)
        }
    }
}