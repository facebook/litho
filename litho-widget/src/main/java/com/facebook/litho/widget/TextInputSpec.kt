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

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.InputFilter
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.ArrowKeyMovementMethod
import android.text.method.KeyListener
import android.text.method.MovementMethod
import android.view.ActionMode
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.EditText
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.Handle
import com.facebook.litho.StateValue
import com.facebook.litho.annotations.CachedValue
import com.facebook.litho.annotations.ExcuseMySpec
import com.facebook.litho.annotations.FromTrigger
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCalculateCachedValue
import com.facebook.litho.annotations.OnCreateInitialState
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnTrigger
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.PropDefault
import com.facebook.litho.annotations.Reason
import com.facebook.litho.annotations.ResType
import com.facebook.litho.annotations.State
import com.facebook.litho.config.ComponentsConfiguration

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
 * private static final InputFilter lenFilter = new InputFilter.LengthFilter(maxLength);
 *
 * TextInput.create(c)
 *   .initialText(text)
 *   .textColorStateList(ColorStateList.valueOf(color))
 *   .multiline(true)
 *   .inputFilter(lenFilter)
 *   .backgroundColor(Color.TRANSPARENT)
 *   .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
 *   .build();
 * ```
 *
 * @see [EditText]
 * @prop initialText Initial text to display. If set, the value is set on the EditText exactly once:
 *   on initial mount. From then on, the EditText's text property is not modified.
 * @prop hint Hint text to display.
 * @prop inputBackground The background of the EditText itself; this is subtly distinct from the
 *   Litho background prop. The padding of the inputBackground drawable will be applied to the
 *   EditText itself, insetting the cursor and text field.
 * @prop shadowRadius Blur radius of the shadow.
 * @prop shadowDx Horizontal offset of the shadow.
 * @prop shadowDy Vertical offset of the shadow.
 * @prop shadowColor Color for the shadow underneath the text.
 * @prop cursorDrawableRes Drawable to set as an edit text cursor.
 * @prop textColorStateList ColorStateList of the text.
 * @prop hintTextColorStateList ColorStateList of the hint text.
 * @prop highlightColor Color for selected text.
 * @prop textSize Size of the text.
 * @prop typeface Typeface for the text.
 * @prop textAlignment Alignment of the text within its container. This only has effect on API level
 *   17 and above; it's up to you to handle earlier API levels by adjusting gravity.
 * @prop gravity Gravity for the text within its container.
 * @prop editable If set, allows the text to be editable.
 * @prop cursorVisible Set whether the cursor is visible. The default is true.
 * @prop inputType Type of data being placed in a text field, used to help an input method decide
 *   how to let the user enter text. To add multiline use multiline(true) method.
 * @prop rawInputType Type of data being placed in a text field. Directly changes the content type
 *   integer of the text view, without modifying any other state. This prop will override inputType
 *   if both are provided.
 * @prop imeOptions Type of data in the text field, reported to an IME when it has focus.
 * @prop disableAutofill If set to true, disables autofill for this text input by setting the
 *   underlying [EditText]'s autofillType to [android.view.View#AUTOFILL_TYPE_NONE].
 * @prop inputFilters Used to filter the input to e.g. a max character count.
 * @prop multiline If set to true, type of the input will be changed to multiline TEXT. Because
 *   passwords or numbers couldn't be multiline by definition.
 * @prop ellipsize If set, specifies the position of the text to be ellipsized. See
 *   [android documentation](https://developer.android.com/reference/android/widget/TextView.html#setEllipsize(android.text.TextUtils.TruncateAt))
 *   for behavior description.
 * @prop minLines Minimum number of lines to show.
 * @prop maxLines Maximum number of lines to show.
 * @prop textWatchers Used to register text watchers e.g. mentions detection.
 * @prop movementMethod Used to set cursor positioning, scrolling and text selection functionality
 *   in EditText
 * @prop error Sets the right-hand compound drawable of the TextView to the "error" icon and sets an
 *   error message that will be displayed in a popup when the TextView has focus. The icon and error
 *   message will be reset to null when any key events cause changes to the TextView's text. If the
 *   error is null, the error message and icon will be cleared. See
 *   https://developer.android.com/reference/android/widget/TextView.html#setError for more details.
 * @prop errorDrawable Will show along with the error message when a message is set.
 */
@ExcuseMySpec(reason = Reason.J2K_CONVERSION)
@LayoutSpec(
    events =
        [
            TextChangedEvent::class,
            SelectionChangedEvent::class,
            InputFocusChangedEvent::class,
            KeyUpEvent::class,
            KeyPreImeEvent::class,
            EditorActionEvent::class,
            SetTextEvent::class,
            InputConnectionEvent::class,
            TextPastedEvent::class])
internal object TextInputSpec {

  @JvmField
  @PropDefault
  val textColorStateList: ColorStateList = ColorStateList.valueOf(Color.BLACK)

  @JvmField
  @PropDefault
  val hintColorStateList: ColorStateList = ColorStateList.valueOf(Color.LTGRAY)

  @JvmField @PropDefault val hint: CharSequence = ""
  @JvmField @PropDefault val initialText: CharSequence = ""
  @JvmField @PropDefault val shadowColor: Int = Color.GRAY
  @JvmField @PropDefault val textSize: Int = TextComponentSpec.UNSET
  @JvmField @PropDefault val inputBackground: Drawable = TextInputComponentSpec.UNSET_DRAWABLE
  @JvmField @PropDefault val typeface: Typeface = Typeface.DEFAULT
  @JvmField @PropDefault val textAlignment: Int = View.TEXT_ALIGNMENT_GRAVITY
  @JvmField @PropDefault val gravity: Int = Gravity.CENTER_VERTICAL or Gravity.START
  @JvmField @PropDefault val editable: Boolean = true
  @JvmField @PropDefault val cursorVisible: Boolean = true
  @JvmField @PropDefault val inputType: Int = EditorInfo.TYPE_CLASS_TEXT
  @JvmField @PropDefault val rawInputType: Int = EditorInfo.TYPE_NULL
  @JvmField @PropDefault val imeOptions: Int = EditorInfo.IME_NULL
  @JvmField @PropDefault val cursorDrawableRes: Int = -1
  @JvmField @PropDefault val multiline: Boolean = false
  @JvmField @PropDefault val minLines: Int = 1
  @JvmField @PropDefault val maxLines: Int = Int.MAX_VALUE
  @JvmField @PropDefault val importantForAutofill: Int = 0
  @JvmField @PropDefault val disableAutofill: Boolean = false
  @JvmField @PropDefault val movementMethod: MovementMethod = ArrowKeyMovementMethod.getInstance()
  @JvmField @PropDefault val shouldExcludeFromIncrementalMount: Boolean = false

  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @Prop(optional = true, resType = ResType.STRING) initialText: CharSequence?,
      @Prop(optional = true, resType = ResType.STRING) hint: CharSequence?,
      @Prop(optional = true, resType = ResType.DRAWABLE) inputBackground: Drawable?,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) shadowRadius: Float,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) shadowDx: Float,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) shadowDy: Float,
      @Prop(optional = true, resType = ResType.COLOR) shadowColor: Int,
      @Prop(optional = true) textColorStateList: ColorStateList,
      @Prop(optional = true) hintColorStateList: ColorStateList,
      @Prop(optional = true, resType = ResType.COLOR) highlightColor: Int?,
      @Prop(optional = true, resType = ResType.DIMEN_TEXT) textSize: Int,
      @Prop(optional = true) typeface: Typeface?,
      @Prop(optional = true) textAlignment: Int,
      @Prop(optional = true) gravity: Int,
      @Prop(optional = true) editable: Boolean,
      @Prop(optional = true) cursorVisible: Boolean,
      @Prop(optional = true) inputType: Int,
      @Prop(optional = true) rawInputType: Int,
      @Prop(optional = true) imeOptions: Int,
      @Prop(optional = true) privateImeOptions: String?,
      @Prop(optional = true, varArg = "inputFilter")
      inputFilters: List<@JvmSuppressWildcards InputFilter?>?,
      @Prop(optional = true) multiline: Boolean,
      @Prop(optional = true) ellipsize: TextUtils.TruncateAt?,
      @Prop(optional = true) minLines: Int,
      @Prop(optional = true) maxLines: Int,
      @Prop(optional = true) cursorDrawableRes: Int,
      @Prop(optional = true, resType = ResType.STRING) error: CharSequence?,
      @Prop(optional = true, resType = ResType.DRAWABLE) errorDrawable: Drawable?,
      @Prop(optional = true) keyListener: KeyListener?,
      @Prop(optional = true) importantForAutofill: Int,
      @Prop(optional = true) autofillHints: Array<String?>?,
      @Prop(optional = true) disableAutofill: Boolean,
      @Prop(optional = true) movementMethod: MovementMethod,
      @Prop(optional = true, varArg = "textWatcher") textWatchers: List<TextWatcher>?,
      @Prop(optional = true) selectionActionModeCallback: ActionMode.Callback?,
      @Prop(optional = true) insertionActionModeCallback: ActionMode.Callback?,
      @Prop(optional = true) shouldExcludeFromIncrementalMount: Boolean,
      @State textInputHandle: Handle,
      @CachedValue textInputController: TextInputController?,
  ): Component {
    return if (ComponentsConfiguration.usePrimitiveTextInput) {
      ExperimentalTextInput(
          initialText = initialText ?: "",
          hint = hint ?: "",
          inputBackground = inputBackground,
          shadowRadius = shadowRadius,
          shadowDx = shadowDx,
          shadowDy = shadowDy,
          shadowColor = shadowColor,
          textColorStateList = textColorStateList,
          hintColorStateList = hintColorStateList,
          highlightColor = highlightColor,
          textSize = textSize,
          typeface = typeface ?: Typeface.DEFAULT,
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
          textWatchers = textWatchers,
          selectionActionModeCallback = selectionActionModeCallback,
          insertionActionModeCallback = insertionActionModeCallback,
          excludeFromIncrementalMount = shouldExcludeFromIncrementalMount,
          textInputController = textInputController,
          onTextChanged =
              TextInput.getTextChangedEventHandler(c)?.let { handler ->
                { editText, text ->
                  val event = TextChangedEvent()
                  event.view = editText
                  event.text = text
                  handler.dispatchEvent(event)
                }
              },
          onTextPasted =
              TextInput.getTextPastedEventHandler(c)?.let { handler ->
                { editText, text ->
                  val event = TextPastedEvent()
                  event.view = editText
                  event.text = text
                  handler.dispatchEvent(event)
                }
              },
          onSelectionChanged =
              TextInput.getSelectionChangedEventHandler(c)?.let { handler ->
                { start, end ->
                  val event = SelectionChangedEvent()
                  event.start = start
                  event.end = end
                  handler.dispatchEvent(event)
                }
              },
          onInputFocusChanged =
              TextInput.getInputFocusChangedEventHandler(c)?.let { handler ->
                { focused ->
                  val event = InputFocusChangedEvent()
                  event.focused = focused
                  handler.dispatchEvent(event)
                }
              },
          onKeyUp =
              TextInput.getKeyUpEventHandler(c)?.let { handler ->
                { keyEvent, keyCode ->
                  val event = KeyUpEvent()
                  event.keyEvent = keyEvent
                  event.keyCode = keyCode
                  handler.dispatchEvent(event) as Boolean
                }
              },
          onKeyPreImeEvent =
              TextInput.getKeyPreImeEventHandler(c)?.let { handler ->
                { keyEvent, keyCode ->
                  val event = KeyPreImeEvent()
                  event.keyEvent = keyEvent
                  event.keyCode = keyCode
                  handler.dispatchEvent(event) as Boolean
                }
              },
          onEditorAction =
              TextInput.getEditorActionEventHandler(c)?.let { handler ->
                { textView, keyEvent, actionId ->
                  val event = EditorActionEvent()
                  event.view = textView
                  event.event = keyEvent
                  event.actionId = actionId
                  handler.dispatchEvent(event) as Boolean
                }
              },
          onInputConnection =
              TextInput.getInputConnectionEventHandler(c)?.let { handler ->
                { inputConnection, editorInfo ->
                  val event = InputConnectionEvent()
                  event.inputConnection = inputConnection
                  event.editorInfo = editorInfo
                  handler.dispatchEvent(event) as? InputConnection
                }
              },
          style = null,
      )
    } else {
      TextInputComponent.create(c)
          .initialText(initialText)
          .hint(hint)
          .inputBackground(inputBackground)
          .shadowRadiusPx(shadowRadius)
          .shadowDxPx(shadowDx)
          .shadowDyPx(shadowDy)
          .shadowColor(shadowColor)
          .textColorStateList(textColorStateList)
          .hintColorStateList(hintColorStateList)
          .highlightColor(highlightColor)
          .textSizePx(textSize)
          .typeface(typeface)
          .textAlignment(textAlignment)
          .gravity(gravity)
          .editable(editable)
          .cursorVisible(cursorVisible)
          .inputType(inputType)
          .rawInputType(rawInputType)
          .imeOptions(imeOptions)
          .privateImeOptions(privateImeOptions)
          .inputFilters(inputFilters)
          .multiline(multiline)
          .ellipsize(ellipsize)
          .minLines(minLines)
          .maxLines(maxLines)
          .cursorDrawableRes(cursorDrawableRes)
          .error(error)
          .errorDrawable(errorDrawable)
          .keyListener(keyListener)
          .importantForAutofill(importantForAutofill)
          .autofillHints(autofillHints)
          .disableAutofill(disableAutofill)
          .movementMethod(movementMethod)
          .textWatchers(textWatchers)
          .selectionActionModeCallback(selectionActionModeCallback)
          .insertionActionModeCallback(insertionActionModeCallback)
          .shouldExcludeFromIncrementalMount(shouldExcludeFromIncrementalMount)
          .handle(textInputHandle)
          .selectionChangedEventHandler(TextInput.getSelectionChangedEventHandler(c))
          .inputFocusChangedEventHandler(TextInput.getInputFocusChangedEventHandler(c))
          .keyUpEventHandler(TextInput.getKeyUpEventHandler(c))
          .keyPreImeEventHandler(TextInput.getKeyPreImeEventHandler(c))
          .editorActionEventHandler(TextInput.getEditorActionEventHandler(c))
          .setTextEventHandler(TextInput.getSetTextEventHandler(c))
          .inputConnectionEventHandler(TextInput.getInputConnectionEventHandler(c))
          .textPastedEventHandler(TextInput.getTextPastedEventHandler(c))
          .textChangedEventHandler(TextInput.getTextChangedEventHandler(c))
          .build()
    }
  }

  @OnCreateInitialState
  fun onCreateInitialState(c: ComponentContext?, textInputHandle: StateValue<Handle>) {
    textInputHandle.set(Handle())
  }

  @JvmStatic
  @OnCalculateCachedValue(name = "textInputController")
  internal fun onCalculateTextInputController(
      c: ComponentContext,
  ): TextInputController? {
    return if (ComponentsConfiguration.usePrimitiveTextInput) {
      TextInputController()
    } else {
      null
    }
  }

  @JvmStatic
  @OnTrigger(RequestFocusEvent::class)
  fun requestFocus(
      c: ComponentContext,
      @State textInputHandle: Handle,
      @CachedValue textInputController: TextInputController?,
  ) {
    if (ComponentsConfiguration.usePrimitiveTextInput) {
      textInputController?.requestFocus()
    } else {
      TextInputComponent.requestFocus(c, textInputHandle)
    }
  }

  @JvmStatic
  @OnTrigger(ClearFocusEvent::class)
  fun clearFocus(
      c: ComponentContext,
      @State textInputHandle: Handle,
      @CachedValue textInputController: TextInputController?,
  ) {
    if (ComponentsConfiguration.usePrimitiveTextInput) {
      textInputController?.clearFocus()
    } else {
      TextInputComponent.clearFocus(c, textInputHandle)
    }
  }

  @OnTrigger(ShowCursorEvent::class)
  fun showCursor(
      c: ComponentContext,
      @State textInputHandle: Handle,
      @CachedValue textInputController: TextInputController?,
  ) {
    if (ComponentsConfiguration.usePrimitiveTextInput) {
      textInputController?.showCursor()
    } else {
      TextInputComponent.showCursor(c, textInputHandle)
    }
  }

  @OnTrigger(HideCursorEvent::class)
  fun hideCursor(
      c: ComponentContext,
      @State textInputHandle: Handle,
      @CachedValue textInputController: TextInputController?,
  ) {
    if (ComponentsConfiguration.usePrimitiveTextInput) {
      textInputController?.hideCursor()
    } else {
      TextInputComponent.hideCursor(c, textInputHandle)
    }
  }

  @JvmStatic
  @OnTrigger(GetTextEvent::class)
  fun getText(
      c: ComponentContext,
      @State textInputHandle: Handle,
      @CachedValue textInputController: TextInputController?,
  ): CharSequence? {
    return if (ComponentsConfiguration.usePrimitiveTextInput) {
      textInputController?.getText()
    } else {
      TextInputComponent.getText(c, textInputHandle)
    }
  }

  @JvmStatic
  @OnTrigger(GetLineCountEvent::class)
  fun getLineCount(
      c: ComponentContext,
      @State textInputHandle: Handle,
      @CachedValue textInputController: TextInputController?,
  ): Int? {
    return if (ComponentsConfiguration.usePrimitiveTextInput) {
      textInputController?.getLineCount()
    } else {
      TextInputComponent.getLineCount(c, textInputHandle)
    }
  }

  @OnTrigger(SetTextEvent::class)
  fun setText(
      c: ComponentContext,
      @FromTrigger text: CharSequence?,
      @State textInputHandle: Handle,
      @CachedValue textInputController: TextInputController?,
  ) {
    if (ComponentsConfiguration.usePrimitiveTextInput) {
      textInputController?.setText(text)
    } else {
      TextInputComponent.setText(c, textInputHandle, text)
    }
  }

  @OnTrigger(ReplaceTextEvent::class)
  fun replaceText(
      c: ComponentContext,
      @FromTrigger text: CharSequence?,
      @FromTrigger startIndex: Int,
      @FromTrigger endIndex: Int,
      @FromTrigger skipSelection: Boolean,
      @State textInputHandle: Handle,
      @CachedValue textInputController: TextInputController?,
  ) {
    if (ComponentsConfiguration.usePrimitiveTextInput) {
      textInputController?.replaceText(text, startIndex, endIndex, skipSelection)
    } else {
      TextInputComponent.replaceText(c, textInputHandle, text, startIndex, endIndex, skipSelection)
    }
  }

  @JvmStatic
  @OnTrigger(DispatchKeyEvent::class)
  fun dispatchKey(
      c: ComponentContext,
      @FromTrigger keyEvent: KeyEvent?,
      @State textInputHandle: Handle,
      @CachedValue textInputController: TextInputController?,
  ) {
    if (ComponentsConfiguration.usePrimitiveTextInput) {
      textInputController?.dispatchKey(keyEvent)
    } else {
      TextInputComponent.dispatchKey(c, textInputHandle, keyEvent)
    }
  }

  @JvmStatic
  @OnTrigger(SetSelectionEvent::class)
  fun setSelection(
      c: ComponentContext,
      @FromTrigger start: Int,
      @FromTrigger end: Int,
      @State textInputHandle: Handle,
      @CachedValue textInputController: TextInputController?,
  ) {
    if (ComponentsConfiguration.usePrimitiveTextInput) {
      textInputController?.setSelection(start, end)
    } else {
      TextInputComponent.setSelection(c, textInputHandle, start, end)
    }
  }

  @OnTrigger(SetSpanEvent::class)
  fun setSpan(
      c: ComponentContext,
      @FromTrigger what: Any?,
      @FromTrigger start: Int,
      @FromTrigger end: Int,
      @FromTrigger flags: Int,
      @State textInputHandle: Handle,
      @CachedValue textInputController: TextInputController?,
  ) {
    if (ComponentsConfiguration.usePrimitiveTextInput) {
      textInputController?.setSpan(what, start, end, flags)
    } else {
      TextInputComponent.setSpan(c, textInputHandle, what, start, end, flags)
    }
  }

  @OnTrigger(RemoveSpanEvent::class)
  fun removeSpan(
      c: ComponentContext,
      @FromTrigger what: Any?,
      @State textInputHandle: Handle,
      @CachedValue textInputController: TextInputController?,
  ) {
    if (ComponentsConfiguration.usePrimitiveTextInput) {
      textInputController?.removeSpan(what)
    } else {
      TextInputComponent.removeSpan(c, textInputHandle, what)
    }
  }

  @OnTrigger(GetSpanStartEvent::class)
  fun getSpanStart(
      c: ComponentContext,
      @FromTrigger what: Any?,
      @State textInputHandle: Handle,
      @CachedValue textInputController: TextInputController?,
  ): Int {
    return if (ComponentsConfiguration.usePrimitiveTextInput) {
      textInputController?.getSpanStart(what) ?: -1
    } else {
      TextInputComponent.getSpanStart(c, textInputHandle, what)
    }
  }
}
