package com.seoul.culture.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.location.*
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import com.seoul.culture.R
import com.seoul.culture.application.ScpApplication
import com.seoul.culture.model.GPSData
import java.text.ParseException
import java.util.ArrayList

class GPSWorker(@param:NonNull private val mContext: Context, @NonNull workerParams: WorkerParameters) :
    Worker(mContext, workerParams) {
    private val TAG = "GPSWorker"

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    private val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 10000

    /**
     * The fastest rate for active location updates. Updates will never be more frequent
     * than this value.
     */
    private val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2


    /**
     * The current location.
     */
//    private var mLocation: Location? = null

    /**
     * Provides access to the Fused Location Provider API.
     */
    private var mFusedLocationClient: FusedLocationProviderClient? = null
    /**
     * Callback for changes in location.
     */
    private var mLocationCallback: LocationCallback? = null
    internal var ignored: ParseException? = null

    @NonNull
    override fun doWork(): Result {
        Log.d(TAG, "doWork: Done")
        if ( (ContextCompat.checkSelfPermission( mContext, android.Manifest.permission.ACCESS_COARSE_LOCATION ) == PackageManager.PERMISSION_GRANTED)
            && (ContextCompat.checkSelfPermission( mContext, android.Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED) ) {
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext)
            mLocationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)
                }
            }

            val mLocationRequest = LocationRequest()
            mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS)
            mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS)
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)

            try {
                mFusedLocationClient?.let {
                    it.getLastLocation()
                        .addOnCompleteListener(object : OnCompleteListener<Location> {
                            override fun onComplete(@NonNull task: Task<Location>) {
                                if (task.isSuccessful() && task.getResult() != null) {
                                    val location: Location = task.getResult()

                                    Log.d(TAG, "Location : " + location.longitude.toString() + " | " + location.latitude.toString())
                                    val gpsInfo = GPSData(location.longitude.toString(),location.latitude.toString())

//                                    val gpsInfoList = ArrayList<GPSData>()
//                                    gpsInfoList.addAll(ScpApplication.prefs.gpsInfoList)
//                                    gpsInfoList.add(gpsInfo)
//                                    ScpApplication.prefs.gpsInfoList = gpsInfoList
//                                    Log.d(TAG, "Location current!! : " + ScpApplication.prefs.gpsInfoList.toString())

                                } else {
                                    Log.w(TAG, "Failed to get location.")
                                }
                            }
                        })
                }

            } catch (unlikely: SecurityException) {
                Log.e(TAG, "Lost location permission.$unlikely")
            }

            try {
                mFusedLocationClient!!.requestLocationUpdates(mLocationRequest, null)
            } catch (unlikely: SecurityException) {
                //Utils.setRequestingLocationUpdates(this, false);
                Log.e(TAG, "Lost location permission. Could not request updates. $unlikely")
            }

            return Result.success()
        }

        return Result.failure()
    }
}