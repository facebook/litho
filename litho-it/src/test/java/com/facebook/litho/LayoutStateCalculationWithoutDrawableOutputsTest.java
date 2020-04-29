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

import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.UNSPECIFIED;
import static android.view.View.MeasureSpec.makeMeasureSpec;
import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.facebook.litho.LayoutOutput.getLayoutOutput;
import static org.assertj.core.api.Java6Assertions.assertThat;

import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.testrunner.ComponentsTestRunner;
import com.facebook.litho.widget.Text;
import com.facebook.litho.widget.layoutstate.withoutdrawableoutput.RootComponent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ComponentsTestRunner.class)
public class LayoutStateCalculationWithoutDrawableOutputsTest {

  private ComponentContext mContext;
  private ComponentTree mComponentTree;
  private LithoView mLithoView;
  private boolean cachedConfigValue;

  @Before
  public void before() {
    cachedConfigValue = ComponentsConfiguration.shouldDisableDrawableOutputs;
    mContext = new ComponentContext(getApplicationContext());
    mLithoView = new LithoView(mContext);
    mComponentTree = ComponentTree.create(mContext).build();
    mLithoView.setComponentTree(mComponentTree);
  }

  @After
  public void after() {
    ComponentsConfiguration.shouldDisableDrawableOutputs = cachedConfigValue;
  }

  @Test
  public void
      whenDrawableOutputsEnabledAndChildrenNotWrappedInView_shouldHaveExplicitDrawableOutputsForBackgroundAndForeground() {
    LayoutState state;
    LayoutOutput output;

    attach();

    ComponentsConfiguration.shouldDisableDrawableOutputs = false;

    mLithoView.setComponent(RootComponent.create(mContext).shouldWrapInView(false).build());
    state = mLithoView.getComponentTree().getMainThreadLayoutState();

    assertThat(state.getMountableOutputCount()).isEqualTo(7);

    output = getLayoutOutput(state.getMountableOutputAt(0)); // root host view
    assertThat(output.getComponent()).isOfAnyClassIn(HostComponent.class);
    if (output.getViewNodeInfo() != null) {
      assertThat(output.getViewNodeInfo().getBackground()).isNull();
    }

    output = getLayoutOutput(state.getMountableOutputAt(1)); // background 1
    assertThat(output.getComponent()).isOfAnyClassIn(DrawableComponent.class);

    output = getLayoutOutput(state.getMountableOutputAt(2)); // text 1
    assertThat(output.getComponent()).isOfAnyClassIn(Text.class);

    output = getLayoutOutput(state.getMountableOutputAt(3)); // foreground 1
    assertThat(output.getComponent()).isOfAnyClassIn(DrawableComponent.class);

    output = getLayoutOutput(state.getMountableOutputAt(4)); // background 2
    assertThat(output.getComponent()).isOfAnyClassIn(DrawableComponent.class);

    output = getLayoutOutput(state.getMountableOutputAt(5)); // text 2
    assertThat(output.getComponent()).isOfAnyClassIn(Text.class);

    output = getLayoutOutput(state.getMountableOutputAt(6)); // foreground 2
    assertThat(output.getComponent()).isOfAnyClassIn(DrawableComponent.class);
  }

  @Test
  public void
      whenDrawableOutputsEnabledAndChildrenWrappedInView_shouldHaveExplicitDrawableOutputsForBackgroundAndForeground() {
    LayoutState state;
    LayoutOutput output;

    attach();

    ComponentsConfiguration.shouldDisableDrawableOutputs = false;

    mLithoView.setComponent(RootComponent.create(mContext).shouldWrapInView(true).build());
    state = mLithoView.getComponentTree().getMainThreadLayoutState();

    assertThat(state.getMountableOutputCount()).isEqualTo(9);

    output = getLayoutOutput(state.getMountableOutputAt(0)); // root host view
    assertThat(output.getComponent()).isOfAnyClassIn(HostComponent.class);
    if (output.getViewNodeInfo() != null) {
      assertThat(output.getViewNodeInfo().getBackground()).isNull();
    }

    output = getLayoutOutput(state.getMountableOutputAt(1)); // host view 1
    assertThat(output.getComponent()).isOfAnyClassIn(HostComponent.class);
    if (output.getViewNodeInfo() != null) {
      assertThat(output.getViewNodeInfo().getBackground()).isNull();
    }

    output = getLayoutOutput(state.getMountableOutputAt(2)); // background 1
    assertThat(output.getComponent()).isOfAnyClassIn(DrawableComponent.class);

    output = getLayoutOutput(state.getMountableOutputAt(3)); // text 1
    assertThat(output.getComponent()).isOfAnyClassIn(Text.class);

    output = getLayoutOutput(state.getMountableOutputAt(4)); // foreground 1
    assertThat(output.getComponent()).isOfAnyClassIn(DrawableComponent.class);

    output = getLayoutOutput(state.getMountableOutputAt(5)); // host view 2
    assertThat(output.getComponent()).isOfAnyClassIn(HostComponent.class);
    if (output.getViewNodeInfo() != null) {
      assertThat(output.getViewNodeInfo().getBackground()).isNull();
    }

    output = getLayoutOutput(state.getMountableOutputAt(6)); // background 2
    assertThat(output.getComponent()).isOfAnyClassIn(DrawableComponent.class);

    output = getLayoutOutput(state.getMountableOutputAt(7)); // text 2
    assertThat(output.getComponent()).isOfAnyClassIn(Text.class);

    output = getLayoutOutput(state.getMountableOutputAt(8)); // foreground 2
    assertThat(output.getComponent()).isOfAnyClassIn(DrawableComponent.class);
  }

  @Test
  public void
      whenDrawableOutputsDisabledAndChildrenNotWrappedInView_shouldNotHaveDrawableOutputsForBackgroundAndForeground() {
    LayoutState state;
    LayoutOutput output;

    attach();

    // disable layout outputs for drawables
    ComponentsConfiguration.shouldDisableDrawableOutputs = true;

    mLithoView.setComponent(RootComponent.create(mContext).shouldWrapInView(false).build());
    state = mLithoView.getComponentTree().getMainThreadLayoutState();
    assertThat(state.getMountableOutputCount()).isEqualTo(5); // 2 bg and fg lesser.

    output = getLayoutOutput(state.getMountableOutputAt(1));
    assertThat(output.getComponent()).isOfAnyClassIn(HostComponent.class);
    assertThat(output.getViewNodeInfo().getBackground()).isNotNull();

    output = getLayoutOutput(state.getMountableOutputAt(2));
    assertThat(output.getComponent()).isOfAnyClassIn(Text.class);

    output = getLayoutOutput(state.getMountableOutputAt(3));
    assertThat(output.getComponent()).isOfAnyClassIn(HostComponent.class);
    assertThat(output.getViewNodeInfo().getBackground()).isNotNull();

    output = getLayoutOutput(state.getMountableOutputAt(4));
    assertThat(output.getComponent()).isOfAnyClassIn(Text.class);

    ComponentsConfiguration.shouldDisableDrawableOutputs = false;
  }

  @Test
  public void
      whenDrawableOutputsDisabledAndChildrenWrappedInView_shouldNotHaveDrawableOutputsForBackgroundAndForeground() {
    LayoutState state;
    LayoutOutput output;

    attach();

    // disable layout outputs for drawables
    ComponentsConfiguration.shouldDisableDrawableOutputs = true;

    mLithoView.setComponent(RootComponent.create(mContext).shouldWrapInView(true).build());
    state = mLithoView.getComponentTree().getMainThreadLayoutState();
    assertThat(state.getMountableOutputCount()).isEqualTo(5); // 2 bg and fg lesser.

    output = getLayoutOutput(state.getMountableOutputAt(1));
    assertThat(output.getComponent()).isOfAnyClassIn(HostComponent.class);
    assertThat(output.getViewNodeInfo().getBackground()).isNotNull();

    output = getLayoutOutput(state.getMountableOutputAt(2));
    assertThat(output.getComponent()).isOfAnyClassIn(Text.class);

    output = getLayoutOutput(state.getMountableOutputAt(3));
    assertThat(output.getComponent()).isOfAnyClassIn(HostComponent.class);
    assertThat(output.getViewNodeInfo().getBackground()).isNotNull();

    output = getLayoutOutput(state.getMountableOutputAt(4));
    assertThat(output.getComponent()).isOfAnyClassIn(Text.class);

    ComponentsConfiguration.shouldDisableDrawableOutputs = false;
  }

  private void attach() {
    mLithoView.onAttachedToWindow();
    mLithoView.measure(makeMeasureSpec(1080, EXACTLY), makeMeasureSpec(1920, UNSPECIFIED));
    mLithoView.layout(0, 0, 1080, 1920);
    mLithoView.notifyVisibleBoundsChanged();
  }
}
