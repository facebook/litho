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

import android.view.ContextThemeWrapper;

import com.facebook.litho.R;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.Text;
import com.facebook.litho.testing.ComponentsRule;
import com.facebook.yoga.YogaEdge;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;

@RunWith(ComponentsTestRunner.class)
@org.junit.Ignore("t16280359")
public class ComponentStyleTest {
  private int mDimen;
  private int mLargeDimen;
  private ComponentContext mContext;

  @Rule
  public ComponentsRule mComponentsRule = new ComponentsRule();

  @Before
  public void setup() {
    mContext = new ComponentContext(
        new ContextThemeWrapper(RuntimeEnvironment.application, R.style.TestTheme));
    mDimen = mContext.getResources().getDimensionPixelSize(R.dimen.test_dimen);
    mLargeDimen = mContext.getResources().getDimensionPixelSize(R.dimen.test_large_dimen);
  }

  @Test
  public void testStyleProp() {
    Component<Text> component =
        Text.create(mContext, 0, R.style.TextSizeStyle)
            .text("text")
            .build();
    assertEquals(mDimen, Whitebox.getInternalState(component, "textSize"));
  }

  @Test
  public void testOverrideStyleProp() {
    Component<Text> component =
        Text.create(mContext, 0, R.style.TextSizeStyle)
            .text("text")
            .textSizePx(2 * mDimen)
            .build();
    assertEquals(2 * mDimen, Whitebox.getInternalState(component, "textSize"));
  }

  @Test
  public void testStyleLayout() {
    InternalNode node = (InternalNode)
        Text.create(mContext, 0, R.style.PaddingStyle)
            .text("text")
            .buildWithLayout();
    node.calculateLayout();
    assertEquals(mDimen, node.getPaddingLeft());
  }

  @Test
  public void testOverrideStyleLayout() {
    InternalNode node = (InternalNode)
        Text.create(mContext, 0, R.style.PaddingStyle)
            .text("text")
            .withLayout().flexShrink(0)
            .paddingPx(YogaEdge.ALL, mDimen * 2)
            .build();
    node.calculateLayout();
    assertEquals(2 * mDimen, node.getPaddingLeft());
  }

  @Test
  public void testAttributeStyleProp() {
    Component<Text> component =
        Text.create(mContext, R.attr.testAttrLargeText, 0)
            .text("text")
            .build();
    assertEquals(mLargeDimen, Whitebox.getInternalState(component, "textSize"));
  }

  @Test
  public void testOverrideAttributeStyleProp() {
    Component<Text> component =
        Text.create(mContext, R.attr.testAttrLargeText, 0)
            .text("text")
            .textSizePx(mDimen)
            .build();
    assertEquals(mDimen, Whitebox.getInternalState(component, "textSize"));
  }

  @Test
  public void testAttributeStyleLayout() {
    InternalNode node = (InternalNode)
        Text.create(mContext, R.attr.testAttrLargePadding, 0)
            .text("text")
            .buildWithLayout();
    node.calculateLayout();
    assertEquals(mLargeDimen, node.getPaddingLeft());
  }

  @Test
  public void testOverrideAttributeStyleLayout() {
    InternalNode node = (InternalNode)
        Text.create(mContext, R.attr.testAttrLargePadding, 0)
            .text("text")
            .withLayout().flexShrink(0)
            .paddingPx(YogaEdge.ALL, mDimen * 2)
            .build();
    node.calculateLayout();
    assertEquals(2 * mDimen, node.getPaddingLeft());
  }

  @Test
  public void testStyleResOverridenByAttrResForProp() {
    Component<Text> component =
        Text.create(mContext, R.attr.testAttrLargeText, R.style.TextSizeStyle)
            .text("text")
            .build();
    assertEquals(mLargeDimen, Whitebox.getInternalState(component, "textSize"));
  }

  @Test
  public void testStyleResOverridenByAttrResForLayout() {
    InternalNode node = (InternalNode)
        Text.create(mContext, R.attr.testAttrLargePadding, R.style.PaddingStyle)
            .text("text")
            .buildWithLayout();
    node.calculateLayout();
    assertEquals(mLargeDimen, node.getPaddingLeft());
  }
}
