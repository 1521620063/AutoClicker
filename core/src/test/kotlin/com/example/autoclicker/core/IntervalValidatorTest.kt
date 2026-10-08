package com.example.autoclicker.core
import org.junit.Assert.*
import org.junit.Test
class IntervalValidatorTest {
    @Test fun acceptsBoundsAndDefault() { listOf(10L, 20L, 50L, 100L, 60000L).forEach { assertEquals(it, IntervalValidator.parse(it.toString())) } }
    @Test fun rejectsInvalid() { listOf("", " ", "9", "60001", "-1", "1.5", "abc", "9".repeat(500), " 100", "+100").forEach { assertNull(it, IntervalValidator.parse(it)) } }
    @Test fun corruptedStorageFallsBack() { assertEquals(100L, IntervalValidator.stored(-1)); assertEquals(60000L, IntervalValidator.stored(60000)) }
}
