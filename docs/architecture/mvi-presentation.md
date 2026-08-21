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
- **Interactor** owns dispatch. It reduces first, then induces asynchronous work or IO.
- **Use cases/domain services** perform application/domain operations behind injected interfaces.
- **Compose views** render passed state and dispatch intents. They do not call repositories, persistence, or `LocalGameLoop` directly.
- **StateFlow** is the KMP state container used by interactors.

This is the Kotlin/Compose equivalent of Achillea ADR-0003's `State + Intent + Reducer + Interactor` model. The pre-backendless Eyespie branch also used the same underlying unidirectional model through `UiState`, actions, `StateFlow`, and ScreenModels; this decision recovers that useful presentation boundary without restoring the retired cloud architecture.

## Boundaries

The MVI layer does not become game authority. Existing backendless boundaries remain authoritative:

- SQLDelight owns persisted local game/progress state.
- `LocalGameLoop` owns game orchestration and matching operations.
- platform implementations own camera/native lifecycle and return bounded `CapturedImage` values.
- MediaPipe remains behind the image-embedding boundary.
- cryptographic identity remains platform-backed.

The presentation interactor consumes those capabilities; it does not duplicate them.

## Rules

1. A feature defines immutable `State` and sealed/typed `Intent` values.
2. Reducers contain no coroutine, IO, clock, random, repository, camera, model, or persistence calls.
3. Dispatch reduces state before induced work starts.
4. Async work is induced by the interactor through injected use cases/services.
5. Results return through typed intents and are reduced like user intents.
6. Views receive state plus a dispatch function and remain render-only apart from platform UI surfaces such as camera preview/capture.
7. Platform UI callbacks may wrap native results into bounded common values and dispatch them; business operations still occur behind the interactor.
8. Derived presentation selections needed by a view belong in reduced state or injected derivation/use-case logic, not ad-hoc repository traversal in the view.
9. New presentation state-management patterns require an explicit architecture decision rather than growing alongside MVI.

## Testing

- reducers: deterministic transition tests;
- interactors: reduce-before-induce and side-effect/result transition tests with fake injected use cases;
- domain/persistence: continue existing unit/integration coverage;
- views: interaction tests should assert emitted intents rather than domain side effects.

## Non-goals

This decision does not restore the old feature graph, Kodein graph, Voyager graph, Supabase dependencies, remote repositories, or vendored Bluebell runtime. Reusable Bluebell abstractions may be adopted separately only when they fit the current backendless architecture.
