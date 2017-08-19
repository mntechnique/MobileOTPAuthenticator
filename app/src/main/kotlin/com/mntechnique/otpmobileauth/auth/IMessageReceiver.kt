package com.mntechnique.otpmobileauth.auth

/**
 * Created by gaurav on 19/8/17.
 */
interface IMessageReceiver {
    fun onMessage(from: String, text: String)
}