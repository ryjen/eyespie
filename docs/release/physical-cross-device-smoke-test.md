# Physical Android ↔ iOS closed-alpha smoke test

Status: release-evidence procedure for #92. This document is **not evidence that the scenario has passed**.

Use this runbook against the exact closed-alpha candidate on representative physical Android and iOS devices. Simulator, mocked camera, and CI evidence are useful prerequisites but do not satisfy #92.

## Purpose

Prove the smallest complete backendless Eyespie flow in both platform directions:

```text
physical creator device
  -> local identity
  -> create target + manual clue
  -> on-device embedding
  -> local persistence
  -> signed .eyespie export/share
  -> native user-controlled transfer
  -> physical guesser device external open/import + signature validation
  -> related guess / match
  -> unrelated guess / non-match
  -> progress persistence across relaunch
```

The application must not require a hosted account, backend configuration, remote inference, or network access for this flow.

## Required test context

From the exact clean candidate checkout, render the candidate identity before installing/testing:

```bash
python3 scripts/release_candidate_identity.py render \
  --output /tmp/eyespie-candidate.json
```

Use that manifest as the canonical source for the candidate SHA, application version/build, persistence/bundle compatibility, MediaPipe versions, and image-embedding model identity recorded with the evidence.

Record these before testing:

| Field | Android | iOS |
|---|---|---|
| Candidate git SHA | | |
| App version/build | | |
| Device model / hardware identifier | | |
| OS version | | |
| MediaPipe runtime identity | | |
| Image-embedding model SHA-256 | | |
| Match-policy/version + threshold | | |
| Local install state | clean / existing | clean / existing |

Expected image-embedding model SHA-256 for the current v1 contract:

`f7b9a563cb803bdcba76e8c7e82abde06f5c7a8e67b5e54e43e23095dfe79a78`

Do not record private signing-key material, hidden expected answers, raw embeddings from real/private scenes, private filesystem paths, tokens, or raw target/guess images in issue evidence.

## Preconditions

Before starting the physical scenario:

- install the **same intended candidate** on both devices;
- verify the candidate was produced from the source SHA recorded in `/tmp/eyespie-candidate.json`;
- verify the installed Android/iOS version and build match that manifest;
- verify #299 native sharing, saving, external document opening, and local-game deep-link routing are present in the build;
- use ordinary system document/share surfaces only — no app backend transport;
- confirm each device can launch and establish its device-local cryptographic identity;
- where #91 calibration reports are collected, use the pinned fixtures and current configured match policy rather than tuning the threshold during this test;
- note the current status of #18 and #125; this runbook does not substitute for those sign-offs.

After installation/setup, disable normal network connectivity for the core create/import/play proof. A user-selected transfer mechanism may itself use a network service, but the received `.eyespie` file must remain self-verifying and playable with application networking unavailable.

## Native handoff and deep-link acceptance

Treat **share**, **save**, **external file open**, and **deep link** as separate platform behaviors. Passing one does not imply another passes.

On both platforms, verify:

1. **Share game** opens the native OS share sheet rather than a document-save picker.
2. Select at least one ordinary messaging/chat target available on the test devices and send the `.eyespie` attachment.
3. From the receiving device, tap the received `.eyespie` attachment rather than first opening Eyespie and choosing Import.
4. Confirm the OS launches or foregrounds Eyespie and reaches the normal verified import-preview flow before persistence.
5. Repeat once from a cold application state and once while Eyespie is already running/backgrounded.
6. Cancel a share and confirm no game authority changes and no stale completion appears later.
7. **Save game file** opens the native document/files destination and remains distinct from Share game.
8. Open the saved `.eyespie` from the platform file surface and confirm it reaches the same verified import-preview path.

The file association is hostile-input ingress. MIME type, filename, file extension, sending application, contact identity, AirDrop/Quick Share proximity, or URI authority must not bypass bounded bundle parsing, signature/schema/domain validation, verified preview, or explicit import confirmation.

### Local-game deep links

Use an already accepted local game on each device and exercise:

```text
eyespie://game/<GameId>
```

Verify all of the following on Android and iOS:

- cold launch routes to the already-local game;
- warm/backgrounded launch routes to the already-local game without corrupting the existing navigation stack;
- an unknown but syntactically valid `GameId` produces the bounded not-available/local-game-needed state and creates/downloads nothing;
- malformed scheme/host/path variants are rejected;
- query, fragment, user-info, port, or other decorated variants are rejected where the platform can deliver them;
- a deep link never carries `.eyespie` bytes, signatures, embeddings, hidden answers, or other portable-game authority.

Opening a `.eyespie` attachment and opening `eyespie://game/<GameId>` are deliberately different paths: the former is untrusted document import; the latter is navigation to authority already accepted locally.

## Direction A — Android creator → iOS guesser

1. **Create on Android**
   - launch Eyespie;
   - record the Android local `PlayerId` suffix or another non-sensitive identity reference;
   - create a local game;
   - capture an outdoor target;
   - author a manual clue and creator-only expected answer;
   - confirm challenge creation completes locally.

2. **Relaunch creator before handoff**
   - terminate and relaunch Eyespie;
   - confirm the game/clue reloads;
   - confirm no target image is required to restore the challenge.

3. **Share and save**
   - use **Share game** and confirm Android opens the system share chooser;
   - send the game through an ordinary messaging/chat target to the iPhone;
   - separately use **Save game file** and confirm Android opens the system document destination;
   - record one resulting `.eyespie` artifact byte size and SHA-256 outside the app;
   - do **not** attach the bundle publicly if its target embedding or clue should remain private.

4. **External open on iOS**
   - tap the received `.eyespie` attachment from the receiving app/file surface;
   - confirm iOS launches/foregrounds Eyespie and reaches verified import preview before persistence;
   - explicitly add the game and expect `Imported` on the first import;
   - repeat the same file/open and expect `AlreadyPresent`, not a duplicate or overwrite;
   - open the imported game and confirm the playable clue is present;
   - confirm no creator-only expected answer is exposed in guesser-facing UI.

5. **Deep-link the accepted iOS game**
   - exercise `eyespie://game/<GameId>` once from cold state and once while the app is running/backgrounded;
   - confirm both navigate only to the already-accepted local game;
   - exercise an unknown/malformed link and confirm it fails without creating/importing authority.

6. **Guess on iOS**
   - capture a related guess and record match/non-match + configured policy identity;
   - capture an unrelated guess and record match/non-match + configured policy identity;
   - repeat several capture/guess operations and observe that work remains bounded.

7. **Relaunch guesser**
   - terminate and relaunch Eyespie;
   - confirm the imported game remains playable;
   - confirm best progress/matched state is retained.

## Direction B — iOS creator → Android guesser

Repeat the complete Direction A procedure with iOS as creator and Android as guesser. Use a newly authored game so this direction exercises iOS signing/share/export rather than merely re-exporting imported authority.

Confirm iOS **Share game** presents `UIActivityViewController` targets such as Messages/AirDrop/installed providers as available on the device, while **Save game file** remains the separate document destination. On Android, tap/open the received attachment externally and repeat the cold/warm deep-link checks after the game is accepted.

An imported game must not be re-signed as though the importing device were the original creator.

## Required negative import checks

Perform these on disposable copies of non-sensitive test bundles. Every rejection must leave previously accepted local authority unchanged.

| Case | Expected result |
|---|---|
| Modified signed payload byte | Invalid/rejected; no persistence mutation |
| Modified signature byte | Invalid signature; no persistence mutation |
| Unsupported schema/version fixture | Unsupported/invalid; no persistence mutation |
| Oversized document (> 4 MiB) | Rejected by platform/common bounds |
| Same `GameId`, different valid portable content fixture | Explicit conflict; existing game retained |
| Exact same accepted bundle | `AlreadyPresent` / idempotent |

Also verify that malformed filenames do not affect imported authority: game identity must come only from verified file contents, not the external filename/path.

## Camera and lifecycle checks

On both physical devices:

- deny camera permission, attempt create/guess, and confirm recoverable UI;
- grant permission through the normal OS path and retry successfully;
- background/cancel during capture where practical and confirm the operation does not later complete unexpectedly;
- repeat target/guess capture cycles and look for unbounded concurrent work or monotonic app-owned temporary-capture growth;
- cancel native Share and Save surfaces and confirm no game authority changes or stale result is applied later;
- receive/open a second `.eyespie` while the app is foregrounded/backgrounded and confirm the external ingress is handled exactly once;
- on Android, confirm normal Eyespie capture does not create a gallery/MediaStore artifact;
- on iOS, confirm cancelled share/export material is cleaned and a cancelled document open does not affect a later operation.

## #91 embedding parity evidence

If collecting #91 calibration reports during the same device session, validate and compare them with the repository tool against the exact clean candidate manifest. Report schema v2 is intentionally candidate-bound: stale pre-reboot reports, model identities, application versions/builds, runtime versions, or embedding contracts fail before comparison. The comparator consumes the configured release policy; it does not select a new threshold.

```bash
python3 scripts/compare_image_embedding_calibration.py validate \
  <android-report.json> \
  --candidate-identity /tmp/eyespie-candidate.json

python3 scripts/compare_image_embedding_calibration.py validate \
  <ios-report.json> \
  --candidate-identity /tmp/eyespie-candidate.json

python3 scripts/compare_image_embedding_calibration.py compare \
  <android-report.json> \
  <ios-report.json> \
  --candidate-identity /tmp/eyespie-candidate.json \
  --json-output calibration/results/cross-platform.json \
  --markdown-output calibration/results/cross-platform.md
```

Do not commit private-scene embeddings just to satisfy this runbook. Use the pinned calibration fixtures/evidence format owned by #91.

## Evidence record per direction

Record a concise result for each Android→iOS and iOS→Android run:

| Field | Result |
|---|---|
| Candidate SHA/version/build | |
| Source device + OS | |
| Destination device + OS | |
| Bundle schema | |
| Bundle byte size | |
| Bundle SHA-256 | |
| Native Share sheet opens | pass/fail |
| Chat/attachment handoff | pass/fail + target category |
| Cold external attachment open | pass/fail |
| Warm external attachment open | pass/fail |
| Separate Save game file surface | pass/fail |
| Cold local-game deep link | pass/fail |
| Warm local-game deep link | pass/fail |
| Invalid/unknown deep link fails boundedly | pass/fail |
| Creator public-key/`PlayerId` consistency | pass/fail |
| Signature verification | pass/fail |
| Embedding model/dimension compatibility | pass/fail |
| Match policy identity | |
| First import | imported/fail |
| Repeated import | already-present/conflict/fail |
| Related guess | match/non-match + similarity if non-sensitive |
| Unrelated guess | match/non-match + similarity if non-sensitive |
| Relaunch preserves game/progress | pass/fail |
| Core flow with application networking unavailable | pass/fail |
| Permission/cancellation recovery | pass/fail |
| Bounded repeated capture/inference observation | pass/fail + note |

For failures, record the stable application diagnostic/result code and a minimal reproduction. Do not paste raw exception payloads if they contain paths or hostile file content.

## Exit criteria

Do not mark #92 complete until all of the following are true:

- Android-created signed game shares/opens/imports and plays on physical iOS;
- iOS-created signed game shares/opens/imports and plays on physical Android;
- native Share and Save surfaces remain distinct on both platforms;
- received `.eyespie` attachments reach verified import preview on cold and warm app launches;
- `eyespie://game/<GameId>` reaches already-local games on cold and warm launches on both platforms;
- unknown/malformed deep links fail safely without creating or downloading authority;
- related/unrelated guesses behave predictably under the **already configured** match policy;
- imported game/progress survives relaunch in both directions;
- both directions demonstrate backend/account/network independence for core play;
- negative bundle checks fail closed without corrupting accepted state;
- camera/share/document cancellation and permission recovery are safe;
- #91 physical embedding evidence is attached/accepted;
- #18 backendless threat-model evidence and #125 telemetry/network evidence are accepted for the candidate;
- the scenario is repeated against the final closed-alpha candidate.

A failure moves or repairs the candidate. It does not justify weakening signature validation, bundle bounds, identity consistency, privacy/retention rules, or physical-device requirements.
