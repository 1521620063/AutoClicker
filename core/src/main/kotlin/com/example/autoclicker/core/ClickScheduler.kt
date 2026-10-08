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
    private val onStopped: (String) -> Unit = {}
) {
    var isRunning = false
        private set
    private var generation = 0L
    private var nextGesture = 0L
    private var inFlight: Long? = null
    private var point = ClickPoint(0f, 0f)
    private var intervalMs = IntervalValidator.DEFAULT
    private var lastDispatch: Long? = null

    fun start(point: ClickPoint, intervalMs: Long): Boolean {
        if (isRunning || intervalMs !in IntervalValidator.MIN..IntervalValidator.MAX ||
            !point.x.isFinite() || !point.y.isFinite() || point.x < 0 || point.y < 0) return false
        generation++
        this.point = point
        this.intervalMs = intervalMs
        lastDispatch = null
        isRunning = true
        schedule()
        return true
    }

    fun stop() {
        isRunning = false
        generation++
        clock.cancelPending()
        // An accepted gesture cannot be recalled. Keep inFlight until its callback,
        // even across a stop/start, so the next run cannot overlap the previous one.
    }

    private fun schedule() {
        if (!isRunning || inFlight != null) return
        val run = generation
        val delay = lastDispatch?.let { (intervalMs - (clock.nowMs() - it)).coerceAtLeast(0) } ?: 0
        clock.postDelayed(delay) {
            if (isRunning && generation == run && inFlight == null) send(run)
        }
    }

    private fun send(run: Long) {
        val token = ++nextGesture
        inFlight = token
        lastDispatch = clock.nowMs()
        val accepted = try {
            driver.dispatch(point, 20) { success ->
                if (inFlight == token) {
                    inFlight = null
                    if (isRunning) {
                        if (run == generation && !success) fail("手势被系统取消，已停止")
                        else schedule()
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
