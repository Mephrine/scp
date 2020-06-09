package com.seoul.culture.scene.manage

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.databinding.ObservableArrayList
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import com.seoul.culture.R
import com.seoul.culture.application.ScpApplication
import com.seoul.culture.config.C
import com.seoul.culture.databinding.FragmentManageAreaBinding
import com.seoul.culture.databinding.FragmentManageInputBinding
import com.seoul.culture.model.ManageDetailData
import com.seoul.culture.model.PatrolData
import com.seoul.culture.scene.patrol.ReceiverActivity
import com.seoul.culture.utils.*
import com.squareup.otto.Subscribe
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_manage_area.*
import kotlinx.android.synthetic.main.fragment_manage_input.*
import java.util.*


class ManageInputFragment: Fragment() {
    val TAG = ManageInputFragment::class.java.simpleName

    private lateinit var binding: FragmentManageInputBinding
    private lateinit var viewModel: ManageInputViewModel
    private lateinit var loading: LoadingDialog

    private val disposables by lazy {
        CompositeDisposable()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentManageInputBinding.inflate(inflater, container, false)
        initDataBinding()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        BusProvider.instance.register(this)

        context?.let {
            loading = LoadingDialog(it)
            gpsForeground()

            val flowableCode = et_code.addTextWatcher()
                .subscribeOn(Schedulers.io())
                .filter { it.type == EditTextFlow.Type.AFTER }
                .map { it.query }
                .distinctUntilChanged()
                .toObservable()

            val flowableLat = et_lat.addTextWatcher()
                .subscribeOn(Schedulers.io())
                .filter { it.type == EditTextFlow.Type.AFTER }
                .map { it.query }
                .distinctUntilChanged()
                .toObservable()

            val flowableLon = et_lon.addTextWatcher()
                .subscribeOn(Schedulers.io())
                .filter { it.type == EditTextFlow.Type.AFTER }
                .map { it.query }
                .distinctUntilChanged()
                .toObservable()
//
            Observable.combineLatest(arrayOf(flowableCode, flowableLat, flowableLon), {
                viewModel.validation(
                    it[0].toString(),
                    it[1].toString(),
                    it[2].toString()
                )
            })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    btn_regist_nfc.isEnabled = it
                }.apply { disposables.add(this) }
        }
    }

    fun gpsForeground() {
        val permissionlistener: PermissionListener = object : PermissionListener {
            override fun onPermissionGranted() {
                context?.let {
                    et_lat.setText(ScpApplication.INSTANCE.gpsLat)
                    et_lon.setText(ScpApplication.INSTANCE.gpsLon)
                }
            }

            override fun onPermissionDenied(deniedPermissions: ArrayList<String>) {
                Toast.makeText(
                    activity,
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
            .setPermissions(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)
            .check()
    }

    private fun initDataBinding() {

        activity?.let {
            viewModel = ManageInputViewModel(it)
            binding.viewModel = viewModel
            binding.view = this

            viewModel.progress
                .subscribeOn(AndroidSchedulers.mainThread())
                .share()
                .subscribe{
                    loading.show(it)
                }.apply { disposables.add(this) }

//            viewModel.sbjLocation
//                .subscribeOn(AndroidSchedulers.mainThread())
//                .subscribe {
//                    et_lat.setText(it.latitude.toString())
//                    et_lon.setText(it.longitude.toString())
//                }.apply { disposables.add(this) }

        }

    }


    override fun onDestroyView() {
        BusProvider.instance.unregister(this)
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!disposables.isDisposed()) {
            disposables.dispose()
        }
    }

    fun onClick(view: View) {
        val nfcCont = Utils.setNfcCont(et_code.text.toString(), et_lat.text.toString(), et_lon.text.toString())
        Log.d("aa","nfcCont = "+nfcCont)
        val intent = Intent(context, ReceiverActivity::class.java)
        intent.putExtra("nfcType", NFC_TYPE.WRITE.name)
        intent.putExtra("payload", nfcCont)
        activity?.startActivityForResult(intent, C.REQ_CODE_NFC_WRITE)
        activity?.overridePendingTransition(R.anim.anim_up, R.anim.anim_no)
    }

    @Subscribe
    public fun onActivityResult(activityResultEvent: ActivityResultEvent){
        onActivityResult(activityResultEvent.requestCode, activityResultEvent.resultCode, activityResultEvent.intent)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
//            C.REQ_CODE_NFC_DELIVERY -> if (resultCode == Activity.RESULT_OK) {
//                val placeId = data?.getStringExtra("placeId")?.let {
//                    viewModel.inserPatrolArea(it)
//                }
//            } else if (resultCode == Activity.RESULT_CANCELED) {
//
//            }
            C.REQ_CODE_NFC_WRITE -> if (resultCode == Activity.RESULT_OK) {
//                val placeId = data?.getStringExtra("placeId")?.let {
//                    viewModel.inserPatrolArea(it)
//                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }
}