# Closed-alpha candidate retention

The protected internal-distribution workflow must preserve the exact validated binary used for physical qualification without consuming GitHub Actions artifact storage.

## Default: draft GitHub Release assets

After Android signing and release-evidence validation succeed, CI retains the exact candidate as a **draft GitHub Release** keyed by the full source SHA:

```text
closed-alpha/android/<40-character-source-sha>
```

The draft contains:

- the validated release APK;
- the validated release AAB;
- `release-evidence.json`;
- `candidate.json`;
- `SHA256SUMS` covering all four retained files.

The release remains a draft. It is an internal qualification handoff, not a public/product release, Play publication, or a replacement for the physical gates in #91/#92/#125.

The workflow refuses to overwrite a non-draft release with the same tag. A rerun of the same candidate may refresh the assets only while the release remains draft.

This path deliberately does **not** use `actions/upload-artifact`, so the APK/AAB do not consume GitHub Actions artifact-storage quota. GitHub Release assets have their own release-asset limits; keep each retained file below GitHub's current per-file release-asset limit.

## Why this is the first choice

The release is already produced inside a protected GitHub environment and is bound to an exact public source commit. Draft release retention therefore adds no external storage credential and no ingress path into a developer machine.

It also preserves the important provenance chain:

```text
exact reviewed SHA
    -> protected signing material
    -> signed APK/AAB
    -> release-evidence validation
    -> draft release assets + SHA256SUMS
    -> physical-device qualification
```

Do not rebuild the APK locally for qualification when the retained CI APK is available. Install the retained APK so the physical evidence refers to the same binary that passed protected validation.

## Cloudflare R2 option

If candidate retention later needs an external object store, prefer a private **Cloudflare R2 Standard** bucket rather than a Pages site. R2 is object storage and exposes S3-compatible and Worker APIs; a web site is unnecessary for storing APK/AAB payloads.

A future R2 sink should preserve the same logical object layout:

```text
eyespie/closed-alpha/android/<source-sha>/
  eyespie-android-<short-sha>.apk
  eyespie-android-<short-sha>.aab
  candidate.json
  release-evidence.json
  SHA256SUMS
```

Requirements for an R2 sink:

- private bucket by default;
- narrowly scoped write credential only in `closed-alpha-internal`;
- lifecycle deletion for stale candidates;
- short-lived authenticated download URLs when a device needs the APK;
- no signing material, account payloads, user data, clues, images, embeddings, or `.eyespie` game payloads;
- verify retained object hashes against `SHA256SUMS` after upload.

Do not make R2 availability part of the backendless gameplay architecture. It is release engineering storage only.

## Dubnium-local option

A local Dubnium spool is also valid, but it should be implemented as a reviewed runner capability rather than by giving an ephemeral CI container an arbitrary host bind mount.

A future local sink should:

- use a dedicated host path such as `/var/lib/eyespie-candidates` or another explicitly managed persistent volume;
- expose only a write-only candidate-spool capability to the Eyespie JIT route;
- atomically stage by full source SHA;
- make the resulting files readable to the local operator/device-transfer path after the JIT container exits;
- enforce retention/size limits;
- never persist signing secrets or temporary decrypted keystores;
- keep the normal JIT runner filesystem ephemeral.

This is useful when qualification is performed physically near Dubnium and avoids external object storage entirely. It requires a Dubnium runner-policy change and is intentionally separate from the Eyespie application architecture.

## Cleanup

Draft candidate releases are disposable qualification material. Keep only candidates still needed for current physical evidence, rollback comparison, or release auditing. Delete obsolete draft releases/tags rather than promoting them to normal releases.

The bounded JSON evidence remains the durable record in the release issue; binaries are retained only as long as they are operationally useful.
