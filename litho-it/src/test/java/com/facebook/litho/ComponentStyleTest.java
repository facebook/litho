/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import static com.facebook.litho.it.R.attr.testAttrLargePadding;
import static com.facebook.litho.it.R.attr.testAttrLargeText;
import static com.facebook.litho.it.R.style.PaddingStyle;
import static com.facebook.litho.it.R.style.TextSizeStyle;
import static com.facebook.yoga.YogaEdge.ALL;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.powermock.reflect.Whitebox.getInternalState;

import android.view.ContextThemeWrapper;
import com.facebook.litho.it.R;
import com.facebook.litho.testing.ComponentsRule;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.Text;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

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
    Component component =
        Text.create(mContext, 0, TextSizeStyle)
            .text("text")
            .build();
    assertThat((int) getInternalState(component, "textSize"))
        .isEqualTo(mDimen);
  }

  @Test
  public void testOverrideStyleProp() {
    Component component =
        Text.create(mContext, 0, TextSizeStyle)
            .text("text")
            .textSizePx(2 * mDimen)
            .build();
    assertThat((int) getInternalState(component, "textSize"))
        .isEqualTo(2 * mDimen);
  }

  @Test
  public void testStyleLayout() {
    InternalNode node = (InternalNode)
        Text.create(mContext, 0, PaddingStyle)
            .text("text")
            .buildWithLayout();
    node.calculateLayout();
    assertThat(node.getPaddingLeft()).isEqualTo(mDimen);
  }

  @Test
  public void testOverrideStyleLayout() {
    InternalNode node = (InternalNode)
        Text.create(mContext, 0, PaddingStyle)
            .text("text")
            .paddingPx(ALL, mDimen * 2)
            .buildWithLayout();
    node.calculateLayout();
    assertThat(node.getPaddingLeft()).isEqualTo(2 * mDimen);
  }

  @Test
  public void testAttributeStyleProp() {
    Component component =
        Text.create(mContext, testAttrLargeText, 0)
            .text("text")
            .build();
    assertThat((int) getInternalState(component, "textSize"))
        .isEqualTo(mLargeDimen);
  }

  @Test
  public void testOverrideAttributeStyleProp() {
    Component component =
        Text.create(mContext, testAttrLargeText, 0)
            .text("text")
            .textSizePx(mDimen)
            .build();
    assertThat((int) getInternalState(component, "textSize"))
        .isEqualTo(mDimen);
  }

  @Test
  public void testAttributeStyleLayout() {
    InternalNode node = (InternalNode)
        Text.create(mContext, testAttrLargePadding, 0)
            .text("text")
            .buildWithLayout();
    node.calculateLayout();
    assertThat(node.getPaddingLeft()).isEqualTo(mLargeDimen);
  }

  @Test
  public void testOverrideAttributeStyleLayout() {
    InternalNode node = (InternalNode)
        Text.create(mContext, testAttrLargePadding, 0)
            .text("text")
            .paddingPx(ALL, mDimen * 2)
            .buildWithLayout();
    node.calculateLayout();
    assertThat(node.getPaddingLeft()).isEqualTo(2 * mDimen);
  }

  @Test
  public void testStyleResOverridenByAttrResForProp() {
    Component component =
        Text.create(mContext, testAttrLargeText, TextSizeStyle)
            .text("text")
            .build();
    assertThat((int) getInternalState(component, "textSize"))
        .isEqualTo(mLargeDimen);
  }

  @Test
  public void testStyleResOverridenByAttrResForLayout() {
    InternalNode node = (InternalNode)
        Text.create(mContext, testAttrLargePadding, PaddingStyle)
            .text("text")
            .buildWithLayout();
    node.calculateLayout();
    assertThat(node.getPaddingLeft()).isEqualTo(mLargeDimen);
  }
}
