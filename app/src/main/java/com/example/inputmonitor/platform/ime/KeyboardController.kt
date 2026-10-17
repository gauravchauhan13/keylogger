package com.example.inputmonitor.platform.ime

import android.view.inputmethod.EditorInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Coordinates keyboard state, layout switches, shift state machine, and action keys (Architecture.md §29, §30).
 */
class KeyboardController(
    private val keyProcessor: KeyProcessor = KeyProcessor()
) {

    private var _shiftState = ShiftState.OFF
    val shiftState: ShiftState
        get() = _shiftState

    private var _layoutType = KeyboardLayoutType.QWERTY
    val layoutType: KeyboardLayoutType
        get() = _layoutType

    private var _enterLabel = "↵"
    val enterLabel: String
        get() = _enterLabel

    private val _currentLayout = MutableStateFlow(createCurrentLayout())
    val currentLayout: StateFlow<KeyboardLayout> = _currentLayout.asStateFlow()

    /**
     * Toggles the shift state machine:
     * OFF -> ON_ONCE (shift) -> CAPS_LOCK -> OFF.
     */
    fun toggleShift() {
        _shiftState = when (_shiftState) {
            ShiftState.OFF -> ShiftState.ON_ONCE
            ShiftState.ON_ONCE -> ShiftState.CAPS_LOCK
            ShiftState.CAPS_LOCK -> ShiftState.OFF
        }
        refreshLayout()
    }

    /**
     * Sets a specific shift state.
     */
    fun setShift(state: ShiftState) {
        _shiftState = state
        refreshLayout()
    }

    /**
     * Called after a character has been typed.
     * If Shift was in ON_ONCE mode, resets it back to OFF.
     */
    fun onCharacterTyped() {
        if (_shiftState == ShiftState.ON_ONCE) {
            _shiftState = ShiftState.OFF
            refreshLayout()
        }
    }

    /**
     * Switches the active keyboard layout type.
     */
    fun switchLayout(type: KeyboardLayoutType) {
        _layoutType = type
        refreshLayout()
    }

    /**
     * Updates editor info to configure appropriate action key labels.
     */
    fun updateEditorInfo(editorInfo: EditorInfo?) {
        val action = editorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
        _enterLabel = when (action) {
            EditorInfo.IME_ACTION_GO -> "Go"
            EditorInfo.IME_ACTION_SEARCH -> "Search"
            EditorInfo.IME_ACTION_SEND -> "Send"
            EditorInfo.IME_ACTION_NEXT -> "Next"
            EditorInfo.IME_ACTION_DONE -> "Done"
            else -> "↵"
        }
        refreshLayout()
    }

    /**
     * Recreates and emits the updated layout reflecting current shift and action states.
     */
    private fun refreshLayout() {
        _currentLayout.value = createCurrentLayout()
    }

    private fun createCurrentLayout(): KeyboardLayout {
        return when (_layoutType) {
            KeyboardLayoutType.QWERTY -> {
                KeyboardLayout.createQwertyLayout(
                    isShifted = _shiftState.isShifted,
                    enterLabel = _enterLabel
                )
            }
            KeyboardLayoutType.SYMBOLS,
            KeyboardLayoutType.NUMERIC -> {
                // Fallback to QWERTY in Phase 4; full symbol layouts added in Phase 5
                KeyboardLayout.createQwertyLayout(
                    isShifted = _shiftState.isShifted,
                    enterLabel = _enterLabel
                )
            }
        }
    }
}
