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

import static org.assertj.core.api.Java6Assertions.assertThat;

import android.content.Context;
import android.view.View;
import com.facebook.rendercore.testing.RenderCoreTestRule;
import com.facebook.rendercore.testing.TestNode;
import com.facebook.rendercore.testing.TestRenderUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class RootHostViewTests {

  @Test
  public void testMountOnLayout() {
    final Context c = RuntimeEnvironment.application;

    final TestNode node = new TestNode();
    final RenderUnit renderUnit = new TestRenderUnit();
    final boolean didMount[] = new boolean[1];
    renderUnit.addOptionalMountBinder(
        RenderUnit.DelegateBinder.createDelegateBinder(
            null, new SimpleTestBinder(() -> didMount[0] = true)));
    node.setRenderUnit(renderUnit);
    RootHostView rootHostView = new RootHostView(c);
    final RenderState renderState =
        new RenderState(
            c,
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
            },
            null,
            null);

    rootHostView.setRenderState(renderState);
    renderState.setTree(new RenderCoreTestRule.IdentityResolveFunc(node));

    rootHostView.measure(
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST),
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST));

    assertThat(didMount[0]).isFalse();

    rootHostView.layout(0, 0, rootHostView.getMeasuredWidth(), rootHostView.getMeasuredHeight());

    assertThat(didMount[0]).isTrue();
  }

  @Test
  public void testNestedMount() {
    final Context c = RuntimeEnvironment.application;
    final RenderState renderState =
        new RenderState(
            c,
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
            },
            null,
            null);

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
                    renderState.setTree(
                        new RenderCoreTestRule.IdentityResolveFunc(nestedMountTestNode)))));
    node.setRenderUnit(renderUnit);

    RootHostView rootHostView = new RootHostView(c);

    rootHostView.setRenderState(renderState);
    renderState.setTree(new RenderCoreTestRule.IdentityResolveFunc(node));

    rootHostView.measure(
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST),
        View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.AT_MOST));

    assertThat(didMount[0]).isFalse();

    rootHostView.layout(0, 0, rootHostView.getMeasuredWidth(), rootHostView.getMeasuredHeight());

    assertThat(didMount[0]).isTrue();
  }
}
