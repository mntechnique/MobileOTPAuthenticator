package com.mntechnique.otpmobileauth.auth

import android.accounts.Account
import android.accounts.AccountAuthenticatorActivity
import android.accounts.AccountManager
import android.accounts.AccountsException
import android.content.Intent
import android.content.pm.PackageManager
import android.support.design.widget.Snackbar
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup

import org.json.JSONObject

import java.util.regex.Pattern

import android.widget.*
import com.mntechnique.otpmobileauth.R
import com.mntechnique.otpmobileauth.server.OTPMobileRESTAPI
import com.mntechnique.otpmobileauth.server.OTPMobileServerCallback
import android.Manifest.permission.READ_SMS
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.os.Build
import android.support.v4.content.LocalBroadcastManager
import com.android.volley.VolleyError
import org.jetbrains.anko.toast
import org.json.JSONException
import java.io.IOException
import java.net.URL

/**
 * A login screen that offers login via mobile/otp.
 */

class AuthenticatorActivity : AccountAuthenticatorActivity() {

    val REQ_SIGNUP = 1

    val TAG = "AuthenticatorActivity"
    lateinit var mAccountManager : AccountManager
    var mAuthTokenType: String? = null

    val ARG_ACCOUNT_TYPE = "ACCOUNT_TYPE"
    val ARG_AUTH_TYPE = "AUTH_TYPE"
    val ARG_ACCOUNT_NAME = "ACCOUNT_NAME"

    // UI references.
    internal lateinit var llProgress: LinearLayout
    var mobileInput: EditText? = null
    var otpInput: EditText? = null
    var submitOtp: Button? = null
    var signIn: Button? = null

    internal var allowMultipleAccounts: Int = 1

    //SMS OTP Verify
    internal lateinit var serverUrl: String
    internal lateinit var getOTPEndpoint: String
    internal lateinit var authOTPEndpoint: String
    internal lateinit var openIDEndpoint: String
    var receiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authenticator)
        llProgress = findViewById(R.id.llProgress)

        //set phone number filter if needed
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M &&
                checkSelfPermission(READ_SMS) == PackageManager.PERMISSION_GRANTED){
            receiver = object: BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if(intent!!.action.equals("otp", ignoreCase = true)) {
                        val sender = intent.getStringExtra("sender")
                        val message = intent.getStringExtra("message")
                        if (sender.contains(context!!.resources.getString(R.string.otpSenderNumber))) {
                            //Parse verification code
                            val code = parseCode(message)

                            //set code in edit text
                            otpInput?.setText(code)

                            authOtp()
                        }
                    }
                }
            }
        }

        mAccountManager = AccountManager.get(baseContext)

        mAuthTokenType = intent.getStringExtra(ARG_AUTH_TYPE)

        if (mAuthTokenType == null) {
            mAuthTokenType = AccountGeneral.AUTHTOKEN_TYPE_FULL_ACCESS
        }

        val accounts = mAccountManager.getAccountsByType(intent.getStringExtra(ARG_ACCOUNT_TYPE))

        allowMultipleAccounts = Integer.parseInt(resources.getString(R.string.allowMultipleAccounts))

        var signInAgain: Boolean? = true

        if (intent.hasExtra(ARG_ACCOUNT_NAME)) {
            for (account in accounts) {
                if (account.name == intent.getStringExtra(ARG_ACCOUNT_NAME)) {
                    wireUpUI()
                    signInAgain = false
                }
            }
        }

        if (allowMultipleAccounts === 0 && accountExists() && signInAgain!!) {
            val progressBar = findViewById<ProgressBar>(R.id.login_progress)
            progressBar.visibility = View.GONE

            val llEditMobileOTP = findViewById<LinearLayout>(R.id.llEditMobileOTP)
            llEditMobileOTP.visibility = View.GONE

            val llEnterMobileOTP = findViewById<LinearLayout>(R.id.llEnterMobileOTP)
            llEnterMobileOTP.visibility = View.GONE

            singleAccountSnackbar()

        } else {
            // Init OAuth2 flow
            wireUpUI()
        }
    }

    override fun onPause() {
        super.onPause()
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M &&
                checkSelfPermission(READ_SMS) == PackageManager.PERMISSION_GRANTED){
            LocalBroadcastManager
                    .getInstance(this)
                    .unregisterReceiver(receiver)
        }
    }

    override fun onResume() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M &&
                checkSelfPermission(READ_SMS) == PackageManager.PERMISSION_GRANTED) {
                LocalBroadcastManager
                        .getInstance(this)
                        .registerReceiver(receiver, IntentFilter("otp"))
        }
        super.onResume()
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                                   grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Manual coded

        when (requestCode) {
            REQUEST_READ_SMS -> {
                // For SMS Read during login using OTP
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    wireUpUI()
                } else {
                    showPermissionSnackbarSMS()
                    wireUpUI()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        llProgress.visibility = View.GONE
        // The sign up activity returned that the user has successfully created an account
        if (requestCode == REQ_SIGNUP && resultCode == RESULT_OK) {
            finishLogin(data)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    fun initOAuth20Service(){
        AccountGeneral (
                resources.getString(R.string.oauth2Scope),
                resources.getString(R.string.clientId),
                resources.getString(R.string.clientSecret),
                resources.getString(R.string.serverURL),
                resources.getString(R.string.redirectURI)
        )
    }

    fun singleAccountSnackbar() {
        Snackbar.make(findViewById<View>(android.R.id.content), R.string.add_account_not_allowed, Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(R.string.close)) { finish() }.show()
    }

    fun wireUpUI() {

        if(!mayRequestSMS()){
            return
        }

        initOAuth20Service()

        llProgress.visibility = View.GONE

        openIDEndpoint = resources.getString(R.string.openIDEndpoint)
        serverUrl = resources.getString(R.string.serverURL)
        getOTPEndpoint = resources.getString(R.string.getOTPEndpoint)
        authOTPEndpoint = resources.getString(R.string.authOTPEndpoint)

        mAuthTokenType = intent.getStringExtra(ARG_AUTH_TYPE)
        if (mAuthTokenType == null) {
            mAuthTokenType = AccountGeneral.AUTHTOKEN_TYPE_FULL_ACCESS
        }

        mobileInput = findViewById(R.id.mobile)
        otpInput = findViewById(R.id.otp)

        signIn = findViewById(R.id.login)
        submitOtp = findViewById(R.id.submitOtp)

        signIn!!.setOnClickListener {
            val asyncTask = object : AsyncTask<Void, Void, Boolean>() {
                override fun doInBackground(vararg params: Void?): Boolean{
                    return checkConnection()
                }
                override fun onPostExecute(result: Boolean) {
                    if (result){
                        signIn()
                    } else {
                        toast(R.string.connection_error)
                    }
                }
            }

            asyncTask.execute()
        }

        submitOtp!!.setOnClickListener { authOtp() }

    }

    fun signIn() {

        //show progress
        llProgress.visibility = View.VISIBLE

        val server = OTPMobileRESTAPI(baseContext)
        server.getOTP(mobileInput!!.text.toString()
                .replace(" ", "")
                .replace("-", ""), resources.getString(R.string.clientId),
                serverUrl, getOTPEndpoint, object : OTPMobileServerCallback {
            override fun onSuccessString(result: String) {
                Log.d(TAG, result)
                var otp: String
                try {
                    otp = JSONObject(result).getString("message").split(":")[1]!!
                } catch (e:JSONException){
                    otp = "ERROR"
                }
                // hide progress
                llProgress.visibility = View.GONE

                otpInput!!.setText(otp)

                // disable mobile number input and generate otp button
                mobileInput!!.isEnabled = false
                signIn!!.isEnabled = false
                signIn!!.visibility = View.GONE

                // enable otp input and send otp button
                otpInput!!.visibility = View.VISIBLE
                submitOtp!!.visibility = View.VISIBLE
                submitOtp!!.visibility = View.VISIBLE
            }

            override fun onErrorString(error: VolleyError) {
                //show progress
                llProgress.visibility = View.GONE

                toast(getString(R.string.please_check_mobile))

                mobileInput!!.isEnabled = true
                signIn!!.isEnabled = true
                otpInput!!.visibility = View.GONE
                otpInput!!.setText("")
                submitOtp!!.visibility = View.GONE
            }
        })
    }

    fun authOtp() {
        val server = OTPMobileRESTAPI(baseContext)

        val accountType = intent.getStringExtra(ARG_ACCOUNT_TYPE)

        //show progress
        llProgress.visibility = View.VISIBLE

        server.authOtp(otpInput!!.text.toString().replace(" ", ""),
                mobileInput!!.text.toString(),
                resources.getString(R.string.clientId),
                serverUrl,
                authOTPEndpoint,
                object : OTPMobileServerCallback {

                    override fun onSuccessString(result: String) {

                        submitOtp!!.isEnabled = false
                        otpInput!!.isEnabled = false

                        val asyncTask = object : AsyncTask<String, Void, Intent>() {
                            override fun doInBackground(vararg params: String): Intent {
                                Log.d(TAG, "> Started authenticating")

                                val data = Bundle()
                                val bearerToken = JSONObject(result)
                                val openIDProfile = AccountGeneral.sServerAuthenticate.getOpenIDProfile(bearerToken.getString("access_token"),
                                        serverUrl, openIDEndpoint)

                                if (openIDProfile.has("email") && intent.hasExtra(ARG_ACCOUNT_NAME)) {
                                    if (openIDProfile.getString("email") != intent.getStringExtra(ARG_ACCOUNT_NAME)) {
                                        if (allowMultipleAccounts === 0 && accountExists()) {
                                            throw AccountsException(getString(R.string.add_account_not_allowed))
                                        }
                                    }
                                }

                                data.putString(AccountManager.KEY_ACCOUNT_NAME, openIDProfile.get("email").toString())
                                data.putString(AccountManager.KEY_ACCOUNT_TYPE, accountType)
                                data.putString(AccountManager.KEY_AUTHTOKEN, result)
                                data.putString(PARAM_USER_PASS, resources.getString(R.string.clientSecret))
                                data.putString("KEY_OPENID_PROFILE", openIDProfile.toString())

                                val res = Intent()
                                res.putExtras(data)
                                return res
                            }

                            override fun onPostExecute(intent: Intent) {

                                //show progress
                                llProgress.visibility = View.GONE

                                if (intent.hasExtra(KEY_ERROR_MESSAGE)) {
                                    Toast.makeText(baseContext, intent.getStringExtra(KEY_ERROR_MESSAGE), Toast.LENGTH_SHORT).show()
                                } else {
                                    finishLogin(intent)
                                }
                            }
                        }

                        asyncTask.execute()
                    }

                    override fun onErrorString(error: VolleyError) {
                        toast(getString(R.string.check_number_or_otp))
                        signIn!!.isEnabled = true
                        submitOtp!!.isEnabled = true
                        otpInput!!.isEnabled = true
                    }

                })
    }

    fun finishLogin(intent: Intent) {
        Log.d(TAG, "> finishLogin")

        val accountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
        val accountPassword = intent.getStringExtra(PARAM_USER_PASS)
        var authtoken: String? = null
        val account = Account(accountName, intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE))

        mAccountManager = AccountManager.get(baseContext)

        if (getIntent().getBooleanExtra(ARG_IS_ADDING_NEW_ACCOUNT, true)) {
            Log.d(TAG,"> finishLogin > addAccountExplicitly")
            authtoken = intent.getStringExtra(AccountManager.KEY_AUTHTOKEN)
            val authtokenType = mAuthTokenType

            // Creating the account on the device and setting the auth token we got
            // (Not setting the auth token will cause another call to the server to authenticate the user)
            mAccountManager.addAccountExplicitly(account, accountPassword, null)
            println(authtoken)

            mAccountManager.setAuthToken(account, authtokenType, authtoken)

            mAccountManager.setUserData(account, "authtoken", authtoken)
            setAccountAuthenticatorResult(getIntent().extras)
            setResult(RESULT_OK, getIntent())

            val bearerToken = JSONObject(authtoken)
            mAccountManager.setUserData(account, "authToken", authtoken)
            mAccountManager.setUserData(account, "refreshToken", bearerToken.getString("refresh_token"))
            mAccountManager.setUserData(account, "accessToken", bearerToken.getString("access_token"))
            mAccountManager.setUserData(account, "serverURL", resources.getString(R.string.serverURL))
            mAccountManager.setUserData(account, "redirectURI", resources.getString(R.string.redirectURI))
            mAccountManager.setUserData(account, "clientId", resources.getString(R.string.clientId))
            mAccountManager.setUserData(account, "clientSecret", resources.getString(R.string.clientSecret))
            mAccountManager.setUserData(account, "oauth2Scope", resources.getString(R.string.oauth2Scope))
            mAccountManager.setUserData(account, "openIDEndpoint", resources.getString(R.string.openIDEndpoint))
            mAccountManager.setUserData(account, "openIDProfile", intent.getStringExtra("KEY_OPENID_PROFILE"))
        } else {
            // No new login
            mAccountManager.setPassword(account, accountPassword)
        }

        setAccountAuthenticatorResult(intent.extras)
        setResult(RESULT_OK, intent)
        finish()
    }

    fun parseCode(message: String?): String {
        val p = Pattern.compile("\\b\\d{" + resources.getString(R.string.otpLen) + "}\\b")
        //val p = Pattern.compile("([A-Z0-9]){" + resources.getString(R.string.otpLen) + "}")
        val m = p.matcher(message)
        var code = ""
        while (m.find()) {
            //code = m.group(0)
            code = m.group()
        }
        return code
    }

    fun accountExists():Boolean {
        val accounts = mAccountManager.getAccountsByType(intent.getStringExtra(ARG_ACCOUNT_TYPE))
        if (accounts.isNotEmpty()){
            return true
        }
        return false
    }

    fun mayRequestSMS(): Boolean {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true
        }

        if (checkSelfPermission(READ_SMS) == PackageManager.PERMISSION_GRANTED) {
            return true
        }

        if (shouldShowRequestPermissionRationale(READ_SMS)) {
            showPermissionSnackbarSMS()
        } else {
            requestPermissions(arrayOf(READ_SMS), REQUEST_READ_SMS)
        }

        return false
    }

    fun showPermissionSnackbarSMS(){
        llProgress.visibility = View.VISIBLE
        Snackbar.make(findViewById<ViewGroup>(android.R.id.content), "App needs Read SMS permission", Snackbar.LENGTH_INDEFINITE)
                .setAction("OK") {
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                        requestPermissions(arrayOf(READ_SMS), REQUEST_READ_SMS)
                    }
                }.show()
    }

    fun checkConnection(): Boolean {
        val connectUrl = URL(serverUrl)
        val connection = connectUrl.openConnection()
        connection.connectTimeout = 10000
        try {
            connection.connect()
            return true
        } catch (e: IOException){
            return false
        }
    }

    companion object {

        val ARG_ACCOUNT_TYPE = "ACCOUNT_TYPE"
        val ARG_AUTH_TYPE = "AUTH_TYPE"
        val ARG_ACCOUNT_NAME = "ACCOUNT_NAME"
        val ARG_IS_ADDING_NEW_ACCOUNT = "IS_ADDING_ACCOUNT"

        val KEY_ERROR_MESSAGE = "ERR_MSG"

        val PARAM_USER_PASS = "USER_PASS"

        /**
         * Id to identity READ_CONTACTS permission request.
         */
        private val REQUEST_READ_SMS = 0
    }
}
