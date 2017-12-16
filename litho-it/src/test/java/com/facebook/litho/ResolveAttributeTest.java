/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import static com.facebook.litho.LayoutState.createAndMeasureTreeForComponent;
import static com.facebook.litho.SizeSpec.UNSPECIFIED;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static com.facebook.litho.it.R.attr.testAttrDimen;
import static com.facebook.litho.it.R.attr.testAttrDrawable;
import static com.facebook.litho.it.R.attr.undefinedAttrDimen;
import static com.facebook.litho.it.R.attr.undefinedAttrDrawable;
import static com.facebook.litho.it.R.dimen.default_dimen;
import static com.facebook.litho.it.R.dimen.test_dimen;
import static com.facebook.litho.it.R.dimen.test_dimen_float;
import static com.facebook.litho.it.R.drawable.test_bg;
import static com.facebook.litho.reference.Reference.acquire;
import static com.facebook.yoga.YogaEdge.LEFT;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.graphics.drawable.Drawable;
import android.view.ContextThemeWrapper;
import com.facebook.litho.it.R;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

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
    Column column = Column.create(mContext).backgroundAttr(testAttrDrawable, 0).build();

    InternalNode node = (InternalNode) Layout.create(mContext, column).build();

    Drawable d = mContext.getResources().getDrawable(test_bg);
    assertThat(acquire(mContext, node.getBackground())).isEqualTo(d);
  }

  @Test
  public void testResolveDimenAttribute() {
    Column column = Column.create(mContext).widthAttr(testAttrDimen, default_dimen).build();

    InternalNode node = (InternalNode) Layout.create(mContext, column).build();
    node.calculateLayout();

    int dimen =
        mContext.getResources().getDimensionPixelSize(R.dimen.test_dimen);
    assertThat((int) node.getWidth()).isEqualTo(dimen);
  }

  @Test
  public void testDefaultDrawableAttribute() {
    Column column = Column.create(mContext).backgroundAttr(undefinedAttrDrawable, test_bg).build();

    InternalNode node = (InternalNode) Layout.create(mContext, column).build();

    Drawable d = mContext.getResources().getDrawable(test_bg);
    assertThat(acquire(mContext, node.getBackground())).isEqualTo(d);
  }

  @Test
  public void testDefaultDimenAttribute() {
    Column column = Column.create(mContext).widthAttr(undefinedAttrDimen, test_dimen).build();

    InternalNode node = (InternalNode) Layout.create(mContext, column).build();
    node.calculateLayout();

    int dimen =
        mContext.getResources().getDimensionPixelSize(R.dimen.test_dimen);
    assertThat((int) node.getWidth()).isEqualTo(dimen);
  }

  @Test
  public void testFloatDimenWidthAttribute() {
    Column column = Column.create(mContext).widthAttr(undefinedAttrDimen, test_dimen_float).build();

    InternalNode node = (InternalNode) Layout.create(mContext, column).build();
    node.calculateLayout();

    int dimen =
        mContext.getResources().getDimensionPixelSize(test_dimen_float);
    assertThat(node.getWidth()).isEqualTo(dimen);
  }

  @Test
  public void testFloatDimenPaddingAttribute() {
    Column column =
        Column.create(mContext).paddingAttr(LEFT, undefinedAttrDimen, test_dimen_float).build();

    InternalNode node = (InternalNode) Layout.create(mContext, column).build();
    node.calculateLayout();

    int dimen =
        mContext.getResources().getDimensionPixelSize(test_dimen_float);
    assertThat(node.getPaddingLeft()).isEqualTo(dimen);
  }
}
