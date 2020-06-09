package com.seoul.culture.scene.main

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.seoul.culture.R
import com.seoul.culture.api.APIClient
import com.seoul.culture.application.ScpApplication
import com.seoul.culture.utils.L
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import java.util.*
import kotlin.collections.ArrayList

class MainViewModel(private val context: Context) : ViewModel() {
    var userName: BehaviorSubject<String> = BehaviorSubject.create()
    var sbjChkLogout: BehaviorSubject<Boolean> = BehaviorSubject.create()

    private val disposables by lazy {
        CompositeDisposable()
    }


    fun setUser() {
        userName.onNext(ScpApplication.prefs.userName)
    }


    fun chkEnableLogout(): Boolean {
//        val patrol = ScpApplication.prefs.startPatrol
//        val training = ScpApplication.prefs.startTraining
//
//        if (!patrol && !training) {
            // 다 초기화하기.
            ScpApplication.prefs.startTraining = false
            ScpApplication.prefs.trainingFailList = emptyList()
            ScpApplication.prefs.trainingList = emptyList()

            ScpApplication.prefs.startPatrol = false
            ScpApplication.prefs.patrolFailList = emptyList()
            ScpApplication.prefs.patrolList = emptyList()

            ScpApplication.prefs.gpsInfoList = emptyList()

            ScpApplication.prefs.userId = ""
            ScpApplication.prefs.userName = ""
            return true
//        }

        return false
    }

    fun chkLogout(): Boolean {
        APIClient().getLoginApi().sendLogin(ScpApplication.prefs.uuid)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ patrolResponse ->
                if (patrolResponse.resultCode == "200") {
                    if (patrolResponse.List.filter { it.userId == ScpApplication.prefs.userId }.count() < 1) {
                        sbjChkLogout.onNext(true)
                    }
                }
            }, { error ->
                error.printStackTrace()
            }).apply { disposables.add(this) }


        return true
    }

    override fun onCleared() {
        super.onCleared()
        if (!disposables.isDisposed) {
            disposables.clear()
        }
    }
}