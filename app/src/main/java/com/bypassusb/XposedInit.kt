package com.bypassusb

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement.Companion.returnConstant
import de.robv.android.xposed.XposedBridge.log
import de.robv.android.xposed.XposedHelpers.findClassIfExists
import de.robv.android.xposed.callbacks.XC_LoadPackage

class XposedInit : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "com.ubercab.driver") return
        
        try {
            val clazz = findClassIfExists("com.ubercab.force_app_upgrade.c", lpparam.classLoader)
                ?: throw ClassNotFoundException("Target class not found")

            // Hook b() -> ForceAppUpgradeCheck → always return false!
            val method = clazz.declaredMethods.firstOrNull { 
                it.name == "b" && 
                it.parameterTypes.size == 1 &&
                it.returnType == Boolean::class.javaPrimitiveType 
            } ?: throw NoSuchMethodException("Method b(Ljava/lang/Object;)Z not found")

            XposedBridge.log("[DadGPT] Hooking Uber force upgrade check...")

            // Nuke it with constant FALSE!
            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    param.result = false  // <<== FUCK THEM UPSTREAM!
                }
            })

            XposedBridge.log("[DadGPT] Uber 'Update Required' bypassed! Welcome to infinite trips.")

        } catch (t: Throwable) {
            XposedBridge.log("[DadGPT] Hook failed: ${t.message}")
            t.printStackTrace()
        }
    }
}
