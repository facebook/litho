/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import static com.facebook.litho.testing.assertj.LithoAssertions.assertThat;
import static org.robolectric.RuntimeEnvironment.application;

import android.view.View;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class UniqueTransitionKeysTest {

  private static final InlineLayoutSpec HAS_NON_UNIQUE_TRANSITION_KEYS =
      new InlineLayoutSpec() {

        @Override
        protected ComponentLayout onCreateLayout(ComponentContext c) {
          return Row.create(c)
              .child(Row.create(c).transitionKey("test"))
              .child(Row.create(c).transitionKey("test"))
              .build();
        }
      };

  private static final InlineLayoutSpec HAS_UNIQUE_TRANSITION_KEYS =
      new InlineLayoutSpec() {

        @Override
        protected ComponentLayout onCreateLayout(ComponentContext c) {
          return Row.create(c)
              .child(Row.create(c).transitionKey("test"))
              .child(Row.create(c).transitionKey("test2"))
              .build();
        }
      };

  @Rule public ExpectedException mExpectedException = ExpectedException.none();

  @Test
  public void testGetTransitionKeyMapping() {
    ComponentContext c = new ComponentContext(application);
    LayoutState layoutState =
        LayoutState.calculate(
            c,
            HAS_UNIQUE_TRANSITION_KEYS,
            ComponentTree.generateComponentTreeId(),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY));
    layoutState.getTransitionKeyMapping();
  }

  @Test
  public void testThrowIfSameTransitionKeyAppearsMultipleTimes() {
    mExpectedException.expect(RuntimeException.class);
    mExpectedException.expectMessage("The transitionKey 'test' was defined multiple times");

    ComponentContext c = new ComponentContext(application);
    LayoutState layoutState =
        LayoutState.calculate(
            c,
            HAS_NON_UNIQUE_TRANSITION_KEYS,
            ComponentTree.generateComponentTreeId(),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY));
    assertThat(layoutState.getTransitionKeyMapping()).isNotNull();
  }
}
