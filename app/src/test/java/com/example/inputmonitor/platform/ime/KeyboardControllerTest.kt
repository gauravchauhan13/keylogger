package com.example.inputmonitor.platform.ime

import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardControllerTest {

    @Test
    fun initialState_isShiftOffAndQwerty() {
        val controller = KeyboardController()
        assertEquals(ShiftState.OFF, controller.shiftState)
        assertEquals(KeyboardLayoutType.QWERTY, controller.layoutType)
        assertFalse(controller.shiftState.isShifted)

        val layout = controller.currentLayout.value
        assertEquals(KeyboardLayoutType.QWERTY, layout.type)
        assertEquals(4, layout.rows.size)
        // First key on row 1 should be lowercase 'q'
        assertEquals("q", layout.rows[0].keys[0].label)
    }

    @Test
    fun toggleShift_cyclesThroughShiftStates() {
        val controller = KeyboardController()

        // OFF -> ON_ONCE
        controller.toggleShift()
        assertEquals(ShiftState.ON_ONCE, controller.shiftState)
        assertTrue(controller.shiftState.isShifted)
        assertEquals("Q", controller.currentLayout.value.rows[0].keys[0].label)

        // ON_ONCE -> CAPS_LOCK
        controller.toggleShift()
        assertEquals(ShiftState.CAPS_LOCK, controller.shiftState)
        assertTrue(controller.shiftState.isShifted)
        assertEquals("Q", controller.currentLayout.value.rows[0].keys[0].label)

        // CAPS_LOCK -> OFF
        controller.toggleShift()
        assertEquals(ShiftState.OFF, controller.shiftState)
        assertFalse(controller.shiftState.isShifted)
        assertEquals("q", controller.currentLayout.value.rows[0].keys[0].label)
    }

    @Test
    fun onCharacterTyped_whenShiftOnOnce_resetsToOff() {
        val controller = KeyboardController()
        controller.toggleShift() // ON_ONCE
        assertEquals(ShiftState.ON_ONCE, controller.shiftState)

        controller.onCharacterTyped()
        assertEquals(ShiftState.OFF, controller.shiftState)
        assertEquals("q", controller.currentLayout.value.rows[0].keys[0].label)
    }

    @Test
    fun onCharacterTyped_whenCapsLock_maintainsCapsLock() {
        val controller = KeyboardController()
        controller.toggleShift() // ON_ONCE
        controller.toggleShift() // CAPS_LOCK
        assertEquals(ShiftState.CAPS_LOCK, controller.shiftState)

        controller.onCharacterTyped()
        assertEquals(ShiftState.CAPS_LOCK, controller.shiftState)
        assertEquals("Q", controller.currentLayout.value.rows[0].keys[0].label)
    }

    @Test
    fun updateEditorInfo_updatesEnterKeyLabel() {
        val controller = KeyboardController()

        controller.updateEditorInfo(EditorInfo().apply { imeOptions = EditorInfo.IME_ACTION_SEARCH })
        assertEquals("Search", controller.enterLabel)

        controller.updateEditorInfo(EditorInfo().apply { imeOptions = EditorInfo.IME_ACTION_GO })
        assertEquals("Go", controller.enterLabel)

        controller.updateEditorInfo(EditorInfo().apply { imeOptions = EditorInfo.IME_ACTION_SEND })
        assertEquals("Send", controller.enterLabel)

        controller.updateEditorInfo(EditorInfo().apply { imeOptions = EditorInfo.IME_ACTION_DONE })
        assertEquals("Done", controller.enterLabel)

        controller.updateEditorInfo(EditorInfo().apply { imeOptions = EditorInfo.IME_ACTION_NEXT })
        assertEquals("Next", controller.enterLabel)

        controller.updateEditorInfo(EditorInfo().apply { imeOptions = EditorInfo.IME_ACTION_NONE })
        assertEquals("↵", controller.enterLabel)
    }
}
