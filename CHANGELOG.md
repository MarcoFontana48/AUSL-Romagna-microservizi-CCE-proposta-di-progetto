# [1.7.0](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/compare/v1.6.2...v1.7.0) (2025-09-12)


### Bug Fixes

* **api-gateway:** could not handle more than 5 clients concurrently ([a794ce3](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/a794ce396e569eebddf85b59ea6d3e287d7a2e03))
* **api-gateway:** number of worker threads ([4d77f9d](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/4d77f9d477528c237c3fe9ed7d618611ac73c27b))
* **api-gateway:** number of worker threads for reroute ([9f8ea9c](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/9f8ea9ce01f5e280246aeac2a5669abe43173a2e))
* **api-gateway:** reroute settings ([e3d2357](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/e3d2357923aceba73ca40e13a2c82a0e1ff829fe))
* fix k8s deploy connection between services ([9e4854c](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/9e4854cbd5075394b4553b1468984d16306dbdb1))
* **qa_scenarios:** test deployment automation ([6e4ed36](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/6e4ed36413dc3d7d6c0fb868945ba98dc4adb7f0))
* **qa_scenarios:** update k8s deploy for tests ([0eef3bc](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/0eef3bcf7b59ca3f67e0baf05465230684bc52dc))


### Features

* **anamnesi-pregressa:** add timers for requests about domain elements ([ef03214](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/ef03214f50c2edbe7c3e392aa6de2bf5bcb19e35))
* **diario-clinico:** add timers for requests about domain elements ([190cdbb](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/190cdbb8c6de303b1292144f095e6d40652d1061))
* **terapia:** add timers for requests about domain elements ([d5e2205](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/d5e220547abb12c77c8e11b9adb025d9127f3e23))

## [1.6.2](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/compare/v1.6.1...v1.6.2) (2025-09-09)


### Bug Fixes

* **anamnesi-pregressa:** fix package structure ([f8a6c49](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/f8a6c494253b8488499494dcf0a9d495c24a2ef4))
* **diario-clinico:** fix package structure ([7b7b2b1](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/7b7b2b1ffb3623e4fb7d0beee486f8c261e22964))
* **terapia:** fix package structure ([953c29a](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/953c29adfb9b8950249709209ecc35a45ad0c42a))

## [1.6.1](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/compare/v1.6.0...v1.6.1) (2025-09-08)


### Bug Fixes

* **terapia:** integrate metrics tracking into CarePlanService and update service initialization ([9632f9a](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/9632f9a5f33cf2c57df1a17a1c33b527cd8868fa))

# [1.6.0](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/compare/v1.5.0...v1.6.0) (2025-09-08)


### Bug Fixes

* **deps:** update kotest to v6.0.2 ([#36](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/issues/36)) ([72b0f21](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/72b0f21fed6605de648fd58fece14bce882fe78e))
* **deps:** update mongodb to v5.5.1 ([0cc4ea7](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/0cc4ea788565c3120bd470f08fcbfbaaff6b2175))
* **deps:** update vertx to v4.5.21 ([#38](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/issues/38)) ([4fb4d3e](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/4fb4d3e9f8842cad4e7b02218f8cd089eca4f87f))
* **diario_clinico:** update service name from 'anamnesi-pregressa' to 'diario-clinico' ([74d927e](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/74d927ee210a67238e9099ff9b873aaf1fdca124))
* **diario_clinico:** update service tags from 'anamnesi-pregressa' to 'diario-clinico' ([6dcdcac](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/6dcdcac6b7be591a65a12e57e26faa5d837ccc6f))
* **terapia:** update import path for CarePlanRepository in MongoRepositoryTest ([741da0b](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/741da0bf1d38441f3ae5194ef60258998f8fc10b))


### Features

* add diario_clinico and terapia microservices with MongoDB support and Docker configurations ([f2981bc](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/f2981bca6a585c259696cdf6031abda36e3ca9a1))
* add FHIR resource constants and integrate HAPI FHIR library ([2fe0989](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/2fe098902a136d903e3d9de53b6135719cdd8884))
* add Kafka and Zookeeper services to Docker Compose configuration ([ca79d8e](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/ca79d8e79650d2e0ab5514eb7bb0c518b3e4c5e6))
* add recovery time tests for high availability scenarios in Kubernetes ([e1927aa](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/e1927aa7a8d62994d1a12d178eec7e42f16c2c95))
* **allergy-intolerance:** integrate AnamnesiProducerVerticle for event publishing ([22ffd43](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/22ffd4328551013e59ae017b9f1fe20a78cd3283))
* **anamnesi_pregressa:** add allergy controller ([b740b0e](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/b740b0e93fe0247e3a3a5afb6874c3b5867c2a64))
* **anamnesi_pregressa:** add allergy ddd service object ([a8c8e76](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/a8c8e762bedfb972240dddbf2681501556c516a7))
* **anamnesi_pregressa:** add Allergy domain resouce ([35aa3c6](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/35aa3c62604c970022f1c7deeed564de05d6d1f3))
* **anamnesi_pregressa:** add AllergyIntolerance endpoint handlers ([e279615](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/e27961568a600805d18005d47000785e08711b99))
* **anamnesi_pregressa:** add mongo repository for allergies ([50fc8ce](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/50fc8ce7c8513ce4a073000400ae7fb9d75ab66c))
* **anamnesi_pregressa:** add mongo repository for allergies ([cc56437](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/cc56437dc979ca075faff164f1e503f7e6487efb))
* **anamnesi-pregressa:** add AnamnesiProducerVerticle for Kafka event publishing ([0b17072](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/0b170722213a6755d39c5615676bb5f9462ae742))
* **anamnesi-pregressa:** add EventProducerVerticle interface for event publishing ([cb7f27a](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/cb7f27a6bbb9cfccd29f2298933e712090c04f5e))
* **anamnesi-pregressa:** add imports for AllergyIntoleranceRepository and DummyRepository ([c4ba868](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/c4ba868aa9394f25012f724bece038c40eb7133d))
* **anamnesi-pregressa:** integrate AnamnesiProducerVerticle and enhance verticle deployment ([8f4a8b6](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/8f4a8b6f5470b7642304f4eecff51358a4e3562b))
* **deploy:** add Zookeeper and Kafka configurations with StatefulSets and services to kubernetes files ([83587d9](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/83587d9a560907ebb3aa1c1faa87e5c57f792e25))
* **diario_clinico:** add Encounter handlers and metrics to ServiceController interface ([f8c6e9a](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/f8c6e9abde185661260e5803b13112bbea81c47b))
* **diario_clinico:** add factory method and extension function for CarePlan diagnosis ([d028032](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/d0280325dae0f339e29b562352bdebab49c95dbd))
* **diario_clinico:** implement Encounter resource with factory methods and JSON serialization ([8a66679](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/8a66679c9b6f7e1ae98e70ed595c39691115ec38))
* **diario_clinico:** implement EncounterService interface and its implementation ([2ca87e7](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/2ca87e778fe600919ffb3361f6acf91e54ab4742))
* **diario_clinico:** integrate EncounterService into ServerVerticle and define encounter endpoints ([04dcee7](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/04dcee7d7caf00d9fde63eb6524aa775cde75f38))
* **diario_clinico:** integrate EncounterService into ServerVerticle and define encounter endpoints ([f64f255](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/f64f2557f5c742dc056c313ae51f807fd701f09b))
* **diario_clinico:** integrate EncounterService into StandardController and add encounter handling methods ([66dec06](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/66dec06624a4988510c016b8887d3d4b23d0b476))
* **diario_clinico:** refactor MongoRepository to be generic and add concrete implementations for DummyEntity and EncounterEntity ([8e170b4](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/8e170b4494edd876efc700cc8b1046a5136d7e58))
* **diario_clinico:** update repository interfaces to include Encounter repository and Closeable ([b41fb01](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/b41fb017d3d6c9797e94f43440e3ce90558bc9e0))
* **diario_clinico:** update repository interfaces to include Encounter repository and Closeable ([2581895](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/25818956c1d655f79cd202c0607d67d3db1b44fc))
* **diario:** add EventProducerVerticle interface for event publishing ([08bf607](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/08bf607274ff87d72038ed46964e18e5d6aa95f2))
* **diario:** implement EncounterEventProducerVerticle for Kafka event publishing ([f4c913d](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/f4c913dce48fc902c19f813ecd3ba4035f4a3d0a))
* **diario:** integrate EncounterEventProducer for publishing encounter concluded events ([c8cdc4e](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/c8cdc4e610adad9eabed8cc05dc7a09f8f7ca2ce))
* **diario:** integrate EncounterEventProducerVerticle into service deployment ([8a2e83b](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/8a2e83b0771820bc283874b4c86c76b818613273))
* **terapia:** add AllergyIntolerance domain model with JSON serialization support ([c4e67f9](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/c4e67f9717145f7439e6c0ac3ab860b512ad9aea))
* **terapia:** add AllergyIntoleranceEventHandler interface for allergy diagnosed events ([b22a78b](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/b22a78b5e1510b675ac1acd7372fa884ee6baa85))
* **terapia:** add Docker Compose configuration for Kafka and MongoDB services ([fbd0786](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/fbd07867481e0dd340c4a7eec19e1a8366a4bac9))
* **terapia:** add EventProducerVerticle interface for event publishing ([d13930f](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/d13930f6169564869f201dc2cba72b5dd4e19f51))
* **terapia:** add imports for CarePlanRepository and DummyRepository in TerapiaRepository ([d2148bc](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/d2148bcb5aa18ad6311b372a00ffe948136be911))
* **terapia:** add TerapiaConsumerVerticle and TerapiaProducerVerticle for Kafka event handling ([a7eafad](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/a7eafad5b513e40e456f4e513b46c53f6a80aadd))
* **terapia:** implement CarePlan domain model with factory methods and JSON serialization ([2491610](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/24916101939675abf75600d5ac343bee26f75f57))
* **terapia:** implement CarePlanService with CRUD operations and allergy conflict check ([4c0dba0](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/4c0dba01ade4cd1b40efa2ba853bff5d6b6abd7c))
* **terapia:** implement conflict check and suspension for CarePlans based on diagnosed allergies ([b4d3eaa](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/b4d3eaa7c2197d00a2ffe909007076fd1295247d))
* **terapia:** implement CRUD operations and metrics for CarePlan entity in ServiceController ([1b59208](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/1b59208ef670c3c63c5e395aa9e97bd82b322732))
* **terapia:** integrate CarePlanService into Main and update ServerVerticle initialization ([8f05b7a](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/8f05b7a82b38a6ad18c805e209f6401e72b30b08))
* **terapia:** integrate CarePlanService into ServerVerticle and update endpoints ([10074ad](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/10074adecd350d3fd8b1cc90b14b358448c3a223))
* **terapia:** integrate TerapiaConsumerVerticle and TerapiaProducerVerticle into Main.kt ([c1c6dc3](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/c1c6dc307e3c0f89151ddc1c1107d57af867d9bd))
* **terapia:** refactor repository structure and implement CarePlan repository with CRUD operations ([04e833d](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/04e833d855756cfdab77786fb254c7fd40be3343))
* update endpoint constants for Encounter ([1b38527](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/1b38527e8c487571ebb58980331d0994d10d9cdc))
* **utils:** add domain events for allergy diagnosis, therapy revocation, and encounter conclusion ([be2d102](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/be2d10218bfd045f28a997a24c5c0104e620160a))
* **utils:** add test resources for CarePlan and AllergyIntolerance ([0e9569b](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/0e9569b96e9ab2aa44131065076f61d8d0307342))

# [1.5.0](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/compare/v1.4.0...v1.5.0) (2025-08-30)


### Bug Fixes

* **deps:** update dependency org.jetbrains.kotlin:kotlin-gradle-plugin to v2.2.10 ([#24](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/issues/24)) ([ac1e13d](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/ac1e13d7f23d2cce36dbd4737b03ea634c66ddeb))
* **deps:** update kotest to v6 ([#30](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/issues/30)) ([e2246d8](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/e2246d895fb2552967c38503137f50f09459ee76))
* **deps:** update kotest to v6.0.1 ([#32](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/issues/32)) ([8a9af90](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/8a9af90af2b010767d1ecc97505e157c44b74c44))
* **deps:** update spring boot to v3.5.5 ([#29](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/issues/29)) ([0f0f359](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/0f0f3590a3903d4aeb82da4b993f1f05d2d85d42))
* **deps:** update vertx to v4.5.18 ([#25](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/issues/25)) ([92cd21d](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/92cd21d8fb30a96969973ec2a352f0888b86d99f))


### Features

* add diario_clinico and anamnesi_pregressa microservices with MongoDB support and Docker configurations ([cd40128](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/cd40128be38b5c895ea68110cc9a957e42b498d5))
* add Gatling load test for performance testing scenarios and update Kubernetes configurations ([6cda9bb](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/6cda9bb0c71b4ce9b7428ebcbda1f53d3a3b4244))
* add k8s test and autoscaling ([e112c56](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/e112c569b120e40d4f0a4e9951b421ba65657c28))
* add k8s test metrics ([f1edbb3](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/f1edbb3ab0bbba87f428c246b0cefa18e389a343))
* add Terapia microservice with Docker support and MongoDB integration ([799dbaa](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/799dbaa6fdd8645e6d973030df7cb70f1abee30a))
* enhance performance testing with new load test scenarios and improved metrics logging ([5c2bc6f](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/5c2bc6fac8461918fb367ce47e51464438ec5125))
* **service:** add GET dummies entities implementation ([05a47fd](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/05a47fd2fa30a94d9fb2949fc40c1c9acfbb42e1))
* **service:** add POST dummies entities implementation ([57eef69](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/57eef69177979f076ba3704b858df3693830045e))

# [1.4.0](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/compare/v1.3.1...v1.4.0) (2025-08-15)


### Bug Fixes

* **deps:** update dependency com.tngtech.archunit:archunit to v1.4.1 ([f6f4fac](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/f6f4fac4e9d147ebf9e5f9c356d50b300d4824d4))
* **deps:** update dependency org.mockito:mockito-core to v5.19.0 ([1cb0003](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/1cb0003165a4c2e1fef1658605517cc15d44f223))
* kubernetes name in README.md ([9d5f06b](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/9d5f06b66462a9ae81c88606f65e6e3e71e29a39))
* **service:** fix circuit breaker name ([490ff75](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/490ff75c365095786ae80855aab8e9fda45b1b3d))


### Features

* **api_gateway:** add metrics provider to controller ([527d626](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/527d62683a7b4324d126ff87e8680dc4d1a954d6))
* **deploy:** add service to k8s ([d6bbc9e](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/d6bbc9e476b2e162f858e501fa3d1cde14726ab8))
* **service:** add dummy domain entity class ([c627d17](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/c627d17764c06f13d6ef8c3d1244a350262afb29))
* **service:** add metrics provider to controller ([76e9f1b](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/76e9f1b5c151d5ad662bb3775c74fb63e091d168))
* **service:** add mongo repository implementation and test ([7d9ce23](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/7d9ce236f3dc9bae21384a8ac1caece6ba63e213))
* **service:** add repository interface ([8176ec7](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/8176ec7636ff62504d8584b61b5781bb1acfaf9e))
* **utils:** add credentials support for repositories ([72bc640](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/72bc6408c4d7be2fad1da64b0a32eec9c66ebcc0))
* **utils:** add DDD support ([71febdf](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/71febdf96a245ba7085c219438775354bcc56256))
* **utils:** add docker test abstract class ([8794cf6](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/8794cf6b0d885258578fdbd347aa7f6b676a8146))
* **utils:** add log4j2 library dependency ([eef955c](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/eef955cf2263063d506c584328f526f9e1e672ab))
* **utils:** add metrics registry ([b860c30](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/b860c3021dcbe32f17549949a8124074d522152c))

## [1.3.1](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/compare/v1.3.0...v1.3.1) (2025-08-13)


### Bug Fixes

* **deps:** update jackson monorepo to v2.19.2 ([18e07c1](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/18e07c1f34e812bf5f99643b0e690aa93c8072ff))
* **deps:** update junit-framework monorepo to v5.13.4 ([938cb35](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/938cb35c1a1375cf746e544a7e77670fee6bb601))
* **deps:** update log4j2 monorepo to v2.25.1 ([d0e8550](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/d0e8550d18e3a39ecd4b49b224093a6a519b520a))
* **deps:** update spring boot to v3.5.4 ([a0f11d7](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/a0f11d76f7a98e75a31542b00c9aa7e3f71f4940))
* **deps:** update vertx to v4.5.17 ([ac91496](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/ac914967680afbae5ee376bd3cd4ed02e6e66fcf))

# [1.3.0](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/compare/v1.2.1...v1.3.0) (2025-08-12)


### Bug Fixes

* **api_gateway:** reroute to correct service ([75aacd1](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/75aacd198b3b3a549825579488efc849791bf1ce))


### Features

* **api_gateway:** add metrics handler ([d322859](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/d32285945ad47bcd8edf5a093d8fb0b395cb9aa5))
* **api_gateway:** add metrics handler ([f86ec35](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/f86ec35b479ad93ee6daf820f14fc6a71fb398c6))
* **api_gateway:** add micrometer dependency ([ebbe98a](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/ebbe98a9e79deacc1b4084b55699c9eab9b65efc))
* **app:** remove module ([06c5c12](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/06c5c12d804d27112adcb7dbb0b2b247553f2dd6))
* **app:** remove module ([4ea6ed4](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/4ea6ed41322f3412d240d9192d9c10490a191794))
* **build:** add micrometer to TOML ([ab700fa](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/ab700fac24df824e7b7775de84d2e0ef828eec79))
* **deploy:** add service to docker compose ([3f518a9](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/3f518a9578ad568b06738f8db4d820933cac7b81))
* **deploy:** run kubernetes on dev images ([4f2cfa7](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/4f2cfa709920b50ac6f3170e5231c66cb3916ca9))
* **metrics:** add prometheus to service metrics ([6aeac7f](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/6aeac7faa9b53660985f488dae477480817b7f42))
* **service:** add metrics endpoint ([2ee508e](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/2ee508e512a7c325175b8e29d8fa3e8ab543139f))
* **service:** add service ([7d3e334](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/7d3e3344dc7342602c120f823aa4157f4308e294))
* **utils:** add abstract metrics controller ([bd81003](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/bd81003422b0787d84eff4bd06e4fceac80c34f1))
* **utils:** add metrics endpoint ([5d9167d](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/5d9167d093d5b3a8d8ebe9868af95e6d708e14d5))
* **utils:** add Ports and Endpoints ([8baf145](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/8baf14564925cbc959b3e978ed509963f9613ffb))

## [1.2.1](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/compare/v1.2.0...v1.2.1) (2025-08-12)


### Bug Fixes

* **deps:** update dependency org.jetbrains.kotlinx:kotlinx-serialization-json to v1.9.0 ([2f2aa5f](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/2f2aa5f684f150c24f52a787521a69a3c9d57e57))

# [1.2.0](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/compare/v1.1.0...v1.2.0) (2025-08-12)


### Bug Fixes

* **app:** remove unnecessary app files ([be5a65c](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/be5a65c384d59101418476e6bffac0a221cc911e))


### Features

* **api_gateway:** add controller implementation ([2146b9d](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/2146b9d7065ba37a02ba137e2b3a14a8d573882b))
* **api_gateway:** add controller interface ([54e3cf2](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/54e3cf2a09e754a29294b437c82165c7df14483f))
* **api_gateway:** add dependency to vertx and jackson libraries, add dependency on utils module ([f0d9063](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/f0d9063f109d9f2c18068b13a2166a14373a05d5))
* **api_gateway:** add log4j2 support ([967beaa](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/967beaa4212735a2242e0df1ee06ca3969f95b79))
* **api_gateway:** add new main class ([a418cdf](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/a418cdf2dbe93bf2040d43f71ad29e801334b2ac))
* **api_gateway:** add server implementation ([412e3eb](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/412e3ebbb9acd4abdd0035f60386de97ee707ae6))
* **api_gateway:** remove dummy tests ([f4bee2e](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/f4bee2ec109b59ec1ca30b1244aa569077ee8140))
* **api_gateway:** remove old main class ([6c5916b](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/6c5916b412912096aab150257d59b8519a77f8d8))
* **build:** add libraries to TOML ([a92f21c](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/a92f21c8c507b2b829b3e47da27ef9892d62017c))
* **utils:** add HTTP Status codes ([9f0bb67](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/9f0bb675e852a23fefa2b9c6e4745852d3f26cf3))
* **utils:** update JVM to 21 ([72e456c](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/72e456cc82ea5de780c5c95c0d0918d2385e7fa1))

# [1.1.0](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/compare/v1.0.0...v1.1.0) (2025-08-12)


### Bug Fixes

* **deps:** update dependency org.jetbrains.kotlinx:kotlinx-coroutines-core to v1.10.2 ([ca2e4f5](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/ca2e4f5dc9d0caa0734b614e601e0e59eb19b47d))
* **deps:** update dependency org.jetbrains.kotlinx:kotlinx-datetime to v0.7.1-0.6.x-compat ([c608d91](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/c608d91839d440e9d633e221d6f565af5e5a8f48))


### Features

* **renovate:** change automerge type to branch for all update types in renovate.json ([0c64a5e](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/0c64a5ee580be6f6e8498cd3eaee617da71cb639))

# 1.0.0 (2025-08-12)


### Bug Fixes

* **api-gateway:** build and run deploy ([b526503](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/b5265034b565737fb2e665186405470fa03b59da))
* **ci-cd:** add build and test dependency on publish docker images ([52b1af9](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/52b1af9ad783a164c097f28e3deae7bc0e8c0087))
* **ci-cd:** dockerhub name ([6e99e84](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/6e99e846c5b1ea949474a71af49a090953379bf3))
* **ci-cd:** java version ([23c1572](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/23c1572fef5930ebd3ac81c3ab5ad2d1ce04567b))
* **ci-cd:** wrong dockerhub repo name ([8595237](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/8595237d139c4f9877c39d30568ca3d9c965d0b6))
* **semantic-release:** url to project ([abbbd5f](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/abbbd5f55e1bcce6d27a4e166773ef4fb0656362))


### Features

* add initial package.json for project setup ([8a5c97e](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/8a5c97e665a7db26b52ded1bc1359793beee28d1))
* **ci-cd:** add ci-cd ([4400f87](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/4400f876aa9f6b53d0362e4dd9d43e8f1a3dacfa))
* **ci-cd:** add ci-cd ([75ecc33](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/75ecc33a134c3df6d76ff59d9faee10a9dd12242))
* **ci-cd:** add dependabot settings ([b60b4c5](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/b60b4c57411a3a02137b61afb19a499386f798a6))
* **ci-cd:** add dependabot settings ([c4c6574](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/c4c6574a88c6f809ab279eec8045a2b805ad380a))
* **ci-cd:** make simpler ci-cd ([61c3764](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/61c376468d5c1b18c6f0ce92090ff4e3bd233050))
* **ci-cd:** make simpler ci-cd ([954a51f](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/954a51ff9af85fba024036727926ce46a34fcf85))
* **ci-cd:** make simpler ci-cd ([edb30f6](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/edb30f67227e2c0a78055d35f190217c079fde60))
* **ci-cd:** remove no longer used java versions and distribution ([562cfe6](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/562cfe6096b83f274bfca924d20cee02b3145c11))
* **deploy:** remove healthcheck on api_gateway ([babaffe](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/babaffea02803a32f1db400938d460cf221b38cc))
* **semantic-release:** add semantic release bot configuration ([4704ef9](https://github.com/MarcoFontana48/AUSL-Romagna-microservizi-CCE-proposta-di-progetto/commit/4704ef97d3a418a127be1cd8495a168cbbb4e548))
