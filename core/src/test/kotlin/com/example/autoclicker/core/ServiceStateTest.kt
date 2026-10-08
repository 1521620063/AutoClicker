package com.example.autoclicker.core
import org.junit.Assert.*
import org.junit.Test
class ServiceStateTest {
 @Test fun disconnectedCannotStart() { val s=ServiceState(); assertFalse(s.canStart(true,false,100)) }
 @Test fun connectedUnlockedScreenCanStart() { val s=ServiceState();s.connect();assertTrue(s.canStart(true,false,100)) }
 @Test fun lockOrDarkScreenOrInvalidIntervalCannotStart() { val s=ServiceState();s.connect();assertFalse(s.canStart(true,true,100));assertFalse(s.canStart(false,false,100));assertFalse(s.canStart(true,false,9));assertFalse(s.canStart(true,false,60001)) }
 @Test fun disconnectAndReconnectNeverResumeExecution() { val s=ServiceState();s.connect();s.setRunning(true);assertTrue(s.running);s.disconnect();assertFalse(s.running);assertFalse(s.connected);s.connect();assertFalse(s.running) }
 @Test fun runningDisablesSettingsAndNewStart() { val s=ServiceState();s.connect();s.setRunning(true);assertFalse(s.canEditSettings());assertFalse(s.canStart(true,false,100));s.setRunning(false);assertTrue(s.canEditSettings()) }
 @Test fun cannotRunWhileDisconnected() { val s=ServiceState();s.setRunning(true);assertFalse(s.running) }
}
