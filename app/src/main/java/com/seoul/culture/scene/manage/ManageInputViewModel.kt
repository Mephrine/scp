package com.seoul.culture.scene.manage

import android.app.Activity
import android.content.Context.LOCATION_SERVICE
import android.location.LocationListener
import android.location.LocationManager
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.android.gms.location.*
import com.seoul.culture.R
import com.seoul.culture.api.APIClient
import com.seoul.culture.application.ScpApplication
import com.seoul.culture.data.ResultResponse
import com.seoul.culture.data.TrainingResponse
import com.seoul.culture.model.TrainingData
import com.seoul.culture.utils.L
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import java.util.*

class ManageInputViewModel(private val context: Activity) : Observable() {

    private val disposables by lazy {
        CompositeDisposable()
    }

    var progress = BehaviorSubject.createDefault(false)

    private var nfcCont = ""

    fun fetchNfcWriteData() {
        progress.onNext(true)

//        APIClient().getManageApi().insertNfcWrite(ScpApplication.prefs.userId, nfcCont)
//            .observeOn(AndroidSchedulers.mainThread())
//            .subscribe({ trainingResponse ->
//                progress.onNext(false)
//                Toast.makeText(context, context.getString(R.string.manage_nfc_server_success), Toast.LENGTH_SHORT).show()
//            }, { error ->
//                progress.onNext(false)
//                Toast.makeText(context, context.getString(R.string.fail_network), Toast.LENGTH_SHORT).show()
//                Log.e("Error", error.message)
//                error.printStackTrace()
//            }).apply { disposables.add(this) }
    }

    fun validation(code: String, lon: String, lat: String): Boolean {
        if (code.isNotEmpty() && lon.isNotEmpty() && lat.isNotEmpty()) {
            return true
        }
        return false
    }
}