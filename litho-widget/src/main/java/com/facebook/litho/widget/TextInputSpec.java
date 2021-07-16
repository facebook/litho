/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.litho.widget;

import static android.graphics.Color.TRANSPARENT;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.os.Build.VERSION_CODES.M;
import static android.view.View.TEXT_ALIGNMENT_GRAVITY;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.ArrowKeyMovementMethod;
import android.text.method.KeyListener;
import android.text.method.MovementMethod;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import androidx.core.util.ObjectsCompat;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.Diff;
import com.facebook.litho.EventHandler;
import com.facebook.litho.Output;
import com.facebook.litho.Size;
import com.facebook.litho.SizeSpec;
import com.facebook.litho.StateValue;
import com.facebook.litho.ThreadUtils;
import com.facebook.litho.annotations.FromTrigger;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnBind;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnLoadStyle;
import com.facebook.litho.annotations.OnMeasure;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.OnTrigger;
import com.facebook.litho.annotations.OnUnbind;
import com.facebook.litho.annotations.OnUnmount;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.ResType;
import com.facebook.litho.annotations.ShouldUpdate;
import com.facebook.litho.annotations.State;
import com.facebook.litho.utils.MeasureUtils;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;

/**
 * Component that renders an editable text input using an android {@link EditText}. It is measured
 * based on the input text {@link String} representation.
 *
 * <p>Performance is critical for good user experience. Follow these tips for good performance:
 *
 * <ul>
 *   <li>Avoid changing props at all costs as it forces expensive EditText reconfiguration.
 *   <li>Avoid updating state, use Event trigger {@link OnTrigger} to update text, request view
 *       focus or set selection. {@code TextInput.setText(c, key, text)}.
 *   <li>Using custom inputFilters take special care to implement equals correctly or the text field
 *       must be reconfigured on every mount. (Better yet, store your InputFilter in a static or
 *       LruCache so that you're not constantly creating new instances.)
 * </ul>
 *
 * <p>Because this component is backed by android {@link EditText} many native capabilities are
 * applicable:
 *
 * <ul>
 *   <li>Use {@link InputFilter} to set a text length limit or modify text input.
 *   <li>Remove android EditText underline by removing background.
 *   <li>Change the input representation by passing one of the {@link android.text.InputType}
 *       constants.
 * </ul>
 *
 * <p>It is also treated by the system as an android {@link EditText}:
 *
 * <ul>
 *   <li>When {@link EditText} receives focus, a system keyboard is shown.
 *   <li>When the user opens the screen and android {@link EditText} is the first element in the
 *       View hierarchy, it gains focus.
 * </ul>
 *
 * <p>Example of multiline editable text with custom text color, text length limit, removed
 * underline drawable, and sentence capitalisation:
 *
 * <pre>{@code
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
 * }</pre>
 *
 * @prop initialText Initial text to display. If set, the value is set on the EditText exactly once:
 *     on initial mount. From then on, the EditText's text property is not modified.
 * @prop hint Hint text to display.
 * @prop inputBackground The background of the EditText itself; this is subtly distinct from the
 *     Litho background prop. The padding of the inputBackground drawable will be applied to the
 *     EditText itself, insetting the cursor and text field.
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
 *     17 and above; it's up to you to handle earlier API levels by adjusting gravity.
 * @prop gravity Gravity for the text within its container.
 * @prop editable If set, allows the text to be editable.
 * @prop inputType Type of data being placed in a text field, used to help an input method decide
 *     how to let the user enter text. To add multiline use multiline(true) method.
 * @prop imeOptions Type of data in the text field, reported to an IME when it has focus.
 * @prop inputFilters Used to filter the input to e.g. a max character count.
 * @prop multiline If set to true, type of the input will be changed to multiline TEXT. Because
 *     passwords or numbers couldn't be multiline by definition.
 * @prop ellipsize If set, specifies the position of the text to be ellipsized. See <a
 *     href="https://developer.android.com/reference/android/widget/TextView.html#setEllipsize(android.text.TextUtils.TruncateAt)">android
 *     documentation</a> for behavior description.
 * @prop minLines Minimum number of lines to show.
 * @prop maxLines Maximum number of lines to show.
 * @prop textWatchers Used to register text watchers e.g. mentions detection.
 * @prop movementMethod Used to set cursor positioning, scrolling and text selection functionality
 *     in EditText
 * @prop error Sets the right-hand compound drawable of the TextView to the "error" icon and sets an
 *     error message that will be displayed in a popup when the TextView has focus. The icon and
 *     error message will be reset to null when any key events cause changes to the TextView's text.
 *     If the error is null, the error message and icon will be cleared. See
 *     https://developer.android.com/reference/android/widget/TextView.html#setError for more
 *     details.
 * @prop errorDrawable Will show along with the error message when a message is set.
 * @see {@link EditText}
 */
@MountSpec(
    isPureRender = true,
    events = {
      TextChangedEvent.class,
      SelectionChangedEvent.class,
      InputFocusChangedEvent.class,
      KeyUpEvent.class,
      KeyPreImeEvent.class,
      EditorActionEvent.class,
      SetTextEvent.class,
      InputConnectionEvent.class,
    })
class TextInputSpec {
  /**
   * Dummy drawable used for differentiating user-provided null background drawable from default
   * drawable of the spec
   */
  static final Drawable UNSET_DRAWABLE = new ColorDrawable(TRANSPARENT);

  @PropDefault
  protected static final ColorStateList textColorStateList = ColorStateList.valueOf(Color.BLACK);

  @PropDefault
  protected static final ColorStateList hintColorStateList = ColorStateList.valueOf(Color.LTGRAY);

  @PropDefault static final CharSequence hint = "";
  @PropDefault static final CharSequence initialText = "";
  @PropDefault protected static final int shadowColor = Color.GRAY;
  @PropDefault protected static final int textSize = TextSpec.UNSET;
  @PropDefault protected static final Drawable inputBackground = UNSET_DRAWABLE;
  @PropDefault protected static final Typeface typeface = Typeface.DEFAULT;
  @PropDefault protected static final int textAlignment = TEXT_ALIGNMENT_GRAVITY;
  @PropDefault protected static final int gravity = Gravity.CENTER_VERTICAL | Gravity.START;
  @PropDefault protected static final boolean editable = true;
  @PropDefault protected static final int inputType = EditorInfo.TYPE_CLASS_TEXT;
  @PropDefault protected static final int imeOptions = EditorInfo.IME_NULL;
  @PropDefault protected static final int cursorDrawableRes = -1;
  @PropDefault static final boolean multiline = false;
  @PropDefault protected static final int minLines = 1;
  @PropDefault protected static final int maxLines = Integer.MAX_VALUE;

  @PropDefault
  protected static final MovementMethod movementMethod = ArrowKeyMovementMethod.getInstance();

  /** UI thread only; used in OnMount. */
  private static final Rect sBackgroundPaddingRect = new Rect();
  /** UI thread only; used in OnMount. */
  private static final InputFilter[] NO_FILTERS = new InputFilter[0];

  @OnCreateInitialState
  static void onCreateInitialState(
      StateValue<AtomicReference<EditTextWithEventHandlers>> mountedView,
      StateValue<AtomicReference<CharSequence>> savedText,
      StateValue<Integer> measureSeqNumber,
      @Prop(optional = true, resType = ResType.STRING) CharSequence initialText) {
    mountedView.set(new AtomicReference<EditTextWithEventHandlers>());
    measureSeqNumber.set(0);
    savedText.set(new AtomicReference<>(initialText));
  }

  @OnLoadStyle
  static void onLoadStyle(ComponentContext c, Output<Integer> highlightColor) {
    TypedArray a = c.obtainStyledAttributes(new int[] {android.R.attr.textColorHighlight}, 0);
    try {
      highlightColor.set(a.getColor(0, 0));
    } finally {
      a.recycle();
    }
  }

  @OnMeasure
  static void onMeasure(
      ComponentContext c,
      ComponentLayout layout,
      int widthSpec,
      int heightSpec,
      Size size,
      @Prop(optional = true, resType = ResType.STRING) CharSequence hint,
      @Prop(optional = true, resType = ResType.DRAWABLE) Drawable inputBackground,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) float shadowRadius,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) float shadowDx,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) float shadowDy,
      @Prop(optional = true, resType = ResType.COLOR) int shadowColor,
      @Prop(optional = true) ColorStateList textColorStateList,
      @Prop(optional = true) ColorStateList hintColorStateList,
      @Prop(optional = true, resType = ResType.COLOR) Integer highlightColor,
      @Prop(optional = true, resType = ResType.DIMEN_TEXT) int textSize,
      @Prop(optional = true) Typeface typeface,
      @Prop(optional = true) int textAlignment,
      @Prop(optional = true) int gravity,
      @Prop(optional = true) boolean editable,
      @Prop(optional = true) int inputType,
      @Prop(optional = true) int imeOptions,
      @Prop(optional = true, varArg = "inputFilter") List<InputFilter> inputFilters,
      @Prop(optional = true) boolean multiline,
      @Prop(optional = true) TextUtils.TruncateAt ellipsize,
      @Prop(optional = true) int minLines,
      @Prop(optional = true) int maxLines,
      @Prop(optional = true) int cursorDrawableRes,
      @Prop(optional = true, resType = ResType.STRING) CharSequence error,
      @Prop(optional = true, resType = ResType.DRAWABLE) Drawable errorDrawable,
      @Prop(optional = true) @Nullable KeyListener keyListener,
      @State AtomicReference<CharSequence> savedText) {
    EditText forMeasure =
        TextInputSpec.createAndMeasureEditText(
            c,
            layout,
            widthSpec,
            heightSpec,
            size,
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
            inputType,
            keyListener,
            imeOptions,
            inputFilters,
            multiline,
            ellipsize,
            minLines,
            maxLines,
            cursorDrawableRes,
            error,
            errorDrawable,
            // onMeasure happens:
            // 1. After initState before onMount: savedText = initText.
            // 2. After onMount before onUnmount: savedText preserved from underlying editText.
            savedText.get());

    setSizeForView(size, widthSpec, heightSpec, forMeasure);
  }

  static void setSizeForView(Size size, int widthSpec, int heightSpec, View forMeasure) {
    size.height = forMeasure.getMeasuredHeight();

    // For width we always take all available space, or collapse to 0 if unspecified.
    if (SizeSpec.getMode(widthSpec) == SizeSpec.UNSPECIFIED) {
      size.width = 0;
    } else {
      size.width = Math.min(SizeSpec.getSize(widthSpec), forMeasure.getMeasuredWidth());
    }
  }

  static EditText createAndMeasureEditText(
      ComponentContext c,
      ComponentLayout layout,
      int widthSpec,
      int heightSpec,
      Size size,
      CharSequence hint,
      Drawable inputBackground,
      float shadowRadius,
      float shadowDx,
      float shadowDy,
      int shadowColor,
      ColorStateList textColorStateList,
      ColorStateList hintColorStateList,
      Integer highlightColor,
      int textSize,
      Typeface typeface,
      int textAlignment,
      int gravity,
      boolean editable,
      int inputType,
      @Nullable KeyListener keyListener,
      int imeOptions,
      List<InputFilter> inputFilters,
      boolean multiline,
      TextUtils.TruncateAt ellipsize,
      int minLines,
      int maxLines,
      int cursorDrawableRes,
      CharSequence error,
      Drawable errorDrawable,
      CharSequence text) {
    // The height should be the measured height of EditText with relevant params
    final EditText forMeasure = new ForMeasureEditText(c.getAndroidContext());
    // If text contains Spans, we don't want it to be mutable for the measurement case
    if (text instanceof Spannable) {
      text = text.toString();
    }
    setParams(
        forMeasure,
        hint,
        getBackgroundOrDefault(
            c, inputBackground == UNSET_DRAWABLE ? forMeasure.getBackground() : inputBackground),
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
        inputType,
        keyListener,
        imeOptions,
        inputFilters,
        multiline,
        ellipsize,
        minLines,
        maxLines,
        cursorDrawableRes,
        forMeasure.getMovementMethod(),
        text,
        error,
        errorDrawable,
        true);
    forMeasure.measure(
        MeasureUtils.getViewMeasureSpec(widthSpec), MeasureUtils.getViewMeasureSpec(heightSpec));
    return forMeasure;
  }

  static void setParams(
      EditText editText,
      @Nullable CharSequence hint,
      @Nullable Drawable background,
      float shadowRadius,
      float shadowDx,
      float shadowDy,
      int shadowColor,
      ColorStateList textColorStateList,
      ColorStateList hintColorStateList,
      Integer highlightColor,
      int textSize,
      Typeface typeface,
      int textAlignment,
      int gravity,
      boolean editable,
      int inputType,
      @Nullable KeyListener keyListener,
      int imeOptions,
      @Nullable List<InputFilter> inputFilters,
      boolean multiline,
      @Nullable TextUtils.TruncateAt ellipsize,
      int minLines,
      int maxLines,
      int cursorDrawableRes,
      MovementMethod movementMethod,
      @Nullable CharSequence text,
      @Nullable CharSequence error,
      @Nullable Drawable errorDrawable,
      boolean isForMeasure) {

    if (textSize == TextSpec.UNSET) {
      editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, TextSpec.DEFAULT_TEXT_SIZE_SP);
    } else {
      editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
    }

    if (multiline) {
      inputType |= EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE;
      editText.setMinLines(minLines);
      editText.setMaxLines(maxLines);
    } else {
      inputType &= ~EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE;
      editText.setLines(1);
    }

    if (!editable) {
      inputType = EditorInfo.TYPE_NULL;
    }
    setInputTypeAndKeyListenerIfChanged(editText, inputType, keyListener);

    // Needs to be set before the text so it would apply to the current text
    if (inputFilters != null) {
      editText.setFilters(inputFilters.toArray(new InputFilter[inputFilters.size()]));
    } else {
      editText.setFilters(NO_FILTERS);
    }
    editText.setHint(hint);

    if (SDK_INT < JELLY_BEAN) {
      editText.setBackgroundDrawable(background);
    } else {
      editText.setBackground(background);
    }
    // From the docs for setBackground:
    // "If the background has padding, this View's padding is set to the background's padding.
    // However, when a background is removed, this View's padding isn't touched. If setting the
    // padding is desired, please use setPadding."
    if (background == null || !background.getPadding(sBackgroundPaddingRect)) {
      editText.setPadding(0, 0, 0, 0);
    }
    editText.setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor);
    editText.setTypeface(typeface, 0);
    editText.setGravity(gravity);
    editText.setImeOptions(imeOptions);
    editText.setFocusable(editable);
    editText.setFocusableInTouchMode(editable);
    editText.setLongClickable(editable);
    editText.setCursorVisible(editable);
    editText.setTextColor(textColorStateList);
    editText.setHintTextColor(hintColorStateList);
    if (highlightColor != null) {
      editText.setHighlightColor(highlightColor);
    }
    editText.setMovementMethod(movementMethod);

    /**
     * Sets error state on the TextInput, which shows an error icon provided by errorDrawable and an
     * error message
     *
     * @param error Message that will be shown when error is not null and text input is in focused
     *     state
     * @param errorDrawable icon that signals an existing error and anchors a popover showing the
     *     errorMessage when component is focused.
     */
    editText.setError(error, errorDrawable);

    if (cursorDrawableRes != -1) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        editText.setTextCursorDrawable(cursorDrawableRes);
      } else {
        try {
          // Uses reflection because there is no public API to change cursor color programmatically.
          // Based on
          // http://stackoverflow.com/questions/25996032/how-to-change-programatically-edittext-cursor-color-in-android.
          Field f = TextView.class.getDeclaredField("mCursorDrawableRes");
          f.setAccessible(true);
          f.set(editText, cursorDrawableRes);
        } catch (Exception exception) {
          // no-op don't set cursor drawable
        }
      }
    }

    editText.setEllipsize(ellipsize);
    if (SDK_INT >= JELLY_BEAN_MR1) {
      editText.setTextAlignment(textAlignment);
    }
    if (text != null && !ObjectsCompat.equals(editText.getText().toString(), text.toString())) {
      editText.setText(text);
      // Set the selection only when mounting because #setSelection does not affect measurement,
      // but it can mutate the span during measurement, potentially causing crashes.
      if (!isForMeasure) {
        editText.setSelection(editText.getText().toString().length());
      }
    }
  }

  private static void setInputTypeAndKeyListenerIfChanged(
      EditText editText, int inputType, @Nullable KeyListener keyListener) {
    // Avoid redundant call to InputMethodManager#restartInput.
    if (inputType != editText.getInputType()) {
      editText.setInputType(inputType);
    }

    // Optionally Set KeyListener later to override the one set by the InputType
    if (keyListener != null && keyListener != editText.getKeyListener()) {
      editText.setKeyListener(keyListener);
    }
  }

  @ShouldUpdate
  static boolean shouldUpdate(
      @Prop(optional = true, resType = ResType.STRING) Diff<CharSequence> initialText,
      @Prop(optional = true, resType = ResType.STRING) Diff<CharSequence> hint,
      @Prop(optional = true, resType = ResType.DRAWABLE) Diff<Drawable> inputBackground,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) Diff<Float> shadowRadius,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) Diff<Float> shadowDx,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) Diff<Float> shadowDy,
      @Prop(optional = true, resType = ResType.COLOR) Diff<Integer> shadowColor,
      @Prop(optional = true) Diff<ColorStateList> textColorStateList,
      @Prop(optional = true) Diff<ColorStateList> hintColorStateList,
      @Prop(optional = true, resType = ResType.COLOR) Diff<Integer> highlightColor,
      @Prop(optional = true, resType = ResType.DIMEN_TEXT) Diff<Integer> textSize,
      @Prop(optional = true) Diff<Typeface> typeface,
      @Prop(optional = true) Diff<Integer> textAlignment,
      @Prop(optional = true) Diff<Integer> gravity,
      @Prop(optional = true) Diff<Boolean> editable,
      @Prop(optional = true) Diff<Integer> inputType,
      @Prop(optional = true) Diff<Integer> imeOptions,
      @Prop(optional = true, varArg = "inputFilter") Diff<List<InputFilter>> inputFilters,
      @Prop(optional = true) Diff<TextUtils.TruncateAt> ellipsize,
      @Prop(optional = true) Diff<Boolean> multiline,
      @Prop(optional = true) Diff<Integer> minLines,
      @Prop(optional = true) Diff<Integer> maxLines,
      @Prop(optional = true) Diff<Integer> cursorDrawableRes,
      @Prop(optional = true) Diff<MovementMethod> movementMethod,
      @Prop(optional = true, resType = ResType.STRING) Diff<CharSequence> error,
      @Prop(optional = true) Diff<KeyListener> keyListener,
      @State Diff<Integer> measureSeqNumber,
      @State Diff<AtomicReference<EditTextWithEventHandlers>> mountedView,
      @State Diff<AtomicReference<CharSequence>> savedText) {
    if (!ObjectsCompat.equals(measureSeqNumber.getPrevious(), measureSeqNumber.getNext())) {
      return true;
    }
    if (!ObjectsCompat.equals(initialText.getPrevious(), initialText.getNext())) {
      return true;
    }
    if (!ObjectsCompat.equals(hint.getPrevious(), hint.getNext())) {
      return true;
    }
    if (!ObjectsCompat.equals(shadowRadius.getPrevious(), shadowRadius.getNext())) {
      return true;
    }
    if (!ObjectsCompat.equals(shadowDx.getPrevious(), shadowDx.getNext())) {
      return true;
    }
    if (!ObjectsCompat.equals(shadowDy.getPrevious(), shadowDy.getNext())) {
      return true;
    }
    if (!ObjectsCompat.equals(shadowColor.getPrevious(), shadowColor.getNext())) {
      return true;
    }
    if (!ObjectsCompat.equals(textColorStateList.getPrevious(), textColorStateList.getNext())) {
      return true;
    }
    if (!ObjectsCompat.equals(hintColorStateList.getPrevious(), hintColorStateList.getNext())) {
      return true;
    }
    if (!ObjectsCompat.equals(highlightColor.getPrevious(), highlightColor.getNext())) {
      return true;
    }
    if (!ObjectsCompat.equals(textSize.getPrevious(), textSize.getNext())) {
      return true;
    }
    if (!ObjectsCompat.equals(typeface.getPrevious(), typeface.getNext())) {
      return true;
    }
    if (!ObjectsCompat.equals(textAlignment.getPrevious(), textAlignment.getNext())) {
      return true;
    }
    if (!ObjectsCompat.equals(gravity.getPrevious(), gravity.getNext())) {
      return true;
    }
    if (!ObjectsCompat.equals(editable.getPrevious(), editable.getNext())) {
      return true;
    }
    if (!ObjectsCompat.equals(inputType.getPrevious(), inputType.getNext())) {
      return true;
    }
    if (!ObjectsCompat.equals(keyListener.getPrevious(), keyListener.getNext())) {
      return true;
    }
    if (!ObjectsCompat.equals(imeOptions.getPrevious(), imeOptions.getNext())) {
      return true;
    }
    if (!equalInputFilters(inputFilters.getPrevious(), inputFilters.getNext())) {
      return true;
    }
    if (!ObjectsCompat.equals(ellipsize.getPrevious(), ellipsize.getNext())) {
      return true;
    }
    if (!ObjectsCompat.equals(multiline.getPrevious(), multiline.getNext())) {
      return true;
    }
    // Minimum and maximum line count should only get checked if multiline is set
    if (multiline.getNext()) {
      if (!ObjectsCompat.equals(minLines.getPrevious(), minLines.getNext())) {
        return true;
      }
      if (!ObjectsCompat.equals(maxLines.getPrevious(), maxLines.getNext())) {
        return true;
      }
    }
    if (!ObjectsCompat.equals(cursorDrawableRes.getPrevious(), cursorDrawableRes.getNext())) {
      return true;
    }
    if (!ObjectsCompat.equals(movementMethod.getPrevious(), movementMethod.getNext())) {
      return true;
    }
    if (!ObjectsCompat.equals(error.getPrevious(), error.getNext())) {
      return true;
    }

    // Note, these are purposefully just comparing the containers, not the contents!
    if (mountedView.getPrevious() != mountedView.getNext()) {
      return true;
    }
    if (savedText.getPrevious() != savedText.getNext()) {
      return true;
    }

    // Save the nastiest for last: trying to diff drawables.
    Drawable previousBackground = inputBackground.getPrevious();
    Drawable nextBackground = inputBackground.getNext();
    if (previousBackground == null && nextBackground != null) {
      return true;
    } else if (previousBackground != null && nextBackground == null) {
      return true;
    } else if (previousBackground != null && nextBackground != null) {
      if (previousBackground instanceof ColorDrawable && nextBackground instanceof ColorDrawable) {
        // This doesn't account for tint list/mode (no way to get that information)
        // and doesn't account for color filter (fine since ColorDrawable ignores it anyway).
        ColorDrawable prevColor = (ColorDrawable) previousBackground;
        ColorDrawable nextColor = (ColorDrawable) nextBackground;
        if (prevColor.getColor() != nextColor.getColor()) {
          return true;
        }
      } else {
        // The best we can do here is compare getConstantState. This can result in spurious updates;
        // they might be different objects representing the same drawable. But it's the best we can
        // do without actually comparing bitmaps (which is too expensive).
        if (!ObjectsCompat.equals(
            previousBackground.getConstantState(), nextBackground.getConstantState())) {
          return true;
        }
      }
    }
    return false;
  }

  /** LengthFilter and AllCaps do not implement isEqual. Correct for the deficiency. */
  static boolean equalInputFilters(List<InputFilter> a, List<InputFilter> b) {
    if (a == null && b == null) {
      return true;
    }
    if (a == null || b == null) {
      return false;
    }
    if (a.size() != b.size()) {
      return false;
    }
    for (int i = 0; i < a.size(); i++) {
      InputFilter fa = a.get(i);
      InputFilter fb = b.get(i);
      if (fa instanceof InputFilter.AllCaps && fb instanceof InputFilter.AllCaps) {
        continue; // equal, AllCaps has no configuration
      }
      if (SDK_INT >= LOLLIPOP) { // getMax added in lollipop
        if (fa instanceof InputFilter.LengthFilter && fb instanceof InputFilter.LengthFilter) {
          if (((InputFilter.LengthFilter) fa).getMax()
              != ((InputFilter.LengthFilter) fb).getMax()) {
            return false;
          }
          continue; // equal, same max
        }
      }
      // Best we can do in this case is call equals().
      if (!ObjectsCompat.equals(fa, fb)) {
        return false;
      }
    }
    return true;
  }

  @OnCreateMountContent
  protected static EditTextWithEventHandlers onCreateMountContent(Context c) {
    return new EditTextWithEventHandlers(c);
  }

  @OnMount
  static void onMount(
      final ComponentContext c,
      EditTextWithEventHandlers editText,
      @Prop(optional = true, resType = ResType.STRING) CharSequence hint,
      @Prop(optional = true, resType = ResType.DRAWABLE) Drawable inputBackground,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) float shadowRadius,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) float shadowDx,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) float shadowDy,
      @Prop(optional = true, resType = ResType.COLOR) int shadowColor,
      @Prop(optional = true) ColorStateList textColorStateList,
      @Prop(optional = true) ColorStateList hintColorStateList,
      @Prop(optional = true, resType = ResType.COLOR) Integer highlightColor,
      @Prop(optional = true, resType = ResType.DIMEN_TEXT) int textSize,
      @Prop(optional = true) Typeface typeface,
      @Prop(optional = true) int textAlignment,
      @Prop(optional = true) int gravity,
      @Prop(optional = true) boolean editable,
      @Prop(optional = true) int inputType,
      @Prop(optional = true) int imeOptions,
      @Prop(optional = true, varArg = "inputFilter") List<InputFilter> inputFilters,
      @Prop(optional = true) boolean multiline,
      @Prop(optional = true) int minLines,
      @Prop(optional = true) int maxLines,
      @Prop(optional = true) TextUtils.TruncateAt ellipsize,
      @Prop(optional = true) int cursorDrawableRes,
      @Prop(optional = true) MovementMethod movementMethod,
      @Prop(optional = true, resType = ResType.STRING) CharSequence error,
      @Prop(optional = true, resType = ResType.DRAWABLE) Drawable errorDrawable,
      @Prop(optional = true) @Nullable KeyListener keyListener,
      @State AtomicReference<CharSequence> savedText,
      @State AtomicReference<EditTextWithEventHandlers> mountedView) {
    mountedView.set(editText);

    setParams(
        editText,
        hint,
        getBackgroundOrDefault(c, inputBackground),
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
        inputType,
        keyListener,
        imeOptions,
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
        savedText.get(),
        error,
        errorDrawable,
        false);
    editText.setTextState(savedText);
  }

  @OnBind
  static void onBind(
      final ComponentContext c,
      EditTextWithEventHandlers editText,
      @Prop(optional = true, varArg = "textWatcher") List<TextWatcher> textWatchers) {
    onBindEditText(
        c,
        editText,
        textWatchers,
        TextInput.getTextChangedEventHandler(c),
        TextInput.getSelectionChangedEventHandler(c),
        TextInput.getInputFocusChangedEventHandler(c),
        TextInput.getKeyUpEventHandler(c),
        TextInput.getKeyPreImeEventHandler(c),
        TextInput.getEditorActionEventHandler(c),
        TextInput.getInputConnectionEventHandler(c));
  }

  static void onBindEditText(
      final ComponentContext c,
      EditTextWithEventHandlers editText,
      @Nullable List<TextWatcher> textWatchers,
      EventHandler textChangedEventHandler,
      EventHandler selectionChangedEventHandler,
      EventHandler inputFocusChangedEventHandler,
      EventHandler keyUpEventHandler,
      EventHandler keyPreImeEventHandler,
      EventHandler EditorActionEventHandler,
      EventHandler inputConnectionEventHandler) {
    editText.attachWatchers(textWatchers);

    editText.setComponentContext(c);
    editText.setTextChangedEventHandler(textChangedEventHandler);
    editText.setSelectionChangedEventHandler(selectionChangedEventHandler);
    editText.setInputFocusChangedEventHandler(inputFocusChangedEventHandler);
    editText.setKeyUpEventHandler(keyUpEventHandler);
    editText.setKeyPreImeEventEventHandler(keyPreImeEventHandler);
    editText.setEditorActionEventHandler(EditorActionEventHandler);
    editText.setInputConnectionEventHandler(inputConnectionEventHandler);
  }

  @OnUnmount
  static void onUnmount(
      ComponentContext c,
      EditTextWithEventHandlers editText,
      @Prop(optional = true) @Nullable KeyListener keyListener,
      @State AtomicReference<EditTextWithEventHandlers> mountedView) {
    if (keyListener != null) {
      editText.setKeyListener(null); // Clear any KeyListener
      editText.setInputType(inputType); // Set the input type back to default.
    }
    editText.setTextState(null);
    mountedView.set(null);
  }

  @OnUnbind
  static void onUnbind(final ComponentContext c, EditTextWithEventHandlers editText) {
    editText.detachWatchers();

    editText.setComponentContext(null);
    editText.setTextChangedEventHandler(null);
    editText.setSelectionChangedEventHandler(null);
    editText.setInputFocusChangedEventHandler(null);
    editText.setKeyUpEventHandler(null);
    editText.setKeyPreImeEventEventHandler(null);
    editText.setEditorActionEventHandler(null);
    editText.setInputConnectionEventHandler(null);
  }

  @Nullable
  static Drawable getBackgroundOrDefault(ComponentContext c, Drawable specifiedBackground) {
    if (specifiedBackground == UNSET_DRAWABLE) {
      final int[] attrs = {android.R.attr.background};
      TypedArray a =
          c.getAndroidContext()
              .obtainStyledAttributes(null, attrs, android.R.attr.editTextStyle, 0);
      Drawable defaultBackground = a.getDrawable(0);
      a.recycle();
      return defaultBackground;
    }

    return specifiedBackground;
  }

  @OnTrigger(RequestFocusEvent.class)
  static void requestFocus(
      ComponentContext c, @State AtomicReference<EditTextWithEventHandlers> mountedView) {
    final EditTextWithEventHandlers view = mountedView.get();
    if (view != null) {
      if (view.requestFocus()) {
        view.setSoftInputVisibility(true);
      }
    }
  }

  @OnTrigger(ClearFocusEvent.class)
  static void clearFocus(
      ComponentContext c, @State AtomicReference<EditTextWithEventHandlers> mountedView) {
    EditTextWithEventHandlers view = mountedView.get();
    if (view != null) {
      view.clearFocus();
      view.setSoftInputVisibility(false);
    }
  }

  @OnTrigger(GetTextEvent.class)
  @Nullable
  static CharSequence getText(
      ComponentContext c,
      @State AtomicReference<EditTextWithEventHandlers> mountedView,
      @State AtomicReference<CharSequence> savedText) {
    final EditTextWithEventHandlers view = mountedView.get();
    return view == null ? savedText.get() : view.getText();
  }

  @OnTrigger(GetLineCountEvent.class)
  @Nullable
  static Integer getLineCount(
      ComponentContext c, @State AtomicReference<EditTextWithEventHandlers> mountedView) {
    final EditTextWithEventHandlers view = mountedView.get();
    return view != null ? view.getLineCount() : null;
  }

  @OnTrigger(SetTextEvent.class)
  static void setText(
      ComponentContext c,
      @State AtomicReference<EditTextWithEventHandlers> mountedView,
      @State AtomicReference<CharSequence> savedText,
      @FromTrigger CharSequence text) {
    boolean shouldRemeasure = setTextEditText(mountedView, savedText, text);
    if (shouldRemeasure) {
      TextInput.remeasureForUpdatedTextSync(c);
    }
  }

  static boolean setTextEditText(
      AtomicReference<EditTextWithEventHandlers> mountedView,
      AtomicReference<CharSequence> savedText,
      @Nullable CharSequence text) {
    ThreadUtils.assertMainThread();

    EditTextWithEventHandlers editText = mountedView.get();
    if (editText == null) {
      savedText.set(text);
      return true;
    }

    // If line count changes state update will be triggered by view
    editText.setText(text);
    editText.setSelection(text != null ? text.length() : 0);
    return false;
  }

  @OnTrigger(ReplaceTextEvent.class)
  static void replaceText(
      ComponentContext c,
      @State AtomicReference<EditTextWithEventHandlers> mountedView,
      @State AtomicReference<CharSequence> savedText,
      @FromTrigger CharSequence text,
      @FromTrigger int startIndex,
      @FromTrigger int endIndex) {
    EditTextWithEventHandlers editText = mountedView.get();
    if (editText != null) {
      editText.getText().replace(startIndex, endIndex, text);
      editText.setSelection(text != null ? startIndex + text.length() : startIndex);
      return;
    }

    CharSequence currentSavedText = savedText.get();
    savedText.set(
        currentSavedText == null
            ? text
            : new SpannableStringBuilder()
                .append(currentSavedText.subSequence(0, startIndex))
                .append(text)
                .append(currentSavedText.subSequence(endIndex, currentSavedText.length())));

    TextInput.remeasureForUpdatedTextSync(c);
  }

  @OnTrigger(DispatchKeyEvent.class)
  static void dispatchKey(
      ComponentContext c,
      @State AtomicReference<EditTextWithEventHandlers> mountedView,
      @FromTrigger KeyEvent keyEvent) {
    EditTextWithEventHandlers view = mountedView.get();
    if (view != null) {
      view.dispatchKeyEvent(keyEvent);
    }
  }

  @OnTrigger(SetSelectionEvent.class)
  static void setSelection(
      ComponentContext c,
      @State AtomicReference<EditTextWithEventHandlers> mountedView,
      @FromTrigger int start,
      @FromTrigger int end) {
    EditTextWithEventHandlers view = mountedView.get();
    if (view != null) {
      view.setSelection(start, end < start ? start : end);
    }
  }

  @OnUpdateState
  static void remeasureForUpdatedText(StateValue<Integer> measureSeqNumber) {
    measureSeqNumber.set(measureSeqNumber.get() + 1);
  }

  static class EditTextWithEventHandlers extends EditText
      implements EditText.OnEditorActionListener {

    private static final int UNMEASURED_LINE_COUNT = -1;

    @Nullable private EventHandler<TextChangedEvent> mTextChangedEventHandler;
    @Nullable private EventHandler<SelectionChangedEvent> mSelectionChangedEventHandler;
    @Nullable private EventHandler<InputFocusChangedEvent> mInputFocusChangedEventHandler;
    @Nullable private EventHandler<KeyUpEvent> mKeyUpEventHandler;
    @Nullable private EventHandler<KeyPreImeEvent> mKeyPreImeEventEventHandler;
    @Nullable private EventHandler<EditorActionEvent> mEditorActionEventHandler;
    @Nullable private EventHandler<InputConnectionEvent> mInputConnectionEventHandler;
    @Nullable private ComponentContext mComponentContext;
    @Nullable private AtomicReference<CharSequence> mTextState;
    private int mLineCount = UNMEASURED_LINE_COUNT;
    @Nullable private TextWatcher mTextWatcher;
    private boolean mIsSoftInputRequested = false;

    public EditTextWithEventHandlers(Context context) {
      super(context);
      // Unfortunately we can't just override `void onEditorAction(int actionCode)` as that only
      // covers a subset of all cases where onEditorActionListener is invoked.
      this.setOnEditorActionListener(this);
    }

    @Override
    public void requestLayout() {
      // TextInputSpec$ForMeasureEditText.setText in API23 causing relayout for
      // EditTextWithEventHandlers https://fburl.com/mgq76t3l
      if (SDK_INT == M && !ThreadUtils.isMainThread()) {
        return;
      }
      super.requestLayout();
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
      super.onTextChanged(text, start, lengthBefore, lengthAfter);
      if (mTextState != null) {
        mTextState.set(text);
      }
      if (mTextChangedEventHandler != null) {
        TextInput.dispatchTextChangedEvent(
            mTextChangedEventHandler, EditTextWithEventHandlers.this, text.toString());
      }
      // Line count of changed text.
      int lineCount = getLineCount();
      if (mLineCount != UNMEASURED_LINE_COUNT
          && mLineCount != lineCount
          && mComponentContext != null) {
        com.facebook.litho.widget.TextInput.remeasureForUpdatedTextSync(mComponentContext);
      }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec);
      // Line count of the current text.
      mLineCount = getLineCount();
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
      super.onSelectionChanged(selStart, selEnd);
      if (mSelectionChangedEventHandler != null) {
        TextInput.dispatchSelectionChangedEvent(mSelectionChangedEventHandler, selStart, selEnd);
      }
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
      super.onFocusChanged(focused, direction, previouslyFocusedRect);
      if (mInputFocusChangedEventHandler != null) {
        TextInput.dispatchInputFocusChangedEvent(mInputFocusChangedEventHandler, focused);
      }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
      if (mKeyUpEventHandler != null) {
        return TextInput.dispatchKeyUpEvent(mKeyUpEventHandler, keyCode, event);
      }
      return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
      if (mKeyPreImeEventEventHandler != null) {
        return TextInput.dispatchKeyPreImeEvent(mKeyPreImeEventEventHandler, keyCode, event);
      }
      return super.onKeyPreIme(keyCode, event);
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
      if (mEditorActionEventHandler != null) {
        return TextInput.dispatchEditorActionEvent(
            mEditorActionEventHandler, actionId, event, EditTextWithEventHandlers.this);
      }
      return false;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo editorInfo) {
      InputConnection inputConnection = super.onCreateInputConnection(editorInfo);
      if (mInputConnectionEventHandler != null) {
        return TextInput.dispatchInputConnectionEvent(
            mInputConnectionEventHandler, inputConnection, editorInfo);
      }
      return inputConnection;
    }

    void setTextChangedEventHandler(
        @Nullable EventHandler<TextChangedEvent> textChangedEventHandler) {
      mTextChangedEventHandler = textChangedEventHandler;
    }

    void setSelectionChangedEventHandler(
        @Nullable EventHandler<SelectionChangedEvent> selectionChangedEventHandler) {
      mSelectionChangedEventHandler = selectionChangedEventHandler;
    }

    void setInputFocusChangedEventHandler(
        @Nullable EventHandler<InputFocusChangedEvent> inputFocusChangedEventHandler) {
      mInputFocusChangedEventHandler = inputFocusChangedEventHandler;
    }

    void setKeyUpEventHandler(@Nullable EventHandler<KeyUpEvent> keyUpEventHandler) {
      mKeyUpEventHandler = keyUpEventHandler;
    }

    void setKeyPreImeEventEventHandler(
        @Nullable EventHandler<KeyPreImeEvent> keyPreImeEventEventHandler) {
      mKeyPreImeEventEventHandler = keyPreImeEventEventHandler;
    }

    void setEditorActionEventHandler(
        @Nullable EventHandler<EditorActionEvent> editorActionEventHandler) {
      mEditorActionEventHandler = editorActionEventHandler;
    }

    void setInputConnectionEventHandler(
        @Nullable EventHandler<InputConnectionEvent> inputConnectionEventHandler) {
      mInputConnectionEventHandler = inputConnectionEventHandler;
    }

    /** Sets context for state update, when the text height has changed. */
    void setComponentContext(@Nullable ComponentContext componentContext) {
      mComponentContext = componentContext;
    }

    /** Sets reference to keep current text up to date. */
    void setTextState(@Nullable AtomicReference<CharSequence> savedText) {
      mTextState = savedText;
    }

    void attachWatchers(@Nullable List<TextWatcher> textWatchers) {
      if (textWatchers != null && textWatchers.size() > 0) {
        mTextWatcher =
            textWatchers.size() == 1 ? textWatchers.get(0) : new CompositeTextWatcher(textWatchers);
        addTextChangedListener(mTextWatcher);
      }
    }

    void detachWatchers() {
      if (mTextWatcher != null) {
        removeTextChangedListener(mTextWatcher);
        mTextWatcher = null;
      }
    }

    void setSoftInputVisibility(boolean visible) {
      final InputMethodManager imm =
          (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
      if (imm == null) {
        return;
      }

      if (visible) {
        if (imm.isActive(this)) {
          imm.showSoftInput(this, 0);
          mIsSoftInputRequested = false;
        } else {
          // Unfortunately, IMM and requesting focus has race conditions and there are cases where
          // even though the focus request went through, IMM hasn't been updated yet (thus the
          // isActive check). Posting a Runnable gives time for the Runnable the IMM Binder posts
          // to run first and update the IMM.
          post(
              new Runnable() {
                @Override
                public void run() {
                  if (mIsSoftInputRequested) {
                    imm.showSoftInput(EditTextWithEventHandlers.this, 0);
                  }
                  mIsSoftInputRequested = false;
                }
              });
          mIsSoftInputRequested = true;
        }
      } else {
        imm.hideSoftInputFromWindow(getWindowToken(), 0);
        mIsSoftInputRequested = false;
      }
    }

    static final class CompositeTextWatcher implements TextWatcher {

      private final List<TextWatcher> mTextWatchers;

      CompositeTextWatcher(List<TextWatcher> textWatchers) {
        mTextWatchers = new ArrayList<>(textWatchers);
      }

      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        for (TextWatcher w : mTextWatchers) {
          w.beforeTextChanged(s, start, count, after);
        }
      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        for (TextWatcher w : mTextWatchers) {
          w.onTextChanged(s, start, before, count);
        }
      }

      @Override
      public void afterTextChanged(Editable editable) {
        for (TextWatcher w : mTextWatchers) {
          w.afterTextChanged(editable);
        }
      }
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
  static class ForMeasureEditText extends EditText {

    public ForMeasureEditText(Context context) {
      super(context);
    }

    // This view is not intended to be drawn and invalidated
    @Override
    public void invalidate() {}

    @Override
    public void setBackground(Drawable background) {
      if (background != null) {
        background.mutate();
      }
      super.setBackground(background);
    }
  }
}
