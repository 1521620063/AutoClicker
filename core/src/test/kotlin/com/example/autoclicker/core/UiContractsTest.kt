package com.example.autoclicker.core

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** Source structure contracts only; not a substitute for device UI testing. */
class UiContractsTest {
    private fun source(path: String): String {
        val root = File(requireNotNull(System.getProperty("uiContracts.root")))
        val file = File(root, "app/src/main/java/com/example/autoclicker/$path")
        assertTrue("Missing UI source: $path", file.isFile)
        return file.readText(Charsets.UTF_8)
    }

    private fun assertContains(source: String, vararg tokens: String) {
        tokens.forEach { token ->
            assertTrue("Missing UI contract token: $token", source.contains(token))
        }
    }

    @Test fun sharedStyleHasInteractiveAndDisabledStates() {
        assertContains(source("ui/UiStyle.kt"), "RippleDrawable", "state_enabled", "state_selected")
    }

    @Test fun settingsAreGroupedAndFrequencyIsSwitch() {
        assertContains(source("MainActivity.kt"), "Switch", "启动与停止", "统计显示", "TextWatcher", "limitLabel.visibility")
    }

    @Test fun overlayKeepsFixedStatisticsAndUsesSharedStyle() {
        assertContains(source("overlay/OverlayController.kt"), "dp(56)", "UiStyle", "onPanelDrag")
    }
}
