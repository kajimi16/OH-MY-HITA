# 整合包接入实践

本文只记录已经落地的三次整合包接入：`hita-l-1.5.3`、`hita-cute-1.6.0`、`hita-android-3.1.2`。
它不是新的迁移方案，而是从当前代码、索引和历史修复中反推出来的操作手册。

## 1. 三种形态

| 预设 | 来源 | 底栏 | Shell | 插件引用 |
|---|---|---|---:|---:|
| `hita-l-1.5.3` | HITA-L 1.5.3 | 静态 3 页 | 无 | 10 |
| `hita-cute-1.6.0` | HITA CUTE 1.6.0 | 静态 3 页 | 无 | 10 |
| `hita-android-3.1.2` | HITA_Android 3.1.2 | 索引声明 6 项 | `PresetShellActivity` | 25 |

结论：

- **HITA-L/CUTE 是原生壳型预设**：`MainActivity` + 固定三页，不声明 `tabs`、不声明 `shell`。
- **HITA Android 是声明布局型预设**：索引声明 6 个 `tabs`，宿主映射到 Compose Shell。
- 同一个 `MarketPreset` 模型可以覆盖两种形态，但宿主必须有明确分支，不能只靠索引字段。

## 2. 数据模型

核心契约在 `app/src/main/java/com/stupidtree/hitax/market/MarketPlugin.kt`：

```text
MarketPreset
  ├─ id / name / version / source / minHostApi
  ├─ plugins: List<PresetPluginRef>
  │    ├─ id
  │    ├─ kind: ui | dex | builtin
  │    └─ required
  ├─ shell: String?             # 目标 UI 壳
  ├─ tabs: List<PresetTab>?     # 固定底栏；为空走宿主默认
  └─ migration / tags / author
```

`PresetTab` 的关键字段：

- `id`：解析顺序是 `hita.more` → 内置 tab（如 `hita.timeline`）→ UI 插件 id → 插件注册的 TabContribution。
- `title` / `order`：底栏文案与排序。
- `iconAsset`：
  - `builtin:<name>`：宿主内置矢量图，见 `BuiltinTabIcons.available()`。
  - 其他字符串：UI 插件 APK 内的图标路径。
- `visible` / `required`：只影响底栏可见性；`hita.more` 等基页不能被普通策略隐藏。

索引中的 `plugins` 与 `tabs` 是**两件事**：

- `plugins` 决定安装/启用哪些能力。
- `tabs` 决定底栏出现哪些入口、顺序和图标。

## 3. 接入流程

### 3.1 先判形态

先回答三个问题，再写索引：

1. 它的主界面是宿主原生 3/4 页，还是需要独立 Shell？
2. 它需要固定底栏，还是让宿主按已启用插件自动追加？
3. 它的上游 UI 代码是否已经在现有 UI 插件里，还是需要新插件？

本项目已有的结论：

- HITA-L/CUTE：原生 3 页，直接复用 `MainActivity.isHitaLFamily()` 分支。
- HITA Android：固定 6 项，必须声明 `tabs` 并映射 Shell。

### 3.2 列插件引用

按能力拆，不按上游 Activity 拆。三次接入实际使用的引用类型：

- `dex`：运行时核心服务，例如 `com.stupidtree.hitax.timetable`、`com.stupidtree.hitax.eas`。
- `ui`：Compose/宿主容器 UI 插件，例如 `com.stupidtree.hita.ui.timetable`。
- `builtin`：宿主内置能力；当前发行版默认空壳，`plugins.json` 为空，不能把“内置一定存在”当成前提。

`required` 的语义：

- `true`：缺插件或停用会阻塞整合包激活。
- `false`：允许缺失，激活后记录为可选失败。
- `null`：按“必选”处理；HITA Android 的若干 UI 插件就是 `null`。

### 3.3 写预设

索引必须同时更新三份文件：

```text
market/index.json
app/src/main/assets/market/index.json
app/src/debug/assets/market/index.json
```

三份不一致时，debug 安装和 release 安装会看到不同整合包。`market/index.json` 是根索引，asset 两份是运行时 fallback。

HITA-L/CUTE 的写法要点：

- `tabs` 为空。
- `shell` 为空。
- 由 `MainActivity.isHitaLFamily()` 识别，强制静态三页。
- 不在 `tabs` 里写 5 个插件页；写错会变成 3 项底栏 + 5 页 pager。

HITA Android 的写法要点：

- 声明 6 个 `tabs`。
- `hita.timeline` 和 `hita.more` 是宿主基页。
- `com.stupidtree.hita.ui.homework/timetable/ai/news` 映射到 UI 插件。
- `iconAsset` 使用 `builtin:*`，避免依赖每个插件 APK 的图标路径。
- `shell` 当前由 `PresetStateStore.shellForPreset()` 映射为 `hita-android-3.1.2`，索引没有显式写。

### 3.4 宿主分支

索引只描述意图，宿主必须实现对应分支：

- `MainActivity.isHitaLFamily()`：HITA-L/CUTE 静态三页。
- `MainActivity.layoutPages()`：按 `PresetTab` 组装固定底栏。
- `PresetStateStore.shellForPreset()`：把预设 id 映射到 Shell。
- `PresetShellActivity.loadShellTabs()`：读取 `PresetLayoutStore` + UI 插件 `mainTabs()`，解析图标与目标。
- `BuiltinTabIcons`：解析 `builtin:*` 图标。

关键约束：

- `active_preset`、`active_shell`、`tabs_json` 必须在一次 `PresetStateStore.saveUi()` 中提交。
- 只写 `active_preset` 不写 `tabs`，会出现“已切换但仍显示旧底栏”的半状态。
- `tabs` 的补写由 `PresetLayoutSync.tabsToBackfill()` 处理；它只补缺失图标，不覆盖用户本地顺序。

## 4. 应用整合包的实际状态机

入口：`PluginMarketViewModel.applyPreset()`。

```text
读取 preset
  → 校验 minHostApi / refs / kind
  → CompositionResolver.resolve()
  → blockingRequiredMissing / blockingRequiredDisabled
  → CompositionStore.record()
  → materializeInstalls()：下载/校验/落 staging
  → CompositionReconciler.executePrepared()
  → PresetStateStore.saveUi(active, shell, tabs)
  → presetSwitch → Shell 或 MainActivity
```

实现约定：

- 失败必须在 `saveUi` 前中止；不能把新预设标成“使用中”。
- 必选 artifact 准备失败时，不要执行任何 executor 动作。
- 可选插件失败不阻塞激活，但必须记录在报告里。
- 切换成功至少展示一次进度窗口，避免一闪而过。
- 切换后关闭市场 Activity，目标 UI 用 `CLEAR_TOP | SINGLE_TOP` 启动。

## 5. 组合与停用语义

`CompositionStore` 区分三类禁用：

- `preset_disabled`：上一个整合包自动停用的条目。
- `USER`：用户手动停用。
- `SAFETY`：宿主 kill switch。

接入时必须保证：

1. `presetDisabled()` 与 `desired_entries` 合并，不能只信可能为空/过期的 `preset_disabled`。
2. 应用整合包时，目标里的 `required` 条目从 `userDisabled` 排除，否则切回 HITA Android 会被旧 UI 的自动停用卡住。
3. DEX kill switch 仍优先于预设。
4. 旧组合独有的 UI/内置插件可以停用；旧组合独有的 DEX 不自动停用，避免误删重数据。

三次接入中实际踩到的坑：

- HITA-L → HITA Android：17 个 UI 插件被旧组合自动停用，`blockingRequiredDisabled` 直接中止。
- 修复后，`applyPreset` 显式启用目标 required 条目；`presetDisabled` 补历史迁移。
- HITA-L 的 5 个错误 tabs 被 `df16f1d` 写入索引，导致 pager 5 页、底栏 3 项；`c0a51b2` 增加 `isHitaLFamily` 保护并从索引删除。

## 6. 校验清单

一次整合包接入至少验证：

- [ ] 三份索引一致：preset id、version、refs、tabs、iconAsset。
- [ ] `:verifyMarketIndex` 通过。
- [ ] `MarketIndexTest`、`PresetTabsParseTest`、Composition 相关单测通过。
- [ ] 冷启动恢复 active preset。
- [ ] 应用后底栏数量、顺序、图标与索引一致。
- [ ] 失败时旧预设、旧底栏、旧插件仍可用。
- [ ] 从目标预设切回 HITA-L/CUTE/Android，不丢状态。
- [ ] 可选的缺失插件只记录失败，不阻塞激活。
- [ ] HITA Android 的 UI 插件 required 状态与索引一致。
- [ ] 升级旧版本后，缺 `tabs_json` 的存量用户能通过 `PresetLayoutSync` 补写。

## 7. 新预设的最小模板

### 原生三页型

```json
{
  "id": "example-1.0.0",
  "name": "Example 1.0.0",
  "version": "1.0.0",
  "minHostApi": 11,
  "plugins": [
    {"id": "com.stupidtree.hitax.timetable", "kind": "dex", "required": true},
    {"id": "com.stupidtree.hita.ui.timetable", "kind": "ui", "required": true},
    {"id": "com.stupidtree.hitax.eas", "kind": "dex", "required": true},
    {"id": "com.stupidtree.hita.ui.eas", "kind": "ui", "required": true}
  ]
}
```

宿主还需要在 `isHitaLFamily()` 中加入 id，否则会追加插件市场/插件 tab。

### 固定底栏型

```json
{
  "id": "example-android-1.0.0",
  "name": "Example Android 1.0.0",
  "version": "1.0.0",
  "minHostApi": 11,
  "tabs": [
    {"id": "hita.timeline", "title": "今日", "order": 0, "iconAsset": "builtin:today"},
    {"id": "hita.more", "title": "更多", "order": 1, "iconAsset": "builtin:navigation"}
  ],
  "plugins": [
    {"id": "com.stupidtree.hitax.timetable", "kind": "dex", "required": true}
  ]
}
```

宿主需要补充 Shell 映射、tab id → UI 插件/基页映射和未知 id 的 fallback。

## 8. 结论

三次接入沉淀下来的规则：

1. **预设描述组合，不打包代码。**
2. **先决定宿主形态，再写 tabs；不要用 tabs 补宿主分支。**
3. **核心服务用 dex，界面用 UI 插件，底栏图标优先 builtin。**
4. **状态必须原子提交，失败不能半切换。**
5. **自动停用与用户停用必须分开记录。**
6. **索引三份同步，引用和版本必须可校验。**
7. **停止维护的发行版只适合做 preset 或 UI 参考，不适合整包热插。**

下一次接入新发行版时，按第 3、6、7 节执行，不要从上游 Activity 逐页搬代码。
