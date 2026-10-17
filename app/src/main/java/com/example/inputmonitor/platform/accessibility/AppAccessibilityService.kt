package com.example.inputmonitor.platform.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.example.inputmonitor.MonitoringApplication

/**
 * Android AccessibilityService monitoring permitted user interactions (SRS.md §31, Architecture.md §18).
 *
 * Implements non-blocking execution, self-monitoring suppression, and strict content minimization.
 */
class AppAccessibilityService : AccessibilityService() {

    var filterOverride: AccessibilityEventFilter? = null
    var mapperOverride: AccessibilityEventMapper? = null
    var publisherOverride: AccessibilityEventPublisher? = null

    internal val filter: AccessibilityEventFilter
        get() = filterOverride ?: (application as? MonitoringApplication)?.container?.accessibilityEventFilter
            ?: AccessibilityEventFilterImpl(packageName)

    internal val mapper: AccessibilityEventMapper
        get() = mapperOverride ?: (application as? MonitoringApplication)?.container?.accessibilityEventMapper
            ?: AccessibilityEventMapperImpl()

    internal val publisher: AccessibilityEventPublisher?
        get() = publisherOverride ?: (application as? MonitoringApplication)?.container?.accessibilityEventPublisher

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        handleEvent(event.toEventInfo())
    }

    internal fun handleEvent(event: AccessibilityEventInfo?) {
        if (event == null) return

        try {
            if (!filter.shouldProcess(event)) {
                return
            }

            val rawEvent = mapper.map(event)
            publisher?.publish(rawEvent)
        } catch (_: Exception) {
            // Failure-tolerance: errors in accessibility mapping/publishing must never crash the service
        }
    }

    override fun onInterrupt() {
        // Accessibility framework callback when service feedback is interrupted
    }
}
