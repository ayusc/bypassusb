package com.bypassusb

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

class UsbStealthModule : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: LoadPackageParam) {
    
        if (lpparam.packageName != "com.sbi.upi") return

        XposedBridge.log("Stealth USB module active for com.sbi.upi")

        XposedHelpers.findAndHookMethod(
            "android.content.Intent",
            lpparam.classLoader,
            "getIntExtra",
            String::class.java,
            Int::class.javaPrimitiveType,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val key = param.args[0] as String
                    if (key == "plugged") {
                        val originalValue = param.result as Int
                        if (originalValue == 2) { // 2 = BATTERY_PLUGGED_USB
                            param.result = 1     // 1 = BATTERY_PLUGGED_AC
                        }
                    }
                }
            }
        )

        XposedHelpers.findAndHookMethod(
            "android.content.Intent",
            lpparam.classLoader,
            "getBooleanExtra",
            String::class.java,
            Boolean::class.javaPrimitiveType,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val key = param.args[0] as String
                    if (key == "connected" || key == "host_connected") {
                        param.result = false // Deny that a host PC is connected
                    }
                }
            }
        )

        XposedHelpers.findAndHookMethod(
            "android.os.SystemProperties",
            lpparam.classLoader,
            "get",
            String::class.java,
            String::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val key = param.args[0] as String
                    if (key == "sys.usb.state" || key == "sys.usb.config") {
                        param.result = "none"
                    }
                }
            }
        )

        val targetSettingsKeys = listOf("usb_mass_storage_enabled", "adb_enabled")
        XposedHelpers.findAndHookMethod(
            "android.provider.Settings\$Global",
            lpparam.classLoader,
            "getInt",
            android.content.ContentResolver::class.java,
            String::class.java,
            Int::class.javaPrimitiveType,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val key = param.args[1] as String
                    if (targetSettingsKeys.contains(key)) {
                        param.result = 0 
                    }
                }
            }
        )
    }
}
