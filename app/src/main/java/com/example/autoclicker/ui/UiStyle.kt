package com.example.autoclicker.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.widget.Button

/** Shared light visual system. Each view receives a fresh drawable instance. */
object UiStyle {
    val ink = Color.rgb(25, 46, 43)
    val muted = Color.rgb(99, 117, 113)
    val green = Color.rgb(19, 112, 91)
    val canvas = Color.rgb(245, 248, 247)
    val tint = Color.rgb(233, 244, 239)
    val border = Color.rgb(220, 230, 225)
    val danger = Color.rgb(162, 57, 57)
    fun dp(context: Context, value: Int) = (value * context.resources.displayMetrics.density + .5f).toInt()
    fun surface(context: Context, color: Int, radius: Int = 16, stroke: Boolean = false) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(context, radius).toFloat()
        if (stroke) setStroke(dp(context, 1), border)
    }
    fun styleButton(button: Button, primary: Boolean = false, destructive: Boolean = false, chip: Boolean = false) {
        val context = button.context
        val accent = if (destructive) danger else green
        button.isAllCaps = false
        button.textSize = if (chip) 12f else 14f
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL))
        button.minWidth = 0; button.minimumWidth = 0
        button.minHeight = dp(context, 48); button.minimumHeight = dp(context, 48)
        button.setPadding(dp(context, if (chip) 12 else 16), dp(context, 8), dp(context, if (chip) 12 else 16), dp(context, 8))
        button.backgroundTintList = null
        val states = arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf(android.R.attr.state_selected), intArrayOf())
        button.setTextColor(ColorStateList(states, intArrayOf(muted, Color.WHITE, if (primary) Color.WHITE else accent)))
        val background = StateListDrawable().apply {
            addState(states[0], surface(context, border, if (chip) 12 else 16))
            addState(states[1], surface(context, accent, if (chip) 12 else 16))
            addState(states[2], surface(context, if (primary) accent else if (destructive) Color.rgb(251, 239, 238) else tint, if (chip) 12 else 16))
        }
        button.background = RippleDrawable(ColorStateList.valueOf(Color.argb(35, 19, 112, 91)), background, surface(context, Color.WHITE))
        button.stateListAnimator = null
    }
}
