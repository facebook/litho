/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import com.facebook.yoga.YogaAlign;

import com.facebook.yoga.YogaFlexDirection;

import android.graphics.Color;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.TestDrawableComponent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@RunWith(ComponentsTestRunner.class)
public class DuplicateParentStateTest {

  private int mUnspecifiedSizeSpec;

  @Before
  public void setUp() throws Exception {
    mUnspecifiedSizeSpec = SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED);
  }

  @Test
  public void testDuplicateParentStateAvoidedIfRedundant() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .duplicateParentState(true)
            .clickHandler(c.newEventHandler(1))
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .duplicateParentState(false)
                    .child(
                        TestDrawableComponent.create(c)
                            .withLayout().flexShrink(0)
                            .duplicateParentState(true)))
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .duplicateParentState(true)
                    .child(
                        TestDrawableComponent.create(c)
                            .withLayout().flexShrink(0)
                            .duplicateParentState(true)))
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .clickHandler(c.newEventHandler(2))
                    .child(
                        TestDrawableComponent.create(c)
                            .withLayout().flexShrink(0)
                            .duplicateParentState(true)))
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .clickHandler(c.newEventHandler(3))
                    .child(
                        TestDrawableComponent.create(c)
                            .withLayout().flexShrink(0)
                            .duplicateParentState(false)))
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .clickHandler(c.newEventHandler(3))
                    .backgroundColor(Color.RED)
                    .foregroundColor(Color.RED))
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .backgroundColor(Color.BLUE)
                    .foregroundColor(Color.BLUE))
            .build();
      }
    };

    LayoutState layoutState = LayoutState.calculate(
        new ComponentContext(RuntimeEnvironment.application),
        component,
        -1,
        mUnspecifiedSizeSpec,
        mUnspecifiedSizeSpec,
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
