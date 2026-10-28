package com.tuvin.pixelautomations.engine

import com.tuvin.pixelautomations.setup.t

import android.content.BroadcastReceiver
import android.content.Context
import android.util.Log
import com.tuvin.pixelautomations.actions.ActionRunner
import com.tuvin.pixelautomations.app
import com.tuvin.pixelautomations.model.Action
import com.tuvin.pixelautomations.model.Automation
import com.tuvin.pixelautomations.model.RunLogRow
import com.tuvin.pixelautomations.model.RunMode
import com.tuvin.pixelautomations.triggers.ArriveLeaveTrigger
import com.tuvin.pixelautomations.triggers.TimeOfDayTrigger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

const val TAG = "PixelAutomations"

/** What to do when a trigger fires, from the automation's iOS run options. */
object RunRules {
    sealed interface Decision {
        data class RunNow(val notify: Boolean) : Decision
        data object AskFirst : Decision
    }

    fun decide(a: Automation): Decision = when (a.runMode) {
        RunMode.IMMEDIATELY -> Decision.RunNow(a.notifyWhenRun)
        RunMode.AFTER_CONFIRMATION -> Decision.AskFirst
    }
}

object Engine {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** A trigger fired for one known automation (e.g. its alarm). */
    suspend fun onTriggered(ctx: Context, id: Long, why: String) {
        val a = ctx.app.repo.get(id) ?: return
        if (a.enabled) fire(ctx, a, why)
    }

    /**
     * A system event happened; fire every enabled automation whose trigger [matches] it. [sender] carries the
     * phone number that triggered an SMS, so a "Send SMS" action can reply to it.
     */
    suspend fun onEvent(ctx: Context, why: String, sender: String? = null, matches: (Automation) -> Boolean) {
        ctx.app.repo.all().filter { it.enabled && matches(it) }.forEach { fire(ctx, it, why, sender) }
    }

    private suspend fun fire(ctx: Context, a: Automation, why: String, sender: String? = null) {
        Log.i(TAG, "trigger: ${a.title} ($why)")
        when (val d = RunRules.decide(a)) {
            RunRules.Decision.AskFirst -> {
                Notifications.askToRun(ctx, a, why)
                log(ctx, a, why, t("Asked to run"))
            }
            is RunRules.Decision.RunNow -> run(ctx, a, why, d.notify, sender)
        }
    }

    suspend fun run(ctx: Context, a: Automation, why: String, notify: Boolean, sender: String? = null) =
        runFrom(ctx, a, 0, why, notify, sender = sender)

    /**
     * Runs [a]'s top-level actions from [start]. A Choose from Menu pauses the run: its notification's buttons
     * resume it through [MenuReceiver] with the chosen option's actions, then the actions after the menu.
     */
    suspend fun runFrom(
        ctx: Context, a: Automation, start: Int, why: String, notify: Boolean,
        prefix: List<Action> = emptyList(), sender: String? = null,
    ) {
        var ran = 0
        val result = try {
            prefix.forEach { ActionRunner.run(ctx, it, sender = sender); ran++ }
            var waiting: String? = null
            for (i in start until a.actions.size) {
                val action = a.actions[i]
                if (action is Action.ChooseFromMenu) {
                    Notifications.menu(ctx, a, i, action, PendingMenus.put(a, i))
                    waiting = t("Waiting for a choice in “%s”", action.prompt.ifBlank { t("Choose") })
                    break
                }
                ActionRunner.run(ctx, action, sender = sender)
                ran++
            }
            waiting ?: t("Ran %d action(s)", ran)
        } catch (e: Exception) {
            Log.e(TAG, "run failed: ${a.title}", e)
            t("Failed: %s", e.message ?: e.javaClass.simpleName)
        }
        log(ctx, a, why, result)
        Log.i(TAG, "ran ${a.title}: $result")
        if (notify) Notifications.ran(ctx, a, result)
    }

    /** Bring alarms and the background service in line with the saved automations. */
    fun sync(ctx: Context) {
        scope.launch {
            val all = ctx.app.repo.all()
            TimeOfDayTrigger.syncAlarms(ctx, all)
            ArriveLeaveTrigger.sync(ctx, all)
            AutomationService.update(ctx, needed = all.any { it.enabled })
        }
    }

    /** Record that a trigger fired but its condition wasn't met (e.g. Before I Commute away from home). */
    suspend fun logSkip(ctx: Context, a: Automation, why: String, result: String) = log(ctx, a, why, result)

    private suspend fun log(ctx: Context, a: Automation, why: String, result: String) {
        ctx.app.repo.addLog(
            RunLogRow(automationId = a.id, name = a.title, at = System.currentTimeMillis(), trigger = why, result = result)
        )
    }
}

/**
 * Menus waiting for a tap: token → (automation snapshot, index of the menu action). In memory only; the snapshot
 * also covers unsaved drafts run with Run Now. If Android kills the app meanwhile, the tap is logged as expired.
 */
object PendingMenus {
    private val pending = java.util.concurrent.ConcurrentHashMap<Int, Pair<Automation, Int>>()
    private val next = java.util.concurrent.atomic.AtomicInteger(1)

    fun put(a: Automation, index: Int): Int = next.getAndIncrement().also { pending[it] = a to index }
    fun take(token: Int): Pair<Automation, Int>? = pending.remove(token)
}

/** Launches [block], logging instead of crashing the service if it throws. */
fun CoroutineScope.launchCatching(block: suspend () -> Unit) = launch {
    try {
        block()
    } catch (e: Exception) {
        Log.e(TAG, "event handling failed", e)
    }
}

/** Runs [block] off the main thread while keeping the receiver alive until it finishes. */
fun BroadcastReceiver.launchAsync(block: suspend () -> Unit) {
    val pending = goAsync()
    Engine.scope.launch {
        try {
            block()
        } catch (e: Exception) {
            Log.e(TAG, "receiver failed", e)
        } finally {
            pending.finish()
        }
    }
}
