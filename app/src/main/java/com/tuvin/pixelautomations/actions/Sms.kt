package com.tuvin.pixelautomations.actions

import android.content.Context
import android.telephony.SmsManager

/** Sending for the Send SMS action. */
object Sms {
    /** Send [body] to [to], splitting into parts if it's long. Needs the SEND_SMS permission. */
    fun send(ctx: Context, to: String, body: String) {
        val manager = ctx.getSystemService(SmsManager::class.java)
        val parts = manager.divideMessage(body)
        if (parts.size <= 1) {
            manager.sendTextMessage(to, null, body, null, null)
        } else {
            manager.sendMultipartTextMessage(to, null, parts, null, null)
        }
    }
}
