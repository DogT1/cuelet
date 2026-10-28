package com.tuvin.pixelautomations.engine

import com.tuvin.pixelautomations.setup.t

import android.app.Activity
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import com.tuvin.pixelautomations.BuildConfig
import com.tuvin.pixelautomations.model.Trigger
import com.tuvin.pixelautomations.triggers.NfcTrigger

/**
 * iOS "NFC". Android hands tags to this invisible activity: blank/unknown tags via TECH_DISCOVERED, and tags
 * written by the editor ("pixelautomations://tag/…") via NDEF_DISCOVERED. It fires matching automations and closes.
 */
class NfcTagActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handle(intent)
        finish()
    }

    private fun handle(intent: Intent) {
        // Always identify by the tag's hardware ID from the NFC stack's Tag object, never by the URI, so another
        // app firing this exported activity with a made-up "pixelautomations://tag/…" link can't trigger anything.
        val tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        val id = tag?.id?.let(NfcTrigger::idOf)
            // Debug builds: lets the PC simulate a tap, since NFC can't be faked over adb.
            ?: intent.getStringExtra("debug_tag_id")?.takeIf { BuildConfig.DEBUG }
            ?: return
        Log.i(TAG, "nfc tag scanned")
        if (BuildConfig.DEBUG) Log.d(TAG, "nfc tag id=$id techs=${tag?.techList?.joinToString { it.substringAfterLast('.') }}")
        val ctx = applicationContext
        Engine.scope.launchCatching {
            Engine.onEvent(ctx, t("NFC tag")) { a -> (a.trigger as? Trigger.Nfc)?.let { NfcTrigger.matches(it, id) } == true }
        }
    }
}
