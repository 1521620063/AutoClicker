package com.example.autoclicker.core

class ServiceState {
    var connected = false
        private set
    var running = false
        private set
    fun connect() { connected = true; running = false }
    fun disconnect() { connected = false; running = false }
    fun setRunning(value: Boolean) { running = value && connected }
    fun canStart(interactive: Boolean, locked: Boolean, intervalMs: Long): Boolean =
        connected && !running && interactive && !locked && intervalMs in IntervalValidator.MIN..IntervalValidator.MAX
    fun canEditSettings(): Boolean = !running
}
