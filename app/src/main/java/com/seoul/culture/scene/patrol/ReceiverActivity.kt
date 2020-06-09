package com.seoul.culture.scene.patrol

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import com.seoul.culture.R
import com.seoul.culture.databinding.ActivityReceiverBinding
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_receiver.*
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.nfc.NdefRecord
import android.os.Parcelable
import androidx.appcompat.app.AlertDialog
import com.seoul.culture.utils.L
import com.seoul.culture.utils.LoadingDialog
import com.seoul.culture.utils.NFCUtil
import com.seoul.culture.utils.NFC_TYPE


const val MIME_TYPE_APP = "application/com.appmaker.nfc"
//const val MIME_ALL = "*/*"

class ReceiverActivity : AppCompatActivity() {

    private var nfcAdapter: NfcAdapter? = null

    // need to check NfcAdapter for nullability. Null means no NFC support on the device
    private var isNfcSupported: Boolean =
        this.nfcAdapter != null

    // 선택한 순찰지역 ID
    private var placeId: String? = null
    private var placeDetailId: String? = null
    private var simulId: String? = null
    private var payload: String? = null
    private var nfcCont: String? = null
    private var isReceiveTag: NFC_TYPE = NFC_TYPE.READ_CHECK
    private var loading = LoadingDialog(this)

    private lateinit var binding: ActivityReceiverBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_receiver)
        val manager = getSystemService(Context.NFC_SERVICE) as NfcManager

        this.nfcAdapter = manager?.defaultAdapter?.let { it }
        isNfcSupported = this.nfcAdapter != null


        if (!isNfcSupported) {
            Toast.makeText(this, "NFC를 지원하지 않는 디바이스입니다.", Toast.LENGTH_SHORT).show()
            setResult(Activity.RESULT_CANCELED)
            this.finishActivity()
            return
        }

        nfcAdapter?.let {
            if (!it.isEnabled) {
                Toast.makeText(
                    this,
                    "NFC가 비활성화 상태입니다. NFC를 켜주세요.",
                    Toast.LENGTH_SHORT
                ).show()
                setResult(Activity.RESULT_CANCELED)
                this.finishActivity()
                return
            }
        }

        binding = DataBindingUtil.setContentView(this, R.layout.activity_receiver)
        binding.view = this

        placeId = intent.getStringExtra("placeId")
        placeDetailId = intent.getStringExtra("placeDetailId")
        payload = intent.getStringExtra("payload")
        simulId = intent.getStringExtra("simulId")
        nfcCont = intent.getStringExtra("nfcCont")

        val nfcType: String? = intent.getStringExtra("nfcType")

        nfcType?.let {
            isReceiveTag =  NFC_TYPE.valueOf(it)
        }
    }

    fun onClick(view: View) {
        when(view.id) {
            R.id.btn_close -> {
                this.finishActivity()
            }
        }
    }

    fun finishActivity() {
        finish()
        overridePendingTransition(R.anim.anim_no, R.anim.anim_down)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        when(isReceiveTag) {
            NFC_TYPE.READ_CHECK -> {
                val string = NFCUtil.retrieveNFCMessage(intent)
                if (string.contains(nfcCont ?: "")) {
                    Toast.makeText(this, getString(R.string.patrol_nfc_success), Toast.LENGTH_SHORT).show()
                    val intent = Intent()
                    intent.putExtra("placeId", placeId)
//                    intent.putExtra("placeDetailId", string)
                 placeDetailId?.let {
                        intent.putExtra("placeDetailId", placeDetailId)
                    }

                    simulId?.let {
                        intent.putExtra("simulId", simulId)
                    }
                    setResult(Activity.RESULT_OK, intent)
                    finishActivity()
                } else {
                    Toast.makeText(this, getString(R.string.patrol_nfc_fail), Toast.LENGTH_SHORT).show()
                }
            }
            NFC_TYPE.READ_DELIVERY -> {
                val string = NFCUtil.retrieveNFCMessage(intent)
                if (string.isNotEmpty()) {
                    val intent = Intent()
                    intent.putExtra("placeId", string)
                    setResult(Activity.RESULT_OK, intent)
                    finishActivity()
                }
            }
            NFC_TYPE.WRITE -> {
                payload?.let {
                    val messageWrittenSuccessfully = NFCUtil.createNFCMessage(it,intent, this)
                    if (messageWrittenSuccessfully) {
                        android.app.AlertDialog.Builder(this)
                            .setMessage(getString(R.string.manage_nfc_write_success, it))
                            .setPositiveButton(
                                android.R.string.ok,
                                DialogInterface.OnClickListener { dialog, which ->
                                    val intent = Intent()
                                    intent.putExtra("placeDetailId", placeDetailId)
                                    setResult(Activity.RESULT_OK, intent)
                                    finishActivity()
                                    dialog.dismiss()
                                })
                            .create()
                            .show()



                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        nfcAdapter?.let {
            NFCUtil.enableNFCInForeground(it, this, javaClass)
        }


    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.let {
            NFCUtil.disableNFCInForeground(it, this)
        }
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_CANCELED)
        this.finishActivity()
    }
}

fun <T> Boolean.ifElse(primaryResult: T, secondaryResult: T) = if (this) primaryResult else secondaryResult