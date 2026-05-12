#!/usr/bin/env python3
import argparse
import json
import re
import sys
from pathlib import Path

WEIGHTS = {
    "✅": 1.0,
    "🔄": 0.5,
    "⏳": 0.0,
    "📅": 0.0,
}


def parse_phase_block(content: str, phase_number: int) -> list[str]:
    pattern = re.compile(
        rf"####\s*Phase\s*{phase_number}\b.*?\n(?P<body>(?:- .*\n)+)",
        re.MULTILINE,
    )
    match = pattern.search(content)
    if not match:
        raise ValueError(f"Phase {phase_number} section not found")
    lines = [line.strip() for line in match.group("body").splitlines() if line.strip()]
    if not lines:
        raise ValueError(f"Phase {phase_number} has no roadmap items")
    return lines


def compute_score(lines: list[str]) -> dict[str, float]:
    total = len(lines)
    weighted = 0.0
    unknown = 0
    for line in lines:
        status = next((icon for icon in WEIGHTS if icon in line), None)
        if status is None:
            unknown += 1
            continue
        weighted += WEIGHTS[status]

    effective_total = total - unknown
    if effective_total <= 0:
        raise ValueError("No recognizable status markers found")

    percent = round((weighted / effective_total) * 100, 2)
    return {
        "items": total,
        "recognized_items": effective_total,
        "unrecognized_items": unknown,
        "completion_percent": percent,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description="Compute roadmap completion for Phase 1 and 2.")
    parser.add_argument(
        "--readme",
        default="../README.md",
        help="Path to README containing roadmap phases (default: ../README.md from MeshVerse).",
    )
    parser.add_argument(
        "--output",
        default="build/reports/ci/phase-progress.json",
        help="Where to write machine-readable phase progress JSON.",
    )
    args = parser.parse_args()

    readme_path = Path(args.readme)
    if not readme_path.exists():
        print(f"ERROR: README path not found: {readme_path}", file=sys.stderr)
        return 1

    content = readme_path.read_text(encoding="utf-8")

    try:
        phase1_lines = parse_phase_block(content, 1)
        phase2_lines = parse_phase_block(content, 2)
        phase1 = compute_score(phase1_lines)
        phase2 = compute_score(phase2_lines)
    except ValueError as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1

    output = {
        "phase_1": phase1,
        "phase_2": phase2,
    }

    output_path = Path(args.output)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(output, indent=2) + "\n", encoding="utf-8")

    print(f"Phase 1 completion: {phase1['completion_percent']}% ({phase1['recognized_items']}/{phase1['items']} recognized)")
    print(f"Phase 2 completion: {phase2['completion_percent']}% ({phase2['recognized_items']}/{phase2['items']} recognized)")
    print(f"Saved report to {output_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
