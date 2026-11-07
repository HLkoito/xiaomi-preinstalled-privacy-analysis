> **Historical analysis with subsequent corrections.** Use the [final review](../REPORT.md) for the reviewed findings. It supersedes earlier claims about plaintext IMEI lists, actual uploads under default settings, absolute coverage, and unverified native behavior. The DAU IMEI and IMSI lists use MD5 and remain subject to region, permission, and network checks. Certificate expiry alone does not establish that ciphertext cannot be decrypted. Local identity paths were removed. Historical source line references may point to files outside the published evidence set.

# Native layer (`.so` + `assets` load + JNI boundary)

> Analyst: `t-native` | Target: `GuardProvider-2.0.3-20231124.0.apk`
> SHA-256 `b0c02fa682b8ca82429b471113d0702042ac76234c85fdcd8efeb7c03d313024`
> Material root: `_analysis\apktool\lib\arm64-v8a\` (12 `.so`), `_analysis\apktool\assets\` (8 files)
> Annotation convention: **[observation]** = bytes/symbols/source code are directly visible; **[inference]** = well-founded semantic induction; **[unknown]** = not covered in this round.
>
> **tool limitation (important, documented in §7-U0)**: The Ghidra provider for REA only accepts **PE / explicit raw 6502** on win32, rejecting ELF-arm64;
> Hopper macOS only; IDA/Kuna not configured. Therefore, `binary_overview` / `list_procedures` / `analyze_function` / decompilation is not available for **in the native**.
> Use instead: ① REA engine-free tool `inspect_native_exports` (4 times, including evidence_id); ② Self-written ELF64/ELF32 parser (PowerShell/.NET);
> ③ Self-written AArch64 `ADRP+ADD` instruction decoder, mechanically restoring "function → quoted string"; ④ Full library Latin1 regular string forensics.
> All ARM-level conclusions in this report come from ③④ and are not decompiled products.

---

## 1. Overview of Responsibilities

[Observation] The native layer of this APK is assembled from **three unrelated technology stacks**. They have no `DT_NEEDED` cross-dependence on each other and are only combined in the Java layer:

| stack | Provider | Library | Responsibilities |
|---|---|---|---|
| **A. Tencent AVL engine** | Tencent (`com.avl.engine.*`) | `libavlasys.so`(JNI shell) + `libavlm.so`(engine) + `libavlurl.so`(URL engine) + `liburldetectorsys.so`(URL JNI shell) + `assets/avlsdk` | Local virus scan / APK analysis / certificate summary / URL credibility determination |
| **B. Tencent TMSDK 2.0.13** | Tencent (`tmsdk*`) | `libTmsdk-2.0.13-mfr.so` + `libams-1.3.0-mfr.so` + `libdce-1.1.21-mfr.so` + `libturingvd.so` + `assets/70003.amf` + `assets/40706.dat` + `assets/mdetector` + `assets/licence*.conf` | String encryption and decryption (TccCryptor), AMF malicious feature matching, deep cleaning (DCE), Turing device risk control, network detection |
| **C. National secret code + Xiaomi self-developed + xcrash** | Tencent / Xiaomi / iqiyi | `libtmf-gm.so`(SM2/SM4) + `libflamingo.so`(Rust regular engine) + `libxcrash*.so`(native crash) | National secret algorithm, Rust multi-pattern matching, native crash tombstone |

[Observation] **12 `.so` are all `ELF64 / little-endian / AARCH64 / ET_DYN`, all stripped** (no `.symtab`, only `.dynsym`).
All with `PT_GNU_RELRO`; only `libflamingo.so` and `libturingvd.so` are set to `DT_FLAGS=BIND_NOW` (the rest are lazy binding).
All PIE (`ET_DYN`), no `DF_TEXTREL`. No library in **is packed with** as a whole (the section names are all standard GNU section names, `.text/.rodata/.data.rel.ro/.eh_frame` is complete, and there is no `upx`/`jiagu`/custom section).

[Observation] `libxcrash_dumper.so` is the only library **of** with `PT_INTERP` (`/system/bin/linker`) and `.preinit_array`,
The essence is fork/exec type **independent executable**, not an ordinary shared library (see §2.2.12).

---

## 2. Component and file list

### 2.1 `.so` full quantity list (12/12, no omissions)

| # | File | size | SHA-256 | SONAME | Type/Architecture | stripped | JNI entrance form | Definitive in one sentence |
|---|---|---|---|---|---|---|---|---|
| 1 | `libams-1.3.0-mfr.so` | 744,392 | `2FE3276BF1FE734AA6847F2E3778CD726FF971615BD675CEAA729751F4914BEF` | `libams-1.3.0-mfr.so` | ELF64 AArch64 ET_DYN | Yes | **8 pieces `Java_tmsdk_..._AmScannerV2_*`** (direct export) | TMSDK AMF Malicious signature scanner (matched with `70003.amf`) |
| 2 | `libavlasys.so` | 89,144 | `8FEC8CF73748963141206CA29CD0FC15D54FF43129AF573ED9E5EAB6BFE12FF0` | `libavlasys.so` | Same as above | Yes | ** None `Java_*`**; `JNI_OnLoad`@0x105fc + RegisterNatives | AVL SDK for JNI case (registered `AVLA`+`NativeUtils`) |
| 3 | `libavlm.so` | 2,304,352 | `65EC01AE59795DC1C17DDCE8B1BAA2EE01DC911C176A336F42432ECA5BAD7688` | `libavlm.so` | Same as above | Yes | **without** (Pure C API `AVLM_*`, 98 exports) | AVL local scan engine body |
| 4 | `libavlurl.so` | 629,976 | `54DFE75A6F6154D787B969E17CF752D32EA337D294F84ACB2F3F72EF3B2BEC20` | **`liburl.so`** (≠ file name) | Same as above | Yes | **without** (Pure C API `AVLM_Match_*`, 33 exports) | AVL URL Reputation Engine |
| 5 | `libdce-1.1.21-mfr.so` | 202,976 | `6670CA20A1D60FFC22A9276D8232400CC5E53EFAF6037C78B49DBDBB2CDABE95` | `libdce-1.1.21-mfr.so` | Same as above | Yes | **None `Java_*`**; `JNI_OnLoad`@0x8920 + RegisterNatives | TMSDK DeepCleanEngine deep cleaning |
| 6 | `libflamingo.so` | 1,506,088 | `BEE0D6ECF1E24AB282CD014958DC23A0675958021A979E2F8CFB9BDD4C0371B9` | **empty** (no SONAME) | Same as above | Yes | **3 `Java_com_miui_..._RustSupport_*`** | Xiaomi self-developed Rust regular/multi-pattern matching engine |
| 7 | `libtmf-gm.so` | 177,920 | `D27A1A6B63F6453904FFDAC0147FCD8D062871256AB26E9DA69FC6981F82C956` | `libtmf-gm.so` | Same as above | Yes | **8 pieces `Java_com_tencent_tmf_gm_{Sm2,Sm4}_*`** | National secret SM2/SM4 JNI implementation |
| 8 | `libTmsdk-2.0.13-mfr.so` | 370,088 | `7DA1A5FEE5CD45B0C9834D47F346E45F7B1DCC247B1DE7BE1F199999F033EAF2` | **`libTmsdk-2.0.12-mfr.so`** (version does not match) | Same as above | Yes | ** None `Java_*`**; `JNI_OnLoad`@0x1f480 + RegisterNatives (only 5 exports) | TMSDK core (TccCryptor / number ownership / yellow pages / differential package) |
| 9 | `libturingvd.so` | 240,448 | `E4068BF300A633A3840783969EA715A806A988188BEFBF32B17CF56D414966A6` | `libturingvd.so` | Same as above | Yes | ** None `Java_*`**; `JNI_OnLoad`@0x1c05c + RegisterNatives | Tencent Turing Device Risk Control SDK |
| 10 | `liburldetectorsys.so` | 14,408 | `E22142D0D2591A63BC804FAC00906AAE3B6826FAC7C9D9C7D2EEBE726FE3A4F8` | `liburldetectorsys.so` | Same as above | Yes | ** None `Java_*`**; `JNI_OnLoad`@0x1db4 + RegisterNatives | Ultra-thin dlopen forwarding case for `UrlDetector` |
| 11 | `libxcrash.so` | 88,360 | `BE831E703CA4D597C531C9E43FEA40210C4E53070F3FE8C1F1F843E11E3D0CCA` | `libxcrash.so` | Same as above | Yes | ** None `Java_*`**; `JNI_OnLoad`@0x328c + RegisterNatives | xcrash native crash capture main library |
| 12 | `libxcrash_dumper.so` | 129,344 | `0CB766583D1ACFC3254817F305721DD13467571BC024ADD2053B33D7B56D3A25` | **empty** + **yes `PT_INTERP`** | Same as above | Yes | None | xcrash independent dumper **executable** (ptrace catches tombstone) |

> [Observation] **`libtmg-gm.so` written in the task list does not exist as** in APK; there is only `libtmf-gm.so` (177,920 B) under `lib/arm64-v8a/`. See §7-U1.
> [Observation] There are only these 12 files **under `lib/arm64-v8a/`** (`Get-ChildItem -File` is exhaustive, no subdirectories).

---

### 2.2 Independent identity table for each `.so`

#### 2.2.1 `libams-1.3.0-mfr.so` — TMSDK AMF scanner

| item | value |
|---|---|
| ELF | `ELF64` LE `AARCH64` `ET_DYN`（PIE），entry `0xb380` |
| `DT_SONAME` | `libams-1.3.0-mfr.so` |
| `DT_NEEDED` | `libz.so, libdl.so, libstdc++.so, libm.so, libc.so` |
| Festival | 22 sections, including `.gcc_except_table`, `.data.rel.ro`; `.text` 0x81fbc, `.rodata` 0x4d78 |
| build-id | **without** (without `PT_NOTE`) |
| `.comment` | 0x25 B (NDK clang, short form) |
| Export | 130 dynsym / 12 globals |
| **JNI Export (8)** | `Java_tmsdk_common_module_qscanner_impl_AmScannerV2_initScanner`@0xd3c8<br>`..._newObject`@0xf000 `..._deleteObject`@0xd528<br>`..._scanApkBytes`@0x1bde0 (768B) `..._extractApkInfo`@0x1c0e0<br>`..._getOpcode`@0x1dbd4 (3488B, maximum) <br>`..._nativeLoadAmfHeaderBytes`@0x1c514 `..._nativeUpdateMalwareInfoBytes`@0x21080 |
| Other exports | `_Znwm`/`_Znam`（operator new）、`VERSION` OBJECT、`_edata/_end/_bss_end__` |
| Packing/obfuscation | **without case**. But `.comment` is extremely short and has no build-id; **signature library string is encrypted** (see AMF) |
| key string | `.rodata`@0x8db48：`amf` `.amf` `AMF` `MalwareData` `PackageName` `CertMd5` `ApkSize` `ClassPrefix` `ClassSuffix` `ZipEntry` `ApkVersion` `MinClassSuffix` `ConstantString` + two suspicious constants `!@#$%^&*()_JHGFD`, `SfJ(*W#&Ddweir309*&@#!_)($39`; `tmsdk/common/TMSDKContext` `checkLisence ()Z` |
| Ability | `dlopen/dlsym/dlclose`, `mmap/mprotect`, `zlib inflate/deflate`, `sigaction/sigsetjmp` (self-collapse protection), `dl_iterate_phdr` |

[Inference] `!@#$%^&*()_JHGFD` / `SfJ(*W#&Ddweir309*&@#!_)($39` is the **table entry name obfuscation key** (string-table XOR key) of the AMF container,
Because they are stored close to table entry names such as `AMF.MalwareData`, and the length/character set conforms to TMSDK's consistent "print ASCII as XOR key" style.

#### 2.2.2 `libavlasys.so` — AVL JNI shell

| item | value |
|---|---|
| ELF | `ELF64` LE `AARCH64` `ET_DYN`，entry `0x2c00` |
| SONAME / NEEDED | `libavlasys.so` / `liblog.so, libz.so, libandroid.so, libc.so, libm.so, libstdc++.so, libdl.so` |
| build-id | `1b6e69c2a46c537b6d8bd5a7ba7448fd7206d537` |
| `.comment` | 0x64 B |
| Export | 87 dynsym, **has only 3 real functions**: `JNI_OnLoad`@0x105fc(204B), `AVL_SetScanLimitEx`@0xe004, `AVL_SetMapleScanLimit`@0xdfec |
| **JNI Form** | **RegisterNatives** (without `Java_*`). [Observation] Use the self-built ADRP+ADD decoder to scan `.text`(0x2c00/0xdd10) and get the string reference **at**316, of which `JNI_OnLoad` directly references **exactly 2 Article**: `com/avl/engine/security/NativeUtils`, `com/avl/engine/security/AVLA` → **machine certificate: This library is the natives implementation of these two classes** |
| Key string (.rodata @0x10ef0 concatenated into a string, that is, JNINativeMethod table) | `./conf/liscense.conf`、`./avl.avl`、`/avl/conf/avllib.conf`、`assets/avllib`、`arm64-v8a`、`dlopen error: %s`、`so name is NULL`、`key is overdue!`、`AVL SDK licence is overdue!`、`%s is test key.`、`key is test key!`、`AVLSDK WARN`、`INIT_FAIL`、**`62BAFD662A63A99A7C96DA027AFDF739`** |
| Ability | `AAssetManager_*` (read assets), `dlopen/dlsym`, `regcomp/regexec` (regular), `socket`, `ioctl`, `mmap`, `__android_log_print` |

#### 2.2.3 `libavlm.so` — AVL scan engine body

| item | value |
|---|---|
| ELF | `ELF64` LE `AARCH64` `ET_DYN`，entry `0x3f390` |
| SONAME / NEEDED | `libavlm.so` / `libz.so, libc.so, libm.so, libstdc++.so, libdl.so` (**without liblog**, use `__assert2`/`printf`) |
| build-id | `dd31d0086fa153d701381d6671a92fa1a2ba6d79` |
| `.comment` | `Android clang version 3.8.256229 (based on LLVM 3.8.256229)` → **2016 NDK r11 series** [observation] |
| Export | **98** (REA `inspect_native_exports` review: `examined_entries 248, exports 98, coverage complete`) |
| **JNI** | ** does not have any `Java_*`**; driven by `libavlasys.so` `dlopen`+`dlsym` |
| Anti-hook design [observation] | Multiple `AVLM_*` exports `size=4` (springboard stub), and **exports an additional 10 meaningless 4-letter aliases**: `aesd`@0xb14f8 `adce`@0xb14f4 `effe`@0xb1508 `efff`@0xb1504 `fffe`@0xb1500 `exdz`@0xb14fc `wtsr`@0xb15d0 `haoz`@0xb15d4 `zavt`@0xb157c(84B) `brmy`@0xb1514(104B). <br> [Inference] The same implementation hangs two exports of "official name + random alias", making the hook hooked by `AVLM_*` invalid. |
| Data symbols in `.data` | `sigx_app`@0x2596e0 (8B, OBJECT)、`sig_app`@0x2596e8 (8B, OBJECT) |
| Characteristics of string encryption [observation] | The **string is packed into continuous blob**: `.rodata` appears `http://.https://.Content-Length:.www.baidu.com.GET`, `...SMS_Send.SMS_Block.System_DexLoad.System_ReflectInvoke.System_ReflectGet.Notification...` - fixed-length tables concatenated with `.` (0x00/0x01 separated), which is a typical "string table + length index" confusion |
| Clear text domain name (see §5.1) | `www.baidu.com`、`0..3.north-america.pool.ntp.org`、`www.wapforum.org`、`www.w3.org`、`openssl.cn`、`www.openssl.org` |
| Network capabilities [observation import] | `socket connect send sendto recv recvfrom gethostbyname inet_addr inet_ntoa inet_ntop select poll shutdown getsockopt` (**has complete network outgoing capabilities**) |
| Anti-debugging related import | **None `ptrace`**; Yes `syscall`, `getuid`, `getpid`, `gettid`, `readlink`, `tcgetattr/tcsetattr`, `getenv` → [Inference] used for environmental detection, excluding ptrace Anti-debugging |

#### 2.2.4 `libavlurl.so` — AVL URL reputation engine

| item | value |
|---|---|
| ELF / SONAME | `ELF64` LE `AARCH64` `ET_DYN`, entry `0x325d0`; **`DT_SONAME = liburl.so` (inconsistent with the file name `libavlurl.so`)** |
| NEEDED | `libz.so, libstdc++.so, libm.so, libc.so, libdl.so` |
| build-id | **No** |
| Export | **33** (REA review: `examined_entries 139, exports 33`), all `AVLM_*` |
| **JNI** | **without**; from `liburldetectorsys.so` dlopen `liburl.so` after dlsym |
| key string | `liburl.so`（SONAME）、`%s.new`、`%stmp/`、`%s.tmp/`、`%s/conf/avllib.conf`、`V([0-9]+) \(Build ([0-9]+)\)`、`HTTP/1.1`、`http://%s` |
| clear text domain name | **400** (after deduplication), but **are all public suffix/free hosted domain name whitelist**, not C2 (see §5.2) |
| Network capabilities | `socket connect send sendto recv recvfrom gethostbyname inet_addr inet_ntoa inet_ntop select getsockopt setsockopt` |

#### 2.2.5 `libdce-1.1.21-mfr.so` — Deep Cleaning Engine

| item | value |
|---|---|
| ELF / SONAME | `ELF64` LE `AARCH64` `ET_DYN`，entry `0x7c60`；`libdce-1.1.21-mfr.so` |
| NEEDED | `liblog.so, libstdc++.so, libm.so, libc.so, libdl.so` |
| build-id | None |
| Export | **329**, but **327 are statically linked libstdc++ symbols** (`_ZTVSt12out_of_range`, `_Unwind_*`, `__gxx_personality_v0`, `_ZTS*`, etc.); the real self-export only **`JNI_OnLoad`@0x8920** |
| **JNI Form** | RegisterNatives. [Observation] `.rodata`@0x27158 Registration text: Class `tmsdk/common/tcc/QFile`, `tmsdk/common/tcc/QSdcardScanner`, `tmsdk/common/tcc/SdcardScannerFactory`, `tmsdk/common/tcc/DeepCleanEngine` |
| Method signature table (observation original text) | `nativeSetWhiteList (J[Ljava/lang/String;)V`, `nativeSetRule (JI[Ljava/lang/String;[Ljava/lang/String;)V`, `nativeInit`, `nativeRelease`, `nativeCancle`, `nativeScan (JLjava/lang/String;)V`, `nativeSetProgressListenLevel (JI)V`, `nativeDeleteAllChildren (Ljava/lang/String;)I`, `nativeDeleteAllChildrenByDay (Ljava/lang/String;I)I`, `nativeList (Ljava/lang/String;)[Ltmsdk/common/tcc/QFile;`, `nativeFillExtraInfo (Ljava/lang/String;)V`, `nativeRemoveEmptyDir`, `nativeAllocate (IJ)J`; `DeepCleanEngine`: `create (I)J` `setWhitePaths` `setRootPaths` `setOtherFilterRule` `scanPath (JLjava/lang/String;...)`; callback `onFound (ILjava/lang/String;IJJJJ)V`, `onProgressChanger (Ljava/lang/String;)Z` |
| internal class name | `12InnerScanner` `16DeepInnerScanner` `16WideInnerScanner` `11BaseVisitor` `13NormalVisitor` `10PathFilter` `15WhitePathFilter` `16ComRubbishFilter` `17SoftRubbishFilter` `14EmptyDirFilter` `13HideDirFilter` `13DirPathFilter`、`nomedia` |
| Ability | File system recursive deletion/enumeration (`nativeDeleteAllChildren*`, `nativeRemoveEmptyDir`) - **This is the only library among the 12 libraries that has the "batch delete any path" primitive** |

#### 2.2.6 `libflamingo.so` — Xiaomi Rust matching engine

| item | value |
|---|---|
| ELF / SONAME | `ELF64` LE `AARCH64` `ET_DYN`, **entry `0x0`, None SONAME**; `DT_FLAGS=BIND_NOW`; `.gnu.hash` only (No SysV `.hash`) |
| NEEDED | **only `libdl.so, libc.so`** |
| `.note.android.ident` | `r25b` / `8937393`（NDK r25b） |
| `.comment` | 0xb1 B |
| Export | **3** (REA review `exports 3`): <br>`Java_com_miui_guardprovider_natives_RustSupport_matches`@0x9d760 (480B)<br>`Java_com_miui_guardprovider_natives_RustSupport_search`@0x9d940 (664B)<br>`Java_com_miui_guardprovider_natives_RustSupport_search___3B_3Ljava_lang_String_2`@0x9dbd8 (1832B) |
| **Overload Analysis [Observation]** | The third symbol has the suffix `__<mangled sig>`, corresponding to `search(byte[], String...)`; JVM first checks the name with the signature suffix for the overloaded native method, which is completely consistent with the two `search` overloads **of `RustSupport`** |
| Ability | No network, no `dlopen`, no crypto; import only `memcpy/memcmp/memset/malloc/memalign/realloc/calloc/free`, `pthread_*`, `mmap/munmap`, `syscall`, `realpath/getcwd/getenv`, `dl_iterate_phdr`, `getrandom`, `writev`, `__register_atfork` |

[Inference] `RustSupport.matches(byte[],String)` → single regular matching; `search(byte[],String)` → single pattern search;
`search(byte[],String...)` → Aho-Corasick multi-mode batch search, returns `boolean[]`.
Java side call points `p0/y.java:111` `RustSupport.matches(bArr, t0.j.a(stringItem))`, `p0/y.java:117` `RustSupport.search(bArr, stringItem.getValue())`
——That is, **performs characteristic string/regular matching on file bytes**, which is the core of MIUI's self-developed scanning engine.

#### 2.2.7 `libtmf-gm.so` — National Secret SM2/SM4

| item | value |
|---|---|
| ELF / SONAME | `ELF64` LE `AARCH64` `ET_DYN`，entry `0xfd0` |
| NEEDED | `liblog.so, libm.so, libstdc++.so, libdl.so, libc.so` |
| build-id | `f6f9519193063732eca0abc789d4a876dc84ad86` |
| `.comment` | `Android clang version 5.0.300080 (based on LLVM 5.0.300080)`〔Observation〕 |
| Export | 40 dynsym, ** 8 `Java_*` + 9 `_ZN7_JNIEnv*` inline JNIEnv wrapper ** |
| JNI full table | `Java_com_tencent_tmf_gm_Sm2_doSm2Decrypt`@0x1bd4(512B)<br>`..._Sm2_doSm2Decrypt2`@0x1f54(328B)<br>`..._Sm2_doSm2Encrypt`@0x18fc(728B)<br>`..._Sm2_doSm2Encrypt2`@0x1dd4(384B)<br>`..._Sm4_doSm4EncryptCBC`@0x156c(456B)<br>`..._Sm4_doSm4DecryptCBC`@0x1734(456B)<br>`..._Sm4_doSm4EncryptECB`@0x100c(400B)<br>`..._Sm4_doSm4DecryptECB`@0x13dc(400B) |
| Ability [observation import] | `memcmp memset memcpy malloc free strlen rand srand time __stack_chk_fail __assert2` only. <br>**No network, no files, no dlopen** → Pure algorithm library.|
| **risk point [observation]** | Import **`rand` + `srand` + `time`**: SM2 encryption requires random k. If libc `rand()` is used to seed `time()`, k is predictable → the private key is recoverable. See §6-R3 |

#### 2.2.8 `libTmsdk-2.0.13-mfr.so` — TMSDK core

| item | value |
|---|---|
| ELF / SONAME | `ELF64` LE `AARCH64` `ET_DYN`, entry `0x30d0`; **`DT_SONAME = libTmsdk-2.0.12-mfr.so`** (file 2.0.13 / soname 2.0.12, version inconsistent) |
| NEEDED | `libz.so, liblog.so, libstdc++.so, libm.so, libc.so, libdl.so` |
| build-id | None |
| Export | **5** (REA review `exports 5`): `JNI_OnLoad`@0x1f480(156B) + `VERSION`(OBJECT) + `_edata/_end/_bss_end__` |
| **JNI Form** | RegisterNatives. [Observation] `.rodata`@0x484e0 Registration indication genre: `tmsdk/common/tcc/TccCryptor`, `tmsdk/bg/tcc/TelNumberLocator`, `tmsdk/bg/tcc/NumMarker`, `tmsdk/common/tcc/TrafficSmsParser`(+`$MatchRule`), `tmsdk/common/YellowPages`, `tmsdk/common/TMSDKContext`, `com/tencent/diffupdate/TccDiff` |
| `TccCryptor` method [observation] | `makePassword ([B)[B`、`encrypt ([B[B)[B`、`decrypt ([B[B)[B`、`add (Ljava/lang/Object;)Z`、`length ()I`、`replace (IILjava/lang/String;)Ljava/lang/StringBuffer;` |
| Others | `TelNumberLocator`: `newObject ()J` `init (JLjava/lang/String;Ljava/lang/String;)I` `patch` `getLocation` `getMobileNumLocation` `getAreaCodeLocation` `getCountryCodeLocation` `getAreaCode (J)[I` `getCountryCode` `deleteObject (J)V` `getYellowPages` `getDetailYellowPages` `getProvinceNameList` `getCityNameList`、`gbk`；`NumMarker`: `nNewInstance (I)J` `nDestroyInstance` `nSetPath (JLjava/lang/String;)Z` `nGetHeaderInfo` |
| error string | `data too large`、`java/lang/RuntimeException`、**`Bad encrypted data`** |
| Ability | No socket, no dlopen; only file stdio + time + pthread → pure local library |

#### 2.2.9 `libturingvd.so` — Tencent Turing Risk Control

| item | value |
|---|---|
| ELF / SONAME | `ELF64` LE `AARCH64` `ET_DYN`, entry `0x3860`; `DT_FLAGS=BIND_NOW`; both `.hash` and `.gnu.hash` |
| NEEDED | `liblog.so, libandroid.so, libz.so, libm.so, libstdc++.so, libdl.so, libc.so` |
| build-id | `e89e7f60fb3847a24c7ebc98433311dee31070a3` |
| `.note.android.ident` | `r20` / `5594570` |
| Export | 112 dynsym, the real export is only `JNI_OnLoad`@0x1c05c(416B) |
| **JNI Form** | RegisterNatives. The class name appears in the form `$` (the DEX class descriptor really contains `$`): `com/tencent/turingfd/sdk/vd/TNative$aa`, `.../TNative$aa$bb` |
| Registered methods (aligned one by one with Java declarations) [Observation] | `(Landroid/util/SparseArray;Landroid/content/Context;Ljava/util/Map;I)Landroid/util/SparseArray;`→`a`<br>`(Landroid/util/SparseArray;[BLjava/util/Map;I)...`→`b`<br>`(Landroid/util/SparseArray;Landroid/content/Context;)...`→`c`<br>`(Landroid/util/SparseArray;Landroid/content/Context;I)...`→`d`<br>`(Landroid/util/SparseArray;Landroid/content/Context;Ljava/util/Map;I)...`→`e`<br>`(Landroid/util/SparseArray;[BI)...`→`f`<br>`(...Context;Map;I)`→`g`/`h`、`(...Context;Map)`→`i`<br>`TNative$aa$bb.onServiceConnected (Landroid/content/ComponentName;Landroid/os/IBinder;)V` |
| version string | `TuringFD v%d (%s, %s, %s, compiled %s)`、`vd.5e7c85b` |
| **risk control string [observation]** | `turingRiskDetect` `riskCheckWup` `pkgVerInfo` `apiLevel` `brand` `model`<br>`/sys/fs/selinux/enforce` (read SELinux status) <br>**kernel thread/root Process regular**: `^/system/xbin/ku[.]sud$|^daemonsu:|^k_worker/[1-9][[:digit:]]*:[[:digit:]]+$|^kr_worker/...|^km_worker/...|^tworker/...|^tu_worker/...|^tq_worker/...|^kworker/[1-9][[:digit:]]{2}$|^permmgrd$|^360sguard$|^/data/data/[[:alnum:]_.-]+/|^/system/bin/[.]|^/system/xbin/[.]` |
| Ability [observation import] | **`popen`/`pclose` (execute shell command)**, `ASensorManager_getInstance` `ASensorManager_getSensorList` `ASensor_getVendor` `ASensor_getName` `ASensor_getType` `ASensor_getResolution` (**read sensor → Simulator fingerprint**), `statfs`, `getuid`, `readlink`, `syscall`, `pthread_create`, `uname`? |

#### 2.2.10 `liburldetectorsys.so` — UrlDetector JNI Shell

| item | value |
|---|---|
| ELF / SONAME | `ELF64` LE `AARCH64` `ET_DYN`，entry `0xe00`；`liburldetectorsys.so` |
| NEEDED | `liblog.so, libc.so, libm.so, libstdc++.so, libdl.so` |
| build-id | `73e529d1ab581f08eec4f44a77cb90806f2db303` |
| size/section | 14,408 B; `.text` is only 0x1098 (4.2 KB) - **is the smallest function library in the entire library** |
| Export | 26 dynsym, the real function is only `JNI_OnLoad`@0x1db4(228B) |
| **JNI Full table [observation, .rodata@0x213a plain text]** | Class `com/avl/engine/urldetector/UrlDetector`; method signature `setLibPath (Ljava/lang/String;)I`, `loadURL`, `unLoadURL ()V`, `matchURL (Ljava/lang/String;Z)I`, `matchURLEx (Ljava/lang/String;Z)[Ljava/lang/String;`; error string `class %s  not found!` |
| behavior (observation) | import only `dlopen dlsym dlerror dlclose strdup sprintf strlen memcpy memset free __android_log_print` → **pure dlopen forwarding layer**, does not contain the URL algorithm itself |
| Load target [observation] | The dlsym name after `dlopen` in the string `liburl.so` and `.text` comes from `AVLM_MatchWithType`/`AVLM_GetSIDInfo`/`AVLM_FreeSIDInfo` - that is, dlopen **`liburl.so`** at runtime |

#### 2.2.11 `libxcrash.so` — native crash capture main library

| item | value |
|---|---|
| ELF / SONAME | `ELF64` LE `AARCH64` `ET_DYN`，entry `0x3250`；`libxcrash.so` |
| NEEDED | `libdl.so, liblog.so, libc.so, libm.so, libstdc++.so` |
| build-id | `80a6cbf5457853174f24ffbc0d47b823dd7d0b55` |
| `.note.android.ident` | `r16b` / `4479499` |
| Export | 128 dynsym, its own real export is only `JNI_OnLoad`@0x328c(224B) + a set of `xc_test_crash`@0xbf88 / `xc_test_call_1..4` (for self-test) + static libgcc unwind (`_Unwind_*`, `__register_frame*`) |
| **JNI Full table [observation, .rodata@0xf980]** | Class `xcrash/NativeHandler`; `nativeInit (ILjava/lang/String;×10;ZZIIIZZZZZI[Ljava/lang/String;ZZIIIZZ)I`, `nativeNotifyJavaCrashed ()V`, `nativeTestCrash (I)V`, `nativeTraceSignalInit`, `crashCallback (Ljava/lang/String;Ljava/lang/String;ZZLjava/lang/String;)V` (also `traceCallback`)|
| key string | `%s/libxcrash_dumper.so`、`%s/tombstone_%020lu_%s__%s%s`、`.native.xcrash` `.trace.xcrash` `.clean.xcrash` `.placeholder_`、`/dev/null`、`xcrash_crash_cb`、`set dumpable failed, errno=%d`、`set traceable failed, errno=%d`、`fork failed`、`waitpid failed`、`child terminated by a signal(%d)` |
| Ability [observation import] | `clone execl pipe2 dup2 prctl waitpid sigaltstack eventfd popen pclose` → **fork/exec child process + prctl(PR_SET_DUMPABLE/PR_SET_PTRACER)** |

#### 2.2.12 `libxcrash_dumper.so` — Standalone dumper executable

| item | value |
|---|---|
| ELF / SONAME | `ELF64` LE `AARCH64` `ET_DYN`, entry `0x22e0`; **without SONAME**; **with `.interp` = `/system/bin/linker` (0x15 B)**, `.preinit_array` |
| NEEDED | `libdl.so, liblog.so, libc.so, libm.so, libstdc++.so` |
| build-id | `352d6e6855884b554dd9a7072bce423d8f72c51e`；`.note.android.ident` = r16b / 4479499 |
| Export | 81 dynsym; true: `XZ_SIG`(OBJECT,6B), `CrcUpdateT4`@0xec08, `CrcUpdateT8`@0xecf8, `g_CrcTable`(OBJECT,8192B), `__PREINIT_ARRAY__/__INIT_ARRAY__/__FINI_ARRAY__` |
| Ability [observation import] | **`ptrace`**、`__libc_init`、`alarm`、`waitpid`、`exit`、`regcomp/regexec`、`/proc/%d/maps`、`dl_iterate_phdr`、`dladdr` |
| additional load | Static link into **7-Zip LZMA/XZ SDK** (`XZ_SIG`, `CrcUpdateT4/T8`, `g_CrcTable`) [inference] used to compress tombstone |
| Qualitative | **This is a ptrace-type independent executable file**. `libxcrash.so` starts `execl` after positioning it through `%s/libxcrash_dumper.so`, and attaches back to the parent process to capture the tombstone. |

---

### 2.3 Independent identification form for each asset (8/8, no omissions)

#### 2.3.1 `assets/avlsdk` — AVL signature library ZIP (**plain text container**)

| item | value |
|---|---|
| size | 1,566,355 B |
| SHA-256 | `287370A5AE11E83A535115C289DF1ED42E6C06A9E86C55E3A6CB6DECE100C84B` |
| Magic number [observation] | `50 4B 03 04` = **`PK\x03\x04` (standard ZIP)** |
| structure (observation) | Standard ZIP, `[System.IO.Compression.ZipFile]` opened successfully, **26 entries**, all deflate (method 8) |
| entropy | 8.0 (due to internal compression of ZIP) |
| visible string | `url/url/white_re.avls`、`url/url/conf/avllib.conf320`、`url/url/conf/liscense.conf`、`url/url/fish_re.avl`、`url/url/fish_url.avl` |
| Supporting | `libavlasys.so`（`AAssetManager_open("avlsdk")` + `unzipAssetsFileEfficient`）、`libavlm.so`、`libavlurl.so` |

**26 entries Full table [observation]**

| entry | Compressed/Raw | Semantics (inference) |
|---|---|---|
| `sdk_conf/version.conf` | 77 / 106 | **plain text JSON**, see below |
| `sdk_conf/sdk.conf` | 10,384 / 10,379 | Custom container: ASCII header `00000` + binary segment + base64 segment (including signature) |
| `av/avl/conf/avllib.conf` | 13 / 11 | Plain text `20220928.ah` |
| `av/avl/conf/liscense.conf` | 409 / **404** | Ciphertext (see §4.3.2) |
| `url/url/conf/avllib.conf` | 13 / 11 | Plain text `20180917.zz` |
| `url/url/conf/liscense.conf` | 409 / **404** | Cipher text (same size as above, different content) |
| `av/avl/android/avlpk_sfmf.avl` | 81,484 / 81,459 | feature package (sfmf) |
| `av/avl/android/avlpk_pornware.avl` | **322,489 / 322,455** | Pornographic feature pack (maximum) |
| `av/avl/android/avlpk_apn.avl` | 131,971 / 131,926 | APN/Tariff type |
| `av/avl/android/avlpk_amc.avl` | 54,404 / 54,384 | AMC Features |
| `av/avl/android/avlpk_sgnl.avl` | 6,254 / 6,249 | Signal class |
| `av/avl/android/avlpk_kw.avl` | 4,599 / 4,594 | keyword features |
| `av/avl/android/avlpk_dhc.avl` | 2,960 / 2,955 | DHC |
| `av/avl/android/6947e41c_60f53057.avl` | 5,073 / 5,068 | Anonymous feature package (double hash naming) |
| `av/avl/android/avlpk_des.avl` | 143 / **151** | **description/index**, uncompressed (raw 151 > comp 151) |
| `av/kw/common/{en,zh}/behavior.des` | 258/477、330/711 | **plain text JSON** behavior description |
| `av/kw/common/{en,zh}/common.des` | 329/689、426/1212 | **plain text JSON** general description |
| `av/kw/common/{en,zh}/recommend.des` | 105/259、108/274 | **plain text JSON** suggested description |
| `url/url/fish_url.avl` | 417,486 / 417,356 | Fishing URL library |
| `url/url/white.avl` | 370,431 / 370,316 | URL whitelist |
| `url/url/fish.avl` | 150,818 / 150,768 | Phishing domain name database |
| `url/url/fish_re.avl` | 545 / 540 | Fishing **regular** library |
| `url/url/white_re.avl` | 95 / 100 | Whitelist **regular** library |

**`sdk_conf/version.conf` Complete plaintext [Observation]**
```
{"sdk.conf":"1","av.siglib":"20220928.ah","av.engine":"351","url.siglib":"20180917.zz","url.engine":"124"}
```
→ **AV engine version 351/signature library 2022-09-28; URL engine version 124/URL library 2018-09-17** (URL library is about 4 years behind, see §6-R5).

**`sdk_conf/sdk.conf` structure (10,379 B, initial judgment of** modified lead **) [Observation]**

The overall entropy is 7.979, but the first 380 bytes of **(0x000–0x17B) are pure ASCII text**, not ciphertext:
```
0x000 "3030303030" 10 B text (version/magic number mark, the "00000" mentioned by lead)
0x00A "f5f245c0c3df05ca6bc0e6a" ~23 B of text (hex digest, ~11–12 B of hex transliteration)
0x021 base64 text, 5 lines / ~341 characters → standard alphabet (including + / =), decoded to 253 B binary
          QcLdyKqxfzZ6hUiLUcrKt1CgvXsnc1yMna7ryoJzYbhW2axcwm496MiqsZXqUfeOPBXIgSVfa
          R7MwYKwr+3DgBz38JOXmaEStCD+SS/k/FfQeo18iGdC5a5IwIgdnVVhrWy8SDmjlHgzicbY4HA6n
          arr3CMxKy1WAR9gwTHDwavBBonEPNS0U0Bxn+zGMi5v9lSqZ5IFN63kaAKnSy4U59Nf/sHGh8bcw
          rZb2s8AV4k+Ve+RLE3KKXNGH7Z3n9K4RknQ7ctv31zij6yTteXnl6F423ZhIn7xJixpRs8JUnIzb
          IjZ4zYLy4i+VkQ1PlbIFnlaszjiTfEaQUsOnhw==
0x17A "@" 1 B text (delimiter)
0x17C high entropy binary, 9,999 B (ends at 0x288B=10379)
          0x17C: a2 ed e2 9a 43 4f b9 f7 12 2d a8 d8 57 76 7a b8 14 19 49 7c 8c a2 2a 9d ...
```
The first 48 bytes of the base64 decoding result (253 B): `41c2ddc8aab17f367a85488b51cacab750a0bd7b27735c8c9daeebca827361b856d9ac5cc26e3de8c8aab195ea51f78e`
- High entropy, non-text, [inferred] to be a signature/certificate block or an encrypted key block.

**This explains why lead uses AES to try to solve offset 0/5/16/32 and why all of them are "not a complete block"**: Those 4 offsets all fall in the **base64 text area**, which is not cipher text.
The real starting point of the ciphertext of **is `0x17C` (380)**, the length is `9,999 B`, and **9,999 is not a multiple of 16** (9999/16=624.9) → the segment **is not a whole block AES/SM4-CBC/ECB**，
[Inferred] It is a stream cipher, or a grouping pattern with a trailing nonce/tag, or a non-block layout of `[u32 len][ciphertext]`. **AES-128 `wcw1xbplx7aijwqr` does not hold** in this segment (the length itself excludes pure CBC/ECB).

[Inference] Container semantics = `"00000"` version header + hex digest + base64 (signature/public key) + `@` + encrypted configuration body.
[Unknown] 9,999 B Ciphertext algorithm and key - see §7-U10 (including new, verifiable attempt plan).

**`av/kw/common/zh/common.des` complete plaintext [observation]** (UTF-8 JSON, `\uXXXX` escape)
```json
{"before":"该应用可能存在","middle":"的行为，建议","end":"。","or":"或",
 "des":{"malicious":"该应用会泄露您的隐私或者造成恶意扣费等情况，请立即清理。",
        "risk":"该应用可能会泄露您的隐私或者造成恶意扣费等情况，请谨慎使用。",
        "PayWare":"该应用包含支付插件，可能会进行支付类操作，建议谨慎使用。",
        "AdWare":"该应用包含广告插件，可能会影响您的使用体验并造成流量损耗，建议谨慎使用。",
        "AvTest":"EICAR 反病毒引擎测试文件，没有恶意性。",
        "NotVir":"该应用被多次投诉，可能存在风险，请谨慎使用."}}
```

**`.avl` private container format [observation, summarized by three cases of `white_re.avl`(100B) / `fish_re.avl`(540B) / `avlpk_des.avl`(151B)]**
```
offset width content
0x00 3 "AVL" magic number
0x03 1 u8 type/flag (white_re: 0xFE; des: 0x06)
0x04   4     u32 LE（white_re: 0x00000200；des: 0x00000000）
0x08 4 u32 LE entry number (white_re=3, fish_re=22, des=1)
0x0C 4 u32 LE segment length 1 (white_re=0x68=104, fish_re=0x39B=923, des=0x81=129)
0x10 4 u32 LE segment length 2 (white_re=0x43=67, fish_re=0x1FA=506, des=0)
0x14 8 all 0
0x1C … encrypted/encoded payload (high entropy, no visible plaintext)
```
The 0x08 field of `avlpk_des.avl` is `dd e9 33 63` = `0x6333E9DD` = **1664293341 → 2022-09-27 15:42 UTC** [observed value/inferred as timestamp], and `av.siglib 20220928` matches.

#### 2.3.2 `assets/70003.amf` — TMSDK **virus signature base library**

| item | value |
|---|---|
| size | 680,928 B |
| SHA-256 | `96CD4C5C0D83080311A4CA0F7198B640C36740C66E4F8FADFF0DA76B080E4E06` |
| Magic number [observation] | `61 6d 66 00` = **`"amf\0"`** |
| Version field [observation] | `u32@0x04 = 0x00000004` |
| Head structure [observation] | `0x00 "amf\0"` \| `0x04 u32=4` \| `0x08..0x17` **16 bytes full 0** \| `0x18 8 bytes non-zero` \| `0x20` Starting load |
| `0x18` 8 bytes [observation] | `bd d2 f6 5f e6 75 25 62` = two u32 LEs: `0x5FF6D2BD`(1610015421 ≈ 2021-01-07), `0x622575E6`(1646643174 ≈ 2022-03-07) [Inferred as "baseline date + update date" dual timestamp] |
| load [observation] | From `0x20` 680,896 B = **42,556 × 16 bytes** (divisible by 16 bytes); entropy 8.0; **repeats 16B number of chunks = 0** (`duplicate_blocks=0`) |
| visible string | None (`[\x20-\x7E]{6,}` only hits 15 random noises) |
| Semantics [observation + intersection] | `tmsdk/common/module/update/UpdateConfig.java:197-198`: `String str24 = intToString(70003) + ".amf"; VIRUS_BASE_NAME = str24;` → **`70003.amf` = `VIRUS_BASE_NAME` (TMSDK virus signature base library)**<br> with the same file `:199` `70002.amf = VIRUS_BASE_EN_NAME`, `:201` `70701.amf = PAYLOAD_NAME`, `:209` `AMS_DATA = 100003.dat` |
| Supporting | `libams-1.3.0-mfr.so`: `nativeLoadAmfHeaderBytes` / `nativeUpdateMalwareInfoBytes` / `scanApkBytes` / `getOpcode` consume this container directly |
| structural judgment | [Observation] Non-standard compression (no zlib/gzip/LZMA magic number), 16B divisibility and no duplicate blocks → [Inference] **customizes the encryption feature library** of "group by 16 bytes + group index table", the key candidate can be found in `libams`'s `!@#$%^&*()_JHGFD` (§5.3) |

#### 2.3.3 `assets/mdetector` — **ARM32 Executable (non-shared library)**

| item | value |
|---|---|
| size | 136,636 B |
| SHA-256 | `AC98526131EEEFEC72B510AC8FD7DF95B8818A9E8A38310C1CF475723242CF1D` |
| Magic number [observation] | `7f 45 4c 46` = **`\x7fELF`**; `EI_CLASS=01`(**ELF32**), `EI_DATA=01`(LE), `e_type=0x0003`(**ET_DYN/PIE**), `e_machine=0x0028`(**40 = EM_ARM, 32-bit ARM**) |
| Portal/Interpreter | `e_entry=0x1df0`; **`PT_INTERP` = `/system/bin/linker` (ARM32 linker)** |
| Festival | Section 23: `.interp .hash .dynsym .dynstr .rel.dyn .rel.plt .plt .text(0xe298) .rodata(0xce74) .ARM.extab .ARM.exidx .preinit_array .init_array .fini_array .ctors .dynamic .got .data .bss .comment .ARM.attributes .shstrtab` |
| Visible strings (first 15, all symbol names) [observation] | `/system/bin/linker`、`liblog.so`、`__android_log_print`、`libstdc++.so`、`libm.so`、`libc.so`、`__cxa_atexit`、`__errno`、`__gnu_Unwind_Find_exidx`、`__libc_init`、`__stack_chk_fail`、`calloc`、`connect`、`fclose`、`fprintf` |
| Decisive Characteristics [Observation] | Exported/internal symbols include **`pcap_in pcap_out pcap_lval pcap_text pcap_nerrs pcap_char pcap_leng pcap_setnonblock_fd pcap_getnonblock_fd`**, and `protocols` `eproto_db` `services` `ethers` `getprotobyname` `getservbyname` `getnetbyname` `if_indextoname` `pcap_parse` Related `no_optimize`, `vnode_base/next_vnode` |
| Structural judgment (observation + inference) | This is a **statically linked libpcap (including the filter expression parser generated by bison/yacc) ARM32 PIE independent executable**. `pcap_lval/pcap_text/pcap_char/pcap_nerrs/pcap_leng` is the yacc global variable, and `eproto_db/services/ethers` is the protocol/port/Ethernet table of libpcap → machine certificate. <br> does not appear in `libpcap.so` in `DT_NEEDED`, so libpcap is **statically linked into**. |
| **decisive string [observation, independent review in this round]** | `--------------- arp packet size should be 42 bytes or more!`(@0xfd77)、`gateway reply`、**`!!This Device is being Hijacked!!`**(@0xfdca)、`mac 1: %02X:%02X:%02X:%02X:%02X:%02X`、`mac 2: %02X:%02X:%02X:%02X`、`Target IP: %d.%d.%d.%d`、`[End]send msg to java: %s`(@0xfaf8)、`close socket`、`sleep %ds`、`%ds passed, exit`、`auto exit`、`Ethernet` `Unknown` `IPv4` `ARP Request` `ARP Reply`, `main-…` (thread name) |
| **Semantics [Observation + Inference]** | **This is a "ARP Gateway Hijacking (Man-in-the-Middle) Detection" native executable**: Catch ARP Request/Reply → Compare `gateway reply`'s MAC with the real gateway MAC → If inconsistent, print `!!This Device is being Hijacked!!`. <br>`[End]send msg to java: %s` directly proves that it has **the output protocol** passed back to Java (the child process stdout is read by the parent process), `sleep %ds` / `%ds passed, exit` / `auto exit` It is its**life cycle self-management**. <br>→ Combined with the call points of `popen`/`Runtime.exec` (see below), the project structure is: **Java uses `Runtime.exec` to start mdetector subprocess (32-bit) → subprocess uses `printf` and press the result `send msg to java:` Call stdout → Java side parses line by line**. |
| Why put **in `assets/` instead of `lib/`** | [Inference] ① `lib/*.so` will be registered by PackageManager and enumerated by third-party security software and `ApplicationInfo.nativeLibraryDir`, but `assets/` will not; ② It is **32-bit arm**, but if it is placed in `lib/arm64-v8a/`, it will cause "the same directory must be the same" ABI" and cannot be installed. |
| **32-bit ABI is inconsistent with "arm64-v8a only"** | [Observation] APK declares `arm64-v8a only`, this file is `EM_ARM` (32-bit). The **64-bit process cannot dlopen the 32-bit ELF** (`dlopen` will report `has unexpected e_machine`), and **can only `exec` a 32-bit child process**. <br> [Inference] Therefore, **must use** in the method of "release to disk + `Runtime.exec`" and cannot be used as a shared library. If the device does not have a 32-bit linker (some Android 13+ pure 64-bit models), this feature will be silently disabled. |
| **Loader Clues [Observation]** | In `_analysis\src\sources\` and `_analysis\apktool\smali\` grep `mdetector` **both hit 0** (file name runtime splicing). But three types of strongly related call points were found: <br>① `smali\com\avl\engine\b\b\d.smali:76-122` —— **ABI Directory name normalizer**: `mips/64`, `arm64*`→`"64"`, `armeabi-v7a*`→`"v7a"`, `arm*`→`""`, otherwise `"unknown"`→AVL press ABI Get **different** native payloads from assets (this is the mechanism that can put 32-bit binaries in assets) <br>② `smali\com\avl\engine\j\b\e.smali:217` and `:383` - **`Runtime.exec(String)` two** (AVL Side screwdriver process) <br> ③ `smali\tmsdkobf\t9.smali:563` —— **`Runtime.exec(String)`** (TMSDK side screwdriver process) <br> and `smali\xcrash\j.smali:857` and `smali\com\xiaomi\onetrack\util\b.smali:789`’s `ProcessBuilder` (xcrash / onetrack, not related to mdetector) |

#### 2.3.4 `assets/40706.dat` — TMSDK **Certificate MD5 Whitelist**

| item | value |
|---|---|
| size | 9,368 B |
| SHA-256 | `E746EE86B0E53718C87A4AB0DAB81BED4A1B84A060AEEB042D05D2AA4D93BA52` |
| Magic number [observation] | **no**; first 16 B `fe2acff7626ac55b2d313981a53c92b7`, last 16 B `3c81765cab70788ce3e177e92023bdea` |
| entropy | **7.983** (near pure random/ciphertext) |
| Alignment [observation] | 9,368 = **8 × 1171** (divisible by 8 bytes; not a multiple of 16) |
| visible string | None (10 hits are all random noise) |
| Semantics [observation + intersection] | `UpdateConfig.java:207-208`: `String str29 = intToString(40706) + ".dat"; CERTMD5_WHITELIST = str29;` → **`40706.dat` = `CERTMD5_WHITELIST` (certificate MD5 whitelist)**<br> consumption point `tmsdk/common/module/qscanner/impl/f.java:415`: `qa.a(ctx, str, UpdateConfig.intToString(40706), new b2(), "UTF-8")`; and `tmsdkobf/qa.java:48` The read path is exactly `TccCryptor.decrypt(bArr, null)` |
| Encryption [observation + inference] | **is encrypted and decrypted by `TccCryptor` (`decrypt([B[B)[B` of `libTmsdk`)**; the 8-byte alignment is consistent with the 8-byte block cipher/stream cipher |

#### 2.3.5 `assets/licence.conf` — TMSDK authorization (**ciphertext, structure has been solved**)

| item | value |
|---|---|
| size | 392 B |
| SHA-256 | `60FB5EB2C9EA51D55E487D4F434CAA12C37D53B4A7D2E01F920D5FF841DA658C` |
| Magic number [observation] | **None**; Entropy **7.525**; First 16 B `335e7397cbb7d899322e648f07b10f03` |
| visible string | None |
| Semantics (observation) | `tmsdkobf/f5.java:146`: `if (i6 == 0) str = "licence.conf"; else str = "licence" + i6 + ".conf";` → `assets.open(str)` read. **TMSDK authorization file** (not AVL; the authorization file for AVL is `av/avl/conf/liscense.conf` in `avlsdk` ZIP, the spelling is different) |
| structure (observation) | `392 = 128 + 264`; **`264 % 8 == 0`** (264/8 = 33 DES blocks, exactly divisible) |
| Decryption algorithm [observation, source code level] | `tmsdkobf/f5.java:53-64`：<br>`byte[128]` ← `bArr[0..128)`<br>`Cipher.getInstance("RSA/ECB/PKCS1Padding")` + `init(DECRYPT_MODE, rSAPublicKey)` + `doFinal(...)` → get DES Key <br>`SecretKeyFactory.getInstance("DES").generateSecret(new DESKeySpec(bArrDoFinal2))`<br>`Cipher.getInstance("DES/ECB/NoPadding")` Decrypt `bArr[128..]`<br>`Properties.load(...)` Parse plain text |
| Plain text format [observation] | Java `.properties` text, the key contains at least: `signature` (APK signature certificate abstract, capitalized HEX), `expiry.seconds` (long), `expiry.2019.enc_seconds` (HEX string), `lc_sdk_channel` (see `tmsdkobf/e5.java:56-73`, `f5.java:107`) |
| **Actual decryption result [observation]** | **failed**. After using the RSA-1024 public key built in `f5.java` (certificate `CN=obamazeng, O=tencent, C=CN`, valid period **2011-11-16 → 2012-02-24, expired**), the MSB of `m` is `0x08`, **is not the `00 01 FF…` or `00 02 …` of PKCS#1 v1.5. The defilling of** → PKCS#1 is not established. <br> also performs an 8-byte sliding window (121 offsets) on the RSA output. DES trial solution, the highest printable rate is only **0.447** (noise level), without any readable text. <br> → **Conclusion: These two license files are not** signed with the public key in `f5.java` (the certificate itself expired in 2012). |
| Structural judgment (reserved) | The container structure `[128B RSA block][8-byte aligned DES-ECB ciphertext]` is divisible by the **length + double confirmed by the source code algorithm**; only "which RSA public key corresponds" is unknown. See §7-U3 |

#### 2.3.6 `assets/licence1.conf` — TMSDK License (Part 2)

| item | value |
|---|---|
| size | 368 B |
| SHA-256 | `FC98C0DB13FFA6F36289123E1DEF3277D5661543D0BBA3A838D8B7DE4E58B817` |
| Magic number / entropy | None / **7.463**; first 16 B `4c60ede98853155e0ff97b00ef0194fe` |
| structure (observation) | `368 = 128 + 240`; **`240 % 8 == 0`** (30 DES blocks) |
| Semantics (observation) | `f5.java:148` of `"licence" + i6 + ".conf"`, `i6=1` → `licence1.conf`. `f5.f()` is `while(true)`. Loop: If `licence.conf` fails to parse, replace `i6++` with `licence1.conf`... Therefore, **is multiple candidate authorizations under the same public key. During the runtime, try** in sequence. |
| Decryption result | Same as §2.3.5: The structure is established, **cannot be decrypted with the built-in public key** |
| visible string | `l}~iFuQ`, `=dbs/iA` (random noise, not plain text) |

#### 2.3.7 `assets/dexopt/baseline.prof` — ART Baseline Profile

| item | value |
|---|---|
| Size / SHA-256 | 127 B / `099F760BFAC16C81F88C18216A96C4E63CB058144E2828FBDEBC6AC3707E1752` |
| Magic number [observation] | `70 72 6f 00` = **`"pro\0"`**, followed by `30 31 30 00` = **`"010\0"`** (ART Profile format version 010) |
| structure (observation) | `[magic "pro\0"][version "010\0"][u32 dex number=1][u32 dex checksum=0xdd22][u32 method number=0x6e]` followed by compressed dex position/method index bitmap (`78 01` = zlib header) |
| Qualitative | **Android standard baseline profile, non-loader**. Generated by R8/AGP when packaging, for ART to do AOT pre-compilation |
| visible string | None (binary bitmap) |

#### 2.3.8 `assets/dexopt/baseline.profm` — ART Profile metadata

| item | value |
|---|---|
| Size / SHA-256 | 55 B / `B8CFB8674ADB1A78DE5C47D3882BAF8751B56AF6E512ABEBF35A74849DDEF8A1` |
| Magic number [observation] | `70 72 6d 00` = **`"prm\0"`**, followed by `30 30 32 00` = **`"002\0"`** |
| structure (observation) | `[magic "prm\0"][version "002\0"][u32 flag=1][u32 number of items=0x1f][u32 length=0x25]` + zlib compressed body |
| Qualitative | Companion metadata (dex checksum/signature) of ART baseline profile, not payload |
| visible string | `d``ePc`` (zlib compressed volume noise) |

---

## 3. Data flow/timing

### 3.1 AVL stack initialization (`libavlasys` → `libavlm`)

```
[assets/avlsdk] --PK\x03\x04 ZIP-->
   com.avl.engine.a.c.d()  (c.java:110)
     ├─ i(eVar): assets.open("avlsdk") → com.avl.engine.k.c.a.e.a(inputStream) ← Container integrity check
     ├─ com.avl.engine.c.b.a.a(...)  unzipAssetsFileEfficient → context.getDir("avlsdk",0)
     │     (c.java:20 → AVLA.unzipAssetsFileEfficient → fell to libavlasys.so JNI)
     ├─ AVLA.a().pcit(<dir>/avl)                       ← c.java:130
     └─ e(eVar)  (c.java:143-163)
          path-N: com.avl.engine.k.f.a(ctx,"avlm") finds the absolute path of libavlm.so
                  → AVLA.setAVLSdkPath(appKey, <path>/libavlm.so, <dir>/avlsdk, <pkgCodePath>, sdkInt)
          path-S: AVLA.a(<appKey>, <dir>/avlsdk, <pkgCodePath>, sdkInt) ← Internal hardcoding "libavlm.so"
```
[Observation] `AVLA.java:42`: `return setAVLSdkPath(str, "libavlm.so", str2, str3, i6);` - **`"libavlm.so"` is the hard-coded string**.
[Observation] `JNI_OnLoad` of `libavlasys.so` only references the two class names `NativeUtils` + `AVLA` (§2.2.2 Machine Certificate).
[Observation] `libavlasys.so` `.rodata` includes `dlopen error: %s`, `so name is NULL`, `./conf/liscense.conf`, `./avl.avl`, `/avl/conf/avllib.conf`
→ During runtime, use `dlopen` to load the engine, and use `./avl.avl` / `./conf/liscense.conf` for authorization verification.

### 3.2 URL engine link

```
com.avl.engine.urldetector.UrlDetector  (UrlDetector.java:80 System.loadLibrary("urldetectorsys"))
   └─ liburldetectorsys.so  JNI_OnLoad@0x1db4  RegisterNatives:
        setLibPath(String)I / loadURL / unLoadURL()V / matchURL(String,Z)I / matchURLEx(String,Z)[String]
          └─ dlopen("liburl.so") ← The actual file is libavlurl.so (SONAME=liburl.so)
               └─ dlsym: AVLM_Load_URL / AVLM_Match_URL / AVLM_Match_URLEx / AVLM_Match_SPURLEx
                          AVLM_GetSIDInfo / AVLM_FreeSIDInfo / AVLM_MatchWithType
```
[Observation] `liburldetectorsys.so` full library 14,408 B, `.text` 4.2 KB, import only `dl*` + string function → pure forwarding.
[Unknown] `dlopen("liburl.so")` and `libavlurl.so` on the disk: either `setLibPath` is called first and changed to the real path, or the Android linker resolves according to the SONAME alias. See §7-U4.

### 3.3 TMSDK feature library loading

```
assets/70003.amf  --(VIRUS_BASE_NAME)-->  AmScannerV2.nativeLoadAmfHeaderBytes(byte[])  → libams
                                          AmScannerV2.nativeUpdateMalwareInfoBytes(byte[])
                                          AmScannerV2.scanApkBytes(byte[],...) → getOpcode(...) → extractApkInfo

assets/40706.dat  --(CERTMD5_WHITELIST)-->  tmsdkobf.qa.a(...)  → TccCryptor.decrypt(bytes,null) → libTmsdk
```

### 3.4 Xiaomi self-developed scanning (Rust)

```
p0/y.java:111 RustSupport.matches(byte[] file content, t0.j.a(stringItem)) → libflamingo(matches) single mode
p0/y.java:117 RustSupport.search (byte[] file content, stringItem.getValue()) → libflamingo(search) single mode
               (Overload search(byte[],String...) → aho-corasick multi-mode, return boolean[])
```

### 3.5 xcrash crash capture

```
xcrash.NativeHandler.d()  → System.loadLibrary("xcrash")  → libxcrash.so JNI_OnLoad@0x328c
   ├─ nativeInit(31 parameters: SDK_INT, RELEASE, ABI, MANUFACTURER, BRAND, MODEL, FINGERPRINT,
   │            logDir, ... , nativeLibraryDir, ...) ← Hand over the fingerprint of the whole machine to native
   ├─ prctl(PR_SET_DUMPABLE / PR_SET_PTRACER)  → "set dumpable failed" / "set traceable failed"
   └─ execl(<nativeLibraryDir>/libxcrash_dumper.so) → child process ptrace parent process, capture /proc/<pid>/maps
        Product: %s/tombstone_%020lu_%s__%s%s ("tombstone_" + 20-digit timestamp)
```

---

## 4. Analysis of key logic one by one

### 4.1 JNI boundary full table (Java declaration → actual symbol → semantics)

Total size of **: 7 Java-side native declaration groups, a total of 5 `.so` hosts, a total of 21 direct `Java_*` exports + 6 RegisterNatives registration tables (about 60 methods). **

#### 4.1.1 `com.miui.guardprovider.natives.RustSupport` → `libflamingo.so` (directly export `Java_*`)

Java side `System.loadLibrary("flamingo")` (`RustSupport.java:6`).

| Java Statement (`RustSupport.java`) | actual exported symbols | Semantics |
|---|---|---|
| `static native int matches(byte[], String)` (:12) | `Java_com_miui_guardprovider_natives_RustSupport_matches`@0x9d760 (480B) | Single regular/single pattern matching, returns hit or not (int) |
| `static native int search(byte[], String)` (:14) | `Java_com_miui_guardprovider_natives_RustSupport_search`@0x9d940 (664B) | Single pattern search, returns position/count (int) |
| `static native boolean[] search(byte[], String...)` (:16) | `..._RustSupport_search___3B_3Ljava_lang_String_2`@0x9dbd8 (1832B) | Multi-pattern batch search (Aho-Corasick), returns Boolean results for each pattern |

[Observation] `RustException.java` is in the same package and is the exception carrier thrown back by native to Kotlin (JNI has no semantic effect).
[Observation] REA `inspect_native_exports` Review `libflamingo.so` **There are exactly 3 derived**, which correspond to the above table one by one, and there are no redundant JNI surfaces.

#### 4.1.2 `com.avl.engine.security.NativeUtils` + `AVLA` → `libavlasys.so`（RegisterNatives）

[Observation] The string reference **of `libavlasys.so` `JNI_OnLoad`@0x105fc has only two string references:** `com/avl/engine/security/NativeUtils` and `com/avl/engine/security/AVLA`.
`AVLA extends NativeUtils` (`AVLA.java:4`), so the natives of the two tables fall in this library.

**`NativeUtils` table [observation, `.rodata`@0x115c6 plain text name + signature]**

| Java declaration | Register signature | Semantics |
|---|---|---|
| `static native byte[][] getMetaInfEntryNames(String)` | `(Ljava/lang/String;)[[B` | Read the `META-INF` entry name of APK (**signature verification related**; call point `com/avl/engine/security/h.java:59`) |
| `static native int ine(String)` / `ine(String, byte[])` | does not appear in the string list | [Unknown] Name is obfuscated, possibly registered as a short name |
| `static native int searchFile(String, FileSearcher)` | `(Ljava/lang/String;Lcom/avl/engine/security/FileSearcher;)I` | Single file scan callback |
| `static native int searchFile(String[], FileSearcher)` | `([Ljava/lang/String;Lcom/avl/engine/security/FileSearcher;)I` | Multiple file scan callback |
| `native String getCpuPlatform()` | `getCpuPlatform` | CPU platform string (used by `UrlDetector.b.a.java:47`) |
| `native String getLocalUID()` | `getLocalUID` | Local UID |
| `native String getFileMD5(String,int)` | `(Ljava/lang/String;I)Ljava/lang/String;` | File MD5 (with offset/length parameters) |
| `native String getStringMD5(String,int)` | `getStringMD5` | String MD5 |
| `native String readAssetsFile(String,String,String)` | `(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;` | Read assets |
| `native String readAssetsFileEfficient(AssetManager,String,String)` | `(Landroid/content/res/AssetManager;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;` | AssetManager reads assets |
| `native int unzipAssetsFile(String,String,String,List)` | `(...Ljava/util/List;)I` | Unzip assets |
| `native int unzipAssetsFileEfficient(AssetManager,String,String,List)` | `(Landroid/content/res/AssetManager;...Ljava/util/List;)I` | AssetManager decompression (`avlsdk` go this way) |
| `native int unzipAvllib(String,String)` | `(Ljava/lang/String;Ljava/lang/String;)I` | Unzip avllib |
| `native int unzipFile(String,String,List)` | `(...Ljava/util/List;)I` | Universal decompression |

**`AVLA` table [observation, `.rodata`@0x10ef0 clear text]**

| Java Statement (`AVLA.java`) | Registered name/signature | Semantics |
|---|---|---|
| `static native int checkKey()` (:12) | `checkKey` | Authorization verification (failure string `AVL SDK licence is overdue!`) |
| `private static native String gci(byte[],int)` (:18) | `gci ([BI)Ljava/lang/String;` | Certificate analysis (`gci` is getCertInfo) |
| `private static native … fbcb(int,int,String)` (:14) | `fbcb (IILjava/lang/String;)V` | native→Java log callback |
| `native int setAVLSdkPath(String,String,String,String,int)` (:103) | `setAVLSdkPath (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)I` | **loads `libavlm.so` + specified assets directory + appkey + SDK_INT** |
| `native int computeToken(String,String,String)` (:51) | `computeToken` | Calculate app token (with `AVLM_ComputeAPPToken`) |
| `native int initEngine()` (:81) | `initEngine` | → `AVLM_Init` / `AVLM_InitEx` |
| `native int releaseEngine()` (:95) | `releaseEngine` | → `AVLM_Release` |
| `native String scan(String,int[])` (:97) | `scan` | → `AVLM_Scan` |
| `native String[] scanEx(String,int[],long)` (:99) | `scanEx` | → `AVLM_ScanExWith*` |
| `native String sceneScan(String,int[],int)` (:101) | `sceneScan` | → `AVLM_ScanWithScanOpt` |
| `native int officialScan(String)` (:89) | `officialScan` | → `AVLM_CheckOfficalAPP` |
| `native String getEngineVersion()` (:67) | `getEngineVersion` | → `AVLM_GetEngineVersion` |
| `native String getSigLibVersion()` (:69) | `getSigLibVersion` | → `AVLM_GetSigLibVersion` |
| `native String getEngConfVersion(String)` (:65) | `getEngConfVersion` | Read `<dir>/conf/avllib.conf` |
| `native String getAPKInfo(String)` (:59) | `getAPKInfo` | → `AVLM_GetAPKInfo` |
| `native String getCertHash/getCertFullHash(String)` (:61,:63) | — | → `AVLM_GetCertHash` / `AVLM_GetCertFullHash` |
| `native String getVirusDescription(String,int)` (:71) | — | → `AVLM_GetVirusDesc` |
| `native String arbitrateVirusName(String)` (:47) | `arbitrateVirusName` | → `AVLM_VirNamesChoice` |
| `native String analyseAppByEngine(String)` (:45) | — | → `AVLM_GetAPKInfo` combination |
| `native int installPackage(String,int,String,String)` (:83) | — | → `AVLM_InstallPackage` |
| `native byte[] gpubkc(String)` (:73) | `gpubkc` | → `AVLM_GetPubkeyContent` |
| `native int nec(byte[],String,int)` (:87) | `nec` | [Unknown] Byte conversion, suspected name confusion |
| `native int keySetHash/signBlockHash/signBlockAndKeySetHash(String)` (:85,:113,:111) | — | Signed block hash |
| `native int pcit(String)` (:91) | `pcit` | Set SDK data directory (`c.java:130` passes `<dir>/avl`) |
| `native void pcr()` (:93) / `ckti()` (:49) / `fcf(int)` / `sjcf(int,int)` / `sjsp(JniFuncProcess)` | — | Internal clear cache/callback registration (`sjsp` registration `JniFuncProcess.checkFuncEnable`) |
| `setAIES/setAIER/setScanLimit/setScanLimitEx/setDEXScanLimit/setOpcDEXScanLimit/setMapleScanLimit` (private) | Partially in table `.rodata` | → Same name `AVLM_Set*` |
| `native int gufbyud(String,int,int,int)` (:77) / `icf(String,int)` / `gcv(String,int)` / `geci(String)` / `gsi(String)` / `sfcs(String)` / `sfcp(String,int,int)` / `suf(int)` / `suhms(int)` | — | [Inference] They are all confusing short names, and the semantics need to be confirmed dynamically; `gsi`≈getSidInfo, `geci`≈getEngineConfInfo |

#### 4.1.3 `com.tencent.turingfd.sdk.vd.TNative$aa` → `libturingvd.so`（RegisterNatives）

[Observation] The class descriptor of DEX actually contains `$`: `com/tencent/turingfd/sdk/vd/TNative$aa`, `.../TNative$aa$bb`; **none `Java_` exports**, so it must be RegisterNatives.

| Java Statement (`TNative$aa.java`) | Register signature | Semantics |
|---|---|---|
| `a(SparseArray, Context, Map, int)` (:22) | `(Landroid/util/SparseArray;Landroid/content/Context;Ljava/util/Map;I)Landroid/util/SparseArray;` | Risk control main entrance (with Context) |
| `b(SparseArray, byte[], Map, int)` (:24) | `(Landroid/util/SparseArray;[BLjava/util/Map;I)...` | Byte input risk control |
| `c(SparseArray, Context)` (:26) | `(Landroid/util/SparseArray;Landroid/content/Context;)...` | Simplified version |
| `d(SparseArray, Context, int)` (:28) | `(Landroid/util/SparseArray;Landroid/content/Context;I)...` | Band type |
| `e(SparseArray, Context, Map, int)` (:30) | `(...Context;Map;I)` | Same as `a` variant |
| `f(SparseArray, byte[], int)` (:32) | `(Landroid/util/SparseArray;[BI)...` | byte + type |
| `g/h(SparseArray, Context, Map, int)` (:34,:36) | `(...Context;Map;I)` | Two other copies of the same signature are implemented |
| `i(SparseArray, Context, Map)` (:38) | `(...Context;Map)` | No type parameters |
| `TNative$aa$bb.onServiceConnected(ComponentName, IBinder)` (:15) | `(Landroid/content/ComponentName;Landroid/os/IBinder;)V` | native implements `ServiceConnection` callback (**completes bindService callback processing** in native) |

#### 4.1.4 `xcrash.NativeHandler` → `libxcrash.so`（RegisterNatives）

| Java declaration | Register signature | Semantics |
|---|---|---|
| `nativeInit(...)` 31 parameters (:92) | `(ILjava/lang/String;×10;ZZIIIZZZZZI[Ljava/lang/String;ZZIIIZZ)I` | Initialization; pass in the fingerprint of the whole machine such as SDK_INT/RELEASE/ABI/MANUFACTURER/BRAND/MODEL/FINGERPRINT/nativeLibraryDir |
| `nativeNotifyJavaCrashed()` (:94) | `()V` | Inform that the native Java side has crashed |
| `nativeTestCrash(int)` (:96) | `(I)V` | Self-test crash |
| `nativeTraceSignalInit()` (:99) | No signature (unique) | Install the signal stack in the main thread |
| `crashCallback(String,String,boolean,boolean,String)` (:74) | `(Ljava/lang/String;Ljava/lang/String;ZZLjava/lang/String;)V` | **native callback Java** (no such statement on Java side, pure native side registration) |
| `traceCallback(String,String)` (:101) | — | Same as above (ANR trace callback) |

#### 4.1.5 `tmsdk.common.tcc.TccCryptor` → `libTmsdk-2.0.13-mfr.so`（RegisterNatives）

| Java Statement (`TccCryptor.java`) | Register signature | Semantics |
|---|---|---|
| `static native byte[] decrypt(byte[], byte[])` (:21) | `decrypt ([B[B)[B` | Decrypt (the second parameter is key, you can use the built-in key for `null`) |
| `static native byte[] encrypt(byte[], byte[])` (:41) | `encrypt ([B[B)[B` | Encryption |
| `static native byte[] makePassword(byte[])` (:61) | `makePassword ([B)[B` | Password derivation |
| `add(Object)Z` / `length()I` / `replace(II,String)StringBuffer` | Same table | [Inference] Auxiliary method, possibly an inner class |

[Observation] The "fake encryption" of pure Java on the Java side is also there: `encryptInt(i)=~i`, `encryptBoolean(b)=!b` (`TccCryptor.java:28,44,63`) - these are **obfuscation, not encryption**.
[Observation] `tmsdkobf/gb.java:490,527,536` Use `TccCryptor.decrypt(new byte[]{8~16 bytes})` to decode the **class name and the string**:
`new String(TccCryptor.decrypt(new byte[]{-82,6,122,-22,71,-51,117,-65}, null))`、`Class.forName(new String(TccCryptor.decrypt(bArr,null)))`
→ The **string/class name encryption table** of TMSDK is in `libTmsdk`, and `decrypt` is its decryption primitive.
[Observation] `tmsdkobf/b5.java:125`: `TMSDKContext.registerNatives(0, TccCryptor.class)` - TMSDK has its own explicit registration entry, which is further confirmed to be RegisterNatives.

#### 4.1.6 `com.tencent.tmf.gm.{Sm2,Sm4}` → `libtmf-gm.so` (directly export `Java_*`)

| Java declaration | Export symbols | Semantics |
|---|---|---|
| `Sm2.doSm2Encrypt(...)` | `Java_com_tencent_tmf_gm_Sm2_doSm2Encrypt`@0x18fc | SM2 encryption |
| `Sm2.doSm2Encrypt2(...)` | `..._doSm2Encrypt2`@0x1dd4 | SM2 encryption (variant, C1C3C2 sequence difference) |
| `Sm2.doSm2Decrypt(...)` | `..._doSm2Decrypt`@0x1bd4 | SM2 decryption |
| `Sm2.doSm2Decrypt2(...)` | `..._doSm2Decrypt2`@0x1f54 | SM2 decryption (variant) |
| `Sm4.doSm4EncryptCBC(...)` | `Java_com_tencent_tmf_gm_Sm4_doSm4EncryptCBC`@0x156c | SM4-CBC encryption |
| `Sm4.doSm4DecryptCBC(...)` | `..._doSm4DecryptCBC`@0x1734 | SM4-CBC decryption |
| `Sm4.doSm4EncryptECB(...)` | `..._doSm4EncryptECB`@0x100c | SM4-ECB encryption |
| `Sm4.doSm4DecryptECB(...)` | `..._doSm4DecryptECB`@0x13dc | SM4-ECB decryption |

#### 4.1.7 `com.avl.engine.urldetector.UrlDetector` → `liburldetectorsys.so`（RegisterNatives）

| Java declaration | Register signature | Semantics |
|---|---|---|
| `setLibPath(String)` | `(Ljava/lang/String;)I` | Specify `liburl.so` path |
| `loadURL(...)` | `loadURL` | Load URL library/feature |
| `unLoadURL()` | `()V` | Uninstall |
| `matchURL(String, boolean)` | `(Ljava/lang/String;Z)I` | Single URL judgment |
| `matchURLEx(String, boolean)` | `(Ljava/lang/String;Z)[Ljava/lang/String;` | Batch URL judgment |
| `unzipAssetsFileEfficient(...)` / `readAssetsFileEfficient(...)` (inherited from `UrlDetector`, called by `a/d.java:17,22`) | See NativeUtils table | Read/unzip assets |

#### 4.1.8 Rest of Java native declaration → Attribution conclusion

| Java class | Statement | Carrying `.so` | Conclusion |
|---|---|---|---|
| `tmsdk.common.tcc.QFile` | natives | **`libdce-1.1.21-mfr.so`**（`QFile.java:28` `loadLibrary("dce-1.1.21-mfr")`） | ✓ Corresponding (registration table contains `tmsdk/common/tcc/QFile`) |
| `tmsdk.common.module.qscanner.impl.AmScannerV2` | 8 | **`libams-1.3.0-mfr.so`** | ✓ Direct `Java_*` export |
| `com.tencent.tcuser.util.Encode` | natives | **has no corresponding library** | ✗ `Encode.java:43,48` `System.loadLibrary("xy")`, but `lib/arm64-v8a/` **no `libxy.so`**. `loadLibrary` is wrapped by `try/catch` and `pa.a(new Thread(), th2, "System.loadLibrary error", null)` only reports → **dead code/downgrade path** [inference]. See §7-U5 |
| `tmsdkobf.ka` | No statement, only `System.load()` | — | [Observation] `ka.java:32,46,56` `System.load(env+path)`, `:64,72` `loadLibrary(name)`: **TMSDK’s private library loader** is explicitly loaded from `/data/data/<pkg>/lib/` (`System.load` requires an absolute path, explain these `.so` may be **released during the running period of**, not in APK `lib/`) |

---

### 4.2 Key function analysis (`analyze_function` alternative: symbol + AArch64 string reference)

> Ghidra is not natively available for ELF (§7-U0). The following table uses ① dynsym’s `addr/size` [observation] ② self-built `ADRP+ADD` decoded “function → reference string” [observation].

| # | function | address/size | mechanical evidence | Judgment |
|---|---|---|---|---|
| **F1** | `libavlasys.so: JNI_OnLoad` | `0x105fc` / 204 B | There are exactly 2 **strings** belonging to this function in the ADRP+ADD scan (`.text` 0x2c00..0x10910, referenced at 316): `com/avl/engine/security/NativeUtils`, `com/avl/engine/security/AVLA` | [Observation] **This function only does two things**: `FindClass` these two classes + `RegisterNatives`. This is the **machine certificate** of "libavlasys is AVL JNI shell", it is not an inference. |
| **F2** | `libavlasys.so: AVL_SetScanLimitEx` / `AVL_SetMapleScanLimit` | `0xe004` (24 B) / `0xdfec` (24 B) | Two 24-byte **extremely short exports**, immediately before `JNI_OnLoad`; `libavlm.so` also exports the same name `AVLM_SetScanLimitEx`@0xaea4c(44B) / `AVLM_SetMapleScanLimit`@0xae9c0(44B) | [Inference] On the `libavlasys` side, **dlsym forwards trampoline** (24 B ≈ `adrp/ldr/br` three instructions), transferring the call to `libavlm`. This is why `AVLA.setScanLimit*` does not appear directly in the JNI table but is assumed by `AVL_*`. |
| **F3** | `libavlm.so: AVLM_Init` | `0xae764` / **8 B** | 8 bytes = one `b` jump | [Observation] The true form of **is not on this symbol**: the `b` springboard of `size=8`. The real implementation is in `AVLM_InitEx`@0xae76c(540 B). `AVLM_Init` is just an alias springboard → confirms the "formal name + alias" anti-hook structure of §2.2.3. |
| **F4** | `libavlm.so: AVLM_ScanWithScanOptEx` | `0xaf318` / 780 B | `libavlasys` `.rodata`'s dlsym name list appears simultaneously with `AVLM_ScanWithScanOpt`(36B) and `AVLM_ScanWithScanOptEx`(780B) | [Inference] `...Opt` is the springboard, and `...OptEx` is the real scanning main loop (780 B is one of the largest real functions in `AVLM_*`). Scanning entrance priority: `Scan`(12B)→`ScanWithArgs`(812B)→`ScanWithScanOptEx`(780B). |
| **F5** | `libavlm.so: AVLM_ComputeAPPToken` / `AVLM_CheckAPPToken` / `AVLM_CheckAPPKey` | `0xb1b00`(220B) / `0xb1bdc`(224B) / `0xb1cbc`(116B) | `libavlasys` `.rodata` Also contains `check key error: token is empty.` / `check key error: appkey is empty.` / `check key error: %s is not found.` / `check key error: sopath is empty.` | [Observation] Authorization chain: Java `AVLA.computeToken(pkgCodePath, pkgName, pkgSig)` → `AVLM_ComputeAPPToken`; `AVLM_CheckAPPToken`/`CheckAPPKey` is stored in the same area as `62BAFD662A63A99A7C96DA027AFDF739` in `libavlasys` → [Inference] The 32-bit HEX string is **AVL is one of the authorization keys**. |
| **F6** | `libavlurl.so: AVLM_Match_URLEx` / `AVLM_Match_SPURLEx` / `AVLM_Load_URL` | `0x32918`(616B) / `0x32d98`(544B) / `0x32774`(172B) | The dlsym name list of `liburldetectorsys` contains `AVLM_MatchWithType` / `AVLM_GetSIDInfo` / `AVLM_FreeSIDInfo`; `libavlurl` `.rodata` contains `%s.new` `%stmp/` `%s.tmp/` `%s/conf/avllib.conf` | [Inference] `AVLM_Load_URL` loads from `<dir>/conf/avllib.conf` + `*.avl` and supports `.new`→`.tmp` atomic hot update; `Match_URLEx` is the main decision loop. |
| **F7** | `libTmsdk: TccCryptor.decrypt` / `encrypt` / `makePassword` | Registered at `JNI_OnLoad`@0x1f480 (156 B) | `.rodata`@0x484e0 plain text `makePassword ([B)[B` `encrypt ([B[B)[B` `decrypt` + error string `Bad encrypted data` `data too large` | [Observation] All three primitives of TccCryptor are in `libTmsdk`. `Bad encrypted data` is the decryption failure string → with **integrity/format check**, the pure XOR trial decryption will fail. |
| **F8** | `libams: AmScannerV2_nativeLoadAmfHeaderBytes` | `0x1c514` / 1308 B | AMF entry name of `.rodata`@0x8db48 + two suspicious constants | [Inference] The function of 1308 B is responsible for parsing the `70003.amf` header and building the table entry name index; `getOpcode`@0x1dbd4 (3488 B) is the largest function **in the** library, corresponding to the `getOpcode` semantics of §4.1 (replace AMF rules compiled into matching bytecode). |

### 4.3 Encryption and state secrets

#### 4.3.1 `libtmf-gm.so` ↔ `com.tencent.tmf.gm.{Sm2,Sm4}`

[Observation] The relationship is **directly JNI bound to**: 8 `Java_com_tencent_tmf_gm_{Sm2,Sm4}_*` symbols + `libtmf-gm.so` explicit `.dynsym` export + `com/tencent/tmf/gm/a.java:26` `System.loadLibrary("tmf-gm")`. Three independent pieces of evidence are consistent.

[Observation] `com/tencent/tmf/gm/b.java` is a Java side call package:
- `b.java:20` `a.C0038a c = Sm4.a(bArr2, bArr)` → SM4 Encryption
- `b.java:34` `Sm4.b(bArr2, bArr)` → SM4 Decryption
- `b.java:52` `Sm2.a(bArr3, bArr4, bArr)` → SM2

[Observation] `libtmf-gm.so` **does not contain any key material**: `.rodata` has only 8 JNI names + 9 `_ZN7_JNIEnv*` method names + dependent library names, **does not have PEM / long HEX / Hardcoded IV**. The keys are all passed in as `byte[]` from the Java side.
[Observation] `.dynstr` does not have the words `SSL`/`TLS`/`X509` → **SM2/SM4 is not used for the transport layer** and is the application layer encryption of the TMF configuration/report message.

[Unknown] The position of SM2/SM4 in the specific protocol (which field is encrypted and where the IV comes from) depends on the caller of `com.tencent.tmf.*`; this round only confirms the algorithm binding and "key external". See §7-U6.

#### 4.3.2 Three coexisting symmetric encryption systems

| System | realize | Key source | Evidence |
|---|---|---|---|
| **TccCryptor** (TMSDK private) | `libTmsdk` of `encrypt/decrypt/makePassword` | **is built into native** (`decrypt(b, null)` passes `null` to the built-in key) | [Observation] `TccCryptor.java:21` + `tmsdkobf/qa.java:48` `TccCryptor.decrypt(bArr, null)`; error string `Bad encrypted data` |
| **RSA-1024 + DES-ECB**（licence） | Java layer `javax.crypto` (no native required) | 128 B RSA block → DES key; RSA certificate hardcoded in `f5.java:52` | [Observation]§2.3.5 |
| **National Secret SM2/SM4** | `libtmf-gm.so` | All external incoming | [Observation]§4.3.1 |

[Observation] The three **do not reuse**: `libtmf-gm` does not import `libTmsdk`, `libTmsdk` does not import `libtmf-gm`; `licence.conf` uses Java standard JCE.

#### 4.3.3 `liscense.conf` within `assets/avlsdk` (AVL’s own authorization)

[Observation] `av/avl/conf/liscense.conf` and `url/url/conf/liscense.conf` are both **404 B**, and the last 4 bytes of **are both `01 00 00 00`**.
[Observation] `404 = 4 + 400`, and **`400 % 16 == 0`** (25 16-byte blocks) → consistent with SM4/AES grouping.
[Observation] The first 16 bytes of the two copies are completely different (`5da3caa9df459f73…` vs needs to be taken separately), indicating that **encrypts** on an instance-by-instance basis, not a fixed template.
[Inference] Structure = `[400 B block ciphertext][u32 LE = 1 (version/number of entries/check)]`. `checkKey`/`key is overdue!` of AVL is in `libavlasys`, and decryption and verification should be completed by `libavlasys` (this library imports `libz`, but **does not depend on cryptographic libraries other than** `__android_log_print` → The algorithm is self-implementing).
[Unknown] The specific algorithm and key cannot be restored. See §7-U3.

#### 4.3.4 Hardcoded Key/IV Summary

| location | value | Type | Status |
|---|---|---|---|
| `libavlasys.so` `.rodata` (`0x10ef0` area, immediately after `AVL SDK licence is overdue!`) | **`62BAFD662A63A99A7C96DA027AFDF739`** | 32 HEX = 16 B | [Observation] Plaintext is hardcoded. Inferred to be AVL licence/appkey calibration material |
| `AndroidManifest` meta-data (Presentation § Confirmed) | `D5DFB878335A3EF4E76F60ED470B8EC8` | 32 HEX | [Observation] `AVL_OPENSDK_APPKEY`; via `AVLA.setAVLSdkPath(appKey,…)` / `computeToken` into native |
| `libams-1.3.0-mfr.so` `.rodata`@0x8db48 | **`!@#$%^&*()_JHGFD`** | 17 B ASCII | [Observation] Plain text; [Inference] AMF table entry name confusion key |
| Same as above | **`SfJ(*W#&Ddweir309*&@#!_)($39`** | 28 B ASCII | [Observation] Plain text; [Inference] AMF secondary obfuscation key |
| `tmsdkobf/f5.java:52` (Java constant, non-native) | RSA-1024 X.509, `CN=obamazeng, OU=3G, O=tencent, L=GZ, S=GD, C=CN`; `NotBefore 2011-11-16 / NotAfter 2012-02-24` (**has expired**) | Certificate | [Observation] Base64 PEM hard-coded |
| `META-INF/flamingo.dat` (t-cloud has been verified) | AES-128 key `wcw1xbplx7aijwqr` | 16 B ASCII | [Observation-Citation] Not related to `libflamingo.so` ** **, see §4.3.5 |

#### 4.3.5 The relationship between `flamingo.dat` and `libflamingo.so` (answer to lead’s question)

[Observation] **is a different entity with the same name and has no cryptographic connection to**:
1. `libflamingo.so`’s **complete import table** only has 40 items of `libc` + `libdl`, **does not have any JNI side crypto, none `__android_log_print`, no file reading**; its three native method signatures are `(byte[],String)` / `(byte[],String[])`, **cannot receive the key** (`RustSupport.java:12-16`) at all.
2. `libflamingo.so`'s `.rodata` are all Rust's panic strings of `regex`/`aho-corasick`/`rustc-demangle` (§2.2.6), **'s cryptographic zero string**.
3. `META-INF/flamingo.dat` is read from the **Java** side: `v0/j.java:38` `classLoader.getResourceAsStream("META-INF/flamingo.dat")`; the related `p0/n.java:749` `map.put("flamingo_dispatcher", …)`, `p0/g.java:712` `aVarA.b("flamingo_dispatcher")` are all **Java/Kotlin The dispatcher of the layer is**.
4. `libflamingo.so` is only used by `System.loadLibrary` in `RustSupport`, and the **feature string whose call point is `p0/y.java:111,117` matches**.

→ **Conclusion: `flamingo` is the codename of Xiaomi’s scanning framework (`flamingo-android_release`, `flamingo.db`, `flamingo_dispatcher` are everywhere); `flamingo.dat` is the configuration key carrier of this framework, and `libflamingo.so` is its Rust matching engine. The two share codenames, not key paths.**

### 4.4 Evidence of questionable/high-risk abilities (string + import double evidence)

> Method: Perform Latin1 decoding on each of 12 `.so` and run 17 sets of regular expressions (`ANTIDBG/ROOT/EMU/PROC/SHELL/MEMSCAN`, etc.), and then perform **±60~200 B context slicing on each hit to manually determine** (this is to exclude false positives such as `Friday`).

| Ability | Conclusion | Evidence | nature |
|---|---|---|---|
| **ptrace anti-debugging** | **only xcrash series** | `libxcrash_dumper.so` import **`ptrace`**, and `/proc/%d/maps`; `libxcrash.so` import `prctl clone execl waitpid` + string `set dumpable failed` / `set traceable failed` | **is not anti-debugging, but debugger** - it ptrace attaches its own parent process to capture tombstones (legitimate crash capture design). **The remaining 10 libraries do not import `ptrace`** [Observation] |
| `TracerPid` / `/proc/self/status` | **No** | Full 12 library regular `TracerPid\|/proc/self/status` **0 hits** | — |
| **Frida / Xposed detection** | **None (Both are false positives)** | `libams` hits `Frida` → the context is `…Thu…Fri…Sat…Sunday…Monday…Tuesday…Wednesday…Thursday…Friday…Saturday…`, which is actually `Fri`+`day`; `libavlurl` hits `frida` → the context `…blackfriday……`; hits `xposed` → `…expert..exposed.extraspace…` | **must be clarified: no library actually does Frida/Xposed detect** |
| **Root detects** | **`libturingvd.so` is real** | String `/system/bin/su`, `/system/xbin/su`, `daemonsu` + process name regular `^/system/xbin/ku[.]sud$\|^daemonsu:\|^kworker/[1-9][[:digit:]]{2}$\|^permmgrd$\|^360sguard$`; import `getuid readlink statfs popen` | [Observation] The real root/daemon process of **scans**, and uses `popen` to run the shell |
| Same as above (false positive) | `libxcrash(.dumper)` also hit `/system/xbin/su` `/system/bin/su` `/su/bin` | [Inference] xcrash collects "device root information" and writes it into the path table of tombstone, not actively detected (xcrash does have this list upstream) | low risk |
| **simulator/VM detects** | **`libturingvd.so` is real** | import `ASensorManager_getInstance` `ASensorManager_getSensorList` `ASensor_getVendor/Name/Type/Resolution` (sensor fingerprint) + `statfs` + `readlink` + `uname`? + string `brand` `model` `Hardware` `model name` `apiLevel` | [Observation] The sensor list is a strong feature to determine the simulator/cloud phone |
| Text emulator string (`goldfish/qemu/ranchu/genymotion`) | 12 library **0 hit** | — | [Inference] Turing uses **behavior** (sensor missing/abnormal) instead of string matching |
| **anti-virtual machine** | **not found** clear `VMware/VBox` check | 12 banks 0 hits | — |
| **Memory Scan/Process Injection** | **not found** `process_vm_readv`, `/proc/<pid>/mem` write, `PTRACE_POKETEXT` | 12 library pair `process_vm_readv\|/proc/%d/mem` **0 hits**; `libxcrash_dumper` reads only `/proc/%d/maps` | — |
| **DEX Decrypt / Dynamically load** | `libavlasys` has `dlopen/dlsym`; `liburldetectorsys` has pure `dlopen`; `libams/libdce/libavlm/libavlurl` also has `dlopen` | [Observation] All are used to load the brother engine library **(`libavlm.so` / `liburl.so`) in** and APK. There is no "decryption dex → `DexClassLoader`" path. | low risk |
| **string decryption table** | **`libavlm.so` clearly has** | Conjoined blobs appear in `.rodata`: `http://.https://.Content-Length:.www.baidu.com.GET` and `…SMS_Send.SMS_Block.System_DexLoad.System_ReflectInvoke.System_ReflectGet.Notification…`; and multiple `AVLM_*` export the alias springboard of `size=4` (`aesd/adce/effe/efff/fffe/exdz/wtsr/haoz/zavt/brmy`) | [Observation] **fixed-length string table + alias export**, which is the usual obfuscation style of AVL (non-packing) |
| Same as above | **string/class name encryption** of `libTmsdk` is widely used on the Java side | `tmsdkobf/gb.java:490,527,536` uses `TccCryptor.decrypt(byte[])` to restore the class name; `tmsdkobf/ib.java:68,409`, `tmsdkobf/m6.java:22`, `tmsdkobf/qa.java:48` are similar | [Inference] `libTmsdk` contains TccCryptor's **algorithm + built-in key**, which is the global string decryption table of TMSDK |
| **network request (hard-coded domain name in the library)** | See §5.1 / §5.2 | `libavlm`: `www.baidu.com`, NTP pool, `www.openssl.org`, `www.w3.org`, `www.wapforum.org`, `openssl.cn`; `libavlurl`: No C2 (see §5.2); `libturingvd`/`libflamingo`/`libtmf-gm`/`libTmsdk`/`libdce`/`libams`: **has no hard-coded domain name** (only hits `android.googlesource.com` Tool chain path, `bzip.org`, `www.memtest86.com`) | [Observation] |
| **network outgoing capability** | `libavlm`, `libavlurl`, and `libavlasys` all import `socket/connect/send/recv/gethostbyname/select`; `libturingvd` and **do not import socket** (via Java network) | [Observation] import table | See §6-R1 |
| **executes external program** | `libturingvd`: `popen/pclose`; `libxcrash`: `execl/popen`; `libxcrash_dumper`: `popen`; `libams`: `open/read/write` (without exec) | [Observation] | See §6-R4 |
| **assets contains the executable** | **`assets/mdetector` = ARM32 PIE + `/system/bin/linker`**（§2.3.3） | [Observation] ELF header + `PT_INTERP` + static libpcap symbols | **High risk**, see §6-R2 |

---

## 5. Configuration items/constants/protocol fields

### 5.1 `libavlm.so` plain text domain name / URL (exhaustive, deduplication)

Source: `DOMAINS`/`URLS` regular code other than `libavlurl` + `SVC` regular code, all hits are subject to context verification.

| value | context | Judgment |
|---|---|---|
| `https://` / `http://` (appears alone) | In the same area as `Content-Length:` `GET` `HEAD` `HTTP/1.1` | HTTP Literal value implemented by the client, non-specific endpoint |
| `www.baidu.com` | In series with `http://` `https://` `Content-Length:` `GET` | [Inference] Connectivity detection/whitelisting |
| `0.north-america.pool.ntp.org`<br>`1.…` `2.…` `3.…` | conjoined blob | [Inference] NTP Calibration time (used to determine authorization expiration/certificate validity period, echoing `key is overdue!`) |
| `www.w3.org`、`www.wapforum.org` | namespace | XML/WAP Resolved DTD namespace, non-network endpoint |
| `www.openssl.org`、`openssl.cn`、`http://www.openssl.org/support/faq.html` | OpenSSL built-in error/FAQ string | **OpenSSL statically linked intrinsic string**, non-communication endpoint |
| `liburl.so`、`%s/conf/avllib.conf`、`%s.new`、`%stmp/`、`%s.tmp/` | `libavlurl` | Local file path template |

[Observation] There is no hard-coded C2 for **`libavlm` / `libavlurl` / URL**. They are just "engines", and the network behavior is downloaded by the `https://api.sec.miui.com/...` and `https://virus-engine.sec.miui.com/...` (listed in the briefing) driver update package of the Java layer.

### 5.2 400 domain names of `libavlurl.so` (**is not C2**)

[Observation] For `libavlurl.so` domain name regularization, 400 entries were obtained (reaching the upper limit of 400 I set, but actually more), the content is:
`dyndns.org` whole family (`dyndns-at-home.com`, `from-ak.com`…`from-wy.com`, `is-a-chef.com`…, `webhop.net`…),
`eu.org` All Level 2 (`cn.eu.org`, `us.eu.org`…),
Cloud/SaaS:`s3.amazonaws.com` FamilyMart, `cloudfront.net`, `elasticbeanstalk.com`, `appspot.com`, `azurewebsites.net`, `herokuapp.com`, `cloudfunctions.net`, `fastly.net`, `cdn77.org`, `firebaseapp.com`, `blogspot.com`, `googleapis.com`, `rhcloud.com`, `sinaapp.com`, `1kapp.com`, `duckdns.org`, `dynv6.net`,
and `edu.cn gov.cn net.cn org.cn mil.cn`.

[Observation] These 400 entries are in the same database as `url/url/white.avl` (370,316 B) and `url/url/fish.avl` (150,768 B), and none of them **is the API domain name** in mainland China.
[Inference] This is a subset of **Public Suffix List + free second-level domain name hosting provider list**, used for URL During the reputation determination, **identified "attackers can register subdomains for free under public suffixes"** (a high-risk area for fraudulent sites), which belongs to **'s defensive white/grey list** and is not a backlink address.
[Observation] The hits of `frida`/`xposed` in `libavlurl` are verified by context as `blackfriday`/`exposed` (the same English vocabulary list), **is a false positive**.

### 5.3 Constant summary table

| constant | value | Source |
|---|---|---|
| AV engine version | `351` | `avlsdk:sdk_conf/version.conf` (plain text JSON) |
| AV signature library version | `20220928.ah` | Same as above + `avlsdk:av/avl/conf/avllib.conf` (clear text) |
| URL engine version | `124` | Same as above |
| URL feature library version | `20180917.zz` | Same as above + `avlsdk:url/url/conf/avllib.conf` (clear text) |
| `sdk.conf` version | `"1"` | Same as above |
| AMF container magic number | `"amf\0"` + `u32=4` | `assets/70003.amf` |
| AMF header extension | `0x18..0x1F` double u32 timestamp (`0x5FF6D2BD`/`0x622575E6`) | Same as above |
| AVL `.avl` magic number | `41 56 4C` = `"AVL"` | 5 `.avl` within `avlsdk` |
| ART profile magic number | `"pro\0"+"010\0"` / `"prm\0"+"002\0"` | `assets/dexopt/baseline.prof{,m}` |
| xcrash tombstone name | `%s/tombstone_%020lu_%s__%s%s` | `libxcrash.so` |
| xcrash dumper location | `%s/libxcrash_dumper.so` | Same as above |
| Turing version string | `TuringFD v%d (%s, %s, %s, compiled %s)` / `vd.5e7c85b` | `libturingvd.so` |
| TMSDK `.amf` No. | `70002`(EN library) `70003`(virus library) `70701`(payload) | `UpdateConfig.java:197-202` |
| TMSDK `.dat` No. | `40706` (certificate MD5 white list) `100003` (AMS) `40685/40822` (TRP) | `UpdateConfig.java:203-229` |
| NDK version | `libavlm`=clang 3.8.256229；`libtmf-gm`=clang 5.0.300080；`libflamingo`=r25b/8937393；`libturingvd`=r20/5594570；`libxcrash*`=r16b/4479499 | `.comment` / `.note.android.ident` |

---

## 6. Permissions and attack surface/risk points

| # | risk | location | Observation/Inference | influence |
|---|---|---|---|---|
| **R1** | The three native libraries **have complete network access capabilities** without going through the Java network stack. | Import of `libavlm.so`, `libavlurl.so`, `libavlasys.so`: `socket/connect/send/sendto/recv/recvfrom/gethostbyname/inet_addr/select` | [Observation] import table | The engine can bypass `NetworkSecurityPolicy`/`StrictMode`/proxy settings **to directly connect to any IP**; `inet_addr` instructions can directly fill in the IP constants. Because the Host is `com.miui.guardprovider` (`system\|signature` permission, system app), its process itself already holds `INTERNET`, so the actual availability depends on whether the **engine will use** (no hard-coded C2 is seen in this round, see §5.1). |
| **R2** | **`assets/mdetector` is a ARM32 executable ELF (ARP gateway hijack detector)**, not a data file | §2.3.3 | [Observation] `\x7fELF`+`ET_DYN`+`EM_ARM`+`PT_INTERP=/system/bin/linker`+static libpcap + `!!This Device is being Hijacked!!` + `send msg to java:` | ① **asset hides the executable**, bypassing the visibility of the `lib/` directory to PackageManager/security software; ② The function is **passive sniffing ARP determines that the middleman hijacked** (requires `CAP_NET_RAW` level capabilities, but because it runs on `com.miui.guardprovider`, the child process of the system permission process, inherits the SELinux domain of the process); ③ 32-bit conflict with `arm64-v8a only` → exec cannot be performed on pure 64-bit models, and the function fails silently. [Unknown] See §7-U2 for location of release/exec code. |
| **R3** | **SM2 random number may use `rand()`/`srand(time())`** | `libtmf-gm.so`'s import: `rand` `srand` `time` (without `getrandom`/`/dev/urandom`) | [Observation] import table | If SM2 encrypted `k` comes from `rand()`, then **can be predicted within the same second → the plaintext and even the private key** can be recovered. This is the most classic high-risk flaw in state secret implementation. Note: `libflamingo` correctly imports `getrandom`. [Unknown] Whether `rand` is really used in `k` of SM2 (requires decompilation and confirmation, not available on this machine). |
| **R4** | **`libturingvd` Use `popen` to execute the shell command** and read the sensor to make the device fingerprint | §2.2.9 | [Observation] import `popen/pclose` + `ASensor*` | The command string comes from within the native (no clear text command string is seen), but `popen` allows the native layer to have the **arbitrary shell execution primitive**; once it is hijacked (such as `PATH`/`LD_*` pollution), the privileges can be escalated to the shell. |
| **R5** | **URL feature library is 4 years behind** (`20180917.zz` vs AV library `20220928.ah`) | `avlsdk:url/url/conf/avllib.conf` | [Observation] Plain text | The 2018 library used for URL phishing/fraud determination has almost no coverage of new domain names after 2019; it is out of touch with the update rhythm of the AV engine. |
| **R6** | **`libavlurl.so` The actual file name is inconsistent with SONAME (`liburl.so`)**, and `liburldetectorsys` press `liburl.so` dlopen | §2.2.4 / §2.2.10 | [Observation] | If `setLibPath()` is not called, `dlopen("liburl.so")` on Android **will not** automatically match `libavlurl.so` (SONAME does not participate in file name search) → URL The engine as a whole fails and is silently degraded. It belongs to **functional defect** rather than a safety issue. |
| **R7** | **`libTmsdk` file name 2.0.13 / SONAME 2.0.12** | §2.2.8 | [Observation] | Version mismatch may cause callers that rely on 2.0.13 behavior to get the 2.0.12 implementation. |
| **R8** | **licence public key certificate expires in 2012** and does not match the license file included in the package | §2.3.5 | [Observation] RSA defill failed + certificate `NotAfter 2012-02-24` | The authorization verification chain **in this package cannot pass** (`f5.f()` will throw `RuntimeException("loadLicence Invaild LicenceFile!")`) → TMSDK. The authorization function is actually in the **unenabled/downgraded** state. |
| **R9** | **TMSDK string/class name encryption key built into native** | `libTmsdk` of `TccCryptor.decrypt(b,null)` | [Observation] Java uses this solution class name in large numbers (`gb.java:490,527,536`) | "Encryption" only increases the reverse cost. **is not the security boundary.**: Get `libTmsdk` to batch decrypt all obfuscated strings. |
| **R10** | **`libdce` has the primitive** to delete any path in batches | §2.2.5 of `nativeDeleteAllChildren`/`nativeDeleteAllChildrenByDay`/`nativeRemoveEmptyDir` | [Observation] Registry signature | If the path parameters are controllable (`selectionArgs=[path,…]` from IPC `guard/sync_scan`, see Briefing §IPC), there is a situation where **deletes files** without permission. Java side verification is required; Java verification logic has not been reviewed in this round. |
| **R11** | **`libavlasys` hard coded `62BAFD…739`** | §4.3.4 | [Observation] | If the string is an authorization key, anyone can use it to forge the license and call the AVL engine. |

---

## 7. Unresolved issues

| # | question | Current situation | Suggestions for next steps |
|---|---|---|---|
| **U0** | **REA cannot be decompiled on this machine ELF** | [Observation] `analyze_function` returns `provider_unavailable`: `ghidra → target_format_unsupported ("Windows Ghidra P0 accepts native PE and explicitly selected raw 6502 targets only")`; `hopper → unsupported_host (win32)`; `ida → not_configured`; `kuna → not_configured`. **This is a hard limit, not a timeout** | ① Run REA on Linux/macOS; or ② configure `REA_KUNA_COMMAND`/`REA_IDA_MCP_CONFIG`; or ③ install `llvm-objdump`/`rizin` for offline disassembly. This report has been partially replaced with "Self-built ADRP+ADD decoding". |
| **U1** | `libtmg-gm.so` does not exist | The task list gave two names `libtmg-gm.so`/`libtmf-gm.so`, but in fact there is only `libtmf-gm.so`; on the Java side `com/tencent/tmf/gm/a.java:26` is also only `loadLibrary("tmf-gm")` | It is determined to be a clerical error in the list and no further action is required. |
| **U2** | The specific code of "release + exec" of **`assets/mdetector` is not located in** (has been significantly narrowed) | Confirmed: It is a ARP gateway hijack detector, has `send msg to java:` backhaul protocol, 32-bit ARM. <br> has located 3 candidate exec points: `com/avl/engine/j/b/e.smali:217,383` (`Runtime.exec(String)`), `tmsdkobf/t9.smali:563`; ABI normalizer `com/avl/engine/b/b/d.smali` has been located. <br> but grep `mdetector` in `sources\` and `smali\` **still 0 hits** → filename is spelled at runtime | ① Decompile `tmsdkobf.t9` / `com.avl.engine.j.b.e` to see if the exec command string is spliced by the ID of `UpdateConfig`; ② Search for the `"mdet"` fragment and `"send msg to java"` in smali (if the Java side parses the prefix, a literal will appear); ③ Check `tmsdkobf.ka` Library release directory `/data/data/<pkg>/lib/` |
| **U3** | The RSA public key of **`licence.conf`/`licence1.conf` is unknown**; the algorithm/key of `liscense.conf` in `avlsdk` is unknown | `licence.conf`/`licence1.conf`: The structure `[128B RSA][8B aligned DES-ECB]` consists of source code + length divisible double certificate, but after using the built-in public key of `f5.java:52` for public key operation, PKCS#1 fails to fill (the first byte of `m` is `0x08` instead of `0x00 0x01`/`0x00 0x02`), the maximum printability rate of the 121 offset sliding window DES trial solution is only 0.447. <br>`avlsdk` `liscense.conf`(404B): Structure `[400B 16-byte block ciphertext][u32=1]` has been determined, the algorithm/key has not been determined | ① Search the second RSA modulus (128B high entropy constant) in `.rodata`/`.data` of `libTmsdk`; ② If the goal is to "understand the authorized content", the faster path is to hook `f5.f()`'s `Cipher.doFinal` return; ③ `liscense.conf` can try to use SM4-ECB/CBC + `62BAFD662A63A99A7C96DA027AFDF739` (16B) or `D5DFB878…8EC8` (16B) to do a 25-block trial solution |
| **U4** | `liburldetectorsys` press `liburl.so` dlopen, but the disk is `libavlurl.so` | [Observation] String `liburl.so` + SONAME `liburl.so`; `setLibPath(String)I` is the remediation interface exposed by JNI | Check whether `com/avl/engine/urldetector/UrlDetector.java` calls `setLibPath(real path)` before `loadURL()` |
| **U5** | There is no corresponding library for `loadLibrary("xy")` of `com.tencent.tcuser.util.Encode` | `Encode.java:43,48` is in `try/catch`. Failure is reported only in `pa.a(...,"System.loadLibrary error",null)`. | It is determined to be **dead code/downgraded branch**; to confirm, check whether the native method of `Encode` has a call point |
| **U6** | The specific position and IV source of SM2/SM4 in the protocol | Only "algorithm binding + all keys are external" is confirmed, no clear text IV/key is seen | Read the upper-level call of `com/tencent/tmf.*` (the caller of `com.tencent.tmf.gm.b.java`) |
| **U7** | Encryption algorithm and group index structure of `70003.amf` | Determined: `"amf\0"`+ver4+16 zero+double u32 timestamp+42,556×16B ciphertext; no duplicate blocks (not the same text block as ECB) | Use `nativeLoadAmfHeaderBytes`(0x1c514) of `libams` as a dynamic hook, or try `!@#$%^&*()_JHGFD` as the table name XOR and then use `nativeUpdateMalwareInfoBytes` to observe |
| **U8** | Clear text of `40706.dat` (certificate MD5 whitelist entry number/content) | Determined semantics (`CERTMD5_WHITELIST`) + cipher (`TccCryptor`) + 8-byte alignment | Hook `TccCryptor.decrypt` on the real device to get the plain text; or statically restore the built-in key of `decrypt` in `libTmsdk` |
| **U9** | 9 obfuscated short name methods for `AVLA` (`gufbyud/icf/sjcf/suhms/suf/sfcs/sfcp/nec/gcv/geci/gsi`) semantics | The name and signature are not in the `.rodata` string table and can only be inferred from the Java call point. | Need to decompile `libavlasys` (U0 is not released) or dynamic trace |

| **U10** | **`avlsdk:sdk_conf/sdk.conf`’s 9,999 B ciphertext segment algorithm/key** | Structure determined (§2.3.1): `"3030303030"` + hex summary + base64(253 B) + `"@"` + `0x17C` starting from 9,999 B High entropy. <br>**has excluded**: pure AES/SM4-ECB/CBC (9,999 is not a multiple of 16), AES-128 `wcw1xbplx7aijwqr` (length is not). | ① Try `[u32 len]` prefix offset (take the whole block starting from 0x17C+4); ② Try streaming mode: RC4 / ChaCha20 / TccCryptor (`libTccCryptor.decrypt` supports any length), key is used `62BAFD662A63A99A7C96DA027AFDF739`(16B), `D5DFB878335A3EF4E76F60ED470B8EC8`(16B), `wcw1xbplx7aijwqr`(16B); ③ If the solution should be text configuration, you can use "plain text contains `http`/`=`/`{}`" for crib verification; ④ Faster way: hook `libavlasys` Function to read `sdk.conf` |

Number of unresolved issues for **: 11 (U0–U10).**

---

## 8. REA evidence ID list

| # | Operation | target | evidence_id | conclusion purpose |
|---|---|---|---|---|
| 1 | `inspect_native_exports` (detail=summary) | `libavlm.so` | `ev_e195c9e10f580a383ae2efe99b1d9bbaf90c48a2032af92a4566700e5abafa51` | Review **98 exports** (`examined_entries 248`, `coverage complete`); used in §2.2.3 / §6 |
| 2 | Same as above | `libavlurl.so` | `ev_7db963e1f608d111e8de5e0e93844bb1820c91a6977ef11885c703531f2d1cf5` | Review **33 exports**; use in §2.2.4 |
| 3 | Same as above | `libTmsdk-2.0.13-mfr.so` | `ev_ec4f7f1d4c232e2514e8c91727e4ba604594e57e6fc83c4d6511e84d10d6c3bf` | Review that **only exports 5** (including the only `JNI_OnLoad`); use §2.2.8 |
| 4 | Same as above | `libflamingo.so` | `ev_b6d546cfcab1ea7265b0a871d2987301484544afe5098b3cf16b2319012f490d` | Review that **has exactly 3 exports**, which correspond to the 3 native methods of `RustSupport` one-to-one; use §4.1.1 / §4.3.5 |
| — | `open_binary` | `libavlasys.so` | sha256 `8fec8cf73748963141206ca29cd0fc15d54ff43129af573ed9e5eab6bfe12ff0` (opened successfully, subsequent `analyze_function` failed due to U0) | §7-U0 |

[Observation] For the above four derived evidences `authority = shipped-artifact` and `confidence = derived`, the restriction conditions (3 items) are included with the tool:
"The hidden symbol version is not the default version of dlsym" "Only global/weak/unique symbols with default/protected visibility are listed" "The symbol table is not equal to reachability/callability".

---

## Attachment: Self-built forensic tools used in this report (all can be reproduced)

| script | Purpose |
|---|---|
| `_work_tnative\elf.ps1` | ELF64 header/section/section/`.dynamic`/dynsym/notes analysis (pure .NET, no third-party dependencies) |
| `_work_tnative\scan2.ps1` | 12 libraries × 17 sets of regular Latin1 full-byte forensics (`URLS/DOMAINS/ANTIDBG/ROOT/EMU/JNI_*/ASSETS/MODLOAD/CRYPTO/PEM/SVC/SHELL/PROC`) |
| `_work_tnative\ctx.ps1` | Hit point ±N byte context slice, used for **to exclude false positive** (`Friday`/`blackfriday`/`exposed`) |
| `_work_tnative\refs.ps1` | AArch64 `ADRP+ADD` instruction decoding → "Function → Quoted string" ownership (replacing `analyze_function`) |
| `_work_tnative\lic.ps1` | RSA-1024 public key operation + DES-ECB + PKCS#1 depadding + 8-byte sliding window trial solution |

