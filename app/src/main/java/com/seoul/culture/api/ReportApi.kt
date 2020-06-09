package com.seoul.culture.api

import com.google.gson.JsonObject
import com.seoul.culture.data.ResultResponse
import io.reactivex.Flowable
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.adapter.rxjava2.Result
import retrofit2.http.*

interface ReportApi {

    // 이상 징후 보고하기
    @Multipart
    @POST("api/sirenInsert.do")
    fun sendReport(
        @Part imgUpload: MultipartBody.Part?,
        @Part("userId") userId: RequestBody,
        @Part("gpsLat") gpsLat: RequestBody,
        @Part("gpsLon") gpsLon: RequestBody,
        @Part("reportText") reportText: RequestBody,
        @Part("placeId") placeId: RequestBody
    ): Flowable<ResultResponse>

    // 순찰 태그 완료
    @GET("api/emerInsert.do")
    fun sendEmergencyReport(@Query("userId") userId: String,
                            @Query("divnCd") divnCd: String,
                            @Query("gpsLon") gpsLon: String,
                            @Query("gpsLat") gpsLat: String): Flowable<ResultResponse>
}