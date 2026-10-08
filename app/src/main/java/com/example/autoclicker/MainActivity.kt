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
import android.text.Editable
import android.text.TextWatcher
import android.content.res.ColorStateList
import com.example.autoclicker.ui.UiStyle
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.*
import com.example.autoclicker.core.IntervalValidator
import com.example.autoclicker.core.RunOptions
import com.example.autoclicker.service.AutoClickAccessibilityService
import com.example.autoclicker.service.ServiceStatus
import com.example.autoclicker.settings.SettingsRepository

class MainActivity : Activity() {
    private val ink = UiStyle.ink
    private val muted = UiStyle.muted
    private val green = UiStyle.green
    private val cream = UiStyle.canvas
    private lateinit var interval: EditText
    private lateinit var press: EditText
    private lateinit var countdown: Spinner
    private lateinit var mode: Spinner
    private lateinit var limit: EditText
    private lateinit var showFrequency: Switch
    private lateinit var permission: Button
    private val settingViews = mutableListOf<View>()
    private val countdownValues = listOf(0,1,3,5)
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
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(24), dp(20), dp(32)) }
        scroll.addView(content)
        if (Build.VERSION.SDK_INT >= 35) {
            scroll.setOnApplyWindowInsetsListener { v, insets ->
                val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                val keyboard = insets.getInsets(WindowInsets.Type.ime())
                v.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, keyboard.bottom))
                insets
            }
        }
        setContentView(scroll)
        val brand = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        brand.addView(text("◎", 28f, green).apply {
            gravity = Gravity.CENTER; background = rounded(UiStyle.tint, 16)
            contentDescription = "轻点标志"
        }, LinearLayout.LayoutParams(dp(52), dp(52)))
        val brandText = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, 0, 0) }
        brandText.addView(text("轻点", 26f, ink, bold = true))
        brandText.addView(text("AUTO CLICKER  /  专注每一次点击", 10f, muted))
        brand.addView(brandText, LinearLayout.LayoutParams(0, -2, 1f))
        content.addView(brand, spaced(bottom = 24))

        val stateCard = card()
        status = text("服务未开启", 14f, ink, bold = true).apply {
            setPadding(dp(12), dp(8), dp(12), dp(8)); background = rounded(UiStyle.tint, 10)
        }
        detail = text("等待开启无障碍服务", 13f, muted)
        stateCard.addView(status)
        stateCard.addView(detail, spaced(top = 8))
        permission = button("开启无障碍服务", primary = false)
        permission.setOnClickListener { showPermissionDisclosure() }
        stateCard.addView(permission, spaced(top = 16))
        content.addView(stateCard, spaced(bottom = 16))

        val settings = card()
        settings.addView(text("点击节奏", 18f, ink, bold = true))
        settings.addView(text("设置两次点击开始之间的目标间隔", 13f, muted), spaced(top = 6, bottom = 12))
        val inputRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        interval = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setSingleLine(true)
            textSize = 25f
            setTextColor(ink)
            setText(repository.intervalMs.toString())
            contentDescription = "点击间隔，单位毫秒，10 到 60000"
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = UiStyle.surface(this@MainActivity, cream, 14, stroke = true)
        }
        inputRow.addView(interval, LinearLayout.LayoutParams(0, dp(58), 1f))
        inputRow.addView(text("毫秒", 14f, muted).apply { setPadding(dp(14), 0, 0, 0) })
        settings.addView(inputRow)
        settings.addView(text("范围 10–60000 毫秒。系统可能延迟，不保证精确频率。", 12f, muted), spaced(top = 10))
        val presets = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val presetScroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(presets) }
        val presetButtons = mutableListOf<Pair<Int, Button>>()
        listOf(10,20,50,100,500,1000).forEach { ms ->
            val b = button("${ms} ms", primary=false).apply { UiStyle.styleButton(this, chip = true) }
            b.setOnClickListener { interval.setText(ms.toString()) }
            presetButtons.add(ms to b)
            settingViews.add(b); presets.addView(b, LinearLayout.LayoutParams(-2, dp(48)).apply { marginEnd = dp(6) })
        }
        settings.addView(presetScroll, spaced(top=12))
        fun updatePresets() { presetButtons.forEach { (ms, b) -> b.isSelected = interval.text.toString().toLongOrNull() == ms.toLong() } }
        interval.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { updatePresets() }
            override fun afterTextChanged(s: Editable?) {}
        })
        updatePresets()
        val current = repository.options
        var section = settings
        fun number(label: String, initial: Long): EditText {
            section.addView(text(label,13f,muted),spaced(top=14, bottom=8))
            return EditText(this).apply {
                inputType=InputType.TYPE_CLASS_NUMBER; setSingleLine(true); setText(initial.toString())
                textSize=18f; setTextColor(ink); setPadding(dp(14),dp(10),dp(14),dp(10))
                background=UiStyle.surface(this@MainActivity,cream,12,stroke=true)
                contentDescription=label
                section.addView(this, spaced()); settingViews.add(this)
            }
        }
        fun selector(label: String, items: List<String>): Spinner {
            section.addView(text(label,13f,muted),spaced(top=14, bottom=8))
            return Spinner(this).apply {
                adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,items)
                backgroundTintList=ColorStateList.valueOf(green)
                setPadding(dp(8),0,dp(8),0); contentDescription=label
                section.addView(this, LinearLayout.LayoutParams(-1,dp(52))); settingViews.add(this)
            }
        }
        press=number("按下时长（1–100 毫秒，不能超过间隔）", current.pressMs)
        content.addView(settings, spaced(bottom=16))
        section=card()
        section.addView(text("启动与停止",18f,ink,bold=true))
        countdown=selector("启动倒计时",listOf("立即开始","1 秒","3 秒","5 秒"))
        countdown.setSelection(countdownValues.indexOf(current.countdownSeconds))
        mode=selector("自动停止方式",listOf("持续点击","指定完成手势次数","指定运行时长（秒）"))
        val initialMode=when { current.maxClicks>0 -> 1; current.maxDurationMs>0 -> 2; else -> 0 }
        val limitLabel=text("",13f,muted)
        section.addView(limitLabel,spaced(top=14,bottom=8))
        limit=EditText(this).apply {
            inputType=InputType.TYPE_CLASS_NUMBER; setSingleLine(true); textSize=18f; setTextColor(ink)
            setPadding(dp(14),dp(10),dp(14),dp(10)); background=UiStyle.surface(this@MainActivity,cream,12,stroke=true)
            section.addView(this); settingViews.add(this)
        }
        limit.setText(when(initialMode) {
            1 -> current.maxClicks; 2 -> current.maxDurationMs/1000; else -> 100
        }.toString())
        mode.onItemSelectedListener=object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                limit.visibility=if(position==0) View.GONE else View.VISIBLE
                limitLabel.visibility=limit.visibility
                limitLabel.text=if(position==1) "完成手势次数 · 1–1,000,000 次" else "运行时长 · 1–86,400 秒"
                limit.contentDescription=limitLabel.text
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        mode.setSelection(initialMode)
        section.addView(text("低间隔不保证实际速度；次数统计为系统完成手势，不代表目标应用响应。倒计时不计入运行时长。",12f,muted),spaced(top=10))
        content.addView(section,spaced(bottom=16))
        section=card()
        section.addView(text("统计显示",18f,ink,bold=true))
        showFrequency=Switch(this).apply {
            text="显示实时点击频率"; isChecked=current.showFrequency; setTextColor(ink)
            contentDescription="显示实时点击频率，最近一秒系统完成的手势数"
            minHeight=dp(56); textSize=14f
            thumbTintList=ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),intArrayOf(green,muted))
            trackTintList=ColorStateList.valueOf(UiStyle.border)
        }
        settingViews.add(showFrequency)
        section.addView(showFrequency,spaced(top=10))
        section.addView(text("最近 1 秒完成手势数，每 500 毫秒刷新。不是理论频率，也不代表目标应用响应次数。",12f,muted),spaced(top=6))
        save = button("保存点击配置", primary = false)
        save.setOnClickListener { saveInterval() }
        section.addView(save, spaced(top = 16))
        content.addView(section, spaced(bottom = 16))

        val controls = card()
        controls.addView(text("准备好，开始轻点", 18f, ink, bold = true))
        controls.addView(text("① 显示控制器   ② 拖动靶心定位   ③ 开始\n显示后可切换到目标应用，点击由你掌控。", 14f, muted), spaced(top = 8, bottom = 14))
        display = button("显示悬浮控制器", primary = true)
        display.setOnClickListener {
            if (ServiceStatus.state.running) { toast("请先停止点击"); return@setOnClickListener }
            val service = AutoClickAccessibilityService.instance
            if (service == null) showPermissionDisclosure()
            else if (saveInterval(showToast = false)) service.showController()
        }
        controls.addView(display)
        stop = button("停止并关闭控制器", primary = false).apply { UiStyle.styleButton(this, destructive = true) }
        stop.setOnClickListener { AutoClickAccessibilityService.instance?.closeController() }
        controls.addView(stop, spaced(top = 6))
        content.addView(controls, 2, spaced(bottom = 16))

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
        status.text = when { state.running -> "●  已启动 · 含倒计时"; state.connected -> "●  服务已就绪"; else -> "○  等待开启服务" }
        permission.text = if (state.connected) "管理无障碍服务" else "开启无障碍服务"
        status.setTextColor(if (state.connected) green else ink)
        detail.text = ServiceStatus.message
        interval.isEnabled = state.canEditSettings()
        interval.alpha = if (state.canEditSettings()) 1f else .55f
        settingViews.forEach { it.isEnabled = state.canEditSettings(); it.alpha = if (state.canEditSettings()) 1f else .55f }
        save.isEnabled = state.canEditSettings()
        display.isEnabled = !state.running
        stop.isEnabled = state.connected
    }
    private fun saveInterval(showToast: Boolean = true): Boolean {
        if (!ServiceStatus.state.canEditSettings()) { toast("点击运行中，请先停止再调整间隔"); return false }
        val value = IntervalValidator.parse(interval.text.toString())
        if (value == null) { interval.error = "请输入 10–60000 的整数毫秒"; interval.requestFocus(); return false }
        val pressValue = press.text.toString().toLongOrNull()
        if (pressValue == null || pressValue !in 1..100 || pressValue > value) {
            press.error="按下时长须为 1–100 毫秒且不超过间隔"; press.requestFocus(); return false
        }
        val selectedMode=mode.selectedItemPosition
        val amount=if(selectedMode==0) 0L else limit.text.toString().toLongOrNull()
        val upper=if(selectedMode==1) 1000000L else 86400L
        if(amount==null || (selectedMode!=0 && amount !in 1..upper)) {
            limit.error="请输入 1–$upper 的整数"; limit.requestFocus(); return false
        }
        val options=RunOptions(value,pressValue,countdownValues[countdown.selectedItemPosition],
            if(selectedMode==1) amount else 0, if(selectedMode==2) amount*1000 else 0, showFrequency.isChecked)
        if(!options.isValid()) { toast("配置无效"); return false }
        repository.options=options
        press.error=null; limit.error=null
        interval.error = null
        if (showToast) toast("点击配置已保存")
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
    private fun card() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(20), dp(20), dp(20)); background = UiStyle.surface(this@MainActivity, Color.WHITE, 20, stroke = true) }
    private fun text(value: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color); setLineSpacing(dp(4).toFloat(), 1f)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }
    private fun button(value: String, primary: Boolean) = Button(this).apply {
        text = value; UiStyle.styleButton(this, primary)
    }
    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }
    private fun spaced(top: Int = 0, bottom: Int = 0) = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(top); bottomMargin = dp(bottom) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()
    private fun toast(value: String) = Toast.makeText(this, value, Toast.LENGTH_SHORT).show()
}
