> **Historical analysis.** This archive includes observations that remain unverified or were later corrected. See the [final report](../REPORT.md) and [independent review](../REVIEW.md) for the reviewed findings on upload scope, consent, local FBO delegation, emergency SMS encryption, and provider authorization. Source references retain their original decompilation paths. The publication includes the selected files under evidence/.

# MIUISecurityCenter built-in data assets reverse investigation report

> Investigator: teammate `assets-data` | Target APK: `MIUISecurityCenter-10.4.9-250402.0.1.apk`
> SHA-256 `fc63de0eb68016fc2c7469ec6d33364ffef073643fea9e86781e5fcb1c678cfd`
> Range: `assets/` all, `res/raw`, `res/xml`, `META-INF/`. **does not include** Java business code behavior analysis (responsible separately).
> Annotation discipline: `Observed` = Directly from shipped artifact / actual decoding output; `Inferred` = Conclusion derived from evidence; `Unknown` = No evidence obtained.

---

## 0. Summary of conclusion (can be excerpted directly)

| # | Conclusion | nature |
|---|---|---|
| 1 | `assets/` Total **248 files / 9,409,463 bytes (8.97 MiB)**. Among them, 18 files without extension occupy 4.39 MiB (including `dict` 3.64 MiB). | Observed |
| 2 | **`assets/dict` is not an encrypted**: it is a custom binary serialization **of the**jieba Chinese word segmentation dictionary, and the format is specified field by field in `mj/a.java:260-330` (`h(InputStream)`). Complete analysis of used Node: **348,982 words + 35,224 word lists**, `totalFreq=60,101,983`, `minFreq=2`, HMM parameters and open source jieba `prob_start`/`prob_trans` **is identical bit by bit to**. | Observation (see §3) |
| 3 | **`assets/pinyinindex.idf` is not an encrypted**: it is `cl/a.java` defined **"IDF " tabular format (version 2)**. Completely parsed: 20,902 Chinese characters → Lowercase Pinyin (including multi-phonetic characters), 1,336 deduplicated strings. Sample: medium→zhong, `Heavy→zhong,chong`, `long→zhang,chang`, `Single→dan,chan,shan`. | Observation (see §4) |
| 4 | **`assets/character.json` is plain text JSON** (600 B), 40 `character→id` (`<pad>=0,<unk>=1,a=2 … 9=37, .=38, _=39`), which is the character vocabulary of `rec_model_20230425.mnn`. The judgment of "unclear text" in the baseline needs to be corrected. | Observation (see §5) |
| 5 | The 4 lines of sample data of **`assets/garbage_clean.db` are all hexadecimal obfuscation string**, which is not a human-readable path; `t_ad` / `t_residual` **each have 0 lines of** (only table structure). And the string `garbage_clean.db`**does not exist in the three dexes of**, nor is there any `t_cache` table name reference → There is no reader **for** in this APK. | Observation (see §2) |
| 6 | The real readable garbage cleaning rule base is in **`res/raw/parse.db`** (apktool name `parse.db`, actual name `res/rKy.db` in ZIP): `DIR_PARSE(PATH, PACKAGE_NAME, APP_NAME)` **1043 lines / 884 Package**, plain text; `TEMP_DIR` 10 lines. Query after copying from `ra/e.java` to `getDatabasePath("parse.db")`. | Observation (see §6) |
| 7 | **`assets/censor_source` is the high-entropy binary** (2688 B, entropy 7.933, 16-byte alignment). **failed to decipher the plaintext**. The call chain has been completely restored: `com.miui.riskcontrol.SensitiveWordDetectProvider` (ContentProvider, **only allows `com.android.camera` / `com.miui.mediaeditor` to call**) → `setAssetManager()` + native `hasSensitiveWords()` → `liblib-riskcontrol.so`’s `SensitiveWordDetector::load` + self-implementation `AES` + `TrieTree`; .so contains the string `censor_source`, debugging paths `/storage/emulated/0/censor_source`, `AssetManager open fail!`, `needPadding %u char(s)`, and `File has %d lines`. **The key** is not transmitted on the Java side, so the key must be built into .so; all 16/32 byte windows (about 490,000) in .so are exhaustively used as AES-128/256 ECB/CBC key and PKCS5 padding + full text verification → **0 By**. | Observed + unknown (see §7) |
| 8 | `assets/rec_model_20230425.mnn` = **Ali MNN FlatBuffers model**, the complete layer graph has been restored from the flatbuffer string: `sparse_inputs_1..6`(int[30]) + `sparse_inputs_time` → `embedding_0..8` → `p2o.MaxPool` → `relu_0..4` → `linear_0..4` → Output `linear_9.tmp_1` (2 categories). That is, the "next App prediction" ranking model is loaded by `ia/j.java` with `MNNNetNative.nativeCreateNetFromFile` and can be retrained on the `MNNTrainNative` end-to-end. | Observations (see §8) |
| 9 | `assets/model` (680,154 B) format **unknown**: The header is an int32BE `1,0,5,68,58` + uint16 table, and the text is mainly `0xC0*` bytes (the proportion of float32BE falling in `[-6,0]` is 36.8% vs LE 18.4%)→ Inferred as float32 weight matrix + integer/index header. **does not have any Java code to open it** as a literal (it only appears in the name array `{"dict","model","pattern","phish"}` of `lj/c.java:17`). | Inferred + unknown (see §9) |
| 10 | `assets/warningcenter/` **71 icons**. `assets/warning_center_disaster_icon` (JSON, 21 disaster codes) has a total of 75 `code→filename` mappings, **all hits the real file**; among them, `warningcenter_fire_*` is replaced by **`11G01` (forest fire) and `11G05` (prairie fire) Shared** (4 files × 2 = 8 mappings). For the complete code→disaster name mapping, see `com/miui/warningcenter/disasterwarning/model/EventType.java` (25 enumeration values, including typhoon/heavy rain/blizzard/cold wave/hail/strong wind/high temperature/fog/haze/road ice/heavy pollution/wave/earthquake/storm surge/thunderstorm/forest fire risk/geological disaster meteorological risk/flood/waterlogging/flash flood/landslide/mudslide/forest fire/grassland fire/others). The icon is loaded directly from `WarningCenterDisasterDetailActivity` by `getAssets().open("warningcenter/"+name)`. | Observations (see §10) |
| 11 | Signature: **v1 + v2 + v3**, single signer = **MIUI Platform certificate** (`C=CN, ST=Beijing, L=Beijing, O=Xiaomi, OU=MIUI, CN=MIUI, emailAddress=[VENDOR_EMAIL_REMOVED]`, serial number `E552A8ECB9011B7C`, 2011-12-06 → 2039-04-23, RSA), SHA-256 fingerprint `c9009d01ebf9f5d0302bc71b2fe9aa9a47a432bba17308a3111b75d7b2149025`; v1 file is `META-INF/PLATFORM.SF` + `PLATFORM.RSA`; v2/v3 digest is consistent with the current byte. | Observations (see §12) |
| 12 | There are two other built-in certificates: `res/raw/wlan_speed.crt` (`CN=www.miwifi.com` self-signed, RSA-1024, 2020→2030) and `res/raw/wlan_speed_new.crt` (`OU=MIWIFI ROOT CA`, issued by `CN=Xiaomi Root CA`, RSA-2048, 2020→2120), which were `res/xml/network_security_config.xml` is designated as the trust anchor for the `miwifi.com` domain. | Observation (see §11) |

**3 corrections to the shared baseline** (please lead to adopt):
1. `assets/address.json` and `assets/pattern` use **UTF-8**. Decoding as GBK produces garbled text such as `銆怺`. Decoding `address.json` as UTF-8 yields `"n":"北京"` (Beijing), and decoding `pattern` yields `"version":"1.2"`.
2. `assets/character.json` is **plaintext** JSON.
3. `assets/dict` / `pinyinindex.idf` **is not the "encrypted or custom unknown format"**, but two custom binaries that **can fully parse** (jieba dictionary / IDF table). All contents and samples have been solved this round.

---

## 1. Methods and tools (reproducible)

| Purpose | means |
|---|---|
| File-by-file recognition (magic / entropy / SHA-256 / JSON structure) | Node v24.22.1 Script `_analysis/tools/scan_assets.js`, `summarize_assets.js`, `assets_summary.tsv` |
| SQLite structure + sample row (evidence) | REA `inspect_sqlite_database`（provider `SQLite 3.53.4`） |
| SQLite full count/full rows | Node built-in `node:sqlite` (`DatabaseSync`, readOnly) |
| APK signature | REA `inspect_apk_signature` |
| Certificate parsing | Node `crypto.X509Certificate` |
| Decompiled source code retrieval | `_analysis/jadxout/sources/**/*.java`; **must use `Get-ChildItem -Recurse` to recurse** (`Select-String -Path "dir\**\*.java"` is not recursive under PowerShell, and the `com/miui/**` deep path file will be missed), a total of 10,624 .java |
| Binary format restoration | Read `mj/a.java`, `cl/a.java`, `ea/c.java`, `ia/j.java`, `ra/e.java`, and then write the Node parser |
| DEX string cross validation | Scan `classes*.dex` byte stream directly (to avoid mistrusting grep results) |

**Evidence ID (REA)**
- `ev_864f519dd5aad070aa0f4b478bc7a10370fda04e0c347fc87a9ceb717bd1e121` — `garbage_clean.db` full database schema
- `ev_e7729c0c5f9049f98f28b5502abc025bbd194d9dd4165b8cb2bc2fb9db7a258a` — `garbage_clean.db` / `t_cache` 4 lines
- `ev_24470dcb25764c0ccf0d0a50d01f9beeb3860cec74648b7f2dd3e1312028af19` — `res/rKy.db`(=parse.db) schema
- `ev_774c8c1faf38e245d0c008a0979e7abc3958ee7dd6e00b48ac3bc5de819cb354` — APK signature

---

## 2. `assets/garbage_clean.db` - Garbage cleaning rule base (the table structure is real, the data is confused)

### 2.1 Structure (`Observed`, REA `ev_864f…`)

- Files: 36,864 B, SHA-256 `e1d838531cc6f604f77484cb9b95cbf9be41ceb1179b255c7cc27c9d74a4d5a3`, SQLite 3, `database_encoding = UTF-8`, none WAL, no indexes, no views, no triggers.
- surface:

| table | CREATE statement (original text) | Number of lines |
|---|---|---|
| `t_cache` | `CREATE TABLE t_cache (_id integer primary key autoincrement, pkg_name TEXT ,dir_path TEXT ,root_dir TEXT ,res_zh_tw TEXT ,res_zh_cn TEXT ,res_default TEXT ,advise_del TEXT )` | **4** |
| `t_ad` | Same as above (same as 8 columns) | **0** |
| `t_residual` | Same as above (same as 8 columns) | **0** |
| `android_metadata` | `CREATE TABLE android_metadata (locale TEXT)` | 1（`locale = zh_CN`） |
| `sqlite_sequence` | automatic table | 1（`t_cache` → `seq = 4`） |

> `t_ad` / `t_residual` has only schema and no rows → **is released with the package only "skeleton + 4 samples"**, and the actual rules are delivered by the cloud or filled in (inferred) by other components.

### 2.2 4 lines of data (`Observed`, REA `ev_e772…`, hex used Node review length)

| _id | pkg_name (hex, number of bytes) | dir_path (byte) | root_dir (byte) | res_zh_tw | res_zh_cn | res_default | advise_del |
|---|---|---|---|---|---|---|---|
| 1 | `b076f58f1ea99d0ec97ec7ac6a907da3` (16) | `fc58f6c501af8103c87dc7b467da6cb574ebb69119eb1d134e3361aef318c9c5a38b058c` (36) | `fc58f6c501af8103` (8) | 178 B | 178 B | 168 B | `0` |
| 2 | `b076f58f1ea99d0ec97ec7ac6a907da3` (16) | `fc54d1f43aefc604867acea529926eb675a0a981` (20) | `fc54d1f43a` (5) | 214 B | 214 B | 178 B | `0` |
| 3 | `b076f58f1ea99d0ec97ec7ac6a907da3` (16) | `fc54d1f43aefaf068b75c3b27fda6cb676b0bf` (19) | `fc54d1f43a` (5) | 205 B | 205 B | 184 B | `0` |
| 4 | `b076f58f0ba989088a7088a36e9461b47ca9` (18) | `fc74f1cd1aa187488478c5a863` (13) | `fc74f1cd1aa187` (7) | 175 B | 175 B | 173 B | `0` |

`res_*` all start with the same prefix (the first 63 hex characters are the same):
```
a83bf9cd16b29c388e77c0af24cf2df835e7b8990fea562b5b267daea35bc443
```
`root_dir` is always the hexadecimal prefix of `dir_path`.

### 2.3 Field semantics (`Inferred`, with basis)

| Field | Semantics | Basis |
|---|---|---|
| `pkg_name` | The **obfuscation identifier** of the target application package name (16 B = MD5 length; 18 B in line 4 does not match) | Length + 3 lines of the same value = multiple directories for the same App |
| `dir_path` | Obfuscated identification of **cache/junk directory** of this App | Consistent with the `t_cache` table name and `root_dir` prefix relationship |
| `root_dir` | Prefix of `dir_path` (coarse-grained root directory id) | Observation: constant prefix |
| `res_zh_tw` / `res_zh_cn` / `res_default` | The **Chinese/default name** (long string after confusion) to be displayed in this directory under three locales | Naming + three columns with the same prefix and different lengths |
| `advise_del` | Is it recommended to delete (`0`/`1`) | 4 lines full `0`; column names read directly |

### 2.4 Determination of confusion properties (`Observed` + `Inferred`)

- All 4 rows of 12 `res_*` values total 104 deduplicated 16-byte blocks, of which **24 blocks repeatedly appear** (the highest one appears 12 times: `a83bf9cd16b29c388e77c0af24cf2df8`).
- But the overall length **is not an integer multiple of 16** (178 / 205 / 173 / 19 / 13 bytes...), so **is not a block cipher like AES/ECB**.
- "The same ciphertext at the same offset + arbitrary length" is a typical feature of **streaming mode (CTR/OFB/CFB) key stream multiplexing** (`Inferred`).
- Counter-proof: Suppose `pkg_name` is MD5 (package name), use 28 common package names (including `com.tencent.mm`, `com.tencent.mobileqq`...) and path candidates (800+ combinations such as `/sdcard/<pkg>/cache`) to match MD5/SHA-1 → **0 Hit**. SHA-1 Assume that line 2 of `dir_path` 20 B is the most similar, but it also misses.

### 2.5 Who is reading it? (`Observed`: no one)

- Directly scan `classes.dex` / `classes2.dex` / `classes3.dex` byte stream: string `garbage_clean.db` **does not exist**; the only hit position of `garbage_clean` is `classes3.dex@5321625`, and the context is `string/activity_title_garbage_cleanup` (**false positive**).
- `t_cache` / `t_residual` also does not exist (the hit for `t_cache` comes from a substring like `get_cache`).
- `lib/arm64-v8a` All 18 `.so` also do not include `garbage_clean`.
- Conclusion: There is no reader **in** and APK. May be used by `com.miui.cleanmaster` (garbage cleaning independent component) or cloud-delivered component → `Unknown`.

---

## 3. `assets/dict` - jieba Chinese word segmentation dictionary (fully decoded)

### 3.1 Judgment (`Observed`)

- 3,639,702 B, SHA-256 first 16 bits `f99086243babe60f`, entropy 6.102, head `00000001 00000000…`.
- **reads chain**: `lj/g.java:32` → `lj/c.b(context,"dict")` → `mj/a.h(InputStream)` (`mj/a.java:260`).
- Name also appears in `lj/c.java:17`: `public static final String[] f29439b = {"dict","model","pattern","phish"};`
- If there is a file with the same name (`context.fileList()` hit) and `lj.c.f29440c == false` in the App private directory, **will read the internal file** (`lj/c.java:42-57`) first → the dictionary can be replaced by hot update (`Inferred`).

### 3.2 Format (`Observed`, field by field from `mj/a.java:260-330`; big endian `DataInputStream`)

```
u32 version? (read and discard) = 1
u32 base? (read and discard) = 0
f64 startProb[4] HMM starting logarithmic probability (B/E/M/S)
f64 transProb[4][4] HMM transfer logarithmic probability
i32 charCnt[4] Number of word/prefix table entries for each word length (1..4)
i32 mLengthCount = L word length number
i32 lenCnt[L] Number of entries in each file
i32 mTotalFreq  i32 mMinFreq
-- After: character table area + vocabulary area, the total length must be equal to available() (mj/a.java:313-317 will verify) --
Character table i: base_i = Σ charCnt[0..i-1]*6; entry = u16 char(BE) + f32 freq(BE)
Vocabulary L: base_L = cumulative; entry = L×u16 char(BE) + i32 freq(BE), length L*2+4
```

### 3.3 Actual analysis output (`Observed`, script `_analysis/tools/parse_dict.js`)

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

- `startProb` / `transProb` is exactly the same as the `prob_start` / `prob_trans` value **of the open source**jieba** ** (`-0.26268660809250016 / -1.4652633398537678 / -0.510825623765990 / -0.916290731874155 / -0.5897149736854513 / -0.8085250474669937 / -0.33344856811948514 / -1.2603623820268226 / -0.7211965654669841 / -0.6658631448798212`) → `Observed` + `Inferred` (homogeneous implementation, MIUI transplanted version).
- The total number of **entries is 348,982**; the total number of word/prefix table entries is **35,224**; the character table area is 211,344 B, and the vocabulary area is 3,428,098 B. The sum of the two is exactly equal to the remaining length of the file (self-verification passed).

### 3.4 Plain text sample (`Observed`, real solution)

```
charTable[L1] count=6857 One:-3.6545 D:-8.1250 Seven:-78174:-6.3096 Three:-5.9321 Upper:-5.7396 Not:-4.2743
wordTable L=1  count=11580   一|217830  丁|3607  丂|2  七|7675  万|29391  三|42542
wordTable L=2 count=114139 B-ultrasound|3 C#|3 T-shirt|4 one by one|1670 ten thousand|442
wordTable L=3 count=131323 C++|3 one one two|11 one one point|8 one tiny bit|3
wordTable L=4 count=84895 AT&T|3 List one by one|34 One-to-one correspondence|43 I don’t know a thing|3 A little bit|24
wordTable L=5 count=3563 more than seventeen thousand | 2 thirteen thousand yuan | 2
wordTable L=11 count=77 Shanghai Municipal Bureau of Labor and Social Security|7 Central Commission for Discipline Inspection of the Communist Party of China|7
wordTable L=14 count=15 Central Military Commission of the People's Republic of China|2066 The First National Congress of the Communist Party of China|45
wordTable L=16 count=6 Memorial Hall of the Victims of the Nanjing Massacre by Japanese Invaders|3 Office of the Commissioner of the Ministry of Foreign Affairs in the Hong Kong Special Administrative Region|4
```

> Purpose (`Inferred`): **Chinese word segmentation** of text message/harassment content, combined with `pattern` (regular rule set) and `model` (suspected classification weight) for offline semantic judgment.

---

## 4. `assets/pinyinindex.idf` - Chinese characters → Pinyin index table (fully decoded)

### 4.1 Judgment (`Observed`)

- 54,199 B, entropy 5.887, magic `IDF ` (`49 44 46 20`).
- **reads the chain**: `am/a.java:47`. Try `cl.b.b(context,"pinyinindex.idf")` (disk cache) first. If it fails, `am/a.java:57` → `cl.a.a(getAssets().open("pinyinindex.idf", 1))` → return `cl.a$i`; the query is in `am/a.java:73`: `iVar.c(0, (char)c10 - 19968, 0)`, and the results are split according to the delimiter. into Pinyin array (`am/a$b.a(char)`).
- The file-level format is fully defined by `cl/a.java`'s `class f` / `class e` / `class d` / `class i`: magic `{73,68,70,32}` (`cl/a.java:491`), version must == 2 (`cl/a.java:514`).

### 4.2 Format (`Observed`, `cl/a.java`)

```
"IDF " (4B)
i32 version (=2)
i32 nKinds each kind = a table
i32 nKinds' (not used)
per kind: i64 idxTableOffset, i64 dataTableOffset
@idxTableOffset:  i32 nGroups; nGroups × {i32 start, i32 end, i64 fileOffset}
@dataTableOffset: i32 nDesc;   nDesc × {u8 type, u8 idxSize, u8 lenSize, u8 elemStride, i64 dataOffset}
```
Element reading (`cl/a.java:410-455` `l()` + `651-683` `d()/e()`):
```
rowBase   = group.fileOffset + (index - group.start) * Σ(idxSize)
sub = read rowBase at idxSize bytes
delta = read (dataOffset+4) + sub*elemStride bytes at elemStride
payloadAt = (dataOffset+4) + delta
len = read payloadAt at lenSize bytes
value = payloadAt + lenSize starting from len bytes (UTF-8 when type=STRING)
```
`type` enumeration sequence (`cl/a.java` `EnumC0089a`): `BYTE, SHORT, INTEGER, LONG, STRING, BYTE_ARRAY, SHORT_ARRAY, INTEGER_ARRAY, LONG_ARRAY` → this document `type=4 = STRING`.

### 4.3 Actual analysis output (`Observed`, script `_analysis/tools/pinyin2.js`)

```
size 54199  tag "IDF "  version 2  nKinds 1
kind0: groups [{s:0, e:20902, off:12395}]  descs [{type:4(STRING), idxSize:2, lenSize:1, elemStride:2, dataOff:68}]
Number of deduplicated string entries (i32 at dataOff) = 1336
```

**plaintext sample (real solution):**

| Chinese characters | index | sub | delta | len | value |
|---|---|---|---|---|---|
| in | 45 | 38 | 2850 | 5 | `zhong` |
| country | 2301 | 472 | 5287 | 3 | `guo` |
| king | 9611 | 120 | 3261 | 4 | `wang` |
| meters | 11891 | 218 | 3778 | 2 | `mi` |
| heavy | 17357 | 1009 | 9531 | 11 | `zhong,chong` |
| long | 18303 | 1265 | 11747 | 11 | `zhang,chang` |
| Single | 1365 | 420 | 4918 | 13 | `dan,chan,shan` |
| sink | 7816 | 877 | 8403 | 9 | `shen,chen` |
| once | 6398 | 800 | 7775 | 9 | `ceng,zeng` |
| Zhai | 12767 | 1066 | 10085 | 7 | `zhai,di` |
| A | 18495 | 1251 | 11623 | 3 | `a,e` |
| U+4E00 | 0 | 1 | 2673 | 2 | `yi` |
| U+4E01 | 1 | 2 | 2676 | 10 | `ding,zheng` |
| U+4E02 | 2 | 3 | 2687 | 3 | `kao` |

- Coverage interval `[0, 20902)` = U+4E00 … U+9FA5 (complete set of CJK basic areas) → row index = `char - 19968` (`Observed`, consistent with the offset of `am/a.java:73`).
- The file layout is self-consistent: header 4+4+4+4+16 = 32 B; delta table @72 onwards 1336×2 = 2672 B; row table @12395 onwards 20902×2 = 41804 B; `12395 + 41804 = 54199 = file length` ✓ (`Observed`).
- `am/a.java` and another 85 **artificial multi-phonetic characters/surname reading coverage table** (`f2410c`): such as `(char)21333("单")→SHAN`, `(char)37325("重")→"chong"`, `(char)21306("区")→OU`, `(char)35299("解")→XIE`, etc. (`am/a.java:121-197`; the package name in the comment `f2409b` and `Chanyu→CHAN,YU`, `Changsun→ZHANG,SUN`, `Wanqi→MO,QI`, `Yuchi→YU,CHI` and other 6 compound surnames) → `Observed`.

---

## 5. `assets/character.json` - plain text character table (`Observed`)

600 B, UTF-8 plain text JSON, 40 keys (full text of `character.json`):

```json
{ "<pad>":"0", "<unk>":"1", "a":"2", … "z":"27", "0":"28", … "9":"37", ".":"38", "_":"39" }
```

- **reads chain**: `ea/c.java:21` → `new JSONObject(...)` → builds `map(char→int)` and `map2(int→char)` → `new fa.a(context, resources, str, map, map2)` (`ea/c.java:39-50`), and throws `"Couldn't read underlying model for AppRecommenderImp"` on failure.
- **consumer**: `fa.a` → `ia/j.java` (App recommendation/sorting), `ia/d.java:202 f(String,HashMap)` converts the package name to `int[30]` (look up the table character by character, add `<unk>`=1 when out of bounds, truncate if more than 30 characters, lowercase).
- Conclusion: **character-level vocabulary table** of `rec_model_20230425.mnn`, model input = package name character id sequence (length 30).

---

## 6. `res/raw/parse.db` - Real garbage/cache directory rule base (plain text)

### 6.1 Positioning (`Observed`)

- The actual name in ZIP is `res/rKy.db` (94,208 B); it is renamed to `res/raw/parse.db` after apktool decodes it; the resource id is `R.raw.parse`.
- SHA-256 `5b1da601c11b20eaf53da468a02ae8fda5fe91a5d048ac557301d03ee88767f1`（REA `ev_2447…`）。
- **read chain**: `ra/e.java:39-45` constant `"parse.db"` / `"DIR_PARSE"` → `ra/e.java:60` `getResources().openRawResource(R.raw.parse)` copied to `getDatabasePath("parse.db")` → `SQLiteDatabase.openOrCreateDatabase(...)`; query method `ra/e.java:256 g(pkg)` (`select _id, PATH, PACKAGE_NAME from DIR_PARSE where PACKAGE_NAME='…'`), `ra/e.java:211 f(list)`, write `ra/e.java:148 c(list,json)` (JSON issued by the **server will insert/overwrite**).
- Path assembly rules (`ra/e.java:267-281`): `PATH` starts with `/` → `Environment.getExternalStorageDirectory() + PATH`; otherwise → `<ext>/ + PATH`; and filter `"."`, `".."`, `android` prefixes.

### 6.2 Data scale (`Observed`, `node:sqlite` full scale)

| table | Number of lines | Notes |
|---|---|---|
| `DIR_PARSE` | **1043** | 884 different `PACKAGE_NAME`; `APP_NAME` None NULL |
| `TEMP_DIR` | **10** | Temporary directory rules |
| `android_metadata` | 1 | `locale = zh_CN` |

The `APP_NAME` format of `DIR_PARSE` is `zh_cn=<name>&zh_tw=<name>&=` (**three languages are concatenated in the same string**, which is different from `garbage_clean.db` which is divided into three columns).

### 6.3 Sample row (`Observed`)

```
1  /.budingdoyen     iw.avatar                 zh_cn=布丁爱生活&zh_tw=&=
2  /.helix.thumbnails com.real.RealPlayer       zh_cn=Real Playar&zh_tw=&=
3  /gomarket         cn.goapk.market           zh_cn=安智市场&zh_tw=&=
7  /.wblog-images    com.tencent.WBlog         zh_cn=腾讯微博&zh_tw=&=
13 /.MTXX            com.mt.mtxx.mtxx          zh_cn=美图秀秀&zh_tw=&=
19 /DUOMI            com.duomi.android         zh_cn=多米音乐&zh_tw=&=
```
The package with the largest number of lines (`Observed`): `""` (36 lines, that is, a general directory without a package name), `com.qzone`(7), `com.taobao.taobao`(6), `com.tencent.hd.qq`(5), `com.tencent.padbrowser`(4), `com.baidu.baike`(4).

### 6.4 `TEMP_DIR` All 10 lines (`Observed`)

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

`TYPE` (`Inferred`): `0` = relative external storage root; `1` = absolute `/data` path (consistent with the assembly logic of `ra/e.java`). `EXCLUDED=1` = Not cleaned by default.

---

## 7. `assets/censor_source` - Sensitive lexicon (encrypted, undeciphered)

### 7.1 Observations

- 2,688 B, SHA-256 first 16 bits `38ca17ec730e6a54`, **Shannon entropy 7.933** (nearly uniformly random), first 16 B = `0c8c3b382bba7afc3f9d9f45ff6eea04`, no magic, length = 168×16 (16-byte aligned).
- The string `censor_source`**does not exist in the three dexes of**; the only dex hit for `censor` is the `classes3.dex@6049860` context `censorResult` (a false positive).
- The real quote is in **`lib/arm64-v8a/liblib-riskcontrol.so`** (247,832 B):

| .so offset | content |
|---|---|
| 18008 / 18043 | Symbol name `SensitiveWordDetector` |
| 18065 | Symbol table area, including `AES::EncryptBlock / DecryptBlock / KeyExpansion / Rcon / SubBytes / XorBlocks / AESKeyLength`, `SensitiveWordDetector::load(char*)`, `SensitiveWordDetector::censor(std::string&)`, `TrieTree::insert/find`, `TreeNode::insertChild/findChild` |
| 195305 / 195411 | String `censor` / **`censor_source`** |
| 1954xx | Runtime diagnostic string: `ERROR!`, `keyword and char size mismatch!!`, `Wrong char size when detecting!`, `File has no content!!!`, `load file!`, `File has %d lines`, `Load file tree fail!!!`, `censor sentence %s`, `AssetManager open fail!`, `AssetManager Error!`, `needPadding %u char(s)` |
| 1954xx | Debug coverage path **`/storage/emulated/0/censor_source`** |
| 1955xx | A section of AES T-table constant (`.w+.R.j.06.8.@…` characteristic string) |

→ `Inferred`: `censor_source` is read by native `SensitiveWordDetector::load()`, and internally uses self-implemented AES to decrypt and build `TrieTree` (sensitive word tree). `File has %d lines` / `needPadding %u char(s)` indicates that the plain text should be a "line-separated keyword table", aligned by byte length when loading.

### 7.2 Java side caller (`Observed`)

`com/miui/riskcontrol/SensitiveWordDetectProvider.java` (`classes3.dex`, 79 lines in total):

```java
public class SensitiveWordDetectProvider extends ContentProvider {
    public static final ArrayList<String> f20241a;      // Caller whitelist
    static {  /* "content://com.miui.riskcontrol/censorResult" */
        f20241a = arrayList;
        System.loadLibrary("lib-riskcontrol");           // ← Load liblib-riskcontrol.so
        arrayList.add("com.android.camera");             // Only these two packages can call
        arrayList.add("com.miui.mediaeditor");
    }
    private native boolean hasSensitiveWords(String str);
    private native void setAssetManager(AssetManager assetManager);
    public final Bundle call(String str, String str2, Bundle bundle) {
        if (!f20241a.contains(getCallingPackage())) return null;      // Whitelist verification
        if (!"METHOD_SENSITIVE_WORD_DETECT".equals(str)) return null;
        setAssetManager(getContext().getAssets());                    // ← Only pass AssetManager
        bundle2.putBoolean("hasSensitiveWords", hasSensitiveWords(str2));
        return bundle2;
    }
}
```

Key point (`Observed`): JNI has only two entrances, `setAssetManager(AssetManager)` and `hasSensitiveWords(String)`. **Java does not pass any key/IV to native** → The guess that "the key is injected by Java" is not true; the AES key must be built into **`liblib-riskcontrol.so` within** (or derived from a constant within .so).
External capabilities (`Inferred`): Any APK except `11B`. If forging the package name is not feasible (`getCallingPackage()` is filled in by the system), therefore this IPC is only open to the **camera and album editor** for sensitive words/content review before shooting/editing.

### 7.3 Decryption attempts (all failed, `Observed`)

Script `_analysis/tools/{aesbrute,censor_attack,aes_pad,aes_pad2}.js`:

| try | result |
|---|---|
| All **4,862** printable 16-byte windows in `.so` are used as AES-128 keys (ECB/CBC-zeroIV, padding off), criterion = output UTF-8 CJK scale > 0.3 | 0 hits |
| **exhaustive `.so` all 16 B / 32 B window for AES-128/256 key**, use the last block to make **PKCS5 Fill check** (ECB: `D(C_last)`; CBC: `D(C_last) XOR C_{last-1}`, has nothing to do with the real IV) | About **490,000** candidates; `pad≥1` hits 3,634 (≈1/256 expected false positive), `pad≥2` converges to **12**; do **full file decryption + text score verification on these 12 → 0 by** |
| 25 groups of candidate words (`censor`, `censor_source`, `miui`, `xiaomi`, `securitycenter`, `riskcontrol`, `0123456789abcdef`…) × {Original text, MD5, SHA-1[0:16], SHA-256} × {AES-128/192/256 ECB/CBC} | 0 hits |
| 256 single bytes XOR | 0 hits |
| `inflateRaw` / `inflate` / `gunzip` / `brotli` | All throw exceptions or have extremely low scores |
| Headless disassembly (Ghidra/Hopper/IDA/Kuna) | **is not available on this machine**: REA `open_binary` explicitly rejects `Windows Ghidra P0 accepts native PE and … only`; Hopper does not support win32; `REA_IDA_MCP_CONFIG` / `REA_KUNA_COMMAND` is not configured; `rizin` / `objdump` are not installed |

→ **algorithm and key: `Unknown`**. Excluded: single-byte XOR, common password-derived keys, deflate/gzip/brotli, and any continuous 16/32 bytes in **.so, which is the key** (including two padding verifications of ECB and CBC).
Remaining possibility (`Inferred`): The key is obtained by splicing/transforming **of** multi-section constants in .so (similar to the `WALI_KEY_SEG_1/2/3` method of the same family `libtcp.so`), or fetching bytes by index/offset (non-continuous window).
**recommends**: You need to perform cross-reference analysis on `_ZN21SensitiveWordDetector4loadEPc` and `_ZN3AES…KeyExpansion` on Linux (or a machine with rizin/Ghidra) to locate the actual parameters passed to `AES(KeyLength, key, iv)`. `native-libs.md` (a product of teammates) has given the same direction conclusion and 24 constant candidates, which can be merged.

---

## 8. Two model files

### 8.1 `assets/rec_model_20230425.mnn` —— MNN（`Observed`）

- 810,908 B, entropy 7.504, header `1c000000 18001c00 08000000 000c0010 …` → `u32 rootOffset = 28`, consistent with **FlatBuffers**; `Net` table of MNN is flatbuffers.
- Printable symbols in the model (`Observed`, a total of 10,951 printable runs, the first 120 after deduplication):

```
sparse_inputs_1 … sparse_inputs_6 | sparse_inputs_time
embedding_0.w_0 … embedding_8.tmp_0 | Const3/Const19/Const33/Const47/Const61/Const75/Const90
transpose_0..5.tmp_0 | squeeze_0..17.tmp_0 | unsqueeze2_1..17.tmp_0
p2o.Add.1/3/5/7/9/11/13/15/17/19 | p2o.MaxPool.1/3/5/7/9/11 | p2o.MatMul.1/3/5/7/9
p2o.Concat.1 | p2o.helper.constant.6 | reshape2_0.tmp_0
relu_0..9.tmp_0 | linear_0..4.w_0 / .b_0 | linear_9.tmp_1 (output)
```

- Completely corresponding to the Java side (`ia/j.java`):
  - `ia/j.java:88,92,105,109` — model path `<files>/apprecmodel/checkpoint_N` or `<files>/rec_model_20230425.mnn`, if missing, `ia/d.d(resources,"rec_model_20230425.mnn", path)` is copied from assets (`ia/d.java:71-182`).
  - `ia/j.java:116` `MNNNetNative.nativeCreateNetFromFile`; `:131` `nativeCreateSession(..., FORWARD_CPU, 4, ...)` (4 threads).
  - `:138-144` input tensor name = `sparse_inputs_1..6` + `sparse_inputs_time`; `:202-209` shape `{batch, 30}` and `{batch,1}`.
  - `:224-233` input content: package name character id of the first 5 apps (`ia/d.f`), the 6th candidate, `int[30]` time embedding = `Calendar.get(HOUR_OF_DAY)` filling; `:262` output tensor `linear_9.tmp_1`; `:283-294` do softmax for every 2 logit and take index 1 → **second category "whether it will be opened/whether it is a positive sample"**.
  - `:451,470,523` `MNNTrainNative.nativeGetRecModelPredictScore` / `nativeTrainRecModel` → **end-side incremental training** (sample <50, give up directly, `:372`).
- Conclusion: **App recommends/preloads the sorting model**, inputs the package name character sequence + hours, outputs a two-category score, and supports end-side fine-tuning.

### 8.2 `assets/model` - unknown format (`Inferred` + `Unknown`)

- 680,154 B, entropy 7.178. Header (int32BE): `1, 0, 5, 68, 58`; followed by uint16BE sequence `13, 36, 96, 123, 125, 183, 924, 927, 934, 955, 1052, 1054, 1057, 8214 …`.
- The top byte histogram `0xC0` appears 62,465 times (9.2%) → a large number of negative float32 values ​​(`0xC0xxxxxx` ≈ `-2.0`~`-4.0`).
- Split by 4 bytes after offset 76: float32BE falls on `[-6,0]` accounting for 36.8% of ** ** (170,019 windows), float32LE only 18.4% → big-endian weight (`Inferred`).
- 883 ASCII runs (≥6 characters) can be printed, but the content is meaningless (`")\".\"f$h$`, `%m%n%o%p%`...) → unsigned table/no layer name (`Observed`).
- **is opened by**: there is only the `{"dict","model","pattern","phish"}` name array of `lj/c.java:17`, and this array is only declared in the entire project **and has never been read by** (`Observed`, grep `f29439b` is only defined in 1 place). Generic `lj/c.b(context,name)` will open by name (assets or internal files).
- Therefore: the format, dimensions, and uses are all `Unknown`; `Inferred` is the **word vector or weight matrix** of the SMS/phishing text classifier (same package `lj` as `dict`/`pattern`/`phish`). `phish` resource **does not exist in assets** (it is unknown whether it is downloaded at runtime).

---

## 9. `assets/pattern` - SMS/call identification rules (plain text, UTF-8, non-strict JSON)

- 5,568 bytes, entropy 4.879, UTF-8 plaintext (**modified baseline: not GBK**).
- Structure: `{"version":"1.2","features":[ … ]}`, but the key **is not quoted** (`{pattern:["^12520"]}`, `sms_type:1`, `neg_pattern:[…]`), so the standard JSON parser will report an error - indicating that the parser is custom/relaxed (`Inferred`).
- **reads chain**: `lj/c.java:22-40 a(Context)` → `bufferedReader` reads in `StringBuilder` line by line (line by line `+ "\n"`) → for compilation by the rule engine.
- Solved feature names (`Observed`): `Fei_xin` (`abstract:"Start with 12520"`, `address.pattern:["^12520"]`), `sms_body_length` (`body_len:["0-15"]`), `bracket_title_not_personal_address` (4 `【…】` regular rules, including `[\u4E00-\u9FFF|a-z|0-9|，。｜ | \.]{2,12}`), `important_info_body` (`neg_pattern:["恭喜.*幸运.*奖品"]` + 6 items `or_pattern`: last number/card number/verification code/password/redemption code/express delivery number), `important_addr` (`address.type:[1,5]`), `important_info` (`combi:["bracket_title_not_personal_address&important_inf…"]`).
- Purpose (`Inferred`): Determine whether the text message is a "notification type/containing important information", thereby deciding whether to display it as an important notification/not to intercept it.

---

## 10. `assets/warningcenter/` - Disaster warning icon (71 photos)

### 10.1 File matrix (`Observed`, script `wc3.js`)

71 files, 481,089 B. Name `warningcenter_<code>_<color>.{png|webp}`:

| code | Number of colors | Color | Format |
|---|---|---|---|
| 11A01 / 11A02 / 11A51 | 4 | blue yellow orange red | webp |
| 11B01 / 11B03 / 11B04 / 11B05 / 11B06 / 11B17 | 4 | blue yellow orange red | png |
| 11B07 / 11B09 / 11B14 / 11B19 / 11B21 | 3 | yellow orange red (**no blue**) | png |
| 11B15 | 3 | blue orange red (**no yellow**) | png |
| 11B16 | 3 | blue yellow orange (**no red**) | png |
| 11B22 | 2 | orange red | png |
| 11D01 / 11D02 | 4 | blue yellow orange red | webp |
| fire | 4 | blue yellow orange red | webp (**is shared by 11G01 and 11G05**) |

> The lack of color is not the lack of files: **Each disaster type only provides the icon** on its "possible warning level" (inference, for example, `11B22` only has orange/red).

### 10.2 The list and icons are completely self-consistent (`Observed`, script `wc4.js` / `wc5.js`)

- The 21 codes of `warning_center_disaster_icon` have a total of **75** `code→filename` mappings, and **75 all point to the existing file** (`MISSING 0`).
- There are 71 files in the `warningcenter/` directory, reverse check: **does not have any "file exists but JSON is not listed" orphan file in**.
- The difference comes from **reusing**: `warningcenter_fire_{blue,yellow,orange,red}.webp` is referenced by `11G01` and `11G05` at the same time:
  ```json
  "11G01": {"blue":"warningcenter_fire_blue.webp","yellow":"warningcenter_fire_yellow.webp","orange":"warningcenter_fire_orange.webp","red":"warningcenter_fire_red.webp"}
  "11G05": { …identical… }
  ```
  → 71 files + 4 duplicate references = 75 mappings ✓
- Another inconsistency observed: `11B16` only has blue/yellow/orange (**has no red**), `11B22` has only orange/red, `11B07/09/14/19/21` has no blue - the text length of the corresponding colors in `warning_center_disaster_guide_text` for these "deficient colors" happens to be 0 (see §10.3), the two corroborate each other.

### 10.3 Complete code → Disaster type mapping (`Observed`)

Source: `com/miui/warningcenter/disasterwarning/model/EventType.java:10-34` (enumeration) + `com/miui/warningcenter/disasterwarning/Utils.java:26-78` (two `Map<String,Integer>`: `ICON_LIST` → drawable, `TITLE_LIST` → string resource).

| code | EventType enumeration name | Chinese (read by R.string name) | local assets icon | `guide_text` has text |
|---|---|---|---|---|
| `11B01` | Typhoon | **Typhoon** `warningcenter_disaster_taifeng` | 4 color png | ✅Blue/Yellow/Orange/Red |
| `11B03` | Rainstorm | **Heavy rain** `…_baoyu` | 4 colors | ✅ All |
| `11B04` | Snowstorm | **Blizzard** `…_baoxue` | 4 colors | ✅ All (Text reads "Prevent Snow and Frost Damage") |
| `11B05` | ColdWave | **cold wave** `…_hanchao` | 4 colors | ✅ All |
| `11B06` | Hurricane | **Gale** `…_dafeng` | 4 colors | ✅ All |
| `11B07` | —— (**is not in the enumeration**) | —— | Yellow/orange/red (no blue) | ✅ Yellow/Orange/Red (Text: "Prevent Sandstorm") |
| `11B09` | Hot | **High temperature** `…_gaowen` | yellow/orange/red | ✅Yellow/orange/red ("heatstroke prevention and cooling") |
| `11B14` | —— (not in enumeration) | —— | yellow/orange/red | ❌ None |
| `11B15` | Hailstone | **hail** `…_bingbao` | Blue/orange/red (no yellow) | ✅ Orange/Red |
| `11B16` | —— (not in enumeration) | —— | Blue/yellow/orange (no red) | ❌ None |
| `11B17` | Fog | **heavy fog** `…_dawu` (drawable name `…_wu`) | 4 colors | ✅ All |
| `11B19` | Haze | **haze** `…_mai` | yellow/orange/red | ✅Yellow/Orange (red is empty) |
| `11B20` | Thunderstorm | **Thunderstorm and strong wind** `…_leiyudafeng` | ❌ None (use drawable `…_others`) | ❌ |
| `11B21` | FrozenRoad | **Icy roads** `…_daolujiebing` | yellow/orange/red | ✅ All ("Ice Preparedness") |
| `11B22` | —— (not in enumeration) | —— | orange/red | ❌ |
| `11B25` | ForestFire | **Forest fire insurance** `…_senlinhuoxian` | ❌（`…_others`） | ❌ |
| `11B29` | Pollution | **Heavy pollution** `…_zhongwuran` | ❌ | ❌ |
| `11B37` | Disaster | **Geological disasters and meteorological risks** `…_dizhizaihaiqixiangfengxian` | ❌（`…_others`） | ❌ |
| `11A01` | Flood | **flood** `…_hongshui` | 4 color webp | ❌ |
| `11A02` | WaterLogging | **waterlogging** `…_neilao` (drawable `…_jishui`) | 4 color webp | ❌ |
| `11A51` | FlashFlood | **flash flood** `…_shanhong` | 4 color webp | ❌ |
| `11C02` | Earthquake | **earthquake** `…_dizhen` | ❌ | ❌ |
| `11D01` | LandSlide | **landslide** `…_huapo` | 4 color webp | ❌ |
| `11D02` | Mudslide | **Debris flow** `…_nishiliu` | 4 color webp | ❌ |
| `11D99` | (Only in `Utils`, mapped to others) | **Others** `…_qita` | ❌ | ❌ |
| `11E02` | WindStorm | **Storm surge** `…_fengbaochao` | ❌（`…_others`） | ❌ |
| `11E06` | Wave | **Wave** `…_hailang` | ❌ | ❌ |
| `11G01` | ForestFireWithinTerritory | **Forest fire** `…_senlinhuozai` | **`warningcenter_fire_*` (4 colors webp) ** | ❌ |
| `11G05` | GrasslandFireWithinTerritory | **grassland fire** `…_caoyuanhuozai` | **`warningcenter_fire_*` (shared) ** | ❌ |
| (no code) | Other | Others | ❌ | ❌ |

→ This completely explains the two "seemingly inconsistent" places: `fire` is not an "orphan file", but an icon of the `11G` class (forest/prairie fire); `11G01`/`11G05` is not a "missing file", but is shared with `fire`. **(This section corrects the wrong judgment in the author’s first draft.)**

**`warning_center_disaster_guide_text` Character length of each text (`Observed`)** - length 0 strictly corresponds to the lack of color of the icon:

| code | blue | Yellow | Orange | red | | code | blue | Yellow | Orange | red |
|---|---|---|---|---|---|---|---|---|---|---|
| 11B01 | 122 | 148 | 214 | 203 | | 11B15 | 0 | 0 | 128 | 124 |
| 11B03 | 100 | 143 | 127 | 74 | | 11B17 | 0 | 83 | 77 | 141 |
| 11B04 | 155 | 153 | 121 | 95 | | 11B19 | 0 | 58 | 93 | **0** |
| 11B05 | 67 | 87 | 81 | 85 | | 11B21 | 0 | 67 | 64 | 71 |
| 11B06 | 170 | 189 | 186 | 179 | | 11B07 | 0 | 148 | 133 | 106 |
| 11B09 | 0 | 101 | 142 | 82 | | | | | | |

(Contrast §10.1: 11B22 only has orange/red and no guide entry; 11B19’s red is empty and the icon has no blue or red. Please note the corresponding relationship - 11B19 has red icon and empty red text, indicating incomplete data, `Observed`.)

### 10.4 Alert level = bit mask (`Observed`)
`Utils.java:19`: `public static final int[] LEVEL = {8, 4, 2, 1};` - consistent with `LEVEL_RED_COUNT=8 / ORANGE=4 / YELLOW=2 / BLUE=1` of `DisasterConstants`; `Utils.getRegistLevel()` (`Utils.java:99-123`) uses `LEVEL[i] & pushLevel` to find the **subscription level bits and**, `getStrongPushLevel()` default `8` (red only), `getSystemPushLevel()` default `12` (red + orange). → 1/2/4/8 is a bit mask and can be combined (`Observed`+`Inferred`).

### 10.5 Two sets of icon systems (`Observed`)

| System | location | Who uses |
|---|---|---|
| **resource icon** | `res/drawable/warningcenter_disaster_icon_{taifeng,baoyu,…,huozai,others}` | `Utils.ICON_LIST` → Use `getIconRes()` in lists/notifications |
| **assets icon** | `assets/warningcenter/warningcenter_<code|fire>_<color>.webp|png` | `WarningCenterDisasterDetailActivity.java:341` reads `assets/warning_center_disaster_icon` and gets the file name → `:370` `getAssets().open("warningcenter/" + name)` → `Drawable.createFromStream` |

Guide text: `WarningCenterDisasterDetailActivity.java:105` `assetManager.open("warning_center_disaster_guide_text")`.

### 10.6 Associated interface (`Observed`)

`DisasterConstants.java` also gives:
- `REQUEST_CITYCODE_URL = "https://api.sec.miui.com/12379/warning/getRegionInfo"` (12379 = National Emergency Warning Information Release System)
- `REQUEST_CITYCODE_SALT = "7war5230-a1cf-3k79-ec73-75cb82145n5x"`
- `UUID_DISASTER_CODE = "0279a99a-80f9-42ec-8945-bfe7d5070ed6"`
- `DISASTER_PUBKEY` = RSA-2048 SPKI (base64): `MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApRu6Zd52/9k2NSgeGUIeSg8rIUtj64REDEBW+wNEnhYiebc9BSUcViBe3rYJvKH9hAXyiVKSVq9V2p/wweXX6QcJgu7P9I2ajyqZ23ChpLG1I3vBl4yBjv1r9+uCz819jZGxzhD4jLwKKLeAbd+cM3mdLwhCwI/5b56VFWrkKpcQQhtrxB8FU4n/wzqSgo38NyerBa7KhcUsCrJxt4Y7sIQ9u+fJAFvN7jpKn+9ey6C9hibBJunnAQj/rCcyC+fcUlOGj2EEoSp+S0s+Rd7gIWYdxl7uAX9P0SIc+ZUGslVjEEvOjpxNcUq/x6aXt6YJt+bZGVXs4FuS/YqGDIOiywIDAQAB` → Used to verify cloud warning data (`Inferred`).

---

## 11. `res/raw` and `res/xml`

### 11.1 `res/raw` (all **38** items, `Observed`)

| Category | File | Notes |
|---|---|---|
| **SQLite rule base** | `parse.db` (94,208 B, ZIP is named `res/rKy.db`, both SHA-256 are identical = `5b1da601c11b20eaf53da468a02ae8fda5fe91a5d048ac557301d03ee88767f1`) | See §6, the only built-in plaintext rule table |
| **certificate** | `wlan_speed.crt` (912 B, ZIP named `res/Dmp.crt`), `wlan_speed_new.crt` (3,090 B, ZIP named `res/xIB.crt`) | See §11.3 |
| OpenGL shader | `fragment.glsl`、`imagefragment.glsl`、`imagevertex.glsl`、`vertex.glsl` | animation rendering |
| video | `antivirus_animation1/2.mp4`、`security_scan_video.mp4`、`firstaidkit_scan_video.mp4`、`optimizemanage_video.mp4`、`ew_guide_video.mp4`、`pc_kit_printer.mp4`、`pp_top_logo_video.mp4`、`securty_protect_top_video.mp4`、`wireless_reverse_charging.mp4`、`icon_wild_model_video.mp4` | Boot/scan animation |
| Audio | `one..ten.mp3` (digital), `di.mp3`, `didi.mp3`, `wu.mp3`, `non.mp3`, `lucky_money.mp3`, `hongbao_arrived.wav`, `mijia_push_alert_p0.ogg`, `number_picker_value_change.ogg` | Sound effects |
| GIF | `vtb_video_effect_fps_before_cn.gif`、`vtb_video_effect_fps_after_cn.gif` | Video toolbox before and after comparison |

> **in `res/raw` does not have** and other `.db` / `.json` / rule table / keystore.

### 11.2 `res/xml` (120 pieces, `Observed`)

All are **Preference/Layout/FilePath/NetworkSecurity** class XML, no rule base. The only thing related to "trust" is:

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
(`base-config cleartextTrafficPermitted="true"` = Globally allowed plaintext HTTP, `Observed`.)

Others worth noting: `file_paths.xml`, `na_filepaths.xml`, `xiaomi_update_sdk_file_paths.xml` (FileProvider path), `priority_storage.xml`, `ew_*.xml` (earthquake early warning), `warning_center_*.xml` (early warning center).

### 11.3 Two built-in certificates (`Observed`, Node `X509Certificate` analysis)

| item | `res/raw/wlan_speed.crt`（ZIP: `res/Dmp.crt`） | `res/raw/wlan_speed_new.crt`（ZIP: `res/xIB.crt`） |
|---|---|---|
| DER length | 632 B | 2,202 B |
| subject | `C=CN, ST=ShenZhen, L=ShenZhen, O=XiaoMi, OU=XiaoMi, CN=www.miwifi.com` | `C=CN, ST=BeiJing, L=BeiJing, O=XIAOMI, OU=MIWIFI ROOT CA, CN=MIWIFI ROOT CA` |
| issuer | Same as subject (self-signed) | `C=CN, ST=Beijing, L=Beijing, O=Xiaomi Inc., OU=Xiaomi CA, CN=Xiaomi Root CA` |
| Validity period | 2020-06-28 → 2030-06-26 | 2020-10-28 → 2120-10-04 |
| serial number | `E53763287569BE1F` | `32060BF01DCEEE20EBFCBD9F35C9AAE5B3FF8629` |
| SHA-256 fingerprint | `A9:9B:E9:57:3E:E1:A1:4E:5D:B3:3F:46:FE:95:02:D1:09:15:E8:3F:E9:DC:FF:87:7D:05:53:81:38:F5:8D:BB` | `76:C7:E3:44:D6:BB:19:36:50:05:A4:C7:23:44:C2:EE:F6:B2:49:E8:06:7C:4B:2C:54:6E:75:0E:0F:FC:7E:52` |
| public key | RSA **1024**，e=65537 | RSA **2048**，e=65537 |
| CA | false | **true** |

Purpose: `u6/e.java:183` uses these two certificates as trust anchors when accessing `https://miwifi.com/cgi-bin/luci/api/xqsystem/maccel?type=1&mode=1` (Xiaomi router speed test) (`Observed`: network configuration + the URL literal; `Inferred`: supporting use).

---

## 12. `META-INF/` and signature

### 12.1 Directory contents (`Observed`)

| File | size | content |
|---|---|---|
| `MANIFEST.MF` | 789,745 | Standard JAR manifest, mark `SHA-256-Digest` for each entry (example: `AndroidManifest.xml → sgsh002rVeL2w+MuUI3bY0kXFBlETSEOQv+SexNxzjU=`) |
| `PLATFORM.SF` | 789,872 | v1 signature file (`Signature-Version` block) |
| `PLATFORM.RSA` | 1,642 | v1 PKCS#7 SignedData block |
| `com/android/build/gradle/app-metadata.properties` | 56 | `appMetadataVersion=1.1` / `androidGradlePluginVersion=7.3.1` |
| `services/kotlinx.coroutines.internal.r` | 5 | Text `nk.a\n` (Kotlin coroutine's `MainDispatcherFactory` service declaration, obfuscated name) |
| `services/mk.e0` | 5 | Text `nk.b\n` (same as above, implemented by `Delay`) |

> `services/*` is the **Kotlin coroutine SPI registers**, not a third-party service; the name is confused by R8.

### 12.2 Signature scheme (`Observed`, REA `ev_774c…`)

| Plan | exist | Signer/certificate | Summary |
|---|---|---|---|
| v1 (JAR) | ✅ | `PLATFORM.SF` + `PLATFORM.RSA`; the same MIUI certificate | `SHA-256-Digest` (OID 2.16.840.1.101.3.4.2.1), signature algorithm OID 1.2.840.113549.1.1.1 (RSA) |
| v2 | ✅ | Same as above | `sha256`，declared `7f809feb864bb03b73460e6edbb092cd32af8be620c8d85c4bfb6c29100403e8`，**matches_apk = true** |
| v3 | ✅ | Same as above; `min_sdk 24`, `max_sdk 2147483647`; **without key rotation lineage** | Ditto abstract, `matches_apk = true` |
| v3.1 | ❌ | — | — |
| Source Stamp | ❌ | — | — |

Certificate (single signer, the three schemes are consistent):
```
subject/issuer : C=CN, ST=Beijing, L=Beijing, O=Xiaomi, OU=MIUI, CN=MIUI, emailAddress=[VENDOR_EMAIL_REMOVED]
serial         : E552A8ECB9011B7C
validity       : 2011-12-06T03:26:26Z → 2039-04-23T03:26:26Z
public key: RSA (292 B SPKI field)
SHA-256 Fingerprint: c9009d01ebf9f5d0302bc71b2fe9aa9a47a432bba17308a3111b75d7b2149025
```
`posture`：`strongest_scheme = v3`，`schemes_agree_on_signer = true`，`self_consistent = true`，`v1_only = false`。
> Description: This is the MIUI **platform signature** (`O=Xiaomi, OU=MIUI`), indicating that it is the ROM preset system application. REA clearly states that **does not perform cryptographic verification of the signature** (only compares the summary of the statement with the current byte).

---

## 13. Full list of assets (248 files, by category)

> For complete file-by-file details, see `_analysis/tools/assets_summary.tsv` (including automatic extraction of type/structure/scale). The following table summarizes the classification of **and**. In the "Reader" column, only **has been located for the class/method of**, and `Unknown` has not been located.

### 13.1 Top-level single file (no extension) - rules/models/key classes

| File | Bytes | Type | Size/Content | reader |
|---|---|---|---|---|
| `dict` | 3,639,702 | Custom binary (jieba) | **348,982 words + 35,224 word list**, HMM parameters | `lj/g.java:32` → `mj/a.h()` |
| `model` | 680,154 | Custom binary (**unknown**) | Head int32 `1,0,5,68,58` + float32 weight | Only `lj/c.java:17` name array; **has no actual reader** |
| `censor_source` | 2,688 | Encrypted binary (entropy 7.933) | Sensitive lexicon (unsolved) | `liblib-riskcontrol.so` `SensitiveWordDetector::load` |
| `rsapubkey` | 419 | **Java serialization** `java.security.KeyRep` | `aced0005 7372 0014 6a6176612e73656375726974792e4b6579526570…` → RSA public key | `Unknown` (KeyRep deserialization) |
| `pattern` | 5,568 | UTF-8 Loose JSON | `version 1.2`, ≥6 feature rule groups | `lj/c.java:22 a(Context)` |
| `placement_id` | 39 | plain text | `123=fb:196567740764356_196569107430886` (Facebook ad slot) | `Unknown` |
| `financial_package_top` | 7,180 | JSON array | **300** financial package names | `Unknown` |
| `photography_package_top` | 7,196 | JSON array | **300** image package names | `Unknown` |
| `safepay_target_package` | 13,076 | JSON array | **211** `{package,state}` (payment security scan target) | `Unknown` |
| `safepay_target_activity` | 1,376 | JSON array | **7** `{package,type,activity[]}` | `Unknown` |
| `safepay_imm_whitelist` | 383 | JSON array | **13** input method package names (payment keyboard whitelist) | `Unknown` |
| `safepay_calling_activity` | 236 | JSON array | **4** payment activation activity | `Unknown` |
| `anti_fraud_pay_activity` | 637 | JSON array | **3** `{package,activity[]}` (anti-fraud transfer page) | `Unknown` |
| `xunyou_support` | 283 | UTF-8 text (line separated) | **20** game names that support "Xunyou Acceleration" (Glory of Kings/Peace Elite/Genshin Impact...) | `Unknown` |
| `vtb_default_support_list` | 6,541 | UTF-8 text (line separated) | **324** package names (Video Toolbox whitelist) | `Unknown` |
| `quickreply_default_list` | 6,526 | JSON array | **263** package names (quick reply/in-game reply) | `f7/b.java:299` |
| `top_200_games.json` | 19,926 | JSON `{packageNames:[…]}` | **607** game package names | `Unknown` |
| `privacyapps_top.json` | 8,091 | JSON array | **4 group** (type 0:100 / 1:20 / 2:85 / 4:29 package names) | `Unknown` |
| `white_url.json` | 8,259 | JSON `{content:[…]}` | **452** domain name/IP whitelist | `m2/e.java:24` |
| `service_num.json` | 4,470 | JSON `{content:[…]}` | **224** customer service/service numbers | `m2/e.java:55` |
| `address.json` | 21,531 | UTF-8 JSON array | **34** Province / **336** City + Area Code (`{"n":"北京","c":"10","l":[…]}`) | `m2/f.java:416,455` |
| `character.json` | 600 | Plain text JSON | **40** characters→id | `ea/c.java:21` |
| `behavior_record_white.csv` | 3,928 | uppercase hex line | **57** row; row length distribution `{32:3, 64:47, 96:4, 128:3}` hex → 16/32/48/64 bytes, that is, **MD5/SHA-256/SHA-384/SHA-512 mixed length summary whitelist** | `mc/d.java:128` (`!IS_INTERNATIONAL_BUILD` branch) |
| `global_behavior_record_white.csv` | 3,115 | Same as above | **43** OK; `{32:2, 64:29, 96:12}` | `mc/d.java:128` (international version branch) |
| `night_charge_back_list.json` | 9 | JSON | `[""]` (empty place) | `Unknown` |
| `tvm_whitelist.json` | 3,221 | JSON | **4** `{package_name, signature, tui_apps[{sha256}]}` (TEE/TUI trusted UI whitelist) | `fh/c.java:112` |
| `scanitem.json` | 16,462 | JSON | `{manual:28, system:8, version:8}` (one-click physical examination scan item) | `Unknown` |
| `firstaid_scanitem.json` | 4,923 | JSON | Group 5: `Performance:3, Internet:3, Operation:5, ConsumePower:2, Other:3` (first aid kit) | `Unknown` |
| `warning_center_disaster_icon` | 5,363 | JSON (with trailing comma) | **21** disaster codes → 75 4-color image name mappings (all hit real files; `fire` is shared by 11G01/11G05) | `com/miui/warningcenter/disasterwarning/WarningCenterDisasterDetailActivity.java:341` |
| `warning_center_disaster_guide_text` | 14,041 | JSON (with trailing comma) | **11** disaster codes → Full text of 4-color defense guide | `…WarningCenterDisasterDetailActivity.java:105` |
| `garbage_clean.db` | 36,864 | SQLite3 | 3 tables, t_cache 4 rows | **None (no references in this APK)** |
| `pinyinindex.idf` | 54,199 | Custom IDF table | **20,902** Chinese characters → Pinyin | `am/a.java:57` → `cl/a.java` |
| `rec_model_20230425.mnn` | 810,908 | MNN FlatBuffers | See §8.1 for layer diagram | `ia/j.java:88` → `MNNNetNative` |
| `protobuf/antispam.proto` | 2,319 | protobuf IDL (clear text) | `package antispam; option optimize_for = LITE_RUNTIME` | Server/client serialization |
| `protobuf/syncroot.proto` | 252 | protobuf IDL | `import "antispam.proto"; package syncroot;` | Same as above |

### 13.2 `*_support_activity_list.json` family ("Video/call window" white list, all plain text JSON array)

| File | Number of items | File | Number of items |
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

The element structure is unified as `{"pkgName":"com.tencent.mm","activityName":"com.tencent.mm.plugin.voip…"}`.

### 13.3 Lottie animation JSON (`{v,fr,ip,op,w,h,nm,ddd,…}`, 30 in total, all in clear text)

`dock_settings_*.json`(8)、`normal-light/dark*.json`(4)、`unfold-*.json`(4)、`popNotice*.json`(4)、`externalDark/Light.json`、`flip_wireless_*.json`、`phone_wireless_*.json`、`n1_wireless_charge.json`、`privacy_camera/privacycamera_*.json`、`img/privacy_apps_lottie*.json`(4)、`performance_mode/data.json`、`redmi_performance_mode/Ai-Model.json`、`game_turbo_box_mode_change/{red,blue}_light.json`、`shoulder_key/{enter,settings,using}_guide.json`、`tb_guide_style1.json`。
(Lottie version field `v` = `5.6.6` / `5.7.14` / `5.8.1`.)

### 13.4 Pictures/Fonts/Compressed Packages

| group | Quantity | Notes |
|---|---|---|
| `warningcenter/` | 71 webp/png | See §10,481,089 B |
| `power_images/` | 8 webp | Power saving mode illustration |
| `performance_mode/images/`、`redmi_performance_mode/images/` | 8 + 7 png | Performance mode illustration. **6 pictures are exactly the same (dislocation one-to-one correspondence)**: `performance_mode/images/img_2..7` ≡ `redmi_performance_mode/images/img_1..6` (SHA-256 are the same one by one); only the first picture is different (`performance_mode/images/img_0.png` 4,806 B vs `redmi_performance_mode/images/img_0.png` 22,601 B) |
| `shoulder_key/images/` | 8（4 webp + 2 png + 2 webp） | Shoulder key guidance |
| `img/` | 4 png + 4 lottie json | App Lock/Game Acceleration/Uninstall/Auto-Launch Icon + Privacy Lottie |
| `images/img_0.png` | 1 | 18,275 B |
| `maml/` | 4 sets × 4-5 files (png + `manifest.xml`) | MAML card: `antispam/{dark,light}/{call,sms}`, `na_nosim(_dark)` |
| `fonts/` | 7 otf + 2 ttf | MiType series (`Mitype.otf`, etc., CFF/OTTO) + `Roboto-BoldCondensed.ttf` + `TitilliumWeb-SemiBold.ttf` |
| `dexopt/` | `baseline.prof`(1,272) / `baseline.profm`(137) | ART baseline profile（magic `pro\0 0 1 0` / `prm\0 0 2 0`） |
| `dock_tips.zip` | 210,751 | ZIP, ~7 entries |
| `shoulder_key/guide_{next,single,trigger}.zip` | 87,432 / 103,850 / 104,928 | ZIP, ~13 entries each |

### 13.5 `maml/` card (`Observed`)

`maml/antispam/{dark,light}/{call,sms}/` and `maml/na_nosim(_dark)/` each contain `manifest.xml` + `bg.png` / `bgmask.png` / `board.png` / `shape.png`.
- 4 `manifest.xml` are almost the same size (4,198–4,199 B) → same template, only theme/scene parameters are different (`Inferred`).
- Purpose: MAML card resource **of** incoming call/text message interception interface (which belongs to the same harassment interception link as `protobuf/antispam.proto` and `pattern`).

### 13.6 General asset opening point (facilitates subsequent positioning)

The call point of `getAssets().open(` (`Observed`, jadx full grep):

| location | Notes |
|---|---|
| `a8/g.java:13`, `a8/y.java:103` (read by line) | Universal asset reading tool |
| `am/a.java:57` | `pinyinindex.idf` |
| `ea/c.java:21` | `character.json` |
| `f7/b.java:299` | `quickreply_default_list` |
| `fh/c.java:112` | `tvm_whitelist.json` |
| `h1/b.java:91` | Picture (`BitmapFactory.decodeStream` + prefix splicing) |
| `i4/a.java:147`、`ia/d.java:80`、`oi/d.java:18`、`w2/t.java:439`、`x4/e.java:13`、`o8/k.java:666` | **opens** according to the variable name (mostly used for Lottie/pictures/models) |
| `lj/c.java:51` | **assets / internal files dual source** (`dict`/`model`/`pattern`/`phish`) |
| `m2/e.java:24/55`、`m2/f.java:416/455` | `white_url.json` / `service_num.json` / `address.json` |
| `mc/d.java:128` | `behavior_record_white.csv` or `global_…` (press `Build.IS_INTERNATIONAL_BUILD`) |
| `xh/a.java:354` | Open via URI scheme `assets://` |
| `com/miui/warningcenter/disasterwarning/WarningCenterDisasterDetailActivity.java:105` | `assetManager.open("warning_center_disaster_guide_text")` |
| `com/miui/warningcenter/disasterwarning/WarningCenterDisasterDetailActivity.java:341` | `getAssets().open("warning_center_disaster_icon")` |
| `com/miui/warningcenter/disasterwarning/WarningCenterDisasterDetailActivity.java:370` | `getAssets().open("warningcenter/" + asString)` → `Drawable.createFromStream` (**The only reading point of 71 warning icons is**) |

> **methodology reminds**: PowerShell's `Select-String -Path "dir\**\*.java"` **will not recurse** and only match one level of directories. The author's first round of grep therefore missed the deep path files under `com/miui/**` (such as `EventType.java`, `SensitiveWordDetectProvider.java`, `WarningCenterDisasterDetailActivity.java`). Corrected conclusions in §7, §10 after using `Get-ChildItem -Recurse -Filter *.java | Select-String` (10,624 .java) instead. Subsequent members should not reuse the `**` wildcard writing method.

---

## 14. Built-in database/data file list (one-page quick check)

| File | Type | Number/scale | Purpose | Read its class/method | Plain text? |
|---|---|---|---|---|---|
| `assets/garbage_clean.db` | SQLite3 | 3 tables; `t_cache` 4 rows, `t_ad`/`t_residual` **0 row** | Garbage cleaning rules (skeleton + sample) | **None (DEX has no such string)** | ❌ Obfuscation (hex, stream cipher characteristics) |
| `res/raw/parse.db` (ZIP `res/rKy.db`) | SQLite3 | `DIR_PARSE` **1043 lines/884 package**; `TEMP_DIR` 10 lines | Application cache directory → package name/Chinese and English name mapping, used for deep cleaning | `ra/e.java`（`R.raw.parse` → `getDatabasePath`；`g()/f()/c()`） | ✅ Clear text |
| `assets/dict` | custom binary | **348,982 words + 35,224 words**, totalFreq 60,101,983 | Chinese word segmentation (jieba) | `lj/g.java:32` → `mj/a.h()` | ✅ Structural plaintext (binary encoding) |
| `assets/pinyinindex.idf` | Custom "IDF " v2 | 20,902 Chinese characters / 1,336 de-emphasized pinyin strings | Chinese characters → Pinyin (initial letter/polyphonetic characters) | `am/a.java:57` → `cl/a.java` | ✅ Structural plaintext (binary encoding) |
| `assets/character.json` | JSON | 40 items | character→id vocabulary | `ea/c.java:21` | ✅ |
| `assets/pattern` | Loose JSON | v1.2, ≥6 sets of regular rules | SMS Importance/Phishing Determination | `lj/c.java:22` | ✅ |
| `assets/model` | custom binary | 680,154 B | Unknown (inference: text classification weights) | No actual readers | ❓ Binary, unknown format |
| `assets/rec_model_20230425.mnn` | MNN FlatBuffers | 810,908 B，embedding×9 + linear×5 | App prediction/preload sorting + client-side training | `ia/j.java` → `MNNNetNative`/`MNNTrainNative` | ❓ Binary (structure readable) |
| `assets/censor_source` | encrypted binary | 2,688 B | Sensitive lexicon | `liblib-riskcontrol.so` `SensitiveWordDetector::load` | ❌ Unsolved |
| `assets/rsapubkey` | Java serialization KeyRep | 419 B | RSA public key | Unknown | ✅ The structure is readable |
| `assets/white_url.json` | JSON | 452 items | URL whitelist | `m2/e.java:24` | ✅ |
| `assets/service_num.json` | JSON | 224 items | Service number database | `m2/e.java:55` | ✅ |
| `assets/address.json` | JSON | 34 provinces/336 cities | place of belonging | `m2/f.java:416/455` | ✅ |
| `assets/tvm_whitelist.json` | JSON | 4 items | TEE/TUI whitelist | `fh/c.java:112` | ✅ |
| `assets/behavior_record_white*.csv` | CSV(hex) | 57 / 43 lines | Behavior record whitelist (by signature) | `mc/d.java:128` | ✅ |
| `assets/safepay_*` | JSON | 211/7/13/4 | Payment Security Scan Targets | Unknown | ✅ |
| `assets/{financial,photography}_package_top` | JSON | 300 / 300 | Category list | Unknown | ✅ |
| `assets/warning_center_disaster_{icon,guide_text}` | JSON | 21 / 11 items | Disaster Icons & Defense Guide | `WarningCenterDisasterDetailActivity.java:341 / :105` | ✅ |
| `assets/warningcenter/` | pictures | 71 photos / 481,089 B | Disaster warning icon (`fire` = 11G01/11G05) | `WarningCenterDisasterDetailActivity.java:370` | — |
| `res/raw/wlan_speed{,_new}.crt` | X.509 PEM | 632 / 2,202 B DER | miwifi.com Trust Anchor | `network_security_config.xml` | — |
| `assets/protobuf/*.proto` | IDL | 2 | Intercept backup/sync schema | Generate code | ✅ |
| `assets/placement_id` | text | 1 line | Facebook ad space | Unknown | ✅ |

---

## 15. Unsolved mysteries (in order of priority)

1. AES key/IV**(high) for**`assets/censor_source`. Known: `SensitiveWordDetectProvider` (ContentProvider, whitelist only `com.android.camera` / `com.miui.mediaeditor`) only passes `AssetManager` to native, **does not pass the key** → the key is built into `liblib-riskcontrol.so`; AES is self-implemented by ECB/CBC/PKCS5. Excluded: **, any continuous 16/32 byte window as key** (490,000 candidates × ECB/CBC, PKCS5+full text verification 0 passed), single byte XOR, password derived key, deflate/gzip/brotli. **is missing**: aarch64 static disassembly (native Ghidra/Hopper/IDA/Kuna/rizin/objdump are all unavailable). **Next step**: Linux + Ghidra/rizin analysis `_ZN21SensitiveWordDetector4loadEPc` → chase `AES::AES(AESKeyLength, key, iv)` actual parameters; or real machine hook `AES::KeyExpansion`. It is in the same direction as the conclusion of teammate `native-libs.md` §2.6 and can be merged.
2. Reader for **`assets/garbage_clean.db` with obfuscation algorithm** (high). **full recursive grep (10,624 .java) + three dex/18 .so byte scan**, `garbage_clean.db` / `t_cache` / `t_residual` have no references → There is no reader in this APK. Either consumed by an external component such as `com.miui.cleanmaster`, or deprecated. It is recommended to file as "dead assets"; if required by Lead, the next step can be to search for the DB with the same name in the `com.miui.cleanmaster`/OTA package.
3. The format and usage of **`assets/model`** (middle). Header `1,0,5,68,58` + float32 weight; `{"dict","model","pattern","phish"}` array **of `lj/c.java:17` has never been read** (full grep has only 1 definition), and the `phish` resource does not exist → The entire set of `lj` logic may have been abandoned or loaded by the plug-in.
4. ~~Disaster name of `11A01/11A02/11A51/11B14/11B16/11B22/11D01/11D02/11G01/11G05/fire`~~ → **has solved** (`EventType.java` 25 enumerations + `Utils.ICON_LIST/TITLE_LIST`; `fire` = 11G01 forest fire / 11G05 grassland fire shared icon). **legacy**: `11B07` (sandstorm), `11B14`, `11B16`, `11B22` These four codes **are not in the `EventType` enumeration**, only in the assets icon and `guide_text` Appears in → Description **The new disaster type** issued by the cloud has prepared an icon in the resource in advance, but the client enumeration has not yet been synchronized (`Inferred`); its specification name needs to be checked in the 12379 standard or server configuration.
5. Complete rules engine **(low) for**`assets/pattern`. Only the JSON structure and 6 groups of features are solved; the evaluator of the `combi` expression, the meaning of the value of `sms_type`, and the semantics of `address.type` 1..6 need to read the corresponding Java parser (outside the scope of this task).
6. The cloud delivers protocol **(low) from**`res/rKy.db`→`parse.db`. `ra/e.java:148 c(list,json)` is the overwrite entry, and the caller/interface is not pursued.
7. The guide text red for **`11B19` (haze) is empty for** (low). There is a red version of the icon but the guide lacks red copy - data is incomplete, not missing in design (`Observed`).

---

## 16. Reproduction command (verified on this machine)

```powershell
# Fingerprints for all assets (size, SHA-256, entropy, magic bytes)
node _analysis\tools\scan_assets.js _analysis\apk_full\assets > _analysis\tools\assets_scan.tsv

# Identify the type and structure of each file
node _analysis\tools\summarize_assets.js > _analysis\tools\assets_summary.tsv

# Parse the full jieba dictionary
node _analysis\tools\parse_dict.js

# Parse pinyinindex.idf in full (IDF v2)
node _analysis\tools\pinyin2.js

# Analyze garbage_clean.db row counts, contents, and duplicate blocks
node _analysis\tools\db_count.js ; node _analysis\tools\final1.js

# Inspect all of parse.db
node _analysis\tools\dump_parse.js

# Brute-force attempts on censor_source (all failed)
node _analysis\tools\aesbrute.js ; node _analysis\tools\censor_attack.js
node _analysis\tools\aes_pad.js   # Full window and preliminary PKCS5 filter
node _analysis\tools\aes_pad2.js  # Restrict to pad>=2 and validate the full text (0 passed)

# Warning Center icon matrix, manifest consistency, and code reuse
node _analysis\tools\wc3.js ; node _analysis\tools\wc4.js ; node _analysis\tools\wc5.js

# Certificates
node _analysis\tools\crt2.js
```

REA side: `inspect_sqlite_database` (garbage_clean.db / res/rKy.db), `inspect_apk_signature`. **Note**: REA In this machine (Windows) **cannot** open aarch64 ELF (`open_binary` returns `target_format_unsupported`: `Windows Ghidra P0 accepts native PE and explicitly selected raw 6502 targets only`), so `.so` Static disassembly is not done.
