# Offline analysis tools

These are self-built scripts used in the original analysis, the public version uses path parameters. Requires Node.js for execution; APK, DEX, ELF and the complete local parse tree are not distributed with the package. The output may contain local paths to input files and should only be saved locally and anonymized again before being made public.

| script | Usage and boundaries |
|---|---|
| `privacy_review_verify.cjs` | `node tools/privacy_review_verify.cjs /path/to/sample-root /path/to/local-result.json`. The input root directory contains the original parse trees of samples APK and `_analysis/`; recalculate the APK/DEX/source code hash and key DEX strings. The default output is `verification-local.json`. This tool does not execute APK; it is not a control flow correctness proof. |
| `axml2xml.js` | `node tools/axml2xml.js input.axml output.xml`. Minimal AXML parser, not a replacement for full resource parsing of official Android tools. |
| `dex_str.js` | `node tools/dex_str.js classes.dex [classes2.dex ...]`. Parse the DEX string table and summarize the preset namespace keywords; the existence of the string does not prove that the code is called. |
| `a64scan.mjs` | `node tools/a64scan.mjs library.so --exact SYMBOL --max 120`, also supports `--list`, `--addr`, `--all`. Only handles the limited AArch64 instructions of the little-endian ELF64. It is a partial forensic tool, not a full native decompiler. |
| `package_reports.cjs` | `node tools/package_reports.cjs /path/to/_analysis`. Reorganize the selected text, source files and hash index from the original analysis directory; overwrite the corresponding products in the application subdirectory. It is only an aid for desensitization and does not replace item-by-item review before release. |

This directory does not have runtime packet capture tools, nor does it contain real user fields, APK or binary files.
