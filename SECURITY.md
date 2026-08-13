# Security Policy

## Reporting a vulnerability

Security vulnerabilities should be reported privately rather than through a public issue when disclosure could expose users, credentials, private data, or an exploitable weakness.

Contact:

- **Email:** [security@micrantha.com](mailto:security@micrantha.com?subject=Eyespie%20Security%20Vulnerability)
- **Subject:** Include `Eyespie Security` and a short description.

Useful report details include:

- affected version/commit/platform;
- clear impact;
- reproducible steps or a minimal proof of concept;
- relevant logs with secrets/private data removed;
- suggested mitigation, if available;
- contact information for follow-up, if desired.

Do not include access tokens, passwords, private image captures, precise user locations, signed URLs, or unrelated personal data in a report unless a secure exchange has been arranged and the information is necessary to reproduce the issue.

## Supported release state

Eyespie is under active development toward the staged release tracked by issue #90. Security support and release claims follow the verified candidate rather than an aspirational feature list.

## Security and privacy architecture

The closed-alpha baseline is documented in:

- [`docs/security/privacy-threat-model.md`](docs/security/privacy-threat-model.md)
- [`docs/architecture/security-and-privacy.md`](docs/architecture/security-and-privacy.md)
- [`docs/architecture/semantic-game-engine.md`](docs/architecture/semantic-game-engine.md)

The baseline treats raw images, exact location, embeddings, hidden clue/answer evidence, account identifiers, signed image URLs, local private paths, and model artifacts as sensitive/security-relevant data.

### Current verified controls

Controls already implemented or explicitly evidenced include:

- Supabase authentication with the application client configured for PKCE flow;
- row-level security policies in the Supabase migration history, with further least-privilege tightening tracked by #122 and #126;
- HTTPS-based Supabase/provider clients through their platform libraries rather than a project-specific plaintext transport path;
- downloaded AI model SHA-256 verification after download and immediately before model initialization (#9);
- project-specific MediaPipe artifact/SBOM integrity validation in CI (#72/#89);
- bounded camera-frame ownership/lifecycle work for Android/iOS (#11/#97);
- local-first semantic-provider architecture with remote execution denied by default unless capability/policy/consent/minimization gates succeed.

Do not infer additional controls such as certificate pinning, root/jailbreak detection, universal rate limiting, encrypted local databases/backups, or a specific TLS protocol version unless the release candidate provides and verifies them.

### Open alpha-critical security/privacy work

The baseline threat review identified current implementation gaps that must remain visible during release work:

- #122 — restrict `Thing`/image/location/embedding/proof access and stop persisting long-lived signed image bearer URLs;
- #123 — isolate GenAI request/session context and remove raw clue/answer logging;
- #124 — minimize raw camera-capture retention and remove unintended Android MediaStore copies;
- #125 — verify actual MediaPipe runtime telemetry/network behavior and align disclosures;
- #126 — separate public Player profile data from private account/location state;
- #91 — complete canonical model/version-aware embedding behavior.

These issues are not replaced by this policy document.

## Secure development rules

### Sensitive-data handling

Do not write the following to ordinary logs, analytics, crash metadata, issue comments, or CI output:

- raw image/frame data;
- embeddings/query vectors;
- exact location;
- hidden clue answers/raw model output;
- passwords/auth tokens/service credentials;
- signed image URLs or receipt/provider tokens;
- local private file paths;
- sensitive model-download URLs/headers;
- production database dumps or unrelated user data.

Prefer stable diagnostic codes and bounded operation context.

### Authorization

- Server/backend authorization is authoritative for cross-account data.
- Client-supplied player IDs, object IDs, paths, and role claims are untrusted.
- Public/social views should use explicit minimum-field projections rather than exposing authority rows by default.
- Adding a new database column must not silently make it visible through wildcard public projections.

### Camera and location

- Live frames remain transient by default.
- An explicit still capture should use app-private temporary storage and bounded cleanup.
- Saving to the user's photo library is a separate explicit product action, not an internal capture side effect.
- Exact location is purpose-limited and must not be exposed as generic social/profile state.
- Future AR/spatial maps, anchors, and pose trails are not persisted/uploaded by default (#8).

### AI/model inputs

- Downloaded model bytes must pass the #9 integrity contract before initialization.
- Independent inference operations must not inherit prior image/prompt/session context implicitly (#123).
- A failed/unavailable local provider must not silently broaden to remote image/location transmission.
- Embeddings are sensitive derived data and require explicit model/version compatibility (#91).

### Dependencies and supply chain

- Dependency and custom artifact identity is tracked through the existing SBOM/release workflows (#72/#93).
- Commercial license/NOTICE completeness is a separate evidence layer (#106/#121), not a replacement dependency scanner.
- Model/runtime license and provenance decisions are distinct from artifact-integrity checks.

## Testing expectations

Security-relevant changes should include the narrowest useful automated test at the appropriate layer.

For the alpha baseline this includes:

- RLS/storage cross-account authorization tests;
- safe public-vs-private projection tests;
- capture cleanup/cancellation tests;
- GenAI context-isolation and logging-redaction tests;
- malformed/wrong-version embedding/model failure tests;
- model-integrity tests;
- complete two-account physical-device E2E validation (#92);
- release-like MediaPipe telemetry verification on physical Android/iOS (#125).

Security tests should fail closed rather than disabling integrity/authorization to make a release pass.

## Disclosure and release claims

Public documentation, onboarding, privacy/store metadata, and release notes must describe the behavior of the **verified release candidate**.

In particular:

- `on-device inference` must not be described as `no third-party network processing` until #125 verifies vendor telemetry behavior;
- image/location visibility must not be described as private until #122/#126 policies are implemented and tested;
- optional/future AR, analytics, commercial, or remote-provider capabilities must not be presented as active unless they are actually enabled and reviewed.

Issue #94 owns final capability/privacy-claim reconciliation before public release.

## Commercial extension

The free/core threat model is intentionally separated from post-alpha commercial incentives. #107 extends the baseline for paid content, public geofenced UGC, entitlement/payment abuse, moderation, and future creator-payout fraud.

Commercialization must not weaken the core camera/location/account/model protections above.

## Maintenance triggers

Re-review security/privacy architecture when materially changing:

- camera/media sources;
- location/presence behavior;
- MediaPipe/model/provider versions;
- remote reasoning;
- Supabase RLS/storage policies;
- offline synchronization;
- persisted embedding/proof formats;
- public/geofenced content;
- monetization or creator publishing;
- AR/spatial persistence.

## Related tracking

- #18 — baseline privacy/security threat model.
- #90 — staged release gate.
- #91 — embedding/verification contract.
- #92 — physical-device two-player E2E.
- #93 — signed distribution/observability/rollback.
- #94 — truthful release/privacy claims.
- #107 — post-alpha commercial/UGC/fraud extension.
