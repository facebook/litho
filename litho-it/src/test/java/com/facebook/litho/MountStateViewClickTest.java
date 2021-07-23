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

import android.view.View;
import android.view.ViewGroup;
import com.facebook.litho.config.TempComponentsConfigurations;
import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.LayoutWithInnerClickableChildTester;
import com.facebook.litho.widget.SimpleLayoutSpecWithClickHandlersTester;
import com.facebook.litho.widget.SimpleMountSpecTester;
import org.junit.After;
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
    TempComponentsConfigurations.setShouldAddHostViewForRootComponent(true);
    mContext = new ComponentContext(getApplicationContext());
  }

  @Test
  public void testInnerComponentHostClickable() {
    final Component component =
        LayoutWithInnerClickableChildTester.create(mContext).shouldSetClickHandler(true).build();

    mLithoViewRule.setRoot(component);
    final LithoView lithoView = mLithoViewRule.getLithoView();

    setupLithoViewParentAndComponentTree(lithoView, component);

    mLithoViewRule.attachToWindow().measure().layout();

    assertThat(lithoView.getChildCount()).isEqualTo(1);
    assertThat(lithoView.isClickable()).isFalse();
    assertThat(lithoView.isLongClickable()).isFalse();

    ComponentHost innerHost = (ComponentHost) lithoView.getChildAt(0);
    assertThat(innerHost.isClickable()).isTrue();
    assertThat(innerHost.isLongClickable()).isFalse();
  }

  @Test
  public void testInnerComponentHostClickableWithLongClickHandler() {
    final Component component =
        LayoutWithInnerClickableChildTester.create(mContext)
            .shouldSetLongClickHandler(true)
            .build();

    mLithoViewRule.setRoot(component);
    final LithoView lithoView = mLithoViewRule.getLithoView();

    setupLithoViewParentAndComponentTree(lithoView, component);

    mLithoViewRule.attachToWindow().measure().layout();

    assertThat(lithoView.getChildCount()).isEqualTo(1);
    assertThat(lithoView.isClickable()).isFalse();
    assertThat(lithoView.isLongClickable()).isFalse();

    ComponentHost innerHost = (ComponentHost) lithoView.getChildAt(0);
    assertThat(innerHost.isClickable()).isFalse();
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

    assertThat(lithoView.getChildCount()).isEqualTo(1);
    assertThat(lithoView.getChildAt(0).isClickable()).isTrue();
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

    assertThat(lithoView.getChildCount()).isEqualTo(1);
    assertThat(lithoView.getChildAt(0).isLongClickable()).isTrue();
  }

  @Test
  public void testRootHostClickableUnmount() {
    final Component component =
        SimpleLayoutSpecWithClickHandlersTester.create(mLithoViewRule.getContext()).build();

    mLithoViewRule.setRoot(component);

    setupLithoViewParentAndComponentTree(mLithoViewRule.getLithoView(), component);

    mLithoViewRule.attachToWindow().measure().layout();

    final View rootHost = mLithoViewRule.getLithoView().getChildAt(0);

    assertThat(rootHost.isClickable()).isTrue();
    assertThat(rootHost.isLongClickable()).isTrue();

    mLithoViewRule.getLithoView().unmountAllItems();

    assertThat(rootHost.isClickable()).isFalse();
    assertThat(rootHost.isLongClickable()).isFalse();
  }

  // When testing a LithoView via a LithoViewRule - we must set the parent and component tree
  // prior to attach / measure / layout for that LithoView otherwise mounting will not behave
  // properly.
  private void setupLithoViewParentAndComponentTree(LithoView lithoView, Component component) {
    final ViewGroup parent =
        new ViewGroup(lithoView.getContext()) {
          @Override
          protected void onLayout(boolean changed, int l, int t, int r, int b) {}
        };

    parent.addView(lithoView);

    lithoView.setComponentTree(
        ComponentTree.create(mContext, component)
            .incrementalMount(false)
            .layoutDiffing(false)
            .visibilityProcessing(false)
            .build());
  }

  @After
  public void restoreConfiguration() {
    TempComponentsConfigurations.restoreShouldAddHostViewForRootComponent();
  }
}
