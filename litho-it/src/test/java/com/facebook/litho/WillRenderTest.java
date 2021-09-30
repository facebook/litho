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
import static com.facebook.litho.LifecycleStep.ON_ATTACHED;
import static com.facebook.litho.LifecycleStep.ON_CALCULATE_CACHED_VALUE;
import static com.facebook.litho.LifecycleStep.ON_CREATE_INITIAL_STATE;
import static com.facebook.litho.LifecycleStep.ON_CREATE_LAYOUT;
import static com.facebook.litho.LifecycleStep.ON_CREATE_TREE_PROP;
import static com.facebook.litho.LifecycleStep.getSteps;
import static com.facebook.litho.testing.assertj.LithoAssertions.assertThat;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.view.View;
import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.ComponentWithState;
import com.facebook.litho.widget.LayoutSpecWillRenderReuseTester;
import com.facebook.litho.widget.LayoutSpecWillRenderTester;
import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class WillRenderTest {

  public final @Rule LithoViewRule mLithoViewRule = new LithoViewRule();

  private final InlineLayoutSpec mNullSpec =
      new InlineLayoutSpec() {

        @Override
        protected Component onCreateLayout(ComponentContext c) {
          return null;
        }
      };

  private final InlineLayoutSpec mNonNullSpec =
      new InlineLayoutSpec() {

        @Override
        protected Component onCreateLayout(ComponentContext c) {
          return Row.create(c).build();
        }
      };

  private final InlineLayoutSpec mLayoutWithSizeSpec =
      new InlineLayoutSpec() {

        @Override
        protected Component onCreateLayoutWithSizeSpec(
            ComponentContext c, int widthSpec, int heightSpec) {
          return Row.create(c)
              .widthDip(View.MeasureSpec.getSize(widthSpec))
              .heightDip(View.MeasureSpec.getSize(heightSpec))
              .build();
        }

        @Override
        protected boolean canMeasure() {
          return true;
        }
      };

  @Rule public ExpectedException mExpectedException = ExpectedException.none();

  @Test
  public void testWillRenderForComponentThatReturnsNull() {
    final ComponentContext c = mLithoViewRule.getContext();
    mLithoViewRule.attachToWindow().setRoot(Wrapper.create(c).delegate(null)).layout().measure();
    assertThat(mLithoViewRule.getCommittedLayoutState().getLayoutRoot()).isNull();
  }

  @Test
  public void testWillRenderForComponentThatReturnsNonNull() {
    ComponentContext c = new ComponentContext(getApplicationContext());
    c.setLayoutStateContext(LayoutStateContext.getTestInstance(c));
    assertThat(c, Wrapper.create(c).delegate(mNonNullSpec).build()).willRender();
  }

  @Test
  public void testWillRenderForComponentWithSizeSpecThrowsException() {
    mExpectedException.expect(IllegalArgumentException.class);
    mExpectedException.expectMessage("@OnCreateLayoutWithSizeSpec");

    ComponentContext c = new ComponentContext(getApplicationContext());
    c.setLayoutStateContext(LayoutStateContext.getTestInstance(c));
    Component.willRender(c, Wrapper.create(c).delegate(mLayoutWithSizeSpec).build());
  }

  @Test
  public void testWillRender_withComponentContextWithoutStateHandler_doesntCrash() {
    ComponentContext c = new ComponentContext(getApplicationContext());
    c.setLayoutStateContext(LayoutStateContext.getTestInstance(c));
    assertThat(Component.willRender(c, ComponentWithState.create(c).build())).isTrue();
  }

  @Test
  public void testWillRender_cachedLayoutUsedInDifferentComponentHierarchy() {
    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final Component component =
        LayoutSpecWillRenderTester.create(mLithoViewRule.getContext()).steps(info).build();
    mLithoViewRule.setRoot(component);

    mLithoViewRule.attachToWindow().measure().layout();

    assertThat(getSteps(info))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            ON_CREATE_INITIAL_STATE,
            ON_CREATE_TREE_PROP,
            ON_CALCULATE_CACHED_VALUE,
            ON_CREATE_LAYOUT,
            ON_ATTACHED);
  }

  @Test
  public void testWillRender_cachedLayoutUsedInSameComponentHierarchy() {
    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final Component component =
        LayoutSpecWillRenderReuseTester.create(mLithoViewRule.getContext()).steps(info).build();
    mLithoViewRule.setRoot(component);

    mLithoViewRule.attachToWindow().measure().layout();

    assertThat(getSteps(info))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            ON_CREATE_INITIAL_STATE,
            ON_CREATE_TREE_PROP,
            ON_CALCULATE_CACHED_VALUE,
            ON_CREATE_LAYOUT,
            ON_ATTACHED);
  }
}
