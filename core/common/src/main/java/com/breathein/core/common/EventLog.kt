package com.breathein.core.common

import com.breathein.core.model.InterruptionOutcome
import com.breathein.core.model.InterruptionRequest

/** TODO(event-log): persist interruptions and outcomes in a local database. Not built yet; the monitor already calls this. */
interface EventLog {
    fun interruptionShown(request: InterruptionRequest)
    fun interruptionFinished(outcome: InterruptionOutcome)
}

object NoOpEventLog : EventLog {
    override fun interruptionShown(request: InterruptionRequest) = Unit
    override fun interruptionFinished(outcome: InterruptionOutcome) = Unit
}
