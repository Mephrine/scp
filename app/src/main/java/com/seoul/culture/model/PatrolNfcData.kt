package com.seoul.culture.model

import com.google.gson.annotations.SerializedName

data class PatrolNfcData(@SerializedName("placeId") val placeId: String,
                      @SerializedName("placeNm") val placeNm: String,
                         @SerializedName("placeDetailId") val placeDetailId: String,
                         @SerializedName("placeDetailNm") val placeDetailNm: String,
                      @SerializedName("placeTime") val placeTime: String,
                         @SerializedName("nfcCont") val nfcCont: String)