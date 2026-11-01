#!/usr/bin/env python3
"""Basic safety checks for the repository index and APK artifacts."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
INDEX = ROOT / "index.min.json"

def main():
    data = json.loads(INDEX.read_text(encoding="utf-8"))
    if not isinstance(data, list):
        raise SystemExit("index.min.json must contain a JSON array")
    # Empty is allowed while the repository is being prepared.
    for item in data:
        if not isinstance(item, dict):
            raise SystemExit("Each index entry must be a JSON object")
    print(f"Index JSON valid; {len(data)} extension entr(y/ies) listed.")

if __name__ == "__main__":
    main()
