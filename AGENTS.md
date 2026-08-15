# Eyespie Repository Guidelines

## Architectural invariant

Eyespie is a backendless-first Kotlin Multiplatform game. Core gameplay must build and function without a hosted account, backend configuration, or network connection.

Cloud and peer networking are optional capabilities behind domain interfaces. Do not make Supabase, Firebase, Appwrite, PocketBase, or any other hosted provider a prerequisite for core domain behavior.

## Core boundaries

- `eyespie/src/commonMain`: portable domain, local gameplay, matching, and UI.
- `eyespie/src/androidMain`: Android platform entry points and device integrations.
- `eyespie/src/iosMain`: Apple platform entry points and device integrations.
- `calibration/`, `models/`, `model-pack/`: retained model provenance and packaging infrastructure.
- `iosApp/MediaPipePodspecs`: retained Eyespie MediaPipe Apple artifacts.

Use interfaces for replaceable capabilities such as identity persistence, game persistence, game transport, and optional cloud sync.

## Product rules

- Local state is authoritative for offline play.
- Image embeddings and similarity matching run on-device.
- A hosted account is never required to create or play a local game.
- Portable games may contain target embeddings; document the anti-cheat tradeoff rather than pretending device-local secrets are inaccessible to the device owner.
- Stronger multiplayer authority belongs in an optional host-authoritative transport.
- Cloud adapters must be removable without changing core domain entities.

## Preservation

The pre-reboot application is preserved at `archive/pre-backendless-reboot-2026-08-15` from commit `50091a631d971c520e48884cfbd15cf15dd7251b`.

Do not copy old backend assumptions back into the reboot merely because code exists on that branch.

Useful shared abstractions may be brought in deliberately from `hackelia-micrantha/bluebell` and `bluebell-community`; avoid restoring the previous vendored framework wholesale unless a concrete capability justifies it.

## Build and test

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Apple/MediaPipe integration is validated by `.github/workflows/ios-mediapipe.yml`.

## Changes

Use conventional commits. Keep reboot slices vertical and small: domain contract, local implementation, platform adapter, tests, then UI wiring. Optional network capability comes after the offline path works.
