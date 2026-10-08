package com.example.autoclicker.core
import org.junit.Assert.*
import org.junit.Test
class ClickSchedulerTest {
    private class Clock : SchedulerClock {
        var time = 0L
        val jobs = mutableListOf<Pair<Long, () -> Unit>>()
        override fun nowMs() = time
        override fun postDelayed(delayMs: Long, action: () -> Unit) { jobs.add((time + delayMs) to action) }
        override fun cancelPending() { jobs.clear() }
        fun advance(delta: Long) {
            val until = time + delta
            while (true) { val job = jobs.minByOrNull { it.first } ?: break; if (job.first > until) break; jobs.remove(job); time = job.first; job.second() }
            time = until
        }
    }
    private class Driver : GestureDriver {
        var accepted = true
        var count = 0
        var pending: ((Boolean) -> Unit)? = null
        var duration = 0L
        override fun dispatch(point: ClickPoint, durationMs: Long, complete: (Boolean) -> Unit): Boolean {
            check(pending == null) { "overlapping gesture" }; count++; duration = durationMs
            if (accepted) pending = complete
            return accepted
        }
        fun finish(success: Boolean = true) { val callback = pending!!; pending = null; callback(success) }
    }
    private val clock = Clock()
    private val driver = Driver()
    private val reasons = mutableListOf<String>()
    private val scheduler = ClickScheduler(clock, driver) { reasons.add(it) }
    private val point = ClickPoint(200f, 300f)
    @Test fun startsSingleShortGesture() { assertTrue(scheduler.start(point,100)); clock.advance(0); assertTrue(scheduler.isRunning); assertEquals(1,driver.count); assertEquals(20L,driver.duration) }
    @Test fun rejectsDuplicateStart() { assertTrue(scheduler.start(point,100)); assertFalse(scheduler.start(point,100)); clock.advance(0); assertEquals(1,driver.count) }
    @Test fun intervalIsBetweenDispatches() { scheduler.start(point,100); clock.advance(0); clock.advance(20); driver.finish(); clock.advance(79); assertEquals(1,driver.count); clock.advance(1); assertEquals(2,driver.count) }
    @Test fun neverOverlapsAndDoesNotCatchUp() { scheduler.start(point,100); clock.advance(1000); assertEquals(1,driver.count); driver.finish(); clock.advance(0); assertEquals(2,driver.count); clock.advance(1000); assertEquals(2,driver.count) }
    @Test fun stopIgnoresLateCallback() { scheduler.start(point,100); clock.advance(0); scheduler.stop(); driver.finish(); clock.advance(10000); assertFalse(scheduler.isRunning); assertEquals(1,driver.count) }
    @Test fun stopCancelsBeforeFirstDispatch() { scheduler.start(point,100); scheduler.stop(); clock.advance(1000); assertEquals(0,driver.count) }
    @Test fun restartWaitsForOldGestureCompletion() { scheduler.start(point,100); clock.advance(0); scheduler.stop(); assertTrue(scheduler.start(point,50)); clock.advance(500); assertEquals(1,driver.count); driver.finish(); clock.advance(0); assertEquals(2,driver.count); assertTrue(scheduler.isRunning) }
    @Test fun oldCancelledCallbackDoesNotStopRestart() { scheduler.start(point,100); clock.advance(0); scheduler.stop(); scheduler.start(point,100); driver.finish(false); clock.advance(0); assertTrue(scheduler.isRunning); assertEquals(2,driver.count) }
    @Test fun dispatchRejectedStops() { driver.accepted = false; scheduler.start(point,100); clock.advance(0); assertFalse(scheduler.isRunning); assertEquals(listOf("手势分发失败，已停止"),reasons); clock.advance(1000); assertEquals(1,driver.count) }
    @Test fun cancelledGestureStops() { scheduler.start(point,100); clock.advance(0); driver.finish(false); clock.advance(1000); assertFalse(scheduler.isRunning); assertEquals(1,driver.count); assertEquals(listOf("手势被系统取消，已停止"),reasons) }
    @Test fun stopIsIdempotent() { scheduler.stop(); scheduler.stop(); assertFalse(scheduler.isRunning); assertTrue(scheduler.start(point,100)); scheduler.stop(); scheduler.stop(); clock.advance(500); assertEquals(0,driver.count) }
    @Test fun invalidInputCannotStart() { for(i in listOf(49L,60001L,-1L)) assertFalse(scheduler.start(point,i)); assertFalse(scheduler.start(ClickPoint(Float.NaN,1f),100)); assertFalse(scheduler.start(ClickPoint(-1f,1f),100)); assertEquals(0,driver.count) }
}
