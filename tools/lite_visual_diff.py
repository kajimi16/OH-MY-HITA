#!/usr/bin/env python3
"""逐页比较 HITA_Lite 原版与移植版截图。

用法：
  python3 tools/lite_visual_diff.py baseline/ current/ --out report.csv --crop-top 120

两个目录中同名 PNG 视为同一页面。默认裁掉状态栏区域，避免系统时钟干扰。
SSIM 使用 scikit-image；差异图输出到 <out-stem>/diff/。
"""

from __future__ import annotations

import argparse
import csv
from pathlib import Path

import numpy as np
from PIL import Image
from skimage.metrics import structural_similarity


def load_rgb(path: Path, crop_top: int) -> np.ndarray:
    with Image.open(path) as image:
        rgb = image.convert("RGB")
        if crop_top > 0:
            rgb = rgb.crop((0, crop_top, rgb.width, rgb.height))
        return np.asarray(rgb, dtype=np.float32) / 255.0


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("baseline", type=Path)
    parser.add_argument("current", type=Path)
    parser.add_argument("--out", type=Path, default=Path("app/build/lite-ssim.csv"))
    parser.add_argument("--crop-top", type=int, default=120)
    parser.add_argument("--threshold", type=float, default=0.98)
    args = parser.parse_args()

    baseline_files = sorted(p for p in args.baseline.glob("*.png") if p.is_file())
    if not baseline_files:
        raise SystemExit(f"no PNG in {args.baseline}")

    diff_dir = args.out.parent / "diff"
    diff_dir.mkdir(parents=True, exist_ok=True)
    rows = []
    failed = 0
    for base in baseline_files:
        current = args.current / base.name
        if not current.is_file():
            rows.append((base.name, "", "MISSING", ""))
            failed += 1
            continue
        left = load_rgb(base, args.crop_top)
        right = load_rgb(current, args.crop_top)
        if left.shape != right.shape:
            rows.append((base.name, "", "SIZE", f"{left.shape}!={right.shape}"))
            failed += 1
            continue
        score, diff = structural_similarity(
            left, right, channel_axis=2, data_range=1.0, full=True,
        )
        status = "OK" if score >= args.threshold else "FAIL"
        if status == "FAIL":
            failed += 1
        rows.append((base.name, f"{score:.6f}", status, ""))

        # 差异热力图：白色=无差异，红色=差异大
        heat = np.clip(np.abs(diff).mean(axis=2), 0.0, 1.0)
        rgb = np.zeros((*heat.shape, 3), dtype=np.uint8)
        rgb[..., 0] = (heat * 255).astype(np.uint8)
        Image.fromarray(rgb, "RGB").save(diff_dir / base.name)

    args.out.parent.mkdir(parents=True, exist_ok=True)
    with args.out.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.writer(handle)
        writer.writerow(["page", "ssim", "status", "note"])
        writer.writerows(rows)

    for page, score, status, note in rows:
        print(f"{page}\t{score}\t{status}\t{note}")
    return 0 if failed == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())
