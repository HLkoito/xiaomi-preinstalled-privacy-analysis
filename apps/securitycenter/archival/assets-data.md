> **历史分析存档，非最终结论。** 本文保留原始分析的技术内容，可能包含尚未验证或已被纠正的表述。隐私数据上传范围、同意条件、FBO 本地委托、报警短信加密、Provider 鉴权等，以 [最终报告](../REPORT.md) 与 [独立复核](../REVIEW.md) 为准。文中源码定位保留为原反编译相对路径；只有 evidence/ 中列出的精选文件随包提供，未打包完整源码树。

# MIUISecurityCenter 内置数据资产 逆向调查报告

> 调查者：teammate `assets-data`　｜　目标 APK：`MIUISecurityCenter-10.4.9-250402.0.1.apk`
> SHA-256 `fc63de0eb68016fc2c7469ec6d33364ffef073643fea9e86781e5fcb1c678cfd`
> 范围：`assets/` 全部、`res/raw`、`res/xml`、`META-INF/`。**不含** Java 业务代码行为分析（另行负责）。
> 标注纪律：`观测` = 直接来自 shipped artifact / 实际解码输出；`推断` = 由证据推出的结论；`未知` = 未取得证据。

---

## 0. 结论摘要（可直接摘录）

| # | 结论 | 性质 |
|---|---|---|
| 1 | `assets/` 共 **248 个文件 / 9,409,463 字节 (8.97 MiB)**。其中 18 个无扩展名文件占 4.39 MiB（含 `dict` 3.64 MiB）。 | 观测 |
| 2 | **`assets/dict` 并非加密**：它是 **jieba 中文分词词典的自定义二进制序列化**，格式在 `mj/a.java:260-330`（`h(InputStream)`）里逐字段写明。已用 Node 完整解析：**348,982 条词 + 35,224 条单字表**，`totalFreq=60,101,983`，`minFreq=2`，HMM 参数与开源 jieba `prob_start`/`prob_trans` **逐位相同**。 | 观测（见 §3） |
| 3 | **`assets/pinyinindex.idf` 并非加密**：它是 `cl/a.java` 定义的 **"IDF " 表格格式（version 2）**。已完整解析：20,902 个汉字 → 小写拼音（含多音字），1,336 条去重字符串。样本：`中→zhong`、`重→zhong,chong`、`长→zhang,chang`、`单→dan,chan,shan`。 | 观测（见 §4） |
| 4 | **`assets/character.json` 是明文 JSON**（600 B），40 条 `字符→id`（`<pad>=0,<unk>=1,a=2 … 9=37, .=38, _=39`），是 `rec_model_20230425.mnn` 的字符词表。基线里"非明文"的判断需修正。 | 观测（见 §5） |
| 5 | **`assets/garbage_clean.db` 的 4 行示例数据全部是十六进制混淆串**，不是人类可读路径；`t_ad` / `t_residual` **各 0 行**（只有表结构）。且**三个 dex 中都不存在字符串 `garbage_clean.db`**，也无任何 `t_cache` 表名引用 → 本 APK 内**没有读取者**。 | 观测（见 §2） |
| 6 | 真正的可读垃圾清理规则库在 **`res/raw/parse.db`**（apktool 名 `parse.db`，ZIP 内实际名 `res/rKy.db`）：`DIR_PARSE(PATH, PACKAGE_NAME, APP_NAME)` **1043 行 / 884 个包**，明文；`TEMP_DIR` 10 行。由 `ra/e.java` 复制到 `getDatabasePath("parse.db")` 后查询。 | 观测（见 §6） |
| 7 | **`assets/censor_source` 是高熵二进制**（2688 B，熵 7.933，16 字节对齐），**没能解出明文**。调用链已完全复原：`com.miui.riskcontrol.SensitiveWordDetectProvider`（ContentProvider，**只允许 `com.android.camera` / `com.miui.mediaeditor` 调用**）→ `setAssetManager()` + native `hasSensitiveWords()` → `liblib-riskcontrol.so` 的 `SensitiveWordDetector::load` + 自实现 `AES` + `TrieTree`；.so 内含字符串 `censor_source`、调试路径 `/storage/emulated/0/censor_source`、`AssetManager open fail!`、`needPadding %u char(s)`、`File has %d lines`。**Java 侧不传密钥**，故密钥必须内置在 .so；穷举 .so 内全部 16/32 字节窗口（约 49 万）作 AES-128/256 ECB/CBC 密钥并用 PKCS5 补位+全文校验 → **0 通过**。 | 观测 + 未知（见 §7） |
| 8 | `assets/rec_model_20230425.mnn` = **阿里 MNN FlatBuffers 模型**，已从 flatbuffer 字符串还原完整层图：`sparse_inputs_1..6`(int[30]) + `sparse_inputs_time` → `embedding_0..8` → `p2o.MaxPool` → `relu_0..4` → `linear_0..4` → 输出 `linear_9.tmp_1`（2 分类）。即"下一个 App 预测"排序模型，由 `ia/j.java` 用 `MNNNetNative.nativeCreateNetFromFile` 加载并可 `MNNTrainNative` 端侧再训练。 | 观测（见 §8） |
| 9 | `assets/model`（680,154 B）格式**未知**：头为 int32BE `1,0,5,68,58` + uint16 表，正文以 `0xC0*` 字节为主（float32BE 落在 `[-6,0]` 的比例 36.8% vs LE 18.4%）→ 推断为 float32 权重矩阵 + 整型/索引头。**没有任何 Java 代码以字面量打开它**（只在 `lj/c.java:17` 的名字数组 `{"dict","model","pattern","phish"}` 里出现）。 | 推断 + 未知（见 §9） |
| 10 | `assets/warningcenter/` **71 个图标**。`assets/warning_center_disaster_icon`（JSON，21 个灾害码）共 75 条 `code→文件名` 映射，**全部命中真实文件**；其中 `warningcenter_fire_*` 被 **`11G01`(森林火灾) 与 `11G05`(草原火灾) 共用**（4 个文件 × 2 = 8 条映射）。完整 code→灾害名映射见 `com/miui/warningcenter/disasterwarning/model/EventType.java`（25 个枚举值，含台风/暴雨/暴雪/寒潮/冰雹/大风/高温/大雾/霾/道路结冰/重污染/海浪/地震/风暴潮/雷雨大风/森林火险/地质灾害气象风险/洪水/内涝/山洪/滑坡/泥石流/森林火灾/草原火灾/其他）。图标由 `WarningCenterDisasterDetailActivity` 直接 `getAssets().open("warningcenter/"+name)` 加载。 | 观测（见 §10） |
| 11 | 签名：**v1 + v2 + v3**，单一签名者 = **MIUI 平台证书**（`C=CN, ST=Beijing, L=Beijing, O=Xiaomi, OU=MIUI, CN=MIUI, emailAddress=[VENDOR_EMAIL_REMOVED]`，序列号 `E552A8ECB9011B7C`，2011-12-06 → 2039-04-23，RSA），SHA-256 指纹 `c9009d01ebf9f5d0302bc71b2fe9aa9a47a432bba17308a3111b75d7b2149025`；v1 文件为 `META-INF/PLATFORM.SF` + `PLATFORM.RSA`；v2/v3 摘要与当前字节一致。 | 观测（见 §12） |
| 12 | 内置证书另有 2 张：`res/raw/wlan_speed.crt`（`CN=www.miwifi.com` 自签，RSA-1024，2020→2030）与 `res/raw/wlan_speed_new.crt`（`OU=MIWIFI ROOT CA`，由 `CN=Xiaomi Root CA` 签发，RSA-2048，2020→2120），被 `res/xml/network_security_config.xml` 指定为 `miwifi.com` 域的信任锚。 | 观测（见 §11） |

**对共享基线的 3 处修正**（请 Lead 采纳）：
1. `assets/address.json`、`assets/pattern` 是 **UTF-8**，不是 GBK（GBK 解码会得到 `銆怺` 之类乱码；按 UTF-8 解码 `address.json` 得 `"n":"北京"`、`pattern` 得 `"version":"1.2"`）。
2. `assets/character.json` 是**明文** JSON。
3. `assets/dict` / `pinyinindex.idf` **不是"加密或自定义未知格式"**，而是两种**完全可解析**的自定义二进制（jieba 词典 / IDF 表），本轮已解出全部内容与样本。

---

## 1. 方法与工具（可复现）

| 用途 | 手段 |
|---|---|
| 逐文件识别（magic / 熵 / SHA-256 / JSON 结构） | Node v24.22.1 脚本 `_analysis/tools/scan_assets.js`、`summarize_assets.js`、`assets_summary.tsv` |
| SQLite 结构 + 样本行（证据化） | REA `inspect_sqlite_database`（provider `SQLite 3.53.4`） |
| SQLite 全量计数/全量行 | Node 内置 `node:sqlite`（`DatabaseSync`，readOnly） |
| APK 签名 | REA `inspect_apk_signature` |
| 证书解析 | Node `crypto.X509Certificate` |
| 反编译源码检索 | `_analysis/jadxout/sources/**/*.java`；**必须用 `Get-ChildItem -Recurse` 递归**（`Select-String -Path "dir\**\*.java"` 在 PowerShell 下不递归，会漏掉 `com/miui/**` 深路径文件），共 10,624 个 .java |
| 二进制格式还原 | 阅读 `mj/a.java`、`cl/a.java`、`ea/c.java`、`ia/j.java`、`ra/e.java`，再写 Node 解析器 |
| DEX 字符串交叉验证 | 直接扫 `classes*.dex` byte stream（避免误信 grep 结果） |

**证据 ID（REA）**
- `ev_864f519dd5aad070aa0f4b478bc7a10370fda04e0c347fc87a9ceb717bd1e121` — `garbage_clean.db` 全库 schema
- `ev_e7729c0c5f9049f98f28b5502abc025bbd194d9dd4165b8cb2bc2fb9db7a258a` — `garbage_clean.db` / `t_cache` 4 行
- `ev_24470dcb25764c0ccf0d0a50d01f9beeb3860cec74648b7f2dd3e1312028af19` — `res/rKy.db`(=parse.db) schema
- `ev_774c8c1faf38e245d0c008a0979e7abc3958ee7dd6e00b48ac3bc5de819cb354` — APK 签名

---

## 2. `assets/garbage_clean.db` —— 垃圾清理规则库（表结构真实，数据被混淆）

### 2.1 结构（`观测`，REA `ev_864f…`）

- 文件：36,864 B，SHA-256 `e1d838531cc6f604f77484cb9b95cbf9be41ceb1179b255c7cc27c9d74a4d5a3`，SQLite 3，`database_encoding = UTF-8`，无 WAL、无索引、无视图、无触发器。
- 表：

| 表 | CREATE 语句（原文） | 行数 |
|---|---|---|
| `t_cache` | `CREATE TABLE t_cache (_id integer primary key autoincrement, pkg_name TEXT ,dir_path TEXT ,root_dir TEXT ,res_zh_tw TEXT ,res_zh_cn TEXT ,res_default TEXT ,advise_del TEXT )` | **4** |
| `t_ad` | 同上（同 8 列） | **0** |
| `t_residual` | 同上（同 8 列） | **0** |
| `android_metadata` | `CREATE TABLE android_metadata (locale TEXT)` | 1（`locale = zh_CN`） |
| `sqlite_sequence` | 自动表 | 1（`t_cache` → `seq = 4`） |

> `t_ad` / `t_residual` 只有 schema、没有任何行 → **随包发布的只是"骨架 + 4 条样例"**，实际规则由云端下发或由其他组件填充（推断）。

### 2.2 4 行数据（`观测`，REA `ev_e772…`，hex 已用 Node 复核长度）

| _id | pkg_name (hex, 字节数) | dir_path (字节) | root_dir (字节) | res_zh_tw | res_zh_cn | res_default | advise_del |
|---|---|---|---|---|---|---|---|
| 1 | `b076f58f1ea99d0ec97ec7ac6a907da3` (16) | `fc58f6c501af8103c87dc7b467da6cb574ebb69119eb1d134e3361aef318c9c5a38b058c` (36) | `fc58f6c501af8103` (8) | 178 B | 178 B | 168 B | `0` |
| 2 | `b076f58f1ea99d0ec97ec7ac6a907da3` (16) | `fc54d1f43aefc604867acea529926eb675a0a981` (20) | `fc54d1f43a` (5) | 214 B | 214 B | 178 B | `0` |
| 3 | `b076f58f1ea99d0ec97ec7ac6a907da3` (16) | `fc54d1f43aefaf068b75c3b27fda6cb676b0bf` (19) | `fc54d1f43a` (5) | 205 B | 205 B | 184 B | `0` |
| 4 | `b076f58f0ba989088a7088a36e9461b47ca9` (18) | `fc74f1cd1aa187488478c5a863` (13) | `fc74f1cd1aa187` (7) | 175 B | 175 B | 173 B | `0` |

`res_*` 全部以同一前缀开头（前 63 个 hex 字符相同）：
```
a83bf9cd16b29c388e77c0af24cf2df835e7b8990fea562b5b267daea35bc443
```
`root_dir` 恒为 `dir_path` 的十六进制前缀。

### 2.3 字段语义（`推断`，附依据）

| 字段 | 语义 | 依据 |
|---|---|---|
| `pkg_name` | 目标应用包名的**混淆标识**（16 B = MD5 长度；第 4 行 18 B 不符） | 长度 + 3 行相同值 = 同一 App 的多个目录 |
| `dir_path` | 该 App 的**缓存/垃圾目录**的混淆标识 | 与 `t_cache` 表名、`root_dir` 前缀关系一致 |
| `root_dir` | `dir_path` 的前缀（更粗粒度的根目录 id） | 观测：恒为前缀 |
| `res_zh_tw` / `res_zh_cn` / `res_default` | 该目录在三套 locale 下要展示的**中文/默认名字**（混淆后的长串） | 命名 + 三列同前缀、长度差异 |
| `advise_del` | 是否建议删除（`0`/`1`） | 4 行全 `0`；列名直读 |

### 2.4 混淆性质判定（`观测` + `推断`）

- 全部 4 行 12 个 `res_*` 值共 104 个去重 16 字节块，其中 **24 个块重复出现**（最高一个出现 12 次：`a83bf9cd16b29c388e77c0af24cf2df8`）。
- 但整体长度**不是 16 的整数倍**（178 / 205 / 173 / 19 / 13 字节…），因此**不是 AES/ECB 之类分组密码**。
- "同一偏移处密文相同 + 长度任意"是 **流式模式（CTR/OFB/CFB）密钥流复用** 的典型特征（`推断`）。
- 反证：假设 `pkg_name` 是 MD5(包名)，用 28 个常见包名（含 `com.tencent.mm`、`com.tencent.mobileqq`…）与路径候选（`/sdcard/<pkg>/cache` 等 800+ 组合）做 MD5/SHA-1 匹配 → **0 命中**。SHA-1 假设下 `dir_path` 20 B 的第 2 行最像，但同样不命中。

### 2.5 谁在读它？（`观测`：没人）

- 直接扫描 `classes.dex` / `classes2.dex` / `classes3.dex` 字节流：字符串 `garbage_clean.db` **不存在**；`garbage_clean` 的唯一命中位置是 `classes3.dex@5321625`，上下文为 `string/activity_title_garbage_cleanup`（**假阳性**）。
- `t_cache` / `t_residual` 同样不存在（`t_cache` 的命中来自 `get_cache` 之类子串）。
- `lib/arm64-v8a` 全部 18 个 `.so` 中也不含 `garbage_clean`。
- 结论：**本 APK 内无读取者**。可能由 `com.miui.cleanmaster`（垃圾清理独立组件）或云端下发组件使用 → `未知`。

---

## 3. `assets/dict` —— jieba 中文分词词典（已完全解码）

### 3.1 判定（`观测`）

- 3,639,702 B，SHA-256 前 16 位 `f99086243babe60f`，熵 6.102，头 `00000001 00000000…`。
- **读取链**：`lj/g.java:32` → `lj/c.b(context,"dict")` → `mj/a.h(InputStream)`（`mj/a.java:260`）。
- 名称也出现在 `lj/c.java:17`：`public static final String[] f29439b = {"dict","model","pattern","phish"};`
- 若 App 私有目录里存在同名文件（`context.fileList()` 命中）且 `lj.c.f29440c == false`，则 **优先读内部文件**（`lj/c.java:42-57`）→ 该词典可被热更新替换（`推断`）。

### 3.2 格式（`观测`，逐字段来自 `mj/a.java:260-330`；大端 `DataInputStream`）

```
u32 version?               (读取后丢弃)   = 1
u32 base?                  (读取后丢弃)   = 0
f64 startProb[4]                        HMM 起始对数概率 (B/E/M/S)
f64 transProb[4][4]                     HMM 转移对数概率
i32 charCnt[4]                          各字长(1..4)的单字/前缀表条目数
i32 mLengthCount = L                    词长档数
i32 lenCnt[L]                           每档词条数
i32 mTotalFreq  i32 mMinFreq
-- 之后：字符表区 + 词表区，总长必须等于 available()（mj/a.java:313-317 会校验） --
字符表 i：base_i = Σ charCnt[0..i-1]*6 ；条目 = u16 char(BE) + f32 freq(BE)
词表  L ：base_L = 累积；条目 = L×u16 char(BE) + i32 freq(BE)，长 L*2+4
```

### 3.3 实际解析输出（`观测`，脚本 `_analysis/tools/parse_dict.js`）

```
header1 1 header2 0
start [-0.262687, -3.14e+100, -3.14e+100, -1.465263]
trans [[-3.14e+100,-0.510826,-0.916291,-3.14e+100],
       [-0.589715,-3.14e+100,-3.14e+100,-0.808525],
       [-3.14e+100,-0.333449,-1.260362,-3.14e+100],
       [-0.721197,-3.14e+100,-3.14e+100,-0.665863]]
charCnt [6857, 7439, 6409, 14519]   L 16
lenCnt  [11580,114139,131323,84895,3563,1641,918,385,225,150,77,30,26,15,9,6]
totalFreq 60101983   minFreq 2
headerEnd 260  fileSize 3639702  payload 3639442
charTableEnd 211344  wordTableEnd 3639442  expected==payload? true
```

- `startProb` / `transProb` 与开源 **jieba** 的 `prob_start` / `prob_trans` 数值**完全一致**（`-0.26268660809250016 / -1.4652633398537678 / -0.510825623765990 / -0.916290731874155 / -0.5897149736854513 / -0.8085250474669937 / -0.33344856811948514 / -1.2603623820268226 / -0.7211965654669841 / -0.6658631448798212`）→ `观测` + `推断`（同源实现，MIUI 移植版）。
- **词条总数 348,982**；单字/前缀表条目合计 **35,224**；字符表区 211,344 B，词表区 3,428,098 B，两者相加精确等于文件剩余长度（自校验通过）。

### 3.4 明文样本（`观测`，真实解出）

```
charTable[L1] count=6857  一:-3.6545 丁:-8.1250 七:-7.8174 万:-6.3096 三:-5.9321 上:-5.7396 不:-4.2743
wordTable L=1  count=11580   一|217830  丁|3607  丂|2  七|7675  万|29391  三|42542
wordTable L=2  count=114139  B超|3  C#|3  T恤|4  一一|1670  一万|442
wordTable L=3  count=131323  C++|3  一一二|11  一一分|8  一丁点|3
wordTable L=4  count=84895   AT&T|3  一一列举|34  一一对应|43  一丁不识|3  一丁点儿|24
wordTable L=5  count=3563    一万七千余|2  一万三千块|2
wordTable L=11 count=77      上海市劳动和社会保障局|7  中共中央纪律检查委员会|7
wordTable L=14 count=15      中华人民共和国中央军事委员会|2066  中国共产党第一次全国代表大会|45
wordTable L=16 count=6       侵华日军南京大屠杀遇难同胞纪念馆|3  外交部驻香港特别行政区特派员公署|4
```

> 用途（`推断`）：短信/骚扰内容的**中文分词**，配合 `pattern`（正则规则集）与 `model`（疑似分类权重）做离线语义判定。

---

## 4. `assets/pinyinindex.idf` —— 汉字→拼音索引表（已完全解码）

### 4.1 判定（`观测`）

- 54,199 B，熵 5.887，magic `IDF ` (`49 44 46 20`)。
- **读取链**：`am/a.java:47` 先试 `cl.b.b(context,"pinyinindex.idf")`（磁盘缓存），失败则 `am/a.java:57` → `cl.a.a(getAssets().open("pinyinindex.idf", 1))` → 返回 `cl.a$i`；查询在 `am/a.java:73`：`iVar.c(0, (char)c10 - 19968, 0)`，结果按分隔符 split 成拼音数组（`am/a$b.a(char)`）。
- 文件级格式由 `cl/a.java` 的 `class f` / `class e` / `class d` / `class i` 完整定义：magic `{73,68,70,32}`（`cl/a.java:491`）、版本必须 == 2（`cl/a.java:514`）。

### 4.2 格式（`观测`，`cl/a.java`）

```
"IDF " (4B)
i32 version (=2)
i32 nKinds          每个 kind = 一种表
i32 nKinds' (未使用)
per kind: i64 idxTableOffset, i64 dataTableOffset
@idxTableOffset:  i32 nGroups; nGroups × {i32 start, i32 end, i64 fileOffset}
@dataTableOffset: i32 nDesc;   nDesc × {u8 type, u8 idxSize, u8 lenSize, u8 elemStride, i64 dataOffset}
```
元素读取（`cl/a.java:410-455` `l()` + `651-683` `d()/e()`）：
```
rowBase   = group.fileOffset + (index - group.start) * Σ(idxSize)
sub       = 读 rowBase 处 idxSize 字节
delta     = 读 (dataOffset+4) + sub*elemStride 处 elemStride 字节
payloadAt = (dataOffset+4) + delta
len       = 读 payloadAt 处 lenSize 字节
value     = payloadAt + lenSize 起 len 字节（type=STRING 时为 UTF-8）
```
`type` 枚举顺序（`cl/a.java` `EnumC0089a`）：`BYTE, SHORT, INTEGER, LONG, STRING, BYTE_ARRAY, SHORT_ARRAY, INTEGER_ARRAY, LONG_ARRAY` → 本文件 `type=4 = STRING`。

### 4.3 实际解析输出（`观测`，脚本 `_analysis/tools/pinyin2.js`）

```
size 54199  tag "IDF "  version 2  nKinds 1
kind0: groups [{s:0, e:20902, off:12395}]  descs [{type:4(STRING), idxSize:2, lenSize:1, elemStride:2, dataOff:68}]
去重字符串条目数（dataOff 处 i32）= 1336
```

**明文样本（真实解出）：**

| 汉字 | index | sub | delta | len | 值 |
|---|---|---|---|---|---|
| 中 | 45 | 38 | 2850 | 5 | `zhong` |
| 国 | 2301 | 472 | 5287 | 3 | `guo` |
| 王 | 9611 | 120 | 3261 | 4 | `wang` |
| 米 | 11891 | 218 | 3778 | 2 | `mi` |
| 重 | 17357 | 1009 | 9531 | 11 | `zhong,chong` |
| 长 | 18303 | 1265 | 11747 | 11 | `zhang,chang` |
| 单 | 1365 | 420 | 4918 | 13 | `dan,chan,shan` |
| 沈 | 7816 | 877 | 8403 | 9 | `shen,chen` |
| 曾 | 6398 | 800 | 7775 | 9 | `ceng,zeng` |
| 翟 | 12767 | 1066 | 10085 | 7 | `zhai,di` |
| 阿 | 18495 | 1251 | 11623 | 3 | `a,e` |
| U+4E00 | 0 | 1 | 2673 | 2 | `yi` |
| U+4E01 | 1 | 2 | 2676 | 10 | `ding,zheng` |
| U+4E02 | 2 | 3 | 2687 | 3 | `kao` |

- 覆盖区间 `[0, 20902)` = U+4E00 … U+9FA5（CJK 基本区全集）→ 行索引 = `char - 19968`（`观测`，与 `am/a.java:73` 的偏移一致）。
- 文件布局自洽：头 4+4+4+4+16 = 32 B；delta 表 @72 起 1336×2 = 2672 B；行表 @12395 起 20902×2 = 41804 B；`12395 + 41804 = 54199 = 文件长度` ✓（`观测`）。
- `am/a.java` 另有 85 条**人工多音字/姓读覆盖表**（`f2410c`）：如 `(char)21333("单")→SHAN`、`(char)37325("重")→"chong"`、`(char)21306("区")→OU`、`(char)35299("解")→XIE` 等（`am/a.java:121-197`；注释中的包名 `f2409b` 还有 `单于→CHAN,YU`、`长孙→ZHANG,SUN`、`万俟→MO,QI`、`尉迟→YU,CHI` 等 6 条复姓）→ `观测`。

---

## 5. `assets/character.json` —— 明文字符表（`观测`）

600 B，UTF-8 明文 JSON，40 键（`character.json` 全文）：

```json
{ "<pad>":"0", "<unk>":"1", "a":"2", … "z":"27", "0":"28", … "9":"37", ".":"38", "_":"39" }
```

- **读取链**：`ea/c.java:21` → `new JSONObject(...)` → 建 `map(字符→int)` 与 `map2(int→字符)` → `new fa.a(context, resources, str, map, map2)`（`ea/c.java:39-50`），失败抛 `"Couldn't read underlying model for AppRecommenderImp"`。
- **消费方**：`fa.a` → `ia/j.java`（App 推荐/排序），`ia/d.java:202 f(String,HashMap)` 把包名转成 `int[30]`（逐字符查表，越界补 `<unk>`=1，超过 30 字符截断，小写化）。
- 结论：`rec_model_20230425.mnn` 的**字符级词表**，模型输入 = 包名字符 id 序列（长度 30）。

---

## 6. `res/raw/parse.db` —— 真正的垃圾/缓存目录规则库（明文）

### 6.1 定位（`观测`）

- ZIP 内实际名 `res/rKy.db`（94,208 B）；apktool 解码后重命名为 `res/raw/parse.db`；资源 id 为 `R.raw.parse`。
- SHA-256 `5b1da601c11b20eaf53da468a02ae8fda5fe91a5d048ac557301d03ee88767f1`（REA `ev_2447…`）。
- **读取链**：`ra/e.java:39-45` 常量 `"parse.db"` / `"DIR_PARSE"` → `ra/e.java:60` `getResources().openRawResource(R.raw.parse)` 复制到 `getDatabasePath("parse.db")` → `SQLiteDatabase.openOrCreateDatabase(...)`；查询方法 `ra/e.java:256 g(pkg)`（`select _id, PATH, PACKAGE_NAME from DIR_PARSE where PACKAGE_NAME='…'`）、`ra/e.java:211 f(list)`、写入 `ra/e.java:148 c(list,json)`（**服务端下发的 JSON 会 insert/覆写**）。
- 路径拼装规则（`ra/e.java:267-281`）：`PATH` 以 `/` 开头 → `Environment.getExternalStorageDirectory() + PATH`；否则 → `<ext>/ + PATH`；并过滤 `"."`、`".."`、`android` 前缀。

### 6.2 数据规模（`观测`，`node:sqlite` 全量）

| 表 | 行数 | 说明 |
|---|---|---|
| `DIR_PARSE` | **1043** | 884 个不同 `PACKAGE_NAME`；`APP_NAME` 无 NULL |
| `TEMP_DIR` | **10** | 临时目录规则 |
| `android_metadata` | 1 | `locale = zh_CN` |

`DIR_PARSE` 的 `APP_NAME` 格式为 `zh_cn=<名称>&zh_tw=<名称>&=`（**三语串联在同一串里**，与 `garbage_clean.db` 分三列不同）。

### 6.3 样本行（`观测`）

```
1  /.budingdoyen     iw.avatar                 zh_cn=布丁爱生活&zh_tw=&=
2  /.helix.thumbnails com.real.RealPlayer       zh_cn=Real Playar&zh_tw=&=
3  /gomarket         cn.goapk.market           zh_cn=安智市场&zh_tw=&=
7  /.wblog-images    com.tencent.WBlog         zh_cn=腾讯微博&zh_tw=&=
13 /.MTXX            com.mt.mtxx.mtxx          zh_cn=美图秀秀&zh_tw=&=
19 /DUOMI            com.duomi.android         zh_cn=多米音乐&zh_tw=&=
```
行数最多的包（`观测`）：`""`(36 行，即无包名的通用目录)、`com.qzone`(7)、`com.taobao.taobao`(6)、`com.tencent.hd.qq`(5)、`com.tencent.padbrowser`(4)、`com.baidu.baike`(4)。

### 6.4 `TEMP_DIR` 全部 10 行（`观测`）

| _id | EXCLUDED | PATH | TYPE |
|---|---|---|---|
| 1 | 0 | `/downloaded_rom` | 0 |
| 2 | 0 | `/MIUI/debug_log` | 0 |
| 3 | 0 | `/ramdump` | 0 |
| 4 | 0 | `/debug_log` | 0 |
| 5 | 0 | `/diag_logs` | 0 |
| 6 | 0 | `/data/tombstones` | 1 |
| 7 | 0 | `/data/log` | 1 |
| 8 | **1** | `/LOST.DIR` | 0 |
| 9 | **1** | `/data/system/dropbox` | 1 |
| 10 | 0 | `/cache` | 1 |

`TYPE`（`推断`）：`0` = 相对外部存储根；`1` = 绝对 `/data` 路径（与 `ra/e.java` 的拼装逻辑一致）。`EXCLUDED=1` = 默认不清理。

---

## 7. `assets/censor_source` —— 敏感词库（加密，未解）

### 7.1 观测

- 2,688 B，SHA-256 前 16 位 `38ca17ec730e6a54`，**Shannon 熵 7.933**（接近均匀随机），头 16 B = `0c8c3b382bba7afc3f9d9f45ff6eea04`，无任何 magic，长度 = 168×16（16 字节对齐）。
- **三个 dex 中都不存在字符串 `censor_source`**；`censor` 的唯一 dex 命中是 `classes3.dex@6049860` 上下文 `censorResult`（假阳性）。
- 真正引用在 **`lib/arm64-v8a/liblib-riskcontrol.so`**（247,832 B）：

| .so 偏移 | 内容 |
|---|---|
| 18008 / 18043 | 符号名 `SensitiveWordDetector` |
| 18065 | 符号表区，含 `AES::EncryptBlock / DecryptBlock / KeyExpansion / Rcon / SubBytes / XorBlocks / AESKeyLength`、`SensitiveWordDetector::load(char*)`、`SensitiveWordDetector::censor(std::string&)`、`TrieTree::insert/find`、`TreeNode::insertChild/findChild` |
| 195305 / 195411 | 字符串 `censor` / **`censor_source`** |
| 1954xx | 运行期诊断串：`ERROR!`、`keyword and char size mismatch!!`、`Wrong char size when detecting!`、`File has no content!!!`、`load file!`、`File has %d lines`、`Load file tree fail!!!`、`censor sentence %s`、`AssetManager open fail!`、`AssetManager Error!`、`needPadding %u char(s)` |
| 1954xx | 调试覆盖路径 **`/storage/emulated/0/censor_source`** |
| 1955xx | 一段 AES T-table 常量（`.w+.R.j.06.8.@…` 特征串） |

→ `推断`：`censor_source` 由 native `SensitiveWordDetector::load()` 读取，内部用自实现 AES 解密后建 `TrieTree`（敏感词树）。`File has %d lines` / `needPadding %u char(s)` 说明明文应是"按行分隔的关键词表"，加载时按字节长度对齐。

### 7.2 Java 侧调用者（`观测`）

`com/miui/riskcontrol/SensitiveWordDetectProvider.java`（`classes3.dex`，共 79 行）：

```java
public class SensitiveWordDetectProvider extends ContentProvider {
    public static final ArrayList<String> f20241a;      // 调用方白名单
    static {  /* "content://com.miui.riskcontrol/censorResult" */
        f20241a = arrayList;
        System.loadLibrary("lib-riskcontrol");           // ← 加载 liblib-riskcontrol.so
        arrayList.add("com.android.camera");             // 只有这两个包能调用
        arrayList.add("com.miui.mediaeditor");
    }
    private native boolean hasSensitiveWords(String str);
    private native void setAssetManager(AssetManager assetManager);
    public final Bundle call(String str, String str2, Bundle bundle) {
        if (!f20241a.contains(getCallingPackage())) return null;      // 白名单校验
        if (!"METHOD_SENSITIVE_WORD_DETECT".equals(str)) return null;
        setAssetManager(getContext().getAssets());                    // ← 只传 AssetManager
        bundle2.putBoolean("hasSensitiveWords", hasSensitiveWords(str2));
        return bundle2;
    }
}
```

关键点（`观测`）：JNI 只有 `setAssetManager(AssetManager)` 与 `hasSensitiveWords(String)` 两个入口，**Java 侧没有把任何密钥/IV 传给 native** → 猜测"密钥由 Java 注入"不成立；AES 密钥必然**内置在 `liblib-riskcontrol.so` 内**（或由 .so 内的常量派生）。
对外能力（`推断`）：`11B` 之外的任意 APK 若伪造包名不可行（`getCallingPackage()` 由系统填充），因此该 IPC 仅对**相机与相册编辑器**开放，用于拍摄/编辑前的敏感词/内容审查。

### 7.3 解密尝试（全部失败，`观测`）

脚本 `_analysis/tools/{aesbrute,censor_attack,aes_pad,aes_pad2}.js`：

| 尝试 | 结果 |
|---|---|
| `.so` 中全部 **4,862** 个可打印 16 字节窗口作 AES-128 密钥（ECB/CBC-zeroIV，关 padding），判据 = 输出 UTF-8 CJK 比例 > 0.3 | 0 命中 |
| **穷举 `.so` 全部 16 B / 32 B 窗口作 AES-128/256 密钥**，用最后一块做 **PKCS5 补位校验**（ECB：`D(C_last)`；CBC：`D(C_last) XOR C_{last-1}`，与真实 IV 无关） | 约 **49 万**候选；`pad≥1` 命中 3,634（≈1/256 的期望假阳性），`pad≥2` 收敛到 **12** 个；对这 12 个做**全文件解密 + 文本得分校验 → 0 个通过** |
| 25 组候选词（`censor`、`censor_source`、`miui`、`xiaomi`、`securitycenter`、`riskcontrol`、`0123456789abcdef`…）× {原文, MD5, SHA-1[0:16], SHA-256} × {AES-128/192/256 ECB/CBC} | 0 命中 |
| 256 个单字节 XOR | 0 命中 |
| `inflateRaw` / `inflate` / `gunzip` / `brotli` | 全部抛异常或得分极低 |
| 无头反汇编（Ghidra/Hopper/IDA/Kuna） | **本机不可用**：REA `open_binary` 明确拒绝 `Windows Ghidra P0 accepts native PE and … only`；Hopper 不支持 win32；`REA_IDA_MCP_CONFIG` / `REA_KUNA_COMMAND` 未配置；`rizin` / `objdump` 均未安装 |

→ **算法与密钥：`未知`**。已排除：单字节 XOR、常见口令派生密钥、deflate/gzip/brotli、**.so 内任意连续 16/32 字节即密钥**（含 ECB 与 CBC 两种补位验证）。
剩余可能性（`推断`）：密钥由 .so 内**多段常量拼接/变换**得到（与同族 `libtcp.so` 的 `WALI_KEY_SEG_1/2/3` 手法类似），或索引/偏移式取字节（非连续窗口）。
**建议**：需要在 Linux（或有 rizin/Ghidra 的机器）对 `_ZN21SensitiveWordDetector4loadEPc` 与 `_ZN3AES…KeyExpansion` 做交叉引用分析，定位传给 `AES(KeyLength, key, iv)` 的实参。`native-libs.md`（队友产物）已给出同方向结论与 24 个常量候选，可合并。

---

## 8. 两个模型文件

### 8.1 `assets/rec_model_20230425.mnn` —— MNN（`观测`）

- 810,908 B，熵 7.504，头 `1c000000 18001c00 08000000 000c0010 …` → `u32 rootOffset = 28`，符合 **FlatBuffers**；MNN 的 `Net` 表即 flatbuffers。
- 模型内可打印符号（`观测`，共 10,951 个可打印 run，去重后前 120 个）：

```
sparse_inputs_1 … sparse_inputs_6 | sparse_inputs_time
embedding_0.w_0 … embedding_8.tmp_0 | Const3/Const19/Const33/Const47/Const61/Const75/Const90
transpose_0..5.tmp_0 | squeeze_0..17.tmp_0 | unsqueeze2_1..17.tmp_0
p2o.Add.1/3/5/7/9/11/13/15/17/19 | p2o.MaxPool.1/3/5/7/9/11 | p2o.MatMul.1/3/5/7/9
p2o.Concat.1 | p2o.helper.constant.6 | reshape2_0.tmp_0
relu_0..9.tmp_0 | linear_0..4.w_0 / .b_0 | linear_9.tmp_1 (输出)
```

- 与 Java 侧完全对应（`ia/j.java`）：
  - `ia/j.java:88,92,105,109` — 模型路径 `<files>/apprecmodel/checkpoint_N` 或 `<files>/rec_model_20230425.mnn`，缺则 `ia/d.d(resources,"rec_model_20230425.mnn", path)` 从 assets 复制（`ia/d.java:71-182`）。
  - `ia/j.java:116` `MNNNetNative.nativeCreateNetFromFile`；`:131` `nativeCreateSession(..., FORWARD_CPU, 4, ...)`（4 线程）。
  - `:138-144` 输入 tensor 名 = `sparse_inputs_1..6` + `sparse_inputs_time`；`:202-209` 形状 `{batch, 30}` 与 `{batch,1}`。
  - `:224-233` 输入内容：前 5 个 App 的包名字符 id（`ia/d.f`）、第 6 个候选、`int[30]` 时间嵌入 = `Calendar.get(HOUR_OF_DAY)` 填充；`:262` 输出 tensor `linear_9.tmp_1`；`:283-294` 对每 2 个 logit 做 softmax 取 index 1 → **二分类"是否会打开/是否正样本"**。
  - `:451,470,523` `MNNTrainNative.nativeGetRecModelPredictScore` / `nativeTrainRecModel` → **端侧增量训练**（样本 <50 直接放弃，`:372`）。
- 结论：**App 推荐/预加载排序模型**，输入包名字符序列 + 小时，输出二分类分数，支持端侧微调。

### 8.2 `assets/model` —— 格式未知（`推断` + `未知`）

- 680,154 B，熵 7.178。头部（int32BE）：`1, 0, 5, 68, 58`；随后 uint16BE 序列 `13, 36, 96, 123, 125, 183, 924, 927, 934, 955, 1052, 1054, 1057, 8214 …`。
- 字节直方图首位 `0xC0` 出现 62,465 次（9.2%）→ 大量 float32 负值（`0xC0xxxxxx` ≈ `-2.0`~`-4.0`）。
- 偏移 76 之后按 4 字节切分：float32BE 落在 `[-6,0]` 占 **36.8%**（170,019 个窗口），float32LE 仅 18.4% → 偏大端权重（`推断`）。
- 可打印 ASCII run（≥6 字符）883 个，但内容无意义（`")\".\"f$h$`、`%m%n%o%p%`…）→ 无符号表/无层名（`观测`）。
- **打开者**：只有 `lj/c.java:17` 的 `{"dict","model","pattern","phish"}` 名字数组，且该数组在全工程**只被声明、从未被读取**（`观测`，grep `f29439b` 仅 1 处定义）。通用的 `lj/c.b(context,name)` 会按名字打开（assets 或内部文件）。
- 因此：格式、维度、用途均 `未知`；`推断` 为短信/钓鱼文本分类器的**词向量或权重矩阵**（与 `dict`/`pattern`/`phish` 同 package `lj`）。`phish` 资源**并不存在于 assets**（未知是否运行时下载）。

---

## 9. `assets/pattern` —— 短信/来电识别规则（明文，UTF-8，非严格 JSON）

- 5,568 B，熵 4.879，UTF-8 明文（**修正基线：不是 GBK**）。
- 结构：`{"version":"1.2","features":[ … ]}`，但键**不加引号**（`{pattern:["^12520"]}`、`sms_type:1`、`neg_pattern:[…]`），因此标准 JSON 解析器会报错 —— 说明解析器是自定义/宽松的（`推断`）。
- **读取链**：`lj/c.java:22-40 a(Context)` → `bufferedReader` 逐行读入 `StringBuilder`（按行 `+ "\n"`）→ 供规则引擎编译。
- 已解出的 feature 名（`观测`）：`Fei_xin`（`abstract:"Start with 12520"`，`address.pattern:["^12520"]`）、`sms_body_length`（`body_len:["0-15"]`）、`bracket_title_not_personal_address`（4 条 `【…】` 正则，含 `[\u4E00-\u9FFF|a-z|0-9|，。｜ | \.]{2,12}`）、`important_info_body`（`neg_pattern:["恭喜.*幸运.*奖品"]` + 6 条 `or_pattern`：尾号/卡号/验证码/密码/兑换码/快递单号）、`important_addr`（`address.type:[1,5]`）、`important_info`（`combi:["bracket_title_not_personal_address&important_inf…"]`）。
- 用途（`推断`）：判断短信是否为"通知类/含重要信息"，从而决定是否按重要通知展示/不拦截。

---

## 10. `assets/warningcenter/` —— 灾害预警图标（71 张）

### 10.1 文件矩阵（`观测`，脚本 `wc3.js`）

71 个文件、481,089 B。命名 `warningcenter_<code>_<color>.{png|webp}`：

| code | 颜色数 | 颜色 | 格式 |
|---|---|---|---|
| 11A01 / 11A02 / 11A51 | 4 | blue yellow orange red | webp |
| 11B01 / 11B03 / 11B04 / 11B05 / 11B06 / 11B17 | 4 | blue yellow orange red | png |
| 11B07 / 11B09 / 11B14 / 11B19 / 11B21 | 3 | yellow orange red（**无 blue**） | png |
| 11B15 | 3 | blue orange red（**无 yellow**） | png |
| 11B16 | 3 | blue yellow orange（**无 red**） | png |
| 11B22 | 2 | orange red | png |
| 11D01 / 11D02 | 4 | blue yellow orange red | webp |
| fire | 4 | blue yellow orange red | webp（**被 11G01 与 11G05 共用**） |

> 缺色不是缺文件：**每个灾害类型只在其"可能出现的预警等级"上才提供图标**（推断，例如 `11B22` 只有橙/红）。

### 10.2 清单与图标完全自洽（`观测`，脚本 `wc4.js` / `wc5.js`）

- `warning_center_disaster_icon` 的 21 个 code 共 **75 条** `code→文件名` 映射，**75 条全部指向存在的文件**（`MISSING 0`）。
- `warningcenter/` 目录 71 个文件，反向检查：**没有**任何"文件存在但 JSON 未列出"的孤儿文件。
- 差异来自**复用**：`warningcenter_fire_{blue,yellow,orange,red}.webp` 同时被 `11G01` 与 `11G05` 引用：
  ```json
  "11G01": {"blue":"warningcenter_fire_blue.webp","yellow":"warningcenter_fire_yellow.webp","orange":"warningcenter_fire_orange.webp","red":"warningcenter_fire_red.webp"}
  "11G05": { …完全相同… }
  ```
  → 71 个文件 + 4 个重复引用 = 75 条映射 ✓
- 观察到的另一处不一致：`11B16` 只有 blue/yellow/orange（**无 red**）、`11B22` 只有 orange/red、`11B07/09/14/19/21` 无 blue —— 这些"缺色"在 `warning_center_disaster_guide_text` 里对应颜色的文本长度恰好也是 0（见 §10.3），两者互相印证。

### 10.3 完整 code → 灾害类型映射（`观测`）

来源：`com/miui/warningcenter/disasterwarning/model/EventType.java:10-34`（枚举）+ `com/miui/warningcenter/disasterwarning/Utils.java:26-78`（两张 `Map<String,Integer>`：`ICON_LIST` → drawable，`TITLE_LIST` → 字符串资源）。

| code | EventType 枚举名 | 中文（由 R.string 名读出） | 本地 assets 图标 | `guide_text` 有文本 |
|---|---|---|---|---|
| `11B01` | Typhoon | **台风** `warningcenter_disaster_taifeng` | 4 色 png | ✅ 蓝/黄/橙/红 |
| `11B03` | Rainstorm | **暴雨** `…_baoyu` | 4 色 | ✅ 全部 |
| `11B04` | Snowstorm | **暴雪** `…_baoxue` | 4 色 | ✅ 全部（文本为"防雪灾和防冻害"） |
| `11B05` | ColdWave | **寒潮** `…_hanchao` | 4 色 | ✅ 全部 |
| `11B06` | Hurricane | **大风** `…_dafeng` | 4 色 | ✅ 全部 |
| `11B07` | —— （**不在枚举里**） | —— | 黄/橙/红（无蓝） | ✅ 黄/橙/红（文本："防沙尘暴"） |
| `11B09` | Hot | **高温** `…_gaowen` | 黄/橙/红 | ✅ 黄/橙/红（"防暑降温"） |
| `11B14` | —— （不在枚举） | —— | 黄/橙/红 | ❌ 无 |
| `11B15` | Hailstone | **冰雹** `…_bingbao` | 蓝/橙/红（无黄） | ✅ 橙/红 |
| `11B16` | —— （不在枚举） | —— | 蓝/黄/橙（无红） | ❌ 无 |
| `11B17` | Fog | **大雾** `…_dawu`（drawable 名 `…_wu`） | 4 色 | ✅ 全部 |
| `11B19` | Haze | **霾** `…_mai` | 黄/橙/红 | ✅ 黄/橙（红为空） |
| `11B20` | Thunderstorm | **雷雨大风** `…_leiyudafeng` | ❌ 无（用 drawable `…_others`） | ❌ |
| `11B21` | FrozenRoad | **道路结冰** `…_daolujiebing` | 黄/橙/红 | ✅ 全部（"结冰应对准备"） |
| `11B22` | —— （不在枚举） | —— | 橙/红 | ❌ |
| `11B25` | ForestFire | **森林火险** `…_senlinhuoxian` | ❌（`…_others`） | ❌ |
| `11B29` | Pollution | **重污染** `…_zhongwuran` | ❌ | ❌ |
| `11B37` | Disaster | **地质灾害气象风险** `…_dizhizaihaiqixiangfengxian` | ❌（`…_others`） | ❌ |
| `11A01` | Flood | **洪水** `…_hongshui` | 4 色 webp | ❌ |
| `11A02` | WaterLogging | **内涝** `…_neilao`（drawable `…_jishui`） | 4 色 webp | ❌ |
| `11A51` | FlashFlood | **山洪** `…_shanhong` | 4 色 webp | ❌ |
| `11C02` | Earthquake | **地震** `…_dizhen` | ❌ | ❌ |
| `11D01` | LandSlide | **滑坡** `…_huapo` | 4 色 webp | ❌ |
| `11D02` | Mudslide | **泥石流** `…_nishiliu` | 4 色 webp | ❌ |
| `11D99` | （仅 `Utils` 里，映射到 others） | **其他** `…_qita` | ❌ | ❌ |
| `11E02` | WindStorm | **风暴潮** `…_fengbaochao` | ❌（`…_others`） | ❌ |
| `11E06` | Wave | **海浪** `…_hailang` | ❌ | ❌ |
| `11G01` | ForestFireWithinTerritory | **森林火灾** `…_senlinhuozai` | **`warningcenter_fire_*`（4 色 webp）** | ❌ |
| `11G05` | GrasslandFireWithinTerritory | **草原火灾** `…_caoyuanhuozai` | **`warningcenter_fire_*`（共用）** | ❌ |
| （无 code） | Other | 其他 | ❌ | ❌ |

→ 由此彻底解释了两处"看起来不一致"的地方：`fire` 不是"孤儿文件"，而是 `11G` 类（森林/草原火灾）的图标；`11G01`/`11G05` 也不是"缺文件"，而是与 `fire` 共用。**（本节修正了笔者初稿中的错误判断。）**

**`warning_center_disaster_guide_text` 每条文本的字符长度（`观测`）** —— 长度 0 与图标缺色严格对应：

| code | 蓝 | 黄 | 橙 | 红 | | code | 蓝 | 黄 | 橙 | 红 |
|---|---|---|---|---|---|---|---|---|---|---|
| 11B01 | 122 | 148 | 214 | 203 | | 11B15 | 0 | 0 | 128 | 124 |
| 11B03 | 100 | 143 | 127 | 74 | | 11B17 | 0 | 83 | 77 | 141 |
| 11B04 | 155 | 153 | 121 | 95 | | 11B19 | 0 | 58 | 93 | **0** |
| 11B05 | 67 | 87 | 81 | 85 | | 11B21 | 0 | 67 | 64 | 71 |
| 11B06 | 170 | 189 | 186 | 179 | | 11B07 | 0 | 148 | 133 | 106 |
| 11B09 | 0 | 101 | 142 | 82 | | | | | | |

（对照 §10.1：11B22 只有橙/红、无 guide 条目；11B19 的红为空且图标无蓝无红对应关系需注意——11B19 图标有红、文本红为空，属数据不齐，`观测`。）

### 10.4 预警等级 = 位掩码（`观测`）
`Utils.java:19`：`public static final int[] LEVEL = {8, 4, 2, 1};` —— 与 `DisasterConstants` 的 `LEVEL_RED_COUNT=8 / ORANGE=4 / YELLOW=2 / BLUE=1` 一致；`Utils.getRegistLevel()`（`Utils.java:99-123`）用 `LEVEL[i] & pushLevel` 求**订阅等级的位与**，`getStrongPushLevel()` 默认 `8`（只推红色）、`getSystemPushLevel()` 默认 `12`（红+橙）。→ 1/2/4/8 为位掩码，可组合（`观测`+`推断`）。

### 10.5 两套图标体系（`观测`）

| 体系 | 位置 | 谁用 |
|---|---|---|
| **资源图标** | `res/drawable/warningcenter_disaster_icon_{taifeng,baoyu,…,huozai,others}` | `Utils.ICON_LIST` → 列表/通知里用 `getIconRes()` |
| **assets 图标** | `assets/warningcenter/warningcenter_<code|fire>_<color>.webp|png` | `WarningCenterDisasterDetailActivity.java:341` 读 `assets/warning_center_disaster_icon` 得文件名 → `:370` `getAssets().open("warningcenter/" + name)` → `Drawable.createFromStream` |

指南文本：`WarningCenterDisasterDetailActivity.java:105` `assetManager.open("warning_center_disaster_guide_text")`。

### 10.6 关联接口（`观测`）

`DisasterConstants.java` 还给出：
- `REQUEST_CITYCODE_URL = "https://api.sec.miui.com/12379/warning/getRegionInfo"`（12379 = 国家突发事件预警信息发布系统）
- `REQUEST_CITYCODE_SALT = "7war5230-a1cf-3k79-ec73-75cb82145n5x"`
- `UUID_DISASTER_CODE = "0279a99a-80f9-42ec-8945-bfe7d5070ed6"`
- `DISASTER_PUBKEY` = RSA-2048 SPKI（base64）：`MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApRu6Zd52/9k2NSgeGUIeSg8rIUtj64REDEBW+wNEnhYiebc9BSUcViBe3rYJvKH9hAXyiVKSVq9V2p/wweXX6QcJgu7P9I2ajyqZ23ChpLG1I3vBl4yBjv1r9+uCz819jZGxzhD4jLwKKLeAbd+cM3mdLwhCwI/5b56VFWrkKpcQQhtrxB8FU4n/wzqSgo38NyerBa7KhcUsCrJxt4Y7sIQ9u+fJAFvN7jpKn+9ey6C9hibBJunnAQj/rCcyC+fcUlOGj2EEoSp+S0s+Rd7gIWYdxl7uAX9P0SIc+ZUGslVjEEvOjpxNcUq/x6aXt6YJt+bZGVXs4FuS/YqGDIOiywIDAQAB` → 校验云端预警数据用（`推断`）。

---

## 11. `res/raw` 与 `res/xml`

### 11.1 `res/raw`（全部 **38** 项，`观测`）

| 类别 | 文件 | 说明 |
|---|---|---|
| **SQLite 规则库** | `parse.db`（94,208 B，ZIP 名 `res/rKy.db`，两者 SHA-256 完全相同 = `5b1da601c11b20eaf53da468a02ae8fda5fe91a5d048ac557301d03ee88767f1`） | 见 §6，唯一的内置明文规则表 |
| **证书** | `wlan_speed.crt`（912 B，ZIP 名 `res/Dmp.crt`）、`wlan_speed_new.crt`（3,090 B，ZIP 名 `res/xIB.crt`） | 见 §11.3 |
| OpenGL shader | `fragment.glsl`、`imagefragment.glsl`、`imagevertex.glsl`、`vertex.glsl` | 动画渲染 |
| 视频 | `antivirus_animation1/2.mp4`、`security_scan_video.mp4`、`firstaidkit_scan_video.mp4`、`optimizemanage_video.mp4`、`ew_guide_video.mp4`、`pc_kit_printer.mp4`、`pp_top_logo_video.mp4`、`securty_protect_top_video.mp4`、`wireless_reverse_charging.mp4`、`icon_wild_model_video.mp4` | 引导/扫描动画 |
| 音频 | `one..ten.mp3`（数字）、`di.mp3`、`didi.mp3`、`wu.mp3`、`non.mp3`、`lucky_money.mp3`、`hongbao_arrived.wav`、`mijia_push_alert_p0.ogg`、`number_picker_value_change.ogg` | 音效 |
| 动图 | `vtb_video_effect_fps_before_cn.gif`、`vtb_video_effect_fps_after_cn.gif` | 视频工具箱前后对比 |

> `res/raw` 里**没有**其它 `.db` / `.json` / 规则表 / 密钥库。

### 11.2 `res/xml`（120 个，`观测`）

全部是 **Preference/布局/文件路径/网络安全** 类 XML，无规则库。唯一与"信任"相关的是：

```xml
<!-- res/xml/network_security_config.xml -->
<network-security-config>
    <base-config cleartextTrafficPermitted="true" />
    <domain-config>
        <domain includeSubdomains="true">miwifi.com</domain>
        <domain includeSubdomains="true">www.miwifi.com</domain>
        <domain includeSubdomains="true">router.miwifi.com</domain>
        <trust-anchors>
            <certificates src="@raw/wlan_speed" />
            <certificates src="@raw/wlan_speed_new" />
        </trust-anchors>
    </domain-config>
</network-security-config>
```
（`base-config cleartextTrafficPermitted="true"` = 全局允许明文 HTTP，`观测`。）

其它值得注意的：`file_paths.xml`、`na_filepaths.xml`、`xiaomi_update_sdk_file_paths.xml`（FileProvider 路径）、`priority_storage.xml`、`ew_*.xml`（地震预警）、`warning_center_*.xml`（预警中心）。

### 11.3 两张内置证书（`观测`，Node `X509Certificate` 解析）

| 项 | `res/raw/wlan_speed.crt`（ZIP: `res/Dmp.crt`） | `res/raw/wlan_speed_new.crt`（ZIP: `res/xIB.crt`） |
|---|---|---|
| DER 长度 | 632 B | 2,202 B |
| subject | `C=CN, ST=ShenZhen, L=ShenZhen, O=XiaoMi, OU=XiaoMi, CN=www.miwifi.com` | `C=CN, ST=BeiJing, L=BeiJing, O=XIAOMI, OU=MIWIFI ROOT CA, CN=MIWIFI ROOT CA` |
| issuer | 同 subject（自签） | `C=CN, ST=Beijing, L=Beijing, O=Xiaomi Inc., OU=Xiaomi CA, CN=Xiaomi Root CA` |
| 有效期 | 2020-06-28 → 2030-06-26 | 2020-10-28 → 2120-10-04 |
| 序列号 | `E53763287569BE1F` | `32060BF01DCEEE20EBFCBD9F35C9AAE5B3FF8629` |
| SHA-256 指纹 | `A9:9B:E9:57:3E:E1:A1:4E:5D:B3:3F:46:FE:95:02:D1:09:15:E8:3F:E9:DC:FF:87:7D:05:53:81:38:F5:8D:BB` | `76:C7:E3:44:D6:BB:19:36:50:05:A4:C7:23:44:C2:EE:F6:B2:49:E8:06:7C:4B:2C:54:6E:75:0E:0F:FC:7E:52` |
| 公钥 | RSA **1024**，e=65537 | RSA **2048**，e=65537 |
| CA | false | **true** |

用途：`u6/e.java:183` 访问 `https://miwifi.com/cgi-bin/luci/api/xqsystem/maccel?type=1&mode=1`（小米路由器测速）时以此二证书作信任锚（`观测`：网络配置 + 该 URL 字面量；`推断`：配套使用）。

---

## 12. `META-INF/` 与签名

### 12.1 目录内容（`观测`）

| 文件 | 大小 | 内容 |
|---|---|---|
| `MANIFEST.MF` | 789,745 | 标准 JAR manifest，对每个条目记 `SHA-256-Digest`（示例：`AndroidManifest.xml → sgsh002rVeL2w+MuUI3bY0kXFBlETSEOQv+SexNxzjU=`） |
| `PLATFORM.SF` | 789,872 | v1 签名文件（`Signature-Version` 块） |
| `PLATFORM.RSA` | 1,642 | v1 PKCS#7 SignedData 块 |
| `com/android/build/gradle/app-metadata.properties` | 56 | `appMetadataVersion=1.1` / `androidGradlePluginVersion=7.3.1` |
| `services/kotlinx.coroutines.internal.r` | 5 | 文本 `nk.a\n`（Kotlin coroutine 的 `MainDispatcherFactory` 服务声明，混淆名） |
| `services/mk.e0` | 5 | 文本 `nk.b\n`（同上，`Delay` 实现） |

> `services/*` 是 **Kotlin 协程 SPI 注册**，不是第三方服务；名字被 R8 混淆。

### 12.2 签名方案（`观测`，REA `ev_774c…`）

| 方案 | 存在 | 签名者/证书 | 摘要 |
|---|---|---|---|
| v1 (JAR) | ✅ | `PLATFORM.SF` + `PLATFORM.RSA`；同一张 MIUI 证书 | `SHA-256-Digest`（OID 2.16.840.1.101.3.4.2.1），签名算法 OID 1.2.840.113549.1.1.1 (RSA) |
| v2 | ✅ | 同上 | `sha256`，declared `7f809feb864bb03b73460e6edbb092cd32af8be620c8d85c4bfb6c29100403e8`，**matches_apk = true** |
| v3 | ✅ | 同上；`min_sdk 24`，`max_sdk 2147483647`；**无密钥轮换 lineage** | 同上摘要，`matches_apk = true` |
| v3.1 | ❌ | — | — |
| Source Stamp | ❌ | — | — |

证书（single signer，三种方案一致）：
```
subject/issuer : C=CN, ST=Beijing, L=Beijing, O=Xiaomi, OU=MIUI, CN=MIUI, emailAddress=[VENDOR_EMAIL_REMOVED]
serial         : E552A8ECB9011B7C
validity       : 2011-12-06T03:26:26Z → 2039-04-23T03:26:26Z
public key     : RSA (292 B SPKI 字段)
SHA-256 指纹   : c9009d01ebf9f5d0302bc71b2fe9aa9a47a432bba17308a3111b75d7b2149025
```
`posture`：`strongest_scheme = v3`，`schemes_agree_on_signer = true`，`self_consistent = true`，`v1_only = false`。
> 说明：这是 MIUI **平台签名**（`O=Xiaomi, OU=MIUI`），说明它是 ROM 预置系统应用。REA 明确声明**未做密码学验签**（仅比对声明的摘要与当前字节）。

---

## 13. assets 全量清单（248 文件，按类别）

> 完整逐文件明细见 `_analysis/tools/assets_summary.tsv`（含类型/结构/规模自动提取）。下表为**归类汇总**，"读取方"一列只写**已定位到**的类/方法，未定位写 `未知`。

### 13.1 顶层单文件（无扩展名）—— 规则/模型/密钥类

| 文件 | 字节 | 类型 | 规模/内容 | 读取方 |
|---|---|---|---|---|
| `dict` | 3,639,702 | 自定义二进制（jieba） | **348,982 词 + 35,224 字表**，HMM 参数 | `lj/g.java:32` → `mj/a.h()` |
| `model` | 680,154 | 自定义二进制（**未知**） | 头部 int32 `1,0,5,68,58` + float32 权重 | 仅 `lj/c.java:17` 名字数组；**无实际读取者** |
| `censor_source` | 2,688 | 加密二进制（熵 7.933） | 敏感词库（未解） | `liblib-riskcontrol.so` `SensitiveWordDetector::load` |
| `rsapubkey` | 419 | **Java 序列化** `java.security.KeyRep` | `aced0005 7372 0014 6a6176612e73656375726974792e4b6579526570…` → RSA 公钥 | `未知`（KeyRep 反序列化） |
| `pattern` | 5,568 | UTF-8 宽松 JSON | `version 1.2`，≥6 个 feature 规则组 | `lj/c.java:22 a(Context)` |
| `placement_id` | 39 | 明文文本 | `123=fb:196567740764356_196569107430886`（Facebook 广告位） | `未知` |
| `financial_package_top` | 7,180 | JSON 数组 | **300** 个金融类包名 | `未知` |
| `photography_package_top` | 7,196 | JSON 数组 | **300** 个影像类包名 | `未知` |
| `safepay_target_package` | 13,076 | JSON 数组 | **211** 个 `{package,state}`（支付安全扫描目标） | `未知` |
| `safepay_target_activity` | 1,376 | JSON 数组 | **7** 个 `{package,type,activity[]}` | `未知` |
| `safepay_imm_whitelist` | 383 | JSON 数组 | **13** 个输入法包名（支付键盘白名单） | `未知` |
| `safepay_calling_activity` | 236 | JSON 数组 | **4** 个支付拉起 Activity | `未知` |
| `anti_fraud_pay_activity` | 637 | JSON 数组 | **3** 个 `{package,activity[]}`（反诈转账页） | `未知` |
| `xunyou_support` | 283 | UTF-8 文本（行分隔） | **20** 个支持"迅游加速"的游戏名（王者荣耀/和平精英/原神…） | `未知` |
| `vtb_default_support_list` | 6,541 | UTF-8 文本（行分隔） | **324** 个包名（视频工具箱白名单） | `未知` |
| `quickreply_default_list` | 6,526 | JSON 数组 | **263** 个包名（快捷回复/游戏内回复） | `f7/b.java:299` |
| `top_200_games.json` | 19,926 | JSON `{packageNames:[…]}` | **607** 个游戏包名 | `未知` |
| `privacyapps_top.json` | 8,091 | JSON 数组 | **4 组**（type 0:100 / 1:20 / 2:85 / 4:29 个包名） | `未知` |
| `white_url.json` | 8,259 | JSON `{content:[…]}` | **452** 个域名/IP 白名单 | `m2/e.java:24` |
| `service_num.json` | 4,470 | JSON `{content:[…]}` | **224** 个客服/服务号码 | `m2/e.java:55` |
| `address.json` | 21,531 | UTF-8 JSON 数组 | **34** 省 / **336** 市 + 区号（`{"n":"北京","c":"10","l":[…]}`） | `m2/f.java:416,455` |
| `character.json` | 600 | 明文 JSON | **40** 条字符→id | `ea/c.java:21` |
| `behavior_record_white.csv` | 3,928 | 大写十六进制行 | **57** 行；行长分布 `{32:3, 64:47, 96:4, 128:3}` hex → 16/32/48/64 字节，即 **MD5/SHA-256/SHA-384/SHA-512 混合长度的摘要白名单** | `mc/d.java:128`（`!IS_INTERNATIONAL_BUILD` 分支） |
| `global_behavior_record_white.csv` | 3,115 | 同上 | **43** 行；`{32:2, 64:29, 96:12}` | `mc/d.java:128`（国际版分支） |
| `night_charge_back_list.json` | 9 | JSON | `[""]`（空占位） | `未知` |
| `tvm_whitelist.json` | 3,221 | JSON | **4** 条 `{package_name, signature, tui_apps[{sha256}]}`（TEE/TUI 可信 UI 白名单） | `fh/c.java:112` |
| `scanitem.json` | 16,462 | JSON | `{manual:28, system:8, version:8}`（一键体检扫描项） | `未知` |
| `firstaid_scanitem.json` | 4,923 | JSON | 5 组：`Performance:3, Internet:3, Operation:5, ConsumePower:2, Other:3`（急救箱） | `未知` |
| `warning_center_disaster_icon` | 5,363 | JSON（带尾逗号） | **21** 个灾害码 → 75 条 4 色图片名映射（全部命中真实文件；`fire` 被 11G01/11G05 共用） | `com/miui/warningcenter/disasterwarning/WarningCenterDisasterDetailActivity.java:341` |
| `warning_center_disaster_guide_text` | 14,041 | JSON（带尾逗号） | **11** 个灾害码 → 4 色防御指南全文 | `…WarningCenterDisasterDetailActivity.java:105` |
| `garbage_clean.db` | 36,864 | SQLite3 | 3 表，t_cache 4 行 | **无（本 APK 内无引用）** |
| `pinyinindex.idf` | 54,199 | 自定义 IDF 表 | **20,902** 汉字 → 拼音 | `am/a.java:57` → `cl/a.java` |
| `rec_model_20230425.mnn` | 810,908 | MNN FlatBuffers | 层图见 §8.1 | `ia/j.java:88` → `MNNNetNative` |
| `protobuf/antispam.proto` | 2,319 | protobuf IDL（明文） | `package antispam; option optimize_for = LITE_RUNTIME` | 服务端/客户端序列化 |
| `protobuf/syncroot.proto` | 252 | protobuf IDL | `import "antispam.proto"; package syncroot;` | 同上 |

### 13.2 `*_support_activity_list.json` 家族（"视频/通话小窗"白名单，均明文 JSON 数组）

| 文件 | 条数 | 文件 | 条数 |
|---|---|---|---|
| `aisubtitle_support_activity_list.json` | 20 | `beauty_face_support_activity_list.json` | 4 |
| `beauty_light_default_support_list.json` | 31 | `conversation_record_activity_list.json` | 9 |
| `game_box_barrage_v3_support_apps.json` | 14 | `gesture_effect_support_list.json` | 28 |
| `pickup_support_activity_list.json` | 18 | `pickup_support_activity_list_phone.json` | 19 |
| `privacy_all_support_activity_list.json` | 6 | `privacy_single_support_activity_list.json` | 11 |
| `protrait_support_activity_list.json` | 23 | `simultaneous_interpretation_system_app_activity_list.json` | 2 |
| `simultaneous_interpretation_third_app_activity_list.json` | 18 | `support_split_scene_activities.json` | 1 |
| `ultraclear_support_activity_list.json` | 5 | `vt_camera_activity_list.json` | 11 |
| `vtb_default_support_activity_list.json` | 34 | `music_app.json` | 14 |
| `charge_current_config.json` | 26 | `charge_time_config.json` | 6 |

元素结构统一为 `{"pkgName":"com.tencent.mm","activityName":"com.tencent.mm.plugin.voip…"}`。

### 13.3 Lottie 动画 JSON（`{v,fr,ip,op,w,h,nm,ddd,…}`，共 30 个，均明文）

`dock_settings_*.json`(8)、`normal-light/dark*.json`(4)、`unfold-*.json`(4)、`popNotice*.json`(4)、`externalDark/Light.json`、`flip_wireless_*.json`、`phone_wireless_*.json`、`n1_wireless_charge.json`、`privacy_camera/privacycamera_*.json`、`img/privacy_apps_lottie*.json`(4)、`performance_mode/data.json`、`redmi_performance_mode/Ai-Model.json`、`game_turbo_box_mode_change/{red,blue}_light.json`、`shoulder_key/{enter,settings,using}_guide.json`、`tb_guide_style1.json`。
（Lottie 版本字段 `v` = `5.6.6` / `5.7.14` / `5.8.1`。）

### 13.4 图片/字体/压缩包

| 组 | 数量 | 说明 |
|---|---|---|
| `warningcenter/` | 71 webp/png | 见 §10，481,089 B |
| `power_images/` | 8 webp | 省电模式插图 |
| `performance_mode/images/`、`redmi_performance_mode/images/` | 8 + 7 png | 性能模式插图。**6 张完全相同（错位一一对应）**：`performance_mode/images/img_2..7` ≡ `redmi_performance_mode/images/img_1..6`（SHA-256 逐一相同）；仅首图不同（`performance_mode/images/img_0.png` 4,806 B vs `redmi_performance_mode/images/img_0.png` 22,601 B） |
| `shoulder_key/images/` | 8（4 webp + 2 png + 2 webp） | 肩键引导 |
| `img/` | 4 png + 4 lottie json | 应用锁/游戏加速/卸载/自启动图标 + 隐私 Lottie |
| `images/img_0.png` | 1 | 18,275 B |
| `maml/` | 4 组 × 4-5 文件（png + `manifest.xml`） | MAML 卡片：`antispam/{dark,light}/{call,sms}`、`na_nosim(_dark)` |
| `fonts/` | 7 otf + 2 ttf | MiType 系列（`Mitype.otf` 等，CFF/OTTO）+ `Roboto-BoldCondensed.ttf` + `TitilliumWeb-SemiBold.ttf` |
| `dexopt/` | `baseline.prof`(1,272) / `baseline.profm`(137) | ART baseline profile（magic `pro\0 0 1 0` / `prm\0 0 2 0`） |
| `dock_tips.zip` | 210,751 | ZIP，~7 条目 |
| `shoulder_key/guide_{next,single,trigger}.zip` | 87,432 / 103,850 / 104,928 | ZIP，各 ~13 条目 |

### 13.5 `maml/` 卡片（`观测`）

`maml/antispam/{dark,light}/{call,sms}/` 与 `maml/na_nosim(_dark)/` 各含 `manifest.xml` + `bg.png` / `bgmask.png` / `board.png` / `shape.png`。
- 4 个 `manifest.xml` 大小几乎相同（4,198–4,199 B）→ 同一模板、仅主题/场景参数不同（`推断`）。
- 用途：**来电/短信拦截界面的 MAML 卡片资源**（与 `protobuf/antispam.proto`、`pattern` 同属骚扰拦截链路）。

### 13.6 通用 asset 打开点（便于后续定位）

`getAssets().open(` 的调用点（`观测`，jadx 全量 grep）：

| 位置 | 说明 |
|---|---|
| `a8/g.java:13`、`a8/y.java:103`（按行读） | 通用 asset 读取工具 |
| `am/a.java:57` | `pinyinindex.idf` |
| `ea/c.java:21` | `character.json` |
| `f7/b.java:299` | `quickreply_default_list` |
| `fh/c.java:112` | `tvm_whitelist.json` |
| `h1/b.java:91` | 图片（`BitmapFactory.decodeStream` + 前缀拼接） |
| `i4/a.java:147`、`ia/d.java:80`、`oi/d.java:18`、`w2/t.java:439`、`x4/e.java:13`、`o8/k.java:666` | **按变量名打开**（多用于 Lottie/图片/模型） |
| `lj/c.java:51` | **assets / 内部 files 双源**（`dict`/`model`/`pattern`/`phish`） |
| `m2/e.java:24/55`、`m2/f.java:416/455` | `white_url.json` / `service_num.json` / `address.json` |
| `mc/d.java:128` | `behavior_record_white.csv` 或 `global_…`（按 `Build.IS_INTERNATIONAL_BUILD`） |
| `xh/a.java:354` | 通过 URI scheme `assets://` 打开 |
| `com/miui/warningcenter/disasterwarning/WarningCenterDisasterDetailActivity.java:105` | `assetManager.open("warning_center_disaster_guide_text")` |
| `com/miui/warningcenter/disasterwarning/WarningCenterDisasterDetailActivity.java:341` | `getAssets().open("warning_center_disaster_icon")` |
| `com/miui/warningcenter/disasterwarning/WarningCenterDisasterDetailActivity.java:370` | `getAssets().open("warningcenter/" + asString)` → `Drawable.createFromStream`（**71 张预警图标的唯一读取点**） |

> **方法学提醒**：PowerShell 的 `Select-String -Path "dir\**\*.java"` **不会递归**，只匹配一层目录。笔者首轮 grep 因此漏掉了 `com/miui/**` 下的深路径文件（如 `EventType.java`、`SensitiveWordDetectProvider.java`、`WarningCenterDisasterDetailActivity.java`）。改用 `Get-ChildItem -Recurse -Filter *.java | Select-String`（10,624 个 .java）后修正了 §7、§10 的结论。后续成员请勿复用 `**` 通配写法。

---

## 14. 内置数据库 / 数据文件清单（一页速查）

| 文件 | 类型 | 条数/规模 | 用途 | 读取它的类/方法 | 明文? |
|---|---|---|---|---|---|
| `assets/garbage_clean.db` | SQLite3 | 3 表；`t_cache` 4 行，`t_ad`/`t_residual` **0 行** | 垃圾清理规则（骨架+样例） | **无（DEX 无该字符串）** | ❌ 混淆（hex，流密码特征） |
| `res/raw/parse.db` (ZIP `res/rKy.db`) | SQLite3 | `DIR_PARSE` **1043 行/884 包**；`TEMP_DIR` 10 行 | 应用缓存目录 → 包名/中英文名映射，用于深度清理 | `ra/e.java`（`R.raw.parse` → `getDatabasePath`；`g()/f()/c()`） | ✅ 明文 |
| `assets/dict` | 自定义二进制 | **348,982 词 + 35,224 字表**，totalFreq 60,101,983 | 中文分词（jieba） | `lj/g.java:32` → `mj/a.h()` | ✅ 结构明文（二进制编码） |
| `assets/pinyinindex.idf` | 自定义 "IDF " v2 | 20,902 汉字 / 1,336 去重拼音串 | 汉字→拼音（首字母/多音字） | `am/a.java:57` → `cl/a.java` | ✅ 结构明文（二进制编码） |
| `assets/character.json` | JSON | 40 条 | 字符→id 词表 | `ea/c.java:21` | ✅ |
| `assets/pattern` | 宽松 JSON | v1.2，≥6 组正则 | 短信重要性/钓鱼判定 | `lj/c.java:22` | ✅ |
| `assets/model` | 自定义二进制 | 680,154 B | 未知（推断：文本分类权重） | 无实际读取者 | ❓ 二进制，格式未知 |
| `assets/rec_model_20230425.mnn` | MNN FlatBuffers | 810,908 B，embedding×9 + linear×5 | App 预测/预加载排序 + 端侧训练 | `ia/j.java` → `MNNNetNative`/`MNNTrainNative` | ❓ 二进制（结构可读） |
| `assets/censor_source` | 加密二进制 | 2,688 B | 敏感词库 | `liblib-riskcontrol.so` `SensitiveWordDetector::load` | ❌ 未解 |
| `assets/rsapubkey` | Java 序列化 KeyRep | 419 B | RSA 公钥 | 未知 | ✅ 结构可读 |
| `assets/white_url.json` | JSON | 452 条 | URL 白名单 | `m2/e.java:24` | ✅ |
| `assets/service_num.json` | JSON | 224 条 | 服务号码库 | `m2/e.java:55` | ✅ |
| `assets/address.json` | JSON | 34 省/336 市 | 归属地 | `m2/f.java:416/455` | ✅ |
| `assets/tvm_whitelist.json` | JSON | 4 条 | TEE/TUI 白名单 | `fh/c.java:112` | ✅ |
| `assets/behavior_record_white*.csv` | CSV(hex) | 57 / 43 行 | 行为记录白名单（按签名） | `mc/d.java:128` | ✅ |
| `assets/safepay_*` | JSON | 211/7/13/4 | 支付安全扫描目标 | 未知 | ✅ |
| `assets/{financial,photography}_package_top` | JSON | 300 / 300 | 分类榜单 | 未知 | ✅ |
| `assets/warning_center_disaster_{icon,guide_text}` | JSON | 21 / 11 条 | 灾害图标 & 防御指南 | `WarningCenterDisasterDetailActivity.java:341 / :105` | ✅ |
| `assets/warningcenter/` | 图片 | 71 张 / 481,089 B | 灾害预警图标（`fire` = 11G01/11G05） | `WarningCenterDisasterDetailActivity.java:370` | — |
| `res/raw/wlan_speed{,_new}.crt` | X.509 PEM | 632 / 2,202 B DER | miwifi.com 信任锚 | `network_security_config.xml` | — |
| `assets/protobuf/*.proto` | IDL | 2 个 | 拦截备份/同步 schema | 生成代码 | ✅ |
| `assets/placement_id` | 文本 | 1 行 | Facebook 广告位 | 未知 | ✅ |

---

## 15. 未解之谜（按优先级）

1. **`assets/censor_source` 的 AES 密钥/IV**（高）。已知：`SensitiveWordDetectProvider`（ContentProvider，白名单仅 `com.android.camera` / `com.miui.mediaeditor`）只把 `AssetManager` 传给 native，**不传密钥** → 密钥内置在 `liblib-riskcontrol.so`；AES 为 ECB/CBC/PKCS5 自实现。已排除：**任意连续 16/32 字节窗口作密钥**（49 万候选 × ECB/CBC，PKCS5+全文校验 0 通过）、单字节 XOR、口令派生密钥、deflate/gzip/brotli。**缺**：aarch64 静态反汇编（本机 Ghidra/Hopper/IDA/Kuna/rizin/objdump 全部不可用）。**下一步**：Linux + Ghidra/rizin 分析 `_ZN21SensitiveWordDetector4loadEPc` → 追 `AES::AES(AESKeyLength, key, iv)` 实参；或真机 hook `AES::KeyExpansion`。与队友 `native-libs.md` §2.6 结论方向一致，可合并。
2. **`assets/garbage_clean.db` 的读取者与混淆算法**（高）。**全量递归 grep（10,624 个 .java）+ 三 dex/18 .so 字节扫描**后，`garbage_clean.db` / `t_cache` / `t_residual` 均无引用 → 本 APK 内无读取者。要么由 `com.miui.cleanmaster` 等外部组件消费，要么已废弃。建议按"死资产"归档；若 Lead 要求，下一步可在 `com.miui.cleanmaster`/OTA 包中搜同名 DB。
3. **`assets/model` 的格式与用途**（中）。头 `1,0,5,68,58` + float32 权重；`lj/c.java:17` 的 `{"dict","model","pattern","phish"}` 数组**从未被读取**（全量 grep 仅 1 处定义），`phish` 资源亦不存在 → 可能整套 `lj` 逻辑已废弃或由插件加载。
4. ~~`11A01/11A02/11A51/11B14/11B16/11B22/11D01/11D02/11G01/11G05/fire` 的灾害名称~~ → **已解决**（`EventType.java` 25 个枚举 + `Utils.ICON_LIST/TITLE_LIST`；`fire` = 11G01 森林火灾 / 11G05 草原火灾 共用图标）。**遗留**：`11B07`（沙尘暴）、`11B14`、`11B16`、`11B22` 这 4 个 code **不在 `EventType` 枚举里**，只在 assets 图标与 `guide_text` 中出现 → 说明**云端下发的新灾害类型**已提前在资源里备好图标，但客户端枚举尚未同步（`推断`）；其规范名称需查 12379 标准或服务端配置。
5. **`assets/pattern` 的完整规则引擎**（低）。只解出 JSON 结构与 6 组 feature；`combi` 表达式的求值器、`sms_type` 取值含义、`address.type` 1..6 语义需读对应 Java 解析器（本任务范围外）。
6. **`res/rKy.db`→`parse.db` 的云端下发协议**（低）。`ra/e.java:148 c(list,json)` 是覆写入口，调用方/接口未追。
7. **`11B19`(霾) 的 guide 文本红色为空**（低）。图标有红色版本但指南缺红色文案 —— 数据不齐，非设计缺失（`观测`）。

---

## 16. 复现命令（本机已验证）

```powershell
# 全量资产指纹（大小/SHA-256/熵/magic）
node _analysis\tools\scan_assets.js _analysis\apk_full\assets > _analysis\tools\assets_scan.tsv

# 逐文件类型与结构识别
node _analysis\tools\summarize_assets.js > _analysis\tools\assets_summary.tsv

# dict（jieba）完整解析
node _analysis\tools\parse_dict.js

# pinyinindex.idf 完整解析（IDF v2）
node _analysis\tools\pinyin2.js

# garbage_clean.db 行数/行内容/块重复分析
node _analysis\tools\db_count.js ; node _analysis\tools\final1.js

# parse.db 全量
node _analysis\tools\dump_parse.js

# censor_source 爆破（全部失败）
node _analysis\tools\aesbrute.js ; node _analysis\tools\censor_attack.js
node _analysis\tools\aes_pad.js   # 全窗口 + PKCS5 粗筛
node _analysis\tools\aes_pad2.js  # pad>=2 收敛 + 全文校验（0 通过）

# warningcenter 图标矩阵 / 清单一致性 / code 复用
node _analysis\tools\wc3.js ; node _analysis\tools\wc4.js ; node _analysis\tools\wc5.js

# 证书
node _analysis\tools\crt2.js
```

REA 侧：`inspect_sqlite_database`（garbage_clean.db / res/rKy.db）、`inspect_apk_signature`。**注意**：REA 在本机（Windows）**无法**打开 aarch64 ELF（`open_binary` 返回 `target_format_unsupported`：`Windows Ghidra P0 accepts native PE and explicitly selected raw 6502 targets only`），故 `.so` 静态反汇编未做。
