package com.seoul.culture.scene.report

import android.Manifest
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import com.seoul.culture.R
import com.seoul.culture.config.C
import com.seoul.culture.databinding.FragmentReportBinding
import com.seoul.culture.utils.*
import com.squareup.otto.Subscribe
import gun0912.tedbottompicker.TedRxBottomPicker
import gun0912.tedbottompicker.util.RealPathUtil
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_report.*
import okhttp3.RequestBody
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

const val MIME_TEXT_PLAIN = "text/plain"

class ReportFragment: Fragment() {
    val TAG = ReportFragment::class.java.simpleName

    private lateinit var binding: FragmentReportBinding
    private lateinit var viewModel: ReportViewModel
    private lateinit var requestManager: RequestManager
    private lateinit var keyboardVisibilityUtils: KeyboardVisibilityUtils

    private lateinit var navController: NavController

    private lateinit var loading: LoadingDialog
    private val disposables by lazy {
        CompositeDisposable()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_report, container, false
        );
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()
        initDataBinding()
        navController = Navigation.findNavController(view)
        requestManager = Glide.with(this);

    }

    private fun initView() {
        et_report.addTextChangedListener(object : TextWatcher {
            var beforeStr = ""
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                beforeStr = s.toString().trim { it <= ' ' }
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(editable: Editable) {
                try {
                    val changeStr = editable.toString().trim { it <= ' ' }
                    if (changeStr.length > 1000) {
                        val subStr = changeStr.substring(0, 1000)
                        et_report.setText(subStr)
                        et_report.setSelection(et_report.text.length)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, e.toString())
                }
            }
        })

        keyboardVisibilityUtils = KeyboardVisibilityUtils(window = activity?.window!!,
            onShowKeyboard = { keyboardHeight ->
                nestedScrollView.run {
                    smoothScrollTo(scrollX, scrollY + keyboardHeight)
                }
            })

        context?.let {
            loading = LoadingDialog(it)
        }
    }

    private fun getTextLength(inStr: String): Int { //        String Str = inStr.toString().trim().replace("\n", "");
        val Str = inStr.trim { it <= ' ' }
        return Str.length
    }

    private fun initDataBinding() {
        context?.let { viewModel = ReportViewModel(it) }

        viewModel.selectedUri
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (it != null && it != Uri.EMPTY) {
                    requestManager
                        .load(it)
                        .into(showImage(true))

                } else {
                    showImage(false).setImageResource(R.drawable.img_pic_4)
                }
            }) {
                showImage(false).setImageResource(R.drawable.img_pic_4)
            }
            .apply { disposables.add(this) }

        viewModel.completePatrol
            .filter { it }
            .subscribe { success ->
                activity?.let {
                    if (success) {
                        Toast.makeText(it, getString(R.string.report_complete_success), Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    } else {
                        Toast.makeText(it, getString(R.string.report_complete_fail), Toast.LENGTH_SHORT).show()
                    }
                }
            }.apply { disposables.add(this) }

        Observable.create<View> { btn_send.setOnClickListener(it::onNext) }
            .debounce(300, TimeUnit.MILLISECONDS)
            .map { viewModel.validation(et_report.text.toString()) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it) {
                    gpsForeground()
                } else {
                    Toast.makeText(
                        activity,
                        R.string.report_empty_text,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }.apply { disposables.add(this) }

        viewModel.patrolProgress
            .observeOn(AndroidSchedulers.mainThread())
            .share()
            .subscribe{
                loading.show(it)
            }.apply { disposables.add(this) }

        binding.view = this
        binding.viewModel = viewModel
    }

    fun showImage(show: Boolean): ImageView {
        return when (show) {
            true -> {
                iv_upload.visibility = View.VISIBLE
                iv_report.visibility = View.GONE
                tv_upload_hint.visibility = View.GONE
                return iv_upload
            }
            false -> {
                iv_upload.visibility = View.GONE
                iv_report.visibility = View.VISIBLE
                tv_upload_hint.visibility = View.VISIBLE
                return iv_report
            }
        }

    }

    fun showAlertDialog(view: View) {
        if (context != null) {
            val items = resources.getStringArray(R.array.report_picker)
            val dialog: AlertDialog.Builder = AlertDialog.Builder(context!!)
            dialog.setTitle(R.string.report_picker_title)
            dialog.setItems(items, DialogInterface.OnClickListener { dialog, position ->
                when (position) {
                    0 -> {
                        selectCamera()
                    }
                    1 -> {
                        selectImageInAlbum()
                    }
//                    1 -> {
//                        selectVideoInAlbum()
//                    }
                    2 -> {
                        selectRemoveImage()
                    }
                }
            })
            dialog.setPositiveButton(R.string.picker_cancel, DialogInterface.OnClickListener { dialog, which ->
                dialog.dismiss()
            })
            val alert: AlertDialog = dialog.create()
            alert.show()
        }
    }

    fun onClick(view: View) {
        when(view.id){
            R.id.review_full_layout -> {
                et_report.hideKeyboard()
                et_report.clearFocus()
            }
        }
    }

    fun hideKeyboard(view: View) {
        activity?.hideKeyboard(view)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (!disposables.isDisposed()) {
            disposables.dispose()
        }
        keyboardVisibilityUtils.detachKeyboardListeners()
    }

    fun selectCamera() {
        val permissionlistener: PermissionListener = object : PermissionListener {
            override fun onPermissionGranted() {

                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                val manager = activity?.packageManager
                L.d("test!!!!!!")
                manager.let {
                    L.d("test!!!!!! 2")
                    if (intent.resolveActivity(it) != null) {
                        L.d("test!!!!!! 3")
                        try {
                            L.d("test!!!!!! 4")
                            context?.let {
                                L.d("test!!!!!! 5")
                                var photoFile = ImageUtils.createImageFile(it)
                                L.d("test!!!!!! 6")
                                viewModel.cameraFilePath = Uri.fromFile(photoFile)
                                L.d("test!!!!!! 7")

                                if (null != photoFile) {
                                    L.d("test!!!!!! 8")
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

    fun selectImageInAlbum() {
        val permissionlistener: PermissionListener = object : PermissionListener {
            override fun onPermissionGranted() {

                TedRxBottomPicker.with(activity) //.setPeekHeight(getResources().getDisplayMetrics().heightPixels/2)
                    .setTitle(R.string.report_pick_image)
                    .setSelectedUri(viewModel.selectedUri.value) //.showVideoMedia()
                    .setPeekHeight(1200)
                    .showCameraTile(true)
                    .show()
                    .subscribe(
                        { uri: Uri ->
                            viewModel.selectedUri.onNext(uri)
                            viewModel.selectedImageType()
//                                iv_image.visibility = View.VISIBLE
//                                mSelectedImagesContainer.setVisibility(View.GONE)
//                                requestManager
//                                    .load(uri)
//                                    .into(iv_image)
                        }
                    ) { obj: Throwable -> obj.printStackTrace() }
                    .apply { disposables.add(this) }


//                TedBottomPicker.with(activity) //.setPeekHeight(getResources().getDisplayMetrics().heightPixels/2)
//                    .setSelectedUri(viewModel.selectedUri.value) //.showVideoMedia()
//                    .setPeekHeight(1200)
//                    .show { uri: Uri ->
//                        Log.d(TAG, "uri: $uri")
//                        Log.d(TAG, "uri.getPath(): " + uri.path)
//                        viewModel.selectedUri.onNext(uri)
//                    }
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

    fun selectVideoInAlbum() {
        val permissionlistener: PermissionListener = object : PermissionListener {
            override fun onPermissionGranted() {
                TedRxBottomPicker.with(activity) //.setPeekHeight(getResources().getDisplayMetrics().heightPixels/2)
                    .setTitle(R.string.report_pick_video)
                    .setSelectedUri(viewModel.selectedUri.value) //.showVideoMedia()
                    .setPeekHeight(1200)
                    .showVideoMedia()
                    .show()
                    .subscribe({ uri: Uri ->
                        Log.d(TAG, "uri: $uri")
                        Log.d(TAG, "uri.getPath(): " + uri.path)
                        viewModel.selectedUri.onNext(uri)
                        viewModel.selectedVideoType()
                    }) { obj: Throwable -> obj.printStackTrace() }
                    .apply { disposables.add(this) }
            }

            override fun onPermissionDenied(deniedPermissions: ArrayList<String>) {
                Toast.makeText(
                    activity,
                    deniedPermissions.toString() + " " + R.string.report_permission_denied,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        checkPermission(permissionlistener)
    }

    fun selectRemoveImage() {
        viewModel.selectedUri.onNext(Uri.EMPTY)
        viewModel.selectedNoneType()
    }

    private fun checkPermission(permissionlistener: PermissionListener) {
        TedPermission.with(context)
            .setPermissionListener(permissionlistener)
            .setDeniedMessage(R.string.report_permission_denied_alert)
            .setPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .check()
    }

    private fun checkCameraPermission(permissionlistener: PermissionListener) {
        TedPermission.with(context)
            .setPermissionListener(permissionlistener)
            .setDeniedMessage(R.string.report_permission_denied_alert)
            .setPermissions(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .check()
    }

//    fun takePhoto() {
//        val rxPermissions = RxPermissions(this)
//        rxPermissions.request(Manifest.permission.CAMERA)
//            .subscribe {
//                val intent1 = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//                val manager = activity?.packageManager
//                manager.let {
//                    if (intent1.resolveActivity(it) != null) {
//                        startActivityForResult(intent1, 0)
//                    }
//                }
//            }
//    }


//    private fun prepareFilePart(partName: String, file: File): MultipartBody.Part {
//        val requestFile = ProgressRequestBody(file)
//        return MultipartBody.Part.createFormData(partName, file.name, requestFile)
//    }

    @Subscribe
    public fun onActivityResult(activityResultEvent: ActivityResultEvent){
        onActivityResult(activityResultEvent.requestCode, activityResultEvent.resultCode, activityResultEvent.intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            C.REQ_CODE_CAEMRA -> if (resultCode == Activity.RESULT_OK) {




//                val bitmap = ImageUtils.getThumbnail(context!!, viewModel.cameraFilePath, 200.0)
//                Log.d(TAG,"bitmap1 : "+bitmap)

                val bitmap2 = MediaStore.Images.Media.getBitmap(context!!.contentResolver, viewModel.cameraFilePath);
                Log.d(TAG,"bitmap2 : "+bitmap2)


//                val options = BitmapFactory.Options();
//                options.inJustDecodeBounds = true;
//                try {
//                    var inputStream = FileInputStream(viewModel.cameraFilePath.path);
//                    BitmapFactory.decodeStream(inputStream, null, options);
//                    inputStream.close()
//                } catch (e: Exception) {
//                    e.printStackTrace();
//                }


                viewModel.selectedUri.onNext(viewModel.cameraFilePath)
                viewModel.selectedCameraType()

//                Log.d(TAG,"result!! 1 : "+data.toString())
//                data?.let {
//                    Log.d(TAG,"result!! 2")
//                    if (null != data.extras) {
//                        Log.d(TAG,"result!! 3")
//                        val extras = data.extras.get("data") as? Bitmap
//                        Log.d(TAG,"result!! 4")
//                        extras?.let { bitmap ->
//                            Log.d(TAG,"result!! 5")
//
//                        }
//                    }
//                }
            } else if (resultCode == Activity.RESULT_CANCELED) {

            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }

    }

    private fun onActivityResultGallery(data: Intent): Uri {
        val temp = data.data
        if (temp == null) {
            Log.d(TAG,"error gallery")
        }
        val realPath = RealPathUtil.getRealPath(activity, temp)
        val selectedImageUri: Uri?
        selectedImageUri = try {
            Uri.fromFile(File(realPath))
        } catch (ex: java.lang.Exception) {
            Uri.parse(realPath)
        }
        return selectedImageUri
    }


    private fun createPart(descriptionString: String): RequestBody {
        return RequestBody.create(okhttp3.MultipartBody.FORM, descriptionString)
    }

    fun gpsForeground() {
        val permissionlistener: PermissionListener = object : PermissionListener {
            override fun onPermissionGranted() {
                context?.let {
                    viewModel.sendReport(et_report.text.toString())
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

        checkGpsPermission(permissionlistener)
    }

    private fun checkGpsPermission(permissionlistener: PermissionListener) {
        TedPermission.with(context)
            .setPermissionListener(permissionlistener)
            .setDeniedMessage(R.string.report_permission_denied_alert)
            .setPermissions(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)
            .check()
    }


}