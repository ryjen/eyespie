"""Pure regression tests for Eyespie emulator launcher-ANR recovery."""

import unittest
import xml.etree.ElementTree as ET

from scripts.verify_installed_onboarding import recover_launcher_anr


def tree(*labels):
    root = ET.Element("hierarchy")
    for label in labels:
        ET.SubElement(root, "node", {"text": label})
    return root


class LauncherAnrRecoveryTest(unittest.TestCase):
    def setUp(self):
        self.taps = []

    def tap(self, node, description):
        self.taps.append((node.get("text"), description))

    def test_no_dialog_is_not_recovered(self):
        self.assertFalse(
            recover_launcher_anr(tree("Play locally", "Close app"),
                                 already_recovered=False, tap=self.tap)
        )
        self.assertEqual([], self.taps)

    def test_exact_launcher_dialog_is_dismissed_once(self):
        self.assertTrue(
            recover_launcher_anr(tree("Pixel Launcher isn't responding", "Close app"),
                                 already_recovered=False, tap=self.tap)
        )
        self.assertEqual([("Close app", "Pixel Launcher Close app")], self.taps)

    def test_second_launcher_anr_fails_closed(self):
        with self.assertRaisesRegex(AssertionError, "Unrecoverable Pixel Launcher ANR"):
            recover_launcher_anr(tree("Pixel Launcher isn't responding", "Close app"),
                                 already_recovered=True, tap=self.tap)
        self.assertEqual([], self.taps)

    def test_missing_close_button_fails_closed(self):
        with self.assertRaisesRegex(AssertionError, "Unrecoverable Pixel Launcher ANR"):
            recover_launcher_anr(tree("Pixel Launcher isn't responding"),
                                 already_recovered=False, tap=self.tap)
        self.assertEqual([], self.taps)

    def test_other_app_anr_does_not_trigger_recovery(self):
        self.assertFalse(
            recover_launcher_anr(tree("Eyespie isn't responding", "Close app"),
                                 already_recovered=False, tap=self.tap)
        )
        self.assertEqual([], self.taps)


if __name__ == "__main__":
    unittest.main()
