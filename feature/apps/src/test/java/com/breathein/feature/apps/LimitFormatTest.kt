package com.breathein.feature.apps

import org.junit.Assert.assertEquals
import org.junit.Test

class LimitFormatTest {

    @Test
    fun `formats seconds minutes and hours`() {
        assertEquals("10 s", formatLimit(10))
        assertEquals("1 min", formatLimit(60))
        assertEquals("45 min", formatLimit(45 * 60))
        assertEquals("1 h", formatLimit(3_600))
        assertEquals("1 h 30 min", formatLimit(5_400))
    }
}
