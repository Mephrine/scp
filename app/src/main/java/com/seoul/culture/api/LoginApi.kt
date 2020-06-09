package com.seoul.culture.api

import com.seoul.culture.data.LoginResponse
import io.reactivex.Flowable
import org.json.JSONObject
import retrofit2.http.*


interface LoginApi {
    // 로그인 리스트
    @POST("api/loginUserList.do")
    fun sendLogin(@Query("uuid") userId: String): Flowable<LoginResponse>
}
