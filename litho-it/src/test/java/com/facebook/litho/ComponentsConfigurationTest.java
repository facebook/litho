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
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static org.assertj.core.api.Assertions.assertThat;

import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.common.SingleComponentSection;
import com.facebook.litho.sections.widget.ListRecyclerConfiguration;
import com.facebook.litho.sections.widget.RecyclerBinderConfiguration;
import com.facebook.litho.sections.widget.RecyclerCollectionComponent;
import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class ComponentsConfigurationTest {

  @Rule public final LithoViewRule mLithoViewRule = new LithoViewRule();
  ComponentContext mComponentContext = new ComponentContext(getApplicationContext());
  ComponentsConfiguration.Builder mDefaultBuilder =
      ComponentsConfiguration.getDefaultComponentsConfigurationBuilder();

  @Test
  public void testSetFlagThroughComponentConfigToComponentTree() {
    ComponentsConfiguration.setDefaultComponentsConfigurationBuilder(
        ComponentsConfiguration.create().useCancelableLayoutFutures(true));
    ComponentTree componentTree =
        ComponentTree.create(mComponentContext)
            .componentsConfiguration(ComponentsConfiguration.getDefaultComponentsConfiguration())
            .build();
    ComponentsConfiguration componentsConfiguration =
        Whitebox.getInternalState(componentTree, "mComponentsConfiguration");
    assertThat(componentsConfiguration.mUseCancelableLayoutFutures).isTrue();
    ComponentsConfiguration.setDefaultComponentsConfigurationBuilder(mDefaultBuilder);
  }

  @Test
  public void testSetFlagThroughComponentConfigToComponentTreeWithRecyclerCollectionComponent() {
    ComponentsConfiguration.setDefaultComponentsConfigurationBuilder(
        ComponentsConfiguration.create().useCancelableLayoutFutures(true));
    RecyclerBinderConfiguration recyclerBinderConfiguration =
        RecyclerBinderConfiguration.create()
            .componentsConfiguration(ComponentsConfiguration.getDefaultComponentsConfiguration())
            .build();
    mLithoViewRule
        .setRoot(
            RecyclerCollectionComponent.create(mComponentContext)
                .recyclerConfiguration(
                    ListRecyclerConfiguration.create()
                        .recyclerBinderConfiguration(recyclerBinderConfiguration)
                        .build())
                .section(
                    SingleComponentSection.create(new SectionContext(mComponentContext))
                        .component(
                            Row.create(mComponentContext)
                                .viewTag("rv_row")
                                .heightDip(100)
                                .widthDip(100))
                        .build())
                .build())
        .setSizeSpecs(makeSizeSpec(10, SizeSpec.EXACTLY), makeSizeSpec(5, SizeSpec.EXACTLY));
    mLithoViewRule.attachToWindow().measure().layout().setSizeSpecs(10, 10);

    final LithoView childView =
        (LithoView) findViewWithTag(mLithoViewRule.getLithoView(), "rv_row");
    assertThat(childView).isNotNull();
    ComponentsConfiguration componentsConfiguration =
        Whitebox.getInternalState(childView.getComponentTree(), "mComponentsConfiguration");
    assertThat(componentsConfiguration.mUseCancelableLayoutFutures).isTrue();
    ComponentsConfiguration.setDefaultComponentsConfigurationBuilder(mDefaultBuilder);
  }

  @Test
  public void testOverrideDefaultBuilder() {
    ComponentsConfiguration.setDefaultComponentsConfigurationBuilder(
        ComponentsConfiguration.create()
            .useCancelableLayoutFutures(true)
            .ignoreNullLayoutStateError(true));
    assertThat(
            ComponentsConfiguration.getDefaultComponentsConfiguration()
                .getUseCancelableLayoutFutures())
        .isTrue();
    ComponentsConfiguration.setDefaultComponentsConfigurationBuilder(
        ComponentsConfiguration.create().useCancelableLayoutFutures(false));
    assertThat(
            ComponentsConfiguration.getDefaultComponentsConfiguration()
                .getUseCancelableLayoutFutures())
        .isFalse();
    assertThat(
            ComponentsConfiguration.getDefaultComponentsConfiguration()
                .getIgnoreNullLayoutStateError())
        .isTrue();
    ComponentsConfiguration.setDefaultComponentsConfigurationBuilder(mDefaultBuilder);
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
}
