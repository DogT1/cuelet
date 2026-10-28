package com.tuvin.pixelautomations.engine

import com.tuvin.pixelautomations.setup.t

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import com.tuvin.pixelautomations.BuildConfig
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.tuvin.pixelautomations.actions.AutoSend
import com.tuvin.pixelautomations.actions.Messaging
import com.tuvin.pixelautomations.model.Trigger
import com.tuvin.pixelautomations.triggers.AppTracker
import com.tuvin.pixelautomations.triggers.AppTrigger

/**
 * iOS "App" trigger. Receives only window-change events (which app is in front); it can't read screen content
 * (canRetrieveWindowContent is false in res/xml/app_watch_service.xml).
 */
class AppWatchService : AccessibilityService() {

    private val tracker = AppTracker()
    private var launchable: Set<String> = emptySet()
    private var launchableAt = 0L

    override fun onServiceConnected() {
        instance = this
        Log.i(TAG, "app watcher connected")
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    private val handler = Handler(Looper.getMainLooper())

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        // Send Location AUTO mode: the chat just came to the front — tap its send button as it finishes loading.
        if (AutoSend.armedFor(pkg)) scheduleAutoSend(pkg)
        val cls = event.className?.toString()
        // Only real app screens count. Overlays (e.g. "dynamic island" apps, chat heads) also send window
        // changes, with a View class name instead of an Activity; counting them made apps look closed.
        val isScreen = cls != null && isActivity(pkg, cls)
        if (BuildConfig.DEBUG) Log.d(TAG, "window pkg=$pkg class=$cls activity=$isScreen")
        if (!isScreen) return
        tracker.onWindow(pkg, ::isApp).forEach { e ->
            val opened = e is AppTracker.Event.Opened
            val ctx = applicationContext
            val name = appName(ctx, e.pkg)
            Engine.scope.launchCatching {
                Engine.onEvent(ctx, t(if (opened) "Opened %s" else "Closed %s", name)) { a ->
                    val t = a.trigger
                    t is Trigger.App && AppTrigger.matches(t, e)
                }
            }
        }
    }

    override fun onInterrupt() {}

    /** Try to tap "send" a few times as the chat UI settles, then give up (the window is short-lived anyway). */
    private fun scheduleAutoSend(pkg: String) {
        for (delay in longArrayOf(500, 1200, 2200)) {
            handler.postDelayed({ if (AutoSend.armedFor(pkg)) tryAutoSend(pkg) }, delay)
        }
    }

    private fun tryAutoSend(pkg: String) {
        val root = rootInActiveWindow ?: return
        val send = findSendButton(root, pkg) ?: return
        val node = if (send.isClickable) send else send.parentClickable()
        if (node?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) {
            AutoSend.clear()
            Log.i(TAG, "auto-send: tapped send in $pkg")
        }
    }

    /** WhatsApp exposes a stable view id; Telegram and others fall back to a node described as "send". */
    private fun findSendButton(root: AccessibilityNodeInfo, pkg: String): AccessibilityNodeInfo? {
        if (pkg == Messaging.WHATSAPP) {
            root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/send").firstOrNull()?.let { return it }
        }
        return firstMatch(root) { n ->
            n.viewIdResourceName?.contains("send", ignoreCase = true) == true ||
                n.contentDescription?.toString()?.let(::looksLikeSend) == true
        }
    }

    private fun looksLikeSend(text: String) =
        listOf("send", "שלח", "отправ", "إرسال").any { text.contains(it, ignoreCase = true) }

    private fun firstMatch(node: AccessibilityNodeInfo?, pred: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        if (node == null) return null
        if (pred(node)) return node
        for (i in 0 until node.childCount) {
            firstMatch(node.getChild(i), pred)?.let { return it }
        }
        return null
    }

    private fun AccessibilityNodeInfo.parentClickable(): AccessibilityNodeInfo? {
        var n: AccessibilityNodeInfo? = this
        while (n != null && !n.isClickable) n = n.parent
        return n
    }

    private val activityCache = HashMap<String, Boolean>()

    /** Whether [cls] is an Activity of [pkg] (cached; window events repeat the same few classes). */
    private fun isActivity(pkg: String, cls: String): Boolean = activityCache.getOrPut("$pkg/$cls") {
        runCatching {
            packageManager.getActivityInfo(ComponentName(pkg, cls), PackageManager.ComponentInfoFlags.of(0))
        }.isSuccess
    }

    /** Launchable apps plus home screens; refreshed every 10 minutes so new installs count. */
    private fun isApp(pkg: String): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (launchable.isEmpty() || now - launchableAt > 10 * 60_000) {
            launchable = launchablePackages(this)
            launchableAt = now
        }
        return pkg in launchable
    }

    companion object {
        /** The running service, for actions that perform global accessibility actions (Lock, Screenshot). */
        @Volatile var instance: AppWatchService? = null
            private set

        fun globalAction(action: Int): Boolean = instance?.performGlobalAction(action) ?: false

        fun launchablePackages(ctx: Context): Set<String> {
            val pm = ctx.packageManager
            val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            return (pm.queryIntentActivities(launcher, PackageManager.ResolveInfoFlags.of(0)) +
                pm.queryIntentActivities(home, PackageManager.ResolveInfoFlags.of(0)))
                .map { it.activityInfo.packageName }
                .toSet()
        }

        fun appName(ctx: Context, pkg: String): String = runCatching {
            val pm = ctx.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))).toString()
        }.getOrDefault(pkg)
    }
}
