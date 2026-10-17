package com.example.inputmonitor.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.inputmonitor.data.entity.InteractionEventEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for interaction events (Architecture.md §49).
 */
@Dao
interface InteractionEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: InteractionEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<InteractionEventEntity>): List<Long>

    @Query("SELECT * FROM interaction_events WHERE id = :id LIMIT 1")
    suspend fun getEventById(id: Long): InteractionEventEntity?

    @Query("SELECT * FROM interaction_events WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun observeEventsForSession(sessionId: String): Flow<List<InteractionEventEntity>>

    @Query("SELECT * FROM interaction_events WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getEventsForSession(sessionId: String): List<InteractionEventEntity>

    @Query("SELECT * FROM interaction_events WHERE sessionId = :sessionId ORDER BY timestamp ASC LIMIT :limit OFFSET :offset")
    suspend fun getEventsForSessionPaged(sessionId: String, limit: Int, offset: Int): List<InteractionEventEntity>

    @Query("""
        SELECT * FROM interaction_events 
        WHERE sessionId = :sessionId 
          AND (:packageFilter IS NULL OR packageName = :packageFilter)
          AND (:eventTypeFilter IS NULL OR eventType = :eventTypeFilter)
          AND (:startTime IS NULL OR timestamp >= :startTime)
          AND (:endTime IS NULL OR timestamp <= :endTime)
        ORDER BY timestamp ASC
    """)
    suspend fun getEventsFiltered(
        sessionId: String,
        packageFilter: String? = null,
        eventTypeFilter: String? = null,
        startTime: Long? = null,
        endTime: Long? = null
    ): List<InteractionEventEntity>

    @Query("SELECT COUNT(*) FROM interaction_events WHERE sessionId = :sessionId")
    suspend fun getEventCountForSession(sessionId: String): Int

    @Query("SELECT COUNT(*) FROM interaction_events WHERE sessionId = :sessionId")
    fun observeEventCountForSession(sessionId: String): Flow<Int>

    @Query("DELETE FROM interaction_events WHERE sessionId = :sessionId")
    suspend fun deleteEventsForSession(sessionId: String): Int

    @Query("DELETE FROM interaction_events")
    suspend fun deleteAllEvents(): Int
}
