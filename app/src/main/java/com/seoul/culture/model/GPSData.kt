package com.seoul.culture.model

import com.google.gson.annotations.SerializedName

data class GPSData(@SerializedName("gpsLon") val lon: String,
                      @SerializedName("gpsLat") val lat: String)