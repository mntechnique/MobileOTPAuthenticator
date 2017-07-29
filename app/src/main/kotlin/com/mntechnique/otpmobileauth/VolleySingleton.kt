package com.mntechnique.otpmobileauth

import android.accounts.Account
import android.app.Application
import android.content.Context
import android.text.TextUtils
import android.util.Log

import com.android.volley.AuthFailureError
import com.android.volley.Network
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyLog
import com.android.volley.toolbox.BasicNetwork
import com.android.volley.toolbox.DiskBasedCache
import com.android.volley.toolbox.HttpStack
import com.android.volley.toolbox.HurlStack
import com.android.volley.toolbox.RequestFuture
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.mntechnique.otpmobileauth.server.OTPMobileServerCallback

import org.json.JSONException
import org.json.JSONObject

import java.io.UnsupportedEncodingException
import java.util.HashMap

class ApplicationController (context: Context) {
    internal lateinit var mInstance: ApplicationController
    internal var mCtx = context

    internal var bearerToken = JSONObject()
    internal lateinit var accounts: Array<Account>

    /**
     * Global request queue for Volley
     */
    private var mRequestQueue: RequestQueue? = null
    private var mSerialRequestQueue: RequestQueue? = null


    /**
     * @return The Volley Request queue, the queue will be created if it is null
     */
    // lazy initialize the request queue, the queue instance will be
    // created when it is accessed for the first time
    val requestQueue: RequestQueue?
        get() {
            if (mRequestQueue == null) {
                mRequestQueue = Volley.newRequestQueue(mCtx)
            }

            return mRequestQueue
        }

    val serialRequestQueue: RequestQueue?
        get() {
            if (mSerialRequestQueue == null) {
                mSerialRequestQueue = prepareSerialRequestQueue(mCtx)
                mSerialRequestQueue!!.start()
            }
            return mSerialRequestQueue
        }

    /**
     * Adds the specified request to the global queue, if tag is specified
     * then it is used else Default TAG is used.

     * @param req
     * *
     * @param tag
     */
    fun <T> addToRequestQueue(req: Request<T>, tag: String) {
        // set the default tag if tag is empty
        req.tag = if (TextUtils.isEmpty(tag)) TAG else tag

        VolleyLog.d("Adding request to queue: %s", req.url)
        requestQueue?.add(req)
    }

    /**
     * Adds the specified request to the global queue using the Default TAG.

     * @param req
     */
    fun <T> addToRequestQueue(req: Request<T>) {
        // set the default tag if tag is empty
        req.tag = TAG

        requestQueue?.add(req)
    }

    /**
     * Cancels all pending requests by the specified TAG, it is important
     * to specify a TAG so that the pending/ongoing requests can be cancelled.

     * @param tag
     */
    fun cancelPendingRequests(tag: Any) {
        if (mRequestQueue != null) {
            mRequestQueue!!.cancelAll(tag)
        }
    }

    // Frappe REST Client
    fun makeAPICall(verb: Int, endpointURL: String, data: JSONObject, callback: OTPMobileServerCallback, accessToken: String) {
        var endpointURL = endpointURL

        Log.d("Endpoint URL", endpointURL)

        endpointURL = endpointURL.replace("+", "%20")


        val future = RequestFuture.newFuture<String>()
        val req = object : StringRequest(verb, endpointURL, Response.Listener<String> { response -> callback.onSuccessString(response) }, Response.ErrorListener { error ->
            VolleyLog.DEBUG = true
            var errorMessage: String? = null
            try {
                errorMessage = String(error.networkResponse.data)
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            } catch (e: NullPointerException) {
                e.printStackTrace()
            }

            println(errorMessage)
            VolleyLog.e("Error: ", error.message)
        }) {
            override fun getBodyContentType(): String {
                return "application/x-www-form-urlencoded; charset=UTF-8"
            }

            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                //..add other headers
                var bToken = JSONObject()
                try {
                    bToken = JSONObject(accessToken)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }

                val params = HashMap<String, String>()
                //params.put("Content-Type", "application/x-www-form-urlencoded");
                try {
                    params.put("Authorization", "Bearer " + bToken.getString("access_token"))
                } catch (e: JSONException) {
                    e.printStackTrace()
                }

                return params
            }

            @Throws(AuthFailureError::class)
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                //..add other headers
                //params.put("Content-Type", "application/x-www-form-urlencoded");
                params.put("data", data.toString())
                return params
            }
        }

        // add the request object to the queue to be executed
        // getInstance().addToRequestQueue(req);
        instance?.serialRequestQueue?.add(req)
    }

    companion object {

        /**
         * Log or request TAG
         */
        val TAG = "VolleyPatterns"

        /**
         * A singleton instance of the application class for easy access in other places
         */
        /**
         * @return ApplicationController singleton instance
         */
        @get:Synchronized var instance: ApplicationController? = null
            private set

        internal var MAX_SERIAL_THREAD_POOL_SIZE = 1
        internal val MAX_CACHE_SIZE = 2 * 1024 * 1024 //2 MB

        private fun prepareSerialRequestQueue(context: Context): RequestQueue {
            val cache = DiskBasedCache(context.cacheDir, MAX_CACHE_SIZE)
            val network = network
            return RequestQueue(cache, network, MAX_SERIAL_THREAD_POOL_SIZE)
        }

        private val network: Network
            get() {
                val stack: HttpStack
                val userAgent = "volley/0"
                stack = HurlStack()
                return BasicNetwork(stack)
            }
    }


}

