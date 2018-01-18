/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.testing.TestLayoutComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class LayoutStateEventHandlerTest {
  private int mUnspecifiedSizeSpec = 0; //SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED);

  private Component mRootComponent;
  private Component mNestedComponent;

  private static void assertCorrectEventHandler(
      EventHandler eventHandler,
      int expectedId,
      Component expectedInput) {
    assertThat(eventHandler.mHasEventDispatcher).isEqualTo(expectedInput);
    assertThat(eventHandler.id).isEqualTo(expectedId);
  }

  @Before
  public void setup() {
    mUnspecifiedSizeSpec = SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED);
    mRootComponent =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            assertCorrectEventHandler(c.newEventHandler(1), 1, mRootComponent);
            Wrapper.create(c).delegate(mNestedComponent).build();
            assertCorrectEventHandler(c.newEventHandler(2), 2, mRootComponent);
            Wrapper.create(c).delegate(mNestedComponent).build();
            assertCorrectEventHandler(c.newEventHandler(3), 3, mRootComponent);

            return TestLayoutComponent.create(c).build();
          }
        };
    mNestedComponent =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            assertCorrectEventHandler(c.newEventHandler(1), 1, mNestedComponent);

            return TestLayoutComponent.create(c).build();
          }
        };
  }

  @Test
  public void testNestedEventHandlerInput() {
    LayoutState.calculate(
        new ComponentContext(RuntimeEnvironment.application),
        mRootComponent,
        -1,
        mUnspecifiedSizeSpec,
        mUnspecifiedSizeSpec,
        LayoutState.CalculateLayoutSource.TEST);
  }
}
