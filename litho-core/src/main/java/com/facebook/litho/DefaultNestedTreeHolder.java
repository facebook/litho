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

import static com.facebook.litho.ComponentContext.NULL_LAYOUT;

import androidx.annotation.Px;
import com.facebook.litho.InternalNode.NestedTreeHolder;
import com.facebook.litho.LithoLayoutResult.NestedTreeHolderResult;
import com.facebook.litho.annotations.OnCreateLayoutWithSizeSpec;
import com.facebook.yoga.YogaConstants;
import com.facebook.yoga.YogaEdge;
import javax.annotation.Nullable;

/**
 * This class is a placeholder for the unresolved layout and result of a {@link Component}s which
 * implement the {@link OnCreateLayoutWithSizeSpec}.The {@link TreeProps}, padding and border width
 * properties and held separately so that they can be copied into the actual nested tree layout
 * before measuring it.
 */
public class DefaultNestedTreeHolder extends DefaultInternalNode
    implements NestedTreeHolder, NestedTreeHolderResult {

  /* INPUTS */
  final @Nullable TreeProps mPendingTreeProps;

  @Nullable Edges mNestedTreePadding;
  @Nullable Edges mNestedTreeBorderWidth;

  /* OUTPUTS */
  @Nullable LithoLayoutResult mNestedTree;

  protected DefaultNestedTreeHolder(ComponentContext context, @Nullable TreeProps props) {
    super(context);
    mPendingTreeProps = TreeProps.copy(props);
  }

  @Override
  public @Nullable TreeProps getPendingTreeProps() {
    return mPendingTreeProps;
  }

  @Override
  public @Nullable LithoLayoutResult getNestedResult() {
    return mNestedTree;
  }

  @Override
  public void setNestedResult(@Nullable LithoLayoutResult tree) {
    mNestedTree = tree;
    if (tree != null) {
      tree.setParent(this);
    }
  }

  @Override
  public DefaultNestedTreeHolder getInternalNode() {
    return this;
  }

  @Override
  public void paddingPercent(YogaEdge edge, float percent) {
    mPrivateFlags |= PFLAG_PADDING_IS_SET;
    getNestedTreePadding().set(edge, percent);
    setIsPaddingPercent(edge, true);
  }

  @Override
  public void paddingPx(YogaEdge edge, @Px int padding) {
    mPrivateFlags |= PFLAG_PADDING_IS_SET;
    getNestedTreePadding().set(edge, padding);
    setIsPaddingPercent(edge, false);
  }

  @Override
  public void setBorderWidth(YogaEdge edge, @Px int borderWidth) {
    if (mNestedTreeBorderWidth == null) {
      mNestedTreeBorderWidth = new Edges();
    }

    mNestedTreeBorderWidth.set(edge, borderWidth);
  }

  private Edges getNestedTreePadding() {
    if (mNestedTreePadding == null) {
      mNestedTreePadding = new Edges();
    }
    return mNestedTreePadding;
  }

  @Override
  protected void clean() {
    super.clean();
    mNestedTree = null;
  }

  @Override
  public void copyInto(InternalNode target) {
    if (target == NULL_LAYOUT) {
      return;
    }

    if (mNodeInfo != null) {
      if (target.getNodeInfo() == null) {
        target.setNodeInfo(mNodeInfo);
      } else {
        mNodeInfo.copyInto(target.getOrCreateNodeInfo());
      }
    }
    if (target.isLayoutDirectionInherit()) {
      target.layoutDirection(getResolvedLayoutDirection());
    }
    if (target.isImportantForAccessibilityIsSet()) {
      target.importantForAccessibility(mImportantForAccessibility);
    }
    if ((mPrivateFlags & PFLAG_DUPLICATE_PARENT_STATE_IS_SET) != 0L) {
      target.duplicateParentState(mDuplicateParentState);
    }
    if ((mPrivateFlags & PFLAG_DUPLICATE_CHILDREN_STATES_IS_SET) != 0L) {
      target.duplicateChildrenStates(mDuplicateChildrenStates);
    }
    if ((mPrivateFlags & PFLAG_BACKGROUND_IS_SET) != 0L) {
      target.background(mBackground);
    }
    if ((mPrivateFlags & PFLAG_FOREGROUND_IS_SET) != 0L) {
      target.foreground(mForeground);
    }
    if (mForceViewWrapping) {
      target.wrapInView();
    }
    if ((mPrivateFlags & PFLAG_VISIBLE_HANDLER_IS_SET) != 0L) {
      target.visibleHandler(mVisibleHandler);
    }
    if ((mPrivateFlags & PFLAG_FOCUSED_HANDLER_IS_SET) != 0L) {
      target.focusedHandler(mFocusedHandler);
    }
    if ((mPrivateFlags & PFLAG_FULL_IMPRESSION_HANDLER_IS_SET) != 0L) {
      target.fullImpressionHandler(mFullImpressionHandler);
    }
    if ((mPrivateFlags & PFLAG_INVISIBLE_HANDLER_IS_SET) != 0L) {
      target.invisibleHandler(mInvisibleHandler);
    }
    if ((mPrivateFlags & PFLAG_UNFOCUSED_HANDLER_IS_SET) != 0L) {
      target.unfocusedHandler(mUnfocusedHandler);
    }
    if ((mPrivateFlags & PFLAG_VISIBLE_RECT_CHANGED_HANDLER_IS_SET) != 0L) {
      target.visibilityChangedHandler(mVisibilityChangedHandler);
    }
    if (mTestKey != null) {
      target.testKey(mTestKey);
    }
    if ((mPrivateFlags & PFLAG_PADDING_IS_SET) != 0L) {
      for (int i = 0; i < Edges.EDGES_LENGTH; i++) {
        float value = mNestedTreePadding.getRaw(i);
        if (!YogaConstants.isUndefined(value)) {
          final YogaEdge edge = YogaEdge.fromInt(i);
          if (isPaddingPercent(edge)) {
            target.paddingPercent(edge, value);
          } else {
            target.paddingPx(edge, (int) value);
          }
        }
      }
    }
    if ((mPrivateFlags & PFLAG_BORDER_IS_SET) != 0L) {
      target.border(mNestedTreeBorderWidth, mBorderColors, mBorderRadius);
    }
    if ((mPrivateFlags & PFLAG_TRANSITION_KEY_IS_SET) != 0L) {
      target.transitionKey(mTransitionKey, mTransitionOwnerKey);
    }
    if ((mPrivateFlags & PFLAG_TRANSITION_KEY_TYPE_IS_SET) != 0L) {
      target.transitionKeyType(mTransitionKeyType);
    }
    if (mVisibleHeightRatio != 0) {
      target.visibleHeightRatio(mVisibleHeightRatio);
    }
    if (mVisibleWidthRatio != 0) {
      target.visibleWidthRatio(mVisibleWidthRatio);
    }
    if ((mPrivateFlags & PFLAG_STATE_LIST_ANIMATOR_SET) != 0L) {
      target.stateListAnimator(mStateListAnimator);
    }
    if ((mPrivateFlags & PFLAG_STATE_LIST_ANIMATOR_RES_SET) != 0L) {
      target.stateListAnimatorRes(mStateListAnimatorRes);
    }
    if (mLayerType != LayerType.LAYER_TYPE_NOT_SET) {
      target.layerType(mLayerType, mLayerPaint);
    }
  }
}
