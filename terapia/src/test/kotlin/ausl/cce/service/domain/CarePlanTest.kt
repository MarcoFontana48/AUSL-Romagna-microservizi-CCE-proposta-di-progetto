package ausl.cce.service.domain

import org.hl7.fhir.r4.model.CarePlan
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.apache.logging.log4j.LogManager

class CarePlanTest {
    val logger = LogManager.getLogger(this::class.java)

    /**
     * Helper method to create a test CarePlan instance used in multiple tests
     */
    fun createTestCarePlanHelper(): CarePlan {
        return CarePlanFactory.of(
            id = "1",
            patientReference = "Patient/123",
            title = "Diabetes Management Plan",
            description = "Comprehensive diabetes care plan with lifestyle and medication management",
            status = "active",
            intent = "plan"
        )
    }

    @Test
    @DisplayName("Test creation of a FHIR CarePlan object")
    fun carePlanCreation() {
        val carePlan = CarePlanFactory.of(
            id = "111",
            patientReference = "Patient/123",
            title = "Hypertension Management Plan",
            description = "Blood pressure control and monitoring plan",
            status = "active",
            intent = "plan"
        )

        assertAll("CarePlan fields should be set correctly",
            { assertNotNull(carePlan) },
            { assertEquals("111", carePlan.id) },
            { assertEquals("Patient/123", carePlan.subject.reference) },
            { assertEquals("Hypertension Management Plan", carePlan.title) },
            { assertEquals("Blood pressure control and monitoring plan", carePlan.description) },
            { assertEquals(CarePlan.CarePlanStatus.ACTIVE, carePlan.status) },
            { assertEquals(CarePlan.CarePlanIntent.PLAN, carePlan.intent) },
            { assertNotNull(carePlan.period) },
            { assertNotNull(carePlan.period.start) }
        )
    }

    @Test
    @DisplayName("Test serialization of CarePlan to JSON")
    fun toJson() {
        val carePlan = createTestCarePlanHelper()
        val json = carePlan.toJson()

        logger.debug(json)

        assertAll("CarePlan fields should be parsed correctly",
            { assertNotNull(json) },
            { assertTrue(json.contains("\"id\" : \"1\"")) },
            { assertTrue(json.contains("\"reference\" : \"Patient/123\"")) },
            { assertTrue(json.contains("\"title\" : \"Diabetes Management Plan\"")) },
            { assertTrue(json.contains("\"description\" : \"Comprehensive diabetes care plan with lifestyle and medication management\"")) },
            { assertTrue(json.contains("\"status\" : \"active\"")) },
            { assertTrue(json.contains("\"intent\" : \"plan\"")) }
        )
    }

    @Test
    @DisplayName("Test deserialization of JSON to CarePlan")
    fun fromJsonCarePlan() {
        val carePlan = createTestCarePlanHelper()
        val json = carePlan.toJson()

        val parsedCarePlan = json.fromJsonToCarePlan()

        assertAll("CarePlan fields should be parsed correctly",
            { assertNotNull(parsedCarePlan) },
            { assertEquals("CarePlan/1", parsedCarePlan.id) },
            { assertEquals("Patient/123", parsedCarePlan.subject.reference) },
            { assertEquals("Diabetes Management Plan", parsedCarePlan.title) },
            { assertEquals("Comprehensive diabetes care plan with lifestyle and medication management", parsedCarePlan.description) },
            { assertEquals(CarePlan.CarePlanStatus.ACTIVE, parsedCarePlan.status) },
            { assertEquals(CarePlan.CarePlanIntent.PLAN, parsedCarePlan.intent) }
        )
    }

    @Test
    @DisplayName("Test CarePlan with categories")
    fun carePlanWithCategories() {
        val carePlan = createTestCarePlanHelper()
        carePlan.addCategory("http://snomed.info/sct", "734163000", "Diabetes care")
            .addCategory("http://hl7.org/fhir/care-plan-category", "assess-plan", "Assessment and Plan")

        assertAll("CarePlan should have categories correctly added",
            { assertEquals(2, carePlan.category.size) },
            { assertEquals("Diabetes care", carePlan.category[0].codingFirstRep.display) },
            { assertEquals("734163000", carePlan.category[0].codingFirstRep.code) },
            { assertEquals("Assessment and Plan", carePlan.category[1].codingFirstRep.display) }
        )
    }

    @Test
    @DisplayName("Test CarePlan with activities")
    fun carePlanWithActivities() {
        val carePlan = createTestCarePlanHelper()
        carePlan.addActivity(
            "http://snomed.info/sct",
            "229065009",
            "Exercise therapy",
            "Daily 30-minute walks",
            "in-progress"
        ).addActivity(
            "http://snomed.info/sct",
            "182840001",
            "Drug therapy",
            "Metformin 500mg twice daily",
            "not-started"
        )

        assertAll("CarePlan should have activities correctly added",
            { assertEquals(2, carePlan.activity.size) },
            { assertEquals("Exercise therapy", carePlan.activity[0].detail.code.codingFirstRep.display) },
            { assertEquals("Daily 30-minute walks", carePlan.activity[0].detail.description) },
            { assertEquals(CarePlan.CarePlanActivityStatus.INPROGRESS, carePlan.activity[0].detail.status) },
            { assertEquals("Drug therapy", carePlan.activity[1].detail.code.codingFirstRep.display) },
            { assertEquals(CarePlan.CarePlanActivityStatus.NOTSTARTED, carePlan.activity[1].detail.status) }
        )
    }

    @Test
    @DisplayName("Test CarePlan with goals")
    fun carePlanWithGoals() {
        val carePlan = createTestCarePlanHelper()
        carePlan.addGoal("Goal/diabetes-hba1c-target")
            .addGoal("Goal/weight-loss-target")

        assertAll("CarePlan should have goals correctly added",
            { assertEquals(2, carePlan.goal.size) },
            { assertEquals("Goal/diabetes-hba1c-target", carePlan.goal[0].reference) },
            { assertEquals("Goal/weight-loss-target", carePlan.goal[1].reference) }
        )
    }

    // === Tests for CarePlanId ===

    @Test
    @DisplayName("Test creation of CarePlanId")
    fun carePlanIdCreation() {
        val idValue = "careplan-123"
        val carePlanId = CarePlanId(idValue)

        assertAll("CarePlanId should be created correctly",
            { assertNotNull(carePlanId) },
            { assertEquals(idValue, carePlanId.value) }
        )
    }

    @Test
    @DisplayName("Test CarePlanId equality")
    fun carePlanIdEquality() {
        val idValue = "careplan-456"
        val id1 = CarePlanId(idValue)
        val id2 = CarePlanId(idValue)
        val id3 = CarePlanId("different-id")

        assertAll("CarePlanId equality should work correctly",
            { assertEquals(id1, id2) },
            { assertNotEquals(id1, id3) },
            { assertEquals(id1.hashCode(), id2.hashCode()) },
            { assertNotEquals(id1.hashCode(), id3.hashCode()) }
        )
    }

    // === Tests for CarePlanEntity ===

    @Test
    @DisplayName("Test CarePlanEntity creation from FHIR resource")
    fun carePlanEntityFromFhirResource() {
        val fhirResource = createTestCarePlanHelper()
        val entity = CarePlanEntity.of(fhirResource)

        assertAll("CarePlanEntity should be created from FHIR resource correctly",
            { assertNotNull(entity) },
            { assertEquals("1", entity.id.value) },
            { assertEquals(fhirResource, entity.carePlan) },
            { assertEquals("1", entity.carePlan.id) }
        )
    }

    @Test
    @DisplayName("Test CarePlanEntity creation with domain ID and FHIR resource")
    fun carePlanEntityWithDomainId() {
        val domainId = CarePlanId("domain-789")
        val fhirResource = createTestCarePlanHelper()
        val entity = CarePlanEntity.of(domainId, fhirResource)

        assertAll("CarePlanEntity should be created with domain ID correctly",
            { assertNotNull(entity) },
            { assertEquals(domainId, entity.id) },
            { assertEquals("domain-789", entity.id.value) },
            { assertEquals(fhirResource, entity.carePlan) },
            { assertEquals("domain-789", entity.carePlan.id) } // FHIR ID should be updated
        )
    }

    @Test
    @DisplayName("Test CarePlanEntity toJson method")
    fun carePlanEntityToJson() {
        val fhirResource = createTestCarePlanHelper()
        val fhirResourceJson = fhirResource.toJson()
        val entity = CarePlanEntity.of(fhirResource)
        val json = entity.toJson()

        assertAll("CarePlanEntity JSON serialization should work correctly",
            { assertNotNull(json) },
            { assertTrue(json.contains("\"resourceType\" : \"CarePlan\"")) },
            { assertTrue(json.contains("\"id\" : \"1\"")) },
            { assertTrue(json.contains("\"title\" : \"Diabetes Management Plan\"")) },
            { assertEquals(fhirResourceJson, json) } // JSON should match FHIR resource JSON
        )
    }

    @Test
    @DisplayName("Test CarePlanEntity updateCarePlan method")
    fun carePlanEntityUpdate() {
        val originalResource = createTestCarePlanHelper()
        val entity = CarePlanEntity.of(originalResource)

        // Create an updated resource with different details
        val updatedResource = CarePlanFactory.of(
            id = "different-id",
            patientReference = "Patient/456",
            title = "Updated Heart Disease Management Plan",
            description = "Updated comprehensive cardiac care plan",
            status = "completed",
            intent = "order"
        )

        val updatedEntity = entity.updateCarePlan(updatedResource)

        assertAll("CarePlanEntity update should preserve domain ID",
            { assertNotNull(updatedEntity) },
            { assertEquals(entity.id, updatedEntity.id) }, // Same domain ID
            { assertEquals("1", updatedEntity.id.value) }, // Original domain ID preserved
            { assertEquals("1", updatedEntity.carePlan.id) }, // FHIR ID updated to match domain ID
            { assertEquals(updatedResource, updatedEntity.carePlan) },
            { assertEquals("Updated Heart Disease Management Plan", updatedEntity.carePlan.title) },
            { assertEquals(CarePlan.CarePlanStatus.COMPLETED, updatedEntity.carePlan.status) }
        )
    }

    @Test
    @DisplayName("Test CarePlanEntity convenience methods")
    fun carePlanEntityConvenienceMethods() {
        val fhirResource = createTestCarePlanHelper()
        val entity = CarePlanEntity.of(fhirResource)

        // Test entity convenience methods
        entity.addCategory("http://snomed.info/sct", "734163000", "Diabetes care")
            .addActivity(
                "http://snomed.info/sct",
                "229065009",
                "Exercise therapy",
                "Daily walks",
                "in-progress"
            )
            .addGoal("Goal/diabetes-target")

        assertAll("CarePlanEntity convenience methods should work correctly",
            { assertEquals(1, entity.carePlan.category.size) },
            { assertEquals("Diabetes care", entity.carePlan.category[0].codingFirstRep.display) },
            { assertEquals(1, entity.carePlan.activity.size) },
            { assertEquals("Exercise therapy", entity.carePlan.activity[0].detail.code.codingFirstRep.display) },
            { assertEquals(1, entity.carePlan.goal.size) },
            { assertEquals("Goal/diabetes-target", entity.carePlan.goal[0].reference) }
        )
    }

    @Test
    @DisplayName("Test CarePlanEntity equality based on ID")
    fun carePlanEntityEquality() {
        val id1 = CarePlanId("same-id")
        val id2 = CarePlanId("same-id")
        val id3 = CarePlanId("different-id")

        val resource1 = createTestCarePlanHelper()
        val resource2 = createTestCarePlanHelper()
        val resource3 = createTestCarePlanHelper()

        val entity1 = CarePlanEntity.of(id1, resource1)
        val entity2 = CarePlanEntity.of(id2, resource2)
        val entity3 = CarePlanEntity.of(id3, resource3)

        assertAll("CarePlanEntity equality should be based on domain ID",
            { assertEquals(entity1, entity2) }, // Same ID, should be equal
            { assertNotEquals(entity1, entity3) }, // Different ID, should not be equal
            { assertEquals(entity1.hashCode(), entity2.hashCode()) },
            { assertNotEquals(entity1.hashCode(), entity3.hashCode()) }
        )
    }

    @Test
    @DisplayName("Test CarePlan factory with minimal parameters")
    fun carePlanFactoryMinimal() {
        val carePlan = CarePlanFactory.of(
            id = "minimal-001",
            patientReference = "Patient/789",
            title = "Basic Care Plan"
        )

        assertAll("CarePlan should be created with minimal parameters",
            { assertNotNull(carePlan) },
            { assertEquals("minimal-001", carePlan.id) },
            { assertEquals("Patient/789", carePlan.subject.reference) },
            { assertEquals("Basic Care Plan", carePlan.title) },
            { assertNull(carePlan.description) }, // Should be null when not provided
            { assertEquals(CarePlan.CarePlanStatus.ACTIVE, carePlan.status) }, // Default value
            { assertEquals(CarePlan.CarePlanIntent.PLAN, carePlan.intent) } // Default value
        )
    }

    @Test
    @DisplayName("Test CarePlan with different status and intent values")
    fun carePlanWithDifferentStatuses() {
        val draftCarePlan = CarePlanFactory.of(
            id = "draft-001",
            patientReference = "Patient/100",
            title = "Draft Plan",
            status = "draft",
            intent = "proposal"
        )

        val completedCarePlan = CarePlanFactory.of(
            id = "completed-001",
            patientReference = "Patient/200",
            title = "Completed Plan",
            status = "completed",
            intent = "order"
        )

        assertAll("CarePlan should handle different status and intent values",
            { assertEquals(CarePlan.CarePlanStatus.DRAFT, draftCarePlan.status) },
            { assertEquals(CarePlan.CarePlanIntent.PROPOSAL, draftCarePlan.intent) },
            { assertEquals(CarePlan.CarePlanStatus.COMPLETED, completedCarePlan.status) },
            { assertEquals(CarePlan.CarePlanIntent.ORDER, completedCarePlan.intent) }
        )
    }
}