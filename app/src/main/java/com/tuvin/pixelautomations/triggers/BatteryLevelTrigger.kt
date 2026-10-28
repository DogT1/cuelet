package com.tuvin.pixelautomations.triggers

import com.tuvin.pixelautomations.model.LevelCondition
import com.tuvin.pixelautomations.model.Trigger

/** iOS "Battery Level": fires once each time the level crosses the threshold in the chosen direction. */
object BatteryLevelTrigger {
    fun matches(t: Trigger.BatteryLevel, previous: Int?, current: Int): Boolean {
        if (previous == null || previous == current) return false
        return when (t.condition) {
            LevelCondition.EQUALS -> current == t.percent
            LevelCondition.RISES_ABOVE -> previous <= t.percent && current > t.percent
            LevelCondition.FALLS_BELOW -> previous >= t.percent && current < t.percent
        }
    }
}
