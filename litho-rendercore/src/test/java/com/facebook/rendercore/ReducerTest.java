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

package com.facebook.rendercore;

import android.content.Context;
import android.graphics.Rect;
import android.util.Pair;
import android.view.View;
import com.facebook.rendercore.extensions.RenderCoreExtension;
import com.facebook.rendercore.testing.TestLayoutResultVisitor;
import com.facebook.rendercore.testing.TestNode;
import com.facebook.rendercore.testing.TestRenderCoreExtension;
import com.facebook.rendercore.testing.TestRenderUnit;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ReducerTest {

  @Test
  public void testHostFlattening() {
    TestNode root = new TestNode();
    TestNode one = new TestNode();
    TestNode two = new TestNode();
    TestNode leaf = new TestNode();
    TestNode leafTwo = new TestNode();

    root.addChild(one);
    one.addChild(two);
    two.addChild(leaf);
    root.addChild(leafTwo);

    leaf.setRenderUnit(new TestRenderUnit());
    leafTwo.setRenderUnit(new TestRenderUnit());

    final Context c = RuntimeEnvironment.getApplication();
    final int widthSpec = View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY);
    final int heightSpec = View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY);
    final LayoutContext layoutContext = new LayoutContext(c, null, -1, new LayoutCache(), null);

    final LayoutResult result = root.calculateLayout(layoutContext, widthSpec, heightSpec);
    final RenderTree renderTree =
        Reducer.getReducedTree(c, result, widthSpec, heightSpec, RenderState.NO_ID, null);

    // We expect one RenderUnit for each of the leaves and one for the root.
    Assertions.assertThat(renderTree.getMountableOutputCount()).isEqualTo(3);
  }

  @Test
  public void testViewTranslation() {
    TestNode root = new TestNode(0, 0, 200, 200);
    TestNode leaf = new TestNode(0, 0, 200, 100);
    TestNode leafTwo = new TestNode(0, 100, 200, 100);

    root.addChild(leaf);
    root.addChild(leafTwo);

    leaf.setRenderUnit(new TestRenderUnit());
    leafTwo.setRenderUnit(new TestRenderUnit());

    final Context c = RuntimeEnvironment.getApplication();
    final int widthSpec = View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY);
    final int heightSpec = View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY);
    final LayoutContext layoutContext = new LayoutContext(c, null, -1, new LayoutCache(), null);

    final LayoutResult result = root.calculateLayout(layoutContext, widthSpec, heightSpec);
    final RenderTree renderTree =
        Reducer.getReducedTree(c, result, widthSpec, heightSpec, RenderState.NO_ID, null);

    // We expect one RenderUnit for each of the leaves, one for the root and one for the Host.
    Assertions.assertThat(renderTree.getMountableOutputCount()).isEqualTo(3);
    Assertions.assertThat(renderTree.getRenderTreeNodeAtIndex(0).getBounds())
        .isEqualTo(new Rect(0, 0, 200, 200));
    Assertions.assertThat(renderTree.getRenderTreeNodeAtIndex(1).getBounds())
        .isEqualTo(new Rect(0, 0, 200, 100));
    Assertions.assertThat(renderTree.getRenderTreeNodeAtIndex(2).getBounds())
        .isEqualTo(new Rect(0, 100, 200, 200));
  }

  @Test
  public void whenReducedWithExtensions_shouldRunLayoutResultVisitors() {
    final TestNode root = new TestNode(0, 0, 200, 200);
    final TestNode leaf = new TestNode(0, 0, 200, 100);
    final TestNode leafTwo = new TestNode(0, 100, 200, 100);

    root.addChild(leaf);
    root.addChild(leafTwo);

    leaf.setRenderUnit(new TestRenderUnit());
    leafTwo.setRenderUnit(new TestRenderUnit());

    final Context c = RuntimeEnvironment.getApplication();
    final int widthSpec = View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY);
    final int heightSpec = View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY);
    final LayoutContext layoutContext = new LayoutContext(c, null, -1, new LayoutCache(), null);
    final LayoutResult result = root.calculateLayout(layoutContext, widthSpec, heightSpec);

    final RenderCoreExtension e1 = new TestRenderCoreExtension();
    final RenderCoreExtension e2 = new RenderCoreExtension();
    final RenderCoreExtension e3 = new TestRenderCoreExtension(new TestLayoutResultVisitor(), null);

    final RenderCoreExtension<?, ?>[] extensions = new RenderCoreExtension<?, ?>[] {e1, e2, e3};

    final RenderTree renderTree =
        Reducer.getReducedTree(c, result, widthSpec, heightSpec, RenderState.NO_ID, extensions);

    List<Pair<RenderCoreExtension<?, ?>, Object>> results = renderTree.getExtensionResults();

    Assertions.assertThat(results).isNotNull();
    Assertions.assertThat(results).hasSize(3);
    for (Pair<RenderCoreExtension<?, ?>, Object> r : results) {
      if (r.first == e1) {
        Assertions.assertThat(r.second).isNotNull();
        Assertions.assertThat(r.second).isInstanceOf(List.class);
        Assertions.assertThat((List) r.second).hasSize(4);
      } else if (r.first == e2 || r.first == e3) {
        Assertions.assertThat(r.second).isNull();
      } else {
        throw new AssertionError("Unexpected extension found");
      }
    }
  }

  @Test
  public void whenReducedWithZeroSizedNonLeafNode_shouldRetainSubtree() {
    final TestNode root = new TestNode(0, 0, 200, 200);
    final TestNode zeroSizedNonLeafNode = new TestNode(0, 0, 0, 0);
    final TestNode leaf = new TestNode(0, 0, 200, 100);
    final TestNode leafTwo = new TestNode(0, 100, 200, 100);

    root.addChild(zeroSizedNonLeafNode);
    zeroSizedNonLeafNode.addChild(leaf);
    zeroSizedNonLeafNode.addChild(leafTwo);

    final int idLeaf = 24;
    final int idLeafTwo = 25;
    leaf.setRenderUnit(new TestRenderUnit(idLeaf));
    leafTwo.setRenderUnit(new TestRenderUnit(idLeafTwo));

    final Context c = RuntimeEnvironment.getApplication();
    final int widthSpec = View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY);
    final int heightSpec = View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY);
    final LayoutContext layoutContext = new LayoutContext(c, null, -1, new LayoutCache(), null);
    final LayoutResult result = root.calculateLayout(layoutContext, widthSpec, heightSpec);

    final RenderTree renderTree =
        Reducer.getReducedTree(c, result, widthSpec, heightSpec, RenderState.NO_ID, null);

    // The root node and the two leaf nodes should be present in the mountable output.
    Assertions.assertThat(renderTree.getMountableOutputCount()).isEqualTo(3);
    Assertions.assertThat(renderTree.getRenderTreeNodeAtIndex(1).getRenderUnit().getId())
        .isEqualTo(idLeaf);
    Assertions.assertThat(renderTree.getRenderTreeNodeAtIndex(2).getRenderUnit().getId())
        .isEqualTo(idLeafTwo);
  }
}
