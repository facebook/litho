/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.facebook.litho.testing.TestViewComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static com.facebook.litho.Column.create;
import static com.facebook.litho.testing.ComponentTestHelper.mountComponent;
import static com.facebook.yoga.YogaEdge.BOTTOM;
import static com.facebook.yoga.YogaEdge.LEFT;
import static com.facebook.yoga.YogaEdge.RIGHT;
import static com.facebook.yoga.YogaEdge.TOP;
import static org.assertj.core.api.Java6Assertions.assertThat;

@RunWith(ComponentsTestRunner.class)
public class MountStateViewTest {

  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
  }

  @Test
  public void testViewPaddingAndBackground() {
    final int color = 0xFFFF0000;
    final LithoView lithoView = mountComponent(
        mContext,
        new InlineLayoutSpec() {
          @Override
          protected ComponentLayout onCreateLayout(ComponentContext c) {
            return create(c)
                .child(
                    TestViewComponent.create(c)
                        .withLayout()
                        .paddingPx(LEFT, 5)
                        .paddingPx(TOP, 6)
                        .paddingPx(RIGHT, 7)
                        .paddingPx(BOTTOM, 8)
                        .backgroundColor(color))
                .build();
          }
        });

    final View child = lithoView.getChildAt(0);
    final Drawable background = child.getBackground();

    assertThat(child.getPaddingLeft()).isEqualTo(5);
    assertThat(child.getPaddingTop()).isEqualTo(6);
    assertThat(child.getPaddingRight()).isEqualTo(7);
    assertThat(child.getPaddingBottom()).isEqualTo(8);
    assertThat(background).isInstanceOf(ColorDrawable.class);
    assertThat(((ColorDrawable) background).getColor()).isEqualTo(color);
  }
}
