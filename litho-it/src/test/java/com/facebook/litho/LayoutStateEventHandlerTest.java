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
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.testing.TestLayoutComponent;
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class LayoutStateEventHandlerTest {
  private int mUnspecifiedSizeSpec = 0; // SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED);

  private Component mRootComponent;
  private Component mNestedComponent;

  private static void assertCorrectEventHandler(
      EventHandler eventHandler, int expectedId, Component expectedInput) {
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
        new ComponentContext(getApplicationContext()),
        mRootComponent,
        -1,
        mUnspecifiedSizeSpec,
        mUnspecifiedSizeSpec,
        LayoutState.CalculateLayoutSource.TEST);
  }
}
