package com.bypassusb

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class XposedInit : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // 1. BROADCAST LOG: Test if the module is alive AT ALL for any app
        XposedBridge.log("BypassUSB: 🚀 Module loaded for package: ${lpparam.packageName}")

        // Filter for Uber
        if (lpparam.packageName != "com.ubercab.driver") return
        
        XposedBridge.log("BypassUSB: 🎯 Target app matched! Inside com.ubercab.driver")
        val loader = lpparam.classLoader

        // =================================================================
        // LAYER 1: Neutralize Server-Side Exception Responses
        // =================================================================
        try {
            val loginErrorsClass = "com.uber.model.core.generated.rtapi.services.auth.LoginErrors"
            
            XposedHelpers.findAndHookMethod(loginErrorsClass, loader, "forceUpgrade", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (param.result != null) {
                        param.result = null
                        XposedBridge.log("BypassUSB: 🛡️ Neutralized Network ForceUpgrade Exception")
                    }
                }
            })

            XposedHelpers.findAndHookMethod(loginErrorsClass, loader, "eatsForceUpgrade", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (param.result != null) {
                        param.result = null
                        XposedBridge.log("BypassUSB: 🛡️ Neutralized Network EatsForceUpgrade Exception")
                    }
                }
            })
        } catch (t: Throwable) {
            XposedBridge.log("BypassUSB: ⚠️ Layer 1 Hook failed: ${t.message}")
        }

        // =================================================================
        // LAYER 2: Defuse the Whitelist Online Blocker Actions
        // =================================================================
        try {
            val whitelistClass = "com.ubercab.whitelist_online_blocker.b"
            
            // Pass the parameter type as a String class path instead of a Class object
            XposedHelpers.findAndHookMethod(whitelistClass, loader, "Hl", whitelistClass, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = null // Block the execution of the Play Store intent entirely
                    XposedBridge.log("BypassUSB: 🛡️ Blocked Whitelist Blocker Intent execution")
                }
            })
        } catch (t: Throwable) {
            XposedBridge.log("BypassUSB: ⚠️ Layer 2 Hook failed: ${t.message}")
        }

        // =================================================================
        // LAYER 3: Data-Layer Spoofing for the Carbon Blocker
        // =================================================================
        try {
            XposedHelpers.findAndHookMethod(
                "com.uber.model.core.generated.rtapi.services.marketplacedriver.DriverCheckIssueData",
                loader,
                "type",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val result = param.result
                        if (result != null && result.toString() == "FORCE_UPGRADE") {
                            param.result = null 
                            XposedBridge.log("BypassUSB: 🛡️ Spoofed Carbon Blocker data state to NULL")
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            XposedBridge.log("BypassUSB: ⚠️ Layer 3 Hook failed: ${t.message}")
        }
    }
}
