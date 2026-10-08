package com.example.autoclicker.core
import org.junit.Assert.*
import org.junit.Test
class SafetyMonitorTest {
 private class Clock: SchedulerClock {
  var now=0L
  val jobs=mutableListOf<Pair<Long,()->Unit>>()
  override fun nowMs()=now
  override fun postDelayed(delayMs: Long, action:()->Unit) { jobs.add((now+delayMs) to action) }
  override fun cancelPending(){jobs.clear()}
  fun advance(ms:Long){val end=now+ms;while(true){val next=jobs.minByOrNull{it.first}?:break;if(next.first>end)break;jobs.remove(next);now=next.first;next.second()};now=end}
 }
 @Test fun lockBetweenLongIntervalClicksLatchesStoppedAfterUnlock() {
  val clickClock=Clock();val safetyClock=Clock();var count=0;var ready=true
  val scheduler=ClickScheduler(clickClock,object:GestureDriver {override fun dispatch(point:ClickPoint,durationMs:Long,complete:(Boolean)->Unit):Boolean{count++;complete(true);return true}})
  val monitor=SafetyMonitor(safetyClock,{ready},{scheduler.stop()})
  scheduler.start(ClickPoint(1f,1f),60000);clickClock.advance(0);monitor.start();assertEquals(1,count)
  ready=false;safetyClock.advance(100);assertFalse(scheduler.isRunning)
  ready=true;safetyClock.advance(1000);clickClock.advance(60000);assertEquals(1,count)
  scheduler.start(ClickPoint(1f,1f),60000);clickClock.advance(0);assertEquals(2,count)
 }
 @Test fun immediateUnsafeStopsOnce() {val c=Clock();var stopped=0;val m=SafetyMonitor(c,{false},{stopped++});m.start();m.checkNow();c.advance(1000);assertEquals(1,stopped)}
 @Test fun stopCancelsMonitoring() {val c=Clock();var checks=0;val m=SafetyMonitor(c,{checks++;true},{});m.start();assertEquals(1,checks);m.stop();c.advance(10000);assertEquals(1,checks)}
 @Test fun eventChecksLatchUnsafeWithoutWaitingForPoll() {val c=Clock();var ready=true;var stopped=0;val m=SafetyMonitor(c,{ready},{stopped++});m.start();ready=false;assertFalse(m.checkNow());ready=true;c.advance(1000);assertEquals(1,stopped);assertTrue(c.jobs.isEmpty())}
}
