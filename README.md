# Eyespie

Eyespie is an offline-first travel spy game built with Kotlin Multiplatform and on-device AI.

## Reboot direction

Core gameplay requires **no backend and no hosted account**. Games, clues, embeddings, matching, and progress are local-authoritative. Cloud and peer networking are optional adapters added only for capabilities that need them.

The former Supabase-based application is preserved at:

- branch: `archive/pre-backendless-reboot-2026-08-15`
- commit: `50091a631d971c520e48884cfbd15cf15dd7251b`

The reboot is tracked in GitHub issue #156.

## Current reboot skeleton

The first slice intentionally contains a small cross-platform surface:

- Compose Android/iOS application entry points;
- provider-neutral game/player/Thing domain models;
- deterministic on-device cosine `MatchEngine` with common tests;
- Android camera + MediaPipe dependencies retained for the next capture slice;
- Eyespie MediaPipe Apple pod graph and model/calibration assets retained;
- no Supabase runtime, auth flow, GraphQL/PostgREST/realtime/storage client, or backend configuration;
- Android core manifest does not request network access.

See [ARCHITECTURE.md](ARCHITECTURE.md) for the target architecture and staged implementation plan.

## Build

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

The iOS MediaPipe integration workflow also compiles the Kotlin simulator target and builds the Swift wrapper app.
