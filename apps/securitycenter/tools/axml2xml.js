// Minimal Android binary XML (AXML) decoder -> pretty XML text.
// Usage: node axml2xml.js <input.axml> <output.xml>
'use strict';
const fs = require('fs');

const RES_STRING_POOL = 0x0001;
const RES_XML_START_NS = 0x0100;
const RES_XML_END_NS = 0x0101;
const RES_XML_START_ELEMENT = 0x0102;
const RES_XML_END_ELEMENT = 0x0103;
const RES_XML_CDATA = 0x0104;
const RES_XML_RESOURCE_MAP = 0x0180;

const TYPE_NULL = 0x00, TYPE_REFERENCE = 0x01, TYPE_ATTRIBUTE = 0x02,
  TYPE_STRING = 0x03, TYPE_FLOAT = 0x04, TYPE_DIMENSION = 0x05, TYPE_FRACTION = 0x06,
  TYPE_INT_DEC = 0x10, TYPE_INT_HEX = 0x11, TYPE_INT_BOOLEAN = 0x12,
  TYPE_INT_COLOR_ARGB8 = 0x1c, TYPE_INT_COLOR_RGB8 = 0x1d,
  TYPE_INT_COLOR_ARGB4 = 0x1e, TYPE_INT_COLOR_RGB4 = 0x1f;

function u16(b, o) { return b.readUInt16LE(o); }
function u32(b, o) { return b.readUInt32LE(o); }

function parseStringPool(buf, start) {
  const headerSize = u16(buf, start + 2);
  const size = u32(buf, start + 4);
  const stringCount = u32(buf, start + 8);
  const styleCount = u32(buf, start + 12);
  const flags = u32(buf, start + 16);
  const stringsStart = u32(buf, start + 20);
  const isUtf8 = (flags & 0x100) !== 0;
  const offsetsBase = start + headerSize;
  const stringsBase = start + stringsStart;
  const out = [];
  for (let i = 0; i < stringCount; i++) {
    const off = u32(buf, offsetsBase + i * 4);
    let p = stringsBase + off;
    try {
      if (isUtf8) {
        // varint char length
        let n = buf[p++]; if (n & 0x80) { n = ((n & 0x7f) << 8) | buf[p++]; }
        let bl = buf[p++]; if (bl & 0x80) { bl = ((bl & 0x7f) << 8) | buf[p++]; }
        out.push(buf.toString('utf8', p, p + bl));
      } else {
        let n = u16(buf, p); p += 2;
        if (n & 0x8000) { n = ((n & 0x7fff) << 16) | u16(buf, p); p += 2; }
        out.push(buf.toString('utf16le', p, p + n * 2));
      }
    } catch (e) { out.push(''); }
  }
  return { strings: out, size, styleCount };
}

function decodeAxml(buf) {
  if (u16(buf, 0) !== 0x0003 || u16(buf, 2) !== 0x0008) {
    throw new Error('not AXML: type=' + u16(buf, 0).toString(16) + ' hdr=' + u16(buf, 2));
  }
  const total = u32(buf, 4);
  let p = u16(buf, 2); // header size of root chunk (8)
  let pool = null;
  const resMap = [];
  const events = [];
  while (p + 8 <= Math.min(total, buf.length)) {
    const type = u16(buf, p);
    const hsz = u16(buf, p + 2);
    const csz = u32(buf, p + 4);
    if (csz < 8) break;
    if (type === RES_STRING_POOL) {
      pool = parseStringPool(buf, p);
    } else if (type === RES_XML_RESOURCE_MAP) {
      const n = (csz - hsz) / 4;
      for (let i = 0; i < n; i++) resMap.push(u32(buf, p + hsz + i * 4));
    } else if (type === RES_XML_START_NS) {
      events.push({ t: 'startns' });
    } else if (type === RES_XML_END_NS) {
      events.push({ t: 'endns' });
    } else if (type === RES_XML_START_ELEMENT) {
      // node header: hsz bytes; then lineNumber, comment at p+8, p+12
      const line = u32(buf, p + 8);
      const nsIdx = u32(buf, p + hsz);
      const nameIdx = u32(buf, p + hsz + 4);
      const attrStart = u16(buf, p + hsz + 8);
      const attrSize = u16(buf, p + hsz + 10);
      const attrCount = u16(buf, p + hsz + 12);
      const abase = p + hsz + attrStart;
      const attrs = [];
      for (let i = 0; i < attrCount; i++) {
        const a = abase + i * attrSize;
        const ansIdx = u32(buf, a);
        const anameIdx = u32(buf, a + 4);
        const arawIdx = u32(buf, a + 8);
        const dsize = u16(buf, a + 12);
        const dtype = buf[a + 15];
        const ddata = u32(buf, a + 16);
        attrs.push({
          ns: ansIdx === 0xffffffff ? null : (pool ? pool.strings[ansIdx] : ansIdx),
          name: pool ? pool.strings[anameIdx] : String(anameIdx),
          raw: arawIdx === 0xffffffff ? null : (pool ? pool.strings[arawIdx] : arawIdx),
          type: dtype, data: ddata,
        });
      }
      events.push({
        t: 'start', line,
        ns: nsIdx === 0xffffffff ? null : (pool ? pool.strings[nsIdx] : nsIdx),
        name: pool ? pool.strings[nameIdx] : String(nameIdx),
        attrs,
      });
    } else if (type === RES_XML_END_ELEMENT) {
      const line = u32(buf, p + 8);
      const nameIdx = u32(buf, p + hsz + 4);
      events.push({ t: 'end', line, name: pool ? pool.strings[nameIdx] : String(nameIdx) });
    } else if (type === RES_XML_CDATA) {
      const line = u32(buf, p + 8);
      const di = u32(buf, p + hsz);
      events.push({ t: 'text', line, text: pool && di !== 0xffffffff ? pool.strings[di] : '' });
    }
    p += csz;
  }
  return { pool, resMap, events };
}

function fmtValue(a, pool) {
  switch (a.type) {
    case TYPE_NULL: return null;
    case TYPE_STRING: return pool ? pool.strings[a.data] : String(a.data);
    case TYPE_REFERENCE: return '@0x' + (a.data >>> 0).toString(16).padStart(8, '0');
    case TYPE_ATTRIBUTE: return '?0x' + (a.data >>> 0).toString(16).padStart(8, '0');
    case TYPE_INT_BOOLEAN: return a.data !== 0 ? 'true' : 'false';
    case TYPE_INT_DEC: return String(a.data | 0);
    case TYPE_INT_HEX: return '0x' + (a.data >>> 0).toString(16);
    case TYPE_FLOAT: { const f = new Float32Array(new Uint32Array([a.data]).buffer)[0]; return String(f); }
    case TYPE_INT_COLOR_ARGB8: return '#%08x'.replace('%08x', (a.data >>> 0).toString(16).padStart(8, '0'));
    case TYPE_INT_COLOR_RGB8: return '#' + (a.data >>> 0).toString(16).padStart(8, '0').slice(2);
    case TYPE_INT_COLOR_ARGB4: return '#' + (a.data >>> 0).toString(16).padStart(8, '0').slice(0, 4);
    case TYPE_INT_COLOR_RGB4: return '#' + (a.data >>> 0).toString(16).padStart(8, '0').slice(4, 7);
    default: return a.raw !== null ? a.raw : '0x' + (a.data >>> 0).toString(16);
  }
}

const PROT_BASE = { 0: 'normal', 1: 'dangerous', 2: 'signature', 3: 'signatureOrSystem' };
const PROT_FLAGS = [
  [0x10, 'privileged'], [0x20, 'development'], [0x40, 'system'],
  [0x80, 'appop'], [0x100, 'pre23'], [0x200, 'installer'],
  [0x400, 'verifier'], [0x800, 'preinstalled'], [0x1000, 'ephemeral'],
  [0x2000, 'instant'], [0x4000, 'runtime'], [0x8000, 'oem'],
  [0x10000, 'vendorPrivileged'], [0x20000, 'textClassifier'], [0x40000, 'wellbeing'],
  [0x80000, 'documenter'], [0x100000, 'configurator'], [0x200000, 'incidentReportApprover'],
  [0x400000, 'appPredictor'], [0x800000, 'companion'], [0x1000000, 'module'],
];

function decodeProtectionLevel(v) {
  const n = Number(v);
  if (!Number.isFinite(n)) return v;
  const base = n & 0xf;
  const parts = [PROT_BASE[base] || ('base0x' + base.toString(16))];
  for (const [mask, nm] of PROT_FLAGS) if (n & mask) parts.push(nm);
  const extra = n & ~0xffffff;
  return parts.join('|') + ' (0x' + n.toString(16) + ')';
}

function decodeDeclaredPermissionish(name, v) {
  if (name === 'protectionLevel') return decodeProtectionLevel(v);
  return v;
}

const ANDROID_NS = 'http://schemas.android.com/apk/res/android';

function esc(s) {
  return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function toXml(dec) {
  const pool = dec.pool;
  const lines = [];
  let depth = 0;
  const ind = () => '    '.repeat(depth);
  lines.push('<?xml version="1.0" encoding="utf-8"?>');
  for (const ev of dec.events) {
    if (ev.t === 'start') {
      const parts = [];
      for (const a of ev.attrs) {
        let v = fmtValue(a, pool);
        if (v === null) continue;
        v = decodeDeclaredPermissionish(a.name, v);
        let pfx = '';
        if (a.ns === ANDROID_NS) pfx = 'android:';
        else if (a.ns) pfx = a.ns + ':';
        parts.push(pfx + a.name + '="' + esc(v) + '"');
      }
      if (parts.length === 0) {
        lines.push(ind() + '<' + ev.name + '>');
      } else if (parts.length <= 2) {
        lines.push(ind() + '<' + ev.name + ' ' + parts.join(' ') + '>');
      } else {
        lines.push(ind() + '<' + ev.name);
        parts.forEach((x, i) => lines.push(ind() + '    ' + x + (i === parts.length - 1 ? '>' : '')));
      }
      ev.xmlLine = lines.length;
      depth++;
    } else if (ev.t === 'end') {
      depth = Math.max(0, depth - 1);
      lines.push(ind() + '</' + ev.name + '>');
    } else if (ev.t === 'text') {
      const t = (ev.text || '').trim();
      if (t) lines.push(ind() + esc(t));
    }
  }
  return lines.join('\n') + '\n';
}

const inPath = process.argv[2];
const outPath = process.argv[3];
const buf = fs.readFileSync(inPath);
const dec = decodeAxml(buf);
const xml = toXml(dec);
fs.writeFileSync(outPath, xml, 'utf8');

// structured sidecar: resolved events with line numbers
const flat = dec.events.map((ev) => {
  if (ev.t === 'start') {
    const attrs = {};
    for (const a of ev.attrs) {
      let v = fmtValue(a, dec.pool);
      if (v === null) continue;
      v = decodeDeclaredPermissionish(a.name, v);
      attrs[a.name] = v;
    }
    return { t: 'start', line: ev.line, xmlLine: ev.xmlLine, name: ev.name, attrs };
  }
  if (ev.t === 'end') return { t: 'end', line: ev.line, xmlLine: ev.xmlLine, name: ev.name };
  if (ev.t === 'text') return { t: 'text', line: ev.line, text: ev.text };
  return { t: ev.t };
});
if (process.argv[4]) fs.writeFileSync(process.argv[4], JSON.stringify(flat, null, 1), 'utf8');

console.error('strings=' + (dec.pool ? dec.pool.strings.length : 0) +
  ' resMap=' + dec.resMap.length + ' events=' + dec.events.length +
  ' outBytes=' + xml.length + ' lines=' + xml.split('\n').length);

