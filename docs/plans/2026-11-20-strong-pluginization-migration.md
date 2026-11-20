# 强插件化迁移设计

日期：2026-11-20  
目标：宿主成为空壳；官方插件与官方整合包独立分发、独立版本、独立更新。

## 1. 现状结论

已通过两种实验确认：

1. 停用全部 5 个 builtin：宿主能启动，今日/时间表显示不可用，功能中心与市场仍可进入。
2. 从 `app/build.gradle` 移除 5 个 `runtimeOnly` 插件模块：`app:assembleDebug` 成功，安装后同样可启动。

所以宿主不是“离开插件就崩溃”，但当前 builtin 仍不是强插件：

- 代码/资源/Manifest 组件打进同一个 APK。
- 同一进程、同一签名、同一权限。
- 禁用只关闭组件，不缩小包体；不能独立升级。
- `plugins.json` 是宿主固定资产。
- 三个官方整合包直接依赖这些 builtin。

## 2. 目标组织布局

建议 GitHub 组织 slug：`oh-my-hita`（显示名可写“OH-MY-HITA 插件组”）。

```text
oh-my-hita/
├── plugin-sdk                 # API 10 的 api/model/component 固定版本产物
├── plugin-template            # 通用 T1/T2 模板 + CI
├── official-plugins/           # 不建议做 monorepo；以下是仓库名
│   ├── ui-ai
│   ├── ui-announcement
│   ├── ...
│   ├── timetable
│   ├── eas
│   ├── resource
│   ├── user
│   └── theta
├── official-presets            # hita-l / hita-cute / hita-android 索引
└── registry                    # 官方插件市场索引
```

插件 ID 保持现有反向域名，不因仓库拆分改变；否则整合包和历史数据全部失效。

### 2.1 契约分发

不能把 `api/model/component` 的 AAR 复制到每个插件仓库。独立仓库必须从
`oh-my-hita/plugin-sdk` 获取固定版本：

```text
io.github.kajimi16.ohmyhita:plugin-api:10
io.github.kajimi16.ohmyhita:plugin-model:10
io.github.kajimi16.ohmyhita:plugin-component:10
```

`plugin-sdk` 负责：

- 从宿主发布 AAR/POM/校验和；
- 只追加版本，不原地覆盖；
- 模板和所有插件仓库只依赖坐标，不提交 `libs/*.aar`；
- API 升级时发布新 SDK 版本，插件按需升级。

当前模板里的 `libs/*.aar` 只是迁移期的临时兜底，批量建仓前必须移除。

### 2.2 三条插件轨道

| 轨道 | 能力 | 模板 |
|---|---|---|
| T1 | 逻辑服务、同步、无界面 | `plugin` |
| T2 | Compose 界面，不能声明组件/资源 | `ui-plugin` |
| privileged | Activity/Widget/Service/资源，需宿主承载或 T3 | 不进通用模板 |

`timetable/eas/resource/user/theta` 不能直接套 T1/T2 模板；它们属于 privileged
track，必须先拆成 T1 core + T2 UI，并把 Widget/Service/资源交给宿主或新增 T3。

## 3. 迁移矩阵

### 3.1 可直接按 T2 迁移的 UI 插件

以下 17 个已有独立 APK 形态，源码在 `plugin-ui-*`，只依赖 `:api` 与 Compose：

```text
ai announcement appearance community compare credit group homework
links live news notice reminder score share training update
```

迁移步骤：

1. 每个插件新建仓库，使用 `oh-my-hita/plugin-template` 的 `ui-plugin`。
2. 保留 `plugin.json` 的 id、entryClass、capabilities、mainTabs。
3. 版本、变更日志迁移到插件仓库。
4. CI 产出 APK、sha256，并更新官方 registry。
5. 官方整合包改为引用这些插件 ID，不再引用宿主内同名模块。

### 3.2 必须做宿主改造的 builtin

| 插件 | 当前内容 | 迁移阻塞 |
|---|---|---|
| `timetable` | Room/DAO、同步、Activity、Widget receiver/service、XML/资源 | T2 不能声明组件/资源；Widget 与 Activity 需要宿主承载或新增 T3 |
| `eas` | 登录/成绩/空教室/考试 Activity、网络 | 登录 WebView 与 Activity UI 需改为 Compose/T2，或定义宿主登录 API |
| `resource` | 课程资源 Activity、网络、WebView | Activity/资源需改 T2 UI |
| `user` | 个人主页 Activity | Activity 需改 T2 UI |
| `theta` | 社区 Activity 集合、WebView、图片资源 | T2 不能声明 Activity/资源；需要 T2 Compose 重写或 T3 完整 APK |

结论：**这 5 个不能通过复制源码变成 T2 插件**。必须先补宿主契约：

- Widget/Receiver/Service 是否由宿主统一承接，或新增 T3（可声明组件但独立安装）插件类型。
- 登录、导入课表、成绩/空教室网络能力拆成 T1 服务。
- 页面改 Compose T2，或保留 T3 APK + host intent 路由。
- 同步表、数据库迁移、通知、提醒、默认 tab 由谁负责。

### 3.3 官方整合包

当前三套：

- `hita-l-1.5.3`
- `hita-cute-1.6.0`
- `hita-android-3.1.2`

迁移后整合包只声明插件 ID/版本约束，不引用宿主 builtin：

```json
{
  "id": "hita-l-official",
  "shell": "hita-l",
  "plugins": [
    {"id": "com.stupidtree.hitax.timetable", "kind": "t2"},
    {"id": "com.stupidtree.hitax.eas", "kind": "t2"}
  ]
}
```

`minHostApi` 保持 API 10；不要再用 API 小版本表达 App 版本。

## 4. 分阶段

### Phase 0：模板（已完成）

- `hita-plugin-template` 已适配 API 10、Kotlin 1.9.24、Compose Compiler 1.5.14。
- 本地 `publishMarket` 成功产出 T1 dex + T2 APK。

### Phase 0.5：plugin-sdk

- 建立 `oh-my-hita/plugin-sdk`，发布 API 10 的固定坐标与校验和。
- 模板改用 Maven 坐标，删除 `libs/*.aar`。
- 每个插件仓库通过同一 SDK 版本构建。

### Phase 1：空壳宿主

- 移除 5 个 `runtimeOnly` 和 `plugins.json` 中的 builtin。
- 默认预设改为“无插件空壳”。
- 市场默认源改为官方 registry。
- 保留 API/kernel/model/component/user/sync/style。
- 验收：没有插件时能进入市场并安装任意官方插件。

### Phase 2：17 个 UI 插件仓库化

- 按 `ui-*` 仓库拆分，CI 发布 APK。
- 官方 registry 发布。
- 官方整合包引用插件版本。
- 验收：空壳 + 官方整合包能恢复 HITA-Android 的全部 UI 页面。

### Phase 3：5 个 builtin 拆分

- 先定 T2/T3 边界。
- `timetable` 的数据库、同步、Widget、通知优先留宿主或 T1。
- `eas/resource/user/theta` 页面改 T2，或全部走 T3。
- 每个仓库独立版本、独立 CI、独立 release。

## 5. 外部阻塞

当前 `gh` token：

```text
scopes: gist, read:org, repo, workflow
```

没有组织创建权限；GitHub 也没有公开的创建组织 API。需要用户：

1. 在 GitHub 创建 `oh-my-hita` 组织，并把 `kajimi16` 设为 owner；
2. 确认组织 slug（`oh-my-hita` 当前 404，可用）；
3. 之后由本会话批量创建仓库、推送模板和插件仓库。

## 6. 建议的验收命令

```bash
# 空壳宿主
./gradlew :app:assembleDebug -PpluginIndexUrl=<official-registry>
# 模板
./gradlew publishMarket
# 插件仓库
./gradlew :ui-plugin:assembleDebug
```
