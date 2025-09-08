package ausl.cce.service.application

import ausl.cce.service.infrastructure.controller.TerapiaProducerVerticle
import org.hl7.fhir.r4.model.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Assertions.*
import io.mockk.mockk

class CarePlanServiceImplTest {

    private lateinit var carePlanService: CarePlanServiceImpl

    @BeforeEach
    fun setUp() {
        val mockRepository = mockk<CarePlanRepository>(relaxed = true)  // 'dummy' test double: it's a mock type that passes an empty implementation of a class required by the constructor but never used in the tests
        val mockProducer = mockk<TerapiaProducerVerticle>(relaxed = true)   // 'dummy' test double: (same as above)
        carePlanService = CarePlanServiceImpl(mockRepository, mockProducer)
    }

    @Nested
    @DisplayName("checkMedicationAllergy Tests")
    inner class CheckMedicationAllergyTests {

        @Test
        @DisplayName("Should return true when medication in CarePlan matches allergy")
        fun shouldReturnTrueWhenMedicationMatchesAllergy() {
            val carePlan = createCarePlanWithMedication(
                system = "http://www.nlm.nih.gov/research/umls/rxnorm",
                code = "7980",
                display = "Penicillin"
            )

            val allergy = createAllergyIntolerance(
                system = "http://www.nlm.nih.gov/research/umls/rxnorm",
                code = "7980",
                display = "Penicillin"
            )

            val result = carePlanService.checkMedicationAllergy(carePlan, allergy)

            assertTrue(result, "Should detect medication-allergy conflict")
        }

        @Test
        @DisplayName("Should return false when medication in CarePlan does not match allergy")
        fun shouldReturnFalseWhenMedicationDoesNotMatchAllergy() {
            val carePlan = createCarePlanWithMedication(
                system = "http://www.nlm.nih.gov/research/umls/rxnorm",
                code = "6809",
                display = "Metformin"
            )

            val allergy = createAllergyIntolerance(
                system = "http://www.nlm.nih.gov/research/umls/rxnorm",
                code = "7980",
                display = "Penicillin"
            )

            val result = carePlanService.checkMedicationAllergy(carePlan, allergy)

            assertFalse(result, "Should not detect conflict for different medications")
        }

        @Test
        @DisplayName("Should return false when medication system differs from allergy system")
        fun shouldReturnFalseWhenSystemsDiffer() {
            val carePlan = createCarePlanWithMedication(
                system = "http://www.nlm.nih.gov/research/umls/rxnorm",
                code = "7980",
                display = "Penicillin"
            )

            val allergy = createAllergyIntolerance(
                system = "http://snomed.info/sct",
                code = "7980",
                display = "Penicillin"
            )

            val result = carePlanService.checkMedicationAllergy(carePlan, allergy)

            assertFalse(result, "Should not detect conflict when coding systems differ")
        }

        @Test
        @DisplayName("Should return false when CarePlan has no activities")
        fun shouldReturnFalseWhenCarePlanHasNoActivities() {
            val carePlan = CarePlan().apply {
                id = "test-plan"
                status = CarePlan.CarePlanStatus.ACTIVE
                intent = CarePlan.CarePlanIntent.PLAN
                subject = Reference("Patient/123")
            }

            val allergy = createAllergyIntolerance(
                system = "http://www.nlm.nih.gov/research/umls/rxnorm",
                code = "7980",
                display = "Penicillin"
            )

            val result = carePlanService.checkMedicationAllergy(carePlan, allergy)

            assertFalse(result, "Should return false when no activities exist")
        }

        @Test
        @DisplayName("Should return false when activities have no medication details")
        fun shouldReturnFalseWhenActivitiesHaveNoMedicationDetails() {
            val carePlan = CarePlan().apply {
                id = "test-plan"
                status = CarePlan.CarePlanStatus.ACTIVE
                intent = CarePlan.CarePlanIntent.PLAN
                subject = Reference("Patient/123")

                addActivity(CarePlan.CarePlanActivityComponent().apply {
                    detail = CarePlan.CarePlanActivityDetailComponent().apply {
                        code = CodeableConcept().apply {
                            addCoding().apply {
                                system = "http://snomed.info/sct"
                                code = "229065009"
                                display = "Exercise therapy"
                            }
                        }
                        description = "Daily exercise"
                        status = CarePlan.CarePlanActivityStatus.NOTSTARTED
                    }
                })
            }

            val allergy = createAllergyIntolerance(
                system = "http://www.nlm.nih.gov/research/umls/rxnorm",
                code = "7980",
                display = "Penicillin"
            )

            val result = carePlanService.checkMedicationAllergy(carePlan, allergy)

            assertFalse(result, "Should return false when activities have no medication products")
        }

        @Test
        @DisplayName("Should return true when CarePlan has multiple medications and one matches allergy")
        fun shouldReturnTrueWhenOneOfMultipleMedicationsMatchesAllergy() {
            val carePlan = CarePlan().apply {
                id = "test-plan"
                status = CarePlan.CarePlanStatus.ACTIVE
                intent = CarePlan.CarePlanIntent.PLAN
                subject = Reference("Patient/123")

                // Add Metformin activity
                addActivity(CarePlan.CarePlanActivityComponent().apply {
                    detail = CarePlan.CarePlanActivityDetailComponent().apply {
                        setProduct(CodeableConcept().apply {
                            addCoding().apply {
                                system = "http://www.nlm.nih.gov/research/umls/rxnorm"
                                code = "6809"
                                display = "Metformin"
                            }
                        })
                        status = CarePlan.CarePlanActivityStatus.INPROGRESS
                    }
                })

                // Add Penicillin activity (this should match the allergy)
                addActivity(CarePlan.CarePlanActivityComponent().apply {
                    detail = CarePlan.CarePlanActivityDetailComponent().apply {
                        setProduct(CodeableConcept().apply {
                            addCoding().apply {
                                system = "http://www.nlm.nih.gov/research/umls/rxnorm"
                                code = "7980"
                                display = "Penicillin"
                            }
                        })
                        status = CarePlan.CarePlanActivityStatus.NOTSTARTED
                    }
                })
            }

            val allergy = createAllergyIntolerance(
                system = "http://www.nlm.nih.gov/research/umls/rxnorm",
                code = "7980",
                display = "Penicillin"
            )

            val result = carePlanService.checkMedicationAllergy(carePlan, allergy)

            assertTrue(result, "Should detect conflict when one of multiple medications matches allergy")
        }

        @Test
        @DisplayName("Should return false when allergy has no coding")
        fun shouldReturnFalseWhenAllergyHasNoCoding() {
            val carePlan = createCarePlanWithMedication(
                system = "http://www.nlm.nih.gov/research/umls/rxnorm",
                code = "7980",
                display = "Penicillin"
            )

            val allergy = AllergyIntolerance().apply {
                id = "allergy-001"
                patient = Reference("Patient/123")
                clinicalStatus = CodeableConcept().apply {
                    addCoding().apply {
                        system = "http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical"
                        code = "active"
                    }
                }
                // No code/coding set for the allergen
            }

            val result = carePlanService.checkMedicationAllergy(carePlan, allergy)

            assertFalse(result, "Should return false when allergy has no coding")
        }
    }

    // Helper methods
    private fun createCarePlanWithMedication(
        system: String,
        code: String,
        display: String
    ): CarePlan {
        return CarePlan().apply {
            id = "test-plan"
            status = CarePlan.CarePlanStatus.ACTIVE
            intent = CarePlan.CarePlanIntent.PLAN
            subject = Reference("Patient/123")

            addActivity(CarePlan.CarePlanActivityComponent().apply {
                detail = CarePlan.CarePlanActivityDetailComponent().apply {
                    setProduct(CodeableConcept().apply {
                        addCoding().apply {
                            this.system = system
                            this.code = code
                            this.display = display
                        }
                    })
                    status = CarePlan.CarePlanActivityStatus.NOTSTARTED
                }
            })
        }
    }

    private fun createAllergyIntolerance(
        system: String,
        code: String,
        display: String
    ): AllergyIntolerance {
        return AllergyIntolerance().apply {
            id = "allergy-001"
            patient = Reference("Patient/123")
            clinicalStatus = CodeableConcept().apply {
                addCoding().apply {
                    this.system = "http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical"
                    this.code = "active"
                }
            }
            this.code = CodeableConcept().apply {
                addCoding().apply {
                    this.system = system
                    this.code = code
                    this.display = display
                }
            }
        }
    }
}