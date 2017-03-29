/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components;

import android.graphics.Color;

import com.facebook.components.testing.testrunner.ComponentsTestRunner;
import com.facebook.components.testing.TestDrawableComponent;

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
        mUnspecifiedSizeSpec,
        mUnspecifiedSizeSpec,
        false,
        null);
