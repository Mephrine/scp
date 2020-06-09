package com.seoul.culture.utils

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import java.io.*
import java.text.SimpleDateFormat
import java.util.*


object ImageUtils {
    @Throws(IOException::class)
    fun bitmapToFile(context: Context, filePath: String?): File {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(
            filePath,
            options
        ) // inJustDecodeBounds 설정을 해주지 않으면 이부분에서 큰 이미지가 들어올 경우 outofmemory Exception이 발생한다.
        var imageHeight = options.outHeight
        var imageWidth = options.outWidth
        val exifDegree = exifOrientationToDegrees(filePath)
        var samplesize = 1
        val resize = 400
        while (true) { //2번
            if (imageWidth / 2 < resize || imageHeight / 2 < resize) break
            imageWidth /= 2
            imageHeight /= 2
            samplesize *= 2
        }
        val bitmapResize =
            getResizeFileImage(filePath, samplesize, imageWidth, imageHeight)
        val bitmapRotate = rotate(bitmapResize, exifDegree.toFloat())
        //create a file to write bitmap data
        val nowTime = System.currentTimeMillis()
        val tempFile = File(context.cacheDir, "temp_$nowTime.jpg")
        tempFile.createNewFile()
        //Convert bitmap to byte array
        val bos = ByteArrayOutputStream()
        bitmapRotate.compress(Bitmap.CompressFormat.JPEG, 90 /*ignored for PNG*/, bos)
        val bitmapdata = bos.toByteArray()
        //write the bytes in file
        val fos = FileOutputStream(tempFile)
        fos.write(bitmapdata)
        fos.flush()
        fos.close()
        return tempFile
    }

    @Throws(IOException::class)
    fun bitmapToFile(
        context: Context,
        bit: Bitmap
    ): File { //create a file to write bitmap data
        val nowTime = System.currentTimeMillis()
        val tempFile = File(context.cacheDir, "temp_$nowTime.jpg")
        tempFile.createNewFile()
        //Convert bitmap to byte array
        val bos = ByteArrayOutputStream()
        bit.compress(Bitmap.CompressFormat.JPEG, 90 /*ignored for PNG*/, bos)
        val bitmapdata = bos.toByteArray()
        //write the bytes in file
        val fos = FileOutputStream(tempFile)
        fos.write(bitmapdata)
        fos.flush()
        fos.close()
        return tempFile
    }

    @Throws(IOException::class)
    fun imageToBitmap(context: Context, filePath: String?): Bitmap {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(
            filePath,
            options
        ) // inJustDecodeBounds 설정을 해주지 않으면 이부분에서 큰 이미지가 들어올 경우 outofmemory Exception이 발생한다.
        var imageHeight = options.outHeight
        var imageWidth = options.outWidth
        val exifDegree = exifOrientationToDegrees(filePath)
        var samplesize = 1
        val resize = 400
        while (true) { //2번
            if (imageWidth / 2 < resize || imageHeight / 2 < resize) break
            imageWidth /= 2
            imageHeight /= 2
            samplesize *= 2
        }
        val bitmapResize =
            getResizeFileImage(filePath, samplesize, imageWidth, imageHeight)
        val bitmapRotate = rotate(bitmapResize, exifDegree.toFloat())
        //create a file to write bitmap data
        val nowTime = System.currentTimeMillis()
        val tempFile = File(context.cacheDir, "temp_$nowTime.jpg")
        tempFile.createNewFile()
        //Convert bitmap to byte array
        val bos = ByteArrayOutputStream()
        bitmapRotate.compress(Bitmap.CompressFormat.JPEG, 90 /*ignored for PNG*/, bos)
        val bitmapdata = bos.toByteArray()
        //write the bytes in file
        val fos = FileOutputStream(tempFile)
        fos.write(bitmapdata)
        fos.flush()
        fos.close()
        return bitmapRotate
    }

    fun exifOrientationToDegrees(filePath: String?): Int {
        var exif: ExifInterface? = null
        try {
            exif = ExifInterface(filePath)
        } catch (e: IOException) {
            Log.e("Error","sendPicture:::IOException")
        } catch (e: Exception) {
            Log.e("Error","sendPicture:::Exception")
        }
        val exifOrientation = exif!!.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270
        }
        return 0
    }

    fun getResizeFileImage(file_route: String?, size: Int, width: Int, height: Int): Bitmap {
        val options = BitmapFactory.Options()
        options.inSampleSize = size
        options.inPreferredConfig = Bitmap.Config.RGB_565
        val src = BitmapFactory.decodeFile(file_route, options)
        return Bitmap.createScaledBitmap(src, width, height, true)
    }

    fun rotate(src: Bitmap, degree: Float): Bitmap { // Matrix 객체 생성
        val matrix = Matrix()
        // 회전 각도 셋팅
        matrix.postRotate(degree)
        // 이미지와 Matrix 를 셋팅해서 Bitmap 객체 생성
        return Bitmap.createBitmap(
            src, 0, 0, src.width,
            src.height, matrix, true
        )
    }

    fun getRealPathFromURI(
        context: Context,
        contentUri: Uri?
    ): String {
        var path = ""
        var column_index = 0
        try {
            val proj =
                arrayOf(MediaStore.Images.Media.DATA)
            val cursor =
                context.contentResolver.query(contentUri, proj, null, null, null)
            if (cursor.moveToFirst()) {
                column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            }
            path = cursor.getString(column_index)
            cursor.close()
        } catch (e: Exception) {
            Log.e("Error",e.toString())
        }
        return path
    }

    fun getTotalfileSize(filePathList: List<String?>): Int {
        var fSize: Long = 0
        try {
            for (i in filePathList.indices) {
                val size = ""
                val filePath = filePathList[i] ?: continue
                val mFile = File(filePath)
                if (mFile.exists() == true) {
                    val fileSize = mFile.length()
                    fSize += fileSize
                } else {
                }
            }
        } catch (e: Exception) {
            Log.e("Error",e.toString())
        }
        return fSize.toInt()
    }

    @Throws(IOException::class)
    fun createImageFile(context: Context): File? {
        L.d("test!!!!!! 11")
        val timeStamp: String = SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.getDefault()
        ).format(Date())
        L.d("test!!!!!! 22")
        val imageFileName = "IMG_" + timeStamp + "_"
        L.d("test!!!!!! 33")
        val storageDir: File = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        L.d("test!!!!!! 44")
        if (!storageDir.exists()) {
            L.d("test!!!!!! 55")
            storageDir.mkdirs()
        }
        L.d("test!!!!!! 66")

        val image = File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",  /* suffix */
            storageDir /* directory */
        )
        L.d("test!!!!!! 77")
//        imageFilePath = image.absolutePath
        return image
    }


    @Throws(FileNotFoundException::class, IOException::class)
    fun getThumbnail(context: Context, uri: Uri?, size: Double): Bitmap? {
        var input: InputStream = context.contentResolver.openInputStream(uri)
        val onlyBoundsOptions = BitmapFactory.Options()
        onlyBoundsOptions.inJustDecodeBounds = true
        onlyBoundsOptions.inDither = true //optional
        onlyBoundsOptions.inPreferredConfig = Bitmap.Config.ARGB_8888 //optional
        BitmapFactory.decodeStream(input, null, onlyBoundsOptions)
        input.close()
        if (onlyBoundsOptions.outWidth == -1 || onlyBoundsOptions.outHeight == -1) {
            return null
        }
        val originalSize =
            if (onlyBoundsOptions.outHeight > onlyBoundsOptions.outWidth) onlyBoundsOptions.outHeight else onlyBoundsOptions.outWidth
        val ratio =
            if (originalSize > size) originalSize / size else 1.0
        val bitmapOptions = BitmapFactory.Options()
        bitmapOptions.inSampleSize = getPowerOfTwoForSampleRatio(ratio)
        bitmapOptions.inDither = true //optional
        bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888 //
        input = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions)
        input.close()
        return bitmap
    }

    private fun getPowerOfTwoForSampleRatio(ratio: Double): Int {
        val k = Integer.highestOneBit(Math.floor(ratio).toInt())
        return if (k == 0) 1 else k
    }


    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @author paulburke
     */
    fun getPath(context: Context, uri: Uri): String? {
        val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) { // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":").toTypedArray()
                val type = split[0]
                if ("primary".equals(type, ignoreCase = true)) {
                    return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                }
                // TODO handle non-primary volumes
            } else if (isDownloadsDocument(uri)) {
                val id = DocumentsContract.getDocumentId(uri)
                val contentUri = ContentUris.withAppendedId(
                    Uri.parse("content://downloads/public_downloads"),
                    java.lang.Long.valueOf(id)
                )
                return getDataColumn(context, contentUri, null, null)
            } else if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":").toTypedArray()
                val type = split[0]
                var contentUri: Uri? = null
                if ("image" == type) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else if ("video" == type) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else if ("audio" == type) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
                val selection = "_id=?"
                val selectionArgs = arrayOf(
                    split[1]
                )
                return getDataColumn(context, contentUri, selection, selectionArgs)
            }
        } else if ("content".equals(uri.scheme, ignoreCase = true)) {
            return getDataColumn(context, uri, null, null)
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }
        return null
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    fun getDataColumn(
        context: Context, uri: Uri?, selection: String?,
        selectionArgs: Array<String>?
    ): String? {
        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(
            column
        )
        try {
            cursor = context.contentResolver.query(
                uri, projection, selection, selectionArgs,
                null
            )
            if (cursor != null && cursor.moveToFirst()) {
                val column_index: Int = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(column_index)
            }
        } finally {
            if (cursor != null) cursor.close()
        }
        return null
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    fun getImageId(context: Context, path: String): Long {
        val projection =
            arrayOf(MediaStore.Images.Media._ID)
        val where = MediaStore.Images.Media.DATA + " =  ?"
        val whereArgs = arrayOf(path)
        val c: Cursor? = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            where,
            whereArgs,
            null
        )
        if (c != null && c.moveToFirst()) {
            val id: Long = c.getLong(0)
            c.close()
            return id
        }
        return -1
    }

    fun getThumbnailPath(context: Context, id: Long): String? {
        val c: Cursor? = MediaStore.Images.Thumbnails.queryMiniThumbnail(
            context.contentResolver,
            id,
            MediaStore.Images.Thumbnails.MINI_KIND,
            null
        )
        if (c != null && c.moveToFirst()) {
            val path: String =
                c.getString(c.getColumnIndex(MediaStore.Images.Thumbnails.DATA))
            c.close()
            return path
        }
        return null
    }
}