package com.mntechnique.otpmobileauth.auth

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.os.AsyncTask

import com.mntechnique.otpmobileauth.R

/**
 * Created by revant on 28/7/17.
 */

class RetrieveAuthTokenTask(private val context: Context, private val callback: AuthReqCallback) : AsyncTask<String, Void, Void>() {

    private var authToken: String? = null

    override fun doInBackground(vararg urls: String): Void? {
        var accounts: Array<Account>? = null
        val am = AccountManager.get(context)
        accounts = am.getAccountsByType(context.resources.getString(R.string.package_name))
        if (accounts!!.size == 1) {
            val account = accounts[0]
            am.getAuthToken(account, AccountGeneral.AUTHTOKEN_TYPE_FULL_ACCESS, null, true, { future ->
                try {
                    val bundle = future.result
                    authToken = bundle.getString(AccountManager.KEY_AUTHTOKEN)
                    callback.onSuccessResponse(authToken!!)
                    am.invalidateAuthToken(account.type, authToken)
                } catch (e: Exception) {
                    callback.onErrorResponse(e.toString())
                }
            }, null)
        }
        return null
    }
}
