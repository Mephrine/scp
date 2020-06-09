package com.seoul.culture.application

import android.util.Log
import androidx.multidex.MultiDexApplication
import com.seoul.culture.utils.LoadingDialog
import com.seoul.culture.utils.ScpSharedPreferences
//import com.squareup.leakcanary.LeakCanary


class ScpApplication : MultiDexApplication() {
    init {
        INSTANCE = this
    }

    companion object {
        lateinit var INSTANCE: ScpApplication
        lateinit var prefs : ScpSharedPreferences
    }

    var gpsLon = ""
    var gpsLat = ""
    var placeId = ""

    override fun onCreate() {
        super.onCreate()
        prefs = ScpSharedPreferences(applicationContext)
//        setLeakCanary()
    }

//    private fun setLeakCanary() {
//        if (LeakCanary.isInAnalyzerProcess(this)) { // This process is dedicated to LeakCanary for heap analysis.
//// You should not init your app in this process.
//            return
//        }
//        LeakCanary.install(this)
//    }

}
