package com.example.inputmonitor.platform.ime

/**
 * Model representing an individual key on the virtual keyboard.
 */
data class KeyModel(
    val label: String,
    val action: KeyAction,
    val weight: Float = 1.0f,
    val isSpecial: Boolean = false
)

/**
 * A horizontal row of keys.
 */
data class KeyboardRow(
    val keys: List<KeyModel>
)

/**
 * Representation of a full keyboard layout.
 */
data class KeyboardLayout(
    val type: KeyboardLayoutType,
    val rows: List<KeyboardRow>
) {
    companion object {

        /**
         * Builds standard English QWERTY layout matching SRS.md §21 and Architecture.md §26.
         *
         * @param isShifted Whether letters should be presented uppercase.
         * @param enterLabel Label for action key (e.g. "↵", "Go", "Search", "Done").
         */
        fun createQwertyLayout(
            isShifted: Boolean = false,
            enterLabel: String = "↵"
        ): KeyboardLayout {
            val row1Chars = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
            val row2Chars = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
            val row3Chars = listOf("z", "x", "c", "v", "b", "n", "m")

            val row1Keys = row1Chars.map { char ->
                val text = if (isShifted) char.uppercase() else char
                KeyModel(label = text, action = KeyAction.Character(text))
            }

            val row2Keys = row2Chars.map { char ->
                val text = if (isShifted) char.uppercase() else char
                KeyModel(label = text, action = KeyAction.Character(text))
            }

            val shiftLabel = if (isShifted) "⇧" else "⇧"
            val row3Keys = buildList {
                add(KeyModel(label = shiftLabel, action = KeyAction.Shift, weight = 1.4f, isSpecial = true))
                addAll(row3Chars.map { char ->
                    val text = if (isShifted) char.uppercase() else char
                    KeyModel(label = text, action = KeyAction.Character(text))
                })
                add(KeyModel(label = "⌫", action = KeyAction.Backspace, weight = 1.4f, isSpecial = true))
            }

            val row4Keys = listOf(
                KeyModel(label = "?123", action = KeyAction.SwitchLayout(KeyboardLayoutType.SYMBOLS), weight = 1.4f, isSpecial = true),
                KeyModel(label = "🌐", action = KeyAction.SwitchIme, weight = 1.0f, isSpecial = true),
                KeyModel(label = ",", action = KeyAction.Character(","), weight = 1.0f),
                KeyModel(label = "SPACE", action = KeyAction.Space, weight = 4.0f),
                KeyModel(label = ".", action = KeyAction.Character("."), weight = 1.0f),
                KeyModel(label = enterLabel, action = KeyAction.Enter, weight = 1.6f, isSpecial = true)
            )

            return KeyboardLayout(
                type = KeyboardLayoutType.QWERTY,
                rows = listOf(
                    KeyboardRow(row1Keys),
                    KeyboardRow(row2Keys),
                    KeyboardRow(row3Keys),
                    KeyboardRow(row4Keys)
                )
            )
        }
    }
}
