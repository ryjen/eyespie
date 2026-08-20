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

Android upload signing material is stored in the private shared repository `ryjen/mobile-signing` on branch `android/com.micrantha.eyespie`.

That branch contains only SOPS-encrypted material:

- `upload-keystore.sops.json` — the Eyespie Android upload keystore encrypted as SOPS binary JSON;
- `signing.sops.yaml` — encrypted `store_password`, `key_alias`, and `key_password`.

Protected CI signing secrets:

- `ANDROID_SOPS_AGE_KEY` — the Eyespie-specific age private identity used to decrypt only Eyespie Android signing material;
- `MATCH_GIT_PRIVATE_KEY_B64` — base64 of the same read-only SSH deploy private key used to read `ryjen/mobile-signing`.

The Android job pins SOPS `3.13.3`, checks out only the Eyespie Android signing branch with the read-only deploy key, verifies GitHub using the published Ed25519 host key, decrypts signing material only under `$RUNNER_TEMP`, injects signing configuration into a temporary Gradle properties file, and removes the decrypted keystore, repository checkout, deploy key, known-hosts file, and Gradle signing properties in `always()` cleanup.

The job records the `mobile-signing` branch and commit SHA plus the SOPS version in the job summary. It does not print or persist decrypted signing values.

Use Google Play App Signing for Play-distributed builds. The key stored here is the upload key; the Play app-signing key remains managed by Google Play. Keep a separately protected recovery copy of the age identity and upload keystore authority outside GitHub Actions.

Publishing additionally requires:

- `GOOGLE_PLAY_JSON_KEY_B64` — base64 of a least-privilege Google Play service-account JSON key.

The job builds both release APK and AAB with Gradle signing injection. It verifies the final APK signature, records the signer certificate SHA-256, and inspects the package using Android SDK tooling. It fails closed unless:

- package ID is `com.micrantha.eyespie`;
- version name/build code equal the candidate manifest;
- the signer certificate digest is a valid SHA-256 identity;
- `android.permission.INTERNET` is absent.

The bounded evidence summary records only candidate/source identity, package/version/build, signer-certificate SHA-256, permission names, APK/AAB SHA-256 values, and the `play-internal` channel. `publish=true` uploads the already-validated AAB to the Play internal track through the pinned Fastlane version.

After validation succeeds, the Android job also retains the **exact validated APK and AAB** for physical qualification without using GitHub Actions artifact storage. It creates or refreshes a SHA-bound draft GitHub Release with tag:

```text
closed-alpha/android/<40-character-source-sha>
```

The draft contains the APK, AAB, `candidate.json`, bounded `release-evidence.json`, and `SHA256SUMS`. The retention step refuses to overwrite a non-draft release. The tag/release is qualification plumbing only: it is not a semantic version tag, public product release, Play publication, or production promotion. See `docs/release/candidate-retention.md` for the storage boundary and the optional R2/Dubnium-local alternatives.

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

The workflow pins Fastlane `2.235.0`. Match uses a separate private encrypted repository and does not alter candidate source/version metadata. Fastlane performs no semantic-version mutation, backend configuration, app login configuration, metadata submission, or production-track promotion. The separate Android candidate-retention step does create a `closed-alpha/android/<source-sha>` draft-release tag solely to retain the exact qualified binary outside Actions artifact storage.

Fastlane usage telemetry is explicitly disabled for these jobs.

## Evidence and secrets

Safe job-summary evidence is limited to:

- exact source SHA/candidate ID;
- app version/build;
- final package/bundle identity;
- Android signer certificate SHA-256;
- Android `mobile-signing` branch/commit and pinned SOPS version;
- iOS team/application-identifier entitlements;
- Android permission names;
- project MediaPipe artifact version on iOS;
- final artifact SHA-256 values;
- intended internal channel.

Never add keystore/certificate/profile contents, age private identities, Match passphrases/private deploy keys, API keys, passwords, Apple/Google private account payloads, private filesystem paths, user images, embeddings, clues/answers, `.eyespie` payloads, or environment dumps to release evidence or retained candidate releases.

Signing material is written only under runner-temporary/private locations. GitHub Actions logs and artifacts are not a credential transport.

## Running

From the GitHub Actions **Internal distribution** workflow, choose:

- exact `source_sha`;
- `android`, `ios`, or `all`;
- `publish=false` to build/validate signed candidates without store upload;
- `publish=true` to upload only after validation succeeds.

For Android, a successful protected run records the SHA-bound draft Release in the job summary. For physical #91/#92/#125 qualification, install the retained APK rather than rebuilding it locally when possible, and record the workflow run plus bounded evidence with the physical-device session. The installed build must have the same version/build and originate from the same candidate SHA. Physical behavior/network observations remain separate evidence.

## Re-verification triggers

Repeat affected signed-build and physical evidence when any of these change:

- candidate source SHA or app version/build;
- signing identity/provisioning profile or package/bundle identifier;
- image-embedding model/runtime identity;
- `.eyespie` compatibility or persistence schema;
- release workflow/Fastlane/SOPS version;
- requested Android permissions;
- any code that changes startup, inference, storage, sharing, or network behavior.
