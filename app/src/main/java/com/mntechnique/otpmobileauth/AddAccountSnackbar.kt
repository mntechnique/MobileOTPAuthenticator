package com.mntechnique.otpmobileauth

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.View

import com.mntechnique.otpmobileauth.auth.AuthenticatorActivity

/**
 * Created by revant on 28/7/17.
 */

class AddAccountSnackbar : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showAddAccountSnackBar()
    }

    public override fun onResume() {
        super.onResume()
        showAddAccountSnackBar()
    }

    fun showAddAccountSnackBar() {
        val packageName = resources.getString(R.string.package_name)
        val mAccountManager = AccountManager.get(this)
        val accounts = mAccountManager.getAccountsByType(packageName)
        if (accounts.size == 0) {
            Snackbar.make(findViewById(android.R.id.content), R.string.please_add_account, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok) {
                        val intent = Intent(this@AddAccountSnackbar, AuthenticatorActivity::class.java)
                        intent.putExtra(AuthenticatorActivity.ARG_ACCOUNT_TYPE, packageName)
                        startActivity(intent)
                    }.show()
        }
    }
}
