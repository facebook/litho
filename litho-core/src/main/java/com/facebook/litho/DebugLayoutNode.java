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

package com.facebook.litho;

import android.graphics.drawable.Drawable;
import com.facebook.yoga.YogaAlign;
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

  private final LithoLayoutResult mResult;
  private final InternalNode mNode;

  DebugLayoutNode(LithoLayoutResult result) {
    mResult = result;
    mNode = result.getInternalNode();
  }

  @Nullable
  public Drawable getForeground() {
    return mNode.getForeground();
  }

  @Nullable
  public Drawable getBackground() {
    return mNode.getBackground();
  }

  public boolean hasViewOutput() {
    return InternalNodeUtils.hasViewOutput(mNode);
  }

  public float getRotation() {
    final NodeInfo nodeInfo = mNode.getNodeInfo();
    if (nodeInfo != null) {
      return nodeInfo.getRotation();
    }
    return 0f;
  }

  public float getAlpha() {
    final NodeInfo nodeInfo = mNode.getNodeInfo();
    if (nodeInfo != null) {
      return nodeInfo.getAlpha();
    }
    return 1f;
  }

  public float getScale() {
    final NodeInfo nodeInfo = mNode.getNodeInfo();
    if (nodeInfo != null) {
      return nodeInfo.getScale();
    }
    return 1f;
  }

  public int getImportantForAccessibility() {
    return mNode.getImportantForAccessibility();
  }

  public boolean getFocusable() {
    final NodeInfo nodeInfo = mNode.getNodeInfo();
    if (nodeInfo != null) {
      return nodeInfo.getFocusState() == NodeInfo.FOCUS_SET_TRUE;
    }
    return false;
  }

  @Nullable
  public CharSequence getContentDescription() {
    final NodeInfo nodeInfo = mNode.getNodeInfo();
    if (nodeInfo != null) {
      return nodeInfo.getContentDescription();
    }
    return null;
  }

  public YogaDirection getLayoutDirection() {
    return mResult.getYogaNode().getStyleDirection();
  }

  public YogaFlexDirection getFlexDirection() {
    return mResult.getYogaNode().getFlexDirection();
  }

  public YogaJustify getJustifyContent() {
    return mResult.getYogaNode().getJustifyContent();
  }

  public YogaAlign getAlignItems() {
    return mResult.getYogaNode().getAlignItems();
  }

  public YogaAlign getAlignSelf() {
    return mResult.getYogaNode().getAlignSelf();
  }

  public YogaAlign getAlignContent() {
    return mResult.getYogaNode().getAlignContent();
  }

  public YogaPositionType getPositionType() {
    return mResult.getYogaNode().getPositionType();
  }

  public float getFlexGrow() {
    return mResult.getYogaNode().getFlexGrow();
  }

  public float getFlexShrink() {
    return mResult.getYogaNode().getFlexShrink();
  }

  public YogaValue getFlexBasis() {
    return mResult.getYogaNode().getFlexBasis();
  }

  public YogaValue getWidth() {
    return mResult.getYogaNode().getWidth();
  }

  public YogaValue getMinWidth() {
    return mResult.getYogaNode().getMinWidth();
  }

  public YogaValue getMaxWidth() {
    return mResult.getYogaNode().getMaxWidth();
  }

  public YogaValue getHeight() {
    return mResult.getYogaNode().getHeight();
  }

  public YogaValue getMinHeight() {
    return mResult.getYogaNode().getMinHeight();
  }

  public YogaValue getMaxHeight() {
    return mResult.getYogaNode().getMaxHeight();
  }

  public float getAspectRatio() {
    return mResult.getYogaNode().getAspectRatio();
  }

  public YogaValue getMargin(YogaEdge edge) {
    return mResult.getYogaNode().getMargin(edge);
  }

  public YogaValue getPadding(YogaEdge edge) {
    return mResult.getYogaNode().getPadding(edge);
  }

  public YogaValue getPosition(YogaEdge edge) {
    return mResult.getYogaNode().getPosition(edge);
  }

  public float getBorderWidth(YogaEdge edge) {
    return mResult.getYogaNode().getBorder(edge);
  }

  @Nullable
  public EventHandler getClickHandler() {
    return mNode.getNodeInfo() != null ? mNode.getNodeInfo().getClickHandler() : null;
  }

  public float getLayoutWidth() {
    return mResult.getYogaNode().getLayoutWidth();
  }

  public float getLayoutHeight() {
    return mResult.getYogaNode().getLayoutHeight();
  }

  public float getLayoutMargin(YogaEdge edge) {
    return mResult.getYogaNode().getLayoutMargin(edge);
  }

  public float getLayoutPadding(YogaEdge edge) {
    return mResult.getYogaNode().getLayoutPadding(edge);
  }
}
