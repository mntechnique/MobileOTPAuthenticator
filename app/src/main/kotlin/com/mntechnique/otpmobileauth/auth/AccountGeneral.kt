package com.mntechnique.otpmobileauth.auth

import com.github.scribejava.apis.FrappeApi
import com.github.scribejava.core.builder.ServiceBuilder
import com.github.scribejava.core.oauth.OAuth20Service

class AccountGeneral(oauth2Scope: String, clientId: String, clientSecret: String, serverURL: String, redirectURI: String) {

    init {
        oauth20Service = ServiceBuilder(clientId)
                .apiSecret(clientSecret)
                .scope(oauth2Scope)
                .callback(redirectURI)
                .build(FrappeApi.instance(serverURL))
    }

    companion object {
        /**
         * Auth token types
         */
        var AUTHTOKEN_TYPE_READ_ONLY = "Read only"
        var AUTHTOKEN_TYPE_READ_ONLY_LABEL = "Read only access to an account"
        var AUTHTOKEN_TYPE_FULL_ACCESS = "Full access"
        var AUTHTOKEN_TYPE_FULL_ACCESS_LABEL = "Full access to an account"
        val sServerAuthenticate: ServerAuthenticate = OTPServerAuthenticate()

        var oauth20Service: OAuth20Service? = null
    }
}
