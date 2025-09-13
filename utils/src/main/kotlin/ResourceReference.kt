package mf.cce.utils

/*
tmp test resources
 */

const val carePlanTest = "{\n" +
        "  \"resourceType\": \"CarePlan\",\n" +
        "  \"id\": \"002\",\n" +
        "  \"subject\": {\n" +
        "    \"reference\": \"Patient/456\"\n" +
        "  },\n" +
        "  \"title\": \"Diabetes Management Plan\",\n" +
        "  \"description\": \"Comprehensive diabetes care plan including medication, diet, and exercise management\",\n" +
        "  \"status\": \"active\",\n" +
        "  \"intent\": \"plan\",\n" +
        "  \"category\": [\n" +
        "    {\n" +
        "      \"coding\": [\n" +
        "        {\n" +
        "          \"system\": \"http://snomed.info/sct\",\n" +
        "          \"code\": \"734163000\",\n" +
        "          \"display\": \"Care of patient with diabetes\"\n" +
        "        }\n" +
        "      ],\n" +
        "      \"text\": \"Diabetes care\"\n" +
        "    }\n" +
        "  ],\n" +
        "  \"period\": {\n" +
        "    \"start\": \"2025-09-04T00:00:00Z\"\n" +
        "  },\n" +
        "  \"activity\": [\n" +
        "    {\n" +
        "      \"detail\": {\n" +
        "        \"code\": {\n" +
        "          \"coding\": [\n" +
        "            {\n" +
        "              \"system\": \"http://snomed.info/sct\",\n" +
        "              \"code\": \"229065009\",\n" +
        "              \"display\": \"Exercise therapy\"\n" +
        "            }\n" +
        "          ],\n" +
        "          \"text\": \"Exercise therapy\"\n" +
        "        },\n" +
        "        \"description\": \"Daily 30-minute moderate exercise\",\n" +
        "        \"status\": \"not-started\"\n" +
        "      }\n" +
        "    },\n" +
        "    {\n" +
        "      \"detail\": {\n" +
        "        \"code\": {\n" +
        "          \"coding\": [\n" +
        "            {\n" +
        "              \"system\": \"http://hl7.org/fhir/uv/cpg/CodeSystem/cpg-activity-type\",\n" +
        "              \"code\": \"medication-administration\",\n" +
        "              \"display\": \"Medication Administration\"\n" +
        "            }\n" +
        "          ],\n" +
        "          \"text\": \"Medication administration\"\n" +
        "        },\n" +
        "        \"description\": \"Metformin 500mg twice daily with meals\",\n" +
        "        \"status\": \"in-progress\",\n" +
        "        \"productCodeableConcept\": {\n" +
        "          \"coding\": [\n" +
        "            {\n" +
        "              \"system\": \"http://www.nlm.nih.gov/research/umls/rxnorm\",\n" +
        "              \"code\": \"6809\",\n" +
        "              \"display\": \"Metformin\"\n" +
        "            }\n" +
        "          ],\n" +
        "          \"text\": \"Metformin 500mg\"\n" +
        "        },\n" +
        "        \"dailyAmount\": {\n" +
        "          \"value\": 2,\n" +
        "          \"unit\": \"tablets\",\n" +
        "          \"system\": \"http://unitsofmeasure.org\",\n" +
        "          \"code\": \"1\"\n" +
        "        },\n" +
        "        \"quantity\": {\n" +
        "          \"value\": 60,\n" +
        "          \"unit\": \"tablets\",\n" +
        "          \"system\": \"http://unitsofmeasure.org\",\n" +
        "          \"code\": \"1\"\n" +
        "        }\n" +
        "      }\n" +
        "    },\n" +
        "    {\n" +
        "      \"detail\": {\n" +
        "        \"code\": {\n" +
        "          \"coding\": [\n" +
        "            {\n" +
        "              \"system\": \"http://hl7.org/fhir/uv/cpg/CodeSystem/cpg-activity-type\",\n" +
        "              \"code\": \"medication-administration\",\n" +
        "              \"display\": \"Medication Administration\"\n" +
        "            }\n" +
        "          ],\n" +
        "          \"text\": \"Medication administration\"\n" +
        "        },\n" +
        "        \"description\": \"Penicillin 500mg four times daily for infection treatment\",\n" +
        "        \"status\": \"not-started\",\n" +
        "        \"productCodeableConcept\": {\n" +
        "          \"coding\": [\n" +
        "            {\n" +
        "              \"system\": \"http://www.nlm.nih.gov/research/umls/rxnorm\",\n" +
        "              \"code\": \"7980\",\n" +
        "              \"display\": \"Penicillin\"\n" +
        "            }\n" +
        "          ],\n" +
        "          \"text\": \"Penicillin 500mg\"\n" +
        "        },\n" +
        "        \"dailyAmount\": {\n" +
        "          \"value\": 4,\n" +
        "          \"unit\": \"tablets\",\n" +
        "          \"system\": \"http://unitsofmeasure.org\",\n" +
        "          \"code\": \"1\"\n" +
        "        },\n" +
        "        \"quantity\": {\n" +
        "          \"value\": 28,\n" +
        "          \"unit\": \"tablets\",\n" +
        "          \"system\": \"http://unitsofmeasure.org\",\n" +
        "          \"code\": \"1\"\n" +
        "        }\n" +
        "      }\n" +
        "    },\n" +
        "    {\n" +
        "      \"reference\": \"MedicationRequest/glucose-meter-strips-001\"\n" +
        "    }\n" +
        "  ],\n" +
        "  \"goal\": [\n" +
        "    {\n" +
        "      \"reference\": \"Goal/hba1c-target-001\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"reference\": \"Goal/weight-loss-target-001\"\n" +
        "    }\n" +
        "  ]\n" +
        "}"

const val allergyIntoleranceTest = "{\n" +
        "  \"resourceType\": \"AllergyIntolerance\",\n" +
        "  \"id\": \"123\",\n" +
        "  \"patient\": {\n" +
        "    \"reference\": \"Patient/456\"\n" +
        "  },\n" +
        "  \"clinicalStatus\": {\n" +
        "    \"coding\": [\n" +
        "      {\n" +
        "        \"system\": \"http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical\",\n" +
        "        \"code\": \"active\",\n" +
        "        \"display\": \"Active\"\n" +
        "      }\n" +
        "    ]\n" +
        "  },\n" +
        "  \"verificationStatus\": {\n" +
        "    \"coding\": [\n" +
        "      {\n" +
        "        \"system\": \"http://terminology.hl7.org/CodeSystem/allergyintolerance-verification\",\n" +
        "        \"code\": \"confirmed\",\n" +
        "        \"display\": \"Confirmed\"\n" +
        "      }\n" +
        "    ]\n" +
        "  },\n" +
        "  \"type\": \"allergy\",\n" +
        "  \"category\": [\"medication\"],\n" +
        "  \"criticality\": \"high\",\n" +
        "  \"code\": {\n" +
        "    \"coding\": [\n" +
        "      {\n" +
        "        \"system\": \"http://www.nlm.nih.gov/research/umls/rxnorm\",\n" +
        "        \"code\": \"7980\",\n" +
        "        \"display\": \"Penicillin\"\n" +
        "      }\n" +
        "    ],\n" +
        "    \"text\": \"Penicillin\"\n" +
        "  },\n" +
        "  \"onsetDateTime\": \"2020-06-15\",\n" +
        "  \"reaction\": [\n" +
        "    {\n" +
        "      \"manifestation\": [\n" +
        "        {\n" +
        "          \"coding\": [\n" +
        "            {\n" +
        "              \"system\": \"http://snomed.info/sct\",\n" +
        "              \"code\": \"247472004\",\n" +
        "              \"display\": \"Hives\"\n" +
        "            }\n" +
        "          ]\n" +
        "        }\n" +
        "      ],\n" +
        "      \"severity\": \"moderate\"\n" +
        "    }\n" +
        "  ]\n" +
        "}"

const val encounterTest = "{\n" +
        "  \"resourceType\": \"Encounter\",\n" +
        "  \"id\": \"111\",\n" +
        "  \"subject\": {\n" +
        "    \"reference\": \"Patient/456\"\n" +
        "  },\n" +
        "  \"class\": {\n" +
        "    \"system\": \"http://terminology.hl7.org/CodeSystem/v3-ActCode\",\n" +
        "    \"code\": \"AMB\",\n" +
        "    \"display\": \"Ambulatory\"\n" +
        "  },\n" +
        "  \"status\": \"finished\",\n" +
        "  \"serviceType\": {\n" +
        "    \"coding\": [\n" +
        "      {\n" +
        "        \"system\": \"http://terminology.hl7.org/CodeSystem/service-type\",\n" +
        "        \"code\": \"408\",\n" +
        "        \"display\": \"General Medicine\"\n" +
        "      }\n" +
        "    ],\n" +
        "    \"text\": \"General Medicine\"\n" +
        "  },\n" +
        "  \"period\": {\n" +
        "    \"start\": \"2025-09-04T12:00:00Z\",\n" +
        "    \"end\": \"2025-09-04T12:00:00Z\"\n" +
        "  }\n" +
        "}"