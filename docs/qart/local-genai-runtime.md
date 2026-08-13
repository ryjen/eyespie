# QART: Which local GenAI runtime should Eyespie use alongside MediaPipe Vision?

## Status

Investigating

## Context

Eyespie already has a semantic-game architecture in #6 and a provider-routing policy in #13. The current Android implementation of `PlatformGenAI` is concrete MediaPipe `LlmInference` / `LlmInferenceSession`, hard-wires the preferred backend to GPU, supports optional image input through MediaPipe GenAI vision options, and owns a long-lived session. iOS local image-capable GenAI remains unsupported by the current project-specific MediaPipe distribution.

Separately, #91 establishes MediaPipe ImageEmbedder and pgvector as the alpha embedding/match path. That path is not under reconsideration here. MediaPipe Vision remains the grounded perception/embedding substrate.

#24 defines optional large-model delivery but intentionally leaves the runtime and artifact format unresolved. Introducing llama.cpp would make GGUF a credible portable model format and could improve Android/iOS parity, but it would also add another native runtime, packaging surface, model ecosystem, and security/maintenance obligation.

The decision must therefore be based on Eyespie workloads and physical-device evidence rather than generic LLM benchmark claims.

## Question

> For optional local semantic reasoning and clue generation, should Eyespie retain MediaPipe LLM Inference, adopt llama.cpp, or support a bounded hybrid behind the existing provider boundary while MediaPipe Vision remains the perception/embedding runtime?

## Decision drivers

1. Cross-platform Android/iOS capability parity for the actual Eyespie workloads.
2. Mobile resource behavior: memory high-water mark, latency, thermals, battery, binary/model size, and lifecycle recovery.
3. Security/privacy: local-only behavior, bounded context, raw-image capability isolation, model integrity, telemetry/network behavior, and auditable provenance.
4. Maintainability: native integration complexity, upgrade burden, build/CI surface, model packaging, and support ownership.
5. Model portability and availability of suitably small licensed text/VLM models.
6. Reversibility and compatibility with #13/#24 without coupling product policy to a runtime.

## Constraints and invariants

- MediaPipe Vision remains the alpha perception/image-embedding path under #91 unless separately reviewed.
- LLM/VLM output cannot become sole final match authority.
- The base game remains usable without optional GenAI.
- Remote inference is disabled by default and local failure cannot silently broaden to remote transmission.
- Raw image access is a distinct provider capability; text-only reasoning should consume structured observations where sufficient.
- Runtime/model/locality identity is application-owned provenance.
- Model artifacts must be immutable, integrity checked, license reviewed, and compatibility gated before initialization.
- Provider/session lifetime must not leak prompt/image context between logically separate requests.
- Logs/diagnostics must exclude raw images, hidden answers, exact location, tokens, embeddings, private paths, and arbitrary sensitive prompt payloads.
- Physical-device evidence is required before a production runtime decision.

## Assumptions and evidence gaps

| Statement | Fact, assumption, or unknown | Evidence or validation needed |
| --- | --- | --- |
| Android currently uses MediaPipe `LlmInference` with a preferred GPU backend | Fact | Current `bluebell/src/androidMain/.../GenAI.kt` |
| iOS image-capable local GenAI is not available through the current project-specific MediaPipe integration | Fact for current project baseline | #13/#75 plus release-candidate verification |
| llama.cpp can provide a more symmetric Android/iOS local runtime | Assumption | Minimal adapters and physical-device builds |
| Small Q4 text models can meet Eyespie clue/reasoning quality at acceptable memory/latency | Unknown | Reproducible Eyespie workload benchmark |
| A VLM is necessary for most clue-generation requests | Unknown | Compare structured MediaPipe observations + text LLM against explicit-still VLM |
| llama.cpp reduces overall product complexity | Unknown | Include native build, ABI, packaging, CI, model delivery, update, and support costs |
| MediaPipe GenAI is more resource-efficient for supported mobile accelerators | Unknown | Same model-class/workload comparison where technically comparable |
| Either runtime emits no unexpected network traffic/telemetry | Unknown until verified | Controlled physical-device network observation |
| GGUF redistribution is viable for selected candidate models | Model-specific unknown | License and artifact provenance review |

## Alternatives

### Alternative A: Retain MediaPipe LLM Inference as the sole local GenAI runtime

- **Mechanism and boundary:** Keep `GenAI` backed by MediaPipe GenAI; preserve MediaPipe Vision separately for perception/embeddings.
- **Benefits:** Smallest change from Android current state; one Google-origin inference family; less immediate native-runtime duplication; existing Android image-capable path.
- **Costs and limitations:** Current iOS capability asymmetry; hard-wired GPU behavior needs repair; model/runtime choices remain constrained by MediaPipe support; existing session behavior requires context-isolation hardening.
- **Security, privacy, and governance:** Reuses existing model-integrity/privacy controls, but #125-style telemetry/runtime verification remains required and GenAI session context must be bounded.
- **Operations and ownership:** Fewer runtime families if iOS support is sufficient; upgrades remain coupled to project-specific MediaPipe distribution work.
- **Compatibility and migration:** Lowest Android migration cost; potentially highest iOS feature gap.
- **Delivery effort and maintenance:** Low on Android, uncertain/high if iOS requires further custom MediaPipe work.
- **Reversibility:** Easy before #24 production model delivery; moderate after model/artifact lifecycle is coupled to MediaPipe.
- **Evidence required:** Current Android baseline measurements; iOS feasibility; supported model matrix; resource/telemetry data.

### Alternative B: Adopt llama.cpp as the sole local GenAI runtime

- **Mechanism and boundary:** Add a native llama.cpp adapter behind the application-owned GenAI/provider contract; use GGUF text/VLM models as selected; retain MediaPipe Vision only for perception/embeddings.
- **Benefits:** Potential Android/iOS runtime symmetry; broad GGUF model ecosystem and quantization options; runtime/model choice is less coupled to MediaPipe release support; text-only semantic reasoning can be cleanly separated from raw-image capability.
- **Costs and limitations:** New C/C++ native dependency and ABI/build surface; binary-size and packaging cost; mobile GPU/accelerator behavior must be proven; VLM support/model preprocessing may add complexity; duplicate inference families remain because MediaPipe Vision still exists.
- **Security, privacy, and governance:** Strong local-only architecture is possible, but model parsing/native attack surface, model provenance, mmap/file access, context lifetime, and dependency CVE/update handling become new obligations.
- **Operations and ownership:** Eyespie owns llama.cpp integration/build compatibility plus model selection/quantization validation.
- **Compatibility and migration:** Requires new artifact path in #24 and adapter migration from current Android MediaPipe GenAI.
- **Delivery effort and maintenance:** Moderate/high initial integration, then potentially simpler cross-platform model/runtime parity.
- **Reversibility:** Moderate; easy while experimental, harder once model delivery and release artifacts standardize on GGUF.
- **Evidence required:** Android/iOS benchmark spike, binary-size/build evidence, selected model licenses, lifecycle tests, network/telemetry verification.

### Alternative C: Bounded hybrid behind one provider contract

- **Mechanism and boundary:** Keep MediaPipe Vision fixed; make the GenAI provider contract runtime-neutral; permit MediaPipe GenAI and llama.cpp adapters during evaluation, then select one default per capability/platform based on evidence. Do not promise indefinite dual-runtime support.
- **Benefits:** Maximizes reversibility; allows direct evidence against the current baseline; avoids prematurely coupling #24 to GGUF or MediaPipe artifacts; supports a staged text-first strategy with optional VLM escalation.
- **Costs and limitations:** Temporary duplicated adapters/test matrix; risk that temporary dual support becomes permanent accidental complexity.
- **Security, privacy, and governance:** Cleanest place to enforce locality, raw-image capability, provenance, context isolation, and remote-fallback policy independently of runtime.
- **Operations and ownership:** Requires explicit deletion/convergence gate after the spike.
- **Compatibility and migration:** Highest short-term compatibility; provider contract can preserve application semantics across a runtime decision.
- **Delivery effort and maintenance:** Moderate bounded spike; unacceptable if both runtimes are retained indefinitely without evidence.
- **Reversibility:** Easy during the evaluation period.
- **Evidence required:** Same benchmark/fixture suite across both adapters and explicit convergence criteria.

### Alternative D: Maintain current state / defer

Keep current Android MediaPipe GenAI, explicit iOS manual fallback, and make no new runtime investment until after alpha.

This is responsible if GenAI remains optional and manual clue authoring satisfies alpha. It becomes unacceptable if post-alpha product goals require symmetric offline semantic reasoning or if the current ~584 MB model/runtime path materially harms installability, performance, or supportability.

## Comparison

| Criterion | MediaPipe GenAI only | llama.cpp only | Bounded hybrid/evaluation | Defer |
| --- | --- | --- | --- | --- |
| Outcome fit | Good on current Android; uncertain iOS | Potentially strong cross-platform | Strongest evidence path | Sufficient for manual-fallback alpha |
| Security and governance | Existing controls, vendor-runtime verification needed | New native/model surface, controllable locality | Contract can enforce policy uniformly | Lowest new surface |
| Reliability and recovery | Partly known Android behavior | Unknown until spike | Best comparative evidence | Current known asymmetry remains |
| Operability and ownership | MediaPipe upgrade coupling | llama.cpp + GGUF ownership | Temporary highest cost, then converge | Lowest immediate cost |
| Compatibility and migration | Lowest Android change | Requires artifact/runtime migration | Preserves both paths during decision | No migration |
| Complexity and maintenance | Lowest if iOS becomes viable | Moderate native integration; one GenAI runtime | High only if allowed to become permanent | Lowest now |
| Cost and delivery | Low Android / uncertain iOS | Moderate-high initial | Moderate bounded research cost | Minimal |
| Reversibility | Moderate | Moderate | High | High |

## Recommendation

**Recommend Alternative C as the decision path, not as a permanent product architecture.**

Create a runtime-neutral application provider contract, retain current MediaPipe GenAI as the baseline, and run a bounded llama.cpp spike against the same Eyespie workload fixtures on physical Android and iOS devices. Preserve MediaPipe Vision/embedding authority unchanged.

Confidence is **moderate** because the architectural fit is clear but the decisive mobile performance and iOS-support evidence does not yet exist.

The likely production preference, if evidence supports it, is:

```text
MediaPipe Vision -> structured observations -> small local text LLM
                                           |
                                           +-> optional explicit-still VLM fallback
```

This minimizes raw-pixel exposure to a generative model and gives a small text model the highest-frequency semantic workload. llama.cpp should become the production GenAI runtime only if it materially improves cross-platform parity, model portability, or resource behavior after including native-build and maintenance costs. If MediaPipe GenAI reaches acceptable iOS parity and wins resource/maintenance comparisons, retain it instead.

Do **not** standardize #24 on GGUF or another new artifact format before this evidence exists.

## Trade-offs

### Accepted

- A small amount of temporary duplicate adapter code and benchmark infrastructure.
- Deferring the final GenAI runtime/model format decision until physical-device evidence exists.
- MediaPipe Vision and a separate GenAI runtime may remain two native inference families if llama.cpp wins; this is acceptable only if the product benefit is material.

### Rejected

- Replacing MediaPipe Vision/embedding solely to unify runtimes.
- Selecting llama.cpp because it has a broader model ecosystem without measuring mobile lifecycle/resource cost.
- Keeping two production GenAI runtimes indefinitely without a capability requirement.
- Giving a VLM continuous camera access or final match authority.
- Silent remote fallback when local inference fails.

### Residual risks and mitigations

| Risk | Mitigation | Acceptance owner |
| --- | --- | --- |
| Benchmark does not predict sustained real gameplay | Include repeated requests, thermal/battery runs, background/resume, and memory-pressure cases | Eyespie maintainers |
| Temporary hybrid becomes permanent common mechanism | Put explicit convergence/deletion criteria in the spike issue | Eyespie maintainers |
| Model quality dominates runtime comparison | Test at least one tiny and one small text model class; compare runtime separately from model quality where possible | Eyespie maintainers |
| New native dependency expands attack/supply-chain surface | Pin source/artifacts, SBOM, CVE/update policy, integrity checks, fuzz/negative testing around model loading | Eyespie maintainers |
| Raw-image capability broadens privacy exposure | Capability-gate image input; prefer structured MediaPipe observations; explicit still/crop only | Eyespie maintainers |
| Runtime telemetry assumptions are wrong | Controlled network observation per runtime/platform before release claims | Eyespie maintainers |

### Revisit triggers

- MediaPipe adds supported iOS GenAI/VLM parity that satisfies Eyespie requirements.
- llama.cpp materially changes mobile backend/accelerator support.
- A selected model requires a runtime-specific feature.
- #24 model delivery constraints make one artifact/runtime materially simpler.
- Physical-device measurements show unacceptable memory, latency, thermal, battery, or binary-size behavior.
- Product requirements change from occasional clue/reasoning inference to continuous generative inference.

## Decision path

**Needs bounded spike or evidence.**

The next artifacts are:

1. provider-contract grooming tied to #13;
2. reproducible MediaPipe-vs-llama.cpp benchmark issue;
3. bounded model/workload evaluation tied to #24;
4. #18 security/privacy evidence update after a candidate runtime exists;
5. update this QART to `Ready for decision` once physical-device results are attached;
6. create an ADR only after a production runtime is selected.

## Decision outcome

- **Decision:** Pending evidence
- **Date:** Pending
- **Decision owners:** Eyespie maintainers
- **Disposition:** Deferred pending bounded spike
- **RFC:** Not required for benchmark; revisit if provider boundary expands beyond Eyespie/Bluebell ownership
- **ADR:** Required after final runtime selection
- **Follow-up work:** benchmark, provider contract, model matrix, security/privacy verification, #24 artifact decision
