// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package com.facebook.rendercore;

import android.content.Context;
import java.util.Map;

public class RenderUnitLayoutFunction implements Node.LayoutFunction {

  public static final Node.LayoutFunction INSTANCE = new RenderUnitLayoutFunction();

  @Override
  public Node.LayoutResult calculateLayout(
      Context context,
      final Node node,
      int widthSpec,
      int heightSpec,
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
    };
  }
}
