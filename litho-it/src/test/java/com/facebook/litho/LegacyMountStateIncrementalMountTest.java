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

import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.graphics.Rect;
import com.facebook.litho.config.TempComponentsConfigurations;
import com.facebook.litho.testing.LegacyLithoViewRule;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.Text;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.LooperMode;

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner.class)
public class LegacyMountStateIncrementalMountTest {
  private ComponentContext mContext;

  public final @Rule LegacyLithoViewRule mLegacyLithoViewRule = new LegacyLithoViewRule();

  @Before
  public void setup() {
    TempComponentsConfigurations.setShouldAddHostViewForRootComponent(false);
    mContext = mLegacyLithoViewRule.getContext();
    mLegacyLithoViewRule.useLithoView(new LithoView(mContext));
  }

  @Test
  public void testRootViewAttributes_incrementalMountAfterUnmount_setViewAttributes() {
    final Component root = Text.create(mContext).text("Test").contentDescription("testcd").build();

    mLegacyLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY))
        .measure()
        .layout();

    final LithoView lithoView = mLegacyLithoViewRule.getLithoView();
    assertThat(lithoView.getContentDescription()).isEqualTo("testcd");

    lithoView.unmountAllItems();
    assertThat(lithoView.getContentDescription()).isNull();

    lithoView.getComponentTree().mountComponent(new Rect(0, 5, 10, 15), true);
    assertThat(lithoView.getContentDescription()).isEqualTo("testcd");
  }

  @After
  public void restoreConfiguration() {
    TempComponentsConfigurations.restoreShouldAddHostViewForRootComponent();
  }
}
