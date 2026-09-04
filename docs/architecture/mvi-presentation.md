# MVI Presentation Architecture

Eyespie uses feature-level MVI adapted to Kotlin Multiplatform and Compose. The backendless/local-authoritative runtime remains the authority below presentation; MVI owns presentation state and orchestration, not game persistence or domain truth.

## Core contract

```text
Intent -> Interactor -> pure Reducer -> immutable State
          |
          +-> induced work through narrow capabilities
          |
          +-> one-shot Effect
          |
          +-> semantic feature Output
```

- `State` is immutable, lightweight presentation state.
- `Intent` is a typed user/system event entering the feature.
- `Reducer` is pure, synchronous, and deterministic: `(State, Intent) -> State`.
- `Interactor` owns orchestration around reduction and asynchronous capability calls.
- `StateReader` and `IntentDispatcher` expose the minimum common interaction surface.
- `EffectSource` exposes transient one-shot presentation events when a feature needs them.
- `Output` is a semantic feature event for the application coordinator; it never contains a navigation-framework type.
- `StateFlow` is the common KMP state container.

Reducers do not perform I/O, launch coroutines, navigate, resolve resources, emit snackbars, access clocks/randomness implicitly, or mutate external state.

## Dependency direction

```text
app composition/navigation
        |
        v
presentation features -> application/domain capabilities
        |                         |
        v                         v
presentation mappers         domain/core
                                  |
                                  v
                          data/persistence
```

The app layer may know all features. A feature must not import application composition/navigation or another feature.

Use narrow capability interfaces named for what they do (`GameCreator`, `GameSnapshotLoader`, `GuessSubmitter`, `GameSharer`, etc.). `Port` and `Adapter` are not default architectural suffixes. Reserve `Adapter` for genuine external API/type translation.

## Data, domain, and presentation boundaries

The preferred flow is:

```text
SQL row / serialized DTO
        |
        v
     data mapper
        |
        v
domain/application model
        |
        v
 presentation mapper
        |
        v
feature State
```

- Persistence/transport representations stay below the data boundary.
- Domain models contain gameplay concepts and invariants, not Compose resources, navigation, formatting, or SQL concerns.
- Presentation state contains exactly what a screen needs and is never persistence authority.
- Non-trivial conversions should be explicit, pure, testable mappers.
- Avoid mapper ceremony where two trivial value representations are genuinely identical.

`LocalGameSnapshot` is the neutral application/domain projection used by current feature loaders; Home, Utility, Game Detail, and Play derive screen-specific presentation content through pure mappers.

## Route / Screen invariant

Every interactive feature has a higher-order route and a pure screen.

```text
Voyager Screen / AppRoute
        |
        v
FeatureRoute
   |-- creates route-scoped interactor through FeatureFactory
   |-- owns route CoroutineScope lifetime
   |-- collects StateFlow and optional EffectSource
        |
        v
FeatureScreen(state, dispatch)
```

A feature screen receives exactly two parameters:

```kotlin
@Composable
fun CreateGameScreen(
    state: CreateGameState,
    dispatch: (CreateGameIntent) -> Unit,
)
```

Screens do not receive or resolve navigators, repositories, capability implementations, factories, coroutine scopes, app graphs, or domain/application services. Platform UI surfaces such as camera preview/capture may live inside a screen but return bounded values by dispatching intents.

Route-owned work is cancelled when the route leaves composition.

## Feature file layout

Prefer one cohesive concern per file without forcing ceremony around tiny declarations:

```text
features/<feature>/
├── <Feature>State.kt
├── <Feature>Intent.kt
├── <Feature>Output.kt
├── <Feature>Effect.kt          # only when transient events exist
├── <Capability>.kt            # one narrow capability per responsibility
├── <Feature>Reducer.kt
├── <Feature>Interactor.kt
├── <Feature>Factory.kt
├── <Feature>Mapper.kt          # when presentation projection is non-trivial
├── <Feature>Route.kt
└── <Feature>Screen.kt
```

## Application composition and navigation

Application composition/navigation belongs outside `features/` in the top-level `app` package:

```text
app/
├── AppCoordinator.kt
├── AppGraph.kt
├── AppGraphFactory.kt
├── AppRoute.kt
├── AppNavigation.kt
├── AppNavigationBridge.kt
├── AppDestination.kt
├── VoyagerAppNavigation.kt
└── RuntimeAdapters.kt
```

The package boundary is the important ownership signal; a second `app/navigation` namespace is unnecessary while this set remains small. `RuntimeAdapters` is retained only for implementations that genuinely translate runtime/platform APIs into the narrow capabilities consumed above them; `Adapter` is not a general architecture suffix.

`AppCoordinator` translates semantic feature outputs into application navigation commands. `AppGraphFactory` is the composition root and supplies already-composed feature factories/capabilities; it does not retain process-lifetime feature interactors.

Voyager is the single back-stack owner. The app keeps a small command-oriented `AppNavigation` seam (`push`, `pop`, `replace`, `replaceAll`) so coordinator behavior remains framework-independent and unit-testable. Voyager types do not enter feature, domain, or data APIs.

Parameterized destinations use explicit screen keys. Route-scoped interactor disposal remains tied to composition lifetime.

## Transient feedback

Do not put one-shot success/minor recoverable feedback into durable state merely so the UI can consume/reset it.

- transient result -> typed `EffectSource` event -> localized presentation message -> shared `SnackbarHost`;
- persistent actionable condition -> feature state;
- destructive/explicit confirmation -> dialog/sheet state;
- application transition -> semantic feature `Output` -> `AppCoordinator`.

The app shell owns the shared snackbar host. Typed lower-layer failures remain UI-independent and are mapped to Compose resources at the presentation edge.

## Package and naming guidance

Package ownership is the first layer signal; add type prefixes only when ambiguity remains.

Current ownership:

- `core` — domain kernel/value types/matching contracts;
- `game` — local application/game orchestration;
- `persistence` — SQLDelight-backed data implementations;
- `features` — presentation features;
- `presentation` — shared presentation mapping/resources;
- `app` — application composition and navigation.

Prefer names such as `data.repository.SqlGameRepository` or `presentation.game.GameState`; do not mechanically produce redundant names such as `data.repository.DataSqlGameRepository` when the package is already clear.

## Routing and product mockups

The product route-map direction comes from `docs/design/eyespie-app-mockups/` and #220. Only implemented destinations belong in `AppRoute`; do not add dead placeholder screens merely to mirror the board.

Keep routing coarse-grained. Transient variants such as import preview/success/conflict/invalid remain feature state/effects unless they require distinct back-stack/lifecycle semantics.

The mockups are UX direction, not authority/security overrides.

## Performance and memory

Application lifetime may retain navigation, coordinator, runtime capability implementations, and feature factories. Route lifetime owns feature interactors, feature state flows, route coroutine jobs, and transient operation closures.

Camera frames, decoded images, `CapturedImage`, embeddings, model/session handles, and other large/native values must not be retained in feature `StateFlow`, navigation arguments/saved state, application-lifetime stores, or persistence.

A bounded route-local presentation surface may temporarily retain **one user-initiated captured still** when the UX explicitly requires review before commit. That exception is presentation-only: the live camera leaves composition during review; the capture is discarded on retake or route disposal; it is dispatched into MVI only by the final commit action; and the induced operation may retain it only while that operation runs. Do not extend this exception to video/frame streams, embeddings, model/session handles, or cross-route handoff.

Prefer IDs and small presentation models over domain aggregates or duplicate whole-app snapshots. If data volume grows, prefer narrow queries/events rather than cross-feature presentation synchronization.

## Backendless authority boundary

MVI does not become game authority:

- SQLDelight owns persisted local game/progress state.
- `LocalGameLoop` owns game orchestration and matching.
- MediaPipe remains behind embedding capabilities.
- platform implementations own camera/native lifecycle.
- cryptographic identity remains platform-backed.

Features depend on narrow capabilities and domain/application results rather than persistence/platform implementations.

## Test triangle

Every interactive feature has three test sides:

1. **Reducer** — synchronous deterministic `State + Intent -> State` tests.
2. **Interactor/factory** — orchestration, capability use, outputs, and effects through focused fakes.
3. **Screen** — render/interaction coverage for the pure two-parameter screen.

Reducer/interactor coverage runs in common/JVM tests. Compose feature-screen interactions run as Android instrumentation on the hosted emulator. Physical MediaPipe calibration remains a separate evidence boundary.

Application tests cover:

- `AppCoordinator` semantic output -> navigation command translation;
- `AppNavigationBridge` delegation;
- `AppGraphFactory` composition from narrow capabilities;
- production runtime capability wiring where useful.
