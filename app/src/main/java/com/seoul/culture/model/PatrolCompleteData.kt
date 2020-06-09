package com.seoul.culture.model

import com.google.gson.annotations.SerializedName


data class PatrolCompleteData(@SerializedName("userId") val userId: String,
                              @SerializedName("placeId") val placeId: String,
                         @SerializedName("placeTime") val placeTime: String)