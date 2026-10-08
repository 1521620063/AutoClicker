package com.example.autoclicker.core
import org.junit.Assert.*
import org.junit.Test
class OverlayGeometryTest {
 private val bounds = ScreenBounds(0f,24f,1080f,1920f)
 @Test fun targetCenterCanReachAllUsableEdges() {
     assertEquals(ClickPoint(-28f,-4f),OverlayGeometry.windowOrigin(ClickPoint(-100f,-100f),56f,56f,bounds,true))
     assertEquals(ClickPoint(1051f,1891f),OverlayGeometry.windowOrigin(ClickPoint(99999f,99999f),56f,56f,bounds,true))
 }
 @Test fun panelStaysFullyVisible() {
     assertEquals(ClickPoint(0f,24f),OverlayGeometry.windowOrigin(ClickPoint(-100f,-100f),236f,100f,bounds,false))
     assertEquals(ClickPoint(844f,1820f),OverlayGeometry.windowOrigin(ClickPoint(99999f,99999f),236f,100f,bounds,false))
 }
 @Test fun clampsAllEdges() { assertEquals(ClickPoint(0f,24f),OverlayGeometry.clamp(ClickPoint(-200f,-1f),bounds)); assertEquals(ClickPoint(1079f,1919f),OverlayGeometry.clamp(ClickPoint(99999f,99999f),bounds)) }
 @Test fun preservesRealScreenCoordinates() { assertEquals(ClickPoint(100f,200f),OverlayGeometry.clamp(ClickPoint(100f,200f),bounds)) }
 @Test fun overlapIncludesEdgesAndExcludesOutside() { val p=ScreenBounds(100f,200f,300f,400f); assertTrue(OverlayGeometry.overlapsTarget(ClickPoint(100f,200f),p)); assertTrue(OverlayGeometry.overlapsTarget(ClickPoint(250f,300f),p)); assertFalse(OverlayGeometry.overlapsTarget(ClickPoint(99f,300f),p)); assertFalse(OverlayGeometry.overlapsTarget(ClickPoint(301f,300f),p)) }
}
class WindowGroupTest {
 private class Window(val fail: Boolean=false) : OverlayWindow { var attached=false; var removed=0; override fun attach(){ if(fail) error("window failure"); attached=true }; override fun detach(){ attached=false;removed++ } }
 @Test fun partialCreationRollsBack() { val a=Window(); val b=Window(true); try { WindowGroup(listOf(a,b)).show(); fail("expected failure") } catch(_: IllegalStateException){}; assertFalse(a.attached);assertEquals(1,a.removed) }
 @Test fun repeatedShowAndCloseAreSafe() { val a=Window(); val group=WindowGroup(listOf(a)); group.show(); group.show(); assertTrue(a.attached); group.close(); group.close(); assertFalse(a.attached); assertEquals(1,a.removed) }
}
