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

import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.widget.HorizontalScroll;
import com.facebook.litho.widget.MountSpecLifecycleTester;
import java.util.Arrays;
import java.util.Collection;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;

@RunWith(ParameterizedRobolectricTestRunner.class)
public class AttachDetachTest {
  private boolean mDefaultReleaseNestedLithoViews;
  private final boolean mReleaseNestedLithoViews;
  public final @Rule LithoViewRule mLithoViewRule = new LithoViewRule();

  @ParameterizedRobolectricTestRunner.Parameters(name = "releaseNestedLithoViews={0}")
  public static Collection data() {
    return Arrays.asList(new Object[][] {{true}});
  }

  public AttachDetachTest(boolean releaseNestedLithoViews) {
    mReleaseNestedLithoViews = releaseNestedLithoViews;
  }

  @Before
  public void before() {
    mDefaultReleaseNestedLithoViews = ComponentsConfiguration.releaseNestedLithoViews;
    ComponentsConfiguration.releaseNestedLithoViews = mReleaseNestedLithoViews;
  }

  @After
  public void after() {
    ComponentsConfiguration.releaseNestedLithoViews = mDefaultReleaseNestedLithoViews;
  }

  @Test
  public void horizontalScroll_releaseTree_childComponentShouldCallOnDetached() {
    final ComponentContext c = mLithoViewRule.getContext();
    final LifecycleTracker lifecycleTracker = new LifecycleTracker();
    mLithoViewRule.setRoot(
        HorizontalScroll.create(c)
            .contentProps(
                MountSpecLifecycleTester.create(c)
                    .intrinsicSize(new Size(800, 600))
                    .lifecycleTracker(lifecycleTracker)
                    .build())
            .build());

    mLithoViewRule.attachToWindow().measure().layout();
    mLithoViewRule.release();

    assertThat(lifecycleTracker.getSteps())
        .describedAs(
            "Child component should call @OnDetached when parent HorizontalScroll is detached")
        .contains(LifecycleStep.ON_DETACHED);
  }
}
