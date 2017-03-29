/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.components;

import android.content.Context;

import com.facebook.components.testing.testrunner.ComponentsTestRunner;
import com.facebook.components.testing.TestDrawableComponent;
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
        return Container.create(c)
            .child(
                Container.create(c)
                    .child(
                        TestDrawableComponent.create(c)
                            .withLayout()
                            .wrapInView()
                            .heightPx(50)))
            .child(
                TestDrawableComponent.create(c)
                    .withLayout()
                    .heightPx(20))
            .child(
                TestDrawableComponent.create(c)
                    .withLayout()
                    .positionType(YogaPositionType.ABSOLUTE)
                    .positionPx(YogaEdge.TOP, 10)
                    .positionPx(YogaEdge.BOTTOM, 30))
