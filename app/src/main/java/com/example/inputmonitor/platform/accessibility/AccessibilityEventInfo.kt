package com.example.inputmonitor.platform.accessibility

import android.view.accessibility.AccessibilityEvent

/**
 * Decoupled representation of accessibility event attributes (Architecture.md §21).
 *
 * Enables deterministic JVM unit testing without requiring Android OS accessibility IPC.
 */
interface AccessibilityEventInfo {
    val eventType: Int
    val packageName: CharSequence?
    val className: CharSequence?
    val contentDescription: CharSequence?
    val isPassword: Boolean
    val itemCount: Int
    val action: Int
    val eventTime: Long
}

/**
 * Adapts an Android [AccessibilityEvent] into [AccessibilityEventInfo].
 */
fun AccessibilityEvent.toEventInfo(): AccessibilityEventInfo = object : AccessibilityEventInfo {
    override val eventType: Int get() = this@toEventInfo.eventType
    override val packageName: CharSequence? get() = this@toEventInfo.packageName
    override val className: CharSequence? get() = this@toEventInfo.className
    override val contentDescription: CharSequence? get() = this@toEventInfo.contentDescription
    override val isPassword: Boolean get() = this@toEventInfo.isPassword
    override val itemCount: Int get() = this@toEventInfo.itemCount
    override val action: Int get() = this@toEventInfo.action
    override val eventTime: Long get() = this@toEventInfo.eventTime
}

/**
 * Lightweight data class for testing and decoupled event processing.
 */
data class SimpleAccessibilityEventInfo(
    override val eventType: Int,
    override val packageName: CharSequence? = null,
    override val className: CharSequence? = null,
    override val contentDescription: CharSequence? = null,
    override val isPassword: Boolean = false,
    override val itemCount: Int = 0,
    override val action: Int = 0,
    override val eventTime: Long = System.currentTimeMillis()
) : AccessibilityEventInfo
