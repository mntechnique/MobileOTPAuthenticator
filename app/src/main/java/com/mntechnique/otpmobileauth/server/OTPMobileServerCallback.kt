package com.mntechnique.otpmobileauth.server

import com.android.volley.VolleyError

/**
 * Created by gaurav on 27/5/17.
 */

interface OTPMobileServerCallback {
    //    public void onSuccessJSONObject(JSONObject result);
    //    public void onErrorJSONObject(VolleyError error);
    fun onSuccessString(result: String)

    fun onErrorString(error: VolleyError)
}
