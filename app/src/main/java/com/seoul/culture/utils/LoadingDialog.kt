package com.seoul.culture.utils

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import com.seoul.culture.R


class LoadingDialog(private val context: Context) {
    var dialog: Dialog? = null

    fun show(show: Boolean) {
        context?.let {
            if (show) {
                dialog?.let {
                    it.show()
                    return
                }
                val inflator = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                val view = inflator.inflate(R.layout.view_loading, null)

                dialog = Dialog(context).apply {
                    this.window.setBackgroundDrawableResource(android.R.color.transparent)
                    this.setContentView(view)
                }

                dialog?.show()

            } else {
                dialog?.let {
                    it.dismiss()
                }
            }
        }

    }
}