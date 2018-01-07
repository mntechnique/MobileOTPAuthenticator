package com.mntechnique.otpmobileauth.auth

import org.json.JSONObject

/**
 * User: Frappe
 * Date: 3/27/13
 * Time: 2:35 AM
 */
interface ServerAuthenticate {
    fun userSignIn(
            authMethod: JSONObject,
            CLIENT_ID: String,
            REDIRECT_URI: String
    ): String

    fun getOpenIDProfile(
            accessToken: String,
            serverURL: String,
            openIDEndpoint: String
    ): JSONObject
}
