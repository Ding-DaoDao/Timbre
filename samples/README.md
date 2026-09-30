# 接口源开发套件（.jdr）

## 🤖 AI 一键编写接口源

不想手写？把下面的提示词**整段复制**给任意 AI（ChatGPT / Claude / Gemini 等），并在末尾追加你目标站点的  
**接口信息**（抓包或接口文档：搜索/章节/音频三个接口的 URL、方法、参数、请求头、签名加密算法、响应 JSON 示例），  
AI 会直接产出 `manifest.json` + 源脚本两个文件，按 [三步上手](#三步上手) 打包导入即可。

<details>
<summary><b>点开复制完整提示词</b></summary>

```text
你是 Timbre 播放器「接口源」的开发专家。请根据我在末尾提供的接口信息，编写一个完整可用的
接口源，直接输出两个文件的完整内容（不要省略、不要留占位符）：
  1. manifest.json —— 包描述 + 源声明
  2. 源脚本 xxx.js

【运行环境】
脚本运行在播放器内置的 QuickJS 沙箱：支持现代 JS 语法（async/await、箭头函数、模板字符串），
但没有 DOM、fetch、XMLHttpRequest、setTimeout；网络只能用下面的 http 桥，其他能力只用
下面列出的沙箱全局函数。

【脚本结构】
;(function () {
  'use strict';
  registerSource({
    id: '源id',                     // 必须与 manifest 里 sources[].id 完全一致
    async search(params) { ... },    // 搜索
    async chapters(params) { ... },  // 章节
    async audio(params) { ... },     // 音频直链
  });
})();
任一阶段失败都 throw new Error('中文原因')，会显示在界面上；调试输出用 log(...)。

【三阶段契约】
- search 入参 { keyword, page, limit }
  返回 [{ id: '字符串', bookTitle, bookImage?, bookAnchor?, bookDesc?, count?, heat?, ...自定义字段 }]
- chapters 入参 { bookId, page, size }（已合并 search 返回的自定义字段）
  返回 [{ chapter_id: '字符串', title, order?, duration?, ...自定义字段 }]
- audio 入参 { bookId, chapterId }（已合并 chapters 返回的自定义字段）
  返回 'https://....mp3'
    或 { url: 'https://...' }
    或 { url: 'https://...', headers: { Referer: '...', 'User-Agent': '...' } }
  第三种用于防盗链 CDN 站点：这些头由播放器在拉流时自动携带，脚本不要自己下载音频。
- 跨阶段传值：把下一阶段需要的字段（内部专辑 id、章节序号等）直接放进返回对象，会自动
  透传到下一阶段的 params；透传缓存只在进程内，脚本要做兜底（拿不到就重新请求）。

【沙箱全局 API】
- 网络：await http.get(url, { params, headers, timeoutMs })
        await http.post(url, { params, body, json, headers, timeoutMs })
  返回 { status, headers, body }。params 自动 URL 编码拼 query；json 自动序列化并设
  Content-Type；body 传字符串或 Uint8Array；响应自动解 gzip；响应 headers 键全小写
  （多个 set-cookie 被用 ", " 拼成一个值）；不要手动设 Accept-Encoding。
- 摘要：md5Hex(s)、sha256Hex(s)、hmacSha256Hex(key, s)
- 编码：base64Encode / base64Decode / base64ToUtf8、hexToBytes、bytesToHex、
  utf8ToBytes、bytesToUtf8、urlEncode、urlDecode
- 加密：aesEcbEncryptB64(明文, 密钥) / aesEcbDecryptB64（PKCS#7 + base64，密钥传字符串
  或 Uint8Array）；aesGcmEncrypt(key, nonce, data) / aesGcmDecrypt（GCM 返回拼好 ct||tag
  的 Uint8Array）；chacha20Poly1305Encrypt / Decrypt（nonce 12 字节 = ChaCha20，
  24 字节 = XChaCha20）；sm4EcbEncrypt / sm4EcbDecrypt（GB/T 标准 SM4）
- 杂项：timestamp()（秒）、timestampMs()（毫秒）、randomBytes(n)（Uint8Array）、log(...)
- 没有定时器：需要限速/延时时，用 timestampMs() 忙等实现 sleep。

【manifest.json 格式】
{
  "id": "com.example.包名",      // 小写字母/数字开头，仅 a-z 0-9 . _ -，全局唯一
  "name": "包显示名",
  "version": "1.0.0",
  "author": "作者",
  "description": "一句话描述",
  "allowInsecure": false,         // 仅源站证书链损坏时设 true
  "sources": [{
    "id": "源id",                 // 全局唯一，与脚本 registerSource({id}) 一致
    "name": "搜索页 chip 显示名",
    "script": "xxx.js",           // 包内脚本文件名
    "capabilities": ["search", "chapters", "audio"]
  }]
}

【编写要求】
1. 严格按我提供的接口信息实现：字段名、参数拼接顺序、加密签名算法、时间戳单位（秒/毫秒）
   都不要改动；接口信息没写到的细节做合理实现，并在注释里标注你的假设。
2. 每个阶段先检查 resp.status，非 2xx 抛出带状态码的中文错误；解析前用
   log(resp.body.slice(0, 200)) 输出响应片段便于排障。
3. Web 类站点带浏览器 User-Agent；需要 Referer / Cookie 的按接口信息带上；站点有 Cookie
   风控时，先请求一次首页收割 Set-Cookie 再调接口。
4. 只输出两个文件的完整代码（用注释或文件名标注哪个是哪个），不要输出解释性文字。

========== 接口信息（抓包/文档资料粘贴在下面）==========

（在这里粘贴：站点首页地址、搜索/章节/音频接口的完整 URL、请求方法与参数、
请求头、签名或加密算法说明、响应 JSON 示例）
```

</details>

---

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
