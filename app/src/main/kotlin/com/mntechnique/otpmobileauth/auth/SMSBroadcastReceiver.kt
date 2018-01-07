package com.mntechnique.otpmobileauth.auth

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.content.LocalBroadcastManager
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.telephony.TelephonyManager
import android.util.Log
import org.jetbrains.anko.telephonyManager

/**
 * Created by gaurav on 19/8/17.
 */

class SMSBroadcastReceiver : BroadcastReceiver() {
    var sms = SmsManager.getDefault()

    override fun onReceive(context: Context, intent: Intent) {
        var bundle = intent.extras
        if (bundle != null) {

            var pdus:Array<Object> = bundle.get("pdus") as Array<Object>

            for (i in 0..pdus.size -1) {
                var message:SmsMessage? = null
                when(context.telephonyManager.phoneType) {
                    TelephonyManager.PHONE_TYPE_GSM -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            message = SmsMessage.createFromPdu(pdus[i] as ByteArray, "3gpp")
                        }
                    }
                    TelephonyManager.PHONE_TYPE_CDMA -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            message = SmsMessage.createFromPdu(pdus[i] as ByteArray,"3gpp2")
                        }
                    }
                }

                var senderNum = message?.displayOriginatingAddress
                var messageBody = message?.displayMessageBody

                Log.d("Number", message?.originatingAddress)
                Log.d("SMS", message?.messageBody)

                var smsIntent = Intent("otp")
                smsIntent.putExtra("message", messageBody)
                smsIntent.putExtra("sender", senderNum)

                LocalBroadcastManager.getInstance(context).sendBroadcast(smsIntent)
            }
        }
    }

}