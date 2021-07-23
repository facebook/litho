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

package com.facebook.litho.sections.widget;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.facebook.litho.testing.assertj.LithoAssertions.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assume.assumeThat;

import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.litho.Row;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.config.TempComponentsConfigurations;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.common.SingleComponentSection;
import com.facebook.litho.testing.helper.ComponentTestHelper;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import org.assertj.core.api.Java6Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.LooperMode;

/** Tests {@link RecyclerCollectionComponentSpec} */
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner.class)
public class LegacyRecyclerCollectionComponentSpecTest {

  private ComponentContext mComponentContext;

  @Before
  public void assumeDebug() {
    assumeThat(
        "These tests can only be run in debug mode.",
        ComponentsConfiguration.IS_INTERNAL_BUILD,
        is(true));
  }

  @Before
  public void setup() throws Exception {
    TempComponentsConfigurations.setShouldAddHostViewForRootComponent(false);
    mComponentContext = new ComponentContext(getApplicationContext());
  }

  @Test
  public void testNestedIncrementalMountNormal() {
    LithoView view =
        ComponentTestHelper.mountComponent(
            mComponentContext,
            RecyclerCollectionComponent.create(mComponentContext)
                .section(
                    SingleComponentSection.create(new SectionContext(mComponentContext))
                        .component(
                            Row.create(mComponentContext)
                                .viewTag("rv_row")
                                .heightDip(100)
                                .widthDip(100))
                        .build())
                .build(),
            true,
            true);

    final LithoView childView = (LithoView) findViewWithTag(view, "rv_row");
    assertThat(childView).isNotNull();
    Java6Assertions.assertThat(childView.getComponentTree().isIncrementalMountEnabled()).isTrue();
  }

  @Test
  public void testNestedIncrementalMountDisabled() {
    LithoView view =
        ComponentTestHelper.mountComponent(
            mComponentContext,
            RecyclerCollectionComponent.create(mComponentContext)
                .section(
                    SingleComponentSection.create(new SectionContext(mComponentContext))
                        .component(
                            Row.create(mComponentContext)
                                .viewTag("rv_row")
                                .heightDip(100)
                                .widthDip(100))
                        .build())
                .build(),
            false,
            false);

    final LithoView childView = (LithoView) findViewWithTag(view, "rv_row");
    assertThat(childView).isNotNull();
    Java6Assertions.assertThat(childView.getComponentTree().isIncrementalMountEnabled()).isFalse();
  }

  @Nullable
  private static View findViewWithTag(@Nullable View root, @Nullable String tag) {
    if (root == null || TextUtils.isEmpty(tag)) {
      return null;
    }

    if (tag.equals(root.getTag())) {
      return root;
    }

    if (root instanceof ViewGroup) {
      ViewGroup vg = (ViewGroup) root;
      for (int i = 0; i < vg.getChildCount(); i++) {
        View v = findViewWithTag(vg.getChildAt(i), tag);
        if (v != null) {
          return v;
        }
      }
    }

    return null;
  }

  @After
  public void restoreConfiguration() {
    TempComponentsConfigurations.restoreShouldAddHostViewForRootComponent();
  }
}
