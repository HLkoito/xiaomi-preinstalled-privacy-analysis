import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

export const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
export function files(dir=root){
  return fs.readdirSync(dir,{withFileTypes:true}).flatMap(e=>e.name==='.git'?[]:e.isDirectory()?files(path.join(dir,e.name)):[path.join(dir,e.name)]).sort();
}

const restrictedName=Buffer.from('686f6e656c696e','hex').toString('utf8');
const rules=[
  ['restricted-text',new RegExp(restrictedName,'i')],
  ['personal-windows-directory',/[a-z]:[\\/]+Users[\\/]+(?!Public(?:[\\/]|\b)|Default(?:[\\/]|\b))[^\s<>"`]+/i],
  ['personal-unix-directory',/\/(?:Users|home)\/(?!user\b|example\b)[a-zA-Z0-9_.-]+\//],
  ['private-session-id',/session-[a-f0-9]{8}-[a-f0-9-]{27,}/i],
  ['private-chat-link',/codex:\/\/threads\/[a-f0-9-]+/i],
  ['private-key',/-----BEGIN (?:OPENSSH |RSA |EC |DSA )?PRIVATE KEY-----/],
  ['service-credential',/\b(?:gh[pousr]_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{30,}|sk-(?:proj-|svcacct-)?[A-Za-z0-9_-]{28,}|AKIA[A-Z0-9]{16})\b/],
  ['binary-export',/\.(?:apk|dex|so|sqlite|har|pcapng?|saz|zstd)$/i]
];

function normalized(s){
  let out=s.normalize('NFKC').replace(/[\u200b-\u200d\ufeff]/g,'');
  for(let i=0;i<2;i++)try{const decoded=decodeURIComponent(out);if(decoded===out)break;out=decoded;}catch{break;}
  return out;
}

export function scan(){
  const findings=[];const list=files();
  for(const f of list){
    const rel=path.relative(root,f).replaceAll('\\','/');
    const buf=fs.readFileSync(f);
    if(buf.includes(0))findings.push({file:rel,rule:'non-text-file'});
    const name=normalized(rel),body=normalized(buf.toString('utf8'));
    for(const [rule,re] of rules){
      const match=re.exec(rule==='binary-export'?name:name+'\n'+body);
      if(match)findings.push({file:rel,rule,line:Math.max(1,body.slice(0,Math.max(0,match.index-name.length-1)).split('\n').length)});
    }
  }
  return {fileCount:list.length,passed:findings.length===0,findings};
}

if(process.argv[1]&&path.resolve(process.argv[1])===fileURLToPath(import.meta.url)){
  const result=scan();console.log(JSON.stringify(result,null,2));if(!result.passed)process.exitCode=1;
}
