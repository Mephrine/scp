package com.seoul.culture.api

import com.google.gson.JsonObject
import com.seoul.culture.data.TrainingResponse
import com.seoul.culture.data.ResultResponse
import com.seoul.culture.model.PatrolCompleteData
import com.seoul.culture.model.TrainingCompleteData
import io.reactivex.Flowable
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface TrainingApi {
    // 순찰훈련 리스트
    @GET("api/patrolSimulList.do")
    fun getTrainingList(@Query("userId") userId: String): Flowable<TrainingResponse>

    // 순찰훈련 태그 완료
//    @GET("api/patrolSimulSuccess.do")
//    fun sendNfcTag(@Query("userId") userId: String,
//                   @Query("planSeq") planSeq: String,
//                   @Query("simulId") simulId: String,
//                   @Query("gpsLon") gpsLon: String,
//                   @Query("gpsLat") gpsLat: String,
//                   @Query("placeDetailId") placeDetailId: String,
//                   @Query("placeTime") placeTime: String): Flowable<ResultResponse>

    // 사진
//    @GET("api/patrolSimulSuccess.do")
//    fun sendStart(
//        @Query("userId") userId: String,
//        @Query("planSeq") planSeq: String,
//        @Query("simulId") simulId: String,
//        @Query("gpsLon") gpsLon: String,
//        @Query("gpsLat") gpsLat: String,
//        @Query("placeDetailId") placeDetailId: String,
//        @Query("placeTime") placeTime: String
//    ): Flowable<ResultResponse>


    @Multipart
    @POST("api/patrolSimulSuccess.do")
    fun sendNfcTag(
        @Part imgUpload: MultipartBody.Part?,
        @Part("userId") userId: RequestBody,
        @Part("planSeq") planSeq: RequestBody,
        @Part("simulId") simulId: RequestBody,
        @Part("gpsLat") gpsLat: RequestBody,
        @Part("gpsLon") gpsLon: RequestBody,
        @Part("placeDetailId") placeDetailId: RequestBody,
        @Part("placeTime") placeTime: RequestBody
    ): Flowable<ResultResponse>


    // 순찰훈련 완료
    @POST("api/patrolSimulResultInsert.do")
    fun sendCompleteTraining(@Query("userId") userId: String,
                             @Body paramList: JsonObject): Flowable<ResultResponse>


}