# Eyespie

Eyespie is an offline-first travel spy game built with Kotlin Multiplatform and on-device computer vision.

## Backendless core

Core gameplay requires **no hosted backend and no hosted account**. Game authority, clues, embeddings, matching, and progress are local to the device. Cloud and peer networking are optional future adapters for capabilities that actually need them.

The former Supabase-based application remains recoverable from:

- branch: `archive/pre-backendless-reboot-2026-08-15`
- commit: `50091a631d971c520e48884cfbd15cf15dd7251b`

The reboot and closed-alpha release are tracked by issues #156 and #90.

## Closed-alpha implementation status

The backendless alpha implementation is now integrated on `main`:

- Compose Android/iOS application entry points;
- SQLDelight local-authoritative Game, Thing, clue-authority, and progress persistence;
- platform-backed P-256 local signing identity with a stable public-key-derived `PlayerId`;
- bounded Android CameraX and iOS AVFoundation capture behind a provider-neutral `ImageCapture` boundary;
- Android/iOS MediaPipe ImageEmbedder adapters using the reviewed 1024-float model contract;
- manual clue authoring with creator-only expected-answer authority excluded from playable projections;
- complete local create → clue → guess → match → persisted-progress flow;
- deterministic signed/versioned `.eyespie` v1 export/import with fail-closed hostile-input validation;
- scoped Android and iOS system document transfer without broad storage permission;
- deterministic release-candidate identity and physical-device evidence collectors;
- protected manual signed internal-distribution workflow for Play Internal Testing and TestFlight;
- no Supabase/Auth/PostgREST/GraphQL/realtime/storage runtime and no Android core `INTERNET` permission.

### Release readiness

Implementation and simulator/CI qualification are **not the same as physical release proof**. Closed-alpha readiness still requires the release evidence owned by:

- #91 / #190 — representative physical Android/iPhone embedding parity and repeated-inference evidence;
- #125 — exact candidate MediaPipe/runtime network and telemetry observation;
- #92 — complete Android → iOS and iOS → Android create/share/import/guess proof;
- #18 — final backendless privacy/security sign-off;
- #93 — protected signed distribution, install/upgrade/relaunch, and recovery evidence;
- #94 — final public/store/privacy capability-claim reconciliation.

Do not describe these physical/release gates as complete until their evidence is recorded.

## Build and verification

The canonical local/CI Android gate is:

```bash
mise run ci
```

That verifies release-candidate identity, Python evidence tooling, Android unit tests, the debug application, and the instrumentation-test APK.

Ordinary build/CI paths intentionally remain network-independent and do not provision the external image-embedding model. To provision the pinned model and install a runnable Android debug application on a connected device/emulator, use:

```bash
mise run android-runtime
```

The Gradle application project is named `:app` even though its source directory is `eyespie/`; the direct provisioning task is therefore `./gradlew :app:stageAndroidImageEmbedderModel`.

See [`docs/development/android-runtime.md`](docs/development/android-runtime.md) for the runtime provisioning contract, failure diagnosis, and shared-automation boundary.

The iOS integration workflow additionally resolves the project-specific MediaPipe CocoaPods graph, compiles the Kotlin/Native simulator target, and builds the real unsigned Xcode simulator application.

Physical/release runbooks live under [`docs/release/`](docs/release/), including candidate identity, Android/iOS embedding calibration, network observation, cross-device smoke testing, and protected internal distribution.

See [ARCHITECTURE.md](ARCHITECTURE.md) for the current architecture and trust boundaries.
