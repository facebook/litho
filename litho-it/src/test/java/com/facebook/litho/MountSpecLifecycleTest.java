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

import static com.facebook.litho.LifecycleStep.ON_CREATE_INITIAL_STATE;
import static com.facebook.litho.LifecycleStep.getSteps;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.UNSPECIFIED;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.os.Looper;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.LithoStatsRule;
import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.MountSpecLifecycleTester;
import com.facebook.litho.widget.PreallocatedMountSpecLifecycleTester;
import com.facebook.litho.widget.RecordsShouldUpdate;
import com.facebook.litho.widget.SimpleStateUpdateEmulator;
import com.facebook.litho.widget.SimpleStateUpdateEmulatorSpec;
import com.facebook.rendercore.MountDelegate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;

@RunWith(ParameterizedRobolectricTestRunner.class)
public class MountSpecLifecycleTest {

  public final @Rule LithoViewRule mLithoViewRule = new LithoViewRule();
  public final @Rule LithoStatsRule mLithoStatsRule = new LithoStatsRule();

  final boolean mUseMountDelegateTarget;
  private boolean mConfigUseMountDelegateTarget;

  @ParameterizedRobolectricTestRunner.Parameters(name = "useMountDelegateTarget={0}")
  public static Collection data() {
    return Arrays.asList(
        new Object[][] {
          {false}, {true},
        });
  }

  public MountSpecLifecycleTest(boolean useMountDelegateTarget) {
    mUseMountDelegateTarget = useMountDelegateTarget;
  }

  @Before
  public void before() {
    mConfigUseMountDelegateTarget = ComponentsConfiguration.useExtensionsWithMountDelegate;
    ComponentsConfiguration.useExtensionsWithMountDelegate = mUseMountDelegateTarget;
  }

  @After
  public void after() {
    ComponentsConfiguration.useExtensionsWithMountDelegate = mConfigUseMountDelegateTarget;
  }

  @Test
  public void lifecycle_onSetComponentWithoutLayout_shouldNotCallLifecycleMethods() {
    final LifecycleTracker lifecycleTracker = new LifecycleTracker();
    final Component component =
        MountSpecLifecycleTester.create(mLithoViewRule.getContext())
            .lifecycleTracker(lifecycleTracker)
            .build();
    mLithoViewRule.setRoot(component);

    assertThat(lifecycleTracker.getSteps())
        .describedAs("No lifecycle methods should be called")
        .isEmpty();
  }

  @Test
  public void lifecycle_onLayout_shouldCallLifecycleMethods() {
    final LifecycleTracker lifecycleTracker = new LifecycleTracker();
    final Component component =
        MountSpecLifecycleTester.create(mLithoViewRule.getContext())
            .lifecycleTracker(lifecycleTracker)
            .intrinsicSize(new Size(800, 600))
            .build();
    mLithoViewRule.setRoot(component);

    mLithoViewRule.attachToWindow().measure().layout();

    assertThat(lifecycleTracker.getSteps())
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_ATTACHED,
            LifecycleStep.ON_CREATE_MOUNT_CONTENT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND);

    assertThat(ComponentsPools.getMountContentPools().size())
        .describedAs("Should contain only 1 content pool")
        .isEqualTo(1);
    assertThat(ComponentsPools.getMountContentPools().get(0).getName())
        .describedAs("Should contain content pool from MountSpecLifecycleTester")
        .isEqualTo("MountSpecLifecycleTester");
  }

  @Test
  public void lifecycle_onLayoutWithExactSize_shouldCallLifecycleMethodsExceptMeasure() {
    final LifecycleTracker lifecycleTracker = new LifecycleTracker();
    final Component component =
        MountSpecLifecycleTester.create(mLithoViewRule.getContext())
            .lifecycleTracker(lifecycleTracker)
            .intrinsicSize(new Size(800, 600))
            .build();
    mLithoViewRule.setRoot(component);

    mLithoViewRule.setSizePx(600, 800).attachToWindow().measure().layout();

    assertThat(lifecycleTracker.getSteps())
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_ATTACHED,
            LifecycleStep.ON_CREATE_MOUNT_CONTENT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND);
  }

  @Test
  public void lifecycle_onDetach_shouldCallLifecycleMethods() {
    final LifecycleTracker lifecycleTracker = new LifecycleTracker();
    final Component component =
        MountSpecLifecycleTester.create(mLithoViewRule.getContext())
            .intrinsicSize(new Size(800, 600))
            .lifecycleTracker(lifecycleTracker)
            .build();
    mLithoViewRule.setRoot(component);

    mLithoViewRule.attachToWindow().measure().layout();

    lifecycleTracker.reset();

    mLithoViewRule.detachFromWindow();

    assertThat(lifecycleTracker.getSteps())
        .describedAs("Should only call")
        .containsExactly(LifecycleStep.ON_UNBIND);
  }

  @Test
  public void lifecycle_onReAttach_shouldCallLifecycleMethods() {
    final LifecycleTracker lifecycleTracker = new LifecycleTracker();
    final Component component =
        MountSpecLifecycleTester.create(mLithoViewRule.getContext())
            .lifecycleTracker(lifecycleTracker)
            .intrinsicSize(new Size(800, 600))
            .build();
    mLithoViewRule.setRoot(component);

    mLithoViewRule.attachToWindow().measure().layout().detachFromWindow();

    lifecycleTracker.reset();

    mLithoViewRule.attachToWindow().measure().layout();

    assertThat(lifecycleTracker.getSteps())
        .describedAs("Should only call")
        .containsExactly(LifecycleStep.ON_BIND);
  }

  @Test
  public void lifecycle_onRemeasureWithSameSpecs_shouldNotCallLifecycleMethods() {
    final LifecycleTracker lifecycleTracker = new LifecycleTracker();
    final Component component =
        MountSpecLifecycleTester.create(mLithoViewRule.getContext())
            .intrinsicSize(new Size(800, 600))
            .lifecycleTracker(lifecycleTracker)
            .build();
    mLithoViewRule.setRoot(component);

    mLithoViewRule.attachToWindow().measure().layout();

    lifecycleTracker.reset();

    mLithoViewRule.measure();

    assertThat(lifecycleTracker.getSteps())
        .describedAs("No lifecycle methods should be called")
        .isEmpty();
  }

  @Test
  public void lifecycle_onRemeasureWithDifferentSpecs_shouldCallLifecycleMethods() {
    final LifecycleTracker lifecycleTracker = new LifecycleTracker();
    final Component component =
        MountSpecLifecycleTester.create(mLithoViewRule.getContext())
            .intrinsicSize(new Size(800, 600))
            .lifecycleTracker(lifecycleTracker)
            .build();
    mLithoViewRule.setRoot(component);

    mLithoViewRule.attachToWindow().measure().layout();

    lifecycleTracker.reset();

    mLithoViewRule
        .setSizeSpecs(makeSizeSpec(800, EXACTLY), makeSizeSpec(600, UNSPECIFIED))
        .measure();

    assertThat(lifecycleTracker.getSteps())
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED);
  }

  @Test
  public void lifecycle_onRemeasureWithExactSize_shouldNotCallLifecycleMethods() {
    final LifecycleTracker lifecycleTracker = new LifecycleTracker();
    final Component component =
        MountSpecLifecycleTester.create(mLithoViewRule.getContext())
            .lifecycleTracker(lifecycleTracker)
            .intrinsicSize(new Size(800, 600))
            .build();
    mLithoViewRule.setRoot(component);

    mLithoViewRule.attachToWindow().measure().layout();

    lifecycleTracker.reset();

    mLithoViewRule.setSizePx(800, 600).measure();

    assertThat(lifecycleTracker.getSteps())
        .describedAs(
            "No lifecycle methods should be called because EXACT measures should skip layout calculation.")
        .isEmpty();
  }

  @Test
  public void lifecycle_onReLayoutAfterMeasureWithExactSize_shouldCallLifecycleMethods() {
    final LifecycleTracker lifecycleTracker = new LifecycleTracker();
    final Component component =
        MountSpecLifecycleTester.create(mLithoViewRule.getContext())
            .lifecycleTracker(lifecycleTracker)
            .intrinsicSize(new Size(800, 600))
            .build();
    mLithoViewRule.setRoot(component);

    mLithoViewRule.attachToWindow().measure().layout();

    lifecycleTracker.reset();

    mLithoViewRule.setSizePx(800, 600).measure().layout();

    assertThat(lifecycleTracker.getSteps())
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND);
  }

  @Test
  public void lifecycle_onReLayoutAfterMeasureWithExactSizeAsNonRoot_shouldCallLifecycleMethods() {
    final LifecycleTracker lifecycleTracker = new LifecycleTracker();
    final Component component =
        MountSpecLifecycleTester.create(mLithoViewRule.getContext())
            .intrinsicSize(new Size(800, 600))
            .lifecycleTracker(lifecycleTracker)
            .build();
    mLithoViewRule.setRoot(Column.create(mLithoViewRule.getContext()).child(component).build());

    mLithoViewRule.attachToWindow().measure().layout();

    lifecycleTracker.reset();

    mLithoViewRule.setSizePx(800, 600).measure().layout();

    assertThat(lifecycleTracker.getSteps())
        .describedAs("Should call the lifecycle methods in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND);
  }

  @Test
  public void lifecycle_onSetShallowCopy_shouldNotCallLifecycleMethods() {
    final LifecycleTracker lifecycleTracker = new LifecycleTracker();
    final Component component =
        MountSpecLifecycleTester.create(mLithoViewRule.getContext())
            .intrinsicSize(new Size(800, 600))
            .lifecycleTracker(lifecycleTracker)
            .build();
    mLithoViewRule.setRoot(component);

    mLithoViewRule.attachToWindow().measure().layout();

    lifecycleTracker.reset();

    mLithoViewRule.setRoot(component.makeShallowCopy());

    assertThat(lifecycleTracker.getSteps())
        .describedAs("No lifecycle methods should be called")
        .isEmpty();
  }

  @Test
  public void lifecycle_onRemeasureWithCompatibleSpecs_shouldNotRemount() {
    final LifecycleTracker lifecycleTracker = new LifecycleTracker();
    final Component component =
        MountSpecLifecycleTester.create(mLithoViewRule.getContext())
            .intrinsicSize(new Size(800, 600))
            .lifecycleTracker(lifecycleTracker)
            .build();

    mLithoViewRule.setRoot(component).measure().layout().attachToWindow();

    lifecycleTracker.reset();

    // Force measure call to propagate to ComponentTree
    mLithoViewRule.getLithoView().requestLayout();
    mLithoViewRule.measure().layout();

    assertThat(lifecycleTracker.getSteps())
        .describedAs("No lifecycle methods should be called because measure was compatible")
        .isEmpty();
  }

  @Test
  public void lifecycle_onSetSemanticallySimilarComponent_shouldCallLifecycleMethods() {
    final LifecycleTracker lifecycleTracker = new LifecycleTracker();
    final Component component =
        MountSpecLifecycleTester.create(mLithoViewRule.getContext())
            .intrinsicSize(new Size(800, 600))
            .lifecycleTracker(lifecycleTracker)
            .build();
    mLithoViewRule.setRoot(component);

    mLithoViewRule.attachToWindow().measure().layout();

    lifecycleTracker.reset();

    final LifecycleTracker newLifecycleTracker = new LifecycleTracker();
    mLithoViewRule.setRoot(
        MountSpecLifecycleTester.create(mLithoViewRule.getContext())
            .intrinsicSize(new Size(800, 600))
            .lifecycleTracker(newLifecycleTracker)
            .build());

    mLithoViewRule.measure().layout();

    assertThat(lifecycleTracker.getSteps())
        .describedAs("Should call the lifecycle methods on old instance in expected order")
        .containsExactly(LifecycleStep.ON_UNBIND, LifecycleStep.ON_UNMOUNT);

    assertThat(newLifecycleTracker.getSteps())
        .describedAs("Should call the lifecycle methods on new instance in expected order")
        .containsExactly(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND);

    assertThat(ComponentsPools.getMountContentPools().size())
        .describedAs("Should still contain only 1 content pool")
        .isEqualTo(1);
    assertThat(ComponentsPools.getMountContentPools().get(0).getName())
        .describedAs("Should still contain content pool from MountSpecLifecycleTester")
        .isEqualTo("MountSpecLifecycleTester");
  }

  @Test
  public void onSetRootWithPreallocatedMountContent_shouldCallLifecycleMethods() {
    final Looper looper = ShadowLooper.getLooperForThread(Thread.currentThread());
    final ComponentTree tree =
        ComponentTree.create(mLithoViewRule.getContext())
            .shouldPreallocateMountContentPerMountSpec(true)
            .preAllocateMountContentHandler(new LithoHandler.DefaultLithoHandler(looper))
            .build();
    mLithoViewRule.useComponentTree(tree);

    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final Component component =
        PreallocatedMountSpecLifecycleTester.create(mLithoViewRule.getContext())
            .steps(info)
            .build();

    mLithoViewRule
        .getComponentTree()
        .setRootAndSizeSpec(
            component, mLithoViewRule.getWidthSpec(), mLithoViewRule.getHeightSpec());

    mLithoViewRule.measure();

    ShadowLooper.runUiThreadTasks();

    assertThat(getSteps(info))
        .describedAs("Should call the lifecycle methods on new instance in expected order")
        .containsExactly(
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_ATTACHED);

    assertThat(ComponentsPools.getMountContentPools().size())
        .describedAs("Should contain only 1 content pool")
        .isEqualTo(1);
    assertThat(ComponentsPools.getMountContentPools().get(0).getName())
        .describedAs("Should contain content pool from PreallocatedMountSpecLifecycleTester")
        .isEqualTo("PreallocatedMountSpecLifecycleTester");
  }

  @Test
  public void shouldUpdate_shouldUpdateIsCalled_prevAndNextAreInRightOrder() {
    final Object firstObject = new Object();
    final List<Diff<Object>> shouldUpdateCalls = new ArrayList<>();
    mLithoViewRule
        .setRoot(
            RecordsShouldUpdate.create(mLithoViewRule.getContext())
                .shouldUpdateCalls(shouldUpdateCalls)
                .testProp(firstObject)
                .build())
        .measure()
        .layout()
        .attachToWindow();

    final Object secondObject = new Object();
    mLithoViewRule
        .setRoot(
            RecordsShouldUpdate.create(mLithoViewRule.getContext())
                .shouldUpdateCalls(shouldUpdateCalls)
                .testProp(secondObject)
                .build())
        .measure()
        .layout();

    assertThat(shouldUpdateCalls).hasSize(1);
    assertThat(shouldUpdateCalls.get(0).getPrevious()).isEqualTo(firstObject);
    assertThat(shouldUpdateCalls.get(0).getNext()).isEqualTo(secondObject);
  }

  @Test
  public void unmountAll_unmountAllItemsExceptRoot() {
    final LifecycleTracker info_child1 = new LifecycleTracker();
    final LifecycleTracker info_child2 = new LifecycleTracker();

    final Component root =
        Column.create(mLithoViewRule.getContext())
            .child(
                MountSpecLifecycleTester.create(mLithoViewRule.getContext())
                    .intrinsicSize(new Size(800, 600))
                    .lifecycleTracker(info_child1)
                    .build())
            .child(
                MountSpecLifecycleTester.create(mLithoViewRule.getContext())
                    .intrinsicSize(new Size(800, 600))
                    .lifecycleTracker(info_child2)
                    .build())
            .build();

    mLithoViewRule.setRoot(root).attachToWindow().measure().layout();

    final MountDelegate.MountDelegateTarget mountDelegateTarget =
        mLithoViewRule.getLithoView().getMountDelegateTarget();
    assertThat(mountDelegateTarget.getMountItemCount()).isGreaterThan(1);
    assertThat(mountDelegateTarget.getMountItemAt(1)).isNotNull();
    assertThat(mountDelegateTarget.getMountItemAt(2)).isNotNull();

    info_child1.reset();
    info_child2.reset();
    mLithoViewRule.getLithoView().unmountAllItems();

    assertThat(mountDelegateTarget.getMountItemAt(1)).isNull();
    assertThat(mountDelegateTarget.getMountItemAt(2)).isNull();
    assertThat(mountDelegateTarget.getMountItemAt(0)).isNotNull();

    assertThat(info_child1.getSteps())
        .describedAs("Should call the following lifecycle methods in the following order:")
        .containsExactly(LifecycleStep.ON_UNBIND, LifecycleStep.ON_UNMOUNT);

    assertThat(info_child2.getSteps())
        .describedAs("Should call the following lifecycle methods in the following order:")
        .containsExactly(LifecycleStep.ON_UNBIND, LifecycleStep.ON_UNMOUNT);
  }

  /*
   * This case comes from a specific bug where when we shallow-copy components (which we do when we
   * update state) we were setting mHasManualKey to false even if there was a manual key which would
   * cause us to generate different keys between layouts.
   */
  @Test
  public void
      lifecycle_stateUpdateWithMultipleChildrenOfSameTypeAndManualKeys_doesNotRecreateInitialState() {
    final LifecycleTracker info_child1 = new LifecycleTracker();
    final LifecycleTracker info_child2 = new LifecycleTracker();
    final SimpleStateUpdateEmulatorSpec.Caller stateUpdater =
        new SimpleStateUpdateEmulatorSpec.Caller();

    final Component root =
        Column.create(mLithoViewRule.getContext())
            .child(
                MountSpecLifecycleTester.create(mLithoViewRule.getContext())
                    .intrinsicSize(new Size(800, 600))
                    .lifecycleTracker(info_child1)
                    .key("some_key"))
            .child(
                MountSpecLifecycleTester.create(mLithoViewRule.getContext())
                    .intrinsicSize(new Size(800, 600))
                    .lifecycleTracker(info_child2)
                    .key("other_key"))
            .child(
                SimpleStateUpdateEmulator.create(mLithoViewRule.getContext()).caller(stateUpdater))
            .build();

    mLithoViewRule.useComponentTree(
        ComponentTree.create(mLithoViewRule.getContext()).isReconciliationEnabled(false).build());
    mLithoViewRule.setRoot(root).attachToWindow().measure().layout();

    final MountDelegate.MountDelegateTarget mountDelegateTarget =
        mLithoViewRule.getLithoView().getMountDelegateTarget();
    assertThat(mountDelegateTarget.getMountItemCount()).isGreaterThan(1);

    info_child1.reset();
    info_child2.reset();

    stateUpdater.increment();

    assertThat(info_child1.getSteps())
        .describedAs("Should not recreate initial state.")
        .doesNotContain(ON_CREATE_INITIAL_STATE);

    assertThat(info_child2.getSteps())
        .describedAs("Should not recreate initial state.")
        .doesNotContain(ON_CREATE_INITIAL_STATE);
  }
}
