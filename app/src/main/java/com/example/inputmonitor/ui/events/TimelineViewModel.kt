package com.example.inputmonitor.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.inputmonitor.domain.model.EventFilterCriteria
import com.example.inputmonitor.domain.model.EventSource
import com.example.inputmonitor.domain.model.EventType
import com.example.inputmonitor.domain.model.InputClassification
import com.example.inputmonitor.domain.model.InteractionEvent
import com.example.inputmonitor.domain.model.MonitoringSession
import com.example.inputmonitor.domain.repository.EventRepository
import com.example.inputmonitor.domain.repository.SessionRepository
import com.example.inputmonitor.domain.usecase.ObserveActiveSessionUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the interaction event timeline screen (Architecture.md §56, SRS.md §48, §49).
 *
 * Implements bounded RAM consumption via paginated Room queries (AC-23, AC-24).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TimelineViewModel(
    private val eventRepository: EventRepository,
    private val sessionRepository: SessionRepository,
    observeActiveSessionUseCase: ObserveActiveSessionUseCase
) : ViewModel() {

    val pageSize: Int = 50

    private val filterFlow = MutableStateFlow(EventFilterCriteria())
    private val pageFlow = MutableStateFlow(0)
    private val selectedEventFlow = MutableStateFlow<InteractionEvent?>(null)
    private val isFilterDialogVisibleFlow = MutableStateFlow(false)

    private val pagedEventsFlow = combine(filterFlow, pageFlow) { filter, page ->
        filter to page
    }.flatMapLatest { (filter, page) ->
        eventRepository.observeEventsPaged(
            filter = filter,
            limit = pageSize,
            offset = page * pageSize
        )
    }

    private val countFlow = filterFlow.flatMapLatest { filter ->
        eventRepository.observeFilteredCount(filter)
    }

    private data class EventsPagingState(
        val events: List<InteractionEvent>,
        val count: Int,
        val filter: EventFilterCriteria,
        val page: Int
    )

    private data class AuxiliaryState(
        val packages: List<String>,
        val sessions: List<MonitoringSession>,
        val activeSession: MonitoringSession?,
        val selectedEvent: InteractionEvent?,
        val isFilterDialogVisible: Boolean
    )

    private val eventsPagingStateFlow = combine(
        pagedEventsFlow,
        countFlow,
        filterFlow,
        pageFlow
    ) { events, count, filter, page ->
        EventsPagingState(events, count, filter, page)
    }

    private val auxiliaryStateFlow = combine(
        eventRepository.observeDistinctPackageNames(),
        sessionRepository.observeAllSessions(),
        observeActiveSessionUseCase(),
        selectedEventFlow,
        isFilterDialogVisibleFlow
    ) { packages, sessions, activeSession, selectedEvent, isFilterDialogVisible ->
        AuxiliaryState(
            packages = packages,
            sessions = sessions,
            activeSession = activeSession,
            selectedEvent = selectedEvent,
            isFilterDialogVisible = isFilterDialogVisible
        )
    }

    val uiState: StateFlow<TimelineUiState> = combine(
        eventsPagingStateFlow,
        auxiliaryStateFlow
    ) { paging, aux ->
        TimelineUiState(
            events = paging.events,
            totalMatchingEvents = paging.count,
            filterCriteria = paging.filter,
            distinctPackages = aux.packages,
            sessions = aux.sessions,
            activeSession = aux.activeSession,
            currentPage = paging.page,
            pageSize = pageSize,
            selectedEventForDetail = aux.selectedEvent,
            isFilterDialogVisible = aux.isFilterDialogVisible
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = TimelineUiState(pageSize = pageSize)
    )

    fun setSessionFilter(sessionId: String?) {
        filterFlow.value = filterFlow.value.copy(sessionId = sessionId)
        pageFlow.value = 0
    }

    fun setPackageFilter(packageName: String?) {
        filterFlow.value = filterFlow.value.copy(packageName = packageName)
        pageFlow.value = 0
    }

    fun setEventTypeFilter(eventType: EventType?) {
        filterFlow.value = filterFlow.value.copy(eventType = eventType)
        pageFlow.value = 0
    }

    fun setSourceFilter(source: EventSource?) {
        filterFlow.value = filterFlow.value.copy(source = source)
        pageFlow.value = 0
    }

    fun setClassificationFilter(classification: InputClassification?) {
        filterFlow.value = filterFlow.value.copy(classification = classification)
        pageFlow.value = 0
    }

    fun setSearchQuery(query: String?) {
        val safeQuery = if (query.isNullOrBlank()) null else query.trim()
        filterFlow.value = filterFlow.value.copy(searchQuery = safeQuery)
        pageFlow.value = 0
    }

    fun setTimeRange(startTime: Long?, endTime: Long?) {
        filterFlow.value = filterFlow.value.copy(startTime = startTime, endTime = endTime)
        pageFlow.value = 0
    }

    fun clearFilters() {
        filterFlow.value = EventFilterCriteria()
        pageFlow.value = 0
    }

    fun nextPage() {
        val currentState = uiState.value
        if (currentState.hasNextPage) {
            pageFlow.value += 1
        }
    }

    fun previousPage() {
        if (pageFlow.value > 0) {
            pageFlow.value -= 1
        }
    }

    fun goToPage(page: Int) {
        val total = uiState.value.totalPages
        if (page in 0 until total) {
            pageFlow.value = page
        }
    }

    fun selectEvent(event: InteractionEvent?) {
        selectedEventFlow.value = event
    }

    fun setFilterDialogVisible(visible: Boolean) {
        isFilterDialogVisibleFlow.value = visible
    }

    fun deleteEventsForSession(sessionId: String) {
        viewModelScope.launch {
            eventRepository.deleteEventsForSession(sessionId)
            if (filterFlow.value.sessionId == sessionId) {
                pageFlow.value = 0
            }
        }
    }

    companion object {
        fun provideFactory(
            eventRepository: EventRepository,
            sessionRepository: SessionRepository,
            observeActiveSessionUseCase: ObserveActiveSessionUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TimelineViewModel(
                    eventRepository,
                    sessionRepository,
                    observeActiveSessionUseCase
                ) as T
            }
        }
    }
}
