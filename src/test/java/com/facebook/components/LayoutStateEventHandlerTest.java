// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import com.facebook.components.testing.testrunner.ComponentsTestRunner;
import com.facebook.components.testing.TestLayoutComponent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static junit.framework.Assert.assertEquals;

@RunWith(ComponentsTestRunner.class)
public class LayoutStateEventHandlerTest {
  private static final int UNSPECIFIED_SIZE_SPEC = SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED);

  private Component mRootComponent;
  private Component mNestedComponent;

  private static void assertCorrectEventHandler(
      EventHandler eventHandler,
      int expectedId,
      Component<?> expectedInput) {
    assertEquals(expectedInput, eventHandler.mHasEventDispatcher);
    assertEquals(expectedId, eventHandler.id);
  }

  @Before
  public void setup() {
    mRootComponent = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        assertCorrectEventHandler(c.newEventHandler(1), 1, mRootComponent);
        Layout.create(c, mNestedComponent).build();
        assertCorrectEventHandler(c.newEventHandler(2), 2, mRootComponent);
        Layout.create(c, mNestedComponent).build();
        assertCorrectEventHandler(c.newEventHandler(3), 3, mRootComponent);

        return TestLayoutComponent.create(c)
            .buildWithLayout();
      }
    };
    mNestedComponent = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        assertCorrectEventHandler(c.newEventHandler(1), 1, mNestedComponent);

        return TestLayoutComponent.create(c)
            .buildWithLayout();
      }
    };
  }

  @Test
  public void testNestedEventHandlerInput() {
    LayoutState.calculate(
        new ComponentContext(RuntimeEnvironment.application),
        mRootComponent,
        -1,
        UNSPECIFIED_SIZE_SPEC,
        UNSPECIFIED_SIZE_SPEC,
        false,
        null);
  }
}
