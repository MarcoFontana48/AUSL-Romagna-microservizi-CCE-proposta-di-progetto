package ausl.cce.service.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import mf.cce.utils.Entity
import mf.cce.utils.Factory
import mf.cce.utils.ID

/**
 * Dummy entity class representing a sample entity in the domain.
 *
 * @property dummyField A sample field of the DummyEntity.
 */
class DummyEntity private constructor(
    @JsonProperty("id") id: DummyId,
    @JsonProperty("field") val dummyField: String
) : Entity<DummyEntity.DummyId>(id) {

    /**
     * Identifier class for DummyEntity.
     */
    data class DummyId @JsonCreator constructor(
        @JsonProperty("value") override val value: String
    ) : ID<String>(value)

    /**
     * Companion object implementing the Factory interface for DummyEntity.
     */
    companion object : Factory<DummyEntity> {
        fun of(id: DummyId, dummyField: String): DummyEntity {
            return DummyEntity(id, dummyField)
        }
    }
}