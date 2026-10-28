package com.tuvin.pixelautomations

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.tuvin.pixelautomations.engine.Engine
import com.tuvin.pixelautomations.engine.TAG
import com.tuvin.pixelautomations.engine.launchAsync

/**
 * Debug builds only: run an automation's actions now, by name.
 *   adb shell am broadcast -n com.tuvin.pixelautomations/.DebugRunReceiver --es name "Test flashlight"
 */
class DebugRunReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val name = intent.getStringExtra("name") ?: return
        launchAsync {
            val a = ctx.app.repo.all().firstOrNull { it.name == name }
            if (a == null) Log.w(TAG, "debug run: no automation named $name")
            else Engine.run(ctx, a, "Debug run", notify = false)
        }
    }
}
