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

package com.facebook.rendercore;

import android.content.Context;
import java.util.Map;

public class RenderUnitLayoutFunction implements Node.LayoutFunction {

  public static final Node.LayoutFunction INSTANCE = new RenderUnitLayoutFunction();

  @Override
  public Node.LayoutResult calculateLayout(
      Context context,
      final Node node,
      final int widthSpec,
      final int heightSpec,
      LayoutCache layoutCache,
      Map layoutContexts) {
    if (node.getRenderUnit() == null) {
      throw new IllegalStateException(
          "Calling a RenderUnitLayoutFunction on a Node that does not have a RenderUnit");
    }

    final RenderUnit renderUnit = node.getRenderUnit();
    final int[] size = new int[2];
    renderUnit.measure(context, widthSpec, heightSpec, size, layoutContexts);

    return new Node.LayoutResult() {
      @Override
      public Node getNode() {
        return node;
      }

      @Override
      public int getChildrenCount() {
        return 0;
      }

      @Override
      public Node.LayoutResult getChildAt(int index) {
        throw new RuntimeException("Accessing a child");
      }

      @Override
      public int getXForChildAtIndex(int index) {
        throw new RuntimeException("Accessing a child");
      }

      @Override
      public int getYForChildAtIndex(int index) {
        throw new RuntimeException("Accessing a child");
      }

      @Override
      public int getWidth() {
        return size[0];
      }

      @Override
      public int getHeight() {
        return size[1];
      }

      @Override
      public int getPaddingTop() {
        return 0;
      }

      @Override
      public int getPaddingRight() {
        return 0;
      }

      @Override
      public int getPaddingBottom() {
        return 0;
      }

      @Override
      public int getPaddingLeft() {
        return 0;
      }

      @Override
      public int getWidthSpec() {
        return widthSpec;
      }

      @Override
      public int getHeightSpec() {
        return heightSpec;
      }
    };
  }
}
