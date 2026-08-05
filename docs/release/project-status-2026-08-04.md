# Eyespie Project Status — August 4, 2026

## Overall assessment

**Requires focused completion work.**

Eyespie has strong build, security, and packaging foundations, but the product release path is constrained by incomplete iOS MediaPipe integration and an unproven cross-platform game loop.

## Status summary

| Area | Status | Evidence and implication |
|---|---|---|
| Android CI and packaging | Implemented | Validation, tests, AAB construction, and model-pack topology are established |
| Workflow security and SBOM | Implemented with active integration failure | The current custom-pod branch fails during locked CocoaPods preparation |
| Custom MediaPipe iOS assets | Published prerelease | Four namespaced podspecs and framework archives exist with checksums and provenance |
| Eyespie iOS MediaPipe integration | Blocked | Pod installation fails before Kotlin compilation and the Xcode app build |
| Android image embedding | Implemented | MediaPipe ImageEmbedder is used with a bundled model |
| iOS image embedding | Prototype-only | The platform provider still uses a deterministic byte-bucketing implementation |
| Camera lifecycle | Partially repaired | Android closes ImageProxy exactly once, but format-correct bitmap conversion remains unresolved |
| Clue generation | Prototype-only | Free-form three-line parsing lacks schema validation and controlled repair |
| Optional large-model delivery | In progress | Architecture and delivery seams exist, but store and physical-device proof are incomplete |
| Release engineering | Incomplete | No application tags or GitHub releases exist; store candidate and rollback proof are pending |
| Documentation | Drifted | README and public capability claims exceed or conflict with verified implementation |

## Immediate blockers

1. Repair deterministic installation of the custom MediaPipe pods.
2. Make the CocoaPods SBOM path consume and verify the same graph.
3. Implement the actual iOS MediaPipe Vision image embedder.
4. Define and test one cross-platform embedding representation.
5. Correct and soak-test camera image conversion and ownership.
6. Replace brittle clue parsing with structured output or a deterministic fallback.
7. Prove the full two-player game loop on physical Android and iOS devices.
8. Establish signed internal distribution, versioning, and rollback.

## Scope recommendation

Target a closed alpha by September 30, 2026 and a public beta by October 31, 2026.

For the first release, keep the approximately 584 MB GenAI model optional and off the critical path. Treat MediaPipe Vision and deterministic gameplay logic as the release baseline. iOS GenAI image input is unsupported and must remain disabled and accurately documented.

## Backlog hygiene

Several older issues describe defects that have since been partially repaired or superseded by later implementation. Issue state alone is not sufficient evidence of current status.

During release work:

- update existing issues with current evidence rather than opening duplicates;
- close superseded CI and integration issues only after their replacement is green on main;
- split broad parent issues into release-critical and deferred slices;
- ensure every release blocker has a concrete acceptance test and owning PR.

## Next review point

Re-run the comprehensive project-status review after:

- the custom MediaPipe integration PR merges;
- iOS Vision embedding is implemented;
- the first signed TestFlight and Play Internal Testing builds are available.
