package com.example.autoclicker.service

import com.example.autoclicker.core.ServiceState

/** Main-thread-only, process-local state. No activity or service is retained by listeners. */
object ServiceStatus {
    val state = ServiceState()
    var message = "等待开启无障碍服务"
        private set
    private val listeners = mutableSetOf<() -> Unit>()
    fun subscribe(listener: () -> Unit) { listeners.add(listener); listener() }
    fun unsubscribe(listener: () -> Unit) { listeners.remove(listener) }
    fun publish(text: String) { message = text; listeners.toList().forEach { it() } }
}
