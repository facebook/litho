/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import static com.facebook.litho.ComponentTree.create;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.os.Looper;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowLooper;

@RunWith(ComponentsTestRunner.class)
public class ComponentTreeHasCompatibleLayoutTest {

  private int mWidthSpec;
  private int mWidthSpec2;
  private int mHeightSpec;
  private int mHeightSpec2;

  private Component mComponent;
  private ShadowLooper mLayoutThreadShadowLooper;
  private ComponentContext mContext;
  private ComponentTree mComponentTree;

  @Before
  public void setup() throws Exception {
    mContext = new ComponentContext(RuntimeEnvironment.application);
    mComponent = TestDrawableComponent.create(mContext).build();
    mComponentTree = create(mContext, mComponent).build();

    mLayoutThreadShadowLooper =
        Shadows.shadowOf(
            (Looper) Whitebox.invokeMethod(ComponentTree.class, "getDefaultLayoutThreadLooper"));

    mWidthSpec = makeSizeSpec(39, EXACTLY);
    mWidthSpec2 = makeSizeSpec(40, EXACTLY);
    mHeightSpec = makeSizeSpec(41, EXACTLY);
    mHeightSpec2 = makeSizeSpec(42, EXACTLY);
  }

  @Test
  public void testNoLayoutComputed() {
    assertThat(mComponentTree.hasCompatibleLayout(mWidthSpec, mHeightSpec)).isFalse();
  }

  @Test
  public void testMainThreadLayoutSet() {
    mComponentTree.setRootAndSizeSpec(mComponent, mWidthSpec, mHeightSpec);
    assertThat(mComponentTree.hasCompatibleLayout(mWidthSpec, mHeightSpec)).isTrue();
    assertThat(mComponentTree.hasCompatibleLayout(mWidthSpec2, mHeightSpec2)).isFalse();
  }

  @Test
  public void testBackgroundLayoutSet() {
    mComponentTree.setSizeSpecAsync(mWidthSpec, mHeightSpec);
    assertThat(mComponentTree.hasCompatibleLayout(mWidthSpec, mHeightSpec)).isFalse();

    // Now the background thread run the queued task.
    mLayoutThreadShadowLooper.runToEndOfTasks();

    assertThat(mComponentTree.hasCompatibleLayout(mWidthSpec, mHeightSpec)).isTrue();
  }
}
