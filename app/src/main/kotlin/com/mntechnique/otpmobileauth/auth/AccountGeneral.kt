package com.mntechnique.otpmobileauth.auth

import com.github.scribejava.core.builder.ServiceBuilder
import com.github.scribejava.core.oauth.OAuth20Service

class AccountGeneral internal constructor(oauth2Scope: String, clientId: String, clientSecret: String, serverURL: String,
                                          redirectURI: String, authEndpoint: String, tokenEndpoint: String) {

    init {
        oauth20Service = ServiceBuilder(clientId)
                .apiSecret(clientSecret)
                .scope(oauth2Scope)
                .callback(redirectURI)
                .build(OAuth2API.instance(serverURL, authEndpoint, tokenEndpoint))
    }

    companion object {

        /**
         * Auth token types
         */
        var AUTHTOKEN_TYPE_READ_ONLY = "Read only"
        var AUTHTOKEN_TYPE_READ_ONLY_LABEL = "Read only access to an account"
        var AUTHTOKEN_TYPE_FULL_ACCESS = "Full access"
        var AUTHTOKEN_TYPE_FULL_ACCESS_LABEL = "Full access to an account"
        val sServerAuthenticate: ServerAuthenticate = FrappeServerAuthenticate()

        var oauth20Service: OAuth20Service? = null
    }
}
