package com.seoul.culture.scene.main

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.seoul.culture.R
import com.seoul.culture.api.APIClient
import com.seoul.culture.application.ScpApplication
import com.seoul.culture.utils.L
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kr.smart.carefarm.base.BaseViewModel

class MainFragmentViewModel constructor(private val context: Context) : BaseViewModel() {
    fun fetchEmergencyReport(position: Int) {
        progress.onNext(true)
        var code = ""

        when (position) {
            0 -> {  // 화재
                code = "화재"
            }
            1 -> {  // 도난
                code = "도난"
            }
            2 -> {  // 기타
                code = "기타"
            }
        }

        APIClient().getReportApi().sendEmergencyReport(ScpApplication.prefs.userId, code, ScpApplication.INSTANCE.gpsLon, ScpApplication.INSTANCE.gpsLat)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
//            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                progress.onNext(false)
                L.d("response : ${it}")
                if (it.resultCode == "200") {
                    L.d("response 2")
                    Toast.makeText(
                        context,
                        R.string.report_emergency_complete_success,
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    L.d("response 3")
                    Toast.makeText(
                        context,
                        R.string.report_emergency_complete_fail,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                L.d("response 4")
            }, { error ->
                progress.onNext(false)
//                Toast.makeText(context, R.string.fail_network_restart, Toast.LENGTH_SHORT).show()
//                Log.e("Error : ", error.message)
//                error.printStackTrace()
            }).apply { disposables.add(this) }
    }
}
