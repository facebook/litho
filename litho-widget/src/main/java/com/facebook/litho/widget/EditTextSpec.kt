/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.Editable
import android.text.InputFilter
import android.text.Layout
import android.text.TextUtils
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.core.view.ViewCompat
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentLayout
import com.facebook.litho.EventHandler
import com.facebook.litho.Output
import com.facebook.litho.R
import com.facebook.litho.Size
import com.facebook.litho.StateValue
import com.facebook.litho.ThreadUtils
import com.facebook.litho.annotations.ExcuseMySpec
import com.facebook.litho.annotations.FromTrigger
import com.facebook.litho.annotations.MountSpec
import com.facebook.litho.annotations.OnBind
import com.facebook.litho.annotations.OnCreateInitialState
import com.facebook.litho.annotations.OnCreateMountContent
import com.facebook.litho.annotations.OnLoadStyle
import com.facebook.litho.annotations.OnMeasure
import com.facebook.litho.annotations.OnMount
import com.facebook.litho.annotations.OnTrigger
import com.facebook.litho.annotations.OnUnbind
import com.facebook.litho.annotations.OnUnmount
import com.facebook.litho.annotations.OnUpdateState
import com.facebook.litho.annotations.Param
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.PropDefault
import com.facebook.litho.annotations.Reason
import com.facebook.litho.annotations.ResType
import com.facebook.litho.annotations.State
import com.facebook.litho.utils.MeasureUtils
import com.facebook.litho.utils.VersionedAndroidApis
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * This class is Deprecated and will not be supported. Use [TextInput] instead.
 *
 * Component that renders an [EditText].
 *
 * @prop text Text to display; changing this overrides and replaces the current text. Leave this as
 *   null to signal that the EditText's text property should be left untouched.
 * @prop initialText Initial text to display. This only takes effect if the text prop is null. If
 *   set, the value is set on the EditText exactly once: on initial mount. From then on, the
 *   EditText's text property is not modified.
 * @prop hint Hint text to display.
 * @prop ellipsize If sets, specifies the position of the text to be ellispized.
 * @prop minLines Minimum number of lines to show.
 * @prop maxLines Maximum number of lines to show.
 * @prop maxLength Specifies the maximum number of characters to accept.
 * @prop shadowRadius Blur radius of the shadow.
 * @prop shadowDx Horizontal offset of the shadow.
 * @prop shadowDy Vertical offset of the shadow.
 * @prop shadowColor Color for the shadow underneath the text.
 * @prop isSingleLine If set, makes the text to be rendered in a single line.
 * @prop isSingleLineWrap If set, single line text would warp and horizontal scroll would be
 *   disabled only works when isSingleLine is set.
 * @prop textColor Color of the text.
 * @prop textColorStateList ColorStateList of the text.
 * @prop hintTextColor Hint color of the text.
 * @prop hintTextColorStateList Hint ColorStateList of the text.
 * @prop textSize Size of the text.
 * @prop extraSpacing Extra spacing between the lines of text.
 * @prop spacingMultiplier Extra spacing between the lines of text, as a multiplier.
 * @prop textStyle Style (bold, italic, bolditalic) for the text.
 * @prop typeface Typeface for the text.
 * @prop textAlignment Alignment of the text within its container.
 * @prop gravity Gravity for the text within its container.
 * @prop editable If set, allows the text to be editable.
 * @prop selection Moves the cursor to the selection index.
 * @prop inputType Type of data being placed in a text field, used to help an input method decide
 *   how to let the user enter text.
 * @prop rawInputType Type of data being placed in a text field, used to help an input method decide
 *   how to let the user enter text. This prop will override inputType if both are provided.
 * @prop imeOptions Type of data in the text field, reported to an IME when it has focus.
 * @prop editorActionListener Special listener to be called when an action is performed
 * @prop requestFocus If set, attempts to give focus.
 * @prop cursorDrawableRes Drawable to set for the edit texts cursor.
 * @prop stateUpdatePolicy A policy describing when and how internal state should be updated. This
 *   does violate encapsulation, but is essential for optimization, so costly state updates, which
 *   trigger relayout, happen only when is really needed.
 * @prop inputFilter The [InputFilter]s to apply to the text. Usually you can use these to apply
 *   spans, restrict text input, and do general text manipulation for added text.
 * @prop textWatcher The text watchers to apply to the text. Mainly designed to add decoration spans
 *   to the text during input. Usually you should use an [InputFilter] instead, but an [InputFilter]
 *   won't allow you to decorate the text outside of the changed selection.
 * @prop highlightColor The color to apply to highlights within the text.
 * @prop hintColor The color to apply to the hint text.
 * @prop hintColorStateList A [ColorStateList] to use for the hint text.
 * @prop linkColor The color to apply to links within the text.
 */
@ExcuseMySpec(Reason.J2K_CONVERSION)
@MountSpec(
    isPureRender = true,
    events =
        [
            TextChangedEvent::class,
            SelectionChangedEvent::class,
            KeyUpEvent::class,
            SetTextEvent::class])
@Deprecated("")
internal object EditTextSpec {

  private val ALIGNMENT: Array<Layout.Alignment> = Layout.Alignment.entries.toTypedArray()
  private val TRUNCATE_AT: Array<TextUtils.TruncateAt> = TextUtils.TruncateAt.entries.toTypedArray()
  private val DEFAULT_TYPEFACE: Typeface = Typeface.DEFAULT
  private const val DEFAULT_COLOR = 0
  private val DEFAULT_TEXT_COLOR_STATE_LIST_STATES = arrayOf(intArrayOf(0))
  private val DEFAULT_TEXT_COLOR_STATE_LIST_COLORS = intArrayOf(Color.BLACK)
  private const val DEFAULT_HINT_COLOR = 0
  private val DEFAULT_HINT_COLOR_STATE_LIST_STATES = arrayOf(intArrayOf(0))
  private val DEFAULT_HINT_COLOR_STATE_LIST_COLORS = intArrayOf(Color.LTGRAY)
  private const val DEFAULT_GRAVITY = Gravity.CENTER_VERTICAL or Gravity.START

  @JvmField @PropDefault val minLines: Int = Int.MIN_VALUE
  @JvmField @PropDefault val maxLines: Int = Int.MAX_VALUE
  @JvmField @PropDefault val maxLength: Int = Int.MAX_VALUE
  @JvmField @PropDefault val shadowColor: Int = Color.GRAY
  @JvmField @PropDefault val textColor: Int = DEFAULT_COLOR

  @JvmField
  @PropDefault
  val textColorStateList: ColorStateList =
      ColorStateList(DEFAULT_TEXT_COLOR_STATE_LIST_STATES, DEFAULT_TEXT_COLOR_STATE_LIST_COLORS)

  @JvmField @PropDefault val hintColor: Int = DEFAULT_HINT_COLOR

  @JvmField
  @PropDefault
  val hintColorStateList: ColorStateList =
      ColorStateList(DEFAULT_HINT_COLOR_STATE_LIST_STATES, DEFAULT_HINT_COLOR_STATE_LIST_COLORS)

  @JvmField @PropDefault val linkColor: Int = DEFAULT_COLOR
  @JvmField @PropDefault val textSize: Int = TextComponentSpec.UNSET
  @JvmField @PropDefault val textStyle: Int = DEFAULT_TYPEFACE.style
  @JvmField @PropDefault val typeface: Typeface = DEFAULT_TYPEFACE
  @JvmField @PropDefault val spacingMultiplier: Float = 1.0f
  @JvmField @PropDefault val textAlignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
  @JvmField @PropDefault val gravity: Int = DEFAULT_GRAVITY
  @JvmField @PropDefault val editable: Boolean = true
  @JvmField @PropDefault val selection: Int = -1

  @JvmField
  @PropDefault
  val inputType: Int = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE

  @JvmField @PropDefault val rawInputType: Int = EditorInfo.TYPE_NULL
  @JvmField @PropDefault val imeOptions: Int = EditorInfo.IME_NULL
  @JvmField @PropDefault val isSingleLineWrap: Boolean = false
  @JvmField @PropDefault val requestFocus: Boolean = false
  @JvmField @PropDefault val cursorDrawableRes: Int = -1
  @JvmField
  @PropDefault
  val stateUpdatePolicy: EditTextStateUpdatePolicy = EditTextStateUpdatePolicy.NO_UPDATES

  @OnLoadStyle
  fun onLoadStyle(
      c: ComponentContext,
      ellipsize: Output<TextUtils.TruncateAt?>,
      spacingMultiplier: Output<Float?>,
      minLines: Output<Int?>,
      maxLines: Output<Int?>,
      isSingleLine: Output<Boolean?>,
      text: Output<CharSequence?>,
      textColorStateList: Output<ColorStateList?>,
      linkColor: Output<Int?>,
      highlightColor: Output<Int?>,
      textSize: Output<Int?>,
      textAlignment: Output<Layout.Alignment?>,
      textStyle: Output<Int?>,
      shadowRadius: Output<Float?>,
      shadowDx: Output<Float?>,
      shadowDy: Output<Float?>,
      shadowColor: Output<Int?>,
      gravity: Output<Int?>,
      inputType: Output<Int?>,
      imeOptions: Output<Int?>
  ) {
    val a = c.obtainStyledAttributes(R.styleable.Text, 0)

    var i = 0
    val size = a.indexCount
    while (i < size) {
      val attr = a.getIndex(i)

      if (attr == R.styleable.Text_android_text) {
        text.set(a.getString(attr))
      } else if (attr == R.styleable.Text_android_textColor) {
        textColorStateList.set(a.getColorStateList(attr))
      } else if (attr == R.styleable.Text_android_textSize) {
        textSize.set(a.getDimensionPixelSize(attr, 0))
      } else if (attr == R.styleable.Text_android_ellipsize) {
        val index = a.getInteger(attr, 0)
        if (index > 0) {
          ellipsize.set(TRUNCATE_AT[index - 1])
        }
      } else if (attr == R.styleable.Text_android_textAlignment) {
        val viewTextAlignment = a.getInt(attr, -1)
        textAlignment.set(getAlignment(viewTextAlignment, Gravity.NO_GRAVITY))
      } else if (attr == R.styleable.Text_android_minLines) {
        minLines.set(a.getInteger(attr, -1))
      } else if (attr == R.styleable.Text_android_maxLines) {
        maxLines.set(a.getInteger(attr, -1))
      } else if (attr == R.styleable.Text_android_singleLine) {
        isSingleLine.set(a.getBoolean(attr, false))
      } else if (attr == R.styleable.Text_android_textColorLink) {
        linkColor.set(a.getColor(attr, 0))
      } else if (attr == R.styleable.Text_android_textColorHighlight) {
        highlightColor.set(a.getColor(attr, 0))
      } else if (attr == R.styleable.Text_android_textStyle) {
        textStyle.set(a.getInteger(attr, 0))
      } else if (attr == R.styleable.Text_android_lineSpacingMultiplier) {
        spacingMultiplier.set(a.getFloat(attr, 0f))
      } else if (attr == R.styleable.Text_android_shadowDx) {
        shadowDx.set(a.getFloat(attr, 0f))
      } else if (attr == R.styleable.Text_android_shadowDy) {
        shadowDy.set(a.getFloat(attr, 0f))
      } else if (attr == R.styleable.Text_android_shadowRadius) {
        shadowRadius.set(a.getFloat(attr, 0f))
      } else if (attr == R.styleable.Text_android_shadowColor) {
        shadowColor.set(a.getColor(attr, 0))
      } else if (attr == R.styleable.Text_android_gravity) {
        gravity.set(a.getInteger(attr, 0))
      } else if (attr == R.styleable.Text_android_inputType) {
        inputType.set(a.getInteger(attr, 0))
      } else if (attr == R.styleable.Text_android_imeOptions) {
        imeOptions.set(a.getInteger(attr, 0))
      }
      i++
    }

    a.recycle()
  }

  @OnMeasure
  fun onMeasure(
      c: ComponentContext,
      layout: ComponentLayout,
      widthSpec: Int,
      heightSpec: Int,
      size: Size,
      @Prop(optional = true, resType = ResType.STRING) text: CharSequence?,
      @Prop(optional = true, resType = ResType.STRING) initialText: CharSequence?,
      @Prop(optional = true, resType = ResType.STRING) hint: CharSequence?,
      @Prop(optional = true) ellipsize: TextUtils.TruncateAt?,
      @Prop(optional = true, resType = ResType.INT) minLines: Int,
      @Prop(optional = true, resType = ResType.INT) maxLines: Int,
      @Prop(optional = true, resType = ResType.INT) maxLength: Int,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) shadowRadius: Float,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) shadowDx: Float,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) shadowDy: Float,
      @Prop(optional = true, resType = ResType.COLOR) shadowColor: Int,
      @Prop(optional = true, resType = ResType.BOOL) isSingleLine: Boolean,
      @Prop(optional = true, resType = ResType.COLOR) textColor: Int,
      @Prop(optional = true) textColorStateList: ColorStateList?,
      @Prop(optional = true, resType = ResType.COLOR) hintColor: Int,
      @Prop(optional = true) hintColorStateList: ColorStateList?,
      @Prop(optional = true, resType = ResType.COLOR) linkColor: Int,
      @Prop(optional = true, resType = ResType.COLOR) highlightColor: Int,
      @Prop(optional = true) tintColorStateList: ColorStateList?,
      @Prop(optional = true, resType = ResType.DIMEN_TEXT) textSize: Int,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) extraSpacing: Float,
      @Prop(optional = true, resType = ResType.FLOAT) spacingMultiplier: Float,
      @Prop(optional = true) textStyle: Int,
      @Prop(optional = true) typeface: Typeface,
      @Prop(optional = true) textAlignment: Layout.Alignment,
      @Prop(optional = true) gravity: Int,
      @Prop(optional = true) editable: Boolean,
      @Prop(optional = true) selection: Int,
      @Prop(optional = true) inputType: Int,
      @Prop(optional = true) rawInputType: Int,
      @Prop(optional = true) imeOptions: Int,
      @Prop(optional = true) editorActionListener: TextView.OnEditorActionListener?,
      @Prop(optional = true) isSingleLineWrap: Boolean,
      @Prop(optional = true) requestFocus: Boolean,
      @Prop(optional = true) cursorDrawableRes: Int,
      @Prop(optional = true, varArg = "inputFilter")
      inputFilters: MutableList<@JvmSuppressWildcards InputFilter>?,
      @State(canUpdateLazily = true) input: CharSequence?
  ) {
    // TODO(11759579) - don't allocate a new EditText in every measure.

    val editText = EditTextForMeasure(c.androidContext)

    initEditText(
        editText,
        input
            ?: text, // We want to use the initialText value for *every* measure, not just the first
        // one.
        initialText,
        hint,
        ellipsize,
        inputFilters,
        minLines,
        maxLines,
        maxLength,
        shadowRadius,
        shadowDx,
        shadowDy,
        shadowColor,
        isSingleLine,
        textColor,
        textColorStateList,
        hintColor,
        hintColorStateList,
        linkColor,
        highlightColor,
        tintColorStateList,
        textSize,
        extraSpacing,
        spacingMultiplier,
        textStyle,
        typeface,
        textAlignment,
        gravity,
        editable,
        selection,
        inputType,
        rawInputType,
        imeOptions,
        editorActionListener,
        isSingleLineWrap,
        requestFocus,
        cursorDrawableRes)

    val background: Drawable? = layout.background

    if (background != null) {
      val rect = Rect()
      background.getPadding(rect)

      if (rect.left != 0 || rect.top != 0 || rect.right != 0 || rect.bottom != 0) {
        // Padding from the background will be added to the layout separately, so does not need to
        // be a part of this measurement.
        editText.setPadding(0, 0, 0, 0)
        editText.setBackground(null)
      }
    }

    editText.measure(
        MeasureUtils.getViewMeasureSpec(widthSpec), MeasureUtils.getViewMeasureSpec(heightSpec))

    size.width = editText.measuredWidth
    size.height = editText.measuredHeight
  }

  @OnCreateMountContent
  internal fun onCreateMountContent(c: Context?): EditTextWithEventHandlers {
    return EditTextWithEventHandlers(c)
  }

  @OnMount
  fun onMount(
      c: ComponentContext?,
      editText: EditTextWithEventHandlers,
      @Prop(optional = true, resType = ResType.STRING) text: CharSequence?,
      @Prop(optional = true, resType = ResType.STRING) initialText: CharSequence?,
      @Prop(optional = true, resType = ResType.STRING) hint: CharSequence?,
      @Prop(optional = true) ellipsize: TextUtils.TruncateAt?,
      @Prop(optional = true, resType = ResType.INT) minLines: Int,
      @Prop(optional = true, resType = ResType.INT) maxLines: Int,
      @Prop(optional = true, resType = ResType.INT) maxLength: Int,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) shadowRadius: Float,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) shadowDx: Float,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) shadowDy: Float,
      @Prop(optional = true, resType = ResType.COLOR) shadowColor: Int,
      @Prop(optional = true, resType = ResType.BOOL) isSingleLine: Boolean,
      @Prop(optional = true, resType = ResType.COLOR) textColor: Int,
      @Prop(optional = true) textColorStateList: ColorStateList?,
      @Prop(optional = true, resType = ResType.COLOR) hintColor: Int,
      @Prop(optional = true) hintColorStateList: ColorStateList?,
      @Prop(optional = true, resType = ResType.COLOR) linkColor: Int,
      @Prop(optional = true, resType = ResType.COLOR) highlightColor: Int,
      @Prop(optional = true) tintColorStateList: ColorStateList?,
      @Prop(optional = true, resType = ResType.DIMEN_TEXT) textSize: Int,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) extraSpacing: Float,
      @Prop(optional = true, resType = ResType.FLOAT) spacingMultiplier: Float,
      @Prop(optional = true) textStyle: Int,
      @Prop(optional = true) typeface: Typeface,
      @Prop(optional = true) textAlignment: Layout.Alignment,
      @Prop(optional = true) gravity: Int,
      @Prop(optional = true) editable: Boolean,
      @Prop(optional = true) selection: Int,
      @Prop(optional = true) inputType: Int,
      @Prop(optional = true) rawInputType: Int,
      @Prop(optional = true) imeOptions: Int,
      @Prop(optional = true) editorActionListener: TextView.OnEditorActionListener?,
      @Prop(optional = true) isSingleLineWrap: Boolean,
      @Prop(optional = true) requestFocus: Boolean,
      @Prop(optional = true) cursorDrawableRes: Int,
      @Prop(optional = true, varArg = "inputFilter")
      inputFilters: MutableList<@JvmSuppressWildcards InputFilter>?,
      @State mountedView: AtomicReference<EditTextWithEventHandlers?>,
      @State configuredInitialText: AtomicBoolean,
      @State(canUpdateLazily = true) input: CharSequence?
  ) {
    mountedView.set(editText)

    initEditText(
        editText,
        input ?: text, // Only set initialText on the EditText during the very first mount.
        if (configuredInitialText.getAndSet(true)) null else initialText,
        hint,
        ellipsize,
        inputFilters,
        minLines,
        maxLines,
        maxLength,
        shadowRadius,
        shadowDx,
        shadowDy,
        shadowColor,
        isSingleLine,
        textColor,
        textColorStateList,
        hintColor,
        hintColorStateList,
        linkColor,
        highlightColor,
        tintColorStateList,
        textSize,
        extraSpacing,
        spacingMultiplier,
        textStyle,
        typeface,
        textAlignment,
        gravity,
        editable,
        selection,
        inputType,
        rawInputType,
        imeOptions,
        editorActionListener,
        isSingleLineWrap,
        requestFocus,
        cursorDrawableRes)
  }

  @OnBind
  fun onBind(
      c: ComponentContext?,
      editText: EditTextWithEventHandlers,
      @Prop(optional = true) stateUpdatePolicy: EditTextStateUpdatePolicy?,
      @Prop(optional = true, varArg = "textWatcher")
      textWatchers: List<@JvmSuppressWildcards TextWatcher>?
  ) {
    editText.setComponentContext(c)
    editText.setTextChangedEventHandler(EditText.getTextChangedEventHandler(c))
    editText.setSelectionChangedEventHandler(EditText.getSelectionChangedEventHandler(c))
    editText.setKeyUpEventHandler(EditText.getKeyUpEventHandler(c))
    editText.setStateUpdatePolicy(stateUpdatePolicy)
    editText.attachWatchers(textWatchers)
  }

  @OnUnbind
  fun onUnbind(c: ComponentContext?, editText: EditTextWithEventHandlers) {
    editText.detachWatchers()
    editText.clear()
  }

  @OnUnmount
  fun onUnmount(
      c: ComponentContext?,
      editText: EditTextWithEventHandlers?,
      @State mountedView: AtomicReference<EditTextWithEventHandlers?>
  ) {
    mountedView.set(null)
  }

  @OnTrigger(RequestFocusEvent::class)
  fun requestFocus(
      c: ComponentContext,
      @State mountedView: AtomicReference<EditTextWithEventHandlers?>
  ) {
    val eventHandler = mountedView.get()
    if (eventHandler != null) {
      if (eventHandler.requestFocus()) {
        val imm =
            c.androidContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(eventHandler, 0)
      }
    }
  }

  @OnTrigger(ClearFocusEvent::class)
  fun clearFocus(
      c: ComponentContext,
      @State mountedView: AtomicReference<EditTextWithEventHandlers?>
  ) {
    val eventHandler = mountedView.get()
    if (eventHandler != null) {
      eventHandler.clearFocus()
      val imm =
          c.androidContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
      imm.hideSoftInputFromWindow(eventHandler.windowToken, 0)
    }
  }

  @OnTrigger(SetTextEvent::class)
  fun setText(
      c: ComponentContext?,
      @State mountedView: AtomicReference<EditTextWithEventHandlers?>,
      @FromTrigger text: CharSequence?
  ) {
    ThreadUtils.assertMainThread()

    EditText.lazyUpdateInput(c, text)

    val view = mountedView.get()
    view?.setText(text)
  }

  @OnUpdateState
  fun updateInput(input: StateValue<CharSequence?>, @Param newInput: CharSequence?) {
    input.set(newInput)
  }

  @OnCreateInitialState
  fun onCreateInitialState(
      mountedView: StateValue<AtomicReference<EditTextWithEventHandlers?>?>,
      configuredInitialText: StateValue<AtomicBoolean?>
  ) {
    mountedView.set(AtomicReference())
    configuredInitialText.set(AtomicBoolean())
  }

  private fun initEditText(
      editText: android.widget.EditText,
      text: CharSequence?,
      initialText: CharSequence?,
      hint: CharSequence?,
      ellipsize: TextUtils.TruncateAt?,
      inputFilters: MutableList<InputFilter>?,
      minLines: Int,
      maxLines: Int,
      maxLength: Int,
      shadowRadius: Float,
      shadowDx: Float,
      shadowDy: Float,
      shadowColor: Int,
      isSingleLine: Boolean,
      textColor: Int,
      textColorStateList: ColorStateList?,
      hintColor: Int,
      hintColorStateList: ColorStateList?,
      linkColor: Int,
      highlightColor: Int,
      tintColorStateList: ColorStateList?,
      textSize: Int,
      extraSpacing: Float,
      spacingMultiplier: Float,
      textStyle: Int,
      typeface: Typeface,
      textAlignment: Layout.Alignment,
      gravity: Int,
      editable: Boolean,
      selection: Int,
      inputType: Int,
      rawInputType: Int,
      imeOptions: Int,
      editorActionListener: TextView.OnEditorActionListener?,
      isSingleLineWrap: Boolean,
      requestFocus: Boolean,
      cursorDrawableRes: Int
  ) {
    var inputFiltersToUse = inputFilters
    var inputTypeToUse = inputType
    if (textSize == TextComponentSpec.UNSET) {
      editText.setTextSize(
          TypedValue.COMPLEX_UNIT_SP, TextComponentSpec.DEFAULT_TEXT_SIZE_SP.toFloat())
    } else {
      editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize.toFloat())
    }

    // We only want to change the input type if it actually needs changing, and we need to take
    // isSingleLine into account so that we get the correct input type.
    inputTypeToUse =
        if (isSingleLine) {
          inputTypeToUse and EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE.inv()
        } else {
          inputTypeToUse or EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE
        }

    if (rawInputType != EditorInfo.TYPE_NULL) {
      editText.isSingleLine = isSingleLine
      editText.setRawInputType(rawInputType)
    } else if (inputTypeToUse != editText.inputType) {
      editText.isSingleLine = isSingleLine
      // Needs to be set before min/max lines. Also calling setSingleLine() affects inputType, thus
      // we should re-set input type every time we call setSingleLine()
      editText.inputType = inputTypeToUse
    }

    // disable horizontally scroll in single line mode to make the text wrap.
    if (isSingleLine && isSingleLineWrap) {
      editText.setHorizontallyScrolling(false)
    }

    // Needs to be set before the text so it would apply to the current text
    val lengthFilter = InputFilter.LengthFilter(maxLength)
    if (inputFiltersToUse == null) {
      editText.filters = arrayOf<InputFilter>(lengthFilter)
    } else {
      inputFiltersToUse = ArrayList(inputFiltersToUse)
      inputFiltersToUse.add(lengthFilter)
      editText.filters = inputFiltersToUse.toTypedArray<InputFilter>()
    }

    // If it's the same text, don't set it again so that the caret won't move to the beginning or
    // end of the string. Only looking at String instances in order to avoid span comparisons.
    if (text !is String || text != editText.text.toString()) {
      editText.setText(text)
    } else if (initialText != null) {
      editText.setText(initialText)
    }

    // Setting the hint causes API 28 to lose the focus of the currently selected
    // text input. This happens during LithoState updates. Only set the hint when necessary
    // to try to avoid this issue.
    val oldHint = editText.hint
    val hintsAreEqual = (oldHint === hint) || (oldHint != null && oldHint == hint)
    if (!hintsAreEqual) {
      editText.hint = hint
    }

    editText.ellipsize = ellipsize
    editText.minLines = minLines
    editText.maxLines = maxLines
    editText.setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor)
    editText.setLinkTextColor(linkColor)
    editText.highlightColor = highlightColor
    editText.setLineSpacing(extraSpacing, spacingMultiplier)
    editText.setTypeface(typeface, textStyle)
    editText.gravity = gravity

    editText.imeOptions = imeOptions
    editText.setOnEditorActionListener(editorActionListener)

    editText.isFocusable = editable
    editText.isFocusableInTouchMode = editable
    editText.isClickable = editable
    editText.isLongClickable = editable
    editText.isCursorVisible = editable
    val editableText = editText.text
    val textLength = editableText?.length ?: -1
    if (selection > -1 && selection <= textLength) {
      editText.setSelection(selection)
    }

    if (textColor != 0 || textColorStateList == null) {
      editText.setTextColor(textColor)
    } else {
      editText.setTextColor(textColorStateList)
    }

    if (hintColor != 0 || hintColorStateList == null) {
      editText.setHintTextColor(hintColor)
    } else {
      editText.setHintTextColor(hintColorStateList)
    }

    if (tintColorStateList != null) {
      ViewCompat.setBackgroundTintList(editText, tintColorStateList)
    }

    if (requestFocus) {
      editText.requestFocus()
    }

    if (cursorDrawableRes != -1) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        VersionedAndroidApis.Q.setTextCursorDrawable(editText, cursorDrawableRes)
      } else {
        try {
          // Uses reflection because there is no public API to change cursor color programmatically.
          // Based on
          // http://stackoverflow.com/questions/25996032/how-to-change-programatically-edittext-cursor-color-in-android.
          val f = TextView::class.java.getDeclaredField("mCursorDrawableRes")
          f.isAccessible = true
          f[editText] = cursorDrawableRes
        } catch (exception: Exception) {
          // no-op don't set cursor drawable
        }
      }
    }

    when (textAlignment) {
      Layout.Alignment.ALIGN_NORMAL -> editText.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
      Layout.Alignment.ALIGN_OPPOSITE -> editText.textAlignment = View.TEXT_ALIGNMENT_TEXT_END
      Layout.Alignment.ALIGN_CENTER -> editText.textAlignment = View.TEXT_ALIGNMENT_CENTER
    }
  }

  private fun getAlignment(viewTextAlignment: Int, gravity: Int): Layout.Alignment {
    val alignment =
        when (viewTextAlignment) {
          View.TEXT_ALIGNMENT_GRAVITY -> getAlignment(gravity)
          View.TEXT_ALIGNMENT_TEXT_START -> Layout.Alignment.ALIGN_NORMAL
          View.TEXT_ALIGNMENT_TEXT_END -> Layout.Alignment.ALIGN_OPPOSITE
          View.TEXT_ALIGNMENT_CENTER -> Layout.Alignment.ALIGN_CENTER
          View.TEXT_ALIGNMENT_VIEW_START -> Layout.Alignment.ALIGN_NORMAL
          View.TEXT_ALIGNMENT_VIEW_END -> Layout.Alignment.ALIGN_OPPOSITE
          View.TEXT_ALIGNMENT_INHERIT -> getAlignment(gravity)
          else -> textAlignment
        }
    return alignment
  }

  private fun getAlignment(gravity: Int): Layout.Alignment {
    val alignment =
        when (gravity and Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) {
          Gravity.START -> Layout.Alignment.ALIGN_NORMAL
          Gravity.END -> Layout.Alignment.ALIGN_OPPOSITE
          Gravity.LEFT -> Layout.Alignment.ALIGN_NORMAL
          Gravity.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
          Gravity.CENTER_HORIZONTAL -> Layout.Alignment.ALIGN_CENTER
          else -> textAlignment
        }
    return alignment
  }

  internal class EditTextWithEventHandlers(context: Context?) : android.widget.EditText(context) {
    private val textWatcher: DelegatingTextWatcher
    private var componentContext: ComponentContext? = null
    private var stateUpdatePolicy: EditTextStateUpdatePolicy? = null
    private var textChangedEventHandler: EventHandler<*>? = null
    private var selectionChangedEventHandler: EventHandler<*>? = null
    private var keyUpEventHandler: EventHandler<*>? = null

    private inner class DelegatingTextWatcher : TextWatcher {
      var delegates: List<TextWatcher>? = null
      var prevLineCount: Int = 0

      override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        delegates?.let { delegates ->
          val stop = delegates.size
          for (i in 0 until stop) {
            delegates[i].beforeTextChanged(s, start, count, after)
          }
        }
        // Only need the previous line count when state update policy is ON_LINE_COUNT_CHANGE
        if (stateUpdatePolicy == EditTextStateUpdatePolicy.UPDATE_ON_LINE_COUNT_CHANGE) {
          prevLineCount = lineCount
        }
      }

      override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        delegates?.let { delegates ->
          val stop = delegates.size
          for (i in 0 until stop) {
            delegates[i].onTextChanged(s, start, before, count)
          }
        }
        if ((stateUpdatePolicy == EditTextStateUpdatePolicy.UPDATE_ON_LINE_COUNT_CHANGE &&
            prevLineCount != lineCount) ||
            stateUpdatePolicy == EditTextStateUpdatePolicy.UPDATE_ON_TEXT_CHANGE) {
          EditText.updateInputSync(componentContext, s.toString())
        } else if (stateUpdatePolicy != EditTextStateUpdatePolicy.NO_UPDATES) {
          EditText.lazyUpdateInput(componentContext, s.toString())
        }
      }

      override fun afterTextChanged(s: Editable) {
        delegates?.let { delegates ->
          val stop = delegates.size
          for (i in 0 until stop) {
            delegates[i].afterTextChanged(s)
          }
        }
        if (textChangedEventHandler != null) {
          EditText.dispatchTextChangedEvent(
              textChangedEventHandler, this@EditTextWithEventHandlers, s.toString())
        }
      }
    }

    init {
      this.textWatcher = DelegatingTextWatcher()
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
      super.onSelectionChanged(selStart, selEnd)
      if (selectionChangedEventHandler != null) {
        EditText.dispatchSelectionChangedEvent(selectionChangedEventHandler, selStart, selEnd)
      }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
      if (keyUpEventHandler != null) {
        EditText.dispatchKeyUpEvent(keyUpEventHandler, keyCode, event)
      }
      return super.onKeyUp(keyCode, event)
    }

    fun setStateUpdatePolicy(stateUpdatePolicy: EditTextStateUpdatePolicy?) {
      this.stateUpdatePolicy = stateUpdatePolicy
    }

    fun setComponentContext(componentContext: ComponentContext?) {
      this.componentContext = componentContext
    }

    fun setTextChangedEventHandler(textChangedEventHandler: EventHandler<*>?) {
      this.textChangedEventHandler = textChangedEventHandler
    }

    fun setSelectionChangedEventHandler(selectionChangedEventHandler: EventHandler<*>?) {
      this.selectionChangedEventHandler = selectionChangedEventHandler
    }

    fun setKeyUpEventHandler(keyUpEventHandler: EventHandler<*>?) {
      this.keyUpEventHandler = keyUpEventHandler
    }

    fun clear() {
      stateUpdatePolicy = EditTextSpec.stateUpdatePolicy
      componentContext = null
      textChangedEventHandler = null
      selectionChangedEventHandler = null
      keyUpEventHandler = null
    }

    fun attachWatchers(textWatchers: List<TextWatcher>?) {
      textWatcher.delegates = textWatchers
      addTextChangedListener(textWatcher)
    }

    fun detachWatchers() {
      textWatcher.delegates = null
      removeTextChangedListener(textWatcher)
    }
  }

  /**
   * We use this instead of vanilla EditText for measurement as the ConstantState of the EditText
   * background drawable is not thread-safe and shared across all EditText instances. This is
   * especially important as we measure this component mostly in background thread and it could lead
   * to race conditions where different instances are accessing/modifying same ConstantState
   * concurrently. Mutating background drawable will make sure that ConstantState is not shared
   * therefore will become thread-safe.
   */
  internal class EditTextForMeasure(context: Context?) : android.widget.EditText(context) {
    override fun setBackground(background: Drawable?) {
      background?.mutate()
      super.setBackground(background)
    }
  }
}
