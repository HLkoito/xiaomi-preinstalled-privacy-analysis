> **历史分析存档，非最终结论。** 本文保留原始分析的技术内容，可能包含尚未验证或已被纠正的表述。隐私数据上传范围、同意条件、FBO 本地委托、报警短信加密、Provider 鉴权等，以 [最终报告](../REPORT.md) 与 [独立复核](../REVIEW.md) 为准。文中源码定位保留为原反编译相对路径；只有 evidence/ 中列出的精选文件随包提供，未打包完整源码树。

# MIUISecurityCenter native 层 (.so) 逆向调查

> 目标：`MIUISecurityCenter-10.4.9-250402.0.1.apk`（SHA-256 `fc63de0e...c678cfd`）
> 范围：`_analysis/apk_full/lib/arm64-v8a/` 18 个 .so，`armeabi-v7a/` 同名 18 个作对照
> 依据基线：`findings/_BASELINE.md`
> 标注约定：**[观测]** = shipped artifact 直接读出的事实；**[推断]** = 基于观测的合理推论；**[未知]** = 无法用当前证据确定

---

## 0. 方法与工具边界（先说清楚，避免误读结论）

| 工具 | 状态 | 证据 |
|---|---|---|
| REA `open_binary` + `provider_id=ghidra` | **不可用于本目标** | 返回 `target_format_unsupported`：“Windows Ghidra P0 accepts native PE and explicitly selected raw 6502 targets only.” |
| Hopper / IDA / Kuna | 不可用 | `unsupported_host`（win32）/ `not_configured` |
| `inspect_with_objdump`（GNU objdump） | 不可用 | `Could not launch ... "objdump" (ERR_REA_WINDOWS_NATIVE)`，PATH 上无 objdump/readelf/nm/strings |
| `execute_rizin_command` | 不可用 | `Could not launch ... "rizin" (ERR_REA_WINDOWS_NATIVE)` |
| Python / pip | 不可用 | `python` 是 WindowsApps 占位 stub（exit 9009），无 `py`/`pip`/`dotnet` |
| REA `inspect_native_exports` | **可用** | 便携式 ELF 动态符号表读取器（无引擎依赖） |
| Node.js v24.22.1 | **可用** | 本报告的自建分析工具全部用它实现 |

因此本报告没有"反编译伪代码"，而是用**自建静态分析器**给出**指令级可核对证据**：

| 自建工具（`_analysis/tools/`） | 作用 |
|---|---|
| `elfscan.mjs` | ELF64/ELF32 头、段表、`.dynsym` 导出/导入、`DT_NEEDED`/`DT_FLAGS`、全量可打印字符串、URL/域名抽取 |
| `strings_human.mjs` | 从全量字符串中过滤"人类可读"证据（剔除 C++ mangled 符号噪声） |
| `resolve_ptrs.mjs` | 用 `R_AARCH64_RELATIVE` 重定位把导出的 8 字节 OBJECT 符号**解析成它指向的字符串**（关键：解出 `WALI_KEY_SEG_*`、`U_URL`） |
| `dump_sym.mjs` | 按符号名 dump 任意长度的导出对象原始字节 |
| `a64scan.mjs` | **AArch64 局部反汇编器**：ELF 段/`.dynsym`/`.eh_frame_hdr` 函数表 + `ADRP/ADD/BL/B/CBZ/TBZ/RET/MOVZ/MOVK/LDR-literal/STP/LDR/STR` 解码 + **PLT→导入符号名解析** + 字符串/调用目标回填 |
| `censor_probe.mjs` | 对 `assets/censor_source` 做熵分析 + 从 AES 代码路径引用的常量中暴力尝试 AES-128/192/256-CBC/ECB 解密 |

> 复现：`node tools/a64scan.mjs "apk_full/lib/arm64-v8a/libtcp.so" --exact exec_protocol --max 60`
> `a64scan` 的函数边界取自 `.eh_frame_hdr`（libtcp 得到 336 个函数起点），PLT 名字取自 `.rela.plt` 顺序。
> **局限**：未做完整 CFG/数据流，ADRP/ADD 追踪限定在单函数线性窗口内；条件跳转不跟随。因此"某函数引用了某字符串"是可靠的，而"该字符串一定在运行时被使用"只到 **[推断]** 级。

---

## 1. 总览表

| # | 库 | 大小(B) | 架构 | stripped | 导出符号 | JNI/初始化入口 | NEEDED（去 libc/log/m） | Java 调用方 | 功能结论 |
|---|---|---|---|---|---|---|---|---|---|
| 1 | libc++_shared | 911,696 | arm64 | 是 | 2195 | 无 | — | （被其他 .so 依赖） | 标准 Android libc++ 运行库 |
| 2 | libfastplayer | 453,504 | arm64 | 是 | 968 | JNI_OnLoad/Unload + `register_FastPlayer` | libmiffmpeg, EGL/GLESv2/3, mediandk, jnigraphics, OpenSLES, android | `com.miui.fastplayer.FastPlayer` | 视频播放器（MediaCodec 硬解 + GL 渲染 + 毛玻璃滤镜） |
| 3 | libjni_load_serinum | 5,512 | arm64 | 是 | **1** | `Java_...LoadSeriNum_readOTP` | — | `com.miui.securitycenter.utils.LoadSeriNum` | 通过 `/dev/otp` + ioctl 读 MTK OTP |
| 4 | libjni_sound_effect | 270,160 | arm64 | 是 | 536 | JNI_OnLoad | — | `com.miui.gamebooster.encoder.SoundSupport`（[观测] 字符串） | 音效：限幅器 + 双二阶滤波 + 相位处理 |
| 5 | libjni_sound_effect_2_0 | 349,808 | arm64 | 是 | 405 | JNI_OnLoad | — | 同上 | **WORLD 声码器**（Dio/CheapTrick/D4C/Synthesis）= 变声/声音转换 |
| 6 | **liblib-riskcontrol** | 247,832 | arm64 | 是 | 455 | `SensitiveWordDetectProvider.setAssetManager` / `hasSensitiveWords` | libandroid（AAssetManager） | `com.miui.riskcontrol.SensitiveWordDetectProvider` | **敏感词检测**：AES 解密 assets 词典 → TrieTree → censor(文本) |
| 7 | libmicontinuity_sdk | 1,014,664 | arm64 | 是 | 45(~35 JNI) | JNI_OnLoad + `com.xiaomi.continuity.channel.*` | — | `com.xiaomi.continuity.jni.NObject` 等 | **小米互联数据通道**：socket/epoll + 文件/字节流 + 输入事件读取 |
| 8 | libmiffmpeg | 2,396,624 | arm64 | 是 | 2320 | 无 | libz | （被 fastplayer 依赖） | FFmpeg（`Lavf58.45.100`）完整音视频编解码 |
| 9 | libMNN | 1,555,208 | arm64 | 是 | 844 | 无 | — | （被 mnncore 依赖） | 阿里 MNN 推理引擎核心 |
| 10 | libmnncore | 170,016 | arm64 | 是 | 84 | `com.miui.mlkit.mobilerec.ranking.mnn.*`（26+ JNI） | libMNN, libMNNTrain, libMNN_Express, libc++_shared, jnigraphics | `MNNNetNative` / `MNNTrainNative` | 端侧推荐/排序模型封装，**含在线训练入口** |
| 11 | libMNNTrain | 1,079,728 | arm64 | 是 | 1552 | 无 | libMNN_Express, libMNN | — | MNN 训练框架（MNN::Train::*） |
| 12 | libMNN_Express | 641,024 | arm64 | 是 | 298 | 无 | libMNN | — | MNN Express 算子层 |
| 13 | libnap | 5,544 | arm64 | 是 | **1** | `NaTrafficStats.nativeGetUidIfaceStat` | — | `com.miui.networkassistant.traffic.statistic.NaTrafficStats` | 解析 `/proc/net/xt_qtaguid/stats` 按 UID 统计流量 |
| 14 | **libsec-common-utils** | 297,960 | arm64 | 是 | 521 | 6 个 `ConfigManager.get*Key` + `getSignature`/`getPackageName`/`getUid` | — | `com.miui.securitykey.ConfigManager` | **密钥派生中心 + 调用方包名/签名白名单校验**（含硬编码证书 SHA-256） |
| 15 | **libtcp** | 189,808 | arm64 | 是 | 393 | JNI_OnLoad + `registerNativeMethods` | — | `com.miui.sdk.tc.TcManager` | **网络助手流量插件**：HTTP 拉服务端指令(base64+JSON)、短信模板解析、AES 上报 |
| 16 | libtensorflowlite_jni | 1,700,448 | arm64 | 是 | 110 | 66 个 `Java_org_tensorflow_lite_*` | — | `org.tensorflow.lite.TensorFlowLite` | Google TFLite 标准运行时（NNAPI/XNNPACK delegate） |
| 17 | libxcrash | 84,184 | arm64 | 是 | 41 | JNI_OnLoad（`xc_test_*` 为测试导出） | libdl | `xcrash.NativeHandler` | 崩溃捕获（xCrash）Java 侧桥 + root 路径探测 |
| 18 | libxcrash_dumper | 125,168 | arm64 | 是 | **7** | 无（**不是 JNI 库**） | — | 无（由 libxcrash 拉起） | 崩溃现场 dump：ptrace + `/proc/self/maps` + LZMA |

**全局事实 [观测]**
- 18/18 全部 **stripped**（无 `.symtab`），只保留 `.dynsym`。
- 18/18 均带 `DF_BIND_NOW`/`DF_1_NOW`（`DT_FLAGS=8` 或 `DT_FLAGS_1=1`；`libxcrash_dumper` 为 `134217729 = 0x8000001`，即 NOW|PIE）。
- **全 18 个库只有 1 个硬编码网络端点**（libtcp 的 `net-assistant.10046.xiaomimobile.com`，见 §3.1）。其余 `http(s)://` 命中全是编译器/库自带字符串（`android.googlesource.com`、`streams.videolan.org`、`tensorflow.org`）。
- **未发现任何反调试/反注入字符串 [观测·有界否定]**：对 18 个库全量字符串 grep `TracerPid|frida|xposed|magisk|ro.debuggable|anti_?debug|isDebuggerConnected|substrate|gdbserver` → 0 命中；`ptrace` 仅出现在 `libxcrash_dumper`（`THREAD: ptrace GETREGSET failed`、`UTIL: ptrace error, addr:%lx`），属崩溃 dump 而非反调试。
  → 结论：**native 层未见主动反调试/反 Frida 逻辑**；加固由 Java 侧/系统负责（不在本次范围）。
- **armeabi-v7a 对照 [观测]**：同为 18 个库、同 soname 集合、同 JNI 入口集合（`a64scan` 不适用 v7a，但 `elfscan` 的 `.dynsym` 结果一致），体积约为 arm64 的 55–80%；libtcp 的 `net-assistant.../directives` 端点在 v7a 中同样存在。→ 两架构同源同功能，无架构特有隐藏逻辑的证据。

---

## 2. 逐库结论与证据

### 2.1 libc++_shared.so
- **[观测]** 911,696 B；stripped；2195 导出，全部为 libc++ ABI 符号（`_ZNKSt6__ndk1...`、`_ZSt9terminatev` 等）；`NEEDED=[]`（无依赖）；导入 `__cxa_*`、`pthread_*`、`localeconv` 等；唯一 URL 是 LLVM 编译串。
- **[观测]** 与 APK 内其他库不同，**arm64 版本无 `libc.so` 依赖**（v7a 版本反而 `NEEDED=[libc.so, libdl.so]`）。
- **结论**：标准 NDK `libc++_shared.so`，无业务逻辑、无敏感能力。

### 2.2 libfastplayer.so
- **[观测]** 453,504 B；968 导出；`JNI_OnLoad(420)` + `JNI_OnUnload(36)` + `_Z19register_FastPlayerP7_JNIEnv`（→ `RegisterNatives` 动态注册，无 `Java_*` 静态导出）。
- **[观测]** 导入里含 `AMediaCodec_*`、`AMediaFormat_*`、`ANativeWindow_*`、`SL_IID_*`（OpenSL ES）、`av_*`（来自 libmiffmpeg）、`av_jni_set_java_vm`。**导入表中没有 `socket`/`connect`/`gethostbyname`**。
- **[观测]** 关键导出：`fastplayer::FFmpegDecoder::{OpenCodec,SendPacket,GetFrame}`、`fastplayer::MediaCodecNdk::{InitMediaCodec,h264_set_extradata,hevc_set_extradata,AVFrame4MediaCodec,FillData4NV12,FillData4YUV420P}`、`fastplayer::ANWDisplay::{swscaleFrame,renderPicture}`、`fastplayer::OpenSLRender::*`、`fastplayer::RingBuffer::*`、`fastplayer::MetadataRetrieverWrapper::*`。
- **[观测]** 特效类导出（证据最直观）：`setBlurEffectUniform`、`setPoorBlurEffectUniform`、`setMoruGlassEffectUniform`、`setMoruPrismGlassEffectUniform`、`setRectGlassEffectUniform`、`setBrightnessEffectUniform`、`setShaderSource`/`addShaderBitmap`/`disableShader`。
- **[观测]** Java 侧：`jadxout/sources/com/miui/fastplayer/FastPlayer.java:247-248` → `System.loadLibrary("miffmpeg"); System.loadLibrary("fastplayer");`
- **结论**：MIUI 自研短视频/预览播放器。FFmpeg 解封装 + 平台 MediaCodec 硬解 + OpenGL(ANativeWindow) 渲染 + 毛玻璃/模糊滤镜叠加。**无独立网络能力**（网络在 FFmpeg 层，见 2.8）。

### 2.3 libjni_load_serinum.so ★（序列号/OTP）
- **[观测]** 5,512 B；**仅 1 个导出**：`Java_com_miui_securitycenter_utils_LoadSeriNum_readOTP`（436 B）。
- **[观测]** 导入仅 10 个：`open`、`ioctl`、`close`、`malloc`、`free`、`__android_log_print`、`__stack_chk_*`、`__cxa_*`。**没有 read/write**（数据交换只走 `ioctl`）。
- **[观测]** 字符串（全库仅 3 条人类可读）：
  - `/dev/otp`
  - `Error: Cannot open the /dev/otp `
  - `OTP: OTP operation error ! status = %d`
  - 编译串 `GCC: (GNU) 4.9 20140514 (mtk-20150408)` → **MediaTek 平台预编译**
- **[观测]** Java 侧：`jadxout/sources/com/miui/securitycenter/utils/LoadSeriNum.java:6` → `System.loadLibrary("jni_load_serinum")`
- **[推断]** 打开 `/dev/otp`（MTK 一次性可编程区）并用 `ioctl` 读取出厂写入的 OTP/序列号数据（MediaTek 的 OTP 区常存 device key / 校准数据 / 唯一 ID）。**这是设备唯一标识读取通道**，敏感性高于普通 `Build.SERIAL`。
- **[未知]** `ioctl` 的 request 号与返回结构（需要 AArch64 反编译器或真机 strace 才能确定；本机无可用反编译器，`a64scan` 对该函数只解到 `stp x29,x30` 序言 + `adrp x8,#0x2c; ldr x8,[x8,#0xe20]`（加载全局 GOT 槽）后即进入 `cbz` 分支，未能在 6 条指令内接触 `open` 的 PLT 调用）。

### 2.4 libjni_sound_effect.so
- **[观测]** 270,160 B；**soname 是 `libmisoundsupport.so`**（与文件名不同，说明是系统同名库的打包副本）。
- **[观测]** 536 导出；含 `CMusicLimiter_{Init,ProcessBuffer,ProcessBufferHardLimit,ProcessSample,Reset,Delete}`（音量硬限幅）、`BiquadClass::{biquadCoeffiCompute,audioProcess,biquadParamSet,...}`（双二阶 EQ）、`RingBuffer::*`、`_Z8princarg*`（相位展开）、`_Z6smoothPfS_ii`。
- **[观测]** 字符串 `com/miui/gamebooster/encoder/SoundSupport` → 与 `libjni_sound_effect_2_0` 共用同一个 Java 宿主类。
- **[观测]** 导入无 socket/HTTP；只有 `dl_iterate_phdr`、`pthread_*`、`sincos(f)`、`pow`、`expf`。
- **结论**：游戏加速器（Game Turbo）音效链的**限幅 + 均衡 + 相位处理**DSP。无网络、无权限相关能力。

### 2.5 libjni_sound_effect_2_0.so ★（变声）
- **[观测]** 349,808 B；405 导出。符号集合完全对应 **WORLD 声码器**（说话人转换/分析-合成）：
  `Dio`、`GetSamplesForDIO`、`InitializeDioOption`、`CheapTrick`、`GetFFTSizeForCheapTrick`、`GetF0FloorForCheapTrick`、`D4C`、`InitializeD4COption`、`CodeSpectralEnvelope`、`DecodeSpectralEnvelope`、`CodeAperiodicity`、`DecodeAperiodicity`、`DCCorrection`、`Synthesis2`、`InitializeSynthesizer`、`RefreshSynthesizer`、`NuttallWindow`、`LinearSmoothing`、`GetMinimumPhaseSpectrum`，以及 Ooura FFT（`rdft`、`cdft`、`bitrv2`、`cftbsub`、`makewt`、`makect`）。
- **[观测]** 同宿主类 `com/miui/gamebooster/encoder/SoundSupport`；导入无 socket。
- **[推断]** 这是**实时变声/音色转换**引擎（WORLD 是 Google 开源的高质量声码器，典型用途即 voice changer）。游戏语音场景下的隐私敏感点：可改变用户声纹特征。
- **[未知]** Java 侧哪条路径触发（`SoundSupport` 的哪个方法对应 `Synthesis2`）未做 Java 侧交叉验证。

### 2.6 liblib-riskcontrol.so ★★（敏感词检测，本次最高价值发现之一）
- **[观测]** 247,832 B；455 导出。JNI 仅 2 个：`Java_com_miui_riskcontrol_SensitiveWordDetectProvider_setAssetManager`（8 B，纯 setter）、`Java_com_miui_riskcontrol_SensitiveWordDetectProvider_hasSensitiveWords`（232 B）。
- **[观测]** 关键 C++ 导出（完整算法骨架，无需反编译即可读出）：
  - 词典结构：`TrieTree::{insert,insertBranch,find}`、`TreeNode::{insertChild,findChild}` → **字典树/AC 自动机式多模式串匹配**
  - 业务：`SensitiveWordDetector::{load(char*), censor(std::string&)}`、`SensitiveWordDetectCore::{censor, encryptToFile, decryptFromFile, readFileFromAsset}`
  - 密码学：整套自实现 AES —— `AES::{KeyExpansion,SubBytes,InvSubBytes,ShiftRows,InvShiftRows,MixColumns,InvMixColumns,AddRoundKey,Rcon,RotWord,SubWord,xtime,XorWords,XorBlocks,EncryptBlock,DecryptBlock}` + `AES::{Encrypt,Decrypt}{ECB,CBC,CFB}` 的 `Phj` 与 `std::vector<uint8_t>` 两种重载 + `AESKeyLength` 枚举
- **[观测]** 导入 `AAssetManager_open/read/getLength/close`（libandroid）→ 词典来自 **APK assets**。
- **[观测]** 字符串证据（全库人类可读字符串只有 62 条，全部列出关键项）：
  - `sensitiveWordDetectCore`（全局对象名）、`SensitiveWordDetect`（TAG）
  - `censor_source` ← **assets 内的文件名**
  - `/storage/emulated/0/censor_source` ← **调试/离线导入用的外部路径**（开发遗留，说明可被外部文件覆盖词典）
  - `AssetManager open fail!`、`AssetManager Error!`
  - `File has %d lines`、`File has no content`、`load file!`、`Load file tree f...`
  - `censor sentence `、`keyword and char size mismatch!!`
  - `CheckLength ERROR! len = %d, bl...`、`inLen must be multiple o...`、`needPadding %u char(s)`
- **[观测]** 交叉验证：APK 内确实存在 `assets/censor_source`，**2,688 B**；熵 **7.933 bits/byte**（接近 8.0 上限），2688 = 16 × 168（AES 块对齐）→ 高度符合 AES 密文/压缩数据，不是明文词表。
- **[观测]** `a64scan` 扫描 `decryptFromFile` → 引用 `"SensitiveWordDetect"`、`"needPadding %u char(s)"`，并 `bl plt:_ZN3AES10MixColumnsEPPh`；扫描 `readFileFromAsset` → 引用 `"censor_source"`，调用 `fopen`、`std::map` 的 `find/__find_equal`、`AES::MixColumns`。
- **[推断]** 数据流：`setAssetManager()` 保存 AssetManager → 首次 `hasSensitiveWords()` 触发 `SensitiveWordDetectCore::readFileFromAsset()` 读 `assets/censor_source` → `AES` 解密（CBC，PKCS5 补位，从 `needPadding` 日志推断）→ 按行切词 → `TrieTree::insert` 建树（`File has %d lines` 是逐行计数日志）→ 对入参文本 `censor()` 做多模式匹配，返回是否命中敏感词。
- **[未知]** AES 密钥与 IV 的来源。`censor_probe.mjs` 把 49 个 AES/censor 相关函数内 `ADRP+ADD` 引用的 24 个常量地址展开成 48 个 16/24/32 字节候选密钥，配合 5 种 IV、CBC/ECB 共尝试解密 `censor_source`，**全部因 PKCS5 补位校验失败而丢弃（有效解密 0 个）**。其中 2 个"高熵常量"经识别是 **AES S-box**（`0x2e934` = `637c777bf26b6fc53001672bfed7ab76`）与**逆 S-box**（`0x2f954` = `52096ad53036a538bf40a39e81f3d7fb`），不是密钥。→ 密钥应为**运行时派生**（可能由 Java 侧传入或与包名/签名派生）。
- **敏感度评估**：高。功能是**对任意字符串做本地敏感词命中判定**（典型调用面：短信、通知、聊天文本）。词典加密 + 调试路径残留说明其被刻意保护。

### 2.7 libmicontinuity_sdk.so ★（小米互联通道）
- **[观测]** 1,014,664 B；仅 45 个导出，其中约 35 个是 `Java_*`，命名空间有两组：
  - `Java_com_xiaomi_continuity_channel_NChannel_{send,send2,isActive,getChannelId,destroyChannel}`
  - `Java_com_xiaomi_continuity_channel_NChannelManager_{initialize,createChannel2,destroyChannel,registerChannelListener,unregisterChannelListener}`
  - `Java_com_xiaomi_continuity_channel_NPacket_{asBytes,fromBytes,asFile,asInputStream,fromInputStream,isReceived,isDiscarded}`
  - `Java_com_xiaomi_continuity_channel_NConnMgr{ChannelListener,SendResultCallback}_*`（含 `onRequestSocketPort`、`onServerRegisterStatus`、`onChannelConfirm`、`onResult`、`onProgress`）
  - `Java_com_xiaomi_continuity_jni_NInputReader_{read,read([BII),skip}`、`NRunnable_run`、`NObject_setClassLoader`
- **[观测]** 导入包含完整 socket 栈：`socket`、`connect`、`accept`、`bind`、`listen`、`recvfrom`、`send`、`select`、`poll`、`epoll_create/ctl/wait`、`getpeername`、`getsockname`、`inet_pton`、`inet_ntop`、`pipe`、`prctl`、`sem_*`、`getauxval`。
- **[观测]** 字符串（C++ RTTI/日志，直接暴露实现类名）：
  `micont::channel::ChannelImpl::Send`、`micont::channel::ChannelImplBySocket::SendFile`、`...::SendBytes`、`...::ContinueSend`、`ChannelImpl::New(const ::ChannelInfo&, IChannelListener*)`、`RtmSendRetErr: %d`、`RtmSendUnsupported!`、`invalid channel_id=%d (%s:%d)`、`[SendTask(%d, %d)]SendEof!`、`[SendTask(%d, %d)]SendDataFrameErrCb`、`/dev/urandom`。
- **[推断]** 小米互联（手机↔平板/PC/车机）的**双向数据通道 SDK**：`micont::channel` 抽象 + socket 实现（`Rtm` = 互联传输层），支持任意字节流与文件传输（带进度/取消/EOF 语义），并用 `/dev/urandom` 生成会话随机数。`NInputReader` 提供**输入事件读取**接口（跨设备输入/投屏类场景）。
- **[未知]** 通道加密与认证细节（`Rtm` 是否 TLS/自研加密）——字符串中未见 TLS 库导入（无 `SSL_*`、无 `libssl`），**倾向于自研/明文通道 [推断]**，但缺少密钥协商路径证据。

### 2.8 libmiffmpeg.so
- **[观测]** 2,396,624 B；2320 导出，全部 `av_*`/`ff_*` + `_Unwind_*`；`NEEDED=[libm, libz, liblog, libc, libdl]`；版本串 **`Lavf58.45.100`**（对应 FFmpeg 4.3 系列）。
- **[观测]** 源文件名字符串证明编译进了解码器集合：`libavcodec/h264dec.c`、`h264_slice.c`、`hevc_ps.c`、`aacdec_template.c`、`aacsbr.c`、`mediacodec_wrapper.c`、`libavformat/mov.c`、`mux.c`、`network.c`、`url.c`、`aviobuf.c`；另有 `libavcodec license: nonfree and unredistributable`（该构建声明了 nonfree 组件）。
- **[观测]** **导入了网络栈**：`socket`、`connect`、`accept`、`bind`、`listen`、`getaddrinfo`、`freeaddrinfo`、`getnameinfo`、`getsockopt` → FFmpeg 协议层（http/rtmp/tcp 等）可用。
- **结论**：完整 FFmpeg 静态库（无 JNI），被 `libfastplayer` 链接。**它是本 APK 中唯一具备通用网络客户端能力的第三方媒体库**；是否被用于非本地 URL 播放取决于上层 Java（[未知]）。

### 2.9–2.12 MNN 系列（libMNN / libmnncore / libMNNTrain / libMNN_Express）
- **[观测]** 依赖链：`libmnncore → {libMNN, libMNNTrain, libMNN_Express, libc++_shared}`；`libMNNTrain → {libMNN_Express, libMNN}`；`libMNN_Express → libMNN`。只有 `libmnncore` 有 JNI。
- **[观测]** `libMNN.so` 844 导出（`MNN::Interpreter/Session/Tensor/Backend/Runtime/GeometryComputer`…），`libMNNTrain.so` 1552 导出（`MNN::Train::{AppDataset,DataLoader,LrScheduler,ImageDataset,Transformer}`），`libMNN_Express.so` 298 导出（`MNN::Express::_Conv/_MatMul/_ReduceSum/...`）。
- **[观测]** `libmnncore.so` 的 JNI 全部落在 `com.miui.mlkit.mobilerec.ranking.mnn`：`MNNNetNative_{nativeCreateNetFromFile,nativeCreateSession,nativeRunSession,nativeRunSessionWithCallback,nativeReshapeSession,nativeConvertBitmapToTensor,nativeConvertBufferToTensor,nativeGetSessionInput/Output,nativeTensorGet*}`，以及 `MNNTrainNative_{nativeLoadMapFromFile,nativeRecLoadMapFromFile,nativeTrainModel,nativeGetModelPredictScore,nativeGetRecModelPredictScore}`。
- **[观测]** 函数体尺寸（`.dynsym` st_size）显示 `nativeTrainModel`=12,924 B、`nativeLoadMapFromFile`=17,532 B、`nativeRecLoadMapFromFile`=15,332 B —— **训练/载入逻辑是实打实的重量级实现**，不是空壳。
- **[观测]** `libMNN.so` 字符串含 `/proc/cpuinfo`（CPU 特性探测 → backend 选择）。`libMNNTrain` 含 `/dev/urandom`。
- **[观测]** Java 侧：`jadxout/sources/com/miui/mlkit/mobilerec/ranking/mnn/MNNNetNative.java:6-9` 依次 loadLibrary `MNN`/`MNN_Express`/`MNNTrain`/`mnncore`。
- **[推断]** MIUI「移动推荐/排序」模型在端侧推理，且带**端侧增量训练**能力（`nativeTrainModel` + `nativeLoadMapFromFile` 读训练样本 map）。对应 assets 里的 `model`(680 KB) 与 `rec_model_20230425.mnn`(810 KB)。
- **敏感度**：中。端侧推断本身不联网，但训练/样本 map 来自何处（是否含用户行为）[未知]。

### 2.13 libnap.so
- **[观测]** 5,544 B；soname **`na.so`**；仅 1 个导出 `Java_com_miui_networkassistant_traffic_statistic_NaTrafficStats_nativeGetUidIfaceStat`（664 B）。
- **[观测]** 导入只有 9 个：`fopen`、`fgets`、`sscanf`、`strcmp`、`fclose`、`__stack_chk_*`、`__cxa_*`。
- **[观测]** **全库唯一人类可读字符串**：`/proc/net/xt_qtaguid/stats`。
- **[推断]** 逐行解析内核 `xt_qtaguid` 统计文件，按 UID + 网络接口汇总收发字节数。`xt_qtaguid` 在 Android 9+ 已废弃（改为 eBPF），所以这是**旧版兼容路径**；在新系统上该函数很可能返回空/失败（[推断]，未见代码分支证据）。
- **敏感度**：低-中（读全局流量统计，非内容）。

### 2.14 libsec-common-utils.so ★★（密钥中心 + 签名白名单）
- **[观测]** 297,960 B；521 导出。JNI 6 个，全部在 `com.miui.securitykey.ConfigManager`：
  `getDecodeSecretKey`(36 B)、`getEncodeSecretKey`(36 B)、`getHttpKey`(36 B)、`getNetworkAssistantKey`(36 B)、`getRubbishSecurityKey`(1,036 B)、`getUuidKey`(2,036 B)。
- **[观测]** 另有 3 个关键 C 导出（符号名即语义）：
  `_Z12getSignatureP7_JNIEnvP8_jobject`（`getSignature(JNIEnv*, jobject)`）、`_Z14getPackageNameP7_JNIEnvP8_jobject`、`_Z6getUidP7_JNIEnvP8_jobject`。
- **[观测]** `a64scan` 扫 `getSignature` 的字符串引用，得到完整的 JNI 反射链：
  `"getPackageManager"` → `"()Landroid/content/pm/PackageManager;"` → `"getPackageInfo"` → `"(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;"` → `"getPackageName"` → `"()Ljava/lang/String;"`；
  配合字符串 `"[Landroid/content/pm/Signature;"`、`"signatures"`、`"sha256"`、`"com/miui/securitykey/EncryptionUtils"`。
- **[观测]** 硬编码的**白名单包名**（作为字符串存在于 .rodata）：
  `com.miui.securitycenter`、`com.miui.cleaner`、`com.miui.cleanmaster`、`com.miui.global.packageinstaller`。
- **[观测·重点] 硬编码证书 SHA-256 指纹**：
  `C8:A2:E9:BC:CF:59:7C:2F:B6:DC:66:BE:E2:93:FC:13:F2:FC:47:EC:77:BC:6B:2B:0D:52:C1:1F:51:19:2A:B8`
  引用点：`0x1ed0c`（`add x8, x8, #0x95f` → `0x1495f`），位于 `Java_..._getNetworkAssistantKey`(0x1ecb4) / `Java_..._getDecodeSecretKey`(0x1ec6c) 之后紧邻的**未导出函数**体内（两个导出函数各仅 36 B，只是跳板）。
- **[观测·交叉验证]** 本 APK 的真实签名证书 SHA-256（REA `inspect_apk_signature`，v1/v2/v3 三个 scheme 一致）=
  `c9009d01ebf9f5d0302bc71b2fe9aa9a47a432bba17308a3111b75d7b2149025`
  → 与硬编码指纹 `C8:A2:E9:...:2A:B8` **不匹配**。
- **[推断]** 机制：`getSignature()` 反射拿到**调用方包**的 `Signature[]` → SHA-256 → 与硬编码指纹/包名白名单比对，作为「谁能调用本库取密钥」的身份闸门。指纹不匹配本 APK 自身证书，说明它 pin 的是**另一把证书**（最可能是 MIUI 平台/系统签名或历史渠道证书）。这带来一个可验证的副作用假设：**任何用非该证书重签名的构建，`get*Key` 会失败**（[推断]，未在真机验证）。
- **[推断]** `get*Key` 系列是 APK 内多处对称加密的密钥来源（`getHttpKey` 对应 HTTP 参数加密、`getNetworkAssistantKey` 对应流量助手、`getRubbishSecurityKey` 对应垃圾清理规则解密、`getUuidKey` 对应设备 UUID 派生）。
- **[观测]** 无网络、无文件系统重操作（导入里没有 socket/fopen 目录遍历）。

### 2.15 libtcp.so ★★★（流量助手插件，本次最复杂的一个）
- **[观测]** 189,808 B；393 导出；`JNI_OnLoad(196)` + `JNI_OnUnload(20)` + `registerNativeMethods(548)`。
- **[观测]** Java 侧：`jadxout/sources/com/miui/sdk/tc/TcManager.java:116` → `System.loadLibrary("tcp")`。
- **[观测]** **唯一硬编码端点**（`U_URL`，已用重定位解析确认：`U_URL @0x2e610 -> 0xbaf6`）：
  ```
  https://net-assistant.10046.xiaomimobile.com/v1/traffic/directives
  ```
- **[观测]** 注册的 native 方法名（`RegisterNatives` 表，字符串与导出符号双证据）：
  `java_tc_plugin_init`、`java_tc_plugin_update`、`java_tc_plugin_update_by_tc_type`、`java_tc_plugin_get_result`、`java_tc_plugin_get_result_by_tc_type`、`java_tc_plugin_get_directions`、`java_tc_plugin_getprovincesmap`、`java_tc_plugin_getcitiesmap`、`java_tc_plugin_getcarriesmap`、`java_tc_plugin_getbrandsmap`、`java_tc_plugin_get_province_code_by_city_code`、`java_tc_plugin_get_tc_pref_value`、`java_tc_plugin_get_aes3`、`java_tc_plugin_get_aes3_iv_key`、`java_tc_plugin_setimsi`。
  日志串：`RegisterNatives successed for '%s' of '%s'` / `RegisterNatives failed for '%s' of '%s'` / `no method matched for '%s' of '%s'`。
- **[观测]** 内嵌组件（导出符号即清单）：libghttp（`ghttp_*` 30+ 个，UA 串 `libghttp/1.0`）、json-c（`json_tokener_*`、`json_object_*`、`lh_table_*`、`printbuf_*`）、MD5（`MD5Init/Update/Final/MD52Hex`）、base64（`b64_alphabet` 已验证为 `ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=`、`base64_encode/decode`）、ustring/vector/array_list/byte_buffer/pref_*（自研容器与配置）、`url_encode/decode`。
- **[观测] 指令执行链**：`exec_protocol`(0x20314) 的 `a64scan` 反汇编：
  ```
  0x2032c  movz x0, #0x1
  0x20330  movz x1, #0x30
  0x20334  bl   0x296f0     ; plt:base64_decode        ← 先做 base64 解码
  0x20344  str  x8, [x9,#0x0]                          ; 填进 0x30 字节结构体
  0x20364  bl   0x203b8     ; sub_203b8
  ...      sub_203b8 内：
  0x203f0  add  x0, x0, #0x2a5   ; =0xd2a5 "PluginRegister"
  0x203f4  bl   0x29930          ; plt:json_object_object_get
  0x203f8  mov  x2, x0
  0x20404  add  x1, x1, #0xbc8   ; =0xcbc8 "MsgType"
  0x20408  bl   0x29932          ; plt:json_object_get_string
  ```
  → **[观测+推断]** 服务端下发的 `directives` 载荷是 **base64(JSON)**：先 `base64_decode`，再用 json-c 取 `PluginRegister` / `MsgType` 等字段分派。
- **[观测] 短信模板解析**：`parse_sms_with_tpl_list`(0x14868) 的字符串/调用：
  字符串引用 `"KeyValues"`、`"MatchedTplIds"`；调用 `json_object_new_object`、`json_object_new_array`、`json_object_object_add`、`json_object_array_get_idx`、`ustring_create_u`、`ustring_splite`、`ustring_toLowerCase`、`ustring_toUperCase`、`vector_new`、`sub_141a4`。
  配套字段名字符串：`TplId`、`TplName`、`TplContent`、`TplType`、`TplList`、`MatchedTplIds`、`SplitChar`、`KeyValues`、`MsgType`、`CmdType`、`DataType`、`CommandList`、`UseCommands`、`SuccIds`、`SmsFilterNum`、`SmsSendNum`、`SmsManageNum`、`PhoneNumberPrefix`、`tplMatch`、`parse_sms`。
  → **[推断]** 用服务端下发的**模板列表**匹配收到的短信正文，命中后把模板 id 与提取出的键值对以 JSON 上报/执行命令。这是「运营商套餐/流量校正」业务的核心：靠解析运营商短信（含验证码类短信）判断套餐余量。
- **[观测] 设备/用户数据采集**：字符串 `collectUserData`、`getDeviceId`、`getSubscriberId`、`setImsi`、`ro.product.model`、`ro.product.manufacturer`、`ro.build.display.id`、`ro.build.version.release`、`ro.build.version.sdk`、`android/telephony/TelephonyManager`。
  上报参数拼接格式（直接可读）：`appVersion=%s&`、`miuiVersion=%s&`、`device=%s&`、`carrier=%s&`、`region=%s&`、`versionType=%s&`、`param=%s`、`dp=%s`。
- **[观测] 加密**：`AES/CBC/PKCS5Padding`、`javax/crypto/Cipher`、`javax/crypto/spec/SecretKeySpec`、`javax/crypto/spec/IvParameterSpec`、`(Ljava/lang/String;)Ljavax/crypto/Cipher;`、`doFinal`、`android/util/Base64`、`encodeToString`；常量 `0102030405060708`（经典硬编码 IV 形状）与 `0123456789abcdef`。
  `aes_encrypt_java`(0x28454, 132 B) 的引用只有 `"javax/crypto/spec/SecretKeySpec"` + `time` + `__android_log_print` → **[推断]** 纯 JNI 反射包装，真正加密在 Java `Cipher` 完成，密钥经 `java_tc_plugin_get_aes3` / `..._get_aes3_iv_key` 从 Java 侧取。
- **[观测·重点] 密钥分段表（重定位解析，逐条可核对）**：13 个导出的 `char*` 全局变量指向 .rodata，且命名带 `PROGUARD_` 后缀 —— 说明是**故意按 ProGuard 风格拆分/改名以防静态提取**的密钥片段：
  | 符号 | 值 |
  |---|---|
  | `WALI_KEY_SEG_1` | `OHZoOG1zeTJo`（base64→`8vh8msy2h`） |
  | `WALI_KEY_SEG_2` | `ZXc5d3FkeA`（base64→`ew9wqdx`） |
  | `WALI_KEY_SEG_3` | `zdGF0LnNlY3JldA` |
  | `WALI_KEY_SEG_PROGUARD_1` | `5oOz56` |
  | `WALI_KEY_SEG_PROGUARD_2` | `L77yM5L2g5aaI6LS15aeT77` |
  | `WALI_KEY_SEG_PROGUARD_3` | `yf5bCP5b+D5YWs5Y` |
  | `WALI_KEY_SEG_PROGUARD_4` | `LW36K+J5L2g5ZOm77yM5LiN` |
  | `WALI_KEY_SEG_PROGUARD_5` | `5q275LiN5LyR5ZOm77yM6KaB5oOz5riF5qWa5ZOm` |
  | `WALI_KEY_SEG_PROGUARD_6` | `5Lqy77yM5aaC5p6c5L2g5oOz56C06Kej77yM5LiU` |
  | `WALI_KEY_SEG_PROGUARD_7` | `56C06Kej5Ye65LqG5q2k5aSE5Luj56CB77yM5bm25LiU5oOz5pCe54K55LuA5LmI5LqL77yM6KaB5oOz5riF5qWa5ZOm77yM5pys5YWs5Y+46LWg6YCBMTDlubTnj63miL/ml4XmuLgr5Lia5YaF5bm/5ZGK` |
  | `WALI_KEY_SEG_PROGUARD_8` | `C06Kej5` |
  | `WALI_KEY_SEG_PROGUARD_9` | `5qE5pyL5Y+` |
  | `WALI_KEY_SEG_PROGUARD_10` | `+45Lya6iuyhnBsdJb` |
  其中多条 base64 解码后是**中文垃圾短信/话术样本**（[观测] 解码结果）：`死不休哦，要想清楚哦`、`亲，如果你想…`、`你妈贵姓…`、`破解，且`、`…本公司赠送10年…枚/文旅+业内广告`（PROGUARD_7 全长解码）。→ **[推断]** 这些"密钥段"实际承担两用：(a) 作为 AES 密钥的拼接材料（片段化存储），(b) 作为**短信分类的特征/模板语料**（与 `parse_sms_with_tpl_list` 配套）。
- **结论**：`libtcp.so` = 「网络助手/流量校正」的 native 插件（`TcPlugin`/`TcDirection`/`TcManager`）。能力面：HTTPS 拉取服务端指令 → base64+JSON 解析 → 按指令更新运营商/省市/品牌映射表 → 解析运营商短信模板 → 采集 IMEI/IMSI/机型/MIUI 版本等上报 → 弹通知（`com/miui/networkassistant/utils/NotificationUtil`、`showNotification`）。
- **敏感度**：**最高的一个**。三点理由：① 唯一外部服务端端点，且通道是"指令下发"（`directives`）；② 明确读取短信正文与 IMSI/IMEI；③ `collectUserData` 直白命名 + 上报参数字段化。

### 2.16 libtensorflowlite_jni.so
- **[观测]** 1,700,448 B；110 导出：66 个 `Java_org_tensorflow_lite_*`（`NativeInterpreterWrapper_{createInterpreter,createModel,createModelWithBuffer,run,resizeInput,createXNNPACKDelegate,...}`、`TensorImpl_*`、`NativeSignatureRunnerWrapper_*`、`nnapi_NnApiDelegate_*`）+ 51 个 `TfLite*` C API（`TfLiteInterpreterCreate/Invoke`、`TfLiteXNNPackDelegateCreate`、`TfLiteVersion`）。
- **[观测]** 字符串含 kernel 源文件清单（`tensorflow/lite/kernels/{conv,add,mul,fully_connected,gather,reduce,reshape,transpose,strided_slice}.cc`）与 `delegates/nnapi/nnapi_delegate.cc`。
- **[观测]** Java 侧：`org/tensorflow/lite/TensorFlowLite.java:14` → `loadLibrary("tensorflowlite_jni")`。
- **结论**：Google 官方 TFLite 运行库的**原样打包**（版本串与 kernel 列表一致），非自研。无网络能力（导入只有 dlopen/dlsym/mmap 等）。

### 2.17–2.18 libxcrash.so / libxcrash_dumper.so
**libxcrash.so**
- **[观测]** 84,184 B；41 导出，其中真正业务导出只有 `xc_test_call_1..4`、`xc_test_crash`（测试用），主入口是 `JNI_OnLoad(224)` → `RegisterNatives`。
- **[观测]** 导入：`clone`、`eventfd`、`pipe2`、`sigaction`、`sigaltstack`、`sigfillset`、`prctl`、`popen`、`execl`、`waitpid`、`dl_iterate_phdr`、`dladdr`、`scandir`、`uname`、`readlink`、`pthread_setname_np`。**注意：`libxcrash.so` 本身不导入 `ptrace`，`ptrace` 只在 `libxcrash_dumper.so` 中** —— 与"父进程负责拉起、子进程负责 attach"的分工一致。
- **[观测]** 路径字符串：`/proc/self/maps`、`/proc/meminfo`、`/dev/ashmem`、`/dev/ashmem/CursorWindow`、`/dev/null`，以及 `**/data/local/su**`、`/data/local/bin/su`、`/data/local/xbin/su`。
- **[推断]** 这是 iqiyi 开源的 **xCrash**：安装信号处理器 → 崩溃时把现场交给 dumper 子进程。`/data/local/*/su` 三个路径是 **root 检测**（xCrash 用它决定是否附加更多诊断信息），**不是反调试**。
- **[观测]** Java 侧：`jadxout/sources/xcrash/NativeHandler.java:158` → `loadLibrary("xcrash")`。

**libxcrash_dumper.so**
- **[观测]** 125,168 B；**仅 7 个导出**：`CrcUpdateT4`、`CrcUpdateT8`、`g_CrcTable`（LZMA/XZ 的 CRC 表）、`XZ_SIG`（XZ 魔数）、`__INIT_ARRAY__`/`__FINI_ARRAY__`/`__PREINIT_ARRAY__`。
- **[观测] 关键身份证据**：导入 **`__libc_init`** —— 这是"被当作可执行体使用"的 ELF 才会导入的 libc 启动符号；同时**没有** `JNI_OnLoad`、没有 `Java_*`，Java 侧也没有任何 `loadLibrary("xcrash_dumper")`。
- **[观测]** 导入 `ptrace`、`alarm`、`regcomp`/`regexec`、`popen`、`dl_iterate_phdr`、`sigaltstack`；字符串 `THREAD: ptrace GETREGSET failed, errno=%d`、`UTIL: ptrace error, addr:%lx, errno:%d`，同款 `/data/local/*/su` 与 `/dev/ashmem`。
- **[推断]** 它是一个**伪装成 .so 的独立进程**（`__libc_init` + 由 `libxcrash` 通过 `fork/clone`+`exec`/`dlopen` 拉起），负责 `ptrace` attach 崩溃进程、读 `/proc/self/maps` 与内存、正则过滤、用 LZMA(XZ) 压缩 tombstone。
- **敏感度**：中。具备 `ptrace` 他人进程 + 读取任意内存映射的能力，但用途限于崩溃转储；`popen` 是一个额外攻击面（[推断]，未见命令串）。

---

## 3. native 层关键发现汇总

### 3.1 网络端点（全量，1 个）
| 端点 | 出处 | 证据 |
|---|---|---|
| `https://net-assistant.10046.xiaomimobile.com/v1/traffic/directives` | `libtcp.so`（arm64 + v7a 都有） | `U_URL` 符号重定位 → `.rodata@0xbaf6`；`resolve_ptrs.mjs` 输出 |

配套 HTTP 客户端是内嵌的 **libghttp**（UA `libghttp/1.0`），并支持代理与 Basic 认证（`ghttp_set_proxy`、`ghttp_set_authinfo`、`ghttp_set_proxy_authinfo`、`http_hdr_Authorization`、`http_hdr_Proxy_Authorization`）。
其他库的 URL 全部是编译器/开源库自带（`android.googlesource.com`、`streams.videolan.org`、`tensorflow.org/lite/guide/*`），非业务端点。

### 3.2 密钥与加密材料
| 位置 | 内容 | 强度判断 |
|---|---|---|
| `libtcp` `WALI_KEY_SEG_1/2/3` + `PROGUARD_1..10` | 13 段 base64 片段（部分解码为中文垃圾短信语料） | **[推断]** 拼接后参与 AES 密钥派生；片段化 + `PROGUARD_` 命名是明显的反静态提取手法 |
| `libtcp` `.rodata` | `0102030405060708`、`0123456789abcdef` | **[推断]** 硬编码 IV/测试密钥（经典弱常量形状） |
| `libtcp` `java_tc_plugin_get_aes3` / `get_aes3_iv_key` | 从 Java 侧取 AES-3 的 key 与 IV | **[观测]** 函数存在（size 分别 60B 级跳板）；实际密钥在 Java/DEX 侧 |
| `libsec-common-utils` | `C8:A2:E9:BC:CF:59:7C:2F:B6:DC:66:BE:E2:93:FC:13:F2:FC:47:EC:77:BC:6B:2B:0D:52:C1:1F:51:19:2A:B8` | **[观测]** 硬编码 SHA-256 证书指纹；**≠ 本 APK 证书** `c9009d01...49025` |
| `libsec-common-utils` | `get{Encode,Decode,Http,Uuid,Rubbish,NetworkAssistant}SecretKey` | **[推断]** APK 内各处对称密钥的统一供给点 |
| `liblib-riskcontrol` | AES 全实现（ECB/CBC/CFB）；密钥**未在常量表中找到** | **[观测]** 48 个候选密钥 × 5 IV × 2 模式解密 `censor_source` 全部补位失败 → 密钥运行时派生 [推断] |

### 3.3 协议 / 数据格式
1. **流量助手指令协议（libtcp）**：`base64(JSON)`。`exec_protocol` 先 `base64_decode` 再 `json_tokener_parse`，按 `PluginRegister` / `MsgType` / `CmdType` / `DataType` / `CommandList` 分派。上报侧是 `application/x-www-form-urlencoded`，参数键固定为 `appVersion/miuiVersion/device/carrier/region/versionType/param/dp`。
2. **短信模板匹配（libtcp）**：模板结构 `{TplId, TplName, TplContent, TplType, SplitChar, CommandList, UseCommands}`；匹配结果 `{MatchedTplIds, KeyValues}` 回传 Java。预处理含大小写归一（`ustring_toLowerCase`/`toUperCase`）与 `ustring_splite` 分词。
3. **敏感词词典（liblib-riskcontrol）**：`assets/censor_source`（2688 B，熵 7.93，16 B 对齐）→ AES-CBC/PKCS5 解密 → 按行 `TrieTree::insert`；`needPadding %u char(s)` 是补位日志。**.censor_source 外部覆盖路径 `/storage/emulated/0/censor_source` 残留**（调试口子，若被利用可替换词典或做注入面）。
4. **互联通道（libmicontinuity_sdk）**：`micont::channel::ChannelImpl(BySocket)`，socket + epoll，支持 `SendFile`/`SendBytes`/`ContinueSend`/`SendEof` + 进度回调；`NPacket` 可在 `byte[]`/`InputStream`/`File` 之间转换。
5. **崩溃转储（libxcrash*）**：`/proc/self/maps` + `ptrace(GETREGSET)` + XZ(LZMA) 压缩（`XZ_SIG`、`CrcUpdateT4/T8`、`g_CrcTable`）。

### 3.4 加固 / 反调试 / 身份校验
- **[观测·有界否定]** 18 个库全量字符串中没有 `TracerPid`、`frida`、`xposed`、`magisk`、`ro.debuggable`、`isDebuggerConnected`、`substrate`、`gdbserver`、`anti_debug`。`ptrace` 只出现在 `libxcrash_dumper`（崩溃 dump 用途）。→ **native 层未见反调试/反注入**。
- **[观测]** 18/18 `stripped` + `BIND_NOW`（符号表裁剪 + 立即绑定，属常规加固，非加壳）。
- **[观测]** 唯一的**主动防御逻辑**是 `libsec-common-utils` 的「包名白名单 + 证书 SHA-256 指纹」校验（4 个白名单包名 + 1 个硬编码指纹）。
- **[观测]** 未见任何 .so 被加壳/自解密（各库 `.rodata` 有完整可读的 C++ RTTI 类名、源文件路径、日志格式串 —— 加壳库不会有这些）。

### 3.5 高危能力分级（按调查提纲逐项回答）
| 库 | 高危/敏感能力 | 依据（证据） |
|---|---|---|
| **libtcp** | ★★★ 服务端指令通道 + 短信正文解析 + IMEI/IMSI/机型上报 | `U_URL` 端点、`exec_protocol`=b64+json 反汇编、`parse_sms_with_tpl_list`、`getSubscriberId`/`setImsi`/`collectUserData` |
| **liblib-riskcontrol** | ★★★ 敏感词审查（可作用于任意文本）；词典可被外部路径覆盖 | TrieTree/AES 导出、`censor_source`、`/storage/emulated/0/censor_source`、`hasSensitiveWords` JNI |
| **libsec-common-utils** | ★★☆ 密钥中心 + 调用方签名门禁（指纹 pinning） | 6 个 `get*Key` JNI、`getSignature` 反射链、白名单包名、硬编码 `C8:A2:...` |
| **libmicontinuity_sdk** | ★★☆ 设备间通道（文件/字节流）+ 输入事件读取 | `ChannelImplBySocket::SendFile/SendBytes`、`NInputReader_read`、socket/epoll 导入 |
| libjni_load_serinum | ★★☆ 设备唯一 ID（MTK OTP）读取 | `/dev/otp`、仅 `open`+`ioctl`、MTK 编译串 |
| libxcrash_dumper | ★★☆ `ptrace` 任意进程 + 内存 dump + `popen` | `ptrace`/`__libc_init`/`popen`/`regexec`/`XZ_SIG` |
| libjni_sound_effect_2_0 | ★★☆ 声纹变换（WORLD 变声） | `Dio/CheapTrick/D4C/Synthesis2` 全套导出 |
| libmnncore + MNN 系 | ★☆☆ 端侧推断 + **端侧训练** | `nativeTrainModel`(12.9 KB)、`nativeLoadMapFromFile`(17.5 KB) |
| libnap | ★☆☆ 按 UID 的流量统计（读 /proc） | `/proc/net/xt_qtaguid/stats` |
| libmiffmpeg + libfastplayer | ★☆☆ 通用解码 + 协议栈（可访问任意 URL） | `getaddrinfo/socket/connect` 导入、`Lavf58.45.100` |
| libtensorflowlite_jni / libMNN / libMNNTrain / libMNN_Express / libc++_shared | ☆ 通用第三方运行时 | kernel/类名清单 |
| libjni_sound_effect | ☆ 音效 DSP | `CMusicLimiter_*`/`BiquadClass`/`RingBuffer` |
| libxcrash | ★☆☆ 信号捕获 + root 路径探测 | `/data/local/*/su`、`sigaction/sigaltstack` |

---

## 4. 未解之谜（明确标注，供后续收敛）

1. **`liblib-riskcontrol` 的 AES 密钥/IV 来源**：48 个候选常量全部解密失败；已排除 S-box/逆 S-box 混淆项。可能由 Java 侧 `SensitiveWordDetectProvider` 传入，或由包名/签名派生。→ 需要 Java 侧交叉（`SensitiveWordDetectProvider` + `libsec-common-utils` 的 `getDecodeSecretKey`）或真机 hook `AES::KeyExpansion`。
2. **`assets/censor_source` 明文内容**：目前只有密文与"逐行插入 TrieTree"的结构性证据，**没有词表本身**。上一条解决后即可解出。
3. **`libsec-common-utils` 硬编码指纹 `C8:A2:E9:...:2A:B8` 属于哪把证书**：已确证 ≠ 本 APK 签名证书（`c9009d01...`）。猜测为 MIUI 平台/系统签名证书，或旧渠道包证书。→ 需要 MIUI 平台证书样本比对。
4. **`libtcp` 的 `WALI_KEY_SEG_*` 如何拼接、拼接后是否真的是 AES 密钥**：已解析出全部 13 段字面值，但拼接顺序/截断规则未还原（`init_weak_string`/`destroy_weak_string` 是可疑的装配函数）。另：多条片段解码为中文垃圾短信文本，说明它同时充当短信语料。
5. **`libtcp` 的 `setimsi` 具体写入路径**：`java_tc_plugin_setimsi` 前 30 条指令只见到参数非空校验 + `cmp` 边界检查 + 返回码 `2/4`，未走到 `TelephonyManager` 反射调用点（函数 492 B，`a64scan` 未跟随条件分支）。
6. **`libmicontinuity_sdk` 通道是否加密**：导入中无 `SSL_*`/`libssl`，倾向自研或明文；`Rtm` 层是否有自己的加密未证。
7. **`libjni_load_serinum` 的 ioctl 请求号与返回结构**：需要 AArch64 反编译器（本机不可用）或真机 strace。
8. **`libmnncore` 训练数据来源**：`nativeLoadMapFromFile` 读的 map 文件是否含用户行为数据，取决于 Java 侧调用方，未查。

---

## 5. 证据索引（可复现）

| 类别 | 路径 |
|---|---|
| 目标库 | `_analysis/apk_full/lib/arm64-v8a/*.so`（18）、`_analysis/apk_full/lib/armeabi-v7a/*.so`（18） |
| 全量扫描报告 | `_analysis/tools/native/report_arm64.txt`、`report_v7a.txt`（每库：大小/架构/stripped/导出数/NEEDED/DT_FLAGS/JNI 入口/其他导出/导入/URL/域名/关键字符串） |
| 逐库字符串全量 | `_analysis/tools/native/<lib>.so.strings.txt`（含文件偏移，格式 `<offset> <text>`） |
| 逐库导出表 | `_analysis/tools/native/<lib>.so.exports.txt`（`addr\tsize\ttype\tbind\tname`） |
| 关键函数反汇编 | `_analysis/tools/native/decomp_notes.txt`（9 个函数带注释反汇编 + 字符串/调用汇总） |
| `libsec` 全符号扫描 | `_analysis/tools/native/sec_all.txt`（521 个导出 × ≤26 条指令，用于定位指纹引用点 0x1ed0c） |
| 工具源码 | `_analysis/tools/{elfscan,strings_human,resolve_ptrs,dump_sym,a64scan,censor_probe}.mjs` |
| APK 签名证据 | REA `inspect_apk_signature` evidence `ev_0040f79d78dc46592c3b7b1aa1c044de0b02498c0ae9b2acfd9e45d2ae87f2ea`（cert SHA-256 `c9009d01...49025`，v1/v2/v3 一致） |
| `libtcp` 导出证据 | REA `inspect_native_exports` evidence `ev_2f72d73e1abfefa6b799a8b35edd6031bb6c001e9c60ab5afc7807f3666392bb`（393 导出 / 452 条目，coverage=complete） |
| 工具不可用的证据 | `open_binary(ghidra)` → `target_format_unsupported`（“Windows Ghidra P0 accepts native PE and explicitly selected raw 6502 targets only”）；`inspect_with_objdump` / `execute_rizin_command` → `ERR_REA_WINDOWS_NATIVE` |
