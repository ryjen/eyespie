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
  -> signed .eyespie export
  -> ordinary user-controlled file transfer
  -> physical guesser device import + signature validation
  -> related guess / match
  -> unrelated guess / non-match
  -> progress persistence across relaunch
```

The application must not require a hosted account, backend configuration, remote inference, or network access for this flow.

## Required test context

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
- verify the candidate was produced from the recorded source SHA;
- verify #173/#174 software sharing is present in the build;
- use ordinary system document/share surfaces only — no app backend transport;
- confirm each device can launch and establish its device-local cryptographic identity;
- where #91 calibration reports are collected, use the pinned fixtures and current configured match policy rather than tuning the threshold during this test;
- note the current status of #18 and #125; this runbook does not substitute for those sign-offs.

After installation/setup, disable normal network connectivity for the core create/import/play proof. A user-selected transfer mechanism may itself use a network service, but the received `.eyespie` file must remain self-verifying and playable with application networking unavailable.

## Direction A — Android creator → iOS guesser

1. **Create on Android**
   - launch Eyespie;
   - record the Android local `PlayerId` suffix or another non-sensitive identity reference;
   - create a local game;
   - capture an outdoor target;
   - author a manual clue and creator-only expected answer;
   - confirm challenge creation completes locally.

2. **Relaunch creator before export**
   - terminate and relaunch Eyespie;
   - confirm the game/clue reloads;
   - confirm no target image is required to restore the challenge.

3. **Export**
   - export the game through the Android system document UI;
   - record the resulting `.eyespie` file byte size and SHA-256 outside the app;
   - do **not** attach the bundle publicly if its target embedding or clue should remain private.

4. **Transfer**
   - move the file to the iPhone using an ordinary user-controlled mechanism;
   - do not modify or repackage the file.

5. **Import on iOS**
   - use `Import .eyespie` and select the transferred document;
   - expect `Imported` on the first import;
   - repeat the same import and expect `AlreadyPresent`, not a duplicate or overwrite;
   - open the imported game and confirm the playable clue is present;
   - confirm no creator-only expected answer is exposed in guesser-facing UI.

6. **Guess on iOS**
   - capture a related guess and record match/non-match + configured policy identity;
   - capture an unrelated guess and record match/non-match + configured policy identity;
   - repeat several capture/guess operations and observe that work remains bounded.

7. **Relaunch guesser**
   - terminate and relaunch Eyespie;
   - confirm the imported game remains playable;
   - confirm best progress/matched state is retained.

## Direction B — iOS creator → Android guesser

Repeat the complete Direction A procedure with iOS as creator and Android as guesser. Use a newly authored game so this direction exercises iOS signing/export rather than merely re-exporting imported authority.

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
- on Android, confirm normal Eyespie capture does not create a gallery/MediaStore artifact;
- on iOS, cancel document import/export and confirm no game authority changes and no stale picker result is applied to a later operation.

## #91 embedding parity evidence

If collecting #91 calibration reports during the same device session, validate and compare them with the existing repository tool. The comparator consumes the configured release policy; it does not select a new threshold.

```bash
python3 scripts/compare_image_embedding_calibration.py validate <android-report.json>
python3 scripts/compare_image_embedding_calibration.py validate <ios-report.json>
python3 scripts/compare_image_embedding_calibration.py compare \
  <android-report.json> \
  <ios-report.json> \
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

- Android-created signed game imports and plays on physical iOS;
- iOS-created signed game imports and plays on physical Android;
- related/unrelated guesses behave predictably under the **already configured** match policy;
- imported game/progress survives relaunch in both directions;
- both directions demonstrate backend/account/network independence for core play;
- negative bundle checks fail closed without corrupting accepted state;
- camera/document cancellation and permission recovery are safe;
- #91 physical embedding evidence is attached/accepted;
- #18 backendless threat-model evidence and #125 telemetry/network evidence are accepted for the candidate;
- the scenario is repeated against the final closed-alpha candidate.

A failure moves or repairs the candidate. It does not justify weakening signature validation, bundle bounds, identity consistency, privacy/retention rules, or physical-device requirements.
