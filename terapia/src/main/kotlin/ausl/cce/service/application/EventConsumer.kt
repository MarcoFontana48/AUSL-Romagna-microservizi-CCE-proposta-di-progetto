package ausl.cce.service.application

import mf.cce.utils.AllergyDiagnosed

/**
 * interface for AllergyIntolerance event handlers.
 */
interface AllergyIntoleranceEventHandler {
    fun allergyDiagnosedHandler(event: AllergyDiagnosed)
}
