# Mesh Networking MVP Notes

## Implemented
- Nearby discovery + advertising via `MeshNetworkManager`.
- AODV-like route request/reply handling.
- Packet deduplication using bounded seen-packet cache.
- Store-and-forward queue for pending destination routes.
- Multi-hop broadcast relaying with TTL-based propagation for DTN-style dissemination.
- Retry-aware packet transmission with backoff on transient link failures.
- Gossip delta transport envelope with optional compression + per-peer encryption.
- SOS broadcast propagation.

## Relay path
1. Device A sends packet to C.
2. If no direct connection, A selects route via B.
3. B decrements TTL and forwards to C.
4. ACK updates message delivery status.

## Hybrid sync behavior
1. On peer connect, `GossipSyncManager` pushes unsynced encrypted deltas to that peer.
2. Large delta payloads are compressed before transport to reduce bandwidth.
3. Receiving peers decrypt/decompress and merge using repository-level conflict handling.
4. Broadcast packets are re-relayed until TTL expires, enabling opportunistic mesh spread.

## Local-only mode
When no peers are connected, app remains fully usable for:
- local chats and feed cache
- offline maps
- AI command shell
- mini-app runtime
