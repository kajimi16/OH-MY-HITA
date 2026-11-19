#!/usr/bin/env python3
"""对比两张同尺寸截图，输出差异比例、差异区域与可选热力图。

用法：visual_diff.py a.png b.png [--out diff.png] [--crop-top N] [--crop-bottom N] [--tolerance 8]
       [--header-bottom N] [--dock-top N]
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
    ap.add_argument("--header-bottom", type=int, default=0)
    ap.add_argument("--dock-top", type=int, default=0)
    args = ap.parse_args()

    a = Image.open(args.a).convert("RGB")
    b = Image.open(args.b).convert("RGB")
    if a.size != b.size:
        print(f"size mismatch: {a.size} vs {b.size}", file=sys.stderr)
        return 2

    def region_ratio(name: str, top: int, bottom: int) -> None:
        top = max(0, top)
        bottom = min(a.height, bottom)
        if bottom <= top:
            return
        crop_a = a.crop((0, top, a.width, bottom))
        crop_b = b.crop((0, top, b.width, bottom))
        mask = ImageChops.difference(crop_a, crop_b).convert("L").point(
            lambda v: 255 if v > args.tolerance else 0
        )
        changed = sum(1 for v in mask.getdata() if v)
        total = crop_a.width * crop_a.height
        print(f"region={name} top={top} bottom={bottom} changed_pixels={changed} ratio={changed / total * 100:.3f}%")

    if args.header_bottom or args.dock_top:
        body_bottom = args.dock_top if args.dock_top > 0 else a.height
        if args.header_bottom > 0:
            region_ratio("header", 0, args.header_bottom)
        region_ratio("body", args.header_bottom, body_bottom)
        if args.dock_top > 0:
            region_ratio("dock", args.dock_top, a.height)

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
