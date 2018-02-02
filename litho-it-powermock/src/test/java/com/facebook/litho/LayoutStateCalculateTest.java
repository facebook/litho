/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho;

import static com.facebook.litho.Column.create;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.robolectric.RuntimeEnvironment.application;

import android.animation.StateListAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

@Config(sdk = Build.VERSION_CODES.LOLLIPOP)
@RunWith(ComponentsTestRunner.class)
public class LayoutStateCalculateTest {

  @Test
  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public void testLayoutOutputsWithStateListAnimator() {
    final StateListAnimator stateListAnimator = new StateListAnimator();

    final Component component =
        new InlineLayoutSpec() {
          @Override
          protected Component onCreateLayout(final ComponentContext c) {
            return create(c)
                .child(
                    create(c)
                        .child(TestDrawableComponent.create(c))
                        .stateListAnimator(stateListAnimator))
                .build();
          }
        };

    final LayoutState layoutState =
        calculateLayoutState(
            application, component, -1, makeSizeSpec(100, EXACTLY), makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(3);

    assertThat(layoutState.getMountableOutputAt(1).getComponent())
        .isExactlyInstanceOf(HostComponent.class);
    assertThat(layoutState.getMountableOutputAt(1).getViewNodeInfo().getStateListAnimator())
        .isSameAs(stateListAnimator);

    assertThat(layoutState.getMountableOutputAt(2).getComponent())
        .isExactlyInstanceOf(TestDrawableComponent.class);
  }

  private static LayoutState calculateLayoutState(
      final Context context,
      final Component component,
      final int componentTreeId,
      final int widthSpec,
      final int heightSpec) {

    return LayoutState.calculate(
        new ComponentContext(context),
        component,
        componentTreeId,
        widthSpec,
        heightSpec,
        LayoutState.CalculateLayoutSource.TEST);
  }
}
