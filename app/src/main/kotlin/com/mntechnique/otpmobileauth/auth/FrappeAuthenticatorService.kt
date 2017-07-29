package com.mntechnique.otpmobileauth.auth

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Created with IntelliJ IDEA.
 * User: Frappe
 * Date: 19/03/13
 * Time: 19:10
 */
class FrappeAuthenticatorService : Service() {
    override fun onBind(intent: Intent): IBinder? {

        val authenticator = FrappeAuthenticator(this)
        return authenticator.iBinder
    }
}
