package com.example.inputmonitor.platform.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardLayoutTest {

    @Test
    fun qwertyLayout_hasCorrectStructure() {
        val layout = KeyboardLayout.createQwertyLayout(isShifted = false, enterLabel = "↵")

        assertEquals(KeyboardLayoutType.QWERTY, layout.type)
        assertEquals(4, layout.rows.size)

        // Row 1: 10 keys (q to p)
        assertEquals(10, layout.rows[0].keys.size)
        assertEquals("q", layout.rows[0].keys.first().label)
        assertEquals("p", layout.rows[0].keys.last().label)

        // Row 2: 9 keys (a to l)
        assertEquals(9, layout.rows[1].keys.size)
        assertEquals("a", layout.rows[1].keys.first().label)
        assertEquals("l", layout.rows[1].keys.last().label)

        // Row 3: 9 keys (Shift + 7 letters + Backspace)
        assertEquals(9, layout.rows[2].keys.size)
        assertTrue(layout.rows[2].keys.first().action is KeyAction.Shift)
        assertEquals("z", layout.rows[2].keys[1].label)
        assertEquals("m", layout.rows[2].keys[7].label)
        assertTrue(layout.rows[2].keys.last().action is KeyAction.Backspace)

        // Row 4: 6 keys (?123, SwitchIme, comma, space, period, enter)
        assertEquals(6, layout.rows[3].keys.size)
        assertTrue(layout.rows[3].keys[0].action is KeyAction.SwitchLayout)
        assertTrue(layout.rows[3].keys[1].action is KeyAction.SwitchIme)
        assertEquals(",", layout.rows[3].keys[2].label)
        assertTrue(layout.rows[3].keys[3].action is KeyAction.Space)
        assertEquals(".", layout.rows[3].keys[4].label)
        assertTrue(layout.rows[3].keys[5].action is KeyAction.Enter)
    }

    @Test
    fun qwertyLayout_isShifted_setsUppercaseLabels() {
        val unshifted = KeyboardLayout.createQwertyLayout(isShifted = false)
        val shifted = KeyboardLayout.createQwertyLayout(isShifted = true)

        assertEquals("q", unshifted.rows[0].keys[0].label)
        assertEquals("Q", shifted.rows[0].keys[0].label)

        assertEquals("a", unshifted.rows[1].keys[0].label)
        assertEquals("A", shifted.rows[1].keys[0].label)

        assertEquals("z", unshifted.rows[2].keys[1].label)
        assertEquals("Z", shifted.rows[2].keys[1].label)
    }
}
