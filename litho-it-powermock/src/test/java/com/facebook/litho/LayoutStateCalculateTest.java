/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
