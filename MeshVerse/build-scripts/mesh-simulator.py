#!/usr/bin/env python3
"""Simple mesh propagation simulator for local testing."""
import argparse
import random
import time


def simulate(nodes: int, duration: int, loss: float) -> None:
    neighbors = {i: set() for i in range(nodes)}
    for i in range(nodes):
        for j in range(i + 1, nodes):
            if random.random() > 0.45:
                neighbors[i].add(j)
                neighbors[j].add(i)

    print(f"nodes={nodes} edges={sum(len(v) for v in neighbors.values()) // 2} loss={loss}")
    delivered = {0}
    start = time.time()
    round_no = 0
    while time.time() - start < duration and len(delivered) < nodes:
        round_no += 1
        new = set(delivered)
        for node in delivered:
            for peer in neighbors[node]:
                if random.random() > loss:
                    new.add(peer)
        delivered = new
        print(f"round={round_no:03d} reached={len(delivered)}/{nodes}")
        time.sleep(0.1)

    print("complete" if len(delivered) == nodes else "partial")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--nodes", type=int, default=8)
    parser.add_argument("--duration", type=int, default=60)
    parser.add_argument("--loss", type=float, default=0.15)
    args = parser.parse_args()
    simulate(args.nodes, args.duration, args.loss)
