package com.seoul.culture.api

import com.seoul.culture.config.C
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


class APIClient {
    val interceptor = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC)

    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(interceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun getPatrolApi(): PatrolApi = Retrofit.Builder()
        .baseUrl(C.BASE_URL)
        .client(okHttpClient)
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(PatrolApi::class.java)

    fun getLoginApi(): LoginApi = Retrofit.Builder()
        .baseUrl(C.BASE_URL)
        .client(okHttpClient)
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(LoginApi::class.java)

    fun getReportApi(): ReportApi = Retrofit.Builder()
        .baseUrl(C.BASE_URL)
        .client(okHttpClient)
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ReportApi::class.java)

    fun getManageApi(): ManageApi = Retrofit.Builder()
        .baseUrl(C.BASE_URL)
        .client(okHttpClient)
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ManageApi::class.java)

    fun getTrainingApi(): TrainingApi = Retrofit.Builder()
        .baseUrl(C.BASE_URL)
        .client(okHttpClient)
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(TrainingApi::class.java)

}