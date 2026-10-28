package com.tuvin.pixelautomations

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tuvin.pixelautomations.model.Action
import com.tuvin.pixelautomations.model.AppDb
import com.tuvin.pixelautomations.model.Automation
import com.tuvin.pixelautomations.model.AutomationRepository
import com.tuvin.pixelautomations.model.Trigger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class StorageTest {
    private lateinit var db: AppDb
    private lateinit var repo: AutomationRepository

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDb::class.java)
            .allowMainThreadQueries().build()
        repo = AutomationRepository(db)
    }

    @After fun tearDown() = db.close()

    @Test fun saveUpdateDelete() = runTest {
        val id = repo.save(Automation(trigger = Trigger.Charger(true), actions = listOf(Action.ShowNotification("Plugged in"))))
        assertEquals(Trigger.Charger(true), repo.get(id)?.trigger)

        repo.save(repo.get(id)!!.copy(enabled = false, name = "Charging"))
        val updated = repo.automations.first().single()
        assertEquals(id, updated.id)
        assertEquals(false, updated.enabled)
        assertEquals("Charging", updated.name)

        repo.delete(id)
        assertNull(repo.get(id))
    }
}
