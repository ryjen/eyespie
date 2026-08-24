# Eyespie Repository Guidelines

## Architectural invariant

Eyespie is a backendless-first Kotlin Multiplatform game. Core gameplay must build and function without a hosted account, backend configuration, or network connection.

Cloud and peer networking are optional capabilities behind domain/application interfaces. Do not make Supabase, Firebase, Appwrite, PocketBase, or another hosted provider a prerequisite for core behavior.

## Package ownership

Within `eyespie/src/commonMain/kotlin/com/micrantha/eyespie`:

- `core` — domain kernel, value types, matching contracts and invariants;
- `game` — local application/game orchestration;
- `persistence` — SQLDelight-backed data implementations and mapping/codecs;
- `features` — presentation features and their MVI state machines;
- `presentation` — shared presentation mapping/resources;
- `app` — application composition and navigation.

Platform source sets own platform entry points and device integrations. `calibration/`, `models/`, `model-pack/`, and `iosApp/MediaPipePodspecs` retain model provenance/packaging infrastructure.

Prefer package ownership as the layer signal; add `Data*`, `Domain*`, or `Presentation*` prefixes only where imports would otherwise be ambiguous. Do not use `Port`/`Adapter` as default architectural suffixes. Reserve `Adapter` for genuine external API/type translation.

## Dependency rules

- Reducers are pure synchronous deterministic `(State, Intent) -> State` functions.
- Interactors orchestrate capability calls and effects; reducers do not perform I/O, launch coroutines, navigate, or resolve resources.
- Features depend on narrow capability interfaces and domain/application results, not SQLDelight rows, platform implementations, app navigation, or other features.
- Data/persistence representations stay below the data boundary.
- Presentation models do not flow downward into runtime/data contracts.
- Non-trivial data -> domain and domain -> presentation translations use explicit testable mappers.
- Transient success/minor recoverable feedback uses typed one-shot effects and the shared snackbar host; persistent actionable conditions remain state.
- Voyager owns the app back stack. Feature outputs remain semantic and never expose Voyager types.
- A feature must not import `com.micrantha.eyespie.app...` or another feature package. `scripts/verify_feature_boundaries.py` enforces this subset in CI.

Use narrow interfaces for replaceable capabilities such as identity persistence, game persistence/query, embedding, import/export, sharing, and optional transports. Consumers should depend on the smallest capability they use.

## Product rules

- Local state is authoritative for offline play.
- Image embeddings and similarity matching run on-device.
- A hosted account is never required to create or play a local game.
- Portable games may contain target embeddings; document the anti-cheat tradeoff rather than pretending device-local secrets are inaccessible to the device owner.
- Stronger multiplayer authority belongs in an optional host-authoritative transport.
- Optional cloud/network implementations must remain removable without changing core domain entities.

## Preservation

The pre-reboot application is preserved at `archive/pre-backendless-reboot-2026-08-15` from commit `50091a631d971c520e48884cfbd15cf15dd7251b`.

Do not copy old backend, DI, or feature-graph assumptions back into the reboot merely because code exists on that branch.

Useful shared abstractions may be brought in deliberately from `hackelia-micrantha/bluebell` and `bluebell-community`; do not restore the previous vendored framework wholesale or extract speculative abstractions before Eyespie proves them locally.

## Build and test

Use the repository-level `mise` tasks as the canonical local and CI entry points:

```bash
mise install
mise run ci
```

Narrow targets are also available as `mise run test` and `mise run build`. These tasks wrap Eyespie's existing Gradle targets; Gradle and the KMP module graph remain the build authority rather than Morifolium.

Reducer/interactor behavior is covered by common/JVM tests. Pure Compose feature-screen interactions run separately with `mise run screen-test` against a connected Android device or emulator; pull-request CI executes that screen suite on a GitHub-hosted emulator with camera hardware present, CAMERA permission ungranted, and enlarged system text. This keeps CameraX/MediaPipe inactive while exercising requestable permission state and long-copy/scroll behavior. Physical MediaPipe calibration remains a separate evidence boundary and is intentionally excluded from the hosted presentation suite.

Apple/MediaPipe integration is validated independently by `.github/workflows/ios-mediapipe.yml`.

## Changes

Use conventional commits. Keep slices small and attributable. Prefer behavior-preserving decomposition/package moves before semantic changes. Preserve the backendless create/import/play/match path and all required CI gates.
