package com.breathein.core.common

/**
 * TODO(check-in): the wholesome check-in shown after "I'm done"
 * ("Something worth sharing with your family?"). Not built yet; the monitor already calls this hook.
 */
interface CheckInLauncher {
    fun launch()
}

object NoOpCheckInLauncher : CheckInLauncher {
    override fun launch() = Unit
}
