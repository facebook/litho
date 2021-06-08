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
import com.facebook.litho.LithoLayoutResult.NestedTreeHolderResult;
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
    final InternalNode node = result.getInternalNode();
    final LayoutStateContext layoutStateContext = node.getContext().getLayoutStateContext();

    final Component component = node.getTailComponent();
    final String componentGlobalKey = node.getTailComponentKey();
    final ComponentContext componentScopedContext =
        component.getScopedContext(layoutStateContext, componentGlobalKey);

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

      result.setLastWidthSpec(widthSpec);
      result.setLastHeightSpec(heightSpec);

      int outputWidth = 0;
      int outputHeight = 0;

      ComponentContext context = result.getContext();

      if (Component.isNestedTree(context, component) || result instanceof NestedTreeHolderResult) {

        final int size = node.getComponents().size();
        final ComponentContext parentContext;
        if (size == 1) {
          final String parentKey = result.getParent().getInternalNode().getTailComponentKey();
          parentContext =
              result
                  .getParent()
                  .getInternalNode()
                  .getTailComponent()
                  .getScopedContext(context.getLayoutStateContext(), parentKey);
        } else {
          parentContext =
              node.getComponents()
                  .get(1)
                  .getScopedContext(layoutStateContext, node.getComponentKeys().get(1));
        }

        final LayoutStateContext prevLayoutStateContext =
            layoutStateContext == null || layoutStateContext.getLayoutState() == null
                ? null
                : layoutStateContext.getLayoutState().getPrevLayoutStateContext();

        final LithoLayoutResult nestedTree =
            Layout.create(
                parentContext,
                (NestedTreeHolderResult) result,
                widthSpec,
                heightSpec,
                prevLayoutStateContext);

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

        component.onMeasure(componentScopedContext, result, widthSpec, heightSpec, size);

        if (size.width < 0 || size.height < 0) {
          throw new IllegalStateException(
              "MeasureOutput not set, ComponentLifecycle is: "
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

        if (node.getDiffNode() != null) {
          node.getDiffNode().setLastWidthSpec(widthSpec);
          node.getDiffNode().setLastHeightSpec(heightSpec);
          node.getDiffNode().setLastMeasuredWidth(outputWidth);
          node.getDiffNode().setLastMeasuredHeight(outputHeight);
        }
      }

      result.setLastMeasuredWidth(outputWidth);
      result.setLastMeasuredHeight(outputHeight);
      result.setLastWidthSpec(widthSpec);
      result.setLastHeightSpec(heightSpec);

      if (isTracing) {
        ComponentsSystrace.endSection();
      }

      return YogaMeasureOutput.make(outputWidth, outputHeight);
    } catch (Exception e) {
      ComponentUtils.handle(componentScopedContext, e);
      return YogaMeasureOutput.make(0, 0);
    }
  }
}
