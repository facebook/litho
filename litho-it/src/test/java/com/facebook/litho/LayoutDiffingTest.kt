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

package com.facebook.litho

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.widget.TextView
import com.facebook.litho.testing.BackgroundLayoutLooperRule
import com.facebook.litho.testing.LegacyLithoViewRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.MountSpecWithShouldUpdate
import com.facebook.litho.widget.SimpleStateUpdateEmulator
import com.facebook.litho.widget.SimpleStateUpdateEmulatorSpec
import com.facebook.litho.widget.TextViewCounter
import java.util.ArrayList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class LayoutDiffingTest {

  @JvmField @Rule var legacyLithoViewRule = LegacyLithoViewRule()

  @JvmField @Rule var backgroundLayoutLooperRule = BackgroundLayoutLooperRule()

  /**
   * In this scenario, we make sure that if a state update happens in the background followed by a
   * second state update in the background before the first can commit on the main thread, that we
   * still properly diff at mount time and don't unmount and remount and MountSpecs
   * with @ShouldUpdate(onMount = true).
   */
  @Test
  fun layoutDiffing_multipleStateUpdatesInParallelWithShouldUpdateFalse_mountContentIsNotRemounted() {
    val stateUpdater = SimpleStateUpdateEmulatorSpec.Caller()
    val operations = ArrayList<LifecycleStep>()
    val firstObjectForShouldUpdate = Any()
    legacyLithoViewRule
        .setRoot(
            createRootComponentWithStateUpdater(
                legacyLithoViewRule.context, firstObjectForShouldUpdate, operations, stateUpdater))
        .setSizePx(100, 100)
        .measure()
        .layout()
        .attachToWindow()
    assertThat(operations)
        .describedAs("Test setup, there should be an initial mount")
        .containsExactly(LifecycleStep.ON_MOUNT)
    operations.clear()

    // Do two state updates sequentially without draining the main thread queue
    stateUpdater.incrementAsync()
    backgroundLayoutLooperRule.runToEndOfTasksSync()
    stateUpdater.incrementAsync()
    backgroundLayoutLooperRule.runToEndOfTasksSync()

    // Now drain the main thread queue and mount the result
    ShadowLooper.idleMainLooper()
    legacyLithoViewRule.layout()
    assertThat(operations).isEmpty()
  }

  @Test
  fun layoutDiffing_multipleSetRootsInParallelWithShouldUpdateFalse_mountContentIsNotRemounted() {
    val operations = ArrayList<LifecycleStep>()
    val firstObjectForShouldUpdate = Any()
    legacyLithoViewRule
        .setRoot(
            createRootComponent(
                legacyLithoViewRule.context, firstObjectForShouldUpdate, operations))
        .setSizePx(100, 100)
        .measure()
        .layout()
        .attachToWindow()
    assertThat(operations)
        .describedAs("Test setup, there should be an initial mount")
        .containsExactly(LifecycleStep.ON_MOUNT)
    operations.clear()

    // Do two prop updates sequentially without draining the main thread queue
    legacyLithoViewRule.setRootAsync(
        createRootComponent(legacyLithoViewRule.context, firstObjectForShouldUpdate, operations))
    backgroundLayoutLooperRule.runToEndOfTasksSync()
    legacyLithoViewRule.setRootAsync(
        createRootComponent(legacyLithoViewRule.context, firstObjectForShouldUpdate, operations))
    backgroundLayoutLooperRule.runToEndOfTasksSync()

    // Now drain the main thread queue and mount the result
    ShadowLooper.idleMainLooper()
    legacyLithoViewRule.layout()
    assertThat(operations).isEmpty()
  }

  /**
   * In this scenario, we make sure that if a setRoot happens in the background followed by a second
   * setRoot in the background before the first can commit on the main thread, that we still
   * properly diff at mount time and don't unmount and remount and MountSpecs
   * with @ShouldUpdate(onMount = true).
   */
  @Test
  fun layoutDiffing_multipleSetRootsInParallelWithShouldUpdateTrueForFirstLayout_mountContentIsRemounted() {
    val operations = ArrayList<LifecycleStep>()
    val firstObjectForShouldUpdate = Any()
    legacyLithoViewRule
        .setRoot(
            createRootComponent(
                legacyLithoViewRule.context, firstObjectForShouldUpdate, operations))
        .setSizePx(100, 100)
        .measure()
        .layout()
        .attachToWindow()
    assertThat(operations).containsExactly(LifecycleStep.ON_MOUNT)
    operations.clear()
    val secondObjectForShouldUpdate = Any()

    // Do two prop updates sequentially without draining the main thread queue
    legacyLithoViewRule.setRootAsync(
        createRootComponent(legacyLithoViewRule.context, secondObjectForShouldUpdate, operations))
    backgroundLayoutLooperRule.runToEndOfTasksSync()
    legacyLithoViewRule.setRootAsync(
        createRootComponent(legacyLithoViewRule.context, secondObjectForShouldUpdate, operations))
    backgroundLayoutLooperRule.runToEndOfTasksSync()

    // Now drain the main thread queue and mount the result
    ShadowLooper.idleMainLooper()
    legacyLithoViewRule.layout()

    // In this case, we did change the object for shouldUpdate in layout 1 even though
    // it was the same for layouts 2. We expect to see unmount and mount.
    assertThat(operations).containsExactly(LifecycleStep.ON_UNMOUNT, LifecycleStep.ON_MOUNT)
    operations.clear()
  }

  @Test
  fun layoutDiffing_multipleSetRootsInParallelWithShouldUpdateTrueForSecondLayout_mountContentIsRemounted() {
    val operations = ArrayList<LifecycleStep>()
    val firstObjectForShouldUpdate = Any()
    legacyLithoViewRule
        .setRoot(
            createRootComponent(
                legacyLithoViewRule.context, firstObjectForShouldUpdate, operations))
        .setSizePx(100, 100)
        .measure()
        .layout()
        .attachToWindow()
    assertThat(operations).containsExactly(LifecycleStep.ON_MOUNT)
    operations.clear()
    val secondObjectForShouldUpdate = Any()

    // Do two prop updates sequentially without draining the main thread queue
    legacyLithoViewRule.setRootAsync(
        createRootComponent(legacyLithoViewRule.context, firstObjectForShouldUpdate, operations))
    backgroundLayoutLooperRule.runToEndOfTasksSync()
    legacyLithoViewRule.setRootAsync(
        createRootComponent(legacyLithoViewRule.context, secondObjectForShouldUpdate, operations))
    backgroundLayoutLooperRule.runToEndOfTasksSync()

    // Now drain the main thread queue and mount the result
    ShadowLooper.idleMainLooper()
    legacyLithoViewRule.layout()

    // Similar to the previous test, but the object changes on the second layout instead.
    assertThat(operations).containsExactly(LifecycleStep.ON_UNMOUNT, LifecycleStep.ON_MOUNT)
  }

  @Test
  fun whenStateUpdateOnPureRenderMountSpec_shouldRemountItem() {
    val c = legacyLithoViewRule.context
    val component =
        Column.create(c)
            .child(TextViewCounter.create(c).viewWidth(200).viewHeight(200).build())
            .build()
    legacyLithoViewRule.attachToWindow().setRoot(component).measure().layout()
    val view = legacyLithoViewRule.lithoView.getChildAt(0)
    assertThat(view).isNotNull
    assertThat(view).isInstanceOf(TextView::class.java)
    assertThat((view as TextView).text).isEqualTo("0")
    view.callOnClick()
    assertThat(view.text).isEqualTo("1")
  }

  @Test
  fun onSetRootWithSameComponent_thenShouldNotRemeasureMountSpec() {
    val c = legacyLithoViewRule.context
    val operations = ArrayList<LifecycleStep>()
    val objectForShouldUpdate = Any()
    val component =
        Row.create(c)
            .child(
                Column.create(c)
                    .child(
                        Row.create(c)
                            .widthPx(100)
                            .heightPx(100)
                            .background(ColorDrawable(Color.RED)))
                    .child(
                        MountSpecWithShouldUpdate.create(c)
                            .objectForShouldUpdate(objectForShouldUpdate)
                            .operationsOutput(operations)))
            .build()
    legacyLithoViewRule.attachToWindow().setRoot(component).measure().layout()
    assertThat(operations).containsExactly(LifecycleStep.ON_MEASURE, LifecycleStep.ON_MOUNT)
    operations.clear()
    legacyLithoViewRule.attachToWindow().setRoot(component).measure().layout()
    assertThat(operations).isEmpty()
  }

  @Test
  fun onSetRootWithSimilarComponent_thenShouldNotRemeasureMountSpec() {
    val c = legacyLithoViewRule.context
    val operations = ArrayList<LifecycleStep>()
    val objectForShouldUpdate = Any()
    val component =
        Row.create(c)
            .child(
                Column.create(c)
                    .child(
                        Row.create(c)
                            .widthPx(100)
                            .heightPx(100)
                            .background(ColorDrawable(Color.RED)))
                    .child(
                        MountSpecWithShouldUpdate.create(c)
                            .objectForShouldUpdate(objectForShouldUpdate)
                            .operationsOutput(operations)))
            .build()
    legacyLithoViewRule.attachToWindow().setRoot(component).measure().layout()
    assertThat(operations).containsExactly(LifecycleStep.ON_MEASURE, LifecycleStep.ON_MOUNT)
    operations.clear()
    val next =
        Row.create(c)
            .child(
                Column.create(c)
                    .child(
                        Row.create(c)
                            .widthPx(100)
                            .heightPx(100)
                            .background(ColorDrawable(Color.RED)))
                    .child(
                        MountSpecWithShouldUpdate.create(c)
                            .objectForShouldUpdate(objectForShouldUpdate)
                            .operationsOutput(operations)))
            .build()
    legacyLithoViewRule.attachToWindow().setRoot(next).measure().layout()
    assertThat(operations).isEmpty()
  }

  @Test
  fun onSetRootWithSimilarComponentWithShouldUpdateTrue_thenShouldRemeasureMountSpec() {
    val c = legacyLithoViewRule.context
    val operations = ArrayList<LifecycleStep>()
    val objectForShouldUpdate = Any()
    val component =
        Row.create(c)
            .child(
                Column.create(c)
                    .child(
                        Row.create(c)
                            .widthPx(100)
                            .heightPx(100)
                            .background(ColorDrawable(Color.RED)))
                    .child(
                        MountSpecWithShouldUpdate.create(c)
                            .objectForShouldUpdate(objectForShouldUpdate)
                            .operationsOutput(operations)))
            .build()
    legacyLithoViewRule.attachToWindow().setRoot(component).measure().layout()
    assertThat(operations).containsExactly(LifecycleStep.ON_MEASURE, LifecycleStep.ON_MOUNT)
    operations.clear()
    val next =
        Row.create(c)
            .child(
                Column.create(c)
                    .child(
                        Row.create(c)
                            .widthPx(100)
                            .heightPx(100)
                            .background(ColorDrawable(Color.RED)))
                    .child(
                        MountSpecWithShouldUpdate.create(c)
                            .objectForShouldUpdate(Any())
                            .operationsOutput(operations)))
            .build()
    legacyLithoViewRule.attachToWindow().setRoot(next).measure().layout()
    assertThat(operations)
        .containsExactly(LifecycleStep.ON_MEASURE, LifecycleStep.ON_UNMOUNT, LifecycleStep.ON_MOUNT)
  }

  companion object {
    private fun createRootComponentWithStateUpdater(
        c: ComponentContext,
        objectForShouldUpdate: Any,
        operationsOutput: List<LifecycleStep>,
        stateUpdateCaller: SimpleStateUpdateEmulatorSpec.Caller
    ): Component =
        Row.create(c)
            .child(
                Column.create(c)
                    .child(
                        SimpleStateUpdateEmulator.create(c)
                            .caller(stateUpdateCaller)
                            .widthPx(100)
                            .heightPx(100)
                            .background(ColorDrawable(Color.RED)))
                    .child(
                        MountSpecWithShouldUpdate.create(c)
                            .objectForShouldUpdate(objectForShouldUpdate)
                            .operationsOutput(operationsOutput)
                            .widthPx(10)
                            .heightPx(10)))
            .build()

    private fun createRootComponent(
        c: ComponentContext,
        objectForShouldUpdate: Any,
        operationsOutput: List<LifecycleStep>
    ): Component =
        Row.create(c)
            .child(
                Column.create(c)
                    .child(
                        Row.create(c)
                            .widthPx(100)
                            .heightPx(100)
                            .background(ColorDrawable(Color.RED)))
                    .child(
                        MountSpecWithShouldUpdate.create(c)
                            .objectForShouldUpdate(objectForShouldUpdate)
                            .operationsOutput(operationsOutput)
                            .widthPx(10)
                            .heightPx(10)))
            .build()
  }
}
