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

import static org.assertj.core.api.Java6Assertions.assertThat;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.TextView;
import com.facebook.litho.testing.BackgroundLayoutLooperRule;
import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.MountSpecWithShouldUpdate;
import com.facebook.litho.widget.SimpleStateUpdateEmulator;
import com.facebook.litho.widget.SimpleStateUpdateEmulatorSpec;
import com.facebook.litho.widget.TextViewCounter;
import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.shadows.ShadowLooper;

@RunWith(LithoTestRunner.class)
public class LayoutDiffingTest {

  public @Rule LithoViewRule mLithoViewRule = new LithoViewRule();
  public @Rule BackgroundLayoutLooperRule mBackgroundLayoutLooperRule =
      new BackgroundLayoutLooperRule();

  /**
   * In this scenario, we make sure that if a state update happens in the background followed by a
   * second state update in the background before the first can commit on the main thread, that we
   * still properly diff at mount time and don't unmount and remount and MountSpecs
   * with @ShouldUpdate(onMount = true).
   */
  @Test
  public void
      layoutDiffing_multipleStateUpdatesInParallelWithShouldUpdateFalse_mountContentIsNotRemounted() {
    final SimpleStateUpdateEmulatorSpec.Caller stateUpdater =
        new SimpleStateUpdateEmulatorSpec.Caller();
    final ArrayList<LifecycleStep> operations = new ArrayList<>();
    final Object firstObjectForShouldUpdate = new Object();

    mLithoViewRule
        .setRoot(
            createRootComponentWithStateUpdater(
                mLithoViewRule.getContext(), firstObjectForShouldUpdate, operations, stateUpdater))
        .setSizePx(100, 100)
        .measure()
        .layout()
        .attachToWindow();

    assertThat(operations)
        .describedAs("Test setup, there should be an initial mount")
        .containsExactly(LifecycleStep.ON_MOUNT);
    operations.clear();

    // Do two state updates sequentially without draining the main thread queue
    stateUpdater.incrementAsync();
    mBackgroundLayoutLooperRule.runToEndOfTasksSync();

    stateUpdater.incrementAsync();
    mBackgroundLayoutLooperRule.runToEndOfTasksSync();

    // Now drain the main thread queue and mount the result
    ShadowLooper.idleMainLooper();
    mLithoViewRule.layout();

    assertThat(operations).isEmpty();
  }

  @Test
  public void
      layoutDiffing_multipleSetRootsInParallelWithShouldUpdateFalse_mountContentIsNotRemounted() {
    final ArrayList<LifecycleStep> operations = new ArrayList<>();
    final Object firstObjectForShouldUpdate = new Object();

    mLithoViewRule
        .setRoot(
            createRootComponent(
                mLithoViewRule.getContext(), firstObjectForShouldUpdate, operations))
        .setSizePx(100, 100)
        .measure()
        .layout()
        .attachToWindow();

    assertThat(operations)
        .describedAs("Test setup, there should be an initial mount")
        .containsExactly(LifecycleStep.ON_MOUNT);
    operations.clear();

    // Do two prop updates sequentially without draining the main thread queue
    mLithoViewRule.setRootAsync(
        createRootComponent(mLithoViewRule.getContext(), firstObjectForShouldUpdate, operations));
    mBackgroundLayoutLooperRule.runToEndOfTasksSync();

    mLithoViewRule.setRootAsync(
        createRootComponent(mLithoViewRule.getContext(), firstObjectForShouldUpdate, operations));
    mBackgroundLayoutLooperRule.runToEndOfTasksSync();

    // Now drain the main thread queue and mount the result
    ShadowLooper.idleMainLooper();
    mLithoViewRule.layout();

    assertThat(operations).isEmpty();
  }

  /**
   * In this scenario, we make sure that if a setRoot happens in the background followed by a second
   * setRoot in the background before the first can commit on the main thread, that we still
   * properly diff at mount time and don't unmount and remount and MountSpecs
   * with @ShouldUpdate(onMount = true).
   */
  @Test
  public void
      layoutDiffing_multipleSetRootsInParallelWithShouldUpdateTrueForFirstLayout_mountContentIsRemounted() {
    final ArrayList<LifecycleStep> operations = new ArrayList<>();
    final Object firstObjectForShouldUpdate = new Object();

    mLithoViewRule
        .setRoot(
            createRootComponent(
                mLithoViewRule.getContext(), firstObjectForShouldUpdate, operations))
        .setSizePx(100, 100)
        .measure()
        .layout()
        .attachToWindow();

    assertThat(operations).containsExactly(LifecycleStep.ON_MOUNT);
    operations.clear();

    final Object secondObjectForShouldUpdate = new Object();

    // Do two prop updates sequentially without draining the main thread queue
    mLithoViewRule.setRootAsync(
        createRootComponent(mLithoViewRule.getContext(), secondObjectForShouldUpdate, operations));
    mBackgroundLayoutLooperRule.runToEndOfTasksSync();

    mLithoViewRule.setRootAsync(
        createRootComponent(mLithoViewRule.getContext(), secondObjectForShouldUpdate, operations));
    mBackgroundLayoutLooperRule.runToEndOfTasksSync();

    // Now drain the main thread queue and mount the result
    ShadowLooper.idleMainLooper();
    mLithoViewRule.layout();

    // In this case, we did change the object for shouldUpdate in layout 1 even though
    // it was the same for layouts 2. We expect to see unmount and mount.
    assertThat(operations).containsExactly(LifecycleStep.ON_UNMOUNT, LifecycleStep.ON_MOUNT);
    operations.clear();
  }

  @Test
  public void
      layoutDiffing_multipleSetRootsInParallelWithShouldUpdateTrueForSecondLayout_mountContentIsRemounted() {
    final ArrayList<LifecycleStep> operations = new ArrayList<>();
    final Object firstObjectForShouldUpdate = new Object();

    mLithoViewRule
        .setRoot(
            createRootComponent(
                mLithoViewRule.getContext(), firstObjectForShouldUpdate, operations))
        .setSizePx(100, 100)
        .measure()
        .layout()
        .attachToWindow();

    assertThat(operations).containsExactly(LifecycleStep.ON_MOUNT);
    operations.clear();

    final Object secondObjectForShouldUpdate = new Object();

    // Do two prop updates sequentially without draining the main thread queue
    mLithoViewRule.setRootAsync(
        createRootComponent(mLithoViewRule.getContext(), firstObjectForShouldUpdate, operations));
    mBackgroundLayoutLooperRule.runToEndOfTasksSync();

    mLithoViewRule.setRootAsync(
        createRootComponent(mLithoViewRule.getContext(), secondObjectForShouldUpdate, operations));
    mBackgroundLayoutLooperRule.runToEndOfTasksSync();

    // Now drain the main thread queue and mount the result
    ShadowLooper.idleMainLooper();
    mLithoViewRule.layout();

    // Similar to the previous test, but the object changes on the second layout instead.
    assertThat(operations).containsExactly(LifecycleStep.ON_UNMOUNT, LifecycleStep.ON_MOUNT);
  }

  @Test
  public void whenStateUpdateOnPureRenderMountSpec_shouldRemountItem() {
    final ComponentContext c = mLithoViewRule.getContext();
    final Component component =
        Column.create(c)
            .child(TextViewCounter.create(c).viewWidth(200).viewHeight(200).build())
            .build();
    mLithoViewRule.attachToWindow().setRoot(component).measure().layout();

    final View view = mLithoViewRule.getLithoView().getChildAt(0);
    assertThat(view).isNotNull();
    assertThat(view).isInstanceOf(TextView.class);
    assertThat(((TextView) view).getText()).isEqualTo("0");
    view.callOnClick();
    assertThat(((TextView) view).getText()).isEqualTo("1");
  }

  private static Component createRootComponentWithStateUpdater(
      ComponentContext c,
      Object objectForShouldUpdate,
      List<LifecycleStep> operationsOutput,
      SimpleStateUpdateEmulatorSpec.Caller stateUpdateCaller) {
    return Row.create(c)
        .child(
            Column.create(c)
                .child(
                    SimpleStateUpdateEmulator.create(c)
                        .caller(stateUpdateCaller)
                        .widthPx(100)
                        .heightPx(100)
                        .background(new ColorDrawable(Color.RED)))
                .child(
                    MountSpecWithShouldUpdate.create(c)
                        .objectForShouldUpdate(objectForShouldUpdate)
                        .operationsOutput(operationsOutput)
                        .widthPx(10)
                        .heightPx(10)))
        .build();
  }

  private static Component createRootComponent(
      ComponentContext c, Object objectForShouldUpdate, List<LifecycleStep> operationsOutput) {
    return Row.create(c)
        .child(
            Column.create(c)
                .child(
                    Row.create(c)
                        .widthPx(100)
                        .heightPx(100)
                        .background(new ColorDrawable(Color.RED)))
                .child(
                    MountSpecWithShouldUpdate.create(c)
                        .objectForShouldUpdate(objectForShouldUpdate)
                        .operationsOutput(operationsOutput)
                        .widthPx(10)
                        .heightPx(10)))
        .build();
  }
}
