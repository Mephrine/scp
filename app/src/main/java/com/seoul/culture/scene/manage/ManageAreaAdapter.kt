package com.seoul.culture.scene.manage

import android.R
import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.BindingAdapter
import androidx.databinding.ObservableArrayList
import androidx.databinding.library.baseAdapters.BR
import androidx.recyclerview.widget.RecyclerView
import com.seoul.culture.databinding.ItemManageBinding
import com.seoul.culture.model.PatrolData


class ManageAreaAdapter(private val activity: Activity, private var items: List<PatrolData> = emptyList(), private val viewModel: ManageAreaViewModel) : RecyclerView.Adapter<ManageAreaAdapter.ManageViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ManageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemManageBinding.inflate(
            inflater,
            parent,
            false)

        return ManageViewHolder(activity, binding, viewModel)
    }

    fun setManageList(manageList: List<PatrolData>?) {
        if (manageList != null) {
            this.items = manageList!!
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: ManageViewHolder, position: Int) {
        holder.bindManage(items[position], position)
    }


    companion object {
        @BindingAdapter("item")
        @JvmStatic
        fun bindItem(recyclerView: RecyclerView, items: ObservableArrayList<PatrolData>) {
            val adapter: ManageAreaAdapter? = recyclerView.adapter as? ManageAreaAdapter
            // 생략
            if (adapter != null) {
                adapter.setManageList(items)
            }
        }
    }

    class ManageViewHolder constructor(private val activity: Activity, private var mItemManageBinding: ItemManageBinding, private val viewModel: ManageAreaViewModel) : RecyclerView.ViewHolder(mItemManageBinding.itemManage) {
        fun bindManage(manageItem: PatrolData, position: Int) {
            mItemManageBinding.setVariable(BR.holder, this)
            mItemManageBinding.setVariable(BR.data, manageItem)
            mItemManageBinding.setVariable(BR.position, position)
        }

        fun onItemClick(manageItem: PatrolData) {
            viewModel.fetchNfcDetail(manageItem.placeId)
        }
    }
}