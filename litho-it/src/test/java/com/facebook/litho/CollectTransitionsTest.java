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

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.robolectric.RuntimeEnvironment.application;

import android.view.View;
import com.facebook.litho.animation.AnimatedProperties;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class CollectTransitionsTest {

  private final InlineLayoutSpec mWrappingContentWithTransition =
      new InlineLayoutSpec() {

        @Override
        protected Component onCreateLayout(ComponentContext c) {
          return Wrapper.create(c).delegate(mComponentWithTransition).build();
        }

        @Override
        protected Transition onCreateTransition(ComponentContext c) {
          return Transition.create(Transition.TransitionKeyType.GLOBAL, "test").animate(AnimatedProperties.Y);
        }
      };

  private final InlineLayoutSpec mComponentWithTransition =
      new InlineLayoutSpec() {

        @Override
        protected Component onCreateLayout(ComponentContext c) {
          return Row.create(c)
              .child(Row.create(c).transitionKey("test"))
.transitionKeyType(Transition.TransitionKeyType.GLOBAL)
              .child(Row.create(c).transitionKey("test2"))
.transitionKeyType(Transition.TransitionKeyType.GLOBAL)
              .build();
        }

        @Override
        protected Transition onCreateTransition(ComponentContext c) {
          return Transition.create(Transition.TransitionKeyType.GLOBAL, "test").animate(AnimatedProperties.X);
        }
      };

  @Rule public ExpectedException mExpectedException = ExpectedException.none();

  @Test
  public void testCollectsWrappingTransitions() {
    ComponentContext c = new ComponentContext(application);
    LayoutState layoutState =
        LayoutState.calculate(
            c,
            mWrappingContentWithTransition,
            ComponentTree.generateComponentTreeId(),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            LayoutState.CalculateLayoutSource.TEST);
    assertThat(layoutState.getTransitions()).hasSize(2);
  }
}
