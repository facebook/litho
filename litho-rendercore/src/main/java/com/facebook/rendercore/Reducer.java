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
import android.graphics.Rect;
import androidx.annotation.Nullable;
import java.util.ArrayList;

/**
 * Reduces a tree of Node into a flattened tree of RenderTreeNode. As part of the reduction process
 * all the positions are translated relative to the new hosts.
 */
public class Reducer {

  private static final RenderUnit sRootHostRenderUnit =
      new RenderUnit(RenderUnit.RenderType.VIEW) {
        @Override
        public Object createContent(Context c) {
          return null;
        }

        @Override
        public long getId() {
          return MountState.ROOT_HOST_ID;
        }
      };

  private static void reduceTree(
      Context context,
      Node.LayoutResult layoutResult,
      RenderTreeNode latestHost,
      int xTranslation,
      int yTranslation,
      int x,
      int y,
      ArrayList<RenderTreeNode> flattenedTree) {
    if (layoutResult.getWidth() == 0 && layoutResult.getHeight() == 0) {
      return;
    }
    final RenderUnit renderUnit = layoutResult.getRenderUnit();

    if (renderUnit != null && layoutResult.getChildrenCount() > 0) { // The renderUnit is a host
      // The translated position keeps into account all the parent Layouts that did not render
      // to any host.
      final int translatedXPosition = x + xTranslation;
      final int translatedYPosition = y + yTranslation;

      RenderTreeNode newHost =
          createRenderTreeNode(
              layoutResult, renderUnit, latestHost, translatedXPosition, translatedYPosition);
      flattenedTree.add(newHost);
      latestHost.child(newHost);
      latestHost = newHost;
      xTranslation = 0;
      yTranslation = 0;
      // If this Node also has a RenderUnit its position inside the Host will be 0,0
      x = 0;
      y = 0;
    } else if (renderUnit != null) { // The renderUnit is a leaf content
      if (layoutResult.getChildrenCount() > 0) {
        throw new IllegalStateException(
            "Only nodes without children can have content. A layoutResult with content "
                + renderUnit
                + " has "
                + layoutResult.getChildrenCount()
                + " children");
      }
      final int translatedXPosition = x + xTranslation;
      final int translatedYPosition = y + yTranslation;

      RenderTreeNode content =
          createRenderTreeNode(
              layoutResult, renderUnit, latestHost, translatedXPosition, translatedYPosition);
      flattenedTree.add(content);
      latestHost.child(content);
    } else if (layoutResult.getChildrenCount() > 0) { // No render unit, update the translated
      // position.
      xTranslation += x;
      yTranslation += y;
    }

    for (int i = 0; i < layoutResult.getChildrenCount(); i++) {
      reduceTree(
          context,
          layoutResult.getChildAt(i),
          latestHost,
          xTranslation,
          yTranslation,
          layoutResult.getXForChildAtIndex(i),
          layoutResult.getYForChildAtIndex(i),
          flattenedTree);
    }

    return;
  }

  private static RenderTreeNode createRenderTreeNode(
      Node.LayoutResult layoutResult,
      @Nullable RenderUnit renderUnit,
      @Nullable RenderTreeNode parent,
      int x,
      int y) {

    final boolean hasPadding =
        layoutResult.getPaddingLeft() != 0
            || layoutResult.getPaddingTop() != 0
            || layoutResult.getPaddingRight() != 0
            || layoutResult.getPaddingBottom() != 0;
    final Rect padding =
        hasPadding
            ? new Rect(
                layoutResult.getPaddingLeft(),
                layoutResult.getPaddingTop(),
                layoutResult.getPaddingRight(),
                layoutResult.getPaddingBottom())
            : null;

    RenderTreeNode renderTreeNode =
        new RenderTreeNode(
            parent,
            renderUnit,
            layoutResult.getLayoutData(),
            new Rect(x, y, x + layoutResult.getWidth(), y + layoutResult.getHeight()),
            padding,
            parent != null ? parent.getChildrenCount() : 0);

    return renderTreeNode;
  }

  public static RenderTree getReducedTree(
      Context context, Node.LayoutResult layoutResult, int widthSpec, int heightSpec) {
    ArrayList<RenderTreeNode> flattenedTree = new ArrayList<>();
    RenderTreeNode rootHostNode =
        createRenderTreeNode(layoutResult, sRootHostRenderUnit, null, 0, 0);
    flattenedTree.add(rootHostNode);
    reduceTree(context, layoutResult, rootHostNode, 0, 0, 0, 0, flattenedTree);
    RenderTreeNode[] trimmedRenderNodeTree =
        flattenedTree.toArray(new RenderTreeNode[flattenedTree.size()]);

    return new RenderTree(rootHostNode, trimmedRenderNodeTree, widthSpec, heightSpec);
  }
}
