package com.example.autoclicker.core

import org.junit.Assert.*
import org.junit.Test

class ClickFrequencyTest {
    @Test fun emptyWindowIsZero() { assertEquals(0.0, ClickFrequency().perSecond(5000), 0.0) }
    @Test fun countsCompletionsNotIntervalReciprocal() {
        val rate=ClickFrequency()
        listOf(0L,100L,200L,300L).forEach(rate::recordCompleted)
        assertEquals(4.0,rate.perSecond(500),0.0)
    }
    @Test fun rollingWindowExcludesExactlyOneSecondOldCompletion() {
        val rate=ClickFrequency()
        rate.recordCompleted(0);rate.recordCompleted(500);rate.recordCompleted(999)
        assertEquals(3.0,rate.perSecond(999),0.0)
        assertEquals(2.0,rate.perSecond(1000),0.0)
        assertEquals(1.0,rate.perSecond(1500),0.0)
        assertEquals(0.0,rate.perSecond(1999),0.0)
    }
    @Test fun recordsTrimExpiredEventsWithoutDisplayPolling() {
        val rate=ClickFrequency()
        repeat(5000) { rate.recordCompleted(it.toLong()) }
        assertEquals(1000.0,rate.perSecond(4999),0.0)
    }
    @Test fun resetRemovesAllPreviousRunEvents() {
        val rate=ClickFrequency();rate.recordCompleted(100)
        rate.reset(); assertEquals(0.0,rate.perSecond(100),0.0)
        rate.recordCompleted(200);assertEquals(1.0,rate.perSecond(200),0.0)
    }
}
