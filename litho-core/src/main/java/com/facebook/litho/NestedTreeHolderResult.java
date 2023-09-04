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

import androidx.annotation.Nullable;
import com.facebook.rendercore.LayoutContext;
import com.facebook.rendercore.MeasureResult;
import com.facebook.yoga.YogaNode;

/**
 * This is an output only {@link NestedTreeHolderResult}; this is created by a {@link
 * NestedTreeHolder}.
 */
public class NestedTreeHolderResult extends LithoLayoutResult {

  @Nullable LithoLayoutResult mNestedTree;

  public NestedTreeHolderResult(
      final ComponentContext c,
      final NestedTreeHolder internalNode,
      final YogaNode yogaNode,
      final float widthFromStyle,
      final float heightFromStyle) {
    super(c, internalNode, yogaNode, widthFromStyle, heightFromStyle);
  }

  @Override
  public NestedTreeHolder getNode() {
    return (NestedTreeHolder) super.getNode();
  }

  public @Nullable LithoLayoutResult getNestedResult() {
    return mNestedTree;
  }

  public void setNestedResult(@Nullable LithoLayoutResult tree) {
    mNestedTree = tree;
  }

  @Override
  protected MeasureResult measureInternal(
      LayoutContext<LithoRenderContext> context, int widthSpec, int heightSpec) {
    final boolean isTracing = ComponentsSystrace.isTracing();
    final Component component = mNode.getTailComponent();
    if (context.getRenderContext().lithoLayoutContext.isReleased()) {
      throw new IllegalStateException(
          component.getSimpleName()
              + ": To measure a component outside of a layout calculation use"
              + " Component#measureMightNotCacheInternalNode.");
    }

    final int count = mNode.getComponentCount();
    final ComponentContext parentContext;
    if (count == 1) {
      final ComponentContext parentFromNode = getNode().mParentContext;
      if (parentFromNode != null) {
        parentContext = parentFromNode;
      } else {
        parentContext = context.getRenderContext().lithoLayoutContext.getRootComponentContext();
      }
    } else {
      parentContext = mNode.getComponentContextAt(1);
    }

    if (parentContext == null) {
      throw new IllegalStateException(
          component.getSimpleName() + ": Null component context during measure");
    }

    if (isTracing) {
      ComponentsSystrace.beginSection("resolveNestedTree:" + component.getSimpleName());
    }
    try {
      final @Nullable LithoLayoutResult nestedTree =
          Layout.measure(
              context.getRenderContext().lithoLayoutContext,
              parentContext,
              this,
              widthSpec,
              heightSpec);

      if (nestedTree != null) {
        return new MeasureResult(
            nestedTree.getWidth(), nestedTree.getHeight(), nestedTree.getLayoutData());
      } else {
        return new MeasureResult(0, 0);
      }
    } finally {
      if (isTracing) {
        ComponentsSystrace.endSection();
      }
    }
  }

  @Override
  void releaseLayoutPhaseData() {
    super.releaseLayoutPhaseData();
    if (mNestedTree != null) {
      mNestedTree.releaseLayoutPhaseData();
    }
  }
}
