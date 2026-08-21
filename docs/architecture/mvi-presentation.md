# MVI Presentation Architecture

Eyespie uses the same unidirectional presentation model established in Achillea, adapted to Kotlin Multiplatform and Compose.

## Decision

For interactive features, the canonical presentation contract is:

```text
View --dispatch(Intent)--> Interactor --reduce--> immutable State
                            |
                            +--induce--> use cases / domain / platform boundary
                                           |
                                           +--dispatch(result Intent)--> State
```

- **State** is immutable feature presentation state.
- **Intent** is a typed user or system event.
- **Reducer** is pure and synchronous: `(State, Intent) -> State`.
- **Interactor** owns dispatch. It reduces first, then induces asynchronous work, IO, or navigation.
- **Use cases/domain services** perform application/domain operations behind injected interfaces.
- **Compose views** render passed state and dispatch intents. They do not call repositories, persistence, or `LocalGameLoop` directly.
- **StateFlow** is the KMP state container used by interactors.

This is the Kotlin/Compose equivalent of Achillea ADR-0003's `State + Intent + Reducer + Interactor` model.

The boundary is **feature-level**, not one global application store. Feature interactors own Home, Onboarding, Create Game, Play Game, and future feature state independently.

## App layer: navigation and dependency injection

`features/app` is the application composition boundary.

`AppGraph` is the single composition root for presentation dependencies. It owns:

- construction of feature interactors;
- injection of `AppGameUseCases` and coroutine scope;
- cross-feature callbacks such as snapshot adoption and home failure reporting;
- creation of route-specific Play interactors from the current authoritative Home snapshot.

`AppNavigator` owns application route state as `StateFlow<AppScreen>`. Feature interactors receive navigation as an injected capability; Compose does not mutate route state directly.

`App.kt` constructs one `AppGraph` from `EyespieRuntime`, observes graph state, and renders the selected route. Rendering helpers live separately from the composition root.

```text
EyespieRuntime
     |
     v
  AppGraph -----------------> AppNavigator / AppScreen
     |
     +-- HomeInteractor
     +-- OnboardingInteractor
     +-- CreateGameInteractor
     +-- PlayGameInteractor(route)
     |
     +-- AppGameUseCases --> LocalGameLoop
```

No second DI container or service locator is introduced. Tests may replace injected interfaces directly by constructing `AppGraph` with fakes.

## Historical Eyespie precedent

The pre-backendless branch at `archive/pre-backendless-reboot-2026-08-15` already organized presentation logic by feature and used reducers/state machines in addition to `UiState`, actions, `StateFlow`, ScreenModels, and use cases.

The old onboarding feature is a particularly useful precedent: `OnboardingState` was feature-scoped and `OnboardingReducer` guarded asynchronous capability results with `requestInFlight` and previous-state checks so stale completions could not overwrite newer state. The new architecture preserves that state-machine discipline while deliberately not restoring the old GenAI download, cloud, Kodein, or Voyager assumptions.

The rebooted onboarding feature is a concrete local-first MVI slice for Welcome, Create, and Play guidance only. It does not perform account creation, model download, remote capability provisioning, or permission requests. Because the rebooted core does not yet persist first-run completion, onboarding is explicitly reachable from Home as **How to play** rather than pretending to be a durable show-once startup flow.

## Current feature topology

```text
AppGraph / AppNavigator
    |
    +-- HomeInteractor --------> HomeState
    |       +-- load/adopt local snapshot
    |
    +-- OnboardingInteractor --> OnboardingState
    |       +-- local product-guidance state machine
    |
    +-- CreateGameInteractor --> CreateGameState
    |       +-- create local game
    |       +-- refresh/adopt snapshot
    |
    +-- PlayGameInteractor ----> PlayGameState
            +-- local guess/match
            +-- refresh/adopt snapshot
```

Home/Onboarding/Create/Play do not share one mutable feature state object.

## Boundaries

The MVI layer does not become game authority. Existing backendless boundaries remain authoritative:

- SQLDelight owns persisted local game/progress state.
- `LocalGameLoop` owns game orchestration and matching operations.
- platform implementations own camera/native lifecycle and return bounded `CapturedImage` values.
- MediaPipe remains behind the image-embedding boundary.
- cryptographic identity remains platform-backed.

Presentation interactors consume those capabilities; they do not duplicate them.

## Rules

1. Each interactive feature defines its own immutable `State`, sealed/typed `Intent`, pure `Reducer`, and `Interactor`.
2. Do not grow one global application reducer/interactor containing unrelated feature state.
3. `features/app` owns navigation and presentation DI/composition.
4. Compose must not construct feature interactors individually or mutate route state directly.
5. Reducers contain no coroutine, IO, clock, random, repository, camera, model, navigation, or persistence calls.
6. Dispatch reduces state before induced work starts.
7. Async work and navigation are induced by the feature interactor through injected capabilities/callback boundaries.
8. Results return through typed intents and are reduced like user intents.
9. Views receive state plus a dispatch function and remain render-only apart from platform UI surfaces such as camera preview/capture.
10. Async completion must remain observable. A successful primary operation followed by a failed refresh must preserve the primary result and surface the refresh failure rather than silently retaining stale state.
11. Stale async results must be rejected or correlated when a feature can issue overlapping operations.
12. New presentation state-management or DI patterns require an explicit architecture decision rather than growing alongside MVI.

## Feature test triangle

Every interactive feature maintains the same three-sided unit-test contract:

```text
              Feature
             /       \
        Reducer       App wiring
          |              |
 pure State+Intent   DI + navigation
          \              /
            Interactor
          behavior/effects
```

### 1. Reducer

Test deterministic `State + Intent -> State` transitions directly. These tests have no DI, coroutines, navigation, or runtime dependencies.

### 2. Interactor through DI

Interactor tests resolve the production feature instance from `AppGraph` using fake injected capabilities. Tests must not manually recreate production constructor wiring. This catches broken dependency composition while retaining unit-test speed and determinism.

### 3. App wiring/navigation

Verify routing and cross-feature propagation through `AppGraph`/`AppNavigator`: Home -> Create, Home -> Onboarding, Play -> Home, snapshot adoption, retained-state reset, and route-specific Play construction.

This triangle applies to **Home, Onboarding, Create Game, Play Game, and every future interactive feature**.

Additional testing rules:

- use controlled coroutine test scopes for induced work;
- explicitly test partial-success cases such as successful guess + failed snapshot refresh;
- test stale-result protection wherever operations may overlap;
- domain/persistence keep their existing unit/integration coverage;
- view/UI tests, when added, assert emitted intents and rendered state rather than bypassing MVI to assert domain side effects.

## Non-goals

This decision does not restore the old feature graph, Kodein graph, Voyager graph, Supabase dependencies, remote repositories, GenAI onboarding requirements, or vendored Bluebell runtime. Reusable Bluebell/community abstractions may be adopted separately only when they fit the current backendless architecture.
