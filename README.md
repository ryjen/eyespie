# Eyespie

Eyespie is an offline-first travel spy game built with Kotlin Multiplatform and on-device computer vision.

## Backendless core

Core gameplay requires **no hosted backend and no hosted account**. Game authority, clues, embeddings, matching, and progress are local to the device. Cloud and peer networking are optional future adapters for capabilities that actually need them.

The former Supabase-based application remains recoverable from:

- branch: `archive/pre-backendless-reboot-2026-08-15`
- commit: `50091a631d971c520e48884cfbd15cf15dd7251b`

The reboot and closed-alpha release are tracked by issues #156 and #90. The backendless/local-authoritative epoch is called **Wayfinder**.

## Closed-alpha implementation status

The Wayfinder closed-alpha implementation is integrated on `main`:

- Compose Android/iOS application entry points and route-scoped feature MVI;
- SQLDelight local-authoritative Game, Thing, clue-authority, and progress persistence;
- platform-backed P-256 local signing identity with a stable public-key-derived `PlayerId`;
- bounded Android CameraX and iOS AVFoundation capture behind a provider-neutral `ImageCapture` boundary;
- Android/iOS MediaPipe ImageEmbedder adapters using the reviewed 1024-float model contract;
- manual clue authoring with capture-first review and creator-only expected-answer authority excluded from playable projections;
- complete local create → clue → guess → match → persisted-progress flow;
- deterministic signed/versioned `.eyespie` v1 export/import with fail-closed hostile-input validation and verified preview before persistence;
- separate native **Share game** and **Save game file** flows on Android/iOS;
- external `.eyespie` document ingress and the narrow `eyespie://game/<GameId>` route for already-accepted local games;
- whole-app Wayfinder visual treatment plus deterministic Android screenshot/golden references;
- bounded privacy-safe operational diagnostics with exact release/runtime provenance and explicit local export;
- deterministic release-candidate identity and physical-device evidence collectors;
- protected manual signed internal-distribution workflow for Play Internal Testing and TestFlight;
- no Supabase/Auth/PostgREST/GraphQL/realtime/storage runtime and no Android core `INTERNET` permission.

Operational diagnostics are deliberately separate from product analytics: the current diagnostic path is bounded, local/process-lifetime, explicit-export only, and has no remote collector or gameplay authority.

### Release readiness

Implementation and simulator/CI qualification are **not the same as physical release proof**. The current release path is qualification of one exact post-Wayfinder/post-telemetry candidate, not feature expansion.

The latest exact-head protected Android `publish=false` candidate build has successfully validated the requested source SHA, staged and verified the pinned runtime model, produced signed APK/AAB artifacts, and validated the signed Android artifact. That does **not** establish iOS signed-distribution or physical-device acceptance by itself; #90 remains the canonical candidate/evidence tracker.

Closed-alpha readiness still requires the evidence owned by:

- #239 — representative physical Android runtime initialization/readiness;
- #295 / #297 — physical Android confirmation of the authoring-orientation and visible-matching-progress fixes on the exact candidate;
- #91 / #190 — representative physical Android/iPhone embedding parity and repeated-inference evidence;
- #125 — exact-candidate MediaPipe/runtime network and telemetry observation;
- #92 / #299 — complete Android ↔ iOS create/share/open/import/deep-link/guess proof, including native handoff behavior;
- #304 — physical verification that exported operational diagnostics are useful, bounded, private, and offline-safe;
- #18 — final backendless privacy/security sign-off;
- #93 — protected signed distribution, install/upgrade/relaunch, and recovery evidence, including the iOS signed path;
- #94 — final public/store/privacy capability-claim reconciliation.

Do not describe these physical/release gates as complete until their evidence is recorded against the exact accepted candidate. Candidate-changing source changes must re-bind affected evidence rather than carrying it forward implicitly.

## Build and verification

The canonical local/CI Android gate is:

```bash
mise run ci
```

That verifies release-candidate identity, Python evidence tooling, Android unit tests, the debug application, and the instrumentation-test APK.

Ordinary build/CI paths intentionally remain network-independent and do not provision the external image-embedding model. To provision the pinned model, verify it, and install a runnable Android debug application on a connected device/emulator, use:

```bash
mise run android-runtime
```

To verify an already-staged Android runtime model **without network access or repair**, use:

```bash
mise run android-runtime-verify
```

The Gradle application project is named `:app` even though its source directory is `eyespie/`. The direct operations are therefore:

```bash
./gradlew :app:stageAndroidImageEmbedderModel
./gradlew :app:verifyAndroidRuntime
./gradlew :app:installDebug
```

See [`docs/development/android-runtime.md`](docs/development/android-runtime.md) for the stage → verify → install contract, failure diagnosis, and shared-automation boundary.

The iOS integration workflow additionally resolves the project-specific MediaPipe CocoaPods graph, compiles the Kotlin/Native simulator target, and builds the real unsigned Xcode simulator application.

Physical/release runbooks live under [`docs/release/`](docs/release/), including candidate identity, Android/iOS embedding calibration, network observation, cross-device smoke testing, and protected internal distribution.

Wayfinder presentation contracts and review evidence live under [`docs/design/`](docs/design/).

See [ARCHITECTURE.md](ARCHITECTURE.md) for the current architecture and trust boundaries.
