package com.tuvin.pixelautomations.setup

import android.content.Context

/**
 * Addresses and secrets for the service actions (Home Assistant). Kept in the app's private storage,
 * separate from automations, so they are never part of an export and never in the repo.
 */
class Connections(ctx: Context) {
    private val prefs = ctx.applicationContext.getSharedPreferences("connections", Context.MODE_PRIVATE)

    var homeAssistantUrl: String
        get() = prefs.getString("ha_url", "")!!
        set(v) = prefs.edit().putString("ha_url", v.trim().trimEnd('/')).apply()

    var homeAssistantToken: String
        get() = prefs.getString("ha_token", "")!!
        set(v) = prefs.edit().putString("ha_token", v.trim()).apply()

    val homeAssistantReady get() = homeAssistantUrl.isNotBlank() && homeAssistantToken.isNotBlank()
}
