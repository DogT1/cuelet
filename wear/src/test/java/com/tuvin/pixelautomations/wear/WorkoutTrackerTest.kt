package com.tuvin.pixelautomations.wear

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WorkoutTrackerTest {
    @Test fun startAndEnd() {
        assertEquals(WorkoutTracker.Change(true, "RUNNING"), WorkoutTracker.change(false, null, true, "RUNNING"))
        assertNull(WorkoutTracker.change(true, "RUNNING", true, "RUNNING")) // still running
        assertEquals(WorkoutTracker.Change(false, "RUNNING"), WorkoutTracker.change(true, "RUNNING", false, null))
        assertNull(WorkoutTracker.change(false, "RUNNING", false, null)) // asleep / passive again
    }

    @Test fun unknownTypeFallsBack() {
        assertEquals(WorkoutTracker.Change(true, "WORKOUT"), WorkoutTracker.change(false, null, true, null))
    }

    @Test fun messageFormatMatchesThePhone() {
        assertEquals("start|RUNNING", WorkoutMessages.encode(true, "RUNNING").decodeToString())
        assertEquals("end|BIKING", WorkoutMessages.encode(false, "BIKING").decodeToString())
    }
}
