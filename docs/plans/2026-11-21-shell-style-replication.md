# 五套界面风格上游复刻记录

日期：2026-11-21
上游：`HIT-A/HITA_Android` v3.3.0，commit `e134d97`

## 范围

赛博朋克、日映构成、波普涂鸦、深空星野、水墨宣纸。宿主仍保留经典与玻璃艺术，不改变其实现。

## 上游实现映射

| 风格 | 上游文件 | 复刻文件 |
|---|---|---|
| 赛博 | `HitaCyberSurfaces.kt` | `HitaCyberSurfaces.kt` |
| 日映 | `HitaSoraCloudSurfaces.kt` | `HitaSoraCloudSurfaces.kt` |
| 波普 | `HitaPersonaSurfaces.kt` | `HitaPersonaSurfaces.kt` |
| 深空 | `HitaDeepSpaceSurfaces.kt` | `HitaDeepSpaceSurfaces.kt` |
| 水墨 | `HitaSumiSurfaces.kt` | `HitaSumiSurfaces.kt` |
| 卡片分发 | `HitaGlassSurfaces.kt` | `HitaGlassSurfaces.kt` |

移植文件与上游的差异仅限 package、`com.limpu.style.ThemeTools` → `com.stupidtree.style.ThemeTools`、`cn.limpu.hita.R` → `com.stupidtree.hitax.R`，以及宿主适配层 `ShellStyleContext.kt`。

## 宿主接入

- `ShellStyleContext.kt`：CompositionLocal + 上游 `HitaTheme.preferenceStyle/isDark` 适配。
- `PresetShellActivity`：向 MaterialTheme 外层提供当前风格；今日卡、底栏、抽屉、MoreCard 使用 `hitaStyleCardShape`、`hitaGlassCardBorder`、`hitaGlassCardModifier`。
- `ShellBackdrop`：日映/深空/水墨使用上游全屏背景；赛博/波普按上游不绘制装饰背景。
- 卡片文字与图标底色随风格切换为 `onSurface/onSurfaceVariant/primaryContainer`，避免白底白字。

## 像素比对

设备：1080×2400。与上游同风格 Today 页比较，裁掉 y=0..99 状态栏：

| 风格 | AE | 差异占比 | RMSE |
|---|---:|---:|---:|
| 日映构成 | 0 | 0.000% | 0 |
| 波普涂鸦 | 0 | 0.000% | 0 |
| 水墨宣纸 | 0 | 0.000% | 0 |
| 深空星野 | 621 | 0.026% | 29.96 |
| 赛博朋克 | 53,778 | 2.263% | 826.42 |

赛博差异集中在流动霓虹边框的动画相位；深空差异来自星光明暗呼吸相位。两张全屏 PNG 的 sha256 不同；裁掉状态栏后静态三套逐像素一致。

## 证据

- 上游：`app/build/ref-{cyber,sora,p5,deep,sumi}-today*.png`
- 宿主：`app/build/our2-{cyber,sora_cloud,p5,deep_space,sumi}.png`
- 五联图：`app/build/five-styles.png`

以上截图在 `build/` 下，不提交。
