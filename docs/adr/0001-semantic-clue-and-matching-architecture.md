# ADR-0001: Semantic Clue and Matching Architecture

## Status

Accepted

## Context

Eyespie began as a computer-vision-driven game experience using perception models to identify objects and scenes.

The product direction has evolved toward a multimodal semantic game engine where:

- LLM reasoning is a core part of clue generation
- image/object embeddings are a core part of clue matching
- computer vision remains the grounded perception layer
- local device models are preferred over remote models
- remote reasoning is treated as a policy-controlled fallback path

The system must support:

- semantic clue generation
- semantic guess interpretation
- multimodal retrieval and matching
- privacy-preserving local reasoning
- cross-platform support across iOS and Android
- future evaluation and determinism testing

## Decision

Eyespie will adopt a layered multimodal architecture composed of:

1. Perception Layer
2. Embedding Layer
3. Semantic Reasoning Layer
4. Matching and Scoring Layer
5. Provider Routing Layer

### Perception Layer

The perception layer is responsible for grounded scene understanding.

Examples:

- object detection
- segmentation
- scene classification
- OCR
- pose/landmark detection
- attribute extraction

The perception layer is the authoritative source of observable scene evidence.

LLMs must not directly replace perception pipelines.

### Embedding Layer

Embeddings are a core matching primitive.

Embeddings may include:

- full image embeddings
- object crop embeddings
- scene embeddings
- text embeddings

Embeddings are used for:

- candidate retrieval
- similarity ranking
- semantic proximity
- clue candidate expansion
- duplicate or ambiguity detection

Embedding values remain owned by the embedding and index subsystem. Cross-layer contracts expose typed embedding references and metadata rather than platform-specific vector containers.

Each embedding reference must identify at least:

- model and model version
- dimensions
- normalization strategy
- similarity metric
- storage or lifecycle scope

### Semantic Reasoning Layer

The semantic reasoning layer uses bounded structured context to:

- generate clues
- generate hints
- interpret user guesses
- evaluate semantic closeness
- adapt difficulty
- provide accessibility-oriented descriptions

The semantic reasoning layer must operate on structured perception context rather than raw unconstrained frame interpretation whenever possible.

### Matching and Scoring Layer

Matching and scoring must combine:

- perception confidence
- embedding similarity
- semantic reasoning
- ambiguity penalties
- game state constraints

No single model output should exclusively determine game scoring.

Signals must be normalized or calibrated before aggregation. Scoring behavior must define:

- model/version-specific thresholds
- missing-signal behavior
- abstention or rejection thresholds
- deterministic tie-breaking
- score provenance

Reasoning providers may contribute bounded semantic evidence, but they must not manufacture authoritative perception confidence.

### Provider Routing Layer

The application must support multiple reasoning providers.

Provider examples:

- Apple Foundation Models
- Gemini Nano / Android AICore
- remote hosted LLMs
- deterministic rules engines

The provider layer must support:

- capability detection
- local-first execution
- privacy policies
- consent state where applicable
- offline behavior
- fallback routing
- future provider replacement

Remote execution is denied by default. A remote route may be selected only when all required gates succeed:

1. the request declares the minimum data capabilities it needs
2. policy permits those capabilities for the selected provider
3. user consent is present where required
4. the payload has been minimized to the approved fields

Fallback routing must never silently broaden the transmitted data. Raw images, object crops, OCR text, faces, precise location, and other sensitive scene data require explicit capabilities rather than inheriting permission from a general remote-model setting.

Remote decisions must preserve provenance for the policy decision, consent basis, provider, model, and transmitted field classes.

## Consequences

### Positive

- stronger semantic gameplay
- privacy-preserving local reasoning
- reduced vendor lock-in
- better cross-platform portability
- clearer separation between perception and reasoning
- easier future evaluation and regression testing
- auditable remote-data boundaries
- portable domain contracts

### Negative

- increased architectural complexity
- nondeterministic model behavior
- device capability fragmentation
- additional evaluation and prompt-versioning requirements
- more difficult debugging and QA workflows
- explicit calibration and policy infrastructure

## Risks

### Determinism

Generated clues and semantic matches may vary between:

- model providers
- model versions
- device hardware
- local vs remote execution

The system should preserve provenance metadata and support evaluation corpora.

### Privacy

Remote reasoning may expose camera-derived scene context.

The system defaults to local execution and requires explicit policy authorization plus consent where applicable before remote reasoning. Only the minimum approved structured payload may cross the remote boundary.

### Vendor Lock-In

The architecture should avoid coupling product behavior to:

- Apple-only APIs
- Android-only APIs
- specific hosted LLM providers
- platform-specific vector or serialization types

## Future Work

Potential future additions:

- evaluation corpora and replay harnesses
- deterministic scoring modes
- multiplayer canonical evaluation
- semantic scene memory
- on-device vector indices
- federated/private personalization
