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

import static com.facebook.litho.widget.TextInputSpec.EditTextWithEventHandlers;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.MovementMethod;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.core.util.ObjectsCompat;
import androidx.core.view.ViewCompat;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.Diff;
import com.facebook.litho.Output;
import com.facebook.litho.Size;
import com.facebook.litho.StateValue;
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
import com.google.android.material.textfield.MountableTextInputLayout;
import com.google.android.material.textfield.TextInputLayout;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;

/**
 * Component that renders an editable text input with a floating label when the hint is hidden while
 * the user inputs text, using an android {@link TextInput} wrapped in a {@link TextInputLayout}.
 *
 * @see {@link TextInput} for usage instructions
 */
@MountSpec(
    isPureRender = true,
    events = {
      TextChangedEvent.class,
      SelectionChangedEvent.class,
      KeyUpEvent.class,
      KeyPreImeEvent.class,
      EditorActionEvent.class,
      SetTextEvent.class,
      InputConnectionEvent.class,
    })
class MaterialTextInputSpec {

  private static final int UNSET = Integer.MIN_VALUE;

  @PropDefault
  protected static final ColorStateList textColorStateList = TextInputSpec.textColorStateList;

  @PropDefault
  protected static final ColorStateList hintColorStateList = TextInputSpec.hintColorStateList;

  @PropDefault static final CharSequence hint = TextInputSpec.hint;
  @PropDefault static final CharSequence initialText = TextInputSpec.initialText;
  @PropDefault protected static final int shadowColor = TextInputSpec.shadowColor;
  @PropDefault protected static final int textSize = TextInputSpec.textSize;
  @PropDefault protected static final Drawable inputBackground = TextInputSpec.inputBackground;
  @PropDefault protected static final Typeface typeface = TextInputSpec.typeface;
  @PropDefault protected static final int textAlignment = TextInputSpec.textAlignment;
  @PropDefault protected static final int gravity = TextInputSpec.gravity;
  @PropDefault protected static final boolean editable = TextInputSpec.editable;
  @PropDefault protected static final int inputType = TextInputSpec.inputType;
  @PropDefault protected static final int imeOptions = TextInputSpec.imeOptions;
  @PropDefault protected static final int cursorDrawableRes = TextInputSpec.cursorDrawableRes;
  @PropDefault static final boolean multiline = TextInputSpec.multiline;
  @PropDefault protected static final int minLines = TextInputSpec.minLines;
  @PropDefault protected static final int maxLines = TextInputSpec.maxLines;
  @PropDefault protected static final MovementMethod movementMethod = TextInputSpec.movementMethod;
  @PropDefault protected static final int boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_NONE;
  @PropDefault protected static final int editTextStartPadding = UNSET;
  @PropDefault protected static final int editTextTopPadding = UNSET;
  @PropDefault protected static final int editTextEndPadding = UNSET;
  @PropDefault protected static final int editTextBottomPadding = UNSET;

  @OnCreateInitialState
  static void onCreateInitialState(
      final ComponentContext c,
      StateValue<AtomicReference<EditTextWithEventHandlers>> mountedEditTextRef,
      StateValue<AtomicReference<CharSequence>> savedText,
      StateValue<Integer> measureSeqNumber,
      @Prop(optional = true, resType = ResType.STRING) CharSequence initialText) {
    TextInputSpec.onCreateInitialState(
        mountedEditTextRef, savedText, measureSeqNumber, initialText);
  }

  @OnLoadStyle
  static void onLoadStyle(ComponentContext c, Output<Integer> highlightColor) {
    TextInputSpec.onLoadStyle(c, highlightColor);
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
      @Prop(optional = true) boolean counterEnabled,
      @Prop(optional = true) int counterMaxLength,
      @Prop(optional = true) @TextInputLayout.BoxBackgroundMode int boxBackgroundMode,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) int boxStrokeWidth,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) int editTextStartPadding,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) int editTextTopPadding,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) int editTextEndPadding,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) int editTextBottomPadding,
      @State AtomicReference<CharSequence> savedText) {
    EditText editText =
        TextInputSpec.createAndMeasureEditText(
            c,
            layout,
            widthSpec,
            heightSpec,
            size,
            null,
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
            imeOptions,
            inputFilters,
            multiline,
            ellipsize,
            minLines,
            maxLines,
            cursorDrawableRes,
            error,
            errorDrawable,
            savedText.get());
    MountableTextInputLayout textInputLayout = new MountableTextInputLayout(c.getAndroidContext());
    setParams(
        editText,
        textInputLayout,
        hint,
        hintColorStateList,
        counterEnabled,
        counterMaxLength,
        boxBackgroundMode,
        boxStrokeWidth,
        editTextStartPadding,
        editTextTopPadding,
        editTextEndPadding,
        editTextBottomPadding);
    textInputLayout.addView(editText);

    textInputLayout.measure(
        MeasureUtils.getViewMeasureSpec(widthSpec), MeasureUtils.getViewMeasureSpec(heightSpec));

    TextInputSpec.setSizeForView(size, widthSpec, heightSpec, textInputLayout);
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
      @Prop(optional = true) Diff<Boolean> counterEnabled,
      @Prop(optional = true) Diff<Integer> counterMaxLength,
      @Prop(optional = true) @TextInputLayout.BoxBackgroundMode Diff<Integer> boxBackgroundMode,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) Diff<Integer> boxStrokeWidth,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) Diff<Integer> editTextStartPadding,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) Diff<Integer> editTextTopPadding,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) Diff<Integer> editTextEndPadding,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) Diff<Integer> editTextBottomPadding,
      @State Diff<Integer> measureSeqNumber,
      @State Diff<AtomicReference<EditTextWithEventHandlers>> mountedEditTextRef,
      @State Diff<AtomicReference<CharSequence>> savedText) {
    boolean shouldUpdateEditText =
        TextInputSpec.shouldUpdate(
            initialText,
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
            imeOptions,
            inputFilters,
            ellipsize,
            multiline,
            minLines,
            maxLines,
            cursorDrawableRes,
            movementMethod,
            error,
            measureSeqNumber,
            mountedEditTextRef,
            savedText);
    if (shouldUpdateEditText
        || !ObjectsCompat.equals(counterEnabled.getPrevious(), counterEnabled.getNext())
        || !ObjectsCompat.equals(counterMaxLength.getPrevious(), counterMaxLength.getNext())
        || !ObjectsCompat.equals(boxBackgroundMode.getPrevious(), boxBackgroundMode.getNext())
        || !ObjectsCompat.equals(boxStrokeWidth.getPrevious(), boxStrokeWidth.getNext())
        || !ObjectsCompat.equals(editTextStartPadding.getPrevious(), editTextStartPadding.getNext())
        || !ObjectsCompat.equals(editTextTopPadding.getPrevious(), editTextTopPadding.getNext())
        || !ObjectsCompat.equals(editTextEndPadding.getPrevious(), editTextEndPadding.getNext())
        || !ObjectsCompat.equals(
            editTextBottomPadding.getPrevious(), editTextBottomPadding.getNext())) {
      return true;
    }

    return false;
  }

  @OnCreateMountContent
  protected static MountableTextInputLayout onCreateMountContent(Context c) {
    MountableTextInputLayout mountableTextInputLayout = new MountableTextInputLayout(c);
    EditTextWithEventHandlers editText = new EditTextWithEventHandlers(c);
    mountableTextInputLayout.addView(editText, -1, ViewGroup.LayoutParams.MATCH_PARENT);
    return mountableTextInputLayout;
  }

  @OnMount
  static void onMount(
      final ComponentContext c,
      MountableTextInputLayout textInputLayout,
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
      @Prop(optional = true) boolean counterEnabled,
      @Prop(optional = true) int counterMaxLength,
      @Prop(optional = true) int boxBackgroundMode,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) int boxStrokeWidth,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) int editTextStartPadding,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) int editTextTopPadding,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) int editTextEndPadding,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) int editTextBottomPadding,
      @State AtomicReference<CharSequence> savedText,
      @State AtomicReference<EditTextWithEventHandlers> mountedEditTextRef) {
    EditTextWithEventHandlers editText = (EditTextWithEventHandlers) textInputLayout.getEditText();
    mountedEditTextRef.set(editText);

    TextInputSpec.setParams(
        editText,
        null,
        TextInputSpec.getBackgroundOrDefault(c, inputBackground),
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
    setParams(
        editText,
        textInputLayout,
        hint,
        hintColorStateList,
        counterEnabled,
        counterMaxLength,
        boxBackgroundMode,
        boxStrokeWidth,
        editTextStartPadding,
        editTextTopPadding,
        editTextEndPadding,
        editTextBottomPadding);
    editText.setTextState(savedText);
  }

  static void setParams(
      EditText editText,
      MountableTextInputLayout textInputLayout,
      CharSequence hint,
      ColorStateList hintColorStateList,
      boolean counterEnabled,
      int counterMaxLength,
      int boxBackgroundMode,
      int boxStrokeWidth,
      int editTextStartPadding,
      int editTextTopPadding,
      int editTextEndPadding,
      int editTextBottomPadding) {
    textInputLayout.setHint(hint);
    textInputLayout.setCounterEnabled(counterEnabled);
    textInputLayout.setCounterMaxLength(counterMaxLength);
    textInputLayout.setDefaultHintTextColor(hintColorStateList);
    // Set box background mode and stroke width in order and edit text padding to fix pre-float
    // vertical center issue
    textInputLayout.setBoxBackgroundMode(boxBackgroundMode);
    textInputLayout.setBoxStrokeWidth(boxStrokeWidth);
    if (editTextStartPadding != UNSET
        || editTextTopPadding != UNSET
        || editTextEndPadding != UNSET
        || editTextBottomPadding != UNSET) {
      int start =
          (editTextStartPadding != UNSET)
              ? editTextStartPadding
              : ViewCompat.getPaddingStart(editText);
      int top = (editTextTopPadding != UNSET) ? editTextTopPadding : editText.getPaddingTop();
      int end =
          (editTextEndPadding != UNSET) ? editTextEndPadding : ViewCompat.getPaddingEnd(editText);
      int bottom = (editTextBottomPadding != UNSET) ? editTextBottomPadding : editText.getBottom();
      editText.setPadding(start, top, end, bottom);
    }
  }

  @OnBind
  static void onBind(
      final ComponentContext c,
      MountableTextInputLayout textInputLayout,
      @Prop(optional = true, varArg = "textWatcher") List<TextWatcher> textWatchers) {
    final EditTextWithEventHandlers editText =
        (EditTextWithEventHandlers) textInputLayout.getEditText();
    TextInputSpec.onBindEditText(
        c,
        editText,
        textWatchers,
        MaterialTextInput.getTextChangedEventHandler(c),
        MaterialTextInput.getSelectionChangedEventHandler(c),
        MaterialTextInput.getKeyUpEventHandler(c),
        MaterialTextInput.getKeyPreImeEventHandler(c),
        MaterialTextInput.getEditorActionEventHandler(c),
        MaterialTextInput.getInputConnectionEventHandler(c));
  }

  @OnUnmount
  static void onUnmount(
      ComponentContext c,
      MountableTextInputLayout textInputLayout,
      @State AtomicReference<EditTextWithEventHandlers> mountedEditTextRef) {
    final EditTextWithEventHandlers editText =
        (EditTextWithEventHandlers) textInputLayout.getEditText();
    TextInputSpec.onUnmount(c, editText, mountedEditTextRef);
  }

  @OnUnbind
  static void onUnbind(final ComponentContext c, MountableTextInputLayout textInputLayout) {
    final EditTextWithEventHandlers editText =
        (EditTextWithEventHandlers) textInputLayout.getEditText();
    TextInputSpec.onUnbind(c, editText);
  }

  @OnTrigger(RequestFocusEvent.class)
  static void requestFocus(
      ComponentContext c, @State AtomicReference<EditTextWithEventHandlers> mountedEditTextRef) {
    TextInputSpec.requestFocus(c, mountedEditTextRef);
  }

  @OnTrigger(ClearFocusEvent.class)
  static void clearFocus(
      ComponentContext c, @State AtomicReference<EditTextWithEventHandlers> mountedEditTextRef) {
    TextInputSpec.clearFocus(c, mountedEditTextRef);
  }

  @OnTrigger(GetTextEvent.class)
  @Nullable
  static CharSequence getText(
      ComponentContext c,
      @State AtomicReference<EditTextWithEventHandlers> mountedEditTextRef,
      @State AtomicReference<CharSequence> savedText) {
    return TextInputSpec.getText(c, mountedEditTextRef, savedText);
  }

  @OnTrigger(SetTextEvent.class)
  static void setText(
      ComponentContext c,
      @State AtomicReference<EditTextWithEventHandlers> mountedEditTextRef,
      @State AtomicReference<CharSequence> savedText,
      @FromTrigger CharSequence text) {
    boolean shouldRemeasure = TextInputSpec.setTextEditText(mountedEditTextRef, savedText, text);
    if (shouldRemeasure) {
      MaterialTextInput.remeasureForUpdatedTextSync(c);
    }
  }

  @OnTrigger(DispatchKeyEvent.class)
  static void dispatchKey(
      ComponentContext c,
      @State AtomicReference<EditTextWithEventHandlers> mountedEditTextRef,
      @FromTrigger KeyEvent keyEvent) {
    TextInputSpec.dispatchKey(c, mountedEditTextRef, keyEvent);
  }

  @OnTrigger(SetSelectionEvent.class)
  static void setSelection(
      ComponentContext c,
      @State AtomicReference<EditTextWithEventHandlers> mountedEditTextRef,
      @FromTrigger int start,
      @FromTrigger int end) {
    TextInputSpec.setSelection(c, mountedEditTextRef, start, end);
  }

  @OnUpdateState
  static void remeasureForUpdatedText(StateValue<Integer> measureSeqNumber) {
    measureSeqNumber.set(measureSeqNumber.get() + 1);
  }
}
