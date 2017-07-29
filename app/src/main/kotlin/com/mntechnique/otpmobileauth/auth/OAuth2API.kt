package com.mntechnique.otpmobileauth.auth

import com.github.scribejava.apis.google.GoogleJsonTokenExtractor
import com.github.scribejava.core.builder.api.DefaultApi20
import com.github.scribejava.core.extractors.TokenExtractor
import com.github.scribejava.core.model.OAuth2AccessToken

/**
 * Created by revant on 15/7/17.
 */

class OAuth2API protected constructor(val serverURL: String, authEndpoint: String, tokenEndpoint: String) : DefaultApi20() {
    private val accessTokenEndpoint: String
    private val authorizationBaseUrl: String

    init {
        this.authorizationBaseUrl = serverURL + authEndpoint
        this.accessTokenEndpoint = serverURL + tokenEndpoint
    }

    override fun getAccessTokenEndpoint(): String {
        return accessTokenEndpoint
    }

    override fun getAuthorizationBaseUrl(): String {
        return authorizationBaseUrl
    }

    override fun getAccessTokenExtractor(): TokenExtractor<OAuth2AccessToken> {
        return GoogleJsonTokenExtractor.instance()
    }

    companion object {

        fun instance(serverUrl: String, authEndpoint: String, tokenEndpoint: String): OAuth2API {
            return OAuth2API(serverUrl, authEndpoint, tokenEndpoint)
        }
    }
}
