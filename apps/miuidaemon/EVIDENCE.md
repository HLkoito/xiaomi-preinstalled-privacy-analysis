# Selected Static Evidence

These are excerpts from the decompiled sample identified in [REPORT.md](REPORT.md). Source paths are relative to a decompilation root, not an analyst workstation. Each numbered line retains its original source line number. Decompiled syntax and comments are tool output; excerpts may not compile. Gaps between ranges are intentional. Embedded credential/key values and personal contact details are omitted where encountered. No runtime output is included.

## D01

Source: `sources/com/miui/daemon/BaseDaemonApplication.java`

Source-file SHA-256: `2f8bbb593b45eb6153fe6863201d45ac461fdc2dd040252b723c4e675afb2e93`

Original lines 1–40:

```text
0001 | package com.miui.daemon;
0002 | 
0003 | import android.app.Application;
0004 | import android.os.ServiceManager;
0005 | import com.android.internal.app.IPerfShielder;
0006 | import com.mi.mibridge.DeviceLevel;
0007 | import com.miui.daemon.fileobserver.FileObserverApp;
0008 | import com.miui.daemon.mqsas.MQSApp;
0009 | import com.miui.daemon.mqsas.MQSService;
0010 | import com.miui.daemon.performance.onetrack.TrackAnalytics;
0011 | import com.miui.daemon.performance.onetrack.memory.AppMemoryTrack;
0012 | import com.miui.daemon.performance.statistics.utils.DeviceUtils;
0013 | 
0014 | /* JADX INFO: loaded from: classes.dex */
0015 | public class BaseDaemonApplication extends Application {
0016 |     public static MQSService mMQSService;
0017 |     public static IPerfShielder mPf;
0018 | 
0019 |     @Override // android.app.Application
0020 |     public void onCreate() {
0021 |         super.onCreate();
0022 |         init();
0023 |     }
0024 | 
0025 |     public final void init() {
0026 |         MQSApp.init(this);
0027 |         DeviceUtils.initDeviceDetails(getApplicationContext());
0028 |         DeviceLevel.initDeviceLevel();
0029 |         FileObserverApp.init(this);
0030 |         TrackAnalytics.init(getApplicationContext());
0031 |         AppMemoryTrack.init(this);
0032 |     }
0033 | 
0034 |     public static IPerfShielder getIPerfShielder() {
0035 |         if (mPf == null) {
0036 |             mPf = IPerfShielder.Stub.asInterface(ServiceManager.getService("perfshielder"));
0037 |         }
0038 |         return mPf;
0039 |     }
0040 | }
```

## D02

Source: `sources/com/miui/daemon/performance/onetrack/memory/AppMemoryTrack.java`

Source-file SHA-256: `0c8ba0621f9ca58b7db1d5dd56c35a7c2e8b7d3972a53e265e8b6fde293c664a`

Original lines 1–309:

```text
0001 | package com.miui.daemon.performance.onetrack.memory;
0002 | 
0003 | import android.app.ActivityManager;
0004 | import android.app.Application;
0005 | import android.content.ComponentCallbacks;
0006 | import android.content.Context;
0007 | import android.content.res.Configuration;
0008 | import android.os.Debug;
0009 | import android.os.Handler;
0010 | import android.os.Looper;
0011 | import android.os.SystemClock;
0012 | import com.miui.daemon.mqsas.utils.OnetrackUtils;
0013 | import com.miui.daemon.mqsas.utils.ThreadPool;
0014 | import java.util.ArrayList;
0015 | import java.util.Comparator;
0016 | import java.util.LinkedHashMap;
0017 | import java.util.List;
0018 | import java.util.concurrent.ConcurrentHashMap;
0019 | import java.util.function.BiConsumer;
0020 | import kotlin.Pair;
0021 | import kotlin.Triple;
0022 | import kotlin.TuplesKt;
0023 | import kotlin.collections.ArraysKt___ArraysKt;
0024 | import kotlin.collections.CollectionsKt__IterablesKt;
0025 | import kotlin.collections.CollectionsKt___CollectionsKt;
0026 | import kotlin.collections.MapsKt__MapsJVMKt;
0027 | import kotlin.comparisons.ComparisonsKt__ComparisonsKt;
0028 | import kotlin.jvm.internal.Intrinsics;
0029 | import kotlin.ranges.RangesKt___RangesKt;
0030 | import miui.os.Build;
0031 | import org.json.JSONException;
0032 | import org.json.JSONObject;
0033 | 
0034 | /* JADX INFO: compiled from: AppMemoryTrack.kt */
0035 | /* JADX INFO: loaded from: classes.dex */
0036 | public final class AppMemoryTrack implements ComponentCallbacks, Runnable {
0037 |     public static Context appContext;
0038 |     public static int deviceMemory;
0039 |     public static long lastTrackMillis;
0040 |     public static final AppMemoryTrack INSTANCE = new AppMemoryTrack();
0041 |     public static final Handler handler = new Handler(Looper.getMainLooper());
0042 |     public static final ConcurrentHashMap<String, AppMemory> appMemoryRecords = new ConcurrentHashMap<>();
0043 | 
0044 |     @Override // android.content.ComponentCallbacks
0045 |     public void onConfigurationChanged(Configuration newConfig) {
0046 |         Intrinsics.checkNotNullParameter(newConfig, "newConfig");
0047 |     }
0048 | 
0049 |     public static final void init(Application app2) {
0050 |         Intrinsics.checkNotNullParameter(app2, "app");
0051 |         if (Build.IS_DEBUGGABLE) {
0052 |             return;
0053 |         }
0054 |         Context applicationContext = app2.getApplicationContext();
0055 |         Intrinsics.checkNotNullExpressionValue(applicationContext, "app.applicationContext");
0056 |         appContext = applicationContext;
0057 |         AppMemoryTrack appMemoryTrack = INSTANCE;
0058 |         app2.registerComponentCallbacks(appMemoryTrack);
0059 |         handler.postDelayed(appMemoryTrack, 3600000L);
0060 |     }
0061 | 
0062 |     @Override // java.lang.Runnable
0063 |     public void run() {
0064 |         recordAppMemory();
0065 |         handler.postDelayed(this, 3600000L);
0066 |     }
0067 | 
0068 |     @Override // android.content.ComponentCallbacks
0069 |     public void onLowMemory() {
0070 |         if (Build.IS_DEBUGGABLE) {
0071 |             return;
0072 |         }
0073 |         long jElapsedRealtime = SystemClock.elapsedRealtime();
0074 |         if (jElapsedRealtime - lastTrackMillis < 7200000) {
0075 |             return;
0076 |         }
0077 |         lastTrackMillis = jElapsedRealtime;
0078 |         ThreadPool.execute(new Runnable() { // from class: com.miui.daemon.performance.onetrack.memory.AppMemoryTrack.onLowMemory.1
0079 |             @Override // java.lang.Runnable
0080 |             public final void run() {
0081 |                 Context context = AppMemoryTrack.appContext;
0082 |                 LinkedHashMap linkedHashMap = null;
0083 |                 if (context == null) {
0084 |                     Intrinsics.throwUninitializedPropertyAccessException("appContext");
0085 |                     context = null;
0086 |                 }
0087 |                 Object systemService = context.getSystemService("activity");
0088 |                 Intrinsics.checkNotNull(systemService, "null cannot be cast to non-null type android.app.ActivityManager");
0089 |                 ActivityManager activityManager = (ActivityManager) systemService;
0090 |                 List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = activityManager.getRunningAppProcesses();
0091 |                 if (runningAppProcesses != null) {
0092 |                     LinkedHashMap linkedHashMap2 = new LinkedHashMap(RangesKt___RangesKt.coerceAtLeast(MapsKt__MapsJVMKt.mapCapacity(CollectionsKt__IterablesKt.collectionSizeOrDefault(runningAppProcesses, 10)), 16));
0093 |                     for (ActivityManager.RunningAppProcessInfo runningAppProcessInfo : runningAppProcesses) {
0094 |                         Pair pair = TuplesKt.to(Integer.valueOf(runningAppProcessInfo.pid), runningAppProcessInfo.processName);
0095 |                         linkedHashMap2.put(pair.getFirst(), pair.getSecond());
0096 |                     }
0097 |                     linkedHashMap = linkedHashMap2;
0098 |                 }
0099 |                 int i = 0;
0100 |                 if (linkedHashMap == null || linkedHashMap.isEmpty()) {
0101 |                     return;
0102 |                 }
0103 |                 int[] intArray = ArraysKt___ArraysKt.toIntArray((Integer[]) linkedHashMap.keySet().toArray(new Integer[0]));
0104 |                 ArrayList arrayList = new ArrayList();
0105 |                 Debug.MemoryInfo[] processMemoryInfo = activityManager.getProcessMemoryInfo(intArray);
0106 |                 Intrinsics.checkNotNullExpressionValue(processMemoryInfo, "ams.getProcessMemoryInfo(pidArray)");
0107 |                 ArrayList arrayList2 = new ArrayList(processMemoryInfo.length);
0108 |                 int length = processMemoryInfo.length;
0109 |                 int i2 = 0;
0110 |                 while (i < length) {
0111 |                     arrayList2.add(TuplesKt.to(Integer.valueOf(intArray[i2]), processMemoryInfo[i]));
0112 |                     i++;
0113 |                     i2++;
0114 |                 }
0115 |                 for (Pair pair2 : CollectionsKt___CollectionsKt.take(CollectionsKt___CollectionsKt.sortedWith(arrayList2, new Comparator() { // from class: com.miui.daemon.performance.onetrack.memory.AppMemoryTrack$onLowMemory$1$run$$inlined$sortedByDescending$1
0116 |                     /* JADX WARN: Multi-variable type inference failed */
0117 |                     @Override // java.util.Comparator
0118 |                     public final int compare(T t, T t2) {
0119 |                         return ComparisonsKt__ComparisonsKt.compareValues(Integer.valueOf(((Debug.MemoryInfo) ((Pair) t2).getSecond()).getSummaryTotalPss()), Integer.valueOf(((Debug.MemoryInfo) ((Pair) t).getSecond()).getSummaryTotalPss()));
0120 |                     }
0121 |                 }), 10)) {
0122 |                     AppMemoryTrack appMemoryTrack = AppMemoryTrack.INSTANCE;
0123 |                     Object obj = linkedHashMap.get(pair2.getFirst());
0124 |                     Intrinsics.checkNotNull(obj);
0125 |                     Object second = pair2.getSecond();
0126 |                     Intrinsics.checkNotNullExpressionValue(second, "it.second");
0127 |                     arrayList.add(appMemoryTrack.generateLowMemoryEventData((String) obj, (Debug.MemoryInfo) second));
0128 |                 }
0129 |                 OnetrackUtils.trackEvents(arrayList);
0130 |             }
0131 |         });
0132 |     }
0133 | 
0134 |     public final void recordAppMemory() {
0135 |         ThreadPool.execute(new Runnable() { // from class: com.miui.daemon.performance.onetrack.memory.AppMemoryTrack.recordAppMemory.1
0136 |             @Override // java.lang.Runnable
0137 |             public final void run() {
0138 |                 Object objPutIfAbsent;
0139 |                 Context context = AppMemoryTrack.appContext;
0140 |                 LinkedHashMap linkedHashMap = null;
0141 |                 if (context == null) {
0142 |                     Intrinsics.throwUninitializedPropertyAccessException("appContext");
0143 |                     context = null;
0144 |                 }
0145 |                 Object systemService = context.getSystemService("activity");
0146 |                 Intrinsics.checkNotNull(systemService, "null cannot be cast to non-null type android.app.ActivityManager");
0147 |                 ActivityManager activityManager = (ActivityManager) systemService;
0148 |                 List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = activityManager.getRunningAppProcesses();
0149 |                 if (runningAppProcesses != null) {
0150 |                     LinkedHashMap linkedHashMap2 = new LinkedHashMap(RangesKt___RangesKt.coerceAtLeast(MapsKt__MapsJVMKt.mapCapacity(CollectionsKt__IterablesKt.collectionSizeOrDefault(runningAppProcesses, 10)), 16));
0151 |                     for (ActivityManager.RunningAppProcessInfo runningAppProcessInfo : runningAppProcesses) {
0152 |                         Pair pair = TuplesKt.to(Integer.valueOf(runningAppProcessInfo.pid), runningAppProcessInfo.processName);
0153 |                         linkedHashMap2.put(pair.getFirst(), pair.getSecond());
0154 |                     }
0155 |                     linkedHashMap = linkedHashMap2;
0156 |                 }
0157 |                 if (linkedHashMap == null || linkedHashMap.isEmpty()) {
0158 |                     return;
0159 |                 }
0160 |                 int[] intArray = ArraysKt___ArraysKt.toIntArray((Integer[]) linkedHashMap.keySet().toArray(new Integer[0]));
0161 |                 Debug.MemoryInfo[] processMemoryInfo = activityManager.getProcessMemoryInfo(intArray);
0162 |                 Intrinsics.checkNotNullExpressionValue(processMemoryInfo, "ams.getProcessMemoryInfo(pidArray)");
0163 |                 ArrayList arrayList = new ArrayList(processMemoryInfo.length);
0164 |                 int length = processMemoryInfo.length;
0165 |                 int i = 0;
0166 |                 int i2 = 0;
0167 |                 while (i < length) {
0168 |                     arrayList.add(TuplesKt.to(Integer.valueOf(intArray[i2]), processMemoryInfo[i]));
0169 |                     i++;
0170 |                     i2++;
0171 |                 }
0172 |                 ArrayList<Pair> arrayList2 = new ArrayList();
0173 |                 for (Object obj : arrayList) {
0174 |                     if (AppMemoryTrack.INSTANCE.calcTotalPssPermil(((Debug.MemoryInfo) ((Pair) obj).getSecond()).getSummaryTotalPss() >> 10) > 5) {
0175 |                         arrayList2.add(obj);
0176 |                     }
0177 |                 }
0178 |                 for (Pair pair2 : arrayList2) {
0179 |                     ConcurrentHashMap concurrentHashMap = AppMemoryTrack.appMemoryRecords;
0180 |                     Object obj2 = linkedHashMap.get(pair2.getFirst());
0181 |                     Intrinsics.checkNotNull(obj2);
0182 |                     Object appMemory = concurrentHashMap.get(obj2);
0183 |                     if (appMemory == null && (objPutIfAbsent = concurrentHashMap.putIfAbsent(obj2, (appMemory = new AppMemory(null, null, null, null, 15, null)))) != null) {
0184 |                         appMemory = objPutIfAbsent;
0185 |                     }
0186 |                     AppMemory appMemory2 = (AppMemory) appMemory;
0187 |                     appMemory2.getJavaHeap().add(((Debug.MemoryInfo) pair2.getSecond()).getSummaryJavaHeap() >> 10);
0188 |                     appMemory2.getNativeHeap().add(((Debug.MemoryInfo) pair2.getSecond()).getSummaryNativeHeap() >> 10);
0189 |                     appMemory2.getSwappedOut().add(((Debug.MemoryInfo) pair2.getSecond()).getSummaryTotalSwapPss() >> 10);
0190 |                     appMemory2.getTotalPss().add(((Debug.MemoryInfo) pair2.getSecond()).getSummaryTotalPss() >> 10);
0191 |                 }
0192 |             }
0193 |         });
0194 |     }
0195 | 
0196 |     public final String retrieveAppMemory(String processName) {
0197 |         String string;
0198 |         Intrinsics.checkNotNullParameter(processName, "processName");
0199 |         AppMemory appMemory = appMemoryRecords.get(processName);
0200 |         return (appMemory == null || (string = appMemory.toString()) == null) ? "no memory data" : string;
0201 |     }
0202 | 
0203 |     public final void requestUpload() {
0204 |         if (Build.IS_DEBUGGABLE) {
0205 |             return;
0206 |         }
0207 |         final ArrayList arrayList = new ArrayList();
0208 |         ConcurrentHashMap<String, AppMemory> concurrentHashMap = appMemoryRecords;
0209 |         concurrentHashMap.forEach(new BiConsumer() { // from class: com.miui.daemon.performance.onetrack.memory.AppMemoryTrack.requestUpload.1
0210 |             @Override // java.util.function.BiConsumer
0211 |             public final void accept(String processName, AppMemory appMemory) {
0212 |                 Intrinsics.checkNotNullParameter(processName, "processName");
0213 |                 Intrinsics.checkNotNullParameter(appMemory, "appMemory");
0214 |                 arrayList.add(AppMemoryTrack.INSTANCE.generateAppMemoryEventData(processName, appMemory));
0215 |             }
0216 |         });
0217 |         concurrentHashMap.clear();
0218 |         OnetrackUtils.trackEvents(arrayList);
0219 |     }
0220 | 
0221 |     public final int calcTotalPssPermil(int i) {
0222 |         if (deviceMemory == 0) {
0223 |             Context context = appContext;
0224 |             if (context == null) {
0225 |                 Intrinsics.throwUninitializedPropertyAccessException("appContext");
0226 |                 context = null;
0227 |             }
0228 |             Object systemService = context.getSystemService("activity");
0229 |             Intrinsics.checkNotNull(systemService, "null cannot be cast to non-null type android.app.ActivityManager");
0230 |             ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
0231 |             ((ActivityManager) systemService).getMemoryInfo(memoryInfo);
0232 |             deviceMemory = (int) (memoryInfo.totalMem >> 20);
0233 |         }
0234 |         return (i * 1000) / deviceMemory;
0235 |     }
0236 | 
0237 |     public final Triple<Integer, Integer, Integer> calcMinMaxAvg(int[] iArr) {
0238 |         if (iArr.length == 0) {
0239 |             return null;
0240 |         }
0241 |         int i = iArr[0];
0242 |         int length = iArr.length;
0243 |         int i2 = i;
0244 |         int i3 = i2;
0245 |         for (int i4 = 1; i4 < length; i4++) {
0246 |             int i5 = iArr[i4];
0247 |             if (i5 < i) {
0248 |                 i = i5;
0249 |             }
0250 |             if (i5 > i3) {
0251 |                 i3 = i5;
0252 |             }
0253 |             i2 += i5;
0254 |         }
0255 |         return new Triple<>(Integer.valueOf(i), Integer.valueOf(i3), Integer.valueOf(i2 / iArr.length));
0256 |     }
0257 | 
0258 |     public final JSONObject generateAppMemoryEventData(String str, AppMemory appMemory) throws JSONException {
0259 |         JSONObject json = OnetrackUtils.obtainEventData("app_memory");
0260 |         json.put("package", str);
0261 |         int[] array = appMemory.getJavaHeap().toArray();
0262 |         Intrinsics.checkNotNullExpressionValue(array, "appMemory.javaHeap.toArray()");
0263 |         Triple<Integer, Integer, Integer> tripleCalcMinMaxAvg = calcMinMaxAvg(array);
0264 |         if (tripleCalcMinMaxAvg != null) {
0265 |             json.put("min_java_heap", tripleCalcMinMaxAvg.getFirst().intValue());
0266 |             json.put("max_java_heap", tripleCalcMinMaxAvg.getSecond().intValue());
0267 |             json.put("avg_java_heap", tripleCalcMinMaxAvg.getThird().intValue());
0268 |         }
0269 |         int[] array2 = appMemory.getNativeHeap().toArray();
0270 |         Intrinsics.checkNotNullExpressionValue(array2, "appMemory.nativeHeap.toArray()");
0271 |         Triple<Integer, Integer, Integer> tripleCalcMinMaxAvg2 = calcMinMaxAvg(array2);
0272 |         if (tripleCalcMinMaxAvg2 != null) {
0273 |             json.put("min_native_heap", tripleCalcMinMaxAvg2.getFirst().intValue());
0274 |             json.put("max_native_heap", tripleCalcMinMaxAvg2.getSecond().intValue());
0275 |             json.put("avg_native_heap", tripleCalcMinMaxAvg2.getThird().intValue());
0276 |         }
0277 |         int[] array3 = appMemory.getSwappedOut().toArray();
0278 |         Intrinsics.checkNotNullExpressionValue(array3, "appMemory.swappedOut.toArray()");
0279 |         Triple<Integer, Integer, Integer> tripleCalcMinMaxAvg3 = calcMinMaxAvg(array3);
0280 |         if (tripleCalcMinMaxAvg3 != null) {
0281 |             json.put("min_swapped_out", tripleCalcMinMaxAvg3.getFirst().intValue());
0282 |             json.put("max_swapped_out", tripleCalcMinMaxAvg3.getSecond().intValue());
0283 |             json.put("avg_swapped_out", tripleCalcMinMaxAvg3.getThird().intValue());
0284 |         }
0285 |         int[] array4 = appMemory.getTotalPss().toArray();
0286 |         Intrinsics.checkNotNullExpressionValue(array4, "appMemory.totalPss.toArray()");
0287 |         Triple<Integer, Integer, Integer> tripleCalcMinMaxAvg4 = calcMinMaxAvg(array4);
0288 |         if (tripleCalcMinMaxAvg4 != null) {
0289 |             json.put("min_total_pss", tripleCalcMinMaxAvg4.getFirst().intValue());
0290 |             json.put("max_total_pss", tripleCalcMinMaxAvg4.getSecond().intValue());
0291 |             json.put("avg_total_pss", tripleCalcMinMaxAvg4.getThird().intValue());
0292 |             json.put("total_pss_permil", INSTANCE.calcTotalPssPermil(tripleCalcMinMaxAvg4.getThird().intValue()));
0293 |         }
0294 |         Intrinsics.checkNotNullExpressionValue(json, "json");
0295 |         return json;
0296 |     }
0297 | 
0298 |     public final JSONObject generateLowMemoryEventData(String str, Debug.MemoryInfo memoryInfo) throws JSONException {
0299 |         JSONObject json = OnetrackUtils.obtainEventData("low_memory");
0300 |         json.put("package", str);
0301 |         json.put("java_heap", memoryInfo.getSummaryJavaHeap() >> 10);
0302 |         json.put("native_heap", memoryInfo.getSummaryNativeHeap() >> 10);
0303 |         json.put("swapped_out", memoryInfo.getSummaryTotalSwapPss() >> 10);
0304 |         json.put("total_pss", memoryInfo.getSummaryTotalPss() >> 10);
0305 |         json.put("total_pss_permil", calcTotalPssPermil(memoryInfo.getSummaryTotalPss() >> 10));
0306 |         Intrinsics.checkNotNullExpressionValue(json, "json");
0307 |         return json;
0308 |     }
0309 | }
```

## D03

Source: `sources/com/miui/daemon/mqsas/jobs/EventUploadService.java`

Source-file SHA-256: `d5dcd3dec5986d1d961b6dea98e7c9ba5de2490a6625ab4a70df6f09e93331fc`

Original lines 1–90:

```text
0001 | package com.miui.daemon.mqsas.jobs;
0002 | 
0003 | import android.app.job.JobParameters;
0004 | import android.app.job.JobService;
0005 | import android.content.Context;
0006 | import com.miui.daemon.mqsas.event.AnrCluesManager;
0007 | import com.miui.daemon.mqsas.event.HighMemoryManager;
0008 | import com.miui.daemon.mqsas.event.XmsEventManager;
0009 | import com.miui.daemon.mqsas.upload.BiometricInfoUploader;
0010 | import com.miui.daemon.mqsas.upload.CamErrInfoOnetrack;
0011 | import com.miui.daemon.mqsas.upload.ExceptionUploader;
0012 | import com.miui.daemon.mqsas.upload.ExceptionUploaderV2;
0013 | import com.miui.daemon.mqsas.upload.FileEventUploader;
0014 | import com.miui.daemon.mqsas.upload.FileUploader;
0015 | import com.miui.daemon.mqsas.upload.FingerprintUploader;
0016 | import com.miui.daemon.mqsas.upload.HWInfoUploaderV2;
0017 | import com.miui.daemon.mqsas.upload.HardwareUploader;
0018 | import com.miui.daemon.mqsas.upload.HeartBeatUploader;
0019 | import com.miui.daemon.mqsas.upload.PerfEventUploader;
0020 | import com.miui.daemon.mqsas.upload.ScreenOnUploader;
0021 | import com.miui.daemon.mqsas.upload.StorageUploadOnetrack;
0022 | import com.miui.daemon.mqsas.upload.StorageUploader;
0023 | import com.miui.daemon.mqsas.upload.WcnsUploader;
0024 | import com.miui.daemon.mqsas.utils.DeviceUtil;
0025 | import com.miui.daemon.mqsas.utils.ThreadPool;
0026 | import com.miui.daemon.mqsas.utils.Utils;
0027 | import com.miui.daemon.mqsas.utils.ffu.FFUManager;
0028 | import com.miui.daemon.performance.onetrack.memory.AppMemoryTrack;
0029 | 
0030 | /* JADX INFO: loaded from: classes.dex */
0031 | public class EventUploadService extends JobService {
0032 |     public static boolean DEBUG = false;
0033 | 
0034 |     @Override // android.app.job.JobService
0035 |     public boolean onStartJob(final JobParameters jobParameters) {
0036 |         if (Utils.isLibraryTest()) {
0037 |             return false;
0038 |         }
0039 |         Utils.logD("EventUploadService", "EventUploadService onStartJob", Boolean.TRUE);
0040 |         ThreadPool.execute(new Runnable() { // from class: com.miui.daemon.mqsas.jobs.EventUploadService.1
0041 |             @Override // java.lang.Runnable
0042 |             public void run() {
0043 |                 String name = Thread.currentThread().getName();
0044 |                 Thread.currentThread().setName("EventUpload");
0045 |                 try {
0046 |                     Context applicationContext = EventUploadService.this.getApplicationContext();
0047 |                     if (Utils.isUploadStorageEnabled(applicationContext) && Utils.needUploadMI_IC(applicationContext)) {
0048 |                         StorageUploadOnetrack.getInstance(applicationContext).requestUpload();
0049 |                     }
0050 |                     if (Utils.isUploadEnabled(applicationContext)) {
0051 |                         ExceptionUploader.getInstance(applicationContext).requestUpload();
0052 |                         HeartBeatUploader.getInstance(applicationContext).uploadScreenonData();
0053 |                         PerfEventUploader.getInstance(applicationContext).uploadPerfEvent();
0054 |                         if (!Utils.IS_INTERNATIONAL_BUILD) {
0055 |                             FingerprintUploader.getInstance(applicationContext).requestUpload();
0056 |                             WcnsUploader.getInstance(applicationContext).requestUpload();
0057 |                             HardwareUploader.getInstance(applicationContext).requestUpload();
0058 |                             HWInfoUploaderV2.getInstance(applicationContext).requestUpload();
0059 |                             FileUploader.uploadFileUnChecked(applicationContext);
0060 |                             ScreenOnUploader.requestUpload(applicationContext);
0061 |                             StorageUploader.getInstance(applicationContext).requestUpload();
0062 |                             BiometricInfoUploader.getInstance(applicationContext).requestUpload();
0063 |                             FileEventUploader.getInstance(applicationContext).requestUpload();
0064 |                             if (DeviceUtil.isUnReleased()) {
0065 |                                 ExceptionUploaderV2.getInstance(EventUploadService.this.getApplicationContext()).requestUpload();
0066 |                             }
0067 |                             XmsEventManager.getInstance(applicationContext).storeXmsEvents();
0068 |                             CamErrInfoOnetrack.getInstance(applicationContext).requestUpload();
0069 |                             FFUManager.getInstance(applicationContext).checkUpgrade();
0070 |                             AppMemoryTrack.INSTANCE.requestUpload();
0071 |                             HighMemoryManager.INSTANCE.cleanupHprofFiles();
0072 |                             AnrCluesManager.INSTANCE.cleanExpiredClues();
0073 |                         }
0074 |                     }
0075 |                 } finally {
0076 |                     EventUploadService.this.jobFinished(jobParameters, false);
0077 |                     Thread.currentThread().setName(name);
0078 |                 }
0079 |             }
0080 |         });
0081 |         Utils.logD("EventUploadService", "Finish upload...");
0082 |         return true;
0083 |     }
0084 | 
0085 |     @Override // android.app.job.JobService
0086 |     public boolean onStopJob(JobParameters jobParameters) {
0087 |         Utils.logD("EventUploadService", "EventUploadService onStopJob");
0088 |         return false;
0089 |     }
0090 | }
```

## D04

Source: `sources/com/miui/daemon/performance/statistics/activitystats/ActivityUtils.java`

Source-file SHA-256: `1af8d69044d1f1cf90a4f672ad600cdf315cab0ec6019e7b5aca4beee73a02bc`

Original lines 1–177:

```text
0001 | package com.miui.daemon.performance.statistics.activitystats;
0002 | 
0003 | import android.content.Context;
0004 | import android.os.Bundle;
0005 | import android.text.TextUtils;
0006 | import android.util.Log;
0007 | import com.miui.daemon.performance.statistics.network.DataUploader;
0008 | import com.miui.daemon.performance.statistics.network.Event;
0009 | import com.miui.daemon.performance.statistics.notifications.NotificationUtils;
0010 | import com.miui.daemon.performance.statistics.utils.ModuleUtils;
0011 | import java.io.File;
0012 | import java.io.Serializable;
0013 | import java.util.ArrayList;
0014 | import java.util.HashMap;
0015 | import java.util.Iterator;
0016 | import java.util.List;
0017 | import org.json.JSONException;
0018 | import org.json.JSONObject;
0019 | 
0020 | /* JADX INFO: loaded from: classes.dex */
0021 | public class ActivityUtils extends ModuleUtils {
0022 |     public static ActivityUtils sModule;
0023 |     public HashMap<String, HashMap<String, ActivityInfo>> mActivities;
0024 |     public final Object mLock;
0025 | 
0026 |     public static class ActivityInfo implements Serializable {
0027 |         private static final long serialVersionUID = 1;
0028 |         final String componentName;
0029 |         final String packageName;
0030 |         final String packageVersion;
0031 |         long launchTimes = 0;
0032 |         long avgLaunchTimeInMillis = 0;
0033 | 
0034 |         public ActivityInfo(String str, String str2, String str3) {
0035 |             this.packageName = str;
0036 |             this.packageVersion = str2;
0037 |             this.componentName = str3;
0038 |         }
0039 | 
0040 |         public void addLaunchTime(long j, long j2) {
0041 |             long j3 = this.launchTimes;
0042 |             this.avgLaunchTimeInMillis = (((this.avgLaunchTimeInMillis * j3) + j2) - j) / (j3 + 1);
0043 |             this.launchTimes = j3 + 1;
0044 |         }
0045 | 
0046 |         public String toString() {
0047 |             return "" + this.packageName + "-" + this.componentName + "-" + this.launchTimes + "-" + this.avgLaunchTimeInMillis;
0048 |         }
0049 | 
0050 |         public JSONObject toJson() {
0051 |             JSONObject jSONObject = new JSONObject();
0052 |             try {
0053 |                 jSONObject.put("activity", this.componentName);
0054 |                 jSONObject.put("launch_times", this.launchTimes);
0055 |                 jSONObject.put("avg_launch_time", this.avgLaunchTimeInMillis);
0056 |             } catch (JSONException unused) {
0057 |             }
0058 |             return jSONObject;
0059 |         }
0060 |     }
0061 | 
0062 |     public static ActivityUtils getInstance() {
0063 |         if (sModule == null) {
0064 |             sModule = new ActivityUtils("activitystats", "activity_stats", "activity");
0065 |         }
0066 |         return sModule;
0067 |     }
0068 | 
0069 |     public ActivityUtils(String str, String str2, String str3) {
0070 |         super(str, str2, str3);
0071 |         this.mLock = new Object();
0072 |         this.mActivities = new HashMap<>();
0073 |     }
0074 | 
0075 |     @Override // com.miui.daemon.performance.statistics.utils.ModuleUtils
0076 |     public List<Event> getEvents(Context context) {
0077 |         HashMap map;
0078 |         ArrayList arrayList = new ArrayList();
0079 |         File latestFile = ModuleUtils.getLatestFile(context, "activity_stats", "activity");
0080 |         if (latestFile == null) {
0081 |             return arrayList;
0082 |         }
0083 |         try {
0084 |             map = (HashMap) ModuleUtils.readFromFile(latestFile);
0085 |         } catch (Throwable unused) {
0086 |             map = null;
0087 |         }
0088 |         if (map != null) {
0089 |             Iterator it = map.values().iterator();
0090 |             while (it.hasNext()) {
0091 |                 for (ActivityInfo activityInfo : ((HashMap) it.next()).values()) {
0092 |                     Event event = new Event();
0093 |                     event.setPkn(activityInfo.packageName);
0094 |                     event.setPid(1000);
0095 |                     event.setTs("" + System.currentTimeMillis());
0096 |                     event.setPrn(TextUtils.isEmpty(activityInfo.packageVersion) ? "Unknown" : activityInfo.packageVersion);
0097 |                     event.setLog(activityInfo.toJson().toString());
0098 |                     event.setDet("performance/activitystats");
0099 |                     event.setSum("Performance target");
0100 |                     event.setTy("perf_activitystats");
0101 |                     event.setKw("Performance");
0102 |                     event.setDgt(DataUploader.getMd5Digest(event.getTs() + event.getDet()));
0103 |                     arrayList.add(event);
0104 |                 }
0105 |             }
0106 |         }
0107 |         return arrayList;
0108 |     }
0109 | 
0110 |     public void interceptAndQueuing(Context context, List<Bundle> list) {
0111 |         File externalFilesDir;
0112 |         HashMap<String, HashMap<String, ActivityInfo>> map;
0113 |         Iterator<Bundle> it = list.iterator();
0114 |         while (it.hasNext()) {
0115 |             ActivityLaunchRecord activityLaunchRecord = new ActivityLaunchRecord(it.next());
0116 |             if (activityLaunchRecord.getPackageName() != null) {
0117 |                 synchronized (this.mLock) {
0118 |                     if (this.mActivities.size() == 0 && (externalFilesDir = context.getExternalFilesDir("activity_stats")) != null) {
0119 |                         File file = new File(externalFilesDir.getAbsolutePath() + File.separator + ModuleUtils.generateNameOfDay("activity"));
0120 |                         if (file.exists()) {
0121 |                             try {
0122 |                                 map = (HashMap) ModuleUtils.readFromFile(file);
0123 |                             } catch (Throwable unused) {
0124 |                                 map = null;
0125 |                             }
0126 |                             if (map != null) {
0127 |                                 this.mActivities = map;
0128 |                             } else {
0129 |                                 Log.d("MiuiPerfServiceClient", "Failure in restoring legacy activity stats records.");
0130 |                             }
0131 |                         }
0132 |                     }
0133 |                     if (activityLaunchRecord.getEndTs() - activityLaunchRecord.getStartTs() < 10000) {
0134 |                         addActivityLaunchTime(activityLaunchRecord);
0135 |                         NotificationUtils.getInstance().addActivityLaunch(activityLaunchRecord);
0136 |                     }
0137 |                 }
0138 |             }
0139 |         }
0140 |     }
0141 | 
0142 |     public final void addActivityLaunchTime(ActivityLaunchRecord activityLaunchRecord) {
0143 |         HashMap<String, ActivityInfo> map = this.mActivities.get(activityLaunchRecord.getPackageName());
0144 |         if (map == null) {
0145 |             map = new HashMap<>();
0146 |             this.mActivities.put(activityLaunchRecord.getPackageName(), map);
0147 |         }
0148 |         ActivityInfo activityInfo = map.get(activityLaunchRecord.getComponentName());
0149 |         if (activityInfo == null) {
0150 |             activityInfo = new ActivityInfo(activityLaunchRecord.getPackageName(), activityLaunchRecord.getPackageVersion(), activityLaunchRecord.getComponentName());
0151 |             map.put(activityLaunchRecord.getComponentName(), activityInfo);
0152 |         }
0153 |         activityInfo.addLaunchTime(activityLaunchRecord.getStartTs(), activityLaunchRecord.getEndTs());
0154 |     }
0155 | 
0156 |     public void flushAndSave(Context context) {
0157 |         File externalFilesDir = context.getExternalFilesDir("activity_stats");
0158 |         if (externalFilesDir != null) {
0159 |             File file = new File(externalFilesDir.getAbsolutePath() + File.separator + ModuleUtils.generateNameOfDay("activity"));
0160 |             synchronized (this.mLock) {
0161 |                 if (!file.exists()) {
0162 |                     clear();
0163 |                     ModuleUtils.writeToFile(this.mActivities, context, "activity_stats", "activity");
0164 |                 }
0165 |                 if (this.mActivities.size() > 0) {
0166 |                     ModuleUtils.writeToFile(this.mActivities, context, "activity_stats", "activity");
0167 |                 }
0168 |             }
0169 |         }
0170 |     }
0171 | 
0172 |     public void clear() {
0173 |         synchronized (this.mLock) {
0174 |             this.mActivities.clear();
0175 |         }
0176 |     }
0177 | }
```

## D05

Source: `sources/com/miui/daemon/performance/statistics/notifications/NotificationUtils.java`

Source-file SHA-256: `c97be2e2d7288f23bbdc67d1f5ee7b064706ee2fb91807184ba2909c82e22eb7`

Original lines 1–146:

```text
0001 | package com.miui.daemon.performance.statistics.notifications;
0002 | 
0003 | import android.content.Context;
0004 | import android.content.Intent;
0005 | import android.os.Handler;
0006 | import android.os.HandlerThread;
0007 | import android.text.TextUtils;
0008 | import com.miui.daemon.performance.statistics.activitystats.ActivityLaunchRecord;
0009 | import com.miui.daemon.performance.statistics.network.DataUploader;
0010 | import com.miui.daemon.performance.statistics.network.Event;
0011 | import com.miui.daemon.performance.statistics.utils.ModuleUtils;
0012 | import java.io.FileDescriptor;
0013 | import java.io.PrintWriter;
0014 | import java.util.ArrayList;
0015 | import java.util.HashMap;
0016 | import java.util.Iterator;
0017 | import java.util.LinkedList;
0018 | import java.util.List;
0019 | import java.util.ListIterator;
0020 | import java.util.Map;
0021 | import org.json.JSONObject;
0022 | 
0023 | /* JADX INFO: loaded from: classes.dex */
0024 | public class NotificationUtils extends ModuleUtils {
0025 |     public static NotificationUtils sModules;
0026 |     public Handler mHandler;
0027 |     public HandlerThread mHandlerThread;
0028 |     public int mNonResponsivePushClickCounts;
0029 |     public HashMap<String, Integer> mNonResponsivePushEvents;
0030 |     public int mPushClickCounts;
0031 |     public LinkedList<PushClickEvent> mPushClickEventQueue;
0032 |     public int mResponsivePushClickCounts;
0033 | 
0034 |     public NotificationUtils(String str, String str2, String str3) {
0035 |         super(str, str2, str3);
0036 |         this.mHandlerThread = new HandlerThread("NotificationUtilsHandler");
0037 |         this.mPushClickEventQueue = new LinkedList<>();
0038 |         this.mPushClickCounts = 0;
0039 |         this.mNonResponsivePushClickCounts = 0;
0040 |         this.mResponsivePushClickCounts = 0;
0041 |         this.mNonResponsivePushEvents = new HashMap<>();
0042 |         this.mHandlerThread.start();
0043 |         this.mHandler = new Handler(this.mHandlerThread.getLooper());
0044 |     }
0045 | 
0046 |     public static NotificationUtils getInstance() {
0047 |         if (sModules == null) {
0048 |             sModules = new NotificationUtils("notifications", "notifications", "notifications");
0049 |         }
0050 |         return sModules;
0051 |     }
0052 | 
0053 |     @Override // com.miui.daemon.performance.statistics.utils.ModuleUtils
0054 |     public List<Event> getEvents(Context context) {
0055 |         ArrayList arrayList = new ArrayList();
0056 |         Event event = new Event();
0057 |         event.setPkn("notification");
0058 |         event.setPid(1000);
0059 |         event.setTs("" + System.currentTimeMillis());
0060 |         event.setPrn("Unknown");
0061 |         event.setLog(String.format("%s(%s/%s)", Integer.valueOf(this.mPushClickCounts), Integer.valueOf(this.mNonResponsivePushClickCounts), Integer.valueOf(this.mResponsivePushClickCounts)));
0062 |         event.setDet(new JSONObject(this.mNonResponsivePushEvents).toString());
0063 |         event.setSum("Performance target");
0064 |         event.setTy("notification");
0065 |         event.setKw("Performance");
0066 |         event.setDgt(DataUploader.getMd5Digest(event.getTs() + event.getDet()));
0067 |         arrayList.add(event);
0068 |         return arrayList;
0069 |     }
0070 | 
0071 |     public void addNotification(String str, Intent intent, final long j) {
0072 |         if (intent == null || intent.getComponent() == null || this.mPushClickEventQueue.size() >= 30) {
0073 |             return;
0074 |         }
0075 |         final String packageName = intent.getComponent().getPackageName();
0076 |         if (TextUtils.isEmpty(packageName) || !"com.xiaomi.xmsf".equals(str)) {
0077 |             return;
0078 |         }
0079 |         this.mHandler.post(new Runnable() { // from class: com.miui.daemon.performance.statistics.notifications.NotificationUtils.1
0080 |             @Override // java.lang.Runnable
0081 |             public void run() {
0082 |                 ListIterator listIterator = NotificationUtils.this.mPushClickEventQueue.listIterator(NotificationUtils.this.mPushClickEventQueue.size());
0083 |                 while (listIterator.hasPrevious()) {
0084 |                     if (((PushClickEvent) listIterator.previous()).mClickTimeMillis <= j) {
0085 |                         listIterator.next();
0086 |                         listIterator.add(new PushClickEvent(packageName, j));
0087 |                         NotificationUtils.this.mPushClickCounts++;
0088 |                     }
0089 |                 }
0090 |                 listIterator.add(new PushClickEvent(packageName, j));
0091 |                 NotificationUtils.this.mPushClickCounts++;
0092 |             }
0093 |         });
0094 |     }
0095 | 
0096 |     public void addActivityLaunch(final ActivityLaunchRecord activityLaunchRecord) {
0097 |         this.mHandler.post(new Runnable() { // from class: com.miui.daemon.performance.statistics.notifications.NotificationUtils.2
0098 |             @Override // java.lang.Runnable
0099 |             public void run() {
0100 |                 NotificationUtils.this.matchActivityLaunch(activityLaunchRecord);
0101 |                 NotificationUtils.this.pruneMismatchedNotification(activityLaunchRecord.getStartTs());
0102 |             }
0103 |         });
0104 |     }
0105 | 
0106 |     public final void matchActivityLaunch(ActivityLaunchRecord activityLaunchRecord) {
0107 |         Iterator<PushClickEvent> it = this.mPushClickEventQueue.iterator();
0108 |         while (it.hasNext()) {
0109 |             PushClickEvent next = it.next();
0110 |             if (next.mTargetPackage.equals(activityLaunchRecord.getPackageName()) && activityLaunchRecord.getStartTs() > next.mClickTimeMillis) {
0111 |                 it.remove();
0112 |                 this.mResponsivePushClickCounts++;
0113 |                 return;
0114 |             }
0115 |         }
0116 |     }
0117 | 
0118 |     public final void pruneMismatchedNotification(long j) {
0119 |         Iterator<PushClickEvent> it = this.mPushClickEventQueue.iterator();
0120 |         while (it.hasNext()) {
0121 |             PushClickEvent next = it.next();
0122 |             if (j > next.mClickTimeMillis + 5000) {
0123 |                 it.remove();
0124 |                 this.mNonResponsivePushClickCounts++;
0125 |                 if (this.mNonResponsivePushEvents.size() < 20) {
0126 |                     String str = next.mTargetPackage;
0127 |                     if (this.mNonResponsivePushEvents.containsKey(str)) {
0128 |                         HashMap<String, Integer> map = this.mNonResponsivePushEvents;
0129 |                         map.put(str, Integer.valueOf(map.get(str).intValue() + 1));
0130 |                     } else {
0131 |                         this.mNonResponsivePushEvents.put(str, 1);
0132 |                     }
0133 |                 }
0134 |             }
0135 |         }
0136 |     }
0137 | 
0138 |     @Override // com.miui.daemon.performance.statistics.utils.ModuleUtils
0139 |     public void dumpRecords(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
0140 |         super.dumpRecords(fileDescriptor, printWriter, strArr);
0141 |         printWriter.println("Push Click Counts: " + this.mPushClickCounts + " Responsive:" + this.mResponsivePushClickCounts + " NonResponsive:" + this.mNonResponsivePushClickCounts);
0142 |         for (Map.Entry<String, Integer> entry : this.mNonResponsivePushEvents.entrySet()) {
0143 |             printWriter.println(entry.getKey() + " failed: " + entry.getValue());
0144 |         }
0145 |     }
0146 | }
```

## D06

Source: `sources/com/miui/daemon/mqsas/upload/PerfEventUploader.java`

Source-file SHA-256: `139c8445c6aeb478e4137de26a0e851f1a8a26838dc216c3e6cbf0cd13fb9b35`

Original lines 1–90:

```text
0001 | package com.miui.daemon.mqsas.upload;
0002 | 
0003 | import android.content.Context;
0004 | import android.text.TextUtils;
0005 | import android.util.Log;
0006 | import app.src.com.miui.daemon.performance.statistics.retentiontstats.RetentionUtils;
0007 | import com.miui.daemon.mqsas.utils.DeviceUtil;
0008 | import com.miui.daemon.mqsas.utils.ThreadPool;
0009 | import com.miui.daemon.performance.MiuiPerfService;
0010 | import com.miui.daemon.performance.onetrack.TrackAnalytics;
0011 | import com.miui.daemon.performance.onetrack.TrackModuleUtils;
0012 | import com.miui.daemon.performance.statistics.network.DataUploader;
0013 | import com.miui.daemon.performance.statistics.network.Event;
0014 | import com.miui.daemon.performance.statistics.network.LegacyUploadEvent;
0015 | import com.miui.daemon.performance.statistics.network.Upload;
0016 | import com.miui.daemon.performance.statistics.utils.DeviceUtils;
0017 | import com.miui.daemon.performance.statistics.utils.ModuleUtils;
0018 | import java.util.Iterator;
0019 | import java.util.List;
0020 | import java.util.Map;
0021 | 
0022 | /* JADX INFO: loaded from: classes.dex */
0023 | public class PerfEventUploader {
0024 |     private static PerfEventUploader sInstance;
0025 |     private static DataUploader sUploader = new DataUploader();
0026 |     private Context mContext;
0027 | 
0028 |     private PerfEventUploader(Context context) {
0029 |         this.mContext = context;
0030 |     }
0031 | 
0032 |     public static synchronized PerfEventUploader getInstance(Context context) {
0033 |         if (sInstance == null) {
0034 |             sInstance = new PerfEventUploader(context);
0035 |         }
0036 |         return sInstance;
0037 |     }
0038 | 
0039 |     public void uploadPerfEvent() {
0040 |         if (DeviceUtils.isRoot() && !DeviceUtil.isUnReleased()) {
0041 |             Log.i("MiuiPerfServiceClient", "is root user not need upload");
0042 |         } else {
0043 |             ThreadPool.execute(new Runnable() { // from class: com.miui.daemon.mqsas.upload.PerfEventUploader.1
0044 |                 @Override // java.lang.Runnable
0045 |                 public void run() {
0046 |                     Map<String, ModuleUtils> modules = MiuiPerfService.getModules();
0047 |                     for (String str : modules.keySet()) {
0048 |                         Upload uploadCreateUploadEventWithUrl = PerfEventUploader.this.createUploadEventWithUrl(str);
0049 |                         if (uploadCreateUploadEventWithUrl == null) {
0050 |                             Log.d("MiuiPerfServiceClient", "Upload " + str + ", but Upload object is null!");
0051 |                         } else {
0052 |                             ModuleUtils moduleUtils = modules.get(str);
0053 |                             List<Event> events = moduleUtils.getEvents(PerfEventUploader.this.mContext);
0054 |                             moduleUtils.removeUploadedFile(PerfEventUploader.this.mContext, moduleUtils.getDataDir(), moduleUtils.getFileSuffix());
0055 |                             if (events != null && events.size() > 0) {
0056 |                                 uploadCreateUploadEventWithUrl.addEvents(events);
0057 |                                 PerfEventUploader.sUploader.request(PerfEventUploader.this.mContext, uploadCreateUploadEventWithUrl, moduleUtils.getModuleName());
0058 |                             } else {
0059 |                                 Log.d("MiuiPerfServiceClient", "Upload " + moduleUtils + " datas is empty!");
0060 |                             }
0061 |                         }
0062 |                     }
0063 |                     TrackAnalytics.updateTrackConfig();
0064 |                     RetentionUtils.getInstance().preparation(PerfEventUploader.this.mContext);
0065 |                     Map<String, TrackModuleUtils> oneTrackModules = MiuiPerfService.getOneTrackModules();
0066 |                     Iterator<String> it = oneTrackModules.keySet().iterator();
0067 |                     while (it.hasNext()) {
0068 |                         oneTrackModules.get(it.next()).uploadOneTrackEvent();
0069 |                     }
0070 |                 }
0071 |             });
0072 |         }
0073 |     }
0074 | 
0075 |     /* JADX INFO: Access modifiers changed from: private */
0076 |     public Upload createUploadEventWithUrl(String str) {
0077 |         if ("matrix".equals(str)) {
0078 |             String uploadUrl = com.miui.daemon.performance.statistics.network.Constants.getUploadUrl();
0079 |             if (!TextUtils.isEmpty(uploadUrl)) {
0080 |                 return new com.miui.daemon.performance.statistics.network.UploadEvent(uploadUrl);
0081 |             }
0082 |         } else {
0083 |             String legacyUploadUrl = com.miui.daemon.performance.statistics.network.Constants.getLegacyUploadUrl();
0084 |             if (!TextUtils.isEmpty(legacyUploadUrl)) {
0085 |                 return new LegacyUploadEvent(legacyUploadUrl);
0086 |             }
0087 |         }
0088 |         return null;
0089 |     }
0090 | }
```

## D07

Source: `sources/com/miui/daemon/mqsas/utils/DeviceUtil.java`

Source-file SHA-256: `8e0da10478bc800cbea5952edaf1b1746e49d3b9ead0f9b7d68677e48f07b45d`

Original lines 1–330:

```text
0001 | package com.miui.daemon.mqsas.utils;
0002 | 
0003 | import android.accounts.Account;
0004 | import android.accounts.AccountManager;
0005 | import android.app.ActivityManager;
0006 | import android.app.ActivityThread;
0007 | import android.content.ContentResolver;
0008 | import android.content.Context;
0009 | import android.os.Build;
0010 | import android.os.FileObserver;
0011 | import android.os.HwBinder;
0012 | import android.os.HwParcel;
0013 | import android.os.IHwBinder;
0014 | import android.os.RemoteException;
0015 | import android.os.SystemProperties;
0016 | import android.os.UserHandle;
0017 | import android.os.storage.DiskInfo;
0018 | import android.os.storage.StorageManager;
0019 | import android.os.storage.VolumeInfo;
0020 | import android.provider.MiuiSettings;
0021 | import android.provider.Settings;
0022 | import android.text.TextUtils;
0023 | import com.android.id.IdentifierManager;
0024 | import com.litesuits.orm.db.assit.SQLBuilder;
0025 | import com.miui.daemon.mqsas.upload.Constants;
0026 | import com.miui.daemon.mqsas.upload.storagemodule.StorageInfo;
0027 | import com.miui.daemon.performance.statistics.utils.PreferenceUtils;
0028 | import java.io.File;
0029 | import java.lang.reflect.InvocationTargetException;
0030 | import java.lang.reflect.Method;
0031 | import java.util.ArrayList;
0032 | import java.util.List;
0033 | import java.util.NoSuchElementException;
0034 | import java.util.UUID;
0035 | import java.util.concurrent.Callable;
0036 | import java.util.concurrent.ExecutorService;
0037 | import java.util.concurrent.Executors;
0038 | import java.util.concurrent.FutureTask;
0039 | import java.util.concurrent.TimeUnit;
0040 | import org.json.JSONException;
0041 | import org.json.JSONObject;
0042 | 
0043 | /* JADX INFO: loaded from: classes.dex */
0044 | public class DeviceUtil {
0045 |     public static String ACCOUNT_ID;
0046 |     public static String ANDROID_ID;
0047 |     public static String DEVICE_FID;
0048 |     public static String IMEI;
0049 |     public static boolean IS_ROOT;
0050 |     public static String MIUI_VERSION_TYPE;
0051 |     public static String ONE_POWERUP_UUID;
0052 |     public static String SERIAL_NO;
0053 |     public static final String DEVICE = Build.DEVICE;
0054 |     public static final String PRODUCT_NAME = Build.PRODUCT;
0055 |     public static final String ANDROID_VERSION = Build.VERSION.RELEASE;
0056 |     public static final String MODEL = Build.MODEL.replace(SQLBuilder.BLANK, "");
0057 |     public static final String MIUI_VERSION = Build.VERSION.INCREMENTAL;
0058 |     public static final int ANDROID_SDK = Build.VERSION.SDK_INT;
0059 |     public static final String CARRIER = SystemProperties.get("ro.carrier", "unknown");
0060 |     public static final String REGION = miui.os.Build.getRegion();
0061 |     public static boolean IS_GKI_DEVICE = SystemProperties.getBoolean("persist.sys.device_config_gki", false);
0062 |     public static final String HWLEVEL = SystemProperties.get("ro.boot.hwlevel", "unknown");
0063 |     public static boolean ENABLE_MIUI_COREDUMP = SystemProperties.getBoolean("persist.sys.enable_miui_coredump", false);
0064 |     public static List<String> DEVICE_IDS = new ArrayList();
0065 | 
0066 |     public static boolean isGlobalBuild() {
0067 |         return miui.os.Build.IS_INTERNATIONAL_BUILD;
0068 |     }
0069 | 
0070 |     public static void initDeviceDetails(Context context) {
0071 |         MIUI_VERSION_TYPE = getMiuiVersionType();
0072 |         ANDROID_ID = getAndroidId(context);
0073 |         IMEI = getImei();
0074 |         IS_ROOT = isRoot();
0075 |         DEVICE_IDS = getAllDeviceIds();
0076 |         SERIAL_NO = SystemProperties.get("ro.ril.oem.sno", "unknown");
0077 |         ONE_POWERUP_UUID = UUID.randomUUID().toString();
0078 |     }
0079 | 
0080 |     public static String getAccountId(final Context context) {
0081 |         ExecutorService executorServiceNewSingleThreadExecutor = Executors.newSingleThreadExecutor();
0082 |         FutureTask futureTask = new FutureTask(new Callable<String>() { // from class: com.miui.daemon.mqsas.utils.DeviceUtil.1
0083 |             @Override // java.util.concurrent.Callable
0084 |             public String call() throws Exception {
0085 |                 Account[] accountsByType;
0086 |                 AccountManager accountManager = AccountManager.get(context);
0087 |                 if (accountManager == null || (accountsByType = accountManager.getAccountsByType(Constants.ACCOUNT_TYPE_STR)) == null || accountsByType.length == 0) {
0088 |                     return "";
0089 |                 }
0090 |                 String str = accountsByType[0].name;
0091 |                 return TextUtils.isEmpty(str) ? "" : str;
0092 |             }
0093 |         });
0094 |         executorServiceNewSingleThreadExecutor.execute(futureTask);
0095 |         try {
0096 |             try {
0097 |                 try {
0098 |                     String str = (String) futureTask.get(300L, TimeUnit.MILLISECONDS);
0099 |                     if (!TextUtils.isEmpty(str)) {
0100 |                         ACCOUNT_ID = str;
0101 |                     }
0102 |                     try {
0103 |                         executorServiceNewSingleThreadExecutor.shutdown();
0104 |                     } catch (Exception e) {
0105 |                         e.printStackTrace();
0106 |                     }
0107 |                     return ACCOUNT_ID;
0108 |                 } catch (Exception e2) {
0109 |                     e2.printStackTrace();
0110 |                     try {
0111 |                         futureTask.cancel(true);
0112 |                     } catch (Exception unused) {
0113 |                         e2.printStackTrace();
0114 |                     }
0115 |                     try {
0116 |                         executorServiceNewSingleThreadExecutor.shutdown();
0117 |                     } catch (Exception e3) {
0118 |                         e3.printStackTrace();
0119 |                     }
0120 |                     return ACCOUNT_ID;
0121 |                 }
0122 |             } catch (Throwable unused2) {
0123 |                 executorServiceNewSingleThreadExecutor.shutdown();
0124 |                 return ACCOUNT_ID;
0125 |             }
0126 |         } catch (Exception e4) {
0127 |             e4.printStackTrace();
0128 |             return ACCOUNT_ID;
0129 |         }
0130 |     }
0131 | 
0132 |     public static boolean isLegalReleaseVersion() {
0133 |         if ("user".equals(Build.TYPE) && "release-keys".equals(Build.TAGS)) {
0134 |             return miui.os.Build.IS_STABLE_VERSION || miui.os.Build.IS_PRE_VERSION || miui.os.Build.IS_DEV_VERSION;
0135 |         }
0136 |         return false;
0137 |     }
0138 | 
0139 |     @Deprecated
0140 |     public static String getDeviceIMEI() {
0141 |         if (TextUtils.isEmpty(IMEI)) {
0142 |             try {
0143 |                 Method declaredMethod = Class.forName("miui.telephony.TelephonyManagerUtil").getDeclaredMethod("getDeviceId", new Class[0]);
0144 |                 declaredMethod.setAccessible(true);
0145 |                 IMEI = (String) declaredMethod.invoke(null, new Object[0]);
0146 |             } catch (Exception e) {
0147 |                 e.printStackTrace();
0148 |             } catch (Throwable unused) {
0149 |             }
0150 |         }
0151 |         return IMEI;
0152 |     }
0153 | 
0154 |     public static String getImei() {
0155 |         String str = "";
0156 |         try {
0157 |             Class<?> cls = Class.forName("miui.telephony.TelephonyManager");
0158 |             Object objInvoke = cls.getMethod("getDefault", new Class[0]).invoke(cls, new Object[0]);
0159 |             Method method = cls.getMethod("getImeiForSlot", Integer.TYPE);
0160 |             String str2 = (String) method.invoke(objInvoke, 0);
0161 |             try {
0162 |                 Utils.logD("DeviceUtil", "imei 1 is --" + str2);
0163 |                 if (TextUtils.isEmpty(str2)) {
0164 |                     str = (String) method.invoke(objInvoke, 1);
0165 |                     Utils.logD("DeviceUtil", "imei 2 is --" + str);
0166 |                 } else {
0167 |                     str = str2;
0168 |                 }
0169 |                 return TextUtils.isEmpty(str) ? "null" : str;
0170 |             } catch (ClassNotFoundException e) {
0171 |                 e = e;
0172 |                 str = str2;
0173 |                 e.printStackTrace();
0174 |                 return str;
0175 |             } catch (IllegalAccessException e2) {
0176 |                 e = e2;
0177 |                 str = str2;
0178 |                 e.printStackTrace();
0179 |                 return str;
0180 |             } catch (NoSuchMethodException e3) {
0181 |                 e = e3;
0182 |                 str = str2;
0183 |                 e.printStackTrace();
0184 |                 return str;
0185 |             } catch (InvocationTargetException e4) {
0186 |                 e = e4;
0187 |                 str = str2;
0188 |                 e.printStackTrace();
0189 |                 return str;
0190 |             }
0191 |         } catch (ClassNotFoundException e5) {
0192 |             e = e5;
0193 |         } catch (IllegalAccessException e6) {
0194 |             e = e6;
0195 |         } catch (NoSuchMethodException e7) {
0196 |             e = e7;
0197 |         } catch (InvocationTargetException e8) {
0198 |             e = e8;
0199 |         }
0200 |     }
0201 | 
0202 |     public static String getImei1() {
0203 |         try {
0204 |             Class<?> cls = Class.forName("miui.telephony.TelephonyManager");
0205 |             String str = (String) cls.getMethod("getImeiForSlot", Integer.TYPE).invoke(cls.getMethod("getDefault", new Class[0]).invoke(cls, new Object[0]), 0);
0206 |             return TextUtils.isEmpty(str) ? "null" : str;
0207 |         } catch (ClassNotFoundException e) {
0208 |             e.printStackTrace();
0209 |             return "null";
0210 |         } catch (IllegalAccessException e2) {
0211 |             e2.printStackTrace();
0212 |             return "null";
0213 |         } catch (NoSuchMethodException e3) {
0214 |             e3.printStackTrace();
0215 |             return "null";
0216 |         } catch (InvocationTargetException e4) {
0217 |             e4.printStackTrace();
0218 |             return "null";
0219 |         }
0220 |     }
0221 | 
0222 |     public static String getImei2() {
0223 |         try {
0224 |             Class<?> cls = Class.forName("miui.telephony.TelephonyManager");
0225 |             String str = (String) cls.getMethod("getImeiForSlot", Integer.TYPE).invoke(cls.getMethod("getDefault", new Class[0]).invoke(cls, new Object[0]), 1);
0226 |             return TextUtils.isEmpty(str) ? "null" : str;
0227 |         } catch (ClassNotFoundException e) {
0228 |             e.printStackTrace();
0229 |             return "null";
0230 |         } catch (IllegalAccessException e2) {
0231 |             e2.printStackTrace();
0232 |             return "null";
0233 |         } catch (NoSuchMethodException e3) {
0234 |             e3.printStackTrace();
0235 |             return "null";
0236 |         } catch (InvocationTargetException e4) {
0237 |             e4.printStackTrace();
0238 |             return "null";
0239 |         }
0240 |     }
0241 | 
0242 |     public static String getUUID(Context context) {
0243 |         String preference = PreferenceUtils.getPreference(context, "uuid");
0244 |         if (!TextUtils.isEmpty(preference) && !preference.equals("unknown")) {
0245 |             return preference;
0246 |         }
0247 |         String string = UUID.randomUUID().toString();
0248 |         PreferenceUtils.putPreference(context, "uuid", string);
0249 |         ContentResolver contentResolver = context.getContentResolver();
0250 |         Utils.logE("DeviceUtil", "set uuid :" + string);
0251 |         MiuiSettings.System.putStringForUser(contentResolver, "key_mqs_uuid", string, UserHandle.myUserId());
0252 |         return string;
0253 |     }
0254 | 
0255 |     public static String getID(Context context) {
0256 |         String oaid = IdentifierManager.getOAID(context);
0257 |         Utils.logD("DeviceUtil", "oaid " + oaid);
0258 |         if (!TextUtils.isEmpty(oaid)) {
0259 |             return oaid;
0260 |         }
0261 |         String uuid = getUUID(context);
0262 |         Utils.logD("DeviceUtil", "deviceId=uuid " + uuid);
0263 |         return uuid;
0264 |     }
0265 | 
0266 |     public static List<String> getAllDeviceIds() {
0267 |         if (DEVICE_IDS.isEmpty()) {
0268 |             ArrayList arrayList = new ArrayList();
0269 |             try {
0270 |                 Class<?> cls = Class.forName("miui.telephony.TelephonyManager");
0271 |                 Object objInvoke = cls.getMethod("getDefault", new Class[0]).invoke(cls, new Object[0]);
0272 |                 List list = (List) cls.getMethod("getImeiList", new Class[0]).invoke(objInvoke, new Object[0]);
0273 |                 if (list != null && !list.isEmpty()) {
0274 |                     arrayList.addAll(list);
0275 |                 }
0276 |                 List list2 = (List) cls.getMethod("getMeidList", new Class[0]).invoke(objInvoke, new Object[0]);
0277 |                 if (list2 != null && !list2.isEmpty()) {
0278 |                     arrayList.addAll(list2);
0279 |                 }
0280 |             } catch (Exception e) {
0281 |                 e.printStackTrace();
0282 |             }
0283 |             return arrayList;
0284 |         }
0285 |         return DEVICE_IDS;
0286 |     }
0287 | 
0288 |     public static String getMiuiVersionType() {
0289 |         if (miui.os.Build.IS_ALPHA_BUILD) {
0290 |             return "alpha";
0291 |         }
0292 |         if (miui.os.Build.IS_DEVELOPMENT_VERSION) {
0293 |             return "development";
0294 |         }
0295 |         return miui.os.Build.IS_STABLE_VERSION ? "stable" : "unknown";
0296 |     }
0297 | 
0298 |     public static String getAndroidId(Context context) {
0299 |         String string;
0300 |         try {
0301 |             string = Settings.Secure.getString(context.getContentResolver(), "android_id");
0302 |         } catch (Exception e) {
0303 |             e.printStackTrace();
0304 |             string = "";
0305 |         }
0306 |         return TextUtils.isEmpty(string) ? "" : string;
0307 |     }
0308 | 
0309 |     public static boolean isHigherMiuiVersion(String str) {
0310 |         if (!TextUtils.isEmpty(str)) {
0311 |             String str2 = MIUI_VERSION;
0312 |             if (!str2.equals(str)) {
0313 |                 String strReplaceAll = str2.replaceAll("[^\\d.]", "");
0314 |                 String[] strArrSplit = str.replaceAll("[^\\d.]", "").split("\\.");
0315 |                 String[] strArrSplit2 = strReplaceAll.split("\\.");
0316 |                 int length = strArrSplit != null ? strArrSplit.length : 0;
0317 |                 if (length == (strArrSplit2 != null ? strArrSplit2.length : 0) && length != 0) {
0318 |                     for (int i = 0; i < length; i++) {
0319 |                         try {
0320 |                             int i2 = Integer.parseInt(strArrSplit[i]);
0321 |                             int i3 = Integer.parseInt(strArrSplit2[i]);
0322 |                             if (i3 > i2) {
0323 |                                 return true;
0324 |                             }
0325 |                             if (i3 < i2) {
0326 |                                 break;
0327 |                             }
0328 |                             if (i == length - 1) {
0329 |                                 return true;
0330 |                             }
```

## D08

Source: `sources/com/miui/daemon/mqsas/upload/BaseUploader.java`

Source-file SHA-256: `9be2feecf4b4d34f48b685402232b9f7df33389c4816045aabacdf1b28d19f5f`

Original lines 65–145:

```text
0065 |                         @Override // java.lang.Runnable
0066 |                         public void run() {
0067 |                             BaseUploader.this.onUploadDone();
0068 |                         }
0069 |                     });
0070 |                 }
0071 |             }).setErrorListener(new Response.ErrorListener() { // from class: com.miui.daemon.mqsas.upload.BaseUploader.1
0072 |                 @Override // com.android.volley.Response.ErrorListener
0073 |                 public void onErrorResponse(VolleyError volleyError) {
0074 |                     Utils.logE(BaseUploader.this.TAG, volleyError != null ? volleyError.toString() : "");
0075 |                 }
0076 |             }).doRequest();
0077 |         }
0078 |     }
0079 | 
0080 |     public Map<String, String> getBaseParams(Context context, String str) {
0081 |         HashMap map = new HashMap();
0082 |         map.put(Constants.PARAM_KEY_DEVICE, DeviceUtil.DEVICE);
0083 |         map.put(Constants.PARAM_KEY_CARRIER, DeviceUtil.CARRIER);
0084 |         map.put(Constants.PARAM_KEY_REGION, DeviceUtil.REGION);
0085 |         map.put(Constants.PARAM_KEY_ANDROID_VERSION, DeviceUtil.ANDROID_VERSION);
0086 |         map.put("l", DeviceUtil.MODEL);
0087 |         map.put(Constants.PARAM_KEY_MIUI_VERSION, DeviceUtil.MIUI_VERSION);
0088 |         map.put(Constants.PARAM_KEY_VERSION_TYPE, DeviceUtil.MIUI_VERSION_TYPE);
0089 |         map.put("mi", DeviceUtil.getUUID(context));
0090 |         map.put(Constants.PARAM_KEY_CLIENT_VERSION, PackageUtil.getVersionName(context));
0091 |         map.put(Constants.PARAM_KEY_DATE, DATE_FORMAT.format(new Date()));
0092 |         map.put(Constants.PARAM_KEY_ACCOUNT, EncodeUtil.getMd5Digest(""));
0093 |         if (Utils.IS_INTERNATIONAL_BUILD) {
0094 |             map.put(Constants.PARAM_KEY_IMEI, DeviceUtil.getUUID(context));
0095 |         } else {
0096 |             map.put(Constants.PARAM_KEY_IMEI, DeviceUtil.getID(context));
0097 |         }
0098 |         map.put("module", getModuleName());
0099 |         return map;
0100 |     }
0101 | 
0102 |     public Map<String, String> makeKeyParams(Map<String, String> map, Map<String, String> map2, String str) {
0103 |         StringBuilder sb = new StringBuilder();
0104 |         for (Map.Entry<String, String> entry : map.entrySet()) {
0105 |             sb.append(entry.getKey());
0106 |             sb.append("=");
0107 |             sb.append(entry.getValue());
0108 |             sb.append("&");
0109 |         }
0110 |         if (map2 != null) {
0111 |             for (Map.Entry<String, String> entry2 : map2.entrySet()) {
0112 |                 sb.append(entry2.getKey());
0113 |                 sb.append("=");
0114 |                 sb.append(entry2.getValue());
0115 |                 sb.append("&");
0116 |                 map.put(entry2.getKey(), entry2.getValue());
0117 |             }
0118 |         }
0119 |         sb.append("time=");
0120 |         sb.append(System.currentTimeMillis());
0121 |         map.put(Constants.PARAM_KEY_REPORT_ID, EncodeUtil.getMd5Digest(sb.toString()));
0122 |         ArrayList<Map.Entry> arrayList = new ArrayList(map.entrySet());
0123 |         Collections.sort(arrayList, new Comparator<Map.Entry<String, String>>() { // from class: com.miui.daemon.mqsas.upload.BaseUploader.3
0124 |             @Override // java.util.Comparator
0125 |             public int compare(Map.Entry<String, String> entry3, Map.Entry<String, String> entry4) {
0126 |                 return entry3.getKey().compareTo(entry4.getKey());
0127 |             }
0128 |         });
0129 |         StringBuilder sb2 = new StringBuilder();
0130 |         boolean z = true;
0131 |         for (Map.Entry entry3 : arrayList) {
0132 |             if (!z) {
0133 |                 sb2.append("&");
0134 |             }
0135 |             sb2.append((String) entry3.getKey());
0136 |             sb2.append("=");
0137 |             sb2.append((String) entry3.getValue());
0138 |             z = false;
0139 |         }
0140 |         sb2.append("&");
0141 |         sb2.append(str);
0142 |         map.put(Constants.PARAM_KEY_SIGN, EncodeUtil.getMd5Digest(Base64.encodeToString(EncodeUtil.getBytes(sb2.toString()), 2)));
0143 |         return map;
0144 |     }
0145 | }
```

## D09

Source: `sources/com/miui/daemon/performance/statistics/network/DataUploader.java`

Source-file SHA-256: `3a955a7bd4930d6ef52002859da145586764b244d374651635769781c003017f`

Original lines 38–105:

```text
0038 |     public static final DateFormat DATE_FORMAT = new SimpleDateFormat(w.g, Locale.getDefault());
0039 |     public static boolean DEBUG = false;
0040 | 
0041 |     public final Map<String, String> getBaseParams(Context context, String str, String str2) {
0042 |         HashMap map = new HashMap();
0043 |         map.put(com.miui.daemon.mqsas.upload.Constants.PARAM_KEY_DEVICE, DeviceUtils.DEVICE);
0044 |         map.put(com.miui.daemon.mqsas.upload.Constants.PARAM_KEY_CARRIER, DeviceUtils.CARRIER);
0045 |         map.put(com.miui.daemon.mqsas.upload.Constants.PARAM_KEY_REGION, DeviceUtils.REGION);
0046 |         map.put(com.miui.daemon.mqsas.upload.Constants.PARAM_KEY_ANDROID_VERSION, DeviceUtils.ANDROID_VERSION);
0047 |         map.put("l", DeviceUtils.MODEL);
0048 |         map.put(com.miui.daemon.mqsas.upload.Constants.PARAM_KEY_MIUI_VERSION, DeviceUtils.MIUI_VERSION);
0049 |         map.put(com.miui.daemon.mqsas.upload.Constants.PARAM_KEY_VERSION_TYPE, DeviceUtils.MIUI_VERSION_TYPE);
0050 |         map.put("mi", toHexReadable(DigestUtils.get(DeviceUtils.ANDROID_ID + str, "MD5")));
0051 |         map.put(com.miui.daemon.mqsas.upload.Constants.PARAM_KEY_CLIENT_VERSION, "1.5.160106");
0052 |         map.put(com.miui.daemon.mqsas.upload.Constants.PARAM_KEY_DATE, DATE_FORMAT.format(new Date()));
0053 |         map.put(com.miui.daemon.mqsas.upload.Constants.PARAM_KEY_ACCOUNT, DeviceUtils.getUploadUserId(context));
0054 |         map.put(com.miui.daemon.mqsas.upload.Constants.PARAM_KEY_IMEI, DeviceUtils.getDeviceId(context));
0055 |         map.put("module", str2);
0056 |         return map;
0057 |     }
0058 | 
0059 |     public final Map<String, String> makeKeyParams(Map<String, String> map, String str) {
0060 |         StringBuilder sb = new StringBuilder();
0061 |         for (Map.Entry<String, String> entry : map.entrySet()) {
0062 |             sb.append(entry.getKey());
0063 |             sb.append("=");
0064 |             sb.append(entry.getValue());
0065 |             sb.append("&");
0066 |         }
0067 |         sb.append("time=");
0068 |         sb.append(System.currentTimeMillis());
0069 |         map.put(com.miui.daemon.mqsas.upload.Constants.PARAM_KEY_REPORT_ID, getMd5Digest(sb.toString()));
0070 |         ArrayList<Map.Entry> arrayList = new ArrayList(map.entrySet());
0071 |         Collections.sort(arrayList, new Comparator<Map.Entry<String, String>>() { // from class: com.miui.daemon.performance.statistics.network.DataUploader.1
0072 |             @Override // java.util.Comparator
0073 |             public int compare(Map.Entry<String, String> entry2, Map.Entry<String, String> entry3) {
0074 |                 return entry2.getKey().compareTo(entry3.getKey());
0075 |             }
0076 |         });
0077 |         StringBuilder sb2 = new StringBuilder();
0078 |         boolean z = true;
0079 |         for (Map.Entry entry2 : arrayList) {
0080 |             if (!z) {
0081 |                 sb2.append("&");
0082 |             }
0083 |             sb2.append((String) entry2.getKey());
0084 |             sb2.append("=");
0085 |             sb2.append((String) entry2.getValue());
0086 |             z = false;
0087 |         }
0088 |         sb2.append("&");
0089 |         sb2.append(str);
0090 |         map.put(com.miui.daemon.mqsas.upload.Constants.PARAM_KEY_SIGN, getMd5Digest(Base64.encodeToString(getBytes(sb2.toString()), 2)));
0091 |         return map;
0092 |     }
0093 | 
0094 |     public static String toHexReadable(byte[] bArr) {
0095 |         StringBuilder sb = new StringBuilder();
0096 |         for (byte b : bArr) {
0097 |             sb.append(Integer.toString((b & 255) + 256, 16).substring(1));
0098 |         }
0099 |         return sb.toString();
0100 |     }
0101 | 
0102 |     public static byte[] getBytes(String str) {
0103 |         try {
0104 |             return str.getBytes("UTF-8");
0105 |         } catch (UnsupportedEncodingException unused) {
```

Original lines 143–205:

```text
0143 |             /* JADX WARN: Type inference failed for: r3v7 */
0144 |             /* JADX WARN: Type inference failed for: r3v8 */
0145 |             /* JADX WARN: Type inference failed for: r3v9 */
0146 |             @Override // java.lang.Runnable
0147 |             public void run() throws Throwable {
0148 |                 OutputStream outputStream;
0149 |                 DataUploader dataUploader = DataUploader.this;
0150 |                 Map<String, String> mapMakeKeyParams = dataUploader.makeKeyParams(dataUploader.getBaseParams(context, com.miui.daemon.mqsas.upload.Constants.SALT, str), com.miui.daemon.mqsas.upload.Constants.SALT);
0151 |                 ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
0152 |                 ?? inputStream = 0;
0153 |                 inputStream = 0;
0154 |                 inputStream = 0;
0155 |                 inputStream = 0;
0156 |                 inputStream = 0;
0157 |                 inputStream = 0;
0158 |                 inputStream = 0;
0159 |                 inputStream = 0;
0160 |                 inputStream = 0;
0161 |                 inputStream = 0;
0162 |                 try {
0163 |                     try {
0164 |                         try {
0165 |                             Gson gson = new Gson();
0166 |                             upload.addParams(mapMakeKeyParams);
0167 |                             String json = gson.toJson(upload);
0168 |                             String strAssembleUploadUrl = upload.assembleUploadUrl(mapMakeKeyParams);
0169 |                             HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(strAssembleUploadUrl).openConnection();
0170 |                             if (URLUtil.isHttpsUrl(strAssembleUploadUrl)) {
0171 |                                 TrustManager[] trustManagerArr = {new TrustAllCerts()};
0172 |                                 SSLContext sSLContext = SSLContext.getInstance("TLS");
0173 |                                 sSLContext.init(null, trustManagerArr, new SecureRandom());
0174 |                                 ((HttpsURLConnection) httpURLConnection).setSSLSocketFactory(sSLContext.getSocketFactory());
0175 |                             }
0176 |                             httpURLConnection.setRequestMethod("POST");
0177 |                             httpURLConnection.setDoOutput(true);
0178 |                             httpURLConnection.setDoInput(true);
0179 |                             httpURLConnection.setUseCaches(false);
0180 |                             httpURLConnection.setRequestProperty(HttpHeaderParser.HEADER_CONTENT_TYPE, "application/json");
0181 |                             outputStream = httpURLConnection.getOutputStream();
0182 |                             try {
0183 |                                 outputStream.write(json.getBytes());
0184 |                                 outputStream.flush();
0185 |                                 Log.d("MiuiPerfServiceClient", "responseCode:" + httpURLConnection.getResponseCode());
0186 |                                 if (DataUploader.DEBUG) {
0187 |                                     Log.d("MiuiPerfServiceClient", "urlStr:" + strAssembleUploadUrl);
0188 |                                     Log.d("MiuiPerfServiceClient", "body:" + json);
0189 |                                 }
0190 |                                 inputStream = httpURLConnection.getInputStream();
0191 |                                 byte[] bArr = new byte[1024];
0192 |                                 while (true) {
0193 |                                     int i = inputStream.read(bArr);
0194 |                                     if (i == -1) {
0195 |                                         break;
0196 |                                     } else {
0197 |                                         byteArrayOutputStream.write(bArr, 0, i);
0198 |                                     }
0199 |                                 }
0200 |                                 Log.d("MiuiPerfServiceClient", new String(byteArrayOutputStream.toByteArray()));
0201 |                                 try {
0202 |                                     inputStream.close();
0203 |                                 } catch (IOException e) {
0204 |                                     e.printStackTrace();
0205 |                                 }
```

## D10

Source: `sources/com/miui/daemon/mqsas/upload/ExceptionUploader.java`

Source-file SHA-256: `e7587eeaccceac2be49d076b77d13d898d885e77b4adfbdddf741b2fde2f63ae`

Original lines 1–180:

```text
0001 | package com.miui.daemon.mqsas.upload;
0002 | 
0003 | import android.content.Context;
0004 | import android.os.RemoteException;
0005 | import com.google.gson.Gson;
0006 | import com.miui.daemon.BaseDaemonApplication;
0007 | import com.miui.daemon.mqsas.providers.DatabaseUtils;
0008 | import com.miui.daemon.mqsas.utils.DeviceUtil;
0009 | import com.miui.daemon.mqsas.utils.OnetrackUtils;
0010 | import com.miui.daemon.mqsas.utils.Utils;
0011 | import java.util.ArrayList;
0012 | import java.util.List;
0013 | import java.util.Map;
0014 | import org.json.JSONException;
0015 | import org.json.JSONObject;
0016 | 
0017 | /* JADX INFO: loaded from: classes.dex */
0018 | public class ExceptionUploader extends BaseUploader {
0019 |     public static final int LIMIT = 30;
0020 |     private static final String TAG = "ExceptionUploader";
0021 |     private static ExceptionUploader sInstance;
0022 |     private String MODULE;
0023 |     private List<MajorExceptionEvent> mMajorExceptionEventList;
0024 | 
0025 |     @Override // com.miui.daemon.mqsas.upload.BaseUploader
0026 |     public /* bridge */ /* synthetic */ void requestUpload() {
0027 |         super.requestUpload();
0028 |     }
0029 | 
0030 |     private ExceptionUploader(Context context) {
0031 |         super(context);
0032 |         this.MODULE = "mqsas";
0033 |     }
0034 | 
0035 |     public static synchronized ExceptionUploader getInstance(Context context) {
0036 |         if (sInstance == null) {
0037 |             sInstance = new ExceptionUploader(context);
0038 |         }
0039 |         return sInstance;
0040 |     }
0041 | 
0042 |     @Override // com.miui.daemon.mqsas.upload.BaseUploader
0043 |     public String getModuleName() {
0044 |         return this.MODULE;
0045 |     }
0046 | 
0047 |     @Override // com.miui.daemon.mqsas.upload.BaseUploader
0048 |     public String getUploadUrl() {
0049 |         return Constants.MQSAS_UPLOAD_URL;
0050 |     }
0051 | 
0052 |     @Override // com.miui.daemon.mqsas.upload.BaseUploader
0053 |     public String buildUploadBody() {
0054 |         if (DeviceUtil.isUnReleased()) {
0055 |             Utils.logE(TAG, "Unreleased device use ExceptionUploaderV2 to upload.");
0056 |             return "";
0057 |         }
0058 |         List<MajorExceptionEvent> listBuildEventList = buildEventList(this.mContext);
0059 |         this.mMajorExceptionEventList = listBuildEventList;
0060 |         if (listBuildEventList != null && listBuildEventList.size() < 1) {
0061 |             Utils.logE(TAG, "mMajorExceptionEventList is empty!");
0062 |             return "";
0063 |         }
0064 |         return new Gson().toJson(buildUploadEvent(this.mMajorExceptionEventList, makeKeyParams(getBaseParams(this.mContext, Constants.SALT), getPrivateParams(), Constants.SALT)));
0065 |     }
0066 | 
0067 |     @Override // com.miui.daemon.mqsas.upload.BaseUploader
0068 |     public void uploadToServer(String str, String str2) {
0069 |         int size;
0070 |         List<MajorExceptionEvent> list = this.mMajorExceptionEventList;
0071 |         if (list != null) {
0072 |             size = list.size();
0073 |             if (size < 1) {
0074 |                 Utils.logE(TAG, "mMajorExceptionEventList is empty!");
0075 |                 return;
0076 |             }
0077 |         } else {
0078 |             size = 0;
0079 |         }
0080 |         ArrayList arrayList = new ArrayList();
0081 |         Gson gson = new Gson();
0082 |         for (int i = 0; i < size; i++) {
0083 |             try {
0084 |                 JSONObject jSONObject = new JSONObject(gson.toJson(this.mMajorExceptionEventList.get(i)));
0085 |                 jSONObject.put(Constants.EVENT_NAME, "release");
0086 |                 OnetrackUtils.getInstance(this.mContext);
0087 |                 OnetrackUtils.addKeyParams(jSONObject, "mqs_data");
0088 |                 Utils.logD(TAG, "jsonObject.toString(): " + jSONObject.toString());
0089 |                 arrayList.add(jSONObject.toString());
0090 |             } catch (RemoteException e) {
0091 |                 e.printStackTrace();
0092 |                 Utils.logE(TAG, "stability onetrack RemoteException " + e.getMessage());
0093 |                 return;
0094 |             } catch (JSONException e2) {
0095 |                 e2.printStackTrace();
0096 |                 Utils.logE(TAG, "stability json exception " + e2.getMessage());
0097 |                 return;
0098 |             } catch (Exception e3) {
0099 |                 e3.printStackTrace();
0100 |                 Utils.logE(TAG, "stability onetrack unknown Exception " + e3.getMessage());
0101 |                 return;
0102 |             }
0103 |         }
0104 |         BaseDaemonApplication.mMQSService.trackEvents(OnetrackUtils.APP_ID_STABILITY, "com.miui.daemon", arrayList, OnetrackUtils.FLAG_ONE_TRACK);
0105 |         onUploadDone();
0106 |         Utils.logD(TAG, "release exceptions has been uploaded with onetrack.");
0107 |     }
0108 | 
0109 |     @Override // com.miui.daemon.mqsas.upload.BaseUploader
0110 |     public void onUploadDone() {
0111 |         Utils.logD(TAG, "Upload done.");
0112 |         updateDB(this.mMajorExceptionEventList);
0113 |     }
0114 | 
0115 |     private List<MajorExceptionEvent> buildEventList(Context context) {
0116 |         ArrayList arrayList = new ArrayList();
0117 |         arrayList.addAll(DatabaseUtils.getEventListFromDB(this.mContext.getApplicationContext(), 8, MtbfEventUploader.CONDITION, new String[]{"0"}, String.format("_id limit %d", 30)));
0118 |         arrayList.addAll(DatabaseUtils.getEventListFromDB(this.mContext.getApplicationContext(), 1, MtbfEventUploader.CONDITION, new String[]{"0"}, String.format("_id limit %d", 30)));
0119 |         arrayList.addAll(DatabaseUtils.getEventListFromDB(this.mContext.getApplicationContext(), 2, MtbfEventUploader.CONDITION, new String[]{"0"}, String.format("_id limit %d", 30)));
0120 |         arrayList.addAll(DatabaseUtils.getEventListFromDB(this.mContext.getApplicationContext(), 4, MtbfEventUploader.CONDITION, new String[]{"0"}, String.format("_id limit %d", 30)));
0121 |         arrayList.addAll(DatabaseUtils.getEventListFromDB(this.mContext.getApplicationContext(), 5, MtbfEventUploader.CONDITION, new String[]{"0"}, String.format("_id limit %d", 30)));
0122 |         arrayList.addAll(DatabaseUtils.getEventListFromDB(this.mContext.getApplicationContext(), 386, MtbfEventUploader.CONDITION, new String[]{"0"}, "_id limit 30"));
0123 |         arrayList.addAll(DatabaseUtils.getFeatureEventListFromDatabase(this.mContext.getApplicationContext(), MtbfEventUploader.CONDITION, new String[]{"0"}, "_id limit 30"));
0124 |         arrayList.addAll(DatabaseUtils.getEventListFromDB(this.mContext.getApplicationContext(), 414, MtbfEventUploader.CONDITION, new String[]{"0"}, String.format("_id limit %d", 30)));
0125 |         arrayList.addAll(DatabaseUtils.getEventListFromDB(this.mContext.getApplicationContext(), 403, MtbfEventUploader.CONDITION, new String[]{"0"}, String.format("_id limit %d", 30)));
0126 |         arrayList.addAll(DatabaseUtils.getEventListFromDB(this.mContext.getApplicationContext(), 10, MtbfEventUploader.CONDITION, new String[]{"0"}, String.format("_id limit %d", 30)));
0127 |         arrayList.addAll(DatabaseUtils.getEventListFromDB(this.mContext.getApplicationContext(), 6, MtbfEventUploader.CONDITION, new String[]{"0"}, String.format("_id limit %d", 30)));
0128 |         arrayList.addAll(DatabaseUtils.getEventListFromDB(this.mContext.getApplicationContext(), 416, MtbfEventUploader.CONDITION, new String[]{"0"}, String.format("_id limit %d", 30)));
0129 |         arrayList.addAll(DatabaseUtils.getEventListFromDB(this.mContext.getApplicationContext(), 418, MtbfEventUploader.CONDITION, new String[]{"0"}, String.format("_idlimit %d", 30)));
0130 |         arrayList.addAll(DatabaseUtils.getEventListFromDB(this.mContext.getApplicationContext(), 417, MtbfEventUploader.CONDITION, new String[]{"0"}, String.format("_idlimit %d", 30)));
0131 |         arrayList.addAll(DatabaseUtils.getEventListFromDB(this.mContext.getApplicationContext(), 421, MtbfEventUploader.CONDITION, new String[]{"0"}, String.format("_id limit %d", 30)));
0132 |         arrayList.addAll(DatabaseUtils.getEventListFromDB(this.mContext.getApplicationContext(), 420, MtbfEventUploader.CONDITION, new String[]{"0"}, String.format("_id limit %d", 30)));
0133 |         arrayList.addAll(DatabaseUtils.getGeneralEventListFromDB(MtbfEventUploader.CONDITION, new String[]{"0"}, String.format("_id limit %d", 30)));
0134 |         arrayList.addAll(DatabaseUtils.getEventListFromDB(this.mContext.getApplicationContext(), 422, MtbfEventUploader.CONDITION, new String[]{"0"}, String.format("_id limit %d", 30)));
0135 |         dumpMajorExceptionEventList(arrayList, "");
0136 |         return arrayList;
0137 |     }
0138 | 
0139 |     private UploadEvent buildUploadEvent(List<MajorExceptionEvent> list, Map<String, String> map) {
0140 |         UploadEvent uploadEvent = new UploadEvent();
0141 |         uploadEvent.setReportId(map.get(Constants.PARAM_KEY_REPORT_ID));
0142 |         uploadEvent.setA(map.get(Constants.PARAM_KEY_ANDROID_VERSION));
0143 |         uploadEvent.setD(map.get(Constants.PARAM_KEY_DEVICE));
0144 |         uploadEvent.setC(map.get(Constants.PARAM_KEY_CARRIER));
0145 |         uploadEvent.setR(map.get(Constants.PARAM_KEY_REGION));
0146 |         uploadEvent.setL(map.get("l"));
0147 |         uploadEvent.setV(map.get(Constants.PARAM_KEY_MIUI_VERSION));
0148 |         uploadEvent.setT(map.get(Constants.PARAM_KEY_VERSION_TYPE));
0149 |         uploadEvent.setE(map.get(Constants.PARAM_KEY_CLIENT_VERSION));
0150 |         uploadEvent.setMi(map.get("mi"));
0151 |         uploadEvent.setI("");
0152 |         uploadEvent.setM(map.get(Constants.PARAM_KEY_IMEI));
0153 |         uploadEvent.setDate(map.get(Constants.PARAM_KEY_DATE));
0154 |         uploadEvent.setModule(map.get("module"));
0155 |         uploadEvent.setSign(map.get(Constants.PARAM_KEY_SIGN));
0156 |         uploadEvent.setMajorExceptionEvents(list);
0157 |         return uploadEvent;
0158 |     }
0159 | 
0160 |     private void updateDB(List<MajorExceptionEvent> list) {
0161 |         for (MajorExceptionEvent majorExceptionEvent : list) {
0162 |             Class classByType = Utils.getClassByType(majorExceptionEvent.getTy());
0163 |             String id = majorExceptionEvent.getId();
0164 |             if (classByType != null) {
0165 |                 DatabaseUtils.deleteUploadedEventDB(classByType, "_id=?", new String[]{id});
0166 |             }
0167 |         }
0168 |     }
0169 | 
0170 |     private void dumpMajorExceptionEventList(List<MajorExceptionEvent> list, String str) {
0171 |         StringBuilder sb = new StringBuilder();
0172 |         sb.append("########### MajorExceptionEvent(" + str + ") LIST BEGIN#################\n");
0173 |         if (list != null) {
0174 |             for (int i = 0; i < list.size(); i++) {
0175 |                 sb.append("MajorExceptionEvent[" + i + "]:" + list.get(i).toShortString() + "\n");
0176 |             }
0177 |         }
0178 |         sb.append("########### MajorExceptionEvent(" + str + ") LIST END#################\n");
0179 |         Utils.logD(TAG, sb.toString());
0180 |     }
```

## D11

Source: `sources/com/miui/daemon/mqsas/upload/HeartBeatUploader.java`

Source-file SHA-256: `943a893a0a2c60f7a713b2d6df3d5521717e3a829c5a1e30731cde50aee006f6`

Original lines 1–275:

```text
0001 | package com.miui.daemon.mqsas.upload;
0002 | 
0003 | import android.content.BroadcastReceiver;
0004 | import android.content.Context;
0005 | import android.content.Intent;
0006 | import android.content.IntentFilter;
0007 | import android.os.PowerManager;
0008 | import android.os.RemoteException;
0009 | import android.os.SystemClock;
0010 | import com.miui.daemon.BaseDaemonApplication;
0011 | import com.miui.daemon.mqsas.db.model.NewScreenonModel;
0012 | import com.miui.daemon.mqsas.providers.DatabaseUtils;
0013 | import com.miui.daemon.mqsas.utils.DeviceUtil;
0014 | import com.miui.daemon.mqsas.utils.OnetrackUtils;
0015 | import com.miui.daemon.mqsas.utils.ThreadPool;
0016 | import com.miui.daemon.mqsas.utils.Utils;
0017 | import java.util.ArrayList;
0018 | import java.util.Iterator;
0019 | import java.util.List;
0020 | import org.json.JSONException;
0021 | import org.json.JSONObject;
0022 | 
0023 | /* JADX INFO: loaded from: classes.dex */
0024 | public class HeartBeatUploader extends BaseUploaderV2<MajorExceptionEventV2> {
0025 |     private static final String TAG = "HeartBeatUploader";
0026 |     private static HeartBeatUploader sInstance;
0027 |     private volatile long APRScreenonTime;
0028 |     private volatile long endBootTime;
0029 |     private volatile long endScreenonTime;
0030 |     private BaseDaemonApplication mApplication;
0031 |     private PowerManager mPowerManager;
0032 |     private ScreenMonitorReceiver mScreenMonitorReceiver;
0033 |     private volatile long startBootTime;
0034 |     private volatile long startScreenonTime;
0035 |     private volatile long totalBootTime;
0036 |     private volatile long totalScreenonTime;
0037 | 
0038 |     @Override // com.miui.daemon.mqsas.upload.BaseUploaderV2
0039 |     public String getAppId() {
0040 |         return "mqs_five_exception_data_73421000";
0041 |     }
0042 | 
0043 |     @Override // com.miui.daemon.mqsas.upload.BaseUploaderV2
0044 |     public boolean isGlobalNeed() {
0045 |         return true;
0046 |     }
0047 | 
0048 |     public static synchronized HeartBeatUploader getInstance(Context context) {
0049 |         if (sInstance == null) {
0050 |             sInstance = new HeartBeatUploader(context);
0051 |         }
0052 |         return sInstance;
0053 |     }
0054 | 
0055 |     private HeartBeatUploader(Context context) {
0056 |         super(context);
0057 |         this.startScreenonTime = 0L;
0058 |         this.endScreenonTime = 0L;
0059 |         this.totalScreenonTime = 0L;
0060 |         this.startBootTime = 0L;
0061 |         this.endBootTime = 0L;
0062 |         this.totalBootTime = 0L;
0063 |         this.APRScreenonTime = 0L;
0064 |         Context applicationContext = context.getApplicationContext();
0065 |         this.mPowerManager = (PowerManager) applicationContext.getSystemService("power");
0066 |         this.mScreenMonitorReceiver = new ScreenMonitorReceiver();
0067 |         registerScreenLockReceiver(applicationContext);
0068 |         this.mApplication = (BaseDaemonApplication) applicationContext.getApplicationContext();
0069 |         initTimeParams();
0070 |     }
0071 | 
0072 |     @Override // com.miui.daemon.mqsas.upload.BaseUploaderV2
0073 |     public void onUploadDone(List<MajorExceptionEventV2> list) {
0074 |         Utils.logD(TAG, "Heartbeat upload.");
0075 |     }
0076 | 
0077 |     @Override // com.miui.daemon.mqsas.upload.BaseUploaderV2
0078 |     public void requestUpload() {
0079 |         synchronized (this) {
0080 |             getFinalTotalTime();
0081 |             this.APRScreenonTime = this.totalScreenonTime;
0082 |             if (DeviceUtil.isUnReleased()) {
0083 |                 try {
0084 |                     try {
0085 |                         JSONObject jSONObject = new JSONObject();
0086 |                         jSONObject.put(Constants.EVENT_NAME, "screenon_unrelease");
0087 |                         jSONObject.put("screen_on_duration", this.totalScreenonTime);
0088 |                         jSONObject.put("boot_duration", this.totalBootTime);
0089 |                         jSONObject.put(Constants.SCREEN_ON_SERIAL_NO, DeviceUtil.SERIAL_NO);
0090 |                         jSONObject.put(Constants.DEVICE_NAME, DeviceUtil.DEVICE);
0091 |                         Utils.logD(TAG, "unrelease screenon upload data: " + jSONObject.toString(), Boolean.TRUE);
0092 |                         BaseDaemonApplication.mMQSService.trackEvent(OnetrackUtils.APP_ID_STABILITY, "com.miui.daemon", jSONObject.toString(), 2);
0093 |                         initTimeParams();
0094 |                     } catch (Exception e) {
0095 |                         e.printStackTrace();
0096 |                         Utils.logE(TAG, "unrelease screenon uploader unknown Exception " + e.getMessage());
0097 |                     }
0098 |                 } catch (RemoteException e2) {
0099 |                     e2.printStackTrace();
0100 |                     Utils.logE(TAG, "unrelease screenon upload RemoteException" + e2.getMessage());
0101 |                 } catch (JSONException e3) {
0102 |                     e3.printStackTrace();
0103 |                     Utils.logE(TAG, "unrelease screenon upload exception " + e3.getMessage());
0104 |                 }
0105 |                 super.requestUpload();
0106 |             } else {
0107 |                 Utils.logD(TAG, "store new screenon data.", Boolean.TRUE);
0108 |                 DatabaseUtils.storeNewScreenonEvent(this.mContext, new NewScreenonModel(this.totalScreenonTime, this.totalBootTime, DeviceUtil.ONE_POWERUP_UUID));
0109 |                 initTimeParams();
0110 |             }
0111 |         }
0112 |     }
0113 | 
0114 |     public synchronized void uploadScreenonData() {
0115 |         int size;
0116 |         if (DeviceUtil.isUnReleased()) {
0117 |             return;
0118 |         }
0119 |         List<NewScreenonModel> newScreenonModelsFromDB = DatabaseUtils.getNewScreenonModelsFromDB();
0120 |         if (newScreenonModelsFromDB == null || (size = newScreenonModelsFromDB.size()) == 0) {
0121 |             Utils.logW(TAG, "new screenon model do not exist.");
0122 |             return;
0123 |         }
0124 |         try {
0125 |             try {
0126 |                 ArrayList arrayList = new ArrayList();
0127 |                 for (int i = 0; i < size; i++) {
0128 |                     JSONObject jSONObject = new JSONObject();
0129 |                     OnetrackUtils.getInstance(this.mContext);
0130 |                     OnetrackUtils.addKeyParams(jSONObject, "mqs_data");
0131 |                     jSONObject.put("screen_on_duration", newScreenonModelsFromDB.get(i).getScreen_on_duration());
0132 |                     jSONObject.put("boot_duration", newScreenonModelsFromDB.get(i).getBoot_duration());
0133 |                     jSONObject.put(Constants.EVENT_NAME, "screenon_release");
0134 |                     arrayList.add(jSONObject.toString());
0135 |                 }
0136 |                 Utils.logD(TAG, "uploadScreenonData, upload dataList: " + arrayList, Boolean.TRUE);
0137 |                 BaseDaemonApplication.mMQSService.trackEvents(OnetrackUtils.APP_ID_STABILITY, "com.miui.daemon", arrayList, 2);
0138 |                 updateDB(newScreenonModelsFromDB);
0139 |             } catch (Exception e) {
0140 |                 e.printStackTrace();
0141 |                 Utils.logE(TAG, "uploadScreenonData, uploader unknown Exception " + e.getMessage());
0142 |             }
0143 |         } catch (RemoteException e2) {
0144 |             e2.printStackTrace();
0145 |             Utils.logE(TAG, "uploadScreenonData, upload RemoteException" + e2.getMessage());
0146 |         } catch (JSONException e3) {
0147 |             e3.printStackTrace();
0148 |             Utils.logE(TAG, "uploadScreenonData, upload exception " + e3.getMessage());
0149 |         }
0150 |     }
0151 | 
0152 |     private void updateDB(List<NewScreenonModel> list) {
0153 |         Iterator<NewScreenonModel> it = list.iterator();
0154 |         while (it.hasNext()) {
0155 |             String strValueOf = String.valueOf(it.next().get_id());
0156 |             Utils.logW(TAG, "new Screenon delete data. _id: " + strValueOf);
0157 |             DatabaseUtils.deleteNewScreenonModelsFromDB("_id=?", new String[]{strValueOf});
0158 |         }
0159 |     }
0160 | 
0161 |     @Override // com.miui.daemon.mqsas.upload.BaseUploaderV2
0162 |     public List<MajorExceptionEventV2> buildEvents(CommonUploadEvent<MajorExceptionEventV2> commonUploadEvent) {
0163 |         ArrayList arrayList = new ArrayList();
0164 |         MajorExceptionEventV2 majorExceptionEventV2 = new MajorExceptionEventV2();
0165 |         majorExceptionEventV2.android_version = DeviceUtil.ANDROID_VERSION;
0166 |         majorExceptionEventV2.carrier = DeviceUtil.CARRIER;
0167 |         majorExceptionEventV2.model = DeviceUtil.MODEL;
0168 |         majorExceptionEventV2.ty = "heartbeat";
0169 |         majorExceptionEventV2.det = "";
0170 |         majorExceptionEventV2.dgt = "";
0171 |         majorExceptionEventV2.log = "";
0172 |         majorExceptionEventV2.pkn = "";
0173 |         majorExceptionEventV2.prn = "";
0174 |         majorExceptionEventV2.sum = "";
0175 |         majorExceptionEventV2.ts = String.valueOf(System.currentTimeMillis());
0176 |         majorExceptionEventV2.id = "";
0177 |         majorExceptionEventV2.kw = generateKw();
0178 |         arrayList.add(majorExceptionEventV2);
0179 |         return arrayList;
0180 |     }
0181 | 
0182 |     private String generateKw() {
0183 |         JSONObject jSONObject = new JSONObject();
0184 |         try {
0185 |             jSONObject.put(Constants.KEY_WORD_KEY_SERIAL_NO, DeviceUtil.SERIAL_NO);
0186 |             jSONObject.put(Constants.KEY_WORD_KEY_IMEI_1, DeviceUtil.getImei1());
0187 |             jSONObject.put(Constants.KEY_WORD_KEY_IMEI_2, DeviceUtil.getImei2());
0188 |             jSONObject.put(Constants.KEY_WORD_KEY_MI_ACCOUNT, DeviceUtil.getAccountId(this.mContext));
0189 |             jSONObject.put("uuid", DeviceUtil.getUUID(this.mContext));
0190 |             jSONObject.put(Constants.ONE_BOOT_UUID, DeviceUtil.ONE_POWERUP_UUID);
0191 |             jSONObject.put("screen_on_duration", this.APRScreenonTime);
0192 |         } catch (JSONException e) {
0193 |             e.printStackTrace();
0194 |         }
0195 |         return jSONObject.toString();
0196 |     }
0197 | 
0198 |     private synchronized void initTimeParams() {
0199 |         Utils.logD(TAG, String.format("totalScreenonTime = %s startScreenonTime = %s endScreenonTime =%s ", Long.valueOf(this.totalScreenonTime), Long.valueOf(this.startScreenonTime), Long.valueOf(this.endScreenonTime)), Boolean.TRUE);
0200 |         this.totalScreenonTime = 0L;
0201 |         this.endScreenonTime = 0L;
0202 |         long jElapsedRealtime = SystemClock.elapsedRealtime();
0203 |         if (this.mPowerManager.isInteractive()) {
0204 |             this.startScreenonTime = jElapsedRealtime;
0205 |         } else {
0206 |             this.startScreenonTime = 0L;
0207 |         }
0208 |         this.startBootTime = jElapsedRealtime;
0209 |     }
0210 | 
0211 |     /* JADX INFO: Access modifiers changed from: private */
0212 |     public void acceptedScreenOnEvent() {
0213 |         this.startScreenonTime = SystemClock.elapsedRealtime();
0214 |     }
0215 | 
0216 |     /* JADX INFO: Access modifiers changed from: private */
0217 |     public void acceptedScreenOffEvent() {
0218 |         if (this.startScreenonTime == 0) {
0219 |             return;
0220 |         }
0221 |         this.endScreenonTime = SystemClock.elapsedRealtime();
0222 |         this.totalScreenonTime += this.endScreenonTime - this.startScreenonTime;
0223 |         this.endScreenonTime = 0L;
0224 |         this.startScreenonTime = 0L;
0225 |     }
0226 | 
0227 |     private void getFinalTotalTime() {
0228 |         long jElapsedRealtime = SystemClock.elapsedRealtime();
0229 |         if (this.mPowerManager.isInteractive() && this.startScreenonTime != 0) {
0230 |             this.totalScreenonTime += jElapsedRealtime - this.startScreenonTime;
0231 |             this.startScreenonTime = jElapsedRealtime;
0232 |             Utils.logD(TAG, "getFinalTotalTime before upload, totalScreenonTime = " + this.totalScreenonTime, Boolean.TRUE);
0233 |         }
0234 |         this.endBootTime = jElapsedRealtime;
0235 |         this.totalBootTime = this.endBootTime - this.startBootTime;
0236 |         this.startBootTime = this.endBootTime;
0237 |         Utils.logD(TAG, "getFinalTotalTime before upload, totalBootTime = " + this.totalBootTime, Boolean.TRUE);
0238 |     }
0239 | 
0240 |     private void registerScreenLockReceiver(Context context) {
0241 |         IntentFilter intentFilter = new IntentFilter();
0242 |         intentFilter.addAction("android.intent.action.SCREEN_OFF");
0243 |         intentFilter.addAction("android.intent.action.SCREEN_ON");
0244 |         intentFilter.addAction("android.intent.action.ACTION_SHUTDOWN");
0245 |         context.registerReceiver(this.mScreenMonitorReceiver, intentFilter);
0246 |     }
0247 | 
0248 |     public class ScreenMonitorReceiver extends BroadcastReceiver {
0249 |         public ScreenMonitorReceiver() {
0250 |         }
0251 | 
0252 |         @Override // android.content.BroadcastReceiver
0253 |         public void onReceive(Context context, Intent intent) {
0254 |             if (intent == null) {
0255 |                 return;
0256 |             }
0257 |             String action = intent.getAction();
0258 |             if ("android.intent.action.SCREEN_ON".equals(action)) {
0259 |                 Utils.logD(HeartBeatUploader.TAG, "screen on");
0260 |                 HeartBeatUploader.this.acceptedScreenOnEvent();
0261 |             } else if ("android.intent.action.SCREEN_OFF".equals(action)) {
0262 |                 Utils.logD(HeartBeatUploader.TAG, "screen off");
0263 |                 HeartBeatUploader.this.acceptedScreenOffEvent();
0264 |             } else if ("android.intent.action.ACTION_SHUTDOWN".equals(action)) {
0265 |                 Utils.logD(HeartBeatUploader.TAG, "shut down");
0266 |                 ThreadPool.execute(new Runnable() { // from class: com.miui.daemon.mqsas.upload.HeartBeatUploader.ScreenMonitorReceiver.1
0267 |                     @Override // java.lang.Runnable
0268 |                     public void run() {
0269 |                         HeartBeatUploader.this.requestUpload();
0270 |                     }
0271 |                 });
0272 |             }
0273 |         }
0274 |     }
0275 | }
```

## D12

Source: `sources/com/miui/daemon/mqsas/upload/FingerprintUploader.java`

Source-file SHA-256: `11217b6e2de5f0b42556fdcd0c08d4bf3407a389597a867a37e04f840a5fb39d`

Original lines 1–210:

```text
0001 | package com.miui.daemon.mqsas.upload;
0002 | 
0003 | import android.content.Context;
0004 | import com.google.gson.Gson;
0005 | import com.litesuits.orm.db.assit.SQLBuilder;
0006 | import com.miui.daemon.mqsas.providers.DatabaseUtils;
0007 | import com.miui.daemon.mqsas.providers.MQSProviderContract;
0008 | import com.miui.daemon.mqsas.utils.DeviceUtil;
0009 | import com.miui.daemon.mqsas.utils.Utils;
0010 | import java.util.ArrayList;
0011 | import java.util.HashMap;
0012 | import java.util.List;
0013 | import java.util.Map;
0014 | 
0015 | /* JADX INFO: loaded from: classes.dex */
0016 | public class FingerprintUploader extends BaseUploader {
0017 |     private static int MAX_UPLOAD_RECORD = 500;
0018 |     private static String MODULE = "fingerprint";
0019 |     private static final String TAG = "FingerprintUploader";
0020 |     private static FingerprintUploader sInstance;
0021 | 
0022 |     @Override // com.miui.daemon.mqsas.upload.BaseUploader
0023 |     public /* bridge */ /* synthetic */ void requestUpload() {
0024 |         super.requestUpload();
0025 |     }
0026 | 
0027 |     private FingerprintUploader(Context context) {
0028 |         super(context);
0029 |     }
0030 | 
0031 |     public static synchronized FingerprintUploader getInstance(Context context) {
0032 |         if (sInstance == null) {
0033 |             sInstance = new FingerprintUploader(context);
0034 |         }
0035 |         return sInstance;
0036 |     }
0037 | 
0038 |     @Override // com.miui.daemon.mqsas.upload.BaseUploader
0039 |     public String getUploadUrl() {
0040 |         return Constants.MQSAS_FINGER_URL;
0041 |     }
0042 | 
0043 |     @Override // com.miui.daemon.mqsas.upload.BaseUploader
0044 |     public Map<String, String> getPrivateParams() {
0045 |         HashMap map = new HashMap();
0046 |         map.put(Constants.PARAM_KEY_FID_DEVICE, DeviceUtil.getDeviceFid());
0047 |         return map;
0048 |     }
0049 | 
0050 |     @Override // com.miui.daemon.mqsas.upload.BaseUploader
0051 |     public String getModuleName() {
0052 |         return MODULE;
0053 |     }
0054 | 
0055 |     @Override // com.miui.daemon.mqsas.upload.BaseUploader
0056 |     public String buildUploadBody() {
0057 |         Gson gson = new Gson();
0058 |         List<FingerRecord> listBuildFingerEvent = buildFingerEvent();
0059 |         if (listBuildFingerEvent == null || listBuildFingerEvent.size() <= 0) {
0060 |             return "";
0061 |         }
0062 |         return "{\"events\":" + gson.toJson(listBuildFingerEvent) + "}";
0063 |     }
0064 | 
0065 |     @Override // com.miui.daemon.mqsas.upload.BaseUploader
0066 |     public void onUploadDone() {
0067 |         Utils.logD(TAG, "Upload done.");
0068 |         DatabaseUtils.deleteSimpleEventListFromDatabase(this.mContext.getApplicationContext(), MQSProviderContract.SimpleDataRule.CONTENT_URI, "type=?", new String[]{"16"});
0069 |     }
0070 | 
0071 |     private List<FingerRecord> buildFingerEvent() {
0072 |         List<String> simpleEventListFromDatabase = DatabaseUtils.getSimpleEventListFromDatabase(this.mContext.getApplicationContext(), MQSProviderContract.SimpleDataRule.CONTENT_URI, "type=?", new String[]{"16"});
0073 |         ArrayList arrayList = new ArrayList();
0074 |         for (int i = 0; i < simpleEventListFromDatabase.size(); i++) {
0075 |             String[] strArrSplit = simpleEventListFromDatabase.get(i).split(",");
0076 |             if (strArrSplit.length == 3) {
0077 |                 Utils.logD(TAG, "getUploadBody line:" + strArrSplit[0] + SQLBuilder.BLANK + strArrSplit[1] + SQLBuilder.BLANK + strArrSplit[2]);
0078 |                 arrayList.add(new FingerRecord(strArrSplit[0], strArrSplit[1], strArrSplit[2]));
0079 |             }
0080 |             if (strArrSplit.length == 8) {
0081 |                 Utils.logD(TAG, "getUploadBody line:" + strArrSplit[0] + SQLBuilder.BLANK + strArrSplit[1] + SQLBuilder.BLANK + strArrSplit[2] + SQLBuilder.BLANK + strArrSplit[3] + SQLBuilder.BLANK + strArrSplit[4] + SQLBuilder.BLANK + strArrSplit[5] + SQLBuilder.BLANK + strArrSplit[6] + SQLBuilder.BLANK + strArrSplit[7]);
0082 |                 arrayList.add(new FingerRecord(simpleEventListFromDatabase.get(i), false));
0083 |             }
0084 |             if (strArrSplit.length == 9) {
0085 |                 Utils.logD(TAG, "getUploadBody line:" + strArrSplit[0] + SQLBuilder.BLANK + strArrSplit[1] + SQLBuilder.BLANK + strArrSplit[2] + SQLBuilder.BLANK + strArrSplit[3] + SQLBuilder.BLANK + strArrSplit[4] + SQLBuilder.BLANK + strArrSplit[5] + SQLBuilder.BLANK + strArrSplit[6] + SQLBuilder.BLANK + strArrSplit[7] + SQLBuilder.BLANK + strArrSplit[8]);
0086 |                 arrayList.add(new FingerRecord(simpleEventListFromDatabase.get(i), true));
0087 |             }
0088 |             if (i > MAX_UPLOAD_RECORD) {
0089 |                 Utils.logE(TAG, "just upload 500 records, others will delete directly.");
0090 |                 break;
0091 |             }
0092 |         }
0093 |         return arrayList;
0094 |     }
0095 | 
0096 |     public class FingerRecord {
0097 |         String authToUnlock;
0098 |         String callingPkn;
0099 |         String callingTime;
0100 |         String captureToAuth;
0101 |         String fingerDownCount;
0102 |         String fingerDownToCapture;
0103 |         String isScreenOn;
0104 |         String isSuccess;
0105 |         String onAuthenCount;
0106 |         String startCaptureCount;
0107 |         String status;
0108 |         String unlockCount;
0109 | 
0110 |         public FingerRecord() {
0111 |         }
0112 | 
0113 |         public FingerRecord(String str, String str2, String str3) {
0114 |             this.callingPkn = str;
0115 |             this.status = str2;
0116 |             this.callingTime = str3;
0117 |         }
0118 | 
0119 |         public FingerRecord(String str, boolean z) {
0120 |             String[] strArrSplit = str.split(",");
0121 |             this.callingPkn = strArrSplit[0];
0122 |             this.status = strArrSplit[1];
0123 |             this.onAuthenCount = "1";
0124 |             if (z) {
0125 |                 this.fingerDownCount = strArrSplit[2];
0126 |                 this.startCaptureCount = strArrSplit[3];
0127 |                 this.fingerDownToCapture = strArrSplit[4];
0128 |                 this.captureToAuth = strArrSplit[5];
0129 |                 this.authToUnlock = strArrSplit[6];
0130 |                 this.isScreenOn = strArrSplit[7];
0131 |                 this.callingTime = strArrSplit[8];
0132 |                 this.isSuccess = "1";
0133 |                 this.unlockCount = "1";
0134 |                 return;
0135 |             }
0136 |             this.fingerDownCount = strArrSplit[2];
0137 |             this.startCaptureCount = strArrSplit[3];
0138 |             this.fingerDownToCapture = "0";
0139 |             this.captureToAuth = "0";
0140 |             this.isScreenOn = strArrSplit[6];
0141 |             this.callingTime = strArrSplit[7];
0142 |             this.isSuccess = "0";
0143 |             this.unlockCount = "0";
0144 |         }
0145 | 
0146 |         public void setCallingPkn(String str) {
0147 |             this.callingPkn = str;
0148 |         }
0149 | 
0150 |         public String getCallingPkn() {
0151 |             return this.callingPkn;
0152 |         }
0153 | 
0154 |         public void setStatus(String str) {
0155 |             this.status = str;
0156 |         }
0157 | 
0158 |         public String getStatus() {
0159 |             return this.status;
0160 |         }
0161 | 
0162 |         public String getFingerDownCount() {
0163 |             return this.fingerDownCount;
0164 |         }
0165 | 
0166 |         public void setFingerDownCount(String str) {
0167 |             this.fingerDownCount = str;
0168 |         }
0169 | 
0170 |         public String getStartCaptureCount() {
0171 |             return this.startCaptureCount;
0172 |         }
0173 | 
0174 |         public void setStartCaptureCount(String str) {
0175 |             this.startCaptureCount = str;
0176 |         }
0177 | 
0178 |         public String getFingerDownToCapture() {
0179 |             return this.fingerDownToCapture;
0180 |         }
0181 | 
0182 |         public void setFingerDownToCapture(String str) {
0183 |             this.fingerDownToCapture = str;
0184 |         }
0185 | 
0186 |         public String getCaptureToAuth() {
0187 |             return this.captureToAuth;
0188 |         }
0189 | 
0190 |         public void setCaptureToAuth(String str) {
0191 |             this.captureToAuth = str;
0192 |         }
0193 | 
0194 |         public String getAuthToUnlock() {
0195 |             return this.authToUnlock;
0196 |         }
0197 | 
0198 |         public void setAuthToUnlock(String str) {
0199 |             this.authToUnlock = str;
0200 |         }
0201 | 
0202 |         public void setCallingTime(String str) {
0203 |             this.callingTime = str;
0204 |         }
0205 | 
0206 |         public String getCallingTime() {
0207 |             return this.callingTime;
0208 |         }
0209 |     }
0210 | }
```

## D13

Source: `sources/com/miui/daemon/mqsas/upload/BiometricInfoUploader.java`

Source-file SHA-256: `b62d230ea43f85693e0bd5937f4ffb838bf97bb358da4042790466006902bb33`

Original lines 1–210:

```text
0001 | package com.miui.daemon.mqsas.upload;
0002 | 
0003 | import android.annotation.SuppressLint;
0004 | import android.content.Context;
0005 | import android.hardware.fingerprint.FingerprintManager;
0006 | import android.os.SystemProperties;
0007 | import android.text.TextUtils;
0008 | import com.google.gson.Gson;
0009 | import com.miui.daemon.mqsas.utils.DateUtil;
0010 | import com.miui.daemon.mqsas.utils.DeviceUtil;
0011 | import com.miui.daemon.mqsas.utils.Utils;
0012 | import java.util.ArrayList;
0013 | import java.util.List;
0014 | import java.util.Map;
0015 | 
0016 | /* JADX INFO: loaded from: classes.dex */
0017 | public class BiometricInfoUploader extends BaseUploader {
0018 |     public static boolean IS_FIRST_UPLOAD = true;
0019 |     private static final String MODULE = "biometricInfo";
0020 |     private static final String TAG = "BiometricInfoUploader";
0021 |     private static BiometricInfoUploader sInstance;
0022 |     private final FingerprintManager mFingerprintManager;
0023 | 
0024 |     @Override // com.miui.daemon.mqsas.upload.BaseUploader
0025 |     public String getModuleName() {
0026 |         return MODULE;
0027 |     }
0028 | 
0029 |     private BiometricInfoUploader(Context context) {
0030 |         super(context);
0031 |         this.mFingerprintManager = (FingerprintManager) context.getSystemService(FingerprintManager.class);
0032 |     }
0033 | 
0034 |     public static synchronized BiometricInfoUploader getInstance(Context context) {
0035 |         if (sInstance == null) {
0036 |             sInstance = new BiometricInfoUploader(context);
0037 |         }
0038 |         return sInstance;
0039 |     }
0040 | 
0041 |     @Override // com.miui.daemon.mqsas.upload.BaseUploader
0042 |     public String getUploadUrl() {
0043 |         return UrlConstants.getUrl(MODULE, true);
0044 |     }
0045 | 
0046 |     @Override // com.miui.daemon.mqsas.upload.BaseUploader
0047 |     public String buildUploadBody() {
0048 |         List<BiometricInfo> listBuildBiometricInfoList = buildBiometricInfoList();
0049 |         if (listBuildBiometricInfoList == null || listBuildBiometricInfoList.isEmpty()) {
0050 |             Utils.logE(TAG, "biometricInfoList is empty!");
0051 |             return "";
0052 |         }
0053 |         BiometricInfoEvent biometricInfoEventBuildUploadEvent = buildUploadEvent(listBuildBiometricInfoList, getBaseParams(this.mContext, Constants.SALT));
0054 |         if (biometricInfoEventBuildUploadEvent.getEvents() == null || biometricInfoEventBuildUploadEvent.getEvents().isEmpty()) {
0055 |             Utils.logE(TAG, "eventList is empty!");
0056 |             return "";
0057 |         }
0058 |         return new Gson().toJson(biometricInfoEventBuildUploadEvent);
0059 |     }
0060 | 
0061 |     @Override // com.miui.daemon.mqsas.upload.BaseUploader
0062 |     public void requestUpload() {
0063 |         if (IS_FIRST_UPLOAD && isNeedUpload()) {
0064 |             String uploadUrl = getUploadUrl();
0065 |             Utils.logD(TAG, "url : " + uploadUrl);
0066 |             String strBuildUploadBody = buildUploadBody();
0067 |             Utils.logD(TAG, "length:" + strBuildUploadBody.length() + " body : " + strBuildUploadBody);
0068 |             uploadToServer(uploadUrl, strBuildUploadBody);
0069 |         }
0070 |     }
0071 | 
0072 |     public BiometricInfoEvent buildUploadEvent(List<BiometricInfo> list, Map<String, String> map) {
0073 |         BiometricInfoEvent biometricInfoEvent = new BiometricInfoEvent();
0074 |         biometricInfoEvent.setReportId(map.get(Constants.PARAM_KEY_REPORT_ID));
0075 |         biometricInfoEvent.setAndroidVersion(map.get(Constants.PARAM_KEY_ANDROID_VERSION));
0076 |         biometricInfoEvent.setDevice(map.get(Constants.PARAM_KEY_DEVICE));
0077 |         biometricInfoEvent.setCarrier(map.get(Constants.PARAM_KEY_CARRIER));
0078 |         biometricInfoEvent.setRegion(map.get(Constants.PARAM_KEY_REGION));
0079 |         biometricInfoEvent.setModel(map.get("l"));
0080 |         biometricInfoEvent.setMiuiReleaseVersion(map.get(Constants.PARAM_KEY_MIUI_VERSION));
0081 |         biometricInfoEvent.setReleaseType(map.get(Constants.PARAM_KEY_VERSION_TYPE));
0082 |         biometricInfoEvent.setClientVersion(map.get(Constants.PARAM_KEY_CLIENT_VERSION));
0083 |         biometricInfoEvent.setMi(map.get("mi"));
0084 |         if (DeviceUtil.isUnReleased()) {
0085 |             biometricInfoEvent.setI(DeviceUtil.getAccountId(this.mContext));
0086 |         } else {
0087 |             biometricInfoEvent.setI("");
0088 |         }
0089 |         biometricInfoEvent.setMd5Imei(map.get(Constants.PARAM_KEY_IMEI));
0090 |         biometricInfoEvent.setDate(map.get(Constants.PARAM_KEY_DATE));
0091 |         biometricInfoEvent.setTmodule(map.get("module"));
0092 |         biometricInfoEvent.setSign(map.get(Constants.PARAM_KEY_SIGN));
0093 |         biometricInfoEvent.setEvents(list);
0094 |         return biometricInfoEvent;
0095 |     }
0096 | 
0097 |     private List<BiometricInfo> buildBiometricInfoList() {
0098 |         ArrayList arrayList = new ArrayList();
0099 |         if (isFingerprintFailed()) {
0100 |             BiometricInfo biometricInfo = new BiometricInfo();
0101 |             String deviceFid = DeviceUtil.getDeviceFid();
0102 |             if (TextUtils.isEmpty(deviceFid)) {
0103 |                 deviceFid = "";
0104 |             }
0105 |             biometricInfo.setFid(deviceFid);
0106 |             biometricInfo.setFailType("FingerFailed");
0107 |             biometricInfo.setExt("");
0108 |             biometricInfo.setPropInfo(getPropInfo());
0109 |             biometricInfo.setFailTime(DateUtil.timestamp2String(System.currentTimeMillis()));
0110 |             arrayList.add(biometricInfo);
0111 |         }
0112 |         return arrayList;
0113 |     }
0114 | 
0115 |     private String getPropInfo() {
0116 |         StringBuilder sb = new StringBuilder();
0117 |         String str = SystemProperties.get("persist.vendor.sys.fp.vendor", "");
0118 |         String str2 = SystemProperties.get("persist.vendor.sys.fp.info", "");
0119 |         String str3 = SystemProperties.get("persist.vendor.sys.fp.uid", "");
0120 |         sb.append(str);
0121 |         sb.append("##");
0122 |         sb.append(str2);
0123 |         sb.append("##");
0124 |         sb.append(str3);
0125 |         return sb.toString();
0126 |     }
0127 | 
0128 |     @SuppressLint({"MissingPermission"})
0129 |     private boolean isFingerprintFailed() {
0130 |         FingerprintManager fingerprintManager = this.mFingerprintManager;
0131 |         if (fingerprintManager != null) {
0132 |             return !fingerprintManager.isHardwareDetected();
0133 |         }
0134 |         Utils.logE(TAG, "mFingerprintManager == null");
0135 |         return true;
0136 |     }
0137 | 
0138 |     private boolean isFingerprintSuccess() {
0139 |         return !isFingerprintFailed();
0140 |     }
0141 | 
0142 |     private boolean isNeedUpload() {
0143 |         return this.mContext.getPackageManager().hasSystemFeature("android.hardware.fingerprint") && !isFingerprintSuccess();
0144 |     }
0145 | 
0146 |     @Override // com.miui.daemon.mqsas.upload.BaseUploader
0147 |     public void onUploadDone() {
0148 |         IS_FIRST_UPLOAD = false;
0149 |         Utils.logD(TAG, "Upload done.");
0150 |     }
0151 | 
0152 |     public class BiometricInfo {
0153 |         String ext;
0154 |         String failTime;
0155 |         String failType;
0156 |         String fid;
0157 |         String propInfo;
0158 | 
0159 |         public BiometricInfo() {
0160 |         }
0161 | 
0162 |         public String getFid() {
0163 |             return this.fid;
0164 |         }
0165 | 
0166 |         public void setFid(String str) {
0167 |             this.fid = str;
0168 |         }
0169 | 
0170 |         public String getFailType() {
0171 |             return this.failType;
0172 |         }
0173 | 
0174 |         public void setFailType(String str) {
0175 |             this.failType = str;
0176 |         }
0177 | 
0178 |         public String getPropInfo() {
0179 |             return this.propInfo;
0180 |         }
0181 | 
0182 |         public void setPropInfo(String str) {
0183 |             this.propInfo = str;
0184 |         }
0185 | 
0186 |         public String getExt() {
0187 |             return this.ext;
0188 |         }
0189 | 
0190 |         public void setExt(String str) {
0191 |             this.ext = str;
0192 |         }
0193 | 
0194 |         public String getFailTime() {
0195 |             return this.failTime;
0196 |         }
0197 | 
0198 |         public void setFailTime(String str) {
0199 |             this.failTime = str;
0200 |         }
0201 |     }
0202 | 
0203 |     public class BiometricInfoEvent extends BaseUploadEvent {
0204 |         List<BiometricInfo> events;
0205 | 
0206 |         public BiometricInfoEvent() {
0207 |         }
0208 | 
0209 |         public List<BiometricInfo> getEvents() {
0210 |             return this.events;
```

## D14

Source: `sources/com/miui/daemon/mqsas/mobilediag/MobileNetworkCapture.java`

Source-file SHA-256: `b27d0703d3a1fc504ab08d0bbc52867e55b322ed46b1d95f2f56b4cc00082673`

Original lines 75–180:

```text
0075 |         if (!isValidRtt()) {
0076 |             return null;
0077 |         }
0078 |         StringBuilder sb = new StringBuilder();
0079 |         for (int i = 0; i < 5; i++) {
0080 |             sb.append(this.mRtt[i]);
0081 |             sb.append(",");
0082 |         }
0083 |         sb.append(this.mRtt[5]);
0084 |         JSONObject jSONObject = new JSONObject();
0085 |         try {
0086 |             jSONObject.put("PackageName", this.mPacketName);
0087 |             jSONObject.put("RttTime", sb.toString());
0088 |         } catch (JSONException e) {
0089 |             Utils.logE("MobileNetworkCapture", "getResult JSONException: ", e);
0090 |         }
0091 |         return jSONObject.toString();
0092 |     }
0093 | 
0094 |     public void clear() {
0095 |         for (int i = 0; i < 6; i++) {
0096 |             this.mRtt[i] = 0;
0097 |         }
0098 |     }
0099 | 
0100 |     public final boolean isValidRtt() {
0101 |         for (int i = 0; i < 6; i++) {
0102 |             if (this.mRtt[i] != 0) {
0103 |                 return true;
0104 |             }
0105 |         }
0106 |         return false;
0107 |     }
0108 | 
0109 |     public final boolean initPacketSocket(String str) {
0110 |         try {
0111 |             IMQSNative iMQSNativeAsInterface = IMQSNative.Stub.asInterface(ServiceManager.getService("miui.mqsas.IMQSNative"));
0112 |             if (iMQSNativeAsInterface == null) {
0113 |                 Utils.logE("MobileNetworkCapture", "mqsasd not available!");
0114 |                 return false;
0115 |             }
0116 |             this.mPacketSock = iMQSNativeAsInterface.attachSockFilter(this.mSrcMac, this.mDstMac, this.mSrcIp, this.mDstIp, this.mProto, this.mSrcPort, this.mDstPort, this.mIdentifyData, 0, str);
0117 |             return true;
0118 |         } catch (Exception e) {
0119 |             Utils.logE("MobileNetworkCapture", "Error creating packet socket", e);
0120 |             return false;
0121 |         }
0122 |     }
0123 | 
0124 |     public class ReceiveThread extends Thread {
0125 |         public final byte[] mPacket = new byte[1500];
0126 |         public volatile boolean mStopped = false;
0127 | 
0128 |         public ReceiveThread() {
0129 |         }
0130 | 
0131 |         public void halt() {
0132 |             this.mStopped = true;
0133 |         }
0134 | 
0135 |         @Override // java.lang.Thread, java.lang.Runnable
0136 |         public void run() {
0137 |             while (!this.mStopped) {
0138 |                 if (MobileNetworkCapture.this.mPacketSock != null) {
0139 |                     try {
0140 |                         FileDescriptor fileDescriptor = MobileNetworkCapture.this.mPacketSock.getFileDescriptor();
0141 |                         if (fileDescriptor == null || !fileDescriptor.valid()) {
0142 |                             Utils.logE("MobileNetworkCapture", "Packet socket is invalid, stop capture");
0143 |                             MobileNetworkCapture.this.stopMonitorRtt();
0144 |                         }
0145 |                         byte[] bArr = this.mPacket;
0146 |                         int i = Os.read(fileDescriptor, bArr, 0, bArr.length);
0147 |                         if (i > 0) {
0148 |                             ByteBuffer byteBufferOrder = ByteBuffer.wrap(this.mPacket, 0, i).order(ByteOrder.BIG_ENDIAN);
0149 |                             MobileNetworkCapture mobileNetworkCapture = MobileNetworkCapture.this;
0150 |                             mobileNetworkCapture.recordRtt(mobileNetworkCapture.getRttFromPacket(byteBufferOrder));
0151 |                         }
0152 |                     } catch (Exception e) {
0153 |                         Utils.logE("MobileNetworkCapture", "Read error sock ", e);
0154 |                     }
0155 |                 } else {
0156 |                     Utils.logE("MobileNetworkCapture", "mPacketSock is null, stop capture");
0157 |                     MobileNetworkCapture.this.stopMonitorRtt();
0158 |                 }
0159 |             }
0160 |         }
0161 |     }
0162 | 
0163 |     public final void recordRtt(long j) {
0164 |         if (j > 0) {
0165 |             int iMin = (int) Math.min(5L, j / 60);
0166 |             long[] jArr = this.mRtt;
0167 |             jArr[iMin] = jArr[iMin] + 1;
0168 |         }
0169 |     }
0170 | 
0171 |     public final long getRttFromPacket(ByteBuffer byteBuffer) {
0172 |         byteBuffer.order(ByteOrder.BIG_ENDIAN);
0173 |         if (byteBuffer.remaining() < 40) {
0174 |             Utils.logE("MobileNetworkCapture", "packet is too short!");
0175 |             return -1L;
0176 |         }
0177 |         byte b = byteBuffer.get();
0178 |         int i = (b & 240) >> 4;
0179 |         if (i != 4) {
0180 |             Utils.logE("MobileNetworkCapture", " Invalid IP version: " + i);
```

## D15

Source: `sources/com/miui/daemon/fileobserver/FileObserverApp.java`

Source-file SHA-256: `af62e7cb52f96e63047ef05fdb12a9680c196122b94c47a1a8b6139dbada5f8e`

Original lines 1–42:

```text
0001 | package com.miui.daemon.fileobserver;
0002 | 
0003 | import android.app.Application;
0004 | import android.os.AsyncTask;
0005 | import com.miui.daemon.fileobserver.log.Log;
0006 | import java.util.LinkedList;
0007 | import java.util.List;
0008 | import miui.os.Build;
0009 | 
0010 | /* JADX INFO: loaded from: classes.dex */
0011 | public class FileObserverApp {
0012 |     public static final boolean ENABLED = !Build.IS_STABLE_VERSION;
0013 |     public static final List<String> SUPPORT_DEVICES;
0014 | 
0015 |     static {
0016 |         LinkedList linkedList = new LinkedList();
0017 |         SUPPORT_DEVICES = linkedList;
0018 |         linkedList.add("mido");
0019 |         linkedList.add("sagit");
0020 |     }
0021 | 
0022 |     public static void init(Application application) {
0023 |         if (ENABLED) {
0024 |             new AsyncTask<Application, Void, Void>() { // from class: com.miui.daemon.fileobserver.FileObserverApp.1
0025 |                 @Override // android.os.AsyncTask
0026 |                 public Void doInBackground(Application... applicationArr) {
0027 |                     if (!FileObserverApp.SUPPORT_DEVICES.contains(Build.DEVICE)) {
0028 |                         return null;
0029 |                     }
0030 |                     FileObserverApp.initInWorkThread(applicationArr[0]);
0031 |                     return null;
0032 |                 }
0033 |             }.execute(application);
0034 |         }
0035 |     }
0036 | 
0037 |     public static void initInWorkThread(Application application) {
0038 |         FileObserverManager.getInstance().init(application);
0039 |         FileObserverManager.getInstance().startWatching();
0040 |         Log.setLevel(android.util.Log.isLoggable("FileObserverApp", 3) ? 3 : 5);
0041 |     }
0042 | }
```

## D16

Source: `sources/com/miui/daemon/mqsas/utils/Utils.java`

Source-file SHA-256: `3f54d118f38486f5a10989458e5d9dd4e391fe4ea2e5e4005e12ab80c63cc7fb`

Original lines 230–310:

```text
0230 |         boolean z;
0231 |         if (context != null) {
0232 |             z = Settings.Secure.getInt(context.getApplicationContext().getContentResolver(), "upload_log_pref", IS_DEVELOPMENT_VERSION ? 1 : 0) == 1;
0233 |         }
0234 |         logI("Utils", "isUploadLogAllowed() allow:" + z);
0235 |         return z;
0236 |     }
0237 | 
0238 |     public static boolean isSystemCoreApp(String str) {
0239 |         for (String str2 : SYSTEM_CORE_APPS) {
0240 |             if (str2.equals(str)) {
0241 |                 return true;
0242 |             }
0243 |         }
0244 |         return false;
0245 |     }
0246 | 
0247 |     public static boolean isUploadEnabled(Context context) {
0248 |         if (Settings.Global.getInt(context.getContentResolver(), "device_provisioned", 0) == 0) {
0249 |             return false;
0250 |         }
0251 |         return isReportEnabled(context);
0252 |     }
0253 | 
0254 |     public static boolean isUploadStorageEnabled(Context context) {
0255 |         if (Settings.Global.getInt(context.getContentResolver(), "device_provisioned", 0) == 0) {
0256 |             return false;
0257 |         }
0258 |         if (isLibraryTest()) {
0259 |             return true;
0260 |         }
0261 |         boolean zIsUploadDebugLogEnable = MiuiSettings.Secure.isUploadDebugLogEnable(context.getApplicationContext().getContentResolver());
0262 |         logI("Utils", "isUploadEnabled() enabled:" + zIsUploadDebugLogEnable);
0263 |         if (IS_INTERNATIONAL_BUILD) {
0264 |             return zIsUploadDebugLogEnable && MiuiSettings.Privacy.isEnabled(context, context.getPackageName());
0265 |         }
0266 |         return zIsUploadDebugLogEnable;
0267 |     }
0268 | 
0269 |     public static int getCameraLogRule() {
0270 |         return SystemProperties.getInt("vendor.camera.sensor.logsystem", 0);
0271 |     }
0272 | 
0273 |     public static boolean isLibraryTest() {
0274 |         return SystemProperties.getBoolean("persist.mtbf.test", false) || isOmniTest();
0275 |     }
0276 | 
0277 |     public static boolean isReportEnabled(Context context) {
0278 |         if (isLibraryTest()) {
0279 |             return true;
0280 |         }
0281 |         if (!DeviceUtil.isUnReleased() && DeviceUtil.IS_ROOT && SystemProperties.get("miui.daemon.debug", "false").equals("false")) {
0282 |             return false;
0283 |         }
0284 |         boolean zIsUploadDebugLogEnable = context != null ? MiuiSettings.Secure.isUploadDebugLogEnable(context.getApplicationContext().getContentResolver()) : false;
0285 |         logI("Utils", "isUploadEnabled() enabled:" + zIsUploadDebugLogEnable);
0286 |         if (IS_INTERNATIONAL_BUILD) {
0287 |             return zIsUploadDebugLogEnable && MiuiSettings.Privacy.isEnabled(context, context.getPackageName());
0288 |         }
0289 |         return zIsUploadDebugLogEnable;
0290 |     }
0291 | 
0292 |     public static final String MD5(String str) {
0293 |         char[] cArr = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
0294 |         try {
0295 |             byte[] bytes = str.getBytes();
0296 |             MessageDigest messageDigest = MessageDigest.getInstance("MD5");
0297 |             messageDigest.update(bytes);
0298 |             byte[] bArrDigest = messageDigest.digest();
0299 |             char[] cArr2 = new char[bArrDigest.length * 2];
0300 |             int i = 0;
0301 |             for (byte b : bArrDigest) {
0302 |                 int i2 = i + 1;
0303 |                 cArr2[i] = cArr[(b >>> 4) & 15];
0304 |                 i = i2 + 1;
0305 |                 cArr2[i2] = cArr[b & 15];
0306 |             }
0307 |             return new String(cArr2);
0308 |         } catch (Exception e) {
0309 |             Log.e("Utils", "Generate md5 value failed." + e.getMessage());
0310 |             return "";
```

## D17

Source: `sources/com/miui/daemon/performance/onetrack/TrackAnalytics.java`

Source-file SHA-256: `424e9d10ca635710361c6b079593cfc63be55e6a7a387da45ba14a65b932162b`

Original lines 1–78:

```text
0001 | package com.miui.daemon.performance.onetrack;
0002 | 
0003 | import android.content.Context;
0004 | import android.provider.MiuiSettings;
0005 | import android.text.TextUtils;
0006 | import android.util.Slog;
0007 | import com.ot.pubsub.PubSubTrack;
0008 | import com.ot.pubsub.g.l;
0009 | import com.xiaomi.onetrack.Configuration;
0010 | import com.xiaomi.onetrack.OneTrack;
0011 | import java.util.Arrays;
0012 | import java.util.HashSet;
0013 | import java.util.Map;
0014 | import java.util.Set;
0015 | import miui.os.Build;
0016 | 
0017 | /* JADX INFO: loaded from: classes.dex */
0018 | public class TrackAnalytics {
0019 |     public static final Set<String> EU;
0020 |     public static Context mContext;
0021 |     public static final boolean sInternational;
0022 |     public static OneTrack sOneTrack;
0023 |     public static boolean sPubSubEable;
0024 |     public static PubSubTrack sPubSubTrack;
0025 | 
0026 |     static {
0027 |         sInternational = Build.IS_INTERNATIONAL_BUILD && !Build.getRegion().equals(l.b);
0028 |         sPubSubEable = false;
0029 |         EU = new HashSet(Arrays.asList("AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR", "DE", "EL", "GR", "HU", "IS", "IE", "IT", "LV", "LI", "LT", "LU", "MT", "NL", "NO", "PL", "PT", "RO", "SK", "SI", "ES", "SE", "UK", "GB"));
0030 |     }
0031 | 
0032 |     public static void init(Context context) {
0033 |         mContext = context;
0034 |         if (!sInternational) {
0035 |             sOneTrack = OneTrack.createInstance(context, new Configuration.Builder().setAppId("30000000045").setChannel("xiaomi").setMode(OneTrack.Mode.APP).setExceptionCatcherEnable(true).build());
0036 |         } else {
0037 |             sPubSubTrack = PubSubTrack.createInstance(context, new com.ot.pubsub.Configuration.Builder().setAppId("31000000923").setChannel("xiaomi").setProjectId("battery-and-performance").setPrivateKeyId("[REDACTED_KEY_IDENTIFIER]").build());
0038 |         }
0039 |     }
0040 | 
0041 |     public static void track(String str, Map<String, Object> map) {
0042 |         OneTrack oneTrack = sOneTrack;
0043 |         if (oneTrack != null) {
0044 |             oneTrack.track(str, map);
0045 |             return;
0046 |         }
0047 |         PubSubTrack pubSubTrack = sPubSubTrack;
0048 |         if (pubSubTrack == null || !sPubSubEable) {
0049 |             return;
0050 |         }
0051 |         pubSubTrack.track("mqs_performance", str, map);
0052 |     }
0053 | 
0054 |     public static void updateTrackConfig() {
0055 |         sPubSubEable = isPrivacyEnable() && getCloudData("PerformanceInfo", "PerfPubsubEnable");
0056 |     }
0057 | 
0058 |     public static boolean isPrivacyEnable() {
0059 |         Context context = mContext;
0060 |         boolean z = MiuiSettings.Privacy.isEnabled(context, context.getPackageName()) || !isInEURegion();
0061 |         Slog.i("TrackAnalytics", "privacy enable " + z);
0062 |         return z;
0063 |     }
0064 | 
0065 |     public static boolean getCloudData(String str, String str2) {
0066 |         boolean cloudDataBoolean = MiuiSettings.SettingsCloudData.getCloudDataBoolean(mContext.getContentResolver(), str, str2, false);
0067 |         Slog.i("TrackAnalytics", "cloud control enable " + cloudDataBoolean);
0068 |         return cloudDataBoolean;
0069 |     }
0070 | 
0071 |     public static boolean isInEURegion() {
0072 |         String region = Build.getRegion();
0073 |         if (TextUtils.isEmpty(region) || TextUtils.equals(region, "unknown")) {
0074 |             return true;
0075 |         }
0076 |         return EU.contains(region.toUpperCase());
0077 |     }
0078 | }
```

## D18

Source: `sources/com/miui/daemon/mqsas/utils/PrivacyUtils.java`

Source-file SHA-256: `571e54524a3d21c1149e41a4fb4588dc7ba10da1ef267804738527e6852f0d59`

Original lines 1–111:

```text
0001 | package com.miui.daemon.mqsas.utils;
0002 | 
0003 | import android.content.Context;
0004 | import android.content.Intent;
0005 | import com.miui.daemon.mqsas.upload.Constants;
0006 | import com.miui.daemon.performance.statistics.utils.PreferenceUtils;
0007 | import com.ot.pubsub.util.w;
0008 | import java.io.File;
0009 | 
0010 | /* JADX INFO: loaded from: classes.dex */
0011 | public class PrivacyUtils {
0012 |     public static final Long DAY = Long.valueOf(w.b);
0013 | 
0014 |     public static boolean canUploadLog(Context context) {
0015 |         return (!needShowDialog(context) || canShowDialog(context)) && PreferenceUtils.getBoolean(context, Constants.CAN_UPLOAD_LOG, true);
0016 |     }
0017 | 
0018 |     public static boolean needShowDialog(Context context) {
0019 |         return PreferenceUtils.getBoolean(context, Constants.NEED_SHOW_DIALOG, true);
0020 |     }
0021 | 
0022 |     public static boolean canShowDialog(Context context) {
0023 |         return needShowDialog(context) && System.currentTimeMillis() - getLastShowDialogTime(context) > DAY.longValue();
0024 |     }
0025 | 
0026 |     public static void setLastShowDialogTime(Context context, long j) {
0027 |         PreferenceUtils.putLong(context, Constants.LAST_SHOW_DIALOG_TIME, j);
0028 |     }
0029 | 
0030 |     public static long getLastShowDialogTime(Context context) {
0031 |         return PreferenceUtils.getLong(context, Constants.LAST_SHOW_DIALOG_TIME, 0L);
0032 |     }
0033 | 
0034 |     public static void setCanUploadLog(Context context, boolean z) {
0035 |         PreferenceUtils.putBoolean(context, Constants.CAN_UPLOAD_LOG, z);
0036 |     }
0037 | 
0038 |     public static void setNeedShowDialog(Context context, boolean z) {
0039 |         PreferenceUtils.putBoolean(context, Constants.NEED_SHOW_DIALOG, z);
0040 |     }
0041 | 
0042 |     public static void deleteLog(int i, String str) {
0043 |         File file = new File("/data/mqsas/" + Utils.getExceptionType(i) + "/online");
0044 |         if (file.exists() && file.isDirectory()) {
0045 |             for (File file2 : file.listFiles()) {
0046 |                 if (file2 != null && file2.getAbsolutePath().contains(str)) {
0047 |                     file2.delete();
0048 |                 }
0049 |             }
0050 |         }
0051 |     }
0052 | 
0053 |     public static void dialogButtonChecked(Context context, int i, int i2, String str, boolean z) {
0054 |         if (i == 0) {
0055 |             setCanUploadLog(context, false);
0056 |             deleteLog(i2, str);
0057 |         } else {
0058 |             if (i != 1) {
0059 |                 if (i != 2) {
0060 |                     return;
0061 |                 }
0062 |                 setCanUploadLog(context, true);
0063 |                 setNeedShowDialog(context, !z);
0064 |                 return;
0065 |             }
0066 |             if (!z) {
0067 |                 setCanUploadLog(context, false);
0068 |             }
0069 |             setNeedShowDialog(context, true);
0070 |             deleteLog(i2, str);
0071 |         }
0072 |     }
0073 | 
0074 |     public static void startPrivacyDialog(Context context, int i, String str) {
0075 |         setLastShowDialogTime(context, System.currentTimeMillis());
0076 |         try {
0077 |             Intent intent = new Intent();
0078 |             intent.setAction("miui.intent.action.MQS_PRIVACY_DIALOG");
0079 |             intent.setFlags(268435456);
0080 |             intent.putExtra("type", i);
0081 |             intent.putExtra("dgt", str);
0082 |             context.startActivity(intent);
0083 |         } catch (Exception e) {
0084 |             e.printStackTrace();
0085 |         }
0086 |     }
0087 | 
0088 |     public static boolean needShowDialogWhenStart(Context context) {
0089 |         return PreferenceUtils.getBoolean(context, Constants.NEED_SHOW_DIALOG_WHEN_START, false);
0090 |     }
0091 | 
0092 |     public static void setNeedShowDialogWhenStart(Context context, boolean z) {
0093 |         PreferenceUtils.putBoolean(context, Constants.NEED_SHOW_DIALOG_WHEN_START, z);
0094 |     }
0095 | 
0096 |     public static void setEventType(Context context, int i) {
0097 |         PreferenceUtils.putLong(context, Constants.DIALOG_EVENT_TYPE, i);
0098 |     }
0099 | 
0100 |     public static int getEventType(Context context) {
0101 |         return (int) PreferenceUtils.getLong(context, Constants.DIALOG_EVENT_TYPE, 0L);
0102 |     }
0103 | 
0104 |     public static void setEventDgt(Context context, String str) {
0105 |         PreferenceUtils.putPreference(context, Constants.DIALOG_EVENT_DGT, str);
0106 |     }
0107 | 
0108 |     public static String getEventDgt(Context context) {
0109 |         return PreferenceUtils.getPreference(context, Constants.DIALOG_EVENT_DGT);
0110 |     }
0111 | }
```

## D19

Source: `sources/com/miui/daemon/performance/statistics/network/TrustAllCerts.java`

Source-file SHA-256: `3c13094f8bc5086ddbe3c9595ed20b966d1e4812f2f8a0d1636a638b4d200504`

Original lines 1–21:

```text
0001 | package com.miui.daemon.performance.statistics.network;
0002 | 
0003 | import java.security.cert.CertificateException;
0004 | import java.security.cert.X509Certificate;
0005 | import javax.net.ssl.X509TrustManager;
0006 | 
0007 | /* JADX INFO: loaded from: classes.dex */
0008 | public class TrustAllCerts implements X509TrustManager {
0009 |     @Override // javax.net.ssl.X509TrustManager
0010 |     public void checkClientTrusted(X509Certificate[] x509CertificateArr, String str) throws CertificateException {
0011 |     }
0012 | 
0013 |     @Override // javax.net.ssl.X509TrustManager
0014 |     public void checkServerTrusted(X509Certificate[] x509CertificateArr, String str) throws CertificateException {
0015 |     }
0016 | 
0017 |     @Override // javax.net.ssl.X509TrustManager
0018 |     public X509Certificate[] getAcceptedIssuers() {
0019 |         return null;
0020 |     }
0021 | }
```

## D20

Source: `sources/com/miui/daemon/mqsas/utils/HttpUtils.java`

Source-file SHA-256: `c0901419b5db43b04d7f94e0271e6e013e76188f7bcbb2d5e0394b2217d7919b`

Original lines 1–150:

```text
0001 | package com.miui.daemon.mqsas.utils;
0002 | 
0003 | import com.android.volley.toolbox.HttpHeaderParser;
0004 | import java.io.BufferedReader;
0005 | import java.io.DataOutputStream;
0006 | import java.io.File;
0007 | import java.io.FileInputStream;
0008 | import java.io.IOException;
0009 | import java.io.InputStream;
0010 | import java.io.InputStreamReader;
0011 | import java.net.HttpURLConnection;
0012 | import java.net.URL;
0013 | import java.util.HashMap;
0014 | import java.util.Iterator;
0015 | 
0016 | /* JADX INFO: loaded from: classes.dex */
0017 | public class HttpUtils {
0018 |     public static String httpGlobalPostFiles(String str, HashMap<String, String> map, HashMap<String, String> map2, String str2) throws IOException, IllegalArgumentException {
0019 |         if (map2 == null || map2.size() == 0) {
0020 |             throw new IllegalArgumentException(" need at lease one file");
0021 |         }
0022 |         HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(str).openConnection();
0023 |         addHeader(httpURLConnection, map);
0024 |         DataOutputStream dataOutputStream = new DataOutputStream(httpURLConnection.getOutputStream());
0025 |         Iterator<String> it = map2.keySet().iterator();
0026 |         while (it.hasNext()) {
0027 |             File file = new File(map2.get(it.next()));
0028 |             dataOutputStream.writeBytes("\r\n------MiuiDaemonBoundaryLM6lcG1l3wG4dBm0\r\n");
0029 |             dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"file\";filename=\"" + file.getName() + "\"");
0030 |             dataOutputStream.writeBytes("\r\n");
0031 |             dataOutputStream.writeBytes("Content-Type:multipart/form-data");
0032 |             dataOutputStream.writeBytes("\r\n");
0033 |             dataOutputStream.writeBytes("\r\n");
0034 |             FileInputStream fileInputStream = new FileInputStream(file);
0035 |             try {
0036 |                 byte[] bArr = new byte[4096];
0037 |                 while (true) {
0038 |                     int i = fileInputStream.read(bArr);
0039 |                     if (i != -1) {
0040 |                         dataOutputStream.write(bArr, 0, i);
0041 |                     }
0042 |                 }
0043 |                 fileInputStream.close();
0044 |                 dataOutputStream.writeBytes("\r\n");
0045 |                 dataOutputStream.writeBytes("------MiuiDaemonBoundaryLM6lcG1l3wG4dBm0\r\n");
0046 |                 dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"file\"\r\n");
0047 |                 dataOutputStream.writeBytes("\r\n");
0048 |                 dataOutputStream.writeBytes(file.getAbsolutePath());
0049 |                 dataOutputStream.writeBytes("\r\n");
0050 |                 dataOutputStream.writeBytes("------MiuiDaemonBoundaryLM6lcG1l3wG4dBm0\r\n");
0051 |                 dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"contentType\"\r\n");
0052 |                 dataOutputStream.writeBytes("\r\n");
0053 |                 dataOutputStream.writeBytes("multipart/form-data");
0054 |                 dataOutputStream.writeBytes("\r\n");
0055 |                 dataOutputStream.writeBytes("------MiuiDaemonBoundaryLM6lcG1l3wG4dBm0\r\n");
0056 |                 dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"cannedAcl\"\r\n");
0057 |                 dataOutputStream.writeBytes("\r\n");
0058 |                 dataOutputStream.writeBytes("PUBLIC_READ");
0059 |                 dataOutputStream.writeBytes("\r\n");
0060 |                 dataOutputStream.writeBytes("------MiuiDaemonBoundaryLM6lcG1l3wG4dBm0\r\n");
0061 |                 dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"key\"\r\n");
0062 |                 dataOutputStream.writeBytes("\r\n");
0063 |                 dataOutputStream.writeBytes(str2);
0064 |                 dataOutputStream.writeBytes("\r\n------MiuiDaemonBoundaryLM6lcG1l3wG4dBm0--\r\n");
0065 |             } catch (Throwable th) {
0066 |                 try {
0067 |                     fileInputStream.close();
0068 |                 } catch (Throwable th2) {
0069 |                     th.addSuppressed(th2);
0070 |                 }
0071 |                 throw th;
0072 |             }
0073 |         }
0074 |         dataOutputStream.flush();
0075 |         dataOutputStream.close();
0076 |         return httpResponse(httpURLConnection);
0077 |     }
0078 | 
0079 |     public static String httpPostFiles(String str, HashMap<String, String> map, HashMap<String, String> map2) throws IOException, IllegalArgumentException {
0080 |         if (map2 == null || map2.size() == 0) {
0081 |             throw new IllegalArgumentException(" need at lease one file");
0082 |         }
0083 |         HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(str).openConnection();
0084 |         addHeader(httpURLConnection, map);
0085 |         DataOutputStream dataOutputStream = new DataOutputStream(httpURLConnection.getOutputStream());
0086 |         for (String str2 : map2.keySet()) {
0087 |             File file = new File(map2.get(str2));
0088 |             dataOutputStream.writeBytes("\r\n------MiuiDaemonBoundaryLM6lcG1l3wG4dBm0\r\n");
0089 |             dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"" + str2 + "\"; filename=\"" + file.getName() + "\"");
0090 |             dataOutputStream.writeBytes("\r\n");
0091 |             dataOutputStream.writeBytes("Content-Type:multipart/form-data");
0092 |             dataOutputStream.writeBytes("\r\n");
0093 |             dataOutputStream.writeBytes("\r\n");
0094 |             FileInputStream fileInputStream = new FileInputStream(file);
0095 |             try {
0096 |                 byte[] bArr = new byte[4096];
0097 |                 while (true) {
0098 |                     int i = fileInputStream.read(bArr);
0099 |                     if (i != -1) {
0100 |                         dataOutputStream.write(bArr, 0, i);
0101 |                     }
0102 |                 }
0103 |                 fileInputStream.close();
0104 |                 dataOutputStream.writeBytes("\r\n------MiuiDaemonBoundaryLM6lcG1l3wG4dBm0--\r\n");
0105 |             } catch (Throwable th) {
0106 |                 try {
0107 |                     fileInputStream.close();
0108 |                 } catch (Throwable th2) {
0109 |                     th.addSuppressed(th2);
0110 |                 }
0111 |                 throw th;
0112 |             }
0113 |         }
0114 |         dataOutputStream.flush();
0115 |         dataOutputStream.close();
0116 |         return httpResponse(httpURLConnection);
0117 |     }
0118 | 
0119 |     /* JADX WARN: Code duplicated, block: B:69:0x008f A[EXC_TOP_SPLITTER, SYNTHETIC] */
0120 |     /* JADX WARN: Code duplicated, block: B:73:0x0085 A[EXC_TOP_SPLITTER, SYNTHETIC] */
0121 |     public static String httpResponse(HttpURLConnection httpURLConnection) throws Throwable {
0122 |         Throwable th;
0123 |         InputStream inputStream;
0124 |         InputStream inputStream2;
0125 |         BufferedReader bufferedReader;
0126 |         BufferedReader bufferedReader2 = null;
0127 |         if (httpURLConnection == null) {
0128 |             if (httpURLConnection != null) {
0129 |             }
0130 |             return null;
0131 |         }
0132 |         try {
0133 |             if (httpURLConnection.getResponseCode() != 200) {
0134 |                 inputStream = httpURLConnection.getErrorStream();
0135 |             } else {
0136 |                 inputStream = httpURLConnection.getInputStream();
0137 |             }
0138 |             try {
0139 |                 BufferedReader bufferedReader3 = new BufferedReader(new InputStreamReader(inputStream), 2048);
0140 |                 try {
0141 |                     StringBuilder sb = new StringBuilder();
0142 |                     while (true) {
0143 |                         String line = bufferedReader3.readLine();
0144 |                         if (line == null) {
0145 |                             break;
0146 |                         }
0147 |                         sb.append(line);
0148 |                     }
0149 |                     bufferedReader3.close();
0150 |                     String string = sb.toString();
```

## D21

Source: `sources/com/miui/daemon/fileobserver/core/MiuiFileObserver.java`

Source-file SHA-256: `aec8f662b667a5624306aab414dcf8502a0cc30a9062499e3995b0398939f90e`

Original lines 1–20:

```text
0001 | package com.miui.daemon.fileobserver.core;
0002 | 
0003 | import android.annotation.SuppressLint;
0004 | import android.content.Context;
0005 | import android.os.FileObserver;
0006 | 
0007 | /* JADX INFO: loaded from: classes.dex */
0008 | public abstract class MiuiFileObserver extends FileObserver {
0009 |     public Context mContext;
0010 | 
0011 |     @SuppressLint({"WrongConstant"})
0012 |     public MiuiFileObserver(Context context, String str, int i) {
0013 |         super(str, i | 4096);
0014 |         this.mContext = context;
0015 |     }
0016 | 
0017 |     public Context getContext() {
0018 |         return this.mContext;
0019 |     }
0020 | }
```

## D22

Source: `sources/com/miui/daemon/fileobserver/FileObserverManager.java`

Source-file SHA-256: `7cc2b8d92b1effc397f60fe52c838f44c85b42d64e3af19d8fcf402792cf6f07`

Original lines 1–83:

```text
0001 | package com.miui.daemon.fileobserver;
0002 | 
0003 | import android.app.Application;
0004 | import android.content.Context;
0005 | import android.os.Environment;
0006 | import android.os.FileObserver;
0007 | import com.miui.daemon.fileobserver.core.MiuiFileObserver;
0008 | import java.util.Iterator;
0009 | import java.util.LinkedList;
0010 | import java.util.List;
0011 | 
0012 | /* JADX INFO: loaded from: classes.dex */
0013 | public class FileObserverManager {
0014 |     public static final List<String> sObservePaths;
0015 |     public Application mApplication;
0016 |     public List<FileObserver> mObservers;
0017 | 
0018 |     public static class Singleton {
0019 |         public static final FileObserverManager INSTANCE = new FileObserverManager();
0020 |     }
0021 | 
0022 |     public static boolean isOperation(int i, int i2) {
0023 |         return (i & i2) != 0;
0024 |     }
0025 | 
0026 |     static {
0027 |         LinkedList linkedList = new LinkedList();
0028 |         sObservePaths = linkedList;
0029 |         String absolutePath = Environment.getExternalStorageDirectory().getAbsolutePath();
0030 |         linkedList.add(absolutePath.concat("/DCIM/Camera"));
0031 |         linkedList.add(absolutePath.concat("/MIUI/Gallery/cloud/owner"));
0032 |     }
0033 | 
0034 |     public FileObserverManager() {
0035 |     }
0036 | 
0037 |     public static FileObserverManager getInstance() {
0038 |         return Singleton.INSTANCE;
0039 |     }
0040 | 
0041 |     public void init(Application application) {
0042 |         this.mApplication = application;
0043 |     }
0044 | 
0045 |     public void startWatching() {
0046 |         if (this.mApplication == null) {
0047 |             throw new IllegalStateException("Haven't called the method of init");
0048 |         }
0049 |         synchronized (this) {
0050 |             if (this.mObservers == null) {
0051 |                 this.mObservers = new LinkedList();
0052 |                 Iterator<String> it = sObservePaths.iterator();
0053 |                 while (it.hasNext()) {
0054 |                     this.mObservers.add(obtainObserver(this.mApplication, it.next()));
0055 |                 }
0056 |                 Iterator<FileObserver> it2 = this.mObservers.iterator();
0057 |                 while (it2.hasNext()) {
0058 |                     it2.next().startWatching();
0059 |                 }
0060 |             }
0061 |         }
0062 |     }
0063 | 
0064 |     public static FileObserver obtainObserver(Context context, String str) {
0065 |         return new MiuiFileObserver(context, str, 1536) { // from class: com.miui.daemon.fileobserver.FileObserverManager.1
0066 |             @Override // android.os.FileObserver
0067 |             public void onEvent(int i, String str2) {
0068 |                 if (FileObserverManager.isOperation(i, 256)) {
0069 |                     FileObserveRecorder.recordCreated(getContext(), str2);
0070 |                 }
0071 |                 if (FileObserverManager.isOperation(i, 1536)) {
0072 |                     FileObserveRecorder.recordDeleted(getContext(), str2);
0073 |                 }
0074 |                 if (FileObserverManager.isOperation(i, 2)) {
0075 |                     FileObserveRecorder.recordModified(getContext(), str2);
0076 |                 }
0077 |                 if (FileObserverManager.isOperation(i, 1)) {
0078 |                     FileObserveRecorder.recordAccessed(getContext(), str2);
0079 |                 }
0080 |             }
0081 |         };
0082 |     }
0083 | }
```
