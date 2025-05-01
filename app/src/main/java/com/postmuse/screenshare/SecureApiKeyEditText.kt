package com.postangel.screenshare

import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.core.content.ContextCompat

/**
 * Custom EditText that can toggle between showing and hiding sensitive text like API keys
 */
class SecureApiKeyEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val editText: EditText
    private val toggleButton: ImageButton
    private var isTextVisible = false

    init {
        // Set up the layout
        orientation = HORIZONTAL
        layoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Create the EditText
        editText = EditText(context).apply {
            layoutParams = LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1.0f
            )
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            transformationMethod = PasswordTransformationMethod.getInstance()
        }
        addView(editText)

        // Create the toggle button
        toggleButton = ImageButton(context).apply {
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setImageDrawable(ContextCompat.getDrawable(context, android.R.drawable.ic_menu_view))
            background = ContextCompat.getDrawable(context, android.R.drawable.list_selector_background)
            contentDescription = "Toggle visibility"
        }
        addView(toggleButton)

        // Set up the toggle button click listener
        toggleButton.setOnClickListener {
            isTextVisible = !isTextVisible
            if (isTextVisible) {
                editText.transformationMethod = null
                toggleButton.setImageDrawable(ContextCompat.getDrawable(context, android.R.drawable.ic_menu_close_clear_cancel))
            } else {
                editText.transformationMethod = PasswordTransformationMethod.getInstance()
                toggleButton.setImageDrawable(ContextCompat.getDrawable(context, android.R.drawable.ic_menu_view))
            }
            // Maintain cursor position
            val position = editText.selectionStart
            editText.setSelection(position)
        }
    }

    /**
     * Set the hint text for the EditText
     */
    fun setHint(hint: String) {
        editText.hint = hint
    }

    /**
     * Get the text from the EditText
     */
    fun getText(): String {
        return editText.text.toString()
    }

    /**
     * Set the text for the EditText
     */
    fun setText(text: String) {
        editText.setText(text)
    }

    /**
     * Add a text change listener
     */
    fun addTextChangedListener(watcher: TextWatcher) {
        editText.addTextChangedListener(watcher)
    }
}
