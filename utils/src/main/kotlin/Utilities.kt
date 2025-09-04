package mf.cce.utils

object HttpStatus {
    const val OK = 200
    const val CREATED = 201
    const val ACCEPTED = 202
    const val NO_CONTENT = 204
    const val BAD_REQUEST = 400
    const val UNAUTHORIZED = 401
    const val FORBIDDEN = 403
    const val NOT_FOUND = 404
    const val CONFLICT = 409
    const val INTERNAL_SERVER_ERROR = 500
    const val SERVICE_UNAVAILABLE = 503
}

object Ports {
    const val HTTP = 8080
}

object Endpoints {
    const val HEALTH = "/health"
    const val SERVICE = "/service"  // a generic test microservice endpoint, not to be used in production
    const val TERAPIA = "/terapia"
    const val DIARIO_CLINICO = "/diario-clinico"
    const val ANAMNESI_PREGRESSA = "/anamnesi-pregressa"
    const val METRICS = "/metrics"
    const val DUMMIES = "/dummies"  // a generic test entity, not to be used in production
    // those follow FHIR's RESTful convention, where the name has to match the resource name in CamelCase (see docs: https://hl7.org/fhir/http.html)
    const val ALLERGY_INTOLERANCES = "/AllergyIntolerance"
    const val ENCOUNTERS = "/Encounter"
    const val CARE_PLANS = "/CarePlan"
}