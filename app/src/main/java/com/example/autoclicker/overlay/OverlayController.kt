package com.example.autoclicker.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.DisplayMetrics
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.autoclicker.core.*
import kotlin.math.abs
import com.example.autoclicker.ui.UiStyle

/** Owns two small touchable windows; never creates a full-screen interception layer. */
class OverlayController(
    private val context: Context,
    private val onStart: (ClickPoint) -> Unit,
    private val onStop: () -> Unit,
    private val onClose: () -> Unit,
    private val onPanelDrag: () -> Unit,
    private val onError: (String) -> Unit
) {
    private val wm = requireNotNull(context.getSystemService(WindowManager::class.java)) { "Window manager is unavailable" }
    private val green = UiStyle.green
    private val white = Color.rgb(248, 250, 245)
    private var running = false
    private var positioned = false
    private var closed = true
    private var targetHidden = false
    private val label = TextView(context)
    private val statistics = TextView(context)
    private val toggle = Button(context)
    private val target = TargetView(context)
    private val panel = LinearLayout(context)
    private val targetParams = params(dp(56), dp(56)).apply {
        // The marker may extend outside usable bounds, but its center stays selectable.
        flags = flags or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        if (Build.VERSION.SDK_INT >= 30) setFitInsetsTypes(0)
    }
    private val panelParams = params(dp(236), WindowManager.LayoutParams.WRAP_CONTENT)
    private val targetWindow = ManagedWindow(target, targetParams)
    private val panelWindow = ManagedWindow(panel, panelParams)
    private val windows = WindowGroup(listOf(panelWindow, targetWindow))

    init {
        panel.orientation = LinearLayout.VERTICAL
        panel.setPadding(dp(12), dp(8), dp(12), dp(12))
        panel.background = UiStyle.surface(context, Color.rgb(26, 48, 43), 20)
        panel.elevation = dp(8).toFloat()
        label.text = "⠿  轻点  ·  拖动移动"
        label.setTextColor(white)
        label.textSize = 12f
        label.setPadding(dp(8), dp(8), dp(8), dp(10))
        label.contentDescription = "控制器拖动手柄"
        label.setSingleLine(true)
        label.ellipsize = android.text.TextUtils.TruncateAt.END
        panel.addView(label)
        statistics.text = "等待开始\n完成手势 0 次"
        statistics.setTextColor(white)
        statistics.textSize = 12f
        statistics.setLines(2)
        statistics.ellipsize = android.text.TextUtils.TruncateAt.END
        statistics.setPadding(dp(8),0,dp(8),0)
        // Reserve this area even with frequency disabled. Running text never enlarges the panel.
        panel.addView(statistics, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(56)))
        val row = LinearLayout(context)
        row.orientation = LinearLayout.HORIZONTAL
        toggle.text = "开始"
        toggle.isAllCaps = false
        toggle.textSize = 14f
        toggle.setTextColor(green)
        UiStyle.styleButton(toggle, primary = true)
        toggle.setOnClickListener {
            if (running) onStop()
            else {
                val point = targetPoint()
                if (!positioned) toast("请先拖动靶心到要点击的位置")
                else if (OverlayGeometry.overlapsTarget(point, panelBounds())) toast("控制器挡住了点击位置，请先移开控制器")
                else onStart(point)
            }
        }
        row.addView(toggle, LinearLayout.LayoutParams(0, dp(48), 1f))
        val close = Button(context)
        close.text = "关闭"
        close.isAllCaps = false
        close.textSize = 14f
        close.setTextColor(white)
        UiStyle.styleButton(close)
        close.setOnClickListener { onClose() }
        val closeLayout = LinearLayout.LayoutParams(0, dp(48), 1f)
        closeLayout.leftMargin = dp(6)
        row.addView(close, closeLayout)
        panel.addView(row)
        target.contentDescription = "拖动靶心，选择点击位置"
        installDrag(label, panel, panelParams, isTarget = false)
        installDrag(target, target, targetParams, isTarget = true)
    }

    fun show() {
        if (!closed) return
        val bounds = usableBounds()
        panelParams.x = dp(12)
        panelParams.y = dp(80)
        targetParams.x = ((bounds.right - bounds.left) * 0.55f - dp(28)).toInt()
        targetParams.y = ((bounds.bottom - bounds.top) * 0.50f - dp(28)).toInt()
        try { windows.show(); closed = false; targetHidden = false }
        catch (e: RuntimeException) { windows.close(); throw e }
    }

    fun hideTarget() {
        if (closed) return
        targetWindow.detach()
        targetHidden = true
    }
    fun restoreTarget() {
        if (closed || !targetHidden) return
        try { targetWindow.attach(); targetHidden = false }
        catch (_: RuntimeException) { onError("无法恢复靶心，控制器已关闭") }
    }
    fun setRunning(value: Boolean) {
        running = value
        toggle.text = if (value) "停止" else "开始"
        UiStyle.styleButton(toggle, primary = true, destructive = value)
        label.text = if (value) "已启动 · 拖动会停止" else "⠿  轻点  ·  拖动移动"
        if (!value) statistics.text = "已停止\n完成手势 0 次"
        toggle.contentDescription = if (value) "停止持续点击" else "开始持续点击"
    }
    fun setProgress(text: String) { if (!closed && running) statistics.text = text }
    fun setIdleStatistics(text: String) { if (!closed && !running) statistics.text = text }
    fun close() {
        closed = true
        running = false
        positioned = false
        windows.close()
        targetHidden = false
    }

    private fun targetPoint(): ClickPoint {
        val loc = IntArray(2)
        target.getLocationOnScreen(loc)
        return OverlayGeometry.clamp(ClickPoint(loc[0] + target.width / 2f, loc[1] + target.height / 2f), usableBounds())
    }
    private fun panelBounds(): ScreenBounds {
        val loc = IntArray(2)
        panel.getLocationOnScreen(loc)
        val margin = dp(4).toFloat()
        return ScreenBounds(loc[0] - margin, loc[1] - margin, loc[0] + panel.width + margin, loc[1] + panel.height + margin)
    }
    private fun installDrag(handle: View, window: View, layout: WindowManager.LayoutParams, isTarget: Boolean) {
        val slop = ViewConfiguration.get(context).scaledTouchSlop
        var downX = 0f; var downY = 0f
        var startX = 0; var startY = 0
        var screenX = 0; var screenY = 0
        var dragging = false
        handle.setOnTouchListener { v, event ->
            if (isTarget && running) return@setOnTouchListener true
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX; downY = event.rawY
                    startX = layout.x; startY = layout.y
                    val loc = IntArray(2); window.getLocationOnScreen(loc)
                    screenX = loc[0]; screenY = loc[1]; dragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX; val dy = event.rawY - downY
                    if (!dragging && (abs(dx) > slop || abs(dy) > slop)) {
                        dragging = true
                        if (!isTarget) onPanelDrag()
                    }
                    if (dragging && !closed) {
                        val b = usableBounds()
                        val origin = OverlayGeometry.windowOrigin(ClickPoint(screenX + dx, screenY + dy), window.width.toFloat(), window.height.toFloat(), b, isTarget)
                        val x = origin.x
                        val y = origin.y
                        // Delta in physical screen coordinates, independent of system inset origin.
                        layout.x = startX + (x - screenX).toInt()
                        layout.y = startY + (y - screenY).toInt()
                        try {
                            wm.updateViewLayout(window, layout)
                            if (isTarget) { positioned = true; target.invalidate() }
                        } catch (_: RuntimeException) { onError("窗口位置更新失败，已停止并关闭") }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> { if (!dragging) v.performClick(); true }
                MotionEvent.ACTION_CANCEL -> { dragging = false; true }
                else -> false
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun usableBounds(): ScreenBounds {
        if (Build.VERSION.SDK_INT >= 30) {
            val metrics = wm.currentWindowMetrics
            val b = metrics.bounds
            val insets = metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            return ScreenBounds((b.left + insets.left).toFloat(), (b.top + insets.top).toFloat(), (b.right - insets.right).toFloat(), (b.bottom - insets.bottom).toFloat())
        }
        val metrics = DisplayMetrics()
        wm.defaultDisplay.getRealMetrics(metrics)
        val frame = android.graphics.Rect()
        if (panel.isAttachedToWindow) {
            panel.getWindowVisibleDisplayFrame(frame)
            if (frame.width() > panel.width && frame.height() > panel.height) return ScreenBounds(frame.left.toFloat(), frame.top.toFloat(), frame.right.toFloat(), frame.bottom.toFloat())
        }
        val statusId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        val status = if (statusId > 0) context.resources.getDimensionPixelSize(statusId) else dp(24)
        return ScreenBounds(0f, status.toFloat(), metrics.widthPixels.toFloat(), (metrics.heightPixels - dp(48)).toFloat())
    }
    private fun params(width: Int, height: Int) = WindowManager.LayoutParams(width, height,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.LEFT }
    private fun dp(value: Int) = (value * context.resources.displayMetrics.density + 0.5f).toInt()
    private fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    private fun rounded(color: Int, radius: Float) = GradientDrawable().apply { setColor(color); cornerRadius = radius }

    private inner class ManagedWindow(private val view: View, private val layout: WindowManager.LayoutParams) : OverlayWindow {
        private var attached = false
        override fun attach() { if (!attached) { wm.addView(view, layout); attached = true } }
        override fun detach() { if (attached || view.isAttachedToWindow) { try { wm.removeViewImmediate(view) } finally { attached = false } } }
    }
    private inner class TargetView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f; val cy = height / 2f
            paint.style = Paint.Style.FILL; paint.color = Color.argb(210, 23, 106, 87)
            canvas.drawCircle(cx, cy, width * 0.43f, paint)
            paint.style = Paint.Style.STROKE; paint.strokeWidth = dp(2).toFloat(); paint.color = Color.WHITE
            canvas.drawCircle(cx, cy, width * 0.29f, paint)
            canvas.drawLine(cx, cy - dp(18), cx, cy + dp(18), paint)
            canvas.drawLine(cx - dp(18), cy, cx + dp(18), cy, paint)
            paint.style = Paint.Style.FILL; paint.color = if (positioned) Color.rgb(217, 244, 172) else Color.WHITE
            canvas.drawCircle(cx, cy, dp(4).toFloat(), paint)
        }
        override fun performClick(): Boolean { super.performClick(); toast("拖动这个靶心到要点击的位置"); return true }
    }
}
