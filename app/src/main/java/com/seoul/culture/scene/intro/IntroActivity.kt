package com.seoul.culture.scene.intro

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.Observable
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import com.seoul.culture.R
import com.seoul.culture.application.ScpApplication
import com.seoul.culture.databinding.ActivityIntroBinding
import com.seoul.culture.scene.main.MainActivity
import com.seoul.culture.scene.login.LoginActivity
import com.seoul.culture.utils.L
import com.seoul.culture.utils.LoadingDialog
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import io.reactivex.functions.Predicate
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.Timed
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_login.*
import java.util.ArrayList
import java.util.concurrent.TimeUnit

class IntroActivity : AppCompatActivity() {
    val TAG = IntroActivity::class.java.simpleName

    private val EXIT_TIMEOUT: Long = 2000
    private val backButtonClickSource = PublishSubject.create<Boolean>()
    private lateinit var binding: ActivityIntroBinding

    private var loading = LoadingDialog(this)

    private val disposables by lazy {
        CompositeDisposable()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initDataBinding()



    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
    }


    private fun initDataBinding() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_intro)
        binding.view = this

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


        io.reactivex.Observable.timer(2, TimeUnit.SECONDS)
            .take(1)
            .subscribe({

                if (ScpApplication.prefs.userId.isEmpty()) {
                    L.d("id!!! 1 :  ${ScpApplication.prefs.userId}")
                    moveLogin()
                } else {
                    L.d("id!!! 2 :  ${ScpApplication.prefs.userId}")
                    moveMain()
                }
            }).apply { disposables.add(this) }
    }

    private fun moveMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        this.finish()
    }

    private fun moveLogin() {
        val intent = Intent(this, LoginActivity::class.java)
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