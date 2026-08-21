# MVI Presentation Architecture

Eyespie uses the feature-level MVI interactor model established in Achillea, adapted to Kotlin Multiplatform and Compose. The pre-backendless Eyespie branch provides historical precedent for feature-owned state machines and ScreenModel-style orchestration; the current backendless/local-authoritative runtime remains the implementation boundary below presentation.

## Core contract

```text
Intent -> Interactor -> Reducer -> immutable State
          |
          +-> induced work through feature-owned ports
          |
          +-> semantic feature Output
```

- `State` is immutable, lightweight presentation state.
- `Intent` is a typed user/system event entering the feature.
- `Reducer` is pure and synchronous: `(State, Intent) -> State`.
- `Interactor` always reduces first, then induces asynchronous work or emits a semantic output.
- `Port` is an interface owned by the consuming feature and implemented by the app/runtime adapter.
- `Output` is a semantic feature event; it never contains an app route.
- `StateFlow` is the common KMP state container.

## Dependency direction

```text
app -> feature contracts + feature factories/routes
feature -> domain/core + feature-owned port
runtime adapter -> feature-owned ports

feature -X-> features.app
feature -X-> another feature
```

The app may know all features. Features do not know application navigation, application DI, or other features.

## Route / Screen invariant

Every interactive feature has a higher-order route and a pure screen.

```text
AppRoute
   |
   v
FeatureRoute
   |-- creates route-scoped interactor through FeatureFactory
   |-- owns route CoroutineScope lifetime
   |-- collects StateFlow
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

Screens do not receive or resolve navigators, repositories, ports, factories, coroutine scopes, app graphs, or domain/application services. Platform UI surfaces such as camera preview/capture may live inside a screen but may only return bounded values by dispatching intents.

The route is responsible for interactor initialization and state binding. Route-owned coroutine scopes are cancelled when the route leaves composition, releasing feature work and retained state.

## Feature file layout

Prefer one concern per file:

```text
features/<feature>/
├── <Feature>State.kt
├── <Feature>Intent.kt
├── <Feature>Output.kt
├── <Feature>Port.kt
├── <Feature>Reducer.kt
├── <Feature>Interactor.kt
├── <Feature>Factory.kt
├── <Feature>Route.kt
└── <Feature>Screen.kt
```

Feature-specific presentation failure types may be separate files as needed.

## App layer

`features/app` owns application composition and route translation, split by responsibility:

```text
features/app/
├── AppRoute.kt
├── AppNavigator.kt
├── StateFlowAppNavigator.kt
├── AppCoordinator.kt
├── AppGraph.kt
├── AppGraphFactory.kt
└── RuntimeAdapters.kt
```

- `AppRoute` defines implemented application destinations.
- `AppNavigator` owns application route state only.
- `AppCoordinator` translates feature outputs into app routes.
- `AppGraph` contains already-composed factories/navigation, not process-lifetime feature interactors.
- `AppGraphFactory` is the composition root.
- `RuntimeAdapters` implement feature-owned ports over `EyespieRuntime`/`LocalGameLoop`.

No service locator or DI/navigation framework is required for this topology.

## Routing and product mockups

The product route-map direction comes from `docs/design/eyespie-app-mockups/` and #220. The board covers onboarding, game list/detail, create/clue authoring, signed share/export, import, play/camera, progress, and profile/settings.

Only implemented destinations belong in `AppRoute`; do not add dead placeholder screens merely to mirror the board. Future destinations must follow the same feature-output -> `AppCoordinator` -> `AppRoute` direction.

Keep routing coarse-grained. Transient variants such as import preview/success/conflict/invalid should normally remain one feature's state unless they require distinct back-stack/lifecycle semantics.

The mockups are UX direction, not authority/security overrides. Their documented corrections for document import, non-mutating conflicts, and signed-bundle privacy/provenance remain binding.

## Performance and memory

Presentation architecture is also a lifetime/allocation boundary.

### Object lifetime

Application lifetime:

- navigator;
- coordinator;
- runtime adapter(s);
- feature factories.

Route lifetime:

- feature interactor;
- feature `StateFlow`;
- route coroutine jobs;
- transient capture/operation closures.

Do not eagerly retain every feature interactor for process lifetime.

### Heavy values

Camera frames, decoded images, `CapturedImage`, embeddings, model/session handles, and other large/native values must not be retained in long-lived presentation state.

A capture may enter as an intent and be held only by the induced operation while it is running. Reducers store lightweight flags/results instead.

### Presentation state sizing

State contains exactly what the screen needs. Prefer IDs and small presentation models over carrying domain aggregates or duplicate whole-app snapshots.

Current implementation follows this by:

- mapping Home's runtime snapshot into `HomeContent`;
- initializing Play from `GameId`/`ThingId` and loading a small `PlayGameContent`;
- using `GuessOutcome.progress` directly after a guess rather than reloading/adopting the entire Home snapshot;
- returning Home to an independent refresh on route entry after Create/Play.

Whole-snapshot reloads remain acceptable at the runtime boundary for alpha, but should not become cross-feature presentation synchronization. If data volume grows, prefer narrow queries/events rather than rebuilding duplicate presentation graphs after every operation.

## Backendless authority boundary

MVI does not become game authority:

- SQLDelight owns persisted local game/progress state.
- `LocalGameLoop` owns game orchestration and matching.
- MediaPipe remains behind embedding boundaries.
- platform implementations own camera/native lifecycle.
- cryptographic identity remains platform-backed.

Runtime adapters translate those capabilities into feature-owned ports; features do not duplicate authority.

## Test triangle

Every interactive feature has three test sides:

1. **Reducer** — deterministic `State + Intent -> State` transitions.
2. **Interactor through feature factory** — behavior/effects using fake feature-owned ports; tests do not recreate production constructor wiring manually.
3. **Screen** — render/interaction coverage against the pure two-parameter screen; user interaction emits the expected intent and rendered output derives only from state.

App tests are separate:

- `AppCoordinator` output -> route translation;
- navigator behavior;
- `AppGraphFactory` composition from replaceable ports;
- production runtime-adapter wiring where useful.

Feature unit tests do not depend on the global `AppGraph`.

## Architecture enforcement

The following are architectural failures:

- a feature source importing `features.app`;
- a feature importing another feature;
- a pure screen with parameters other than `state` and `dispatch`;
- a screen resolving DI/navigation/services;
- a feature interactor depending on `AppRoute`/`AppNavigator`;
- heavy/native capture/model values stored in long-lived state;
- route-scoped work launched into an application-lifetime scope.

These rules should be enforced with lightweight static checks in addition to code review.

## Non-goals

This architecture does not restore the old Kodein/Voyager/Supabase feature graph, add Koin/Hilt/Decompose, or change SQLDelight/MediaPipe/identity authority. Framework adoption requires a demonstrated complexity need and a separate architecture decision.
