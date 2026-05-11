# Performance Optimization Guide

- Keep mesh scan intervals adaptive to battery level.
- Prefer chunked media packets (`MediaTransferManager`) and resumable transfer state.
- Use local-only mode when no peers exist to avoid unnecessary radio usage.
