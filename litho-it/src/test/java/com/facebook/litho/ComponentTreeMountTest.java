/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import static android.graphics.Color.BLACK;
import static android.graphics.Color.YELLOW;
import static com.facebook.litho.testing.ComponentTestHelper.mountComponent;
import static com.facebook.litho.testing.TestDrawableComponent.create;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

import android.graphics.drawable.ColorDrawable;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class ComponentTreeMountTest {

  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
  }

  @Test
  public void testRemountsWithNewInputOnSameLayout() {
    final LithoView lithoView = mountComponent(
        mContext,
        create(mContext)
            .color(BLACK)
            .build());
    shadowOf(lithoView).callOnAttachedToWindow();

    assertThat(lithoView.getDrawables()).hasSize(1);
    assertThat(((ColorDrawable) lithoView.getDrawables().get(0)).getColor()).isEqualTo(BLACK);

    lithoView.getComponentTree().setRoot(
        create(mContext)
            .color(YELLOW)
            .build());
    assertThat(lithoView.getDrawables()).hasSize(1);
    assertThat(((ColorDrawable) lithoView.getDrawables().get(0)).getColor()).isEqualTo(YELLOW);
  }
}
