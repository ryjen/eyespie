# Commercial Distribution Inventory

## Status

Preliminary, stable evidence inventory for #106.

This document records repository facts and unresolved commercial-distribution questions that can be investigated before a paid launch. It is **not legal advice** and does not freeze Apple App Store or Google Play policy conclusions. Store-product classification and any material GPL/store compatibility conclusion must be re-verified against then-current official terms and, where necessary, qualified counsel before production billing or paid distribution.

This work is post-alpha and does not expand #90.

## Executive findings

The current repository evidence supports the following working conclusions:

1. **Eyespie is distributed under GPLv3 today.** Commercial use is not the same question as compatibility with a particular mobile-store distribution path; the exact iOS/Android commercial path remains a #106 launch gate.
2. **GitHub currently reports one human Eyespie contributor (`ryjen`) plus automation accounts**, but this is only repository metadata. It is not sufficient evidence by itself to prove complete relicensing authority or copyright provenance for every line, asset, generated artifact, copied component, or co-authored/squashed contribution.
3. **No CLA/DCO mechanism was found in the current repository search.** If an additional permission, exception, dual-license path, or license change is considered, contributor/provenance review remains necessary.
4. **Bluebell has an Apache-2.0 upstream boundary**, while Eyespie also contains a local `:bluebell` module. The provenance and modification relationship between the local module and the public/private Bluebell repositories should be reconciled before making license-boundary claims.
5. **`ryjen/mediapipe` is Apache-2.0, but the custom iOS distribution still contains upstream/transitive components whose notices and redistribution obligations must be verified from the actual shipped XCFramework/pod bundle.** A green SBOM/integrity check is useful evidence, not a substitute for a redistribution-rights review.
6. **The optional Gemma `.task` model is a distinct licensing/provenance item from MediaPipe.** The repository documents a Kaggle-hosted Gemma artifact but the current tree/search did not find the referenced tracked `models.lock.json`. Model-weight terms and the missing provenance/lock evidence must be resolved before bundling or commercially redistributing that artifact.
7. **Brand, app icons, screenshots, landing-page art, and Mission content need their own provenance/licensing records.** Open-source client licensing must not accidentally be treated as granting rights to Eyespie trademarks or separately licensed commercial content.
8. **Store rules remain deliberately deferred.** #115/#116 must not be activated merely from this inventory; #106 must still classify the selected commercial SKU(s) under then-current Apple/Google rules.

## Repository and license boundaries

| Component / boundary | Repository / location | Observed license or status | Commercial-distribution review |
| --- | --- | --- | --- |
| Eyespie application/client | `ryjen/eyespie` | GNU GPLv3 | Verify exact App Store/Play distribution path, source/redistribution obligations, contributor authority, and compatibility of shipped linked/bundled components. |
| Eyespie local Bluebell module | `ryjen/eyespie:bluebell` | Provenance to reconcile | Determine source relationship and local modifications relative to Bluebell repositories; preserve applicable notices/license information. |
| Public Bluebell | `hackelia-micrantha/bluebell` | Apache-2.0 | Reusable framework boundary; verify actual version/source incorporated into Eyespie. |
| Bluebell SDK | `hackelia-micrantha/bluebell-sdk` | Apache-2.0 | Verify whether/local code was copied from this repository and whether any private-only implementation enters distributed artifacts. |
| Project-specific MediaPipe fork/distribution | `ryjen/mediapipe` | Apache-2.0 observed at repo root | Review actual packaged upstream/transitive notices, frameworks, models, and redistribution terms rather than relying only on repository root license. |
| Project-specific iOS MediaPipe podspecs/artifacts | `iosApp/MediaPipePodspecs`, release artifacts | Project-specific distribution | Preserve artifact provenance, exact version, checksums, SBOM, upstream notices, and redistribution evidence for the bundle actually submitted. |
| Optional Gemma task model | `model-pack/MODEL_ARTIFACT.md` / downloaded `.task` | **Separate model terms unresolved here** | Verify model-weight license/terms, redistribution rights, required notices/attribution, and provenance before bundling/commercial redistribution. |
| Third-party KMP/mobile dependencies | Gradle/CocoaPods/SBOM | Mixed | Use generated SBOM/license evidence plus targeted review for restrictions/notice/attribution obligations. |
| Eyespie brand and content | app/site assets, Mission content | Project policy not yet formalized | Define trademark/brand and official/organization/user/creator content licensing independently of GPL client source. |

The table records repository observations, not legal compatibility determinations.

## Copyright and contributor provenance

### Current repository evidence

GitHub's contributor API currently reports:

- `ryjen` as the sole human account in the returned contributor set;
- Dependabot, Copilot SWE agent, and GitHub web-flow automation accounts;
- no additional anonymous contributor entry in the queried contributor endpoint.

The repository's current `CONTRIBUTING.md` and code search did not reveal a CLA, DCO, or equivalent contributor-license/sign-off mechanism.

### What this evidence does not establish

Before any relicensing, additional permission, exception, or dual-license decision, inspect repository history/provenance for at least:

- commits with `Co-authored-by` or equivalent attribution;
- squashed contributions whose original authors are not represented by contributor counts;
- externally copied/adapted snippets;
- vendor/generated source checked into the repository;
- AI-generated code/assets where applicable terms or provenance records matter;
- code copied from Bluebell or other Micrantha projects;
- imported graphics, icons, screenshots, data, fixtures, or media;
- any prior employer/client-owned material.

GitHub contributor counts are useful triage evidence; they are not a chain-of-title opinion.

## Bluebell boundary

Eyespie currently includes a local `:bluebell` module, while both `hackelia-micrantha/bluebell` and `hackelia-micrantha/bluebell-sdk` expose Apache-2.0 licenses in the repositories examined.

Before commercial distribution or a license change:

1. identify the exact upstream commit/history from which Eyespie's local Bluebell code was derived;
2. diff Eyespie-local changes from the relevant upstream repository;
3. identify copyright notices or NOTICE requirements that must remain with distributed source/binaries;
4. confirm whether any private `bluebell-sdk` implementation is actually distributed or merely used as development history/reference;
5. keep Eyespie product/domain code, Bluebell generic framework code, and separately licensed commercial content/assets as explicit boundaries.

Do not infer that Bluebell's Apache-2.0 license changes the GPLv3 license currently applied to the Eyespie repository as a whole, or vice versa. The combined/distributed work must be reviewed based on the components actually shipped.

## MediaPipe boundary

`ryjen/mediapipe` currently carries Apache-2.0 at its repository root. Eyespie's project-specific iOS podspecs point at project-built MediaPipe Tasks Vision artifacts, and `docs/release/mediapipe-ios-distribution.md` records why the project-specific distribution exists.

The existing integrity/SBOM work provides useful evidence for exact artifact identity. Commercial review still needs the actual release bundle to answer:

- which upstream MediaPipe/TensorFlow/Google components are contained in each XCFramework/pod;
- which third-party notices are required with binary redistribution;
- whether each bundled model/runtime asset has terms different from the MediaPipe source license;
- whether generated podspec metadata includes all required license/notice references;
- whether the exact artifact submitted to Apple/Google matches the reviewed checksums/SBOM.

A repository-level Apache-2.0 license must not be used as a blanket statement about unrelated model weights or every transitive binary packaged with the app.

## Optional model artifact

`model-pack/MODEL_ARTIFACT.md` documents an optional `gemma3-1b-it-int4.task` artifact of roughly 584 MB and a Kaggle MediaPipe/Gemma source URL.

Treat the model as a separate supply-chain and licensing object:

- record the exact source URL/version/model card;
- retain the accepted model terms/license applicable to the exact artifact;
- record whether redistribution inside an app/store bundle is permitted and under what conditions;
- capture required attribution/notice/restriction information;
- retain SHA-256 and provenance metadata independently from runtime source licensing;
- document whether download-on-demand and bundle-at-build have different obligations.

`MODEL_ARTIFACT.md` references `models.lock.json`, but the current repository tree/search did not find that tracked manifest. Resolve the missing lock/provenance evidence before treating the optional model as commercially distributable.

This item should coordinate with #24 rather than creating a second model-delivery lifecycle.

## Third-party dependency review

The Gradle version catalog currently declares a broad mobile/KMP dependency set including Bluebell, Apollo, Compose, MediaPipe GenAI/Vision, Koin, Ktor, kotlinx libraries, SQLDelight, Supabase, Firebase, Coil, Google Play Services Location, and Mapbox.

The catalog is an **inventory input**, not proof that every dependency is present in the production artifact or exercised by production code. Current code-index searches did not establish direct Firebase Analytics or Mapbox usage; verify the resolved release dependency graph and packaged artifacts before drawing conclusions about shipping/terms.

For the commercial review, classify resolved dependencies by:

- source license;
- binary redistribution terms;
- NOTICE/attribution requirements;
- service/API terms separate from code license;
- account/API-key restrictions;
- data/privacy implications;
- whether the dependency is actually present in the production release bundle;
- whether it is optional, debug/test-only, or runtime-required.

### Service-specific items worth explicit review

- **Mapbox:** code/library licensing is not the entire product/service agreement; verify attribution, token, map/tile/data, offline, and commercial-service terms if actually shipped/used.
- **Firebase Analytics:** determine whether it is actually linked/initialized in the production app. ADR-0007's preferred product-analytics architecture is first-party; dependency presence alone must not silently introduce a third-party analytics data path.
- **Google Play Services Location:** verify privacy disclosures and platform/service terms for actual location behavior.
- **Supabase:** distinguish open-source client-library licensing from hosted-service terms and backend data-processing obligations.
- **MediaPipe/ML runtimes:** review model artifacts independently from runtime/library licenses.

Use #72's SBOM evidence as the mechanically reproducible dependency baseline, then layer the legal/redistribution review over it.

## Brand, media, and content provenance

The repository contains or references project-facing assets including app icons, travel-mode/location graphics, Eyespie marks/banners, screenshots, website assets, and store/fastlane media.

Before a commercial launch, create provenance/evidence for each source asset category:

- author/creator or source;
- date/source file where useful;
- whether created in-house, commissioned, generated, adapted, or third-party;
- applicable license/assignment/permission;
- attribution requirements;
- trademark implications;
- whether it may be redistributed in source, binaries, website, and store listings.

Generated build derivatives need not each receive a separate provenance record when they can be traced deterministically to one reviewed source asset.

### Eyespie brand

Define explicitly that open-source client licensing does not, by itself, grant rights to use Eyespie trademarks/branding beyond whatever trademark policy is intentionally published.

Before commercial expansion, decide:

- owner of the Eyespie name/logo/marks;
- permitted community/fork use;
- official-versus-unofficial naming requirements;
- treatment of app-store listings and screenshots;
- whether organization-branded missions may use Eyespie marks and under what agreement.

### Mission and user content

Separate licensing terms are required for:

- official Mission/Adventure content;
- organization-authored/branded experiences;
- user-uploaded photos and clues needed for hosting/sharing/verification;
- future creator-authored content;
- future marketplace promotional images/descriptions.

The GPLv3 client license should not accidentally be presented as the license for separately authored commercial Mission content, user media, or Eyespie trademarks.

## Open-source client versus commercial boundary

The preferred commercial boundary remains:

```text
GPLv3 client / protocol-capable application
        |
        +--> separately governed official Mission/content catalog
        +--> hosted entitlement/commerce normalization
        +--> organization operations and support
        +--> moderation/safety operations
        +--> trademark/brand
        +--> future creator ecosystem
```

Important consequence: client-side content digests or signed entitlement snapshots improve correctness and official-client authorization behavior, but they are **not DRM**. A modified open-source client can alter client-side checks. Commercially sensitive operations therefore require appropriate server-side authorization/content-delivery boundaries rather than relying on obscurity or a writable local premium flag.

## Stable counsel/legal questions

These questions can be prepared now even though final advice may occur closer to launch:

1. Given the verified contributor/provenance history, who must consent to an additional permission, exception, dual-license option, or license change?
2. What is the correct licensing treatment of the Eyespie-local Bluebell module relative to the Apache-2.0 upstream repositories and the GPLv3 Eyespie work?
3. What notices/source obligations must accompany the exact custom MediaPipe XCFramework/pod artifacts distributed with Eyespie?
4. Are any linked/bundled dependencies incompatible with the intended GPLv3 distribution, or subject to terms that require architectural separation?
5. What are the redistribution conditions for the exact optional Gemma model artifact, and do store-bundled versus on-demand download paths differ?
6. What source-availability and installation-information obligations apply to the exact Android/iOS binaries planned for distribution?
7. What additional permission/exception, if any, is advisable for a practical mobile-store distribution path while preserving the intended open-source rights?
8. What trademark policy best preserves the ability to fork GPL client code without implying that forks may market themselves as official Eyespie products?
9. What licenses/permissions are required from players, organizations, and future creators to host/process/share mission photos, clues, and authored content?
10. Which legal/privacy documents must exist before paid Mission Packs, Private Events, or organization deployments are activated?

## Deferred time-sensitive store questions

Do not answer these permanently in this inventory. Re-verify against current official Apple/Google terms before #115/#116 and paid release:

- whether each selected Mission Pack SKU is digital content requiring a particular in-app purchase mechanism;
- classification of a Private Event host purchase;
- treatment of organization/B2B real-world-service access;
- purchase/restore requirements across iOS and Android;
- external/web purchase linking or steering restrictions;
- commission/fee/program eligibility;
- subscription rules if Passport is activated;
- creator marketplace/payment rules if that phase is ever activated;
- refund/revocation notification mechanisms and required consumer flows.

ADR-0006 supplies the platform-neutral entitlement/portability architecture. #106 supplies the then-current legal/product configuration.

## Evidence checklist before paid launch

### Ownership/provenance

- [ ] Inspect full Git history for human/co-authored/squashed contributions beyond the contributor endpoint.
- [ ] Resolve any copied/generated/vendor-code provenance requiring permission.
- [ ] Determine whether additional contributor permissions are required for the selected licensing strategy.
- [ ] Reconcile local `:bluebell` provenance and modifications.

### Dependencies/artifacts

- [ ] Capture the exact release SBOM/dependency graph for each paid-release candidate.
- [ ] Map required LICENSE/NOTICE/attribution obligations to the shipped bundle.
- [ ] Review custom MediaPipe binary/transitive redistribution evidence.
- [ ] Resolve Gemma model terms and restore/replace the missing tracked lock/provenance manifest if the model is shipped.
- [ ] Review map/location/analytics/service terms for dependencies actually enabled in production.

### Brand/content

- [ ] Record provenance for source brand/store/website assets.
- [ ] Publish/approve trademark and official-brand policy.
- [ ] Define official Mission/content license boundary.
- [ ] Define user/organization content grant needed for hosting, sharing, processing, moderation, and takedown.

### Distribution

- [ ] Obtain/document qualified review of the exact GPLv3 iOS distribution path.
- [ ] Obtain/document qualified review of the exact GPLv3 Android distribution path.
- [ ] Decide whether to retain GPLv3 unchanged or use an authorized additional permission/exception/other strategy.
- [ ] Re-verify first commercial SKU classification under current Apple/Google policies.
- [ ] Configure ADR-0006 cross-platform portability/restore/refund policy from that decision.

## Related work

- #24 — optional GenAI model delivery lifecycle.
- #72 — SBOM enforcement/evidence.
- #90 — closed-alpha/public-beta release; this inventory is non-blocking.
- #103 / ADR-0005 — Mission/Adventure content boundary.
- #104 / ADR-0006 — platform-neutral commerce/entitlement boundary.
- #105 / ADR-0007 — privacy-preserving product analytics.
- #106 — commercial-distribution/licensing launch gate tracked by this inventory.
- #115 — first production store adapters; blocked on current #106 classification.
- #116 — optional Private Event credit path; conditional on #106 product classification.
