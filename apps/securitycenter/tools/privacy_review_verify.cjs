const fs=require('fs'),path=require('path'),crypto=require('crypto'),zlib=require('zlib');
const root=path.resolve(process.argv[2]||'.'),a=path.join(root,'_analysis');
const sha=b=>crypto.createHash('sha256').update(b).digest('hex');
const apk=root+'/MIUISecurityCenter-10.4.9-250402.0.1.apk',zip=fs.readFileSync(apk);
let eocd=zip.length-22;while(eocd>=Math.max(0,zip.length-65557)&&zip.readUInt32LE(eocd)!==0x06054b50)eocd--;if(eocd<0)throw Error('No ZIP EOCD');
const need=['https://adv.sec.miui.com/info/layout','https://flash.sec.miui.com','/user/ra','rivacy_risk_check','isPersonalizedAdEnabled','ror','rbr','getOAID','https://api.sec.miui.com/v1/traffic/report','https://api.miui.security.xiaomi.com/netassist/flow/queryflow','https://gateway-papa.cloud.weizhi.com/papa/upload','https://eps.caict.ac.cn/eps/collect','https://api.sec.miui.com/earthquake/volunteer/sensor/upload','FBO_new_trigger','KEY_SECURITY_CENTER_ALLOW_CONNECT_NETWORK'];
const found=Object.fromEntries(need.map(s=>[s,[]])),dex=[];
let cursor=zip.readUInt32LE(eocd+16),n=zip.readUInt16LE(eocd+10);
for(let i=0;i<n;i++){
 if(zip.readUInt32LE(cursor)!==0x02014b50)throw Error('Bad central directory');
 const method=zip.readUInt16LE(cursor+10),size=zip.readUInt32LE(cursor+20),nl=zip.readUInt16LE(cursor+28),el=zip.readUInt16LE(cursor+30),cl=zip.readUInt16LE(cursor+32),local=zip.readUInt32LE(cursor+42),name=zip.toString('utf8',cursor+46,cursor+46+nl);cursor+=46+nl+el+cl;
 if(!/^classes\d*\.dex$/.test(name))continue;
 const start=local+30+zip.readUInt16LE(local+26)+zip.readUInt16LE(local+28),packed=zip.subarray(start,start+size),b=method===8?zlib.inflateRawSync(packed):method===0?packed:null;
 if(!b)throw Error('Unsupported compression');
 const count=b.readUInt32LE(56),off=b.readUInt32LE(60),ss=new Set();
 for(let j=0;j<count;j++){let p=b.readUInt32LE(off+j*4);while(b[p++]&128){}const end=b.indexOf(0,p);ss.add(b.toString('utf8',p,end));}
 dex.push({name,size:b.length,sha256:sha(b),string_ids_size:count,extracted_copy_matches:sha(fs.readFileSync(a+'/apk_full/'+name))===sha(b)});
 for(const s of need)if(ss.has(s))found[s].push(name);
}
const files=['eg/j.java','eg/c.java','xe/a.java','q2/a.java','ef/w.java','y4/a.java','x4/l1.java','ef/x.java','x4/u1.java','ac/c.java','ub/d.java','xc/h.java','ta/d.java','com/miui/permcenter/detection/PrivacyRiskDetectionActivity.java','com/miui/networkassistant/webapi/WebApiAccessHelper.java','com/miui/networkassistant/utils/DeviceUtil.java','com/miui/networkassistant/service/TcSmsReportService.java','com/miui/networkassistant/ui/fragment/TcSmsReportFragment.java','com/miui/networkassistant/ui/base/TrafficRelatedPreFragment.java','com/miui/warningcenter/policeassist/PaService.java','com/miui/warningcenter/policeassist/PaUtils.java','com/miui/warningcenter/policeassist/PaCommon.java','com/miui/policeassist/EPSManager.java','com/miui/warningcenter/policeassist/PaSettingActivity.java','com/miui/earthquakewarning/service/EarthquakeMonitorDataManager.java','com/miui/earthquakewarning/service/EarthquakeMonitorUtils.java','com/miui/earthquakewarning/service/EarthquakeWarningService.java','com/miui/earthquakewarning/ui/EarthquakeWarningMonitorOpenFragment.java','com/miui/bubbles/services/BubblesNotificationListenerService.java','com/miui/bubbles/services/BubblesNotificationHelper.java','com/miui/gamebooster/gbservices/AntiMsgAccessibilityService.java','com/miui/analytics/StatManager.java','com/miui/securitycenter/provider/DeviceCredentialProvider.java','com/miui/permcenter/privacycenter/PrivacyCenterProvider.java','com/miui/privacyapps/PrivacyAppsProvider.java','com/miui/monthreport/c.java','ma/c.java'];
const hashFile=p=>{const b=fs.readFileSync(p);return {path:p,sha256:sha(b),lines:b.toString('utf8').split(/\r?\n/).length};};
const result={date:'2026-11-06',method:'Offline shell/Node verification, not REA Evidence. Existing decompiled sources audited; no target execution or traffic capture. ZIP/DEX strings corroborate membership only, not control flow or decompiler correctness.',apk:{path:apk,size:zip.length,sha256:sha(zip)},dex,string_presence:found,reviewed_source_hashes:[...files.map(f=>hashFile(a+'/jadxout/sources/'+f)),...['apktool_sc/AndroidManifest.xml','apktool_sc/res/xml/network_security_config.xml'].map(f=>hashFile(a+'/'+f))]};
const out=path.resolve(process.argv[3]||'verification-local.json');fs.writeFileSync(out,JSON.stringify(result,null,2));console.log(JSON.stringify({apk:result.apk,dex,source_files_hashed:result.reviewed_source_hashes.length,missing_strings:need.filter(s=>!found[s].length),evidence_file:out},null,2));
