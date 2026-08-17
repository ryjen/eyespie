# Backendless internal distribution

Status: protected signed-build/upload procedure for #199 / #93. This workflow supplies release-like artifacts for physical #91/#92/#125 validation; running it does **not** itself satisfy those physical gates.

## Boundary

`.github/workflows/internal-distribution.yml` is manual-only (`workflow_dispatch`) and builds an exact 40-character source SHA. It does not generate runtime configuration, create accounts, contact Supabase, mutate `APP_VERSION` / `APP_BUILD`, increment store versions, or restore the pre-reboot deployment graph.

The workflow uses the GitHub environment `closed-alpha-internal`. Configure required reviewers/environment protection before adding secrets.

## Candidate identity

The caller supplies an exact `source_sha`. Each platform job:

1. checks out that SHA directly;
2. verifies `git rev-parse HEAD` is identical;
3. runs `scripts/release_candidate_identity.py verify`;
4. renders candidate-identity schema v2 from the clean checkout;
5. runs `scripts/internal_release_evidence.py verify-source` before signing/building.

Distribution never edits `iosApp/Configuration/Version.xcconfig`. A version/build change is a reviewed source change that produces a new candidate identity.

## Android

Protected signing secrets:

- `ANDROID_STORE_FILE_B64` — base64 of the release keystore;
- `ANDROID_STORE_PASSWORD`;
- `ANDROID_KEY_ALIAS`;
- `ANDROID_KEY_PASSWORD`.

Publishing additionally requires:

- `GOOGLE_PLAY_JSON_KEY_B64` — base64 of a least-privilege Google Play service-account JSON key.

The job builds both release APK and AAB with Gradle signing injection. It inspects the final APK using Android SDK `apkanalyzer` and fails closed unless:

- package ID is `com.micrantha.eyespie`;
- version name/build code equal the candidate manifest;
- `android.permission.INTERNET` is absent.

The bounded evidence summary records only candidate/source identity, package/version/build, permission names, APK/AAB SHA-256 values, and the `play-internal` channel. `publish=true` uploads the already-validated AAB to the Play internal track through the pinned Fastlane version.

## iOS

Protected signing secrets:

- `IOS_DISTRIBUTION_CERTIFICATE_P12_B64` — base64 Apple Distribution certificate/private-key PKCS#12;
- `IOS_DISTRIBUTION_CERTIFICATE_PASSWORD`;
- `IOS_PROVISIONING_PROFILE_B64` — base64 App Store provisioning profile for `com.micrantha.eyespie` / team `FKL5L3E8N8`.

Publishing additionally requires App Store Connect API-key material:

- `APP_STORE_CONNECT_KEY_ID`;
- `APP_STORE_CONNECT_ISSUER_ID`;
- `APP_STORE_CONNECT_KEY_B64` — base64 `.p8` key content.

The job imports signing material into an ephemeral runner keychain, validates the profile team/application identifier, installs CocoaPods through the same pinned model-staging path used by normal iOS builds, archives a Release build, and exports one IPA.

The archive command explicitly pins the store bundle ID `com.micrantha.eyespie`; the exported IPA is then inspected and fails closed unless bundle ID, marketing version, build number, and `EyespieMediaPipeTasksVersion` match the candidate manifest. The bounded summary records only those values plus the IPA SHA-256 and `testflight-internal` channel.

`publish=true` uploads the already-validated IPA to TestFlight using App Store Connect API-key authentication. It does not enable external distribution or notify external testers.

## Fastlane scope

Fastlane is deliberately upload-only. The workflow pins Fastlane `2.235.0`; no versioning plugins, `match`, semantic-version mutation, backend configuration, app login credentials, git tagging, metadata submission, or production-track promotion are restored.

Fastlane usage telemetry is explicitly disabled for these jobs.

## Evidence and secrets

Safe job-summary evidence is limited to:

- exact source SHA/candidate ID;
- app version/build;
- final package/bundle identity;
- Android permission names;
- project MediaPipe artifact version on iOS;
- final artifact SHA-256 values;
- intended internal channel.

Never add keystore/certificate/profile contents, API keys, passwords, Apple/Google private account payloads, private filesystem paths, user images, embeddings, clues/answers, `.eyespie` payloads, or environment dumps to release evidence.

Signing material is written only under runner-temporary/private locations and removed in `always()` cleanup steps. GitHub Actions logs and artifacts are not a credential transport.

## Running

From the GitHub Actions **Internal distribution** workflow, choose:

- exact `source_sha`;
- `android`, `ios`, or `all`;
- `publish=false` to build/validate signed candidates without store upload;
- `publish=true` to upload only after validation succeeds.

For #91/#92/#125, record the workflow run and bounded evidence summary with the physical-device session. The installed build must have the same version/build and originate from the same candidate SHA. Physical behavior/network observations remain separate evidence.

## Re-verification triggers

Repeat affected signed-build and physical evidence when any of these change:

- candidate source SHA or app version/build;
- signing identity/provisioning profile or package/bundle identifier;
- image-embedding model/runtime identity;
- `.eyespie` compatibility or persistence schema;
- release workflow/Fastlane version;
- requested Android permissions;
- any code that changes startup, inference, storage, sharing, or network behavior.
