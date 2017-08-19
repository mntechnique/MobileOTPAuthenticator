package com.mntechnique.otpmobileauth.auth

import android.content.BroadcastReceiver

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.util.Log

/**
 * Created by gaurav on 19/8/17.
 */

class IncomingSMS : BroadcastReceiver() {
    var sms = SmsManager.getDefault()

    override fun onReceive(context: Context, intent: Intent) {
        var bundle = intent.extras
        if (bundle != null) {

            var pdusObj:Array<Object> = bundle.get("pdus") as Array<Object>

            for (i in 0..pdusObj.size -1) {
                var currentMessage = SmsMessage.createFromPdu(pdusObj[i] as ByteArray)
                var phoneNumber = currentMessage.displayOriginatingAddress
                var senderNum = phoneNumber
                var message = currentMessage.displayMessageBody
                Log.d("Incoming Message", message)
                Log.d("Incoming Sender", senderNum)

                var smsIntent = Intent("otp")
                smsIntent.putExtra("message", message)
                smsIntent.putExtra("sender", senderNum)
                LocalBroadcastManager.getInstance(context).sendBroadcast(smsIntent)
            }
        }
    }

}
