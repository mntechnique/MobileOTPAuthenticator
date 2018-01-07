package com.mntechnique.otpmobileauth.auth

import android.util.Log

import com.github.scribejava.core.model.OAuth2AccessToken
import com.github.scribejava.core.model.OAuthRequest
import com.github.scribejava.core.model.Verb
import org.json.JSONException

import org.json.JSONObject

import java.util.HashMap

/**
 * User: revant
 * Date: 3/27/13
 * Time: 3:30 AM
 */
class OTPServerAuthenticate : ServerAuthenticate {

    override fun userSignIn(
            authCode: JSONObject,
            CLIENT_ID: String,
            REDIRECT_URI: String
    ): String {
        var oAuth2AccessToken: OAuth2AccessToken? = null
        val params = HashMap<String, String>()
        params.put("client_id", CLIENT_ID)
        params.put("redirect_uri", REDIRECT_URI)
        if (authCode.get("type") === "refresh") {
            val refresh_token = authCode.get("refresh_token") as String
            params.put("grant_type", "refresh_token")
            params.put("refresh_token", refresh_token)
            oAuth2AccessToken = AccountGeneral.oauth20Service!!.refreshAccessToken(refresh_token)
        } else if (authCode.get("type") === "code") {
            val code = authCode.get("code") as String
            params.put("grant_type", "authorization_code")
            params.put("code", code)
            oAuth2AccessToken = AccountGeneral.oauth20Service!!.getAccessToken(code)
        }
        return oAuth2AccessToken!!.rawResponse
    }

    override fun getOpenIDProfile(
            accessToken: String,
            serverURL: String,
            openIDEndpoint: String
    ): JSONObject {
        val oAuth2AccessToken = OAuth2AccessToken(accessToken)
        val request = OAuthRequest(Verb.GET, serverURL + openIDEndpoint)
        AccountGeneral.oauth20Service!!.signRequest(oAuth2AccessToken, request)
        val response = AccountGeneral.oauth20Service!!.execute(request)
        var openIDProfile: JSONObject
        try {
            openIDProfile = JSONObject(response!!.body)
        } catch (e: JSONException) {
            openIDProfile = JSONObject()
        }

        Log.d("OpenID JSON", openIDProfile.toString())
        return openIDProfile
    }
}
