# 插件主界面 tab 哈希的二次扫描优化

## 背景

`UiPluginManager.mainTabs()` 在每个主界面构建周期运行。它原来的实现分成两层：

1. `enabled()` 调用 `installed()`，后者对每个已安装 UI 插件 APK 做一次 SHA-256 校验。
2. 对每个启用插件调用 `compatibility(it)`，而 `compatibility()` 内部再次调用 `installed()`。

因此一次 `mainTabs()` 的哈希次数是 `N + N×N`（N 个 tint 插件），而不是 `N`。
22 个 UI 插件时，一次主界面构建会重复读取并哈希约 22×23 个 APK。
这些调用位于 `Application`/`Activity` 启动路径，直接计入冷启动。

## 设计

### 数据流

```
UiPluginStore.list()
        │
        ▼
installed() ── 每个 APK 一次 isUsable/SHA-256
        │
        ├── installedById: Map<id, InstalledUiPlugin>
        │
        ▼
mainTabs()
  filter enabled / kill switch
  compatibility(record, installedById)
  flatMap MainTabSpec
  sortedBy order
```

### 契约

`mainTabs()` 前置条件：

- `UiPluginStore` 可读。
- `CompositionStore` 可读。

后置条件：

- 返回的插件集合、tab 顺序、`enabled` 状态与优化前一致。
- 每个 APK 在一次 `mainTabs()` 中最多校验一次。
- kill switch 仍会过滤对应插件。

不变式：

- `compatibility()` 的依赖查询只使用传入快照，不再隐式全量扫描。
- 文件校验仍是 SHA-256；没有跳过完整性校验。

违约处理：

- `installed()` 内部的损坏/缺失插件继续被过滤。
- `compatibility()` 缺服务、缺依赖、未验证预设仍返回 `satisfied=false`。

### 备选方案与否决理由

| 方案 | 否决理由 |
|---|---|
| 按 `path+size+mtime` 缓存 SHA | 更快，但把文件完整性判断降级为元数据判断；本期不引入安全语义变化 |
| 把插件恢复移到后台线程 | 改动生命周期和 host-ready 时序，风险高于本次目标 |
| 直接删掉重复校验 | 会让 manifest 组件与已安装状态不一致，否决 |
| 只优化 `enabled()` | 无法消除 `compatibility()` 的 N 次全量扫描 |

## 基准

设备：Android API 36 模拟器，debug APK，相同应用数据，同一 `ActivitySplash`。
每轮交替安装 baseline / optimized，`am force-stop` 后 `am start -W`。

| round | baseline TotalTime | optimized TotalTime |
|---|---:|---:|
| 1 | 4904 ms | 2365 ms |
| 2 | 4578 ms | 2199 ms |
| 3 | 3690 ms | 1662 ms |

- baseline 中位数：4578 ms
- optimized 中位数：2199 ms
- 中位数下降：2379 ms，约 52%
- 三次交错测量 optimized 均低于 baseline

冷启动包含 `Application` 初始化、插件恢复和首屏构建；这次变化只减少重复哈希，
没有改变插件加载、服务注册或 tab 输出。

## 验收

- `:app:assembleDebug` 通过。
- `:app:testDebugUnitTest` 通过。
- 真机/模拟器：今日、时间表、待办、助手、资讯、更多六个 tab 均正常渲染。
- 没有新增依赖、没有 schema 变更、没有缓存失效策略。

## 保留结论

保留该优化。收益来自消除 `compatibility()` 内的隐式全量 `installed()`，
不是缓存或跳过校验，因此正确性边界保持不变。
