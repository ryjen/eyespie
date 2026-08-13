# SBOM License and NOTICE Gap Review

## Status

Commercial-distribution evidence review supporting #72 and #106.

This document reviews what the current Eyespie SBOM pipeline proves and what it does **not** yet prove for paid/commercial distribution. It does not replace qualified legal review and does not expand the #90 closed-alpha product scope.

## Evidence snapshot

Reviewed successful `SBOM` workflow run:

```text
run:       31685055135
artifact:  eyespie-sbom / 9175084256
```

The workflow produces two CycloneDX documents:

```text
eyespie-gradle.cdx.json
eyespie-cocoapods.cdx.json
```

This is a point-in-time evidence snapshot. The exact paid-release candidate must regenerate and pass the same or stronger checks; these counts must not be treated as permanent dependency inventory.

## What #72 already proves well

The existing SBOM pipeline is strong on dependency/artifact identity:

- generates a Gradle/KMP CycloneDX SBOM from the resolved dependency graph;
- generates a CocoaPods SBOM from the resolved pod lock/spec state;
- validates CycloneDX structure and component presence;
- verifies the expected project-specific MediaPipe pod version;
- checks podspec/lock/artifact drift;
- includes the optional ML model only when it is actually staged;
- verifies model integrity when the optional artifact is staged;
- uploads the two SBOM documents as reproducible workflow evidence.

This is the right mechanical foundation. A second dependency scanner would add mechanism without addressing the commercial gap.

## Current Gradle SBOM findings

The reviewed Gradle SBOM contains:

```text
449 total components
446 components with license metadata
3 components without license metadata
```

The unresolved entries are:

```text
dev.icerock.moko:fetch2:0.7.2
dev.icerock.moko:fetch2core:0.7.2
com.micrantha.bluebell:bluebell:unspecified
```

The local Bluebell gap is expected to be resolvable from the Apache-2.0 provenance evidence already recorded in:

- `docs/commercial/provenance-evidence.md`
- `docs/commercial/bluebell-provenance-delta.md`

Do not merely hard-code `Apache-2.0` into generated output without making the build/module metadata or reviewed attribution input the durable source.

The two `fetch2` entries require source/project metadata review before assigning a license identifier. A missing CycloneDX license field is not evidence that the dependency is unlicensed; it is evidence that the current SBOM output does not carry the fact needed by the commercial review.

## Current CocoaPods SBOM findings

The reviewed CocoaPods SBOM contains five components and all five carry license metadata:

```text
EyespieMediaPipeTasksGenAI    0.10.26.2  Apache
EyespieMediaPipeTasksVision   0.10.26.2  Apache
EyespieMediaPipeTasksCommon   0.10.26.2  Apache
EyespieMediaPipeTasksText     0.10.26.2  Apache
Eyespie                       GPLv3
```

The current labels are useful but not normalized SPDX identifiers. Prefer normalized identifiers such as `Apache-2.0` / `GPL-3.0-only` or `GPL-3.0-or-later` only after confirming the exact intended license expression. Do not normalize `GPLv3` by guessing whether the repository means `only` versus `or-later`; the root license text/decision must drive the expression.

More importantly, a pod-level `Apache` label describes the project-specific pod component but does **not** prove that every nested framework, upstream binary, model, or third-party notice inside the XCFramework distribution has been accounted for.

## Resolved-graph observations

The reviewed Gradle SBOM gives a better picture than the version catalog alone:

- Mapbox is **not present** in this resolved SBOM snapshot;
- Google Play Services Location is present;
- MediaPipe GenAI/Vision dependencies are present;
- Supabase dependencies are present;
- Firebase Analytics itself is not present; three Firebase encoder transitives are present and carry Apache-2.0 metadata;
- the optional Gemma model is absent because it was not staged in this artifact.

These observations are build-snapshot facts, not permanent architecture claims. Future release candidates must be judged by their own resolved graph and packaged binary.

## Commercial evidence gap

The remaining gap is a **license/NOTICE/attribution evidence layer over the resolved SBOM**, with explicit inputs for artifacts the package manager cannot fully describe.

The pipeline should eventually answer:

```text
resolved component/artifact
  -> known license expression?
  -> required notice/attribution source?
  -> redistribution exception/restriction requiring review?
  -> included in exact release bundle?
  -> human-readable attribution output generated?
  -> unresolved item blocks commercial release?
```

## Recommended commercial gate

Add a release/commercial validation step that consumes the generated SBOMs rather than rescanning dependencies.

### 1. License metadata completeness

For each resolved component:

- require a reviewed SPDX-compatible license expression where technically available;
- permit a narrow checked-in exception record only when the generator cannot supply metadata;
- require each exception to name the component/version, evidence source, reviewed expression/status, and reason;
- fail on new `UNKNOWN`/missing entries not covered by the reviewed exception set.

The immediate initial exception backlog is the three components identified above.

### 2. NOTICE / attribution inventory

Maintain explicit attribution inputs for components/artifacts where license expression alone is insufficient.

Generate a deterministic human-readable artifact such as:

```text
THIRD_PARTY_NOTICES.txt
```

or an equivalent packaged/linked attribution document.

Generation should be driven from reviewed inputs and the **resolved** release component set so stale notices do not become the source of truth.

### 3. Custom MediaPipe bundle evidence

The project-specific CocoaPods components need an additional artifact-level provenance/notice input that can identify:

- project-specific pod name/version;
- exact release URL/checksum;
- XCFramework contents where relevant;
- upstream MediaPipe/TensorFlow/third-party notice sources;
- required attributions copied into the final distribution evidence.

Do not infer complete compliance from the root `ryjen/mediapipe` Apache-2.0 license or from one podspec `license` field.

### 4. Optional model evidence

If the optional Gemma `.task` is staged, the commercial gate must also require reviewed model-specific provenance/license evidence in addition to SHA-256 integrity.

At minimum record:

- exact model/version/source;
- applicable model terms/license;
- redistribution/bundling permission status;
- required attribution/restriction metadata;
- hash and release artifact identity.

If the model is absent, this model-specific gate should be non-applicable rather than inventing a shipped component.

### 5. Exact-candidate binding

The resulting evidence should bind to the release candidate:

```text
commit/release identity
SBOM artifact hashes
resolved component set
custom binary/model evidence hashes
THIRD_PARTY_NOTICES hash
validation conclusion
```

This keeps the commercial review reproducible and prevents a reviewed notice set from silently drifting away from the app actually submitted.

## Failure semantics

For closed alpha, #90 remains authoritative. This commercial license/notice layer is non-blocking unless it reveals a defect that also makes ordinary free distribution invalid or unsafe.

For a paid/commercial release, fail closed when:

- a newly resolved component has unknown/missing license status without a reviewed exception;
- required attribution/notice evidence is missing;
- an optional model is staged without reviewed redistribution evidence;
- a custom MediaPipe artifact cannot be tied to the reviewed provenance/notice set;
- the generated attribution artifact does not correspond to the exact release SBOM.

## Testing

The implementation should cover at least:

- current known-license SBOM passes;
- one unreviewed missing-license component fails;
- reviewed exception for a generator metadata gap passes;
- adding a new unresolved component fails without changing code-specific allowlists;
- local Bluebell metadata is resolved from durable module/review metadata;
- custom MediaPipe attribution input is required for the project-specific pods;
- optional model absent => model license check is not applicable;
- optional model present without reviewed license/provenance => fails;
- deterministic notice output for the same resolved graph;
- release evidence changes when SBOM/component or notice inputs change.

## Immediate follow-up

1. Resolve the license metadata source for `fetch2` and `fetch2core`.
2. Make the local Bluebell module expose/associate its Apache-2.0 provenance/license metadata durably.
3. Define reviewed MediaPipe XCFramework NOTICE/provenance input.
4. Define optional model license/provenance input coordinated with #24.
5. Add the SBOM license/notice completeness validator and generated attribution evidence.

## Related work

- #24 — optional GenAI model delivery/provenance lifecycle.
- #72 — existing dependency/SBOM integrity foundation.
- #90 — closed-alpha/public-beta scope; this commercial layer is non-blocking.
- #93 — exact release identity and distribution evidence.
- #106 — commercial distribution/license launch gate.
- `docs/commercial/commercial-distribution-inventory.md`
- `docs/commercial/provenance-evidence.md`
- `docs/commercial/bluebell-provenance-delta.md`
