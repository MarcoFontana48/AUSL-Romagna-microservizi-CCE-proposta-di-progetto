package ausl.cce.service.application

import mf.cce.utils.AllergyDiagnosed

interface AllergyIntoleranceEventHandler {
    fun allergyDiagnosedHandler(event: AllergyDiagnosed)
}
