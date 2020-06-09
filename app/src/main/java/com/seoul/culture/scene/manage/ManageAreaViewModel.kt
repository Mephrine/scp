package com.seoul.culture.scene.manage

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.databinding.ObservableField
import androidx.databinding.ObservableFloat
import androidx.databinding.ObservableInt
import androidx.lifecycle.ViewModel
import com.seoul.culture.R
import com.seoul.culture.api.APIClient
import com.seoul.culture.application.ScpApplication
import com.seoul.culture.config.C
import com.seoul.culture.data.PatrolResponse
import com.seoul.culture.model.ManageDetailData
import com.seoul.culture.model.PatrolData
import com.seoul.culture.model.PatrolNfcData
import com.seoul.culture.scene.patrol.ReceiverActivity
import com.seoul.culture.utils.NFC_TYPE
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.util.*
import kotlin.collections.ArrayList

class ManageAreaViewModel(private val context: Activity) : Observable() {
    var patrolProgress = BehaviorSubject.createDefault(false)
    var detailData = PublishSubject.create<List<ManageDetailData>>()

    private var manageList: List<PatrolData> = ArrayList()
    private var compositeDisposable: CompositeDisposable? = CompositeDisposable()

    var startPatrol: Boolean = false

    fun reloadData() {
        fetchPatrolList()
    }

    fun fetchNfcDetail(placeId: String) {
        patrolProgress.onNext(true)

        APIClient().getManageApi().nfcDetail(ScpApplication.prefs.userId ,placeId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ detailResponse ->
                patrolProgress.onNext(false)
                if (detailResponse.resultCode == "200") {
                    detailData.onNext(detailResponse.List)
//                    val intent = Intent(context, ReceiverActivity::class.java)
//                    intent.putExtra("nfcType", NFC_TYPE.WRITE.name)
//                    intent.putExtra("payload", detailResponse.results.first().nfcCont)
//                    context.startActivityForResult(intent, C.REQ_CODE_NFC_WRITE)
//                    context.overridePendingTransition(R.anim.anim_up, R.anim.anim_no)
                } else {
                    Toast.makeText(context, context.getString(R.string.fail_network_restart), Toast.LENGTH_SHORT).show()
                }
            }, { error ->
                patrolProgress.onNext(false)

                Toast.makeText(context, context.getString(R.string.fail_network_restart), Toast.LENGTH_SHORT).show()
                Log.e("Error", error.message)
                error.printStackTrace()
            }).apply { compositeDisposable?.add(this) }
    }

    private fun fetchPatrolList() {
        patrolProgress.onNext(true)

        val userId = ScpApplication.prefs.userId
        APIClient().getManageApi().nfcList(userId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ manageResponse ->
                patrolProgress.onNext(false)
                if (manageResponse.resultCode == "200") {
                    changePatrolDataSet(manageResponse.List)
                } else {
                    Toast.makeText(context, context.getString(R.string.fail_network_restart), Toast.LENGTH_SHORT).show()
                }
            }, { error ->
                patrolProgress.onNext(false)

                Toast.makeText(context, context.getString(R.string.fail_network_restart), Toast.LENGTH_SHORT).show()
                Log.e("Error", error.message)
                error.printStackTrace()
            }).apply { compositeDisposable?.add(this) }
    }

    fun inserPatrolArea(placeDetailId: String) {
        patrolProgress.onNext(true)

//        val userId = ScpApplication.prefs.userId
        APIClient().getManageApi().insertNfcWrite(placeDetailId, ScpApplication.INSTANCE.gpsLat, ScpApplication.INSTANCE.gpsLon)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ patrolResponse ->
                patrolProgress.onNext(false)
                if (patrolResponse.resultCode == "200") {
                    Toast.makeText(context, context.getString(R.string.manage_insert_success), Toast.LENGTH_SHORT).show()

                    // 등록 완료되면 다시 불러오기
                    fetchPatrolList()
                } else {
                    Toast.makeText(context, context.getString(R.string.manage_insert_fail), Toast.LENGTH_SHORT).show()
                }

            }, { error ->
                patrolProgress.onNext(false)
                Toast.makeText(context, context.getString(R.string.fail_network_restart), Toast.LENGTH_SHORT).show()

                error.printStackTrace()
            }).apply { compositeDisposable?.add(this) }
    }

    fun deletePatrolArea(manageItem: PatrolData) {
        patrolProgress.onNext(true)

        val userId = ScpApplication.prefs.userId
        APIClient().getManageApi().deletePatrol(userId, manageItem.placeId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ patrolResponse ->
                patrolProgress.onNext(false)
                if (patrolResponse.resultCode == "200") {
                    Toast.makeText(context, context.getString(R.string.manage_delete_success), Toast.LENGTH_SHORT).show()

                    // 삭제 완료되면 다시 불러오기
                    fetchPatrolList()
                } else {
                    Toast.makeText(context, context.getString(R.string.manage_delete_fail), Toast.LENGTH_SHORT).show()
                }

            }, { error ->
                patrolProgress.onNext(false)
                Toast.makeText(context, context.getString(R.string.fail_network_restart), Toast.LENGTH_SHORT).show()

                error.printStackTrace()
            }).apply { compositeDisposable?.add(this) }
    }

    private fun changePatrolDataSet(patrols: List<PatrolData>) {
        manageList = patrols
        setChanged()
        notifyObservers()
    }

    fun getManageList(): List<PatrolData> {
        return manageList
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