/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

import static com.facebook.litho.testing.TestDrawableComponent.create;
import static com.facebook.litho.testing.helper.ComponentTestHelper.mountComponent;
import static com.facebook.yoga.YogaAlign.FLEX_END;
import static com.facebook.yoga.YogaEdge.ALL;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.graphics.Rect;
import android.view.View;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.TestViewComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaJustify;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class MountStateBoundsTest {

  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
  }

  @Test
  public void testMountedDrawableBounds() {
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return create(c).widthPx(10).heightPx(10).build();
              }
            });

    assertThat(lithoView.getDrawables().get(0).getBounds()).isEqualTo(new Rect(0, 0, 10, 10));
  }

  @Test
  public void testMountedViewBounds() {
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return TestViewComponent.create(c).widthPx(10).heightPx(10).build();
              }
            });

    final View mountedView = lithoView.getChildAt(0);
    assertThat(new Rect(
        mountedView.getLeft(),
        mountedView.getTop(),
        mountedView.getRight(),
        mountedView.getBottom())).isEqualTo(new Rect(0, 0, 10, 10));
  }

  @Test
  public void testInnerComponentHostBounds() {
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .child(
                        Column.create(c)
                            .widthPx(20)
                            .heightPx(20)
                            .wrapInView()
                            .child(create(c).widthPx(10).heightPx(10)))
                    .build();
              }
            });

    final ComponentHost host = (ComponentHost) lithoView.getChildAt(0);
    assertThat(host.getDrawables().get(0).getBounds()).isEqualTo(new Rect(0, 0, 10, 10));
    assertThat(new Rect(
        host.getLeft(),
        host.getTop(),
        host.getRight(),
        host.getBottom())).isEqualTo(new Rect(0, 0, 20, 20));
  }

  @Test
  public void testDoubleInnerComponentHostBounds() {
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .alignItems(FLEX_END)
                    .justifyContent(YogaJustify.FLEX_END)
                    .child(
                        Column.create(c)
                            .widthPx(100)
                            .heightPx(100)
                            .paddingPx(ALL, 20)
                            .wrapInView()
                            .child(
                                Column.create(c)
                                    .widthPx(60)
                                    .heightPx(60)
                                    .wrapInView()
                                    .child(create(c).widthPx(20).heightPx(20).marginPx(ALL, 20))))
                    .build();
              }
            },
            200,
            200);

    final ComponentHost host = (ComponentHost) lithoView.getChildAt(0);
    final ComponentHost nestedHost = (ComponentHost) host.getChildAt(0);

    assertThat(new Rect(
        host.getLeft(),
        host.getTop(),
        host.getRight(),
        host.getBottom())).isEqualTo(new Rect(100, 100, 200, 200));

    assertThat(nestedHost.getDrawables().get(0).getBounds()).isEqualTo(new Rect(20, 20, 40, 40));

    assertThat(new Rect(
        nestedHost.getLeft(),
        nestedHost.getTop(),
        nestedHost.getRight(),
        nestedHost.getBottom())).isEqualTo(new Rect(20, 20, 80, 80));
  }

  @Test
  public void testSkippingPhantomHosts() {
    ComponentsConfiguration.createPhantomLayoutOutputsForTransitions = true;

    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .justifyContent(YogaJustify.CENTER)
                    .alignItems(YogaAlign.CENTER)
                    .child(
                        Row.create(c)
                            .widthPx(20)
                            .heightPx(20)
                            .transitionKey("row")
                            .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
                            .justifyContent(YogaJustify.CENTER)
                            .alignItems(YogaAlign.CENTER)
                            .child(create(c).widthPx(10).heightPx(10)))
                    .build();
              }
            });

    // Check that a ComponentHost for the LayoutOutput with the transition key was not mounted
    assertThat(lithoView.getChildCount()).isZero();
    // Checking that the drawable is bounded directly into the LithoView, and correctly placed right
    // at the center
    assertThat(lithoView.getDrawables().get(0).getBounds()).isEqualTo(new Rect(45, 45, 55, 55));

    ComponentsConfiguration.createPhantomLayoutOutputsForTransitions = false;
  }
}
