/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.graphics.drawable.Drawable;
import com.facebook.litho.reference.Reference;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaConstants;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaFlexDirection;
import com.facebook.yoga.YogaJustify;
import com.facebook.yoga.YogaPositionType;
import com.facebook.yoga.YogaValue;
import javax.annotation.Nullable;

/**
 * A DebugLayoutNode is a wrapper around InternalNode which allows debug tools to inspect and mutate
 * internal nodes without making InternalNode a public class. This class should never be used in
 * production and only for building debug tools.
 */
public final class DebugLayoutNode {
  private InternalNode mNode;

  DebugLayoutNode(InternalNode node) {
    mNode = node;
  }

  @Nullable
  public Drawable getForeground() {
    return mNode.getForeground();
  }

  public void setForegroundColor(int color) {
    mNode.foregroundColor(color);
  }

  @Nullable
  public Reference<? extends Drawable> getBackground() {
    return mNode.getBackground();
  }

  public void setBackgroundColor(int color) {
    mNode.backgroundColor(color);
  }

  @Nullable
  public Integer getImportantForAccessibility() {
    return mNode.getImportantForAccessibility();
  }

  public void setImportantForAccessibility(int importantForAccessibility) {
    mNode.importantForAccessibility(importantForAccessibility);
  }

  public boolean getFocusable() {
    final NodeInfo nodeInfo = mNode.getNodeInfo();
    if (nodeInfo != null) {
      return nodeInfo.getFocusState() == NodeInfo.FOCUS_SET_TRUE;
    }
    return false;
  }

  public void setFocusable(boolean focusable) {
    mNode.focusable(focusable);
  }

  @Nullable
  public CharSequence getContentDescription() {
    final NodeInfo nodeInfo = mNode.getNodeInfo();
    if (nodeInfo != null) {
      return nodeInfo.getContentDescription();
    }
    return null;
  }

  public void setContentDescription(CharSequence contentDescription) {
    mNode.contentDescription(contentDescription);
  }

  public void setLayoutDirection(YogaDirection yogaDirection) {
    mNode.layoutDirection(yogaDirection);
  }

  public YogaDirection getLayoutDirection() {
    return mNode.mYogaNode.getLayoutDirection();
  }

  public void setFlexDirection(YogaFlexDirection direction) {
    mNode.flexDirection(direction);
  }

  public YogaFlexDirection getFlexDirection() {
    return mNode.mYogaNode.getFlexDirection();
  }

  public void setJustifyContent(YogaJustify yogaJustify) {
    mNode.justifyContent(yogaJustify);
  }

  public YogaJustify getJustifyContent() {
    return mNode.mYogaNode.getJustifyContent();
  }

  public void setAlignItems(YogaAlign yogaAlign) {
    mNode.alignItems(yogaAlign);
  }

  public YogaAlign getAlignItems() {
    return mNode.mYogaNode.getAlignItems();
  }

  public void setAlignSelf(YogaAlign yogaAlign) {
    mNode.alignSelf(yogaAlign);
  }

  public YogaAlign getAlignSelf() {
    return mNode.mYogaNode.getAlignSelf();
  }

  public void setAlignContent(YogaAlign yogaAlign) {
    mNode.alignContent(yogaAlign);
  }

  public YogaAlign getAlignContent() {
    return mNode.mYogaNode.getAlignContent();
  }

  public void setPositionType(YogaPositionType yogaPositionType) {
    mNode.positionType(yogaPositionType);
  }

  public YogaPositionType getPositionType() {
    return mNode.mYogaNode.getPositionType();
  }

  public void setFlexGrow(float value) {
    mNode.flexGrow(value);
  }

  public float getFlexGrow() {
    return mNode.mYogaNode.getFlexGrow();
  }

  public void setFlexShrink(float value) {
    mNode.flexShrink(value);
  }

  public float getFlexShrink() {
    return mNode.mYogaNode.getFlexShrink();
  }

  public void setFlexBasis(YogaValue value) {
    switch (value.unit) {
      case UNDEFINED:
      case AUTO:
        mNode.flexBasisAuto();
        break;
      case PERCENT:
        mNode.flexBasisPercent(value.value);
        break;
      case POINT:
        mNode.flexBasisPx((int) value.value);
        break;
    }
  }

  public YogaValue getFlexBasis() {
    return mNode.mYogaNode.getFlexBasis();
  }

  public void setWidth(YogaValue value) {
    switch (value.unit) {
      case UNDEFINED:
      case AUTO:
        mNode.widthAuto();
        break;
      case PERCENT:
        mNode.widthPercent(value.value);
        break;
      case POINT:
        mNode.widthPx((int) value.value);
        break;
    }
  }

  public YogaValue getWidth() {
    return mNode.mYogaNode.getWidth();
  }

  public void setMinWidth(YogaValue value) {
    switch (value.unit) {
      case UNDEFINED:
      case AUTO:
        mNode.minWidthPx(Integer.MIN_VALUE);
        break;
      case PERCENT:
        mNode.minWidthPercent(value.value);
        break;
      case POINT:
        mNode.minWidthPx((int) value.value);
        break;
    }
  }

  public YogaValue getMinWidth() {
    return mNode.mYogaNode.getMinWidth();
  }

  public void setMaxWidth(YogaValue value) {
    switch (value.unit) {
      case UNDEFINED:
      case AUTO:
        mNode.maxWidthPx(Integer.MAX_VALUE);
        break;
      case PERCENT:
        mNode.maxWidthPercent(value.value);
        break;
      case POINT:
        mNode.maxWidthPx((int) value.value);
        break;
    }
  }

  public YogaValue getMaxWidth() {
    return mNode.mYogaNode.getMaxWidth();
  }

  public void setHeight(YogaValue value) {
    switch (value.unit) {
      case UNDEFINED:
      case AUTO:
        mNode.heightAuto();
        break;
      case PERCENT:
        mNode.heightPercent(value.value);
        break;
      case POINT:
        mNode.heightPx((int) value.value);
        break;
    }
  }

  public YogaValue getHeight() {
    return mNode.mYogaNode.getHeight();
  }

  public void setMinHeight(YogaValue value) {
    switch (value.unit) {
      case UNDEFINED:
      case AUTO:
        mNode.minHeightPx(Integer.MIN_VALUE);
        break;
      case PERCENT:
        mNode.minHeightPercent(value.value);
        break;
      case POINT:
        mNode.minHeightPx((int) value.value);
        break;
    }
  }

  public YogaValue getMinHeight() {
    return mNode.mYogaNode.getMinHeight();
  }

  public void setMaxHeight(YogaValue value) {
    switch (value.unit) {
      case UNDEFINED:
      case AUTO:
        mNode.maxHeightPx(Integer.MAX_VALUE);
        break;
      case PERCENT:
        mNode.maxHeightPercent(value.value);
        break;
      case POINT:
        mNode.maxHeightPx((int) value.value);
        break;
    }
  }

  public YogaValue getMaxHeight() {
    return mNode.mYogaNode.getMaxHeight();
  }

  public void setAspectRatio(float aspectRatio) {
    mNode.aspectRatio(aspectRatio);
  }

  public float getAspectRatio() {
    return mNode.mYogaNode.getAspectRatio();
  }

  public void setMargin(YogaEdge edge, YogaValue value) {
    switch (value.unit) {
      case UNDEFINED:
        mNode.marginPx(edge, 0);
        break;
      case AUTO:
        mNode.marginAuto(edge);
        break;
      case PERCENT:
        mNode.marginPercent(edge, value.value);
        break;
      case POINT:
        mNode.marginPx(edge, (int) value.value);
        break;
    }
  }

  public YogaValue getMargin(YogaEdge edge) {
    return mNode.mYogaNode.getMargin(edge);
  }

  public float getResultMargin(YogaEdge edge) {
    return mNode.mYogaNode.getLayoutMargin(edge);
  }

  public void setPadding(YogaEdge edge, YogaValue value) {
    switch (value.unit) {
      case UNDEFINED:
      case AUTO:
        mNode.paddingPx(edge, 0);
        break;
      case PERCENT:
        mNode.paddingPercent(edge, value.value);
        break;
      case POINT:
        mNode.paddingPx(edge, (int) value.value);
        break;
    }
  }

  public YogaValue getPadding(YogaEdge edge) {
    return mNode.mYogaNode.getPadding(edge);
  }

  public float getResultPadding(YogaEdge edge) {
    return mNode.mYogaNode.getLayoutPadding(edge);
  }

  public void setPosition(YogaEdge edge, YogaValue value) {
    switch (value.unit) {
      case UNDEFINED:
      case AUTO:
        mNode.positionPercent(edge, YogaConstants.UNDEFINED);
        break;
      case PERCENT:
        mNode.positionPercent(edge, value.value);
        break;
      case POINT:
        mNode.positionPx(edge, (int) value.value);
        break;
    }
  }

  public YogaValue getPosition(YogaEdge edge) {
    return mNode.mYogaNode.getPosition(edge);
  }

  public void setBorderWidth(YogaEdge edge, float value) {
    mNode.setBorderWidth(edge, (int) value);
  }

  public float getBorderWidth(YogaEdge edge) {
    return mNode.mYogaNode.getBorder(edge);
  }

  @Nullable
  public EventHandler getClickHandler() {
    return mNode.getClickHandler();
  }
}
