package com.seoul.culture.utils

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.util.Log
import android.widget.Toast
import com.seoul.culture.BuildConfig
import com.seoul.culture.R
import com.seoul.culture.config.C
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*


object NFCUtil {

    fun createNFCMessage(payload: String, intent: Intent?, context: Context): Boolean {

        val pathPrefix = "com.seoul.culture"
//        val nfcRecord = NdefRecord(NdefRecord.TNF_EXTERNAL_TYPE, pathPrefix.toByteArray(), ByteArray(0), payload.toByteArray())
        val nfcRecord = NdefRecord(NdefRecord.TNF_EXTERNAL_TYPE, "text/plain".toByteArray(), ByteArray(0), payload.toByteArray())
//        val nfcRecord = NdefRecord.createMime(payload, payload.toByteArray())
        val nfcMessage = NdefMessage(arrayOf(nfcRecord))


//        val nfcMessage = createTextMessage(payload)
//        intent?.let {
//            val tag = it.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
//            nfcMessage?.let {
//                return writeMessageToTag(it, tag, context)
//            }
//        }
        intent?.let {
            val tag = it.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
//            return writeMessageToTag(nfcMessage, tag, context)
            return writeTag(tag, nfcMessage, context)
        }

        return false
    }

    fun writeTag(tag: Tag?, message: NdefMessage, context: Context): Boolean {
        val size = message.toByteArray().size
        return try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                if (!ndef.isWritable) {
                    Toast.makeText(
                        context, context.getString(R.string.manage_nfc_isreadonly),
                        Toast.LENGTH_SHORT
                    ).show()
                    return false
                }
                if (ndef.maxSize < size) {
                    Toast.makeText(
                        context, context.getString(R.string.manage_nfc_max_tag),
                        Toast.LENGTH_SHORT
                    ).show()
                    return false
                }
//                if (BuildConfig.IS_OP) {
                    ndef.canMakeReadOnly()
//                }

                ndef.writeNdefMessage(message)
                ndef.close()

//                Toast.makeText(
//                    context, context.getString(R.string.manage_nfc_write_success, message),
//                    Toast.LENGTH_SHORT
//                ).show()

                return true
            } else {
                val format = NdefFormatable.get(tag)
                if (format != null) {
                    try {
                        format.connect()
                        format.format(message)
//                        Toast.makeText(
//                            context, context.getString(R.string.manage_nfc_write_success, message),
//                            Toast.LENGTH_SHORT
//                        ).show()
                        format.close()
                        return true
                    } catch (e: IOException) {
                        Toast.makeText(
                            context, context.getString(R.string.manage_nfc_fail),
                            Toast.LENGTH_SHORT
                        ).show()
                        return false
                    }
                } else {
                    Toast.makeText(
                        context, context.getString(R.string.manage_nfc_fail),
                        Toast.LENGTH_SHORT
                    ).show()
                    return false
                }
            }
        } catch (e: java.lang.Exception) {
            return false
        }
    }

//    private fun writeMessageToTag(nfcMessage: NdefMessage, tag: Tag?, context: Context): Boolean {
//
//        try {
//            val nDefTag = Ndef.get(tag)
//            Log.d("Error","####test1")
//            nDefTag?.let {
//                Log.d("Error","####test2")
//                it.connect()
//                if (it.maxSize < nfcMessage.toByteArray().size) {
//                    Log.d("Error","####test3")
//                    //Message to large to write to NFC tag
//                    Toast.makeText(
//                        context, context.getString(R.string.manage_nfc_max_tag),
//                        Toast.LENGTH_SHORT
//                    ).show()
//                    return false
//                }
//                if (!it.isWritable) {
//                    Log.d("Error","####test4")
//                    it.writeNdefMessage(nfcMessage)
//                    it.close()
//                    //Message is written to tag
//                    Toast.makeText(
//                        context, context.getString(R.string.manage_nfc_write_success),
//                        Toast.LENGTH_SHORT
//                    ).show()
//                    return true
//                } else {
//                    Log.d("Error","####test5")
//                    //NFC tag is read-only
//                    Toast.makeText(
//                        context, context.getString(R.string.manage_nfc_isreadonly),
//                        Toast.LENGTH_SHORT
//                    ).show()
//                    return false
//                }
//            }
//
//            val nDefFormatableTag = NdefFormatable.get(tag)
//
//            nDefFormatableTag?.let {
//                try {
//                    Log.d("Error","####test6")
//                    it.connect()
//                    //TODO test readonly
//                    it.formatReadOnly(nfcMessage)
//                    it.format(nfcMessage)
//                    it.close()
//                    //The data is written to the tag
//                    Toast.makeText(
//                        context, context.getString(R.string.manage_nfc_write_success),
//                        Toast.LENGTH_SHORT
//                    ).show()
//                    return true
//                } catch (e: IOException) {
//                    Log.d("Error","####test7")
//                    Log.e("Error",e.message)
//                    Toast.makeText(
//                        context, context.getString(R.string.manage_nfc_fail),
//                        Toast.LENGTH_SHORT
//                    ).show()
//                    //Failed to format tag
//                    return false
//                }
//            }
//            Log.d("Error","####test8")
//            Toast.makeText(
//                context, context.getString(R.string.manage_nfc_write_impossible),
//                Toast.LENGTH_SHORT
//            ).show()
//            //NDEF is not supported
//            return false
//
//        } catch (e: Exception) {
//            //Write operation has failed
////            Log.e("Error",e.message)
//            Toast.makeText(
//                context, context.getString(R.string.manage_nfc_write_fail),
//                Toast.LENGTH_SHORT
//            ).show()
//        }
//        return false
//    }
//
//    fun createUriMessage(content: String, type: String): NdefMessage {
//        val record: NdefRecord = NdefRecord.createUri(type + content)
//        return NdefMessage(arrayOf<NdefRecord>(record))
//    }
//
//    fun createTextMessage(content: String): NdefMessage? {
//        try { // Get UTF-8 byte
//
//            val lang: ByteArray = Locale.getDefault().language.toByteArray(charset("UTF-8"))
//            val text = content.toByteArray(charset("UTF-8")) // Content in UTF-8
//            val langSize = lang.size
//            val textLength = text.size
//            val payload = ByteArrayOutputStream(1 + langSize + textLength)
//            payload.write((langSize and 0x1F))
//            payload.write(lang, 0, langSize)
//            payload.write(text, 0, textLength)
//            val record = NdefRecord(
//                NdefRecord.TNF_WELL_KNOWN,
//                NdefRecord.RTD_TEXT, ByteArray(0),
//                payload.toByteArray()
//            )
//            return NdefMessage(arrayOf<NdefRecord>(record))
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//        return null
//    }

    fun retrieveNFCMessage(intent: Intent?): String {
        intent?.let {
            if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
                val nDefMessages = getNDefMessages(intent)
                nDefMessages[0].records?.let {
                    it.forEach {
                        it?.payload?.let {
//                            it.let {
                                return String(it)
//                            }
                        }
                    }
                }

            } else if (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action) {
                val techMessage = getTechMessages(intent)
                techMessage.records?.let {
                    it.forEach {
                        it?.payload?.let {
//                            it?.let {
                                return String(it)
//                            }
                        }
                    }
                }
            }

            else {
                return "Touch NFC tag to read data"
            }
        }
        return "Touch NFC tag to read data"
    }

    private fun getTechMessages(intent: Intent): NdefMessage {

        val tag: Tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        val techList = tag.techList
        val searchedTech = Ndef::class.java.name


        for (tech in techList) {
            if (searchedTech == tech) {
                tag?.let {
                    val rawMessage = Ndef.get(tag)
                    return rawMessage.cachedNdefMessage
                }
            }
        }

        // Unknown tag type
        val empty = byteArrayOf()
        val record = NdefRecord(NdefRecord.TNF_UNKNOWN, empty, empty, empty)
        val msg = NdefMessage(arrayOf(record))
        return msg
    }


    private fun getNDefMessages(intent: Intent): Array<NdefMessage> {

        val rawMessage = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        rawMessage?.let {
            return rawMessage.map {
                it as NdefMessage
            }.toTypedArray()
        }
        // Unknown tag type
        val empty = byteArrayOf()
        val record = NdefRecord(NdefRecord.TNF_UNKNOWN, empty, empty, empty)
        val msg = NdefMessage(arrayOf(record))
        return arrayOf(msg)
    }

    fun disableNFCInForeground(nfcAdapter: NfcAdapter, activity: Activity) {
        nfcAdapter.disableForegroundDispatch(activity)
    }

    fun <T> enableNFCInForeground(nfcAdapter: NfcAdapter, activity: Activity, classType: Class<T>) {
        val pendingIntent = PendingIntent.getActivity(activity, 0,
            Intent(activity, classType).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0)
        val nfcIntentFilter = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
        val filters = arrayOf(nfcIntentFilter)

        val TechLists = arrayOf(arrayOf(Ndef::class.java.name), arrayOf(NdefFormatable::class.java.name))

        nfcAdapter.enableForegroundDispatch(activity, pendingIntent, filters, TechLists)
    }




}