package com.seoul.culture.scene.patrol

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.BindingAdapter
import androidx.databinding.Observable
import androidx.databinding.ObservableArrayList
import androidx.databinding.library.baseAdapters.BR
import androidx.recyclerview.widget.RecyclerView
import com.seoul.culture.R
import com.seoul.culture.config.C
import com.seoul.culture.databinding.ItemPatrolBinding
import com.seoul.culture.model.PatrolData
import com.seoul.culture.model.PatrolNfcData
import com.seoul.culture.utils.FontSansType
import com.seoul.culture.utils.NFC_TYPE
import com.seoul.culture.utils.setFont


class PatrolAdapter(
    private val activity: Activity,
    private var items: List<PatrolNfcData> = emptyList()
) : RecyclerView.Adapter<PatrolAdapter.PatrolViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatrolViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemPatrolBinding.inflate(
            inflater,
            parent,
            false
        )

        return PatrolViewHolder(activity, binding)
    }

    fun setPatrolList(patrolList: List<PatrolNfcData>?) {
        patrolList?.let {
            this.items = patrolList
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: PatrolViewHolder, position: Int) {
        holder.bindPatrol(items[position], position)
    }

    companion object {
        @BindingAdapter("item")
        @JvmStatic
        fun bindItem(recyclerView: RecyclerView, items: ObservableArrayList<PatrolNfcData>) {
            val adapter: PatrolAdapter? = recyclerView.adapter as? PatrolAdapter
            // 생략
            if (adapter != null) {
                adapter.setPatrolList(items)
            }
        }
    }

    class PatrolViewHolder constructor(
        private val activity: Activity,
        private var mItemPatrolBinding: ItemPatrolBinding
    ) : RecyclerView.ViewHolder(mItemPatrolBinding.itemPatrol) {
        fun bindPatrol(patrolItem: PatrolNfcData, position: Int) {
            mItemPatrolBinding.setVariable(BR.holder, this)
            mItemPatrolBinding.setVariable(BR.data, patrolItem)
            mItemPatrolBinding.setVariable(BR.position, position)
        }


        fun onItemClick(data: PatrolNfcData) {

            if (data.placeTime.isEmpty()) {
                val intent = Intent(activity, ReceiverActivity::class.java)
                intent.putExtra("placeId", data.placeId)
                intent.putExtra("placeDetailId", data.placeDetailId)
                intent.putExtra("nfcCont", data.nfcCont)
                intent.putExtra("nfcType", NFC_TYPE.READ_CHECK.name)
                activity.startActivityForResult(intent, C.REQ_CODE_NFC_CHECK)
                activity.overridePendingTransition(R.anim.anim_up, R.anim.anim_no)

            }
        }
    }
}
