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
import android.graphics.Rect;
import android.support.v4.util.SparseArrayCompat;

import com.facebook.components.testing.testrunner.ComponentsTestRunner;
import com.facebook.components.testing.TestComponent;
import com.facebook.components.testing.TestDrawableComponent;
import com.facebook.components.testing.TestSizeDependentComponent;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaConstants;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaMeasureFunction;
import com.facebook.yoga.YogaMeasureOutput;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;

import static com.facebook.yoga.YogaMeasureMode.EXACTLY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@PrepareForTest(Component.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*"})
@RunWith(ComponentsTestRunner.class)
public class TreeDiffingTest {

  @Rule
  public PowerMockRule mPowerMockRule = new PowerMockRule();

  private int mUnspecifiedSpec;

  private ComponentContext mContext;

  @Before
  public void setup() throws Exception {
    mContext = new ComponentContext(RuntimeEnvironment.application);
    mUnspecifiedSpec = SizeSpec.makeSizeSpec(0, SizeSpec.UNSPECIFIED);
  }

  @Test
  public void testDiffTreeDisabled() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c)
            .child(TestDrawableComponent.create(c))
            .child(
                Container.create(c)
                    .child(TestDrawableComponent.create(c)))
            .build();
      }
    };

    LayoutState layoutState = LayoutState.calculate(
        mContext,
        component,
        -1,
        SizeSpec.makeSizeSpec(350, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY),
        false,
        null);

    // Check diff tree is null.
    assertNull(layoutState.getDiffTree());
  }

  @Test
  public void testDiffTreeEnabled() {
    final Component component = new InlineLayoutSpec() {
      @Override
      protected ComponentLayout onCreateLayout(ComponentContext c) {
        return Container.create(c)
            .child(TestDrawableComponent.create(c))
            .child(
                Container.create(c)
                    .child(TestDrawableComponent.create(c)))
            .build();
      }
    };

    LayoutState layoutState = LayoutState.calculate(
        mContext,
        component,
        -1,
        SizeSpec.makeSizeSpec(350, SizeSpec.EXACTLY),
        SizeSpec.makeSizeSpec(200, SizeSpec.EXACTLY),
        true,
        null);

    // Check diff tree is not null and consistent.
    DiffNode node = layoutState.getDiffTree();
    assertNotNull(node);
    assertEquals(countNodes(node), 4);
  }

  private static int countNodes(DiffNode node) {
    int sum = 1;
    for (int i = 0; i < node.getChildCount(); i++) {
      sum += countNodes(node.getChildAt(i));
    }

    return sum;
  }

  private InternalNode createInternalNodeForMeasurableComponent(Component component) {
    InternalNode node = LayoutState.createTree(
        component,
        mContext);

    return node;
  }

  private long measureInternalNode(
      InternalNode node,
      float widthConstranint,
      float heightConstraint) {

    final YogaMeasureFunction measureFunc =
        Whitebox.getInternalState(
            node.mYogaNode,
            "mMeasureFunction");

    return measureFunc.measure(
        node.mYogaNode,
        widthConstranint,
        EXACTLY,
        heightConstraint,
        EXACTLY);
  }

  @Test
  public void testCachedMeasureFunction() {
    final Component component = TestDrawableComponent.create(mContext)
        .build();

    InternalNode node = createInternalNodeForMeasurableComponent(component);
    DiffNode diffNode = new DiffNode();
    diffNode.setLastHeightSpec(mUnspecifiedSpec);
    diffNode.setLastWidthSpec(mUnspecifiedSpec);
    diffNode.setLastMeasuredWidth(10);
    diffNode.setLastMeasuredHeight(5);
    diffNode.setComponent(component);

    node.setCachedMeasuresValid(true);
    node.setDiffNode(diffNode);

    long output = measureInternalNode(
        node,
