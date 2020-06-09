package com.seoul.culture.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.seoul.culture.model.GPSData
import com.seoul.culture.model.PatrolNfcData
import com.seoul.culture.model.TrainingData
import java.lang.Exception


private const val FILENAME = "ScpSharedPreferences"
private const val PREF_USER_NAME = "userName"
private const val PREF_USER_ID = "userId"
private const val PREF_UUID = "UUID"
private const val PREF_START_PATROL = "startPatrol" // 순찰 시작 관련.
private const val PREF_PATROL_LIST = "patrolList" // 순찰 시작 관련.
private const val PREF_PATROL_FAIL_LIST = "patrolFailList" // 순찰 시작 관련.

//순찰 훈련
private const val PREF_START_PATROL_TRAINING = "startPatrolTraining" // 순찰 시작 관련.
private const val PREF_PATROL_TRAINING_LIST = "patroTraininglList" // 순찰 시작 관련.
private const val PREF_PATROL_TRAINING_FAIL_LIST = "patrolTrainingFailList" // 순찰 시작 관련.

private const val PREF_GPS_INFO_LIST = "gpsInfoList" // 순찰 시작 관련.

class ScpSharedPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(FILENAME, 0)
    var userName: String
        get() = prefs.getString(PREF_USER_NAME, "")
        set(value) = prefs.edit().putString(PREF_USER_NAME, value).apply()

    var userId: String
        get() = prefs.getString(PREF_USER_ID, "")
        set(value) = prefs.edit().putString(PREF_USER_ID, value).apply()

    var uuid: String
        get() = prefs.getString(PREF_UUID, "")
        set(value) = prefs.edit().putString(PREF_UUID, value).apply()

    var startPatrol: Boolean
        get() = prefs.getBoolean(PREF_START_PATROL, false)
        set(value) = prefs.edit().putBoolean(PREF_START_PATROL, value).apply()

    var patrolList: List<PatrolNfcData>
        get() = prefs.getList(prefs, PREF_PATROL_LIST)
        set(value) = prefs.edit().putList(prefs, PREF_PATROL_LIST, value).apply {  }

    // 네트워크 오류 시, 순찰 완료에서 추가해서 보낼 데이터들.
    var patrolFailList: List<PatrolNfcData>
        get() = prefs.getList(prefs, PREF_PATROL_FAIL_LIST)
        set(value) = prefs.edit().putList(prefs, PREF_PATROL_FAIL_LIST, value).apply {  }

    var startTraining: Boolean
        get() = prefs.getBoolean(PREF_START_PATROL_TRAINING, false)
        set(value) = prefs.edit().putBoolean(PREF_START_PATROL_TRAINING, value).apply()

    var trainingList: List<TrainingData>
        get() = prefs.getList(prefs, PREF_PATROL_TRAINING_LIST)
        set(value) = prefs.edit().putList(prefs, PREF_PATROL_TRAINING_LIST, value).apply {  }

    // 네트워크 오류 시, 순찰 완료에서 추가해서 보낼 데이터들.
    var trainingFailList: List<TrainingData>
        get() = prefs.getList(prefs, PREF_PATROL_TRAINING_FAIL_LIST)
        set(value) = prefs.edit().putList(prefs, PREF_PATROL_TRAINING_FAIL_LIST, value).apply {  }

    var gpsInfoList: List<GPSData>
        get() = prefs.getList(prefs, PREF_GPS_INFO_LIST)
        set(value) = prefs.edit().putList(prefs, PREF_GPS_INFO_LIST, value).apply {  }
}

class SomeClass<T> {
    var myVar: T? = null
        set(value) {
            executeCustomFunc(value)
            field = value
        }

    private fun executeCustomFunc(v: T?) {

    }
}

private fun <T> SharedPreferences.Editor.putList(prefs: SharedPreferences, id: String, values: List<T>) {
    val editor = prefs.edit()
    val gson = Gson()
    val type = object: TypeToken<List<T>>() {
    }.type

    val json = gson.toJson(values, type)

    editor.putString(id, json)
    editor.apply()
}

private fun <T> SharedPreferences.getList(prefs: SharedPreferences, id: String): List<T> {
    val gson = Gson()
    val json = prefs.getString(id, "")



    val type = if (id == PREF_PATROL_LIST || id == PREF_PATROL_FAIL_LIST) {
        object : TypeToken<List<PatrolNfcData>>() {
        }.type
    } else if (id == PREF_PATROL_TRAINING_LIST || id == PREF_PATROL_TRAINING_FAIL_LIST) {
        object : TypeToken<List<TrainingData>>() {
        }.type
    }else if (id == PREF_GPS_INFO_LIST) {
        object : TypeToken<List<GPSData>>() {
        }.type
    } else {
        object : TypeToken<List<T>>() {
        }.type
    }

    if (json == null || json == "") {
        return ArrayList()
    } else {
        try {
            return gson.fromJson(json, type)
        } catch (e: Exception) {
            return ArrayList()
        }
    }
}