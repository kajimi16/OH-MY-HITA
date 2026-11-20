# Shell 动效基线（2026-11-21）

## 环境

- 设备：Android 模拟器，1080x2400。
- 包：`io.github.kajimi16.ohmyhita`，debug 构建。
- 录屏：`scripts/motion_capture.sh`，`adb screenrecord`，60fps 抽帧。
- 分析：`scripts/motion_envelope.py --fps 60 --threshold 0.8`。

## 采样

| 场景 | 视频 | 帧数 | 运动帧 | 稳定帧 | 运动时长 |
|---|---|---:|---:|---:|---:|
| 抽屉打开（玻璃） | `baseline-drawer.mp4` | 112 | 85–99 | 100 | 250.0ms |
| 抽屉关闭（玻璃） | `baseline-drawer-close3.mp4` | 78 | 66–77 | 文件结束 | 183.3ms |
| Dock 左滑 | `baseline-dock.mp4` | 405 | 89–99 | 100 | 183.3ms |
| 完整交互序列 | `baseline-motion.mp4` | 307 | 61–270 | 271 | 3500.0ms |

完整序列包含：切到待办、左滑内容切到今日、Dock 左滑、Dock 右滑、打开抽屉。

## 结论

- 抽屉打开实测 250ms，目标 240ms，误差 10ms，小于一帧（60fps 为 16.7ms）。
- Dock 左滑的可见运动段 183ms；释放后的 settle 在录屏中被并入该段。
- 逐帧 CSV 在 `docs/baselines/data/`。
- 抽屉关闭实测 183.3ms，目标 200ms，误差 16.7ms，恰好一帧。
- `adb screenrecord` 帧率不恒定，抽帧按 60fps 换算；跨设备比较时先校正实际 fps。
- 关闭录屏在运动结束处截断，`stable_frame` 记为文件结束，不当作稳定帧。

## 静态差异

同风格、同页面状态，`1080×2400（20:9）` 截图，tolerance 10：

| 场景 | 内容区 | header | dock | 结论 |
|---|---:|---:|---:|---|
| Today 玻璃 | 1.467% | 0.752% | 4.898% | 内容/header 通过，dock 未达 1% |
| Today 经典 | 1.531% | 1.899% | 3.804% | 内容通过，header/dock 未达 1% |

dock 与 header 残差沿用 7.1 §8.2 的“已接受残差”，本轮不改样式几何；
dock 区域含系统导航栏，数值高于仅栏体区域。

## 复现

```bash
export ADB=$ANDROID_HOME/platform-tools/adb
./scripts/motion_capture.sh io.github.kajimi16.ohmyhita app/build/baseline-drawer.mp4 6
ffmpeg -i app/build/baseline-drawer.mp4 -vf fps=60 app/build/drawer-frames/%04d.png
python3 scripts/motion_envelope.py app/build/drawer-frames --fps 60 --threshold 0.8
```
