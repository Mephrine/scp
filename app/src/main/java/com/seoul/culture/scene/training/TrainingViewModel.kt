package com.seoul.culture.scene.training

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.seoul.culture.model.TrainingData
import com.seoul.culture.R
import com.seoul.culture.api.APIClient
import com.seoul.culture.application.ScpApplication
import com.seoul.culture.data.ResultResponse
import com.seoul.culture.data.TrainingResponse
import com.seoul.culture.model.PatrolNfcData
import com.seoul.culture.model.TrainingCompleteData
import com.seoul.culture.scene.patrol.SendType
import com.seoul.culture.utils.ImageUtils
import com.seoul.culture.utils.L
import com.seoul.culture.utils.Utils
import com.seoul.culture.utils.toRequestBody
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.util.*
import kotlin.collections.ArrayList

class TrainingViewModel(private val context: Context) : Observable() {

    // 프로그레스
    var trainingProgress = BehaviorSubject.createDefault(false)
    var completeTraining = PublishSubject.create<Boolean>()
    // 순찰시작 버튼 바인딩용
    var trainingStart: ObservableInt = ObservableInt(View.GONE)

    // 순찰 중이 아님 -> 순찰 시작 버튼 누르면 서버에서 불러와서 프리퍼런스에 저장하고 사용.
    // 순찰 중 -> 프리퍼런스에서 불러오기. 데이터 없으면 서버에서 불러오고 위랑 같이 진행.
    // 순찰 완료 -> 프리퍼런스 데이터 지우기.
    private var trainingList: ArrayList<TrainingData> = ArrayList()
    private var compositeDisposable: CompositeDisposable? = CompositeDisposable()

    lateinit var cameraFilePath: Uri
    lateinit var sendType: SendType

    var placeId: String = ""
    var placeDetailId: String = ""
    var simulId = ""

    // 처음 뷰로드 시 실행.
    fun loadData() {
        fetchTrainingList()
    }

    fun initializeViews() {
        trainingStart.set(View.VISIBLE)
        ScpApplication.prefs.trainingList = emptyList()
        ScpApplication.prefs.trainingFailList = emptyList()
//        when (ScpApplication.prefs.startTraining) {
//            true -> {
//                trainingStart.set(View.GONE)
//                fetchTrainingList()
//            }
//            false -> {
//                trainingStart.set(View.VISIBLE)
//            }
//        }
    }

    private fun fetchTrainingList() {
        trainingProgress.onNext(true)

        val trainingList = ScpApplication.prefs.trainingList
        // 순찰 중이고, 이전에 순찰 중인 리스트를 받아온 상태.
        if (trainingList.size > 0) {
            io.reactivex.Observable.just(trainingList)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ trainingResponse ->
                    trainingProgress.onNext(false)

                    if (trainingResponse.isEmpty()) {
                        completeTraining.onNext(false)
                    } else {
                        trainingStart.set(View.GONE)
                        ScpApplication.prefs.startTraining = true
                        changeTrainingDataSet(trainingResponse)
                    }
                }, { error ->
                    trainingProgress.onNext(false)
                    trainingStart.set(View.VISIBLE)
                    ScpApplication.prefs.startTraining = false

                    Toast.makeText(context, context.getString(R.string.fail_network), Toast.LENGTH_SHORT).show()
                    Log.e("Error", error.message)
                    error.printStackTrace()
                }).apply { compositeDisposable?.add(this) }
        }
        // 순찰 시작.
        else {
            val userIdStr = ScpApplication.prefs.userId
            val gpsLonStr = ScpApplication.INSTANCE.gpsLon
            val gpsLatStr = ScpApplication.INSTANCE.gpsLat


            APIClient().getTrainingApi().getTrainingList(userIdStr)
                .doOnNext {
                    if (it.resultCode != "200") {
                        trainingProgress.onNext(false)
                        Toast.makeText(context, R.string.fail_network_restart, Toast.LENGTH_SHORT).show()
                    }
                }
                .filter { it.resultCode == "200" }
                .map { it.List.map { item -> TrainingData(userIdStr, item.nfcYn, item.orderMsg, item.simulId, item.planSeq, item.placeDetailId, item.placeDetailNm, item.nfcCont, item.nfcCd, "", gpsLatStr, gpsLonStr) } }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ trainingResponse ->
                    ScpApplication.prefs.trainingList = trainingResponse
                    changeTrainingDataSet(trainingResponse)
                    sendStartSign(trainingResponse.firstOrNull()?.simulId ?: "0")
                }, { error ->
                    trainingProgress.onNext(false)

                    Toast.makeText(context, context.getString(R.string.fail_network), Toast.LENGTH_SHORT).show()
                    Log.e("Error", error.message)
                    error.printStackTrace()
                }).apply { compositeDisposable?.add(this) }

        }
    }

    // 시작 종료 -> planSeq에 simulId값을 전달해야함!!!!!
    private  fun sendStartSign(simulId: String) {
        val placeTimeStr = Utils.getCurrentDateString()
        val userIdStr = ScpApplication.prefs.userId
        val gpsLonStr = ScpApplication.INSTANCE.gpsLon
        val gpsLatStr = ScpApplication.INSTANCE.gpsLat

        val userId = toRequestBody(userIdStr)
        val placeId = toRequestBody(simulId)
        val simulId = toRequestBody("S")
        val gpsLon = toRequestBody(gpsLonStr)
        val gpsLat = toRequestBody(gpsLatStr)
        val placeDetailId = toRequestBody("0")
        val placeTime = toRequestBody(placeTimeStr)

        L.d("simulId : ${simulId}")

        if (userId != null && placeId != null && placeDetailId != null && simulId != null && gpsLon != null && gpsLat != null && placeTime != null) {
            APIClient().getTrainingApi().sendNfcTag(null, userId!!, placeId!!, simulId!!, gpsLat!!, gpsLon!!, placeDetailId!!, placeTime!!)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ trainingResponse ->
                    L.d("test1 ####")
                    trainingProgress.onNext(false)
                    if (trainingResponse.resultCode != "200") {
                        Toast.makeText(context, R.string.fail_network, Toast.LENGTH_SHORT).show()
                        trainingStart.set(View.VISIBLE)
                        ScpApplication.prefs.startTraining = false

                        Toast.makeText(context, context.getString(R.string.fail_network), Toast.LENGTH_SHORT).show()
                    } else {
                        ScpApplication.prefs.startTraining = true
                        trainingStart.set(View.GONE)
                    }
                }, { error ->
                    trainingProgress.onNext(false)
                    trainingStart.set(View.VISIBLE)
                    ScpApplication.prefs.startTraining = false

                    Toast.makeText(context, context.getString(R.string.fail_network), Toast.LENGTH_SHORT).show()
                    Log.e("Error", error.message)
                    error.printStackTrace()
                }).apply { compositeDisposable?.add(this) }
        }
    }

    private fun changeTrainingDataSet(trainings: List<TrainingData>) {
        trainingList = ArrayList(trainings)
        setChanged()
        notifyObservers()
    }

    fun getTrainingList(): List<TrainingData> {
        return trainingList
    }

    fun checkTrainingComplete(): Boolean {
        if (trainingList.map { it.placeTime?.isEmpty() }.filter { it ?: true }.isEmpty()) {
            return true
        }
        return false
    }

    fun successNFC() {
        trainingProgress.onNext(true)
        val placeTimeStr = Utils.getCurrentDateString()
        val userIdStr = ScpApplication.prefs.userId
        val gpsLonStr = ScpApplication.INSTANCE.gpsLon
        val gpsLatStr = ScpApplication.INSTANCE.gpsLat

        val userId = toRequestBody(userIdStr)
        val placeId = toRequestBody(placeId)
        val simulId = toRequestBody(simulId)
        val gpsLon = toRequestBody(gpsLonStr)
        val gpsLat = toRequestBody(gpsLatStr)
        val placeDetailId = toRequestBody(placeDetailId)
        val placeTime = toRequestBody(placeTimeStr)


//        userId, placeId, simulId, ScpApplication.INSTANCE.gpsLon, ScpApplication.INSTANCE.gpsLat, placeDetailId, placeTime

        var imageUpload: MultipartBody.Part? = null
        cameraFilePath?.path?.let {
            //            val file = File(it)
            try {
                val file = ImageUtils.bitmapToFile(context, it)

                //multipart/form-data
                val imageBody = RequestBody.create(MediaType.parse("image/jpg"), file)
                imageUpload = MultipartBody.Part.createFormData("imgUpload", file.name, imageBody)
            } catch (e: Exception) {

            }
        }

        if (userId != null && placeId != null && placeDetailId != null && simulId != null && gpsLon != null && gpsLat != null && placeTime != null) {
            for (i in trainingList.indices) {
                if (trainingList[i].planSeq == this.placeId) {
                    APIClient().getTrainingApi().sendNfcTag(imageUpload, userId!!, placeId!!, simulId!!, gpsLat!!, gpsLon!!, placeDetailId!!, placeTime!!)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            { res ->
                                trainingProgress.onNext(false)
                                //서버 응답후 처리.
                                if (res.resultCode == "200") {
                                    trainingList.set(i, TrainingData(userIdStr, trainingList[i].nfcYn, trainingList[i].orderMsg, trainingList[i].simulId, trainingList[i].planSeq, trainingList[i].placeDetailId, trainingList[i].placeDetailNm, trainingList[i].nfcCont, trainingList[i].nfcCd, placeTimeStr, gpsLatStr, gpsLonStr))
                                    L.d("list : ${trainingList}")
                                    L.d("list : ${placeTimeStr}")

                                    ScpApplication.prefs.trainingList = trainingList
                                } else {
                                    Toast.makeText(context, context.getString(R.string.patrol_nfc_fail), Toast.LENGTH_SHORT).show()
                                }
                                setChanged()
                                notifyObservers()
                            },
                            { e ->
                                trainingProgress.onNext(false)
                                Toast.makeText(context, context.getString(R.string.fail_network), Toast.LENGTH_SHORT).show()

                                // 순찰 시작 누른 데이터로 순찰 완료 시까지 보여주기 위해서 실패 시에도 저장.
                                val trainingData = TrainingData(userIdStr, trainingList[i].nfcYn, trainingList[i].orderMsg, trainingList[i].simulId, trainingList[i].planSeq, trainingList[i].placeDetailId, trainingList[i].placeDetailNm, trainingList[i].nfcCont, trainingList[i].nfcCd, placeTimeStr, gpsLatStr, gpsLonStr)
                                trainingList.set(i, trainingData)
                                ScpApplication.prefs.trainingList = trainingList

                                // 실패 데이터는 따로 모아서 순찰 완료 시에 보냄.
                                val failList = ArrayList<TrainingData>()
                                failList.addAll(ScpApplication.prefs.trainingFailList)
                                ScpApplication.prefs.trainingFailList = failList

                                e.printStackTrace()

                                setChanged()
                                notifyObservers()
                            }
                        )
                    break
                }
            }
        }
    }

    fun sendTrainingComplete() {
        trainingProgress.onNext(true)

        if (ScpApplication.prefs.trainingFailList.isEmpty() ) {
            ScpApplication.prefs.startTraining = false
            ScpApplication.prefs.trainingFailList = emptyList()
            ScpApplication.prefs.trainingList = emptyList()

            trainingProgress.onNext(false)
            completeTraining.onNext(true)
        } else {
            val placeTimeStr = Utils.getCurrentDateString()
            val userIdStr = ScpApplication.prefs.userId
            val gpsLonStr = ScpApplication.INSTANCE.gpsLon
            val gpsLatStr = ScpApplication.INSTANCE.gpsLat
            val simulIdStr = trainingList.first().simulId

            val userId = toRequestBody(userIdStr)
            val placeId = toRequestBody(simulIdStr)
            val simulId = toRequestBody("E")
            val gpsLon = toRequestBody(gpsLonStr)
            val gpsLat = toRequestBody(gpsLatStr)
            val placeDetailId = toRequestBody("0")
            val placeTime = toRequestBody(placeTimeStr)


            if (userId != null && placeId != null && placeDetailId != null && simulId != null && gpsLon != null && gpsLat != null && placeTime != null) {
                val failList = JsonArray()

                for (item in ScpApplication.prefs.trainingFailList) {
                    val jo = JsonObject()
                    jo.addProperty("userId",userIdStr)
                    jo.addProperty("simulId",item.simulId)
                    jo.addProperty("placeId",item.planSeq)
                    jo.addProperty("gpsLon",item.gpsLon ?: "")
                    jo.addProperty("gpsLat",item.gpsLat ?: "")
                    jo.addProperty("placeDetailId",item.placeDetailId ?: "")
                    jo.addProperty("placeTime",item.placeTime ?: "")
                    failList.add(jo)
                }
                val objMain = JsonObject()
                if (failList.size() > 0) {
                    objMain.addProperty("paramList", Gson().toJson(failList))
                }
                objMain.addProperty("userId",userIdStr)

                APIClient().getTrainingApi().sendCompleteTraining(userIdStr, objMain)
                    .zipWith( APIClient().getTrainingApi().sendNfcTag(null, userId!!, placeId!!, simulId!!, gpsLat!!, gpsLon!!, placeDetailId!!, placeTime!!), BiFunction { list: ResultResponse, nfc: ResultResponse ->
                        list
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ trainingResponse ->
                        trainingProgress.onNext(false)
                        // 데이터 초기화하고 뒤로 이동.
                        if (trainingResponse.resultCode == "200") {
                            ScpApplication.prefs.startTraining = false
                            ScpApplication.prefs.trainingFailList = emptyList()
                            ScpApplication.prefs.trainingList = emptyList()

                            completeTraining.onNext(true)
                        } else {
                            Toast.makeText(context, context.getString(R.string.fail_network), Toast.LENGTH_SHORT).show()
                        }

                    }, { error ->
                        trainingProgress.onNext(false)
                        Toast.makeText(context, context.getString(R.string.fail_network_restart), Toast.LENGTH_SHORT).show()

                        error.printStackTrace()
                    }).apply { compositeDisposable?.add(this) }
            }
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
}