package com.seoul.culture.utils

import android.content.Context
import android.graphics.Typeface
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.seoul.culture.R

enum class FontSansType {
    DEMI_LIGHT, NORMAL
//    DEMI_LIGHT{
//        override fun path(): String {
//            return "/system/font_family/NotoSansCJK-DemiLight.ttc"
//        }
//    },REGULAR {
//        override fun path(): String {
//            return "/system/font_family/NotoSansCJK-Regular.ttc"
//        }
//    };
//
//    abstract fun path(): String
}

fun TextView.setFont(context: Context, type: FontSansType) {
    when(type){
        FontSansType.DEMI_LIGHT -> {
            this.typeface = ResourcesCompat.getFont(context, R.font.sans_light)
        }
        FontSansType.NORMAL -> {
            this.typeface = ResourcesCompat.getFont(context, R.font.sans_medium)
        }
    }

    this.includeFontPadding = false
}