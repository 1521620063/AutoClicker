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
    @Test fun invalidInputCannotStart() { for(i in listOf(9L,60001L,-1L)) assertFalse(scheduler.start(point,i)); assertFalse(scheduler.start(ClickPoint(Float.NaN,1f),100)); assertFalse(scheduler.start(ClickPoint(-1f,1f),100)); assertEquals(0,driver.count) }
    @Test fun configurableDurationAndCountLimit() {
        assertTrue(scheduler.start(point, RunOptions(intervalMs=10, pressMs=1, maxClicks=2)))
        clock.advance(0); assertEquals(1L, driver.duration)
        driver.finish(); clock.advance(10); driver.finish()
        assertFalse(scheduler.isRunning); clock.advance(100); assertEquals(2,driver.count)
        assertEquals(2L,scheduler.completedClicks)
    }
    @Test fun countdownCanBeCancelled() {
        scheduler.start(point, RunOptions(countdownSeconds=3)); clock.advance(2999)
        assertEquals(0,driver.count); scheduler.stop(); clock.advance(10000); assertEquals(0,driver.count)
    }
    @Test fun countdownThenTimedStopDoesNotWaitForCallback() {
        scheduler.start(point, RunOptions(countdownSeconds=1, maxDurationMs=1000))
        clock.advance(999); assertEquals(0,driver.count)
        clock.advance(1); assertEquals(1,driver.count)
        clock.advance(1000); assertFalse(scheduler.isRunning)
        driver.finish(); clock.advance(5000); assertEquals(1,driver.count)
    }
    @Test fun invalidOptionsRejected() {
        listOf(RunOptions(intervalMs=10,pressMs=11),RunOptions(pressMs=0),
            RunOptions(countdownSeconds=2),RunOptions(maxClicks=1,maxDurationMs=1000),
            RunOptions(maxClicks=-1)).forEach { assertFalse(scheduler.start(point,it)) }
    }
    @Test fun restartIgnoresOldCompletionInNewCount() {
        scheduler.start(point,RunOptions(maxClicks=1)); clock.advance(0); scheduler.stop()
        scheduler.start(point,RunOptions(maxClicks=1)); driver.finish(); clock.advance(0)
        assertEquals(0L,scheduler.completedClicks); driver.finish(); assertEquals(1L,scheduler.completedClicks)
        assertFalse(scheduler.isRunning)
    }
    @Test fun lowIntervalWaitsForSlowGestureWithoutBurst() {
        scheduler.start(point,RunOptions(intervalMs=10,pressMs=5)); clock.advance(0)
        clock.advance(1000); assertEquals(1,driver.count)
        driver.finish(); clock.advance(0); assertEquals(2,driver.count)
        clock.advance(1000); assertEquals(2,driver.count)
    }
    @Test fun stopAndRestartCancelPreviousDeadline() {
        scheduler.start(point,RunOptions(maxDurationMs=1000)); clock.advance(0)
        scheduler.stop(); scheduler.start(point,RunOptions())
        driver.finish(); clock.advance(2000); assertTrue(scheduler.isRunning)
    }
    @Test fun progressSeparatesCountdownAndElapsed() {
        val progress=mutableListOf<Triple<Long,Long,Long>>()
        val observed=ClickScheduler(clock,driver,onProgress={ c,e,r -> progress.add(Triple(c,e,r)) })
        observed.start(point,RunOptions(countdownSeconds=1)); assertEquals(Triple(0L,0L,1000L),progress.last())
        clock.advance(1000); driver.finish(); clock.advance(500)
        assertEquals(Triple(1L,500L,0L),progress.last())
        observed.stop(); val size=progress.size; clock.advance(1000); assertEquals(size,progress.size)
    }
    @Test fun frequencyCountsOnlySuccessfulCallbacksAndDecays() {
        scheduler.start(point,RunOptions(intervalMs=1000,showFrequency=true));clock.advance(0)
        assertEquals(0.0,scheduler.clickRatePerSecond,0.0)
        driver.finish();assertEquals(1.0,scheduler.clickRatePerSecond,0.0)
        clock.advance(999);assertEquals(1.0,scheduler.clickRatePerSecond,0.0)
        clock.advance(1);assertEquals(0.0,scheduler.clickRatePerSecond,0.0)
        driver.finish(false);assertEquals(0.0,scheduler.clickRatePerSecond,0.0)
    }
    @Test fun frequencyResetsOnStopAndOldCallbackCannotLeakIntoRestart() {
        val options=RunOptions(showFrequency=true)
        scheduler.start(point,options);clock.advance(0);driver.finish()
        assertEquals(1.0,scheduler.clickRatePerSecond,0.0)
        clock.advance(100);scheduler.stop();assertEquals(0.0,scheduler.clickRatePerSecond,0.0)
        scheduler.start(point,options);driver.finish();clock.advance(0)
        assertEquals(0.0,scheduler.clickRatePerSecond,0.0)
        driver.finish();assertEquals(1.0,scheduler.clickRatePerSecond,0.0)
    }
    @Test fun frequencyDisabledAndCountdownAreZero() {
        scheduler.start(point,RunOptions());clock.advance(0);driver.finish()
        assertEquals(0.0,scheduler.clickRatePerSecond,0.0);scheduler.stop()
        scheduler.start(point,RunOptions(countdownSeconds=1,showFrequency=true));clock.advance(999)
        assertEquals(0.0,scheduler.clickRatePerSecond,0.0)
        clock.advance(1);driver.finish();assertEquals(1.0,scheduler.clickRatePerSecond,0.0)
    }
    @Test fun progressRefreshesAtHalfSecondNotOnEveryGesture() {
        var updates=0
        val observed=ClickScheduler(clock,driver,onProgress={ _,_,_ -> updates++ })
        observed.start(point,RunOptions());clock.advance(0);driver.finish()
        assertEquals(1,updates);clock.advance(499);assertEquals(1,updates)
        clock.advance(1);assertEquals(2,updates)
    }
    @Test fun completionAtOrAfterDeadlineCannotCountBeforeDelayedTimerRuns() {
        listOf(1000L,1001L).forEach { completionTime ->
            val delayedClock=Clock(); val delayedDriver=Driver(); val stopped=mutableListOf<String>()
            val observed=ClickScheduler(delayedClock,delayedDriver,onStopped={ stopped.add(it) })
            observed.start(point,RunOptions(maxDurationMs=1000,showFrequency=true))
            delayedClock.advance(0)
            // Simulate a blocked main thread: clock advances but queued timer has not run yet.
            delayedClock.time=completionTime
            delayedDriver.finish()
            assertEquals(0L,observed.completedClicks)
            assertFalse(observed.isRunning)
            assertEquals(0.0,observed.clickRatePerSecond,0.0)
            assertEquals(listOf("已达到运行时长，自动停止"),stopped)
            delayedClock.advance(1000);assertEquals(1,delayedDriver.count)
        }
    }
}
