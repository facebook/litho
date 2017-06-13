/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.content.ContextWrapper;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import com.facebook.litho.testing.testrunner.ComponentsTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static com.facebook.litho.ComponentsPools.acquireMountContent;
import static com.facebook.litho.ComponentsPools.release;
import static org.assertj.core.api.Java6Assertions.assertThat;

@RunWith(ComponentsTestRunner.class)
public class ComponentsPoolsTest {
  private final ComponentLifecycle mLifecycle = new ComponentLifecycle() {
    @Override
    int getId() {
      return 1;
    }
  };

  private ComponentContext mContext1;
  private ComponentContext mContext2;
  private ComponentContext mContext3;
  private ColorDrawable mMountContent;

  @Before
  public void setup() {
    mContext1 = new ComponentContext(RuntimeEnvironment.application);
    mContext2 = new ComponentContext(new ComponentContext(RuntimeEnvironment.application));
    mContext3 = new ComponentContext(new ContextWrapper(RuntimeEnvironment.application));
    mMountContent = new ColorDrawable(Color.RED);
  }

  @Test
  public void testAcquireMountContentWithSameContext() {
    assertThat(acquireMountContent(mContext1, mLifecycle.getId())).isNull();

    release(mContext1, mLifecycle, mMountContent);

    assertThat(mMountContent).isSameAs(acquireMountContent(mContext1, mLifecycle.getId()));
  }

  @Test
  public void testAcquireMountContentWithSameUnderlyingContext() {
    assertThat(acquireMountContent(mContext1, mLifecycle.getId())).isNull();

    release(mContext1, mLifecycle, mMountContent);

    assertThat(mMountContent).isSameAs(acquireMountContent(mContext2, mLifecycle.getId()));
  }

  @Test
  public void testAcquireMountContentWithDifferentUnderlyingContext() {
    assertThat(acquireMountContent(mContext1, mLifecycle.getId())).isNull();

    release(mContext1, mLifecycle, mMountContent);

    assertThat(acquireMountContent(mContext3, mLifecycle.getId())).isNull();
  }
}
