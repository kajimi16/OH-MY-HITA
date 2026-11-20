#!/usr/bin/env python3
"""对比两张截图，输出整屏与分区（header/body/dock/右侧面板）的变更像素统计。

用法：
    glass_regions.py a.png b.png --out DIR [--tolerance 8]
    glass_regions.py a.png b.png --out DIR --json ...
    glass_regions.py --self-test

分区默认按 1080x2400 截图划分：header 0-300、body 300-2100、dock 2100-2400，
右侧面板默认 x=397..width、全高。图像高度不等于 2400 时各分段按图像高度截断。

输出目录内容：
    report.json   完整统计
    report.txt    文本报告
    heatmap.png   A 为底，变更像素染红，分区边界描边
    masks/*.png   每个分区的二值变更掩码
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

DEFAULT_HEADER_BOTTOM = 300
DEFAULT_BODY_BOTTOM = 2100
DEFAULT_DOCK_BOTTOM = 2400
DEFAULT_RIGHT_X = 397

REGION_COLORS = {
    "header": (255, 214, 0),
    "body": (0, 200, 255),
    "dock": (0, 230, 120),
    "right": (255, 0, 200),
}


def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        prog="glass_regions.py",
        description="对比两张同尺寸 PNG，统计整屏与 header/body/dock/右侧面板的变更像素。",
        formatter_class=argparse.ArgumentDefaultsHelpFormatter,
    )
    p.add_argument("a", nargs="?", help="基准 PNG")
    p.add_argument("b", nargs="?", help="对比 PNG")
    p.add_argument("--out", metavar="DIR", help="输出目录（report.json、report.txt、heatmap.png、masks/）")
    p.add_argument("--tolerance", type=int, default=8,
                   help="单像素任一通道差值的绝对上限；超过该值记为变更")
    p.add_argument("--header-bottom", type=int, default=DEFAULT_HEADER_BOTTOM,
                   help="header 分段下边界（不含）")
    p.add_argument("--body-bottom", type=int, default=DEFAULT_BODY_BOTTOM,
                   help="body 分段下边界（不含），同时是 dock 上边界")
    p.add_argument("--dock-bottom", type=int, default=DEFAULT_DOCK_BOTTOM,
                   help="dock 分段下边界（不含），超过图像高度时截断")
    p.add_argument("--right-x", type=int, default=DEFAULT_RIGHT_X,
                   help="右侧面板左边界；负数表示跳过该分区")
    p.add_argument("--right-y0", type=int, default=0, help="右侧面板上边界")
    p.add_argument("--right-y1", type=int, default=-1,
                   help="右侧面板下边界（不含）；负数表示到图像底部")
    p.add_argument("--stdout", choices=("text", "json", "both"), default="text",
                   help="标准输出格式")
    p.add_argument("--self-test", action="store_true",
                   help="在 app/build/glass-selftest 下生成合成 PNG 并自检，忽略截图参数")
    return p


def load_images(a_path: Path, b_path: Path) -> tuple[Image.Image, Image.Image]:
    a = Image.open(a_path).convert("RGB")
    b = Image.open(b_path).convert("RGB")
    return a, b


def difference(a: Image.Image, b: Image.Image, tolerance: int) -> tuple[np.ndarray, np.ndarray]:
    """返回 (逐像素最大通道差, 变更掩码)。

    前置条件：a 与 b 尺寸一致；tolerance >= 0。
    后置条件：掩码形状为 (h, w)，元素为 np.bool_，delta > tolerance 处为 True。
    不变式：同一输入与 tolerance 产生同一输出。
    违约处理：尺寸不一致抛 ValueError。
    """
    if a.size != b.size:
        raise ValueError(f"size mismatch: {a.size} vs {b.size}")
    arr_a = np.asarray(a, dtype=np.int16)
    arr_b = np.asarray(b, dtype=np.int16)
    delta = np.abs(arr_a - arr_b).max(axis=2)
    return delta, delta > tolerance


def _bbox(mask: np.ndarray) -> list[int] | None:
    rows = np.flatnonzero(mask.any(axis=1))
    if rows.size == 0:
        return None
    cols = np.flatnonzero(mask.any(axis=0))
    return [int(cols[0]), int(rows[0]), int(cols[-1]) + 1, int(rows[-1]) + 1]


def region_stats(mask: np.ndarray, delta: np.ndarray, name: str,
                 x0: int, y0: int, x1: int, y1: int) -> dict:
    """统计一个矩形区域。

    前置条件：mask/delta 形状一致；坐标已裁剪到图像范围内且 x1>=x0、y1>=y0。
    后置条件：返回 changed/pixels/ratio/ratio_pct，pixels 为 0 时 ratio 为 0.0。
    不变式：changed 等于区域内 mask 为 True 的元素个数。
    违约处理：坐标越界抛 ValueError（由调用方裁剪后再调用）。
    """
    h, w = mask.shape
    if not (0 <= x0 <= x1 <= w and 0 <= y0 <= y1 <= h):
        raise ValueError(f"region {name} out of bounds: {(x0, y0, x1, y1)} vs {(w, h)}")
    sub_mask = mask[y0:y1, x0:x1]
    pixels = int(sub_mask.size)
    changed = int(sub_mask.sum())
    ratio = changed / pixels if pixels else 0.0
    sub_delta = delta[y0:y1, x0:x1]
    return {
        "name": name,
        "x0": x0, "y0": y0, "x1": x1, "y1": y1,
        "width": x1 - x0, "height": y1 - y0,
        "pixels": pixels,
        "changed_pixels": changed,
        "ratio": round(ratio, 6),
        "ratio_pct": round(ratio * 100.0, 4),
        "mean_abs_diff": round(float(sub_delta.mean()), 4) if pixels else 0.0,
        "bbox": _bbox(sub_mask),
        "empty": pixels == 0,
    }


def plan_regions(width: int, height: int, header_bottom: int, body_bottom: int,
                 dock_bottom: int, right_x: int, right_y0: int, right_y1: int) -> list[dict]:
    """把配置裁剪成合法矩形列表。

    前置条件：width/height > 0。
    后置条件：每个矩形满足 0<=x0<=x1<=width 且 0<=y0<=y1<=height；顺序为 header、body、dock、right。
    不变式：分段边界对图像高度做截断，不缩放。
    违约处理：非正尺寸抛 ValueError。
    """
    if width <= 0 or height <= 0:
        raise ValueError(f"invalid image size: {(width, height)}")
    hb = min(max(header_bottom, 0), height)
    bb = min(max(body_bottom, hb), height)
    db = min(max(dock_bottom, bb), height)
    regions = [
        {"name": "header", "x0": 0, "y0": 0, "x1": width, "y1": hb},
        {"name": "body", "x0": 0, "y0": hb, "x1": width, "y1": bb},
        {"name": "dock", "x0": 0, "y0": bb, "x1": width, "y1": db},
    ]
    if right_x >= 0:
        rx = min(max(right_x, 0), width)
        ry0 = min(max(right_y0, 0), height)
        ry1 = height if right_y1 < 0 else min(max(right_y1, ry0), height)
        regions.append({"name": "right", "x0": rx, "y0": ry0, "x1": width, "y1": ry1})
    return regions


def analyze(a: Image.Image, b: Image.Image, tolerance: int, regions: list[dict]) -> dict:
    """生成完整统计字典（不含文件副作用）。

    前置条件：a/b 尺寸一致；tolerance >= 0；regions 来自 plan_regions。
    后置条件：返回包含 size/tolerance/full/regions 的字典，regions 顺序与输入一致。
    不变式：full.changed_pixels 等于各变更像素总数，且等于整屏掩码求和。
    违约处理：尺寸不一致抛 ValueError。
    """
    delta, mask = difference(a, b, tolerance)
    width, height = a.size
    full = region_stats(mask, delta, "full", 0, 0, width, height)
    out_regions = [
        region_stats(mask, delta, r["name"], r["x0"], r["y0"], r["x1"], r["y1"])
        for r in regions
    ]
    return {
        "size": [width, height],
        "tolerance": tolerance,
        "bands_clamped": height != DEFAULT_DOCK_BOTTOM,
        "full": full,
        "regions": out_regions,
        "mask": mask,
        "delta": delta,
    }


def render_heatmap(a: Image.Image, mask: np.ndarray, regions: list[dict], path: Path) -> None:
    """写热力图：A 为底，变更像素染红，分区边界按 REGION_COLORS 描边。"""
    arr = np.asarray(a.convert("RGB")).copy()
    arr[mask] = (arr[mask] * 0.4 + np.array([255, 0, 0]) * 0.6).astype(np.uint8)
    heat = Image.fromarray(arr)
    draw = ImageDraw.Draw(heat)
    for r in regions:
        color = REGION_COLORS.get(r["name"], (255, 255, 255))
        x1 = max(r["x1"] - 1, r["x0"])
        y1 = max(r["y1"] - 1, r["y0"])
        if r["x1"] > r["x0"] and r["y1"] > r["y0"]:
            draw.rectangle((r["x0"], r["y0"], x1, y1), outline=color, width=3)
    path.parent.mkdir(parents=True, exist_ok=True)
    heat.save(path)


def write_artifacts(report: dict, a: Image.Image, out_dir: Path) -> dict:
    """写 report.json、report.txt、heatmap.png、masks/*.png，返回文件路径映射。"""
    out_dir.mkdir(parents=True, exist_ok=True)
    mask = report.pop("mask")
    report.pop("delta")
    regions = report["regions"]
    json_path = out_dir / "report.json"
    text_path = out_dir / "report.txt"
    heat_path = out_dir / "heatmap.png"
    mask_dir = out_dir / "masks"

    render_heatmap(a, mask, regions, heat_path)
    mask_dir.mkdir(parents=True, exist_ok=True)
    for r in regions:
        sub = mask[r["y0"]:r["y1"], r["x0"]:r["x1"]]
        Image.fromarray((sub * 255).astype(np.uint8), mode="L").save(mask_dir / f"{r['name']}.png")
    Image.fromarray((mask * 255).astype(np.uint8), mode="L").save(mask_dir / "full.png")

    text = format_text(report)
    json_path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    text_path.write_text(text + "\n", encoding="utf-8")
    report["outputs"] = {
        "json": str(json_path),
        "text": str(text_path),
        "heatmap": str(heat_path),
        "masks": str(mask_dir),
    }
    return report["outputs"]


def format_text(report: dict) -> str:
    """把统计字典格式化成定宽文本表。"""
    lines = [
        f"size={report['size'][0]}x{report['size'][1]} tolerance={report['tolerance']}",
        f"{'region':<8} {'x':>4} {'y':>4} {'w':>4} {'h':>4} "
        f"{'changed':>9} {'pixels':>9} {'ratio%':>8}",
    ]
    for r in [report["full"], *report["regions"]]:
        lines.append(
            f"{r['name']:<8} {r['x0']:>4} {r['y0']:>4} {r['width']:>4} {r['height']:>4} "
            f"{r['changed_pixels']:>9} {r['pixels']:>9} {r['ratio_pct']:>8.4f}"
        )
    bbox = report["full"]["bbox"]
    lines.append(f"full_bbox={bbox}")
    if report.get("bands_clamped"):
        lines.append(f"note=image height {report['size'][1]} != {DEFAULT_DOCK_BOTTOM}; bands clamped")
    return "\n".join(lines)


def run(a_path: Path, b_path: Path, out_dir: Path | None, args: argparse.Namespace) -> int:
    """加载图片、统计、按需写文件并打印。返回进程退出码。"""
    a, b = load_images(a_path, b_path)
    regions = plan_regions(a.width, a.height, args.header_bottom, args.body_bottom,
                           args.dock_bottom, args.right_x, args.right_y0, args.right_y1)
    report = analyze(a, b, args.tolerance, regions)
    if out_dir is not None:
        write_artifacts(report, a, out_dir)
    if args.stdout in ("text", "both"):
        print(format_text(report))
    if args.stdout in ("json", "both"):
        printable = {k: v for k, v in report.items() if k not in ("mask", "delta")}
        print(json.dumps(printable, ensure_ascii=False, indent=2))
    return 0


def _paint(img: Image.Image, x0: int, y0: int, x1: int, y1: int,
           color: tuple[int, int, int]) -> None:
    img.paste(color, (x0, y0, x1, y1))


def self_test(root: Path) -> int:
    """合成 PNG 自检。返回 0 表示全部断言通过。

    前置条件：root 可写。
    后置条件：app/build/glass-selftest 下存在 base.png、variant.png、
              subtle.png 与 out/ 报告；断言覆盖 header/body/dock/right、
              tolerance 过滤、尺寸不一致与空分区。
    违约处理：断言失败抛 AssertionError。
    """
    out = root / "app" / "build" / "glass-selftest"
    out.mkdir(parents=True, exist_ok=True)
    w, h = 1080, 2400
    base = Image.new("RGB", (w, h), (30, 30, 30))
    variant = base.copy()
    _paint(variant, 100, 50, 200, 150, (200, 60, 60))        # header 100x100 = 10000
    _paint(variant, 100, 500, 300, 700, (60, 200, 60))       # body 200x200 = 40000
    _paint(variant, 100, 2150, 150, 2200, (60, 60, 200))     # dock 50x50 = 2500
    _paint(variant, 800, 1000, 900, 1100, (220, 220, 60))    # right 100x100 = 10000
    subtle = variant.copy()
    _paint(subtle, 600, 1500, 650, 1550, (35, 35, 35))       # delta 5, 2500 px
    base_path = out / "base.png"
    variant_path = out / "variant.png"
    subtle_path = out / "subtle.png"
    base.save(base_path)
    variant.save(variant_path)
    subtle.save(subtle_path)

    def stats(path_b: Path, tolerance: int) -> dict:
        img_a = Image.open(base_path).convert("RGB")
        img_b = Image.open(path_b).convert("RGB")
        regions = plan_regions(w, h, DEFAULT_HEADER_BOTTOM, DEFAULT_BODY_BOTTOM,
                               DEFAULT_DOCK_BOTTOM, DEFAULT_RIGHT_X, 0, -1)
        return analyze(img_a, img_b, tolerance, regions)

    r8 = stats(variant_path, 8)
    by_name = {r["name"]: r for r in r8["regions"]}
    assert r8["full"]["changed_pixels"] == 62500, r8["full"]["changed_pixels"]
    assert by_name["header"]["changed_pixels"] == 10000
    assert by_name["header"]["pixels"] == 1080 * 300
    assert by_name["body"]["changed_pixels"] == 50000
    assert by_name["body"]["pixels"] == 1080 * 1800
    assert by_name["dock"]["changed_pixels"] == 2500
    assert by_name["dock"]["pixels"] == 1080 * 300
    assert by_name["right"]["changed_pixels"] == 10000
    assert by_name["right"]["pixels"] == (1080 - DEFAULT_RIGHT_X) * h
    assert r8["full"]["bbox"] == [100, 50, 900, 2200], r8["full"]["bbox"]

    r0 = stats(subtle_path, 8)
    assert r0["full"]["changed_pixels"] == 62500, "tolerance must ignore delta=5"
    r3 = stats(subtle_path, 3)
    assert r3["full"]["changed_pixels"] == 65000, r3["full"]["changed_pixels"]
    r3_body = {r["name"]: r for r in r3["regions"]}["body"]
    assert r3_body["changed_pixels"] == 52500, r3_body["changed_pixels"]

    try:
        difference(Image.new("RGB", (10, 10)), Image.new("RGB", (11, 10)), 8)
    except ValueError:
        pass
    else:
        raise AssertionError("size mismatch must raise ValueError")

    empty_regions = plan_regions(w, h, 0, 0, 0, -1, 0, -1)
    assert [r["name"] for r in empty_regions] == ["header", "body", "dock"]
    empty_report = analyze(Image.open(base_path).convert("RGB"),
                           Image.open(base_path).convert("RGB"), 8, empty_regions)
    assert empty_report["full"]["changed_pixels"] == 0
    assert all(r["pixels"] == 0 for r in empty_report["regions"])

    out_dir = out / "out"
    report = analyze(Image.open(base_path).convert("RGB"),
                     Image.open(variant_path).convert("RGB"), 8,
                     plan_regions(w, h, DEFAULT_HEADER_BOTTOM, DEFAULT_BODY_BOTTOM,
                                  DEFAULT_DOCK_BOTTOM, DEFAULT_RIGHT_X, 0, -1))
    outputs = write_artifacts(report, Image.open(base_path).convert("RGB"), out_dir)
    reloaded = json.loads(Path(outputs["json"]).read_text(encoding="utf-8"))
    assert reloaded["full"]["changed_pixels"] == 62500
    assert [r["name"] for r in reloaded["regions"]] == ["header", "body", "dock", "right"]
    for key, p in outputs.items():
        assert Path(p).exists(), f"missing artifact {key}: {p}"
    assert (out_dir / "masks" / "right.png").exists()
    assert "header" in build_parser().format_help()

    print("SELFTEST PASS")
    print(f"artifacts: {out_dir}")
    print("cases: full=62500 header=10000 body=50000 dock=2500 right=10000; "
          "tolerance 8 ignores delta=5, tolerance 3 counts 65000; size mismatch raises")
    return 0


def main(argv: list[str] | None = None) -> int:
    """CLI 入口。返回进程退出码：0 成功、2 输入错误。

    前置条件：argv 为命令行参数；--self-test 时不要求截图参数。
    后置条件：正常模式写输出目录并打印报告；自检模式只写 app/build/glass-selftest。
    违约处理：缺少参数由 argparse 报错退出 2；文件不存在或尺寸不一致打印错误并退出 2。
    """
    args = build_parser().parse_args(argv)
    root = Path(__file__).resolve().parent.parent
    if args.self_test:
        return self_test(root)
    if not args.a or not args.b:
        print("error: two PNG paths are required unless --self-test is used", file=sys.stderr)
        return 2
    if not args.out:
        print("error: --out DIR is required", file=sys.stderr)
        return 2
    a_path, b_path = Path(args.a), Path(args.b)
    for p in (a_path, b_path):
        if not p.is_file():
            print(f"error: not a file: {p}", file=sys.stderr)
            return 2
    if args.tolerance < 0:
        print("error: --tolerance must be >= 0", file=sys.stderr)
        return 2
    try:
        return run(a_path, b_path, Path(args.out), args)
    except ValueError as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
