package com.seoul.culture.utils

import android.util.Log
import com.seoul.culture.BuildConfig
import com.seoul.culture.application.ScpApplication
import com.seoul.culture.config.C

object L {
    const val TAG = "L"

    fun e(message: String?) {
        if (!BuildConfig.IS_OP) {
            Log.e(
                TAG,
                Throwable().stackTrace[1].className + "[Line = " + Throwable().stackTrace[1].lineNumber + "]"
            )
            Log.e(TAG, message)
        }
    }

    fun e(tag: String, message: String) {
        if (!BuildConfig.IS_OP) {
            Log.e(
                TAG,
                Throwable().stackTrace[1].className + "[Line = " + Throwable().stackTrace[1].lineNumber + "]"
            )
            Log.e(TAG, "$tag::$message")
        }
    }

    fun w(message: String?) {
        if (!BuildConfig.IS_OP) {
            Log.w(TAG, message)
        }
    }

    fun w(tag: String, message: String) {
        if (!BuildConfig.IS_OP) {
            Log.w(TAG, "$tag::$message")
        }
    }


    fun i(message: String?) {
        if (!BuildConfig.IS_OP) {
            Log.i(TAG, message)
        }
    }

    fun i(tag: String, message: String) {
        if (!BuildConfig.IS_OP) {
            Log.i(TAG, "$tag::$message")
        }
    }


    fun d(message: String?) {
        if (!BuildConfig.IS_OP) {
            Log.d(TAG, message)
        }
    }

    fun d(tag: String, message: String) {
        if (!BuildConfig.IS_OP) {
            Log.d(TAG, "$tag::$message")
        }
    }


    fun v(message: String?) {
        if (!BuildConfig.IS_OP) {
            Log.v(TAG, message)
        }
    }

    fun v(tag: String, message: String) {
        if (!BuildConfig.IS_OP) {
            Log.v(TAG, "$tag::$message")
        }
    }

    fun lc(tag: String, lifecycle: String?) {
        if (!BuildConfig.IS_OP) {
            if (lifecycle != null) {
                when (lifecycle) {
                    "onCreate" -> Log.v(TAG, "::::::$tag::::::onCreate")
                    "onResume" -> Log.v(TAG, "::::::$tag::::::onResume")
                    "onPause" -> Log.v(TAG, "::::::$tag::::::onPause")
                    "onRestart" -> Log.v(TAG, "::::::$tag::::::onRestart")
                    "onDestroy" -> Log.v(TAG, "::::::$tag::::::onDestroy")
                    "onCreateView" -> Log.v(
                        TAG,
                        "::::::$tag::::::onCreateView"
                    )
                    "onViewCreated" -> Log.v(
                        TAG,
                        "::::::$tag::::::onViewCreated"
                    )
                    "onDestroyView" -> Log.v(
                        TAG,
                        "::::::$tag::::::onDestroyView"
                    )
                }
            }
        }
    }
}
