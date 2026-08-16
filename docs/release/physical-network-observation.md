# Physical release-candidate network observation

Status: evidence procedure for #125. This document defines how to collect and report candidate network behavior; it is **not evidence that network traffic is absent**.

Eyespie closed-alpha core gameplay is designed to be backendless. That architecture claim and Android's intended no-`INTERNET` package boundary are useful controls, but neither is a substitute for observing the exact installed Android/iOS candidate and project-specific MediaPipe runtime on representative physical devices.

## Evidence question

The release question is deliberately narrower than “prove the entire phone is network silent”:

> During controlled Eyespie launch, MediaPipe initialization/inference, lifecycle, and offline gameplay scenarios, what network activity is observable from the test device, what can reasonably be attributed to Eyespie or its packaged runtime, and what cannot be concluded from the capture method?

Use three distinct evidence classes:

1. **static candidate evidence** — package permissions/configuration/dependency identity;
2. **transport metadata** — DNS queries, IP connections, ports, timing, byte counts;
3. **payload evidence** — only when controlled TLS interception or an equivalent method is justified and successful.

Do not infer payload content from endpoint metadata alone.

## 1. Bind the observation to one exact candidate

From the exact clean source commit used to build/install the physical candidate:

```sh
python3 scripts/release_candidate_identity.py render \
  --output /tmp/eyespie-candidate.json
```

Record:

- candidate identity string and full source SHA;
- app version/build;
- Android/iOS device + OS;
- Android MediaPipe Tasks version or project-specific iOS artifact version;
- image-embedding model ID/SHA-256;
- capture date/time/timezone;
- network topology/capture point;
- device IP/MAC or other non-public test-network identifier used only to filter the capture.

Do not include private keys, images, embeddings, clues/hidden answers, bundle payloads, private app paths, tokens, personal account identifiers, or unrelated user traffic in issue evidence.

## 2. Use a controlled device and capture point

Prefer a dedicated test device or a device temporarily stripped of unrelated background activity. Put it on a controlled Wi-Fi/VLAN/hotspot where traffic can be observed **at a gateway/access-point layer that all device traffic traverses**.

The capture point should let the tester filter packets by the test device without relying on the app honoring an HTTP proxy.

Record enough topology to interpret a negative observation, for example:

```text
physical iPhone/Android
  -> dedicated test Wi-Fi
  -> controlled gateway/capture interface
  -> Internet
```

Before Eyespie testing, capture a short idle-device baseline. If the device emits unrelated OS/account/background traffic, identify it as background noise or restart with a cleaner test device/session rather than retaining unrelated payloads.

### Proxy limitation

A system HTTP(S) proxy can be useful for controlled attribution, but **proxy-only silence is not proof of no network traffic**. Native/vendor code can use transports that bypass the proxy.

If a proxy is used, pair it with gateway-level observation or explicitly mark the result as proxy-scoped/inconclusive.

### TLS interception limitation

TLS interception is not required for the first release question if endpoint/timing metadata is sufficient to show whether candidate-attributable connections exist.

Use controlled TLS interception only when payload classification is needed to resolve a release/privacy decision. If used:

- use non-sensitive controlled Eyespie fixtures/scenarios;
- use a dedicated test CA/device profile;
- document certificate-pinning or protocol failures;
- do not weaken production trust settings in the application to make interception succeed;
- remove the test CA/profile after collection;
- retain only minimized evidence needed for #125/#18/#94.

Failure to decrypt a connection says nothing about the payload contents.

## 3. Collect static package evidence

Static evidence supports attribution; it does not replace physical observation.

### Android

Inspect the **final installed/candidate APK**, not only the source manifest, for merged permissions. Confirm whether `android.permission.INTERNET` is present and record the result.

The active source manifest intentionally requests camera only, but dependencies can contribute manifest entries; release evidence should therefore inspect the packaged artifact with Android Studio APK Analyzer or an equivalent Android SDK package-inspection tool.

If `INTERNET` appears unexpectedly, treat that as a release investigation before relying on traffic captures.

### iOS

Record the exact app build and project-specific MediaPipe artifact identity from the candidate manifest. iOS has no Android-equivalent `INTERNET` permission boundary, so runtime observation carries more weight.

Record any candidate-specific network entitlements/configuration that materially affect interpretation. Do not equate absence of an App Transport Security exception with absence of networking.

## 4. Capture a baseline

With network connectivity enabled and the candidate installed but Eyespie not running in foreground:

1. start gateway capture;
2. leave the device idle for a fixed short interval;
3. note existing OS/background endpoints and traffic classes;
4. stop or mark the baseline boundary;
5. avoid retaining unrelated payload data.

Use the same device/capture filter for the subsequent Eyespie intervals.

## 5. Exercise the required candidate scenarios

Record explicit start/end timestamps or capture markers for each interval.

### A. Cold launch before camera/model use

- force-quit Eyespie;
- start capture interval;
- launch Eyespie;
- do not enter a camera/inference action;
- observe launch/idle traffic;
- end interval.

### B. First embedding initialization/inference

Using a controlled non-sensitive target/guess:

- enter the first operation that initializes the production image-embedding path;
- perform one inference;
- record candidate-attributable DNS/connections/bytes, if any.

### C. Repeated inference

- perform several normal target/guess embedding operations through the production path;
- observe whether traffic appears only on first initialization, every inference, periodically, or not at all under the tested conditions.

Do not export real user-scene embeddings or images as network evidence.

### D. Background/foreground lifecycle

After the embedder has been initialized:

- background Eyespie;
- leave it backgrounded for a bounded interval;
- foreground it;
- perform another controlled inference;
- note traffic around lifecycle transitions.

### E. Offline/no-network core behavior

Disable normal network connectivity after installation/setup and exercise the backendless create/guess path expected by #92:

- launch app;
- create/open local game state;
- capture/embed a controlled target/guess;
- perform local matching;
- where practical, import/play a previously transferred `.eyespie` file.

Record whether core behavior succeeds without network. A failure here is a release defect even if no telemetry traffic was observed in the online scenarios.

### F. Vendor/runtime telemetry control, if applicable

If the shipped MediaPipe/runtime documents or exposes a telemetry/metrics disable/opt-out mechanism that Eyespie actually uses or could use, repeat the relevant scenario with the reviewed configuration and record the observable difference.

If no such app/runtime control exists, record that rather than inventing one.

## 6. Attribute conservatively

For each observed flow, record only what can be supported:

| Field | Example classification |
|---|---|
| Scenario interval | first inference |
| Destination hostname/IP | observed DNS hostname or IP |
| Transport | TCP/UDP/QUIC/other observed metadata |
| Port | observed value |
| Timing | relative to scenario marker |
| Approx bytes | coarse count if useful |
| Attribution | Eyespie/runtime likely / OS/background likely / unknown |
| Confidence | high / medium / low |
| Payload classification | unknown unless directly observed |
| Notes/limitations | proxy bypass, encrypted, shared endpoint, etc. |

A connection to a vendor-owned hostname can support endpoint attribution; it does **not** by itself prove which Eyespie fields, if any, were transmitted.

Conversely, “no candidate-attributable connection observed” means only that the chosen capture method/scenarios did not observe one. State the capture scope and limitations with the conclusion.

## 7. Evidence outcomes

Use one of these bounded conclusions per platform/candidate:

- **Observed candidate-attributable traffic** — endpoint/traffic class recorded; disclosure/consent/control decision required.
- **No candidate-attributable traffic observed under tested scenarios** — capture topology and limitations recorded; do not upgrade this to a universal impossibility claim.
- **Inconclusive** — capture method/noise/attribution did not support a release conclusion; improve the test before #125 sign-off.

If traffic is observed, investigate whether it is application code, MediaPipe/runtime, model/download behavior, OS service, diagnostics, or another dependency before changing public claims.

## 8. #125 evidence record

Attach a minimized summary to #125 containing:

```text
Candidate: <version+build@sha>
Full source SHA: <sha>
Platform/device/OS: <controlled physical device>
MediaPipe/runtime: <candidate identity>
Model: <id + sha256>
Capture topology: <gateway/AP/proxy details>
Capture intervals: <scenario timestamps>
Static package evidence: <Android final INTERNET permission present/absent; iOS relevant config>
Observed candidate-attributable traffic: <none observed / table>
Offline core play: <pass/fail>
Telemetry control tested: <yes/no/not exposed>
Limitations: <capture/decryption/attribution limitations>
Conclusion: <observed / none observed under tested scenarios / inconclusive>
```

Do not attach raw packet captures publicly when they contain unrelated device traffic or identifiers. Retain minimized/redacted evidence according to the release/security evidence policy.

## 9. Disclosure/security reconciliation

After both physical platforms are tested:

- update #125 with the actual observation;
- reconcile #18's data-flow/threat model with the evidence;
- reconcile #94 README/site/store/privacy wording;
- distinguish application-controlled backendless behavior from any verified vendor/runtime telemetry;
- do not describe retired Supabase/account/upload flows as active.

If consent would be required for observed traffic before Eyespie can present the relevant education/choice boundary, treat it as a release defect or disable the affected capability.

## Re-verification triggers

Repeat the affected physical observation when changes can alter network behavior, including:

- MediaPipe Tasks/runtime version change;
- project-specific iOS MediaPipe artifact rebuild/version change;
- model delivery mechanism change;
- new diagnostics/crash/product analytics;
- new networking library/permission/entitlement;
- new remote inference/cloud/P2P capability;
- material OS/platform integration change;
- release candidate source/build identity change after evidence was collected.

A source-only documentation change that cannot alter packaged/runtime network behavior does not by itself invalidate prior traffic evidence, but the final #90 candidate must still be traceable to accepted #125 evidence.
