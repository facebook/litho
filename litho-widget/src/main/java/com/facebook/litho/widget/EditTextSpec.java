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

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static android.text.Layout.Alignment.ALIGN_CENTER;
import static android.text.Layout.Alignment.ALIGN_NORMAL;
import static android.text.Layout.Alignment.ALIGN_OPPOSITE;
import static android.view.View.TEXT_ALIGNMENT_CENTER;
import static android.view.View.TEXT_ALIGNMENT_TEXT_END;
import static android.view.View.TEXT_ALIGNMENT_TEXT_START;
import static com.facebook.litho.widget.EditTextStateUpdatePolicy.NO_UPDATES;
import static com.facebook.litho.widget.EditTextStateUpdatePolicy.UPDATE_ON_LINE_COUNT_CHANGE;
import static com.facebook.litho.widget.EditTextStateUpdatePolicy.UPDATE_ON_TEXT_CHANGE;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Layout;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.EventHandler;
import com.facebook.litho.Output;
import com.facebook.litho.R;
import com.facebook.litho.Size;
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
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.ResType;
import com.facebook.litho.annotations.State;
import com.facebook.litho.utils.MeasureUtils;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This class is Deprecated and will not be supported. Use {@link TextInput} instead.
 *
 * <p>Component that renders an {@link EditText}.
 *
 * @prop text Text to display; changing this overrides and replaces the current text. Leave this as
 *     null to signal that the EditText's text property should be left untouched.
 * @prop initialText Initial text to display. This only takes effect if the text prop is null. If
 *     set, the value is set on the EditText exactly once: on initial mount. From then on, the
 *     EditText's text property is not modified.
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
 *     disabled only works when isSingleLine is set.
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
 *     how to let the user enter text.
 * @prop rawInputType Type of data being placed in a text field, used to help an input method decide
 *     how to let the user enter text. This prop will override inputType if both are provided.
 * @prop imeOptions Type of data in the text field, reported to an IME when it has focus.
 * @prop editorActionListener Special listener to be called when an action is performed
 * @prop requestFocus If set, attempts to give focus.
 * @prop cursorDrawableRes Drawable to set for the edit texts cursor.
 * @prop stateUpdatePolicy A policy describing when and how internal state should be updated. This
 *     does violate encapsulation, but is essential for optimization, so costly state updates, which
 *     trigger relayout, happen only when is really needed.
 * @prop inputFilter The {@link InputFilter}s to apply to the text. Usually you can use these to
 *     apply spans, restrict text input, and do general text manipulation for added text.
 * @prop textWatcher The text watchers to apply to the text. Mainly designed to add decoration spans
 *     to the text during input. Usually you should use an {@link InputFilter} instead, but an
 *     {@link InputFilter} won't allow you to decorate the text outside of the changed selection.
 * @prop highlightColor The color to apply to highlights within the text.
 * @prop hintColor The color to apply to the hint text.
 * @prop hintColorStateList A {@link ColorStateList to use for the hint text}.
 * @prop linkColor The color to apply to links within the text.
 */
@MountSpec(
    isPureRender = true,
    events = {
      TextChangedEvent.class,
      SelectionChangedEvent.class,
      KeyUpEvent.class,
      SetTextEvent.class
    })
@Deprecated
class EditTextSpec {

  private static final Layout.Alignment[] ALIGNMENT = Layout.Alignment.values();
  private static final TextUtils.TruncateAt[] TRUNCATE_AT = TextUtils.TruncateAt.values();
  private static final Typeface DEFAULT_TYPEFACE = Typeface.DEFAULT;
  private static final int DEFAULT_COLOR = 0;
  private static final int[][] DEFAULT_TEXT_COLOR_STATE_LIST_STATES = {{0}};
  private static final int[] DEFAULT_TEXT_COLOR_STATE_LIST_COLORS = {Color.BLACK};
  private static final int DEFAULT_HINT_COLOR = 0;
  private static final int[][] DEFAULT_HINT_COLOR_STATE_LIST_STATES = {{0}};
  private static final int[] DEFAULT_HINT_COLOR_STATE_LIST_COLORS = {Color.LTGRAY};
  private static final int DEFAULT_GRAVITY = Gravity.CENTER_VERTICAL | Gravity.START;

  @PropDefault protected static final int minLines = Integer.MIN_VALUE;
  @PropDefault protected static final int maxLines = Integer.MAX_VALUE;
  @PropDefault protected static final int maxLength = Integer.MAX_VALUE;
  @PropDefault protected static final int shadowColor = Color.GRAY;
  @PropDefault protected static final int textColor = DEFAULT_COLOR;

  @PropDefault
  protected static final ColorStateList textColorStateList =
      new ColorStateList(
          DEFAULT_TEXT_COLOR_STATE_LIST_STATES, DEFAULT_TEXT_COLOR_STATE_LIST_COLORS);

  @PropDefault protected static final int hintColor = DEFAULT_HINT_COLOR;

  @PropDefault
  protected static final ColorStateList hintColorStateList =
      new ColorStateList(
          DEFAULT_HINT_COLOR_STATE_LIST_STATES, DEFAULT_HINT_COLOR_STATE_LIST_COLORS);

  @PropDefault protected static final int linkColor = DEFAULT_COLOR;
  @PropDefault protected static final int textSize = TextSpec.UNSET;
  @PropDefault protected static final int textStyle = DEFAULT_TYPEFACE.getStyle();
  @PropDefault protected static final Typeface typeface = DEFAULT_TYPEFACE;
  @PropDefault protected static final float spacingMultiplier = 1.0f;
  @PropDefault protected static final Layout.Alignment textAlignment = ALIGN_NORMAL;
  @PropDefault protected static final int gravity = DEFAULT_GRAVITY;
  @PropDefault protected static final boolean editable = true;
  @PropDefault protected static final int selection = -1;

  @PropDefault
  protected static final int inputType =
      EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE;

  @PropDefault protected static final int rawInputType = EditorInfo.TYPE_NULL;
  @PropDefault protected static final int imeOptions = EditorInfo.IME_NULL;
  @PropDefault protected static final boolean isSingleLineWrap = false;
  @PropDefault protected static final boolean requestFocus = false;
  @PropDefault protected static final int cursorDrawableRes = -1;
  @PropDefault protected static final EditTextStateUpdatePolicy stateUpdatePolicy = NO_UPDATES;

  @OnLoadStyle
  static void onLoadStyle(
      ComponentContext c,
      Output<TextUtils.TruncateAt> ellipsize,
      Output<Float> spacingMultiplier,
      Output<Integer> minLines,
      Output<Integer> maxLines,
      Output<Boolean> isSingleLine,
      Output<CharSequence> text,
      Output<ColorStateList> textColorStateList,
      Output<Integer> linkColor,
      Output<Integer> highlightColor,
      Output<Integer> textSize,
      Output<Layout.Alignment> textAlignment,
      Output<Integer> textStyle,
      Output<Float> shadowRadius,
      Output<Float> shadowDx,
      Output<Float> shadowDy,
      Output<Integer> shadowColor,
      Output<Integer> gravity,
      Output<Integer> inputType,
      Output<Integer> imeOptions) {

    final TypedArray a = c.obtainStyledAttributes(R.styleable.Text, 0);

    for (int i = 0, size = a.getIndexCount(); i < size; i++) {
      final int attr = a.getIndex(i);

      if (attr == R.styleable.Text_android_text) {
        text.set(a.getString(attr));
      } else if (attr == R.styleable.Text_android_textColor) {
        textColorStateList.set(a.getColorStateList(attr));
      } else if (attr == R.styleable.Text_android_textSize) {
        textSize.set(a.getDimensionPixelSize(attr, 0));
      } else if (attr == R.styleable.Text_android_ellipsize) {
        final int index = a.getInteger(attr, 0);
        if (index > 0) {
          ellipsize.set(TRUNCATE_AT[index - 1]);
        }
      } else if (attr == R.styleable.Text_android_textAlignment) {
        if (SDK_INT >= JELLY_BEAN_MR1) {
          int viewTextAlignment = a.getInt(attr, -1);
          textAlignment.set(getAlignment(viewTextAlignment, Gravity.NO_GRAVITY));
        }
      } else if (attr == R.styleable.Text_android_minLines) {
        minLines.set(a.getInteger(attr, -1));
      } else if (attr == R.styleable.Text_android_maxLines) {
        maxLines.set(a.getInteger(attr, -1));
      } else if (attr == R.styleable.Text_android_singleLine) {
        isSingleLine.set(a.getBoolean(attr, false));
      } else if (attr == R.styleable.Text_android_textColorLink) {
        linkColor.set(a.getColor(attr, 0));
      } else if (attr == R.styleable.Text_android_textColorHighlight) {
        highlightColor.set(a.getColor(attr, 0));
      } else if (attr == R.styleable.Text_android_textStyle) {
        textStyle.set(a.getInteger(attr, 0));
      } else if (attr == R.styleable.Text_android_lineSpacingMultiplier) {
        spacingMultiplier.set(a.getFloat(attr, 0));
      } else if (attr == R.styleable.Text_android_shadowDx) {
        shadowDx.set(a.getFloat(attr, 0));
      } else if (attr == R.styleable.Text_android_shadowDy) {
        shadowDy.set(a.getFloat(attr, 0));
      } else if (attr == R.styleable.Text_android_shadowRadius) {
        shadowRadius.set(a.getFloat(attr, 0));
      } else if (attr == R.styleable.Text_android_shadowColor) {
        shadowColor.set(a.getColor(attr, 0));
      } else if (attr == R.styleable.Text_android_gravity) {
        gravity.set(a.getInteger(attr, 0));
      } else if (attr == R.styleable.Text_android_inputType) {
        inputType.set(a.getInteger(attr, 0));
      } else if (attr == R.styleable.Text_android_imeOptions) {
        imeOptions.set(a.getInteger(attr, 0));
      }
    }

    a.recycle();
  }

  @OnMeasure
  static void onMeasure(
      ComponentContext c,
      ComponentLayout layout,
      int widthSpec,
      int heightSpec,
      Size size,
      @Prop(optional = true, resType = ResType.STRING) CharSequence text,
      @Prop(optional = true, resType = ResType.STRING) CharSequence initialText,
      @Prop(optional = true, resType = ResType.STRING) CharSequence hint,
      @Prop(optional = true) TextUtils.TruncateAt ellipsize,
      @Prop(optional = true, resType = ResType.INT) int minLines,
      @Prop(optional = true, resType = ResType.INT) int maxLines,
      @Prop(optional = true, resType = ResType.INT) int maxLength,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) float shadowRadius,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) float shadowDx,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) float shadowDy,
      @Prop(optional = true, resType = ResType.COLOR) int shadowColor,
      @Prop(optional = true, resType = ResType.BOOL) boolean isSingleLine,
      @Prop(optional = true, resType = ResType.COLOR) int textColor,
      @Prop(optional = true) ColorStateList textColorStateList,
      @Prop(optional = true, resType = ResType.COLOR) int hintColor,
      @Prop(optional = true) ColorStateList hintColorStateList,
      @Prop(optional = true, resType = ResType.COLOR) int linkColor,
      @Prop(optional = true, resType = ResType.COLOR) int highlightColor,
      @Prop(optional = true) ColorStateList tintColorStateList,
      @Prop(optional = true, resType = ResType.DIMEN_TEXT) int textSize,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) float extraSpacing,
      @Prop(optional = true, resType = ResType.FLOAT) float spacingMultiplier,
      @Prop(optional = true) int textStyle,
      @Prop(optional = true) Typeface typeface,
      @Prop(optional = true) Layout.Alignment textAlignment,
      @Prop(optional = true) int gravity,
      @Prop(optional = true) boolean editable,
      @Prop(optional = true) int selection,
      @Prop(optional = true) int inputType,
      @Prop(optional = true) int rawInputType,
      @Prop(optional = true) int imeOptions,
      @Prop(optional = true) TextView.OnEditorActionListener editorActionListener,
      @Prop(optional = true) boolean isSingleLineWrap,
      @Prop(optional = true) boolean requestFocus,
      @Prop(optional = true) int cursorDrawableRes,
      @Prop(optional = true, varArg = "inputFilter") List<InputFilter> inputFilters,
      @State(canUpdateLazily = true) CharSequence input) {

    // TODO(11759579) - don't allocate a new EditText in every measure.
    final EditTextForMeasure editText = new EditTextForMeasure(c.getAndroidContext());

    initEditText(
        editText,
        input == null ? text : input,
        // We want to use the initialText value for *every* measure, not just the first one.
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
        cursorDrawableRes);

    Drawable background = layout.getBackground();

    if (background != null) {
      Rect rect = new Rect();
      background.getPadding(rect);

      if (rect.left != 0 || rect.top != 0 || rect.right != 0 || rect.bottom != 0) {
        // Padding from the background will be added to the layout separately, so does not need to
        // be a part of this measurement.
        editText.setPadding(0, 0, 0, 0);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
          editText.setBackgroundDrawable(null);
        } else {
          editText.setBackground(null);
        }
      }
    }

    editText.measure(
        MeasureUtils.getViewMeasureSpec(widthSpec), MeasureUtils.getViewMeasureSpec(heightSpec));

    size.width = editText.getMeasuredWidth();
    size.height = editText.getMeasuredHeight();
  }

  @OnCreateMountContent
  protected static EditTextWithEventHandlers onCreateMountContent(Context c) {
    return new EditTextWithEventHandlers(c);
  }

  @OnMount
  static void onMount(
      final ComponentContext c,
      EditTextWithEventHandlers editText,
      @Prop(optional = true, resType = ResType.STRING) CharSequence text,
      @Prop(optional = true, resType = ResType.STRING) CharSequence initialText,
      @Prop(optional = true, resType = ResType.STRING) CharSequence hint,
      @Prop(optional = true) TextUtils.TruncateAt ellipsize,
      @Prop(optional = true, resType = ResType.INT) int minLines,
      @Prop(optional = true, resType = ResType.INT) int maxLines,
      @Prop(optional = true, resType = ResType.INT) int maxLength,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) float shadowRadius,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) float shadowDx,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) float shadowDy,
      @Prop(optional = true, resType = ResType.COLOR) int shadowColor,
      @Prop(optional = true, resType = ResType.BOOL) boolean isSingleLine,
      @Prop(optional = true, resType = ResType.COLOR) int textColor,
      @Prop(optional = true) ColorStateList textColorStateList,
      @Prop(optional = true, resType = ResType.COLOR) int hintColor,
      @Prop(optional = true) ColorStateList hintColorStateList,
      @Prop(optional = true, resType = ResType.COLOR) int linkColor,
      @Prop(optional = true, resType = ResType.COLOR) int highlightColor,
      @Prop(optional = true) ColorStateList tintColorStateList,
      @Prop(optional = true, resType = ResType.DIMEN_TEXT) int textSize,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) float extraSpacing,
      @Prop(optional = true, resType = ResType.FLOAT) float spacingMultiplier,
      @Prop(optional = true) int textStyle,
      @Prop(optional = true) Typeface typeface,
      @Prop(optional = true) Layout.Alignment textAlignment,
      @Prop(optional = true) int gravity,
      @Prop(optional = true) boolean editable,
      @Prop(optional = true) int selection,
      @Prop(optional = true) int inputType,
      @Prop(optional = true) int rawInputType,
      @Prop(optional = true) int imeOptions,
      @Prop(optional = true) TextView.OnEditorActionListener editorActionListener,
      @Prop(optional = true) boolean isSingleLineWrap,
      @Prop(optional = true) boolean requestFocus,
      @Prop(optional = true) int cursorDrawableRes,
      @Prop(optional = true, varArg = "inputFilter") List<InputFilter> inputFilters,
      @State AtomicReference<EditTextWithEventHandlers> mountedView,
      @State AtomicBoolean configuredInitialText,
      @State(canUpdateLazily = true) CharSequence input) {

    mountedView.set(editText);

    initEditText(
        editText,
        input == null ? text : input,
        // Only set initialText on the EditText during the very first mount.
        configuredInitialText.getAndSet(true) ? null : initialText,
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
        cursorDrawableRes);
  }

  @OnBind
  static void onBind(
      ComponentContext c,
      EditTextWithEventHandlers editText,
      @Prop(optional = true) EditTextStateUpdatePolicy stateUpdatePolicy,
      @Prop(optional = true, varArg = "textWatcher") List<TextWatcher> textWatchers) {
    editText.setComponentContext(c);
    editText.setTextChangedEventHandler(
        com.facebook.litho.widget.EditText.getTextChangedEventHandler(c));
    editText.setSelectionChangedEventHandler(
        com.facebook.litho.widget.EditText.getSelectionChangedEventHandler(c));
    editText.setKeyUpEventHandler(com.facebook.litho.widget.EditText.getKeyUpEventHandler(c));
    editText.setStateUpdatePolicy(stateUpdatePolicy);
    editText.attachWatchers(textWatchers);
  }

  @OnUnbind
  static void onUnbind(ComponentContext c, EditTextWithEventHandlers editText) {
    editText.detachWatchers();
    editText.clear();
  }

  @OnUnmount
  static void onUnmount(
      ComponentContext c,
      EditTextWithEventHandlers editText,
      @State AtomicReference<EditTextWithEventHandlers> mountedView) {
    mountedView.set(null);
  }

  @OnTrigger(RequestFocusEvent.class)
  static void requestFocus(
      ComponentContext c, @State AtomicReference<EditTextWithEventHandlers> mountedView) {
    EditTextWithEventHandlers eventHandler = mountedView.get();
    if (eventHandler != null) {
      if (eventHandler.requestFocus()) {
        InputMethodManager imm =
            (InputMethodManager)
                c.getAndroidContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(eventHandler, 0);
      }
    }
  }

  @OnTrigger(ClearFocusEvent.class)
  static void clearFocus(
      ComponentContext c, @State AtomicReference<EditTextWithEventHandlers> mountedView) {
    EditTextWithEventHandlers eventHandler = mountedView.get();
    if (eventHandler != null) {
      eventHandler.clearFocus();
      InputMethodManager imm =
          (InputMethodManager) c.getAndroidContext().getSystemService(Context.INPUT_METHOD_SERVICE);
      imm.hideSoftInputFromWindow(eventHandler.getWindowToken(), 0);
    }
  }

  @OnTrigger(SetTextEvent.class)
  static void setText(
      ComponentContext c,
      @State AtomicReference<EditTextWithEventHandlers> mountedView,
      @FromTrigger CharSequence text) {
    ThreadUtils.assertMainThread();

    com.facebook.litho.widget.EditText.lazyUpdateInput(c, text);

    final EditTextWithEventHandlers view = mountedView.get();
    if (view != null) {
      view.setText(text);
    }
  }

  @OnUpdateState
  static void updateInput(StateValue<CharSequence> input, @Param CharSequence newInput) {
    input.set(newInput);
  }

  @OnCreateInitialState
  static void onCreateInitialState(
      StateValue<AtomicReference<EditTextWithEventHandlers>> mountedView,
      StateValue<AtomicBoolean> configuredInitialText) {
    mountedView.set(new AtomicReference<EditTextWithEventHandlers>());
    configuredInitialText.set(new AtomicBoolean());
  }

  private static void initEditText(
      EditText editText,
      CharSequence text,
      CharSequence initialText,
      CharSequence hint,
      TextUtils.TruncateAt ellipsize,
      @Nullable List<InputFilter> inputFilters,
      int minLines,
      int maxLines,
      int maxLength,
      float shadowRadius,
      float shadowDx,
      float shadowDy,
      int shadowColor,
      boolean isSingleLine,
      int textColor,
      ColorStateList textColorStateList,
      int hintColor,
      ColorStateList hintColorStateList,
      int linkColor,
      int highlightColor,
      ColorStateList tintColorStateList,
      int textSize,
      float extraSpacing,
      float spacingMultiplier,
      int textStyle,
      Typeface typeface,
      Layout.Alignment textAlignment,
      int gravity,
      boolean editable,
      int selection,
      int inputType,
      int rawInputType,
      int imeOptions,
      TextView.OnEditorActionListener editorActionListener,
      boolean isSingleLineWrap,
      boolean requestFocus,
      int cursorDrawableRes) {

    if (textSize == TextSpec.UNSET) {
      editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, TextSpec.DEFAULT_TEXT_SIZE_SP);
    } else {
      editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
    }

    // We only want to change the input type if it actually needs changing, and we need to take
    // isSingleLine into account so that we get the correct input type.
    if (isSingleLine) {
      inputType &= ~EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE;
    } else {
      inputType |= EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE;
    }

    if (rawInputType != EditorInfo.TYPE_NULL) {
      editText.setSingleLine(isSingleLine);
      editText.setRawInputType(rawInputType);
    } else if (inputType != editText.getInputType()) {
      editText.setSingleLine(isSingleLine);
      // Needs to be set before min/max lines. Also calling setSingleLine() affects inputType, thus
      // we should re-set input type every time we call setSingleLine()
      editText.setInputType(inputType);
    }

    // disable horizontally scroll in single line mode to make the text wrap.
    if (isSingleLine && isSingleLineWrap) {
      editText.setHorizontallyScrolling(false);
    }

    // Needs to be set before the text so it would apply to the current text
    InputFilter.LengthFilter lengthFilter = new InputFilter.LengthFilter(maxLength);
    if (inputFilters == null) {
      editText.setFilters(new InputFilter[] {lengthFilter});
    } else {
      inputFilters = new ArrayList<>(inputFilters);
      inputFilters.add(lengthFilter);
      editText.setFilters(inputFilters.toArray(new InputFilter[inputFilters.size()]));
    }

    // If it's the same text, don't set it again so that the caret won't move to the beginning or
    // end of the string. Only looking at String instances in order to avoid span comparisons.
    if (!(text instanceof String) || !text.equals(editText.getText().toString())) {
      editText.setText(text);
    } else if (initialText != null) {
      editText.setText(initialText);
    }

    // Setting the hint causes API 28 to lose the focus of the currently selected
    // text input. This happens during LithoState updates. Only set the hint when necessary
    // to try to avoid this issue.
    CharSequence oldHint = editText.getHint();
    boolean hintsAreEqual = (oldHint == hint) || (oldHint != null && oldHint.equals(hint));
    if (!hintsAreEqual) {
      editText.setHint(hint);
    }

    editText.setEllipsize(ellipsize);
    editText.setMinLines(minLines);
    editText.setMaxLines(maxLines);
    editText.setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor);
    editText.setLinkTextColor(linkColor);
    editText.setHighlightColor(highlightColor);
    editText.setLineSpacing(extraSpacing, spacingMultiplier);
    editText.setTypeface(typeface, textStyle);
    editText.setGravity(gravity);

    editText.setImeOptions(imeOptions);
    editText.setOnEditorActionListener(editorActionListener);

    editText.setFocusable(editable);
    editText.setFocusableInTouchMode(editable);
    editText.setClickable(editable);
    editText.setLongClickable(editable);
    editText.setCursorVisible(editable);
    @Nullable Editable editableText = editText.getText();
    int textLength = editableText != null ? editableText.length() : -1;
    if (selection > -1 && selection <= textLength) {
      editText.setSelection(selection);
    }

    if (textColor != 0 || textColorStateList == null) {
      editText.setTextColor(textColor);
    } else {
      editText.setTextColor(textColorStateList);
    }

    if (hintColor != 0 || hintColorStateList == null) {
      editText.setHintTextColor(hintColor);
    } else {
      editText.setHintTextColor(hintColorStateList);
    }

    if (tintColorStateList != null) {
      ViewCompat.setBackgroundTintList(editText, tintColorStateList);
    }

    if (requestFocus) {
      editText.requestFocus();
    }

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

    switch (textAlignment) {
      case ALIGN_NORMAL:
        if (SDK_INT >= JELLY_BEAN_MR1) {
          editText.setTextAlignment(TEXT_ALIGNMENT_TEXT_START);
        } else {
          editText.setGravity(gravity | Gravity.LEFT);
        }
        break;
      case ALIGN_OPPOSITE:
        if (SDK_INT >= JELLY_BEAN_MR1) {
          editText.setTextAlignment(TEXT_ALIGNMENT_TEXT_END);
        } else {
          editText.setGravity(gravity | Gravity.RIGHT);
        }
        break;
      case ALIGN_CENTER:
        if (SDK_INT >= JELLY_BEAN_MR1) {
          editText.setTextAlignment(TEXT_ALIGNMENT_CENTER);
        } else {
          editText.setGravity(gravity | Gravity.CENTER_HORIZONTAL);
        }
        break;
    }
  }

  static class EditTextWithEventHandlers extends EditText {
    private final DelegatingTextWatcher mTextWatcher;
    private ComponentContext mComponentContext;
    private EditTextStateUpdatePolicy mStateUpdatePolicy;
    private EventHandler mTextChangedEventHandler;
    private EventHandler mSelectionChangedEventHandler;
    private EventHandler mKeyUpEventHandler;

    private class DelegatingTextWatcher implements TextWatcher {

      @Nullable List<TextWatcher> mDelegates;
      int mPrevLineCount;

      public void setDelegates(@Nullable List<TextWatcher> delegates) {
        mDelegates = delegates;
      }

      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        if (mDelegates != null) {
          for (int i = 0, stop = mDelegates.size(); i < stop; i++) {
            mDelegates.get(i).beforeTextChanged(s, start, count, after);
          }
        }
        // Only need the previous line count when state update policy is ON_LINE_COUNT_CHANGE
        if (mStateUpdatePolicy == UPDATE_ON_LINE_COUNT_CHANGE) {
          mPrevLineCount = getLineCount();
        }
      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (mDelegates != null) {
          for (int i = 0, stop = mDelegates.size(); i < stop; i++) {
            mDelegates.get(i).onTextChanged(s, start, before, count);
          }
        }
        if ((mStateUpdatePolicy == UPDATE_ON_LINE_COUNT_CHANGE && mPrevLineCount != getLineCount())
            || mStateUpdatePolicy == UPDATE_ON_TEXT_CHANGE) {
          com.facebook.litho.widget.EditText.updateInputSync(mComponentContext, s.toString());
        } else if (mStateUpdatePolicy != NO_UPDATES) {
          com.facebook.litho.widget.EditText.lazyUpdateInput(mComponentContext, s.toString());
        }
      }

      @Override
      public void afterTextChanged(Editable s) {
        if (mDelegates != null) {
          for (int i = 0, stop = mDelegates.size(); i < stop; i++) {
            mDelegates.get(i).afterTextChanged(s);
          }
        }
        if (mTextChangedEventHandler != null) {
          com.facebook.litho.widget.EditText.dispatchTextChangedEvent(
              mTextChangedEventHandler, EditTextWithEventHandlers.this, s.toString());
        }
      }
    }

    EditTextWithEventHandlers(Context context) {
      super(context);
      this.mTextWatcher = new DelegatingTextWatcher();
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
      super.onSelectionChanged(selStart, selEnd);
      if (mSelectionChangedEventHandler != null) {
        com.facebook.litho.widget.EditText.dispatchSelectionChangedEvent(
            mSelectionChangedEventHandler, selStart, selEnd);
      }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
      if (mKeyUpEventHandler != null) {
        com.facebook.litho.widget.EditText.dispatchKeyUpEvent(mKeyUpEventHandler, keyCode, event);
      }
      return super.onKeyUp(keyCode, event);
    }

    void setStateUpdatePolicy(EditTextStateUpdatePolicy stateUpdatePolicy) {
      mStateUpdatePolicy = stateUpdatePolicy;
    }

    void setComponentContext(ComponentContext componentContext) {
      mComponentContext = componentContext;
    }

    void setTextChangedEventHandler(EventHandler textChangedEventHandler) {
      mTextChangedEventHandler = textChangedEventHandler;
    }

    void setSelectionChangedEventHandler(EventHandler selectionChangedEventHandler) {
      mSelectionChangedEventHandler = selectionChangedEventHandler;
    }

    void setKeyUpEventHandler(EventHandler keyUpEventHandler) {
      mKeyUpEventHandler = keyUpEventHandler;
    }

    void clear() {
      mStateUpdatePolicy = stateUpdatePolicy;
      mComponentContext = null;
      mTextChangedEventHandler = null;
      mSelectionChangedEventHandler = null;
      mKeyUpEventHandler = null;
    }

    void attachWatchers(@Nullable List<TextWatcher> textWatchers) {
      mTextWatcher.setDelegates(textWatchers);
      addTextChangedListener(mTextWatcher);
    }

    void detachWatchers() {
      mTextWatcher.setDelegates(null);
      removeTextChangedListener(mTextWatcher);
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
  static class EditTextForMeasure extends EditText {
    EditTextForMeasure(Context context) {
      super(context);
    }

    @Override
    public void setBackground(Drawable background) {
      if (background != null) {
        background.mutate();
      }
      super.setBackground(background);
    }
  }

  private static Layout.Alignment getAlignment(int viewTextAlignment, int gravity) {
    final Layout.Alignment alignment;
    // This was copied from TextSpec for handling text alignment
    switch (viewTextAlignment) {
      case View.TEXT_ALIGNMENT_GRAVITY:
        alignment = getAlignment(gravity);
        break;
      case View.TEXT_ALIGNMENT_TEXT_START:
        alignment = ALIGN_NORMAL;
        break;
      case View.TEXT_ALIGNMENT_TEXT_END:
        alignment = ALIGN_OPPOSITE;
        break;
      case View.TEXT_ALIGNMENT_CENTER:
        alignment = ALIGN_CENTER;
        break;
      case View.TEXT_ALIGNMENT_VIEW_START: // unsupported, default to normal
        alignment = ALIGN_NORMAL;
        break;
      case View.TEXT_ALIGNMENT_VIEW_END: // unsupported, default to opposite
        alignment = ALIGN_OPPOSITE;
        break;
      case View.TEXT_ALIGNMENT_INHERIT: // unsupported, default to gravity
        alignment = getAlignment(gravity);
        break;
      default:
        alignment = textAlignment;
        break;
    }
    return alignment;
  }

  private static Layout.Alignment getAlignment(int gravity) {
    final Layout.Alignment alignment;
    // This was copied from TextSpec for handling text alignment
    switch (gravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) {
      case Gravity.START:
        alignment = ALIGN_NORMAL;
        break;
      case Gravity.END:
        alignment = ALIGN_OPPOSITE;
        break;
      case Gravity.LEFT: // unsupported, default to normal
        alignment = ALIGN_NORMAL;
        break;
      case Gravity.RIGHT: // unsupported, default to opposite
        alignment = ALIGN_OPPOSITE;
        break;
      case Gravity.CENTER_HORIZONTAL:
        alignment = ALIGN_CENTER;
        break;
      default:
        alignment = textAlignment;
        break;
    }
    return alignment;
  }
}
