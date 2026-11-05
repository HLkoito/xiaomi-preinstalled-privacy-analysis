# 离线分析工具

这些是原分析中使用的自建脚本，公开版使用路径参数。执行需要 Node.js；APK、DEX、ELF 与完整本地分析树未随包分发。输出可能包含输入文件的本地路径，只应在本地保存，公开前需再次脱敏。

| 脚本 | 用法与边界 |
|---|---|
| `privacy_review_verify.cjs` | `node tools/privacy_review_verify.cjs /path/to/sample-root /path/to/local-result.json`。输入根目录含样本 APK 与 `_analysis/` 原分析树；复算 APK/DEX/源码 hash 和关键 DEX 字符串。默认输出 `verification-local.json`。该工具不会执行 APK；不是控制流正确性证明。 |
| `axml2xml.js` | `node tools/axml2xml.js input.axml output.xml`。最小 AXML 解析器，不替代官方 Android 工具的完整资源解析。 |
| `dex_str.js` | `node tools/dex_str.js classes.dex [classes2.dex ...]`。解析 DEX 字符串表并汇总预设命名空间关键词；字符串存在不证明代码被调用。 |
| `a64scan.mjs` | `node tools/a64scan.mjs library.so --exact SYMBOL --max 120`，另支持 `--list`、`--addr`、`--all`。只处理 little-endian ELF64 的有限 AArch64 指令，是局部取证工具，不是完整 native 反编译器。 |
| `package_reports.cjs` | `node tools/package_reports.cjs /path/to/_analysis`。从原分析目录重新整理精选文本、源文件与 hash 索引；覆盖本应用子目录中的对应产物。它只是脱敏整理辅助，不替代发布前逐项审核。 |

本目录没有运行时抓包工具，也不含真实用户字段、APK 或二进制文件。
