package com.seoul.culture.scene.main

import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageInfo
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.seoul.culture.R
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.location.*
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import com.seoul.culture.application.ScpApplication
import com.seoul.culture.config.C
import com.seoul.culture.databinding.ActivityMainBinding
import com.seoul.culture.scene.login.LoginActivity
import com.seoul.culture.utils.ActivityResultEvent
import com.seoul.culture.utils.BusProvider
import com.seoul.culture.utils.FontSansType
import com.seoul.culture.utils.setFont
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import io.reactivex.functions.Predicate
import io.reactivex.schedulers.Timed
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.navi_main.*
import java.util.ArrayList
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {
    val TAG = MainActivity::class.java.simpleName

    private val EXIT_TIMEOUT: Long = 2000
    private val backButtonClickSource = PublishSubject.create<Boolean>()

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController
    private lateinit var viewModel: MainViewModel
    private lateinit var binding: ActivityMainBinding

    private var fusedLocationClient: FusedLocationProviderClient? = null

    private lateinit var locationRequest: LocationRequest

    private lateinit var locationCallback: LocationCallback

    val permissions = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE
    )
    private val disposables by lazy {
        CompositeDisposable()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initDataBinding()
        initDrawer()
        gpsForeground()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!disposables.isDisposed()) {
            disposables.dispose()
        }
    }

    override fun onResume() {
        super.onResume()

        startLocationUpdates()
        viewModel.chkLogout()
    }

    override fun onStop() {
        super.onStop()
        stopLocationUpdates()

    }

    private fun initDataBinding() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        viewModel = MainViewModel(this)

        viewModel.userName
            .distinctUntilChanged()
            .subscribe({
                this.tv_greet.text = getString(R.string.drawer_greet, it)
            }).apply { disposables.add(this) }

        viewModel.sbjChkLogout
            .distinctUntilChanged()
            .subscribe({
                if (it) {
                    AlertDialog.Builder(this)
                        .setMessage(getString(R.string.logout_auto))
                        .setPositiveButton(
                            android.R.string.ok,
                            DialogInterface.OnClickListener { dialog, which ->
                                // 다 초기화하기.
                                this.moveLogin()
                                dialog.dismiss()
                            })
                        .create()
                        .show()


                }
            }).apply { disposables.add(this) }

        binding.view = this
        binding.model = viewModel

        navController = Navigation.findNavController(this, R.id.nav_fragment)
        appBarConfiguration = AppBarConfiguration(navController.graph, drawer_layout)

        // Set up ActionBar
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            this.setDisplayShowTitleEnabled(false)
        }

        setupActionBarWithNavController(navController, appBarConfiguration)


        // Set up navigation menu
        navigation_view.setupWithNavController(navController)


        backButtonClickSource
            .debounce(100, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext {
                Toast.makeText(
                    this,
                    R.string.app_exit_title,
                    Toast.LENGTH_SHORT
                ).show()
            }
            .timeInterval(TimeUnit.MILLISECONDS)
            .skip(1)
            .filter(Predicate {
                    interval: Timed<Boolean?> -> interval.time() < EXIT_TIMEOUT
            })
            .subscribe(Consumer<Timed<Boolean?>> {
                this.finish()
            }).apply { disposables.add(this) }

        
        navController.addOnNavigatedListener { controller, destination ->
            when(destination.id) {
                R.id.mainFragment -> {
                    toolbar_title.text = getString(R.string.main_bar_title)
                }
                R.id.patrolFragment -> {
                    toolbar_title.text = getString(R.string.patrol_bar_title)
                }
                R.id.reportFragment -> {
                    toolbar_title.text = getString(R.string.report_bar_title)
                }
                R.id.manageAreaFragment -> {
                    toolbar_title.text = getString(R.string.manage_bar_title)
                }
                R.id.trainingFragment -> {
                    toolbar_title.text = getString(R.string.training_bar_title)
                }
            }
        }

    }

    private fun initDrawer() {
        viewModel.setUser()
        setCurrentNavi(R.id.linear_drawer_patrol)
        tv_app_version.text = getVersionInfo()
        tv_app_version.setFont(this ,FontSansType.DEMI_LIGHT)
        tv_app_version_title.setFont(this ,FontSansType.DEMI_LIGHT)

        tv_uuid.text = ScpApplication.prefs.uuid
        tv_uuid.setFont(this ,FontSansType.DEMI_LIGHT)
        tv_uuid_title.setFont(this ,FontSansType.DEMI_LIGHT)
    }

    private fun setCurrentNavi(id: Int) {
        when (id) {
            R.id.linear_drawer_patrol -> {
                v_highlight_1.isVisible = true
                iv_highlight.isVisible = true
                v_highlight_2.isVisible = false
                iv_regi_highlight.isVisible = false
                v_highlight_3.isVisible = false
                iv_regi_highlight_input.isVisible = false
                iv_update.isVisible = false
                v_highlight_4.isVisible = false
                iv_regi_highlight_input.isVisible = false
                tv_patrol.setFont(this ,FontSansType.NORMAL)
                tv_regi_patrol.setFont(this ,FontSansType.DEMI_LIGHT)
                tv_regi_patrol_input.setFont(this ,FontSansType.DEMI_LIGHT)
                tv_update.setFont(this ,FontSansType.DEMI_LIGHT)
            }
            R.id.linear_drawer_manage -> {
                v_highlight_1.isVisible = false
                iv_highlight.isVisible = false
                v_highlight_2.isVisible = true
                iv_regi_highlight.isVisible = true
                v_highlight_3.isVisible = false
                iv_regi_highlight_input.isVisible = false
                iv_update.isVisible = false
                v_highlight_4.isVisible = false
                iv_regi_highlight_input.isVisible = false
                tv_patrol.setFont(this ,FontSansType.DEMI_LIGHT)
                tv_regi_patrol.setFont(this ,FontSansType.NORMAL)
                tv_regi_patrol_input.setFont(this ,FontSansType.DEMI_LIGHT)
                tv_update.setFont(this ,FontSansType.DEMI_LIGHT)
            }

            R.id.linear_drawer_manage_input -> {
                v_highlight_1.isVisible = false
                iv_highlight.isVisible = false
                v_highlight_2.isVisible = false
                iv_regi_highlight.isVisible = false
                v_highlight_3.isVisible = true
                iv_regi_highlight_input.isVisible = true
                iv_update.isVisible = false
                v_highlight_4.isVisible = false
                iv_regi_highlight_input.isVisible = true
                tv_patrol.setFont(this ,FontSansType.DEMI_LIGHT)
                tv_regi_patrol.setFont(this ,FontSansType.DEMI_LIGHT)
                tv_regi_patrol_input.setFont(this ,FontSansType.NORMAL)
                tv_update.setFont(this ,FontSansType.DEMI_LIGHT)
            }

            R.id.linear_drawer_update -> {
                v_highlight_1.isVisible = false
                iv_highlight.isVisible = false
                v_highlight_2.isVisible = false
                iv_regi_highlight.isVisible = false
                v_highlight_3.isVisible = false
                iv_regi_highlight_input.isVisible = false
                iv_update.isVisible = true
                v_highlight_4.isVisible = true
                iv_regi_highlight_input.isVisible = true
                tv_patrol.setFont(this ,FontSansType.DEMI_LIGHT)
                tv_regi_patrol.setFont(this ,FontSansType.DEMI_LIGHT)
                tv_regi_patrol_input.setFont(this ,FontSansType.DEMI_LIGHT)
                tv_update.setFont(this ,FontSansType.NORMAL)
            }
        }

    }


    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
            return
        } else {
            if (Navigation.findNavController(this,R.id.nav_fragment)
                    .currentDestination?.id == navController.graph.startDestination) {
                backButtonClickSource.onNext(true)
                return
            }
        }
        super.onBackPressed()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        BusProvider.instance.post(ActivityResultEvent(requestCode, resultCode, data))
    }

    fun moveLogin(view: View) {
        moveLogin()
    }

    fun moveLogin() {
        if (viewModel.chkEnableLogout()) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            this.finish()
        } else {
            if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
                drawer_layout.closeDrawer(GravityCompat.START)
            }
            Toast.makeText(
                this,
                R.string.drawer_logout_impossible,
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    fun movePatrol(view: View) {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        }

        navController.navigate(R.id.action_mainFragment_to_patrolFragment)
        setCurrentNavi(view.id)
    }

    fun moveManage(view: View) {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        }
        navController.navigate(R.id.action_mainFragment_to_manageAreaFragment)
        setCurrentNavi(view.id)
    }

    fun moveManageInput(view: View) {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        }
        navController.navigate(R.id.action_mainFragment_to_manageInputFragment)
        setCurrentNavi(view.id)
    }

    fun moveUpdate(view: View) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(C.APK_URL))
        intent.setPackage("com.android.chrome")
        startActivity(intent)
    }

    fun gpsForeground() {
        val permissionlistener: PermissionListener = object : PermissionListener {
            override fun onPermissionGranted() {
                initGps()
            }

            override fun onPermissionDenied(deniedPermissions: ArrayList<String>) {
                Toast.makeText(
                    baseContext,
                    R.string.report_permission_denied,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        checkPermission(permissionlistener)
    }

    private fun checkPermission(permissionlistener: PermissionListener) {
        TedPermission.with(this)
            .setPermissionListener(permissionlistener)
            .setDeniedMessage(R.string.report_permission_denied_alert)
            .setPermissions(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)
            .check()
    }


    fun initGps() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest()
        locationRequest.interval = 100000
        locationRequest.fastestInterval = 50000
//        locationRequest.smallestDisplacement = 170f // 170 m = 0.1 mile
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY //set according to your app function

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return

                if (locationResult.locations.isNotEmpty()) {
                    // get latest location
                    val location =
                        locationResult.lastLocation
                    ScpApplication.INSTANCE.gpsLon = location.longitude.toString()
                    ScpApplication.INSTANCE.gpsLat = location.latitude.toString()

                    stopLocationUpdates()
                    // use your location object
                    // get latitude , longitude and other info from this
                }
            }
        }

        startLocationUpdates()
    }


    //start location updates
    fun startLocationUpdates() {
        fusedLocationClient?.run {
            this.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null
            )
        }
    }

    // stop location updates
    fun stopLocationUpdates() {
        fusedLocationClient?.run {
            this.removeLocationUpdates(locationCallback)
        }
    }

    fun getVersionInfo(): String {
        val info: PackageInfo = baseContext.packageManager.getPackageInfo(baseContext.packageName, 0)
        return info.versionName
    }
}