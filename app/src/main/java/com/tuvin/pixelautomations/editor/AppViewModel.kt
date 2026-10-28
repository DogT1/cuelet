package com.tuvin.pixelautomations.editor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tuvin.pixelautomations.app
import com.tuvin.pixelautomations.engine.Engine
import com.tuvin.pixelautomations.model.Action
import com.tuvin.pixelautomations.model.Automation
import com.tuvin.pixelautomations.model.AutomationJson
import com.tuvin.pixelautomations.model.MenuOption


import com.tuvin.pixelautomations.model.RunLogRow
import com.tuvin.pixelautomations.triggers.TimeOfDayTrigger
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = application.app.repo

    val automations: StateFlow<List<Automation>> =
        repo.automations.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val log: StateFlow<List<RunLogRow>> =
        repo.log.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(a: Automation) = viewModelScope.launch {
        repo.save(a)
        Engine.sync(getApplication())
    }

    fun setEnabled(a: Automation, enabled: Boolean) = save(a.copy(enabled = enabled))

    fun delete(a: Automation) = viewModelScope.launch {
        TimeOfDayTrigger.cancel(getApplication(), a.id)
        repo.delete(a.id)
        Engine.sync(getApplication())
    }

    /** iOS ▶: run an automation's actions now, whatever its trigger. Works for unsaved drafts too. */
    fun runNow(a: Automation) = Engine.scope.launch {
        Engine.run(getApplication(), a, "Run Now", notify = false)
    }

    /** All automations as one JSON file (Connections' passwords and tokens are stored separately, so not included). */
    suspend fun exportJson(): String = AutomationJson.encodeAll(repo.all())

    /**
     * Adds the automations in [json] as new ones. "Run Another Automation" references are remapped to the new IDs.
     * Returns how many were imported.
     */
    suspend fun importJson(json: String): Int {
        val incoming = AutomationJson.decodeAll(json)
        val newIds = incoming.associate { it.id to repo.save(it.copy(id = 0)) }
        incoming.forEach { a ->
            val remapped = a.copy(id = newIds.getValue(a.id), actions = ImportRemap.remap(a.actions, newIds))
            repo.save(remapped)
        }
        Engine.sync(getApplication())
        return incoming.size
    }

    /** Re-arm alarms and the service whenever the app opens. */
    fun refresh() = Engine.sync(getApplication())
}

/** Points "Run Another Automation" at the imported copies' new IDs, including inside If and menus. */
object ImportRemap {
    fun remap(actions: List<Action>, ids: Map<Long, Long>): List<Action> = actions.map { a ->
        when (a) {
            is Action.RunAutomation -> ids[a.automationId]?.let { a.copy(automationId = it) } ?: a
            is Action.If -> a.copy(then = remap(a.then, ids), otherwise = remap(a.otherwise, ids))
            is Action.ChooseFromMenu -> a.copy(options = a.options.map { o: MenuOption -> o.copy(actions = remap(o.actions, ids)) })
            else -> a
        }
    }
}
