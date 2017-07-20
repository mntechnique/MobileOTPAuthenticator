package com.mntechnique.otpmobileauth.server

import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.VolleyLog
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.mntechnique.otpmobileauth.ApplicationController

import org.json.JSONException
import org.json.JSONObject

import java.io.UnsupportedEncodingException
import java.util.HashMap


/**
 * Created by gaurav on 27/5/17.
 */

class OTPMobileRESTAPI {

    fun getOTP(mobileNo: String, serverUrl: String, getOTPEndpoint: String, callback: OTPMobileServerCallback) {
        val req = object : JsonObjectRequest(
                serverUrl + getOTPEndpoint + "?mobile_no=" + mobileNo, null,
                object : Response.Listener<JSONObject> {
                    override fun onResponse(response: JSONObject) {
                        callback.onSuccessString(response.toString())
                    }
                },
                object : Response.ErrorListener {
                    override fun onErrorResponse(error: VolleyError) {
                        callback.onErrorString(error)
                        System.out.println(error.javaClass.simpleName)
                        VolleyLog.e("Error: ", error.message)
                    }
                }) {

        }

        // add the request object to the queue to be executed
        ApplicationController.instance?.addToRequestQueue(req)

    }

    fun authOtp(otp: String, mobileNo: String, clientId: String, serverURL: String,
                authOTPEndpoint: String, callback: OTPMobileServerCallback) {
        val req = object : StringRequest(Request.Method.POST, serverURL + authOTPEndpoint,
                object : Response.Listener<String> {
                    override fun onResponse(response: String) {
                        var resp: JSONObject? = null
                        try {
                            resp = JSONObject(response)
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }

                        callback.onSuccessString(resp!!.toString())
                    }
                },
                object : Response.ErrorListener {
                    override fun onErrorResponse(error: VolleyError) {
                        callback.onErrorString(error)
                    }
                }) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params.put("otp", otp)
                params.put("mobile_no", mobileNo)
                params.put("client_id", clientId)
                return params
            }
        }

        // add the request object to the queue to be executed
        ApplicationController.instance?.addToRequestQueue(req)

    }
}
