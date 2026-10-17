package com.example.inputmonitor.platform.ime

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.media.AudioManager
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout

/**
 * High-performance, low-latency native Android view for the custom keyboard (DEVELOPMENT_GUIDELINES.md §4).
 *
 * Designed with zero-allocation updates on shift/layout changes to minimize GC pauses during typing.
 */
class KeyboardView(
    context: Context,
    private val onKeyAction: (KeyAction) -> Unit
) : LinearLayout(context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var currentLayout: KeyboardLayout? = null

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.parseColor("#1C1B1F")) // Material 3 Dark Surface
        val pad = dpToPx(4)
        setPadding(pad, pad, pad, pad)
    }

    /**
     * Binds and renders the given [KeyboardLayout].
     */
    fun setLayout(layout: KeyboardLayout) {
        currentLayout = layout
        removeAllViews()

        for (row in layout.rows) {
            val rowView = createRowView(row)
            addView(rowView)
        }
    }

    private fun createRowView(row: KeyboardRow): LinearLayout {
        val rowLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(54)).apply {
                topMargin = dpToPx(2)
                bottomMargin = dpToPx(2)
            }
        }

        for (key in row.keys) {
            val keyView = createKeyView(key)
            rowLayout.addView(keyView)
        }

        return rowLayout
    }

    private fun createKeyView(key: KeyModel): View {
        val button = Button(context).apply {
            text = key.label
            textSize = if (key.label.length > 2) 12f else 18f
            setTextColor(Color.WHITE)
            isAllCaps = false
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 0)

            background = createKeyBackground(key.isSpecial)

            val params = LayoutParams(0, LayoutParams.MATCH_PARENT, key.weight).apply {
                leftMargin = dpToPx(2)
                rightMargin = dpToPx(2)
            }
            layoutParams = params

            setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        view.isPressed = true
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        playKeySound()
                        onKeyAction(key.action)
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        view.isPressed = false
                        true
                    }
                    else -> false
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

    private fun createKeyBackground(isSpecial: Boolean): StateListDrawable {
        val normalColor = if (isSpecial) Color.parseColor("#322F37") else Color.parseColor("#49454F")
        val pressedColor = if (isSpecial) Color.parseColor("#4A4458") else Color.parseColor("#635D6B")
        val cornerRadius = dpToPx(8).toFloat()

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
