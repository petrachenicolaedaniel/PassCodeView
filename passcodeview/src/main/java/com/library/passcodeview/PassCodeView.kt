package com.library.passcodeview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.support.v7.widget.AppCompatEditText
import android.text.Editable
import android.text.InputFilter
import android.text.InputType.TYPE_CLASS_NUMBER
import android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
import android.text.TextWatcher
import android.text.method.TransformationMethod
import android.util.AttributeSet
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.LinearLayout

@SuppressLint("ClickableViewAccessibility")
class PassCodeView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), TextWatcher, View.OnFocusChangeListener, View.OnKeyListener {

    companion object {
        private const val DEFAULT_COUNT = 6
    }

    private val items = mutableListOf<AppCompatEditText>()

    private var itemsCount: Int = DEFAULT_COUNT
    private var itemHeight: Int = 0
    private var itemWidth: Int = 0
    private var itemSpacing: Int = 0
    private var finalPositionReached = false

    private var currentFocusedView: View? = null

    init {
        val style = context.obtainStyledAttributes(attrs, R.styleable.PassCodeView)

        itemsCount = style.getInt(R.styleable.PassCodeView_itemsCount, DEFAULT_COUNT)
        itemHeight = style.getDimension(
            R.styleable.PassCodeView_itemHeight,
            resources.getDimensionPixelSize(R.dimen.pass_code_view_item_size).toFloat()
        ).toInt()
        itemWidth = style.getDimension(
            R.styleable.PassCodeView_itemWidth,
            resources.getDimensionPixelSize(R.dimen.pass_code_view_item_size).toFloat()
        ).toInt()
        itemSpacing = style.getDimension(
            R.styleable.PassCodeView_itemSpacing,
            resources.getDimensionPixelSize(R.dimen.pass_code_view_item_spacing).toFloat()
        ).toInt()

        style.recycle()

        val layoutParams = LayoutParams(itemWidth, itemHeight)
        val filters = arrayOfNulls<InputFilter>(1)
        filters[0] = InputFilter.LengthFilter(1)

        for (i in 0 until itemsCount) {
            val item = AppCompatEditText(context)
            layoutParams.setMargins(itemSpacing / 2, itemSpacing / 2, itemSpacing / 2, itemSpacing / 2)
            item.filters = filters
            item.layoutParams = layoutParams
            item.gravity = Gravity.CENTER
            item.isCursorVisible = false
            item.isClickable = false
            item.setTextColor(Color.BLACK)
            item.tag = "" + i
            item.inputType = TYPE_CLASS_NUMBER or TYPE_NUMBER_VARIATION_PASSWORD
            item.onFocusChangeListener = this
            item.setOnKeyListener(this)
            item.transformationMethod = PinTransformationMethod()
            item.addTextChangedListener(this)
            item.setOnTouchListener { _, _ ->
                false
            }
            addView(item)
            items.add(i, item)
        }

        updateEnabledState()
    }

    private fun updateEnabledState() {
        val currentTag = Math.max(0, items.indexOf(currentFocusedView))
        for (index in items.indices) {
            val editText = items[index]
            editText.isEnabled = index <= currentTag
        }
    }

    override fun afterTextChanged(s: Editable?) { /* not used */
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { /* not used */
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

        val currentTag = items.indexOf(currentFocusedView)

        if (currentFocusedView != null && s != null && s.length == 1) {
            if (currentTag < itemsCount - 1) {
                val nextEditText = items[currentTag + 1]
                nextEditText.isEnabled = true
                nextEditText.requestFocus()
            } else {
                finalPositionReached = true
            }
        }

        updateEnabledState()
    }

    override fun onFocusChange(v: View?, hasFocus: Boolean) {
        if (hasFocus) {
            // do not allow other items to request focus
            for (editText in items) {
                if (editText.length() == 0) {
                    if (editText !== v) {
                        editText.requestFocus()
                    } else {
                        currentFocusedView = v
                    }
                    return
                }
            }

            val lastItem = items.last()

            if (lastItem !== v) {
                lastItem.requestFocus()
            } else {
                currentFocusedView = v
            }

        } else {
            v?.clearFocus()
        }
    }

    override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
        event?.let {
            if (it.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_DEL) {
                val currentTag = items.indexOf(currentFocusedView)
                val currentItem = items[currentTag]
                if (finalPositionReached) {
                    // clear text
                    if (!currentItem.text.isNullOrEmpty()) {
                        currentItem.setText("")
                    }
                    finalPositionReached = false
                } else if (currentTag > 0) {
                    if (currentItem.length() == 0) {
                        // go to previous
                        items[currentTag - 1].requestFocus()
                    }
                    // clear text
                    currentItem.setText("")
                }
                return true
            }
            return false
        }
        return false
    }

    private inner class PinTransformationMethod : TransformationMethod {

        private val BULLET = '\u2022'

        override fun getTransformation(source: CharSequence, view: View): CharSequence {
            return PasswordCharSequence(source)
        }

        override fun onFocusChanged(
            view: View?,
            sourceText: CharSequence?,
            focused: Boolean,
            direction: Int,
            previouslyFocusedRect: Rect?
        ) {

        }

        inner class PasswordCharSequence(private val source: CharSequence) : CharSequence {

            override val length: Int
                get() = source.length

            override fun get(index: Int): Char {
                return BULLET
            }

            override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
                return PasswordCharSequence(this.source.subSequence(startIndex, endIndex))
            }

        }
    }

}