package com.example.autoclicker.core
import org.junit.Assert.*
import org.junit.Test
class RunOptionsTest {
    @Test fun defaultsAndBounds() {
        assertTrue(RunOptions().isValid())
        assertTrue(RunOptions(intervalMs=10,pressMs=1,maxClicks=1000000).isValid())
        assertTrue(RunOptions(intervalMs=100,pressMs=100,maxDurationMs=86400000).isValid())
        listOf(0,1,3,5).forEach { assertTrue(RunOptions(countdownSeconds=it).isValid()) }
    }
    @Test fun rejectsInvalidAndConflictingLimits() {
        listOf(RunOptions(pressMs=101), RunOptions(maxClicks=1000001), RunOptions(maxDurationMs=999),
            RunOptions(maxDurationMs=86400001), RunOptions(maxDurationMs=-1), RunOptions(countdownSeconds=-1),
            RunOptions(maxClicks=1,maxDurationMs=1000)).forEach { assertFalse(it.isValid()) }
    }
}
