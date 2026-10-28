package com.tuvin.pixelautomations.actions

import android.content.Intent
import android.net.Uri
import android.os.SystemClock

/** Deep links into WhatsApp / Telegram to send text (a Maps link) to someone. */
object Messaging {
    const val WHATSAPP = "com.whatsapp"
    const val TELEGRAM = "org.telegram.messenger"

    /** Digits only, no leading +, for wa.me (e.g. "+972 50-123" -> "97250123"). */
    fun digits(number: String) = number.filter { it.isDigit() }

    /** Opens WhatsApp at [number]'s chat with [text] ready to send. */
    fun whatsappIntent(number: String, text: String): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${digits(number)}?text=${Uri.encode(text)}"))
            .setPackage(WHATSAPP)

    /** Opens Telegram's "share to" picker with [text]. Telegram can't reliably pre-target a phone contact. */
    fun telegramShareIntent(text: String): Intent =
        Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text).setPackage(TELEGRAM)
}

/**
 * A one-shot request for the accessibility service to tap "send" in [pkg] within the time window.
 * Set by the Send Location AUTO mode; read and cleared by [com.tuvin.pixelautomations.engine.AppWatchService].
 */
object AutoSend {
    @Volatile private var pkg: String? = null
    @Volatile private var expiresAt = 0L

    fun arm(target: String, windowMs: Long = 12_000) {
        pkg = target
        expiresAt = SystemClock.uptimeMillis() + windowMs
    }

    /** True if an un-expired request is armed for [forPkg]. */
    fun armedFor(forPkg: String): Boolean = pkg == forPkg && SystemClock.uptimeMillis() <= expiresAt

    fun clear() { pkg = null }
}
