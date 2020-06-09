package com.seoul.culture.scene.login

import android.Manifest
import android.content.Context
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import com.seoul.culture.R
import com.seoul.culture.api.APIClient
import com.seoul.culture.application.ScpApplication
import com.seoul.culture.model.UserData
import com.seoul.culture.model.UuidData
import com.seoul.culture.utils.L
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import org.json.JSONObject
import java.util.ArrayList


class LoginViewModel constructor(private val context: Context) : ViewModel() {
    var progress = PublishSubject.create<Boolean>()
    var moveMain = BehaviorSubject.createDefault(false)
    var userList = PublishSubject.create<ArrayList<UserData>>()
    var sbjUUID = PublishSubject.create<String>()

    var isFirst = true

    private val disposables by lazy {
        CompositeDisposable()
    }

    fun startLogin() {
        // 우선순위 - ADID > SSAID > UUID
        if(chkEnableLogin()) {
            return
        }

        Observable.fromCallable { AdvertisingIdClient.getAdvertisingIdInfo(context) }
            .map { it.id }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe( {
                requestLogin(it)
            }, {
                getUUID()
            }).apply { disposables.add(this) }

    }

    fun chkEnableLogin(): Boolean {
        val patrol = ScpApplication.prefs.startPatrol
        val training = ScpApplication.prefs.startTraining
        val userId = ScpApplication.prefs.userId
        // 순찰, 훈련 중이면 현재 아이디로 자동 진행.
        if (patrol || training) {
            L.d("userId : ${userId}")
            if (userId.isEmpty()) {
                // 다 초기화하기.
                ScpApplication.prefs.startTraining = false
                ScpApplication.prefs.trainingFailList = emptyList()
                ScpApplication.prefs.trainingList = emptyList()

                ScpApplication.prefs.startPatrol = false
                ScpApplication.prefs.patrolFailList = emptyList()
                ScpApplication.prefs.patrolList = emptyList()
            } else {
                moveMain.onNext(true)
                return true
            }
        }

        return false
    }

    val uuid: String
        get() = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

    private fun getUUID() {
        val permissionlistener: PermissionListener = object : PermissionListener {
            override fun onPermissionGranted() {
                ScpApplication.prefs.uuid = uuid
                sbjUUID.onNext(uuid)
                requestLogin(uuid)
            }

            override fun onPermissionDenied(deniedPermissions: ArrayList<String>) {
                Toast.makeText(
                    context,
                    R.string.report_permission_denied,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        checkPermission(permissionlistener)
    }

    fun requestUUID() {
        val permissionlistener: PermissionListener = object : PermissionListener {
            override fun onPermissionGranted() {
                ScpApplication.prefs.uuid = uuid
                sbjUUID.onNext(uuid)
            }

            override fun onPermissionDenied(deniedPermissions: ArrayList<String>) {
                Toast.makeText(
                    context,
                    R.string.report_permission_denied,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        checkPermission(permissionlistener)
    }

    private fun checkPermission(permissionlistener: PermissionListener) {
        TedPermission.with(context)
            .setPermissionListener(permissionlistener)
            .setDeniedMessage(R.string.report_permission_denied_alert)
            .setPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .check()
    }

    fun requestLogin(uuid: String) {
        APIClient().getLoginApi().sendLogin(uuid)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ patrolResponse ->
                L.d("aaa : ${patrolResponse}")
                if (patrolResponse.resultCode == "200") {
                    isFirst = false
                    userList.onNext(ArrayList(patrolResponse.List))

                    progress.onNext(false)
                } else {
                    Toast.makeText(context, R.string.fail_login, Toast.LENGTH_SHORT).show()
                }
            }, { error ->
                progress.onNext(false)
                Log.e("Error : ", error.message)
                error.printStackTrace()
                Toast.makeText(context, R.string.fail_network_restart, Toast.LENGTH_SHORT).show()
            }).apply { disposables.add(this) }
    }

    override fun onCleared() {
        super.onCleared()
        if (!disposables.isDisposed) {
            disposables.clear()
        }
    }

}