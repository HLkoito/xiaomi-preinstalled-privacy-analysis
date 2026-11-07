'use strict';
// Rebuild the text-only research package from a local analysis directory.
// Usage: node tools/package_reports.cjs /path/to/_analysis
const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto');
const src=path.resolve(process.argv[2]||'.'),dst=path.resolve(__dirname,'..');
const sha=b=>crypto.createHash('sha256').update(b).digest('hex');
const normal=s=>s.replaceAll('\\','/');
const root=normal(src);
const forbidden=String.fromCharCode(104,111,110,101,108,105,110);
function scrub(t){
 t=t.replace(/（Asia\/Tokyo）|\(Asia\/Tokyo\)| Asia\/Tokyo/g,'');
 t=t.replaceAll(normal(path.dirname(src))+'/','').replaceAll(path.dirname(src)+'\\','');
 t=t.replaceAll(root+'/jadxout/sources/','evidence/sources/').replaceAll(root+'/apktool_sc/','evidence/manifest/');
 t=t.replaceAll(root+'/tools/','tools/').replaceAll(root+'/findings/','archival/');
 t=t.replace(/\b[A-Za-z]:[\\/][^\s`"'<>|)]+/g,'[LOCAL_PATH_REMOVED]');
 t=t.replace(/session-[0-9a-f-]{20,}/gi,'[SESSION_REMOVED]');
 t=t.replace(/[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}/gi,'[VENDOR_EMAIL_REMOVED]');
 t=t.replace(/(?<![\d.])(?:\d{1,3}\.){3}\d{1,3}(?![\d.])/g,value=>value.split('.').every(n=>Number(n)<=255)?'[ADDRESS_REMOVED]':value);
 t=t.replace(new RegExp(forbidden,'gi'),'[IDENTITY_REMOVED]');
 return t;
}
function write(rel,text){const p=path.join(dst,rel);fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,text,'utf8');}
const review=fs.readFileSync(path.join(src,'PRIVACY_REVIEW_20261106.md'),'utf8');
const entries=[];
const refRe=/^\[([^\]]+)\]: (.+?)(?::(\d+))?\s*$/gm;
let m;
while((m=refRe.exec(review))){
 const local=normal(m[2]);let rel;
 if(local.includes('/jadxout/sources/'))rel='evidence/sources/'+local.split('/jadxout/sources/')[1];
 else if(local.includes('/apktool_sc/'))rel='evidence/manifest/'+local.split('/apktool_sc/')[1];else continue;
 const sourceFile=path.join(src,local.includes('/jadxout/sources/')?'jadxout/sources':'apktool_sc',local.split(local.includes('/jadxout/sources/')?'/jadxout/sources/':'/apktool_sc/')[1]);
 const bytes=fs.readFileSync(sourceFile),clean=scrub(bytes.toString('utf8'));
 write(rel,clean);
 entries.push({reference:m[1],path:rel,line:m[3]?Number(m[3]):null,source_sha256:sha(bytes),published_sha256:sha(Buffer.from(clean)),line_count:clean.split(/\r?\n/).length});
}
// Include gate/source helpers hashed by the earlier review, not just linked files.
const prior=JSON.parse(fs.readFileSync(path.join(src,'findings/privacy-review-evidence-20261106.json'),'utf8'));
for(const item of prior.reviewed_source_hashes){
 const p=normal(item.path);const marker=p.includes('/jadxout/sources/')?'/jadxout/sources/':'/apktool_sc/';
 const suffix=p.split(marker)[1]; if(!suffix)continue;
 const rel=(marker.includes('sources')?'evidence/sources/':'evidence/manifest/')+suffix;
 if(entries.some(e=>e.path===rel))continue;
 const bytes=fs.readFileSync(path.join(src,marker.includes('sources')?'jadxout/sources':'apktool_sc',suffix));const clean=scrub(bytes.toString('utf8'));write(rel,clean);
 entries.push({reference:null,path:rel,line:null,source_sha256:sha(bytes),published_sha256:sha(Buffer.from(clean)),line_count:clean.split(/\r?\n/).length});
}
let cleanReview=review;
cleanReview=cleanReview.replace(/^\[([^\]]+)\]: (.+?)(?::(\d+))?\s*$/gm,(_,key,p,line)=>{const e=entries.find(x=>x.reference===key);return e?`[${key}]: ${e.path}${line?'#L'+line:''}`:scrub(_);});
cleanReview=scrub(cleanReview).replace(/\]\([^)]*privacy_review_verify\.cjs\)/g,'](tools/privacy_review_verify.cjs)').replace(/\]\([^)]*privacy-review-evidence-20261106\.json\)/g,'](evidence/verification.json)');
cleanReview=cleanReview.replace('本文件为补充复核，保留原报告与专项文件，不直接修改其内容。','本文件为补充复核的脱敏公开版；源码链接指向本目录随附的证据文件。原报告及专项存档中的相冲突结论，以本文件和 REPORT.md 的纠错为准。');
cleanReview=cleanReview.replace('此特定布局请求另构造复合设备指纹，不等同于随机会话 ID','此特定布局请求另构造复合设备指纹，不等同于随机会话 ID；所见静态字段只证明进程内缓存，不证明安装级生命周期');
write('REVIEW.md',cleanReview);
const banner='> **历史分析存档，非最终结论。** 本文保留原始分析的技术内容，可能包含尚未验证或已被纠正的表述。隐私数据上传范围、同意条件、FBO 本地委托、报警短信加密、Provider 鉴权等，以 [最终报告](FINAL_LINK) 与 [独立复核](REVIEW_LINK) 为准。文中源码定位保留为原反编译相对路径；只有 evidence/ 中列出的精选文件随包提供，未打包完整源码树。\n\n';
write('SOURCE_REPORT.md',banner.replace('FINAL_LINK','REPORT.md').replace('REVIEW_LINK','REVIEW.md')+scrub(fs.readFileSync(path.join(src,'REPORT_MIUISecurityCenter.md'),'utf8')));
for(const name of ['assets-data','core-security','manifest-sdk','native-libs','network-warning','power-gaming','privacy-permcenter']){
 write('archival/'+name+'.md',banner.replace('FINAL_LINK','../REPORT.md').replace('REVIEW_LINK','../REVIEW.md')+scrub(fs.readFileSync(path.join(src,'findings',name+'.md'),'utf8')));
}
const verification={...prior,date:'2026-11-06',apk:{filename:path.basename(prior.apk.path),size:prior.apk.size,sha256:prior.apk.sha256},reviewed_source_hashes:entries.map(e=>({path:e.path,sha256:e.source_sha256,published_sha256:e.published_sha256,lines:e.line_count})),publication_note:'Local identity and paths removed. This records prior static checks; no execution or traffic evidence.'};
write('evidence/verification.json',JSON.stringify(verification,null,2)+'\n');
write('evidence/index.json',JSON.stringify({method:'Selected complete decompiled Java and decoded XML, with original line numbers. Hash equality indicates unchanged bytes. Static evidence only.',references:entries},null,2)+'\n');
for(const name of ['a64scan.mjs','axml2xml.js','dex_str.js'])write('tools/'+name,scrub(fs.readFileSync(path.join(src,'tools',name),'utf8')));
let verifier=fs.readFileSync(path.join(src,'tools/privacy_review_verify.cjs'),'utf8');
verifier=verifier.replace(/^const root=.*$/m,"const root=path.resolve(process.argv[2]||'.'),a=path.join(root,'_analysis');");
verifier=verifier.replace("const out=a+'/findings/privacy-review-evidence-20261106.json';","const out=path.resolve(process.argv[3]||'verification-local.json');");
write('tools/privacy_review_verify.cjs',scrub(verifier));
console.log(JSON.stringify({references:entries.length,unique_evidence_files:new Set(entries.map(e=>e.path)).size,modified_evidence:entries.filter(e=>e.source_sha256!==e.published_sha256).length}));
