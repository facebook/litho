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

import static android.view.View.MeasureSpec.AT_MOST;
import static android.view.View.MeasureSpec.makeMeasureSpec;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import com.facebook.rendercore.testing.RenderCoreTestRule;
import com.facebook.rendercore.testing.TestNode;
import com.facebook.rendercore.testing.TestRenderUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class RenderTreeHostViewTests {
  @Test
  public void testMountOnLayout() {
    final Context c = RuntimeEnvironment.application;
    final RenderTreeHostView renderTreeHostView = new RenderTreeHostView(c);

    final TestNode node = new TestNode();
    final RenderUnit renderUnit = new TestRenderUnit();
    final boolean didMount[] = new boolean[1];
    renderUnit.addOptionalMountBinder(
        RenderUnit.DelegateBinder.createDelegateBinder(
            null, new SimpleTestBinder(() -> didMount[0] = true)));
    node.setRenderUnit(renderUnit);

    RenderTree renderTree =
        RenderResult.render(
                c,
                new RenderCoreTestRule.IdentityResolveFunc(node),
                null,
                null,
                null,
                0,
                makeMeasureSpec(100, AT_MOST),
                makeMeasureSpec(100, AT_MOST))
            .getRenderTree();

    renderTreeHostView.setRenderTree(renderTree);
    renderTreeHostView.measure(makeMeasureSpec(100, AT_MOST), makeMeasureSpec(100, AT_MOST));
    renderTreeHostView.layout(
        0, 0, renderTreeHostView.getMeasuredWidth(), renderTreeHostView.getMeasuredHeight());

    assertThat(didMount[0]).isTrue();
  }

  @Test
  public void testDuplicateRenderUnitIdsInTreeCauseException() {
    final TestRenderUnit renderUnit1 = new TestRenderUnit();
    final TestRenderUnit renderUnit2 = new TestRenderUnit();

    // Setup tree with 2 render-units with the same ID.
    renderUnit1.setId(1);
    renderUnit2.setId(1);

    RenderTreeNode node1 = new RenderTreeNode(null, renderUnit1, null, new Rect(), null, 0);
    RenderTreeNode node2 = new RenderTreeNode(null, renderUnit2, null, new Rect(), null, 1);

    boolean exceptionOccurred = false;

    try {
      // RenderTree ctor should detect duplicate RU ids and throw an illegal state exception here.
      new RenderTree(
          node1, new RenderTreeNode[] {node1, node2}, 0, 0, RenderState.NO_ID, null, null);
    } catch (IllegalStateException e) {
      // Exception occurred as expected, raise flag indicate valid state for assert.
      exceptionOccurred = true;
    }

    assertThat(exceptionOccurred).isTrue();
  }

  @Test
  public void testMeasureReturnsRenderTreeSize() {
    final Context c = RuntimeEnvironment.application;
    final RenderTreeHostView renderTreeHostView = new RenderTreeHostView(c);

    final TestNode node = new TestNode(0, 0, 99, 99);
    final RenderUnit renderUnit = new TestRenderUnit();
    node.setRenderUnit(renderUnit);

    RenderTree renderTree =
        RenderResult.render(
                c,
                new RenderCoreTestRule.IdentityResolveFunc(node),
                null,
                null,
                null,
                0,
                makeMeasureSpec(100, AT_MOST),
                makeMeasureSpec(100, AT_MOST))
            .getRenderTree();

    renderTreeHostView.setRenderTree(renderTree);
    renderTreeHostView.measure(
        makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
        makeMeasureSpec(200, View.MeasureSpec.EXACTLY));
    assertThat(renderTreeHostView.getMeasuredWidth()).isEqualTo(99);
    assertThat(renderTreeHostView.getMeasuredHeight()).isEqualTo(99);
  }

  @Test
  public void testNestedMount() {
    final Context c = RuntimeEnvironment.application;
    final RenderTreeHostView renderTreeHostView = new RenderTreeHostView(c);

    final boolean didMount[] = new boolean[1];
    final TestNode nestedMountTestNode = new TestNode();
    final RenderUnit nestedMountRenderUnit = new TestRenderUnit();
    nestedMountRenderUnit.addOptionalMountBinder(
        RenderUnit.DelegateBinder.createDelegateBinder(
            null, new SimpleTestBinder(() -> didMount[0] = true)));
    nestedMountTestNode.setRenderUnit(nestedMountRenderUnit);

    final TestNode node = new TestNode();
    final RenderUnit renderUnit = new TestRenderUnit();
    renderUnit.addOptionalMountBinder(
        RenderUnit.DelegateBinder.createDelegateBinder(
            null,
            new SimpleTestBinder(
                () ->
                    renderTreeHostView.setRenderTree(
                        RenderResult.render(
                                c,
                                new RenderCoreTestRule.IdentityResolveFunc(nestedMountTestNode),
                                null,
                                null,
                                null,
                                0,
                                makeMeasureSpec(100, AT_MOST),
                                makeMeasureSpec(100, AT_MOST))
                            .getRenderTree()))));
    node.setRenderUnit(renderUnit);

    RenderTree renderTree =
        RenderResult.render(
                c,
                new RenderCoreTestRule.IdentityResolveFunc(node),
                null,
                null,
                null,
                0,
                makeMeasureSpec(100, AT_MOST),
                makeMeasureSpec(100, AT_MOST))
            .getRenderTree();
    renderTreeHostView.setRenderTree(renderTree);

    renderTreeHostView.measure(
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST),
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST));

    assertThat(didMount[0]).isFalse();

    renderTreeHostView.layout(
        0, 0, renderTreeHostView.getMeasuredWidth(), renderTreeHostView.getMeasuredHeight());

    assertThat(didMount[0]).isTrue();
  }
}
