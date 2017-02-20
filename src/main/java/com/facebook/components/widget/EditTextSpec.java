// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Layout;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.EditText;

import com.facebook.R;
import com.facebook.components.ComponentContext;
import com.facebook.components.ComponentLayout;
import com.facebook.components.EventHandler;
import com.facebook.components.Output;
import com.facebook.components.Size;
import com.facebook.components.annotations.MountSpec;
import com.facebook.components.annotations.OnBind;
import com.facebook.components.annotations.OnCreateMountContent;
import com.facebook.components.annotations.OnLoadStyle;
import com.facebook.components.annotations.OnMeasure;
import com.facebook.components.annotations.OnMount;
import com.facebook.components.annotations.OnUnbind;
import com.facebook.components.annotations.OnUnmount;
import com.facebook.components.annotations.Prop;
import com.facebook.components.annotations.PropDefault;
import com.facebook.components.annotations.ResType;
import com.facebook.components.utils.MeasureUtils;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static android.text.Layout.Alignment.ALIGN_NORMAL;
import static android.view.View.TEXT_ALIGNMENT_CENTER;
import static android.view.View.TEXT_ALIGNMENT_TEXT_END;
import static android.view.View.TEXT_ALIGNMENT_TEXT_START;

@MountSpec(isPureRender = true, events = {TextChangedEvent.class})
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
  @PropDefault protected static final ColorStateList textColorStateList =
      new ColorStateList(DEFAULT_TEXT_COLOR_STATE_LIST_STATES,DEFAULT_TEXT_COLOR_STATE_LIST_COLORS);
  @PropDefault protected static final int hintColor = DEFAULT_HINT_COLOR;
  @PropDefault protected static final ColorStateList hintColorStateList =
      new ColorStateList(DEFAULT_HINT_COLOR_STATE_LIST_STATES,DEFAULT_HINT_COLOR_STATE_LIST_COLORS);
  @PropDefault protected static final int linkColor = DEFAULT_COLOR;
  @PropDefault protected static final int textSize = 13;
  @PropDefault protected static final int textStyle = DEFAULT_TYPEFACE.getStyle();
  @PropDefault protected static final Typeface typeface = DEFAULT_TYPEFACE;
  @PropDefault protected static final float spacingMultiplier = 1.0f;
  @PropDefault protected static final Layout.Alignment textAlignment = ALIGN_NORMAL;
  @PropDefault protected static final int gravity = DEFAULT_GRAVITY;
  @PropDefault protected static final boolean editable = true;
  @PropDefault protected static final int selection = -1;

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
      Output<Integer> gravity) {

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
      } else if (SDK_INT >= JELLY_BEAN_MR1 &&
          attr == R.styleable.Text_android_textAlignment) {
        textAlignment.set(ALIGNMENT[a.getInteger(attr, 0)]);
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
      @Prop(optional = true, resType = ResType.DIMEN_TEXT) int textSize,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) float extraSpacing,
      @Prop(optional = true, resType = ResType.FLOAT) float spacingMultiplier,
      @Prop(optional = true) int textStyle,
      @Prop(optional = true) Typeface typeface,
      @Prop(optional = true) Layout.Alignment textAlignment,
      @Prop(optional = true) int gravity,
      @Prop(optional = true) boolean editable,
      @Prop(optional = true) int selection) {

    // TODO(11759579) - don't allocate a new EditText in every measure.
    final EditText editText = new EditText(c);

    initEditText(
        editText,
        text,
        hint,
        ellipsize,
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
        textSize,
        extraSpacing,
        spacingMultiplier,
        textStyle,
        typeface,
        textAlignment,
        gravity,
        editable,
        selection);

    editText.measure(
        MeasureUtils.getViewMeasureSpec(widthSpec),
        MeasureUtils.getViewMeasureSpec(heightSpec));

    size.width = editText.getMeasuredWidth();
    size.height = editText.getMeasuredHeight();
  }

  @OnCreateMountContent
  protected static EditTextTextTextChangedEventHandler onCreateMountContent(
      ComponentContext c) {
    return new EditTextTextTextChangedEventHandler(c);
  }

  @OnMount
  static void onMount(
      final ComponentContext c,
      EditTextTextTextChangedEventHandler editTextView,
      @Prop(optional = true, resType = ResType.STRING) CharSequence text,
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
      @Prop(optional = true, resType = ResType.DIMEN_TEXT) int textSize,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) float extraSpacing,
      @Prop(optional = true, resType = ResType.FLOAT) float spacingMultiplier,
      @Prop(optional = true) int textStyle,
      @Prop(optional = true) Typeface typeface,
      @Prop(optional = true) Layout.Alignment textAlignment,
      @Prop(optional = true) int gravity,
      @Prop(optional = true) boolean editable,
      @Prop(optional = true) int selection) {
    editTextView.setEventHandler(
        com.facebook.components.widget.EditText.getTextChangedEventHandler(c));

    initEditText(
        editTextView,
        text,
        hint,
        ellipsize,
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
        textSize,
        extraSpacing,
        spacingMultiplier,
        textStyle,
        typeface,
        textAlignment,
        gravity,
        editable,
        selection);
  }

  @OnBind
  static void onBind(
      ComponentContext c,
      EditTextTextTextChangedEventHandler editText) {
    editText.attachWatcher();
  }

  @OnUnbind
  static void onUnbind(
      ComponentContext c,
      EditTextTextTextChangedEventHandler editText) {
    editText.detachWatcher();
  }

  @OnUnmount
  static void onUnmount(
      ComponentContext c,
      EditTextTextTextChangedEventHandler editText) {
    editText.setEventHandler(null);
  }

  private static void initEditText(
      EditText editText,
      CharSequence text,
      CharSequence hint,
      TextUtils.TruncateAt ellipsize,
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
      int textSize,
      float extraSpacing,
      float spacingMultiplier,
      int textStyle,
      Typeface typeface,
      Layout.Alignment textAlignment,
      int gravity,
      boolean editable,
      int selection) {

    editText.setText(text);
    editText.setHint(hint);
    editText.setEllipsize(ellipsize);
    editText.setMinLines(minLines);
    editText.setMaxLines(maxLines);
    editText.setFilters(new InputFilter[] {new InputFilter.LengthFilter(maxLength)});
    editText.setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor);
    editText.setSingleLine(isSingleLine);
    editText.setLinkTextColor(linkColor);
    editText.setHighlightColor(highlightColor);
    editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
    editText.setLineSpacing(extraSpacing, spacingMultiplier);
    editText.setTypeface(typeface, textStyle);
    editText.setGravity(gravity);

    editText.setFocusable(editable);
    editText.setFocusableInTouchMode(editable);
    editText.setClickable(editable);
    editText.setLongClickable(editable);
    editText.setCursorVisible(editable);
    if (selection > -1) {
      editText.setSelection(selection);
    }

    if (textColor != 0) {
      editText.setTextColor(textColor);
    } else {
      editText.setTextColor(textColorStateList);
    }

    if (hintColor != 0) {
      editText.setHintTextColor(hintColor);
    } else {
      editText.setHintTextColor(hintColorStateList);
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

  static class EditTextTextTextChangedEventHandler extends EditText {
    private final TextWatcher mTextWatcher;
    private EventHandler mEventHandler;

    EditTextTextTextChangedEventHandler(Context context) {
      super(context);
      this.mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
          if (mEventHandler != null) {
            com.facebook.components.widget.EditText.dispatchTextChangedEvent(
                mEventHandler,
                s.toString());
          }
        }
      };
    }

    void attachWatcher() {
      addTextChangedListener(mTextWatcher);
    }

    void detachWatcher() {
      removeTextChangedListener(mTextWatcher);
    }

    public void setEventHandler(EventHandler eventHandler) {
      mEventHandler = eventHandler;
    }
  }
}
