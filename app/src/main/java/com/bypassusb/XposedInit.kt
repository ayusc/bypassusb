package com.bypassusb

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import de.robv.android.xposed.XposedBridge.log

class XposedInit : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "com.ubercab.driver") return

        val loader = lpparam.classLoader

        // =================================================================
        // LAYER 1: Neutralize Server-Side Exception Responses
        // =================================================================
        try {
            val loginErrorsClass = "com.uber.model.core.generated.rtapi.services.auth.LoginErrors"
            
            // Force the app to think there is no ForceUpgrade exception object
            XposedHelpers.findAndHookMethod(loginErrorsClass, loader, "forceUpgrade", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (param.result != null) {
                        param.result = null
                        log("BypassUSB: 🛡️ Neutralized Network ForceUpgrade Exception")
                    }
                }
            })

            XposedHelpers.findAndHookMethod(loginErrorsClass, loader, "eatsForceUpgrade", object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (param.result != null) {
                        param.result = null
                        log("BypassUSB: 🛡️ Neutralized Network EatsForceUpgrade Exception")
                    }
                }
            })
        } catch (t: Throwable) {
            log("BypassUSB: ⚠️ Layer 1 Hook failed (Class might vary): ${t.message}")
        }

        // =================================================================
        // LAYER 2: Defuse the Whitelist Online Blocker Actions
        // =================================================================
        try {
            val whitelistClass = "com.ubercab.whitelist_online_blocker.b"
            val whitelistClassObj = XposedHelpers.findClass(whitelistClass, loader)

            // Intercept the static method that launches the Play Store update screen
            XposedHelpers.findAndHookMethod(whitelistClass, loader, "Hl", whitelistClassObj, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = null // Return early and skip execution completely
                    log("BypassUSB: 🛡️ Blocked Whitelist Blocker Intent execution")
                }
            })
        } catch (t: Throwable) {
            log("BypassUSB: ⚠️ Layer 2 Hook failed: ${t.message}")
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
                            log("BypassUSB: 🛡️ Spoofed Carbon Blocker data state to NULL")
                        }
                    }
                }
            )
        } catch (t: Throwable) {
            log("BypassUSB: ⚠️ Layer 3 Hook failed: ${t.message}")
        }
    }
}
