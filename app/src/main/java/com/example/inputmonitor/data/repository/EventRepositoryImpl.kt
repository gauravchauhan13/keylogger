package com.example.inputmonitor.data.repository

import com.example.inputmonitor.core.DispatcherProvider
import com.example.inputmonitor.data.dao.InteractionEventDao
import com.example.inputmonitor.data.entity.InteractionEventEntity
import com.example.inputmonitor.data.mapper.toDomain
import com.example.inputmonitor.data.mapper.toEntity
import com.example.inputmonitor.domain.model.InteractionEvent
import com.example.inputmonitor.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Implementation of [EventRepository] with defensive security validation (Architecture.md §51).
 *
 * Defense in depth:
 * Even if an upstream component erroneously passes raw text in a sensitive event,
 * the repository enforces that sensitive classified events have null eventData before storage.
 */
class EventRepositoryImpl(
    private val eventDao: InteractionEventDao,
    private val dispatchers: DispatcherProvider
) : EventRepository {

    override suspend fun insertEvent(event: InteractionEvent): Long = withContext(dispatchers.io) {
        val safeEntity = sanitizeEventForPersistence(event)
        eventDao.insertEvent(safeEntity)
    }

    override suspend fun insertEvents(events: List<InteractionEvent>): List<Long> = withContext(dispatchers.io) {
        val safeEntities = events.map { sanitizeEventForPersistence(it) }
        eventDao.insertEvents(safeEntities)
    }

    override suspend fun getEventById(id: Long): InteractionEvent? = withContext(dispatchers.io) {
        eventDao.getEventById(id)?.toDomain()
    }

    override fun observeEventsForSession(sessionId: String): Flow<List<InteractionEvent>> {
        return eventDao.observeEventsForSession(sessionId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(dispatchers.io)
    }

    override suspend fun getEventsForSession(sessionId: String): List<InteractionEvent> = withContext(dispatchers.io) {
        eventDao.getEventsForSession(sessionId).map { it.toDomain() }
    }

    override suspend fun getEventsForSessionPaged(
        sessionId: String,
        limit: Int,
        offset: Int
    ): List<InteractionEvent> = withContext(dispatchers.io) {
        eventDao.getEventsForSessionPaged(sessionId, limit, offset).map { it.toDomain() }
    }

    override suspend fun getEventsFiltered(
        sessionId: String,
        packageFilter: String?,
        eventTypeFilter: String?,
        startTime: Long?,
        endTime: Long?
    ): List<InteractionEvent> = withContext(dispatchers.io) {
        eventDao.getEventsFiltered(
            sessionId = sessionId,
            packageFilter = packageFilter,
            eventTypeFilter = eventTypeFilter,
            startTime = startTime,
            endTime = endTime
        ).map { it.toDomain() }
    }

    override suspend fun getEventCountForSession(sessionId: String): Int = withContext(dispatchers.io) {
        eventDao.getEventCountForSession(sessionId)
    }

    override fun observeEventCountForSession(sessionId: String): Flow<Int> {
        return eventDao.observeEventCountForSession(sessionId)
            .flowOn(dispatchers.io)
    }

    override fun observeEventCounts(sessionId: String?): Flow<com.example.inputmonitor.domain.model.EventCounts> {
        return eventDao.observeEventCounts(sessionId)
            .map { tuple ->
                if (tuple != null) {
                    com.example.inputmonitor.domain.model.EventCounts(
                        total = tuple.total,
                        keyboard = tuple.keyboard,
                        accessibility = tuple.accessibility,
                        clicks = tuple.clicks,
                        focus = tuple.focus,
                        scrolls = tuple.scrolls,
                        windowChanges = tuple.windowChanges
                    )
                } else {
                    com.example.inputmonitor.domain.model.EventCounts()
                }
            }
            .flowOn(dispatchers.io)
    }

    override fun observeLatestEvent(): Flow<InteractionEvent?> {
        return eventDao.observeLatestEvent()
            .map { it?.toDomain() }
            .flowOn(dispatchers.io)
    }

    override fun observeEventsPaged(
        filter: com.example.inputmonitor.domain.model.EventFilterCriteria,
        limit: Int,
        offset: Int
    ): Flow<List<InteractionEvent>> {
        return eventDao.observeEventsFilteredPaged(
            sessionId = filter.sessionId,
            packageFilter = filter.packageName,
            eventTypeFilter = filter.eventType?.name,
            sourceFilter = filter.source?.name,
            classificationFilter = filter.classification?.name,
            startTime = filter.startTime,
            endTime = filter.endTime,
            searchQuery = if (filter.searchQuery.isNullOrBlank()) null else filter.searchQuery.trim(),
            limit = limit,
            offset = offset
        )
            .map { list -> list.map { it.toDomain() } }
            .flowOn(dispatchers.io)
    }

    override fun observeFilteredCount(
        filter: com.example.inputmonitor.domain.model.EventFilterCriteria
    ): Flow<Int> {
        return eventDao.observeFilteredCount(
            sessionId = filter.sessionId,
            packageFilter = filter.packageName,
            eventTypeFilter = filter.eventType?.name,
            sourceFilter = filter.source?.name,
            classificationFilter = filter.classification?.name,
            startTime = filter.startTime,
            endTime = filter.endTime,
            searchQuery = if (filter.searchQuery.isNullOrBlank()) null else filter.searchQuery.trim()
        ).flowOn(dispatchers.io)
    }

    override fun observeDistinctPackageNames(): Flow<List<String>> {
        return eventDao.observeDistinctPackageNames()
            .flowOn(dispatchers.io)
    }

    override fun observeMatchingApplications(query: String, limit: Int): Flow<List<String>> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return kotlinx.coroutines.flow.flowOf(emptyList())
        return eventDao.observeMatchingApplications(trimmed, limit)
            .flowOn(dispatchers.io)
    }

    override fun observeMatchingPackages(query: String, limit: Int): Flow<List<String>> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return kotlinx.coroutines.flow.flowOf(emptyList())
        return eventDao.observeMatchingPackages(trimmed, limit)
            .flowOn(dispatchers.io)
    }

    override fun observeSearchSuggestions(
        query: String,
        limit: Int
    ): Flow<List<com.example.inputmonitor.domain.model.SearchSuggestion>> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return kotlinx.coroutines.flow.flowOf(emptyList())

        return kotlinx.coroutines.flow.combine(
            eventDao.observeMatchingApplications(trimmed, limit),
            eventDao.observeMatchingPackages(trimmed, limit)
        ) { apps, pkgs ->
            val suggestions = mutableListOf<com.example.inputmonitor.domain.model.SearchSuggestion>()

            apps.forEach { appName ->
                if (suggestions.size < limit) {
                    suggestions.add(
                        com.example.inputmonitor.domain.model.SearchSuggestion(
                            query = appName,
                            type = com.example.inputmonitor.domain.model.SuggestionType.APPLICATION
                        )
                    )
                }
            }

            pkgs.forEach { pkgName ->
                if (suggestions.size < limit && suggestions.none { it.query.equals(pkgName, ignoreCase = true) }) {
                    suggestions.add(
                        com.example.inputmonitor.domain.model.SearchSuggestion(
                            query = pkgName,
                            type = com.example.inputmonitor.domain.model.SuggestionType.PACKAGE
                        )
                    )
                }
            }

            if (suggestions.size < limit) {
                com.example.inputmonitor.domain.model.EventType.entries
                    .filter { it.name.contains(trimmed, ignoreCase = true) }
                    .take(limit - suggestions.size)
                    .forEach { eventType ->
                        suggestions.add(
                            com.example.inputmonitor.domain.model.SearchSuggestion(
                                query = eventType.name,
                                type = com.example.inputmonitor.domain.model.SuggestionType.EVENT_TYPE
                            )
                        )
                    }
            }

            suggestions
        }.flowOn(dispatchers.io)
    }

    override suspend fun deleteEventsForSession(sessionId: String): Boolean = withContext(dispatchers.io) {
        eventDao.deleteEventsForSession(sessionId) >= 0
    }

    override suspend fun deleteAllEvents(): Boolean = withContext(dispatchers.io) {
        eventDao.deleteAllEvents() >= 0
    }

    /**
     * Defense-in-depth safety validation (SECURITY.md §4, Architecture.md §51).
     * Enforces that sensitive input content NEVER reaches Room storage.
     */
    private fun sanitizeEventForPersistence(event: InteractionEvent): InteractionEventEntity {
        val safeEvent = if (event.inputClassification.isSensitive && event.eventData != null) {
            event.copy(eventData = null)
        } else {
            event
        }
        return safeEvent.toEntity()
    }
}
