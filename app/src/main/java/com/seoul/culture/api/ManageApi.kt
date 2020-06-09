package com.seoul.culture.api

import com.seoul.culture.data.ManageDetailResponse
import com.seoul.culture.data.PatrolResponse
import com.seoul.culture.data.ResultResponse
import com.seoul.culture.data.TrainingResponse
import io.reactivex.Flowable
import retrofit2.http.GET
import retrofit2.http.Query




interface ManageApi {
    // nfc 리스트
    @GET("api/nfcList.do")
    fun nfcList(@Query("userId") uuid: String): Flowable<PatrolResponse>
    // nfc 상세 리스트
    @GET("api/nfcDetailList.do")
    fun nfcDetail(@Query("userId") userId: String,
                    @Query("placeId") placeId: String): Flowable<ManageDetailResponse>
    // nfc 좌표 데이터 전달.
    @GET("api/nfcUpdate.do")
    fun insertNfcWrite(@Query("placeDetailId") placeDetailId: String,
                    @Query("gpsLat") gpsLat: String,
                     @Query("gpsLon") gpsLon: String): Flowable<ResultResponse>

    // nfc리스트 등록 - 사용 X
    @GET("api/patrolInsert.do")
    fun insertPatrol(@Query("userId") userId: String,
                    @Query("placeId") placeId: String): Flowable<ResultResponse>
    // nfc리스트 삭제 - 사용 X
    @GET("api/patrolDelete.do")
    fun deletePatrol(@Query("userId") userId: String,
                   @Query("placeId") placeId: String): Flowable<ResultResponse>
}
