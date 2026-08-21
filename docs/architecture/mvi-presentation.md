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

The boundary is **feature-level**, not one global application store. The app shell owns composition/routing; feature interactors own Home, Onboarding, Create Game, Play Game, and future feature state independently.

## Historical Eyespie precedent

The pre-backendless branch at `archive/pre-backendless-reboot-2026-08-15` already organized presentation logic by feature and used reducers/state machines in addition to `UiState`, actions, `StateFlow`, ScreenModels, and use cases.

The old onboarding feature is a particularly useful precedent: `OnboardingState` was feature-scoped and `OnboardingReducer` guarded asynchronous capability results with `requestInFlight` and previous-state checks so stale completions could not overwrite newer state. The new architecture preserves that state-machine discipline while deliberately not restoring the old GenAI download, cloud, Kodein, or Voyager assumptions.

The rebooted onboarding feature is now a concrete local-first MVI slice for product guidance. It models Welcome, Create, and Play pages only. It does not perform account creation, model download, remote capability provisioning, or permission requests. Because the rebooted core does not yet persist first-run completion, onboarding is explicitly reachable from Home as **How to play** rather than pretending to be a durable show-once startup flow. A future local preference may select it as the initial route without changing the feature contract.

## Current feature topology

```text
App shell / route composition
    |
    +-- HomeInteractor --------> HomeState
    |       |
    |       +-- load/adopt local snapshot
    |
    +-- OnboardingInteractor --> OnboardingState
    |       |
    |       +-- local product-guidance state machine
    |
    +-- CreateGameInteractor --> CreateGameState
    |       |
    |       +-- create local game
    |       +-- refresh/adopt snapshot
    |
    +-- PlayGameInteractor ----> PlayGameState
            |
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
3. Reducers contain no coroutine, IO, clock, random, repository, camera, model, navigation, or persistence calls.
4. Dispatch reduces state before induced work starts.
5. Async work and navigation are induced by the feature interactor through injected capabilities/callback boundaries.
6. Results return through typed intents and are reduced like user intents.
7. Views receive state plus a dispatch function and remain render-only apart from platform UI surfaces such as camera preview/capture.
8. Platform UI callbacks may wrap native results into bounded common values and dispatch them; business operations still occur behind the interactor.
9. Async completion must remain observable. A successful primary operation followed by a failed refresh must preserve the primary result and surface the refresh failure rather than silently retaining stale state.
10. Stale async results must be rejected or correlated when a feature can issue overlapping operations; the old onboarding `requestInFlight` pattern is a valid precedent.
11. New presentation state-management patterns require an explicit architecture decision rather than growing alongside MVI.

## Testing

- reducers: deterministic transition tests;
- interactors: prove reduce-before-induce with an initial state that makes the synchronous transition observable and a controlled coroutine scope;
- explicitly test partial-success cases such as successful guess + failed snapshot refresh;
- test stale-result protection wherever operations may overlap;
- domain/persistence: continue existing unit/integration coverage;
- views: interaction tests should assert emitted intents rather than domain side effects.

## Non-goals

This decision does not restore the old feature graph, Kodein graph, Voyager graph, Supabase dependencies, remote repositories, GenAI onboarding requirements, or vendored Bluebell runtime. Reusable Bluebell/community abstractions may be adopted separately only when they fit the current backendless architecture.
