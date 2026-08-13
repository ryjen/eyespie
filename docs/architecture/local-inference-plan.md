# Local inference plan

## Purpose

Evaluate whether Eyespie should keep MediaPipe LLM Inference as its local generative runtime, introduce llama.cpp as a portable local inference runtime, or support both behind the application-owned provider boundary.

This plan does **not** replace MediaPipe Vision. The current alpha embedding contract and match authority remain owned by MediaPipe ImageEmbedder / pgvector under #91. Camera ownership remains under #11/#97. This work concerns semantic reasoning and clue generation behind #6/#12/#13/#24.

## Architectural boundary

```text
camera / still image
        |
        +--> MediaPipe Vision ------------------> grounded perception / embeddings
        |                                          |
        |                                          v
        |                                   deterministic match policy
        |
        +--> optional image-capable GenAI provider
                                                   |
structured observations ---------------------------+
                                                   v
                                      SemanticReasoner / ClueAuthoring
                                                   |
                         +-------------------------+------------------------+
                         |                         |                        |
                  MediaPipe GenAI            llama.cpp                remote provider
                    (candidate)               (candidate)             (policy-gated)
```

### Invariants

- MediaPipe Vision remains the real-time / deterministic perception and embedding substrate unless separately re-evaluated.
- An LLM/VLM never becomes sole final match authority.
- Local failure never silently authorizes remote image or context transmission.
- Raw camera frames are not retained or exposed to a text-only provider by default.
- Model/runtime identity and locality are application-owned provenance, not model-authored data.
- Provider selection is injectable and testable.
- Model artifacts are immutable, integrity-checked, license-reviewed, and compatibility-gated before use.
- Base gameplay remains usable when optional GenAI is absent.

## Workstreams

### 1. Provider contract

Refine #13 so Eyespie product policy depends on capabilities rather than a concrete runtime.

Required capabilities should distinguish at least:

- text generation;
- image input;
- streaming;
- local execution;
- supported context / token limits;
- runtime/model identity;
- cancellation;
- lifecycle/readiness.

Do not move Eyespie routing policy into Bluebell. A generic runtime adapter may live in Bluebell only after the application contract is proven and reusable.

### 2. Comparative runtime spike

Implement benchmark-only adapters for:

- current MediaPipe `LlmInference` / `LlmInferenceSession` path;
- llama.cpp using a representative Q4 GGUF model;
- optional image-capable variants only where a supported model/runtime path exists.

The spike must avoid changing release behavior.

Measure on representative physical Android and iOS devices:

- model package size;
- cold initialization latency;
- warm prompt latency;
- time to first token;
- generation throughput;
- peak RSS / native allocations;
- sustained memory growth;
- CPU/GPU utilization where observable;
- thermal state / throttling over repeated requests;
- battery impact over a bounded scenario;
- cancellation latency and cleanup;
- suspend/resume/relaunch behavior;
- unsupported/failure semantics;
- image preprocessing overhead for VLM paths;
- binary/runtime size added to the app.

Record exact device, OS, app SHA, runtime version, model identity/hash, quantization, context size, prompt fixture, and run configuration.

### 3. Workload fixtures

Benchmark the workloads Eyespie actually needs rather than generic chat.

#### Text-only semantic reasoning

Input: bounded structured MediaPipe observations plus game context.

Expected output:

- clue generation;
- candidate ranking;
- bounded semantic interpretation;
- age/difficulty adaptation;
- structured JSON conforming to #12.

#### Image-capable clue generation

Input: one explicit still/crop plus bounded prompt context.

Expected output: structured clue/answer/provenance envelope.

#### Failure/negative cases

- model missing/corrupt/incompatible;
- malformed output;
- oversized context;
- cancellation mid-generation;
- app backgrounding;
- low-memory pressure;
- repeated session creation/destruction;
- unsupported image input;
- provider unavailable;
- remote route disabled.

### 4. Model matrix

Use a small bounded set instead of optimizing for one vendor model prematurely.

| Class | Target | Intended use |
| --- | --- | --- |
| Tiny text LLM | ~0.5-1.5B, Q4 where supported | clue/rule/basic semantic reasoning |
| Small text LLM | ~2-4B, Q4 where supported | richer clue generation and structured reasoning |
| Small VLM | ~2-4B equivalent | explicit-still semantic fallback only |

Model selection criteria:

- redistribution license;
- mobile runtime support;
- model size and memory high-water mark;
- structured-output reliability;
- prompt adherence;
- image capability where needed;
- quantization quality;
- deterministic/reproducible packaging;
- Android/iOS parity.

North Micro or another compact VLM is a candidate, not an architectural dependency.

### 5. Security/privacy review

Extend #18 evidence for any new runtime:

- confirm no unexpected network traffic / telemetry;
- minimize raw-image access by capability;
- keep prompts, hidden answers, exact location, tokens, embeddings, and private paths out of logs;
- verify model provenance and integrity before initialization;
- bound context/session lifetime so request B cannot inherit request A data;
- define filesystem access and temporary image ownership;
- test model-file tampering and incompatible-model rejection;
- add new runtime/model artifacts to SBOM/provenance evidence.

### 6. Delivery decision

Use the QART in `docs/qart/local-genai-runtime.md` after benchmark evidence is available.

The likely end state is one application-owned provider contract with one default local runtime per platform/model class. Supporting two production runtimes indefinitely requires evidence that the compatibility or capability benefit justifies duplicated native integration, testing, model packaging, and support burden.

## Delivery sequence

1. **QART + backlog only** — record alternatives, invariants, gaps, benchmark contract.
2. **Benchmark harness** — no product behavior change.
3. **MediaPipe baseline measurements** — establish current cost/capability.
4. **llama.cpp text-model spike** — Android and iOS, same workload fixtures.
5. **Optional VLM spike** — only if text + MediaPipe observations do not satisfy gameplay needs.
6. **Security/privacy and artifact review** — telemetry, provenance, licenses, model delivery.
7. **Decision** — update QART and, if accepted, create ADR.
8. **Production adapter** — one focused implementation slice behind #13.
9. **Model delivery integration** — update #24 only after runtime/artifact format is selected.

## Decision gates

### Gate A — text-only local reasoning is sufficient

Prefer MediaPipe Vision -> structured observations -> small text LLM if it meets clue quality and latency goals. This keeps raw images outside the LLM path for most requests.

### Gate B — llama.cpp materially improves portability or efficiency

Adopt llama.cpp when physical-device evidence shows a material advantage in at least one primary driver (cross-platform parity, memory/latency, model choice, operational simplicity) without unacceptable binary, security, or maintenance cost.

### Gate C — VLM is justified

Add image-capable LLM inference only for workloads that cannot be satisfied reliably from MediaPipe-derived observations. Treat raw-pixel access as a separate capability and privacy boundary.

## Exit criteria

The planning phase is complete when:

- QART alternatives and recommendation conditions are documented;
- benchmark issue defines reproducible fixtures and evidence format;
- provider-contract issue is linked to #13;
- model/runtime evaluation issue is linked to #24;
- #18/#91 boundaries are explicitly preserved;
- no production runtime/model choice is claimed without physical-device evidence.
