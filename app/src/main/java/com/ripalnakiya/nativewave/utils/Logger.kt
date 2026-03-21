package com.ripalnakiya.nativewave.utils

import android.util.Log

private const val TAG = "Logger"

object Logger {
    fun log(className: String, functionName: String, message: String = "") {
        Log.d(TAG, "$className: $functionName: $message")
    }
}