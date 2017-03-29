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
import com.facebook.litho.testing.TestLayoutComponent;
import com.facebook.litho.testing.TestNullLayoutComponent;
import com.facebook.yoga.YogaEdge;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;

@PrepareForTest({Component.class})
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*"})
@RunWith(ComponentsTestRunner.class)
public class LayoutStateCalculateVisibilityOutputsTest {

  @Rule
  public PowerMockRule mPowerMockRule = new PowerMockRule();

  @Before
  public void setup() throws Exception {
  }

  @Test
  public void testNoUnnecessaryVisibilityOutputs() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .child(
                        TestDrawableComponent.create(c)
                            .withLayout().flexShrink(0)
                            .visibleHandler(c.newEventHandler(1))))
            .child(
                TestDrawableComponent.create(c)
                    .withLayout().flexShrink(0)
                    .invisibleHandler(c.newEventHandler(2)))
            .child(TestDrawableComponent.create(c))
            .build();
      }
    };

    LayoutState layoutState = calculateLayoutState(
        RuntimeEnvironment.application,
        component,
        -1,
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY));

    assertEquals(2, layoutState.getVisibilityOutputCount());
  }

  @Test
  public void testNoUnnecessaryVisibilityOutputsWithFullImpression() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .child(
                        TestDrawableComponent.create(c)
                            .withLayout().flexShrink(0)
                            .visibleHandler(c.newEventHandler(1))))
            .child(
                TestDrawableComponent.create(c)
                    .withLayout().flexShrink(0)
                    .fullImpressionHandler(c.newEventHandler(3)))
            .child(TestDrawableComponent.create(c))
            .build();
      }
    };

    LayoutState layoutState = calculateLayoutState(
        RuntimeEnvironment.application,
        component,
        -1,
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(100, SizeSpec.EXACTLY));

    assertEquals(2, layoutState.getVisibilityOutputCount());
  }

  @Test
  public void testNoUnnecessaryVisibilityOutputsWithFocused() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
            .child(
                Container.create(c).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
                    .child(
                        TestDrawableComponent.create(c)
                            .withLayout().flexShrink(0)
                            .visibleHandler(c.newEventHandler(1))))
            .child(
                TestDrawableComponent.create(c)
