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

package com.facebook.litho;

import android.graphics.PathEffect;
import androidx.annotation.Nullable;
import com.facebook.yoga.YogaNode;

/**
 * This class is a placeholder for the unresolved layout and result of a {@link Component}s which
 * implement the {@link @OnCreateLayoutWithSizeSpec}.The {@link TreeProps}, padding and border width
 * properties and held separately so that they can be copied into the actual nested tree layout
 * before measuring it.
 */
public class NestedTreeHolder extends LithoNode {

  final @Nullable TreeProps mPendingTreeProps;
  final @Nullable LithoNode mCachedNode;

  @Nullable ComponentContext mParentContext;
  @Nullable int[] mNestedBorderEdges;
  @Nullable Edges mNestedTreePadding;
  @Nullable boolean[] mNestedIsPaddingPercentage;

  protected NestedTreeHolder(@Nullable TreeProps props) {
    this(props, null);
  }

  protected NestedTreeHolder(@Nullable TreeProps props, @Nullable LithoNode cacheNode) {
    super();
    mPendingTreeProps = TreeProps.copy(props);
    mCachedNode = cacheNode;
  }

  protected NestedTreeHolder(
      @Nullable TreeProps props,
      @Nullable LithoNode cacheNode,
      @Nullable ComponentContext parentContext) {
    this(props, cacheNode);
    mParentContext = parentContext;
  }

  /**
   * When a node is measured during Component.measure and a layout-result is cached, it is cached
   * using that node as the key. Later, this layout may resolve a nested-tree-holder node, and so in
   * order to be able to access this cache, this node is used.
   */
  @Nullable
  public LithoNode getCachedNode() {
    return mCachedNode;
  }

  public @Nullable TreeProps getPendingTreeProps() {
    return mPendingTreeProps;
  }

  @Override
  public void border(int[] widths, int[] colors, float[] radii, PathEffect effect) {
    mNestedBorderEdges = new int[Border.EDGE_COUNT];
    System.arraycopy(widths, 0, mNestedBorderEdges, 0, mNestedBorderEdges.length);
    System.arraycopy(colors, 0, getBorderColors(), 0, getBorderColors().length);
    System.arraycopy(radii, 0, getBorderRadius(), 0, getBorderRadius().length);
    setBorderPathEffect(effect);
  }

  @Override
  protected NestedTreeYogaLayoutProps createYogaNodeWriter() {
    return new NestedTreeYogaLayoutProps(NodeConfig.createYogaNode());
  }

  @Override
  public void writeToYogaNode(YogaLayoutProps writer) {
    NestedTreeYogaLayoutProps actual = (NestedTreeYogaLayoutProps) writer;
    super.writeToYogaNode(writer);
    mNestedBorderEdges = actual.getBorderWidth();
    mNestedTreePadding = actual.getPadding();
    mNestedIsPaddingPercentage = actual.isPaddingPercentage();
  }

  @Override
  public NestedTreeHolderResult createLayoutResult(
      final YogaNode node, final float widthFromStyle, final float heightFromStyle) {
    return new NestedTreeHolderResult(
        getTailComponentContext(), this, node, widthFromStyle, heightFromStyle);
  }

  public void copyInto(LithoNode target) {
    // Defer copying, and set this NestedTreeHolder on the target. The props will be
    // transferred to the nested result during layout calculation.
    target.setNestedTreeHolder(this);
  }

  public void transferInto(LithoNode target) {
    if (getNodeInfo() != null) {
      target.applyNodeInfo(getNodeInfo());
    }
    if (target.isImportantForAccessibilityIsSet()) {
      target.importantForAccessibility(getImportantForAccessibility());
    }
    target.duplicateParentState(isDuplicateParentStateEnabled());
    if ((getPrivateFlags() & PFLAG_DUPLICATE_CHILDREN_STATES_IS_SET) != 0L) {
      target.duplicateChildrenStates(isDuplicateChildrenStatesEnabled());
    }
    if ((getPrivateFlags() & PFLAG_BACKGROUND_IS_SET) != 0L) {
      target.background(getBackground());
      target.setPaddingFromBackground(getPaddingFromBackground());
    }
    if ((getPrivateFlags() & PFLAG_FOREGROUND_IS_SET) != 0L) {
      target.foreground(getForeground());
    }
    if (isForceViewWrapping()) {
      target.wrapInView();
    }
    if ((getPrivateFlags() & PFLAG_VISIBLE_HANDLER_IS_SET) != 0L) {
      target.visibleHandler(getVisibleHandler());
    }
    if ((getPrivateFlags() & PFLAG_FOCUSED_HANDLER_IS_SET) != 0L) {
      target.focusedHandler(getFocusedHandler());
    }
    if ((getPrivateFlags() & PFLAG_FULL_IMPRESSION_HANDLER_IS_SET) != 0L) {
      target.fullImpressionHandler(getFullImpressionHandler());
    }
    if ((getPrivateFlags() & PFLAG_INVISIBLE_HANDLER_IS_SET) != 0L) {
      target.invisibleHandler(getInvisibleHandler());
    }
    if ((getPrivateFlags() & PFLAG_UNFOCUSED_HANDLER_IS_SET) != 0L) {
      target.unfocusedHandler(getUnfocusedHandler());
    }
    if ((getPrivateFlags() & PFLAG_VISIBLE_RECT_CHANGED_HANDLER_IS_SET) != 0L) {
      target.visibilityChangedHandler(getVisibilityChangedHandler());
    }
    if (getTestKey() != null) {
      target.testKey(getTestKey());
    }
    if (mNestedBorderEdges != null) {
      target.border(
          mNestedBorderEdges, getBorderColors(), getBorderRadius(), getBorderPathEffect());
    }
    if ((getPrivateFlags() & PFLAG_TRANSITION_KEY_IS_SET) != 0L) {
      target.transitionKey(getTransitionKey(), getTransitionOwnerKey());
    }
    if ((getPrivateFlags() & PFLAG_TRANSITION_KEY_TYPE_IS_SET) != 0L) {
      target.transitionKeyType(getTransitionKeyType());
    }
    if (getVisibleHeightRatio() != 0) {
      target.visibleHeightRatio(getVisibleHeightRatio());
    }
    if (getVisibleWidthRatio() != 0) {
      target.visibleWidthRatio(getVisibleWidthRatio());
    }
    if ((getPrivateFlags() & PFLAG_STATE_LIST_ANIMATOR_SET) != 0L) {
      target.stateListAnimator(getStateListAnimator());
    }
    if ((getPrivateFlags() & PFLAG_STATE_LIST_ANIMATOR_RES_SET) != 0L) {
      target.stateListAnimatorRes(getStateListAnimatorRes());
    }
    if (getLayerType() != LayerType.LAYER_TYPE_NOT_SET) {
      target.layerType(getLayerType(), getLayerPaint());
    }

    target.setNestedPadding(mNestedTreePadding, mNestedIsPaddingPercentage);
  }
}
