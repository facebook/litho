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

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.facebook.litho.Column.create;
import static com.facebook.litho.testing.helper.ComponentTestHelper.mountComponent;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.view.ViewGroup;
import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.TestViewComponent;
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.SimpleLayoutSpecWithClickHandlersTester;
import com.facebook.litho.widget.SimpleMountSpecTester;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class MountStateViewClickTest {

  private ComponentContext mContext;
  public final @Rule LithoViewRule mLithoViewRule = new LithoViewRule();

  @Before
  public void setup() {
    mContext = new ComponentContext(getApplicationContext());
  }

  @Test
  public void testInnerComponentHostClickable() {
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return create(c)
                    .child(
                        create(c)
                            .clickHandler(c.newEventHandler(1))
                            .child(TestViewComponent.create(c)))
                    .build();
              }
            });

    assertThat(lithoView.getChildCount()).isEqualTo(1);
    assertThat(lithoView.isClickable()).isFalse();

    ComponentHost innerHost = (ComponentHost) lithoView.getChildAt(0);
    assertThat(innerHost.isClickable()).isTrue();
  }

  @Test
  public void testInnerComponentHostClickableWithLongClickHandler() {
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return create(c)
                    .child(
                        create(c)
                            .longClickHandler(c.newEventHandler(1))
                            .child(TestViewComponent.create(c)))
                    .build();
              }
            });

    assertThat(lithoView.getChildCount()).isEqualTo(1);
    assertThat(lithoView.isClickable()).isFalse();

    ComponentHost innerHost = (ComponentHost) lithoView.getChildAt(0);
    assertThat(innerHost.isLongClickable()).isTrue();
  }

  @Test
  public void testRootHostClickable() {
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return create(c)
                    .clickHandler(c.newEventHandler(1))
                    .child(SimpleMountSpecTester.create(c))
                    .build();
              }
            });

    assertThat(lithoView.getChildCount()).isEqualTo(0);
    assertThat(lithoView.isClickable()).isTrue();
  }

  @Test
  public void testRootHostClickableWithLongClickHandler() {
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return create(c)
                    .longClickHandler(c.newEventHandler(1))
                    .child(SimpleMountSpecTester.create(c))
                    .build();
              }
            });

    assertThat(lithoView.getChildCount()).isEqualTo(0);
    assertThat(lithoView.isLongClickable()).isTrue();
  }

  @Test
  public void testRootHostClickableUnmount() {
    final Component component =
        SimpleLayoutSpecWithClickHandlersTester.create(mLithoViewRule.getContext()).build();

    mLithoViewRule.setRoot(component);

    final ViewGroup parent =
        new ViewGroup(mLithoViewRule.getLithoView().getContext()) {
          @Override
          protected void onLayout(boolean changed, int l, int t, int r, int b) {}
        };

    parent.addView(mLithoViewRule.getLithoView());

    mLithoViewRule
        .getLithoView()
        .setComponentTree(
            ComponentTree.create(mLithoViewRule.getContext(), component)
                .incrementalMount(false)
                .layoutDiffing(false)
                .visibilityProcessing(false)
                .build());

    mLithoViewRule.attachToWindow().measure().layout();

    assertThat(mLithoViewRule.getLithoView().isClickable()).isTrue();
    assertThat(mLithoViewRule.getLithoView().isLongClickable()).isTrue();

    mLithoViewRule.getLithoView().unmountAllItems();

    assertThat(mLithoViewRule.getLithoView().isClickable()).isFalse();
    assertThat(mLithoViewRule.getLithoView().isLongClickable()).isFalse();
  }
}
