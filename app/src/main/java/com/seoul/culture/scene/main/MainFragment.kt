package com.seoul.culture.scene.main

import android.content.DialogInterface
import android.content.pm.PackageInfo
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import com.seoul.culture.R
import com.seoul.culture.application.ScpApplication
import com.seoul.culture.utils.LoadingDialog
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.fragment_main.*
import kr.smart.carefarm.base.BaseFragment
import java.util.ArrayList

class MainFragment : BaseFragment() {
    private lateinit var viewModel: MainFragmentViewModel
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navController = Navigation.findNavController(view)
        layout_patrol.setOnClickListener { navController.navigate(R.id.action_mainFragment_to_patrolFragment) }
        layout_report.setOnClickListener { navController.navigate(R.id.action_mainFragment_to_reportFragment) }
        layout_emergency_report.setOnClickListener { emergencyReport(it) }
        layout_manage.setOnClickListener { navController.navigate(R.id.action_mainFragment_to_trainingFragment) }

        context?.let {
            viewModel = MainFragmentViewModel(it)

            viewModel.progress
                .subscribeOn(AndroidSchedulers.mainThread())
                .share()
                .subscribe{
                    loading.show(it)
                }.apply { disposables.add(this) }
        }

    }

    fun emergencyReport(view: View) {
        gpsForeground()
    }

    fun gpsForeground() {
        val permissionlistener: PermissionListener = object : PermissionListener {
            override fun onPermissionGranted() {
                context?.let {
                    val items = resources.getStringArray(R.array.report_emergency)
                    val dialog: AlertDialog.Builder = AlertDialog.Builder(it)
                    dialog.setTitle(R.string.report_picker_siren)
                    dialog.setItems(items, DialogInterface.OnClickListener { dialog, position ->
                        viewModel.fetchEmergencyReport(position)
                    })
                    dialog.setPositiveButton(R.string.picker_cancel, DialogInterface.OnClickListener { dialog, which ->
                        dialog.dismiss()
                    })
                    val alert: AlertDialog = dialog.create()
                    alert.show()
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


}