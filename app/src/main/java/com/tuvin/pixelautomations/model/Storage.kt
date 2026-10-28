package com.tuvin.pixelautomations.model

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Each automation is stored as its JSON, so new trigger and action types need no schema change. */
@Entity(tableName = "automations")
data class AutomationRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val json: String,
)

@Entity(tableName = "run_log")
data class RunLogRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val automationId: Long,
    val name: String,
    val at: Long,
    val trigger: String,
    val result: String,
)

@Dao
interface AutomationDao {
    @Query("SELECT * FROM automations ORDER BY id")
    fun observe(): Flow<List<AutomationRow>>

    @Query("SELECT * FROM automations ORDER BY id")
    suspend fun all(): List<AutomationRow>

    @Query("SELECT * FROM automations WHERE id = :id")
    suspend fun get(id: Long): AutomationRow?

    @Insert
    suspend fun insert(row: AutomationRow): Long

    @Update
    suspend fun update(row: AutomationRow)

    @Query("DELETE FROM automations WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface RunLogDao {
    @Query("SELECT * FROM run_log ORDER BY at DESC LIMIT 200")
    fun observe(): Flow<List<RunLogRow>>

    @Insert
    suspend fun insert(row: RunLogRow)

    @Query("DELETE FROM run_log WHERE at < :before")
    suspend fun deleteOlderThan(before: Long)
}

@Database(entities = [AutomationRow::class, RunLogRow::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun automations(): AutomationDao
    abstract fun runLog(): RunLogDao
}

class AutomationRepository(private val db: AppDb) {
    val automations: Flow<List<Automation>> = db.automations().observe().map { rows -> rows.map(::toAutomation) }
    val log: Flow<List<RunLogRow>> = db.runLog().observe()

    suspend fun all(): List<Automation> = db.automations().all().map(::toAutomation)
    suspend fun get(id: Long): Automation? = db.automations().get(id)?.let(::toAutomation)

    /** Inserts when [a] has no id yet, otherwise updates. Returns the id. */
    suspend fun save(a: Automation): Long =
        if (a.id == 0L) {
            db.automations().insert(AutomationRow(json = AutomationJson.encode(a)))
        } else {
            db.automations().update(AutomationRow(a.id, AutomationJson.encode(a)))
            a.id
        }

    suspend fun delete(id: Long) = db.automations().delete(id)

    suspend fun addLog(row: RunLogRow) = db.runLog().insert(row)

    private fun toAutomation(row: AutomationRow) = AutomationJson.decode(row.json).copy(id = row.id)
}
