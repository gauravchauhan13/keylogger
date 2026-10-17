package com.example.inputmonitor.platform.ime

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager

/**
 * Android InputMethodService implementing the custom keyboard (SRS.md §22, Architecture.md §23).
 *
 * Implements the Keyboard Independence rule:
 * Keyboard typing operates completely isolated from event logging or persistence layers.
 */
class CustomKeyboardService : InputMethodService() {

    private val keyProcessor = KeyProcessor()
    private val keyboardController = KeyboardController(keyProcessor)
    private var keyboardView: KeyboardView? = null

    override fun onCreate() {
        super.onCreate()
    }

    override fun onCreateInputView(): View {
        val view = KeyboardView(this) { action ->
            handleKeyAction(action)
        }
        view.setLayout(keyboardController.currentLayout.value)
        keyboardView = view
        return view
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

    private fun handleKeyAction(action: KeyAction) {
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
            }
            is KeyAction.Space,
            is KeyAction.Backspace,
            is KeyAction.Enter -> {
                keyProcessor.processKey(action, currentInputConnection, currentInputEditorInfo)
            }
        }
    }

    override fun onDestroy() {
        keyboardView = null
        super.onDestroy()
    }
}
