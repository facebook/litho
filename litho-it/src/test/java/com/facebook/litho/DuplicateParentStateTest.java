/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import static android.graphics.Color.BLUE;
import static android.graphics.Color.RED;
import static com.facebook.litho.Column.create;
import static com.facebook.litho.LayoutState.calculate;
import static com.facebook.litho.MountItem.isDuplicateParentState;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.robolectric.RuntimeEnvironment.application;

import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class DuplicateParentStateTest {

  private int mUnspecifiedSizeSpec;

  @Before
  public void setUp() throws Exception {
    mUnspecifiedSizeSpec = SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED);
  }

  @Test
  public void testDuplicateParentStateAvoidedIfRedundant() {
    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(ComponentContext c) {
            return create(c)
                .duplicateParentState(true)
                .clickHandler(c.newEventHandler(1))
                .child(
                    create(c)
                        .duplicateParentState(false)
                        .child(TestDrawableComponent.create(c).duplicateParentState(true)))
                .child(
                    create(c)
                        .duplicateParentState(true)
                        .child(TestDrawableComponent.create(c).duplicateParentState(true)))
                .child(
                    create(c)
                        .clickHandler(c.newEventHandler(2))
                        .child(TestDrawableComponent.create(c).duplicateParentState(true)))
                .child(
                    create(c)
                        .clickHandler(c.newEventHandler(3))
                        .child(TestDrawableComponent.create(c).duplicateParentState(false)))
                .child(
                    create(c)
                        .clickHandler(c.newEventHandler(3))
                        .backgroundColor(RED)
                        .foregroundColor(RED))
                .child(create(c).backgroundColor(BLUE).foregroundColor(BLUE))
                .build();
          }
        };

    LayoutState layoutState =
        calculate(
            new ComponentContext(application),
            component,
            -1,
            mUnspecifiedSizeSpec,
            mUnspecifiedSizeSpec,
            LayoutState.CalculateLayoutSource.TEST);

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(12);

    assertTrue(
        "Clickable root output has duplicate state",
        isDuplicateParentState(layoutState.getMountableOutputAt(0).getFlags()));

    assertFalse(
        "Parent doesn't duplicate host state",
        isDuplicateParentState(layoutState.getMountableOutputAt(1).getFlags()));

    assertTrue(
        "Parent does duplicate host state",
        isDuplicateParentState(layoutState.getMountableOutputAt(2).getFlags()));

    assertTrue(
        "Drawable duplicates clickable parent state",
        isDuplicateParentState(layoutState.getMountableOutputAt(4).getFlags()));

    assertFalse(
        "Drawable doesn't duplicate clickable parent state",
        isDuplicateParentState(layoutState.getMountableOutputAt(6).getFlags()));

    assertTrue(
        "Background should duplicate clickable node state",
        isDuplicateParentState(layoutState.getMountableOutputAt(8).getFlags()));
    assertTrue(
        "Foreground should duplicate clickable node state",
        isDuplicateParentState(layoutState.getMountableOutputAt(9).getFlags()));

    assertFalse(
        "Background should duplicate non-clickable node state",
        isDuplicateParentState(layoutState.getMountableOutputAt(10).getFlags()));
    assertFalse(
        "Foreground should duplicate non-clickable node state",
        isDuplicateParentState(layoutState.getMountableOutputAt(11).getFlags()));
  }
}
