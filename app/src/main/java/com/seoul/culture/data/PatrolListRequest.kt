package com.seoul.culture.data

import com.seoul.culture.model.PatrolCompleteData


data class PatrolListRequest(var userId: String, var paramList: List<PatrolCompleteData>)