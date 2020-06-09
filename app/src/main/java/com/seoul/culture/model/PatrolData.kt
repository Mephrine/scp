package com.seoul.culture.model

import com.google.gson.annotations.SerializedName

data class PatrolData(@SerializedName("placeid") val placeId: String,
                     @SerializedName("placenm") val placeNm: String,
                      @SerializedName("placedetailid") val placeDetailId: String,
                      @SerializedName("placedetailnm") val placeDetailNm: String,
                      @SerializedName("nfccont") val nfcCont: String)