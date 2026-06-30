package com.bypassusb

import android.app.Dialog
import android.content.Context
import android.content.Intent
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class XposedInit : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "com.ubercab.driver") return

        XposedBridge.log("BypassUSB: 🎯 Target app matched! Applying advanced bypass filters...")
        val loader = lpparam.classLoader

        // =================================================================
        // 1. DATA LAYER: Kill both FORCE_UPGRADE and UNKNOWN signals
        // =================================================================
        try {
            XposedHelpers.findAndHookMethod(
                "com.uber.model.core.generated.rtapi.services.marketplacedriver.DriverCheckIssueData",
                loader,
                "type",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val result = param.result
                        if (result != null) {
                            val typeStr = result.toString()
                            if (typeStr == "FORCE_UPGRADE" || typeStr == "UNKNOWN") {
                                param.result = null
                                XposedBridge.log("BypassUSB: 🛡️ Intercepted and wiped issue type: $typeStr")
                            }
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            XposedBridge.log("BypassUSB: ⚠️ Data-layer interceptor failed to bind: ${t.message}")
        }

        // =================================================================
        // 2. DIAGNOSTIC: Catch UI Dialogs and dump their creation paths
        // =================================================================
        try {
            XposedHelpers.findAndHookMethod(
                "android.app.Dialog",
                loader,
                "show",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val dialog = param.thisObject as Dialog
                        XposedBridge.log("BypassUSB: 🚨 DIALOG CAPTURED! Object class: ${dialog.javaClass.name}")
                        
                        // Dump the entire call stack to pinpoint exactly what class invoked it
                        val stackTrace = RuntimeException("BypassUSB Tracer").stackTrace
                        for (i in 0 until minOf(15, stackTrace.size)) {
                            XposedBridge.log("   at ${stackTrace[i]}")
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            XposedBridge.log("BypassUSB: ⚠️ Framework Dialog tracker failed to bind: ${t.message}")
        }

        // =================================================================
        // 3. FRAMEWORK: Block outbound Play Store redirects globally
        // =================================================================
        try {
            XposedHelpers.findAndHookMethod(
                "android.content.ContextWrapper",
                loader,
                "startActivity",
                Intent::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val intent = param.args[0] as? Intent
                        val dataUrl = intent?.data?.toString() ?: ""
                        if (dataUrl.contains("market://details") || dataUrl.contains("com.ubercab.driver")) {
                            param.result = null // Block the redirection intent cleanly
                            XposedBridge.log("BypassUSB: 🚫 Blocked system redirect to Play Store: $dataUrl")
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            XposedBridge.log("BypassUSB: ⚠️ Framework Intent blocker failed to bind: ${t.message}")
        }
    }
}
