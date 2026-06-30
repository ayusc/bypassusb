package com.bypassusb

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import de.robv.android.xposed.callbacks.XC_LoadPackage
import de.robv.android.xposed.XposedBridge.log

class XposedInit : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "com.ubercab.driver") return

        // Approach 1: The Data-Layer Hook (More reliable across app updates)
        try {
            findAndHookMethod(
                "com.uber.model.core.generated.rtapi.services.marketplacedriver.DriverCheckIssueData",
                lpparam.classLoader,
                "type",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val result = param.result
                        // If the data layer reports a FORCE_UPGRADE, change it to something harmless or null
                        if (result != null && result.toString() == "FORCE_UPGRADE") {
                            param.result = null 
                            log("BypassUSB: 🛡️ Blocked FORCE_UPGRADE data layer signal")
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            log("BypassUSB: ❌ Data layer hook failed: ${t.message}")
        }

        // Approach 2: Your Original UI Hook (Kept as backup)
        try {
            findAndHookMethod(
                "com.ubercab.force_app_upgrade.c",
                lpparam.classLoader,
                "b",
                java.lang.Object::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        param.result = false
                        log("BypassUSB: ✅ UI Force upgrade method returned false")
                    }
                }
            )
        } catch (t: Throwable) {
            log("BypassUSB: ⚠️ UI hook failed (Class name might have changed): ${t.message}")
        }
    }
}
