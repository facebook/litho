// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components;

import android.graphics.Color;

import com.facebook.components.testing.testrunner.ComponentsTestRunner;
import com.facebook.components.testing.TestDrawableComponent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@RunWith(ComponentsTestRunner.class)
public class DuplicateParentStateTest {

  private static final int UNSPECIFIED_SIZE_SPEC = SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED);

  @Test
  public void testDuplicateParentStateAvoidedIfRedundant() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c)
            .duplicateParentState(true)
            .clickHandler(c.newEventHandler(1))
            .child(
                Container.create(c)
                    .duplicateParentState(false)
                    .child(
                        TestDrawableComponent.create(c)
                            .withLayout()
                            .duplicateParentState(true)))
            .child(
                Container.create(c)
                    .duplicateParentState(true)
                    .child(
                        TestDrawableComponent.create(c)
                            .withLayout()
                            .duplicateParentState(true)))
            .child(
                Container.create(c)
                    .clickHandler(c.newEventHandler(2))
                    .child(
                        TestDrawableComponent.create(c)
                            .withLayout()
                            .duplicateParentState(true)))
            .child(
                Container.create(c)
                    .clickHandler(c.newEventHandler(3))
                    .child(
                        TestDrawableComponent.create(c)
                            .withLayout()
                            .duplicateParentState(false)))
            .child(
                Container.create(c)
                    .clickHandler(c.newEventHandler(3))
                    .backgroundColor(Color.RED)
                    .foregroundColor(Color.RED))
            .child(
                Container.create(c)
                    .backgroundColor(Color.BLUE)
                    .foregroundColor(Color.BLUE))
            .build();
      }
    };

    LayoutState layoutState = LayoutState.calculate(
        new ComponentContext(RuntimeEnvironment.application),
        component,
        -1,
        UNSPECIFIED_SIZE_SPEC,
        UNSPECIFIED_SIZE_SPEC,
        false,
        null);

    assertEquals(12, layoutState.getMountableOutputCount());

    assertTrue(
        "Clickable root output has duplicate state",
        MountItem.isDuplicateParentState(layoutState.getMountableOutputAt(0).getFlags()));

    assertFalse(
        "Parent doesn't duplicate host state",
        MountItem.isDuplicateParentState(layoutState.getMountableOutputAt(1).getFlags()));

    assertTrue(
        "Parent does duplicate host state",
        MountItem.isDuplicateParentState(layoutState.getMountableOutputAt(2).getFlags()));

    assertTrue(
        "Drawable duplicates clickable parent state",
        MountItem.isDuplicateParentState(layoutState.getMountableOutputAt(4).getFlags()));

    assertFalse(
        "Drawable doesn't duplicate clickable parent state",
        MountItem.isDuplicateParentState(layoutState.getMountableOutputAt(6).getFlags()));

    assertTrue(
        "Background should duplicate clickable node state",
        MountItem.isDuplicateParentState(layoutState.getMountableOutputAt(8).getFlags()));
    assertTrue(
        "Foreground should duplicate clickable node state",
        MountItem.isDuplicateParentState(layoutState.getMountableOutputAt(9).getFlags()));

    assertFalse(
        "Background should duplicate non-clickable node state",
        MountItem.isDuplicateParentState(layoutState.getMountableOutputAt(10).getFlags()));
    assertFalse(
        "Foreground should duplicate non-clickable node state",
        MountItem.isDuplicateParentState(layoutState.getMountableOutputAt(11).getFlags()));
  }
}
