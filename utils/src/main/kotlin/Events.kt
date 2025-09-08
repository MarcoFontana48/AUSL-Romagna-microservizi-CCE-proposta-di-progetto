package mf.cce.utils

import org.hl7.fhir.r4.model.AllergyIntolerance
import org.hl7.fhir.r4.model.CarePlan
import org.hl7.fhir.r4.model.Encounter

data class AllergyDiagnosed(val allergyIntolerance: AllergyIntolerance) : DomainEvent {
    companion object {
        const val TOPIC = "allergy-diagnosed"

        fun of(allergyIntolerance: AllergyIntolerance): AllergyDiagnosed {
            return AllergyDiagnosed(allergyIntolerance)
        }
    }
}

data class TherapyRevoked(val carePlan: CarePlan) : DomainEvent {
    companion object {
        const val TOPIC = "therapy-revoked"

        fun of(carePlan: CarePlan): TherapyRevoked {
            return TherapyRevoked(carePlan)
        }
    }
}

data class EncounterConcluded(val encounter: Encounter) : DomainEvent {
    companion object {
        const val TOPIC = "encounter-concluded"

        fun of(encounter: Encounter): EncounterConcluded {
            return EncounterConcluded(encounter)
        }
    }
}