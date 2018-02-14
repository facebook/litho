/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho;

import android.animation.StateListAnimator;
import android.graphics.drawable.Drawable;
import android.support.annotation.AttrRes;
import android.support.annotation.Nullable;
import android.support.annotation.Px;
import android.support.annotation.StyleRes;
import android.util.SparseArray;
import android.view.ViewOutlineProvider;
import com.facebook.litho.reference.Reference;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaPositionType;

/**
 * Allows access to common props stored in a Component.
 *
 * TODO: This name is temporary to reduce the size of the individual diffs and will be
 *       replaced in the next.
 */
public interface ICommonProps {
  @AttrRes
  int getDefStyleAttr();

  @StyleRes
  int getDefStyleRes();

  @Nullable
  YogaPositionType getPositionType();

  int getWidthPx();

  int getHeightPx();

  @Nullable
  Reference<? extends Drawable> getBackground();

  @Nullable
  String getTestKey();

  boolean isWrapInView();

  @Nullable
  YogaDirection getLayoutDirection();

  @Nullable
  YogaAlign getAlignSelf();

  float getFlex();

  float getFlexGrow();

  float getFlexShrink();

  @Px
  int getFlexBasisPx();

  float getFlexBasisPercent();

  int getImportantForAccessibility();

  boolean getDuplicateParentState();

  @Nullable
  Border getBorder();

  @Nullable
  StateListAnimator getStateListAnimator();

  float getAspectRatio();

  @Nullable
  Drawable getForeground();

  @Nullable
  EventHandler<ClickEvent> getClickHandler();

  @Nullable
  EventHandler<LongClickEvent> getLongClickHandler();

  @Nullable
  EventHandler<FocusChangedEvent> getFocusChangeHandler();

  @Nullable
  EventHandler<TouchEvent> getTouchHandler();

  @Nullable
  EventHandler<InterceptTouchEvent> getInterceptTouchHandler();

  boolean getFocusable();

  boolean getEnabled();

  boolean getSelected();

  float getVisibleHeightRatio();

  float getVisibleWidthRatio();

  @Nullable
  EventHandler<VisibleEvent> getVisibleHandler();

  @Nullable
  EventHandler<FocusedVisibleEvent> getFocusedHandler();

  @Nullable
  EventHandler<UnfocusedVisibleEvent> getUnfocusedHandler();

  @Nullable
  EventHandler<FullImpressionVisibleEvent> getFullImpressionHandler();

  @Nullable
  EventHandler<InvisibleEvent> getInvisibleHandler();

  @Nullable
  CharSequence getContentDescription();

  @Nullable
  Object getViewTag();

  @Nullable
  SparseArray getViewTags();

  float getShadowElevationPx();

  @Nullable
  ViewOutlineProvider getOutlineProvider();

  boolean getClipToOutline();

  @AccessibilityRole.AccessibilityRoleType
  @Nullable
  String getAccessibilityRole();

  @Nullable
  EventHandler<DispatchPopulateAccessibilityEventEvent>
      getDispatchPopulateAccessibilityEventHandler();

  @Nullable
  EventHandler<OnInitializeAccessibilityEventEvent> getOnInitializeAccessibilityEventHandler();

  @Nullable
  EventHandler<OnInitializeAccessibilityNodeInfoEvent>
      getOnInitializeAccessibilityNodeInfoHandler();

  @Nullable
  EventHandler<OnPopulateAccessibilityEventEvent> getOnPopulateAccessibilityEventHandler();

  @Nullable
  EventHandler<OnRequestSendAccessibilityEventEvent> getOnRequestSendAccessibilityEventHandler();

  @Nullable
  EventHandler<PerformAccessibilityActionEvent> getPerformAccessibilityActionHandler();

  @Nullable
  EventHandler<SendAccessibilityEventEvent> getSendAccessibilityEventHandler();

  @Nullable
  EventHandler<SendAccessibilityEventUncheckedEvent> getSendAccessibilityEventUncheckedHandler();

  float getScale();

  float getAlpha();

  @Nullable
  String getTransitionKey();
}
