/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.facebook.litho.Column.create;
import static com.facebook.litho.LayoutOutput.getLayoutOutput;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.animation.StateListAnimator;
import android.annotation.TargetApi;
import android.os.Build;
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.SimpleMountSpecTester;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.annotation.Config;

@PrepareForTest({LayoutState.class})
@PowerMockIgnore({
  "org.mockito.*",
  "org.robolectric.*",
  "android.*",
  "androidx.*",
  "com.facebook.yoga.*"
})
@Config(sdk = Build.VERSION_CODES.LOLLIPOP)
@RunWith(LithoTestRunner.class)
public class LayoutStateCalculateTest {

  @Rule public PowerMockRule mPowerMockRule = new PowerMockRule();

  private ComponentContext mBaseContext;

  @Before
  public void setup() {
    mBaseContext = new ComponentContext(getApplicationContext());
  }

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
                        .child(SimpleMountSpecTester.create(c))
                        .stateListAnimator(stateListAnimator))
                .build();
          }
        };

    final ComponentTree componentTree = ComponentTree.create(mBaseContext).build();
    final LayoutState layoutState =
        calculateLayoutState(
            componentTree.getContext(),
            component,
            -1,
            makeSizeSpec(100, EXACTLY),
            makeSizeSpec(100, EXACTLY));

    assertThat(layoutState.getMountableOutputCount()).isEqualTo(3);

    assertThat(getLayoutOutput(layoutState.getMountableOutputAt(1)).getComponent())
        .isExactlyInstanceOf(HostComponent.class);
    assertThat(
            getLayoutOutput(layoutState.getMountableOutputAt(1))
                .getViewNodeInfo()
                .getStateListAnimator())
        .isSameAs(stateListAnimator);

    assertThat(getLayoutOutput(layoutState.getMountableOutputAt(2)).getComponent())
        .isExactlyInstanceOf(SimpleMountSpecTester.class);
  }

  private static LayoutState calculateLayoutState(
      final ComponentContext context,
      final Component component,
      final int componentTreeId,
      final int widthSpec,
      final int heightSpec) {

    return LayoutState.calculate(
        context,
        component,
        componentTreeId,
        widthSpec,
        heightSpec,
        LayoutState.CalculateLayoutSource.TEST);
  }

  private static Component getComponentAt(final LayoutState layoutState, final int index) {
    return getLayoutOutput(layoutState.getMountableOutputAt(index)).getComponent();
  }

  private static boolean isHostComponent(final Component component) {
    return component instanceof HostComponent;
  }
}
