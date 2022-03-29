/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.litho.stateupdates;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static org.assertj.core.api.Assertions.assertThat;

import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.NestedTreeHolderResult;
import com.facebook.litho.StateHandler;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.TestLithoView;
import com.facebook.litho.testing.Whitebox;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.LooperMode;

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner.class)
public class StateUpdatesTest {

  private ComponentContext mContext;
  private int mWidthSpec;
  private int mHeightSpec;

  public final @Rule LithoViewRule mLithoViewRule = new LithoViewRule();

  @Before
  public void setup() {
    mContext = new ComponentContext(getApplicationContext());
    mWidthSpec = makeSizeSpec(100, EXACTLY);
    mHeightSpec = makeSizeSpec(100, EXACTLY);
  }

  private TestLithoView createTestLithoView(Component component) {
    return mLithoViewRule
        .createTestLithoView()
        .setRoot(component)
        .setSizeSpecs(mWidthSpec, mHeightSpec)
        .attachToWindow()
        .measure()
        .layout();
  }

  @Test
  public void testKeepInitialStateValuesDeepHierarchy() {
    final Component component =
        Column.create(mContext).child(ComponentWithCounterStateLayout.create(mContext)).build();
    final TestLithoView testLithoView = createTestLithoView(component);
    final ComponentTree componentTree = testLithoView.getComponentTree();
    final StateHandler stateHandler = Whitebox.getInternalState(componentTree, "mStateHandler");
    final String globalKey =
        testLithoView.getCurrentRootNode().getNode().getChildAt(0).getGlobalKeyAt(1);
    ComponentWithCounterStateLayout.ComponentWithCounterStateLayoutStateContainer
        stateHandlerStateContainer =
            (ComponentWithCounterStateLayout.ComponentWithCounterStateLayoutStateContainer)
                stateHandler.mStateContainers.get(globalKey);

    ComponentWithCounterStateLayout.ComponentWithCounterStateLayoutStateContainer
        initialStateStateContainer =
            (ComponentWithCounterStateLayout.ComponentWithCounterStateLayoutStateContainer)
                componentTree.getInitialStateContainer().mInitialStates.get(globalKey);

    assertThat(stateHandlerStateContainer).isNotNull();
    assertThat(stateHandlerStateContainer.count).isEqualTo(0);
    assertThat(componentTree.getInitialStateContainer().mInitialStates.isEmpty()).isTrue();
    assertThat(initialStateStateContainer).isNull();
  }

  @Test
  public void testKeepInitialStateValuesNestedTree() {
    final Component component =
        Column.create(mContext)
            .child(ComponentWithCounterStateNestedParent.create(mContext))
            .build();
    final TestLithoView testLithoView = createTestLithoView(component);
    final ComponentTree componentTree = testLithoView.getComponentTree();
    final StateHandler stateHandler = Whitebox.getInternalState(componentTree, "mStateHandler");
    final String globalKey =
        ((NestedTreeHolderResult) testLithoView.getCurrentRootNode().getChildAt(0))
            .getNestedResult()
            .getNode()
            .getChildAt(0)
            .getGlobalKeyAt(1);

    ComponentWithCounterStateLayout.ComponentWithCounterStateLayoutStateContainer
        stateHandlerStateContainer =
            (ComponentWithCounterStateLayout.ComponentWithCounterStateLayoutStateContainer)
                stateHandler.mStateContainers.get(globalKey);

    ComponentWithCounterStateLayout.ComponentWithCounterStateLayoutStateContainer
        initialStateStateContainer =
            (ComponentWithCounterStateLayout.ComponentWithCounterStateLayoutStateContainer)
                componentTree.getInitialStateContainer().mInitialStates.get(globalKey);

    if (ComponentsConfiguration.applyStateUpdateEarly) {
      assertThat(stateHandlerStateContainer).isNull();
      assertThat(componentTree.getInitialStateContainer().mInitialStates.isEmpty()).isFalse();
      assertThat(initialStateStateContainer).isNotNull();
      assertThat(initialStateStateContainer.count).isEqualTo(0);
    } else {
      assertThat(stateHandlerStateContainer).isNotNull();
      assertThat(stateHandlerStateContainer.count).isEqualTo(0);
      assertThat(componentTree.getInitialStateContainer().mInitialStates.isEmpty()).isTrue();
      assertThat(initialStateStateContainer).isNull();
    }
  }
}
