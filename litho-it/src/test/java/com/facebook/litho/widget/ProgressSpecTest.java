/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import static org.assertj.core.api.Java6Assertions.assertThat;

import android.view.View;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.litho.testing.ComponentTestHelper;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

/**
 * Tests {@link ProgressSpec}
 */

@RunWith(ComponentsTestRunner.class)
public class ProgressSpecTest {
  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
  }

  @Test
  public void testDefault() {
    LithoView view = getMountedView();
    assertThat(view.getMeasuredWidth()).isGreaterThan(0);
    assertThat(view.getMeasuredHeight()).isGreaterThan(0);
  }

  @Test
  public void testUnsetSize() {
    LithoView view = getMountedView();

    view.measure(
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

    assertThat(view.getMeasuredWidth()).isEqualTo(ProgressSpec.DEFAULT_SIZE);
    assertThat(view.getMeasuredHeight()).isEqualTo(ProgressSpec.DEFAULT_SIZE);
  }

  private LithoView getMountedView() {
    Progress.Builder progress = Progress.create(mContext);

    return ComponentTestHelper.mountComponent(
        progress);
  }
}
