package com.seoul.culture.scene.training

import android.Manifest
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.FileProvider
import androidx.databinding.ObservableArrayList
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import com.seoul.culture.R
import com.seoul.culture.application.ScpApplication
import com.seoul.culture.config.C
import com.seoul.culture.databinding.FragmentTrainingBinding
import com.seoul.culture.model.TrainingData
import com.seoul.culture.utils.*
import com.squareup.otto.Subscribe
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_training.*
import kr.smart.carefarm.base.BaseBindingFragment
import kr.smart.carefarm.base.BaseFragment
import java.util.*
import java.util.concurrent.TimeUnit



class TrainingFragment : Fragment(), Observer {
    override fun update(observable: java.util.Observable?, arg: Any?) {
        if (observable is TrainingViewModel) {
            binding.listTraining.background = resources.getDrawable(R.color.color_line_bg)
            val trainingAdapter = binding.listTraining.adapter as TrainingAdapter
            trainingAdapter.setTrainingList(observable.getTrainingList())
        }
    }

    val TAG = TrainingFragment::class.java.simpleName

    private lateinit var binding: FragmentTrainingBinding
    private lateinit var viewModel: TrainingViewModel
    private lateinit var trainingList: ObservableArrayList<TrainingData>

    private lateinit var loading: LoadingDialog

    private lateinit var navController: NavController

    private lateinit var callback: OnBackPressedCallback

    private val disposables by lazy {
        CompositeDisposable()
    }


//    val adapter = TrainingAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setBackButtonDispatcher()
    }

    private fun setBackButtonDispatcher() {
        callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBackPressed()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentTrainingBinding.inflate(inflater, container, false)
        initDataBinding()

//        list_training.addItemDecoration(DividerItemDecoration(context,GridLayoutManager.VERTICAL))

        return binding.root

//        return inflater.inflate(R.layout.fragment_training, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        BusProvider.instance.register(this)
        list_training.addItemDecoration(
            DividerItemDecoration(4,3)
        )

        navController = Navigation.findNavController(view)

        Observable.create<View> { btn_complete.setOnClickListener(it::onNext) }
            .debounce(300, TimeUnit.MILLISECONDS)
            .map { viewModel.checkTrainingComplete() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it) {
                    viewModel.sendTrainingComplete()
                } else {
                    AlertDialog.Builder(activity)
                        .setMessage(R.string.training_not_complete)
                        .setPositiveButton(
                            android.R.string.ok,
                            DialogInterface.OnClickListener { dialog, which ->
                                dialog.dismiss()
                            })
                        .create()
                        .show()
                }
            }.apply { disposables.add(this) }

        context?.let {
            loading = LoadingDialog(it)
        }
    }

    override fun onDestroyView() {
        BusProvider.instance.unregister(this)
        super.onDestroyView()
    }

    private fun initDataBinding() {

        context?.let {
            viewModel = TrainingViewModel(it)
            binding.viewModel = viewModel
            binding.view = this
            setupListPeopleView(binding.listTraining)
            setupList()
            setupObserver(viewModel)
            viewModel.initializeViews()

            viewModel.trainingProgress
                .subscribeOn(AndroidSchedulers.mainThread())
                .share()
                .subscribe{
                    loading.show(it)
                }.apply { disposables.add(this) }

            viewModel.completeTraining
                .subscribeOn(AndroidSchedulers.mainThread())
                .filter { it }
                .subscribe { isSuccess ->
                    activity?.let {
                        if (isSuccess){
                            Toast.makeText(it, getString(R.string.training_complete_success), Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        } else {
                            AlertDialog.Builder(activity)
                                .setMessage(getString(R.string.training_empty_list))
                                .setPositiveButton(
                                    android.R.string.ok,
                                    DialogInterface.OnClickListener { dialog, which ->
                                        dialog.dismiss()
                                        navController.popBackStack()
                                    })
                                .create()
                                .show()
                        }
                    }
                }.apply { disposables.add(this) }
        }

    }

    private fun setupListPeopleView(listPeople: RecyclerView) {
        activity?.let {
            val adapter = TrainingAdapter(it, this, emptyList(), viewModel)
            listPeople.adapter = adapter
            listPeople.layoutManager = GridLayoutManager(it, 3)
        }

    }

    private fun setupList() {
        trainingList = ObservableArrayList()
        binding.trainingList = trainingList
    }

    private fun setupObserver(observable: java.util.Observable) {
        observable.addObserver(this)
    }

    override fun onDestroy() {
        viewModel.reset()
        if (!disposables.isDisposed()) {
            disposables.dispose()
        }
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
    }

    fun onClick(view: View) {
        when(view) {
            btn_start -> {
                viewModel.loadData()
            }
        }
    }

    @Subscribe
    public fun onActivityResult(activityResultEvent: ActivityResultEvent){
        onActivityResult(activityResultEvent.requestCode, activityResultEvent.resultCode, activityResultEvent.intent)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        L.d("REQ_CODE_NFC_CHECK@@@@@ 0")
        when (requestCode) {
            C.REQ_CODE_NFC_CHECK -> if (resultCode == RESULT_OK) {
                L.d("REQ_CODE_NFC_CHECK@@@@@")
                data?.let {
                    L.d("REQ_CODE_NFC_CHECK@@@@@ 2")
                    val simulId = it.getStringExtra("simulId")
                    val placeId = it.getStringExtra("placeId")
                    val placeDetailId = it.getStringExtra("placeDetailId")

                    L.d("REQ_CODE_NFC_CHECK@@@@@ 3")
//                    viewModel.successNFC(placeId, placeDetailId, simulId)


                    selectCamera(placeId, placeDetailId, simulId)
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                L.d("REQ_CODE_NFC_CHECK@@@@@ 5")
            }
            C.REQ_CODE_CAEMRA -> if (resultCode == Activity.RESULT_OK) {
                L.d("REQ_CODE_NFC_CHECK@@@@@ 4")
                viewModel.successNFC()
            } else if (resultCode == Activity.RESULT_CANCELED) {
                L.d("REQ_CODE_NFC_CHECK@@@@@ 3")
            }
            else -> {
                super.onActivityResult(requestCode, resultCode, data)
                L.d("REQ_CODE_NFC_CHECK@@@@@ 2")
            }
        }

    }

    fun selectCamera(placeId: String, placeDetailId: String, simulId: String) {
        L.d("REQ_CODE_NFC_CHECK@@@@@ 4")
        val permissionlistener: PermissionListener = object : PermissionListener {
            override fun onPermissionGranted() {

                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                val manager = activity?.packageManager
                manager.let {
                    if (intent.resolveActivity(it) != null) {
                        try {
                            context?.let {
                                var photoFile = ImageUtils.createImageFile(it)
                                viewModel.cameraFilePath = Uri.fromFile(photoFile)
                                viewModel.placeId = placeId
                                viewModel.placeDetailId = placeDetailId
                                viewModel.simulId = simulId

                                if (null != photoFile) {
//                                    viewModel.cameraFilePath = photoFile.absolutePath
                                    val uri = FileProvider.getUriForFile(it,it.applicationContext.packageName.toString()+".provider",photoFile)


                                    Log.d(TAG,"### provider : " + it.applicationContext.packageName.toString()+".provider")
                                    Log.d(TAG,"### uri : " + uri.toString())
                                    Log.d(TAG,"### filepath : " + photoFile.absolutePath)
//                                    intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
//                                    startActivityForResult(intent, C.REQ_CODE_CAEMRA)

                                    val resolvedIntentActivities =
                                        context!!.packageManager.queryIntentActivities(
                                            intent,
                                            PackageManager.MATCH_DEFAULT_ONLY
                                        )
                                    for (resolvedIntentInfo in resolvedIntentActivities) {
                                        val packageName =
                                            resolvedIntentInfo.activityInfo.packageName
                                        context!!.grantUriPermission(
                                            packageName,
                                            uri,
                                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        )
                                    }
                                    intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                                    startActivityForResult(intent, C.REQ_CODE_CAEMRA)
                                }
                            }

                        } catch (e: Exception) {

                        }
                    }
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

        checkCameraPermission(permissionlistener)
    }

    private fun checkCameraPermission(permissionlistener: PermissionListener) {
        TedPermission.with(context)
            .setPermissionListener(permissionlistener)
            .setDeniedMessage(R.string.report_permission_denied_alert)
            .setPermissions(android.Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .check()
    }

    fun onBackPressed() {
        navController.popBackStack()
    }
}