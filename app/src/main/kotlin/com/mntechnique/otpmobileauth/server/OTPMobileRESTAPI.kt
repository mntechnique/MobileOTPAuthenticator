package com.mntechnique.otpmobileauth.server

import android.content.Context
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.VolleyLog
import com.android.volley.toolbox.StringRequest
import com.mntechnique.otpmobileauth.VolleySingleton
import java.util.*


/**
 * Created by revant on 27/5/17.
 */

class OTPMobileRESTAPI (context: Context) {
    internal var mCtx = context

    fun getOTP(mobileNo: String, clientID: String, serverUrl: String, getOTPEndpoint: String, callback: OTPMobileServerCallback) {


        val req = object : StringRequest(
                Request.Method.POST,
                serverUrl + getOTPEndpoint,
                Response.Listener<String> { response -> callback.onSuccessString(response) },
                Response.ErrorListener { error ->
                    callback.onErrorString(error)
                    VolleyLog.e("VolleyError: ", error.message)
                }
        ){
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params.put("mobile_no", mobileNo)
                params.put("client_id", clientID)
                return params
            }
        }

        // add the request object to the queue to be executed
        VolleySingleton(mCtx).addToRequestQueue(req)

    }

    fun authOtp(otp: String, mobileNo: String, clientId: String, serverURL: String,
                authOTPEndpoint: String, callback: OTPMobileServerCallback) {
        val req = object : StringRequest(Request.Method.POST, serverURL + authOTPEndpoint,
                Response.Listener<String> { response -> callback.onSuccessString(response) },
                Response.ErrorListener { error -> callback.onErrorString(error) }) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params.put("otp", otp)
                params.put("mobile_no", mobileNo)
                params.put("client_id", clientId)
                return params
            }
        }

        // add the request object to the queue to be executed
        VolleySingleton(mCtx).addToRequestQueue(req)

    }
}
