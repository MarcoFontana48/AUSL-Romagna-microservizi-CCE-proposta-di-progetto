package ausl.cce.service.domain

import org.hl7.fhir.r4.model.Encounter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.apache.logging.log4j.LogManager

/**
 * Test class for Encounter domain model and related classes.
 */
class EncounterTest {
    val logger = LogManager.getLogger(this::class.java)

    /**
     * Helper method to create a test Encounter instance used in multiple tests
     */
    fun createTestEncounterHelper(): Encounter {
        return EncounterFactory.of(
            id = "1",
            patientReference = "Patient/123",
            encounterClass = "AMB",
            status = "finished",
            serviceTypeSystem = "http://terminology.hl7.org/CodeSystem/service-type",
            serviceTypeCode = "408",
            serviceTypeDisplay = "General Medicine"
        )
    }

    @Test
    @DisplayName("Test creation of a FHIR Encounter object")
    fun encounterCreation() {
        val encounter = EncounterFactory.of(
            id = "111",
            patientReference = "Patient/123",
            encounterClass = "AMB",
            status = "finished",
            serviceTypeSystem = "http://terminology.hl7.org/CodeSystem/service-type",
            serviceTypeCode = "408",
            serviceTypeDisplay = "General Medicine"
        )

        assertAll("Encounter fields should be set correctly",
            { assertNotNull(encounter) },
            { assertEquals("111", encounter.id) },
            { assertEquals("Patient/123", encounter.subject.reference) },
            { assertEquals("AMB", encounter.class_.code) },
            { assertEquals("finished", encounter.status.toCode()) },
            { assertEquals("General Medicine", encounter.serviceType.codingFirstRep.display) }
        )
    }

    @Test
    @DisplayName("Test serialization of Encounter to JSON")
    fun toJson() {
        val encounter = createTestEncounterHelper()
        val json = encounter.toJson()

        logger.debug(json)

        assertAll("Encounter fields should be serialized correctly",
            { assertNotNull(json) },
            { assertTrue(json.contains("\"id\" : \"1\"")) },
            { assertTrue(json.contains("\"reference\" : \"Patient/123\"")) },
            { assertTrue(json.contains("\"code\" : \"AMB\"")) },
            { assertTrue(json.contains("\"status\" : \"finished\"")) },
            { assertTrue(json.contains("\"display\" : \"General Medicine\"")) }
        )
    }

    @Test
    @DisplayName("Test deserialization of JSON to Encounter")
    fun fromJsonEncounter() {
        val encounter = createTestEncounterHelper()
        val json = encounter.toJson()

        val parsedEncounter = json.fromJsonToEncounter()

        assertAll("Encounter fields should be parsed correctly",
            { assertNotNull(parsedEncounter) },
            { assertEquals("Encounter/1", parsedEncounter.id) },
            { assertEquals("Patient/123", parsedEncounter.subject.reference) },
            { assertEquals("AMB", parsedEncounter.class_.code) },
            { assertEquals("finished", parsedEncounter.status.toCode()) },
            { assertEquals("General Medicine", parsedEncounter.serviceType.codingFirstRep.display) }
        )
    }

    @Test
    @DisplayName("Test Encounter with participant")
    fun encounterWithParticipant() {
        val encounter = createTestEncounterHelper()
            .addParticipant("Practitioner/456", "PPRF")

        assertAll("Encounter with participant should be created correctly",
            { assertNotNull(encounter) },
            { assertEquals(1, encounter.participant.size) },
            { assertEquals("Practitioner/456", encounter.participant[0].individual.reference) },
            { assertEquals("PPRF", encounter.participant[0].typeFirstRep.codingFirstRep.code) },
            { assertEquals("Primary Performer", encounter.participant[0].typeFirstRep.codingFirstRep.display) }
        )
    }

    @Test
    @DisplayName("Test Encounter with reason")
    fun encounterWithReason() {
        val encounter = createTestEncounterHelper()
            .addReason(
                "http://snomed.info/sct",
                "25064002",
                "Headache"
            )

        assertAll("Encounter with reason should be created correctly",
            { assertNotNull(encounter) },
            { assertEquals(1, encounter.reasonCode.size) },
            { assertEquals("25064002", encounter.reasonCode[0].codingFirstRep.code) },
            { assertEquals("Headache", encounter.reasonCode[0].codingFirstRep.display) }
        )
    }

    @Test
    @DisplayName("Test Encounter with location")
    fun encounterWithLocation() {
        val encounter = createTestEncounterHelper()
            .addLocation("Location/789", "active")

        assertAll("Encounter with location should be created correctly",
            { assertNotNull(encounter) },
            { assertEquals(1, encounter.location.size) },
            { assertEquals("Location/789", encounter.location[0].location.reference) },
            { assertEquals("active", encounter.location[0].status.toCode()) }
        )
    }

    // === Tests for EncounterId ===

    @Test
    @DisplayName("Test creation of EncounterId")
    fun encounterIdCreation() {
        val idValue = "encounter-123"
        val encounterId = EncounterId(idValue)

        assertAll("EncounterId should be created correctly",
            { assertNotNull(encounterId) },
            { assertEquals(idValue, encounterId.value) }
        )
    }

    @Test
    @DisplayName("Test EncounterId equality")
    fun encounterIdEquality() {
        val idValue = "encounter-456"
        val id1 = EncounterId(idValue)
        val id2 = EncounterId(idValue)
        val id3 = EncounterId("different-id")

        assertAll("EncounterId equality should work correctly",
            { assertEquals(id1, id2) },
            { assertNotEquals(id1, id3) },
            { assertEquals(id1.hashCode(), id2.hashCode()) },
            { assertNotEquals(id1.hashCode(), id3.hashCode()) }
        )
    }

    // === Tests for EncounterEntity ===

    @Test
    @DisplayName("Test EncounterEntity creation from FHIR resource")
    fun encounterEntityFromFhirResource() {
        val fhirResource = createTestEncounterHelper()
        val entity = EncounterEntity.of(fhirResource)

        assertAll("EncounterEntity should be created from FHIR resource correctly",
            { assertNotNull(entity) },
            { assertEquals("1", entity.id.value) },
            { assertEquals(fhirResource, entity.encounter) },
            { assertEquals("1", entity.encounter.id) }
        )
    }

    @Test
    @DisplayName("Test EncounterEntity creation with domain ID and FHIR resource")
    fun encounterEntityWithDomainId() {
        val domainId = EncounterId("domain-789")
        val fhirResource = createTestEncounterHelper()
        val entity = EncounterEntity.of(domainId, fhirResource)

        assertAll("EncounterEntity should be created with domain ID correctly",
            { assertNotNull(entity) },
            { assertEquals(domainId, entity.id) },
            { assertEquals("domain-789", entity.id.value) },
            { assertEquals(fhirResource, entity.encounter) },
            { assertEquals("domain-789", entity.encounter.id) } // FHIR ID should be updated
        )
    }

    @Test
    @DisplayName("Test EncounterEntity toJson method")
    fun encounterEntityToJson() {
        val fhirResource = createTestEncounterHelper()
        val fhirResourceJson = fhirResource.toJson()
        val entity = EncounterEntity.of(fhirResource)
        val json = entity.toJson()

        assertAll("EncounterEntity JSON serialization should work correctly",
            { assertNotNull(json) },
            { assertTrue(json.contains("\"resourceType\" : \"Encounter\"")) },
            { assertTrue(json.contains("\"id\" : \"1\"")) },
            { assertTrue(json.contains("\"display\" : \"General Medicine\"")) },
            { assertEquals(fhirResourceJson, json) } // JSON should match FHIR resource JSON
        )
    }

    @Test
    @DisplayName("Test EncounterEntity updateEncounter method")
    fun encounterEntityUpdate() {
        val originalResource = createTestEncounterHelper()
        val entity = EncounterEntity.of(originalResource)

        // Create an updated resource with different properties
        val updatedResource = EncounterFactory.of(
            id = "different-id",
            patientReference = "Patient/456",
            encounterClass = "IMP",
            status = "in-progress",
            serviceTypeSystem = "http://terminology.hl7.org/CodeSystem/service-type",
            serviceTypeCode = "394802001",
            serviceTypeDisplay = "Cardiology"
        )

        val updatedEntity = entity.updateEncounter(updatedResource)

        assertAll("EncounterEntity update should preserve domain ID",
            { assertNotNull(updatedEntity) },
            { assertEquals(entity.id, updatedEntity.id) }, // Same domain ID
            { assertEquals("1", updatedEntity.id.value) }, // Original domain ID preserved
            { assertEquals("1", updatedEntity.encounter.id) }, // FHIR ID updated to match domain ID
            { assertEquals(updatedResource, updatedEntity.encounter) },
            { assertEquals("IMP", updatedEntity.encounter.class_.code) },
            { assertEquals("in-progress", updatedEntity.encounter.status.toCode()) },
            { assertEquals("Cardiology", updatedEntity.encounter.serviceType.codingFirstRep.display) }
        )
    }

    @Test
    @DisplayName("Test EncounterEntity equality based on ID")
    fun encounterEntityEquality() {
        val id1 = EncounterId("same-id")
        val id2 = EncounterId("same-id")
        val id3 = EncounterId("different-id")

        val resource1 = createTestEncounterHelper()
        val resource2 = createTestEncounterHelper()
        val resource3 = createTestEncounterHelper()

        val entity1 = EncounterEntity.of(id1, resource1)
        val entity2 = EncounterEntity.of(id2, resource2)
        val entity3 = EncounterEntity.of(id3, resource3)

        assertAll("EncounterEntity equality should be based on domain ID",
            { assertEquals(entity1, entity2) }, // Same ID, should be equal
            { assertNotEquals(entity1, entity3) }, // Different ID, should not be equal
            { assertEquals(entity1.hashCode(), entity2.hashCode()) },
            { assertNotEquals(entity1.hashCode(), entity3.hashCode()) }
        )
    }

    @Test
    @DisplayName("Test different encounter classes")
    fun differentEncounterClasses() {
        val ambulatoryEncounter = EncounterFactory.of("1", "Patient/123", "AMB")
        val inpatientEncounter = EncounterFactory.of("2", "Patient/123", "IMP")
        val emergencyEncounter = EncounterFactory.of("3", "Patient/123", "EMER")

        assertAll("Different encounter classes should be created correctly",
            { assertEquals("AMB", ambulatoryEncounter.class_.code) },
            { assertEquals("Ambulatory", ambulatoryEncounter.class_.display) },
            { assertEquals("IMP", inpatientEncounter.class_.code) },
            { assertEquals("Inpatient Encounter", inpatientEncounter.class_.display) },
            { assertEquals("EMER", emergencyEncounter.class_.code) },
            { assertEquals("Emergency", emergencyEncounter.class_.display) }
        )
    }

    @Test
    @DisplayName("Test encounter without service type")
    fun encounterWithoutServiceType() {
        val encounter = EncounterFactory.of(
            id = "1",
            patientReference = "Patient/123",
            encounterClass = "AMB",
            status = "finished"
        )

        assertAll("Encounter without service type should be created correctly",
            { assertNotNull(encounter) },
            { assertEquals("1", encounter.id) },
            { assertEquals("Patient/123", encounter.subject.reference) },
            { assertEquals("AMB", encounter.class_.code) },
            { assertEquals("finished", encounter.status.toCode()) },
            { assertNotNull(encounter.serviceType) }
        )
    }
}