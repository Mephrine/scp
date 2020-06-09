package com.seoul.culture.scene.patrol

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Context.LOCATION_SERVICE
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.databinding.ObservableArrayList
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.google.android.gms.location.*
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import com.seoul.culture.R
import com.seoul.culture.application.ScpApplication
import com.seoul.culture.config.C
import com.seoul.culture.databinding.FragmentPatrolBinding
import com.seoul.culture.model.PatrolData
import com.seoul.culture.model.PatrolNfcData
import com.seoul.culture.utils.*
import com.squareup.otto.Subscribe
import gun0912.tedbottompicker.TedRxBottomPicker
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.fragment_patrol.*
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.jar.Manifest


class PatrolFragment : Fragment(), Observer {
    override fun update(observable: java.util.Observable?, arg: Any?) {
        if (observable is PatrolViewModel) {
            binding.listPatrol.background = resources.getDrawable(R.color.color_line_bg)
            val patrolAdapter = binding.listPatrol.adapter as PatrolAdapter
            patrolAdapter.setPatrolList(observable.getPatrolList())
        }
    }
    val TAG = PatrolFragment::class.java.simpleName

    private lateinit var binding: FragmentPatrolBinding
    private lateinit var viewModel: PatrolViewModel
    private lateinit var patrolList: ObservableArrayList<PatrolNfcData>
    private lateinit var loading: LoadingDialog

    private lateinit var navController: NavController

    private var fusedLocationClient: FusedLocationProviderClient? = null

    private lateinit var locationRequest: LocationRequest

    private lateinit var locationCallback: LocationCallback

    private lateinit var callback: OnBackPressedCallback

    private val disposables by lazy {
        CompositeDisposable()
    }

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
        binding = FragmentPatrolBinding.inflate(inflater, container, false)
        initDataBinding()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        BusProvider.instance.register(this)
        list_patrol.addItemDecoration(
            DividerItemDecoration(4,3)
        )

        if (ScpApplication.prefs.startPatrol) {
            layout_start.visibility = View.GONE
            gpsForeground()
        } else {
            layout_start.visibility = View.VISIBLE
        }

        navController = Navigation.findNavController(view)

        Observable.create<View> { btn_report.setOnClickListener(it::onNext) }
            .debounce(300, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                navController.navigate(R.id.action_patrolFragment_to_reportFragment)
            }.apply { disposables.add(this) }

        Observable.create<View> { btn_complete.setOnClickListener(it::onNext) }
            .debounce(300, TimeUnit.MILLISECONDS)
            .map { viewModel.checkPatrolComplete() }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it) {
                    viewModel.sendPatrolComplete()
                } else {
                    AlertDialog.Builder(activity)
                        .setMessage(getString(R.string.patrol_omission_complete_alert))
                        .setPositiveButton(
                            android.R.string.ok,
                            DialogInterface.OnClickListener { dialog, which ->
                                viewModel.sendPatrolComplete()
                                dialog.dismiss()
                            })
                        .setNegativeButton(android.R.string.cancel,
                            DialogInterface.OnClickListener { dialog, which ->
                                dialog.dismiss()
                            })
                        .create()
                        .show()
                }
            }.apply { disposables.add(this) }

        context?.let {
            loading = LoadingDialog(it)
//            fusedLocationClient = LocationServices.getFusedLocationProviderClient(it)
//            getLocationUpdates()
        }

    }

    override fun onDestroyView() {
        BusProvider.instance.unregister(this)
        super.onDestroyView()
    }

    private fun initDataBinding() {

        context?.let {
            viewModel = PatrolViewModel(it)
            binding.viewModel = viewModel
            binding.view = this
            setupListPeopleView(binding.listPatrol)
            setupList()
            setupObserver(viewModel)
            viewModel.initializeViews()

            viewModel.patrolProgress
                .subscribeOn(AndroidSchedulers.mainThread())
                .share()
                .subscribe{
                loading.show(it)
            }.apply { disposables.add(this) }

            viewModel.completePatrol
                .filter { it }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    activity?.let {
                        Toast.makeText(it, getString(R.string.patrol_complete_success), Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }
                }.apply { disposables.add(this) }
        }

    }

    private fun setupListPeopleView(listPeople: RecyclerView) {
        activity?.let {
            val adapter = PatrolAdapter(it, emptyList())
            listPeople.adapter = adapter
            listPeople.layoutManager = GridLayoutManager(it, 3)
        }

    }

    private fun setupList() {
        patrolList = ObservableArrayList()
        binding.patrolList = patrolList
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

    fun onClick(view: View) {
        when(view) {
            btn_start -> {
                gpsForeground()
            }
        }
    }

//    fun chkPermission(): Boolean {
//        context?.let {
//            var permissionAccessCoarseLocationApproved = ActivityCompat
//                .checkSelfPermission(it, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
//                    PackageManager.PERMISSION_GRANTED
//
//            if (permissionAccessCoarseLocationApproved) {
//                val backgroundLocationPermissionApproved = ActivityCompat
//                    .checkSelfPermission(this, android.Manifest.permission.ACCESS_) ==
//                        PackageManager.PERMISSION_GRANTED
//
//                if (backgroundLocationPermissionApproved) {
//                    // App can access location both in the foreground and in the background.
//                    // Start your service that doesn't have a foreground service type
//                    // defined.
//                } else {
//                    // App can only access location in the foreground. Display a dialog
//                    // warning the user that your app must have all-the-time access to
//                    // location in order to function properly. Then, request background
//                    // location.
//                    ActivityCompat.requestPermissions(this,
//                        arrayOf(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION),
//                        your-permission-request-code
//                    )
//                }
//            } else {
//                // App doesn't have access to the device's location at all. Make full request
//                // for permission.
//                ActivityCompat.requestPermissions(this,
//                    arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION,
//                        android.Manifest.permission.ACCESS_BACKGROUND_LOCATION),
//                    your-permission-request-code
//                )
//            }
//        }
//    }

    fun gpsForeground() {
        val permissionlistener: PermissionListener = object : PermissionListener {
            override fun onPermissionGranted() {
                context?.let {
                    viewModel.loadData()
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


//    private fun getLocationUpdates() {
//        context?.let {
//            val periodicWork = PeriodicWorkRequest.Builder(GPSWorker::class.java, 15, TimeUnit.MINUTES)
//                .addTag(C.TAG_LOCATION)
//                .build();
//
//            WorkManager.getInstance(it).enqueueUniquePeriodicWork(C.TAG_LOCATION, ExistingPeriodicWorkPolicy.REPLACE, periodicWork);
//        }

//    }

    //start location updates
//    private fun startLocationUpdates() {
//        context?.let {
//            val periodicWork = PeriodicWorkRequest.Builder(GPSWorker::class.java, 15, TimeUnit.MINUTES)
//                .addTag(TAG)
//                .build();
//
//            WorkManager.getInstance(it).enqueueUniquePeriodicWork("Location", ExistingPeriodicWorkPolicy.REPLACE, periodicWork);
//        }
//    }

    // stop location updates
//    private fun stopLocationUpdates() {
////        fusedLocationClient.removeLocationUpdates(locationCallback)
//        context?.let {
//            WorkManager.getInstance(it).cancelAllWorkByTag(C.TAG_LOCATION)
//            ScpApplication.prefs.gpsInfoList = emptyList()
//        }
//    }


    @Subscribe
    public fun onActivityResult(activityResultEvent: ActivityResultEvent){
        onActivityResult(activityResultEvent.requestCode, activityResultEvent.resultCode, activityResultEvent.intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            C.REQ_CODE_NFC_CHECK -> if (resultCode == RESULT_OK) {
                data?.let {
                    val placeId = it.getStringExtra("placeId")
                    val placeDetailId = it.getStringExtra("placeDetailId")

                    viewModel.successNFC(placeId, placeDetailId)
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {

            }

            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    fun onBackPressed() {
        if (viewModel.completeCntMoreOne()) {
            AlertDialog.Builder(activity)
                .setMessage(R.string.patrol_cancel_alert)
                .setPositiveButton(
                    android.R.string.ok,
                    DialogInterface.OnClickListener { dialog, which ->
                        ScpApplication.prefs.startPatrol = false
                        ScpApplication.prefs.patrolFailList = emptyList()
                        ScpApplication.prefs.patrolList = emptyList()

                        navController.popBackStack()
                        dialog.dismiss()
                    })
                .setNegativeButton(android.R.string.cancel,
                    DialogInterface.OnClickListener { dialog, which ->
                        navController.popBackStack()
                        dialog.dismiss()
                    })
                .create()
                .show()
        }
    }
}