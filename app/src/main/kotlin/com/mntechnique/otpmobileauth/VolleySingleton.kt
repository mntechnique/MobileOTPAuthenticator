package com.mntechnique.otpmobileauth

import android.content.Context
import android.text.TextUtils
import com.android.volley.Network
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.VolleyLog
import com.android.volley.toolbox.BasicNetwork
import com.android.volley.toolbox.DiskBasedCache
import com.android.volley.toolbox.HttpStack
import com.android.volley.toolbox.HurlStack
import com.android.volley.toolbox.Volley

class VolleySingleton(context: Context) {
    internal var mCtx = context

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

    companion object {

        /**
         * Log or request TAG
         */
        val TAG = "VolleyPatterns"

        /**
         * A singleton instance of the application class for easy access in other places
         */
        /**
         * @return VolleySingleton singleton instance
         */
        @get:Synchronized var instance: VolleySingleton? = null
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

