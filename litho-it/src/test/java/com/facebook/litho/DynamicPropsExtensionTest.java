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

import static org.assertj.core.api.Java6Assertions.assertThat;

import android.view.View;
import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.MountSpecLifecycleTester;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class DynamicPropsExtensionTest {

  public final @Rule LithoViewRule mLithoViewRule = new LithoViewRule();

  @Test
  public void whenDynamicValeIsSet_ShouldOverrideAttributeSetByMountSpec() {
    ComponentContext c = mLithoViewRule.getContext();

    final Component root =
        MountSpecLifecycleTester.create(c)
            .intrinsicSize(new Size(100, 100))
            .lifecycleTracker(new LifecycleTracker())
            .defaultScale(0.5f)
            .scaleX(new DynamicValue<>(0.2f))
            .scaleY(new DynamicValue<>(0.2f))
            .build();

    mLithoViewRule.attachToWindow().setRoot(root).measure().layout();

    View content = mLithoViewRule.getLithoView().getChildAt(0);

    assertThat(content.getScaleX())
        .describedAs("scale should be applied from the dynamic value")
        .isEqualTo(0.2f);

    // unmount everything
    mLithoViewRule.getLithoView().setComponentTree(null);

    assertThat(content.getScaleX())
        .describedAs("scale should be restored to the initial value")
        .isEqualTo(0.5f);
  }
}
