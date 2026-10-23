package com.breathein.core.common

import com.breathein.core.model.InterruptionOutcome
import com.breathein.core.model.InterruptionRequest

/** Shows the interruption to the user. Implemented by :feature:overlay. Safe to call from any thread. */
interface InterruptionTrigger {
    /** Shows the interruption; ignored if one is already showing. [onOutcome] is called once, on the main thread. */
    fun show(request: InterruptionRequest, onOutcome: (InterruptionOutcome) -> Unit)

    fun dismiss()
}
