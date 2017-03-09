// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import com.facebook.components.testing.testrunner.ComponentsTestRunner;
import com.facebook.components.testing.TestLayoutComponent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(ComponentsTestRunner.class)
public class LayoutStateSpecTest {

  private static final int COMPONENT_ID = 37;
  private static final int WIDTH_SPEC = SizeSpec.makeSizeSpec(39, SizeSpec.EXACTLY);
  private static final int HEIGHT_SPEC = SizeSpec.makeSizeSpec(41, SizeSpec.EXACTLY);

  private LayoutState mLayoutState;
  private Component<?> mComponent;
  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
    mComponent = TestLayoutComponent.create(mContext)
        .build();
    Whitebox.setInternalState(mComponent, "mId", COMPONENT_ID);

    mLayoutState = new LayoutState();
    Whitebox.setInternalState(mLayoutState, "mComponent", mComponent);
    Whitebox.setInternalState(mLayoutState, "mWidthSpec", WIDTH_SPEC);
    Whitebox.setInternalState(mLayoutState, "mHeightSpec", HEIGHT_SPEC);
  }

  @Test
  public void testCompatibleInputAndSpec() {
    assertTrue(mLayoutState.isCompatibleComponentAndSpec(COMPONENT_ID, WIDTH_SPEC, HEIGHT_SPEC));
  }

  @Test
  public void testIncompatibleInput() {
    assertFalse(mLayoutState.isCompatibleComponentAndSpec(
            COMPONENT_ID + 1000, WIDTH_SPEC, HEIGHT_SPEC));
  }

  @Test
  public void testIncompatibleWidthSpec() {
    assertFalse(mLayoutState.isCompatibleComponentAndSpec(
            COMPONENT_ID, WIDTH_SPEC + 1000, HEIGHT_SPEC));
  }

  @Test
  public void testIncompatibleHeightSpec() {
    assertFalse(mLayoutState.isCompatibleComponentAndSpec(
            COMPONENT_ID, WIDTH_SPEC, HEIGHT_SPEC + 1000));
  }
}
