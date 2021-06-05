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

import static com.facebook.litho.LifecycleStep.getSteps;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.graphics.Color;
import androidx.core.view.ViewCompat;
import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.LayoutWithSizeSpecLifecycleTester;
import com.facebook.litho.widget.SolidColor;
import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class LayoutWithSizeSpecLifecycleTest {

  public final @Rule LithoViewRule mLithoViewRule = new LithoViewRule();

  @Test
  public void onSetRootWithoutLayoutWithSizeSpec_shouldNotCallLifecycleMethods() {
    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final Component component =
        LayoutWithSizeSpecLifecycleTester.create(mLithoViewRule.getContext()).steps(info).build();
    mLithoViewRule.setRoot(component);

    assertThat(getSteps(info)).describedAs("No lifecycle methods should be called").isEmpty();
  }

  @Test
  public void onSetRootWithLayoutWithSizeSpecAtRoot_shouldCallLifecycleMethods() {
    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final Component component =
        LayoutWithSizeSpecLifecycleTester.create(mLithoViewRule.getContext()).steps(info).build();
    mLithoViewRule.setRoot(component);

    mLithoViewRule.attachToWindow().measure().layout();

    assertThat(getSteps(info))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE, LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC);
  }

  @Test
  public void onSetRootWithLayoutWithSizeSpecWhichDoesNotRemeasure_shouldCallLifecycleMethods() {
    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final ComponentContext c = mLithoViewRule.getContext();
    final Component component =
        Column.create(c)
            .child(
                LayoutWithSizeSpecLifecycleTester.create(c)
                    .steps(info)
                    .importantForAccessibility(ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES))
            .build();

    mLithoViewRule.setRoot(component);
    mLithoViewRule.attachToWindow().measure().layout();

    assertThat(getSteps(info))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE, LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC);
  }

  @Test
  public void onSetRootWithLayoutWithSizeSpecWhichRemeasure_shouldCallLifecycleMethods() {
    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final ComponentContext c = mLithoViewRule.getContext();
    final Component component =
        Row.create(c)
            .child(SolidColor.create(c).color(Color.BLACK).widthDip(100).heightDip(100))
            .child(
                LayoutWithSizeSpecLifecycleTester.create(c)
                    .steps(info)
                    .widthPercent(50)
                    .heightPercent(50))
            .build();

    mLithoViewRule.setRoot(component);
    mLithoViewRule.attachToWindow().measure().layout();

    assertThat(getSteps(info))
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC);
  }
}
