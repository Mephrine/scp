package com.seoul.culture.scene.training

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.BindingAdapter
import androidx.databinding.ObservableArrayList
import androidx.databinding.library.baseAdapters.BR
import androidx.recyclerview.widget.RecyclerView
import com.seoul.culture.R
import com.seoul.culture.config.C
import com.seoul.culture.databinding.ItemTrainingBinding
import com.seoul.culture.model.TrainingData
import com.seoul.culture.scene.patrol.ReceiverActivity
import com.seoul.culture.scene.patrol.SendType
import com.seoul.culture.utils.NFC_TYPE


class TrainingAdapter(private val activity: Activity, private val fragment: TrainingFragment, private var items: List<TrainingData> = emptyList(), private var viewModel: TrainingViewModel) : RecyclerView.Adapter<TrainingAdapter.TrainingViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrainingViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemTrainingBinding.inflate(
            inflater,
            parent,
            false)

        return TrainingViewHolder(activity, fragment, binding, viewModel)
    }

    fun setTrainingList(trainingList: List<TrainingData>?) {
        trainingList?.let {
            this.items = trainingList
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: TrainingViewHolder, position: Int) {
        holder.bindTraining(items[position], position)
    }


    companion object {
        @BindingAdapter("item")
        @JvmStatic
        fun bindItem(recyclerView: RecyclerView, items: ObservableArrayList<TrainingData>) {
            val adapter: TrainingAdapter? = recyclerView.adapter as? TrainingAdapter
            // 생략
            if (adapter != null) {
                adapter.setTrainingList(items)
            }
        }
    }

    class TrainingViewHolder constructor(private val activity: Activity, private val fragment: TrainingFragment, private var mItemTrainingBinding: ItemTrainingBinding, private var viewModel: TrainingViewModel) : RecyclerView.ViewHolder(mItemTrainingBinding.itemTraining) {
        fun bindTraining(trainingItem: TrainingData, position: Int) {
            mItemTrainingBinding.setVariable(BR.holder,this)
            mItemTrainingBinding.setVariable(BR.data, trainingItem)
            mItemTrainingBinding.setVariable(BR.position, position)
        }


        fun onItemClick(data: TrainingData) {
            if (data.placeTime == null || data.placeTime.isEmpty()) {
                when(data.nfcYn) {
                    "Y" -> {
                        val intent = Intent(activity, ReceiverActivity::class.java)
                        intent.putExtra("placeDetailId",data.placeDetailId)
                        intent.putExtra("simulId",data.simulId)
                        intent.putExtra("placeId",data.planSeq)
                        intent.putExtra("nfcType",NFC_TYPE.READ_CHECK.name)
                        activity.startActivityForResult(intent, C.REQ_CODE_NFC_CHECK)
                        activity.overridePendingTransition(R.anim.anim_up, R.anim.anim_no)

                    }
                    else -> {
                        fragment.selectCamera(data.planSeq, data.placeDetailId ?: "", data.simulId)
                    }
                }
            }
        }
    }
}