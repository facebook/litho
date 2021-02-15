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

import android.annotation.SuppressLint;
import androidx.annotation.Nullable;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.yoga.YogaMeasureFunction;
import com.facebook.yoga.YogaMeasureMode;
import com.facebook.yoga.YogaMeasureOutput;
import com.facebook.yoga.YogaNode;

public class LithoYogaMeasureFunction implements YogaMeasureFunction {

  private final @Nullable LayoutStateContext mLayoutStateContext;

  private final @Nullable LayoutStateContext mPrevLayoutStateContext;

  private Size acquireSize(int initialValue) {
    return new Size(initialValue, initialValue);
  }

  public LithoYogaMeasureFunction(
      @Nullable LayoutStateContext layoutStateContext,
      @Nullable LayoutStateContext prevLayoutStateContext) {
    if (ComponentsConfiguration.useStatelessComponent && layoutStateContext == null) {
      throw new IllegalStateException("You must pass a non-null LayoutStateContext instance");
    }

    mLayoutStateContext = layoutStateContext;
    mPrevLayoutStateContext = prevLayoutStateContext;
  }

  @Override
  @SuppressLint("WrongCall")
  @SuppressWarnings("unchecked")
  public long measure(
      YogaNode cssNode,
      float width,
      YogaMeasureMode widthMode,
      float height,
      YogaMeasureMode heightMode) {
    final InternalNode node = (InternalNode) cssNode.getData();
    final Component component = node.getTailComponent();
    final String componentGlobalKey = node.getTailComponentKey();
    final ComponentContext componentScopedContext =
        component.getScopedContext(mLayoutStateContext, componentGlobalKey);

    try {
      if (componentScopedContext != null && componentScopedContext.wasLayoutCanceled()) {
        return 0;
      }

      final DiffNode diffNode = node.areCachedMeasuresValid() ? node.getDiffNode() : null;

      final int widthSpec;
      final int heightSpec;
      final boolean isTracing = ComponentsSystrace.isTracing();

      widthSpec = SizeSpec.makeSizeSpecFromCssSpec(width, widthMode);
      heightSpec = SizeSpec.makeSizeSpecFromCssSpec(height, heightMode);

      if (isTracing) {
        ComponentsSystrace.beginSectionWithArgs("measure:" + component.getSimpleName())
            .arg("widthSpec", SizeSpec.toString(widthSpec))
            .arg("heightSpec", SizeSpec.toString(heightSpec))
            .arg("componentId", component.getId())
            .flush();
      }

      node.setLastWidthSpec(widthSpec);
      node.setLastHeightSpec(heightSpec);

      int outputWidth = 0;
      int outputHeight = 0;

      ComponentContext context = node.getContext();

      if (Component.isNestedTree(context, component) || node.hasNestedTree()) {

        // Find the nearest parent component context.
        final Component head = node.getHeadComponent();
        final String headKey = node.getHeadComponentKey();
        final Component parent;
        final String parentKey;

        if (component != head) { // If the head and tail are different, use the head.
          parent = head;
          parentKey = headKey;
        } else if (node.getParent() != null) { // Otherwise use the tail of the parent node.
          parent = node.getParent().getTailComponent();
          parentKey = node.getParent().getTailComponentKey();
        } else {
          parent = null;
          parentKey = null;
        }

        if (parent != null) {
          context = parent.getScopedContext(mLayoutStateContext, parentKey);
        }

        final InternalNode nestedTree =
            Layout.create(context, node, widthSpec, heightSpec, mPrevLayoutStateContext);

        outputWidth = nestedTree.getWidth();
        outputHeight = nestedTree.getHeight();
      } else if (diffNode != null
          && diffNode.getLastWidthSpec() == widthSpec
          && diffNode.getLastHeightSpec() == heightSpec
          && !component.shouldAlwaysRemeasure()) {
        outputWidth = (int) diffNode.getLastMeasuredWidth();
        outputHeight = (int) diffNode.getLastMeasuredHeight();
      } else {
        final Size size = acquireSize(Integer.MIN_VALUE /* initialValue */);

        component.onMeasure(componentScopedContext, node, widthSpec, heightSpec, size);

        if (size.width < 0 || size.height < 0) {
          throw new IllegalStateException(
              "MeasureOutput not set, ComponentLifecycle is: " + component);
        }

        outputWidth = size.width;
        outputHeight = size.height;

        if (node.getDiffNode() != null) {
          node.getDiffNode().setLastWidthSpec(widthSpec);
          node.getDiffNode().setLastHeightSpec(heightSpec);
          node.getDiffNode().setLastMeasuredWidth(outputWidth);
          node.getDiffNode().setLastMeasuredHeight(outputHeight);
        }
      }

      node.setLastMeasuredWidth(outputWidth);
      node.setLastMeasuredHeight(outputHeight);

      if (isTracing) {
        ComponentsSystrace.endSection();
      }

      return YogaMeasureOutput.make(outputWidth, outputHeight);
    } catch (Exception e) {
      throw ComponentUtils.wrapWithMetadata(componentScopedContext, e);
    }
  }
}
