package com.seoul.culture.api

import com.google.gson.JsonObject
import com.seoul.culture.data.PatrolListRequest
import com.seoul.culture.data.PatrolResponse
import com.seoul.culture.data.ResultResponse
import com.seoul.culture.model.PatrolCompleteData
import io.reactivex.Flowable
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*


interface PatrolApi {
    // 순찰 리스트
    @GET("api/patrolList.do")
    fun getPatrolList(@Query("userId") userId: String): Flowable<PatrolResponse>

    // 순찰 태그 완료
    @GET("api/patrolSuccess.do")
    fun sendNfcTag(@Query("userId") userId: String,
                   @Query("placeId") placeId: String,
                   @Query("placeDetailId") placeDetailId: String,
                   @Query("placeTime") placeTime: String,
                   @Query("gpsLon") gpsLon: String,
                   @Query("gpsLat") gpsLat: String): Flowable<ResultResponse>

    // 순찰 완료
    @POST("api/patrolResultInsert.do")
    fun sendCompltePatrol(@Query("userId") userId: String,
                            @Body paramList: JsonObject): Flowable<ResultResponse>
}
