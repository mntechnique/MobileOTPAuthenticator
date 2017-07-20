package com.mntechnique.otpmobileauth.auth

import android.content.Context
import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import android.util.Log

import com.github.scribejava.core.builder.ServiceBuilder
import com.github.scribejava.core.model.OAuth2AccessToken
import com.github.scribejava.core.model.OAuthRequest
import com.github.scribejava.core.model.Response
import com.github.scribejava.core.oauth.OAuth20Service

import java.io.IOException
import java.util.concurrent.ExecutionException

/**
 * Created by revant on 17/7/17.
 */

class AuthRequest(internal var mContext: Context, oauth2Scope: String, clientId: String, clientSecret: String, serverURL: String,
                  redirectURI: String, authEndpoint: String, tokenEndpoint: String) {

    internal var oauth20Service: OAuth20Service

    init {
        this.oauth20Service = ServiceBuilder(clientId)
                .apiSecret(clientSecret)
                .scope(oauth2Scope)
                .callback(redirectURI)
                .build(OAuth2API.instance(serverURL, authEndpoint, tokenEndpoint))
    }

    fun makeRequest(accessToken: String, request: OAuthRequest, callback: AuthReqCallback) {
        val oAuth2AccessToken = OAuth2AccessToken(accessToken)
        val mHandler = Handler(Looper.getMainLooper())
        mHandler.post {
            object : AsyncTask<Void, Void, String>() {
                override fun doInBackground(vararg params: Void): String {
                    oauth20Service.signRequest(oAuth2AccessToken, request)
                    var response: Response? = null
                    var out: String = ""
                    try {
                        response = oauth20Service.execute(request)
                        out = response!!.body
                    } catch (e: InterruptedException) {
                        Log.e("OAuth2Authenticator", e.message, e)
                    } catch (e: ExecutionException) {
                        Log.e("OAuth2Authenticator", e.message, e)
                    } catch (e: IOException) {
                        Log.e("OAuth2Authenticator", e.message, e)
                    }

                    return out
                }

                override fun onPostExecute(result: String) {
                    callback.onSuccessResponse(result)
                }
            }.execute()
        }
    }
}
