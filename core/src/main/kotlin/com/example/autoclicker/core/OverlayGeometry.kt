package com.example.autoclicker.core

data class ScreenBounds(val left: Float, val top: Float, val right: Float, val bottom: Float)
object OverlayGeometry {
    fun windowOrigin(desired: ClickPoint, width: Float, height: Float, bounds: ScreenBounds, centerConstrained: Boolean): ClickPoint {
        if (centerConstrained) {
            val center = clamp(ClickPoint(desired.x + width / 2, desired.y + height / 2), bounds)
            return ClickPoint(center.x - width / 2, center.y - height / 2)
        }
        return ClickPoint(
            desired.x.coerceIn(bounds.left, (bounds.right - width).coerceAtLeast(bounds.left)),
            desired.y.coerceIn(bounds.top, (bounds.bottom - height).coerceAtLeast(bounds.top))
        )
    }

    fun clamp(point: ClickPoint, bounds: ScreenBounds): ClickPoint = ClickPoint(
        point.x.coerceIn(bounds.left, (bounds.right - 1).coerceAtLeast(bounds.left)),
        point.y.coerceIn(bounds.top, (bounds.bottom - 1).coerceAtLeast(bounds.top))
    )
    fun overlapsTarget(point: ClickPoint, panel: ScreenBounds): Boolean =
        point.x >= panel.left && point.x <= panel.right && point.y >= panel.top && point.y <= panel.bottom
}
interface OverlayWindow { fun attach(); fun detach() }
/** Transactional ownership: partial creation must never leave the other window behind. */
class WindowGroup(private val windows: List<OverlayWindow>) {
    private val attached = mutableListOf<OverlayWindow>()
    fun show() {
        if (attached.isNotEmpty()) return
        try {
            windows.forEach { window -> attached.add(window); window.attach() }
        } catch (error: RuntimeException) {
            close()
            throw error
        }
    }
    fun close() {
        attached.asReversed().forEach { runCatching { it.detach() } }
        attached.clear()
    }
}
