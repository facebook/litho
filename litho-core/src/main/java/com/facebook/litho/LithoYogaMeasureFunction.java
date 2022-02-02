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

import static com.facebook.litho.Component.hasCachedLayout;
import static com.facebook.litho.Component.isNestedTree;

import android.annotation.SuppressLint;
import androidx.annotation.Nullable;
import androidx.core.util.Preconditions;
import com.facebook.yoga.YogaMeasureFunction;
import com.facebook.yoga.YogaMeasureMode;
import com.facebook.yoga.YogaMeasureOutput;
import com.facebook.yoga.YogaNode;

public class LithoYogaMeasureFunction implements YogaMeasureFunction {

  private Size acquireSize(int initialValue) {
    return new Size(initialValue, initialValue);
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
    final LithoLayoutResult result = (LithoLayoutResult) cssNode.getData();
    final LithoNode node = result.getNode();
    final LayoutStateContext layoutStateContext = result.getLayoutStateContext();

    final Component component = Preconditions.checkNotNull(node.getTailComponent());
    final ComponentContext componentScopedContext = node.getTailComponentContext();

    if (layoutStateContext.isLayoutReleased()) {
      return 0;
    }

    final DiffNode diffNode = result.areCachedMeasuresValid() ? result.getDiffNode() : null;

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
    try {

      result.setLastWidthSpec(widthSpec);
      result.setLastHeightSpec(heightSpec);

      int outputWidth;
      int outputHeight;

      if (isNestedTree(component)
          || hasCachedLayout(layoutStateContext, component)
          || result instanceof NestedTreeHolderResult) {

        final LayoutState layoutState = layoutStateContext.getLayoutState();
        if (layoutState == null) {
          throw new IllegalStateException(
              component.getSimpleName()
                  + ": To measure a component outside of a layout calculation use"
                  + " Component#measureMightNotCacheInternalNode.");
        }

        final int size = node.getComponentCount();
        final ComponentContext parentContext;
        if (size == 1) {
          if (result.getParent() != null) {
            final LithoNode internalNode = result.getParent().getNode();
            parentContext = internalNode.getTailComponentContext();
          } else {
            parentContext = layoutState.getComponentContext();
          }
        } else {
          parentContext = node.getComponentContextAt(1);
        }

        if (isTracing) {
          ComponentsSystrace.beginSection("resolveNestedTree:" + component.getSimpleName());
        }
        try {
          final @Nullable LithoLayoutResult nestedTree =
              Layout.create(
                  layoutStateContext,
                  parentContext,
                  (NestedTreeHolderResult) result,
                  widthSpec,
                  heightSpec);

          outputWidth = nestedTree != null ? nestedTree.getWidth() : 0;
          outputHeight = nestedTree != null ? nestedTree.getHeight() : 0;
        } finally {
          if (isTracing) {
            ComponentsSystrace.endSection();
          }
        }
      } else if (diffNode != null
          && diffNode.getLastWidthSpec() == widthSpec
          && diffNode.getLastHeightSpec() == heightSpec
          && !component.shouldAlwaysRemeasure()) {
        outputWidth = (int) diffNode.getLastMeasuredWidth();
        outputHeight = (int) diffNode.getLastMeasuredHeight();
      } else {
        final Size size = acquireSize(Integer.MIN_VALUE /* initialValue */);

        if (isTracing) {
          ComponentsSystrace.beginSection("onMeasure:" + component.getSimpleName());
        }
        try {
          component.onMeasure(componentScopedContext, result, widthSpec, heightSpec, size, null);
        } catch (Exception e) {
          ComponentUtils.handle(componentScopedContext, e);
          return YogaMeasureOutput.make(0, 0);
        } finally {
          if (isTracing) {
            ComponentsSystrace.endSection();
          }
        }

        if (size.width < 0 || size.height < 0) {
          throw new IllegalStateException(
              "MeasureOutput not set, Component is: "
                  + component
                  + " Width: "
                  + width
                  + " Height: "
                  + height
                  + " WidthMode: "
                  + widthMode.name()
                  + " HeightMode: "
                  + heightMode.name()
                  + " Measured width : "
                  + size.width
                  + " Measured Height: "
                  + size.height);
        }

        outputWidth = size.width;
        outputHeight = size.height;

        if (result.getDiffNode() != null) {
          result.getDiffNode().setLastWidthSpec(widthSpec);
          result.getDiffNode().setLastHeightSpec(heightSpec);
          result.getDiffNode().setLastMeasuredWidth(outputWidth);
          result.getDiffNode().setLastMeasuredHeight(outputHeight);
        }
      }

      result.setLastMeasuredWidth(outputWidth);
      result.setLastMeasuredHeight(outputHeight);
      result.setLastWidthSpec(widthSpec);
      result.setLastHeightSpec(heightSpec);

      return YogaMeasureOutput.make(outputWidth, outputHeight);
    } finally {
      if (isTracing) {
        ComponentsSystrace.endSection();
      }
    }
  }
}
