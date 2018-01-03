/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import static com.facebook.litho.it.R.dimen.test_dimen;
import static com.facebook.litho.it.R.dimen.test_dimen_float;
import static com.facebook.yoga.YogaEdge.LEFT;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.view.ContextThemeWrapper;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.it.R;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class ResolveResTest {
  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(
        new ContextThemeWrapper(RuntimeEnvironment.application, R.style.TestTheme));
  }

  @After
  public void tearDown() {
    ComponentsConfiguration.usePooling = true;
  }

  @Test
  public void testDefaultDimenWidthRes() {
    Column column = Column.create(mContext).widthRes(test_dimen).build();

    InternalNode node = Layout.create(mContext, column);
    node.calculateLayout();

    int dimen = mContext.getResources().getDimensionPixelSize(test_dimen);
    assertThat(node.getWidth()).isEqualTo(dimen);
  }

  @Test
  public void testDefaultDimenPaddingRes() {
    Column column = Column.create(mContext).paddingRes(LEFT, test_dimen).build();

    InternalNode node = Layout.create(mContext, column);
    node.calculateLayout();

    int dimen = mContext.getResources().getDimensionPixelSize(test_dimen);
    assertThat(node.getWidth()).isEqualTo(dimen);
  }

  @Test
  public void testFloatDimenWidthRes() {
    Column column = Column.create(mContext).widthRes(test_dimen_float).build();

    InternalNode node = Layout.create(mContext, column);
    node.calculateLayout();

    int dimen = mContext.getResources().getDimensionPixelSize(test_dimen_float);
    assertThat(node.getWidth()).isEqualTo(dimen);
  }

  @Test
  public void testFloatDimenPaddingRes() {
    Column column = Column.create(mContext).paddingRes(LEFT, test_dimen_float).build();

    InternalNode node = Layout.create(mContext, column);
    node.calculateLayout();

    int dimen = mContext.getResources().getDimensionPixelSize(test_dimen_float);
    assertThat(node.getPaddingLeft()).isEqualTo(dimen);
  }
}
