import importlib.util
import pathlib
import unittest


SCRIPT = pathlib.Path(__file__).resolve().parents[1] / "verify_feature_boundaries.py"
SPEC = importlib.util.spec_from_file_location("verify_feature_boundaries", SCRIPT)
assert SPEC is not None and SPEC.loader is not None
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class FeatureBoundaryVerifierTest(unittest.TestCase):
    def test_parses_multiline_state_and_dispatch(self):
        signature = """
            state: HomeState,
            dispatch: (HomeIntent) -> Unit,
        """
        self.assertEqual(
            ["state: HomeState", "dispatch: (HomeIntent) -> Unit"],
            MODULE.split_top_level_parameters(signature),
        )

    def test_parses_one_line_signature_independent_of_formatting(self):
        signature = "state: HomeState, dispatch: (HomeIntent) -> Unit"
        self.assertEqual(
            ["state: HomeState", "dispatch: (HomeIntent) -> Unit"],
            MODULE.split_top_level_parameters(signature),
        )

    def test_does_not_split_generic_type_arguments(self):
        signature = "state: Pair<First, Second>, dispatch: (Intent) -> Unit"
        self.assertEqual(
            ["state: Pair<First, Second>", "dispatch: (Intent) -> Unit"],
            MODULE.split_top_level_parameters(signature),
        )


if __name__ == "__main__":
    unittest.main()
