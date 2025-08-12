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
    const val SERVICE = "/service"
    const val METRICS = "/metrics"
}