/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;


import static android.support.v4.widget.ExploreByTouchHelper.INVALID_ID;
import static android.text.Layout.Alignment.ALIGN_CENTER;
import static android.text.Layout.Alignment.ALIGN_NORMAL;
import static android.text.Layout.Alignment.ALIGN_OPPOSITE;
import static com.facebook.litho.FrameworkLogEvents.EVENT_ERROR;
import static com.facebook.litho.FrameworkLogEvents.PARAM_MESSAGE;
import static com.facebook.litho.SizeSpec.AT_MOST;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.UNSPECIFIED;
import static com.facebook.litho.annotations.ResType.BOOL;
import static com.facebook.litho.annotations.ResType.STRING;
import static com.facebook.litho.widget.VerticalGravity.BOTTOM;
import static com.facebook.litho.widget.VerticalGravity.CENTER;
import static com.facebook.litho.widget.VerticalGravity.TOP;

import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.support.v4.text.TextDirectionHeuristicCompat;
import android.support.v4.text.TextDirectionHeuristicsCompat;
import android.support.v4.util.Pools.SynchronizedPool;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.text.Layout;
import android.text.Layout.Alignment;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import com.facebook.fbui.textlayoutbuilder.TextLayoutBuilder;
import com.facebook.fbui.textlayoutbuilder.util.LayoutMeasureUtil;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.ComponentsLogger;
import com.facebook.litho.EventHandler;
import com.facebook.litho.LogEvent;
import com.facebook.litho.Output;
import com.facebook.litho.R;
import com.facebook.litho.Size;
import com.facebook.litho.SizeSpec;
import com.facebook.litho.annotations.FromBoundsDefined;
import com.facebook.litho.annotations.FromMeasure;
import com.facebook.litho.annotations.GetExtraAccessibilityNodeAt;
import com.facebook.litho.annotations.GetExtraAccessibilityNodesCount;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnBoundsDefined;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnLoadStyle;
import com.facebook.litho.annotations.OnMeasure;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.OnPopulateAccessibilityNode;
import com.facebook.litho.annotations.OnPopulateExtraAccessibilityNode;
import com.facebook.litho.annotations.OnUnmount;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.ResType;
import com.facebook.litho.utils.DisplayListUtils;
import com.facebook.widget.accessibility.delegates.AccessibleClickableSpan;
import com.facebook.yoga.YogaDirection;

/**
 * Component to render text.
 *
 * @uidocs
 * @prop text Text to display.
 * @prop ellipsize If set, specifies the position of the text to be ellipsized.
 * @prop minLines Minimum number of lines to show.
 * @prop maxLines Maximum number of lines to show.
 * @prop minEms Makes the text to be mim ems wide.
 * @prop maxEms Makes the text to be max ems wide.
 * @prop minTextWidth Makes the text to be min pixels wide.
 * @prop maxTextWidth Makes the text to be max pixels wide.
 * @prop shadowRadius Blur radius of the shadow.
 * @prop shadowDx Horizontal offset of the shadow.
 * @prop shadowDy Vertical offset of the shadow.
 * @prop shadowColor Color for the shadow underneath the text.
 * @prop isSingleLine If set, makes the text to be rendered in a single line.
 * @prop textColor Color of the text.
 * @prop textColorStateList ColorStateList of the text.
 * @prop linkColor Color for links in the text.
 * @prop textSize Size of the text.
 * @prop extraSpacing Extra spacing between the lines of text.
 * @prop spacingMultiplier Extra spacing between the lines of text, as a multiplier.
 * @prop textStyle Style (bold, italic, bolditalic) for the text.
 * @prop typeface Typeface for the text.
 * @prop textAlignment Alignment of the text within its container.
 * @prop glyphWarming If set, pre-renders the text to an off-screen Canvas to boost performance.
 * @prop textDirection Heuristic to use to determine the direction of the text.
 * @prop shouldIncludeFontPadding If set, uses extra padding for ascenders and descenders.
 * @prop verticalGravity Vertical gravity for the text within its container.
 */
@MountSpec(
    isPureRender = true,
    shouldUseDisplayList = true,
    poolSize = 30,
    events = {TextOffsetOnTouchEvent.class})
class TextSpec {
  static {
    SynchronizedTypefaceHelper.setupSynchronizedTypeface();
  }

  private static final TruncateAt[] TRUNCATE_AT = TruncateAt.values();

  private static final Typeface DEFAULT_TYPEFACE = Typeface.DEFAULT;
  private static final int DEFAULT_COLOR = 0;
  private static final int DEFAULT_EMS = -1;
  private static final int DEFAULT_MIN_WIDTH = 0;
  private static final int DEFAULT_MAX_WIDTH = Integer.MAX_VALUE;

  private static final int[][] DEFAULT_TEXT_COLOR_STATE_LIST_STATES = {{0}};
  private static final int[] DEFAULT_TEXT_COLOR_STATE_LIST_COLORS = {Color.BLACK};
  private static final int[] DEFAULT_TEXT_DRAWABLE_STATE = {android.R.attr.state_enabled};

  private static final String TAG = "TextSpec";

  @PropDefault protected static final int minLines = Integer.MIN_VALUE;
  @PropDefault protected static final int maxLines = Integer.MAX_VALUE;
  @PropDefault protected static final int minEms = DEFAULT_EMS;
  @PropDefault protected static final int maxEms = DEFAULT_EMS;
  @PropDefault protected static final int minTextWidth = DEFAULT_MIN_WIDTH;
  @PropDefault protected static final int maxTextWidth = DEFAULT_MAX_WIDTH;
  @PropDefault protected static final int shadowColor = Color.GRAY;
  @PropDefault protected static final int textColor = DEFAULT_COLOR;
  @PropDefault protected static final int linkColor = DEFAULT_COLOR;
  @PropDefault protected static final ColorStateList textColorStateList = new ColorStateList(
      DEFAULT_TEXT_COLOR_STATE_LIST_STATES,
      DEFAULT_TEXT_COLOR_STATE_LIST_COLORS);
  @PropDefault protected static final int textSize = 13;
  @PropDefault protected static final int textStyle = DEFAULT_TYPEFACE.getStyle();
  @PropDefault protected static final Typeface typeface = DEFAULT_TYPEFACE;
  @PropDefault protected static final float spacingMultiplier = 1.0f;
  @PropDefault protected static final VerticalGravity verticalGravity = VerticalGravity.TOP;
  @PropDefault protected static final boolean glyphWarming = false;
  @PropDefault protected static final boolean shouldIncludeFontPadding = true;
  @PropDefault protected static final Alignment textAlignment = ALIGN_NORMAL;
  @PropDefault protected static final int highlightStartOffset = -1;
  @PropDefault protected static final int highlightEndOffset = -1;

  private static final Path sTempPath = new Path();
  private static final Rect sTempRect = new Rect();
  private static final RectF sTempRectF = new RectF();

  private static final SynchronizedPool<TextLayoutBuilder> sTextLayoutBuilderPool =
      new SynchronizedPool<>(2);

  @OnLoadStyle
  static void onLoadStyle(
      ComponentContext c,
      Output<TruncateAt> ellipsize,
      Output<Boolean> shouldIncludeFontPadding,
      Output<Float> spacingMultiplier,
      Output<Integer> minLines,
      Output<Integer> maxLines,
      Output<Integer> minEms,
      Output<Integer> maxEms,
      Output<Integer> minTextWidth,
      Output<Integer> maxTextWidth,
      Output<Boolean> isSingleLine,
      Output<CharSequence> text,
      Output<ColorStateList> textColorStateList,
      Output<Integer> linkColor,
      Output<Integer> highlightColor,
      Output<Integer> textSize,
      Output<Alignment> textAlignment,
      Output<Integer> textStyle,
      Output<Float> shadowRadius,
      Output<Float> shadowDx,
      Output<Float> shadowDy,
      Output<Integer> shadowColor,
      Output<VerticalGravity> verticalGravity,
      Output<Typeface> typeface) {

    //check first if provided attributes contain textAppearance. As an analogy to TextView behavior,
    //we will parse textAppearance attributes first and then will override leftovers from main style
    TypedArray a = c.obtainStyledAttributes(
        R.styleable.Text_TextAppearanceAttr,
        0);

    int textAppearanceResId = a.getResourceId(
        R.styleable.Text_TextAppearanceAttr_android_textAppearance,
        -1);
    a.recycle();
    if (textAppearanceResId != -1) {
      a = c.getTheme().obtainStyledAttributes(
          textAppearanceResId,
          R.styleable.Text);
      resolveStyleAttrsForTypedArray(
          a,
          ellipsize,
          shouldIncludeFontPadding,
          spacingMultiplier,
          minLines,
          maxLines,
          minEms,
          maxEms,
          minTextWidth,
          maxTextWidth,
          isSingleLine,
          text,
          textColorStateList,
          linkColor,
          highlightColor,
          textSize,
          textAlignment,
          textStyle,
          shadowRadius,
          shadowDx,
          shadowDy,
          shadowColor,
          verticalGravity,
          typeface);
      a.recycle();
    }

    //now (after we parsed textAppearance) we can move on to main style attributes
    a = c.obtainStyledAttributes(
        R.styleable.Text,
        0);
    resolveStyleAttrsForTypedArray(
        a,
        ellipsize,
        shouldIncludeFontPadding,
        spacingMultiplier,
        minLines,
        maxLines,
        minEms,
        maxEms,
        minTextWidth,
        maxTextWidth,
        isSingleLine,
        text,
        textColorStateList,
        linkColor,
        highlightColor,
        textSize,
        textAlignment,
        textStyle,
        shadowRadius,
        shadowDx,
        shadowDy,
        shadowColor,
        verticalGravity,
        typeface);

    a.recycle();
  }

  private static void resolveStyleAttrsForTypedArray(
      TypedArray a,
      Output<TruncateAt> ellipsize,
      Output<Boolean> shouldIncludeFontPadding,
      Output<Float> spacingMultiplier,
      Output<Integer> minLines,
      Output<Integer> maxLines,
      Output<Integer> minEms,
      Output<Integer> maxEms,
      Output<Integer> minTextWidth,
      Output<Integer> maxTextWidth,
      Output<Boolean> isSingleLine,
      Output<CharSequence> text,
      Output<ColorStateList> textColorStateList,
      Output<Integer> linkColor,
      Output<Integer> highlightColor,
      Output<Integer> textSize,
      Output<Alignment> textAlignment,
      Output<Integer> textStyle,
      Output<Float> shadowRadius,
      Output<Float> shadowDx,
      Output<Float> shadowDy,
      Output<Integer> shadowColor,
      Output<VerticalGravity> verticalGravity,
      Output<Typeface> typeface
  ) {
    int viewTextAlignment = View.TEXT_ALIGNMENT_GRAVITY;
    int gravity = Gravity.NO_GRAVITY;
    String fontFamily = null;

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
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 &&
          attr == R.styleable.Text_android_textAlignment) {
        viewTextAlignment = a.getInt(attr, -1);
        textAlignment.set(getAlignment(viewTextAlignment, gravity));
      } else if (attr == R.styleable.Text_android_gravity) {
        gravity = a.getInt(attr, -1);
        textAlignment.set(getAlignment(viewTextAlignment, gravity));
        verticalGravity.set(getVerticalGravity(gravity));
      } else if (attr == R.styleable.Text_android_includeFontPadding) {
        shouldIncludeFontPadding.set(a.getBoolean(attr, false));
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
      } else if (attr == R.styleable.Text_android_minEms) {
        minEms.set(a.getInteger(attr, DEFAULT_EMS));
      } else if (attr == R.styleable.Text_android_maxEms) {
        maxEms.set(a.getInteger(attr, DEFAULT_EMS));
      } else if (attr == R.styleable.Text_android_minWidth) {
        minTextWidth.set(a.getDimensionPixelSize(attr, DEFAULT_MIN_WIDTH));
      } else if (attr == R.styleable.Text_android_maxWidth) {
        maxTextWidth.set(a.getDimensionPixelSize(attr, DEFAULT_MAX_WIDTH));
      } else if (attr == R.styleable.Text_android_fontFamily) {
        fontFamily = a.getString(attr);
      }
    }

    if (fontFamily != null) {
      final Integer styleValue = textStyle.get();
      // https://github.com/facebook/litho/issues/204
      // Typeface.create() reads from a static cache and is not thread-safe.
      // Disable support until we can use our own cache or come up with another solution.
      // typeface.set(Typeface.create(fontFamily, styleValue == null ? -1 : styleValue));
    }
  }

  @OnMeasure
  static void onMeasure(
      ComponentContext context,
      ComponentLayout layout,
      int widthSpec,
      int heightSpec,
      Size size,
      @Prop(resType = ResType.STRING) CharSequence text,
      @Prop(optional = true) TruncateAt ellipsize,
      @Prop(optional = true, resType = ResType.BOOL) boolean shouldIncludeFontPadding,
      @Prop(optional = true, resType = ResType.INT) int minLines,
      @Prop(optional = true, resType = ResType.INT) int maxLines,
      @Prop(optional = true, resType = ResType.INT) int minEms,
      @Prop(optional = true, resType = ResType.INT) int maxEms,
      @Prop(optional = true, resType = ResType.DIMEN_SIZE) int minTextWidth,
      @Prop(optional = true, resType = ResType.DIMEN_SIZE) int maxTextWidth,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) float shadowRadius,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) float shadowDx,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) float shadowDy,
      @Prop(optional = true, resType = ResType.COLOR) int shadowColor,
      @Prop(optional = true, resType = ResType.BOOL) boolean isSingleLine,
      @Prop(optional = true, resType = ResType.COLOR) int textColor,
      @Prop(optional = true) ColorStateList textColorStateList,
      @Prop(optional = true, resType = ResType.COLOR) int linkColor,
      @Prop(optional = true, resType = ResType.DIMEN_TEXT) int textSize,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) float extraSpacing,
      @Prop(optional = true, resType = ResType.FLOAT) float spacingMultiplier,
      @Prop(optional = true) int textStyle,
      @Prop(optional = true) Typeface typeface,
      @Prop(optional = true) Alignment textAlignment,
      @Prop(optional = true) boolean glyphWarming,
      @Prop(optional = true) TextDirectionHeuristicCompat textDirection,
      Output<Layout> measureLayout,
      Output<Integer> measuredWidth,
      Output<Integer> measuredHeight) {

    if (TextUtils.isEmpty(text)) {
      measureLayout.set(null);
      size.width = 0;
      size.height = 0;
      return;
    }

    Layout newLayout =
        createTextLayout(
            widthSpec,
            ellipsize,
            shouldIncludeFontPadding,
            maxLines,
            shadowRadius,
            shadowDx,
            shadowDy,
            shadowColor,
            isSingleLine,
            text,
            textColor,
            textColorStateList,
            linkColor,
            textSize,
            extraSpacing,
            spacingMultiplier,
            textStyle,
            typeface,
            textAlignment,
            glyphWarming,
            layout.getResolvedLayoutDirection(),
            minEms,
            maxEms,
            minTextWidth,
            maxTextWidth,
            textDirection);

    measureLayout.set(newLayout);

    size.width = SizeSpec.resolveSize(widthSpec, newLayout.getWidth());

    // Adjust height according to the minimum number of lines.
    int preferredHeight = LayoutMeasureUtil.getHeight(newLayout);
    final int lineCount = newLayout.getLineCount();
    if (lineCount < minLines) {
      final TextPaint paint = newLayout.getPaint();

      final int lineHeight =
          Math.round(paint.getFontMetricsInt(null) * spacingMultiplier + extraSpacing);
      preferredHeight += lineHeight * (minLines - lineCount);
    }

    size.height = SizeSpec.resolveSize(heightSpec, preferredHeight);

    // Some devices seem to be returning negative sizes in some cases.
    if (size.width < 0 || size.height < 0) {
      size.width = Math.max(size.width, 0);
      size.height = Math.max(size.height, 0);

      final ComponentsLogger logger = context.getLogger();
      if (logger != null) {
        final LogEvent event = logger.newEvent(EVENT_ERROR);
        event.addParam(PARAM_MESSAGE, "Text layout measured to less than 0 pixels");
        logger.log(event);
      }
    }

    measuredWidth.set(size.width);
    measuredHeight.set(size.height);
  }

  private static Layout createTextLayout(
      int widthSpec,
      TruncateAt ellipsize,
      boolean shouldIncludeFontPadding,
      int maxLines,
      float shadowRadius,
      float shadowDx,
      float shadowDy,
      int shadowColor,
      boolean isSingleLine,
      CharSequence text,
      int textColor,
      ColorStateList textColorStateList,
      int linkColor,
      int textSize,
      float extraSpacing,
      float spacingMultiplier,
      int textStyle,
      Typeface typeface,
      Alignment textAlignment,
      boolean glyphWarming,
      YogaDirection layoutDirection,
      int minEms,
      int maxEms,
      int minTextWidth,
      int maxTextWidth,
      TextDirectionHeuristicCompat textDirection) {
    Layout newLayout;

    TextLayoutBuilder layoutBuilder = sTextLayoutBuilderPool.acquire();
    if (layoutBuilder == null) {
      layoutBuilder = new TextLayoutBuilder();
      layoutBuilder.setShouldCacheLayout(false);
    }

    @TextLayoutBuilder.MeasureMode final int textMeasureMode;
    switch (SizeSpec.getMode(widthSpec)) {
      case UNSPECIFIED:
        textMeasureMode = TextLayoutBuilder.MEASURE_MODE_UNSPECIFIED;
        break;
      case EXACTLY:
        textMeasureMode = TextLayoutBuilder.MEASURE_MODE_EXACTLY;
        break;
      case AT_MOST:
        textMeasureMode = TextLayoutBuilder.MEASURE_MODE_AT_MOST;
        break;
      default:
        throw new IllegalStateException("Unexpected size mode: " + SizeSpec.getMode(widthSpec));
    }

    layoutBuilder
        .setEllipsize(ellipsize)
        .setMaxLines(maxLines)
        .setShadowLayer(shadowRadius, shadowDx, shadowDy, shadowColor)
        .setSingleLine(isSingleLine)
        .setText(text)
        .setTextSize(textSize)
        .setWidth(
            SizeSpec.getSize(widthSpec),
            textMeasureMode);

    if (minEms != DEFAULT_EMS) {
      layoutBuilder.setMinEms(minEms);
    } else {
      layoutBuilder.setMinWidth(minTextWidth);
    }

    if (maxEms != DEFAULT_EMS) {
      layoutBuilder.setMaxEms(maxEms);
    } else {
      layoutBuilder.setMaxWidth(maxTextWidth);
    }

    if (textColor != 0) {
      layoutBuilder.setTextColor(textColor);
    } else {
      layoutBuilder.setTextColor(textColorStateList);
    }

    if (typeface != DEFAULT_TYPEFACE) {
      layoutBuilder.setTypeface(typeface);
    } else {
      layoutBuilder.setTextStyle(textStyle);
    }

    if (textDirection != null) {
      layoutBuilder.setTextDirection(textDirection);
    } else {
      layoutBuilder.setTextDirection(layoutDirection == YogaDirection.RTL
          ? TextDirectionHeuristicsCompat.FIRSTSTRONG_RTL
          : TextDirectionHeuristicsCompat.FIRSTSTRONG_LTR);
    }

    layoutBuilder.setIncludeFontPadding(shouldIncludeFontPadding);
    layoutBuilder.setTextSpacingExtra(extraSpacing);
    layoutBuilder.setTextSpacingMultiplier(spacingMultiplier);
    layoutBuilder.setAlignment(textAlignment);
    layoutBuilder.setLinkColor(linkColor);

    newLayout = layoutBuilder.build();

    layoutBuilder.setText(null);
    sTextLayoutBuilderPool.release(layoutBuilder);

    if (glyphWarming && !DisplayListUtils.isEligibleForCreatingDisplayLists()) {
      GlyphWarmer.getInstance().warmLayout(newLayout);
    }

    return newLayout;
  }

  private static Alignment getAlignment(int viewTextAlignment, int gravity) {
    final Alignment alignment;
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

  private static Alignment getAlignment(int gravity) {
    final Alignment alignment;
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

  private static VerticalGravity getVerticalGravity(int gravity) {
    final VerticalGravity verticalGravity;
    switch (gravity & Gravity.VERTICAL_GRAVITY_MASK) {
      case Gravity.TOP:
        verticalGravity = TOP;
        break;
      case Gravity.CENTER_VERTICAL:
        verticalGravity = CENTER;
        break;
      case Gravity.BOTTOM:
        verticalGravity = BOTTOM;
        break;
      default:
        verticalGravity = TextSpec.verticalGravity;
        break;
    }
    return verticalGravity;
  }

  @OnBoundsDefined
  static void onBoundsDefined(
      ComponentContext c,
      ComponentLayout layout,
      @Prop(resType = ResType.STRING) CharSequence text,
      @Prop(optional = true) TruncateAt ellipsize,
      @Prop(optional = true, resType = ResType.BOOL) boolean shouldIncludeFontPadding,
      @Prop(optional = true, resType = ResType.INT) int maxLines,
      @Prop(optional = true, resType = ResType.INT) int minEms,
      @Prop(optional = true, resType = ResType.INT) int maxEms,
      @Prop(optional = true, resType = ResType.DIMEN_SIZE) int minTextWidth,
      @Prop(optional = true, resType = ResType.DIMEN_SIZE) int maxTextWidth,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) float shadowRadius,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) float shadowDx,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) float shadowDy,
      @Prop(optional = true, resType = ResType.COLOR) int shadowColor,
      @Prop(optional = true, resType = ResType.BOOL) boolean isSingleLine,
      @Prop(optional = true, resType = ResType.COLOR) int textColor,
      @Prop(optional = true) ColorStateList textColorStateList,
      @Prop(optional = true, resType = ResType.COLOR) int linkColor,
      @Prop(optional = true, resType = ResType.DIMEN_TEXT) int textSize,
      @Prop(optional = true, resType = ResType.DIMEN_OFFSET) float extraSpacing,
      @Prop(optional = true, resType = ResType.FLOAT) float spacingMultiplier,
      @Prop(optional = true) VerticalGravity verticalGravity,
      @Prop(optional = true) int textStyle,
      @Prop(optional = true) Typeface typeface,
      @Prop(optional = true) Alignment textAlignment,
      @Prop(optional = true) boolean glyphWarming,
      @Prop(optional = true) TextDirectionHeuristicCompat textDirection,
      @FromMeasure Layout measureLayout,
      @FromMeasure Integer measuredWidth,
      @FromMeasure Integer measuredHeight,
      Output<Layout> textLayout,
      Output<Float> textLayoutTranslationY,
      Output<ClickableSpan[]> clickableSpans,
      Output<ImageSpan[]> imageSpans) {

    if (TextUtils.isEmpty(text)) {
      return;
    }

    final float layoutWidth =
        layout.getWidth() - layout.getPaddingLeft() - layout.getPaddingRight();
    final float layoutHeight =
        layout.getHeight() - layout.getPaddingTop() - layout.getPaddingBottom();

    if (measureLayout != null &&
        measuredWidth == layoutWidth &&
        measuredHeight == layoutHeight) {
      textLayout.set(measureLayout);
    } else {
      if (measureLayout != null) {
        Log.w(
            TAG,
            "Remeasuring Text component.  This is expensive: consider changing parent layout " +
                "so that double measurement is not necessary.");
      }

      textLayout.set(
          createTextLayout(
              SizeSpec.makeSizeSpec((int) layoutWidth, EXACTLY),
              ellipsize,
              shouldIncludeFontPadding,
              maxLines,
              shadowRadius,
              shadowDx,
              shadowDy,
              shadowColor,
              isSingleLine,
              text,
              textColor,
              textColorStateList,
              linkColor,
              textSize,
              extraSpacing,
              spacingMultiplier,
              textStyle,
              typeface,
              textAlignment,
              glyphWarming,
              layout.getResolvedLayoutDirection(),
              minEms,
              maxEms,
              minTextWidth,
              maxTextWidth,
              textDirection));
    }

    final float textHeight = LayoutMeasureUtil.getHeight(textLayout.get());

    switch (verticalGravity) {
      case CENTER:
        textLayoutTranslationY.set((layoutHeight - textHeight) / 2);
        break;

      case BOTTOM:
        textLayoutTranslationY.set(layoutHeight - textHeight);
        break;

      default:
        textLayoutTranslationY.set(0f);
        break;
    }

    if (text instanceof Spanned) {
      Spanned spanned = (Spanned) text;
      clickableSpans.set(spanned.getSpans(0, text.length(), ClickableSpan.class));
      imageSpans.set(spanned.getSpans(0, text.length(), ImageSpan.class));
    }
  }

  @OnCreateMountContent
  static TextDrawable onCreateMountContent(ComponentContext c) {
    return new TextDrawable();
  }

  @OnMount
  static void onMount(
      ComponentContext c,
      TextDrawable textDrawable,
      @Prop(resType = ResType.STRING) final CharSequence text,
      @Prop(optional = true, resType = ResType.COLOR) int textColor,
      @Prop(optional = true, resType = ResType.COLOR) int highlightColor,
      @Prop(optional = true) ColorStateList textColorStateList,
      @Prop(optional = true) final EventHandler textOffsetOnTouchHandler,
      @Prop(optional = true) int highlightStartOffset,
      @Prop(optional = true) int highlightEndOffset,
      @FromBoundsDefined Layout textLayout,
      @FromBoundsDefined Float textLayoutTranslationY,
      @FromBoundsDefined ClickableSpan[] clickableSpans,
      @FromBoundsDefined ImageSpan[] imageSpans) {

    //make sure we set default state to drawable because default dummy state set in Drawable
    //matches anything which can cause wrong text color to be selected by default
    textDrawable.setState(DEFAULT_TEXT_DRAWABLE_STATE);
    TextDrawable.TextOffsetOnTouchListener textOffsetOnTouchListener = null;

    if (textOffsetOnTouchHandler != null) {
      textOffsetOnTouchListener = new TextDrawable.TextOffsetOnTouchListener() {
        @Override
        public void textOffsetOnTouch(int textOffset) {
          Text.dispatchTextOffsetOnTouchEvent(textOffsetOnTouchHandler, text, textOffset);
        }
      };
    }
    textDrawable.mount(
        text,
        textLayout,
        textLayoutTranslationY == null ? 0 : textLayoutTranslationY,
        textColorStateList,
        textColor,
        highlightColor,
        clickableSpans,
        imageSpans,
        textOffsetOnTouchListener,
        highlightStartOffset,
        highlightEndOffset);

    if (text instanceof MountableCharSequence) {
      ((MountableCharSequence) text).onMount(textDrawable);
    }
  }

  @OnUnmount
  static void onUnmount(
      ComponentContext c,
      TextDrawable textDrawable,
      @Prop(resType = ResType.STRING) CharSequence text) {
    textDrawable.unmount();

    if (text instanceof MountableCharSequence) {
      ((MountableCharSequence) text).onUnmount(textDrawable);
    }
  }

  @OnPopulateAccessibilityNode
  static void onPopulateAccessibilityNode(
      AccessibilityNodeInfoCompat node,
      @Prop(resType = STRING) CharSequence text,
      @Prop(optional = true, resType = ResType.BOOL) boolean isSingleLine) {
    node.setText(text);
    node.setContentDescription(text);

    node.addAction(AccessibilityNodeInfoCompat.ACTION_NEXT_AT_MOVEMENT_GRANULARITY);
    node.addAction(AccessibilityNodeInfoCompat.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY);
    node.setMovementGranularities(
        AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_CHARACTER
            | AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_WORD
            | AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_PARAGRAPH);

    if (!isSingleLine) {
      node.setMultiLine(true);
    }
  }

  @GetExtraAccessibilityNodesCount
  static int getExtraAccessibilityNodesCount(
      @Prop(optional = true, resType = BOOL) boolean accessibleClickableSpans,
      @FromBoundsDefined ClickableSpan[] clickableSpans) {
    return (accessibleClickableSpans && clickableSpans != null) ? clickableSpans.length : 0;
  }

  @OnPopulateExtraAccessibilityNode
  static void onPopulateExtraAccessibilityNode(
      AccessibilityNodeInfoCompat node,
      int extraNodeIndex,
      int componentBoundsLeft,
      int componentBoundsTop,
      @Prop(resType = STRING) CharSequence text,
      @FromBoundsDefined Layout textLayout,
      @FromBoundsDefined ClickableSpan[] clickableSpans) {
    final Spanned spanned = (Spanned) text;

    final ClickableSpan span = clickableSpans[extraNodeIndex];
    final int start = spanned.getSpanStart(span);
    final int end = spanned.getSpanEnd(span);
    final int startLine = textLayout.getLineForOffset(start);
    final int endLine = textLayout.getLineForOffset(end);

    // The bounds for multi-line strings should *only* include the first line.  This is because
    // Talkback triggers its click at the center point of these bounds, and if that center point
    // is outside the spannable, it will click on something else.  There is no harm in not outlining
    // the wrapped part of the string, as the text for the whole string will be read regardless of
    // the bounding box.
    final int selectionPathEnd =
        startLine == endLine ? end : textLayout.getLineVisibleEnd(startLine);

    textLayout.getSelectionPath(start, selectionPathEnd, sTempPath);
    sTempPath.computeBounds(sTempRectF, /* unused */true);

    sTempRect.set(
        componentBoundsLeft + (int) sTempRectF.left,
        componentBoundsTop + (int) sTempRectF.top,
        componentBoundsLeft + (int) sTempRectF.right,
        componentBoundsTop + (int) sTempRectF.bottom);

    if (sTempRect.isEmpty()) {
      // Text is not actually visible.
      // Override bounds so it doesn't crash ExploreByTouchHelper.java
      sTempRect.set(0, 0, 1, 1);
      node.setBoundsInParent(sTempRect);
      node.setContentDescription(""); // make node non-focusable
      return;
    }

    node.setBoundsInParent(sTempRect);

    node.setClickable(true);
    node.setFocusable(true);
    node.setEnabled(true);
    node.setVisibleToUser(true);
    if (span instanceof AccessibleClickableSpan) {
      node.setText(((AccessibleClickableSpan) span).getAccessibilityDescription());
    } else {
      node.setText(spanned.subSequence(start, end));
    }
  }

  @GetExtraAccessibilityNodeAt
  static int getExtraAccessibilityNodeAt(
      int x,
      int y,
      @Prop(resType = STRING) CharSequence text,
      @FromBoundsDefined Layout textLayout,
      @FromBoundsDefined ClickableSpan[] clickableSpans) {
    final Spanned spanned = (Spanned) text;

    for (int i = 0; i < clickableSpans.length; i++) {
      final ClickableSpan span = clickableSpans[i];
      final int start = spanned.getSpanStart(span);
      final int end = spanned.getSpanEnd(span);

      textLayout.getSelectionPath(start, end, sTempPath);
      sTempPath.computeBounds(sTempRectF, /* unused */true);

      if (sTempRectF.contains(x, y)) {
        return i;
      }
    }

    return INVALID_ID;
  }
}
