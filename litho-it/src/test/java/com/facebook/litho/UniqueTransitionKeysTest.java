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
import static com.facebook.litho.testing.assertj.LithoAssertions.assertThat;

import android.view.View;
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class UniqueTransitionKeysTest {

  private final InlineLayoutSpec mHasNonUniqueTransitionKeys =
      new InlineLayoutSpec() {

        @Override
        protected Component onCreateLayout(ComponentContext c) {
          return Row.create(c)
              .child(
                  Row.create(c)
                      .transitionKey("test")
                      .transitionKeyType(Transition.TransitionKeyType.GLOBAL))
              .child(
                  Row.create(c)
                      .transitionKey("test")
                      .transitionKeyType(Transition.TransitionKeyType.GLOBAL))
              .build();
        }
      };

  private final InlineLayoutSpec mHasUniqueTransitionKeys =
      new InlineLayoutSpec() {

        @Override
        protected Component onCreateLayout(ComponentContext c) {
          return Row.create(c)
              .child(
                  Row.create(c)
                      .transitionKey("test")
                      .transitionKeyType(Transition.TransitionKeyType.GLOBAL))
              .child(
                  Row.create(c)
                      .transitionKey("test2")
                      .transitionKeyType(Transition.TransitionKeyType.GLOBAL))
              .build();
        }
      };

  @Rule public ExpectedException mExpectedException = ExpectedException.none();

  @Test
  public void testGetTransitionKeyMapping() {
    ComponentContext c = new ComponentContext(getApplicationContext());
    LayoutState layoutState =
        LayoutState.calculate(
            c,
            mHasUniqueTransitionKeys,
            ComponentTree.generateComponentTreeId(),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            LayoutState.CalculateLayoutSource.TEST);
    layoutState.getTransitionIdMapping();
  }

  @Test
  public void testThrowIfSameTransitionKeyAppearsMultipleTimes() {
    mExpectedException.expect(RuntimeException.class);
    mExpectedException.expectMessage(
        "The transitionId 'TransitionId{\"test\", GLOBAL}' is defined multiple times in the same layout.");

    ComponentContext c = new ComponentContext(getApplicationContext());
    LayoutState layoutState =
        LayoutState.calculate(
            c,
            mHasNonUniqueTransitionKeys,
            ComponentTree.generateComponentTreeId(),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            LayoutState.CalculateLayoutSource.TEST);
    assertThat(layoutState.getTransitionIdMapping()).isNotNull();
  }
}
