package com.mntechnique.otpmobileauth.auth

/**
 * Created by revant on 17/7/17.
 */

interface AuthReqCallback {
    fun onSuccessResponse(result: String)
    fun onErrorResponse(error: String)
}
