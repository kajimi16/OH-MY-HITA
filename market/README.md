# 插件市场索引

客户端通过 `PLUGIN_INDEX_URL` 读取这个目录下的 `index.json`（CI 构建默认指向
`https://raw.githubusercontent.com/kajimi16/OH-MY-HITA/master/market/index.json`）。

## 目录结构

```
market/
├── index.json      # 索引：插件列表 + 每个插件的 dex 地址与 sha256
└── hello.dex       # 示例插件的 dex（由 :sample-plugin 生成）
```

## 索引格式

```json
{
  "plugins": [
    {
      "id": "com.example.hello",
      "name": "夜间模式（示例）",
      "version": "0.1.0",
      "apiVersion": 2,
      "url": "hello.dex",
      "sha256": "3fd0ef21…",
      "entryClass": "com.example.hello.HelloPlugin"
    }
  ]
}
```

- `url` 可以是相对路径（相对索引所在位置）或绝对 HTTPS 地址
- `sha256` 是完整性校验的唯一依据，必须与文件一致，否则安装会被拒绝
- `apiVersion` 必须 ≤ 客户端的 `HITA_API_VERSION`（当前 2）
- `entryClass` 是 dex 中实现 `com.stupidtree.hita.api.Plugin` 的类

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

- 传输走 HTTPS，内容以索引里的 `sha256` 为准；**索引本身的写权限就是发布权限**
- 运行时插件与宿主同进程，拥有宿主全部权限——只上架可信代码
- 插件不能声明四大组件、不能携带资源，只能通过 `:api` 契约贡献服务 / 同步表 / 导航项
- 破坏性契约变更会递增 `HITA_API_VERSION`，客户端会拒绝 `apiVersion` 过高的插件
