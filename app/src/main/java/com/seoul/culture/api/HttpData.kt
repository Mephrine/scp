package com.seoul.culture.api

import okhttp3.*
import java.io.IOException
import java.util.concurrent.TimeUnit

interface Http{
//    fun url(method:String, url:String):Http
    fun header(key:String, value:String):Http
    fun form(key:String, value:String):Http
    fun json(json:String):Http
    fun file(key: String, filename: String, mine: String, file: ByteArray):Http
    fun send(callback:(ResponseBody?, String?)->Unit)
}

private val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(3, TimeUnit.SECONDS)
    .writeTimeout(10, TimeUnit.SECONDS)
    .readTimeout(5, TimeUnit.SECONDS)
    .build()

private val JSON = MediaType.parse("application/json; charset=utf-8")

class HttpData internal constructor(
    private val method:String,
    private var request:Request.Builder
):Http{
    private var form: FormBody.Builder? = null
    private var json:String? = null
    private var multi:MultipartBody.Builder? = null



    override fun file(key: String, filename: String, mine: String, file: ByteArray):Http {
        if(multi == null) multi = MultipartBody.Builder().setType(MultipartBody.FORM)
        multi?.addFormDataPart(key, filename, RequestBody.create(MediaType.parse(mine), file))
        return this
    }

    override fun header(key:String, value:String):Http{
        request = request.addHeader(key, value)
        return this
    }
    override fun form(key:String, value:String):Http{
        if(form == null) form = FormBody.Builder()
        form?.add(key, value)
        return this
    }
    override fun json(json:String):Http{
        this.json = json
        return this
    }
    override fun send(callback:(ResponseBody?, String?)->Unit){
        if(method == "POST") multi?.let {multi->
            json?.let {multi.addPart(RequestBody.create(JSON, it))} ?:
            form?.let {multi.addPart(it.build())}
            request = request.post(multi.build())
        } ?:
        json?.let{request = request.post(RequestBody.create(JSON, it))} ?:
        form?.let{request = request.post(it.build())}
        okHttpClient.newCall(request.build()).enqueue(object:Callback{
            override fun onFailure(call: Call, e: IOException){
                callback(null, e.toString())
            }
            override fun onResponse(call: Call, response: Response){
                response.body()?.let{callback(it, null)} ?: callback(null, "body error")
            }
        })
    }
}
