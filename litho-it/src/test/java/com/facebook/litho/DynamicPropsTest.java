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

import static com.facebook.litho.testing.TestDrawableComponent.create;
import static com.facebook.litho.testing.helper.ComponentTestHelper.mountComponent;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.graphics.Color;
import android.view.View;
import com.facebook.litho.testing.TestViewComponent;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.testing.util.InlineLayoutSpec;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(ComponentsTestRunner.class)
public class DynamicPropsTest {
  private ComponentContext mContext;

  @Before
  public void setup() {
    mContext = new ComponentContext(RuntimeEnvironment.application);
  }

  @Test
  public void testLayoutSpecComponent() {
    final float startValue = 0.8f;
    final DynamicValue<Float> alphaDV = new DynamicValue<>(startValue);
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .widthPx(100)
                    .widthPx(100)
                    .child(
                        Column.create(c)
                            .widthPx(80)
                            .heightPx(80)
                            .backgroundColor(Color.WHITE)
                            .alpha(alphaDV))
                    .build();
              }
            });

    // The Column component with dynamic alpha should've gotten a HostComponent, thus there should
    // be a mounted ComponentHost, which gets bound to the DynamicValue
    assertThat(lithoView.getChildCount()).isEqualTo(1);
    assertThat(lithoView.getChildAt(0)).isInstanceOf(ComponentHost.class);

    final ComponentHost host = (ComponentHost) lithoView.getChildAt(0);

    assertThat(host.getAlpha()).isEqualTo(startValue);

    alphaDV.set(0.5f);
    assertThat(host.getAlpha()).isEqualTo(0.5f);

    alphaDV.set(0.f);
    assertThat(host.getAlpha()).isEqualTo(0.f);

    alphaDV.set(1.f);
    assertThat(host.getAlpha()).isEqualTo(1.f);
  }

  @Test
  public void testMountDrawableComponent() {
    final float startValue = 100;
    final DynamicValue<Float> translationDV = new DynamicValue<>(startValue);
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .widthPx(100)
                    .widthPx(100)
                    .child(create(c).widthPx(80).heightPx(80).alpha(translationDV))
                    .build();
              }
            });

    // The TestDrawableComponent with dynamic alpha should've gotten a HostComponent, thus there
    // should be a mounted ComponentHost, which gets bound to the DynamicValue
    assertThat(lithoView.getChildCount()).isEqualTo(1);
    assertThat(lithoView.getChildAt(0)).isInstanceOf(ComponentHost.class);

    final ComponentHost host = (ComponentHost) lithoView.getChildAt(0);

    assertThat(host.getAlpha()).isEqualTo(startValue);

    translationDV.set(50.f);
    assertThat(host.getAlpha()).isEqualTo(50.f);

    translationDV.set(-100.f);
    assertThat(host.getAlpha()).isEqualTo(-100.f);

    translationDV.set(0.f);
    assertThat(host.getAlpha()).isEqualTo(0.f);
  }

  @Test
  public void testMountViewComponent() {
    final float startValue = 1.5f;
    final DynamicValue<Float> scaleDV = new DynamicValue<>(startValue);
    final LithoView lithoView =
        mountComponent(
            mContext,
            new InlineLayoutSpec() {
              @Override
              protected Component onCreateLayout(ComponentContext c) {
                return Column.create(c)
                    .widthPx(100)
                    .widthPx(100)
                    .child(
                        TestViewComponent.create(c)
                            .widthPx(80)
                            .heightPx(80)
                            .scaleX(scaleDV)
                            .scaleY(scaleDV))
                    .build();
              }
            });

    assertThat(lithoView.getChildCount()).isEqualTo(1);

    final View view = lithoView.getChildAt(0);

    assertThat(view.getScaleX()).isEqualTo(startValue);
    assertThat(view.getScaleY()).isEqualTo(startValue);

    scaleDV.set(0.5f);
    assertThat(view.getScaleX()).isEqualTo(0.5f);
    assertThat(view.getScaleY()).isEqualTo(0.5f);

    scaleDV.set(2.f);
    assertThat(view.getScaleX()).isEqualTo(2.f);
    assertThat(view.getScaleY()).isEqualTo(2.f);

    scaleDV.set(1.f);
    assertThat(view.getScaleX()).isEqualTo(1.f);
    assertThat(view.getScaleY()).isEqualTo(1.f);
  }
}
