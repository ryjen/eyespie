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

- `ANDROID_STORE_FILE_B64` — base64 of the release/upload keystore;
- `ANDROID_STORE_PASSWORD`;
- `ANDROID_KEY_ALIAS`;
- `ANDROID_KEY_PASSWORD`.

Publishing additionally requires:

- `GOOGLE_PLAY_JSON_KEY_B64` — base64 of a least-privilege Google Play service-account JSON key.

The canonical Android keystore must be backed up outside GitHub Actions. Do not commit a raw `.jks` / `.keystore` file to `ryjen/mobile-signing`; that repository is currently the Fastlane Match store for Apple signing material only.

The job builds both release APK and AAB with Gradle signing injection. It verifies the final APK signature, records the signer certificate SHA-256, and inspects the package using Android SDK tooling. It fails closed unless:

- package ID is `com.micrantha.eyespie`;
- version name/build code equal the candidate manifest;
- the signer certificate digest is a valid SHA-256 identity;
- `android.permission.INTERNET` is absent.

The bounded evidence summary records only candidate/source identity, package/version/build, signer-certificate SHA-256, permission names, APK/AAB SHA-256 values, and the `play-internal` channel. `publish=true` uploads the already-validated AAB to the Play internal track through the pinned Fastlane version.

## iOS

iOS signing identities are managed with Fastlane Match in the private shared repository `ryjen/mobile-signing`.

Repository layout:

- `main` — signing policy/documentation only;
- `FKL5L3E8N8` — encrypted Match material for Apple Developer team `FKL5L3E8N8`;
- future Apple Developer teams use their own branch.

Eyespie config is in `fastlane/Matchfile`. The bundle identifier remains `com.micrantha.eyespie` and the Apple Developer team remains `FKL5L3E8N8`.

Protected CI signing secrets:

- `MATCH_PASSWORD` — Fastlane Match encryption passphrase;
- `MATCH_GIT_PRIVATE_KEY_B64` — base64 of a read-only SSH deploy private key whose public key is installed on `ryjen/mobile-signing`.

The Match encryption passphrase and deploy private key are never committed to either repository. CI decodes the deploy key only into `$RUNNER_TEMP`, runs `match` in read-only mode, and removes the deploy key afterward. Fastlane `setup_ci` provides the temporary signing keychain.

### Bootstrap Match material

The first certificate/profile population must be done from an authorized Mac, not by CI. From a clean Eyespie checkout with Fastlane installed:

```sh
export MATCH_PASSWORD='<from password manager>'
fastlane match appstore --app_identifier com.micrantha.eyespie --team_id FKL5L3E8N8
```

Use normal authenticated GitHub access on that Mac so Match can write encrypted signing material to branch `FKL5L3E8N8`. Fastlane may require Apple Developer authentication when it must create or repair the Apple Distribution certificate/profile. Do not run the CI lane to bootstrap credentials: `ios sync_signing_ci` is intentionally read-only.

After bootstrap, create a dedicated SSH deploy key for CI and install only the **public** key on `ryjen/mobile-signing` with write access disabled. Base64 the private key locally and store that value as `MATCH_GIT_PRIVATE_KEY_B64` in the Eyespie `closed-alpha-internal` environment.

Publishing additionally requires App Store Connect API-key material:

- `APP_STORE_CONNECT_KEY_ID`;
- `APP_STORE_CONNECT_ISSUER_ID`;
- `APP_STORE_CONNECT_KEY_B64` — base64 `.p8` key content.

The job installs the Apple Distribution identity/profile from Match, installs CocoaPods through the same pinned model-staging path used by normal iOS builds, archives a Release build, and exports one IPA.

The archive command explicitly pins the store bundle ID `com.micrantha.eyespie` and uses the profile supplied by Match. The exported app signature is verified and its signed entitlements are inspected. Validation fails closed unless:

- bundle ID, marketing version, and build number match the candidate;
- `EyespieMediaPipeTasksVersion` matches the candidate;
- signed team identifier is `FKL5L3E8N8`;
- signed application identifier is `FKL5L3E8N8.com.micrantha.eyespie`.

The bounded summary records those safe signing identifiers plus the IPA SHA-256 and `testflight-internal` channel. `publish=true` uploads the already-validated IPA to TestFlight using App Store Connect API-key authentication. It does not enable external distribution or notify external testers.

## Fastlane scope

Fastlane is used for:

- read-only Apple certificate/profile synchronization through Match on CI;
- upload of an already-validated AAB to Play Internal;
- upload of an already-validated IPA to TestFlight.

The workflow pins Fastlane `2.235.0`. Match uses a separate private encrypted repository and does not alter candidate source/version metadata. No semantic-version mutation, backend configuration, app login credentials, git tagging, metadata submission, or production-track promotion is restored.

Fastlane usage telemetry is explicitly disabled for these jobs.

## Evidence and secrets

Safe job-summary evidence is limited to:

- exact source SHA/candidate ID;
- app version/build;
- final package/bundle identity;
- Android signer certificate SHA-256;
- iOS team/application-identifier entitlements;
- Android permission names;
- project MediaPipe artifact version on iOS;
- final artifact SHA-256 values;
- intended internal channel.

Never add keystore/certificate/profile contents, Match passphrases/private deploy keys, API keys, passwords, Apple/Google private account payloads, private filesystem paths, user images, embeddings, clues/answers, `.eyespie` payloads, or environment dumps to release evidence.

Signing material is written only under runner-temporary/private locations. GitHub Actions logs and artifacts are not a credential transport.

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
