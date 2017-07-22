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

import com.android.volley.VolleyError
import com.stfalcon.smsverifycatcher.OnSmsCatchListener
import com.stfalcon.smsverifycatcher.SmsVerifyCatcher

import org.json.JSONException
import org.json.JSONObject

import java.util.regex.Pattern

import android.widget.*
import com.mntechnique.otpmobileauth.R
import com.mntechnique.otpmobileauth.server.OTPMobileRESTAPI
import com.mntechnique.otpmobileauth.server.OTPMobileServerCallback
import android.Manifest.permission.READ_SMS
import android.os.Build

/**
 * A login screen that offers login via mobile/otp.
 */
class AuthenticatorActivity : AccountAuthenticatorActivity() {

    private val REQ_SIGNUP = 1

    private val TAG = this.javaClass.simpleName
    private lateinit var mAccountManager : AccountManager
    private var mAuthTokenType: String? = null

    val ARG_ACCOUNT_TYPE = "ACCOUNT_TYPE"
    val ARG_AUTH_TYPE = "AUTH_TYPE"
    val ARG_ACCOUNT_NAME = "ACCOUNT_NAME"

    // UI references.
    private var mobileInput: EditText? = null
    private var otpInput: EditText? = null
    private var submitOtp: Button? = null
    private var signIn: Button? = null

    internal var allowMultipleAccounts: Int = 1

    //SMS OTP Verify
    internal lateinit var smsVerifyCatcher: SmsVerifyCatcher

    internal lateinit var serverUrl: String
    internal lateinit var getOTPEndpoint: String
    internal lateinit var authOTPEndpoint: String
    internal lateinit var openIDEndpoint: String

    internal lateinit var llProgress: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authenticator)
        llProgress = findViewById<LinearLayout>(R.id.llProgress)
        smsVerifyCatcher = SmsVerifyCatcher(this, OnSmsCatchListener<String> { message ->
            val code = parseCode(message)//Parse verification code
            //then you can send verification code to server
            otpInput?.setText(code)//set code in edit text
            otpInput?.isEnabled = true
            authOtp()
        })

        //set phone number filter if needed
        smsVerifyCatcher.setPhoneNumberFilter(resources.getString(R.string.otpSenderNumber))
        //smsVerifyCatcher.setFilter("regexp");

        mAccountManager = AccountManager.get(baseContext)
        Log.d("IntentExtras", intent.extras.toString())

        mAuthTokenType = intent.getStringExtra(ARG_AUTH_TYPE)

        if (mAuthTokenType == null) {
            mAuthTokenType = AccountGeneral.AUTHTOKEN_TYPE_FULL_ACCESS
        }

        Log.d("Intent:ACCOUNT_NAME", intent.extras.toString())
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

    private fun initOAuth20Service() : AccountGeneral{
        val accountGeneral = AccountGeneral(
                resources.getString(R.string.oauth2Scope),
                resources.getString(R.string.clientId),
                resources.getString(R.string.clientSecret),
                resources.getString(R.string.serverURL),
                resources.getString(R.string.redirectURI),
                resources.getString(R.string.authEndpoint),
                resources.getString(R.string.tokenEndpoint)
        )
        return accountGeneral
    }

    private fun singleAccountSnackbar() {
        Snackbar.make(findViewById<View>(android.R.id.content), "You can only add one account", Snackbar.LENGTH_INDEFINITE)
                .setAction("Close") { finish() }.show()
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
                if (grantResults.isNotEmpty() && grantResults.get(0) == PackageManager.PERMISSION_GRANTED) {
                    // Handle action of grant
                    smsVerifyCatcher.onStart()
                    // For receiving otp sms
                    smsVerifyCatcher.onRequestPermissionsResult(requestCode, permissions, grantResults)

                    wireUpUI()

                } else {
                    showPermissionSnackbarSMS()
                }
            }
        }

    }

    override fun onStart() {
        super.onStart()
        smsVerifyCatcher.onStart()
    }

    override fun onStop() {
        super.onStop()
        smsVerifyCatcher.onStop()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {

        // The sign up activity returned that the user has successfully created an account
        if (requestCode == REQ_SIGNUP && resultCode == RESULT_OK) {
            finishLogin(data)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun wireUpUI() {

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

        mobileInput = findViewById<EditText>(R.id.mobile)
        otpInput = findViewById<EditText>(R.id.otp)

        signIn = findViewById<Button>(R.id.login)
        submitOtp = findViewById<Button>(R.id.submitOtp)

        signIn!!.setOnClickListener { signIn() }

        submitOtp!!.setOnClickListener { authOtp() }

    }

    fun signIn() {
        val server = OTPMobileRESTAPI()
        server.getOTP(mobileInput!!.text.toString().replace(" ", ""),
                serverUrl, getOTPEndpoint, object : OTPMobileServerCallback {
            override fun onSuccessString(result: String) {
                Log.d("OTPSuccess", result)

                try {
                    val resultJSON = JSONObject(result)
                    val otpMessage = resultJSON.getString("message")
                    otpInput!!.setText(otpMessage.substring(otpMessage.lastIndexOf(":") + 1))
                } catch (e: JSONException) {
                    e.printStackTrace()
                }

                mobileInput!!.isEnabled = false
                signIn!!.isEnabled = false
                signIn!!.visibility = View.GONE
                otpInput!!.visibility = View.VISIBLE
                submitOtp!!.visibility = View.VISIBLE
            }

            override fun onErrorString(error: VolleyError) {
                Toast.makeText(baseContext, "Something went wrong, please check mobile number", Toast.LENGTH_LONG).show()

                mobileInput!!.isEnabled = true
                signIn!!.isEnabled = true
                otpInput!!.visibility = View.GONE
                otpInput!!.setText("")
                submitOtp!!.visibility = View.GONE
            }
        })
    }

    fun authOtp() {
        val server = OTPMobileRESTAPI()

        val accountType = intent.getStringExtra(ARG_ACCOUNT_TYPE)

        server.authOtp(otpInput!!.text.toString().replace(" ", ""),
                mobileInput!!.text.toString(),
                resources.getString(R.string.clientId),
                serverUrl,
                authOTPEndpoint,
                object : OTPMobileServerCallback {

                    override fun onSuccessString(result: String) {
                        Log.d("OTPAuthenticated", result)

                        submitOtp!!.isEnabled = false
                        otpInput!!.isEnabled = false

                        object : AsyncTask<String, Void, Intent>() {
                            override fun doInBackground(vararg params: String): Intent {
                                Log.d("frappe", TAG + "> Started authenticating")

                                val authtoken: String? = null
                                val data = Bundle()
                                try {
                                    val bearerToken = JSONObject(result)
                                    Log.d("access_token", bearerToken.getString("access_token"))
                                    val openIDProfile = AccountGeneral.sServerAuthenticate.getOpenIDProfile(bearerToken.getString("access_token"),
                                            serverUrl, openIDEndpoint)

                                    if (openIDProfile.has("email") && intent.hasExtra(ARG_ACCOUNT_NAME)) {
                                        if (openIDProfile.getString("email") != intent.getStringExtra(ARG_ACCOUNT_NAME)) {
                                            if (allowMultipleAccounts === 0 && accountExists()) {
                                                throw AccountsException("Not allowed to add new account")
                                            }
                                        }
                                    }

                                    //JSONObject id_token = JWTUtils.decoded(bearerToken.get("id_token").toString());
                                    data.putString(AccountManager.KEY_ACCOUNT_NAME, openIDProfile.get("email").toString())
                                    data.putString(AccountManager.KEY_ACCOUNT_TYPE, accountType)
                                    data.putString(AccountManager.KEY_AUTHTOKEN, result)
                                    data.putString(PARAM_USER_PASS, resources.getString(R.string.clientSecret))
                                    data.putString("KEY_OPENID_PROFILE", openIDProfile.toString())
                                } catch (e: Exception) {
                                    Log.d("Error is ", e.toString())
                                    data.putString(KEY_ERROR_MESSAGE, e.message)
                                }

                                val res = Intent()
                                res.putExtras(data)
                                return res
                            }

                            override fun onPostExecute(intent: Intent) {
                                if (intent.hasExtra(KEY_ERROR_MESSAGE)) {
                                    Toast.makeText(baseContext, intent.getStringExtra(KEY_ERROR_MESSAGE), Toast.LENGTH_SHORT).show()
                                } else {
                                    finishLogin(intent)
                                }
                            }
                        }.execute()
                    }

                    override fun onErrorString(error: VolleyError) {
                        Toast.makeText(baseContext, "Please check OTP or tap back to try with different number", Toast.LENGTH_LONG).show()
                        signIn!!.isEnabled = true
                        submitOtp!!.isEnabled = true
                        otpInput!!.isEnabled = true
                    }

                })
    }

    private fun finishLogin(intent: Intent) {
        Log.d("frappe", TAG + "> finishLogin")

        val accountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
        val accountPassword = intent.getStringExtra(PARAM_USER_PASS)
        var authtoken: String? = null
        val account = Account(accountName, intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE))

        mAccountManager = AccountManager.get(baseContext)

        if (getIntent().getBooleanExtra(ARG_IS_ADDING_NEW_ACCOUNT, true)) {
            Log.d("frappe", TAG + "> finishLogin > addAccountExplicitly")
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

            var bearerToken: JSONObject
            try {
                bearerToken = JSONObject(authtoken)
                val tokenExpiryTime = System.currentTimeMillis() / 1000 + java.lang.Long.parseLong(resources.getString(R.string.expiresIn))
                mAccountManager.setUserData(account, "authToken", authtoken)
                mAccountManager.setUserData(account, "refreshToken", bearerToken.getString("refresh_token"))
                mAccountManager.setUserData(account, "accessToken", bearerToken.getString("access_token"))
                mAccountManager.setUserData(account, "serverURL", resources.getString(R.string.serverURL))
                mAccountManager.setUserData(account, "redirectURI", resources.getString(R.string.redirectURI))
                mAccountManager.setUserData(account, "clientId", resources.getString(R.string.clientId))
                mAccountManager.setUserData(account, "clientSecret", resources.getString(R.string.clientSecret))
                mAccountManager.setUserData(account, "oauth2Scope", resources.getString(R.string.oauth2Scope))
                mAccountManager.setUserData(account, "authEndpoint", resources.getString(R.string.authEndpoint))
                mAccountManager.setUserData(account, "tokenEndpoint", resources.getString(R.string.tokenEndpoint))
                mAccountManager.setUserData(account, "openIDEndpoint", resources.getString(R.string.openIDEndpoint))
                mAccountManager.setUserData(account, "expiresIn", resources.getString(R.string.expiresIn))
                mAccountManager.setUserData(account, "tokenExpiryTime", tokenExpiryTime.toString())
                mAccountManager.setUserData(account, "openIDProfile", intent.getStringExtra("KEY_OPENID_PROFILE"))
            } catch (e: JSONException) {
                e.printStackTrace()
            }

            Log.d("frappe", TAG + "> finishLogin > setPassword new " + authtoken)
        } else {
            Log.d("frappe", TAG + "> finishLogin > setPassword no new login" + authtoken)
            mAccountManager.setPassword(account, accountPassword)
        }

        setAccountAuthenticatorResult(intent.extras)
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun parseCode(message: String): String {
        //val p = Pattern.compile("\\b\\d{" + resources.getString(R.string.otpLen) + "}\\b")
        val p = Pattern.compile("([A-Z0-9]){" + resources.getString(R.string.otpLen) + "}")
        val m = p.matcher(message)
        var code = ""
        while (m.find()) {
            //code = m.group(0)
            code = m.group()
        }
        return code
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

    internal fun accountExists():Boolean {
        val accounts = mAccountManager.getAccountsByType(intent.getStringExtra(ARG_ACCOUNT_TYPE))
        if (accounts.isNotEmpty()){
            return true
        }
        return false
    }

    internal fun mayRequestSMS(): Boolean {

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
            .setAction("OK", object: View.OnClickListener {
                override fun onClick(v: View) {
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                        requestPermissions(arrayOf(READ_SMS), REQUEST_READ_SMS);
                    }
                }
            }).show();
    }
}

