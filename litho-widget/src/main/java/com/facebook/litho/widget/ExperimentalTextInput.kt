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

import android.R
import android.content.Context
import android.content.ContextWrapper
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.Editable
import android.text.InputFilter
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.ArrowKeyMovementMethod
import android.text.method.KeyListener
import android.text.method.MovementMethod
import android.text.style.SuggestionRangeSpan
import android.util.TypedValue
import android.view.ActionMode
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.GravityInt
import androidx.core.util.ObjectsCompat
import androidx.core.view.ViewCompat
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentScope
import com.facebook.litho.LithoPrimitive
import com.facebook.litho.PrimitiveComponent
import com.facebook.litho.PrimitiveComponentScope
import com.facebook.litho.State
import com.facebook.litho.Style
import com.facebook.litho.ThreadUtils.assertMainThread
import com.facebook.litho.annotations.Hook
import com.facebook.litho.annotations.OnTrigger
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.useCached
import com.facebook.litho.useState
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.LayoutScope
import com.facebook.rendercore.primitives.PrimitiveLayoutResult
import com.facebook.rendercore.primitives.ViewAllocator
import com.facebook.rendercore.toHeightSpec
import com.facebook.rendercore.toWidthSpec
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.min

/**
 * Component that renders an editable text input using an android [EditText]. It is measured based
 * on the input text [String] representation.
 *
 * Performance is critical for good user experience. Follow these tips for good performance:
 * * Avoid changing props at all costs as it forces expensive EditText reconfiguration.
 * * Avoid updating state, use Event trigger [OnTrigger] to update text, request view focus or set
 *   selection. `TextInput.setText(c, key, text)`.
 * * Using custom inputFilters take special care to implement equals correctly or the text field
 *   must be reconfigured on every mount. (Better yet, store your InputFilter in a static or
 *   LruCache so that you're not constantly creating new instances.)
 *
 * Because this component is backed by android [EditText] many native capabilities are applicable:
 * * Use [InputFilter] to set a text length limit or modify text input.
 * * Remove android EditText underline by removing background.
 * * Change the input representation by passing one of the [android.text.InputType] constants.
 *
 * It is also treated by the system as an android [EditText]:
 * * When [EditText] receives focus, a system keyboard is shown.
 * * When the user opens the screen and android [EditText] is the first element in the View
 *   hierarchy, it gains focus.
 *
 * Example of multiline editable text with custom text color, text length limit, removed underline
 * drawable, and sentence capitalisation:
 * ```
 * private val lenFilter: InputFilter = InputFilter.LengthFilter(maxLength);
 *
 * ExperimentalTextInput(
 *   initialText = text,
 *   textColorStateList = ColorStateList.valueOf(color),
 *   multiline = true,
 *   inputFilter = lenFilter,
 *   backgroundColor = Color.TRANSPARENT,
 *   inputType =InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES,
 * )
 * ```
 *
 * @property initialText Initial text to display. If set, the value is set on the EditText exactly
 *   once: on initial mount. From then on, the EditText's text property is not modified.
 * @property hint Hint text to display.
 * @property inputBackground The background of the EditText itself; this is subtly distinct from the
 *   Litho background prop. The padding of the inputBackground drawable will be applied to the
 *   EditText itself, insetting the cursor and text field.
 * @property shadowRadius Blur radius of the shadow.
 * @property shadowDx Horizontal offset of the shadow.
 * @property shadowDy Vertical offset of the shadow.
 * @property shadowColor Color for the shadow underneath the text.
 * @property textColorStateList ColorStateList of the text.
 * @property hintColorStateList ColorStateList of the hint text.
 * @property highlightColor Color for selected text.
 * @property textSize Size of the text.
 * @property typeface Typeface for the text.
 * @property textAlignment Alignment of the text within its container. This only has effect on API
 *   level 17 and above; it's up to you to handle earlier API levels by adjusting gravity.
 * @property gravity Gravity for the text within its container.
 * @property editable If set, allows the text to be editable.
 * @property cursorVisible Set whether the cursor is visible. The default is true.
 * @property inputType Type of data being placed in a text field, used to help an input method
 *   decide how to let the user enter text. To add multiline use multiline(true) method.
 * @property rawInputType Type of data being placed in a text field. Directly changes the content
 *   type integer of the text view, without modifying any other state. This prop will override
 *   inputType if both are provided.
 * @property imeOptions Type of data in the text field, reported to an IME when it has focus.
 * @property privateImeOptions The private content type of the text, which is the
 *   [EditorInfo.privateImeOptions] field that will be filled in when creating an input connection.
 * @property inputFilters Used to filter the input to e.g. a max character count.
 * @property multiline If set to true, type of the input will be changed to multiline TEXT. Because
 *   passwords or numbers couldn't be multiline by definition.
 * @property ellipsize If set, specifies the position of the text to be ellipsized. See
 *   [android documentation](https://developer.android.com/reference/android/widget/TextView.html#setEllipsize(android.text.TextUtils.TruncateAt))
 *   for behavior description.
 * @property minLines Minimum number of lines to show.
 * @property maxLines Maximum number of lines to show.
 * @property cursorDrawableRes Drawable to set as an edit text cursor.
 * @property error Sets the right-hand compound drawable of the TextView to the "error" icon and
 *   sets an error message that will be displayed in a popup when the TextView has focus. The icon
 *   and error message will be reset to null when any key events cause changes to the TextView's
 *   text. If the error is null, the error message and icon will be cleared. See
 *   https://developer.android.com/reference/android/widget/TextView.html#setError for more details.
 * @property errorDrawable Will show along with the error message when a message is set.
 * @property keyListener The key listener to be used with this component.
 * @property importantForAutofill The mode for determining whether this view is considered important
 *   for autofill.
 * @property autofillHints The hints that help an [android.service.autofill.AutofillService]
 *   determine how to autofill the view with the user's data.
 * @property disableAutofill If set to true, disables autofill for this text input by setting the
 *   underlying [EditText]'s autofillType to [android.view.View#AUTOFILL_TYPE_NONE].
 * @property movementMethod Used to set cursor positioning, scrolling and text selection
 *   functionality in EditText
 * @property textWatchers Used to register text watchers e.g. mentions detection.
 * @property selectionActionModeCallback If provided, this [ActionMode.Callback] will be used to
 *   create the ActionMode when text selection is initiated in this View.
 * @property insertionActionModeCallback If provided, this ActionMode.Callback will be used to
 *   create the [ActionMode] when text insertion is initiated in this View.
 * @property excludeFromIncrementalMount If the component should be excluded from Litho incremental
 *   mount.
 * @property textInputController The controller for invoking actions on the component. Use
 *   [useTextInputController] for creating an instance of the controller.
 * @property onTextChanged A callback that will be called when the text gets changed.
 * @property onTextPasted A callback that will be called when the text gets pasted.
 * @property onSelectionChanged A callback that will be called when the text selection changes.
 * @property onInputFocusChanged A callback that will be called when the input focus changes.
 * @property onKeyUp A callback that will be called when the key up event is triggered.
 * @property onKeyPreImeEvent A callback that allows to handle a key event before it is processed by
 *   any input method associated with the view hierarchy.
 * @property onEditorAction A callback that will be called when an action is invoked on the Editor.
 * @property onInputConnection A callback that will be called when the input connection is created.
 * @property style A style for the component.
 * @see [EditText]
 */
class ExperimentalTextInput(
    private val initialText: CharSequence = "",
    private val hint: CharSequence = "",
    private val inputBackground: Drawable? = UNSET_DRAWABLE,
    private val shadowRadius: Float = 0f,
    private val shadowDx: Float = 0f,
    private val shadowDy: Float = 0f,
    @ColorInt private val shadowColor: Int = Color.GRAY,
    private val textColorStateList: ColorStateList = ColorStateList.valueOf(Color.BLACK),
    private val hintColorStateList: ColorStateList = ColorStateList.valueOf(Color.LTGRAY),
    @ColorInt private val highlightColor: Int? = null,
    private val textSize: Int = TextComponentSpec.UNSET,
    private val typeface: Typeface = Typeface.DEFAULT,
    private val textAlignment: Int = View.TEXT_ALIGNMENT_GRAVITY,
    @GravityInt private val gravity: Int = Gravity.CENTER_VERTICAL or Gravity.START,
    private val editable: Boolean = true,
    private val cursorVisible: Boolean = true,
    private val inputType: Int = EditorInfo.TYPE_CLASS_TEXT,
    private val rawInputType: Int = EditorInfo.TYPE_NULL,
    private val imeOptions: Int = EditorInfo.IME_NULL,
    private val privateImeOptions: String? = null,
    private val inputFilters: List<@JvmSuppressWildcards InputFilter?>? = null,
    private val multiline: Boolean = false,
    private val ellipsize: TextUtils.TruncateAt? = null,
    private val minLines: Int = 1,
    private val maxLines: Int = Int.MAX_VALUE,
    private val cursorDrawableRes: Int = -1,
    private val error: CharSequence? = null,
    private val errorDrawable: Drawable? = null,
    private val keyListener: KeyListener? = null,
    private val importantForAutofill: Int = 0,
    private val autofillHints: Array<String?>? = null,
    private val disableAutofill: Boolean = false,
    private val movementMethod: MovementMethod = ArrowKeyMovementMethod.getInstance(),
    private val textWatchers: List<TextWatcher>? = null,
    private val selectionActionModeCallback: ActionMode.Callback? = null,
    private val insertionActionModeCallback: ActionMode.Callback? = null,
    private val excludeFromIncrementalMount: Boolean = false,
    private val textInputController: TextInputController? = null,
    private val onTextChanged: ((EditText, String) -> Unit)? = null,
    private val onTextPasted: ((EditText, String) -> Unit)? = null,
    private val onSelectionChanged: ((Int, Int) -> Unit)? = null,
    private val onInputFocusChanged: ((Boolean) -> Unit)? = null,
    private val onKeyUp: ((KeyEvent, Int) -> Boolean)? = null,
    private val onKeyPreImeEvent: ((KeyEvent, Int) -> Boolean)? = null,
    private val onEditorAction: ((TextView, KeyEvent?, Int) -> Boolean)? = null,
    private val onInputConnection: ((InputConnection?, EditorInfo) -> InputConnection?)? = null,
    private val style: Style? = null,
) : PrimitiveComponent() {
  override fun PrimitiveComponentScope.render(): LithoPrimitive {
    val mountedView = useState { AtomicReference<EditTextWithEventHandlers?>() }
    val savedText = useState { AtomicReference(initialText) }
    val textAndConstraintsForMeasure =
        useState<TextAndConstraints> { TextAndConstraints(initialText.toString(), null) }

    val resolvedHighlightColor = useCached {
      if (highlightColor != null) {
        highlightColor
      } else {
        val attrs = context.obtainStyledAttributes(intArrayOf(R.attr.textColorHighlight), 0)
        try {
          attrs.getColor(0, 0)
        } finally {
          attrs.recycle()
        }
      }
    }

    return LithoPrimitive(
        layoutBehavior =
            TextInputLayoutBehavior(
                hint = hint,
                inputBackground = inputBackground,
                shadowRadius = shadowRadius,
                shadowDx = shadowDx,
                shadowDy = shadowDy,
                shadowColor = shadowColor,
                textColorStateList = textColorStateList,
                hintColorStateList = hintColorStateList,
                highlightColor = resolvedHighlightColor,
                textSize = textSize,
                typeface = typeface,
                textAlignment = textAlignment,
                gravity = gravity,
                editable = editable,
                cursorVisible = cursorVisible,
                inputType = inputType,
                rawInputType = rawInputType,
                imeOptions = imeOptions,
                privateImeOptions = privateImeOptions,
                inputFilters = inputFilters,
                multiline = multiline,
                ellipsize = ellipsize,
                minLines = minLines,
                maxLines = maxLines,
                cursorDrawableRes = cursorDrawableRes,
                error = error,
                errorDrawable = errorDrawable,
                keyListener = keyListener,
                importantForAutofill = importantForAutofill,
                autofillHints = autofillHints,
                disableAutofill = disableAutofill,
                movementMethod = movementMethod,
                savedText = savedText.value,
                textAndConstraintsForMeasure = textAndConstraintsForMeasure,
            ),
        mountBehavior =
            MountBehavior(
                ViewAllocator { context ->
                  val editText = EditTextWithEventHandlers(context)
                  // Setting a custom editable factory so we can catch and rethrow crashes from
                  // SpannableStringBuilder#setSpan with additional information. This should cause
                  // no functional changes.
                  editText.setEditableFactory(SafeSetSpanEditableFactory())
                  editText
                }) {
                  shouldExcludeFromIncrementalMount = excludeFromIncrementalMount

                  // Controller
                  withDescription("text-input-controller") {
                    bind(textInputController, savedText.value, textAndConstraintsForMeasure) {
                        editText ->
                      textInputController?.bind(
                          editText, savedText.value, textAndConstraintsForMeasure)
                      onUnbind { textInputController?.unbind() }
                    }
                  }
                  // OnMount
                  withDescription("text-input-equivalent-mount") {
                    bind(
                        initialText,
                        hint,
                        shadowRadius,
                        shadowDx,
                        shadowDy,
                        shadowColor,
                        textColorStateList,
                        hintColorStateList,
                        resolvedHighlightColor,
                        textSize,
                        typeface,
                        textAlignment,
                        gravity,
                        editable,
                        cursorVisible,
                        inputType,
                        rawInputType,
                        keyListener,
                        imeOptions,
                        privateImeOptions,
                        InputFiltersComparator(inputFilters),
                        ellipsize,
                        multiline,
                        LineRangeComparator(minLines, maxLines, multiline),
                        cursorDrawableRes,
                        movementMethod,
                        error,
                        // Note, these are purposefully just comparing the containers, not the
                        // contents!
                        mountedView,
                        savedText,
                        InputBackgroundComparator(inputBackground),
                        excludeFromIncrementalMount,
                    ) { editText ->
                      mountedView.value.set(editText)

                      setParams(
                          editText,
                          hint,
                          getBackgroundOrDefault(androidContext, inputBackground),
                          shadowRadius,
                          shadowDx,
                          shadowDy,
                          shadowColor,
                          textColorStateList,
                          hintColorStateList,
                          resolvedHighlightColor,
                          textSize,
                          typeface,
                          textAlignment,
                          gravity,
                          editable,
                          cursorVisible,
                          inputType,
                          rawInputType,
                          keyListener,
                          imeOptions,
                          privateImeOptions,
                          inputFilters,
                          multiline,
                          ellipsize,
                          minLines,
                          maxLines,
                          cursorDrawableRes,
                          movementMethod,
                          // onMount happens:
                          // 1. After initState: savedText = initText.
                          // 2. After onUnmount: savedText preserved from underlying editText.
                          savedText.value.get(),
                          error,
                          errorDrawable,
                          false,
                          importantForAutofill,
                          autofillHints)
                      editText.setDisableAutofill(disableAutofill)
                      editText.setTextState(savedText.value)

                      onUnbind {
                        if (keyListener != null) {
                          editText.keyListener = null // Clear any KeyListener
                          editText.inputType =
                              TextInputSpec.inputType // Set the input type back to default.
                        }
                        editText.movementMethod = null
                        editText.setTextState(null)
                        editText.removeOnWindowFocusChangeListener()
                        editText.privateImeOptions = null
                        mountedView.value.set(null)
                      }
                    }
                  }

                  // OnBind
                  withDescription("text-input-equivalent-bind") {
                    bind(Any()) { editText ->
                      editText.attachWatchers(textWatchers)
                      editText.customSelectionActionModeCallback = selectionActionModeCallback
                      editText.customInsertionActionModeCallback = insertionActionModeCallback
                      editText.componentContext = context
                      editText.onTextChanged = onTextChanged
                      editText.onSelectionChanged = onSelectionChanged
                      editText.onInputFocusChanged = onInputFocusChanged
                      editText.onKeyUp = onKeyUp
                      editText.onKeyPreImeEvent = onKeyPreImeEvent
                      editText.onEditorAction = onEditorAction
                      editText.onInputConnection = onInputConnection
                      editText.onTextPasted = onTextPasted
                      editText.textAndConstraintsForMeasure = textAndConstraintsForMeasure

                      onUnbind {
                        editText.detachWatchers()
                        editText.componentContext = null
                        editText.onTextChanged = null
                        editText.onSelectionChanged = null
                        editText.onInputFocusChanged = null
                        editText.onKeyUp = null
                        editText.onKeyPreImeEvent = null
                        editText.onEditorAction = null
                        editText.onInputConnection = null
                        editText.customSelectionActionModeCallback = null
                        editText.customInsertionActionModeCallback = null
                        editText.onTextPasted = null
                        editText.textAndConstraintsForMeasure = null
                      }
                    }
                  }
                },
        style = style)
  }
}

internal class TextInputLayoutBehavior(
    private val hint: CharSequence,
    private val inputBackground: Drawable?,
    private val shadowRadius: Float,
    private val shadowDx: Float,
    private val shadowDy: Float,
    @ColorInt private val shadowColor: Int,
    private val textColorStateList: ColorStateList,
    private val hintColorStateList: ColorStateList,
    @ColorInt private val highlightColor: Int?,
    private val textSize: Int,
    private val typeface: Typeface,
    private val textAlignment: Int,
    @GravityInt private val gravity: Int,
    private val editable: Boolean,
    private val cursorVisible: Boolean,
    private val inputType: Int,
    private val rawInputType: Int,
    private val imeOptions: Int,
    private val privateImeOptions: String?,
    private val inputFilters: List<@JvmSuppressWildcards InputFilter?>?,
    private val multiline: Boolean,
    private val ellipsize: TextUtils.TruncateAt?,
    private val minLines: Int,
    private val maxLines: Int,
    private val cursorDrawableRes: Int,
    private val error: CharSequence?,
    private val errorDrawable: Drawable?,
    private val keyListener: KeyListener?,
    private val importantForAutofill: Int,
    private val autofillHints: Array<String?>?,
    private val disableAutofill: Boolean,
    private val movementMethod: MovementMethod,
    private val savedText: AtomicReference<CharSequence?>,
    // we're only reading it here in order to force remeasure if it gets updated
    private val textAndConstraintsForMeasure: State<TextAndConstraints>,
) : LayoutBehavior {
  override fun LayoutScope.layout(sizeConstraints: SizeConstraints): PrimitiveLayoutResult {
    // When input type has NO_SUGGESTIONS flag set then suggestion spans are removed when
    // setText is called. This causes that SpanWatcher is invoked and TextView listens to
    // those span changes. It caused a crash like T223197933. Here, we're removing
    // NO_SUGGESTIONS flag if it exists to prevent TextView from removing them and dispatching
    // SpanWatcher listeners. NO_SUGGESTIONS flag shouldn't affect measurement since
    // suggestions are displayed in a separate window.
    val inputTypeForMeasure = removeNoSuggestionsFlagIfExists(inputType)
    val rawInputTypeForMeasure = removeNoSuggestionsFlagIfExists(rawInputType)

    val forMeasure =
        createAndMeasureEditText(
            MeasureContext(androidContext),
            sizeConstraints,
            hint,
            inputBackground,
            shadowRadius,
            shadowDx,
            shadowDy,
            shadowColor,
            textColorStateList,
            hintColorStateList,
            highlightColor,
            textSize,
            typeface,
            textAlignment,
            gravity,
            editable,
            cursorVisible,
            inputTypeForMeasure,
            rawInputTypeForMeasure,
            keyListener,
            imeOptions,
            privateImeOptions,
            inputFilters,
            multiline,
            ellipsize,
            minLines,
            maxLines,
            cursorDrawableRes,
            error,
            errorDrawable,
            importantForAutofill,
            autofillHints,
            disableAutofill,
            // onMeasure happens:
            // 1. After initState before onMount: savedText = initText.
            // 2. After onMount before onUnmount: savedText preserved from underlying editText.
            savedText.get(),
            textAndConstraintsForMeasure)

    return PrimitiveLayoutResult(
        height = forMeasure.measuredHeight,
        width =
            if (!sizeConstraints.hasBoundedWidth) {
              // For width we always take all available space, or collapse to 0 if unbounded.
              0
            } else {
              min(sizeConstraints.maxWidth, forMeasure.measuredWidth)
            })
  }
}

private fun removeNoSuggestionsFlagIfExists(inputType: Int): Int {
  return if (inputType and EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS > 0) {
    inputType and EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS.inv()
  } else {
    inputType
  }
}

/**
 * Dummy drawable used for differentiating user-provided null background drawable from default
 * drawable of the spec
 */
@JvmField val UNSET_DRAWABLE: Drawable = TextInputComponentSpec.UNSET_DRAWABLE

/** UI thread only; used in OnMount. */
private val BackgroundPaddingRect = Rect()

/** UI thread only; used in OnMount. */
private val NO_FILTERS = arrayOfNulls<InputFilter>(0)

internal fun createAndMeasureEditText(
    context: Context,
    sizeConstraints: SizeConstraints,
    hint: CharSequence?,
    inputBackground: Drawable?,
    shadowRadius: Float,
    shadowDx: Float,
    shadowDy: Float,
    shadowColor: Int,
    textColorStateList: ColorStateList?,
    hintColorStateList: ColorStateList?,
    highlightColor: Int?,
    textSize: Int,
    typeface: Typeface?,
    textAlignment: Int,
    gravity: Int,
    editable: Boolean,
    cursorVisible: Boolean,
    inputType: Int,
    rawInputType: Int,
    keyListener: KeyListener?,
    imeOptions: Int,
    privateImeOptions: String?,
    inputFilters: List<InputFilter?>?,
    multiline: Boolean,
    ellipsize: TextUtils.TruncateAt?,
    minLines: Int,
    maxLines: Int,
    cursorDrawableRes: Int,
    error: CharSequence?,
    errorDrawable: Drawable?,
    importantForAutofill: Int,
    autofillHints: Array<String?>?,
    disableAutofill: Boolean,
    text: CharSequence?,
    textAndConstraintsForMeasure: State<TextAndConstraints>,
): EditText {
  // The height should be the measured height of EditText with relevant params
  val constraints = textAndConstraintsForMeasure.value.constraints
  val textToMeasure =
      if (sizeConstraints == constraints) {
        // If text contains Spans, we don't want it to be mutable for the measurement case
        textAndConstraintsForMeasure.value.text
      } else {
        text
      }

  val forMeasure = ForMeasureEditText(context)

  if (context is MeasureContext) {
    context.withInputMethodManagerDisabled {
      setParams(
          forMeasure,
          hint,
          getBackgroundOrDefault(
              context,
              if (inputBackground === UNSET_DRAWABLE) {
                forMeasure.background
              } else {
                inputBackground
              }),
          shadowRadius,
          shadowDx,
          shadowDy,
          shadowColor,
          textColorStateList,
          hintColorStateList,
          highlightColor,
          textSize,
          typeface,
          textAlignment,
          gravity,
          editable,
          cursorVisible,
          inputType,
          rawInputType,
          keyListener,
          imeOptions,
          privateImeOptions,
          inputFilters,
          multiline,
          ellipsize,
          minLines,
          maxLines,
          cursorDrawableRes,
          forMeasure.movementMethod,
          textToMeasure,
          error,
          errorDrawable,
          true,
          importantForAutofill,
          autofillHints)
    }
  } else {
    setParams(
        forMeasure,
        hint,
        getBackgroundOrDefault(
            context,
            if (inputBackground === UNSET_DRAWABLE) {
              forMeasure.background
            } else {
              inputBackground
            }),
        shadowRadius,
        shadowDx,
        shadowDy,
        shadowColor,
        textColorStateList,
        hintColorStateList,
        highlightColor,
        textSize,
        typeface,
        textAlignment,
        gravity,
        editable,
        cursorVisible,
        inputType,
        rawInputType,
        keyListener,
        imeOptions,
        privateImeOptions,
        inputFilters,
        multiline,
        ellipsize,
        minLines,
        maxLines,
        cursorDrawableRes,
        forMeasure.movementMethod,
        textToMeasure,
        error,
        errorDrawable,
        true,
        importantForAutofill,
        autofillHints)
  }
  forMeasure.setDisableAutofill(disableAutofill)
  forMeasure.measure(sizeConstraints.toWidthSpec(), sizeConstraints.toHeightSpec())
  return forMeasure
}

fun getBackgroundOrDefault(context: Context, specifiedBackground: Drawable?): Drawable? {
  if (specifiedBackground === UNSET_DRAWABLE) {
    val attrs = intArrayOf(R.attr.background)
    val a = context.obtainStyledAttributes(null, attrs, R.attr.editTextStyle, 0)
    val defaultBackground = a.getDrawable(0)
    a.recycle()
    return defaultBackground
  }

  return specifiedBackground
}

fun setParams(
    editText: EditText,
    hint: CharSequence?,
    background: Drawable?,
    shadowRadius: Float,
    shadowDx: Float,
    shadowDy: Float,
    shadowColor: Int,
    textColorStateList: ColorStateList?,
    hintColorStateList: ColorStateList?,
    highlightColor: Int?,
    textSize: Int,
    typeface: Typeface?,
    textAlignment: Int,
    gravity: Int,
    editable: Boolean,
    cursorVisible: Boolean,
    inputType: Int,
    rawInputType: Int,
    keyListener: KeyListener?,
    imeOptions: Int,
    privateImeOptions: String?,
    inputFilters: List<InputFilter?>?,
    multiline: Boolean,
    ellipsize: TextUtils.TruncateAt?,
    minLines: Int,
    maxLines: Int,
    cursorDrawableRes: Int,
    movementMethod: MovementMethod?,
    text: CharSequence?,
    error: CharSequence?,
    errorDrawable: Drawable?,
    isForMeasure: Boolean,
    importantForAutofill: Int,
    autofillHints: Array<String?>?
) {
  var inputTypeToSet = inputType
  if (textSize == TextComponentSpec.UNSET) {
    editText.setTextSize(
        TypedValue.COMPLEX_UNIT_SP, TextComponentSpec.DEFAULT_TEXT_SIZE_SP.toFloat())
  } else {
    editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize.toFloat())
  }

  if (multiline) {
    inputTypeToSet =
        inputTypeToSet or (EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE)
    editText.minLines = minLines
    editText.maxLines = maxLines
  } else {
    inputTypeToSet = inputTypeToSet and EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE.inv()
    editText.setLines(1)
  }

  if (!editable) {
    inputTypeToSet = EditorInfo.TYPE_NULL
  }

  if (rawInputType != EditorInfo.TYPE_NULL) {
    editText.setRawInputType(rawInputType)
  } else {
    // Only set inputType if rawInputType is not specified.
    setInputTypeAndKeyListenerIfChanged(editText, inputTypeToSet, keyListener)
  }

  // Needs to be set before the text so it would apply to the current text
  if (inputFilters != null) {
    editText.filters = inputFilters.toTypedArray<InputFilter?>()
  } else {
    editText.filters = NO_FILTERS
  }
  editText.hint = hint
  editText.background = background
  // From the docs for setBackground:
  // "If the background has padding, this View's padding is set to the background's padding.
  // However, when a background is removed, this View's padding isn't touched. If setting the
  // padding is desired, please use setPadding."
  if (background == null || !background.getPadding(BackgroundPaddingRect)) {
    editText.setPadding(0, 0, 0, 0)
  }
  editText.setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor)
  editText.setTypeface(typeface, Typeface.NORMAL)
  editText.gravity = gravity
  editText.imeOptions = imeOptions
  if (privateImeOptions != null) {
    editText.privateImeOptions = privateImeOptions
  }
  editText.isFocusable = editable
  editText.isFocusableInTouchMode = editable
  editText.isLongClickable = editable
  editText.isCursorVisible = cursorVisible
  editText.setTextColor(textColorStateList)
  editText.setHintTextColor(hintColorStateList)
  if (highlightColor != null) {
    editText.highlightColor = highlightColor
  }
  editText.movementMethod = movementMethod

  /**
   * Sets error state on the TextInput, which shows an error icon provided by errorDrawable and an
   * error message
   *
   * @param error Message that will be shown when error is not null and text input is in focused
   *   state
   * @param errorDrawable icon that signals an existing error and anchors a popover showing the
   *   errorMessage when component is focused.
   */
  editText.setError(error, errorDrawable)

  if (cursorDrawableRes != -1) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      editText.setTextCursorDrawable(cursorDrawableRes)
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

  editText.ellipsize = ellipsize
  editText.textAlignment = textAlignment
  if (text != null && !ObjectsCompat.equals(editText.text.toString(), text.toString())) {
    editText.setText(text)
    // Set the selection only when mounting because #setSelection does not affect measurement,
    // but it can mutate the span during measurement, potentially causing crashes.
    if (!isForMeasure) {
      editText.setSelection(editText.text.toString().length)
    }
  }

  Api26Utils.setAutoFillProps(editText, importantForAutofill, autofillHints)
}

private fun setInputTypeAndKeyListenerIfChanged(
    editText: EditText,
    inputType: Int,
    keyListener: KeyListener?
) {
  // Avoid redundant call to InputMethodManager#restartInput.
  if (inputType != editText.inputType) {
    editText.inputType = inputType
  }

  // Optionally Set KeyListener later to override the one set by the InputType
  if (keyListener != null && keyListener !== editText.keyListener) {
    editText.keyListener = keyListener
  }
}

internal class EditTextWithEventHandlers(context: Context?) :
    EditText(context), TextView.OnEditorActionListener {
  var onTextPasted: ((EditText, String) -> Unit)? = null
  var onTextChanged: ((EditText, String) -> Unit)? = null
  var onSelectionChanged: ((Int, Int) -> Unit)? = null
  var onInputFocusChanged: ((Boolean) -> Unit)? = null
  var onKeyUp: ((KeyEvent, Int) -> Boolean)? = null
  var onKeyPreImeEvent: ((KeyEvent, Int) -> Boolean)? = null
  var onEditorAction: ((TextView, KeyEvent?, Int) -> Boolean)? = null
  var onInputConnection: ((InputConnection?, EditorInfo) -> InputConnection?)? = null
  var componentContext: ComponentContext? = null
  var textAndConstraintsForMeasure: com.facebook.litho.State<TextAndConstraints>? = null
  private var textState: AtomicReference<CharSequence?>? = null
  private var textLineCount = UNMEASURED_LINE_COUNT
  private var textWatcher: TextWatcher? = null
  private var isSoftInputRequested = false

  private var disableAutofill = false
  private var isTextPasted = false

  private var lastWidthSpec: Int = -1
  private var lastHeightSpec: Int = -1

  private var onWindowFocusChangeListener: ViewTreeObserver.OnWindowFocusChangeListener? = null

  init {
    // Unfortunately we can't just override `void onEditorAction(int actionCode)` as that only
    // covers a subset of all cases where onEditorActionListener is invoked.
    this.setOnEditorActionListener(this)
  }

  override fun onTextChanged(text: CharSequence, start: Int, lengthBefore: Int, lengthAfter: Int) {
    super.onTextChanged(text, start, lengthBefore, lengthAfter)
    textState?.set(text)
    onTextChanged?.invoke(this@EditTextWithEventHandlers, text.toString())

    if (isTextPasted && onTextPasted != null) {
      onTextPasted?.invoke(this@EditTextWithEventHandlers, text.toString())
      isTextPasted = false
    }
    // Line count of changed text.
    val lineCount = lineCount
    if (this.textLineCount != UNMEASURED_LINE_COUNT &&
        (this.textLineCount != lineCount) &&
        (componentContext != null)) {
      val constraints =
          if (lastWidthSpec != -1 && lastHeightSpec != -1) {
            SizeConstraints.fromMeasureSpecs(lastWidthSpec, lastHeightSpec)
          } else {
            null
          }
      textAndConstraintsForMeasure?.update(TextAndConstraints(text.toString(), constraints))
    }
  }

  override fun onTextContextMenuItem(id: Int): Boolean {
    if (id == R.id.paste && onTextPasted != null) {
      isTextPasted = true
    }
    return super.onTextContextMenuItem(id)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    lastWidthSpec = widthMeasureSpec
    lastHeightSpec = heightMeasureSpec
    // Line count of the current text.
    textLineCount = lineCount
  }

  override fun onSelectionChanged(selStart: Int, selEnd: Int) {
    super.onSelectionChanged(selStart, selEnd)
    onSelectionChanged?.invoke(selStart, selEnd)
  }

  override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
    super.onFocusChanged(focused, direction, previouslyFocusedRect)
    onInputFocusChanged?.invoke(focused)
  }

  override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
    if (onKeyUp != null) {
      onKeyUp?.invoke(event, keyCode) ?: super.onKeyUp(keyCode, event)
    }
    return super.onKeyUp(keyCode, event)
  }

  override fun onKeyPreIme(keyCode: Int, event: KeyEvent): Boolean {
    if (onKeyPreImeEvent != null) {
      return onKeyPreImeEvent?.invoke(event, keyCode) ?: super.onKeyPreIme(keyCode, event)
    }
    return super.onKeyPreIme(keyCode, event)
  }

  override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent?): Boolean {
    return onEditorAction?.invoke(v, event, actionId) ?: false
  }

  override fun onCreateInputConnection(editorInfo: EditorInfo): InputConnection? {
    val inputConnection = super.onCreateInputConnection(editorInfo)
    return onInputConnection?.invoke(inputConnection, editorInfo) ?: inputConnection
  }

  override fun getAutofillType(): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && this.disableAutofill) {
      AUTOFILL_TYPE_NONE
    } else {
      super.getAutofillType()
    }
  }

  fun setDisableAutofill(value: Boolean) {
    this.disableAutofill = value
  }

  /** Sets reference to keep current text up to date. */
  fun setTextState(savedText: AtomicReference<CharSequence?>?) {
    textState = savedText
  }

  fun attachWatchers(textWatchers: List<TextWatcher?>?) {
    if (textWatchers == null) {
      return
    }

    val nonNullTextWatchers = textWatchers.filterNotNull()

    if (nonNullTextWatchers.isNotEmpty()) {
      textWatcher =
          if (nonNullTextWatchers.size == 1) nonNullTextWatchers[0]
          else CompositeTextWatcher(nonNullTextWatchers)
      addTextChangedListener(textWatcher)
    }
  }

  fun detachWatchers() {
    if (textWatcher != null) {
      removeTextChangedListener(textWatcher)
      textWatcher = null
    }
  }

  fun removeOnWindowFocusChangeListener() {
    if (onWindowFocusChangeListener != null) {
      viewTreeObserver.removeOnWindowFocusChangeListener(onWindowFocusChangeListener)
      onWindowFocusChangeListener = null
    }
  }

  fun setSoftInputVisibility(visible: Boolean) {
    val imm =
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager ?: return

    if (visible) {
      showKeyboardWhenWindowIsFocused(imm)
    } else {
      hideKeyboard(imm)
    }
  }

  private fun showKeyboardWhenWindowIsFocused(imm: InputMethodManager) {
    if (hasWindowFocus()) {
      showKeyboard(imm)
    } else {
      // We need to wait until the window gets focus.
      // https://developer.android.com/develop/ui/views/touch-and-input/keyboard-input/visibility#ShowReliably
      onWindowFocusChangeListener =
          ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (hasFocus) {
              showKeyboard(imm)
              removeOnWindowFocusChangeListener()
            }
          }
      viewTreeObserver.addOnWindowFocusChangeListener(onWindowFocusChangeListener)
    }
  }

  private fun showKeyboard(imm: InputMethodManager) {
    if (!isFocused) {
      return
    }

    if (imm.isActive(this)) {
      imm.showSoftInput(this, 0)
      isSoftInputRequested = false
    } else {
      // Unfortunately, IMM and requesting focus has race conditions and there are cases where
      // even though the focus request went through, IMM hasn't been updated yet (thus the
      // isActive check). Posting a Runnable gives time for the Runnable the IMM Binder posts
      // to run first and update the IMM.
      post {
        if (isSoftInputRequested) {
          imm.showSoftInput(this@EditTextWithEventHandlers, 0)
        }
        isSoftInputRequested = false
      }
      isSoftInputRequested = true
    }
  }

  private fun hideKeyboard(imm: InputMethodManager) {
    imm.hideSoftInputFromWindow(windowToken, 0)
    isSoftInputRequested = false
  }

  fun performAccessibilityFocus() {
    try {
      ViewCompat.performAccessibilityAction(
          this, AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null)
    } catch (npe: NullPointerException) {
      // do nothing
    }
  }

  internal class CompositeTextWatcher(textWatchers: List<TextWatcher>) : TextWatcher {
    private val textWatchers: List<TextWatcher> = ArrayList(textWatchers)

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
      for (w in textWatchers) {
        w.beforeTextChanged(s, start, count, after)
      }
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
      for (w in textWatchers) {
        w.onTextChanged(s, start, before, count)
      }
    }

    override fun afterTextChanged(editable: Editable) {
      for (w in textWatchers) {
        w.afterTextChanged(editable)
      }
    }
  }

  companion object {
    private const val UNMEASURED_LINE_COUNT = -1
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
internal class ForMeasureEditText(context: Context?) : EditText(context) {
  private var disableAutofill = false

  // This view is not intended to be drawn and invalidated
  override fun invalidate(): Unit = Unit

  override fun setBackground(background: Drawable?) {
    background?.mutate()
    super.setBackground(background)
  }

  override fun getAutofillType(): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && this.disableAutofill) {
      AUTOFILL_TYPE_NONE
    } else {
      super.getAutofillType()
    }
  }

  fun setDisableAutofill(value: Boolean) {
    this.disableAutofill = value
  }
}

private object Api26Utils {
  fun setAutoFillProps(
      editText: EditText,
      importantForAutofill: Int,
      autofillHints: Array<String?>?
  ) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      editText.importantForAutofill = importantForAutofill
      if (autofillHints != null) {
        editText.setAutofillHints(*autofillHints)
      } else {
        editText.setAutofillHints(null)
      }
    }
  }
}

private class SafeSetSpanEditableFactory : Editable.Factory() {
  override fun newEditable(source: CharSequence): Editable {
    return object : SpannableStringBuilder(source) {
      override fun setSpan(what: Any?, start: Int, end: Int, flags: Int) {
        /*
          Fix for a crash in the Android Framework when deleting a character while the spell checker popup shows.
          Prevent setting a SuggestionRangeSpan with end past the Editable length using Math.min
          (https://issuetracker.google.com/issues/314288203)
        */
        try {
          val spanEnd = if (shouldUseSafeSpanEnd(what)) min(end, length) else end
          super.setSpan(what, start, spanEnd, flags)
        } catch (e: IndexOutOfBoundsException) {
          /*
           Catching and rethrowing IndexOutOfBoundsExceptions with additional info. One known source
           of this crash is when using spell checker exceeds EditText maxLength
           (https://issuetracker.google.com/issues/36944935)
          */
          throw IndexOutOfBoundsException(
              String.format(
                  "%s | span=%s | flags=%d", e.message, what?.javaClass ?: "Unknown", flags))
        }
      }

      private fun shouldUseSafeSpanEnd(span: Any?): Boolean {
        return (SuggestionRangeSpanApi33Util.isSuggestionRangeSpan(span) &&
            ComponentsConfiguration.useSafeSpanEndInTextInputSpec)
      }
    }
  }
}

private object SuggestionRangeSpanApi33Util {
  private const val SUGGESTION_RANGE_SPAN_CLASS_NAME = "android.text.style.SuggestionRangeSpan"

  // SuggestionRangeSpan was made public in API 33, so check class name for lower API levels
  fun isSuggestionRangeSpan(span: Any?): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      span is SuggestionRangeSpan
    } else {
      span != null && span.javaClass.name == SUGGESTION_RANGE_SPAN_CLASS_NAME
    }
  }
}

@Hook
fun ComponentScope.useTextInputController(): TextInputController {
  return useCached { TextInputController() }
}

class TextInputController internal constructor() {
  private var editText: EditTextWithEventHandlers? = null
  private var savedText: AtomicReference<CharSequence?>? = null
  private var createAndMeasureEditText: com.facebook.litho.State<TextAndConstraints>? = null

  internal fun bind(
      editText: EditTextWithEventHandlers,
      savedText: AtomicReference<CharSequence?>,
      createAndMeasureEditText: com.facebook.litho.State<TextAndConstraints>?,
  ) {
    this.editText = editText
    this.savedText = savedText
    this.createAndMeasureEditText = createAndMeasureEditText
  }

  internal fun unbind() {
    editText = null
  }

  fun requestFocus() {
    val view = editText
    if (view != null) {
      if (view.requestFocus()) {
        view.setSoftInputVisibility(true)
        // Force request of accessibility focus because in some cases if something else is
        // requesting accessibility focus, it can lead to race condition with taking the focus from
        // view.requestFocus and input will not be highlighted by TalkBack
        view.performAccessibilityFocus()
      }
    }
  }

  fun clearFocus() {
    val view = editText
    if (view != null) {
      view.clearFocus()
      view.setSoftInputVisibility(false)
    }
  }

  fun showCursor() {
    val view = editText
    if (view != null) {
      view.isCursorVisible = true
    }
  }

  fun hideCursor() {
    val view = editText
    if (view != null) {
      view.isCursorVisible = false
    }
  }

  fun getText(): CharSequence? {
    val view = editText
    return if (view == null) savedText?.get() else view.text
  }

  fun getLineCount(): Int? {
    return editText?.lineCount
  }

  fun setText(text: CharSequence?) {
    val shouldRemeasure = setTextEditText(savedText, text)
    if (shouldRemeasure) {
      remeasureForUpdatedTextSync()
    }
  }

  fun replaceText(text: CharSequence?, startIndex: Int, endIndex: Int, skipSelection: Boolean) {
    val view = editText
    val editable = view?.text
    if (editable != null) {
      editable.replace(startIndex, endIndex, text)
      if (!skipSelection) {
        view.setSelection(if (text != null) (startIndex + text.length) else startIndex)
      }
      return
    }

    val currentSavedText = savedText?.get()
    savedText?.set(
        if (currentSavedText == null) text
        else
            SpannableStringBuilder()
                .append(currentSavedText.subSequence(0, startIndex))
                .append(text)
                .append(currentSavedText.subSequence(endIndex, currentSavedText.length)))

    remeasureForUpdatedTextSync()
  }

  fun dispatchKey(keyEvent: KeyEvent?) {
    editText?.dispatchKeyEvent(keyEvent)
  }

  fun setSelection(start: Int, end: Int) {
    editText?.setSelection(start, if (end < start) start else end)
  }

  fun setSpan(what: Any?, start: Int, end: Int, flags: Int) {
    assertMainThread()

    val view = editText
    val editable = view?.text
    editable?.setSpan(what, start, end, flags)
  }

  fun removeSpan(what: Any?) {
    assertMainThread()

    val view = editText
    val editable = view?.text
    editable?.removeSpan(what)
  }

  fun getSpanStart(what: Any?): Int {
    assertMainThread()

    val view = editText
    val editable = view?.text
    if (editable != null) {
      return editable.getSpanStart(what)
    }

    return -1
  }

  private fun setTextEditText(
      savedText: AtomicReference<CharSequence?>?,
      text: CharSequence?
  ): Boolean {
    assertMainThread()

    val view = editText
    if (view == null) {
      savedText?.set(text)
      return true
    }

    // If line count changes state update will be triggered by view
    view.setText(text)
    val editable = view.text
    val length = editable?.length ?: 0
    view.setSelection(length)
    return false
  }

  private fun remeasureForUpdatedTextSync() {
    createAndMeasureEditText?.updateSync(TextAndConstraints(savedText?.get().toString(), null))
  }
}

/** A class that exists only to have a custom compare logic for minLines and maxLines. */
private class LineRangeComparator(
    private val minLines: Int,
    private val maxLines: Int,
    private val multiline: Boolean
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as LineRangeComparator

    // Minimum and maximum line count should only get checked if multiline is set
    if (multiline || other.multiline) {
      if (minLines != other.minLines) {
        return false
      }
      if (maxLines != other.maxLines) {
        return false
      }
    }

    return true
  }

  override fun hashCode(): Int {
    var result = minLines
    result = 31 * result + maxLines
    result = 31 * result + multiline.hashCode()
    return result
  }
}

/** A class that exists only to have a custom compare logic for inputBackground. */
private class InputBackgroundComparator(private val inputBackground: Drawable?) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as InputBackgroundComparator

    // Save the nastiest for last: trying to diff drawables.
    val previousBackground = inputBackground
    val nextBackground = other.inputBackground

    if (previousBackground == null && nextBackground != null) {
      return false
    } else if (previousBackground != null && nextBackground == null) {
      return false
    } else if (previousBackground != null && nextBackground != null) {
      if (previousBackground is ColorDrawable && nextBackground is ColorDrawable) {
        // This doesn't account for tint list/mode (no way to get that information)
        // and doesn't account for color filter (fine since ColorDrawable ignores it anyway).
        if (previousBackground.color != nextBackground.color) {
          return false
        }
      } else {
        // The best we can do here is compare getConstantState. This can result in spurious updates;
        // they might be different objects representing the same drawable. But it's the best we can
        // do without actually comparing bitmaps (which is too expensive).
        if (previousBackground.constantState != nextBackground.constantState) {
          return false
        }
      }
    }

    return true
  }

  override fun hashCode(): Int {
    return inputBackground?.hashCode() ?: 0
  }
}

/** A class that exists only to have a custom compare logic for inputFilters. */
private class InputFiltersComparator(
    private val inputFilters: List<@JvmSuppressWildcards InputFilter?>?
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as InputFiltersComparator

    val a = inputFilters
    val b = other.inputFilters
    /** LengthFilter and AllCaps do not implement isEqual. Correct for the deficiency. */
    if (a == null && b == null) {
      return true
    }
    if (a == null || b == null) {
      return false
    }
    if (a.size != b.size) {
      return false
    }
    for (i in a.indices) {
      val fa = a[i]
      val fb = b[i]
      if (fa is InputFilter.AllCaps && fb is InputFilter.AllCaps) {
        continue // equal, AllCaps has no configuration
      }
      if (fa is InputFilter.LengthFilter && fb is InputFilter.LengthFilter) {
        if (fa.max != fb.max) {
          return false
        }
        continue // equal, same max
      }

      // Best we can do in this case is call equals().
      if (!ObjectsCompat.equals(fa, fb)) {
        return false
      }
    }
    return true
  }

  override fun hashCode(): Int {
    return inputFilters?.hashCode() ?: 0
  }
}

/**
 * A custom context that is used only during TextInput component measurement.
 *
 * This is done to fix a crash during TextInput measurement. It seems like it happens when
 * setInputTypeAndKeyListenerIfChanged method is called which tries to hide the keyboard and this
 * affects the window size which results in scheduling a layout pass, but before that happens there
 * is a main thread assertion but the measurement happens on background thread.
 *
 * With this custom Context subclass, we're allowing to disable returning InputMethodManager for a
 * given block of code. With this we hope to fail this check:
 * https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/core/java/android/widget/TextView.java;l=7817?q=setInputType
 * and avoid keyboard hide during measurement.
 */
private class MeasureContext(ctx: Context) : ContextWrapper(ctx) {

  private var inputMethodManagerDisabled: Boolean = false

  override fun getSystemServiceName(serviceClass: Class<*>): String? =
      if (inputMethodManagerDisabled && serviceClass == InputMethodManager::class.java) {
        null
      } else {
        super.getSystemServiceName(serviceClass)
      }

  inline fun withInputMethodManagerDisabled(block: () -> Unit) {
    try {
      inputMethodManagerDisabled = true
      block()
    } finally {
      inputMethodManagerDisabled = false
    }
  }
}

@DataClassGenerate
internal data class TextAndConstraints(val text: CharSequence, val constraints: SizeConstraints?)
