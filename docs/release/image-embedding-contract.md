# Image embedding contract

Eyespie release schema **v1** uses one canonical semantic image-vector representation across Android, iOS, local persistence, and Supabase.

The vector representation and immutable model artifact are now explicit build contracts. Production iOS MediaPipe Vision inference and cross-platform/device validation remain follow-up work in #91 before closed alpha.

## Contract

| Property | Release schema v1 |
| --- | --- |
| Logical model | `mediapipe-mobilenet-v3-small-100-224-embedder-v1` |
| Model file | `mobilenet_v3_small_100_224_embedder.tflite` |
| Dimensions | exactly `1024` |
| Numeric type | IEEE-754 `float32` |
| Quantization | unsupported; production providers emit floats |
| Runtime normalization | no additional MediaPipe `l2Normalize`; use the model-native output |
| Similarity | cosine similarity |
| Local binary encoding | 4096 bytes, big-endian float32 |
| Supabase/Postgres encoding | `vector(1024)` / pgvector textual DTO form `[f1,f2,...]` |
| Schema version | `1` |

Cosine similarity computes its own magnitude normalization. Eyespie therefore does not mutate the model output through a second L2-normalization step in schema v1. Both platforms must use the same model artifact and MediaPipe options before their vectors are considered compatible.

## Immutable model artifact

The release model is pinned by `models/image-embedder.json`. Generated model bytes are deliberately not committed; clean builds reproduce the artifact from immutable provenance and fail closed unless both size and digest match.

| Property | Pinned value |
| --- | --- |
| Model ID | `mediapipe-mobilenet-v3-small-100-224-embedder-v1` |
| File | `mobilenet_v3_small_100_224_embedder.tflite` |
| Byte size | `6,116,906` |
| SHA-256 | `f7b9a563cb803bdcba76e8c7e82abde06f5c7a8e67b5e54e43e23095dfe79a78` |
| MediaPipe manifest repository | `ryjen/mediapipe` |
| MediaPipe manifest revision | `0ad5a71bcdff3d756dc5b07f93765aaeb4152538` |
| Manifest path | `third_party/external_files.bzl` |
| Manifest entry | `com_google_mediapipe_tasks_testdata_vision_mobilenet_v3_small_100_224_embedder_tflite` |
| Source object | generation-pinned `storage.googleapis.com/mediapipe-assets/tasks/testdata/vision/...` object, generation `1782184982945130` |
| Source-repository license evidence | `tensorflow/tflite-support`, Apache-2.0 |

The source-repository SPDX value records the available upstream repository evidence; distribution/release review must continue to preserve applicable license/NOTICE evidence rather than treating a filename or repository license alone as provenance.

`scripts/stage_image_embedder_model.py` accepts only the approved HTTPS GCS host/path with exactly one numeric generation pin. It validates manifest structure, expected dimension, byte size, and SHA-256; installation is atomic and the installed bytes are re-verified.

## Product packaging evidence

### Android

The mandatory model is staged to:

`eyespie/src/androidMain/assets/mobilenet_v3_small_100_224_embedder.tflite`

The Android bundle gate builds the application, requires exactly one final AAB entry at:

`base/assets/mobilenet_v3_small_100_224_embedder.tflite`

and extracts/re-hashes that packaged entry against the same manifest. The first provenance-slice CI proof measured exactly `6,116,906` bytes and verified the product digest.

### iOS

The same verified bytes are staged to:

`iosApp/ModelArtifacts/mobilenet_v3_small_100_224_embedder.tflite`

`EyespieImageEmbedderModel` is a local resource-only CocoaPod. CocoaPods stages the resource into the application product, and the generated `Verify Eyespie Image Embedder Model` end-of-app-build phase requires exactly one packaged model and re-verifies its byte size and SHA-256. This is a packaging contract only; production `MPPImageEmbedder` wiring remains the next #91 platform slice.

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

- use the pinned artifact above;
- request non-quantized output;
- return exactly one embedding head;
- expose the supported float embedding API;
- reject any output that is not 1024 finite floats;
- close the per-inference `MPImage` after use.

Reflection into MediaPipe byte-embedding internals is not part of the supported contract.

## iOS

Production iOS composition currently remains a test double. The next #91 platform slice must:

- construct `MPPImageEmbedder` from `EyespieMediaPipeTasksVision` using the pinned packaged model;
- set quantization off and preserve the schema-v1 normalization policy;
- convert the existing app-owned camera image into an `MPPImage` without reintroducing borrowed camera-buffer lifetime;
- emit the same canonical 1024-float representation;
- fail deterministically for missing model, construction, image conversion, inference, and incompatible output.

## Compatibility and migration

Schema v1 rejects incompatible vectors rather than guessing their representation.

Any future model or representation change must explicitly decide whether it is vector-compatible. If not, it requires a new schema/model identity and a migration or recomputation strategy for stored challenge embeddings. A package version change must not silently reinterpret existing vectors.

## Current backend

The current Supabase migration defines `things.embedding vector(1024)` and `match_things(query_embedding vector(1024), ...)`. That dimension is the authoritative backend boundary for schema v1.

## Release validation still required

The representation and artifact-provenance slices are necessary but not sufficient to close #91. Closed-alpha evidence still requires:

1. production iOS MediaPipe Vision instead of `DeterministicImageEmbeddingGenerator`;
2. representative Android/iOS fixture comparison using this exact artifact;
3. physical-device Vision task construction and repeated inference;
4. backend create/match round trips using both platforms;
5. SBOM/release documentation identifying the model, provenance, and runtime versions.
