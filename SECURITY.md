# Security

## Backendless trust model

Eyespie's default trust boundary is the local device. Core play does not rely on a hosted account, database, storage service, or server-side authorization policy.

Private key material for the planned local player identity must use Android Keystore / Apple Keychain or equivalent platform-backed secure storage. It must never be placed in portable game bundles.

## Embeddings and anti-cheat

Offline portable games may contain target image embeddings so matching can run locally. Data available to the application on a player's device cannot be treated as secret from a sufficiently motivated owner of that device.

Do not claim encryption of a locally usable target embedding as a strong anti-cheat boundary. Where stronger authority is required, use the planned host-authoritative transport and keep target embeddings on the host.

## Network capabilities

The initial Android reboot manifest intentionally omits the `INTERNET` permission. Future cloud or remote-transport features must be optional modules/adapters with explicit security review before that capability becomes part of a production build.

## Historical backend

The retired Supabase security implementation and its RLS/RPC/storage-policy work remain available from `archive/pre-backendless-reboot-2026-08-15` for provenance. Those controls are not security requirements for the backendless core.
