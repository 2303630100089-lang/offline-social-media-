# Mesh Networking MVP Notes

## Implemented
- Nearby discovery + advertising via `MeshNetworkManager`.
- AODV-like route request/reply handling.
- Packet deduplication using bounded seen-packet cache.
- Store-and-forward queue for pending destination routes.
- SOS broadcast propagation.

## Relay path
1. Device A sends packet to C.
2. If no direct connection, A selects route via B.
3. B decrements TTL and forwards to C.
4. ACK updates message delivery status.

## Local-only mode
When no peers are connected, app remains fully usable for:
- local chats and feed cache
- offline maps
- AI command shell
- mini-app runtime
