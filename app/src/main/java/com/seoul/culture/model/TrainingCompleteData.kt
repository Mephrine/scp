package com.seoul.culture.model

import com.google.gson.annotations.SerializedName


data class TrainingCompleteData(@SerializedName("userId") val userId: String,
                                @SerializedName("simulId") val simulId: String,
                              @SerializedName("placeId") val placeId: String,
                              @SerializedName("placeTime") val placeTime: String)