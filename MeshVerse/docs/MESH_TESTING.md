# Mesh Testing Instructions

- Run simulator: `python3 build-scripts/mesh-simulator.py --nodes 10 --duration 90 --loss 0.2`
- Manual Android test:
  1. Launch MeshVerse on two or more devices.
  2. Confirm peers in Nearby tab.
  3. Send chat message and verify relay delivery.
  4. Trigger SOS and validate broadcast receipt.
