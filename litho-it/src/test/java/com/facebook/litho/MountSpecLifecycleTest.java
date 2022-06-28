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

package com.facebook.litho;

import static com.facebook.litho.LifecycleStep.ON_CREATE_INITIAL_STATE;
import static com.facebook.litho.LifecycleStep.getSteps;
import static com.facebook.litho.SizeSpec.EXACTLY;
import static com.facebook.litho.SizeSpec.UNSPECIFIED;
import static com.facebook.litho.SizeSpec.makeSizeSpec;
import static com.facebook.rendercore.utils.MeasureSpecUtils.exactly;
import static org.assertj.core.api.Assertions.assertThat;

import android.os.Looper;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.testing.LegacyLithoViewRule;
import com.facebook.litho.testing.LithoStatsRule;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.MountSpecLifecycleTester;
import com.facebook.litho.widget.MountSpecWithMountUnmountAssertion;
import com.facebook.litho.widget.MountSpecWithMountUnmountAssertionSpec;
import com.facebook.litho.widget.PreallocatedMountSpecLifecycleTester;
import com.facebook.litho.widget.RecordsShouldUpdate;
import com.facebook.litho.widget.SimpleStateUpdateEmulator;
import com.facebook.litho.widget.SimpleStateUpdateEmulatorSpec;
import com.facebook.litho.widget.Text;
import com.facebook.rendercore.MountDelegateTarget;
import com.facebook.rendercore.MountItemsPool;
import com.facebook.rendercore.RunnableHandler;
import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner.class)
public class MountSpecLifecycleTest {

  public final @Rule LegacyLithoViewRule mLegacyLithoViewRule = new LegacyLithoViewRule();
  public final @Rule LithoStatsRule mLithoStatsRule = new LithoStatsRule();
  public final @Rule ExpectedException mExpectedException = ExpectedException.none();

  @Test
  public void lifecycle_onSetComponentWithoutLayout_shouldNotCallLifecycleMethods() {
    final LifecycleTracker lifecycleTracker = new LifecycleTracker();
    final Component component =
        MountSpecLifecycleTester.create(mLegacyLithoViewRule.getContext())
            .lifecycleTracker(lifecycleTracker)
            .build();
    mLegacyLithoViewRule.setRoot(component);

    assertThat(lifecycleTracker.getSteps())
        .describedAs("No lifecycle methods should be called")
        .isEmpty();
  }

  @Test
  public void lifecycle_onLayout_shouldCallLifecycleMethods() {
    final LifecycleTracker lifecycleTracker = new LifecycleTracker();
    final Component component =
        MountSpecLifecycleTester.create(mLegacyLithoViewRule.getContext())
            .lifecycleTracker(lifecycleTracker)
            .intrinsicSize(new Size(800, 600))
            .build();
    mLegacyLithoViewRule.setRoot(component);

    mLegacyLithoViewRule.attachToWindow().measure().layout();

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
  }

  @Test
  public void lifecycle_onLayoutWithExactSize_shouldCallLifecycleMethodsExceptMeasure() {
    final LifecycleTracker lifecycleTracker = new LifecycleTracker();
    final Component component =
        MountSpecLifecycleTester.create(mLegacyLithoViewRule.getContext())
            .lifecycleTracker(lifecycleTracker)
            .intrinsicSize(new Size(800, 600))
            .build();
    mLegacyLithoViewRule.setRoot(component);

    mLegacyLithoViewRule.setSizePx(600, 800).attachToWindow().measure().layout();

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
        MountSpecLifecycleTester.create(mLegacyLithoViewRule.getContext())
            .intrinsicSize(new Size(800, 600))
            .lifecycleTracker(lifecycleTracker)
            .build();
    mLegacyLithoViewRule.setRoot(component);

    mLegacyLithoViewRule.attachToWindow().measure().layout();

    lifecycleTracker.reset();

    mLegacyLithoViewRule.detachFromWindow();

    assertThat(lifecycleTracker.getSteps())
        .describedAs("Should only call")
        .containsExactly(LifecycleStep.ON_UNBIND);
  }

  @Test
  public void lifecycle_onReAttach_shouldCallLifecycleMethods() {
    final LifecycleTracker lifecycleTracker = new LifecycleTracker();
    final Component component =
        MountSpecLifecycleTester.create(mLegacyLithoViewRule.getContext())
            .lifecycleTracker(lifecycleTracker)
            .intrinsicSize(new Size(800, 600))
            .build();
    mLegacyLithoViewRule.setRoot(component);

    mLegacyLithoViewRule.attachToWindow().measure().layout().detachFromWindow();

    lifecycleTracker.reset();

    mLegacyLithoViewRule.attachToWindow().measure().layout();

    assertThat(lifecycleTracker.getSteps())
        .describedAs("Should only call")
        .containsExactly(LifecycleStep.ON_BIND);
  }

  @Test
  public void lifecycle_onRemeasureWithSameSpecs_shouldNotCallLifecycleMethods() {
    final LifecycleTracker lifecycleTracker = new LifecycleTracker();
    final Component component =
        MountSpecLifecycleTester.create(mLegacyLithoViewRule.getContext())
            .intrinsicSize(new Size(800, 600))
            .lifecycleTracker(lifecycleTracker)
            .build();
    mLegacyLithoViewRule.setRoot(component);

    mLegacyLithoViewRule.attachToWindow().measure().layout();

    lifecycleTracker.reset();

    mLegacyLithoViewRule.measure();

    assertThat(lifecycleTracker.getSteps())
        .describedAs("No lifecycle methods should be called")
        .isEmpty();
  }

  @Test
  public void lifecycle_onRemeasureWithDifferentSpecs_shouldCallLifecycleMethods() {
    final LifecycleTracker lifecycleTracker = new LifecycleTracker();
    final Component component =
        MountSpecLifecycleTester.create(mLegacyLithoViewRule.getContext())
            .intrinsicSize(new Size(800, 600))
            .lifecycleTracker(lifecycleTracker)
            .build();
    mLegacyLithoViewRule.setRoot(component);

    mLegacyLithoViewRule.attachToWindow().measure().layout();

    lifecycleTracker.reset();

    mLegacyLithoViewRule
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
        MountSpecLifecycleTester.create(mLegacyLithoViewRule.getContext())
            .lifecycleTracker(lifecycleTracker)
            .intrinsicSize(new Size(800, 600))
            .build();
    mLegacyLithoViewRule.setRoot(component);

    mLegacyLithoViewRule.attachToWindow().measure().layout();

    lifecycleTracker.reset();

    mLegacyLithoViewRule.setSizePx(800, 600).measure();

    assertThat(lifecycleTracker.getSteps())
        .describedAs(
            "No lifecycle methods should be called because EXACT measures should skip layout calculation.")
        .isEmpty();
  }

  @Test
  public void lifecycle_onReLayoutAfterMeasureWithExactSize_shouldCallLifecycleMethods() {
    final LifecycleTracker lifecycleTracker = new LifecycleTracker();
    final Component component =
        MountSpecLifecycleTester.create(mLegacyLithoViewRule.getContext())
            .lifecycleTracker(lifecycleTracker)
            .intrinsicSize(new Size(800, 600))
            .build();
    mLegacyLithoViewRule.setRoot(component);

    mLegacyLithoViewRule.attachToWindow().measure().layout();

    lifecycleTracker.reset();

    mLegacyLithoViewRule.setSizePx(800, 600).measure().layout();

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
        MountSpecLifecycleTester.create(mLegacyLithoViewRule.getContext())
            .intrinsicSize(new Size(800, 600))
            .lifecycleTracker(lifecycleTracker)
            .build();
    mLegacyLithoViewRule.setRoot(
        Column.create(mLegacyLithoViewRule.getContext()).child(component).build());

    mLegacyLithoViewRule.attachToWindow().measure().layout();

    lifecycleTracker.reset();

    mLegacyLithoViewRule.setSizePx(800, 600).measure().layout();

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
        MountSpecLifecycleTester.create(mLegacyLithoViewRule.getContext())
            .intrinsicSize(new Size(800, 600))
            .lifecycleTracker(lifecycleTracker)
            .build();
    mLegacyLithoViewRule.setRoot(component);

    mLegacyLithoViewRule.attachToWindow().measure().layout();

    lifecycleTracker.reset();

    mLegacyLithoViewRule.setRoot(component.makeShallowCopy());

    assertThat(lifecycleTracker.getSteps())
        .describedAs("No lifecycle methods should be called")
        .isEmpty();
  }

  @Test
  public void lifecycle_onRemeasureWithCompatibleSpecs_shouldNotRemount() {
    final LifecycleTracker lifecycleTracker = new LifecycleTracker();
    final Component component =
        MountSpecLifecycleTester.create(mLegacyLithoViewRule.getContext())
            .intrinsicSize(new Size(800, 600))
            .lifecycleTracker(lifecycleTracker)
            .build();

    mLegacyLithoViewRule.setRoot(component).measure().layout().attachToWindow();

    lifecycleTracker.reset();

    // Force measure call to propagate to ComponentTree
    mLegacyLithoViewRule.getLithoView().requestLayout();
    mLegacyLithoViewRule.measure().layout();

    assertThat(lifecycleTracker.getSteps())
        .describedAs("No lifecycle methods should be called because measure was compatible")
        .isEmpty();
  }

  @Test
  public void lifecycle_onSetSemanticallySimilarComponent_shouldCallLifecycleMethods() {
    final LifecycleTracker lifecycleTracker = new LifecycleTracker();
    final Component component =
        MountSpecLifecycleTester.create(mLegacyLithoViewRule.getContext())
            .intrinsicSize(new Size(800, 600))
            .lifecycleTracker(lifecycleTracker)
            .build();
    mLegacyLithoViewRule.setRoot(component);

    mLegacyLithoViewRule.attachToWindow().measure().layout();

    lifecycleTracker.reset();

    final LifecycleTracker newLifecycleTracker = new LifecycleTracker();
    mLegacyLithoViewRule.setRoot(
        MountSpecLifecycleTester.create(mLegacyLithoViewRule.getContext())
            .intrinsicSize(new Size(800, 600))
            .lifecycleTracker(newLifecycleTracker)
            .build());

    mLegacyLithoViewRule.measure().layout();

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
  }

  @Test
  public void onSetRootWithPreallocatedMountContent_shouldCallLifecycleMethods() {
    final Looper looper = ShadowLooper.getLooperForThread(Thread.currentThread());
    final ComponentTree tree =
        ComponentTree.create(mLegacyLithoViewRule.getContext())
            .shouldPreallocateMountContentPerMountSpec(true)
            .preAllocateMountContentHandler(new RunnableHandler.DefaultHandler(looper))
            .build();
    mLegacyLithoViewRule.useComponentTree(tree);

    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final Component component =
        PreallocatedMountSpecLifecycleTester.create(mLegacyLithoViewRule.getContext())
            .steps(info)
            .build();

    mLegacyLithoViewRule
        .getComponentTree()
        .setRootAndSizeSpecSync(
            component, mLegacyLithoViewRule.getWidthSpec(), mLegacyLithoViewRule.getHeightSpec());

    mLegacyLithoViewRule.measure();

    ShadowLooper.runUiThreadTasks();

    assertThat(getSteps(info))
        .describedAs("Should call the lifecycle methods on new instance in expected order")
        .containsExactly(
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_ATTACHED);
  }

  @Test
  public void onSetRootWithPreallocatedMountContent_shouldCallLifecycleMethodsInRenderCore() {
    final Looper looper = ShadowLooper.getLooperForThread(Thread.currentThread());
    final ComponentTree tree =
        ComponentTree.create(mLegacyLithoViewRule.getContext())
            .shouldPreallocateMountContentPerMountSpec(true)
            .preAllocateMountContentHandler(new RunnableHandler.DefaultHandler(looper))
            .build();
    mLegacyLithoViewRule.useComponentTree(tree);

    final List<LifecycleStep.StepInfo> info = new ArrayList<>();
    final Component component =
        PreallocatedMountSpecLifecycleTester.create(mLegacyLithoViewRule.getContext())
            .steps(info)
            .build();

    mLegacyLithoViewRule
        .getComponentTree()
        .setRootAndSizeSpecSync(
            component, mLegacyLithoViewRule.getWidthSpec(), mLegacyLithoViewRule.getHeightSpec());

    mLegacyLithoViewRule.measure();

    ShadowLooper.runUiThreadTasks();

    assertThat(getSteps(info))
        .describedAs("Should call the lifecycle methods on new instance in expected order")
        .containsExactly(
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_ATTACHED);

    assertThat(MountItemsPool.getMountItemPools().size())
        .describedAs("Should contain only 1 content pool")
        .isEqualTo(1);
  }

  @Test
  public void shouldUpdate_shouldUpdateIsCalled_prevAndNextAreInRightOrder() {
    final Object firstObject = new Object();
    final List<Diff<Object>> shouldUpdateCalls = new ArrayList<>();
    mLegacyLithoViewRule
        .setRoot(
            RecordsShouldUpdate.create(mLegacyLithoViewRule.getContext())
                .shouldUpdateCalls(shouldUpdateCalls)
                .testProp(firstObject)
                .build())
        .measure()
        .layout()
        .attachToWindow();

    final Object secondObject = new Object();
    mLegacyLithoViewRule
        .setRoot(
            RecordsShouldUpdate.create(mLegacyLithoViewRule.getContext())
                .shouldUpdateCalls(shouldUpdateCalls)
                .testProp(secondObject)
                .build())
        .measure()
        .layout();

    assertThat(shouldUpdateCalls).hasSize(1);
    assertThat(shouldUpdateCalls.get(0).getPrevious()).isEqualTo(firstObject);
    assertThat(shouldUpdateCalls.get(0).getNext()).isEqualTo(secondObject);
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
        Column.create(mLegacyLithoViewRule.getContext())
            .child(
                MountSpecLifecycleTester.create(mLegacyLithoViewRule.getContext())
                    .intrinsicSize(new Size(800, 600))
                    .lifecycleTracker(info_child1)
                    .key("some_key"))
            .child(
                MountSpecLifecycleTester.create(mLegacyLithoViewRule.getContext())
                    .intrinsicSize(new Size(800, 600))
                    .lifecycleTracker(info_child2)
                    .key("other_key"))
            .child(
                SimpleStateUpdateEmulator.create(mLegacyLithoViewRule.getContext())
                    .caller(stateUpdater))
            .build();

    mLegacyLithoViewRule.useComponentTree(
        ComponentTree.create(mLegacyLithoViewRule.getContext())
            .isReconciliationEnabled(false)
            .build());
    mLegacyLithoViewRule.setRoot(root).attachToWindow().measure().layout();

    final MountDelegateTarget mountDelegateTarget =
        mLegacyLithoViewRule.getLithoView().getMountDelegateTarget();
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

  /**
   * This test case captures the scenario where unmount can get called on a component for which
   * mount was never invoked. 1. A layout is mounted. 2. The next layout update does not cause a new
   * mount pass. 3. When the next mount pass is triggered, an item is unmounted as it is out of the
   * view port. 4. The unmount must be called on the old (currently) mounted component.
   */
  @Test
  public void whenItemsAreUmounted_thenUnmountMustbeInvokedOnTheCurrentlyMountedComponent() {
    final ComponentContext c = mLegacyLithoViewRule.getContext();
    final Component initialComponent =
        Column.create(c)
            .heightPx(200)
            .child(Text.create(c).text("1").heightPx(100))
            .child(
                MountSpecWithMountUnmountAssertion.create(c)
                    .container(new MountSpecWithMountUnmountAssertionSpec.Container())
                    .heightPx(100))
            .build();

    final ComponentTree initialComponentTree =
        ComponentTree.create(c, initialComponent).useRenderUnitIdMap(true).build();

    LithoView lithoView = new LithoView(c.getAndroidContext());

    // Mount a layout with the component.
    lithoView.setComponentTree(initialComponentTree);
    lithoView.measure(exactly(100), exactly(200));
    lithoView.layout(0, 0, 100, 200);

    // Assert that the view is mounted
    assertThat(lithoView.getChildCount()).isEqualTo(1);

    Component newComponent =
        Column.create(c)
            .heightPx(200)
            .child(Text.create(c).text("1").heightPx(100))
            .child(
                MountSpecWithMountUnmountAssertion.create(c)
                    .container(new MountSpecWithMountUnmountAssertionSpec.Container())
                    .heightPx(100)
                    .build())
            .build();

    // Create a new component tree so that state is recreated
    // but use the id maps, and component tree id from the initial
    // tree so that new ids match the current one
    final ComponentTree newComponentTree =
        ComponentTree.create(c, newComponent)
            .useRenderUnitIdMap(true)
            .overrideRenderUnitIdMap(initialComponentTree)
            .overrideComponentTreeId(initialComponentTree.mId)
            .build();

    lithoView.setComponentTree(newComponentTree);

    // Mount a new layout, but with a shorter height, to make the item unmount
    lithoView.measure(exactly(100), exactly(95));
    lithoView.layout(0, 0, 100, 95);

    // Assert that the items is unmounted.
    assertThat(lithoView.getChildCount()).isEqualTo(0);
  }

  @Test
  public void mountTimeLifecycleMethodsShouldBeCalledInExpectedOrder() {
    final Component root =
        Column.create(mLegacyLithoViewRule.getContext())
            .child(
                MountSpecWithMountUnmountAssertion.create(mLegacyLithoViewRule.getContext())
                    .viewTag("tag")
                    .hasTagSet(true)
                    .container(new MountSpecWithMountUnmountAssertionSpec.Container()))
            .build();

    mLegacyLithoViewRule.attachToWindow().setRoot(root).measure().layout();

    mLegacyLithoViewRule.getLithoView().unmountAllItems();
  }

  @Test
  public void lifecycle_onComponentWithExactSize_shouldStoreMeasurementsInDiffNode() {
    final boolean original = ComponentsConfiguration.alwaysWriteDiffNodes;
    ComponentsConfiguration.alwaysWriteDiffNodes = true;

    final LifecycleTracker lifecycleTracker = new LifecycleTracker();

    mLegacyLithoViewRule
        .attachToWindow()
        .setRoot(
            MountSpecLifecycleTester.create(mLegacyLithoViewRule.getContext())
                .lifecycleTracker(lifecycleTracker)
                .widthPx(800)
                .heightPx(600)
                .build())
        .measure()
        .layout();

    final DiffNode node = mLegacyLithoViewRule.getCommittedLayoutState().getDiffTree();

    assertThat(node).isNotNull();
    assertThat(node.getLastWidthSpec()).isEqualTo(exactly(800));
    assertThat(node.getLastHeightSpec()).isEqualTo(exactly(600));
    assertThat(node.getLastMeasuredWidth()).isEqualTo(800);
    assertThat(node.getLastMeasuredHeight()).isEqualTo(600);

    ComponentsConfiguration.alwaysWriteDiffNodes = original;
  }
}
