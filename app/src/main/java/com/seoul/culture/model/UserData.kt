package com.seoul.culture.model

import com.google.gson.annotations.SerializedName

data class UserData(@SerializedName("userid") val userId: String,
                      @SerializedName("usernm") val userNm: String)


data class UuidData(@SerializedName("uuid") val uuid: String)