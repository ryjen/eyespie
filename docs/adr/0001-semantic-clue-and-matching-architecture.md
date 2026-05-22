# ADR-0001: Semantic Clue and Matching Architecture

## Status

Accepted

## Context

Eyespie began as a computer-vision-driven game experience using perception models to identify objects and scenes.

The product direction has evolved toward a multimodal semantic game engine where:

- LLM reasoning is a core part of clue generation
- Image/object embeddings are a core part of clue matching
- Computer vision remains the grounded perception layer
- Local device models are preferred over remote models
- Remote reasoning is treated as a failsafe and fallback path

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
- offline behavior
- fallback routing
- future provider replacement

## Consequences

### Positive

- stronger semantic gameplay
- privacy-preserving local reasoning
- reduced vendor lock-in
- better cross-platform portability
- clearer separation between perception and reasoning
- easier future evaluation and regression testing

### Negative

- increased architectural complexity
- nondeterministic model behavior
- device capability fragmentation
- additional evaluation and prompt-versioning requirements
- more difficult debugging and QA workflows

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

The system should prefer local execution and require explicit policy or user consent before remote reasoning.

### Vendor Lock-In

The architecture should avoid coupling product behavior to:

- Apple-only APIs
- Android-only APIs
- specific hosted LLM providers

## Future Work

Potential future additions:

- evaluation corpora and replay harnesses
- deterministic scoring modes
- multiplayer canonical evaluation
- semantic scene memory
- on-device vector indices
- federated/private personalization
