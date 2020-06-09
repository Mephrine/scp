package com.seoul.culture.scene.report

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.seoul.culture.R
import com.seoul.culture.api.APIClient
import com.seoul.culture.application.ScpApplication
import com.seoul.culture.utils.ImageUtils
import com.seoul.culture.utils.toRequestBody
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File


class ReportViewModel constructor(private val context: Context): ViewModel() {
    enum class FileType {
        NONE, CAMERA, PHOTO, VIDEO
    }

    private val disposables by lazy {
        CompositeDisposable()
    }
    var patrolProgress = BehaviorSubject.createDefault(false)

    var completePatrol = PublishSubject.create<Boolean>()
    var selectedUri = BehaviorSubject.create<Uri?>()

    lateinit var cameraFilePath: Uri

    var fileType: FileType = FileType.NONE

    override fun onCleared() {
        super.onCleared()

        if(!disposables.isDisposed) {
            disposables.clear()
        }

    }

    fun validation(text: String): Boolean {
        if (text.trim().isEmpty()) {
            return false
        }

        return true
    }

    fun selectedImageType() {
        fileType = FileType.PHOTO
    }

    fun selectedVideoType() {
        fileType = FileType.PHOTO
    }

    fun selectedNoneType() {
        fileType = FileType.NONE
    }

    fun selectedCameraType() {
        fileType = FileType.CAMERA
    }

    fun sendReport(text: String) {
//        val maps = hashMapOf<String, RequestBody>("userId" to toRequestBody("id")!!)
//        maps.put("text", toRequestBody(text).let { it!! })
        patrolProgress.onNext(true)

        val userIdStr = ScpApplication.prefs.userId
        val userId = toRequestBody(userIdStr)
        val reportText = toRequestBody(text)

        val gpsLon = toRequestBody(ScpApplication.INSTANCE.gpsLon)
        val gpsLat = toRequestBody(ScpApplication.INSTANCE.gpsLat)
        val placeId = toRequestBody(ScpApplication.INSTANCE.placeId)

        var imageUpload: MultipartBody.Part? = null
        selectedUri.value?.path?.let {
//            val file = File(it)
            try {
                val file = ImageUtils.bitmapToFile(context, it)

                when (fileType) {
                    FileType.PHOTO, FileType.CAMERA -> {
                        //multipart/form-data
                        val imageBody = RequestBody.create(MediaType.parse("image/jpg"), file)
                        imageUpload = MultipartBody.Part.createFormData("imgUpload", file.name, imageBody)
//                    maps.put("imgUpload", toRequestBody(files).let { it!! })
                    }
//                FileType.VIDEO -> {
//                    val videoBody = RequestBody.create(MediaType.parse("multipart/form-data"), file)
//                    videoUpload = MultipartBody.Part.createFormData("video", file.name, videoBody)
////                    maps.put("videoUpload", toRequestBody(files).let { it!! })
//                }
                    else -> {
                    }
                }
            } catch (e: Exception) {

            }
        }

        if (gpsLat != null && gpsLon != null && userId != null && reportText != null && placeId != null) {
            APIClient().getReportApi().sendReport(imageUpload, userId!!, gpsLat!!, gpsLon!!, reportText!!, placeId!!)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { res ->
                        patrolProgress.onNext(false)
                        //서버 응답후 처리.
                        if (res.resultCode == "200") {
                            completePatrol.onNext(true)
                        } else {
                            completePatrol.onNext(false)
                        }
                    },
                    { e ->
                        patrolProgress.onNext(false)
                        Toast.makeText(context, context.getString(R.string.fail_network_restart), Toast.LENGTH_SHORT).show()
                    }
                )
        }


    }

}