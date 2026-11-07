import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import {root} from './privacy-check.mjs';

const manifest=JSON.parse(fs.readFileSync(path.join(root,'ARTIFACTS.json'),'utf8'));
const errors=[];
for(const item of manifest.files){
  const f=path.resolve(root,item.path);
  if(!f.startsWith(root+path.sep)){errors.push({file:item.path,error:'path-outside-repository'});continue;}
  if(!fs.existsSync(f)){errors.push({file:item.path,error:'missing'});continue;}
  const b=fs.readFileSync(f),sha256=crypto.createHash('sha256').update(b).digest('hex');
  if(b.length!==item.bytes||sha256!==item.sha256)errors.push({file:item.path,error:'content-mismatch'});
}
console.log(JSON.stringify({checked:manifest.files.length,passed:errors.length===0,errors},null,2));
if(errors.length)process.exitCode=1;
