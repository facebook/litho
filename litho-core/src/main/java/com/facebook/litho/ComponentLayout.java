/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import static android.support.annotation.Dimension.DP;

import android.graphics.drawable.Drawable;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.annotation.DimenRes;
import android.support.annotation.Dimension;
import android.support.annotation.DrawableRes;
import android.support.annotation.Px;
import android.support.annotation.StringRes;
import android.util.SparseArray;
import android.view.ViewOutlineProvider;
import com.facebook.infer.annotation.ReturnsOwnership;
import com.facebook.litho.annotations.ImportantForAccessibility;
import com.facebook.litho.reference.Reference;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaJustify;
import com.facebook.yoga.YogaPositionType;
import com.facebook.yoga.YogaWrap;

/**
 * Represents a {@link Component}'s computed layout state. The computed bounds will be
 * used by the framework to define the size and position of the component's mounted
 * {@link android.view.View}s and {@link android.graphics.drawable.Drawable}s returned.
 * by {@link ComponentLifecycle#mount(ComponentContext, Object, Component)}.
 *
 * @see ComponentLifecycle#createLayout(ComponentContext, Component, boolean)
 * @see ComponentLifecycle#mount(ComponentContext, Object, Component)
 */
public interface ComponentLayout {

  @Px
  int getX();
  @Px
  int getY();

  @Px
  int getWidth();
  @Px
  int getHeight();

  @Px
  int getPaddingTop();
  @Px
  int getPaddingRight();
  @Px
  int getPaddingBottom();
  @Px
  int getPaddingLeft();

  YogaDirection getResolvedLayoutDirection();

  interface Builder {
    @ReturnsOwnership Builder layoutDirection(YogaDirection direction);
    @ReturnsOwnership Builder alignSelf(YogaAlign alignSelf);
    @ReturnsOwnership Builder positionType(YogaPositionType positionType);
    @ReturnsOwnership Builder flex(float flex);
    @ReturnsOwnership Builder flexGrow(float flexGrow);
    @ReturnsOwnership Builder flexShrink(float flexShrink);
    @ReturnsOwnership Builder flexBasisPx(@Px int flexBasis);
    @ReturnsOwnership Builder flexBasisAttr(@AttrRes int resId, @DimenRes int defaultResId);
    @ReturnsOwnership Builder flexBasisAttr(@AttrRes int resId);
    @ReturnsOwnership Builder flexBasisRes(@DimenRes int resId);
    @ReturnsOwnership Builder flexBasisDip(@Dimension(unit = DP) float flexBasis);
    @ReturnsOwnership Builder flexBasisPercent(float percent);

    @ReturnsOwnership Builder importantForAccessibility(
        @ImportantForAccessibility int importantForAccessibility);
    @ReturnsOwnership Builder duplicateParentState(boolean duplicateParentState);

    @ReturnsOwnership Builder marginPx(YogaEdge edge, @Px int margin);
    @ReturnsOwnership Builder marginAttr(
        YogaEdge edge,
        @AttrRes int resId,
        @DimenRes int defaultResId);
    @ReturnsOwnership Builder marginAttr(
        YogaEdge edge,
        @AttrRes int resId);
    @ReturnsOwnership Builder marginRes(YogaEdge edge, @DimenRes int resId);
    @ReturnsOwnership Builder marginDip(YogaEdge edge, @Dimension(unit = DP) float margin);
    @ReturnsOwnership Builder marginPercent(YogaEdge edge, float percent);
    @ReturnsOwnership Builder marginAuto(YogaEdge edge);

    @ReturnsOwnership Builder paddingPx(YogaEdge edge, @Px int padding);
    @ReturnsOwnership Builder paddingAttr(
        YogaEdge edge,
        @AttrRes int resId,
        @DimenRes int defaultResId);
    @ReturnsOwnership Builder paddingAttr(
        YogaEdge edge,
        @AttrRes int resId);
    @ReturnsOwnership Builder paddingRes(YogaEdge edge, @DimenRes int resId);
    @ReturnsOwnership Builder paddingDip(YogaEdge edge, @Dimension(unit = DP) float padding);
    @ReturnsOwnership Builder paddingPercent(YogaEdge edge, float percent);

    @ReturnsOwnership
    Builder border(Border border);
    /** @deprecated Please use {@link #border(Border)} instead */
    @ReturnsOwnership
    Builder borderWidthPx(YogaEdge edge, @Px int borderWidth);
    /** @deprecated Please use {@link #border(Border)} instead */
    @ReturnsOwnership
    Builder borderWidthAttr(YogaEdge edge, @AttrRes int resId, @DimenRes int defaultResId);
    /** @deprecated Please use {@link #border(Border)} instead */
    @ReturnsOwnership
    Builder borderWidthAttr(YogaEdge edge, @AttrRes int resId);
    /** @deprecated Please use {@link #border(Border)} instead */
    @ReturnsOwnership
    Builder borderWidthRes(YogaEdge edge, @DimenRes int resId);
    /** @deprecated Please use {@link #border(Border)} instead */
    @ReturnsOwnership
    Builder borderWidthDip(YogaEdge edge, @Dimension(unit = DP) float borderWidth);
    /** @deprecated Please use {@link #border(Border)} instead */
    @ReturnsOwnership
    Builder borderColor(@ColorInt int borderColor);

    @ReturnsOwnership Builder positionPx(YogaEdge edge, @Px int value);
    @ReturnsOwnership Builder positionAttr(
        YogaEdge edge,
        @AttrRes int resId,
        @DimenRes int defaultResId);
    @ReturnsOwnership Builder positionAttr(YogaEdge edge, @AttrRes int resId);
    @ReturnsOwnership Builder positionRes(YogaEdge edge, @DimenRes int resId);
    @ReturnsOwnership Builder positionDip(YogaEdge edge, @Dimension(unit = DP) float value);
    @ReturnsOwnership Builder positionPercent(YogaEdge edge, float percent);

    @ReturnsOwnership Builder widthPx(@Px int width);
    @ReturnsOwnership Builder widthAttr(@AttrRes int resId, @DimenRes int defaultResId);
    @ReturnsOwnership Builder widthAttr(@AttrRes int resId);
    @ReturnsOwnership Builder widthRes(@DimenRes int resId);
    @ReturnsOwnership Builder widthDip(@Dimension(unit = DP) float width);
    @ReturnsOwnership Builder widthPercent(float percent);

    @ReturnsOwnership Builder minWidthPx(@Px int minWidth);
    @ReturnsOwnership Builder minWidthAttr(@AttrRes int resId, @DimenRes int defaultResId);
    @ReturnsOwnership Builder minWidthAttr(@AttrRes int resId);
    @ReturnsOwnership Builder minWidthRes(@DimenRes int resId);
    @ReturnsOwnership Builder minWidthDip(@Dimension(unit = DP) float minWidth);
    @ReturnsOwnership Builder minWidthPercent(float percent);

    @ReturnsOwnership Builder maxWidthPx(@Px int maxWidth);
    @ReturnsOwnership Builder maxWidthAttr(@AttrRes int resId, @DimenRes int defaultResId);
    @ReturnsOwnership Builder maxWidthAttr(@AttrRes int resId);
    @ReturnsOwnership Builder maxWidthRes(@DimenRes int resId);
    @ReturnsOwnership Builder maxWidthDip(@Dimension(unit = DP) float maxWidth);
    @ReturnsOwnership Builder maxWidthPercent(float percent);

    @ReturnsOwnership Builder heightPx(@Px int height);
    @ReturnsOwnership Builder heightAttr(@AttrRes int resId, @DimenRes int defaultResId);
    @ReturnsOwnership Builder heightAttr(@AttrRes int resId);
    @ReturnsOwnership Builder heightRes(@DimenRes int resId);
    @ReturnsOwnership Builder heightDip(@Dimension(unit = DP) float height);
    @ReturnsOwnership Builder heightPercent(float percent);

    @ReturnsOwnership Builder minHeightPx(@Px int minHeight);
    @ReturnsOwnership Builder minHeightAttr(@AttrRes int resId, @DimenRes int defaultResId);
    @ReturnsOwnership Builder minHeightAttr(@AttrRes int resId);
    @ReturnsOwnership Builder minHeightRes(@DimenRes int resId);
    @ReturnsOwnership Builder minHeightDip(@Dimension(unit = DP) float minHeight);
    @ReturnsOwnership Builder minHeightPercent(float percent);

    @ReturnsOwnership Builder maxHeightPx(@Px int maxHeight);
    @ReturnsOwnership Builder maxHeightAttr(@AttrRes int resId, @DimenRes int defaultResId);
    @ReturnsOwnership Builder maxHeightAttr(@AttrRes int resId);
    @ReturnsOwnership Builder maxHeightRes(@DimenRes int resId);
    @ReturnsOwnership Builder maxHeightDip(@Dimension(unit = DP) float maxHeight);
    @ReturnsOwnership Builder maxHeightPercent(float percent);

    @ReturnsOwnership Builder aspectRatio(float aspectRatio);

    @ReturnsOwnership Builder touchExpansionPx(YogaEdge edge, @Px int value);
    @ReturnsOwnership Builder touchExpansionAttr(
        YogaEdge edge,
        @AttrRes int resId,
        @DimenRes int defaultResId);
    @ReturnsOwnership Builder touchExpansionAttr(YogaEdge edge, @AttrRes int resId);
    @ReturnsOwnership Builder touchExpansionRes(YogaEdge edge, @DimenRes int resId);
    @ReturnsOwnership Builder touchExpansionDip(YogaEdge edge, @Dimension(unit = DP) float value);

    /**
     * @deprecated just use {@link #background(Drawable)} instead.
     */
    @Deprecated
    @ReturnsOwnership Builder background(Reference<? extends Drawable> background);
    /**
     * @deprecated just use {@link #background(Drawable)} instead.
     */
    @Deprecated
    @ReturnsOwnership Builder background(Reference.Builder<? extends Drawable> backgroundBuilder);
    @ReturnsOwnership Builder background(Drawable drawable);
    @ReturnsOwnership Builder backgroundAttr(@AttrRes int resId, @DrawableRes int defaultResId);
    @ReturnsOwnership Builder backgroundAttr(@AttrRes int resId);
    @ReturnsOwnership Builder backgroundRes(@DrawableRes int resId);
    @ReturnsOwnership Builder backgroundColor(@ColorInt int backgroundColor);

    @ReturnsOwnership Builder foreground(Drawable drawable);
    @ReturnsOwnership Builder foregroundAttr(@AttrRes int resId, @DrawableRes int defaultResId);
    @ReturnsOwnership Builder foregroundAttr(@AttrRes int resId);
    @ReturnsOwnership Builder foregroundRes(@DrawableRes int resId);
    @ReturnsOwnership Builder foregroundColor(@ColorInt int foregroundColor);

    @ReturnsOwnership Builder wrapInView();
    @ReturnsOwnership Builder clickHandler(EventHandler<ClickEvent> clickHandler);
    @ReturnsOwnership Builder focusChangeHandler(
        EventHandler<FocusChangedEvent> focusChangeHandler);
    @ReturnsOwnership Builder longClickHandler(EventHandler<LongClickEvent> clickHandler);
    @ReturnsOwnership Builder touchHandler(EventHandler<TouchEvent> touchHandler);
    @ReturnsOwnership Builder interceptTouchHandler(
        EventHandler<InterceptTouchEvent> interceptTouchHandler);
    @ReturnsOwnership Builder focusable(boolean isFocusable);
    @ReturnsOwnership Builder enabled(boolean isEnabled);
    @ReturnsOwnership Builder visibleHeightRatio(float visibleHeightRatio);
    @ReturnsOwnership Builder visibleWidthRatio(float visibleWidthRatio);
    @ReturnsOwnership Builder visibleHandler(EventHandler<VisibleEvent> visibleHandler);
    @ReturnsOwnership Builder focusedHandler(EventHandler<FocusedVisibleEvent> focusedHandler);
    @ReturnsOwnership Builder unfocusedHandler(
        EventHandler<UnfocusedVisibleEvent> unfocusedHandler);
    @ReturnsOwnership Builder fullImpressionHandler(
        EventHandler<FullImpressionVisibleEvent> fullImpressionHandler);
    @ReturnsOwnership Builder invisibleHandler(EventHandler<InvisibleEvent> invisibleHandler);
    @ReturnsOwnership Builder contentDescription(CharSequence contentDescription);
    @ReturnsOwnership Builder contentDescription(@StringRes int stringId);
    @ReturnsOwnership Builder contentDescription(@StringRes int stringId, Object... formatArgs);
    @ReturnsOwnership Builder viewTag(Object viewTag);
    @ReturnsOwnership Builder viewTags(SparseArray<Object> viewTags);
    /**
     * Shadow elevation and outline provider methods are only functional on
     * {@link android.os.Build.VERSION_CODES#LOLLIPOP} and above.
     */
    @ReturnsOwnership Builder shadowElevationPx(float shadowElevation);
    @ReturnsOwnership Builder shadowElevationAttr(@AttrRes int resId, @DimenRes int defaultResId);
    @ReturnsOwnership Builder shadowElevationAttr(@AttrRes int resId);
    @ReturnsOwnership Builder shadowElevationRes(@DimenRes int resId);
    @ReturnsOwnership Builder shadowElevationDip(@Dimension(unit = DP) float shadowElevation);
    @ReturnsOwnership Builder outlineProvider(ViewOutlineProvider outlineProvider);
    @ReturnsOwnership Builder clipToOutline(boolean clipToOutline);
    @ReturnsOwnership Builder transitionKey(String key);
    @ReturnsOwnership Builder testKey(String testKey);
    @ReturnsOwnership Builder dispatchPopulateAccessibilityEventHandler(
        EventHandler<DispatchPopulateAccessibilityEventEvent>
            dispatchPopulateAccessibilityEventHandler);
    @ReturnsOwnership Builder onInitializeAccessibilityEventHandler(
        EventHandler<OnInitializeAccessibilityEventEvent> onInitializeAccessibilityEventHandler);
    @ReturnsOwnership Builder onInitializeAccessibilityNodeInfoHandler(
        EventHandler<OnInitializeAccessibilityNodeInfoEvent>
            onInitializeAccessibilityNodeInfoHandler);
    @ReturnsOwnership Builder onPopulateAccessibilityEventHandler(
        EventHandler<OnPopulateAccessibilityEventEvent> onPopulateAccessibilityEventHandler);
    @ReturnsOwnership Builder onRequestSendAccessibilityEventHandler(
        EventHandler<OnRequestSendAccessibilityEventEvent>
            onRequestSendAccessibilityEventHandler);
    @ReturnsOwnership Builder performAccessibilityActionHandler(
        EventHandler<PerformAccessibilityActionEvent>
            performAccessibilityActionHandler);
    @ReturnsOwnership Builder sendAccessibilityEventHandler(
    EventHandler<SendAccessibilityEventEvent> sendAccessibilityEventHandler);
    @ReturnsOwnership Builder sendAccessibilityEventUncheckedHandler(
        EventHandler<SendAccessibilityEventUncheckedEvent> sendAccessibilityEventUncheckedHandler);

    @ReturnsOwnership
    ComponentLayout build();
  }

  interface ContainerBuilder extends Builder {
    @ReturnsOwnership ContainerBuilder layoutDirection(YogaDirection direction);
    @ReturnsOwnership ContainerBuilder alignSelf(YogaAlign alignSelf);
    @ReturnsOwnership ContainerBuilder positionType(YogaPositionType positionType);
    @ReturnsOwnership ContainerBuilder flex(float flex);
    @ReturnsOwnership ContainerBuilder flexGrow(float flexGrow);
    @ReturnsOwnership ContainerBuilder flexShrink(float flexShrink);
    @ReturnsOwnership ContainerBuilder flexBasisPx(@Px int flexBasis);
    @ReturnsOwnership ContainerBuilder flexBasisAttr(
        @AttrRes int resId,
        @DimenRes int defaultResId);
    @ReturnsOwnership ContainerBuilder flexBasisAttr(@AttrRes int resId);
    @ReturnsOwnership ContainerBuilder flexBasisRes(@DimenRes int resId);
    @ReturnsOwnership ContainerBuilder flexBasisDip(@Dimension(unit = DP) float flexBasis);
    @ReturnsOwnership ContainerBuilder flexBasisPercent(float percent);

    @ReturnsOwnership ContainerBuilder importantForAccessibility(
        @ImportantForAccessibility int importantForAccessibility
    );

    @ReturnsOwnership ContainerBuilder duplicateParentState(boolean duplicateParentState);

    @ReturnsOwnership ContainerBuilder marginPx(YogaEdge edge, @Px int margin);
    @ReturnsOwnership ContainerBuilder marginAttr(
        YogaEdge edge,
        @AttrRes int resId,
        @DimenRes int defaultResId);
    @ReturnsOwnership ContainerBuilder marginAttr(
        YogaEdge edge,
        @AttrRes int resId);
    @ReturnsOwnership ContainerBuilder marginRes(YogaEdge edge, @DimenRes int resId);
    @ReturnsOwnership ContainerBuilder marginDip(YogaEdge edge, @Dimension(unit = DP) float margin);
    @ReturnsOwnership ContainerBuilder marginPercent(YogaEdge edge, float percent);
    @ReturnsOwnership ContainerBuilder marginAuto(YogaEdge edge);

    @ReturnsOwnership ContainerBuilder paddingPx(YogaEdge edge, @Px int padding);
    @ReturnsOwnership ContainerBuilder paddingAttr(
        YogaEdge edge,
        @AttrRes int resId,
        @DimenRes int defaultResId);
    @ReturnsOwnership ContainerBuilder paddingAttr(
        YogaEdge edge,
        @AttrRes int resId);
    @ReturnsOwnership ContainerBuilder paddingRes(YogaEdge edge, @DimenRes int resId);
    @ReturnsOwnership ContainerBuilder paddingDip(YogaEdge edge, @Dimension(unit = DP) float padding);
    @ReturnsOwnership ContainerBuilder paddingPercent(YogaEdge edge, float percent);

    @ReturnsOwnership ContainerBuilder positionPx(YogaEdge edge, @Px int position);
    @ReturnsOwnership ContainerBuilder positionAttr(
        YogaEdge edge,
        @AttrRes int resId,
        @DimenRes int defaultResId);
    @ReturnsOwnership ContainerBuilder positionAttr(YogaEdge edge, @AttrRes int resId);
    @ReturnsOwnership ContainerBuilder positionRes(YogaEdge edge, @DimenRes int resId);
    @ReturnsOwnership ContainerBuilder positionDip(
        YogaEdge edge,
        @Dimension(unit = DP) float position);
    @ReturnsOwnership ContainerBuilder positionPercent(YogaEdge edge, float percent);

    @ReturnsOwnership ContainerBuilder widthPx(@Px int width);
    @ReturnsOwnership ContainerBuilder widthAttr(@AttrRes int resId, @DimenRes int defaultResId);
    @ReturnsOwnership ContainerBuilder widthAttr(@AttrRes int resId);
    @ReturnsOwnership ContainerBuilder widthRes(@DimenRes int resId);
    @ReturnsOwnership ContainerBuilder widthDip(@Dimension(unit = DP) float width);
    @ReturnsOwnership ContainerBuilder widthPercent(float percent);

    @ReturnsOwnership ContainerBuilder minWidthPx(@Px int minWidth);
    @ReturnsOwnership ContainerBuilder minWidthAttr(@AttrRes int resId, @DimenRes int defaultResId);
    @ReturnsOwnership ContainerBuilder minWidthAttr(@AttrRes int resId);
    @ReturnsOwnership ContainerBuilder minWidthRes(@DimenRes int resId);
    @ReturnsOwnership ContainerBuilder minWidthDip(@Dimension(unit = DP) float minWidth);
    @ReturnsOwnership ContainerBuilder minWidthPercent(float percent);

    @ReturnsOwnership ContainerBuilder maxWidthPx(@Px int maxWidth);
    @ReturnsOwnership ContainerBuilder maxWidthAttr(@AttrRes int resId, @DimenRes int defaultResId);
    @ReturnsOwnership ContainerBuilder maxWidthAttr(@AttrRes int resId);
    @ReturnsOwnership ContainerBuilder maxWidthRes(@DimenRes int resId);
    @ReturnsOwnership ContainerBuilder maxWidthDip(@Dimension(unit = DP) float maxWidth);
    @ReturnsOwnership ContainerBuilder maxWidthPercent(float percent);

    @ReturnsOwnership ContainerBuilder heightPx(@Px int height);
    @ReturnsOwnership ContainerBuilder heightAttr(@AttrRes int resId, @DimenRes int defaultResId);
    @ReturnsOwnership ContainerBuilder heightAttr(@AttrRes int resId);
    @ReturnsOwnership ContainerBuilder heightRes(@DimenRes int resId);
    @ReturnsOwnership ContainerBuilder heightDip(@Dimension(unit = DP) float height);
    @ReturnsOwnership ContainerBuilder heightPercent(float percent);

    @ReturnsOwnership ContainerBuilder minHeightPx(@Px int minHeight);
    @ReturnsOwnership ContainerBuilder minHeightAttr(
        @AttrRes int resId,
        @DimenRes int defaultResId);
    @ReturnsOwnership ContainerBuilder minHeightAttr(@AttrRes int resId);
    @ReturnsOwnership ContainerBuilder minHeightRes(@DimenRes int resId);
    @ReturnsOwnership ContainerBuilder minHeightDip(@Dimension(unit = DP) float minHeight);
    @ReturnsOwnership ContainerBuilder minHeightPercent(float percent);

    @ReturnsOwnership ContainerBuilder maxHeightPx(@Px int maxHeight);
    @ReturnsOwnership ContainerBuilder maxHeightAttr(
        @AttrRes int resId,
        @DimenRes int defaultResId);
    @ReturnsOwnership ContainerBuilder maxHeightAttr(@AttrRes int resId);
    @ReturnsOwnership ContainerBuilder maxHeightRes(@DimenRes int resId);
    @ReturnsOwnership ContainerBuilder maxHeightDip(@Dimension(unit = DP) float maxHeight);
    @ReturnsOwnership ContainerBuilder maxHeightPercent(float percent);

    @ReturnsOwnership ContainerBuilder aspectRatio(float aspectRatio);

    @ReturnsOwnership ContainerBuilder touchExpansionPx(YogaEdge edge, @Px int value);
    @ReturnsOwnership ContainerBuilder touchExpansionAttr(
        YogaEdge edge,
        @AttrRes int resId,
        @DimenRes int defaultResId);
    @ReturnsOwnership ContainerBuilder touchExpansionAttr(YogaEdge edge, @AttrRes int resId);
    @ReturnsOwnership ContainerBuilder touchExpansionRes(YogaEdge edge, @DimenRes int resId);
    @ReturnsOwnership ContainerBuilder touchExpansionDip(
        YogaEdge edge,
        @Dimension(unit = DP) float value);

    @ReturnsOwnership ContainerBuilder wrap(YogaWrap wrap);
    @ReturnsOwnership ContainerBuilder justifyContent(YogaJustify justifyContent);
    @ReturnsOwnership ContainerBuilder alignItems(YogaAlign alignItems);
    @ReturnsOwnership ContainerBuilder alignContent(YogaAlign alignContent);
    @ReturnsOwnership ContainerBuilder child(ComponentLayout child);
    @ReturnsOwnership ContainerBuilder child(ComponentLayout.Builder childBuilder);
    @ReturnsOwnership ContainerBuilder child(Component<?> component);
    @ReturnsOwnership ContainerBuilder child(Component.Builder<?, ?> componentBuilder);

    /**
     * @deprecated just use {@link #background(Drawable)} instead.
     */
    @Deprecated
    @ReturnsOwnership ContainerBuilder background(Reference<? extends Drawable> background);
    /**
     * @deprecated just use {@link #background(Drawable)} instead.
     */
    @Deprecated
    @ReturnsOwnership ContainerBuilder background(
        Reference.Builder<? extends Drawable> backgroundBuilder);
    @ReturnsOwnership ContainerBuilder background(Drawable drawable);
    @ReturnsOwnership ContainerBuilder backgroundAttr(
        @AttrRes int resId,
        @DrawableRes int defaultResId);
    @ReturnsOwnership ContainerBuilder backgroundAttr(@AttrRes int resId);
    @ReturnsOwnership ContainerBuilder backgroundRes(@DrawableRes int resId);
    @ReturnsOwnership ContainerBuilder backgroundColor(@ColorInt int backgroundColor);

    @ReturnsOwnership ContainerBuilder foreground(Drawable drawable);
    @ReturnsOwnership ContainerBuilder foregroundAttr(
        @AttrRes int resId,
        @DrawableRes int defaultResId);
    @ReturnsOwnership ContainerBuilder foregroundAttr(@AttrRes int resId);
    @ReturnsOwnership ContainerBuilder foregroundRes(@DrawableRes int resId);
    @ReturnsOwnership ContainerBuilder foregroundColor(@ColorInt int foregroundColor);

    @ReturnsOwnership ContainerBuilder wrapInView();
    @ReturnsOwnership ContainerBuilder clickHandler(EventHandler<ClickEvent> clickHandler);
    @ReturnsOwnership ContainerBuilder focusChangeHandler(
        EventHandler<FocusChangedEvent> focusChangeHandler);
    @ReturnsOwnership ContainerBuilder longClickHandler(EventHandler<LongClickEvent> clickHandler);
    @ReturnsOwnership ContainerBuilder touchHandler(EventHandler<TouchEvent> touchHandler);
    @ReturnsOwnership ContainerBuilder interceptTouchHandler(
        EventHandler<InterceptTouchEvent> interceptTouchHandler);
    @ReturnsOwnership ContainerBuilder focusable(boolean isFocusable);
    @ReturnsOwnership ContainerBuilder enabled(boolean isEnabled);
    @ReturnsOwnership ContainerBuilder visibleHeightRatio(float visibleHeightRatio);
    @ReturnsOwnership ContainerBuilder visibleWidthRatio(float visibleWidthRatio);
    @ReturnsOwnership ContainerBuilder visibleHandler(EventHandler<VisibleEvent> visibleHandler);
    @ReturnsOwnership ContainerBuilder focusedHandler(
        EventHandler<FocusedVisibleEvent> focusedHandler);
    @ReturnsOwnership ContainerBuilder unfocusedHandler(
        EventHandler<UnfocusedVisibleEvent> unfocusedHandler);
    @ReturnsOwnership ContainerBuilder fullImpressionHandler(
        EventHandler<FullImpressionVisibleEvent> fullImpressionHandler);
    @ReturnsOwnership ContainerBuilder invisibleHandler(
        EventHandler<InvisibleEvent> invisibleHandler);
    @ReturnsOwnership ContainerBuilder contentDescription(CharSequence contentDescription);
    @ReturnsOwnership ContainerBuilder contentDescription(@StringRes int stringId);
    @ReturnsOwnership ContainerBuilder viewTag(Object viewTag);
    @ReturnsOwnership ContainerBuilder viewTags(SparseArray<Object> viewTags);
    /**
     * Shadow elevation and outline provider methods are only functional on
     * {@link android.os.Build.VERSION_CODES#LOLLIPOP} and above.
     */
    @ReturnsOwnership ContainerBuilder shadowElevationPx(float shadowElevation);
    @ReturnsOwnership ContainerBuilder shadowElevationAttr(
        @AttrRes int resId,
        @DimenRes int defaultResId);
    @ReturnsOwnership ContainerBuilder shadowElevationAttr(@AttrRes int resId);
    @ReturnsOwnership ContainerBuilder shadowElevationRes(@DimenRes int resId);
    @ReturnsOwnership ContainerBuilder shadowElevationDip(
        @Dimension(unit = DP) float shadowElevation);
    @ReturnsOwnership ContainerBuilder outlineProvider(ViewOutlineProvider outlineProvider);
    @ReturnsOwnership ContainerBuilder clipToOutline(boolean clipToOutline);
    @ReturnsOwnership ContainerBuilder transitionKey(String key);
    @ReturnsOwnership ContainerBuilder dispatchPopulateAccessibilityEventHandler(
        EventHandler<DispatchPopulateAccessibilityEventEvent>
            dispatchPopulateAccessibilityEventHandler);
    @ReturnsOwnership ContainerBuilder onInitializeAccessibilityEventHandler(
        EventHandler<OnInitializeAccessibilityEventEvent> onInitializeAccessibilityEventHandler);
    @ReturnsOwnership ContainerBuilder onInitializeAccessibilityNodeInfoHandler(
        EventHandler<OnInitializeAccessibilityNodeInfoEvent>
            onInitializeAccessibilityNodeInfoHandler);
    @ReturnsOwnership ContainerBuilder onPopulateAccessibilityEventHandler(
        EventHandler<OnPopulateAccessibilityEventEvent> onPopulateAccessibilityEventHandler);
    @ReturnsOwnership ContainerBuilder onRequestSendAccessibilityEventHandler(
        EventHandler<OnRequestSendAccessibilityEventEvent>
            onRequestSendAccessibilityEventHandler);
    @ReturnsOwnership ContainerBuilder performAccessibilityActionHandler(
        EventHandler<PerformAccessibilityActionEvent>
            performAccessibilityActionHandler);
    @ReturnsOwnership ContainerBuilder sendAccessibilityEventHandler(
        EventHandler<SendAccessibilityEventEvent> sendAccessibilityEventHandler);
    @ReturnsOwnership ContainerBuilder sendAccessibilityEventUncheckedHandler(
        EventHandler<SendAccessibilityEventUncheckedEvent> sendAccessibilityEventUncheckedHandler);
    @ReturnsOwnership ContainerBuilder testKey(String testKey);
  }
}
