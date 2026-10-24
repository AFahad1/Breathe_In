package com.breathein.feature.apps

/** "10 s", "5 min", "1 h", "1 h 30 min". */
internal fun formatLimit(seconds: Int): String = when {
    seconds < 60 -> "$seconds s"
    seconds < 3_600 -> "${seconds / 60} min"
    seconds % 3_600 == 0 -> "${seconds / 3_600} h"
    else -> "${seconds / 3_600} h ${seconds % 3_600 / 60} min"
}
