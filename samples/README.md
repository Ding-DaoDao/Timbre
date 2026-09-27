# 接口源开发套件（.jdr）

这里是播放器「接口源」功能的示例与开发文档目录。一个**接口源** = 一个 `.jdr` 文件  
（本质是 ZIP），里面装着一份 `manifest.json`（接口信息）+ 一个或多个 `.js` 脚本  
（实现 搜索 / 获取章节 / 音频直链 三个阶段）。脚本由播放器内置的 QuickJS 沙箱在  
**设备本地**执行，请求直接发往各接口自己的服务器，与播放器内置的任何在线服务无关。

## 目录导航

| 文件                                                            | 说明                                                                  |
| ------------------------------------------------------------- | ------------------------------------------------------------------- |
| [extension-dev-guide.md](extension-dev-guide.md)              | **接口源开发指南**（主文档）：如何编写接口（含 Python→JS 对照、加密桥用法、防盗链头）、如何打包 .jdr、如何导入验证、常见坑 |
| [abts-bili/](abts-bili/)                                      | **完整真实示例**：B站有声源（Cookie 风控预热 + Wbi 签名 + 防盗链 `{url, headers}`）——Web API 类站点的参考模板 |
| [\_template.js](_template.js)                                 | **空白脚本模板**：复制它开始写你自己的接口，注释里有 manifest.json 的完整字段说明                  |
| [demo-source/](demo-source/)                                  | **最小示例包**：一个只依赖普通 HTTP 接口的完整源（manifest.json + demo.js），演示标准字段与三阶段结构 |
| [../scripts/pack\_jdr.py](../scripts/pack_jdr.py)             | **打包脚本**（Python 版）：把目录压成 .jdr                                       |
| [../scripts/pack\_jdr.main.kts](../scripts/pack_jdr.main.kts) | 打包脚本（Kotlin 版，二选一）                                                  |

## 三步上手

1. **写接口**：复制 `_template.js`（或直接以 [abts-bili/](abts-bili/) 为模板），实现  
   `search / chapters / audio` 三个 `async` 函数。  
   标准字段与三阶段的入参出参，见 [开发指南 · 契约对照](extension-dev-guide.md#一契约对照python--js)。
2. **配 manifest**：包 id 与源 id 的规则、多接口合一的写法，  
   见 [开发指南 · 打包](extension-dev-guide.md#四打包成-jdr) 与 [demo-source/manifest.json](demo-source/manifest.json)。
3. **打包导入**：`python scripts/pack_jdr.py 你的目录 输出.jdr`（或把目录压成 zip 改后缀），  
   播放器 **设置 → 接口源 → 导入 .jdr 文件 / 从链接导入**。

## 音频返回的三种形态（重要）

| 写法 | 用途 |
| --- | --- |
| `return 'https://...mp3'` | 普通CDN:无特殊请求头 |
| `return { url: 'https://...' }` | 等价于上一种 |
| `return { url: '...', headers: { Referer: '...' } }` | **防盗链CDN**(B站等):拉流时播放器自动带上头 |

遇到"搜索正常但播放 403"的站点，就是最后一种：参考 [abts-bili/abts.js](abts-bili/abts.js) 的  
audio 阶段，把站点要求的 Referer/UA/Cookie 放进 `headers` 一起返回，其余交给播放器。  
完整说明见 [开发指南 · 示例源C](extension-dev-guide.md#三五示例源-cb站有声web-api--wbi-签名--防盗链-referer-头)。

## 沙箱能力速览

脚本里可以直接用的全局函数（完整说明见指南）：

- 网络：`http.get(url, opts)` / `http.post(url, opts)` → `{status, headers, body}`
- 摘要：**`md5Hex`&#x20;**`sha256Hex` `hmacSha256Hex`
- 编码：`base64Encode/Decode/ToUtf8` `hexToBytes` `bytesToHex` `utf8ToBytes` `bytesToUtf8`
- 加密：`aesEcbEncryptB64/Decrypt` `aesGcmEncrypt/Decrypt`  
  `chacha20Poly1305Encrypt/Decrypt`（12 字节 nonce = ChaCha20，24 字节 = XChaCha20）  
  `sm4EcbEncrypt/Decrypt`
- 杂项：`randomBytes` `timestamp` `timestampMs` `urlEncode/Decode` `log`

## 注意事项

- 源 id（`sources[].id`）**全局唯一**且须与脚本里 `registerSource({ id })` 一致；  
  同一源 id 不能同时存在于两个已导入的包。
- 包 id（`manifest.id`）可以随意改（小写字母/数字/`._-`），同名包重复导入 = 更新。
- 两个源可以封装进**同一个 .jdr**（`sources` 数组多项、多个脚本文件），参考指南打包一节。
- 源站证书链损坏时，在 manifest 里加 `"allowInsecure": true`（只影响该包）。
