package com.example.mobile_app.util

import android.content.Context
import android.content.pm.ApplicationInfo

object DeviceUtils {
    fun isDebuggable(context: Context): Boolean {
        return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
}
