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

import android.graphics.drawable.Drawable;
import android.view.ContextThemeWrapper;

import com.facebook.litho.R;
import com.facebook.litho.reference.Reference;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static junit.framework.Assert.assertEquals;

@RunWith(ComponentsTestRunner.class)
public class ResolveAttributeTest {
  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(
        new ContextThemeWrapper(RuntimeEnvironment.application, R.style.TestTheme));
  }

  @Test
  public void testResolveDrawableAttribute() {
    InternalNode node = (InternalNode) Container.create(mContext).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
        .backgroundAttr(R.attr.testAttrDrawable, 0)
        .build();

    Drawable d = mContext.getResources().getDrawable(R.drawable.test_bg);
    assertEquals(d, Reference.acquire(mContext, node.getBackground()));
  }

  @Test
  public void testResolveDimenAttribute() {
    InternalNode node = (InternalNode) Container.create(mContext).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
        .widthAttr(R.attr.testAttrDimen, R.dimen.default_dimen)
        .build();
    node.calculateLayout();

    int dimen =
        mContext.getResources().getDimensionPixelSize(R.dimen.test_dimen);
    assertEquals(dimen, (int) node.getWidth());
  }

  @Test
  public void testDefaultDrawableAttribute() {
    InternalNode node = (InternalNode) Container.create(mContext).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
        .backgroundAttr(R.attr.undefinedAttrDrawable, R.drawable.test_bg)
        .build();

    Drawable d = mContext.getResources().getDrawable(R.drawable.test_bg);
    assertEquals(d, Reference.acquire(mContext, node.getBackground()));
  }

  @Test
  public void testDefaultDimenAttribute() {
    InternalNode node = (InternalNode) Container.create(mContext).flexDirection(YogaFlexDirection.COLUMN).flexShrink(0).alignContent(YogaAlign.FLEX_START)
        .widthAttr(R.attr.undefinedAttrDimen, R.dimen.test_dimen)
        .build();
    node.calculateLayout();

    int dimen =
        mContext.getResources().getDimensionPixelSize(R.dimen.test_dimen);
    assertEquals(dimen, (int) node.getWidth());
  }
}
