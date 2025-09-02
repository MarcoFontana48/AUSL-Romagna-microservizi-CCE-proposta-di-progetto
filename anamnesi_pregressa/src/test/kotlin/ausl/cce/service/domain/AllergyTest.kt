package ausl.cce.service.domain

import org.hl7.fhir.r4.model.AllergyIntolerance
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.apache.logging.log4j.LogManager

class AllergyTest {
    val logger = LogManager.getLogger(this::class.java)

    /**
     * Helper method to create a test AllergyIntolerance instance used in multiple tests
     */
    fun createTestAllergyHelper(): AllergyIntolerance {
        return AllergyIntoleranceFactory.of(
            id = "1",
            patientReference = "Patient/123",
            allergenSystem = "http://snomed.info/sct",
            allergenCode = "227493005",
            allergenDisplay = "Cashew nuts",
            clinicalStatus = "active",
        )
    }

    @Test
    @DisplayName("Test creation of a FHIR Allergy object")
    fun allergyCreation() {
        val allergy = AllergyIntoleranceFactory.of(
            id = "111",
            patientReference = "Patient/123",
            allergenSystem = "http://snomed.info/sct",
            allergenCode = "227493005",
            allergenDisplay = "Cashew nuts",
            clinicalStatus = "active",
        )

        assertAll("Allergy fields should be set correctly",
            { assertNotNull(allergy) },
            { assertEquals("111", allergy.id) },
            { assertEquals("Patient/123", allergy.patient.reference) },
            { assertEquals("Cashew nuts", allergy.code.codingFirstRep.display) },
            { assertEquals("active", allergy.clinicalStatus.codingFirstRep.code) },
        )
    }

    @Test
    @DisplayName("Test serialization of Allergy to JSON")
    fun toJson() {
        val allergy = createTestAllergyHelper()
        val json = allergy.toJson()

        logger.debug(json)

        assertAll("Allergy fields should be parsed correctly",
            { assertNotNull(json) },
            { assertTrue(json.contains("\"id\" : \"1\"")) },
            { assertTrue(json.contains("\"reference\" : \"Patient/123\"")) },
            { assertTrue(json.contains("\"display\" : \"Cashew nuts\"")) },
            { assertTrue(json.contains("\"code\" : \"active\"")) },
        )
    }

    @Test
    @DisplayName("Test deserialization of JSON to Allergy")
    fun fromJsonAllergyIntolerance() {
        val allergy = createTestAllergyHelper()
        val json = allergy.toJson()

        val parsedAllergy = json.fromJsonToAllergyIntolerance()

        assertAll("Allergy fields should be parsed correctly",
            { assertNotNull(parsedAllergy) },
            { assertEquals("AllergyIntolerance/1", parsedAllergy.id) },
            { assertEquals("Patient/123", parsedAllergy.patient.reference) },
            { assertEquals("Cashew nuts", parsedAllergy.code.codingFirstRep.display) },
            { assertEquals("active", parsedAllergy.clinicalStatus.codingFirstRep.code) },
        )
    }

    // === Tests for AllergyIntoleranceId ===

    @Test
    @DisplayName("Test creation of AllergyIntoleranceId")
    fun allergyIntoleranceIdCreation() {
        val idValue = "allergy-123"
        val allergyId = AllergyIntoleranceId(idValue)

        assertAll("AllergyIntoleranceId should be created correctly",
            { assertNotNull(allergyId) },
            { assertEquals(idValue, allergyId.value) },
        )
    }

    @Test
    @DisplayName("Test AllergyIntoleranceId equality")
    fun allergyIntoleranceIdEquality() {
        val idValue = "allergy-456"
        val id1 = AllergyIntoleranceId(idValue)
        val id2 = AllergyIntoleranceId(idValue)
        val id3 = AllergyIntoleranceId("different-id")

        assertAll("AllergyIntoleranceId equality should work correctly",
            { assertEquals(id1, id2) },
            { assertNotEquals(id1, id3) },
            { assertEquals(id1.hashCode(), id2.hashCode()) },
            { assertNotEquals(id1.hashCode(), id3.hashCode()) }
        )
    }

    // === Tests for AllergyIntoleranceEntity ===

    @Test
    @DisplayName("Test AllergyIntoleranceEntity creation from FHIR resource")
    fun allergyIntoleranceEntityFromFhirResource() {
        val fhirResource = createTestAllergyHelper()
        val entity = AllergyIntoleranceEntity.of(fhirResource)

        assertAll("AllergyIntoleranceEntity should be created from FHIR resource correctly",
            { assertNotNull(entity) },
            { assertEquals("1", entity.id.value) },
            { assertEquals(fhirResource, entity.allergyIntolerance) },
            { assertEquals("1", entity.allergyIntolerance.id) }
        )
    }

    @Test
    @DisplayName("Test AllergyIntoleranceEntity creation with domain ID and FHIR resource")
    fun allergyIntoleranceEntityWithDomainId() {
        val domainId = AllergyIntoleranceId("domain-789")
        val fhirResource = createTestAllergyHelper()
        val entity = AllergyIntoleranceEntity.of(domainId, fhirResource)

        assertAll("AllergyIntoleranceEntity should be created with domain ID correctly",
            { assertNotNull(entity) },
            { assertEquals(domainId, entity.id) },
            { assertEquals("domain-789", entity.id.value) },
            { assertEquals(fhirResource, entity.allergyIntolerance) },
            { assertEquals("domain-789", entity.allergyIntolerance.id) } // FHIR ID should be updated
        )
    }

    @Test
    @DisplayName("Test AllergyIntoleranceEntity toJson method")
    fun allergyIntoleranceEntityToJson() {
        val fhirResource = createTestAllergyHelper()
        val fhirResourceJson = fhirResource.toJson()
        val entity = AllergyIntoleranceEntity.of(fhirResource)
        val json = entity.toJson()

        assertAll("AllergyIntoleranceEntity JSON serialization should work correctly",
            { assertNotNull(json) },
            { assertTrue(json.contains("\"resourceType\" : \"AllergyIntolerance\"")) },
            { assertTrue(json.contains("\"id\" : \"1\"")) },
            { assertTrue(json.contains("\"display\" : \"Cashew nuts\"")) },
            { assertEquals(fhirResourceJson, json) } // JSON should match FHIR resource JSON
        )
    }

    @Test
    @DisplayName("Test AllergyIntoleranceEntity updateAllergyIntolerance method")
    fun allergyIntoleranceEntityUpdate() {
        val originalResource = createTestAllergyHelper()
        val entity = AllergyIntoleranceEntity.of(originalResource)

        // Create an updated resource with different allergen
        val updatedResource = AllergyIntoleranceFactory.of(
            id = "different-id",
            patientReference = "Patient/456",
            allergenSystem = "http://snomed.info/sct",
            allergenCode = "762952008",
            allergenDisplay = "Peanuts",
            clinicalStatus = "inactive"
        )

        val updatedEntity = entity.updateAllergyIntolerance(updatedResource)

        assertAll("AllergyIntoleranceEntity update should preserve domain ID",
            { assertNotNull(updatedEntity) },
            { assertEquals(entity.id, updatedEntity.id) }, // Same domain ID
            { assertEquals("1", updatedEntity.id.value) }, // Original domain ID preserved
            { assertEquals("1", updatedEntity.allergyIntolerance.id) }, // FHIR ID updated to match domain ID
            { assertEquals(updatedResource, updatedEntity.allergyIntolerance) },
            { assertEquals("Peanuts", updatedEntity.allergyIntolerance.code.codingFirstRep.display) }
        )
    }

    @Test
    @DisplayName("Test AllergyIntoleranceEntity equality based on ID")
    fun allergyIntoleranceEntityEquality() {
        val id1 = AllergyIntoleranceId("same-id")
        val id2 = AllergyIntoleranceId("same-id")
        val id3 = AllergyIntoleranceId("different-id")

        val resource1 = createTestAllergyHelper()
        val resource2 = createTestAllergyHelper()
        val resource3 = createTestAllergyHelper()

        val entity1 = AllergyIntoleranceEntity.of(id1, resource1)
        val entity2 = AllergyIntoleranceEntity.of(id2, resource2)
        val entity3 = AllergyIntoleranceEntity.of(id3, resource3)

        assertAll("AllergyIntoleranceEntity equality should be based on domain ID",
            { assertEquals(entity1, entity2) }, // Same ID, should be equal
            { assertNotEquals(entity1, entity3) }, // Different ID, should not be equal
            { assertEquals(entity1.hashCode(), entity2.hashCode()) },
            { assertNotEquals(entity1.hashCode(), entity3.hashCode()) }
        )
    }
}