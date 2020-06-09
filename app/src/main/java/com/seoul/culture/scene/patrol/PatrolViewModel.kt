package com.seoul.culture.scene.patrol

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import com.seoul.culture.model.PatrolData
import com.seoul.culture.R
import com.seoul.culture.api.APIClient
import com.seoul.culture.application.ScpApplication
import com.seoul.culture.data.PatrolListRequest
import com.seoul.culture.data.PatrolResponse
import com.seoul.culture.data.ResultResponse
import com.seoul.culture.model.PatrolCompleteData
import com.seoul.culture.model.PatrolNfcData
import com.seoul.culture.utils.Utils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.util.*
import kotlin.collections.ArrayList
import com.google.android.gms.internal.add
import com.google.gson.JsonObject
import com.google.gson.JsonElement
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.seoul.culture.scene.report.ReportViewModel
import com.seoul.culture.utils.ImageUtils
import com.seoul.culture.utils.L
import com.seoul.culture.utils.toRequestBody
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody

enum class SendType {
    PHOTO, NFC
}


class PatrolViewModel(private val context: Context) : Observable() {

    // 프로그레스
    var patrolProgress = PublishSubject.create<Boolean>()
    var completePatrol = PublishSubject.create<Boolean>()
    // 순찰시작 버튼 바인딩용
    var patrolStart: ObservableInt = ObservableInt(View.GONE)

    // 순찰 중이 아님 -> 순찰 시작 버튼 누르면 서버에서 불러와서 프리퍼런스에 저장하고 사용.
    // 순찰 중 -> 프리퍼런스에서 불러오기. 데이터 없으면 서버에서 불러오고 위랑 같이 진행.
    // 순찰 완료 -> 프리퍼런스 데이터 지우기.
    private var patrolList: ArrayList<PatrolNfcData> = ArrayList()
    private var compositeDisposable: CompositeDisposable? = CompositeDisposable()

    lateinit var cameraFilePath: Uri
    lateinit var sendType: SendType

    var placeId: String = ""
    var placeDetailId: String = ""

    var gpsLon = ""
    var gpsLat = ""


    // 처음 뷰로드 시 실행.
    fun loadData() {
        fetchPatrolList()
    }

    fun initializeViews() {
        when (ScpApplication.prefs.startPatrol) {
            true -> {
                patrolStart.set(View.GONE)
                fetchPatrolList()
            }
            false -> {
                patrolStart.set(View.VISIBLE)
            }
        }
    }


    private fun fetchPatrolList() {
        patrolProgress.onNext(true)

        val patrolList = ScpApplication.prefs.patrolList
        // 순찰 중이고, 이전에 순찰 중인 리스트를 받아온 상태.
        if (patrolList.size > 0) {
            io.reactivex.Observable.just(patrolList)
                .subscribe({ patrolResponse ->
                    patrolProgress.onNext(false)
                    ScpApplication.prefs.startPatrol = true
                    patrolStart.set(View.GONE)
                    changePatrolDataSet(patrolResponse)
                }, { error ->
                    patrolProgress.onNext(false)
                    patrolStart.set(View.VISIBLE)
                    ScpApplication.prefs.startPatrol = false

                    Toast.makeText(context, context.getString(R.string.fail_network), Toast.LENGTH_SHORT).show()
                    Log.e("Error", error.message)
                    error.printStackTrace()
                }).apply { compositeDisposable?.add(this) }
        }
        // 순찰 시작.
        else {
            val userId = ScpApplication.prefs.userId
            val placeTime = Utils.getCurrentDateString()

            val listApi = APIClient().getPatrolApi().getPatrolList(userId).doOnNext {
                if (it.resultCode != "200") {
                    Toast.makeText(context, R.string.fail_network_restart, Toast.LENGTH_SHORT).show()
                    patrolProgress.onNext(false)
                    patrolStart.set(View.VISIBLE)
                    ScpApplication.prefs.startPatrol = false
                }
            }.filter { it.resultCode == "200" }

            val nfcApi = APIClient().getPatrolApi().sendNfcTag(userId,"S", "S",placeTime, gpsLon, gpsLat)
                .doOnNext {
                    if (it.resultCode != "200") {
                        Toast.makeText(context, R.string.fail_network_restart, Toast.LENGTH_SHORT).show()
                        patrolProgress.onNext(false)
                        patrolStart.set(View.VISIBLE)
                        ScpApplication.prefs.startPatrol = false
                    }
                }.filter { it.resultCode == "200" }

            listApi
                .zipWith( nfcApi, BiFunction{
                        list: PatrolResponse, nfc: ResultResponse ->
                    list.List
                }).subscribeOn(Schedulers.io())
            .map { it.map { PatrolNfcData(it.placeId,it.placeNm, it.placeDetailId, it.placeDetailNm, "", it.nfcCont) } }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ patrolResponse ->
                patrolProgress.onNext(false)
                patrolStart.set(View.GONE)
                ScpApplication.prefs.startPatrol = true
                ScpApplication.prefs.patrolList = patrolResponse
                changePatrolDataSet(patrolResponse)
            }, { error ->
                patrolProgress.onNext(false)
                patrolStart.set(View.VISIBLE)
                ScpApplication.prefs.startPatrol = false

                Toast.makeText(context, context.getString(R.string.fail_network), Toast.LENGTH_SHORT).show()
                Log.e("Error", error.message)
                error.printStackTrace()
            }).apply { compositeDisposable?.add(this) }
        }
    }

    private fun changePatrolDataSet(patrols: List<PatrolNfcData>) {
        patrolList = ArrayList(patrols)
        setChanged()
        notifyObservers()
    }

    fun getPatrolList(): List<PatrolNfcData> {
        return patrolList
    }

    fun checkPatrolComplete(): Boolean {
        if (patrolList.map { it.placeTime.isEmpty() }.filter { it }.isEmpty()) {
            return true
        }
        return false
    }

    fun successNFC(placeId: String, placeDetailId: String) {
//        var newList: ArrayList<PatrolNfcData> = ArrayList()
//        newList.addAll(patrolList)

        ScpApplication.INSTANCE.placeId = placeId
        for (i in patrolList.indices) {
            L.d("placeId : ${placeId} || ${patrolList[i].placeId} || ${patrolList[i].placeTime}")
            if (patrolList[i].placeDetailId == placeDetailId) {
                val userId = ScpApplication.prefs.userId
                val placeTime = Utils.getCurrentDateString()
                patrolProgress.onNext(true)
                APIClient().getPatrolApi().sendNfcTag(userId, placeId, placeDetailId, placeTime, ScpApplication.INSTANCE.gpsLon, ScpApplication.INSTANCE.gpsLat)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ patrolResponse ->
                        patrolProgress.onNext(false)
                        if (patrolResponse.resultCode == "200") {
                            L.d("patrolList : ${patrolList}")
                            patrolList.set(i, PatrolNfcData(patrolList[i].placeId, patrolList[i].placeNm, patrolList[i].placeDetailId, patrolList[i].placeDetailNm, placeTime, patrolList[i].nfcCont))
                            ScpApplication.prefs.patrolList = patrolList
                            L.d("patrolList222 : ${patrolList}")
                        } else {
                            Toast.makeText(context, context.getString(R.string.patrol_nfc_fail), Toast.LENGTH_SHORT).show()
                        }

                        setChanged()
                        notifyObservers()

                    }, { error ->
                        Log.e("Error",error.message)
                        Toast.makeText(context, context.getString(R.string.fail_network), Toast.LENGTH_SHORT).show()

                        patrolProgress.onNext(false)

                        // 순찰 시작 누른 데이터로 순찰 완료 시까지 보여주기 위해서 실패 시에도 저장.
                        val patrolData = PatrolNfcData(patrolList[i].placeId, patrolList[i].placeNm, patrolList[i].placeDetailId, patrolList[i].placeDetailNm, placeTime, patrolList[i].nfcCont)
                        patrolList.set(i, patrolData)
                        ScpApplication.prefs.patrolList = patrolList

                        // 실패 데이터는 따로 모아서 순찰 완료 시에 보냄.
                        val failList = ArrayList<PatrolNfcData>()
                        failList.addAll(ScpApplication.prefs.patrolFailList)

                        error.printStackTrace()

                        setChanged()
                        notifyObservers()
                    }).apply { compositeDisposable?.add(this) }


                break
            }
        }
    }

    fun sendPatrolComplete() {
        patrolProgress.onNext(true)

        if (ScpApplication.prefs.patrolFailList.isEmpty()) {
            ScpApplication.prefs.startPatrol = false
            ScpApplication.prefs.patrolFailList = emptyList()
            ScpApplication.prefs.patrolList = emptyList()

            patrolProgress.onNext(false)
            completePatrol.onNext(true)
        } else {
            val userId = ScpApplication.prefs.userId

            val failList = JsonArray()
            val placeTime = Utils.getCurrentDateString()
            for (item in ScpApplication.prefs.patrolFailList) {
                val jo = JsonObject()
                jo.addProperty("userId",userId)
                jo.addProperty("placeId",item.placeId)
                jo.addProperty("placeDetailId",item.placeDetailId)
                jo.addProperty("placeTime",item.placeTime)
                jo.addProperty("gpsLon", gpsLon)
                jo.addProperty("gpsLat", gpsLat)
                failList.add(jo)
            }

            val objMain = JsonObject()
            if (failList.size() > 0) {
                objMain.addProperty("paramList", Gson().toJson(failList))
            }
            objMain.addProperty("userId",userId)

//        APIClient().getPatrolApi().sendCompletePatrol(PatrolListRequest(userId,failList))
            APIClient().getPatrolApi().sendCompltePatrol(userId, objMain)
                .zipWith( APIClient().getPatrolApi().sendNfcTag(userId,"E", "E",placeTime, gpsLon, gpsLat), BiFunction{
                        list: ResultResponse, nfc: ResultResponse ->
                    list
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ patrolResponse ->
                    patrolProgress.onNext(false)
                    // 데이터 초기화하고 뒤로 이동.
                    if (patrolResponse.resultCode == "200") {
                        ScpApplication.prefs.startPatrol = false
                        ScpApplication.prefs.patrolFailList = emptyList()
                        ScpApplication.prefs.patrolList = emptyList()

                        completePatrol.onNext(true)
                    } else {
                        Toast.makeText(context, context.getString(R.string.fail_network), Toast.LENGTH_SHORT).show()
//                        Toast.makeText(context, context.getString(R.string.patrol_complete_fail), Toast.LENGTH_SHORT).show()
                    }

                }, { error ->
                    patrolProgress.onNext(false)
                    Toast.makeText(context, context.getString(R.string.fail_network_restart), Toast.LENGTH_SHORT).show()

                    error.printStackTrace()
                }).apply { compositeDisposable?.add(this) }
        }
    }

    private fun unSubscribeFromObservable() {
        if (compositeDisposable != null && !compositeDisposable!!.isDisposed) {
            compositeDisposable!!.dispose()
        }
    }

    fun reset() {
        unSubscribeFromObservable()
        compositeDisposable = null
    }

    fun completeCntMoreOne(): Boolean {
        L.d("test : ${patrolList.map { it.placeTime.isNotEmpty() }.filter { it }.count()}")
        if (patrolList.map { it.placeTime.isNotEmpty() }.filter { it }.count() > 0) {
            return true
        }
        return false
    }
}