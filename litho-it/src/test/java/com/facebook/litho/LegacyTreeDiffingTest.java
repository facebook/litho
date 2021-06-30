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

import static android.R.drawable.btn_default;
import static android.graphics.Color.TRANSPARENT;
import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.facebook.litho.Column.create;
import static com.facebook.litho.LayoutOutput.STATE_DIRTY;
import static com.facebook.litho.LayoutOutput.STATE_UNKNOWN;
import static com.facebook.litho.LayoutOutput.STATE_UPDATED;
import static com.facebook.litho.SizeSpec.makeSizeSpec;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import com.facebook.litho.config.TempComponentsConfigurations;
import com.facebook.litho.drawable.ComparableColorDrawable;
import com.facebook.litho.testing.TestDrawableComponent;
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec;
import com.facebook.litho.testing.testrunner.LithoTestRunner;

import static com.facebook.litho.LayoutOutput.getLayoutOutput;
import static org.assertj.core.api.Java6Assertions.assertThat;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(LithoTestRunner.class)
public class LegacyTreeDiffingTest {
  private ComponentContext mContext;
  private static Drawable sRedDrawable;
  private static Drawable sBlackDrawable;
  private static Drawable sTransparentDrawable;

  @Before
  public void setup() throws Exception {
    TempComponentsConfigurations.setShouldDisableDrawableOutputs(false);
    mContext = new ComponentContext(getApplicationContext());
    sRedDrawable = ComparableColorDrawable.create(Color.RED);
    sBlackDrawable = ComparableColorDrawable.create(Color.BLACK);
    sTransparentDrawable = ComparableColorDrawable.create(TRANSPARENT);
  }

  @Test
  public void testLayoutOutputUpdateStateWithBackground() {
    final Component component1 = new TestLayoutSpecBgState(false);
    final Component component2 = new TestLayoutSpecBgState(false);
    final Component component3 = new TestLayoutSpecBgState(true);

    LithoView lithoView = new LithoView(mContext);
    ComponentTree componentTree = ComponentTree.create(mContext, component1).build();
    lithoView.setComponentTree(componentTree);
    lithoView.onAttachedToWindow();

    componentTree.setRootAndSizeSpec(
        component1, makeSizeSpec(10, SizeSpec.EXACTLY), makeSizeSpec(10, SizeSpec.EXACTLY));
    LayoutState state = componentTree.getMainThreadLayoutState();

    assertOutputsState(state, STATE_UNKNOWN);

    componentTree.setRoot(component2);
    LayoutState secondState = componentTree.getMainThreadLayoutState();

    assertThat(5).isEqualTo(secondState.getMountableOutputCount());
    assertOutputsState(secondState, STATE_UPDATED);

    componentTree.setRoot(component3);
    LayoutState thirdState = componentTree.getMainThreadLayoutState();

    assertThat(5).isEqualTo(thirdState.getMountableOutputCount());
    assertThat(getLayoutOutput(thirdState.getMountableOutputAt(1)).getUpdateState())
        .isEqualTo(STATE_DIRTY);
    assertThat(getLayoutOutput(thirdState.getMountableOutputAt(2)).getUpdateState())
        .isEqualTo(STATE_UPDATED);
    assertThat(getLayoutOutput(thirdState.getMountableOutputAt(3)).getUpdateState())
        .isEqualTo(STATE_UPDATED);
    assertThat(getLayoutOutput(thirdState.getMountableOutputAt(4)).getUpdateState())
        .isEqualTo(STATE_UPDATED);
  }

  // This test covers the same case with the foreground since the code path is the same!
  @Test
  public void testLayoutOutputUpdateStateWithBackgroundInWithLayout() {
    final Component component1 = new TestLayoutSpecInnerState(false);
    final Component component2 = new TestLayoutSpecInnerState(false);
    final Component component3 = new TestLayoutSpecInnerState(true);

    LithoView lithoView = new LithoView(mContext);
    ComponentTree componentTree = ComponentTree.create(mContext, component1).build();
    lithoView.setComponentTree(componentTree);
    lithoView.onAttachedToWindow();

    componentTree.setRootAndSizeSpec(
        component1, makeSizeSpec(10, SizeSpec.EXACTLY), makeSizeSpec(10, SizeSpec.EXACTLY));
    LayoutState state = componentTree.getMainThreadLayoutState();

    assertThat(getLayoutOutput(state.getMountableOutputAt(2)).getUpdateState())
        .isEqualTo(STATE_UNKNOWN);

    componentTree.setRoot(component2);
    LayoutState secondState = componentTree.getMainThreadLayoutState();

    assertThat(getLayoutOutput(secondState.getMountableOutputAt(2)).getUpdateState())
        .isEqualTo(STATE_UPDATED);

    componentTree.setRoot(component3);
    LayoutState thirdState = componentTree.getMainThreadLayoutState();

    assertThat(getLayoutOutput(thirdState.getMountableOutputAt(2)).getUpdateState())
        .isEqualTo(STATE_DIRTY);
  }

  private static void assertOutputsState(
      LayoutState layoutState, @LayoutOutput.UpdateState int state) {
    assertThat(STATE_DIRTY)
        .isEqualTo(getLayoutOutput(layoutState.getMountableOutputAt(0)).getUpdateState());
    for (int i = 1; i < layoutState.getMountableOutputCount(); i++) {
      LayoutOutput output = getLayoutOutput(layoutState.getMountableOutputAt(i));
      assertThat(state).isEqualTo(output.getUpdateState());
    }
  }

  private static class TestLayoutSpecBgState extends InlineLayoutSpec {
    private final boolean mChangeBg;

    TestLayoutSpecBgState(boolean changeBg) {
      super();
      mChangeBg = changeBg;
    }

    @Override
    protected Component onCreateLayout(ComponentContext c) {
      return create(c)
          .background(mChangeBg ? sBlackDrawable : sRedDrawable)
          .foreground(sTransparentDrawable)
          .child(TestDrawableComponent.create(c))
          .child(create(c).child(TestDrawableComponent.create(c)))
          .build();
    }
  }

  private static class TestLayoutSpecInnerState extends InlineLayoutSpec {
    private final boolean mChangeChildDrawable;

    TestLayoutSpecInnerState(boolean changeChildDrawable) {
      super();
      mChangeChildDrawable = changeChildDrawable;
    }

    @Override
    protected Component onCreateLayout(ComponentContext c) {
      return create(c)
          .background(sRedDrawable)
          .foregroundRes(btn_default)
          .child(
              TestDrawableComponent.create(c)
                  .background(mChangeChildDrawable ? sRedDrawable : sBlackDrawable))
          .child(create(c).child(TestDrawableComponent.create(c)))
          .build();
    }
  }

  @After
  public void restoreConfiguration() {
    TempComponentsConfigurations.restoreShouldDisableDrawableOutputs();
  }
}
