package com.example.inputmonitor.platform.ime

import android.inputmethodservice.InputMethodService
import android.view.View

/**
 * Custom Keyboard Input Method Service stub.
 *
 * Full implementation will be added in Phase 4 / Phase 5.
 * Declared in AndroidManifest.xml and bound via BIND_INPUT_METHOD.
 */
class CustomKeyboardService : InputMethodService() {

    override fun onCreate() {
        super.onCreate()
    }

    override fun onCreateInputView(): View {
        return super.onCreateInputView()
    }
}
