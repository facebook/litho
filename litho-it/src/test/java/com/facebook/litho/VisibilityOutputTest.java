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

import static org.assertj.core.api.Java6Assertions.assertThat;

import android.graphics.Rect;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class VisibilityOutputTest {

  private static final int LIFECYCLE_TEST_ID = 1;

  private Component mComponent;
  private VisibilityOutput mVisibilityOutput;

  @Before
  public void setup() {
    mVisibilityOutput = new VisibilityOutput();

    mComponent =
        new Component("TestComponent") {
          @Override
          int getTypeId() {
            return LIFECYCLE_TEST_ID;
          }

          @Override
          String getGlobalKey() {
            return "testKey";
          }
        };
  }

  @Test
  public void testPositionAndSizeSet() {
    mVisibilityOutput.setBounds(0, 1, 3, 4);
    assertThat(mVisibilityOutput.getBounds().left).isEqualTo(0);
    assertThat(mVisibilityOutput.getBounds().top).isEqualTo(1);
    assertThat(mVisibilityOutput.getBounds().right).isEqualTo(3);
    assertThat(mVisibilityOutput.getBounds().bottom).isEqualTo(4);
  }

  @Test
  public void testRectBoundsSet() {
    Rect bounds = new Rect(0, 1, 3, 4);
    mVisibilityOutput.setBounds(bounds);
    assertThat(mVisibilityOutput.getBounds().left).isEqualTo(0);
    assertThat(mVisibilityOutput.getBounds().top).isEqualTo(1);
    assertThat(mVisibilityOutput.getBounds().right).isEqualTo(3);
    assertThat(mVisibilityOutput.getBounds().bottom).isEqualTo(4);
  }

  @Test
  public void testHandlersSet() {
    EventHandler visibleHandler = new EventHandler(null, 1);
    EventHandler invisibleHandler = new EventHandler(null, 2);
    EventHandler focusedHandler = new EventHandler(null, 3);
    EventHandler unfocusedHandler = new EventHandler(null, 4);
    EventHandler fullImpressionHandler = new EventHandler(null, 5);

    mVisibilityOutput.setVisibleEventHandler(visibleHandler);
    mVisibilityOutput.setInvisibleEventHandler(invisibleHandler);
    mVisibilityOutput.setFocusedEventHandler(focusedHandler);
    mVisibilityOutput.setUnfocusedEventHandler(unfocusedHandler);
    mVisibilityOutput.setFullImpressionEventHandler(fullImpressionHandler);
    assertThat(visibleHandler).isSameAs(mVisibilityOutput.getVisibleEventHandler());
    assertThat(invisibleHandler).isSameAs(mVisibilityOutput.getInvisibleEventHandler());
    assertThat(focusedHandler).isSameAs(mVisibilityOutput.getFocusedEventHandler());
    assertThat(unfocusedHandler).isSameAs(mVisibilityOutput.getUnfocusedEventHandler());
    assertThat(fullImpressionHandler).isSameAs(mVisibilityOutput.getFullImpressionEventHandler());
  }

  @Test
  public void testId() {
    mVisibilityOutput.setComponent(mComponent);
    assertThat(mVisibilityOutput.getId()).isEqualTo(mComponent.getGlobalKey());
  }
}
