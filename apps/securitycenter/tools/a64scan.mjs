#!/usr/bin/env node
// Focused AArch64 (arm64-v8a) static scanner for Android ELF .so:
//  - export symbol map (addr -> name)
//  - .eh_frame_hdr function-start map (when present)
//  - partial instruction decoder covering ADRP/ADR/ADD/BL/B/CBZ/TBZ/RET/MOVZ/MOVK/MOV/LDR-literal/STP
//  - per-function annotated disassembly with resolved strings/symbols, string refs and callees
// Usage: node a64scan.mjs FILE [--exact NAME|--addr 0xNN] [--max 120] [--list] [--all]
import fs from 'node:fs';

const argv = process.argv.slice(2);
const file = argv[0];
const opt = { exact: [], addr: [], max: 120, list: false, all: false };
for (let i = 1; i < argv.length; i++) {
  const a = argv[i];
  if (a === '--exact') opt.exact.push(argv[++i]);
  else if (a === '--addr') opt.addr.push(parseInt(argv[++i], 16));
  else if (a === '--max') opt.max = parseInt(argv[++i], 10);
  else if (a === '--list') opt.list = true;
  else if (a === '--all') opt.all = true;
}

const buf = fs.readFileSync(file);
const le = buf[5] === 1, is64 = buf[4] === 2;
if (!le || !is64) throw new Error('expected little-endian ELF64 (arm64-v8a); use a v7a-specific decoder for arm');
const u16 = o => buf.readUInt16LE(o), u32 = o => buf.readUInt32LE(o), u64 = o => Number(buf.readBigUInt64LE(o));
const phoff = u64(32), phentsize = u16(54), phnum = u16(56);
const shoff = u64(40), shentsize = u16(58), shnum = u16(60), shstrndx = u16(62);
const secs = [];
for (let i = 0; i < shnum; i++) {
  const o = shoff + i * shentsize;
  secs.push({ nameOff: u32(o), type: u32(o + 4), flags: u64(o + 8), addr: u64(o + 16), off: u64(o + 24), size: u64(o + 32), entsize: u64(o + 56) });
}
const shstr = buf.subarray(secs[shstrndx].off, secs[shstrndx].off + secs[shstrndx].size);
const cs = (t, o) => { let e = o; while (e < t.length && t[e]) e++; return t.toString('utf8', o, e); };
secs.forEach(s => s.name = cs(shstr, s.nameOff));
const v2o = v => { for (const s of secs) if (s.flags & 2 && v >= s.addr && v < s.addr + s.size) return s.off + (v - s.addr); return null; };
const o2v = o => { for (const s of secs) if (s.flags & 2 && o >= s.off && o < s.off + s.size) return s.addr + (o - s.off); return null; };
const readCStr = v => {
  const fo = v2o(v); if (fo === null) return null;
  let e = fo; while (e < buf.length && e - fo < 300 && buf[e] && buf[e] >= 0x20) e++;
  if (e - fo < 3) return null;
  const s = buf.toString('utf8', fo, e);
  return /[\x00-\x1f]/.test(s) ? null : s;
};

// ---- export map
const dynsym = secs.find(s => s.name === '.dynsym');
const dynstr = secs.find(s => s.name === '.dynstr');
const dstr = buf.subarray(dynstr.off, dynstr.off + dynstr.size);
const symByAddr = new Map();
const funcs = [];
for (let i = 0; i < Math.floor(dynsym.size / dynsym.entsize); i++) {
  const o = dynsym.off + i * dynsym.entsize;
  const nameOff = u32(o), shndx = u16(o + 6), value = u64(o + 8), size = u64(o + 16);
  const nm = cs(dstr, nameOff);
  if (!nm || shndx === 0) continue;
  symByAddr.set(value, nm);
  funcs.push({ addr: value, size, name: nm });
}
const nameOf = a => symByAddr.get(a) || pltName(a) || null;

// ---- PLT -> imported symbol name (via .rela.plt order)
const pltSec = secs.find(s => s.name === '.plt');
const relaPlt = secs.find(s => s.name === '.rela.plt');
const pltNames = [];
if (pltSec && relaPlt) {
  const per = pltSec.entsize || 16;
  const total = Math.floor(pltSec.size / per);
  for (let i = 0; i < total; i++) pltNames.push(null);
  for (let i = 0; i < Math.floor(relaPlt.size / relaPlt.entsize); i++) {
    const o = relaPlt.off + i * relaPlt.entsize;
    const symIdx = Math.floor(u64(o + 8) / 4294967296);
    const symOff = dynsym.off + symIdx * dynsym.entsize;
    const nm = cs(dstr, u32(symOff));
    if (i + 1 < pltNames.length) pltNames[i + 1] = nm;   // slot 0 is the resolver stub
  }
}
function pltName(a) {
  if (!pltSec || a < pltSec.addr || a >= pltSec.addr + pltSec.size) return null;
  const idx = Math.floor((a - pltSec.addr) / (pltSec.entsize || 16));
  const nm = pltNames[idx];
  return nm ? 'plt:' + nm : null;
}

// ---- .eh_frame_hdr -> function starts
let funcStarts = [];
const eh = secs.find(s => s.name === '.eh_frame_hdr');
if (eh) {
  try {
    let p = eh.off;
    const ver = buf[p], ehEnc = buf[p + 1], cntEnc = buf[p + 2], tabEnc = buf[p + 3];
    p += 4;
    const rdEnc = (enc) => {
      const fmt = enc & 0x0f, app = enc & 0x70;
      let v, at = p;
      if (fmt === 0x03) { v = u32(p); p += 4; }
      else if (fmt === 0x0b) { v = u32(p) | 0; p += 4; }
      else if (fmt === 0x02) { v = u16(p); p += 2; }
      else if (fmt === 0x01) { v = buf[p]; p += 1; }
      else if (fmt === 0x04) { v = u64(p); p += 8; }
      else return null;
      if (app === 0x10) v = (o2v(at) || 0) + v;      // pcrel-ish
      return v;
    };
    rdEnc(ehEnc);
    const cnt = rdEnc(cntEnc);
    const starts = [];
    const base = eh.addr;
    for (let i = 0; i < (cnt || 0); i++) {
      const f = tabEnc & 0x0f, app = tabEnc & 0x70;
      let loc, at = p;
      if (f === 0x0b) { loc = u32(p); p += 4; }
      else if (f === 0x03) { loc = u32(p); p += 4; }
      else if (f === 0x02) { loc = u16(p); p += 2; }
      else if (f === 0x01) { loc = buf[p]; p += 1; }
      else if (f === 0x04) { loc = u64(p); p += 8; }
      else break;
      if (app === 0x30) loc = base + loc;            // datarel
      if (f === 0x0b) loc = base + loc;
      let dummy; const at2 = p;
      if (f === 0x0b) { dummy = u32(p); p += 4; } else if (f === 0x03) { dummy = u32(p); p += 4; } else if (f === 0x02) { dummy = u16(p); p += 2; } else if (f === 0x01) { dummy = buf[p]; p += 1; } else { dummy = u64(p); p += 8; }
      starts.push(loc);
    }
    funcStarts = [...new Set(starts)].sort((a, b) => a - b);
  } catch (e) { funcStarts = []; }
}
if (!funcStarts.length) {
  funcStarts = [...new Set([...funcs.filter(f => f.size > 0).map(f => f.addr)])].sort((a, b) => a - b);
}
const nextStart = a => { for (const s of funcStarts) if (s > a) return s; return a + 0x4000; };

// ---- instruction decode helpers
const sx = (v, bits) => (v & (1 << (bits - 1))) ? v - (1 << bits) : v;
const MSK = (i, m) => ((i & m) >>> 0);
function decode(pc, insn) {
  if (insn === 0xD503201F) return { m: 'nop' };
  if (MSK(insn, 0x1F000000) === 0x10000000) {
    const op = (insn >>> 31) & 1, rd = insn & 0x1f;
    const immlo = (insn >>> 29) & 3, immhi = (insn >>> 5) & 0x7ffff;
    const imm = sx((immhi << 2) | immlo, 21);
    return op ? { m: 'adrp', rd, val: (pc - (pc % 0x1000)) + (imm * 0x1000), abs: true }
              : { m: 'adr', rd, val: pc + imm, abs: true };
  }
  if (MSK(insn, 0x7F000000) === 0x11000000 && ((insn >>> 29) & 3) === 0) {
    const rd = insn & 0x1f, rn = (insn >>> 5) & 0x1f, imm = (insn >>> 10) & 0xfff, sh = (insn >>> 22) & 1;
    return { m: (insn & 0x80000000) ? 'add' : 'add32', rd, rn, imm: sh ? imm << 12 : imm, isAdd: true };
  }
  if (MSK(insn, 0x7F000000) === 0x51000000 && ((insn >>> 29) & 3) === 0) {
    const rd = insn & 0x1f, rn = (insn >>> 5) & 0x1f, imm = (insn >>> 10) & 0xfff, sh = (insn >>> 22) & 1;
    return { m: 'sub', rd, rn, imm: sh ? imm << 12 : imm };
  }
  if (MSK(insn, 0x7F200000) === 0x0B000000) {
    const rd = insn & 0x1f, rn = (insn >>> 5) & 0x1f, rm = (insn >>> 16) & 0x1f, sh = (insn >>> 22) & 3;
    return { m: 'add_r', rd, rn, rm, shift: sh };
  }
  if ((insn >>> 26) === 0x25) { const imm = insn & 0x3ffffff; return { m: 'bl', target: pc + (sx(imm, 26) << 2) }; }
  if ((insn >>> 26) === 0x05) { const imm = insn & 0x3ffffff; return { m: 'b', target: pc + (sx(imm, 26) << 2) }; }
  if ((insn >>> 24) === 0x54 && (insn & 0x10) === 0) {
    const imm = (insn >>> 5) & 0x7ffff; return { m: 'b.' + (insn & 0xf).toString(16), target: pc + (sx(imm, 19) << 2) };
  }
  if (MSK(insn, 0x7E000000) === 0x34000000) {
    const rt = insn & 0x1f, imm = (insn >>> 5) & 0x7ffff;
    return { m: (insn & 0x01000000) ? 'cbnz' : 'cbz', rt, target: pc + (sx(imm, 19) << 2) };
  }
  if (MSK(insn, 0x7E000000) === 0x36000000) {
    const rt = insn & 0x1f, bit = ((insn >>> 26) & 0x20) | ((insn >>> 19) & 0x1f), imm = (insn >>> 5) & 0x3fff;
    return { m: (insn & 0x01000000) ? 'tbnz' : 'tbz', rt, bit, target: pc + (sx(imm, 14) << 2) };
  }
  if (MSK(insn, 0xFFFFFC1F) === 0xD65F0000) return { m: 'ret', rn: (insn >>> 5) & 0x1f };
  if (MSK(insn, 0xFFE0FFE0) === 0xAA0003E0) return { m: 'mov', rd: insn & 0x1f, rm: (insn >>> 16) & 0x1f };
  if (MSK(insn, 0x7F800000) === 0x52800000 || MSK(insn, 0x7F800000) === 0x12800000) {
    const rd = insn & 0x1f, imm16 = (insn >>> 5) & 0xffff, hw = (insn >>> 21) & 3;
    return { m: 'movz', rd, imm: imm16 * Math.pow(2, hw * 16), wide: (insn & 0x80000000) !== 0 };
  }
  if (MSK(insn, 0x7F800000) === 0x72800000 || MSK(insn, 0x7F800000) === 0xF2800000) {
    const rd = insn & 0x1f, imm16 = (insn >>> 5) & 0xffff, hw = (insn >>> 21) & 3;
    return { m: 'movk', rd, imm: imm16 * Math.pow(2, hw * 16), wide: (insn & 0x80000000) !== 0 };
  }
  if (MSK(insn, 0x3B000000) === 0x18000000) {
    const rt = insn & 0x1f, imm = (insn >>> 5) & 0x7ffff;
    return { m: 'ldr_lit', rt, target: pc + (sx(imm, 19) << 2), ptr: true };
  }
  if (MSK(insn, 0x3B200000) === 0x38000000) {
    const rt = insn & 0x1f, rn = (insn >>> 5) & 0x1f, imm = sx((insn >>> 12) & 0x1ff, 9);
    const load = (insn >>> 22) & 1, mode = (insn >>> 10) & 3;
    return { m: (load ? 'ldr' : 'str') + (mode === 3 ? '.pre' : mode === 1 ? '.post' : '.unscaled'), rt, rn, imm };
  }
  if (MSK(insn, 0x3B000000) === 0x39000000) {
    const rt = insn & 0x1f, rn = (insn >>> 5) & 0x1f, imm = ((insn >>> 10) & 0xfff) * Math.pow(2, (insn >>> 30) & 3);
    const load = (insn >>> 22) & 1;
    return { m: load ? 'ldr' : 'str', rt, rn, imm };
  }
  if (MSK(insn, 0x3A000000) === 0x28000000) {
    const rt = insn & 0x1f, rn = (insn >>> 5) & 0x1f, rt2 = (insn >>> 10) & 0x1f, imm = sx((insn >>> 15) & 0x7f, 7) * 8;
    const load = (insn >>> 22) & 1;
    return { m: load ? 'ldp' : 'stp', rt, rt2, rn, imm };
  }
  if (MSK(insn, 0xFFC00000) === 0xD5300000) return { m: 'mrs/msr' };
  return { m: '??', raw: insn };
}

function scanFunction(start, limit) {
  const end = Math.min(nextStart(start), start + 0x8000);
  const out = [];
  const regs = new Map();
  const calls = [], strs = [];
  let n = 0;
  for (let pc = start; pc < end && n < limit; pc += 4) {
    const fo = v2o(pc);
    if (fo === null) break;
    const insn = buf.readUInt32LE(fo);
    const d = decode(pc, insn);
    let note = '';
    if (d.abs) { regs.set(d.rd, d.val); note = `; =0x${d.val.toString(16)}` + (readCStr(d.val) ? ` "${readCStr(d.val)}"` : ''); }
    else if (d.isAdd && regs.has(d.rn)) {
      const v = regs.get(d.rn) + d.imm; regs.set(d.rd, v);
      const s = readCStr(v);
      note = `; =0x${v.toString(16)}` + (s ? ` "${s}"` : '');
      if (s) strs.push({ pc, addr: v, s });
    } else if (d.m === 'ldr_lit') {
      const dpc = pc + d.target;
      const dfo = v2o(dpc);
      if (dfo !== null) {
        const ptr = Number(buf.readBigUInt64LE(dfo));
        const s = readCStr(ptr);
        note = `; [0x${dpc.toString(16)}]=0x${ptr.toString(16)}` + (s ? ` "${s}"` : '');
        if (s) strs.push({ pc, addr: ptr, s });
      }
    } else if (d.m === 'bl' || d.m === 'b') {
      const nm = nameOf(d.target);
      note = nm ? `; ${nm}` : `; sub_${d.target.toString(16)}`;
      if (d.m === 'bl') calls.push({ pc, target: d.target, name: nm });
    } else if (d.m === 'add_r' && regs.has(d.rn) && regs.has(d.rm)) {
      const v = regs.get(d.rn) + regs.get(d.rm);
      const s = readCStr(v);
      if (s) { note = `; =0x${v.toString(16)} "${s}"`; strs.push({ pc, addr: v, s }); }
    }
    const rd = d.rd !== undefined ? `x${d.rd}` : '';
    const rn = d.rn !== undefined ? `x${d.rn}` : '';
    const rm = d.rm !== undefined ? `x${d.rm}` : '';
    let ops = '';
    if (d.m === 'adrp' || d.m === 'adr') ops = `${rd}, #0x${(d.val >>> 12).toString(16)}`;
    else if (d.m === 'add' || d.m === 'add32' || d.m === 'sub') ops = `${rd}, ${rn}, #0x${d.imm.toString(16)}`;
    else if (d.m === 'add_r') ops = `${rd}, ${rn}, ${rm}`;
    else if (d.m === 'bl' || d.m === 'b' || d.m.startsWith('b.')) ops = `0x${d.target.toString(16)}`;
    else if (d.m === 'cbz' || d.m === 'cbnz') ops = `x${d.rt}, 0x${d.target.toString(16)}`;
    else if (d.m === 'tbz' || d.m === 'tbnz') ops = `x${d.rt}, #${d.bit}, 0x${d.target.toString(16)}`;
    else if (d.m === 'mov') ops = `${rd}, ${rm}`;
    else if (d.m === 'movz' || d.m === 'movk') ops = `${rd}, #0x${d.imm.toString(16)}`;
    else if (d.m === 'ldr' || d.m === 'str') ops = `x${d.rt}, [${d.rn === 31 ? 'sp' : rn}, #0x${d.imm.toString(16)}]`;
    else if (d.m.endsWith('.pre')) ops = `x${d.rt}, [${d.rn === 31 ? 'sp' : rn}, #${d.imm}]!`;
    else if (d.m.endsWith('.post')) ops = `x${d.rt}, [${d.rn === 31 ? 'sp' : rn}], #${d.imm}`;
    else if (d.m.endsWith('.unscaled')) ops = `x${d.rt}, [${d.rn === 31 ? 'sp' : rn}, #${d.imm}]`;
    else if (d.m === 'ldp' || d.m === 'stp') ops = `x${d.rt}, x${d.rt2}, [${d.rn === 31 ? 'sp' : rn}, #0x${d.imm}]`;
    else if (d.m === 'ldr_lit') ops = `x${d.rt}, 0x${d.target.toString(16)}`;
    else if (d.m === 'ret') ops = '';
    else if (d.raw !== undefined) ops = `0x${d.raw.toString(16).padStart(8, '0')}`;
    out.push(`  0x${pc.toString(16)}  ${d.m.padEnd(8)} ${ops.padEnd(34)} ${note}`);
    n++;
  }
  return { out, calls, strs };
}

// ---- main
const targets = [];
if (opt.addr.length) for (const a of opt.addr) targets.push({ addr: a, name: nameOf(a) || `sub_${a.toString(16)}` });
for (const nm of opt.exact) {
  const e = funcs.filter(f => f.name === nm);
  if (!e.length) console.log(`!! symbol not found: ${nm}`);
  for (const x of e) targets.push({ addr: x.addr, name: x.name });
}
if (opt.all) for (const e of [...funcs].sort((a, b) => a.addr - b.addr)) targets.push({ addr: e.addr, name: e.name });

console.log(`# ${file}`);
console.log(`# sections: ${secs.filter(s => s.flags & 2).map(s => s.name + '@' + s.addr.toString(16)).join(' ')}`);
console.log(`# exported symbols=${funcs.length}  eh_frame_hdr funcstarts=${funcStarts.length}  text=${(secs.find(s => s.name === '.text') || {}).size}`);
for (const t of targets) {
  const r = scanFunction(t.addr, opt.max);
  console.log(`\n=== ${t.name} @0x${t.addr.toString(16)} (next=0x${nextStart(t.addr).toString(16)}, len=0x${(nextStart(t.addr) - t.addr).toString(16)})`);
  console.log(r.out.join('\n'));
  const uniq = [...new Set(r.strs.map(x => x.s))];
  if (uniq.length) console.log(`  -> strings: ${uniq.map(s => JSON.stringify(s)).join(', ')}`);
  const callees = [...new Set(r.calls.map(c => c.name || ('sub_' + c.target.toString(16))))];
  if (callees.length) console.log(`  -> calls: ${callees.join(', ')}`);
}
