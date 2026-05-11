# MeshVerse MVP API

## Mesh packet contract
- `MeshPacket.packetId`: unique packet identity for deduplication.
- `MeshPacket.payloadType`: MESSAGE, MEDIA_CHUNK, ROUTE_REQUEST, ROUTE_REPLY, ROUTE_ERROR, HEARTBEAT, SOS_BROADCAST, ACK.
- `ttl`: decremented for relay hops.

## Mini-app bridge API
- `MeshVerseBridge.showToast(message)`
- `MeshVerseBridge.getPaymentDisclaimer()`
- `MeshVerseBridge.getDeviceMode()`

## AI command router
- `Send SOS`
- `Find nearby hospital`
- `Start walkie-talkie`
- `Open marketplace`
