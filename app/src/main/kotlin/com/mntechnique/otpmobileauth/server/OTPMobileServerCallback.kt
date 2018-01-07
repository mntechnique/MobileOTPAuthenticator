package com.mntechnique.otpmobileauth.server

import com.android.volley.VolleyError

/**
 * Created by revant on 27/5/17.
 */

interface OTPMobileServerCallback {

    fun onSuccessString(result: String)

    fun onErrorString(error: VolleyError)
}
