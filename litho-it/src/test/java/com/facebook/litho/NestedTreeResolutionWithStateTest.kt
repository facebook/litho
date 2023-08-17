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

import com.facebook.litho.LifecycleStep.StepInfo
import com.facebook.litho.stateupdates.BaseIncrementStateCaller
import com.facebook.litho.stateupdates.ComponentWithMeasureCallAndState
import com.facebook.litho.stateupdates.ComponentWithMeasureCallAndStateSpec
import com.facebook.litho.stateupdates.ComponentWithSizeAndMeasureCallAndState
import com.facebook.litho.stateupdates.ComponentWithSizeAndMeasureCallAndStateSpec
import com.facebook.litho.testing.LegacyLithoViewRule
import com.facebook.litho.testing.exactly
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.MountSpecPureRenderLifecycleTester
import java.util.ArrayList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

/**
 * IMPORTANT INFORMATION! PLEASE READ!
 *
 * In this test class, all tests will test for complex component hierarchies of the following form:
 * Root --> Mid --> Bot Where Root, Mid and Bot can either be an OnCreateLayout (OCL) component, or
 * an OnCreateLayoutWithSizeSpec (OCLWSS) component.
 *
 * All 3 components hold an int state value, initialized as zero. Each component will render a Text
 * displaying a prefix ("root", "mid", "bot") + the value of the state value as their 1st child.
 * Root will accept Mid as a @Prop component, will call Component.measure on it, and use it as it's
 * 2nd child. Mid will accept Bot as a @Prop component, will call Component.measure on it, and use
 * it as it's 2nd child. Bot will accept a MountSpec component and use it as it's 2nd child.
 *
 * All tests will follow the same flow:
 * 1. Build such a hierarchy, each test will have a different variation of OCL / OCLWSS comps
 * 2. Ensure lifecycle steps for each comp + the MountSpec are as expected
 * 3. Ensure all texts are displaying the correct state
 * 4. Update state on the root comp, and repeat #2 and #3
 * 5. Update state on the mid comp, and repeat #2 and #3
 * 6. Update state on the bot comp, and repeat #2 and #3
 *
 * All tests use a common main test method ("testSpecificSetup") that is accessed via a Builder
 * class. Since all tests use the same flow, they all funnel into this method with different
 * component hierarchy setup and different expected LifecycleSteps for each component / phase.
 *
 * The name of each test will indicate the hierarchy being tested. For example, a test named
 * "test_OCLWSS_OCL_OCL" will hold the hierarchy of: Root as OCLWSS, Mid as OCL, Bot as OCL
 */
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class NestedTreeResolutionWithStateTest {

  @JvmField @Rule val legacyLithoViewRule: LegacyLithoViewRule = LegacyLithoViewRule()

  /**
   * Tests OCL_OCL_OCL hierarchy.
   *
   * @see NestedTreeResolutionWithStateTest class declaration documentation for additional details.
   */
  @Test
  fun test_OCL_OCL_OCL() {
    // Mid and bot steps will be the same on preUpdate
    val expectedStepsForMidAndBotPreUpdate =
        arrayOf(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT,
            LifecycleStep.ON_ATTACHED)

    // Mid and bot steps will be the same on update 1
    val expectedStepsForMidAndBotUpdate1 =
        arrayOf(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT,
        )

    // Testing OCL -> OCL -> OCL
    TestHierarchyBuilder.create(this, true, true, true)
        .setRootStepsPreUpdate(
            arrayOf(
                LifecycleStep.ON_CREATE_INITIAL_STATE,
                LifecycleStep.ON_CREATE_TREE_PROP,
                LifecycleStep.ON_CALCULATE_CACHED_VALUE,
                LifecycleStep.ON_CREATE_LAYOUT,
                LifecycleStep.ON_ATTACHED))
        .setMidStepsPreUpdate(expectedStepsForMidAndBotPreUpdate)
        .setBotStepsPreUpdate(expectedStepsForMidAndBotPreUpdate)
        .setMountSpecStepsPreUpdate(
            arrayOf(
                LifecycleStep.ON_CREATE_INITIAL_STATE,
                LifecycleStep.ON_CREATE_TREE_PROP,
                LifecycleStep.ON_CALCULATE_CACHED_VALUE,
                LifecycleStep.ON_PREPARE,
                LifecycleStep.ON_MEASURE,
                LifecycleStep.ON_BOUNDS_DEFINED,
                LifecycleStep.ON_ATTACHED,
                LifecycleStep.ON_CREATE_MOUNT_CONTENT,
                LifecycleStep.ON_MOUNT,
                LifecycleStep.ON_BIND))
        .setRootStepsUpdate1(
            arrayOf(
                LifecycleStep.ON_CREATE_TREE_PROP,
                LifecycleStep.ON_CALCULATE_CACHED_VALUE,
                LifecycleStep.ON_CREATE_LAYOUT))
        .setMidStepsUpdate1(expectedStepsForMidAndBotUpdate1)
        .setBotStepsUpdate1(expectedStepsForMidAndBotUpdate1)
        .setMountSpecStepsUpdate1(
            arrayOf(
                LifecycleStep.ON_CREATE_TREE_PROP,
                LifecycleStep.ON_PREPARE,
                LifecycleStep.ON_MEASURE,
                LifecycleStep.ON_BOUNDS_DEFINED,
                LifecycleStep.SHOULD_UPDATE,
                LifecycleStep.ON_UNBIND,
                LifecycleStep.ON_UNMOUNT,
                LifecycleStep.ON_MOUNT,
                LifecycleStep.ON_BIND))
        .setRootStepsUpdate2(arrayOf())
        .setMidStepsUpdate2(
            arrayOf(
                LifecycleStep.ON_CREATE_TREE_PROP,
                LifecycleStep.ON_CALCULATE_CACHED_VALUE,
                LifecycleStep.ON_CREATE_LAYOUT))
        .setBotStepsUpdate2(
            arrayOf(
                LifecycleStep.ON_CREATE_TREE_PROP,
                LifecycleStep.ON_CALCULATE_CACHED_VALUE,
                LifecycleStep.ON_CREATE_LAYOUT))
        .setMountSpecStepsUpdate2(
            arrayOf(
                LifecycleStep.ON_CREATE_TREE_PROP,
                LifecycleStep.ON_PREPARE,
                LifecycleStep.ON_MEASURE,
                LifecycleStep.ON_BOUNDS_DEFINED,
                LifecycleStep.SHOULD_UPDATE, // TODO(T160790123): Why?
                LifecycleStep.ON_UNBIND,
                LifecycleStep.ON_UNMOUNT,
                LifecycleStep.ON_MOUNT,
                LifecycleStep.ON_BIND))
        .setRootStepsUpdate3(arrayOf())
        .setMidStepsUpdate3(arrayOf()) // empty
        .setBotStepsUpdate3(
            arrayOf(
                LifecycleStep.ON_CREATE_TREE_PROP,
                LifecycleStep.ON_CALCULATE_CACHED_VALUE,
                LifecycleStep.ON_CREATE_LAYOUT,
            ))
        .setMountSpecStepsUpdate3(
            arrayOf(
                LifecycleStep.ON_CREATE_TREE_PROP,
                LifecycleStep.ON_PREPARE,
                LifecycleStep.SHOULD_UPDATE,
                LifecycleStep.ON_MEASURE,
                LifecycleStep.ON_BOUNDS_DEFINED,
                LifecycleStep.ON_UNBIND,
                LifecycleStep.ON_UNMOUNT,
                LifecycleStep.ON_MOUNT,
                LifecycleStep.ON_BIND))
        .test()
  }

  /**
   * Tests OCLWSS_OCL_OCL hierarchy.
   *
   * @see NestedTreeResolutionWithStateTest class declaration documentation for additional details.
   */
  @Test
  fun test_OCLWSS_OCL_OCL() {
    val rootStepsPreUpdate =
        arrayOf<LifecycleStep?>(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
            LifecycleStep.ON_ATTACHED)

    // Mid and bot steps will be the same pre update
    val expectedStepsForMidAndBotPreUpdate =
        arrayOf(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT,
            LifecycleStep.ON_ATTACHED)

    // Mid and bot steps will be the same for all updates
    val expectedStepsForMidAndBotAllUpdates =
        arrayOf(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT,
        )

    // Root steps will be the same for all updates
    val expectedStepsForRootAllUpdates =
        arrayOf<LifecycleStep?>(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)

    // MountSpec steps will be the same for all updates
    val expectedStepsForMountSpecAllUpdates =
        arrayOf<LifecycleStep?>(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.SHOULD_UPDATE,
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)

    // Testing OCLWSS -> OCL -> OCL
    TestHierarchyBuilder.create(this, false, true, true)
        .setRootStepsPreUpdate(rootStepsPreUpdate)
        .setMidStepsPreUpdate(expectedStepsForMidAndBotPreUpdate)
        .setBotStepsPreUpdate(expectedStepsForMidAndBotPreUpdate)
        .setMountSpecStepsPreUpdate(
            arrayOf(
                LifecycleStep.ON_CREATE_INITIAL_STATE,
                LifecycleStep.ON_CREATE_TREE_PROP,
                LifecycleStep.ON_CALCULATE_CACHED_VALUE,
                LifecycleStep.ON_PREPARE,
                LifecycleStep.ON_MEASURE,
                LifecycleStep.ON_BOUNDS_DEFINED,
                LifecycleStep.ON_ATTACHED,
                LifecycleStep.ON_CREATE_MOUNT_CONTENT,
                LifecycleStep.ON_MOUNT,
                LifecycleStep.ON_BIND))
        .setRootStepsUpdate1(expectedStepsForRootAllUpdates)
        .setMidStepsUpdate1(expectedStepsForMidAndBotAllUpdates)
        .setBotStepsUpdate1(expectedStepsForMidAndBotAllUpdates)
        .setMountSpecStepsUpdate1(expectedStepsForMountSpecAllUpdates)
        .setRootStepsUpdate2(expectedStepsForRootAllUpdates)
        .setMidStepsUpdate2(expectedStepsForMidAndBotAllUpdates)
        .setBotStepsUpdate2(expectedStepsForMidAndBotAllUpdates)
        .setMountSpecStepsUpdate2(expectedStepsForMountSpecAllUpdates)
        .setRootStepsUpdate3(expectedStepsForRootAllUpdates)
        .setMidStepsUpdate3(expectedStepsForMidAndBotAllUpdates)
        .setBotStepsUpdate3(expectedStepsForMidAndBotAllUpdates)
        .setMountSpecStepsUpdate3(expectedStepsForMountSpecAllUpdates)
        .test()
  }

  /**
   * Tests OCL_OCLWSS_OCL hierarchy.
   *
   * @see NestedTreeResolutionWithStateTest class declaration documentation for additional details.
   */
  @Test
  fun test_OCL_OCLWSS_OCL() {
    val midStepsPreUpdate =
        arrayOf(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
            LifecycleStep.ON_ATTACHED)
    val botStepsPreUpdate =
        arrayOf(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT,
            LifecycleStep.ON_ATTACHED)
    val mountSpecStepsPreUpdate =
        arrayOf<LifecycleStep?>(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_ATTACHED,
            LifecycleStep.ON_CREATE_MOUNT_CONTENT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)
    val midStepsForUpdate1 =
        arrayOf(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)
    val botStepsForUpdate1 =
        arrayOf(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT)
    val mountSpecStepsForUpdate1 =
        arrayOf<LifecycleStep?>(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.SHOULD_UPDATE,
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)
    val rootStepsForUpdate2And3 = arrayOf<LifecycleStep?>()
    val midStepsForUpdate2And3 =
        arrayOf(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)
    val botStepsForUpdate2And3 =
        arrayOf(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT,
        )
    val mountSpecStepsForUpdate2And3 =
        arrayOf<LifecycleStep?>(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.SHOULD_UPDATE,
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)

    // Testing OCL -> OCLWSS -> OCL
    TestHierarchyBuilder.create(this, true, false, true)
        .setRootStepsPreUpdate(
            arrayOf(
                LifecycleStep.ON_CREATE_INITIAL_STATE,
                LifecycleStep.ON_CREATE_TREE_PROP,
                LifecycleStep.ON_CALCULATE_CACHED_VALUE,
                LifecycleStep.ON_CREATE_LAYOUT,
                LifecycleStep.ON_ATTACHED))
        .setMidStepsPreUpdate(midStepsPreUpdate)
        .setBotStepsPreUpdate(botStepsPreUpdate)
        .setMountSpecStepsPreUpdate(mountSpecStepsPreUpdate)
        .setRootStepsUpdate1(
            arrayOf(
                LifecycleStep.ON_CREATE_TREE_PROP,
                LifecycleStep.ON_CALCULATE_CACHED_VALUE,
                LifecycleStep.ON_CREATE_LAYOUT))
        .setMidStepsUpdate1(midStepsForUpdate1)
        .setBotStepsUpdate1(botStepsForUpdate1)
        .setMountSpecStepsUpdate1(mountSpecStepsForUpdate1)
        .setRootStepsUpdate2(rootStepsForUpdate2And3)
        .setMidStepsUpdate2(midStepsForUpdate2And3)
        .setBotStepsUpdate2(botStepsForUpdate2And3)
        .setMountSpecStepsUpdate2(mountSpecStepsForUpdate2And3)
        .setRootStepsUpdate3(rootStepsForUpdate2And3)
        .setMidStepsUpdate3(midStepsForUpdate2And3)
        .setBotStepsUpdate3(botStepsForUpdate2And3)
        .setMountSpecStepsUpdate3(mountSpecStepsForUpdate2And3)
        .test()
  }

  /**
   * Tests OCL_OCL_OCLWSS hierarchy.
   *
   * @see NestedTreeResolutionWithStateTest class declaration documentation for additional details.
   */
  @Test
  fun test_OCL_OCL_OCLWSS() {
    val botStepsPreUpdate =
        arrayOf(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
            LifecycleStep.ON_ATTACHED)
    val botStepsForUpdate1 =
        arrayOf(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)
    val botStepsForUpdate2 =
        arrayOf(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
        )

    // Testing OCL -> OCL -> OCLWSS
    TestHierarchyBuilder.create(this, true, true, false)
        .setRootStepsPreUpdate(
            arrayOf(
                LifecycleStep.ON_CREATE_INITIAL_STATE,
                LifecycleStep.ON_CREATE_TREE_PROP,
                LifecycleStep.ON_CALCULATE_CACHED_VALUE,
                LifecycleStep.ON_CREATE_LAYOUT,
                LifecycleStep.ON_ATTACHED))
        .setMidStepsPreUpdate(
            arrayOf(
                LifecycleStep.ON_CREATE_INITIAL_STATE,
                LifecycleStep.ON_CREATE_TREE_PROP,
                LifecycleStep.ON_CALCULATE_CACHED_VALUE,
                LifecycleStep.ON_CREATE_LAYOUT,
                LifecycleStep.ON_ATTACHED,
            ))
        .setBotStepsPreUpdate(botStepsPreUpdate)
        .setMountSpecStepsPreUpdate(
            arrayOf(
                LifecycleStep.ON_CREATE_INITIAL_STATE,
                LifecycleStep.ON_CREATE_TREE_PROP,
                LifecycleStep.ON_CALCULATE_CACHED_VALUE,
                LifecycleStep.ON_PREPARE,
                LifecycleStep.ON_MEASURE,
                LifecycleStep.ON_BOUNDS_DEFINED,
                LifecycleStep.ON_ATTACHED,
                LifecycleStep.ON_CREATE_MOUNT_CONTENT,
                LifecycleStep.ON_MOUNT,
                LifecycleStep.ON_BIND))
        .setRootStepsUpdate1(
            arrayOf(
                LifecycleStep.ON_CREATE_TREE_PROP,
                LifecycleStep.ON_CALCULATE_CACHED_VALUE,
                LifecycleStep.ON_CREATE_LAYOUT))
        .setMidStepsUpdate1(
            arrayOf(
                LifecycleStep.ON_CREATE_TREE_PROP,
                LifecycleStep.ON_CALCULATE_CACHED_VALUE,
                LifecycleStep.ON_CREATE_LAYOUT,
            ))
        .setBotStepsUpdate1(botStepsForUpdate1)
        .setMountSpecStepsUpdate1(
            arrayOf(
                LifecycleStep.ON_CREATE_TREE_PROP,
                LifecycleStep.ON_PREPARE,
                LifecycleStep.ON_MEASURE,
                LifecycleStep.ON_BOUNDS_DEFINED,
                LifecycleStep.SHOULD_UPDATE,
                LifecycleStep.ON_UNBIND,
                LifecycleStep.ON_UNMOUNT,
                LifecycleStep.ON_MOUNT,
                LifecycleStep.ON_BIND))
        .setRootStepsUpdate2(arrayOf())
        .setMidStepsUpdate2(
            arrayOf(
                LifecycleStep.ON_CREATE_TREE_PROP,
                LifecycleStep.ON_CALCULATE_CACHED_VALUE,
                LifecycleStep.ON_CREATE_LAYOUT,
            ))
        .setBotStepsUpdate2(botStepsForUpdate2)
        .setMountSpecStepsUpdate2(
            arrayOf(
                LifecycleStep.ON_CREATE_TREE_PROP,
                LifecycleStep.ON_PREPARE,
                LifecycleStep.ON_MEASURE,
                LifecycleStep.ON_BOUNDS_DEFINED,
                LifecycleStep.SHOULD_UPDATE, // TODO(T160790123): WHY?
                LifecycleStep.ON_UNBIND,
                LifecycleStep.ON_UNMOUNT,
                LifecycleStep.ON_MOUNT,
                LifecycleStep.ON_BIND))
        .setRootStepsUpdate3(arrayOf())
        .setMidStepsUpdate3(arrayOf()) // empty
        .setBotStepsUpdate3(
            arrayOf(
                LifecycleStep.ON_CREATE_TREE_PROP,
                LifecycleStep.ON_CALCULATE_CACHED_VALUE,
                LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
            ))
        .setMountSpecStepsUpdate3(
            arrayOf(
                LifecycleStep.ON_CREATE_TREE_PROP,
                LifecycleStep.ON_PREPARE,
                LifecycleStep.SHOULD_UPDATE,
                LifecycleStep.ON_MEASURE,
                LifecycleStep.ON_BOUNDS_DEFINED,
                LifecycleStep.ON_UNBIND,
                LifecycleStep.ON_UNMOUNT,
                LifecycleStep.ON_MOUNT,
                LifecycleStep.ON_BIND))
        .test()
  }

  /**
   * Tests OCLWSS_OCLWSS_OCL hierarchy.
   *
   * @see NestedTreeResolutionWithStateTest class declaration documentation for additional details.
   */
  @Test
  fun test_OCLWSS_OCLWSS_OCL() {
    val rootStepsPreUpdate =
        arrayOf<LifecycleStep?>(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
            LifecycleStep.ON_ATTACHED)
    val midStepsPreUpdate =
        arrayOf(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
            LifecycleStep.ON_ATTACHED)
    val botStepsPreUpdate =
        arrayOf(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT,
            LifecycleStep.ON_ATTACHED)
    val mountSpecStepsPreUpdate =
        arrayOf<LifecycleStep?>(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_ATTACHED,
            LifecycleStep.ON_CREATE_MOUNT_CONTENT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)

    // Root steps will be the same for all updates
    val expectedStepsForRootAllUpdates =
        arrayOf<LifecycleStep?>(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)

    // Mid steps will be the same for all updates
    val expectedStepsForMidAllUpdates =
        arrayOf(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)

    // Bot steps will be the same for all updates
    val expectedStepsForBotAllUpdates =
        arrayOf(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT)

    // MountSpec steps will be the same for all updates
    val expectedStepsForMountSpecAllUpdates =
        arrayOf<LifecycleStep?>(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.SHOULD_UPDATE,
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)

    // Testing OCLWSS -> OCLWSS -> OCL
    TestHierarchyBuilder.create(this, false, false, true)
        .setRootStepsPreUpdate(rootStepsPreUpdate)
        .setMidStepsPreUpdate(midStepsPreUpdate)
        .setBotStepsPreUpdate(botStepsPreUpdate)
        .setMountSpecStepsPreUpdate(mountSpecStepsPreUpdate)
        .setRootStepsUpdate1(expectedStepsForRootAllUpdates)
        .setMidStepsUpdate1(expectedStepsForMidAllUpdates)
        .setBotStepsUpdate1(expectedStepsForBotAllUpdates)
        .setMountSpecStepsUpdate1(expectedStepsForMountSpecAllUpdates)
        .setRootStepsUpdate2(expectedStepsForRootAllUpdates)
        .setMidStepsUpdate2(expectedStepsForMidAllUpdates)
        .setBotStepsUpdate2(expectedStepsForBotAllUpdates)
        .setMountSpecStepsUpdate2(expectedStepsForMountSpecAllUpdates)
        .setRootStepsUpdate3(expectedStepsForRootAllUpdates)
        .setMidStepsUpdate3(expectedStepsForMidAllUpdates)
        .setBotStepsUpdate3(expectedStepsForBotAllUpdates)
        .setMountSpecStepsUpdate3(expectedStepsForMountSpecAllUpdates)
        .test()
  }

  /**
   * Tests OCLWSS_OCL_OCLWSS hierarchy.
   *
   * @see NestedTreeResolutionWithStateTest class declaration documentation for additional details.
   */
  @Test
  fun test_OCLWSS_OCL_OCLWSS() {
    val rootStepsPreUpdate =
        arrayOf<LifecycleStep?>(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
            LifecycleStep.ON_ATTACHED)
    val botStepsPreUpdate =
        arrayOf(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
            LifecycleStep.ON_ATTACHED)
    val mountSpecStepsPreUpdate =
        arrayOf<LifecycleStep?>(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_ATTACHED,
            LifecycleStep.ON_CREATE_MOUNT_CONTENT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)

    // Root steps will be the same for all updates
    val expectedStepsForRootAllUpdates =
        arrayOf<LifecycleStep?>(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)

    // Mid steps will be the same for all updates
    val expectedStepsForMidAllUpdates =
        arrayOf(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT)

    // Bot steps will be the same for all updates
    val expectedStepsForBotAllUpdates =
        arrayOf(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)

    // MountSpec steps will be the same for all updates
    val expectedStepsForMountSpecAllUpdates =
        arrayOf<LifecycleStep?>(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.SHOULD_UPDATE,
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)

    // Testing OCLWSS -> OCL -> OCLWSS
    TestHierarchyBuilder.create(this, false, true, false)
        .setRootStepsPreUpdate(rootStepsPreUpdate)
        .setMidStepsPreUpdate(
            arrayOf(
                LifecycleStep.ON_CREATE_INITIAL_STATE,
                LifecycleStep.ON_CREATE_TREE_PROP,
                LifecycleStep.ON_CALCULATE_CACHED_VALUE,
                LifecycleStep.ON_CREATE_LAYOUT,
                LifecycleStep.ON_ATTACHED))
        .setBotStepsPreUpdate(botStepsPreUpdate)
        .setMountSpecStepsPreUpdate(mountSpecStepsPreUpdate)
        .setRootStepsUpdate1(expectedStepsForRootAllUpdates)
        .setMidStepsUpdate1(expectedStepsForMidAllUpdates)
        .setBotStepsUpdate1(expectedStepsForBotAllUpdates)
        .setMountSpecStepsUpdate1(expectedStepsForMountSpecAllUpdates)
        .setRootStepsUpdate2(expectedStepsForRootAllUpdates)
        .setMidStepsUpdate2(expectedStepsForMidAllUpdates)
        .setBotStepsUpdate2(expectedStepsForBotAllUpdates)
        .setMountSpecStepsUpdate2(expectedStepsForMountSpecAllUpdates)
        .setRootStepsUpdate3(expectedStepsForRootAllUpdates)
        .setMidStepsUpdate3(expectedStepsForMidAllUpdates)
        .setBotStepsUpdate3(expectedStepsForBotAllUpdates)
        .setMountSpecStepsUpdate3(expectedStepsForMountSpecAllUpdates)
        .test()
  }

  /**
   * Tests OCL_OCLWSS_OCLWSS hierarchy.
   *
   * @see NestedTreeResolutionWithStateTest class declaration documentation for additional details.
   */
  @Test
  fun test_OCL_OCLWSS_OCLWSS() {
    val midStepsPreUpdate =
        arrayOf(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
            LifecycleStep.ON_ATTACHED)
    val botStepsPreUpdate =
        arrayOf(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
            LifecycleStep.ON_ATTACHED)
    val mountSpecStepsPreUpdate =
        arrayOf<LifecycleStep?>(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_ATTACHED,
            LifecycleStep.ON_CREATE_MOUNT_CONTENT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)
    val midStepsForUpdate1 =
        arrayOf(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)
    val botStepsForUpdate1 =
        arrayOf(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)
    val mountSpecStepsForUpdate1 =
        arrayOf<LifecycleStep?>(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.SHOULD_UPDATE,
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)
    val rootStepsForUpdate2And3 = arrayOf<LifecycleStep?>()
    val midStepsForUpdate2And3 =
        arrayOf(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)
    val botStepsForUpdate2And3 =
        arrayOf(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)
    val mountSpecStepsForUpdate2And3 =
        arrayOf<LifecycleStep?>(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.SHOULD_UPDATE,
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)

    // Testing OCL -> OCLWSS -> OCLWSS
    TestHierarchyBuilder.create(this, true, false, false)
        .setRootStepsPreUpdate(
            arrayOf(
                LifecycleStep.ON_CREATE_INITIAL_STATE,
                LifecycleStep.ON_CREATE_TREE_PROP,
                LifecycleStep.ON_CALCULATE_CACHED_VALUE,
                LifecycleStep.ON_CREATE_LAYOUT,
                LifecycleStep.ON_ATTACHED))
        .setMidStepsPreUpdate(midStepsPreUpdate)
        .setBotStepsPreUpdate(botStepsPreUpdate)
        .setMountSpecStepsPreUpdate(mountSpecStepsPreUpdate)
        .setRootStepsUpdate1(
            arrayOf(
                LifecycleStep.ON_CREATE_TREE_PROP,
                LifecycleStep.ON_CALCULATE_CACHED_VALUE,
                LifecycleStep.ON_CREATE_LAYOUT))
        .setMidStepsUpdate1(midStepsForUpdate1)
        .setBotStepsUpdate1(botStepsForUpdate1)
        .setMountSpecStepsUpdate1(mountSpecStepsForUpdate1)
        .setRootStepsUpdate2(rootStepsForUpdate2And3)
        .setMidStepsUpdate2(midStepsForUpdate2And3)
        .setBotStepsUpdate2(botStepsForUpdate2And3)
        .setMountSpecStepsUpdate2(mountSpecStepsForUpdate2And3)
        .setRootStepsUpdate3(rootStepsForUpdate2And3)
        .setMidStepsUpdate3(midStepsForUpdate2And3)
        .setBotStepsUpdate3(botStepsForUpdate2And3)
        .setMountSpecStepsUpdate3(mountSpecStepsForUpdate2And3)
        .test()
  }

  /**
   * Tests OCLWSS_OCLWSS_OCLWSS hierarchy.
   *
   * @see NestedTreeResolutionWithStateTest class declaration documentation for additional details.
   */
  @Test
  fun test_OCLWSS_OCLWSS_OCLWSS() {
    val rootStepsPreUpdate =
        arrayOf<LifecycleStep?>(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
            LifecycleStep.ON_ATTACHED)
    val midStepsPreUpdate =
        arrayOf(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
            LifecycleStep.ON_ATTACHED)
    val botStepsPreUpdate =
        arrayOf(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
            LifecycleStep.ON_ATTACHED)
    val mountSpecStepsPreUpdate =
        arrayOf<LifecycleStep?>(
            LifecycleStep.ON_CREATE_INITIAL_STATE,
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.ON_ATTACHED,
            LifecycleStep.ON_CREATE_MOUNT_CONTENT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)

    // Root steps will be the same for all updates
    val expectedStepsForRootAllUpdates =
        arrayOf<LifecycleStep?>(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)

    // Mid steps will be the same for all updates
    val expectedStepsForMidAllUpdates =
        arrayOf(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)

    // Bot steps will be the same for all updates
    val expectedStepsForBotAllUpdates =
        arrayOf(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_CALCULATE_CACHED_VALUE,
            LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC)

    // MountSpec steps will be the same for all updates
    val expectedStepsForMountSpecAllUpdates =
        arrayOf<LifecycleStep?>(
            LifecycleStep.ON_CREATE_TREE_PROP,
            LifecycleStep.ON_PREPARE,
            LifecycleStep.ON_MEASURE,
            LifecycleStep.ON_BOUNDS_DEFINED,
            LifecycleStep.SHOULD_UPDATE,
            LifecycleStep.ON_UNBIND,
            LifecycleStep.ON_UNMOUNT,
            LifecycleStep.ON_MOUNT,
            LifecycleStep.ON_BIND)

    // Testing OCLWSS -> OCLWSS -> OCLWSS
    TestHierarchyBuilder.create(this, false, false, false)
        .setRootStepsPreUpdate(rootStepsPreUpdate)
        .setMidStepsPreUpdate(midStepsPreUpdate)
        .setBotStepsPreUpdate(botStepsPreUpdate)
        .setMountSpecStepsPreUpdate(mountSpecStepsPreUpdate)
        .setRootStepsUpdate1(expectedStepsForRootAllUpdates)
        .setMidStepsUpdate1(expectedStepsForMidAllUpdates)
        .setBotStepsUpdate1(expectedStepsForBotAllUpdates)
        .setMountSpecStepsUpdate1(expectedStepsForMountSpecAllUpdates)
        .setRootStepsUpdate2(expectedStepsForRootAllUpdates)
        .setMidStepsUpdate2(expectedStepsForMidAllUpdates)
        .setBotStepsUpdate2(expectedStepsForBotAllUpdates)
        .setMountSpecStepsUpdate2(expectedStepsForMountSpecAllUpdates)
        .setRootStepsUpdate3(expectedStepsForRootAllUpdates)
        .setMidStepsUpdate3(expectedStepsForMidAllUpdates)
        .setBotStepsUpdate3(expectedStepsForBotAllUpdates)
        .setMountSpecStepsUpdate3(expectedStepsForMountSpecAllUpdates)
        .test()
  }

  /**
   * Consolidated method to test all cases in this test file. Called from the Builder class.
   *
   * @param isRootOCL True if the Root is OnCreateLayout. False for OnCreateLayoutWithSizeSpec
   * @param isMidOCL True if the Mid is OnCreateLayout. False for OnCreateLayoutWithSizeSpec
   * @param isBotOCL True if the Bot is OnCreateLayout. False for OnCreateLayoutWithSizeSpec
   * @param rootStepsPreStateUpdate Expected LifecycleSteps for root, pre-update
   * @param midStepsPreStateUpdate Expected LifecycleSteps for mid, pre-update
   * @param botStepsPreStateUpdate Expected LifecycleSteps for bot, pre-update
   * @param mountSpecStepsPreStateUpdate Expected LifecycleSteps for mount-spec, pre-update
   * @param rootStepsStateUpdate1 Expected LifecycleSteps for root, post-update-1
   * @param midStepsStateUpdate1 Expected LifecycleSteps for mid, post-update-1
   * @param botStepsStateUpdate1 Expected LifecycleSteps for bot, post-update-1
   * @param mountSpecStateUpdate1 Expected LifecycleSteps for mount-spec, post-update-1
   * @param rootStepsStateUpdate2 Expected LifecycleSteps for root, post-update-2
   * @param midStepsStateUpdate2 Expected LifecycleSteps for mid, post-update-2
   * @param botStepsStateUpdate2 Expected LifecycleSteps for bot, post-update-2
   * @param mountSpecStateUpdate2 Expected LifecycleSteps for mount-spec, post-update-2
   * @param rootStepsStateUpdate3 Expected LifecycleSteps for root, post-update-3
   * @param midStepsStateUpdate3 Expected LifecycleSteps for mid, post-update-3
   * @param botStepsStateUpdate3 Expected LifecycleSteps for bot, post-update-3
   * @param mountSpecStateUpdate3 Expected LifecycleSteps for mount-spec, post-update-3
   * @see NestedTreeResolutionWithStateTest.TestHierarchyBuilder
   * @see NestedTreeResolutionWithStateTest class declaration documentation for additional details.
   */
  private fun testSpecificSetup(
      isRootOCL: Boolean,
      isMidOCL: Boolean,
      isBotOCL: Boolean,
      rootStepsPreStateUpdate: Array<LifecycleStep?>,
      midStepsPreStateUpdate: Array<LifecycleStep>,
      botStepsPreStateUpdate: Array<LifecycleStep>,
      mountSpecStepsPreStateUpdate: Array<LifecycleStep?>,
      rootStepsStateUpdate1: Array<LifecycleStep?>,
      midStepsStateUpdate1: Array<LifecycleStep>,
      botStepsStateUpdate1: Array<LifecycleStep>,
      mountSpecStateUpdate1: Array<LifecycleStep?>,
      rootStepsStateUpdate2: Array<LifecycleStep?>,
      midStepsStateUpdate2: Array<LifecycleStep>,
      botStepsStateUpdate2: Array<LifecycleStep>,
      mountSpecStateUpdate2: Array<LifecycleStep?>,
      rootStepsStateUpdate3: Array<LifecycleStep?>,
      midStepsStateUpdate3: Array<LifecycleStep>,
      botStepsStateUpdate3: Array<LifecycleStep>,
      mountSpecStateUpdate3: Array<LifecycleStep?>
  ) {
    val c = legacyLithoViewRule.context
    val widthSpec = exactly(500)
    val heightSpec = exactly(500)

    // Generate a component + state-update-callers for requested setup.
    val holder =
        createComponentHierarchySetup(c, widthSpec, heightSpec, isRootOCL, isMidOCL, isBotOCL)

    // Set the root and layout
    legacyLithoViewRule.setRoot(holder.component)
    legacyLithoViewRule.setSizeSpecs(widthSpec, heightSpec)
    legacyLithoViewRule.attachToWindow().measure().layout()

    // Ensure the root node is not null. We'll need this to extract scoped contexts to simulate
    // state updates.
    assertThat(legacyLithoViewRule.currentRootNode).isNotNull

    // Ensure the lifecycle steps are as expected before any state updates.
    var rootSteps = LifecycleStep.getSteps(holder.rootLayoutSpecSteps)
    var midSteps = LifecycleStep.getSteps(holder.midLayoutSpecSteps)
    var botSteps = LifecycleStep.getSteps(holder.botLayoutSpecSteps)
    var mountableSteps = holder.mountableLifecycleTracker.steps
    assertThat(rootSteps)
        .describedAs("Root steps pre update")
        .containsExactly(*rootStepsPreStateUpdate)
    assertThat(midSteps)
        .describedAs("Mid steps pre update")
        .containsExactly(*midStepsPreStateUpdate)
    assertThat(botSteps)
        .describedAs("Bot steps pre update")
        .containsExactly(*botStepsPreStateUpdate)
    assertThat(mountableSteps)
        .describedAs("MountSpec steps pre update")
        .containsExactly(*mountSpecStepsPreStateUpdate)

    // Ensure all texts showing initial states are as expected
    assertThat(legacyLithoViewRule.findViewWithText("root 0")).isNotNull
    assertThat(legacyLithoViewRule.findViewWithText("mid 0")).isNotNull
    assertThat(legacyLithoViewRule.findViewWithText("bot 0")).isNotNull

    // Test state update 1/3
    // Reset the lifecycle steps
    holder.clearAllSteps()

    // Extract the root component's context to simulate state updates
    val rootScopedContext = rootComponentContext

    // Simulate the state update for the root.
    holder.rootStateCaller.increment(rootScopedContext)

    // Ensure lifecycle steps are as expected after the state update
    rootSteps = LifecycleStep.getSteps(holder.rootLayoutSpecSteps)
    midSteps = LifecycleStep.getSteps(holder.midLayoutSpecSteps)
    botSteps = LifecycleStep.getSteps(holder.botLayoutSpecSteps)
    mountableSteps = holder.mountableLifecycleTracker.steps
    assertThat(rootSteps)
        .describedAs("Root steps after update 1")
        .containsExactly(*rootStepsStateUpdate1)
    assertThat(midSteps)
        .describedAs("Mid steps after update 1")
        .containsExactly(*midStepsStateUpdate1)
    assertThat(botSteps)
        .describedAs("Bot steps after update 1")
        .containsExactly(*botStepsStateUpdate1)
    assertThat(mountableSteps)
        .describedAs("MountSpec steps after update 1")
        .containsExactly(*mountSpecStateUpdate1)

    // Ensure the texts properly reflect the correct values after the state update
    assertThat(legacyLithoViewRule.findViewWithText("root 1")).isNotNull // Updated!
    assertThat(legacyLithoViewRule.findViewWithText("mid 0")).isNotNull
    assertThat(legacyLithoViewRule.findViewWithText("bot 0")).isNotNull

    // Test state update 2/3
    // Reset the lifecycle steps
    holder.clearAllSteps()

    // Extract the mid component's context to simulate state updates
    val midScopedContext = midComponentContext

    // Simulate the state update for the mid component.
    holder.midStateCaller.increment(midScopedContext)

    // Ensure lifecycle steps are as expected after the state update
    rootSteps = LifecycleStep.getSteps(holder.rootLayoutSpecSteps)
    midSteps = LifecycleStep.getSteps(holder.midLayoutSpecSteps)
    botSteps = LifecycleStep.getSteps(holder.botLayoutSpecSteps)
    mountableSteps = holder.mountableLifecycleTracker.steps
    assertThat(rootSteps)
        .describedAs("Root steps after update 2")
        .containsExactly(*rootStepsStateUpdate2)
    assertThat(midSteps)
        .describedAs("Mid steps after update 2")
        .containsExactly(*midStepsStateUpdate2)
    assertThat(botSteps)
        .describedAs("Bot steps after update 2")
        .containsExactly(*botStepsStateUpdate2)
    assertThat(mountableSteps)
        .describedAs("MountSpec steps after update 2")
        .containsExactly(*mountSpecStateUpdate2)

    // Ensure the texts properly reflect the correct values after the state update
    assertThat(legacyLithoViewRule.findViewWithText("root 1")).isNotNull
    assertThat(legacyLithoViewRule.findViewWithText("mid 1")).isNotNull // Updated!
    assertThat(legacyLithoViewRule.findViewWithText("bot 0")).isNotNull

    // Test state update 3/3
    // Reset the lifecycle steps
    holder.clearAllSteps()

    // Extract the bottom component's context to simulate state updates
    val botScopedContext = botComponentContext

    // Simulate the state update for the bottom component.
    holder.botStateCaller.increment(botScopedContext)

    // Ensure lifecycle steps are as expected after the state update
    rootSteps = LifecycleStep.getSteps(holder.rootLayoutSpecSteps)
    midSteps = LifecycleStep.getSteps(holder.midLayoutSpecSteps)
    botSteps = LifecycleStep.getSteps(holder.botLayoutSpecSteps)
    mountableSteps = holder.mountableLifecycleTracker.steps
    assertThat(rootSteps)
        .describedAs("Root steps after update 3")
        .containsExactly(*rootStepsStateUpdate3)
    assertThat(midSteps)
        .describedAs("Mid steps after update 3")
        .containsExactly(*midStepsStateUpdate3)
    assertThat(botSteps)
        .describedAs("Bot steps after update 3")
        .containsExactly(*botStepsStateUpdate3)
    assertThat(mountableSteps)
        .describedAs("MountSpec steps after update 3")
        .containsExactly(*mountSpecStateUpdate3)

    // Ensure the texts properly reflect the correct values after the state update
    assertThat(legacyLithoViewRule.findViewWithText("root 1")).isNotNull
    assertThat(legacyLithoViewRule.findViewWithText("mid 1")).isNotNull
    assertThat(legacyLithoViewRule.findViewWithText("bot 1")).isNotNull // Updated!
  }

  /** Returns the ComponentContext to be used to trigger state-updates on the Root. */
  private val rootComponentContext: ComponentContext
    get() {
      assertThat(legacyLithoViewRule.currentRootNode).isNotNull
      return requireNotNull(
          getCorrectLayoutResult(legacyLithoViewRule.currentRootNode)
              ?.mNode
              ?.getComponentContextAt(1))
    }

  /** Returns the ComponentContext to be used to trigger state-updates on the Mid */
  private val midComponentContext: ComponentContext
    get() {
      val rootLayoutResult = legacyLithoViewRule.currentRootNode
      assertThat(rootLayoutResult).isNotNull
      return requireNotNull(
          if (rootLayoutResult is NestedTreeHolderResult) {
            val nestedResult = getCorrectLayoutResult(rootLayoutResult)
            val nestedMidResult = getCorrectLayoutResult(nestedResult?.getChildAt(1))
            assertThat(nestedMidResult).isNotNull
            nestedMidResult?.mNode?.getComponentContextAt(1)
          } else {
            rootLayoutResult?.mNode?.getChildAt(1)?.getComponentContextAt(0)
          })
    }

  /** Returns the ComponentContext to be used to trigger state-updates on the Bot */
  private val botComponentContext: ComponentContext
    get() {
      val rootLayoutResult = legacyLithoViewRule.currentRootNode
      assertThat(rootLayoutResult).isNotNull
      return requireNotNull(
          if (rootLayoutResult is NestedTreeHolderResult) {
            val nestedResult = getCorrectLayoutResult(rootLayoutResult)
            val nestedMidResult = getCorrectLayoutResult(nestedResult?.getChildAt(1))
            assertThat(nestedMidResult).isNotNull
            val nestedBotResult = getCorrectLayoutResult(nestedMidResult?.getChildAt(1))
            getCorrectLayoutResult(nestedBotResult)?.mNode?.getComponentContextAt(1)
          } else {
            val midResult = rootLayoutResult?.getChildAt(1)
            if (midResult is NestedTreeHolderResult) {
              val nestedResult = getCorrectLayoutResult(midResult)
              val nestedBotResult = getCorrectLayoutResult(nestedResult?.getChildAt(1))
              getCorrectLayoutResult(nestedBotResult)?.mNode?.getComponentContextAt(1)
            } else {
              rootLayoutResult?.mNode?.getChildAt(1)?.getChildAt(1)?.getComponentContextAt(0)
            }
          })
    }

  /**
   * Holder class for a component hierarchy described above. Holds the root component, state update
   * callers for each 3 components, and step info arrays for each component including the leaf
   * MountSpec.
   */
  class StateUpdateComponentHolder(
      val component: Component,
      rootStateCaller: BaseIncrementStateCaller,
      midStateCaller: BaseIncrementStateCaller,
      botStateCaller: BaseIncrementStateCaller,
      rootLayoutSpecSteps: MutableList<StepInfo>,
      midLayoutSpecSteps: MutableList<StepInfo>,
      botLayoutSpecSteps: MutableList<StepInfo>,
      mountableLifecycleTracker: LifecycleTracker
  ) {
    val rootStateCaller: BaseIncrementStateCaller
    val midStateCaller: BaseIncrementStateCaller
    val botStateCaller: BaseIncrementStateCaller
    val rootLayoutSpecSteps: MutableList<StepInfo>
    val midLayoutSpecSteps: MutableList<StepInfo>
    val botLayoutSpecSteps: MutableList<StepInfo>
    val mountableLifecycleTracker: LifecycleTracker

    /** Resets all lifecycle steps for all components. */
    fun clearAllSteps() {
      rootLayoutSpecSteps.clear()
      midLayoutSpecSteps.clear()
      botLayoutSpecSteps.clear()
      mountableLifecycleTracker.reset()
    }

    init {
      this.rootStateCaller = rootStateCaller
      this.midStateCaller = midStateCaller
      this.botStateCaller = botStateCaller
      this.rootLayoutSpecSteps = rootLayoutSpecSteps
      this.midLayoutSpecSteps = midLayoutSpecSteps
      this.botLayoutSpecSteps = botLayoutSpecSteps
      this.mountableLifecycleTracker = mountableLifecycleTracker
    }
  }

  /** Builder class used to prepare and trigger each individual test in this file. */
  private class TestHierarchyBuilder {
    private var testRunner: NestedTreeResolutionWithStateTest? = null
    private var isRootOCL = false
    private var isMidOCL = false
    private var isBotOCL = false
    private lateinit var rootStepsPreStateUpdate: Array<LifecycleStep?>
    private lateinit var midStepsPreStateUpdate: Array<LifecycleStep>
    private lateinit var botStepsPreStateUpdate: Array<LifecycleStep>
    private lateinit var mountSpecStepsPreStateUpdate: Array<LifecycleStep?>
    private lateinit var rootStepsStateUpdate1: Array<LifecycleStep?>
    private lateinit var midStepsStateUpdate1: Array<LifecycleStep>
    private lateinit var botStepsStateUpdate1: Array<LifecycleStep>
    private lateinit var mountSpecStateUpdate1: Array<LifecycleStep?>
    private lateinit var rootStepsStateUpdate2: Array<LifecycleStep?>
    private lateinit var midStepsStateUpdate2: Array<LifecycleStep>
    private lateinit var botStepsStateUpdate2: Array<LifecycleStep>
    private lateinit var mountSpecStateUpdate2: Array<LifecycleStep?>
    private lateinit var rootStepsStateUpdate3: Array<LifecycleStep?>
    private lateinit var midStepsStateUpdate3: Array<LifecycleStep>
    private lateinit var botStepsStateUpdate3: Array<LifecycleStep>
    private lateinit var mountSpecStateUpdate3: Array<LifecycleStep?>

    fun setRootStepsPreUpdate(steps: Array<LifecycleStep?>): TestHierarchyBuilder {
      rootStepsPreStateUpdate = steps
      return this
    }

    fun setMidStepsPreUpdate(steps: Array<LifecycleStep>): TestHierarchyBuilder {
      midStepsPreStateUpdate = steps
      return this
    }

    fun setBotStepsPreUpdate(steps: Array<LifecycleStep>): TestHierarchyBuilder {
      botStepsPreStateUpdate = steps
      return this
    }

    fun setMountSpecStepsPreUpdate(steps: Array<LifecycleStep?>): TestHierarchyBuilder {
      mountSpecStepsPreStateUpdate = steps
      return this
    }

    fun setRootStepsUpdate1(steps: Array<LifecycleStep?>): TestHierarchyBuilder {
      rootStepsStateUpdate1 = steps
      return this
    }

    fun setMidStepsUpdate1(steps: Array<LifecycleStep>): TestHierarchyBuilder {
      midStepsStateUpdate1 = steps
      return this
    }

    fun setBotStepsUpdate1(steps: Array<LifecycleStep>): TestHierarchyBuilder {
      botStepsStateUpdate1 = steps
      return this
    }

    fun setMountSpecStepsUpdate1(steps: Array<LifecycleStep?>): TestHierarchyBuilder {
      mountSpecStateUpdate1 = steps
      return this
    }

    fun setRootStepsUpdate2(steps: Array<LifecycleStep?>): TestHierarchyBuilder {
      rootStepsStateUpdate2 = steps
      return this
    }

    fun setMidStepsUpdate2(steps: Array<LifecycleStep>): TestHierarchyBuilder {
      midStepsStateUpdate2 = steps
      return this
    }

    fun setBotStepsUpdate2(steps: Array<LifecycleStep>): TestHierarchyBuilder {
      botStepsStateUpdate2 = steps
      return this
    }

    fun setMountSpecStepsUpdate2(steps: Array<LifecycleStep?>): TestHierarchyBuilder {
      mountSpecStateUpdate2 = steps
      return this
    }

    fun setRootStepsUpdate3(steps: Array<LifecycleStep?>): TestHierarchyBuilder {
      rootStepsStateUpdate3 = steps
      return this
    }

    fun setMidStepsUpdate3(steps: Array<LifecycleStep>): TestHierarchyBuilder {
      midStepsStateUpdate3 = steps
      return this
    }

    fun setBotStepsUpdate3(steps: Array<LifecycleStep>): TestHierarchyBuilder {
      botStepsStateUpdate3 = steps
      return this
    }

    fun setMountSpecStepsUpdate3(steps: Array<LifecycleStep?>): TestHierarchyBuilder {
      mountSpecStateUpdate3 = steps
      return this
    }

    fun test() {
      testRunner?.testSpecificSetup(
          isRootOCL,
          isMidOCL,
          isBotOCL,
          rootStepsPreStateUpdate,
          midStepsPreStateUpdate,
          botStepsPreStateUpdate,
          mountSpecStepsPreStateUpdate,
          rootStepsStateUpdate1,
          midStepsStateUpdate1,
          botStepsStateUpdate1,
          mountSpecStateUpdate1,
          rootStepsStateUpdate2,
          midStepsStateUpdate2,
          botStepsStateUpdate2,
          mountSpecStateUpdate2,
          rootStepsStateUpdate3,
          midStepsStateUpdate3,
          botStepsStateUpdate3,
          mountSpecStateUpdate3)
    }

    companion object {
      @JvmStatic
      fun create(
          testRunner: NestedTreeResolutionWithStateTest?,
          isRootOCL: Boolean,
          isMidOCL: Boolean,
          isBotOCL: Boolean
      ): TestHierarchyBuilder {
        val builder = TestHierarchyBuilder()
        builder.isRootOCL = isRootOCL
        builder.isMidOCL = isMidOCL
        builder.isBotOCL = isBotOCL
        builder.testRunner = testRunner
        return builder
      }
    }
  }

  companion object {
    /**
     * Generate a component hierarchy as described above.
     *
     * @param c the component context
     * @param widthSpec the width-spec to use in Component.measure calls
     * @param heightSpec the height-spec to use in Component.measure calls
     * @param isRootOCL true if the root should be OCL (OCLWSS otherwise)
     * @param isMidOCL true if the mid should be OCL (OCLWSS otherwise)
     * @param isBottomOCL true if the bot should be OCL (OCLWSS otherise)
     * @return a holder class containing the root component, state update callers for each comp, and
     *   lifecycle step arrays for each component including the MountSpec held by the bot comp.
     */
    private fun createComponentHierarchySetup(
        c: ComponentContext,
        widthSpec: Int,
        heightSpec: Int,
        isRootOCL: Boolean,
        isMidOCL: Boolean,
        isBottomOCL: Boolean
    ): StateUpdateComponentHolder {
      val rootStepsInfo: MutableList<StepInfo> = ArrayList<StepInfo>()
      val midStepsInfo: MutableList<StepInfo> = ArrayList<StepInfo>()
      val botStepsInfo: MutableList<StepInfo> = ArrayList<StepInfo>()
      val mountableLifecycleTracker = LifecycleTracker()
      val mountable =
          MountSpecPureRenderLifecycleTester.create(c)
              .lifecycleTracker(mountableLifecycleTracker)
              .build()
      val rootCaller: BaseIncrementStateCaller
      val midCaller: BaseIncrementStateCaller
      val bottomCaller: BaseIncrementStateCaller
      val rootComponent: Component
      val midComponent: Component
      val bottomComponent: Component

      // bottom comp will set half height to make room for text
      if (isBottomOCL) {
        bottomCaller = ComponentWithMeasureCallAndStateSpec.Caller()
        bottomComponent =
            ComponentWithMeasureCallAndState.create(c)
                .steps(botStepsInfo)
                .shouldCacheResult(true)
                .widthPx(SizeSpec.getSize(widthSpec) / 2)
                .heightPx(SizeSpec.getSize(heightSpec) / 2)
                .prefix("bot")
                .mountSpec(mountable)
                .build()
      } else {
        bottomCaller = ComponentWithSizeAndMeasureCallAndStateSpec.Caller()
        bottomComponent =
            ComponentWithSizeAndMeasureCallAndState.create(c)
                .steps(botStepsInfo)
                .shouldCacheResult(true)
                .widthPx(SizeSpec.getSize(widthSpec) / 2)
                .heightPx(SizeSpec.getSize(heightSpec) / 2)
                .prefix("bot")
                .mountSpec(mountable)
                .build()
      }
      if (isMidOCL) {
        midCaller = ComponentWithMeasureCallAndStateSpec.Caller()
        midComponent =
            ComponentWithMeasureCallAndState.create(c)
                .steps(midStepsInfo)
                .shouldCacheResult(true)
                .component(bottomComponent)
                .widthSpec(widthSpec)
                .heightSpec(heightSpec)
                .prefix("mid")
                .build()
      } else {
        midCaller = ComponentWithSizeAndMeasureCallAndStateSpec.Caller()
        midComponent =
            ComponentWithSizeAndMeasureCallAndState.create(c)
                .steps(midStepsInfo)
                .shouldCacheResult(true)
                .component(bottomComponent)
                .prefix("mid")
                .build()
      }
      if (isRootOCL) {
        rootCaller = ComponentWithMeasureCallAndStateSpec.Caller()
        rootComponent =
            ComponentWithMeasureCallAndState.create(c)
                .steps(rootStepsInfo)
                .shouldCacheResult(true)
                .component(midComponent)
                .widthSpec(widthSpec)
                .heightSpec(heightSpec)
                .prefix("root")
                .build()
      } else {
        rootCaller = ComponentWithSizeAndMeasureCallAndStateSpec.Caller()
        rootComponent =
            ComponentWithSizeAndMeasureCallAndState.create(c)
                .steps(rootStepsInfo)
                .shouldCacheResult(true)
                .component(midComponent)
                .prefix("root")
                .build()
      }
      return StateUpdateComponentHolder(
          rootComponent,
          rootCaller,
          midCaller,
          bottomCaller,
          rootStepsInfo,
          midStepsInfo,
          botStepsInfo,
          mountableLifecycleTracker)
    }

    /**
     * Extracts the Nested-Result from a NestedTreeHolderResult if the provided LithoLayoutResult is
     * of that type, otherwise simply returns the provided LithoLayoutResult.
     *
     * Used as a helper method to extract the correct ComponentContext for state-updates for Root,
     * Mid or Bot.
     */
    private fun getCorrectLayoutResult(from: LithoLayoutResult?): LithoLayoutResult? =
        if (from is NestedTreeHolderResult) {
          val nestedResult = from.nestedResult
          assertThat(nestedResult).isNotNull
          nestedResult
        } else {
          from
        }
  }
}
