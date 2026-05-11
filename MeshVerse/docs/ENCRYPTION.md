# Security Notes

## Current MVP
- ECDH key exchange (P-256 approximation of Signal-like flow).
- AES-GCM payload encryption support.
- Periodic key rotation via `SecurityMaintenanceWorker`.
- Signed packet primitives in `CryptoManager`.

## Hardening backlog
- full Signal Double Ratchet sessions
- per-conversation ephemeral keys
- secure hardware-backed key storage
