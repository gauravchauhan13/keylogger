package com.example.inputmonitor.platform.ime

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.media.AudioManager
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.LinearLayout

/**
 * High-performance, low-latency native Android view for the custom keyboard (DEVELOPMENT_GUIDELINES.md §4).
 *
 * Includes an offline emoji picker using the device's emoji font.
 */
class KeyboardView(
    context: Context,
    private val onKeyAction: (KeyAction) -> Unit
) : LinearLayout(context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var currentLayout: KeyboardLayout? = null
    private var showingEmoji = false
    private var suggestions = emptyList<String>()
    var onSuggestion: (String) -> Unit = {}
    private var suggestionStrip: LinearLayout? = null
    private var suggestionsAllowed = false

    fun setSuggestionsAllowed(allowed: Boolean) {
        suggestionsAllowed = allowed
        setSuggestions(if (allowed) suggestions else emptyList())
    }

    fun setSuggestions(values: List<String>) {
        suggestions = values
        suggestionStrip?.let { strip ->
            strip.removeAllViews()
            if (values.isEmpty() && suggestionsAllowed) {
                strip.addView(android.widget.TextView(context).apply {
                    text = "Suggestions · autocorrect on Space"
                    textSize = 13f
                    setTextColor(textColor)
                    gravity = Gravity.CENTER
                    alpha = 0.65f
                }, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            }
            values.forEach { value ->
                strip.addView(createPickerButton(value, "Use suggestion $value") { onSuggestion(value) },
                    LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
            }
            strip.visibility = if (suggestionsAllowed) View.VISIBLE else View.GONE
        }
    }
    private val dark = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    private val textColor = Color.parseColor(if (dark) "#E3E3E3" else "#202124")
    private val surfaceColor = Color.parseColor(if (dark) "#202124" else "#E8EAED")
    private val accentColor = Color.parseColor(if (dark) "#A8C7FA" else "#D3E3FD")

    init {
        orientation = VERTICAL
        setBackgroundColor(surfaceColor)
        isFocusable = false
        isFocusableInTouchMode = false
        val pad = dpToPx(4)
        setPadding(pad, pad, pad, pad)
        setOnApplyWindowInsetsListener { _, insets ->
            val bottom = insets.getInsets(WindowInsets.Type.navigationBars()).bottom
            setPadding(pad, pad, pad, pad + bottom)
            insets
        }
    }

    /**
     * Binds and renders the given [KeyboardLayout].
     */
    fun setLayout(layout: KeyboardLayout) {
        currentLayout = layout
        removeAllViews()
        suggestionStrip = null

        if (showingEmoji && layout.type != KeyboardLayoutType.NUMERIC && layout.type != KeyboardLayoutType.PHONE) {
            renderEmojiPicker()
            return
        }
        showingEmoji = false
        suggestionStrip = LinearLayout(context).apply { orientation = HORIZONTAL }.also {
            addView(it, LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(44)))
        }
        setSuggestions(suggestions)
        for ((index, row) in layout.rows.withIndex()) {
            val rowView = createRowView(row, bottomRow = index == layout.rows.lastIndex)
            if (index == 1 && layout.type in listOf(KeyboardLayoutType.QWERTY, KeyboardLayoutType.EMAIL, KeyboardLayoutType.URI)) {
                rowView.setPadding(dpToPx(16), 0, dpToPx(16), 0)
            }
            addView(rowView)
        }
    }

    /** A newly shown editor always starts with letters/numbers, not a stale emoji panel. */
    fun startInput(layout: KeyboardLayout) {
        showingEmoji = false
        visibility = View.VISIBLE
        setLayout(layout)
        requestLayout()
    }

    private fun createPickerButton(label: String, description: String, action: () -> Unit): Button =
        Button(context).apply {
            text = label
            contentDescription = description
            isAllCaps = false
            textSize = 16f
            minWidth = 0
            minimumWidth = 0
            minHeight = 0
            minimumHeight = 0
            stateListAnimator = null
            isFocusable = false
            setPadding(0, 0, 0, 0)
            setTextColor(textColor)
            background = createKeyBackground(true)
            setOnClickListener { action() }
        }

    private fun renderEmojiPicker() {
        addView(EmojiPickerView(context, onKeyAction) {
            showingEmoji = false
            currentLayout?.let(::setLayout)
        }, LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(280)))
    }
    private fun createRowView(row: KeyboardRow, bottomRow: Boolean = false): LinearLayout {
        val rowLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(54)).apply {
                topMargin = dpToPx(2)
                bottomMargin = dpToPx(2)
            }
        }

        for (key in row.keys) {
            // Use the bottom-row switcher position for emoji; long-press Space still switches IMEs.
            val keyView = if (bottomRow && key.action == KeyAction.SwitchIme) {
                createEmojiButton(key.weight)
            } else createKeyView(key)
            if (bottomRow && key.action == KeyAction.Space && row.keys.none { it.action == KeyAction.SwitchIme }
                && currentLayout?.type != KeyboardLayoutType.NUMERIC && currentLayout?.type != KeyboardLayoutType.PHONE) {
                rowLayout.addView(createEmojiButton(1f))
            }
            rowLayout.addView(keyView)
        }

        return rowLayout
    }

    private fun createEmojiButton(weight: Float): View = createPickerButton("☺", "Open emoji picker") {
        showingEmoji = true
        currentLayout?.let(::setLayout)
    }.apply {
        textSize = 24f
        layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, weight).apply {
            leftMargin = dpToPx(2)
            rightMargin = dpToPx(2)
        }
    }

    private fun createKeyView(key: KeyModel): View {
        val button = Button(context).apply {
            text = if (key.action == KeyAction.Space) "English" else key.label
            contentDescription = when (key.action) {
                KeyAction.Space -> "Space. Long press to change keyboard"
                KeyAction.Backspace -> "Delete"
                KeyAction.Shift -> "Shift"
                else -> key.label
            }
            textSize = if (key.label.length > 2) 14f else 22f
            setTextColor(if (key.action == KeyAction.Enter) Color.parseColor("#202124") else textColor)
            isAllCaps = false
            isFocusable = false
            minWidth = 0
            minimumWidth = 0
            minHeight = 0
            minimumHeight = 0
            stateListAnimator = null
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 0)

            background = createKeyBackground(key.isSpecial, key.action == KeyAction.Enter)

            val params = LayoutParams(0, LayoutParams.MATCH_PARENT, key.weight).apply {
                leftMargin = dpToPx(2)
                rightMargin = dpToPx(2)
            }
            layoutParams = params

            // Dispatch after a completed tap: rebuilding a layout on ACTION_DOWN can
            // detach the pressed view, and scrolling the emoji grid must not type.
            setOnClickListener {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                playKeySound()
                onKeyAction(key.action)
            }
            if (key.action == KeyAction.Space) {
                setOnLongClickListener {
                    onKeyAction(KeyAction.SwitchIme)
                    true
                }
            }
        }

        return button
    }

    private fun playKeySound() {
        try {
            audioManager?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD)
        } catch (_: Exception) {
            // Audio failure must never block typing
        }
    }

    private fun createKeyBackground(isSpecial: Boolean, isAction: Boolean = false): StateListDrawable {
        val normalColor = when {
            isAction -> accentColor
            isSpecial -> Color.parseColor(if (dark) "#303134" else "#DADCE0")
            else -> Color.parseColor(if (dark) "#3C4043" else "#FFFFFF")
        }
        val pressedColor = Color.parseColor(if (dark) "#5F6368" else "#BDC1C6")
        val cornerRadius = dpToPx(if (isSpecial || isAction) 24 else 6).toFloat()

        val normalDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(normalColor)
            this.cornerRadius = cornerRadius
        }

        val pressedDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(pressedColor)
            this.cornerRadius = cornerRadius
        }

        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), pressedDrawable)
            addState(intArrayOf(), normalDrawable)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}
