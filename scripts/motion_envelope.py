#!/usr/bin/env python3
"""Measure frame-to-frame motion envelopes from extracted video frames.

Usage:
  ffmpeg -i motion.mp4 -vf fps=60 frames/%04d.png
  python3 scripts/motion_envelope.py frames --fps 60 --threshold 0.8

Outputs a CSV of per-frame mean absolute RGB difference and a summary:
start of motion, last motion frame, stable frame, duration in ms.
"""
from __future__ import annotations

import argparse
import csv
import sys
from pathlib import Path

try:
    from PIL import Image, ImageChops, ImageStat
except ImportError:
    sys.exit("Pillow is required: pip install pillow")


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("frames", type=Path)
    ap.add_argument("--fps", type=float, default=60.0)
    ap.add_argument("--threshold", type=float, default=0.8,
                    help="mean absolute RGB delta considered still")
    ap.add_argument("--out", type=Path, default=None)
    args = ap.parse_args()

    frames = sorted(args.frames.glob("*.png"))
    if len(frames) < 2:
        sys.exit(f"need >=2 PNG frames in {args.frames}")

    rows: list[tuple[int, float]] = []
    prev = Image.open(frames[0]).convert("RGB")
    for index, path in enumerate(frames[1:], start=1):
        cur = Image.open(path).convert("RGB")
        stat = ImageStat.Stat(ImageChops.difference(prev, cur))
        delta = sum(stat.mean) / 3.0
        rows.append((index, delta))
        prev = cur

    moving = [i for i, d in rows if d >= args.threshold]
    first = moving[0] if moving else 0
    last = moving[-1] if moving else 0
    stable = next((i for i, d in rows if i > last and d < args.threshold), len(frames) - 1)
    duration_ms = (stable - first) / args.fps * 1000.0

    out = args.out or args.frames / "motion_envelope.csv"
    with out.open("w", newline="") as f:
        w = csv.writer(f)
        w.writerow(["frame", "delta"])
        w.writerows(rows)

    print(f"frames={len(frames)} fps={args.fps:g} threshold={args.threshold:g}")
    print(f"motion_start_frame={first} motion_last_frame={last} stable_frame={stable}")
    print(f"motion_duration_ms={duration_ms:.1f}")
    print(f"envelope_csv={out}")


if __name__ == "__main__":
    main()
