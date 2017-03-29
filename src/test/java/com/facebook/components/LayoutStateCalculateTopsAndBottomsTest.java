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

import android.content.Context;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaPositionType;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

@RunWith(ComponentsTestRunner.class)
public class LayoutStateCalculateTopsAndBottomsTest {

  @Test
  public void testCalculateTopsAndBottoms() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .child(
                        TestDrawableComponent.create(c)
                            .withLayout().flexShrink(0)
                            .wrapInView()
                            .heightPx(50)))
            .child(
                TestDrawableComponent.create(c)
                    .withLayout().flexShrink(0)
                    .heightPx(20))
            .child(
                TestDrawableComponent.create(c)
