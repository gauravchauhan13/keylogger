package com.example.inputmonitor.platform.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Accessibility Service stub for interaction event monitoring.
 *
 * Full implementation will be added in Phase 8.
 * Declared in AndroidManifest.xml and bound via BIND_ACCESSIBILITY_SERVICE.
 */
class AppAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Handled in Phase 8
    }

    override fun onInterrupt() {
        // Handled in Phase 8
    }
}
