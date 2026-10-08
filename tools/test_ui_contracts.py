"""Source contracts for the native UI; not a substitute for device rendering."""
from pathlib import Path
import unittest
ROOT = Path(__file__).resolve().parents[1]
class UiContracts(unittest.TestCase):
    def test_shared_style_has_interactive_and_disabled_states(self):
        path = ROOT / 'app/src/main/java/com/example/autoclicker/ui/UiStyle.kt'
        self.assertTrue(path.exists(), 'shared visual system missing')
        source = path.read_text(encoding='utf-8')
        for token in ['RippleDrawable', 'state_enabled', 'state_selected']:
            self.assertIn(token, source)
    def test_settings_are_grouped_and_frequency_is_switch(self):
        source = (ROOT / 'app/src/main/java/com/example/autoclicker/MainActivity.kt').read_text(encoding='utf-8')
        for token in ['Switch', '启动与停止', '统计显示', 'TextWatcher', 'limitLabel.visibility']:
            self.assertIn(token, source)
    def test_overlay_keeps_fixed_statistics_and_uses_shared_style(self):
        source = (ROOT / 'app/src/main/java/com/example/autoclicker/overlay/OverlayController.kt').read_text(encoding='utf-8')
        self.assertIn('dp(56)', source)
        self.assertIn('UiStyle', source)
        self.assertIn('onPanelDrag', source)
if __name__ == '__main__': unittest.main()
