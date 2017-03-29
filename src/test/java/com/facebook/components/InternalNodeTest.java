/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.graphics.Color;
import android.support.v4.view.ViewCompat;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaDirection;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaPositionType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(ComponentsTestRunner.class)
public class InternalNodeTest {
  private static final int LIFECYCLE_TEST_ID = 1;

  private final ComponentLifecycle mLifecycle = new ComponentLifecycle() {
    @Override
    int getId() {
      return LIFECYCLE_TEST_ID;
    }
  };

  private static class TestComponent<L extends ComponentLifecycle> extends Component<L> {
    public TestComponent(L component) {
      super(component);
    }

    @Override
    public String getSimpleName() {
      return "TestComponent";
    }
  }

  private InternalNode mNode;

  @Before
  public void setup() {
    mNode = ComponentsPools.acquireInternalNode(
        new ComponentContext(RuntimeEnvironment.application),
        RuntimeEnvironment.application.getResources());
  }

  @Test
  public void testLayoutDirectionFlag() {
    mNode.layoutDirection(YogaDirection.INHERIT);
    assertTrue(isFlagSet(mNode, "PFLAG_LAYOUT_DIRECTION_IS_SET"));
    clearFlag(mNode, "PFLAG_LAYOUT_DIRECTION_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testAlignSelfFlag() {
    mNode.alignSelf(YogaAlign.STRETCH);
    assertTrue(isFlagSet(mNode, "PFLAG_ALIGN_SELF_IS_SET"));
    clearFlag(mNode, "PFLAG_ALIGN_SELF_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testPositionTypeFlag() {
    mNode.positionType(YogaPositionType.ABSOLUTE);
    assertTrue(isFlagSet(mNode, "PFLAG_POSITION_TYPE_IS_SET"));
    clearFlag(mNode, "PFLAG_POSITION_TYPE_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testFlexFlag() {
    mNode.flex(1.5f);
    assertTrue(isFlagSet(mNode, "PFLAG_FLEX_IS_SET"));
    clearFlag(mNode, "PFLAG_FLEX_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testFlexGrowFlag() {
    mNode.flexGrow(1.5f);
    assertTrue(isFlagSet(mNode, "PFLAG_FLEX_GROW_IS_SET"));
    clearFlag(mNode, "PFLAG_FLEX_GROW_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testFlexShrinkFlag() {
    mNode.flexShrink(1.5f);
    assertTrue(isFlagSet(mNode, "PFLAG_FLEX_SHRINK_IS_SET"));
    clearFlag(mNode, "PFLAG_FLEX_SHRINK_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testFlexBasisFlag() {
    mNode.flexBasisPx(1);
    assertTrue(isFlagSet(mNode, "PFLAG_FLEX_BASIS_IS_SET"));
    clearFlag(mNode, "PFLAG_FLEX_BASIS_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testImportantForAccessibilityFlag() {
    mNode.importantForAccessibility(ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
    assertTrue(isFlagSet(mNode, "PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET"));
    clearFlag(mNode, "PFLAG_IMPORTANT_FOR_ACCESSIBILITY_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testDuplicateParentStateFlag() {
    mNode.duplicateParentState(false);
    assertTrue(isFlagSet(mNode, "PFLAG_DUPLICATE_PARENT_STATE_IS_SET"));
    clearFlag(mNode, "PFLAG_DUPLICATE_PARENT_STATE_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testMarginFlag() {
    mNode.marginPx(YogaEdge.ALL, 3);
    assertTrue(isFlagSet(mNode, "PFLAG_MARGIN_IS_SET"));
    clearFlag(mNode, "PFLAG_MARGIN_IS_SET");
    assertEmptyFlags(mNode);
  }

  @Test
  public void testPaddingFlag() {
