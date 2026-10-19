package com.example.inputmonitor.platform.ime

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager

import com.example.inputmonitor.MonitoringApplication
import com.example.inputmonitor.domain.model.EventSource
import com.example.inputmonitor.domain.model.EventType
import com.example.inputmonitor.domain.model.InputClassification
import com.example.inputmonitor.domain.model.RawInteractionEvent

/**
 * Android InputMethodService implementing the custom keyboard (SRS.md §22, Architecture.md §23).
 *
 * Implements the Keyboard Independence rule:
 * Keyboard typing operates completely isolated from event logging or persistence layers.
 */
class CustomKeyboardService : InputMethodService() {

    private val keyProcessor = KeyProcessor()
    private val keyboardController = KeyboardController()
    private var keyboardView: KeyboardView? = null

    // Allows test harness injection; falls back to AppContainer singleton
    var publisherOverride: ImeEventPublisher? = null
    internal val publisher: ImeEventPublisher?
        get() = publisherOverride ?: runCatching { (application as? MonitoringApplication)?.container?.imeEventPublisher }.getOrNull()
    var inputConnectionOverride: InputConnection? = null
    var editorInfoOverride: EditorInfo? = null

    override fun getCurrentInputConnection(): InputConnection? {
        return inputConnectionOverride ?: runCatching { super.getCurrentInputConnection() }.getOrNull()
    }

    override fun getCurrentInputEditorInfo(): EditorInfo? {
        return editorInfoOverride ?: runCatching { super.getCurrentInputEditorInfo() }.getOrNull()
    }

    override fun onEvaluateInputViewShown(): Boolean {
        return true
    }

    override fun onCreateInputView(): View {
        val view = KeyboardView(this) { action ->
            handleKeyAction(action)
        }
        view.setLayout(keyboardController.currentLayout.value)
        keyboardView = view
        return view
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        keyboardController.updateEditorInfo(attribute)
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        keyboardController.updateEditorInfo(info)
        keyboardView?.setLayout(keyboardController.currentLayout.value)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        keyboardController.setShift(ShiftState.OFF)
    }

    internal fun handleKeyAction(action: KeyAction) {
        // Step 1: Input delivery to target app via InputConnection ALWAYS executes first
        when (action) {
            is KeyAction.Shift -> {
                keyboardController.toggleShift()
                keyboardView?.setLayout(keyboardController.currentLayout.value)
            }
            is KeyAction.SwitchIme -> {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.showInputMethodPicker()
            }
            is KeyAction.SwitchLayout -> {
                keyboardController.switchLayout(action.target)
                keyboardView?.setLayout(keyboardController.currentLayout.value)
            }
            is KeyAction.Character -> {
                keyProcessor.processKey(action, currentInputConnection, currentInputEditorInfo)
                if (keyboardController.shiftState == ShiftState.ON_ONCE) {
                    keyboardController.onCharacterTyped()
                    keyboardView?.setLayout(keyboardController.currentLayout.value)
                }
                emitKeyEventCandidate(action)
            }
            is KeyAction.Space,
            is KeyAction.Backspace,
            is KeyAction.Enter -> {
                keyProcessor.processKey(action, currentInputConnection, currentInputEditorInfo)
                emitKeyEventCandidate(action)
            }
        }
    }

    /**
     * Emits a raw event candidate asynchronously.
     * Isolated in a defensive try-catch so auxiliary pipeline failures never impact typing.
     */
    private fun emitKeyEventCandidate(action: KeyAction) {
        try {
            val pub = publisher ?: return
            val context = keyboardController.currentEditorContext
            val packageName = currentInputEditorInfo?.packageName ?: context?.packageName

            val (eventType, candidateData) = when (action) {
                is KeyAction.Character -> EventType.TEXT_COMMITTED to action.text
                is KeyAction.Space -> EventType.TEXT_COMMITTED to " "
                is KeyAction.Backspace -> EventType.DELETE_KEY to null
                is KeyAction.Enter -> EventType.ACTION_KEY to (context?.imeActionLabel ?: "↵")
                else -> return
            }

            val classification = context?.suggestedClassification ?: InputClassification.UNSPECIFIED

            val rawEvent = RawInteractionEvent(
                timestamp = System.currentTimeMillis(),
                source = EventSource.KEYBOARD,
                eventType = eventType,
                packageName = packageName,
                candidateData = candidateData,
                inputClassification = classification,
                metadata = buildMap {
                    context?.fieldCategory?.let { put("fieldCategory", it.name) }
                    context?.imeActionLabel?.let { put("actionLabel", it) }
                }
            )

            pub.publish(rawEvent)
        } catch (_: Throwable) {
            // Failure tolerance: logging failures must never affect typing
        }
    }

    override fun onDestroy() {
        keyboardView = null
        super.onDestroy()
    }
}
