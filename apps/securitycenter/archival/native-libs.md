> **Historical analysis.** This archive includes observations that remain unverified or were later corrected. See the [final report](../REPORT.md) and [independent review](../REVIEW.md) for the reviewed findings on upload scope, consent, local FBO delegation, emergency SMS encryption, and provider authorization. Source references retain their original decompilation paths. The publication includes the selected files under evidence/.

# MIUISecurityCenter native layer (.so) reverse investigation

> Target: `MIUISecurityCenter-10.4.9-250402.0.1.apk` (SHA-256 `fc63de0e...c678cfd`)
> Range: 18 .so for `_analysis/apk_full/lib/arm64-v8a/`, 18 .so for `armeabi-v7a/` with the same name for comparison
> Based on baseline: `findings/_BASELINE.md`
> Annotation convention: **[observation]** = fact read directly from shipped artifact; **[inference]** = reasonable inference based on observation; **[unknown]** = cannot be determined with current evidence

---

## 0. Boundaries between methods and tools (state it clearly first to avoid misreading the conclusion)

| Tools | Status | Evidence |
|---|---|---|
| REA `open_binary` + `provider_id=ghidra` | **cannot be used for this target** | Return `target_format_unsupported`: "Windows Ghidra P0 accepts native PE and explicitly selected raw 6502 targets only." |
| Hopper / IDA / Kuna | Not available | `unsupported_host`（win32）/ `not_configured` |
| `inspect_with_objdump`（GNU objdump） | Not available | `Could not launch ... "objdump" (ERR_REA_WINDOWS_NATIVE)`, objdump/readelf/nm/strings not available on PATH |
| `execute_rizin_command` | Not available | `Could not launch ... "rizin" (ERR_REA_WINDOWS_NATIVE)` |
| Python / pip | Not available | `python` is a WindowsApps placeholder stub (exit 9009), no `py`/`pip`/`dotnet` |
| REA `inspect_native_exports` | **available** | Portable ELF dynamic symbol table reader (no engine dependency) |
| Node.js v24.22.1 | **available** | All the self-built analysis tools of this report are implemented using it. |

Therefore, this report does not "decompile pseudocode", but uses **'s self-built static analyzer** to provide **instruction-level verifiable evidence**:

| Self-built tool (`_analysis/tools/`) | Purpose |
|---|---|
| `elfscan.mjs` | ELF64/ELF32 header, segment table, `.dynsym` export/import, `DT_NEEDED`/`DT_FLAGS`, full printable string, URL/domain name extraction |
| `strings_human.mjs` | Filter "human-readable" evidence from full strings (remove C++ mangled symbol noise) |
| `resolve_ptrs.mjs` | Use `R_AARCH64_RELATIVE` relocation to parse the exported 8-byte OBJECT symbol **into the string it points to** (key: solve `WALI_KEY_SEG_*`, `U_URL`) |
| `dump_sym.mjs` | Dump raw bytes of exported objects of arbitrary length by symbol name |
| `a64scan.mjs` | **AArch64 partial disassembler**: ELF section/`.dynsym`/`.eh_frame_hdr` function table + `ADRP/ADD/BL/B/CBZ/TBZ/RET/MOVZ/MOVK/LDR-literal/STP/LDR/STR` decoding + **PLT→Import symbol name resolution** + string/call target backfill |
| `censor_probe.mjs` | Do entropy analysis on `assets/censor_source` + brute force attempt to decrypt AES-128/192/256-CBC/ECB from the constants referenced by the AES code path |

> Recurrence: `node tools/a64scan.mjs "apk_full/lib/arm64-v8a/libtcp.so" --exact exec_protocol --max 60`
> The function boundary of `a64scan` is taken from `.eh_frame_hdr` (libtcp gets 336 function starting points), and the PLT name is taken from the `.rela.plt` sequence.
> **limitations**: incomplete CFG/data flow, ADRP/ADD tracking is limited to a single function linear window; conditional jumps are not followed. Therefore, "a certain function refers to a certain string" is reliable, but "this string must be used at runtime" only reaches the **[inference]** level.

---

## 1. Overview

| # | Library | Size(B) | Architecture | stripped | Export symbols | JNI/Initialization entrance | NEEDED (go to libc/log/m) | Java caller | Functional conclusion |
|---|---|---|---|---|---|---|---|---|---|
| 1 | libc++_shared | 911,696 | arm64 | Yes | 2195 | None | — | (depended on by other .so) | Standard Android libc++ runtime library |
| 2 | libfastplayer | 453,504 | arm64 | Yes | 968 | JNI_OnLoad/Unload + `register_FastPlayer` | libmiffmpeg, EGL/GLESv2/3, mediandk, jnigraphics, OpenSLES, android | `com.miui.fastplayer.FastPlayer` | Video player (MediaCodec hard solution + GL rendering + ground glass filter) |
| 3 | libjni_load_serinum | 5,512 | arm64 | Yes | **1** | `Java_...LoadSeriNum_readOTP` | — | `com.miui.securitycenter.utils.LoadSeriNum` | Read MTK OTP through `/dev/otp` + ioctl |
| 4 | libjni_sound_effect | 270,160 | arm64 | Yes | 536 | JNI_OnLoad | — | `com.miui.gamebooster.encoder.SoundSupport` ([observation] string) | Sound effects: limiter + biquad filter + phase processing |
| 5 | libjni_sound_effect_2_0 | 349,808 | arm64 | Yes | 405 | JNI_OnLoad | — | Same as above | **WORLD Vocoder** (Dio/CheapTrick/D4C/Synthesis) = Voice Changing/Voice Conversion |
| 6 | **liblib-riskcontrol** | 247,832 | arm64 | Yes | 455 | `SensitiveWordDetectProvider.setAssetManager` / `hasSensitiveWords` | libandroid（AAssetManager） | `com.miui.riskcontrol.SensitiveWordDetectProvider` | **sensitive word detection**: AES decryption assets dictionary → TrieTree → censor (text) |
| 7 | libmicontinuity_sdk | 1,014,664 | arm64 | Yes | 45(~35 JNI) | JNI_OnLoad + `com.xiaomi.continuity.channel.*` | — | `com.xiaomi.continuity.jni.NObject` etc. | **Xiaomi Internet Data Channel**: socket/epoll + file/byte stream + input event reading |
| 8 | libmiffmpeg | 2,396,624 | arm64 | Yes | 2320 | None | libz | (Depends on fastplayer) | FFmpeg (`Lavf58.45.100`) complete audio and video codec |
| 9 | libMNN | 1,555,208 | arm64 | Yes | 844 | None | — | (Depends on mnncore) | Ali MNN inference engine core |
| 10 | libmnncore | 170,016 | arm64 | Yes | 84 | `com.miui.mlkit.mobilerec.ranking.mnn.*`（26+ JNI） | libMNN, libMNNTrain, libMNN_Express, libc++_shared, jnigraphics | `MNNNetNative` / `MNNTrainNative` | End-side recommendation/ranking model package, **includes online training entrance** |
| 11 | libMNNTrain | 1,079,728 | arm64 | Yes | 1552 | None | libMNN_Express, libMNN | — | MNN training framework (MNN::Train::*) |
| 12 | libMNN_Express | 641,024 | arm64 | Yes | 298 | None | libMNN | — | MNN Express operator layer |
| 13 | libnap | 5,544 | arm64 | Yes | **1** | `NaTrafficStats.nativeGetUidIfaceStat` | — | `com.miui.networkassistant.traffic.statistic.NaTrafficStats` | Analyze `/proc/net/xt_qtaguid/stats` and count traffic according to UID |
| 14 | **libsec-common-utils** | 297,960 | arm64 | Yes | 521 | 6 pcs `ConfigManager.get*Key` + `getSignature`/`getPackageName`/`getUid` | — | `com.miui.securitykey.ConfigManager` | **key derivation center + caller package name/signature whitelist verification** (including hard-coded certificate SHA-256) |
| 15 | **libtcp** | 189,808 | arm64 | Yes | 393 | JNI_OnLoad + `registerNativeMethods` | — | `com.miui.sdk.tc.TcManager` | **network assistant traffic plug-in**: HTTP pull server command (base64+JSON), SMS template analysis, AES reporting |
| 16 | libtensorflowlite_jni | 1,700,448 | arm64 | Yes | 110 | 66 `Java_org_tensorflow_lite_*` | — | `org.tensorflow.lite.TensorFlowLite` | Google TFLite standard runtime (NNAPI/XNNPACK delegate) |
| 17 | libxcrash | 84,184 | arm64 | Yes | 41 | JNI_OnLoad (`xc_test_*` is exported for testing) | libdl | `xcrash.NativeHandler` | Crash capture (xCrash) Java side bridge + root path detection |
| 18 | libxcrash_dumper | 125,168 | arm64 | Yes | **7** | None (**is not JNI library**) | — | None (pulled up by libxcrash) | Crash scene dump: ptrace + `/proc/self/maps` + LZMA |

**Global Fact [Observation]**
- 18/18 All **stripped** (no `.symtab`), only `.dynsym` is retained.
- 18/18 all carry `DF_BIND_NOW`/`DF_1_NOW` (`DT_FLAGS=8` or `DT_FLAGS_1=1`; `libxcrash_dumper` is `134217729 = 0x8000001`, that is, NOW|PIE).
- **There is only one hard-coded network endpoint** among the 18 libraries (libtcp's `net-assistant.10046.xiaomimobile.com`, see §3.1). The remaining `http(s)://` hits are all compiler/library built-in strings (`android.googlesource.com`, `streams.videolan.org`, `tensorflow.org`).
- No anti-debugging/anti-injection strings were found in **[Observation·Bounded Negation]**: Grep `TracerPid|frida|xposed|magisk|ro.debuggable|anti_?debug|isDebuggerConnected|substrate|gdbserver` → 0 hit for the full string of 18 libraries; `ptrace` only appears in `libxcrash_dumper` (`THREAD: ptrace GETREGSET failed`, `UTIL: ptrace error, addr:%lx`), which is a crash dump rather than anti-debugging.
  → Conclusion: There is no active anti-debugging/anti-Frida logic **in the**native layer; the reinforcement is the responsibility of the Java side/system (outside the scope of this article).
- **armeabi-v7a comparison [Observation]**: the same 18 libraries, the same soname collection, the same JNI entry collection (`a64scan` is not applicable to v7a, but the `.dynsym` result of `elfscan` is consistent), the volume is about arm64 55–80%; libtcp’s `net-assistant.../directives` endpoint also exists in v7a. → The two architectures have the same origin and functions, and there is no evidence of the architecture’s unique hidden logic.

---

## 2. Database-by-database conclusions and evidence

### 2.1 libc++_shared.so
- **[observation]** 911,696 B; stripped; 2195 export, all libc++ ABI symbols (`_ZNKSt6__ndk1...`, `_ZSt9terminatev`, etc.); `NEEDED=[]` (no dependencies); import `__cxa_*`, `pthread_*`, `localeconv`, etc.; the only URL is the LLVM compiled string.
- **[Observation]** is different from other libraries in APK. **arm64 version does not have `libc.so` dependent on** (v7a version has `NEEDED=[libc.so, libdl.so]` instead).
- **Conclusion**: Standard NDK `libc++_shared.so`, no business logic and no sensitive capabilities.

### 2.2 libfastplayer.so
- **[observation]** 453,504 B; 968 export; `JNI_OnLoad(420)` + `JNI_OnUnload(36)` + `_Z19register_FastPlayerP7_JNIEnv` (→ `RegisterNatives` dynamic registration, no `Java_*` static export).
- **[Observation] The** import contains `AMediaCodec_*`, `AMediaFormat_*`, `ANativeWindow_*`, `SL_IID_*` (OpenSL ES), `av_*` (from libmiffmpeg), and `av_jni_set_java_vm`. There is no `socket`/`connect`/`gethostbyname`**in the** import table.
- **[observation]** Key exports: `fastplayer::FFmpegDecoder::{OpenCodec,SendPacket,GetFrame}`, `fastplayer::MediaCodecNdk::{InitMediaCodec,h264_set_extradata,hevc_set_extradata,AVFrame4MediaCodec,FillData4NV12,FillData4YUV420P}`, `fastplayer::ANWDisplay::{swscaleFrame,renderPicture}`, `fastplayer::OpenSLRender::*`, `fastplayer::RingBuffer::*`, `fastplayer::MetadataRetrieverWrapper::*`.
- **[observation]** Special effects export (the most intuitive evidence): `setBlurEffectUniform`, `setPoorBlurEffectUniform`, `setMoruGlassEffectUniform`, `setMoruPrismGlassEffectUniform`, `setRectGlassEffectUniform`, `setBrightnessEffectUniform`, `setShaderSource`/`addShaderBitmap`/`disableShader`.
- **[Observation]** Java side: `jadxout/sources/com/miui/fastplayer/FastPlayer.java:247-248` → `System.loadLibrary("miffmpeg"); System.loadLibrary("fastplayer");`
- **Conclusion**: MIUI self-developed short video/preview player. FFmpeg decapsulation + platform MediaCodec hard decoding + OpenGL(ANativeWindow) rendering + frosted glass/blur filter overlay. **does not have independent network capabilities** (the network is at the FFmpeg layer, see 2.8).

### 2.3 libjni_load_serinum.so★(Serial number/OTP)
- **[observation]** 5,512 B; **has only 1 export**: `Java_com_miui_securitycenter_utils_LoadSeriNum_readOTP` (436 B).
- **[Observation]** imports only 10: `open`, `ioctl`, `close`, `malloc`, `free`, `__android_log_print`, `__stack_chk_*`, `__cxa_*`. **does not have read/write** (only `ioctl` is used for data exchange).
- **[Observation]** string (only 3 human-readable strings in the entire database):
  - `/dev/otp`
  - `Error: Cannot open the /dev/otp `
  - `OTP: OTP operation error ! status = %d`
  - Compile string `GCC: (GNU) 4.9 20140514 (mtk-20150408)` → **MediaTek platform pre-compiled**
- **[Observation]** Java side: `jadxout/sources/com/miui/securitycenter/utils/LoadSeriNum.java:6` → `System.loadLibrary("jni_load_serinum")`
- **[Inference]** Open `/dev/otp` (MTK one-time programmable area) and use `ioctl` to read the factory-written OTP/serial number data (MediaTek's OTP area always stores device key/calibration data/unique ID). **This is the device’s unique identification reading channel**, and its sensitivity is higher than the ordinary `Build.SERIAL`.
- **[Unknown]** The request number and return structure of `ioctl` (requires AArch64 decompiler or real machine strace to determine; there is no decompiler available on this machine, `a64scan` only understands the `stp x29,x30` preface + `adrp x8,#0x2c; ldr x8,[x8,#0xe20]` (loading global GOT) for this function After slot), it enters the `cbz` branch and fails to contact the PLT call of `open` within 6 instructions).

### 2.4 libjni_sound_effect.so
- **[Observation]** 270,160 B; **soname is `libmisoundsupport.so`** (different from the file name, indicating that it is a packaged copy of the system's library with the same name).
- **[observation]** 536 export; includes `CMusicLimiter_{Init,ProcessBuffer,ProcessBufferHardLimit,ProcessSample,Reset,Delete}` (volume hard limiting), `BiquadClass::{biquadCoeffiCompute,audioProcess,biquadParamSet,...}` (bi-quad EQ), `RingBuffer::*`, `_Z8princarg*` (phase expansion), `_Z6smoothPfS_ii`.
- **[Observation]** string `com/miui/gamebooster/encoder/SoundSupport` → shares the same Java host class as `libjni_sound_effect_2_0`.
- **[Observation]** imports no socket/HTTP; only `dl_iterate_phdr`, `pthread_*`, `sincos(f)`, `pow`, `expf`.
- **Conclusion**: **limiting + equalization + phase processing**DSP of the Game Turbo sound effects chain. No network, no permission-related capabilities.

### 2.5 libjni_sound_effect_2_0.so ★ (voice change)
- **[observation]** 349,808 B; 405 export. The symbol set completely corresponds to **WORLD vocoder** (speaker conversion/analysis-synthesis):
  `Dio`, `GetSamplesForDIO`, `InitializeDioOption`, `CheapTrick`, `GetFFTSizeForCheapTrick`, `GetF0FloorForCheapTrick`, `D4C`, `InitializeD4COption`, `CodeSpectralEnvelope`, `DecodeSpectralEnvelope`, `CodeAperiodicity`, `DecodeAperiodicity`, `DCCorrection`, `Synthesis2`, `InitializeSynthesizer`, `RefreshSynthesizer`, `NuttallWindow`, `LinearSmoothing`, `GetMinimumPhaseSpectrum`, and Ooura FFT (`rdft`, `cdft`, `bitrv2`, `cftbsub`, `makewt`, `makect`).
- **[Observation]** is the same as the host class `com/miui/gamebooster/encoder/SoundSupport`; imported without socket.
- **[Inference]** This is the **real-time voice changing/timbre conversion** engine (WORLD is a high-quality open source vocoder from Google, a typical use is voice changer). Privacy-sensitive points in game voice scenarios: User voiceprint characteristics can be changed.
- **[Unknown] Which path on the Java side of** is triggered (which method of `SoundSupport` corresponds to `Synthesis2`) has not been cross-validated on the Java side.

### 2.6 liblib-riskcontrol.so ★★ (sensitive word detection, one of the highest value discoveries this time)
- **[observation]** 247,832 B; 455 export. JNI Only 2: `Java_com_miui_riskcontrol_SensitiveWordDetectProvider_setAssetManager` (8 B, pure setter), `Java_com_miui_riskcontrol_SensitiveWordDetectProvider_hasSensitiveWords` (232 B).
- **[Observation]** key C++ export (complete algorithm skeleton, can be read without decompilation):
  - Dictionary structure: `TrieTree::{insert,insertBranch,find}`, `TreeNode::{insertChild,findChild}` → **dictionary tree/AC automatic multi-mode string matching**
  - Business: `SensitiveWordDetector::{load(char*), censor(std::string&)}`, `SensitiveWordDetectCore::{censor, encryptToFile, decryptFromFile, readFileFromAsset}`
  - Cryptography: A complete set of self-implemented AES - two overloads of `Phj` and `std::vector<uint8_t>` of `AES::{KeyExpansion,SubBytes,InvSubBytes,ShiftRows,InvShiftRows,MixColumns,InvMixColumns,AddRoundKey,Rcon,RotWord,SubWord,xtime,XorWords,XorBlocks,EncryptBlock,DecryptBlock}` + `AES::{Encrypt,Decrypt}{ECB,CBC,CFB}` + `AESKeyLength` enumeration
- **[observation]** import `AAssetManager_open/read/getLength/close` (libandroid) → dictionary from **APK assets**.
- **[Observation]** string evidence (there are only 62 human-readable strings in the entire database, all key items are listed):
  - `sensitiveWordDetectCore` (global object name), `SensitiveWordDetect` (TAG)
  - `censor_source` ← File name **in**assets
  - `/storage/emulated/0/censor_source` ← **External path for debugging/offline import** (development legacy, indicating that the dictionary can be overwritten by external files)
  - `AssetManager open fail!`、`AssetManager Error!`
  - `File has %d lines`、`File has no content`、`load file!`、`Load file tree f...`
  - `censor sentence `、`keyword and char size mismatch!!`
  - `CheckLength ERROR! len = %d, bl...`、`inLen must be multiple o...`、`needPadding %u char(s)`
- **[Observation]** Cross-validation: `assets/censor_source` does exist in APK, **2,688 B**; entropy **7.933 bits/byte** (close to 8.0 upper limit), 2688 = 16 × 168 (AES block-aligned) → highly consistent with AES ciphertext/compressed data, not plaintext vocabulary.
- **[observation]** `a64scan` scan `decryptFromFile` → reference `"SensitiveWordDetect"`, `"needPadding %u char(s)"`, and `bl plt:_ZN3AES10MixColumnsEPPh`; scan `readFileFromAsset` → reference `"censor_source"`, call `find/__find_equal`, `AES::MixColumns` of `fopen`, `std::map`.
- **[Inference]** Data flow: `setAssetManager()` Save AssetManager → First time `hasSensitiveWords()` triggers `SensitiveWordDetectCore::readFileFromAsset()` Read `assets/censor_source` → `AES` Decrypt (CBC, PKCS5 padding, from `needPadding` log inference) → word segmentation by line → `TrieTree::insert` tree creation (`File has %d lines` is a line-by-line counting log) → do multi-pattern matching on the input parameter text `censor()`, and return whether the sensitive word is hit.
- **[Unknown]** AES Source of key and IV. `censor_probe.mjs` expands the 24 constant addresses referenced by `ADRP+ADD` in 49 AES/censor related functions into 48 16/24/32 byte candidate keys, and uses 5 types of IVs and CBC/ECB to try to decrypt `censor_source`, **all because of PKCS5 The padding check failed and** was discarded (effectively decrypted 0). Two of the "high entropy constants" were identified as **AES S-box** (`0x2e934` = `637c777bf26b6fc53001672bfed7ab76`) and **inverse S-box** (`0x2f954` = `52096ad53036a538bf40a39e81f3d7fb`), which are not keys. → The key should be **runtime derived** (maybe passed in by Java side or derived from package name/signature).
- **Sensitivity evaluation**: High. The function is **to make local sensitive word hit determination** for any string (typical calling surfaces: SMS, notifications, chat text). Dictionary encryption + debug path residue indicates that it is deliberately protected.

### 2.7 libmicontinuity_sdk.so ★ (Xiaomi Internet Channel)
- **[observation]** 1,014,664 B; only 45 exports, about 35 of which are `Java_*`, with two sets of namespaces:
  - `Java_com_xiaomi_continuity_channel_NChannel_{send,send2,isActive,getChannelId,destroyChannel}`
  - `Java_com_xiaomi_continuity_channel_NChannelManager_{initialize,createChannel2,destroyChannel,registerChannelListener,unregisterChannelListener}`
  - `Java_com_xiaomi_continuity_channel_NPacket_{asBytes,fromBytes,asFile,asInputStream,fromInputStream,isReceived,isDiscarded}`
  - `Java_com_xiaomi_continuity_channel_NConnMgr{ChannelListener,SendResultCallback}_*` (including `onRequestSocketPort`, `onServerRegisterStatus`, `onChannelConfirm`, `onResult`, `onProgress`)
  - `Java_com_xiaomi_continuity_jni_NInputReader_{read,read([BII),skip}`、`NRunnable_run`、`NObject_setClassLoader`
- **[Observation]** import contains complete socket Stack: `socket`, `connect`, `accept`, `bind`, `listen`, `recvfrom`, `send`, `select`, `poll` , `epoll_create/ctl/wait`, `getpeername`, `getsockname`, `inet_pton`, `inet_ntop`, `pipe`, `prctl`, `sem_*`, `getauxval`.
- **[observation]** string (C++ RTTI/log, directly exposing the implementation class name):
  `micont::channel::ChannelImpl::Send`、`micont::channel::ChannelImplBySocket::SendFile`、`...::SendBytes`、`...::ContinueSend`、`ChannelImpl::New(const ::ChannelInfo&, IChannelListener*)`、`RtmSendRetErr: %d`、`RtmSendUnsupported!`、`invalid channel_id=%d (%s:%d)`、`[SendTask(%d, %d)]SendEof!`、`[SendTask(%d, %d)]SendDataFrameErrCb`、`/dev/urandom`。
- **[Inference]** **bidirectional data channel of Xiaomi Internet (mobile phone ↔ tablet / PC / car machine) SDK**: `micont::channel` abstraction + socket implementation (`Rtm` = Internet transport layer), supports arbitrary byte stream and file transfer (with progress/cancellation/EOF semantics), and uses `/dev/urandom` to generate session random numbers. `NInputReader` provides the **interface for reading** input events (cross-device input/screen projection scenarios).
- **[Unknown]** channel encryption and authentication details (`Rtm` whether TLS/self-developed encryption) - TLS library import not seen in the string (no `SSL_*`, no `libssl`), **tends to self-developed/clear text channel [Inferred]**, but key agreement path evidence is missing.

### 2.8 libmiffmpeg.so
- **[observation]** 2,396,624 B; 2320 export, all `av_*`/`ff_*` + `_Unwind_*`; `NEEDED=[libm, libz, liblog, libc, libdl]`; version string **`Lavf58.45.100`** (corresponding to FFmpeg 4.3 series).
- **[observation]** The source file name string proves compiled into the decoder set: `libavcodec/h264dec.c`, `h264_slice.c`, `hevc_ps.c`, `aacdec_template.c`, `aacsbr.c`, `mediacodec_wrapper.c`, `libavformat/mov.c`, `mux.c`, `network.c`, `url.c`, `aviobuf.c`; other `libavcodec license: nonfree and unredistributable` (this build declares nonfree components).
- **[observation]** **imported the network stack**: `socket`, `connect`, `accept`, `bind`, `listen`, `getaddrinfo`, `freeaddrinfo`, `getnameinfo`, `getsockopt` → FFmpeg Protocol layers (http/rtmp/tcp, etc.) are available.
- **Conclusion**: Complete FFmpeg static library (without JNI), linked by `libfastplayer`. **It is the only third-party media library** in this APK that has universal network client capabilities; whether it is used for non-local URL playback depends on the upper-level Java ([Unknown]).

### 2.9–2.12 MNN series (libMNN/libmnncore/libMNNTrain/libMNN_Express)
- **[Observation]** dependency chain: `libmnncore → {libMNN, libMNNTrain, libMNN_Express, libc++_shared}`; `libMNNTrain → {libMNN_Express, libMNN}`; `libMNN_Express → libMNN`. Only `libmnncore` has JNI.
- **[observation]** `libMNN.so` 844 export (`MNN::Interpreter/Session/Tensor/Backend/Runtime/GeometryComputer`…), `libMNNTrain.so` 1552 export (`MNN::Train::{AppDataset,DataLoader,LrScheduler,ImageDataset,Transformer}`), `libMNN_Express.so` 298 export (`MNN::Express::_Conv/_MatMul/_ReduceSum/...`).
- **[observation]** JNI of `libmnncore.so` all fall on `com.miui.mlkit.mobilerec.ranking.mnn`: `MNNNetNative_{nativeCreateNetFromFile,nativeCreateSession,nativeRunSession,nativeRunSessionWithCallback,nativeReshapeSession,nativeConvertBitmapToTensor,nativeConvertBufferToTensor,nativeGetSessionInput/Output,nativeTensorGet*}`, and `MNNTrainNative_{nativeLoadMapFromFile,nativeRecLoadMapFromFile,nativeTrainModel,nativeGetModelPredictScore,nativeGetRecModelPredictScore}`.
- **[Observation]** function body size (`.dynsym` st_size) display `nativeTrainModel`=12,924 B, `nativeLoadMapFromFile`=17,532 B, `nativeRecLoadMapFromFile`=15,332 B —— **training/loading logic is a real heavyweight implementation of**, not an empty shell.
- **[observation]** `libMNN.so` string contains `/proc/cpuinfo` (CPU feature detection → backend selection). `libMNNTrain` contains `/dev/urandom`.
- **[Observation]** Java side: `jadxout/sources/com/miui/mlkit/mobilerec/ranking/mnn/MNNNetNative.java:6-9` loadLibrary `MNN`/`MNN_Express`/`MNNTrain`/`mnncore` in sequence.
- **[Inference]** MIUI "mobile recommendation/ranking" model is inferred on the device side, and has **device-side incremental training** capability (`nativeTrainModel` + `nativeLoadMapFromFile` read training sample map). Corresponds to `model`(680 KB) and `rec_model_20230425.mnn`(810 KB) in assets.
- **Sensitivity**: Medium. The client-side inference itself is not connected to the Internet, but where the training/sample map comes from (whether it contains user behavior) [unknown].

### 2.13 libnap.so
- **[observation]** 5,544 B; soname **`na.so`**; only 1 export `Java_com_miui_networkassistant_traffic_statistic_NaTrafficStats_nativeGetUidIfaceStat` (664 B).
- **[Observation]** imports only 9: `fopen`, `fgets`, `sscanf`, `strcmp`, `fclose`, `__stack_chk_*`, `__cxa_*`.
- **[Observation]** **The only human-readable string in the entire database**: `/proc/net/xt_qtaguid/stats`.
- **[Inference]** parses the kernel `xt_qtaguid` statistics file line by line, and summarizes the number of bytes sent and received by UID + network interface. `xt_qtaguid` has been deprecated in Android 9+ (changed to eBPF), so this is the compatibility path **for the old version of**; on the new system, this function is likely to return empty/failed ([inference], no evidence of code branching).
- **Sensitivity**: Low-Medium (read global traffic statistics, not content).

### 2.14 libsec-common-utils.so ★★ (Key Center + Signature Whitelist)
- **[observation]** 297,960 B; 521 exported. JNI 6, all in `com.miui.securitykey.ConfigManager`:
  `getDecodeSecretKey`(36 B)、`getEncodeSecretKey`(36 B)、`getHttpKey`(36 B)、`getNetworkAssistantKey`(36 B)、`getRubbishSecurityKey`(1,036 B)、`getUuidKey`(2,036 B)。
- **[Observation]** also has 3 key C exports (symbol names are semantics):
  `_Z12getSignatureP7_JNIEnvP8_jobject`（`getSignature(JNIEnv*, jobject)`）、`_Z14getPackageNameP7_JNIEnvP8_jobject`、`_Z6getUidP7_JNIEnvP8_jobject`。
- **[Observation]** `a64scan` Scan the string reference of `getSignature` to get the complete JNI reflection chain:
  `"getPackageManager"` → `"()Landroid/content/pm/PackageManager;"` → `"getPackageInfo"` → `"(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;"` → `"getPackageName"` → `"()Ljava/lang/String;"`；
  Match the strings `"[Landroid/content/pm/Signature;"`, `"signatures"`, `"sha256"`, `"com/miui/securitykey/EncryptionUtils"`.
- **[Observation]** Hard-coded **whitelist package name** (exists as a string in .rodata):
  `com.miui.securitycenter`、`com.miui.cleaner`、`com.miui.cleanmaster`、`com.miui.global.packageinstaller`。
- **[Observation·Key] Hard-coded certificate SHA-256 Fingerprint**:
  `C8:A2:E9:BC:CF:59:7C:2F:B6:DC:66:BE:E2:93:FC:13:F2:FC:47:EC:77:BC:6B:2B:0D:52:C1:1F:51:19:2A:B8`
  Reference point: `0x1ed0c` (`add x8, x8, #0x95f` → `0x1495f`), located in the **unexported function** immediately after `Java_..._getNetworkAssistantKey`(0x1ecb4) / `Java_..._getDecodeSecretKey`(0x1ec6c) (the two exported functions are only 36 B each, just a springboard).
- **[Observation·Cross-validation]** The real signature certificate of this APK SHA-256 (REA `inspect_apk_signature`, v1/v2/v3 three schemes are consistent) =
  `c9009d01ebf9f5d0302bc71b2fe9aa9a47a432bba17308a3111b75d7b2149025`
  → **does not match the hardcoded fingerprint `C8:A2:E9:...:2A:B8`**.
- **[Inference]** Mechanism: `getSignature()` Reflection gets the **caller package** `Signature[]` → SHA -256 → and compares it with the hard-coded fingerprint/package name whitelist, as the identity gate of "who can call this library to get the key". The fingerprint does not match this APK own certificate, indicating that it pinned **another certificate** (most likely MIUI platform/system signature or historical channel certificate). This has a verifiable side-effect hypothesis: ** Any build re-signed with a certificate other than this one, `get*Key` will fail ** ([Inferred], not verified on a real machine).
- **[Inference] The** `get*Key` series is the key source for multiple symmetric encryptions in APK (`getHttpKey` corresponds to HTTP parameter encryption, `getNetworkAssistantKey` corresponds to traffic assistant, `getRubbishSecurityKey` corresponds to garbage cleaning rule decryption, `getUuidKey` Corresponding device UUID derived).
- **[Observation]** has no network and no file system reoperation (there is no socket/fopen directory traversal in the import).

### 2.15 libtcp.so ★★★ (traffic assistant plug-in, the most complex one this time)
- **[observation]** 189,808 B; 393 export; `JNI_OnLoad(196)` + `JNI_OnUnload(20)` + `registerNativeMethods(548)`.
- **[observation]** Java side: `jadxout/sources/com/miui/sdk/tc/TcManager.java:116` → `System.loadLibrary("tcp")`.
- **[Observation]** **unique hardcoded endpoint** (`U_URL`, confirmed with relocation resolution: `U_URL @0x2e610 -> 0xbaf6`):
  ```
  https://net-assistant.10046.xiaomimobile.com/v1/traffic/directives
  ```
- **[Observation]** registered native method name (`RegisterNatives` table, double evidence of string and export symbol):
  `java_tc_plugin_init`、`java_tc_plugin_update`、`java_tc_plugin_update_by_tc_type`、`java_tc_plugin_get_result`、`java_tc_plugin_get_result_by_tc_type`、`java_tc_plugin_get_directions`、`java_tc_plugin_getprovincesmap`、`java_tc_plugin_getcitiesmap`、`java_tc_plugin_getcarriesmap`、`java_tc_plugin_getbrandsmap`、`java_tc_plugin_get_province_code_by_city_code`、`java_tc_plugin_get_tc_pref_value`、`java_tc_plugin_get_aes3`、`java_tc_plugin_get_aes3_iv_key`、`java_tc_plugin_setimsi`。
  Log string: `RegisterNatives successed for '%s' of '%s'`/`RegisterNatives failed for '%s' of '%s'`/`no method matched for '%s' of '%s'`.
- **[Observation]** embedded components (exported symbols are lists): libghttp (`ghttp_*` 30+, UA string `libghttp/1.0`), json-c (`json_tokener_*`, `json_object_*`, `lh_table_*`, `printbuf_*`), MD5 (`MD5Init/Update/Final/MD52Hex`), base64 (`b64_alphabet` has been verified as `ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=`, `base64_encode/decode`), ustring/vector/array_list/byte_buffer/pref_* (self-developed container and configuration), `url_encode/decode`.
- **[Observation] Instruction execution chain**: `a64scan` disassembly of `exec_protocol`(0x20314):
  ```
  0x2032c  movz x0, #0x1
  0x20330  movz x1, #0x30
  0x20334 bl 0x296f0 ; plt:base64_decode ← Do base64 decoding first
  0x20344 str x8, [x9,#0x0] ; Fill in the 0x30 byte structure
  0x20364  bl   0x203b8     ; sub_203b8
  ... Within sub_203b8:
  0x203f0  add  x0, x0, #0x2a5   ; =0xd2a5 "PluginRegister"
  0x203f4  bl   0x29930          ; plt:json_object_object_get
  0x203f8  mov  x2, x0
  0x20404  add  x1, x1, #0xbc8   ; =0xcbc8 "MsgType"
  0x20408  bl   0x29932          ; plt:json_object_get_string
  ```
  → **[Observation + Inference]** The `directives` payload issued by the server is **base64(JSON)**: `base64_decode` first, then use json-c to get `PluginRegister` / `MsgType` and other fields for distribution.
- **[Observation] SMS template parsing**: `parse_sms_with_tpl_list`(0x14868) string/call:
  String reference `"KeyValues"`, `"MatchedTplIds"`; call `json_object_new_object`, `json_object_new_array`, `json_object_object_add`, `json_object_array_get_idx`, `ustring_create_u`, `ustring_splite`, `ustring_toLowerCase`, `ustring_toUperCase`, `vector_new`, `sub_141a4`.
  Matching field name strings: `TplId`, `TplName`, `TplContent`, `TplType`, `TplList`, `MatchedTplIds`, `SplitChar`, `KeyValues`, `MsgType`, `CmdType`, `DataType`, `CommandList`, `UseCommands` , `SuccIds` , `SmsFilterNum` , `SmsSendNum` , `SmsManageNum` , `PhoneNumberPrefix` , `tplMatch` , `parse_sms` .
  → **[Inference]** uses the **template list** issued by the server to match the received text message text. After hitting the target, the template id and the extracted key-value pair are used as JSON to report/execute the command. This is the core of the "operator package/traffic correction" business: determining the package balance by analyzing the operator's text messages (including verification code text messages).
- **[Observation] Device/user data collection**: string `collectUserData`, `getDeviceId`, `getSubscriberId`, `setImsi`, `ro.product.model`, `ro.product.manufacturer`, `ro.build.display.id`, `ro.build.version.release`, `ro.build.version.sdk`, `android/telephony/TelephonyManager`.
  Reported parameter splicing format (directly readable): `appVersion=%s&`, `miuiVersion=%s&`, `device=%s&`, `carrier=%s&`, `region=%s&`, `versionType=%s&`, `param=%s`, `dp=%s`.
- **[Observation] Encryption**: `AES/CBC/PKCS5Padding`, `javax/crypto/Cipher`, `javax/crypto/spec/SecretKeySpec`, `javax/crypto/spec/IvParameterSpec`, `(Ljava/lang/String;)Ljavax/crypto/Cipher;`, `doFinal`, `android/util/Base64`, `encodeToString`; constant `0102030405060708` (classic hardcoded IV shape) vs. `0123456789abcdef`.
  The reference of `aes_encrypt_java` (0x28454, 132 B) is only `"javax/crypto/spec/SecretKeySpec"` + `time` + `__android_log_print` → **[Inference]** pure JNI reflection packaging, the real encryption is completed in Java `Cipher`, the key is `java_tc_plugin_get_aes3` / `..._get_aes3_iv_key` taken from Java side.
- **[Observation·Key points] Key segmentation table (relocation analysis, checkable one by one)**: 13 exported `char*` global variables point to .rodata, and the names have the `PROGUARD_` suffix - indicating that **deliberately pressed ProGuard Style splitting/renaming to prevent static extraction of the key fragment of**:
  | symbol | value |
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
  Many of them, after base64 decoding, are **Chinese spam text messages/speech samples** ([observation] decoding results): `Don’t stop, think clearly`, `Dear, if you want...`, `What’s your mother’s surname...`, `Crack, and`, `... Our company will give away 10 years of... coins/culture tourism + industry advertising` (PROGUARD_7 full-length decoding). → **[Inference]** These "key segments" actually serve two purposes: (a) as splicing material (fragmented storage) for the AES key, (b) as the feature/template corpus **for** SMS classification (matched with `parse_sms_with_tpl_list`).
- **Conclusion**: `libtcp.so` = native plug-in of "Network Assistant/Traffic Correction" (`TcPlugin`/`TcDirection`/`TcManager`). Capability: HTTPS pulls the server command → base64+JSON analysis → updates the operator/province/brand mapping table according to the command → parses the operator SMS template → collects the IMEI/IMSI/model/MIUI version and reports → pops up the notification (`com/miui/networkassistant/utils/NotificationUtil`, `showNotification`).
- **sensitivity**: **is the highest sensitivity**. Three reasons: ① The only external server endpoint, and the channel is "command delivery" (`directives`); ② Clearly read the text message text and IMSI/IMEI; ③ `collectUserData` straightforward naming + fielding of reported parameters.

### 2.16 libtensorflowlite_jni.so
- **[observation]** 1,700,448 B; 110 Export: 66 `Java_org_tensorflow_lite_*` (`NativeInterpreterWrapper_{createInterpreter,createModel,createModelWithBuffer,run,resizeInput,createXNNPACKDelegate,...}`, `TensorImpl_*`, `NativeSignatureRunnerWrapper_*`, `nnapi_NnApiDelegate_*`) + 51 `TfLite*` C API (`TfLiteInterpreterCreate/Invoke`, `TfLiteXNNPackDelegateCreate`, `TfLiteVersion`).
- **[Observation] The** string contains the kernel source file list (`tensorflow/lite/kernels/{conv,add,mul,fully_connected,gather,reduce,reshape,transpose,strided_slice}.cc`) and `delegates/nnapi/nnapi_delegate.cc`.
- **[observation]** Java side: `org/tensorflow/lite/TensorFlowLite.java:14` → `loadLibrary("tensorflowlite_jni")`.
- **Conclusion**: **of Google’s official TFLite runtime library packages** as it is (the version string is consistent with the kernel list), which is not self-developed. No network capability (only dlopen/dlsym/mmap, etc. can be imported).

### 2.17–2.18 libxcrash.so / libxcrash_dumper.so
**libxcrash.so**
- **[Observation]** 84,184 B; 41 exports, among which the real business exports are only `xc_test_call_1..4` and `xc_test_crash` (for testing), and the main entrance is `JNI_OnLoad(224)` → `RegisterNatives`.
- **[Observation]** Import: `clone`, `eventfd`, `pipe2`, `sigaction`, `sigaltstack`, `sigfillset`, `prctl`, `popen`, `execl`, `waitpid`, `dl_iterate_phdr`, `dladdr`, `scandir`, `uname`, `readlink`, `pthread_setname_np`. **Note: `libxcrash.so` itself does not import `ptrace`, `ptrace` is only in `libxcrash_dumper.so`** - consistent with the division of labor of "the parent process is responsible for pulling up, and the child process is responsible for attaching".
- **[observation]** path strings: `/proc/self/maps`, `/proc/meminfo`, `/dev/ashmem`, `/dev/ashmem/CursorWindow`, `/dev/null`, and `**/data/local/su**`, `/data/local/bin/su`, `/data/local/xbin/su`.
- **[Inference]** This is the open source **xCrash** from iqiyi: install the signal processor → hand over the scene to the dumper child process when it crashes. The three paths of `/data/local/*/su` are **root to detect** (xCrash uses it to decide whether to attach more diagnostic information), **is not anti-debugging**.
- **[observation]** Java side: `jadxout/sources/xcrash/NativeHandler.java:158` → `loadLibrary("xcrash")`.

**libxcrash_dumper.so**
- **[observation]** 125,168 B; **has only 7 exports of**: `CrcUpdateT4`, `CrcUpdateT8`, `g_CrcTable` (CRC of LZMA/XZ Table), `XZ_SIG` (XZ magic number), `__INIT_ARRAY__`/`__FINI_ARRAY__`/`__PREINIT_ARRAY__`.
- **[Observation] Key identity evidence**: Import **`__libc_init`** - This is the libc startup symbol that is imported only when ELF is "used as an executable"; at the same time, **does not have** `JNI_OnLoad`, no `Java_*`, nor any `loadLibrary("xcrash_dumper")` on the Java side.
- **[observation]** import `ptrace`, `alarm`, `regcomp`/`regexec`, `popen`, `dl_iterate_phdr`, `sigaltstack`; string `THREAD: ptrace GETREGSET failed, errno=%d`, `UTIL: ptrace error, addr:%lx, errno:%d`, the same style `/data/local/*/su` and `/dev/ashmem`.
- **[Inference]** It is an independent process **disguised as .so by** (`__libc_init` + pulled up by `libxcrash` through `fork/clone`+`exec`/`dlopen`), responsible for `ptrace` Attach crash process, read `/proc/self/maps` and memory, regular filtering, compress tombstone with LZMA(XZ).
- **Sensitivity**: Medium. It has `ptrace` other people's processes + the ability to read any memory map, but its use is limited to crash dumps; `popen` is an additional attack surface ([inference], no command string seen).

---

## 3. Summary of key findings in the native layer

### 3.1 Network endpoint (full quantity, 1)
| endpoint | Source | Evidence |
|---|---|---|
| `https://net-assistant.10046.xiaomimobile.com/v1/traffic/directives` | `libtcp.so` (both arm64 + v7a) | `U_URL` symbol relocation → `.rodata@0xbaf6`; `resolve_ptrs.mjs` output |

The supporting HTTP client is the embedded **libghttp** (UA `libghttp/1.0`), and supports proxy and Basic authentication (`ghttp_set_proxy`, `ghttp_set_authinfo`, `ghttp_set_proxy_authinfo`, `http_hdr_Authorization`, `http_hdr_Proxy_Authorization`).
URL of other libraries are all built-in with the compiler/open source library (`android.googlesource.com`, `streams.videolan.org`, `tensorflow.org/lite/guide/*`) and are not business endpoints.

### 3.2 Keys and encryption materials
| location | content | Strength judgment |
|---|---|---|
| `libtcp` `WALI_KEY_SEG_1/2/3` + `PROGUARD_1..10` | 13 base64 fragments (partially decoded into Chinese spam text message corpus) | **[Inference]** participates in AES key derivation after splicing; fragmentation + `PROGUARD_` naming is an obvious anti-static extraction method |
| `libtcp` `.rodata` | `0102030405060708`、`0123456789abcdef` | **[Inference]** hardcoded IV/test key (classic weak constant shape) |
| `libtcp` `java_tc_plugin_get_aes3` / `get_aes3_iv_key` | Get the key and IV of AES-3 from Java | **[Observation]** function exists (size respectively 60B springboard); the actual key is on the Java/DEX side |
| `libsec-common-utils` | `C8:A2:E9:BC:CF:59:7C:2F:B6:DC:66:BE:E2:93:FC:13:F2:FC:47:EC:77:BC:6B:2B:0D:52:C1:1F:51:19:2A:B8` | **[Observation]** hard-coded SHA-256 certificate fingerprint; **≠ this APK certificate** `c9009d01...49025` |
| `libsec-common-utils` | `get{Encode,Decode,Http,Uuid,Rubbish,NetworkAssistant}SecretKey` | **[Inference] Unified supply point for symmetric keys everywhere in** APK |
| `liblib-riskcontrol` | AES full implementation (ECB/CBC/CFB); key **not found in constant table** | **[Observation]** 48 candidate keys × 5 IV × 2 mode decryption `censor_source` All padding failed → Key runtime derivation [Inference] |

### 3.3 Protocol/Data Format
1. **Traffic Assistant Command Protocol (libtcp)**: `base64(JSON)`. `exec_protocol` first `base64_decode` and then `json_tokener_parse`, assigned according to `PluginRegister` / `MsgType` / `CmdType` / `DataType` / `CommandList`. The reporting side is `application/x-www-form-urlencoded`, and the parameter key is fixed to `appVersion/miuiVersion/device/carrier/region/versionType/param/dp`.
2. **SMS template matching (libtcp)**: template structure `{TplId, TplName, TplContent, TplType, SplitChar, CommandList, UseCommands}`; matching result `{MatchedTplIds, KeyValues}` returns Java. Preprocessing includes case normalization (`ustring_toLowerCase`/`toUperCase`) and `ustring_splite` word segmentation.
3. **sensitive word dictionary (liblib-riskcontrol)**: `assets/censor_source` (2688 B, entropy 7.93, 16 B alignment) → AES-CBC/PKCS5 decryption → by line `TrieTree::insert`; `needPadding %u char(s)` is a padding log. **.censor_source external coverage path `/storage/emulated/0/censor_source` remains** (debug hole, if used, can replace the dictionary or make an injection surface).
4. **interconnection channel (libmicontinuity_sdk)**: `micont::channel::ChannelImpl(BySocket)`, socket + epoll, supports `SendFile`/`SendBytes`/`ContinueSend`/`SendEof` + progress callback; `NPacket` can be used in Convert between `byte[]`/`InputStream`/`File`.
5. ** crash dump (libxcrash*) **: `/proc/self/maps` + `ptrace(GETREGSET)` + XZ(LZMA) compression (`XZ_SIG`, `CrcUpdateT4/T8`, `g_CrcTable`).

### 3.4 Reinforcement/anti-debugging/identity verification
- **[Observation·Bounded Negation]** does not exist in the full string of 18 libraries `TracerPid`, `frida`, `xposed`, `magisk`, `ro.debuggable`, `isDebuggerConnected`, `substrate`, `gdbserver`, `anti_debug`. `ptrace` only appears in `libxcrash_dumper` (crash dump purpose). → **native layer does not see anti-debugging/anti-injection**.
- **[Observation]** 18/18 `stripped` + `BIND_NOW` (symbol table clipping + immediate binding, regular reinforcement, non-packing).
- **[Observation]** The only **active defense logic** is the "package name whitelist + certificate SHA-256 fingerprint" verification of `libsec-common-utils` (4 whitelist package names + 1 hard-coded fingerprint).
- **[Observation]** does not see any .so being packed/self-decrypted (each library `.rodata` has a complete and readable C++ RTTI class name, source file path, log format string - the packed library will not have these).

### 3.5 High-risk ability classification (answer item by item according to the survey outline)
| Library | High risk/sensitive abilities | Basis (evidence) |
|---|---|---|
| **libtcp** | ★★★ Server command channel + SMS text analysis + IMEI/IMSI/model reporting | `U_URL` endpoint, `exec_protocol`=b64+json disassembly, `parse_sms_with_tpl_list`, `getSubscriberId`/`setImsi`/`collectUserData` |
| **liblib-riskcontrol** | ★★★ Sensitive word review (can be applied to any text); the dictionary can be overwritten by external paths | TrieTree/AES export, `censor_source`, `/storage/emulated/0/censor_source`, `hasSensitiveWords` JNI |
| **libsec-common-utils** | ★★☆ Key center + caller signature access control (fingerprint pinning) | 6 `get*Key` JNI, `getSignature` reflection chain, whitelist package name, hard-coded `C8:A2:...` |
| **libmicontinuity_sdk** | ★★☆ Inter-device channel (file/byte stream) + input event reading | `ChannelImplBySocket::SendFile/SendBytes`, `NInputReader_read`, socket/epoll import |
| libjni_load_serinum | ★★☆ Read device unique ID (MTK OTP) | `/dev/otp`, only `open`+`ioctl`, MTK compiled string |
| libxcrash_dumper | ★★☆ `ptrace` any process + memory dump + `popen` | `ptrace`/`__libc_init`/`popen`/`regexec`/`XZ_SIG` |
| libjni_sound_effect_2_0 | ★★☆ Voiceprint transformation (WORLD voice change) | `Dio/CheapTrick/D4C/Synthesis2` full set of export |
| libmnncore + MNN series | ★☆☆ End-to-end inference + **End-to-end training** | `nativeTrainModel`(12.9 KB)、`nativeLoadMapFromFile`(17.5 KB) |
| libnap | ★☆☆ Traffic statistics by UID (read /proc) | `/proc/net/xt_qtaguid/stats` |
| libmiffmpeg + libfastplayer | ★☆☆ Universal decoding + protocol stack (can access any URL) | `getaddrinfo/socket/connect` import, `Lavf58.45.100` |
| libtensorflowlite_jni / libMNN / libMNNTrain / libMNN_Express / libc++_shared | ☆ Universal third-party runtime | kernel/class name list |
| libjni_sound_effect | ☆ Sound effect DSP | `CMusicLimiter_*`/`BiquadClass`/`RingBuffer` |
| libxcrash | ★☆☆ Signal capture + root path detection | `/data/local/*/su`、`sigaction/sigaltstack` |

---

## 4. Unsolved mysteries (clearly marked for subsequent convergence)

1. AES key/IV source **for**`liblib-riskcontrol`: All 48 candidate constants failed to decrypt; S-box/inverse S-box obfuscation items have been excluded. May be passed in from the Java side `SensitiveWordDetectProvider`, or derived from the package name/signature. → Requires Java side crossover (`getDecodeSecretKey` for `SensitiveWordDetectProvider` + `libsec-common-utils`) or real machine hook `AES::KeyExpansion`.
2. **`assets/censor_source` Plain text content**: Currently there is only ciphertext and structural evidence of "line-by-line insertion into TrieTree", **does not have the vocabulary itself**. It can be solved after solving the previous one.
3. Which certificate **`libsec-common-utils` hard-coded fingerprint `C8:A2:E9:...:2A:B8` belongs to**: confirmed ≠ this APK signature certificate (`c9009d01...`). It is guessed to be the MIUI platform/system signing certificate, or the old channel package certificate. → MIUI platform certificate sample is required for comparison.
4. How to splice `WALI_KEY_SEG_*` of **`libtcp`, and whether it is really AES after splicing. Key**: All 13 segments of literal values ​​have been parsed, but the splicing order/truncation rules have not been restored (`init_weak_string`/`destroy_weak_string` are suspicious assembly functions). In addition: multiple fragments are decoded into Chinese spam text messages, indicating that they also serve as text message corpus.
5. The specific writing path of `setimsi` of **`libtcp` is**: The first 30 instructions of `java_tc_plugin_setimsi` only see the parameter non-null check + `cmp` boundary check + return code `2/4`, and have not reached the `TelephonyManager` reflection call point (function 492 B, `a64scan` does not follow conditional branches).
6. Whether the **`libmicontinuity_sdk` channel is encrypted**: There is no `SSL_*`/`libssl` in the import, tending to be self-developed or plain text; whether the `Rtm` layer has its own encryption is not certified.
7. **`libjni_load_serinum`’s ioctl request number and return structure**: requires AArch64 decompiler (not available on this machine) or real machine strace.
8. **`libmnncore` Training data source**: `nativeLoadMapFromFile` Whether the map file read contains user behavior data depends on the Java side caller and has not been checked.

---

## 5. Evidence index (reproducible)

| Category | Path |
|---|---|
| target library | `_analysis/apk_full/lib/arm64-v8a/*.so`（18）、`_analysis/apk_full/lib/armeabi-v7a/*.so`（18） |
| Full scan report | `_analysis/tools/native/report_arm64.txt`, `report_v7a.txt` (each database: size/architecture/stripped/number of exports/NEEDED/DT_FLAGS/JNI entrance/other export/import/URL/domain name/key string) |
| Full string string per library | `_analysis/tools/native/<lib>.so.strings.txt` (including file offset, format `<offset> <text>`) |
| Export table by database | `_analysis/tools/native/<lib>.so.exports.txt`（`addr\tsize\ttype\tbind\tname`） |
| Key function disassembly | `_analysis/tools/native/decomp_notes.txt` (9 functions annotated disassembly + string/call summary) |
| `libsec` full symbol scan | `_analysis/tools/native/sec_all.txt` (521 exports × ≤26 instructions, used to locate fingerprint reference point 0x1ed0c) |
| Tool source code | `_analysis/tools/{elfscan,strings_human,resolve_ptrs,dump_sym,a64scan,censor_probe}.mjs` |
| APK signature evidence | REA `inspect_apk_signature` evidence `ev_0040f79d78dc46592c3b7b1aa1c044de0b02498c0ae9b2acfd9e45d2ae87f2ea` (cert SHA-256 `c9009d01...49025`, v1/v2/v3 consistent) |
| `libtcp` export evidence | REA `inspect_native_exports` evidence `ev_2f72d73e1abfefa6b799a8b35edd6031bb6c001e9c60ab5afc7807f3666392bb` (393 exports / 452 entries, coverage=complete) |
| Evidence that the tool is unavailable | `open_binary(ghidra)` → `target_format_unsupported`（“Windows Ghidra P0 accepts native PE and explicitly selected raw 6502 targets only”）；`inspect_with_objdump` / `execute_rizin_command` → `ERR_REA_WINDOWS_NATIVE` |
