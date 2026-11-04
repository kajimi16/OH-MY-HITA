# 插件市场索引

客户端通过 `PLUGIN_INDEX_URL` 读取这个目录下的 `index.json`（CI 构建默认指向
`https://raw.githubusercontent.com/kajimi16/OH-MY-HITA/market/market/index.json`）。

## 目录结构

```
market/
├── index.json      # 索引：逻辑插件 + 界面插件
├── hello.dex       # 示例逻辑插件的 dex（由 :sample-plugin 生成）
└── course-resource-0.4.8.apk   # 课程资源界面插件（示例，实际可放任意 HTTPS 地址）
```

## 索引格式

```json
{
  "plugins": [
    {
      "id": "com.example.hello",
      "name": "示例逻辑插件",
      "version": "0.1.0",
      "apiVersion": 2,
      "url": "hello.dex",
      "sha256": "3fd0ef21…",
      "entryClass": "com.example.hello.HelloPlugin",
      "description": "可选的插件说明",
      "changelog": "可选的更新日志",
      "size": 3352,
      "minHostApi": 2
    }
  ],
  "uiPlugins": [
    {
      "id": "com.stupidtree.hita.ui.resource",
      "name": "课程资源",
      "version": "0.4.8",
      "url": "course-resource-0.4.8.apk",
      "sha256": "8fc623f8…",
      "description": "课程资料浏览与搜索",
      "changelog": "首个市场版本",
      "size": 647061,
      "minHostApi": 3
    }
  ]
}
```

- `url` 可以是相对路径（相对索引所在位置）或绝对 HTTPS 地址
- `sha256` 是完整性校验的唯一依据，必须与文件一致，否则安装会被拒绝
- `apiVersion` 必须 ≤ 客户端的 `HITA_API_VERSION`（当前 3）
- `entryClass` 是 dex 中实现 `com.stupidtree.hita.api.Plugin` 的类
- `description` / `changelog` / `size` / `minHostApi` 可选；旧客户端会忽略，新客户端用于详情弹窗与兼容性检查
- `uiPlugins` 的 APK 必须包含 `assets/plugin.json`，字段为 `id`、`name`、`version`、`entryClass`、
  `apiVersion`、`composeVersion`、`capabilities`；`composeVersion` 需与宿主一致

## 界面插件的安装方式

界面插件（Compose 容器）**只能通过市场索引的 `uiPlugins` 安装/更新**；
客户端不再提供「从本地选 APK」的入口（避免任意 APK 的侧载风险）。
开发调试可用 debug 包的广播通道：`scripts/dev-install-ui-plugin.sh`。

## 插件模板

从这里开始最省事：<https://github.com/kajimi16/hita-plugin-template>

克隆后改 `DemoPlugin` 的 id，执行 `./gradlew :plugin:publishToMarket` 即得到 dex 与索引。

## 发布一个插件

仓库内的示例插件：

```bash
./gradlew :sample-plugin:publishToMarket
# 重新生成 market/hello.dex，并同步 index.json 里的 sha256
```

第三方插件（不属于本仓库）：

1. 用 `compileOnly` 依赖 `:api` 编译，产出 `classes.jar`
2. `d8 --lib android.jar --min-api 23 --output out/ classes.jar`
3. 上传 dex 到可访问的 HTTPS 地址（仓库、Release 资产、自己的 CDN）
4. 在 `index.json` 里加一条记录，填 `url` 与 `sha256`
5. 提 PR

## 信任模型与限制

- **所有插件默认「未验证」**：安装或打开前会弹出提示，说明它将拥有应用完整权限
- 只有与 OH-MY-HITA 一起核验过的半官方插件，才加入 `app/src/main/assets/verified_plugins.json`
  的白名单并显示「已验证」；白名单随应用版本发布，修改需要发版
- 传输走 HTTPS，内容以索引里的 `sha256` 为准；**索引本身的写权限就是发布权限**
- 运行时插件与宿主同进程，拥有宿主全部权限——只上架可信代码
- 逻辑插件不能声明四大组件、不能携带资源，只能通过 `:api` 契约贡献服务 / 同步表等能力
- 界面插件同样不注册组件、不合并资源，由宿主 Compose 容器渲染；APK 只提供代码与 `plugin.json`
- 破坏性契约变更会递增 `HITA_API_VERSION`，客户端会拒绝 `apiVersion` 过高的插件
