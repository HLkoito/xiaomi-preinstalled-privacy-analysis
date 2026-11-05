> **归档警示（已纠错）**：这是原分析材料的脱敏归档，不能代替 [最终复核报告](../REPORT.md)。原文关于“明文 IMEI 列表”“默认配置即实际上传”、绝对覆盖率及未知 native 行为的部分结论不成立。IMEI/IMSI 的相关 DAU 列表经过 MD5，仍受区域/权限/联网门控。证书过期也不能单独推出密文无法解密。请先阅读最终报告与其中纠错表。本文删除本地身份路径，原有文件行号引用可能指未纳入发布的完整分析树。

# Native 层（`.so` + `assets` 载荷 + JNI 边界）

> 分析者：`t-native`　｜　目标：`GuardProvider-2.0.3-20231124.0.apk`
> SHA-256 `b0c02fa682b8ca82429b471113d0702042ac76234c85fdcd8efeb7c03d313024`
> 素材根：`_analysis\apktool\lib\arm64-v8a\`（12 个 `.so`）、`_analysis\apktool\assets\`（8 个文件）
> 标注约定：**〔观测〕**=字节/符号/源码直接可见；**〔推断〕**=有依据的语义归纳；**〔未知〕**=本轮未覆盖。
>
> **工具限制（重要，已在 §7-U0 记录）**：REA 的 Ghidra provider 在 win32 上只接受 **PE / 显式 raw 6502**，拒绝 ELF-arm64；
> Hopper 仅 macOS；IDA/Kuna 未配置。因此 `binary_overview` / `list_procedures` / `analyze_function` / 反编译在本机**不可用**。
> 改用：① REA 免引擎工具 `inspect_native_exports`（4 次，含 evidence_id）；② 自写 ELF64/ELF32 解析器（PowerShell/.NET）；
> ③ 自写 AArch64 `ADRP+ADD` 指令解码器，机械还原「函数 → 引用字符串」；④ 全库 Latin1 正则字符串取证。
> 本报告所有 ARM 级结论均来自 ③④，不是反编译产物。

---

## 1. 职责概述

〔观测〕本 APK 的 native 层由 **三个互不相关的技术栈** 拼装而成，彼此无 `DT_NEEDED` 交叉依赖，只在 Java 层被组合：

| 栈 | 提供方 | 库 | 职责 |
|---|---|---|---|
| **A. 腾讯 AVL 引擎** | 腾讯（`com.avl.engine.*`） | `libavlasys.so`(JNI 壳) + `libavlm.so`(引擎) + `libavlurl.so`(URL 引擎) + `liburldetectorsys.so`(URL JNI 壳) + `assets/avlsdk` | 本地病毒扫描 / APK 解析 / 证书摘要 / URL 信誉判定 |
| **B. 腾讯 TMSDK 2.0.13** | 腾讯（`tmsdk*`） | `libTmsdk-2.0.13-mfr.so` + `libams-1.3.0-mfr.so` + `libdce-1.1.21-mfr.so` + `libturingvd.so` + `assets/70003.amf` + `assets/40706.dat` + `assets/mdetector` + `assets/licence*.conf` | 字符串加解密（TccCryptor）、AMF 恶意特征匹配、深度清理（DCE）、Turing 设备风控、网络探测 |
| **C. 国密 + 小米自研 + xcrash** | 腾讯 / 小米 / iqiyi | `libtmf-gm.so`(SM2/SM4) + `libflamingo.so`(Rust 正则引擎) + `libxcrash*.so`(native crash) | 国密算法、Rust 多模式匹配、native 崩溃 tombstone |

〔观测〕**12 个 `.so` 全部为 `ELF64 / little-endian / AARCH64 / ET_DYN`，全部 stripped**（无 `.symtab`，只有 `.dynsym`）。
全部带 `PT_GNU_RELRO`；仅 `libflamingo.so` 与 `libturingvd.so` 置 `DT_FLAGS=BIND_NOW`（其余 lazy binding）。
全部 PIE（`ET_DYN`），无 `DF_TEXTREL`。**没有任何一个库被整体加壳**（节名均为标准 GNU 节名，`.text/.rodata/.data.rel.ro/.eh_frame` 齐全，无 `upx`/`jiagu`/自定义段）。

〔观测〕`libxcrash_dumper.so` 是**唯一带 `PT_INTERP`（`/system/bin/linker`）与 `.preinit_array` 的库**，
实质是 fork/exec 型**独立可执行体**，不是普通共享库（见 §2.2.12）。

---

## 2. 组件与文件清单

### 2.1 `.so` 全量总表（12/12，无遗漏）

| # | 文件 | 大小 | SHA-256 | SONAME | 类型/架构 | stripped | JNI 入口形态 | 一句话定性 |
|---|---|---|---|---|---|---|---|---|
| 1 | `libams-1.3.0-mfr.so` | 744,392 | `2FE3276BF1FE734AA6847F2E3778CD726FF971615BD675CEAA729751F4914BEF` | `libams-1.3.0-mfr.so` | ELF64 AArch64 ET_DYN | 是 | **8 个 `Java_tmsdk_..._AmScannerV2_*`**（直接导出） | TMSDK AMF 恶意特征扫描器（配套 `70003.amf`） |
| 2 | `libavlasys.so` | 89,144 | `8FEC8CF73748963141206CA29CD0FC15D54FF43129AF573ED9E5EAB6BFE12FF0` | `libavlasys.so` | 同上 | 是 | **无 `Java_*`**；`JNI_OnLoad`@0x105fc + RegisterNatives | AVL SDK 的 JNI 壳（注册 `AVLA`+`NativeUtils`） |
| 3 | `libavlm.so` | 2,304,352 | `65EC01AE59795DC1C17DDCE8B1BAA2EE01DC911C176A336F42432ECA5BAD7688` | `libavlm.so` | 同上 | 是 | **无**（纯 C API `AVLM_*`，98 个导出） | AVL 本地扫描引擎本体 |
| 4 | `libavlurl.so` | 629,976 | `54DFE75A6F6154D787B969E17CF752D32EA337D294F84ACB2F3F72EF3B2BEC20` | **`liburl.so`**（≠文件名） | 同上 | 是 | **无**（纯 C API `AVLM_Match_*`，33 个导出） | AVL URL 信誉引擎 |
| 5 | `libdce-1.1.21-mfr.so` | 202,976 | `6670CA20A1D60FFC22A9276D8232400CC5E53EFAF6037C78B49DBDBB2CDABE95` | `libdce-1.1.21-mfr.so` | 同上 | 是 | **无 `Java_*`**；`JNI_OnLoad`@0x8920 + RegisterNatives | TMSDK DeepCleanEngine 深度清理 |
| 6 | `libflamingo.so` | 1,506,088 | `BEE0D6ECF1E24AB282CD014958DC23A0675958021A979E2F8CFB9BDD4C0371B9` | **空**（无 SONAME） | 同上 | 是 | **3 个 `Java_com_miui_..._RustSupport_*`** | 小米自研 Rust 正则/多模式匹配引擎 |
| 7 | `libtmf-gm.so` | 177,920 | `D27A1A6B63F6453904FFDAC0147FCD8D062871256AB26E9DA69FC6981F82C956` | `libtmf-gm.so` | 同上 | 是 | **8 个 `Java_com_tencent_tmf_gm_{Sm2,Sm4}_*`** | 国密 SM2/SM4 JNI 实现 |
| 8 | `libTmsdk-2.0.13-mfr.so` | 370,088 | `7DA1A5FEE5CD45B0C9834D47F346E45F7B1DCC247B1DE7BE1F199999F033EAF2` | **`libTmsdk-2.0.12-mfr.so`**（版本不符） | 同上 | 是 | **无 `Java_*`**；`JNI_OnLoad`@0x1f480 + RegisterNatives（仅 5 个导出） | TMSDK 核心（TccCryptor / 号码归属地 / 黄页 / 差分包） |
| 9 | `libturingvd.so` | 240,448 | `E4068BF300A633A3840783969EA715A806A988188BEFBF32B17CF56D414966A6` | `libturingvd.so` | 同上 | 是 | **无 `Java_*`**；`JNI_OnLoad`@0x1c05c + RegisterNatives | 腾讯 Turing 设备风控 SDK |
| 10 | `liburldetectorsys.so` | 14,408 | `E22142D0D2591A63BC804FAC00906AAE3B6826FAC7C9D9C7D2EEBE726FE3A4F8` | `liburldetectorsys.so` | 同上 | 是 | **无 `Java_*`**；`JNI_OnLoad`@0x1db4 + RegisterNatives | `UrlDetector` 的极薄 dlopen 转发壳 |
| 11 | `libxcrash.so` | 88,360 | `BE831E703CA4D597C531C9E43FEA40210C4E53070F3FE8C1F1F843E11E3D0CCA` | `libxcrash.so` | 同上 | 是 | **无 `Java_*`**；`JNI_OnLoad`@0x328c + RegisterNatives | xcrash native 崩溃捕获主库 |
| 12 | `libxcrash_dumper.so` | 129,344 | `0CB766583D1ACFC3254817F305721DD13467571BC024ADD2053B33D7B56D3A25` | **空** + **有 `PT_INTERP`** | 同上 | 是 | 无 | xcrash 独立 dumper **可执行体**（ptrace 抓 tombstone） |

> 〔观测〕任务清单里写的 **`libtmg-gm.so` 在 APK 中不存在**；`lib/arm64-v8a/` 下只有 `libtmf-gm.so`（177,920 B）。见 §7-U1。
> 〔观测〕`lib/arm64-v8a/` 下**只有这 12 个文件**（`Get-ChildItem -File` 穷举，无子目录）。

---

### 2.2 每个 `.so` 的独立身份表

#### 2.2.1 `libams-1.3.0-mfr.so` — TMSDK AMF 扫描器

| 项 | 值 |
|---|---|
| ELF | `ELF64` LE `AARCH64` `ET_DYN`（PIE），entry `0xb380` |
| `DT_SONAME` | `libams-1.3.0-mfr.so` |
| `DT_NEEDED` | `libz.so, libdl.so, libstdc++.so, libm.so, libc.so` |
| 节 | 22 节，含 `.gcc_except_table`、`.data.rel.ro`；`.text` 0x81fbc、`.rodata` 0x4d78 |
| build-id | **无**（无 `PT_NOTE`） |
| `.comment` | 0x25 B（NDK clang，短格式） |
| 导出 | 130 dynsym / 12 个全局 |
| **JNI 导出（8）** | `Java_tmsdk_common_module_qscanner_impl_AmScannerV2_initScanner`@0xd3c8<br>`..._newObject`@0xf000　`..._deleteObject`@0xd528<br>`..._scanApkBytes`@0x1bde0 (768B)　`..._extractApkInfo`@0x1c0e0<br>`..._getOpcode`@0x1dbd4 (3488B，最大)<br>`..._nativeLoadAmfHeaderBytes`@0x1c514　`..._nativeUpdateMalwareInfoBytes`@0x21080 |
| 其它导出 | `_Znwm`/`_Znam`（operator new）、`VERSION` OBJECT、`_edata/_end/_bss_end__` |
| 加壳/混淆 | **无壳**。但 `.comment` 极短、无 build-id；**特征库字符串被加密**（见 AMF） |
| 关键字符串 | `.rodata`@0x8db48：`amf` `.amf` `AMF` `MalwareData` `PackageName` `CertMd5` `ApkSize` `ClassPrefix` `ClassSuffix` `ZipEntry` `ApkVersion` `MinClassSuffix` `ConstantString` + 两个可疑常量 `!@#$%^&*()_JHGFD`、`SfJ(*W#&Ddweir309*&@#!_)($39`；`tmsdk/common/TMSDKContext` `checkLisence ()Z` |
| 能力 | `dlopen/dlsym/dlclose`、`mmap/mprotect`、`zlib inflate/deflate`、`sigaction/sigsetjmp`（自身崩溃保护）、`dl_iterate_phdr` |

〔推断〕`!@#$%^&*()_JHGFD` / `SfJ(*W#&Ddweir309*&@#!_)($39` 是 AMF 容器的**表项名混淆密钥**（string-table XOR key），
因为它们紧邻 `AMF.MalwareData` 等表项名存放，且长度/字符集符合 TMSDK 一贯的「可打印 ASCII 做 XOR key」风格。

#### 2.2.2 `libavlasys.so` — AVL JNI 壳

| 项 | 值 |
|---|---|
| ELF | `ELF64` LE `AARCH64` `ET_DYN`，entry `0x2c00` |
| SONAME / NEEDED | `libavlasys.so` / `liblog.so, libz.so, libandroid.so, libc.so, libm.so, libstdc++.so, libdl.so` |
| build-id | `1b6e69c2a46c537b6d8bd5a7ba7448fd7206d537` |
| `.comment` | 0x64 B |
| 导出 | 87 dynsym，**仅 3 个真实函数**：`JNI_OnLoad`@0x105fc(204B)、`AVL_SetScanLimitEx`@0xe004、`AVL_SetMapleScanLimit`@0xdfec |
| **JNI 形态** | **RegisterNatives**（无 `Java_*`）。〔观测〕用自建 ADRP+ADD 解码器扫描 `.text`(0x2c00/0xdd10) 得 **316 处字符串引用**，其中 `JNI_OnLoad` 直接引用 **恰好 2 条**：`com/avl/engine/security/NativeUtils`、`com/avl/engine/security/AVLA` → **机证：本库就是这两个类的 natives 实现** |
| 关键字符串（.rodata @0x10ef0 起连成一串，即 JNINativeMethod 表） | `./conf/liscense.conf`、`./avl.avl`、`/avl/conf/avllib.conf`、`assets/avllib`、`arm64-v8a`、`dlopen error: %s`、`so name is NULL`、`key is overdue!`、`AVL SDK licence is overdue!`、`%s is test key.`、`key is test key!`、`AVLSDK WARN`、`INIT_FAIL`、**`62BAFD662A63A99A7C96DA027AFDF739`** |
| 能力 | `AAssetManager_*`（读 assets）、`dlopen/dlsym`、`regcomp/regexec`（正则）、`socket`、`ioctl`、`mmap`、`__android_log_print` |

#### 2.2.3 `libavlm.so` — AVL 扫描引擎本体

| 项 | 值 |
|---|---|
| ELF | `ELF64` LE `AARCH64` `ET_DYN`，entry `0x3f390` |
| SONAME / NEEDED | `libavlm.so` / `libz.so, libc.so, libm.so, libstdc++.so, libdl.so`（**无 liblog**，用 `__assert2`/`printf`） |
| build-id | `dd31d0086fa153d701381d6671a92fa1a2ba6d79` |
| `.comment` | `Android clang version 3.8.256229 (based on LLVM 3.8.256229)` → **2016 年 NDK r11 系**〔观测〕 |
| 导出 | **98 个**（REA `inspect_native_exports` 复核：`examined_entries 248, exports 98, coverage complete`） |
| **JNI** | **无任何 `Java_*`**；由 `libavlasys.so` `dlopen`+`dlsym` 驱动 |
| 反 hook 设计〔观测〕 | 多个 `AVLM_*` 导出 `size=4`（跳板 stub），并**额外导出 10 个无意义 4 字母别名**：`aesd`@0xb14f8 `adce`@0xb14f4 `effe`@0xb1508 `efff`@0xb1504 `fffe`@0xb1500 `exdz`@0xb14fc `wtsr`@0xb15d0 `haoz`@0xb15d4 `zavt`@0xb157c(84B) `brmy`@0xb1514(104B)。<br>〔推断〕同一实现挂「正式名 + 随机别名」两份导出，使按 `AVLM_*` 下钩的 hook 失效 |
| `.data` 中的数据符号 | `sigx_app`@0x2596e0 (8B, OBJECT)、`sig_app`@0x2596e8 (8B, OBJECT) |
| 字符串加密特征〔观测〕 | **字符串被打包成连续 blob**：`.rodata` 出现 `http://.https://.Content-Length:.www.baidu.com.GET`、`...SMS_Send.SMS_Block.System_DexLoad.System_ReflectInvoke.System_ReflectGet.Notification...` —— 以 `.`(0x00/0x01 分隔) 串联的定长表，是典型「字符串表 + 长度索引」混淆 |
| 明文域名（见 §5.1） | `www.baidu.com`、`0..3.north-america.pool.ntp.org`、`www.wapforum.org`、`www.w3.org`、`openssl.cn`、`www.openssl.org` |
| 网络能力〔观测 import〕 | `socket connect send sendto recv recvfrom gethostbyname inet_addr inet_ntoa inet_ntop select poll shutdown getsockopt`（**具备完整出网能力**） |
| 反调试相关 import | **无 `ptrace`**；有 `syscall`、`getuid`、`getpid`、`gettid`、`readlink`、`tcgetattr/tcsetattr`、`getenv` → 〔推断〕用于环境探测，不含 ptrace 反调试 |

#### 2.2.4 `libavlurl.so` — AVL URL 信誉引擎

| 项 | 值 |
|---|---|
| ELF / SONAME | `ELF64` LE `AARCH64` `ET_DYN`，entry `0x325d0`；**`DT_SONAME = liburl.so`（与文件名 `libavlurl.so` 不一致）** |
| NEEDED | `libz.so, libstdc++.so, libm.so, libc.so, libdl.so` |
| build-id | **无** |
| 导出 | **33 个**（REA 复核：`examined_entries 139, exports 33`），全部 `AVLM_*` |
| **JNI** | **无**；由 `liburldetectorsys.so` dlopen `liburl.so` 后 dlsym |
| 关键字符串 | `liburl.so`（SONAME）、`%s.new`、`%stmp/`、`%s.tmp/`、`%s/conf/avllib.conf`、`V([0-9]+) \(Build ([0-9]+)\)`、`HTTP/1.1`、`http://%s` |
| 明文域名 | **400 条**（去重后），但**全部是公共后缀/免费托管域名白名单**，不是 C2（见 §5.2） |
| 网络能力 | `socket connect send sendto recv recvfrom gethostbyname inet_addr inet_ntoa inet_ntop select getsockopt setsockopt` |

#### 2.2.5 `libdce-1.1.21-mfr.so` — 深度清理引擎

| 项 | 值 |
|---|---|
| ELF / SONAME | `ELF64` LE `AARCH64` `ET_DYN`，entry `0x7c60`；`libdce-1.1.21-mfr.so` |
| NEEDED | `liblog.so, libstdc++.so, libm.so, libc.so, libdl.so` |
| build-id | 无 |
| 导出 | **329 个**，但 **327 个是静态链入的 libstdc++ 符号**（`_ZTVSt12out_of_range`、`_Unwind_*`、`__gxx_personality_v0`、`_ZTS*` 等）；真实自有导出只有 **`JNI_OnLoad`@0x8920** |
| **JNI 形态** | RegisterNatives。〔观测〕`.rodata`@0x27158 注册表明文：类 `tmsdk/common/tcc/QFile`、`tmsdk/common/tcc/QSdcardScanner`、`tmsdk/common/tcc/SdcardScannerFactory`、`tmsdk/common/tcc/DeepCleanEngine` |
| 方法签名表（观测原文） | `nativeSetWhiteList (J[Ljava/lang/String;)V`、`nativeSetRule (JI[Ljava/lang/String;[Ljava/lang/String;)V`、`nativeInit`、`nativeRelease`、`nativeCancle`、`nativeScan (JLjava/lang/String;)V`、`nativeSetProgressListenLevel (JI)V`、`nativeDeleteAllChildren (Ljava/lang/String;)I`、`nativeDeleteAllChildrenByDay (Ljava/lang/String;I)I`、`nativeList (Ljava/lang/String;)[Ltmsdk/common/tcc/QFile;`、`nativeFillExtraInfo (Ljava/lang/String;)V`、`nativeRemoveEmptyDir`、`nativeAllocate (IJ)J`；`DeepCleanEngine`: `create (I)J` `setWhitePaths` `setRootPaths` `setOtherFilterRule` `scanPath (JLjava/lang/String;...)`；回调 `onFound (ILjava/lang/String;IJJJJ)V`、`onProgressChanger (Ljava/lang/String;)Z` |
| 内部类名 | `12InnerScanner` `16DeepInnerScanner` `16WideInnerScanner` `11BaseVisitor` `13NormalVisitor` `10PathFilter` `15WhitePathFilter` `16ComRubbishFilter` `17SoftRubbishFilter` `14EmptyDirFilter` `13HideDirFilter` `13DirPathFilter`、`nomedia` |
| 能力 | 文件系统递归删除/列举（`nativeDeleteAllChildren*`、`nativeRemoveEmptyDir`）——**这是 12 个库里唯一具备「批量删除任意路径」原语的库** |

#### 2.2.6 `libflamingo.so` — 小米 Rust 匹配引擎

| 项 | 值 |
|---|---|
| ELF / SONAME | `ELF64` LE `AARCH64` `ET_DYN`，**entry `0x0`、无 SONAME**；`DT_FLAGS=BIND_NOW`；仅 `.gnu.hash`（无 SysV `.hash`） |
| NEEDED | **只有 `libdl.so, libc.so`** |
| `.note.android.ident` | `r25b` / `8937393`（NDK r25b） |
| `.comment` | 0xb1 B |
| 导出 | **3 个**（REA 复核 `exports 3`）：<br>`Java_com_miui_guardprovider_natives_RustSupport_matches`@0x9d760 (480B)<br>`Java_com_miui_guardprovider_natives_RustSupport_search`@0x9d940 (664B)<br>`Java_com_miui_guardprovider_natives_RustSupport_search___3B_3Ljava_lang_String_2`@0x9dbd8 (1832B) |
| **重载解析〔观测〕** | 第 3 个符号带 `__<mangled sig>` 后缀，对应 `search(byte[], String...)`；JVM 对重载 native 方法先查带签名后缀的名字，与 `RustSupport` 的两个 `search` 重载**完全吻合** |
| 能力 | 无网络、无 `dlopen`、无 crypto；import 只有 `memcpy/memcmp/memset/malloc/memalign/realloc/calloc/free`、`pthread_*`、`mmap/munmap`、`syscall`、`realpath/getcwd/getenv`、`dl_iterate_phdr`、`getrandom`、`writev`、`__register_atfork` |

〔推断〕`RustSupport.matches(byte[],String)` → 单正则匹配；`search(byte[],String)` → 单模式查找；
`search(byte[],String...)` → Aho-Corasick 多模式批量查找，返回 `boolean[]`。
Java 侧调用点 `p0/y.java:111` `RustSupport.matches(bArr, t0.j.a(stringItem))`、`p0/y.java:117` `RustSupport.search(bArr, stringItem.getValue())`
——即**对文件字节做特征串/正则匹配**，是 MIUI 自研扫描引擎的核心。

#### 2.2.7 `libtmf-gm.so` — 国密 SM2/SM4

| 项 | 值 |
|---|---|
| ELF / SONAME | `ELF64` LE `AARCH64` `ET_DYN`，entry `0xfd0` |
| NEEDED | `liblog.so, libm.so, libstdc++.so, libdl.so, libc.so` |
| build-id | `f6f9519193063732eca0abc789d4a876dc84ad86` |
| `.comment` | `Android clang version 5.0.300080 (based on LLVM 5.0.300080)`〔观测〕 |
| 导出 | 40 dynsym，**8 个 `Java_*` + 9 个 `_ZN7_JNIEnv*` 内联 JNIEnv 包装** |
| JNI 全表 | `Java_com_tencent_tmf_gm_Sm2_doSm2Decrypt`@0x1bd4(512B)<br>`..._Sm2_doSm2Decrypt2`@0x1f54(328B)<br>`..._Sm2_doSm2Encrypt`@0x18fc(728B)<br>`..._Sm2_doSm2Encrypt2`@0x1dd4(384B)<br>`..._Sm4_doSm4EncryptCBC`@0x156c(456B)<br>`..._Sm4_doSm4DecryptCBC`@0x1734(456B)<br>`..._Sm4_doSm4EncryptECB`@0x100c(400B)<br>`..._Sm4_doSm4DecryptECB`@0x13dc(400B) |
| 能力〔观测 import〕 | 仅 `memcmp memset memcpy malloc free strlen rand srand time __stack_chk_fail __assert2`。<br>**无网络、无文件、无 dlopen** → 纯算法库。|
| **风险点〔观测〕** | 导入 **`rand` + `srand` + `time`**：SM2 加密需要随机 k，若用 libc `rand()` 播种 `time()` 则 k 可预测 → 私钥可恢复。见 §6-R3 |

#### 2.2.8 `libTmsdk-2.0.13-mfr.so` — TMSDK 核心

| 项 | 值 |
|---|---|
| ELF / SONAME | `ELF64` LE `AARCH64` `ET_DYN`，entry `0x30d0`；**`DT_SONAME = libTmsdk-2.0.12-mfr.so`**（文件 2.0.13 / soname 2.0.12，版本不一致） |
| NEEDED | `libz.so, liblog.so, libstdc++.so, libm.so, libc.so, libdl.so` |
| build-id | 无 |
| 导出 | **5 个**（REA 复核 `exports 5`）：`JNI_OnLoad`@0x1f480(156B) + `VERSION`(OBJECT) + `_edata/_end/_bss_end__` |
| **JNI 形态** | RegisterNatives。〔观测〕`.rodata`@0x484e0 注册表明文类：`tmsdk/common/tcc/TccCryptor`、`tmsdk/bg/tcc/TelNumberLocator`、`tmsdk/bg/tcc/NumMarker`、`tmsdk/common/tcc/TrafficSmsParser`(+`$MatchRule`)、`tmsdk/common/YellowPages`、`tmsdk/common/TMSDKContext`、`com/tencent/diffupdate/TccDiff` |
| `TccCryptor` 方法〔观测〕 | `makePassword ([B)[B`、`encrypt ([B[B)[B`、`decrypt ([B[B)[B`、`add (Ljava/lang/Object;)Z`、`length ()I`、`replace (IILjava/lang/String;)Ljava/lang/StringBuffer;` |
| 其它 | `TelNumberLocator`: `newObject ()J` `init (JLjava/lang/String;Ljava/lang/String;)I` `patch` `getLocation` `getMobileNumLocation` `getAreaCodeLocation` `getCountryCodeLocation` `getAreaCode (J)[I` `getCountryCode` `deleteObject (J)V` `getYellowPages` `getDetailYellowPages` `getProvinceNameList` `getCityNameList`、`gbk`；`NumMarker`: `nNewInstance (I)J` `nDestroyInstance` `nSetPath (JLjava/lang/String;)Z` `nGetHeaderInfo` |
| 错误串 | `data too large`、`java/lang/RuntimeException`、**`Bad encrypted data`** |
| 能力 | 无 socket、无 dlopen；只有文件 stdio + 时间 + pthread → 纯本地库 |

#### 2.2.9 `libturingvd.so` — 腾讯 Turing 风控

| 项 | 值 |
|---|---|
| ELF / SONAME | `ELF64` LE `AARCH64` `ET_DYN`，entry `0x3860`；`DT_FLAGS=BIND_NOW`；同时有 `.hash` 与 `.gnu.hash` |
| NEEDED | `liblog.so, libandroid.so, libz.so, libm.so, libstdc++.so, libdl.so, libc.so` |
| build-id | `e89e7f60fb3847a24c7ebc98433311dee31070a3` |
| `.note.android.ident` | `r20` / `5594570` |
| 导出 | 112 dynsym，真实导出仅 `JNI_OnLoad`@0x1c05c(416B) |
| **JNI 形态** | RegisterNatives。类名以 `$` 形式出现（DEX 类描述符真的含 `$`）：`com/tencent/turingfd/sdk/vd/TNative$aa`、`.../TNative$aa$bb` |
| 注册的方法（与 Java 声明逐一对齐）〔观测〕 | `(Landroid/util/SparseArray;Landroid/content/Context;Ljava/util/Map;I)Landroid/util/SparseArray;`→`a`<br>`(Landroid/util/SparseArray;[BLjava/util/Map;I)...`→`b`<br>`(Landroid/util/SparseArray;Landroid/content/Context;)...`→`c`<br>`(Landroid/util/SparseArray;Landroid/content/Context;I)...`→`d`<br>`(Landroid/util/SparseArray;Landroid/content/Context;Ljava/util/Map;I)...`→`e`<br>`(Landroid/util/SparseArray;[BI)...`→`f`<br>`(...Context;Map;I)`→`g`/`h`、`(...Context;Map)`→`i`<br>`TNative$aa$bb.onServiceConnected (Landroid/content/ComponentName;Landroid/os/IBinder;)V` |
| 版本串 | `TuringFD v%d (%s, %s, %s, compiled %s)`、`vd.5e7c85b` |
| **风控字符串〔观测〕** | `turingRiskDetect` `riskCheckWup` `pkgVerInfo` `apiLevel` `brand` `model`<br>`/sys/fs/selinux/enforce`（读 SELinux 状态）<br>**内核线程/root 进程正则**：`^/system/xbin/ku[.]sud$|^daemonsu:|^k_worker/[1-9][[:digit:]]*:[[:digit:]]+$|^kr_worker/...|^km_worker/...|^tworker/...|^tu_worker/...|^tq_worker/...|^kworker/[1-9][[:digit:]]{2}$|^permmgrd$|^360sguard$|^/data/data/[[:alnum:]_.-]+/|^/system/bin/[.]|^/system/xbin/[.]` |
| 能力〔观测 import〕 | **`popen`/`pclose`（执行 shell 命令）**、`ASensorManager_getInstance` `ASensorManager_getSensorList` `ASensor_getVendor` `ASensor_getName` `ASensor_getType` `ASensor_getResolution`（**读传感器 → 模拟器指纹**）、`statfs`、`getuid`、`readlink`、`syscall`、`pthread_create`、`uname`? |

#### 2.2.10 `liburldetectorsys.so` — UrlDetector JNI 壳

| 项 | 值 |
|---|---|
| ELF / SONAME | `ELF64` LE `AARCH64` `ET_DYN`，entry `0xe00`；`liburldetectorsys.so` |
| NEEDED | `liblog.so, libc.so, libm.so, libstdc++.so, libdl.so` |
| build-id | `73e529d1ab581f08eec4f44a77cb90806f2db303` |
| 大小/节 | 14,408 B；`.text` 仅 0x1098（4.2 KB）——**全库最小的功能库** |
| 导出 | 26 dynsym，真实函数仅 `JNI_OnLoad`@0x1db4(228B) |
| **JNI 全表〔观测，.rodata@0x213a 明文〕** | 类 `com/avl/engine/urldetector/UrlDetector`；方法签名 `setLibPath (Ljava/lang/String;)I`、`loadURL`、`unLoadURL ()V`、`matchURL (Ljava/lang/String;Z)I`、`matchURLEx (Ljava/lang/String;Z)[Ljava/lang/String;`；错误串 `class %s  not found!` |
| 行为〔观测〕 | import 只有 `dlopen dlsym dlerror dlclose strdup sprintf strlen memcpy memset free __android_log_print` → **纯粹的 dlopen 转发层**，自身不含 URL 算法 |
| 加载目标〔观测〕 | 字符串 `liburl.so` 且 `.text` 内 `dlopen` 后的 dlsym 名字来自 `AVLM_MatchWithType`/`AVLM_GetSIDInfo`/`AVLM_FreeSIDInfo` —— 即运行时 dlopen **`liburl.so`** |

#### 2.2.11 `libxcrash.so` — native 崩溃捕获主库

| 项 | 值 |
|---|---|
| ELF / SONAME | `ELF64` LE `AARCH64` `ET_DYN`，entry `0x3250`；`libxcrash.so` |
| NEEDED | `libdl.so, liblog.so, libc.so, libm.so, libstdc++.so` |
| build-id | `80a6cbf5457853174f24ffbc0d47b823dd7d0b55` |
| `.note.android.ident` | `r16b` / `4479499` |
| 导出 | 128 dynsym，自有真实导出仅 `JNI_OnLoad`@0x328c(224B) + 一组 `xc_test_crash`@0xbf88 / `xc_test_call_1..4`（自测用）+ 静态 libgcc unwind（`_Unwind_*`、`__register_frame*`） |
| **JNI 全表〔观测，.rodata@0xf980〕** | 类 `xcrash/NativeHandler`；`nativeInit (ILjava/lang/String;×10;ZZIIIZZZZZI[Ljava/lang/String;ZZIIIZZ)I`、`nativeNotifyJavaCrashed ()V`、`nativeTestCrash (I)V`、`nativeTraceSignalInit`、`crashCallback (Ljava/lang/String;Ljava/lang/String;ZZLjava/lang/String;)V`（另有 `traceCallback`）|
| 关键字符串 | `%s/libxcrash_dumper.so`、`%s/tombstone_%020lu_%s__%s%s`、`.native.xcrash` `.trace.xcrash` `.clean.xcrash` `.placeholder_`、`/dev/null`、`xcrash_crash_cb`、`set dumpable failed, errno=%d`、`set traceable failed, errno=%d`、`fork failed`、`waitpid failed`、`child terminated by a signal(%d)` |
| 能力〔观测 import〕 | `clone execl pipe2 dup2 prctl waitpid sigaltstack eventfd popen pclose` → **fork/exec 子进程 + prctl(PR_SET_DUMPABLE/PR_SET_PTRACER)** |

#### 2.2.12 `libxcrash_dumper.so` — 独立 dumper 可执行体

| 项 | 值 |
|---|---|
| ELF / SONAME | `ELF64` LE `AARCH64` `ET_DYN`，entry `0x22e0`；**无 SONAME**；**有 `.interp` = `/system/bin/linker`（0x15 B）**、`.preinit_array` |
| NEEDED | `libdl.so, liblog.so, libc.so, libm.so, libstdc++.so` |
| build-id | `352d6e6855884b554dd9a7072bce423d8f72c51e`；`.note.android.ident` = r16b / 4479499 |
| 导出 | 81 dynsym；真实：`XZ_SIG`(OBJECT,6B)、`CrcUpdateT4`@0xec08、`CrcUpdateT8`@0xecf8、`g_CrcTable`(OBJECT,8192B)、`__PREINIT_ARRAY__/__INIT_ARRAY__/__FINI_ARRAY__` |
| 能力〔观测 import〕 | **`ptrace`**、`__libc_init`、`alarm`、`waitpid`、`exit`、`regcomp/regexec`、`/proc/%d/maps`、`dl_iterate_phdr`、`dladdr` |
| 附加载荷 | 静态链入 **7-Zip LZMA/XZ SDK**（`XZ_SIG`、`CrcUpdateT4/T8`、`g_CrcTable`）〔推断〕用于压缩 tombstone |
| 定性 | **这是一个 ptrace 型独立可执行文件**，由 `libxcrash.so` 通过 `%s/libxcrash_dumper.so` 定位后 `execl` 启动，attach 回父进程抓 tombstone |

---

### 2.3 每个 asset 的独立鉴定表（8/8，无遗漏）

#### 2.3.1 `assets/avlsdk` — AVL 特征库 ZIP（**明文容器**）

| 项 | 值 |
|---|---|
| 大小 | 1,566,355 B |
| SHA-256 | `287370A5AE11E83A535115C289DF1ED42E6C06A9E86C55E3A6CB6DECE100C84B` |
| 魔数〔观测〕 | `50 4B 03 04` = **`PK\x03\x04`（标准 ZIP）** |
| 结构〔观测〕 | 标准 ZIP，`[System.IO.Compression.ZipFile]` 成功打开，**26 个 entry**，全部 deflate（method 8） |
| 熵 | 8.0（ZIP 内部已压缩所致） |
| 可见字符串 | `url/url/white_re.avls`、`url/url/conf/avllib.conf320`、`url/url/conf/liscense.conf`、`url/url/fish_re.avl`、`url/url/fish_url.avl` |
| 配套 | `libavlasys.so`（`AAssetManager_open("avlsdk")` + `unzipAssetsFileEfficient`）、`libavlm.so`、`libavlurl.so` |

**26 个 entry 全表〔观测〕**

| entry | 压缩/原始 | 语义〔推断〕 |
|---|---|---|
| `sdk_conf/version.conf` | 77 / 106 | **明文 JSON**，见下 |
| `sdk_conf/sdk.conf` | 10,384 / 10,379 | 自定义容器：ASCII 头 `00000` + 二进制段 + base64 段（含签名） |
| `av/avl/conf/avllib.conf` | 13 / 11 | 明文 `20220928.ah` |
| `av/avl/conf/liscense.conf` | 409 / **404** | 密文（见 §4.3.2） |
| `url/url/conf/avllib.conf` | 13 / 11 | 明文 `20180917.zz` |
| `url/url/conf/liscense.conf` | 409 / **404** | 密文（与上同尺寸，内容不同） |
| `av/avl/android/avlpk_sfmf.avl` | 81,484 / 81,459 | 特征包（sfmf） |
| `av/avl/android/avlpk_pornware.avl` | **322,489 / 322,455** | 色情类特征包（最大） |
| `av/avl/android/avlpk_apn.avl` | 131,971 / 131,926 | APN/资费类 |
| `av/avl/android/avlpk_amc.avl` | 54,404 / 54,384 | AMC 特征 |
| `av/avl/android/avlpk_sgnl.avl` | 6,254 / 6,249 | 信号类 |
| `av/avl/android/avlpk_kw.avl` | 4,599 / 4,594 | 关键字特征 |
| `av/avl/android/avlpk_dhc.avl` | 2,960 / 2,955 | DHC |
| `av/avl/android/6947e41c_60f53057.avl` | 5,073 / 5,068 | 匿名特征包（双哈希命名） |
| `av/avl/android/avlpk_des.avl` | 143 / **151** | **描述/索引**，非压缩（raw 151 > comp 151） |
| `av/kw/common/{en,zh}/behavior.des` | 258/477、330/711 | **明文 JSON** 行为描述 |
| `av/kw/common/{en,zh}/common.des` | 329/689、426/1212 | **明文 JSON** 通用描述 |
| `av/kw/common/{en,zh}/recommend.des` | 105/259、108/274 | **明文 JSON** 建议描述 |
| `url/url/fish_url.avl` | 417,486 / 417,356 | 钓鱼 URL 库 |
| `url/url/white.avl` | 370,431 / 370,316 | URL 白名单 |
| `url/url/fish.avl` | 150,818 / 150,768 | 钓鱼域名库 |
| `url/url/fish_re.avl` | 545 / 540 | 钓鱼**正则**库 |
| `url/url/white_re.avl` | 95 / 100 | 白名单**正则**库 |

**`sdk_conf/version.conf` 完整明文〔观测〕**
```
{"sdk.conf":"1","av.siglib":"20220928.ah","av.engine":"351","url.siglib":"20180917.zz","url.engine":"124"}
```
→ **AV 引擎 351 版 / 特征库 2022-09-28；URL 引擎 124 版 / URL 库 2018-09-17**（URL 库落后约 4 年，见 §6-R5）。

**`sdk_conf/sdk.conf` 结构（10,379 B，**修正 lead 的初判**）〔观测〕**

整体熵 7.979，但**前 380 字节（0x000–0x17B）是纯 ASCII 文本**，不是密文：
```
0x000  "3030303030"                       10 B  文本（版本/魔数标记，即 lead 说的「00000」）
0x00A  "f5f245c0c3df05ca6bc0e6a"          ~23 B 文本（hex 摘要，约 11–12 B 的 hex 转写）
0x021  base64 文本，5 行 / ~341 字符       → 标准字母表（含 + / =），解码得 253 B 二进制
          QcLdyKqxfzZ6hUiLUcrKt1CgvXsnc1yMna7ryoJzYbhW2axcwm496MiqsZXqUfeOPBXIgSVfa
          R7MwYKwr+3DgBz38JOXmaEStCD+SS/k/FfQeo18iGdC5a5IwIgdnVVhrWy8SDmjlHgzicbY4HA6n
          arr3CMxKy1WAR9gwTHDwavBBonEPNS0U0Bxn+zGMi5v9lSqZ5IFN63kaAKnSy4U59Nf/sHGh8bcw
          rZb2s8AV4k+Ve+RLE3KKXNGH7Z3n9K4RknQ7ctv31zij6yTteXnl6F423ZhIn7xJixpRs8JUnIzb
          IjZ4zYLy4i+VkQ1PlbIFnlaszjiTfEaQUsOnhw==
0x17A  "@"                                  1 B 文本（分隔符）
0x17C  高熵二进制，9,999 B（到 0x288B=10379 结束）
          0x17C: a2 ed e2 9a 43 4f b9 f7 12 2d a8 d8 57 76 7a b8 14 19 49 7c 8c a2 2a 9d ...
```
base64 解码结果（253 B）前 48 字节：`41c2ddc8aab17f367a85488b51cacab750a0bd7b27735c8c9daeebca827361b856d9ac5cc26e3de8c8aab195ea51f78e`
——高熵、非文本，〔推断〕为签名/证书块或加密的密钥块。

**这解释了 lead 用 AES 试解 offset 0/5/16/32 为什么全部 "not a complete block"**：那 4 个偏移都落在**base64 文本区**里，不是密文。
**真正的密文起点是 `0x17C`（380）**，长度 `9,999 B`，而 **9,999 不是 16 的倍数**（9999/16=624.9）→ 该段**不是整块 AES/SM4-CBC/ECB**，
〔推断〕是流式密码、或带尾部 nonce/tag 的分组模式、或 `[u32 len][密文]` 的非整块布局。**AES-128 `wcw1xbplx7aijwqr` 在此段上不成立**（长度本身就否掉了纯 CBC/ECB）。

〔推断〕容器语义 = `"00000"` 版本头 + hex 摘要 + base64（签名/公钥）+ `@` + 加密配置体。
〔未知〕9,999 B 密文的算法与密钥——见 §7-U10（含新的、可验证的尝试计划）。

**`av/kw/common/zh/common.des` 完整明文〔观测〕**（UTF-8 JSON，`\uXXXX` 转义）
```json
{"before":"该应用可能存在","middle":"的行为，建议","end":"。","or":"或",
 "des":{"malicious":"该应用会泄露您的隐私或者造成恶意扣费等情况，请立即清理。",
        "risk":"该应用可能会泄露您的隐私或者造成恶意扣费等情况，请谨慎使用。",
        "PayWare":"该应用包含支付插件，可能会进行支付类操作，建议谨慎使用。",
        "AdWare":"该应用包含广告插件，可能会影响您的使用体验并造成流量损耗，建议谨慎使用。",
        "AvTest":"EICAR 反病毒引擎测试文件，没有恶意性。",
        "NotVir":"该应用被多次投诉，可能存在风险，请谨慎使用."}}
```

**`.avl` 私有容器格式〔观测，由 `white_re.avl`(100B) / `fish_re.avl`(540B) / `avlpk_des.avl`(151B) 三例归纳〕**
```
偏移  宽度  内容
0x00   3     "AVL"                      魔数
0x03   1     u8 类型/标志（white_re: 0xFE；des: 0x06）
0x04   4     u32 LE（white_re: 0x00000200；des: 0x00000000）
0x08   4     u32 LE 条目数（white_re=3, fish_re=22, des=1）
0x0C   4     u32 LE 段长1（white_re=0x68=104, fish_re=0x39B=923, des=0x81=129）
0x10   4     u32 LE 段长2（white_re=0x43=67,  fish_re=0x1FA=506, des=0）
0x14   8     全 0
0x1C   …     加密/编码载荷（高熵，无可见明文）
```
`avlpk_des.avl` 的 0x08 字段为 `dd e9 33 63` = `0x6333E9DD` = **1664293341 → 2022-09-27 15:42 UTC**〔观测数值／推断为时间戳〕，与 `av.siglib 20220928` 吻合。

#### 2.3.2 `assets/70003.amf` — TMSDK **病毒特征基库**

| 项 | 值 |
|---|---|
| 大小 | 680,928 B |
| SHA-256 | `96CD4C5C0D83080311A4CA0F7198B640C36740C66E4F8FADFF0DA76B080E4E06` |
| 魔数〔观测〕 | `61 6d 66 00` = **`"amf\0"`** |
| 版本字段〔观测〕 | `u32@0x04 = 0x00000004` |
| 头结构〔观测〕 | `0x00 "amf\0"` \| `0x04 u32=4` \| `0x08..0x17` **16 字节全 0** \| `0x18 8 字节非零` \| `0x20` 起载荷 |
| `0x18` 8 字节〔观测〕 | `bd d2 f6 5f e6 75 25 62` = 两个 u32 LE：`0x5FF6D2BD`(1610015421 ≈ 2021-01-07)、`0x622575E6`(1646643174 ≈ 2022-03-07)〔推断为「基线日期 + 更新日期」双时间戳〕 |
| 载荷〔观测〕 | 从 `0x20` 起 680,896 B = **42,556 × 16 字节**（16 字节整除）；熵 8.0；**重复 16B 分块数 = 0**（`duplicate_blocks=0`） |
| 可见字符串 | 无（`[\x20-\x7E]{6,}` 仅命中 15 条随机噪声） |
| 语义〔观测+交叉〕 | `tmsdk/common/module/update/UpdateConfig.java:197-198`：`String str24 = intToString(70003) + ".amf"; VIRUS_BASE_NAME = str24;` → **`70003.amf` = `VIRUS_BASE_NAME`（TMSDK 病毒特征基库）**<br>同文件 `:199` `70002.amf = VIRUS_BASE_EN_NAME`、`:201` `70701.amf = PAYLOAD_NAME`、`:209` `AMS_DATA = 100003.dat` |
| 配套 | `libams-1.3.0-mfr.so`：`nativeLoadAmfHeaderBytes` / `nativeUpdateMalwareInfoBytes` / `scanApkBytes` / `getOpcode` 直接消费该容器 |
| 结构判断 | 〔观测〕非标准压缩（无 zlib/gzip/LZMA 魔数），16B 整除且无重复块 → 〔推断〕**自定义「按 16 字节分组 + 分组索引表」的加密特征库**，密钥候选见 `libams` 的 `!@#$%^&*()_JHGFD`（§5.3） |

#### 2.3.3 `assets/mdetector` — **ARM32 可执行体（非共享库）**

| 项 | 值 |
|---|---|
| 大小 | 136,636 B |
| SHA-256 | `AC98526131EEEFEC72B510AC8FD7DF95B8818A9E8A38310C1CF475723242CF1D` |
| 魔数〔观测〕 | `7f 45 4c 46` = **`\x7fELF`**；`EI_CLASS=01`(**ELF32**)、`EI_DATA=01`(LE)、`e_type=0x0003`(**ET_DYN/PIE**)、`e_machine=0x0028`(**40 = EM_ARM，32 位 ARM**) |
| 入口 / 解释器 | `e_entry=0x1df0`；**`PT_INTERP` = `/system/bin/linker`（ARM32 链接器）** |
| 节 | 23 节：`.interp .hash .dynsym .dynstr .rel.dyn .rel.plt .plt .text(0xe298) .rodata(0xce74) .ARM.extab .ARM.exidx .preinit_array .init_array .fini_array .ctors .dynamic .got .data .bss .comment .ARM.attributes .shstrtab` |
| 可见字符串（前 15 条，全部为符号名）〔观测〕 | `/system/bin/linker`、`liblog.so`、`__android_log_print`、`libstdc++.so`、`libm.so`、`libc.so`、`__cxa_atexit`、`__errno`、`__gnu_Unwind_Find_exidx`、`__libc_init`、`__stack_chk_fail`、`calloc`、`connect`、`fclose`、`fprintf` |
| 决定性特征〔观测〕 | 导出/内部符号含 **`pcap_in pcap_out pcap_lval pcap_text pcap_nerrs pcap_char pcap_leng pcap_setnonblock_fd pcap_getnonblock_fd`**，以及 `protocols` `eproto_db` `services` `ethers` `getprotobyname` `getservbyname` `getnetbyname` `if_indextoname` `pcap_parse` 相关 `no_optimize`、`vnode_base/next_vnode` |
| 结构判断〔观测+推断〕 | 这是一份**静态链接 libpcap（含 bison/yacc 生成的过滤表达式解析器）的 ARM32 PIE 独立可执行体**。`pcap_lval/pcap_text/pcap_char/pcap_nerrs/pcap_leng` 是 yacc 全局变量，`eproto_db/services/ethers` 是 libpcap 的协议/端口/以太网表 → 机证。<br>没有 `libpcap.so` 出现在 `DT_NEEDED`，故 libpcap 是**静态链入**。 |
| **决定性字符串〔观测，本轮独立复核〕** | `--------------- arp packet size should be 42 bytes or more!`(@0xfd77)、`gateway reply`、**`!!This Device is being Hijacked!!`**(@0xfdca)、`mac 1: %02X:%02X:%02X:%02X:%02X:%02X`、`mac 2: %02X:%02X:%02X:%02X`、`Target IP: %d.%d.%d.%d`、`[End]send msg to java: %s`(@0xfaf8)、`close socket`、`sleep %ds`、`%ds passed, exit`、`auto exit`、`Ethernet` `Unknown` `IPv4` `ARP Request` `ARP Reply`、`main-…`（线程名） |
| **语义〔观测+推断〕** | **这是一份「ARP 网关劫持（中间人）检测」原生可执行体**：抓 ARP Request/Reply → 比对 `gateway reply` 的 MAC 与真实网关 MAC → 不一致即打印 `!!This Device is being Hijacked!!`。<br>`[End]send msg to java: %s` 直接证明它有**回传给 Java 的输出协议**（子进程 stdout 被父进程读取），`sleep %ds` / `%ds passed, exit` / `auto exit` 是它的**生命周期自管理**。<br>→ 结合 `popen`/`Runtime.exec` 的调用点（见下），工程结构是：**Java 用 `Runtime.exec` 起 mdetector 子进程（32 位）→ 子进程用 `printf` 把结果按 `send msg to java:` 打给 stdout → Java 侧逐行解析**。 |
| **为什么放进 `assets/` 而不是 `lib/`** | 〔推断〕① `lib/*.so` 会被 PackageManager 登记、被第三方安全软件与 `ApplicationInfo.nativeLibraryDir` 枚举，`assets/` 不会；② 它是 **32 位 arm**，放进 `lib/arm64-v8a/` 反而会因为「同目录必须同 ABI」而装不上。 |
| **32 位 ABI 与「arm64-v8a only」矛盾** | 〔观测〕APK 声明 `arm64-v8a only`，本文件是 `EM_ARM`(32 位)。**64 位进程无法 dlopen 32 位 ELF**（`dlopen` 会报 `has unexpected e_machine`），**只能 `exec` 一个 32 位子进程**。<br>〔推断〕所以它**必须以「释放到磁盘 + `Runtime.exec`」方式使用**，不能当共享库。若设备无 32 位 linker（部分 Android 13+ 纯 64 位机型），该功能静默失效。 |
| **加载者线索〔观测〕** | 在 `_analysis\src\sources\` 与 `_analysis\apktool\smali\` 中 grep `mdetector` **均 0 命中**（文件名运行时拼接）。但找到三类强相关调用点：<br>① `smali\com\avl\engine\b\b\d.smali:76-122` —— **ABI 目录名归一化器**：`mips/64`、`arm64*`→`"64"`、`armeabi-v7a*`→`"v7a"`、`arm*`→`""`、否则 `"unknown"` → AVL 按 ABI 从 assets 取**不同的** native 载荷（这正是 asset 里能放 32 位二进制的机制）<br>② `smali\com\avl\engine\j\b\e.smali:217` 与 `:383` —— **`Runtime.exec(String)` 两处**（AVL 侧起子进程）<br>③ `smali\tmsdkobf\t9.smali:563` —— **`Runtime.exec(String)`**（TMSDK 侧起子进程）<br>另有 `smali\xcrash\j.smali:857` 与 `smali\com\xiaomi\onetrack\util\b.smali:789` 的 `ProcessBuilder`（xcrash / onetrack，与 mdetector 无关） |

#### 2.3.4 `assets/40706.dat` — TMSDK **证书 MD5 白名单**

| 项 | 值 |
|---|---|
| 大小 | 9,368 B |
| SHA-256 | `E746EE86B0E53718C87A4AB0DAB81BED4A1B84A060AEEB042D05D2AA4D93BA52` |
| 魔数〔观测〕 | **无**；首 16 B `fe2acff7626ac55b2d313981a53c92b7`，末 16 B `3c81765cab70788ce3e177e92023bdea` |
| 熵 | **7.983**（接近纯随机/密文） |
| 对齐〔观测〕 | 9,368 = **8 × 1171**（8 字节整除；不是 16 的倍数） |
| 可见字符串 | 无（命中 10 条均为随机噪声） |
| 语义〔观测+交叉〕 | `UpdateConfig.java:207-208`：`String str29 = intToString(40706) + ".dat"; CERTMD5_WHITELIST = str29;` → **`40706.dat` = `CERTMD5_WHITELIST`（证书 MD5 白名单）**<br>消费点 `tmsdk/common/module/qscanner/impl/f.java:415`：`qa.a(ctx, str, UpdateConfig.intToString(40706), new b2(), "UTF-8")`；而 `tmsdkobf/qa.java:48` 的读取路径正是 `TccCryptor.decrypt(bArr, null)` |
| 加密〔观测+推断〕 | **由 `TccCryptor`（`libTmsdk` 的 `decrypt([B[B)[B`）加解密**；8 字节对齐与 8 字节分组密码/流密码一致 |

#### 2.3.5 `assets/licence.conf` — TMSDK 授权（**密文，结构已解**）

| 项 | 值 |
|---|---|
| 大小 | 392 B |
| SHA-256 | `60FB5EB2C9EA51D55E487D4F434CAA12C37D53B4A7D2E01F920D5FF841DA658C` |
| 魔数〔观测〕 | **无**；熵 **7.525**；首 16 B `335e7397cbb7d899322e648f07b10f03` |
| 可见字符串 | 无 |
| 语义〔观测〕 | `tmsdkobf/f5.java:146`：`if (i6 == 0) str = "licence.conf"; else str = "licence" + i6 + ".conf";` → `assets.open(str)` 读取。**TMSDK 授权文件**（非 AVL；AVL 的授权文件在 `avlsdk` ZIP 内的 `av/avl/conf/liscense.conf`，拼写不同） |
| 结构〔观测〕 | `392 = 128 + 264`；**`264 % 8 == 0`**（264/8 = 33 个 DES 块，精确整除） |
| 解密算法〔观测，源码级〕 | `tmsdkobf/f5.java:53-64`：<br>`byte[128]` ← `bArr[0..128)`<br>`Cipher.getInstance("RSA/ECB/PKCS1Padding")` + `init(DECRYPT_MODE, rSAPublicKey)` + `doFinal(...)` → 得到 DES 密钥<br>`SecretKeyFactory.getInstance("DES").generateSecret(new DESKeySpec(bArrDoFinal2))`<br>`Cipher.getInstance("DES/ECB/NoPadding")` 解密 `bArr[128..]`<br>`Properties.load(...)` 解析明文 |
| 明文格式〔观测〕 | Java `.properties` 文本，键至少含：`signature`（APK 签名证书摘要，大写 HEX）、`expiry.seconds`（long）、`expiry.2019.enc_seconds`（HEX 串）、`lc_sdk_channel`（见 `tmsdkobf/e5.java:56-73`、`f5.java:107`） |
| **实际解密结果〔观测〕** | **失败**。用 `f5.java` 内置的 RSA-1024 公钥（证书 `CN=obamazeng, O=tencent, C=CN`，有效期 **2011-11-16 → 2012-02-24，已过期**）做公钥运算后，`m` 的 MSB 为 `0x08`，**不是 PKCS#1 v1.5 的 `00 01 FF…` 或 `00 02 …` 头部** → PKCS#1 去填充不成立。<br>又对 RSA 输出做 8 字节滑窗（121 个偏移）DES 试解，最高可打印率仅 **0.447**（噪声水平），无任何可读文本。<br>→ **结论：这两个 licence 文件不是用 `f5.java` 里那把公钥签发的**（那把证书本身 2012 年就过期了）。 |
| 结构判断（保留） | 容器结构 `[128B RSA 块][8 字节对齐的 DES-ECB 密文]` 由 **长度整除 + 源码算法**双重确认；只有「对应哪把 RSA 公钥」未知。见 §7-U3 |

#### 2.3.6 `assets/licence1.conf` — TMSDK 授权（第 2 份）

| 项 | 值 |
|---|---|
| 大小 | 368 B |
| SHA-256 | `FC98C0DB13FFA6F36289123E1DEF3277D5661543D0BBA3A838D8B7DE4E58B817` |
| 魔数 / 熵 | 无 / **7.463**；首 16 B `4c60ede98853155e0ff97b00ef0194fe` |
| 结构〔观测〕 | `368 = 128 + 240`；**`240 % 8 == 0`**（30 个 DES 块） |
| 语义〔观测〕 | `f5.java:148` 的 `"licence" + i6 + ".conf"`，`i6=1` → `licence1.conf`。`f5.f()` 是 `while(true)` 循环：`licence.conf` 解析失败就 `i6++` 换 `licence1.conf` …… 因此**这两份是同一把公钥下的多份候选授权，运行期按序试** |
| 解密结果 | 同 §2.3.5：结构成立，**用内置公钥解不开** |
| 可见字符串 | `l}~iFuQ`、`=dbs/iA`（随机噪声，非明文） |

#### 2.3.7 `assets/dexopt/baseline.prof` — ART Baseline Profile

| 项 | 值 |
|---|---|
| 大小 / SHA-256 | 127 B / `099F760BFAC16C81F88C18216A96C4E63CB058144E2828FBDEBC6AC3707E1752` |
| 魔数〔观测〕 | `70 72 6f 00` = **`"pro\0"`**，紧跟 `30 31 30 00` = **`"010\0"`**（ART Profile 格式版本 010） |
| 结构〔观测〕 | `[magic "pro\0"][version "010\0"][u32 dex 数=1][u32 dex 校验和=0xdd22][u32 方法数=0x6e]` 后接压缩的 dex 位置/方法索引位图（`78 01` = zlib 头） |
| 定性 | **Android 标准基线 profile，非载荷**。由 R8/AGP 在打包时生成，供 ART 做 AOT 预编译 |
| 可见字符串 | 无（二进制位图） |

#### 2.3.8 `assets/dexopt/baseline.profm` — ART Profile 元数据

| 项 | 值 |
|---|---|
| 大小 / SHA-256 | 55 B / `B8CFB8674ADB1A78DE5C47D3882BAF8751B56AF6E512ABEBF35A74849DDEF8A1` |
| 魔数〔观测〕 | `70 72 6d 00` = **`"prm\0"`**，紧跟 `30 30 32 00` = **`"002\0"`** |
| 结构〔观测〕 | `[magic "prm\0"][version "002\0"][u32 标志=1][u32 项数=0x1f][u32 长度=0x25]` + zlib 压缩体 |
| 定性 | ART baseline profile 的伴生元数据（dex 校验和 / 签名），非载荷 |
| 可见字符串 | `d``ePc``（zlib 压缩体噪声） |

---

## 3. 数据流 / 时序

### 3.1 AVL 栈初始化（`libavlasys` → `libavlm`）

```
[assets/avlsdk] --PK\x03\x04 ZIP-->
   com.avl.engine.a.c.d()  (c.java:110)
     ├─ i(eVar): assets.open("avlsdk") → com.avl.engine.k.c.a.e.a(inputStream)  ← 容器完整性校验
     ├─ com.avl.engine.c.b.a.a(...)  unzipAssetsFileEfficient → context.getDir("avlsdk",0)
     │     （c.java:20 → AVLA.unzipAssetsFileEfficient → 落到 libavlasys.so JNI）
     ├─ AVLA.a().pcit(<dir>/avl)                       ← c.java:130
     └─ e(eVar)  (c.java:143-163)
          path-N: com.avl.engine.k.f.a(ctx,"avlm") 找到 libavlm.so 绝对路径
                  → AVLA.setAVLSdkPath(appKey, <path>/libavlm.so, <dir>/avlsdk, <pkgCodePath>, sdkInt)
          path-S: AVLA.a(<appKey>, <dir>/avlsdk, <pkgCodePath>, sdkInt)   ← 内部硬编码 "libavlm.so"
```
〔观测〕`AVLA.java:42`：`return setAVLSdkPath(str, "libavlm.so", str2, str3, i6);` —— **`"libavlm.so"` 是硬编码字符串**。
〔观测〕`libavlasys.so` 的 `JNI_OnLoad` 只引用 `NativeUtils` + `AVLA` 两个类名（§2.2.2 机证）。
〔观测〕`libavlasys.so` `.rodata` 含 `dlopen error: %s`、`so name is NULL`、`./conf/liscense.conf`、`./avl.avl`、`/avl/conf/avllib.conf`
→ 运行期用 `dlopen` 装载引擎，并用 `./avl.avl` / `./conf/liscense.conf` 做授权校验。

### 3.2 URL 引擎链路

```
com.avl.engine.urldetector.UrlDetector  (UrlDetector.java:80 System.loadLibrary("urldetectorsys"))
   └─ liburldetectorsys.so  JNI_OnLoad@0x1db4  RegisterNatives:
        setLibPath(String)I / loadURL / unLoadURL()V / matchURL(String,Z)I / matchURLEx(String,Z)[String]
          └─ dlopen("liburl.so")                     ← 实际文件是 libavlurl.so（SONAME=liburl.so）
               └─ dlsym: AVLM_Load_URL / AVLM_Match_URL / AVLM_Match_URLEx / AVLM_Match_SPURLEx
                          AVLM_GetSIDInfo / AVLM_FreeSIDInfo / AVLM_MatchWithType
```
〔观测〕`liburldetectorsys.so` 全库 14,408 B、`.text` 4.2 KB、import 只有 `dl*` + 字符串函数 → 纯转发。
〔未知〕`dlopen("liburl.so")` 而磁盘上是 `libavlurl.so`：要么 `setLibPath` 先被调用改成真实路径，要么 Android 链接器按 SONAME 别名解析。见 §7-U4。

### 3.3 TMSDK 特征库加载

```
assets/70003.amf  --(VIRUS_BASE_NAME)-->  AmScannerV2.nativeLoadAmfHeaderBytes(byte[])  → libams
                                          AmScannerV2.nativeUpdateMalwareInfoBytes(byte[])
                                          AmScannerV2.scanApkBytes(byte[],...) → getOpcode(...) → extractApkInfo

assets/40706.dat  --(CERTMD5_WHITELIST)-->  tmsdkobf.qa.a(...)  → TccCryptor.decrypt(bytes,null) → libTmsdk
```

### 3.4 小米自研扫描（Rust）

```
p0/y.java:111  RustSupport.matches(byte[] 文件内容, t0.j.a(stringItem))   → libflamingo(matches)  单模式
p0/y.java:117  RustSupport.search (byte[] 文件内容, stringItem.getValue()) → libflamingo(search)   单模式
               （重载 search(byte[],String...) → aho-corasick 多模式，返回 boolean[]）
```

### 3.5 xcrash 崩溃捕获

```
xcrash.NativeHandler.d()  → System.loadLibrary("xcrash")  → libxcrash.so JNI_OnLoad@0x328c
   ├─ nativeInit(31 参数：SDK_INT, RELEASE, ABI, MANUFACTURER, BRAND, MODEL, FINGERPRINT,
   │            logDir, ... , nativeLibraryDir, ...)   ← 把整机指纹交给 native
   ├─ prctl(PR_SET_DUMPABLE / PR_SET_PTRACER)  → "set dumpable failed" / "set traceable failed"
   └─ execl(<nativeLibraryDir>/libxcrash_dumper.so)  → 子进程 ptrace 父进程，抓 /proc/<pid>/maps
        产物：%s/tombstone_%020lu_%s__%s%s  （"tombstone_" + 20 位时间戳）
```

---

## 4. 关键逻辑逐条解析

### 4.1 JNI 边界全表（Java 声明 → 实际符号 → 语义）

**总规模：7 个 Java 侧 native 声明类群，共 5 个 `.so` 承载，合计 21 个直接 `Java_*` 导出 + 6 张 RegisterNatives 注册表（约 60 个方法）。**

#### 4.1.1 `com.miui.guardprovider.natives.RustSupport` → `libflamingo.so`（直接 `Java_*` 导出）

Java 侧 `System.loadLibrary("flamingo")`（`RustSupport.java:6`）。

| Java 声明（`RustSupport.java`） | 实际导出符号 | 语义 |
|---|---|---|
| `static native int matches(byte[], String)` (:12) | `Java_com_miui_guardprovider_natives_RustSupport_matches`@0x9d760 (480B) | 单正则/单模式匹配，返回命中与否（int） |
| `static native int search(byte[], String)` (:14) | `Java_com_miui_guardprovider_natives_RustSupport_search`@0x9d940 (664B) | 单模式查找，返回位置/计数（int） |
| `static native boolean[] search(byte[], String...)` (:16) | `..._RustSupport_search___3B_3Ljava_lang_String_2`@0x9dbd8 (1832B) | 多模式批量查找（Aho-Corasick），返回每条模式的布尔结果 |

〔观测〕`RustException.java` 同包，是 native 抛回 Kotlin 的异常载体（无语义 JNI 作用）。
〔观测〕REA `inspect_native_exports` 复核 `libflamingo.so` **恰好 3 个导出**，与上表一一对应，无多余 JNI 面。

#### 4.1.2 `com.avl.engine.security.NativeUtils` + `AVLA` → `libavlasys.so`（RegisterNatives）

〔观测〕`libavlasys.so` `JNI_OnLoad`@0x105fc 的字符串引用**只有** `com/avl/engine/security/NativeUtils` 与 `com/avl/engine/security/AVLA` 两条。
`AVLA extends NativeUtils`（`AVLA.java:4`），所以两张表的 natives 都落在这个库。

**`NativeUtils` 表〔观测，`.rodata`@0x115c6 明文名字+签名〕**

| Java 声明 | 注册签名 | 语义 |
|---|---|---|
| `static native byte[][] getMetaInfEntryNames(String)` | `(Ljava/lang/String;)[[B` | 读 APK 的 `META-INF` 条目名（**签名校验相关**；调用点 `com/avl/engine/security/h.java:59`） |
| `static native int ine(String)` / `ine(String, byte[])` | 未在串表出现 | 〔未知〕名字被混淆，可能以短名注册 |
| `static native int searchFile(String, FileSearcher)` | `(Ljava/lang/String;Lcom/avl/engine/security/FileSearcher;)I` | 单文件扫描回调 |
| `static native int searchFile(String[], FileSearcher)` | `([Ljava/lang/String;Lcom/avl/engine/security/FileSearcher;)I` | 多文件扫描回调 |
| `native String getCpuPlatform()` | `getCpuPlatform` | CPU 平台串（`UrlDetector.b.a.java:47` 用它） |
| `native String getLocalUID()` | `getLocalUID` | 本地 UID |
| `native String getFileMD5(String,int)` | `(Ljava/lang/String;I)Ljava/lang/String;` | 文件 MD5（带偏移/长度参数） |
| `native String getStringMD5(String,int)` | `getStringMD5` | 字符串 MD5 |
| `native String readAssetsFile(String,String,String)` | `(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;` | 读 assets |
| `native String readAssetsFileEfficient(AssetManager,String,String)` | `(Landroid/content/res/AssetManager;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;` | AssetManager 读 assets |
| `native int unzipAssetsFile(String,String,String,List)` | `(...Ljava/util/List;)I` | 解压 assets |
| `native int unzipAssetsFileEfficient(AssetManager,String,String,List)` | `(Landroid/content/res/AssetManager;...Ljava/util/List;)I` | AssetManager 解压（`avlsdk` 走这条） |
| `native int unzipAvllib(String,String)` | `(Ljava/lang/String;Ljava/lang/String;)I` | 解压 avllib |
| `native int unzipFile(String,String,List)` | `(...Ljava/util/List;)I` | 通用解压 |

**`AVLA` 表〔观测，`.rodata`@0x10ef0 明文〕**

| Java 声明（`AVLA.java`） | 注册名/签名 | 语义 |
|---|---|---|
| `static native int checkKey()` (:12) | `checkKey` | 授权校验（失败串 `AVL SDK licence is overdue!`） |
| `private static native String gci(byte[],int)` (:18) | `gci ([BI)Ljava/lang/String;` | 证书解析（`gci` 即 getCertInfo） |
| `private static native … fbcb(int,int,String)` (:14) | `fbcb (IILjava/lang/String;)V` | native→Java 日志回调 |
| `native int setAVLSdkPath(String,String,String,String,int)` (:103) | `setAVLSdkPath (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)I` | **装载 `libavlm.so` + 指定 assets 目录 + appkey + SDK_INT** |
| `native int computeToken(String,String,String)` (:51) | `computeToken` | 计算 app token（配合 `AVLM_ComputeAPPToken`） |
| `native int initEngine()` (:81) | `initEngine` | → `AVLM_Init` / `AVLM_InitEx` |
| `native int releaseEngine()` (:95) | `releaseEngine` | → `AVLM_Release` |
| `native String scan(String,int[])` (:97) | `scan` | → `AVLM_Scan` |
| `native String[] scanEx(String,int[],long)` (:99) | `scanEx` | → `AVLM_ScanExWith*` |
| `native String sceneScan(String,int[],int)` (:101) | `sceneScan` | → `AVLM_ScanWithScanOpt` |
| `native int officialScan(String)` (:89) | `officialScan` | → `AVLM_CheckOfficalAPP` |
| `native String getEngineVersion()` (:67) | `getEngineVersion` | → `AVLM_GetEngineVersion` |
| `native String getSigLibVersion()` (:69) | `getSigLibVersion` | → `AVLM_GetSigLibVersion` |
| `native String getEngConfVersion(String)` (:65) | `getEngConfVersion` | 读 `<dir>/conf/avllib.conf` |
| `native String getAPKInfo(String)` (:59) | `getAPKInfo` | → `AVLM_GetAPKInfo` |
| `native String getCertHash/getCertFullHash(String)` (:61,:63) | — | → `AVLM_GetCertHash` / `AVLM_GetCertFullHash` |
| `native String getVirusDescription(String,int)` (:71) | — | → `AVLM_GetVirusDesc` |
| `native String arbitrateVirusName(String)` (:47) | `arbitrateVirusName` | → `AVLM_VirNamesChoice` |
| `native String analyseAppByEngine(String)` (:45) | — | → `AVLM_GetAPKInfo` 组合 |
| `native int installPackage(String,int,String,String)` (:83) | — | → `AVLM_InstallPackage` |
| `native byte[] gpubkc(String)` (:73) | `gpubkc` | → `AVLM_GetPubkeyContent` |
| `native int nec(byte[],String,int)` (:87) | `nec` | 〔未知〕字节变换，疑似名字混淆 |
| `native int keySetHash/signBlockHash/signBlockAndKeySetHash(String)` (:85,:113,:111) | — | 签名块哈希 |
| `native int pcit(String)` (:91) | `pcit` | 设置 SDK 数据目录（`c.java:130` 传 `<dir>/avl`） |
| `native void pcr()` (:93) / `ckti()` (:49) / `fcf(int)` / `sjcf(int,int)` / `sjsp(JniFuncProcess)` | — | 内部清缓存/回调注册（`sjsp` 注册 `JniFuncProcess.checkFuncEnable`） |
| `setAIES/setAIER/setScanLimit/setScanLimitEx/setDEXScanLimit/setOpcDEXScanLimit/setMapleScanLimit` (private) | 部分在 `.rodata` 表 | → 同名 `AVLM_Set*` |
| `native int gufbyud(String,int,int,int)` (:77) / `icf(String,int)` / `gcv(String,int)` / `geci(String)` / `gsi(String)` / `sfcs(String)` / `sfcp(String,int,int)` / `suf(int)` / `suhms(int)` | — | 〔推断〕全为混淆短名，语义需动态确认；`gsi`≈getSidInfo，`geci`≈getEngineConfInfo |

#### 4.1.3 `com.tencent.turingfd.sdk.vd.TNative$aa` → `libturingvd.so`（RegisterNatives）

〔观测〕DEX 类描述符真含 `$`：`com/tencent/turingfd/sdk/vd/TNative$aa`、`.../TNative$aa$bb`；**无 `Java_` 导出**，故必为 RegisterNatives。

| Java 声明（`TNative$aa.java`） | 注册签名 | 语义 |
|---|---|---|
| `a(SparseArray, Context, Map, int)` (:22) | `(Landroid/util/SparseArray;Landroid/content/Context;Ljava/util/Map;I)Landroid/util/SparseArray;` | 风控主入口（带 Context） |
| `b(SparseArray, byte[], Map, int)` (:24) | `(Landroid/util/SparseArray;[BLjava/util/Map;I)...` | 字节输入型风控 |
| `c(SparseArray, Context)` (:26) | `(Landroid/util/SparseArray;Landroid/content/Context;)...` | 简版 |
| `d(SparseArray, Context, int)` (:28) | `(Landroid/util/SparseArray;Landroid/content/Context;I)...` | 带类型 |
| `e(SparseArray, Context, Map, int)` (:30) | `(...Context;Map;I)` | 同 `a` 变形 |
| `f(SparseArray, byte[], int)` (:32) | `(Landroid/util/SparseArray;[BI)...` | 字节 + 类型 |
| `g/h(SparseArray, Context, Map, int)` (:34,:36) | `(...Context;Map;I)` | 同签名另两份实现 |
| `i(SparseArray, Context, Map)` (:38) | `(...Context;Map)` | 无类型参数 |
| `TNative$aa$bb.onServiceConnected(ComponentName, IBinder)` (:15) | `(Landroid/content/ComponentName;Landroid/os/IBinder;)V` | native 实现 `ServiceConnection` 回调（**在 native 里完成 bindService 回调处理**） |

#### 4.1.4 `xcrash.NativeHandler` → `libxcrash.so`（RegisterNatives）

| Java 声明 | 注册签名 | 语义 |
|---|---|---|
| `nativeInit(...)` 31 参 (:92) | `(ILjava/lang/String;×10;ZZIIIZZZZZI[Ljava/lang/String;ZZIIIZZ)I` | 初始化；把 SDK_INT/RELEASE/ABI/MANUFACTURER/BRAND/MODEL/FINGERPRINT/nativeLibraryDir 等整机指纹传入 |
| `nativeNotifyJavaCrashed()` (:94) | `()V` | 告知 native Java 侧已崩 |
| `nativeTestCrash(int)` (:96) | `(I)V` | 自测崩溃 |
| `nativeTraceSignalInit()` (:99) | 无签名（唯一） | 在主线程装信号栈 |
| `crashCallback(String,String,boolean,boolean,String)` (:74) | `(Ljava/lang/String;Ljava/lang/String;ZZLjava/lang/String;)V` | **native 回调 Java**（Java 侧无此声明，纯 native 侧注册） |
| `traceCallback(String,String)` (:101) | — | 同上（ANR trace 回调） |

#### 4.1.5 `tmsdk.common.tcc.TccCryptor` → `libTmsdk-2.0.13-mfr.so`（RegisterNatives）

| Java 声明（`TccCryptor.java`） | 注册签名 | 语义 |
|---|---|---|
| `static native byte[] decrypt(byte[], byte[])` (:21) | `decrypt ([B[B)[B` | 解密（第二参为 key，可为 `null` 用内置 key） |
| `static native byte[] encrypt(byte[], byte[])` (:41) | `encrypt ([B[B)[B` | 加密 |
| `static native byte[] makePassword(byte[])` (:61) | `makePassword ([B)[B` | 口令派生 |
| `add(Object)Z` / `length()I` / `replace(II,String)StringBuffer` | 同表 | 〔推断〕辅助方法，可能属内部类 |

〔观测〕Java 侧纯 Java 的「假加密」也在：`encryptInt(i)=~i`、`encryptBoolean(b)=!b`（`TccCryptor.java:28,44,63`）——这些是**混淆，不是加密**。
〔观测〕`tmsdkobf/gb.java:490,527,536` 用 `TccCryptor.decrypt(new byte[]{8~16 字节})` 解出**类名与字符串**：
`new String(TccCryptor.decrypt(new byte[]{-82,6,122,-22,71,-51,117,-65}, null))`、`Class.forName(new String(TccCryptor.decrypt(bArr,null)))`
→ TMSDK 的**字符串/类名加密表**就在 `libTmsdk` 里，`decrypt` 就是它的解密原语。
〔观测〕`tmsdkobf/b5.java:125`：`TMSDKContext.registerNatives(0, TccCryptor.class)` —— TMSDK 有自己的显式注册入口，进一步确认是 RegisterNatives。

#### 4.1.6 `com.tencent.tmf.gm.{Sm2,Sm4}` → `libtmf-gm.so`（直接 `Java_*` 导出）

| Java 声明 | 导出符号 | 语义 |
|---|---|---|
| `Sm2.doSm2Encrypt(...)` | `Java_com_tencent_tmf_gm_Sm2_doSm2Encrypt`@0x18fc | SM2 加密 |
| `Sm2.doSm2Encrypt2(...)` | `..._doSm2Encrypt2`@0x1dd4 | SM2 加密（变体，C1C3C2 顺序差异） |
| `Sm2.doSm2Decrypt(...)` | `..._doSm2Decrypt`@0x1bd4 | SM2 解密 |
| `Sm2.doSm2Decrypt2(...)` | `..._doSm2Decrypt2`@0x1f54 | SM2 解密（变体） |
| `Sm4.doSm4EncryptCBC(...)` | `Java_com_tencent_tmf_gm_Sm4_doSm4EncryptCBC`@0x156c | SM4-CBC 加密 |
| `Sm4.doSm4DecryptCBC(...)` | `..._doSm4DecryptCBC`@0x1734 | SM4-CBC 解密 |
| `Sm4.doSm4EncryptECB(...)` | `..._doSm4EncryptECB`@0x100c | SM4-ECB 加密 |
| `Sm4.doSm4DecryptECB(...)` | `..._doSm4DecryptECB`@0x13dc | SM4-ECB 解密 |

#### 4.1.7 `com.avl.engine.urldetector.UrlDetector` → `liburldetectorsys.so`（RegisterNatives）

| Java 声明 | 注册签名 | 语义 |
|---|---|---|
| `setLibPath(String)` | `(Ljava/lang/String;)I` | 指定 `liburl.so` 路径 |
| `loadURL(...)` | `loadURL` | 装载 URL 库/特征 |
| `unLoadURL()` | `()V` | 卸载 |
| `matchURL(String, boolean)` | `(Ljava/lang/String;Z)I` | 单 URL 判定 |
| `matchURLEx(String, boolean)` | `(Ljava/lang/String;Z)[Ljava/lang/String;` | 批量 URL 判定 |
| `unzipAssetsFileEfficient(...)` / `readAssetsFileEfficient(...)`（继承自 `UrlDetector`，`a/d.java:17,22` 调用） | 见 NativeUtils 表 | 读/解压 assets |

#### 4.1.8 其余 Java native 声明 → 归属结论

| Java 类 | 声明 | 承载 `.so` | 结论 |
|---|---|---|---|
| `tmsdk.common.tcc.QFile` | natives | **`libdce-1.1.21-mfr.so`**（`QFile.java:28` `loadLibrary("dce-1.1.21-mfr")`） | ✓ 对应（注册表含 `tmsdk/common/tcc/QFile`） |
| `tmsdk.common.module.qscanner.impl.AmScannerV2` | 8 个 | **`libams-1.3.0-mfr.so`** | ✓ 直接 `Java_*` 导出 |
| `com.tencent.tcuser.util.Encode` | natives | **无对应库** | ✗ `Encode.java:43,48` `System.loadLibrary("xy")`，但 `lib/arm64-v8a/` **无 `libxy.so`**。`loadLibrary` 被 `try/catch` 包住并 `pa.a(new Thread(), th2, "System.loadLibrary error", null)` 只上报 → **死代码/降级路径**〔推断〕。见 §7-U5 |
| `tmsdkobf.ka` | 无声明，只做 `System.load()` | — | 〔观测〕`ka.java:32,46,56` `System.load(env+path)`、`:64,72` `loadLibrary(name)`：**TMSDK 的私有库装载器**，从 `/data/data/<pkg>/lib/` 显式装载（`System.load` 需要绝对路径，说明这些 `.so` 可能是**运行期释放**的，不在 APK `lib/` 里） |

---

### 4.2 关键函数分析（`analyze_function` 替代方案：符号 + AArch64 字符串引用）

> Ghidra 在本机不可用于 ELF（§7-U0）。下表用 ① dynsym 的 `addr/size`〔观测〕 ② 自建 `ADRP+ADD` 解码得到的「函数→引用字符串」〔观测〕。

| # | 函数 | 地址 / 大小 | 机械证据 | 判定 |
|---|---|---|---|---|
| **F1** | `libavlasys.so: JNI_OnLoad` | `0x105fc` / 204 B | ADRP+ADD 扫描（`.text` 0x2c00..0x10910，316 处引用）中归属本函数的字符串**恰好 2 条**：`com/avl/engine/security/NativeUtils`、`com/avl/engine/security/AVLA` | 〔观测〕**本函数只做两件事**：`FindClass` 这两个类 + `RegisterNatives`。这是「libavlasys 是 AVL JNI 壳」的**机证**，不是推断。 |
| **F2** | `libavlasys.so: AVL_SetScanLimitEx` / `AVL_SetMapleScanLimit` | `0xe004` (24 B) / `0xdfec` (24 B) | 两个 24 字节的**极短导出**，紧跟 `JNI_OnLoad` 之前；`libavlm.so` 也导出同名 `AVLM_SetScanLimitEx`@0xaea4c(44B) / `AVLM_SetMapleScanLimit`@0xae9c0(44B) | 〔推断〕`libavlasys` 侧是 **dlsym 转发 trampoline**（24 B ≈ `adrp/ldr/br` 三指令），把调用转给 `libavlm`。这就是为什么 `AVLA.setScanLimit*` 不直接出现在 JNI 表而由 `AVL_*` 承担。 |
| **F3** | `libavlm.so: AVLM_Init` | `0xae764` / **8 B** | 8 字节 = 一条 `b` 跳转 | 〔观测〕**真身不在这个符号上**：`size=8` 的 `b` 跳板。真实实现在 `AVLM_InitEx`@0xae76c(540 B)。`AVLM_Init` 只是别名跳板 → 印证 §2.2.3 的「正式名 + 别名」反 hook 结构。 |
| **F4** | `libavlm.so: AVLM_ScanWithScanOptEx` | `0xaf318` / 780 B | `libavlasys` `.rodata` 的 dlsym 名字表里同时出现 `AVLM_ScanWithScanOpt`(36B) 与 `AVLM_ScanWithScanOptEx`(780B) | 〔推断〕`...Opt` 是跳板、`...OptEx` 是真实扫描主循环（780 B 是 `AVLM_*` 里最大的实函数之一）。扫描入口优先级：`Scan`(12B)→`ScanWithArgs`(812B)→`ScanWithScanOptEx`(780B)。 |
| **F5** | `libavlm.so: AVLM_ComputeAPPToken` / `AVLM_CheckAPPToken` / `AVLM_CheckAPPKey` | `0xb1b00`(220B) / `0xb1bdc`(224B) / `0xb1cbc`(116B) | `libavlasys` `.rodata` 同时含 `check key error: token is empty.` / `check key error: appkey is empty.` / `check key error: %s is not found.` / `check key error: sopath is empty.` | 〔观测〕授权链：Java `AVLA.computeToken(pkgCodePath, pkgName, pkgSig)` → `AVLM_ComputeAPPToken`；`AVLM_CheckAPPToken`/`CheckAPPKey` 在 `libavlasys` 里与 `62BAFD662A63A99A7C96DA027AFDF739` 同区存放 → 〔推断〕该 32 位 HEX 串即 **AVL 授权 key 之一**。 |
| **F6** | `libavlurl.so: AVLM_Match_URLEx` / `AVLM_Match_SPURLEx` / `AVLM_Load_URL` | `0x32918`(616B) / `0x32d98`(544B) / `0x32774`(172B) | `liburldetectorsys` 的 dlsym 名字表含 `AVLM_MatchWithType` / `AVLM_GetSIDInfo` / `AVLM_FreeSIDInfo`；`libavlurl` `.rodata` 含 `%s.new` `%stmp/` `%s.tmp/` `%s/conf/avllib.conf` | 〔推断〕`AVLM_Load_URL` 从 `<dir>/conf/avllib.conf` + `*.avl` 装载并支持 `.new`→`.tmp` 原子热更新；`Match_URLEx` 是主判定循环。 |
| **F7** | `libTmsdk: TccCryptor.decrypt` / `encrypt` / `makePassword` | 注册于 `JNI_OnLoad`@0x1f480 (156 B) | `.rodata`@0x484e0 明文 `makePassword ([B)[B` `encrypt ([B[B)[B` `decrypt` + 错误串 `Bad encrypted data` `data too large` | 〔观测〕TccCryptor 三原语全在 `libTmsdk`。`Bad encrypted data` 是解密失败串 → 有**完整性/格式校验**，纯 XOR 试解会失败。 |
| **F8** | `libams: AmScannerV2_nativeLoadAmfHeaderBytes` | `0x1c514` / 1308 B | `.rodata`@0x8db48 的 AMF 表项名 + 两个可疑常量 | 〔推断〕1308 B 的函数负责解析 `70003.amf` 头并建表项名索引；`getOpcode`@0x1dbd4(3488 B) 是**全库最大函数**，对应 §4.1 的 `getOpcode` 语义（把 AMF 规则编译成匹配字节码）。 |

### 4.3 加密与国密

#### 4.3.1 `libtmf-gm.so` ↔ `com.tencent.tmf.gm.{Sm2,Sm4}`

〔观测〕关系是**直接 JNI 绑定**：8 个 `Java_com_tencent_tmf_gm_{Sm2,Sm4}_*` 符号 + `libtmf-gm.so` 显式 `.dynsym` 导出 + `com/tencent/tmf/gm/a.java:26` `System.loadLibrary("tmf-gm")`。三处独立证据一致。

〔观测〕`com/tencent/tmf/gm/b.java` 是 Java 侧调用封装：
- `b.java:20` `a.C0038a c = Sm4.a(bArr2, bArr)` → SM4 加密
- `b.java:34` `Sm4.b(bArr2, bArr)` → SM4 解密
- `b.java:52` `Sm2.a(bArr3, bArr4, bArr)` → SM2

〔观测〕`libtmf-gm.so` **不含任何密钥材料**：`.rodata` 只有 8 个 JNI 名字 + 9 个 `_ZN7_JNIEnv*` 方法名 + 依赖库名，**没有 PEM / 长 HEX / 硬编码 IV**。密钥全部由 Java 侧以 `byte[]` 传入。
〔观测〕`.dynstr` 无 `SSL`/`TLS`/`X509` 字样 → **SM2/SM4 不用于传输层**，是 TMF 配置/上报报文的应用层加密。

〔未知〕SM2/SM4 在具体协议中的位置（加密哪个字段、IV 从哪来）需看 `com.tencent.tmf.*` 的调用方；本轮只确认算法绑定与「密钥外置」。见 §7-U6。

#### 4.3.2 三套并存的对称加密体系

| 体系 | 实现 | 密钥来源 | 证据 |
|---|---|---|---|
| **TccCryptor**（TMSDK 私有） | `libTmsdk` 的 `encrypt/decrypt/makePassword` | **内置在 native**（`decrypt(b, null)` 传 `null` 就走内置 key） | 〔观测〕`TccCryptor.java:21` + `tmsdkobf/qa.java:48` `TccCryptor.decrypt(bArr, null)`；错误串 `Bad encrypted data` |
| **RSA-1024 + DES-ECB**（licence） | Java 层 `javax.crypto`（不用 native） | 128 B RSA 块 → DES key；RSA 证书硬编码在 `f5.java:52` | 〔观测〕§2.3.5 |
| **国密 SM2/SM4** | `libtmf-gm.so` | 全部外部传入 | 〔观测〕§4.3.1 |

〔观测〕三者**互不复用**：`libtmf-gm` 不 import `libTmsdk`，`libTmsdk` 也不 import `libtmf-gm`；`licence.conf` 用 Java 标准 JCE。

#### 4.3.3 `assets/avlsdk` 内 `liscense.conf`（AVL 自己的授权）

〔观测〕`av/avl/conf/liscense.conf` 与 `url/url/conf/liscense.conf` 都是 **404 B**，且**末 4 字节均为 `01 00 00 00`**。
〔观测〕`404 = 4 + 400`，而 **`400 % 16 == 0`**（25 个 16 字节块）→ 与 SM4/AES 分组一致。
〔观测〕两份的首 16 字节完全不同（`5da3caa9df459f73…` vs 需另取），说明内容是**逐实例加密**，不是固定模板。
〔推断〕结构 = `[400 B 分组密文][u32 LE = 1（版本/条目数/校验）]`。AVL 的 `checkKey`/`key is overdue!` 就在 `libavlasys` 里，解密与校验应由 `libavlasys` 完成（该库 import `libz`，但**没有** `__android_log_print` 之外的密码库依赖 → 算法自实现）。
〔未知〕具体算法与 key 未能还原。见 §7-U3。

#### 4.3.4 硬编码密钥 / IV 汇总

| 位置 | 值 | 类型 | 状态 |
|---|---|---|---|
| `libavlasys.so` `.rodata`（`0x10ef0` 区，紧随 `AVL SDK licence is overdue!` 之后） | **`62BAFD662A63A99A7C96DA027AFDF739`** | 32 HEX = 16 B | 〔观测〕明文硬编码。推断为 AVL licence/appkey 校验材料 |
| `AndroidManifest` meta-data（简报 §已确认） | `D5DFB878335A3EF4E76F60ED470B8EC8` | 32 HEX | 〔观测〕`AVL_OPENSDK_APPKEY`；经 `AVLA.setAVLSdkPath(appKey,…)` / `computeToken` 进 native |
| `libams-1.3.0-mfr.so` `.rodata`@0x8db48 | **`!@#$%^&*()_JHGFD`** | 17 B ASCII | 〔观测〕明文；〔推断〕AMF 表项名混淆 key |
| 同上 | **`SfJ(*W#&Ddweir309*&@#!_)($39`** | 28 B ASCII | 〔观测〕明文；〔推断〕AMF 二级混淆 key |
| `tmsdkobf/f5.java:52`（Java 常量，非 native） | RSA-1024 X.509，`CN=obamazeng, OU=3G, O=tencent, L=GZ, S=GD, C=CN`；`NotBefore 2011-11-16 / NotAfter 2012-02-24`（**已过期**） | 证书 | 〔观测〕Base64 PEM 硬编码 |
| `META-INF/flamingo.dat`（t-cloud 已验算） | AES-128 key `wcw1xbplx7aijwqr` | 16 B ASCII | 〔观测-引用〕与 `libflamingo.so` **无关**，见 §4.3.5 |

#### 4.3.5 `flamingo.dat` 与 `libflamingo.so` 的关系（回答 lead 的疑问）

〔观测〕**同名不同物，没有密码学关联**：
1. `libflamingo.so` 的 **完整 import 表**只有 `libc` + `libdl` 的 40 项，**无任何 JNI 侧 crypto、无 `__android_log_print`、无文件读取**；其 3 个 native 方法签名是 `(byte[],String)` / `(byte[],String[])`，**根本收不到密钥**（`RustSupport.java:12-16`）。
2. `libflamingo.so` 的 `.rodata` 全是 Rust `regex`/`aho-corasick`/`rustc-demangle` 的 panic 串（§2.2.6），**零密码学字符串**。
3. `META-INF/flamingo.dat` 由 **Java** 侧读取：`v0/j.java:38` `classLoader.getResourceAsStream("META-INF/flamingo.dat")`；相关的 `p0/n.java:749` `map.put("flamingo_dispatcher", …)`、`p0/g.java:712` `aVarA.b("flamingo_dispatcher")` 都是 **Java/Kotlin 层的派发器**。
4. `libflamingo.so` 只在 `RustSupport` 里被 `System.loadLibrary`，调用点是 `p0/y.java:111,117` 的**特征串匹配**。

→ **结论：`flamingo` 是小米这套扫描框架的代号（`flamingo-android_release`、`flamingo.db`、`flamingo_dispatcher` 到处都是）；`flamingo.dat` 是这个框架的配置密钥载体，`libflamingo.so` 是它的 Rust 匹配引擎。两者共享代号，不共享密钥路径。**

### 4.4 可疑/高危能力举证（字符串 + import 双重证据）

> 方法：对 12 个 `.so` 各自做 Latin1 解码后跑 17 组正则（`ANTIDBG/ROOT/EMU/PROC/SHELL/MEMSCAN` 等），再对每条命中做 **±60~200 B 上下文切片人工判定**（这是为了排除 `Friday` 这类假阳性）。

| 能力 | 结论 | 证据 | 性质 |
|---|---|---|---|
| **ptrace 反调试** | **仅 xcrash 系** | `libxcrash_dumper.so` import **`ptrace`**，另有 `/proc/%d/maps`；`libxcrash.so` import `prctl clone execl waitpid` + 串 `set dumpable failed` / `set traceable failed` | **不是反调试，而是调试器**——它 ptrace attach 自己父进程抓 tombstone（合法的崩溃捕获设计）。**其余 10 个库均不 import `ptrace`**〔观测〕 |
| `TracerPid` / `/proc/self/status` | **无** | 全 12 库正则 `TracerPid\|/proc/self/status` **0 命中** | — |
| **Frida / Xposed 检测** | **无（两处都是假阳性）** | `libams` 命中 `Frida` → 上下文为 `…Thu…Fri…Sat…Sunday…Monday…Tuesday…Wednesday…Thursday…Friday…Saturday…`，实为 `Fri`+`day`；`libavlurl` 命中 `frida` → 上下文 `…blackfriday……`；命中 `xposed` → `…expert..exposed.extraspace…` | **必须澄清：没有任何库真的做 Frida/Xposed 检测** |
| **Root 检测** | **`libturingvd.so` 真实存在** | 串 `/system/bin/su`、`/system/xbin/su`、`daemonsu` + 进程名正则 `^/system/xbin/ku[.]sud$\|^daemonsu:\|^kworker/[1-9][[:digit:]]{2}$\|^permmgrd$\|^360sguard$`；import `getuid readlink statfs popen` | 〔观测〕**真实的 root/守护进程扫描**，且用 `popen` 跑 shell |
| 同上（假阳性） | `libxcrash(.dumper)` 也命中 `/system/xbin/su` `/system/bin/su` `/su/bin` | 〔推断〕来自 xcrash 收集「设备 root 信息」写入 tombstone 的路径表，非主动检测（xcrash 上游确有该清单） | 低危 |
| **模拟器/VM 检测** | **`libturingvd.so` 真实存在** | import `ASensorManager_getInstance` `ASensorManager_getSensorList` `ASensor_getVendor/Name/Type/Resolution`（传感器指纹）+ `statfs` + `readlink` + `uname`? + 串 `brand` `model` `Hardware` `model name` `apiLevel` | 〔观测〕传感器列表是判定模拟器/云手机的强特征 |
| 文本型 emulator 串（`goldfish/qemu/ranchu/genymotion`） | 12 库 **0 命中** | — | 〔推断〕Turing 用**行为式**（传感器缺失/异常）而非字符串匹配 |
| **反虚拟机** | **未发现** 明确 `VMware/VBox` 检查 | 12 库 0 命中 | — |
| **内存扫描 / 进程注入** | **未发现** `process_vm_readv`、`/proc/<pid>/mem` 写入、`PTRACE_POKETEXT` | 12 库对 `process_vm_readv\|/proc/%d/mem` **0 命中**；`libxcrash_dumper` 只读 `/proc/%d/maps` | — |
| **DEX 解密 / 动态加载** | `libavlasys` 有 `dlopen/dlsym`；`liburldetectorsys` 纯 `dlopen`；`libams/libdce/libavlm/libavlurl` 也有 `dlopen` | 〔观测〕全部用于装载**同 APK 内的兄弟引擎库**（`libavlm.so` / `liburl.so`），无「解密 dex → `DexClassLoader`」路径 | 低危 |
| **字符串解密表** | **`libavlm.so` 明确有** | `.rodata` 出现连体 blob：`http://.https://.Content-Length:.www.baidu.com.GET` 与 `…SMS_Send.SMS_Block.System_DexLoad.System_ReflectInvoke.System_ReflectGet.Notification…`；且多个 `AVLM_*` 导出 `size=4` 的别名跳板（`aesd/adce/effe/efff/fffe/exdz/wtsr/haoz/zavt/brmy`） | 〔观测〕**定长字符串表 + 别名导出**，是 AVL 一贯的混淆风格（非加壳） |
| 同上 | `libTmsdk` 的**字符串/类名加密**在 Java 侧被大量使用 | `tmsdkobf/gb.java:490,527,536` 用 `TccCryptor.decrypt(byte[])` 还原类名；`tmsdkobf/ib.java:68,409`、`tmsdkobf/m6.java:22`、`tmsdkobf/qa.java:48` 同类 | 〔推断〕`libTmsdk` 内含 TccCryptor 的**算法 + 内置密钥**，是 TMSDK 的全局字符串解密表 |
| **网络请求（库内硬编码域名）** | 见 §5.1 / §5.2 | `libavlm`：`www.baidu.com`、NTP pool、`www.openssl.org`、`www.w3.org`、`www.wapforum.org`、`openssl.cn`；`libavlurl`：无 C2（见 §5.2）；`libturingvd`/`libflamingo`/`libtmf-gm`/`libTmsdk`/`libdce`/`libams`：**无硬编码域名**（只命中 `android.googlesource.com` 工具链路径、`bzip.org`、`www.memtest86.com`） | 〔观测〕 |
| **出网能力** | `libavlm`、`libavlurl`、`libavlasys` 均 import `socket/connect/send/recv/gethostbyname/select`；`libturingvd` **不 import socket**（走 Java 网络） | 〔观测〕import 表 | 见 §6-R1 |
| **执行外部程序** | `libturingvd`：`popen/pclose`；`libxcrash`：`execl/popen`；`libxcrash_dumper`：`popen`；`libams`：`open/read/write`（无 exec） | 〔观测〕 | 见 §6-R4 |
| **assets 里藏可执行体** | **`assets/mdetector` = ARM32 PIE + `/system/bin/linker`**（§2.3.3） | 〔观测〕ELF 头 + `PT_INTERP` + 静态 libpcap 符号 | **高危**，见 §6-R2 |

---

## 5. 配置项 / 常量 / 协议字段

### 5.1 `libavlm.so` 明文域名 / URL（穷举，去重）

来源：`libavlurl` 之外的 `DOMAINS`/`URLS` 正则 + `SVC` 正则，全部命中都做了上下文核验。

| 值 | 上下文 | 判定 |
|---|---|---|
| `https://` / `http://`（单独出现） | 与 `Content-Length:` `GET` `HEAD` `HTTP/1.1` 同区 | HTTP 客户端实现的字面量，非具体端点 |
| `www.baidu.com` | 与 `http://` `https://` `Content-Length:` `GET` 串联 | 〔推断〕连通性探测/白名单 |
| `0.north-america.pool.ntp.org`<br>`1.…` `2.…` `3.…` | 连体 blob | 〔推断〕NTP 校时（用于授权到期/证书有效期判断，与 `key is overdue!` 呼应） |
| `www.w3.org`、`www.wapforum.org` | 命名空间 | XML/WAP 解析的 DTD 命名空间，非网络端点 |
| `www.openssl.org`、`openssl.cn`、`http://www.openssl.org/support/faq.html` | OpenSSL 内置错误/FAQ 串 | **OpenSSL 静态链接的固有字符串**，非通信端点 |
| `liburl.so`、`%s/conf/avllib.conf`、`%s.new`、`%stmp/`、`%s.tmp/` | `libavlurl` | 本地文件路径模板 |

〔观测〕**`libavlm` / `libavlurl` 都没有硬编码的 C2 / 上报 URL**。它们只是「引擎」，网络行为由 Java 层的 `https://api.sec.miui.com/...`、`https://virus-engine.sec.miui.com/...`（简报已列）驱动更新包下载。

### 5.2 `libavlurl.so` 的 400 条域名（**不是 C2**）

〔观测〕对 `libavlurl.so` 跑域名正则得到 400 条（达到我设的 400 上限，实际更多），内容为：
`dyndns.org` 全家族（`dyndns-at-home.com`、`from-ak.com`…`from-wy.com`、`is-a-chef.com`…、`webhop.net`…）、
`eu.org` 全部二级（`cn.eu.org`、`us.eu.org`…）、
云/SaaS：`s3.amazonaws.com` 全家、`cloudfront.net`、`elasticbeanstalk.com`、`appspot.com`、`azurewebsites.net`、`herokuapp.com`、`cloudfunctions.net`、`fastly.net`、`cdn77.org`、`firebaseapp.com`、`blogspot.com`、`googleapis.com`、`rhcloud.com`、`sinaapp.com`、`1kapp.com`、`duckdns.org`、`dynv6.net`，
以及 `edu.cn gov.cn net.cn org.cn mil.cn`。

〔观测〕这 400 条与 `url/url/white.avl`(370,316 B)、`url/url/fish.avl`(150,768 B) 同库，且它们**没有一个是中国大陆的 API 域名**。
〔推断〕这是 **Public Suffix List 子集 + 免费二级域名托管商清单**，用于 URL 信誉判定时**识别「攻击者可在公共后缀下免费注册子域」**（诈骗站点高发区），属于**防御性白/灰名单**，不是回连地址。
〔观测〕`libavlurl` 中 `frida`/`xposed` 的命中经上下文核验为 `blackfriday` / `exposed`（同一份英文词表），**假阳性**。

### 5.3 常量汇总表

| 常量 | 值 | 出处 |
|---|---|---|
| AV 引擎版本 | `351` | `avlsdk:sdk_conf/version.conf`（明文 JSON） |
| AV 特征库版本 | `20220928.ah` | 同上 + `avlsdk:av/avl/conf/avllib.conf`（明文） |
| URL 引擎版本 | `124` | 同上 |
| URL 特征库版本 | `20180917.zz` | 同上 + `avlsdk:url/url/conf/avllib.conf`（明文） |
| `sdk.conf` 版本 | `"1"` | 同上 |
| AMF 容器魔数 | `"amf\0"` + `u32=4` | `assets/70003.amf` |
| AMF 头扩展 | `0x18..0x1F` 双 u32 时间戳（`0x5FF6D2BD`/`0x622575E6`） | 同上 |
| AVL `.avl` 魔数 | `41 56 4C` = `"AVL"` | `avlsdk` 内 5 个 `.avl` |
| ART profile 魔数 | `"pro\0"+"010\0"` / `"prm\0"+"002\0"` | `assets/dexopt/baseline.prof{,m}` |
| xcrash tombstone 名 | `%s/tombstone_%020lu_%s__%s%s` | `libxcrash.so` |
| xcrash dumper 定位 | `%s/libxcrash_dumper.so` | 同上 |
| Turing 版本串 | `TuringFD v%d (%s, %s, %s, compiled %s)` / `vd.5e7c85b` | `libturingvd.so` |
| TMSDK `.amf` 编号 | `70002`(EN 库) `70003`(病毒库) `70701`(payload) | `UpdateConfig.java:197-202` |
| TMSDK `.dat` 编号 | `40706`(证书 MD5 白名单) `100003`(AMS) `40685/40822`(TRP) | `UpdateConfig.java:203-229` |
| NDK 版本 | `libavlm`=clang 3.8.256229；`libtmf-gm`=clang 5.0.300080；`libflamingo`=r25b/8937393；`libturingvd`=r20/5594570；`libxcrash*`=r16b/4479499 | `.comment` / `.note.android.ident` |

---

## 6. 权限与攻击面 / 风险点

| # | 风险 | 位置 | 观测/推断 | 影响 |
|---|---|---|---|---|
| **R1** | **三个 native 库具备完整出网能力**，且不经 Java 网络栈 | `libavlm.so`、`libavlurl.so`、`libavlasys.so` 的 import：`socket/connect/send/sendto/recv/recvfrom/gethostbyname/inet_addr/select` | 〔观测〕import 表 | 引擎可绕过 `NetworkSecurityPolicy`/`StrictMode`/代理设置**直连任意 IP**；`inet_addr` 说明可直填 IP 常量。因 Host 是 `com.miui.guardprovider`（`system\|signature` 权限、system app），其进程本身已持 `INTERNET`，故实际可利用性取决于**引擎会不会用**（本轮未见硬编码 C2，见 §5.1）。 |
| **R2** | **`assets/mdetector` 是一个 ARM32 可执行 ELF（ARP 网关劫持检测器）**，不是数据文件 | §2.3.3 | 〔观测〕`\x7fELF`+`ET_DYN`+`EM_ARM`+`PT_INTERP=/system/bin/linker`+静态 libpcap + `!!This Device is being Hijacked!!` + `send msg to java:` | ① **asset 藏可执行体**，绕过 `lib/` 目录对 PackageManager/安全软件的可见性；② 功能是**被动嗅探 ARP 判定中间人劫持**（需要 `CAP_NET_RAW` 级能力，但因为它跑在 `com.miui.guardprovider` 这个 system 权限进程的子进程里，继承了该进程的 SELinux 域）；③ 32 位与 `arm64-v8a only` 矛盾 → 纯 64 位机型上无法 exec，功能静默失效。〔未知〕释放/exec 代码位置见 §7-U2。 |
| **R3** | **SM2 随机数可能用 `rand()`/`srand(time())`** | `libtmf-gm.so` 的 import：`rand` `srand` `time`（无 `getrandom`/`/dev/urandom`） | 〔观测〕import 表 | SM2 加密的 `k` 若来自 `rand()`，则**同一秒内可预测 → 可恢复明文甚至私钥**。这是国密实现里最经典的高危缺陷。注：`libflamingo` 反而正确 import 了 `getrandom`。〔未知〕`rand` 是否真用于 SM2 的 `k`（需反编译确认，本机不可用）。 |
| **R4** | **`libturingvd` 用 `popen` 执行 shell 命令**并读传感器做设备指纹 | §2.2.9 | 〔观测〕import `popen/pclose` + `ASensor*` | 命令字符串来自 native 内部（未见明文命令串），但 `popen` 让 native 层具备**任意 shell 执行原语**；一旦被劫持（如 `PATH`/`LD_*` 污染）即可提权到 shell。 |
| **R5** | **URL 特征库落后 4 年**（`20180917.zz` vs AV 库 `20220928.ah`） | `avlsdk:url/url/conf/avllib.conf` | 〔观测〕明文 | URL 钓鱼/诈骗判定用的 2018 年库，对 2019 年后新域名几乎无覆盖；与 AV 引擎的更新节奏脱节。 |
| **R6** | **`libavlurl.so` 实际文件名与 SONAME（`liburl.so`）不一致**，且 `liburldetectorsys` 按 `liburl.so` dlopen | §2.2.4 / §2.2.10 | 〔观测〕 | 若 `setLibPath()` 未被调用，`dlopen("liburl.so")` 在 Android 上**不会**自动匹配 `libavlurl.so`（SONAME 不参与文件名查找）→ URL 引擎整体失效、静默降级。属**功能缺陷**而非安全问题。 |
| **R7** | **`libTmsdk` 文件名 2.0.13 / SONAME 2.0.12** | §2.2.8 | 〔观测〕 | 版本错配，可能导致依赖 2.0.13 行为的调用方拿到 2.0.12 实现。 |
| **R8** | **licence 公钥证书 2012 年即过期**且与随包 licence 文件不匹配 | §2.3.5 | 〔观测〕RSA 去填充失败 + 证书 `NotAfter 2012-02-24` | 授权校验链在本包内**不可能通过**（`f5.f()` 会抛 `RuntimeException("loadLicence Invaild LicenceFile!")`）→ TMSDK 授权功能实际处于**未启用/降级**状态。 |
| **R9** | **TMSDK 字符串/类名加密密钥内置在 native** | `libTmsdk` 的 `TccCryptor.decrypt(b,null)` | 〔观测〕Java 侧大量以此解类名（`gb.java:490,527,536`） | 「加密」只增加逆向成本，**不是安全边界**：拿到 `libTmsdk` 即可批量解密全部混淆字符串。 |
| **R10** | **`libdce` 具备批量删除任意路径的原语** | §2.2.5 的 `nativeDeleteAllChildren`/`nativeDeleteAllChildrenByDay`/`nativeRemoveEmptyDir` | 〔观测〕注册表签名 | 若路径参数可控（来自 IPC `guard/sync_scan` 的 `selectionArgs=[path,…]`，见简报 §IPC），存在**越权删文件**面。需 Java 侧校验；本轮未审 Java 校验逻辑。 |
| **R11** | **`libavlasys` 硬编码 `62BAFD…739`** | §4.3.4 | 〔观测〕 | 若该串是授权 key，则任何人可用它伪造 licence 调用 AVL 引擎。 |

---

## 7. 未解决问题

| # | 问题 | 现状 | 下一步建议 |
|---|---|---|---|
| **U0** | **REA 无法在本机反编译 ELF** | 〔观测〕`analyze_function` 返回 `provider_unavailable`：`ghidra → target_format_unsupported ("Windows Ghidra P0 accepts native PE and explicitly selected raw 6502 targets only")`；`hopper → unsupported_host (win32)`；`ida → not_configured`；`kuna → not_configured`。**这是硬限制，不是超时** | ① 在 Linux/macOS 上跑 REA；或②配置 `REA_KUNA_COMMAND`/`REA_IDA_MCP_CONFIG`；或③装 `llvm-objdump`/`rizin` 做离线反汇编。本报告已用「自建 ADRP+ADD 解码」部分替代。 |
| **U1** | `libtmg-gm.so` 不存在 | 任务清单给了 `libtmg-gm.so`/`libtmf-gm.so` 两个名字，实际只有 `libtmf-gm.so`；Java 侧 `com/tencent/tmf/gm/a.java:26` 也只 `loadLibrary("tmf-gm")` | 判定为清单笔误，无需进一步动作 |
| **U2** | **`assets/mdetector` 的「释放 + exec」具体代码未定位**（已大幅收窄） | 已确认：它是 ARP 网关劫持检测器、有 `send msg to java:` 回传协议、32 位 ARM。<br>已定位 3 处候选 exec 点：`com/avl/engine/j/b/e.smali:217,383`（`Runtime.exec(String)`）、`tmsdkobf/t9.smali:563`；已定位 ABI 归一化器 `com/avl/engine/b/b/d.smali`。<br>但 grep `mdetector` 在 `sources\` 与 `smali\` **仍 0 命中** → 文件名是运行时拼的 | ① 反编译 `tmsdkobf.t9` / `com.avl.engine.j.b.e` 看 exec 的命令串是否由 `UpdateConfig` 的 ID 拼接；② 在 smali 里搜 `"mdet"` 片段与 `"send msg to java"`（若 Java 侧解析该前缀，必然出现字面量）；③ 查 `tmsdkobf.ka` 的库释放目录 `/data/data/<pkg>/lib/` |
| **U3** | **`licence.conf`/`licence1.conf` 的 RSA 公钥未知**；`avlsdk` 内 `liscense.conf` 的算法/key 未知 | `licence.conf`/`licence1.conf`：结构 `[128B RSA][8B 对齐 DES-ECB]` 由源码 + 长度整除双证，但用 `f5.java:52` 的内置公钥做公钥运算后 PKCS#1 去填充失败（`m` 首字节 `0x08` 而非 `0x00 0x01`/`0x00 0x02`），121 偏移滑窗 DES 试解最高可打印率仅 0.447。<br>`avlsdk` 内 `liscense.conf`(404B)：结构 `[400B 16 字节分组密文][u32=1]` 已定，算法/密钥未定 | ① 在 `libTmsdk` 的 `.rodata`/`.data` 里搜第二把 RSA 模数（128B 高熵常量）；② 若目标是「读懂授权内容」，更快的路径是 hook `f5.f()` 的 `Cipher.doFinal` 返回；③ `liscense.conf` 可尝试用 SM4-ECB/CBC + `62BAFD662A63A99A7C96DA027AFDF739`（16B）或 `D5DFB878…8EC8`（16B）做 25 块试解 |
| **U4** | `liburldetectorsys` 按 `liburl.so` dlopen，但磁盘上是 `libavlurl.so` | 〔观测〕串 `liburl.so` + SONAME `liburl.so`；`setLibPath(String)I` 是 JNI 暴露的补救接口 | 查 `com/avl/engine/urldetector/UrlDetector.java` 是否在 `loadURL()` 前调用 `setLibPath(真实路径)` |
| **U5** | `com.tencent.tcuser.util.Encode` 的 `loadLibrary("xy")` 无对应库 | `Encode.java:43,48` 在 `try/catch` 里，失败只 `pa.a(...,"System.loadLibrary error",null)` 上报 | 判定为**死代码/降级分支**；若要确认，查 `Encode` 的 native 方法是否有调用点 |
| **U6** | SM2/SM4 在协议中的具体位置、IV 来源 | 仅确认「算法绑定 + 密钥全部外置」，未见明文 IV/密钥 | 读 `com/tencent/tmf.*` 的上层调用（`com.tencent.tmf.gm.b.java` 的调用方） |
| **U7** | `70003.amf` 的加密算法与分组索引结构 | 已定：`"amf\0"`+ver4+16 零+双 u32 时间戳+42,556×16B 密文；无重复块（非 ECB 同文块） | 用 `libams` 的 `nativeLoadAmfHeaderBytes`(0x1c514) 做动态 hook，或尝试 `!@#$%^&*()_JHGFD` 做表名 XOR 后用 `nativeUpdateMalwareInfoBytes` 观察 |
| **U8** | `40706.dat` 的明文（证书 MD5 白名单条目数/内容） | 已定语义（`CERTMD5_WHITELIST`）+ 加密者（`TccCryptor`）+ 8 字节对齐 | 在真实设备上 hook `TccCryptor.decrypt` 拿明文；或静态还原 `libTmsdk` 里 `decrypt` 的内置 key |
| **U9** | `AVLA` 的 9 个混淆短名方法（`gufbyud/icf/sjcf/suhms/suf/sfcs/sfcp/nec/gcv/geci/gsi`）语义 | 名字与签名不在 `.rodata` 串表中，只能靠 Java 调用点推测 | 需要反编译 `libavlasys`（U0 未解除）或动态 trace |

| **U10** | **`avlsdk:sdk_conf/sdk.conf` 的 9,999 B 密文段算法/密钥** | 结构已定（§2.3.1）：`"3030303030"` + hex 摘要 + base64(253 B) + `"@"` + `0x17C` 起 9,999 B 高熵。<br>**已排除**：纯 AES/SM4-ECB/CBC（9,999 非 16 的倍数）、AES-128 `wcw1xbplx7aijwqr`（长度即否）。 | ① 试 `[u32 len]` 前缀偏移（从 0x17C+4 起取整块）；② 试流式：RC4 / ChaCha20 / TccCryptor（`libTccCryptor.decrypt` 支持任意长度），key 用 `62BAFD662A63A99A7C96DA027AFDF739`(16B)、`D5DFB878335A3EF4E76F60ED470B8EC8`(16B)、`wcw1xbplx7aijwqr`(16B)；③ 若解出应为文本配置，可用「明文含 `http`/`=`/`{}`」做 crib 校验；④ 更快的路：hook `libavlasys` 里读 `sdk.conf` 的函数 |

**未解决问题数：11（U0–U10）。**

---

## 8. REA 证据 ID 列表

| # | 操作 | 目标 | evidence_id | 结论用途 |
|---|---|---|---|---|
| 1 | `inspect_native_exports` (detail=summary) | `libavlm.so` | `ev_e195c9e10f580a383ae2efe99b1d9bbaf90c48a2032af92a4566700e5abafa51` | 复核 **98 个导出**（`examined_entries 248`, `coverage complete`）；§2.2.3 / §6 用 |
| 2 | 同上 | `libavlurl.so` | `ev_7db963e1f608d111e8de5e0e93844bb1820c91a6977ef11885c703531f2d1cf5` | 复核 **33 个导出**；§2.2.4 用 |
| 3 | 同上 | `libTmsdk-2.0.13-mfr.so` | `ev_ec4f7f1d4c232e2514e8c91727e4ba604594e57e6fc83c4d6511e84d10d6c3bf` | 复核 **仅 5 个导出**（含唯一 `JNI_OnLoad`）；§2.2.8 用 |
| 4 | 同上 | `libflamingo.so` | `ev_b6d546cfcab1ea7265b0a871d2987301484544afe5098b3cf16b2319012f490d` | 复核 **恰好 3 个导出**，与 `RustSupport` 3 个 native 方法一一对应；§4.1.1 / §4.3.5 用 |
| — | `open_binary` | `libavlasys.so` | sha256 `8fec8cf73748963141206ca29cd0fc15d54ff43129af573ed9e5eab6bfe12ff0`（打开成功，后续 `analyze_function` 因 U0 失败） | §7-U0 |

〔观测〕上述 4 条导出证据的 `authority = shipped-artifact`、`confidence = derived`，限制条件（3 条）为工具自带：
「hidden symbol version 不是 dlsym 默认版本」「只列 default/protected 可见性的 global/weak/unique 符号」「符号表不等于可达性/可调用性」。

---

## 附：本报告使用的自建取证工具（均可复现）

| 脚本 | 作用 |
|---|---|
| `_work_tnative\elf.ps1` | ELF64 头/节/段/`.dynamic`/dynsym/notes 解析（纯 .NET，无第三方依赖） |
| `_work_tnative\scan2.ps1` | 12 库 × 17 组正则的 Latin1 全字节取证（`URLS/DOMAINS/ANTIDBG/ROOT/EMU/JNI_*/ASSETS/MODLOAD/CRYPTO/PEM/SVC/SHELL/PROC`） |
| `_work_tnative\ctx.ps1` | 命中点 ±N 字节上下文切片，用于**排除假阳性**（`Friday`/`blackfriday`/`exposed`） |
| `_work_tnative\refs.ps1` | AArch64 `ADRP+ADD` 指令解码 → 「函数 → 引用字符串」归属（替代 `analyze_function`） |
| `_work_tnative\lic.ps1` | RSA-1024 公钥运算 + DES-ECB + PKCS#1 去填充 + 8 字节滑窗试解 |

