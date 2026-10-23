package com.breathein.feature.usage

/** One activity moving into or out of the foreground. */
internal data class ForegroundEvent(
    val packageName: String,
    val className: String,
    val timestampMillis: Long,
    val type: Type,
) {
    enum class Type { Resumed, Paused }
}
