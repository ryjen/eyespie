# Image embedding contract

Eyespie release schema **v1** uses one canonical semantic image-vector representation across Android, iOS, local persistence, and Supabase.

This document defines the representation boundary. The exact immutable model artifact and production iOS MediaPipe Vision adapter remain follow-up work in #91 and must satisfy this contract before the closed alpha.

## Contract

| Property | Release schema v1 |
| --- | --- |
| Logical model | `mobilenet-v3-small-100-224-embedder` |
| Dimensions | exactly `1024` |
| Numeric type | IEEE-754 `float32` |
| Quantization | unsupported; production providers emit floats |
| Runtime normalization | no additional MediaPipe `l2Normalize`; use the model-native output |
| Similarity | cosine similarity |
| Local binary encoding | 4096 bytes, big-endian float32 |
| Supabase/Postgres encoding | `vector(1024)` / pgvector textual DTO form `[f1,f2,...]` |
| Schema version | `1` |

Cosine similarity computes its own magnitude normalization. Eyespie therefore does not mutate the model output through a second L2-normalization step in schema v1. Both platforms must use the same model artifact and MediaPipe options before their vectors are considered compatible.

## Boundary rules

- An embedding is canonical only when it contains exactly 1024 finite float values.
- Empty, short, oversized, NaN, infinite, malformed pgvector, and quantized-byte embeddings are rejected.
- Local byte order is explicit and must not depend on CPU/native endianness.
- pgvector text is a DTO/database representation only; it is not hex and must never be decoded as hex.
- SQLDelight stores the canonical 4096-byte binary representation in its BLOB columns.
- Supabase RPC requests carry exactly 1024 floats.
- Matching policy such as threshold and result count is not encoded in the vector schema.

## Android

The Android MediaPipe `ImageEmbedder` must:

- request non-quantized output;
- return exactly one embedding head;
- expose the supported float embedding API;
- reject any output that is not 1024 finite floats;
- close the per-inference `MPImage` after use.

Reflection into MediaPipe byte-embedding internals is not part of the supported contract.

## iOS

Production iOS composition currently remains a test double. The next #91 platform slice must:

- package or deterministically stage the exact same immutable model used by Android;
- construct `MPPImageEmbedder` from `EyespieMediaPipeTasksVision`;
- set quantization off and preserve the schema-v1 normalization policy;
- convert the existing app-owned camera image into an `MPPImage` without reintroducing borrowed camera-buffer lifetime;
- emit the same canonical 1024-float representation;
- fail deterministically for missing model, construction, image conversion, inference, and incompatible output.

## Model artifact identity — unresolved release gate

The current Android implementation names `mobilenet_v3_small_100_224_embedder.tflite`, but the repository contains no corresponding `.tflite` asset and the build-configuration action does not inject one.

Before production Vision is considered available on either platform, #91 must record and enforce:

- immutable artifact source/release identity;
- SHA-256 digest;
- expected byte size;
- MediaPipe/runtime compatibility;
- deterministic Android and iOS packaging/staging location;
- clean-checkout and produced-application evidence that the artifact is present;
- physical-device construction and representative inference.

A filename alone is not model identity.

## Compatibility and migration

Schema v1 rejects incompatible vectors rather than guessing their representation.

Any future model or representation change must explicitly decide whether it is vector-compatible. If not, it requires a new schema/model identity and a migration or recomputation strategy for stored challenge embeddings. A package version change must not silently reinterpret existing vectors.

## Current backend

The current Supabase migration defines `things.embedding vector(1024)` and `match_things(query_embedding vector(1024), ...)`. That dimension is the authoritative backend boundary for schema v1.

## Release validation still required

This representation slice is necessary but not sufficient to close #91. Closed-alpha evidence still requires:

1. the exact model artifact packaged/staged on both platforms;
2. production iOS MediaPipe Vision instead of `DeterministicImageEmbeddingGenerator`;
3. representative Android/iOS fixture comparison;
4. physical-device Vision task construction and inference;
5. backend create/match round trips using both platforms;
6. SBOM/release documentation identifying the model and runtime versions.
