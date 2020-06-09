package com.seoul.culture.scene.manage

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableArrayList
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.seoul.culture.R
import com.seoul.culture.application.ScpApplication
import com.seoul.culture.config.C
import com.seoul.culture.databinding.FragmentManageAreaBinding
import com.seoul.culture.model.ManageDetailData
import com.seoul.culture.model.PatrolData
import com.seoul.culture.scene.manage.ManageAreaAdapter
import com.seoul.culture.scene.manage.ManageAreaFragment
import com.seoul.culture.scene.manage.ManageAreaViewModel
import com.seoul.culture.scene.patrol.ReceiverActivity
import com.seoul.culture.utils.*
import com.squareup.otto.Subscribe
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.fragment_manage_area.*
import kotlinx.android.synthetic.main.fragment_manage_input.*
import java.util.*

class ManageAreaFragment: Fragment(), Observer {
    override fun update(observable: java.util.Observable?, arg: Any?) {
        if (observable is ManageAreaViewModel) {
            binding.listManage.background = resources.getDrawable(R.color.color_line_bg)
            val manageAdapter = binding.listManage.adapter as ManageAreaAdapter
            manageAdapter.setManageList(observable.getManageList())
        }
    }
    val TAG = ManageAreaFragment::class.java.simpleName

    private lateinit var binding: FragmentManageAreaBinding
    private lateinit var viewModel: ManageAreaViewModel
    private lateinit var manageList: ObservableArrayList<PatrolData>
    private lateinit var loading: LoadingDialog



    private val disposables by lazy {
        CompositeDisposable()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentManageAreaBinding.inflate(inflater, container, false)
        initDataBinding()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        BusProvider.instance.register(this)
        viewModel.reloadData()

        list_manage.addItemDecoration(
            DividerItemDecoration(4,3)
        )
        context?.let {
            loading = LoadingDialog(it)
        }
    }

    private fun initDataBinding() {

        activity?.let {
            viewModel = ManageAreaViewModel(it)
            binding.viewModel = viewModel
            binding.view = this
            setupListManageView(binding.listManage)
            setupList()
            setupObserver(viewModel)

            viewModel.patrolProgress
                .subscribeOn(AndroidSchedulers.mainThread())
                .share()
                .subscribe{
                    loading.show(it)
                }.apply { disposables.add(this) }

            viewModel.detailData
                .subscribe {
                    showAlertDialog(it)
                }.apply { disposables.add(this) }
        }

    }

    private fun setupListManageView(listManage: RecyclerView) {
        activity?.let {
            val adapter = ManageAreaAdapter(it, items = emptyList(), viewModel = viewModel)
            listManage.adapter = adapter
            listManage.layoutManager = GridLayoutManager(it, 3)
        }

    }

    private fun setupList() {
        manageList = ObservableArrayList()
        binding.manageList = manageList
    }

    private fun setupObserver(observable: java.util.Observable) {
        observable.addObserver(this)
    }

    override fun onDestroyView() {
        BusProvider.instance.unregister(this)
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.reset()
        if (!disposables.isDisposed()) {
            disposables.dispose()
        }
    }

    fun showAlertDialog(data: List<ManageDetailData>) {
        context?.let {
//            val items = resources.getStringArray(R.array.report_picker)
            if (data.size > 1) {
                val listItem = data.map { it.placeDetailNm }
                val items = listItem.map { s -> s as CharSequence }.toTypedArray()
                val dialog: AlertDialog.Builder = AlertDialog.Builder(it)
                dialog.setTitle(R.string.manage_nfc_picker_title)
                dialog.setItems(items, DialogInterface.OnClickListener { dialog, position ->
                    showNfcTag(data[position].nfcCont, data[position].placeDetailId)
                })
                dialog.setPositiveButton(R.string.picker_cancel, DialogInterface.OnClickListener { dialog, which ->
                    dialog.dismiss()
                })
                val alert: AlertDialog = dialog.create()
                alert.show()
            } else if (data.size > 0) {
                val nfcCont = Utils.setNfcCont(data.first().nfcCont, ScpApplication.INSTANCE.gpsLat, ScpApplication.INSTANCE.gpsLon)

                val intent = Intent(context, ReceiverActivity::class.java)
                intent.putExtra("nfcType", NFC_TYPE.WRITE.name)
                intent.putExtra("payload", nfcCont)
                intent.putExtra("placeDetailId", data.first().placeDetailId)
                activity?.startActivityForResult(intent, C.REQ_CODE_NFC_WRITE)
                activity?.overridePendingTransition(R.anim.anim_up, R.anim.anim_no)
            }
        }
    }

    fun showNfcTag(nfcCont: String, placeDetailId: String) {
        val nfcCont = Utils.setNfcCont(nfcCont, ScpApplication.INSTANCE.gpsLat, ScpApplication.INSTANCE.gpsLon)
        val intent = Intent(context, ReceiverActivity::class.java)
        intent.putExtra("nfcType", NFC_TYPE.WRITE.name)
        intent.putExtra("payload", nfcCont)
        intent.putExtra("placeDetailId", placeDetailId)
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
                val placeId = data?.getStringExtra("placeDetailId")?.let {
                    viewModel.inserPatrolArea(it)
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }

    }
}