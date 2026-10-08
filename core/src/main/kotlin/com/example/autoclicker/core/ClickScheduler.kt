package com.example.autoclicker.core

data class ClickPoint(val x: Float, val y: Float)
interface SchedulerClock {
    fun nowMs(): Long
    fun postDelayed(delayMs: Long, action: () -> Unit)
    fun cancelPending()
}
interface GestureDriver {
    fun dispatch(point: ClickPoint, durationMs: Long, complete: (Boolean) -> Unit): Boolean
}

/** All calls, including driver callbacks, must be serialized on the same thread. */
class ClickScheduler(
    private val clock: SchedulerClock,
    private val driver: GestureDriver,
    private val onProgress: (Long, Long, Long) -> Unit = { _, _, _ -> },
    private val onStopped: (String) -> Unit = {}
) {
    var isRunning = false
        private set
    private val frequency = ClickFrequency()
    val clickRatePerSecond: Double
        get() = if (isRunning && options.showFrequency) frequency.perSecond(clock.nowMs()) else 0.0
    private var generation = 0L
    private var nextGesture = 0L
    private var inFlight: Long? = null
    private var point = ClickPoint(0f, 0f)
    private var options = RunOptions()
    var completedClicks = 0L
        private set
    private var startAt = 0L
    private var deadline = 0L
    private var lastDispatch: Long? = null

    fun start(point: ClickPoint, intervalMs: Long): Boolean = start(point, RunOptions(intervalMs=intervalMs, pressMs=minOf(20L,intervalMs)))

    fun start(point: ClickPoint, options: RunOptions): Boolean {
        if (isRunning || !options.isValid() ||
            !point.x.isFinite() || !point.y.isFinite() || point.x < 0 || point.y < 0) return false
        generation++
        this.point = point
        this.options = options
        completedClicks = 0
        frequency.reset()
        startAt = clock.nowMs() + options.countdownSeconds * 1000L
        deadline = if (options.maxDurationMs > 0) startAt + options.maxDurationMs else Long.MAX_VALUE
        lastDispatch = null
        isRunning = true
        schedule()
        pulse(generation)
        return true
    }

    fun stop() {
        isRunning = false
        generation++
        clock.cancelPending()
        frequency.reset()
        // An accepted gesture cannot be recalled. Keep inFlight until its callback,
        // even across a stop/start, so the next run cannot overlap the previous one.
    }

    private fun schedule() {
        if (!isRunning || inFlight != null) return
        val run = generation
        val now = clock.nowMs()
        if (now >= deadline) { fail("已达到运行时长，自动停止"); return }
        val delay = lastDispatch?.let { (options.intervalMs - (now - it)).coerceAtLeast(0) } ?: (startAt-now).coerceAtLeast(0)
        clock.postDelayed(delay) {
            if (isRunning && generation == run && inFlight == null) send(run)
        }
    }

    private fun pulse(run: Long) {
        if (!isRunning || run != generation) return
        val now = clock.nowMs()
        if (now >= deadline) { fail("已达到运行时长，自动停止"); return }
        onProgress(completedClicks, (now-startAt).coerceAtLeast(0), (startAt-now).coerceAtLeast(0))
        clock.postDelayed(minOf(500L, (deadline-now).coerceAtLeast(1))) { pulse(run) }
    }

    private fun send(run: Long) {
        if (clock.nowMs() >= deadline) { fail("已达到运行时长，自动停止"); return }
        val token = ++nextGesture
        inFlight = token
        lastDispatch = clock.nowMs()
        val accepted = try {
            driver.dispatch(point, options.pressMs) { success ->
                if (inFlight == token) {
                    inFlight = null
                    if (isRunning) {
                        if (run != generation) {
                            // Release the old gesture, but never count it in the new run.
                            schedule()
                        } else if (clock.nowMs() >= deadline) {
                            // A delayed main-thread callback must not extend the timed run.
                            fail("已达到运行时长，自动停止")
                        } else if (!success) {
                            fail("手势被系统取消，已停止")
                        } else {
                            completedClicks++
                            if (options.showFrequency) frequency.recordCompleted(clock.nowMs())
                            if (options.maxClicks > 0 && completedClicks >= options.maxClicks) fail("已完成 $completedClicks 次手势，自动停止")
                            else schedule()
                        }
                    }
                }
            }
        } catch (_: RuntimeException) { false }
        if (!accepted && inFlight == token) {
            inFlight = null
            if (isRunning && run == generation) fail("手势分发失败，已停止")
        }
    }

    private fun fail(reason: String) {
        stop()
        onStopped(reason)
    }
}
