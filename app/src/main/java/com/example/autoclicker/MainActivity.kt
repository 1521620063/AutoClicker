package com.example.autoclicker

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.*
import com.example.autoclicker.core.IntervalValidator
import com.example.autoclicker.service.AutoClickAccessibilityService
import com.example.autoclicker.service.ServiceStatus
import com.example.autoclicker.settings.SettingsRepository

class MainActivity : Activity() {
    private val ink = Color.rgb(28, 46, 40)
    private val muted = Color.rgb(101, 118, 109)
    private val green = Color.rgb(23, 106, 87)
    private val cream = Color.rgb(243, 245, 239)
    private lateinit var interval: EditText
    private lateinit var status: TextView
    private lateinit var detail: TextView
    private lateinit var save: Button
    private lateinit var display: Button
    private lateinit var stop: Button
    private lateinit var repository: SettingsRepository
    private val listener: () -> Unit = { renderState() }
    private var counter = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = SettingsRepository(this)
        counter = savedInstanceState?.getInt("counter", 0) ?: 0
        val scroll = ScrollView(this).apply { setBackgroundColor(cream); isFillViewport = true }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(30), dp(24), dp(32)) }
        scroll.addView(content)
        if (Build.VERSION.SDK_INT >= 35) {
            scroll.setOnApplyWindowInsetsListener { v, insets ->
                val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                insets
            }
        }
        setContentView(scroll)
        content.addView(text("轻点 / AUTO CLICKER", 12f, muted).apply { letterSpacing = 0.10f })
        content.addView(text("重复的事，\n交给轻点。", 32f, ink, bold = true), spaced(top = 16, bottom = 8))
        content.addView(text("固定位置 · 持续点击 · 由你掌控", 14f, muted), spaced(bottom = 24))

        val stateCard = card()
        status = text("服务未开启", 18f, ink, bold = true)
        detail = text("等待开启无障碍服务", 13f, muted)
        stateCard.addView(status)
        stateCard.addView(detail, spaced(top = 8))
        val permission = button("开启 / 管理无障碍服务", primary = false)
        permission.setOnClickListener { showPermissionDisclosure() }
        stateCard.addView(permission, spaced(top = 16))
        content.addView(stateCard, spaced(bottom = 16))

        val settings = card()
        settings.addView(text("01  点击节奏", 18f, ink, bold = true))
        settings.addView(text("设置两次点击开始之间的目标间隔", 13f, muted), spaced(top = 6, bottom = 12))
        val inputRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        interval = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setSingleLine(true)
            textSize = 25f
            setTextColor(ink)
            setText(repository.intervalMs.toString())
            contentDescription = "点击间隔，单位毫秒，50 到 60000"
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = rounded(Color.rgb(241, 244, 238), 10)
        }
        inputRow.addView(interval, LinearLayout.LayoutParams(0, dp(58), 1f))
        inputRow.addView(text("毫秒", 14f, muted).apply { setPadding(dp(14), 0, 0, 0) })
        settings.addView(inputRow)
        settings.addView(text("范围 50–60000 毫秒。系统可能延迟，不保证精确频率。", 12f, muted), spaced(top = 10))
        save = button("保存间隔", primary = false)
        save.setOnClickListener { saveInterval() }
        settings.addView(save, spaced(top = 10))
        content.addView(settings, spaced(bottom = 16))

        val controls = card()
        controls.addView(text("02  定位与开始", 18f, ink, bold = true))
        controls.addView(text("显示控制器后，切换到目标应用，拖动靶心定位，再按「开始」。", 14f, muted), spaced(top = 8, bottom = 14))
        display = button("显示悬浮控制器", primary = true)
        display.setOnClickListener {
            if (ServiceStatus.state.running) { toast("请先停止点击"); return@setOnClickListener }
            val service = AutoClickAccessibilityService.instance
            if (service == null) showPermissionDisclosure()
            else if (saveInterval(showToast = false)) service.showController()
        }
        controls.addView(display)
        stop = button("停止并关闭控制器", primary = false)
        stop.setOnClickListener { AutoClickAccessibilityService.instance?.closeController() }
        controls.addView(stop, spaced(top = 6))
        content.addView(controls, spaced(bottom = 16))

        val help = card()
        help.addView(text("随时停下，放心使用", 17f, ink, bold = true))
        help.addView(text("• 悬浮条可随时停止或关闭。\n• 点击时靶心隐藏，不遮挡目标。\n• 移动控制条、锁屏或旋转都会停止。\n• 重新打开后，需要重新定位。\n• 不读取屏幕文字，不截图，不联网。", 13f, muted), spaced(top = 10))
        val demo = button("测试点击：$counter 次", primary = false)
        demo.setOnClickListener { counter++; demo.text = "测试点击：$counter 次" }
        demo.contentDescription = "测试点击计数按钮"
        help.addView(demo, spaced(top = 16))
        help.addView(text("可将靶心放到上面的按钮，检查点击与停止是否正常。", 12f, muted), spaced(top = 6))
        content.addView(help)
        content.addView(text("只在你明确允许的场景使用自动点击。", 12f, muted).apply { gravity = Gravity.CENTER }, spaced(top = 22))
    }

    override fun onStart() { super.onStart(); ServiceStatus.subscribe(listener) }
    override fun onStop() { ServiceStatus.unsubscribe(listener); super.onStop() }
    override fun onSaveInstanceState(outState: Bundle) { outState.putInt("counter", counter); super.onSaveInstanceState(outState) }

    private fun renderState() {
        val state = ServiceStatus.state
        status.text = when { state.running -> "● 正在持续点击"; state.connected -> "● 服务已就绪"; else -> "○ 服务未开启" }
        status.setTextColor(if (state.connected) green else ink)
        detail.text = ServiceStatus.message
        interval.isEnabled = state.canEditSettings()
        save.isEnabled = state.canEditSettings()
        display.isEnabled = !state.running
        stop.isEnabled = state.connected
    }
    private fun saveInterval(showToast: Boolean = true): Boolean {
        if (!ServiceStatus.state.canEditSettings()) { toast("点击运行中，请先停止再调整间隔"); return false }
        val value = IntervalValidator.parse(interval.text.toString())
        if (value == null) { interval.error = "请输入 50–60000 的整数毫秒"; interval.requestFocus(); return false }
        repository.intervalMs = value
        interval.error = null
        if (showToast) toast("已保存：${value} 毫秒")
        return true
    }
    private fun showPermissionDisclosure() {
        AlertDialog.Builder(this)
            .setTitle("无障碍服务用途说明")
            .setMessage(getString(R.string.service_description) + "\n\n只有你点击悬浮条的「开始」才会执行。开启后，请返回本应用。若系统提示“受限设置”，仅在确认 APK 来源可信时，按手机系统指引允许相关设置。")
            .setNegativeButton("取消", null)
            .setPositiveButton("了解，前往设置") { _, _ ->
                try { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                catch (_: RuntimeException) { toast("无法打开设置，请手动进入系统无障碍设置") }
            }.show()
    }
    private fun card() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(20), dp(20), dp(20)); background = rounded(Color.WHITE, 20) }
    private fun text(value: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color); setLineSpacing(dp(4).toFloat(), 1f)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }
    private fun button(value: String, primary: Boolean) = Button(this).apply {
        text = value; isAllCaps = false; textSize = 14f
        minHeight = dp(52); minimumHeight = dp(52)
        setTextColor(if (primary) Color.WHITE else green)
        backgroundTintList = android.content.res.ColorStateList.valueOf(if (primary) green else Color.rgb(237, 243, 232))
    }
    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }
    private fun spaced(top: Int = 0, bottom: Int = 0) = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(top); bottomMargin = dp(bottom) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()
    private fun toast(value: String) = Toast.makeText(this, value, Toast.LENGTH_SHORT).show()
}
