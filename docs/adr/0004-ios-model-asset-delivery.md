# ADR 0004: iOS model asset delivery baseline

- Status: Accepted
- Date: 2026-07-24
- Issue: #24

## Context

EyesPie currently targets iOS 15 and needs to deliver an optional model of approximately 584 MB outside the base application bundle.

Apple now offers two materially different Background Assets generations:

1. The low-level, self-managed Background Assets APIs, where the app and a downloader extension schedule individual downloads and host the content themselves.
2. Managed Background Assets and Apple-Hosted Background Assets, where the system manages asset packs and Apple can host them separately from the application build.

Apple's current App Store Connect documentation states that Managed Background Assets require apps targeting iOS 26 or later. Moving EyesPie directly from iOS 15 to iOS 26 would remove support for every earlier iOS release solely to obtain the newest managed delivery API. That device-coverage cost is not justified by the current product requirements.

Apple-Hosted Background Assets also require an application extension, a shared App Group, Background Assets property-list configuration, asset-pack manifests, and App Store Connect/TestFlight asset-pack lifecycle management. Apple documents machine-learning models as a supported asset type and requires explicit removal when an installed pack is no longer needed.

## Decision

EyesPie will retain its iOS 15 deployment target for now.

The first production iOS delivery implementation will use a self-hosted background transfer behind the existing `ModelAssetRepository` contract:

- use low-level Background Assets where the running OS and project configuration support it;
- provide a background `URLSession` implementation for the retained iOS 15 compatibility floor;
- keep both implementations behind one repository and one immutable model descriptor;
- do not expose transport-specific states to shared UI code;
- do not silently fall back after an integrity, compatibility, or runtime-smoke-check failure;
- allow transport fallback only when the preferred transport is unavailable before download scheduling.

Managed or Apple-Hosted Background Assets may replace both transports in a future baseline migration to iOS 26 or later. That migration requires a separate product decision supported by device-coverage data.

### Background URLSession policy

The iOS 15-compatible transport uses a delegate-backed background `URLSession` with a stable identifier derived from the application bundle identifier.

- `isDiscretionary` is disabled because scheduling follows explicit user consent rather than an opportunistic maintenance download.
- Cellular, constrained, and expensive-network access are disabled because the future production artifact is approximately 584 MB.
- Connectivity waiting is enabled so temporary network unavailability maps to a queued/recoverable state rather than an immediate terminal failure.
- The session permits system relaunch events and limits itself to one active model task.
- The application retains the system background-session completion handler before reconstructing the session and invokes it only after the session delegate reports that all queued events have been delivered.
- Completed transfers are moved from the system temporary URL into a fixed app-owned staging directory. Server-provided filenames are ignored, staging filenames are validated, and replacement is atomic where supported.
- Transfer completion enters `Verifying`; staging alone never produces `Ready`.

The first transport slice uses a small immutable HTTPS fixture from this repository, pinned to commit `745fc97a149fcd67f4a673fd5612f972ec874126`. Its expected byte count and SHA-256 are recorded in a test-only configuration that is separate from future production model configuration. No model URL, credential, or approximately 584 MB artifact is included.

## Shared lifecycle mapping

Platform observations map to the shared lifecycle as follows:

| Platform observation | Shared state |
|---|---|
| Supported and configured, no verified local model | `NotInstalled` |
| User has not confirmed the large optional download | `AwaitingConsent` |
| Scheduled but waiting for platform/network/storage | `Queued` with the applicable reason |
| Transfer progress available | `Downloading` |
| Transfer complete; digest verification running | `Verifying` |
| Digest, compatibility, and runtime smoke check pass | `Ready` |
| OS cannot support any configured transport | non-recoverable compatibility failure |
| Transport is supported but project configuration is incomplete | non-recoverable scheduling/configuration failure |
| Transfer interruption or temporary network failure | recoverable download failure |
| Digest mismatch | recoverable verification failure after removing the corrupt artifact |

The repository must never report `Ready` from file presence alone.

## Artifact identity and storage

- The iOS artifact uses the same immutable descriptor fields as Android: model ID, version, filename, exact byte count, SHA-256, runtime engine, minimum runtime version, and model ABI.
- Downloads are moved into app-owned staging after transfer and atomically promoted to final storage only after verification.
- The verified artifact is stored in an application-controlled directory that is excluded from iCloud backup.
- Partial, corrupt, obsolete, and superseded artifacts are removed deterministically.
- `remove()` deletes the verified artifact and transport bookkeeping, then returns the repository to `NotInstalled`.

## Testing strategy

The implementation must include:

- pure mapping tests for supported, unsupported, and misconfigured capabilities;
- repository tests for interruption, retry, cancellation, checksum mismatch, removal, and relaunch restoration;
- deterministic coordinator/state-machine tests for scheduling, restoration, stale callbacks, progress, staging, cleanup, and background completion handling;
- simulator validation using the controlled immutable HTTPS fixture without downloading the production model;
- physical-device tests for background execution and process termination;
- TestFlight validation before adopting Apple-hosted asset packs in a future iOS 26 migration.

No production model or credentials are added by this ADR slice.

## Consequences

### Positive

- Preserves current iOS device coverage.
- Keeps shared lifecycle semantics aligned with Android.
- Avoids coupling the first iOS implementation to an iOS 26-only managed service.
- Leaves a clear migration path to Apple-hosted asset packs.

### Negative

- Requires maintaining a compatibility transport until the minimum iOS version reaches the managed Background Assets baseline.
- Self-hosting requires a durable HTTPS origin, release metadata, and operational monitoring.
- Full background-transfer behavior still requires an extension/project configuration slice and physical-device validation.

## Follow-up slices

1. Add the iOS repository shell and capability detection without network transfer.
2. Implement verified background `URLSession` download for iOS 15 compatibility.
3. Add low-level Background Assets scheduling where available and configured.
4. Add extension/App Group configuration and physical-device lifecycle tests.
5. Revisit Apple-Hosted Managed Background Assets when an iOS 26 minimum is acceptable.
