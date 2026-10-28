package com.tuvin.pixelautomations

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Base64
import android.util.Log
import com.tuvin.pixelautomations.engine.Engine
import com.tuvin.pixelautomations.engine.TAG
import com.tuvin.pixelautomations.engine.launchAsync
import com.tuvin.pixelautomations.model.AutomationJson
import com.tuvin.pixelautomations.triggers.TimeOfDayTrigger

/**
 * Debug builds only: load test automations from the PC.
 *   adb shell am broadcast -n com.tuvin.pixelautomations/.DebugImportReceiver --es b64 <base64 JSON list> [--ez replace true]
 */
class DebugImportReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val json = String(Base64.decode(intent.getStringExtra("b64") ?: return, Base64.DEFAULT))
        val replace = intent.getBooleanExtra("replace", false)
        launchAsync {
            val repo = ctx.app.repo
            if (replace) repo.all().forEach {
                TimeOfDayTrigger.cancel(ctx, it.id)
                repo.delete(it.id)
            }
            AutomationJson.decodeAll(json).forEach { repo.save(it.copy(id = 0)) }
            Log.i(TAG, "debug import: ${repo.all().size} automations")
            Engine.sync(ctx)
        }
    }
}
