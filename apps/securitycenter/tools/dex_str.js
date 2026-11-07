'use strict';
const fs = require('fs');
const files = process.argv.slice(2);
function stringsOf(buf) {
  const size = buf.readUInt32LE(0x38);
  const off = buf.readUInt32LE(0x3c);
  const out = [];
  for (let i = 0; i < size; i++) {
    const so = buf.readUInt32LE(off + i * 4);
    let p = so, b;
    do { b = buf[p++]; } while (b & 0x80);
    let end = p;
    while (end < buf.length && buf[end] !== 0) end++;
    out.push(buf.toString('utf8', p, end));
  }
  return out;
}
for (const f of files) {
  const buf = fs.readFileSync(f);
  const s = stringsOf(buf);
  const counts = {};
  const pats = ['Ltmsdk/', 'com/miui/guardprovider', 'com/miui/securityscan', 'com/miui/securitycenter',
    'com/tencent', 'okhttp3', 'retrofit2', 'dagger', 'hilt', 'glide', 'io/reactivex', 'DexClassLoader',
    'InMemoryDexClassLoader', 'Lcom/squareup', 'turingfd', 'Lcom/google/gson', 'Lcom/google/protobuf',
    'exoplayer', 'Lokio', 'kotlinx/coroutines', 'xcrash'];
  for (const p of pats) counts[p] = s.filter((x) => x.includes(p)).length;
  console.log('## ' + require('path').basename(f) + '  strings=' + s.length);
  for (const [k, v] of Object.entries(counts)) console.log('   ' + v + '\t' + k);
}
