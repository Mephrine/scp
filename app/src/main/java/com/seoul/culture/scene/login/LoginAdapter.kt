package com.seoul.culture.scene.login

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.BindingAdapter
import androidx.databinding.ObservableArrayList
import androidx.databinding.library.baseAdapters.BR
import androidx.recyclerview.widget.RecyclerView
import com.seoul.culture.application.ScpApplication
import com.seoul.culture.databinding.ItemLoginBinding
import com.seoul.culture.model.UserData


class LoginAdapter(private val activity: Activity, private val viewModel: LoginViewModel) : RecyclerView.Adapter<LoginAdapter.LoginViewHolder>() {
    private var items = ArrayList<UserData>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LoginViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemLoginBinding.inflate(
            inflater,
            parent,
            false)

        return LoginViewHolder(activity, viewModel, binding)
    }

    fun setLoginList(userList: List<UserData>?) {
        userList?.let {
            items = ArrayList(userList)
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: LoginViewHolder, position: Int) {
        holder.bindLogin(items[position], position)
    }


    class LoginViewHolder constructor(private val activity: Activity, private val viewModel: LoginViewModel, private var mItemLoginBinding: ItemLoginBinding): RecyclerView.ViewHolder(mItemLoginBinding.itemLogin) {
        fun bindLogin(loginItem: UserData, position: Int) {
            mItemLoginBinding.setVariable(BR.holder,this)
            mItemLoginBinding.setVariable(BR.data, loginItem)
            mItemLoginBinding.setVariable(BR.position, position)
        }


        fun onItemClick(data: UserData) {
            ScpApplication.prefs.userId = data.userId
            ScpApplication.prefs.userName = data.userNm

            viewModel.moveMain.onNext(true)
        }
    }
}