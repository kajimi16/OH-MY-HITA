#!/usr/bin/env python3
"""对比两张同尺寸截图，输出差异比例、差异区域与可选热力图。

用法：visual_diff.py a.png b.png [--out diff.png] [--crop-top N] [--crop-bottom N] [--tolerance 8]
"""
import argparse
import sys

from PIL import Image, ImageChops, ImageDraw

def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("a")
    ap.add_argument("b")
    ap.add_argument("--out", default="")
    ap.add_argument("--crop-top", type=int, default=0)
    ap.add_argument("--crop-bottom", type=int, default=0)
    ap.add_argument("--tolerance", type=int, default=8)
    args = ap.parse_args()

    a = Image.open(args.a).convert("RGB")
    b = Image.open(args.b).convert("RGB")
    if a.size != b.size:
        print(f"size mismatch: {a.size} vs {b.size}", file=sys.stderr)
        return 2

    if args.crop_top or args.crop_bottom:
        bottom = a.height - args.crop_bottom
        a = a.crop((0, args.crop_top, a.width, bottom))
        b = b.crop((0, args.crop_top, b.width, bottom))

    diff = ImageChops.difference(a, b)
    gray = diff.convert("L")
    mask = gray.point(lambda v: 255 if v > args.tolerance else 0)
    changed = sum(1 for v in mask.getdata() if v)
    total = a.width * a.height
    ratio = changed / total * 100

    box = mask.getbbox()
    print(f"size={a.size} changed_pixels={changed} ratio={ratio:.3f}%")
    print(f"changed_bbox={box}")

    if args.out:
        heat = a.copy()
        draw = ImageDraw.Draw(heat)
        if box:
            draw.rectangle(box, outline=(255, 0, 0), width=4)
        heat.paste((255, 0, 0), mask=mask)
        heat.save(args.out)
        print(f"diff image: {args.out}")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
