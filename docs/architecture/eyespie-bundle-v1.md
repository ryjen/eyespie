# `.eyespie` bundle v1

Status: implementation contract for #173 / parent #167.

## Purpose

`.eyespie` v1 is the first portable game format for the backendless Eyespie core. It carries only the signed state required to play a game offline on another device.

It is deliberately **one bounded canonical file, not an archive**. No v1 requirement needs multiple files, so archive extraction, path traversal, duplicate-path, compression-bomb, and extraction-lifecycle semantics are excluded by construction.

A signature provides payload integrity and continuity of the creator's device-local signing key. It does **not** provide confidentiality, DRM, verified human identity, or strong anti-cheat against a player who controls the importing device.

## Security boundary

Treat every imported byte as hostile until the complete verification pipeline succeeds:

```text
total byte bound
  -> magic / schema / canonicalization / signature algorithm
  -> bounded strict UTF-8 / collection / primitive parse
  -> P-256 public-key shape
  -> PlayerId(publicKey) consistency
  -> embedding model / digest / dimension compatibility
  -> match-policy compatibility
  -> ECDSA-SHA256 verification over exact unsigned bytes
  -> imported domain construction
  -> idempotency / conflict decision
  -> one SQLDelight save transaction
```

No parsing, signature, or compatibility failure is allowed to partially persist game authority.

## v1 constants

| Field | v1 value |
|---|---|
| Magic | ASCII `EYESPIE1` |
| Bundle schema | `1` |
| Canonicalization version | `1` |
| Signature algorithm | `1` = P-256 ECDSA-SHA256, X9.62/DER signature |
| Embedding contract version | `1` |
| Embedding model ID | `mediapipe-mobilenet-v3-small-100-224-embedder-v1` |
| Embedding model SHA-256 | `f7b9a563cb803bdcba76e8c7e82abde06f5c7a8e67b5e54e43e23095dfe79a78` |
| Embedding dimension | `1024` Float32 values |
| Match policy version | `1` = cosine similarity against an explicit per-Thing threshold |
| Public key | 65-byte X9.63 uncompressed P-256 point, first byte `0x04` |
| Player ID | `p256:` + lowercase SHA-256 hex of the canonical 65-byte public key |
| Maximum bundle size | 4 MiB |
| Maximum Thing count | 256 |
| Maximum signature field | 128 bytes; cryptographic verifier still requires valid X9.62/DER ECDSA |

`models/image-embedder.json` is the reviewed model evidence. Tests fail when application model constants drift from that manifest.

## Primitive encoding

All fixed-width wire primitives are **big-endian**. This wire representation is intentionally independent of SQLDelight's local embedding storage codec.

- Int32: 4 bytes, two's-complement big-endian.
- Int64: 8 bytes, two's-complement big-endian.
- Float32: raw IEEE-754 `toRawBits()` encoded as one big-endian Int32.
- Float64: raw IEEE-754 `toRawBits()` encoded as one big-endian Int64.
- UTF-8 string: Int32 byte count followed by exactly that many UTF-8 bytes.
- Fixed byte field: bytes are emitted directly with no implicit platform representation.

Decoding uses strict UTF-8. Invalid byte sequences fail closed rather than being replacement-decoded.

All embeddings must be exactly 1024 finite Float32 values. Match thresholds must be finite Float64 values in `[-1, 1]`.

## Unsigned canonical payload

Fields appear exactly in this order:

```text
8 bytes   magic = "EYESPIE1"
Int32     bundleSchemaVersion = 1
Int32     canonicalizationVersion = 1
Int32     signatureAlgorithm = 1
Int32     embeddingContractVersion = 1
String    embeddingModelId
32 bytes  embeddingModelSha256
Int32     embeddingDimension = 1024

String    gameId
String    gameName
String    creatorPlayerId
65 bytes  creatorPublicKey
Int32     thingCount

repeat thingCount:
  String  thingId
  String  playableClueText
  Int32   matchPolicyVersion = 1
  Float64 matchThreshold raw bits
  Int32   embeddingCount = 1024
  repeat 1024:
    Float32 targetEmbedding raw bits
```

The complete byte prefix above is the **unsigned canonical byte sequence**.

## Signature field and final file

The creator signs the exact unsigned canonical bytes through `SigningIdentity.sign`.

Current platform implementations use:

- Android: `SHA256withECDSA` over the platform-backed P-256 identity;
- iOS: `kSecKeyAlgorithmECDSASignatureMessageX962SHA256` over the platform-backed P-256 identity.

Both produce X9.62/DER ECDSA signatures.

The final file appends:

```text
Int32 signatureByteCount
bytes signature
```

The length prefix and signature bytes themselves are not included in the signed prefix. No trailing bytes are permitted after the signature.

## Portable allowlist

A v1 bundle contains only:

- game ID and bounded display name;
- creator `PlayerId` and canonical public key;
- ordered Thing IDs;
- playable clue text;
- target embeddings;
- match-policy/threshold values;
- model/format/signature compatibility metadata.

It must not contain:

- creator-only expected answers;
- generated-provider provenance that is not required for play;
- private signing-key material;
- raw target or guess images;
- exact location;
- local/private filesystem paths;
- hosted account IDs, tokens, signed URLs, or backend configuration;
- raw prompts/model output;
- filenames interpreted as filesystem destinations;
- executable/plugin/archive content.

## Imported clue authority

Creator-side `MANUAL` and `GENERATED` clue authority may contain a hidden expected answer. Portable play intentionally does not.

An imported bundle therefore persists clue text as `ClueOrigin.SHARED`:

- `expectedAnswer = null`;
- `generatedProvenance = null`;
- playable projection is the signed clue text;
- the importer never invents creator-only authority;
- `SHARED` is distinct from historical `LEGACY` data.

A creator's original local game keeps its richer MANUAL/GENERATED authority after export.

## Export authorization

Export succeeds only when:

1. the current platform signing public key derives to the active local `PlayerId`;
2. the requested Game creator equals that active local `PlayerId`;
3. every portable field satisfies v1 canonical validation;
4. signing succeeds;
5. the generated signature verifies against the same public key before bytes are returned.

This prevents an importing player from re-signing a foreign game as though they were its original creator.

## Import idempotency and conflict semantics

After signature/domain validation, import serializes the local check/save decision within the application process.

For the signed `GameId`:

- no local game: persist the imported game;
- same portable projection: return `AlreadyPresent` without rewriting;
- different portable projection: return `Conflict` and do not overwrite.

Portable equivalence compares only fields actually present in the signed bundle. Creator-only expected answers/provenance are excluded, so a creator importing their own export is idempotent even though their local MANUAL authority is richer than the shared projection.

`SqlGameRepository.save` remains the atomic SQLDelight persistence transaction after all validation succeeds.

## Failure behavior

The codec/service uses typed failures for malformed and incompatible content, including:

- oversized/truncated input;
- bad magic;
- unsupported schema/canonicalization/signature algorithm;
- invalid string length or UTF-8;
- malformed IDs/public-key shape;
- creator PlayerId/public-key mismatch;
- unsupported model/digest/dimension/match-policy identity;
- duplicate Thing IDs;
- non-finite/wrong-dimension embeddings;
- invalid match threshold;
- invalid signature;
- trailing bytes;
- local same-ID conflict;
- persistence failure.

User-facing/platform layers should map these to bounded diagnostic states. They must not echo hostile bundle payloads, embeddings, signatures, private paths, or hidden authority into ordinary logs/UI.

## Golden contract

The JVM integration test constructs a fixed one-Thing v1 payload and deterministic test signature. The resulting signed file currently has SHA-256:

```text
d1e0db592801eaa876ce768010b355cbb498c6f77ef947acf166bdb799ec18c9
```

Changing this digest is a wire-compatibility change and requires an explicit format/canonicalization decision rather than an incidental serializer or storage refactor.

The golden test is a byte-layout contract, not production-key cryptographic evidence. Production signatures remain platform-backed and are verified through the real `SigningIdentity` implementation.

## Platform file handling

Android/iOS document pickers, share sheets, scoped URLs, and file I/O are intentionally outside this v1 common codec. #174 owns those adapters.

The common layer receives/returns bounded bytes and typed results; Android `Uri`, iOS URLs, filesystem paths, and document-provider handles never become game-domain authority.

## Physical interoperability

Golden/common tests prove deterministic representation and supported-target compilation. They do not replace #92.

#92 remains the release gate that transfers real `.eyespie` files Android → iOS and iOS → Android on representative physical devices and proves import + local gameplay in both directions.
