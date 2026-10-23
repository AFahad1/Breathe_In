package com.breathein.core.model

/** What the user chose when the interruption finished. */
sealed interface InterruptionOutcome {
    data object KeepScrolling : InterruptionOutcome
    data object ImDone : InterruptionOutcome
}
