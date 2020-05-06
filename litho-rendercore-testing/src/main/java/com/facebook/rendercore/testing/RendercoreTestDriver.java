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

package com.facebook.rendercore.testing;

import android.util.Pair;
import android.view.View;
import com.facebook.rendercore.Node;
import com.facebook.rendercore.RenderState;
import com.facebook.rendercore.RenderTree;
import com.facebook.rendercore.RootHost;

/**
 * Utility methods to render a RenderCore {@link Node} into a {@link RootHost}.
 *
 * <pre>
 *   Example:
 *
 *   Node rootNode = ...;
 *   RootHostView rootHostView = new RootHostView(RuntimeEnvironment.application);
 *
 *   RendercoreTestDriver.forHost(rootHostView)
 *       .withNode(root)
 *       .layoutWithBounds(500, 500)
 *       .render();
 * </pre>
 */
public class RendercoreTestDriver {

  public static NodeBuilderPart forHost(RootHost rootHost) {
    return new Builder(rootHost);
  }

  public interface NodeBuilderPart {

    /** Supply the root Node of the tree. */
    LayoutBuilderPart withNode(Node rootNode);
  }

  public interface LayoutBuilderPart {

    /** Measure the RootHost with these specs. */
    RenderBuilderPart layoutWithSpecs(int widthSpec, int heightSpec);

    /** Equivalent to {@link #layoutWithSpecs} using EXACT specs. */
    RenderBuilderPart layoutWithBounds(int widthPx, int heightPx);
  }

  public interface RenderBuilderPart {

    /** Render into the RootHost using the supplied configuration. */
    void render();
  }

  private static class Builder implements NodeBuilderPart, LayoutBuilderPart, RenderBuilderPart {

    private final RootHost mRootHost;
    private Node mRootNode;
    private int mWidthSpec;
    private int mHeightSpec;

    private Builder(RootHost rootHost) {
      mRootHost = rootHost;
    }

    @Override
    public LayoutBuilderPart withNode(Node rootNode) {
      mRootNode = rootNode;
      return this;
    }

    @Override
    public RenderBuilderPart layoutWithSpecs(int widthSpec, int heightSpec) {
      mWidthSpec = widthSpec;
      mHeightSpec = heightSpec;
      return this;
    }

    @Override
    public RenderBuilderPart layoutWithBounds(int widthPx, int heightPx) {
      mWidthSpec = View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY);
      mHeightSpec = View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY);
      return this;
    }

    @Override
    public void render() {
      View rootHostAsView = (View) mRootHost;

      RenderState renderState =
          new RenderState(
              rootHostAsView.getContext(),
              new RenderState.Delegate() {
                @Override
                public void commit(
                    int layoutVersion,
                    RenderTree current,
                    RenderTree next,
                    Object currentState,
                    Object nextState) {}

                @Override
                public void commitToUI(RenderTree tree, Object o) {}
              });

      mRootHost.setRenderState(renderState);

      renderState.setTree(
          new RenderState.LazyTree() {
            @Override
            public Pair resolve() {
              return new Pair(mRootNode, null);
            }
          });

      rootHostAsView.measure(mWidthSpec, mHeightSpec);
      rootHostAsView.layout(
          0, 0, rootHostAsView.getMeasuredWidth(), rootHostAsView.getMeasuredHeight());
    }
  }
}
