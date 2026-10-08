package com.example.autoclicker.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.example.autoclicker.core.*
import com.example.autoclicker.overlay.OverlayController
import com.example.autoclicker.settings.SettingsRepository

class AutoClickAccessibilityService : AccessibilityService() {
    companion object {
        var instance: AutoClickAccessibilityService? = null
            private set
    }
    private val callbackHandler = Handler(Looper.getMainLooper())
    private val scheduleHandler = Handler(Looper.getMainLooper())
    private val safetyHandler = Handler(Looper.getMainLooper())
    private var safetyMonitor: SafetyMonitor? = null
    private var scheduler: ClickScheduler? = null
    private var overlay: OverlayController? = null
    private var receiverRegistered = false
    private var showFrequency = false
    private val screenOff = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) stopClicking("屏幕已关闭，点击已停止")
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        if (scheduler != null) cleanup()
        instance = this
        ServiceStatus.state.connect()
        scheduler = ClickScheduler(object : SchedulerClock {
            override fun nowMs() = SystemClock.uptimeMillis()
            override fun postDelayed(delayMs: Long, action: () -> Unit) { scheduleHandler.postDelayed({ action() }, delayMs) }
            override fun cancelPending() { scheduleHandler.removeCallbacksAndMessages(null) }
        }, object : GestureDriver {
            override fun dispatch(point: ClickPoint, durationMs: Long, complete: (Boolean) -> Unit): Boolean {
                if (!deviceReady()) return false
                val path = Path().apply { moveTo(point.x, point.y) }
                val gesture = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, durationMs)).build()
                return dispatchGesture(gesture, object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) { complete(true) }
                    override fun onCancelled(gestureDescription: GestureDescription?) { complete(false) }
                }, callbackHandler)
            }
        }, onProgress = { count, elapsed, remaining ->
            val summary=if(remaining>0) "倒计时 ${(remaining+999)/1000} 秒 · 可停止"
                else "完成 $count 次手势 · ${elapsed/1000} 秒"
            val frequencyText=if (remaining>0) "实时频率：等待开始" else
                "实时频率：${String.format(java.util.Locale.ROOT, "%.1f", scheduler?.clickRatePerSecond ?: 0.0)} 次/秒"
            val message=summary + if(showFrequency) "\n$frequencyText" else "\n拖动控制条会停止"
            overlay?.setProgress(message)
            ServiceStatus.publish(message)
        }) { reason -> stopClicking(reason) }
        safetyMonitor = SafetyMonitor(object : SchedulerClock {
            override fun nowMs() = SystemClock.uptimeMillis()
            override fun postDelayed(delayMs: Long, action: () -> Unit) { safetyHandler.postDelayed({ action() }, delayMs) }
            override fun cancelPending() { safetyHandler.removeCallbacksAndMessages(null) }
        }, { deviceReady() }, { stopClicking("设备已锁定或熄屏，点击已停止") })
        if (!receiverRegistered) {
            if (Build.VERSION.SDK_INT >= 33) registerReceiver(screenOff, IntentFilter(Intent.ACTION_SCREEN_OFF), Context.RECEIVER_NOT_EXPORTED)
            else @Suppress("DEPRECATION") registerReceiver(screenOff, IntentFilter(Intent.ACTION_SCREEN_OFF))
            receiverRegistered = true
        }
        ServiceStatus.publish("服务已连接，可以显示控制器")
    }

    private fun deviceReady(): Boolean =
        getSystemService(PowerManager::class.java)?.isInteractive == true && getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == false

    fun showController(): Boolean {
        if (instance !== this || scheduler == null) return false
        if (!deviceReady()) { report("请先解锁并点亮屏幕"); return false }
        if (overlay != null) { report("控制器已经显示，请拖动靶心定位"); return true }
        val controller = OverlayController(this,
            onStart = { point -> startClicking(point) },
            onStop = { stopClicking("已停止，可以重新定位") },
            onClose = { closeController("控制器已关闭") },
            onPanelDrag = { if (scheduler?.isRunning == true) stopClicking("移动控制器，点击已停止") },
            onError = { text -> closeController(text); report(text) })
        return try {
            overlay = controller
            controller.show()
            showFrequency = SettingsRepository(this).options.showFrequency
            controller.setIdleStatistics(if(showFrequency) "等待开始\n实时频率：0.0 次/秒" else "等待开始\n完成手势 0 次")
            ServiceStatus.publish("拖动靶心定位，然后点击开始")
            true
        } catch (_: RuntimeException) {
            closeController("无法创建悬浮控制器")
            report("无法创建悬浮控制器，请重新开启服务")
            false
        }
    }

    private fun startClicking(point: ClickPoint) {
        val options = SettingsRepository(this).options
        val interval = options.intervalMs
        val power = getSystemService(PowerManager::class.java)
        val keyguard = getSystemService(KeyguardManager::class.java)
        if (!ServiceStatus.state.canStart(power?.isInteractive == true, keyguard?.isKeyguardLocked != false, interval)) {
            report("当前无法开始，请检查服务、锁屏状态及间隔")
            return
        }
        try {
            showFrequency = options.showFrequency
            overlay?.hideTarget()
            // Mark active before start's synchronous progress callback (also covers countdown).
            ServiceStatus.state.setRunning(true)
            overlay?.setRunning(true)
            if (scheduler?.start(point, options) == true) {
                safetyMonitor?.start()
            } else { stopClicking("无法开始点击") }
        } catch (_: RuntimeException) { closeController("悬浮窗口异常，点击已停止") }
    }

    fun stopClicking(reason: String = "已停止") {
        safetyMonitor?.stop()
        scheduler?.stop()
        ServiceStatus.state.setRunning(false)
        overlay?.setRunning(false)
        overlay?.setIdleStatistics(if(showFrequency) "已停止\n实时频率：0.0 次/秒" else "已停止\n完成手势 ${scheduler?.completedClicks ?: 0} 次")
        overlay?.restoreTarget()
        ServiceStatus.publish(reason)
    }

    fun closeController(reason: String = "控制器已关闭") {
        safetyMonitor?.stop()
        scheduler?.stop()
        ServiceStatus.state.setRunning(false)
        val old = overlay
        overlay = null
        old?.close()
        ServiceStatus.publish(reason)
    }

    private fun report(text: String) { ServiceStatus.publish(text); Toast.makeText(this, text, Toast.LENGTH_SHORT).show() }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Observe only device readiness, not event text, classes, sources or nodes.
        safetyMonitor?.checkNow()
    }
    override fun onInterrupt() { stopClicking("服务被中断，点击已停止") }
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        closeController("屏幕配置已变化，请重新显示控制器并定位")
    }
    override fun onUnbind(intent: Intent?): Boolean { cleanup(); return super.onUnbind(intent) }
    override fun onDestroy() { cleanup(); super.onDestroy() }
    private fun cleanup() {
        closeController("无障碍服务已断开")
        if (receiverRegistered) { unregisterReceiver(screenOff); receiverRegistered = false }
        scheduleHandler.removeCallbacksAndMessages(null)
        callbackHandler.removeCallbacksAndMessages(null)
        scheduler = null
        safetyMonitor = null
        safetyHandler.removeCallbacksAndMessages(null)
        if (instance === this) { instance = null; ServiceStatus.state.disconnect() }
        ServiceStatus.publish("无障碍服务已断开")
    }
}
