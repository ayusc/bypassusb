package com.bypassusb

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class XposedInit : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "com.ubercab.driver") return

        XposedBridge.log("BypassUSB: 🎯 Target app matched! Initializing stream desensitization filters...")
        val loader = lpparam.classLoader

        // =================================================================
        // CORE PIPELINE: Intercept the obfuscated stream wrapper data class
        // =================================================================
        try {
            val optionalClass = XposedHelpers.findClass("com.google.common.base.Optional", loader)
            val absentOptional = XposedHelpers.callStaticMethod(optionalClass, "absent")
            
            // The 6 data getters inside the wk5.f model used by the Interactor engine
            val dataAccessors = listOf("a", "b", "c", "d", "e", "f")

            for (methodName in dataAccessors) {
                XposedHelpers.findAndHookMethod(
                    "wk5.f",
                    loader,
                    methodName,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            // Spoof the response to look like a clean, empty data packet
                            param.result = absentOptional
                        }
                    }
                )
            }
            XposedBridge.log("BypassUSB: 🛡️ Stream desensitization hooks successfully bound to wk5.f accessors")
            
        } catch (t: Throwable) {
            XposedBridge.log("BypassUSB: ❌ Critical failure targeting data stream layer: ${t.message}")
        }

        // =================================================================
        // MONITORING LAYER: Track Interactor engine state transitions
        // =================================================================
        try {
            XposedHelpers.findAndHookMethod(
                "com.uber.blockers.core.rib.b",
                loader,
                "Il",
                "com.uber.blockers.core.rib.b",
                "wk5.f",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        XposedBridge.log("BypassUSB: 📊 Interactor processing incoming state validation packet...")
                    }
                }
            )
        } catch (t: Throwable) {
            // Quiet fallback if engine layout varies slightly
        }
    }
}
