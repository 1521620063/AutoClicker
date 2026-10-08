package com.example.autoclicker.core

object IntervalValidator {
    const val DEFAULT = 100L
    const val MIN = 50L
    const val MAX = 60000L
    fun parse(text: String): Long? {
        if (text.isEmpty() || text.length > 5 || text.any { it !in '0'..'9' }) return null
        return text.toLongOrNull()?.takeIf { it in MIN..MAX }
    }
    fun stored(value: Long): Long = value.takeIf { it in MIN..MAX } ?: DEFAULT
}
