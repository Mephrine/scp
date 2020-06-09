package com.seoul.culture.scene.login

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableArrayList
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import com.seoul.culture.R
import com.seoul.culture.api.APIClient
import com.seoul.culture.application.ScpApplication
import com.seoul.culture.databinding.ActivityLoginBinding
import com.seoul.culture.scene.main.MainActivity
import com.seoul.culture.utils.*
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import io.reactivex.functions.Predicate
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.Timed
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.fragment_report.*
import java.util.ArrayList
import java.util.concurrent.TimeUnit


class LoginActivity : AppCompatActivity() {
    val TAG = LoginActivity::class.java.simpleName

    private val EXIT_TIMEOUT: Long = 2000
    private val backButtonClickSource = PublishSubject.create<Boolean>()
    private lateinit var binding: ActivityLoginBinding

    private var viewModel = LoginViewModel(this)
    private var loading = LoadingDialog(this)

    private val disposables by lazy {
        CompositeDisposable()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.requestUUID()
        initDataBinding()

    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
    }


    private fun initDataBinding() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login)
        binding.view = this
        setupListView()

        // 뒤로가기
        backButtonClickSource
            .debounce(100, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext {
                Toast.makeText(
                    this,
                    R.string.app_exit_title,
                    Toast.LENGTH_SHORT
                ).show()
            }
            .timeInterval(TimeUnit.MILLISECONDS)
            .skip(1)
            .filter(Predicate { interval: Timed<Boolean?> ->
                interval.time() < EXIT_TIMEOUT
            })
            .subscribe(Consumer<Timed<Boolean?>> {
                this.finish()
            })


        viewModel.moveMain
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it) moveMain()
            }.apply { disposables.add(this) }

        viewModel.progress
            .observeOn(AndroidSchedulers.mainThread())
            .share()
            .subscribe {
                loading.show(it)
            }.apply { disposables.add(this) }

        viewModel.userList.observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                (binding.listUser.adapter as LoginAdapter).run {
                    this.setLoginList(it)
                }
            }

        viewModel.sbjUUID
            .subscribe {
                tv_uuid.text = "UUID : ${it}"
            }.apply { disposables.add(this) }

        viewModel.startLogin()
    }

    override fun onResume() {
        super.onResume()
        if(!viewModel.isFirst) {
            viewModel.requestLogin(viewModel.uuid)
        }
    }

    private fun setupListView() {
        val adapter = LoginAdapter(this, viewModel)
        list_user.apply {
            this.layoutManager = LinearLayoutManager(context)
            this.adapter = adapter
        }

    }

    private fun moveMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        this.finish()
    }


    override fun onBackPressed() {
        backButtonClickSource.onNext(true)
    }

    override fun onDestroy() {
        if (!disposables.isDisposed) {
            disposables.clear()
        }
        super.onDestroy()
    }

}