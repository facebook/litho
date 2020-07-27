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

import android.graphics.Color;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.DynamicPropsComponentTester;
import com.facebook.litho.widget.SolidColor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class MountStateTest {

  public final @Rule LithoViewRule mLithoViewRule = new LithoViewRule();

  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = mLithoViewRule.getContext();
  }

  @Test
  public void testDetachLithoView_unbindComponentFromContent() {
    final Component child1 =
        DynamicPropsComponentTester.create(mContext).dynamicPropValue(1).build();

    final Component root =
        Column.create(mContext)
            .child(Wrapper.create(mContext).delegate(child1).widthPx(10).heightPx(10))
            .build();

    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY))
        .measure()
        .layout();

    final MountState mountState = mLithoViewRule.getLithoView().getMountState();
    final DynamicPropsManager dynamicPropsManager = mountState.getDynamicPropsManager();
    assertThat(dynamicPropsManager.hasCachedContent(child1)).isTrue();

    mLithoViewRule.detachFromWindow();
    assertThat(dynamicPropsManager.hasCachedContent(child1)).isFalse();
  }

  @Test
  public void testUnbindMountItem_unbindComponentFromContent() {
    final Component child1 =
        DynamicPropsComponentTester.create(mContext).dynamicPropValue(1).build();

    final Component root =
        Column.create(mContext)
            .child(Wrapper.create(mContext).delegate(child1).widthPx(10).heightPx(10))
            .build();

    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY))
        .measure()
        .layout();

    final MountState mountState = mLithoViewRule.getLithoView().getMountState();
    final DynamicPropsManager dynamicPropsManager = mountState.getDynamicPropsManager();
    assertThat(dynamicPropsManager.hasCachedContent(child1)).isTrue();

    mLithoViewRule.setRoot(Column.create(mContext).build());
    assertThat(dynamicPropsManager.hasCachedContent(child1)).isFalse();
  }

  @Test
  public void onSetRootWithNoOutputsWithRenderCore_shouldSuccessfullyCompleteMount() {
    final boolean delegateToRenderCoreMount = ComponentsConfiguration.delegateToRenderCoreMount;
    final boolean useExtensions = ComponentsConfiguration.useExtensionsWithMountDelegate;
    final boolean incrementalMountExtension = ComponentsConfiguration.useIncrementalMountExtension;

    ComponentsConfiguration.delegateToRenderCoreMount = true;
    ComponentsConfiguration.useExtensionsWithMountDelegate = true;
    ComponentsConfiguration.useIncrementalMountExtension = true;

    final Component root =
        Wrapper.create(mContext)
            .delegate(SolidColor.create(mContext).color(Color.BLACK).build())
            .build();

    mLithoViewRule
        .setRoot(root)
        .attachToWindow()
        .setSizeSpecs(makeSizeSpec(1000, EXACTLY), makeSizeSpec(1000, EXACTLY))
        .measure()
        .layout();

    final Component emptyRoot = Wrapper.create(mContext).delegate(null).build();

    mLithoViewRule.setRoot(emptyRoot);

    ComponentsConfiguration.delegateToRenderCoreMount = delegateToRenderCoreMount;
    ComponentsConfiguration.useExtensionsWithMountDelegate = useExtensions;
    ComponentsConfiguration.useIncrementalMountExtension = incrementalMountExtension;
  }
}
