package com.seoul.culture.model

import com.google.gson.annotations.SerializedName

data class TrainingData(
    @SerializedName("userid") val userId: String?,
    @SerializedName("nfcyn") val nfcYn: String,
    @SerializedName("ordermsg") val orderMsg: String,
    @SerializedName("simulid") val simulId: String,
    @SerializedName("planseq") val planSeq: String,
    @SerializedName("placedetailid") val placeDetailId: String,
    @SerializedName("placedetailnm") val placeDetailNm: String? = "",
    @SerializedName("nfccont") val nfcCont: String? = "",
    @SerializedName("nfccd") val nfcCd: String,
    @SerializedName("placeTime") val placeTime: String?,
    @SerializedName("gpsLat") val gpsLat: String?,
    @SerializedName("gpsLon") val gpsLon: String?
)