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

import static com.facebook.litho.LifecycleStep.ON_ATTACHED;
import static com.facebook.litho.LifecycleStep.ON_BIND;
import static com.facebook.litho.LifecycleStep.ON_BOUNDS_DEFINED;
import static com.facebook.litho.LifecycleStep.ON_CALCULATE_CACHED_VALUE;
import static com.facebook.litho.LifecycleStep.ON_CREATE_INITIAL_STATE;
import static com.facebook.litho.LifecycleStep.ON_CREATE_LAYOUT;
import static com.facebook.litho.LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC;
import static com.facebook.litho.LifecycleStep.ON_CREATE_MOUNT_CONTENT;
import static com.facebook.litho.LifecycleStep.ON_CREATE_TREE_PROP;
import static com.facebook.litho.LifecycleStep.ON_DETACHED;
import static com.facebook.litho.LifecycleStep.ON_MEASURE;
import static com.facebook.litho.LifecycleStep.ON_MOUNT;
import static com.facebook.litho.LifecycleStep.ON_PREPARE;
import static com.facebook.litho.LifecycleStep.ON_UNBIND;
import static com.facebook.litho.LifecycleStep.ON_UNMOUNT;
import static com.facebook.litho.LifecycleStep.SHOULD_UPDATE;
import static com.facebook.litho.LifecycleStep.getSteps;
import static org.assertj.core.api.Assertions.assertThat;

import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.stateupdates.BaseIncrementStateCaller;
import com.facebook.litho.stateupdates.ComponentWithMeasureCallAndState;
import com.facebook.litho.stateupdates.ComponentWithMeasureCallAndStateSpec;
import com.facebook.litho.stateupdates.ComponentWithSizeAndMeasureCallAndState;
import com.facebook.litho.stateupdates.ComponentWithSizeAndMeasureCallAndStateSpec;
import com.facebook.litho.testing.LegacyLithoViewRule;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import com.facebook.litho.widget.MountSpecPureRenderLifecycleTester;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.LooperMode;

/**
 * In this test class, all tests will test for complex component hierarchies of the following form:
 * Root --> Mid --> Bot Where Root, Mid and Bot can either be an OnCreateLayout component, or an
 * OnCreateLayoutWithSizeSpec component.
 *
 * <p>All 3 components hold an int state value, initialized as zero. Each component will render a
 * Text displaying a prefix ("root", "mid", "bot") + the value of the state value as their 1st
 * child. Root will accept Mid as a @Prop component, will call Component.measure on it, and use it
 * as it's 2nd child. Mid will accept Bot as a @Prop component, will call Component.measure on it,
 * and use it as it's 2nd child. Bot will accept a MountSpec component and use it as it's 2nd child.
 *
 * <p>All tests will follow the same flow: 1. Build such a hierarchy, each test will have a
 * different variation of OCL / OCLWSS comps 2. Ensure lifecycle steps for each comp + the MountSpec
 * are as expected 3. Ensure all texts are displaying the correct state 4. Update state on the root
 * comp, and repeat #2 and #3 5. Update state on the mid comp, and repeat #2 and #3 6. Update state
 * on the bot comp, and repeat #2 and #3
 *
 * <p>The name of each test will indicate the hierarchy being tested. For example, a test named
 * "test_OCLWSS_OCL_OCL" will hold the hierarchy of: Root as OCLWSS, Mid as OCL, Bot as OCL
 */
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner.class)
public class NestedTreeResolutionWithStateTest {

  public final @Rule LegacyLithoViewRule mLegacyLithoViewRule = new LegacyLithoViewRule();
  private boolean defaultIsSplitStateHandlersEnabled;

  @Before
  public void before() {
    defaultIsSplitStateHandlersEnabled = ComponentsConfiguration.isSplitStateHandlersEnabled;
    ComponentsConfiguration.isSplitStateHandlersEnabled = false;
    ComponentsConfiguration.isEndToEndTestRun = true;
  }

  @After
  public void after() {
    ComponentsConfiguration.isSplitStateHandlersEnabled = defaultIsSplitStateHandlersEnabled;
  }

  @Test
  public void test_OCL_OCL_OCL() {
    // Mid and bot steps will be the same on preUpdate
    final LifecycleStep[] expectedStepsForMidAndBotPreUpdate =
        new LifecycleStep[] {
          ON_CREATE_INITIAL_STATE,
          ON_CREATE_TREE_PROP,
          ON_CALCULATE_CACHED_VALUE,
          ON_CREATE_LAYOUT,
          ON_CREATE_INITIAL_STATE,
          ON_CREATE_TREE_PROP, // TODO (T133075661) duplicate OCTPs
          ON_ATTACHED
        };

    // Mid and bot steps will be the same on update 1
    final LifecycleStep[] expectedStepsForMidAndBotUpdate1 =
        new LifecycleStep[] {
          // TODO (T133075661) duplicate OCTPs
          ON_CREATE_TREE_PROP, ON_CALCULATE_CACHED_VALUE, ON_CREATE_LAYOUT, ON_CREATE_TREE_PROP
        };

    // Testing OCL -> OCL -> OCL
    TestHierarchyBuilder.create(this, true, true, true)
        .setRootStepsPreUpdate(
            new LifecycleStep[] {
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_CREATE_LAYOUT,
              ON_ATTACHED
            })
        .setMidStepsPreUpdate(expectedStepsForMidAndBotPreUpdate)
        .setBotStepsPreUpdate(expectedStepsForMidAndBotPreUpdate)
        .setMountSpecStepsPreUpdate(
            new LifecycleStep[] {
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_PREPARE,
              ON_MEASURE,
              ON_BOUNDS_DEFINED,
              ON_ATTACHED,
              ON_CREATE_MOUNT_CONTENT,
              ON_MOUNT,
              ON_BIND
            })
        .setRootStepsUpdate1(
            new LifecycleStep[] {
              // TODO (T133075661) duplicate OCTPs
              ON_CREATE_TREE_PROP, ON_CREATE_TREE_PROP, ON_CALCULATE_CACHED_VALUE, ON_CREATE_LAYOUT
            })
        .setMidStepsUpdate1(expectedStepsForMidAndBotUpdate1)
        .setBotStepsUpdate1(expectedStepsForMidAndBotUpdate1)
        .setMountSpecStepsUpdate1(
            new LifecycleStep[] {
              ON_CREATE_TREE_PROP,
              ON_PREPARE,
              ON_MEASURE,
              ON_BOUNDS_DEFINED,
              SHOULD_UPDATE,
              ON_UNBIND,
              ON_UNMOUNT,
              ON_MOUNT,
              ON_BIND
            })
        // TODO (T133075661) expected empty, no OCTP
        .setRootStepsUpdate2(new LifecycleStep[] {ON_CREATE_TREE_PROP})
        .setMidStepsUpdate2(
            new LifecycleStep[] {
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE, // TODO (T133075661) unexpected OCCV
              ON_CREATE_LAYOUT,
              ON_DETACHED,
              ON_ATTACHED
            })
        .setBotStepsUpdate2(
            new LifecycleStep[] {
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_CREATE_LAYOUT,
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_DETACHED,
              ON_ATTACHED
            })
        .setMountSpecStepsUpdate2(
            new LifecycleStep[] {
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_PREPARE,
              ON_MEASURE,
              ON_BOUNDS_DEFINED,
              ON_DETACHED,
              ON_ATTACHED,
              ON_UNBIND,
              ON_UNMOUNT,
              ON_MOUNT,
              ON_BIND
            })
        .setRootStepsUpdate3(new LifecycleStep[] {ON_CREATE_TREE_PROP})
        .setMidStepsUpdate3(new LifecycleStep[] {}) // empty
        .setBotStepsUpdate3(
            new LifecycleStep[] {
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_CREATE_LAYOUT,
              ON_DETACHED,
              ON_ATTACHED
            })
        .setMountSpecStepsUpdate3(
            new LifecycleStep[] {
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_PREPARE,
              SHOULD_UPDATE,
              ON_MEASURE,
              ON_BOUNDS_DEFINED,
              ON_DETACHED,
              ON_ATTACHED,
              ON_UNBIND,
              ON_UNMOUNT,
              ON_MOUNT,
              ON_BIND
            })
        .test();
  }

  @Test
  public void test_OCLWSS_OCL_OCL() {
    final LifecycleStep[] rootStepsPreUpdate =
        ComponentsConfiguration.shouldAlwaysResolveNestedTreeInMeasure
            ? new LifecycleStep[] {
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CREATE_TREE_PROP, // TODO (T133075661) duplicate OCTPs
              ON_CALCULATE_CACHED_VALUE,
              ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
              ON_ATTACHED
            }
            : new LifecycleStep[] {
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
              ON_ATTACHED
            };

    // Mid and bot steps will be the same pre update
    final LifecycleStep[] expectedStepsForMidAndBotPreUpdate =
        new LifecycleStep[] {
          ON_CREATE_INITIAL_STATE,
          ON_CREATE_TREE_PROP,
          ON_CALCULATE_CACHED_VALUE,
          ON_CREATE_LAYOUT,
          ON_CREATE_INITIAL_STATE,
          ON_CREATE_TREE_PROP,
          ON_ATTACHED
        };

    // Mid and bot steps will be the same for all updates
    final LifecycleStep[] expectedStepsForMidAndBotAllUpdates =
        new LifecycleStep[] {
          ON_CREATE_TREE_PROP, ON_CALCULATE_CACHED_VALUE, ON_CREATE_LAYOUT, ON_CREATE_TREE_PROP
        };

    // Root steps will be the same for all updates
    final LifecycleStep[] expectedStepsForRootAllUpdates =
        new LifecycleStep[] {
          ON_CREATE_TREE_PROP,
          ON_CREATE_TREE_PROP,
          ON_CREATE_TREE_PROP, // TODO (T133075661) duplicate OCTPs
          ON_CALCULATE_CACHED_VALUE,
          ON_CREATE_LAYOUT_WITH_SIZE_SPEC
        };

    // MountSpec steps will be the same for all updates
    final LifecycleStep[] expectedStepsForMountSpecAllUpdates =
        new LifecycleStep[] {
          ON_CREATE_TREE_PROP,
          ON_PREPARE,
          ON_MEASURE,
          ON_BOUNDS_DEFINED,
          SHOULD_UPDATE,
          ON_UNBIND,
          ON_UNMOUNT,
          ON_MOUNT,
          ON_BIND
        };

    // Testing OCLWSS -> OCL -> OCL
    TestHierarchyBuilder.create(this, false, true, true)
        .setRootStepsPreUpdate(rootStepsPreUpdate)
        .setMidStepsPreUpdate(expectedStepsForMidAndBotPreUpdate)
        .setBotStepsPreUpdate(expectedStepsForMidAndBotPreUpdate)
        .setMountSpecStepsPreUpdate(
            new LifecycleStep[] {
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_PREPARE,
              ON_MEASURE,
              ON_BOUNDS_DEFINED,
              ON_ATTACHED,
              ON_CREATE_MOUNT_CONTENT,
              ON_MOUNT,
              ON_BIND
            })
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
        .test();
  }

  @Test
  public void test_OCL_OCLWSS_OCL() {
    final LifecycleStep[] midStepsPreUpdate =
        ComponentsConfiguration.shouldAlwaysResolveNestedTreeInMeasure
            ? new LifecycleStep[] {
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CREATE_TREE_PROP, // TODO (T133075661) duplicate OCTPs
              ON_CALCULATE_CACHED_VALUE,
              ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
              ON_ATTACHED
            }
            : new LifecycleStep[] {
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CREATE_TREE_PROP, // TODO (T133075661) duplicate OCTPs
              ON_CALCULATE_CACHED_VALUE,
              ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
              ON_ATTACHED
            };
    final LifecycleStep[] botStepsPreUpdate =
        ComponentsConfiguration.shouldAlwaysResolveNestedTreeInMeasure
            ? new LifecycleStep[] {
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_CREATE_LAYOUT,
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_ATTACHED
            }
            : new LifecycleStep[] {
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_CREATE_LAYOUT,
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_CREATE_LAYOUT,
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_ATTACHED
            };
    final LifecycleStep[] mountSpecStepsPreUpdate =
        ComponentsConfiguration.shouldAlwaysResolveNestedTreeInMeasure
            ? new LifecycleStep[] {
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_PREPARE,
              ON_MEASURE,
              ON_BOUNDS_DEFINED,
              ON_ATTACHED,
              ON_CREATE_MOUNT_CONTENT,
              ON_MOUNT,
              ON_BIND
            }
            : new LifecycleStep[] {
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_PREPARE,
              ON_MEASURE,
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_PREPARE,
              ON_MEASURE,
              ON_BOUNDS_DEFINED,
              ON_ATTACHED,
              ON_CREATE_MOUNT_CONTENT,
              ON_MOUNT,
              ON_BIND
            };
    final LifecycleStep[] midStepsForUpdate1 =
        ComponentsConfiguration.shouldAlwaysResolveNestedTreeInMeasure
            ? new LifecycleStep[] {
              ON_CREATE_TREE_PROP,
              ON_CREATE_TREE_PROP,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_CREATE_LAYOUT_WITH_SIZE_SPEC
            }
            : new LifecycleStep[] {
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
              ON_CREATE_TREE_PROP,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_CREATE_LAYOUT_WITH_SIZE_SPEC
            };
    final LifecycleStep[] botStepsForUpdate1 =
        ComponentsConfiguration.shouldAlwaysResolveNestedTreeInMeasure
            ? new LifecycleStep[] {
              ON_CREATE_TREE_PROP, ON_CALCULATE_CACHED_VALUE, ON_CREATE_LAYOUT, ON_CREATE_TREE_PROP
            }
            : new LifecycleStep[] {
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_CREATE_LAYOUT,
              ON_CREATE_TREE_PROP,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_CREATE_LAYOUT,
              ON_CREATE_TREE_PROP
            };
    final LifecycleStep[] mountSpecStepsForUpdate1 =
        ComponentsConfiguration.shouldAlwaysResolveNestedTreeInMeasure
            ? new LifecycleStep[] {
              ON_CREATE_TREE_PROP,
              ON_PREPARE,
              ON_MEASURE,
              ON_BOUNDS_DEFINED,
              SHOULD_UPDATE,
              ON_UNBIND,
              ON_UNMOUNT,
              ON_MOUNT,
              ON_BIND
            }
            : new LifecycleStep[] {
              ON_CREATE_TREE_PROP,
              ON_PREPARE,
              ON_MEASURE,
              ON_CREATE_TREE_PROP,
              ON_PREPARE,
              ON_MEASURE,
              ON_BOUNDS_DEFINED,
              SHOULD_UPDATE,
              ON_UNBIND,
              ON_UNMOUNT,
              ON_MOUNT,
              ON_BIND
            };
    final LifecycleStep[] rootStepsForUpdate2And3 = new LifecycleStep[] {ON_CREATE_TREE_PROP};
    final LifecycleStep[] midStepsForUpdate2And3 =
        new LifecycleStep[] {
          ON_CREATE_TREE_PROP,
          ON_CREATE_TREE_PROP,
          ON_CALCULATE_CACHED_VALUE,
          ON_CREATE_LAYOUT_WITH_SIZE_SPEC
        };
    final LifecycleStep[] botStepsForUpdate2And3 =
        new LifecycleStep[] {
          ON_CREATE_TREE_PROP, ON_CALCULATE_CACHED_VALUE, ON_CREATE_LAYOUT, ON_CREATE_TREE_PROP
        };
    final LifecycleStep[] mountSpecStepsForUpdate2And3 =
        new LifecycleStep[] {
          ON_CREATE_TREE_PROP,
          ON_PREPARE,
          ON_MEASURE,
          ON_BOUNDS_DEFINED,
          SHOULD_UPDATE,
          ON_UNBIND,
          ON_UNMOUNT,
          ON_MOUNT,
          ON_BIND
        };

    // Testing OCL -> OCLWSS -> OCL
    TestHierarchyBuilder.create(this, true, false, true)
        .setRootStepsPreUpdate(
            new LifecycleStep[] {
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_CREATE_LAYOUT,
              ON_ATTACHED
            })
        .setMidStepsPreUpdate(midStepsPreUpdate)
        .setBotStepsPreUpdate(botStepsPreUpdate)
        .setMountSpecStepsPreUpdate(mountSpecStepsPreUpdate)
        .setRootStepsUpdate1(
            new LifecycleStep[] {
              ON_CREATE_TREE_PROP, ON_CREATE_TREE_PROP, ON_CALCULATE_CACHED_VALUE, ON_CREATE_LAYOUT
            })
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
        .test();
  }

  @Test
  public void test_OCL_OCL_OCLWSS() {
    final LifecycleStep[] botStepsPreUpdate =
        ComponentsConfiguration.shouldAlwaysResolveNestedTreeInMeasure
            ? new LifecycleStep[] {
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
              ON_ATTACHED
            }
            : new LifecycleStep[] {
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_ATTACHED
            };
    final LifecycleStep[] botStepsForUpdate1 =
        ComponentsConfiguration.shouldAlwaysResolveNestedTreeInMeasure
            ? new LifecycleStep[] {
              ON_CREATE_TREE_PROP,
              ON_CREATE_TREE_PROP,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_CREATE_LAYOUT_WITH_SIZE_SPEC
            }
            : new LifecycleStep[] {
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
              ON_CREATE_TREE_PROP
            };

    final LifecycleStep[] botStepsForUpdate2 =
        ComponentsConfiguration.shouldAlwaysResolveNestedTreeInMeasure
            ? new LifecycleStep[] {
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
              ON_DETACHED,
              ON_ATTACHED
            }
            : new LifecycleStep[] {
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_DETACHED,
              ON_ATTACHED
            };

    // Testing OCL -> OCL -> OCLWSS
    TestHierarchyBuilder.create(this, true, true, false)
        .setRootStepsPreUpdate(
            new LifecycleStep[] {
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_CREATE_LAYOUT,
              ON_ATTACHED
            })
        .setMidStepsPreUpdate(
            new LifecycleStep[] {
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_CREATE_LAYOUT,
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP, // TODO (T133075661) duplicate OCTPs
              ON_ATTACHED
            })
        .setBotStepsPreUpdate(botStepsPreUpdate)
        .setMountSpecStepsPreUpdate(
            new LifecycleStep[] {
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_PREPARE,
              ON_MEASURE,
              ON_BOUNDS_DEFINED,
              ON_ATTACHED,
              ON_CREATE_MOUNT_CONTENT,
              ON_MOUNT,
              ON_BIND
            })
        .setRootStepsUpdate1(
            new LifecycleStep[] {
              // TODO (T133075661) duplicate OCTPs
              ON_CREATE_TREE_PROP, ON_CREATE_TREE_PROP, ON_CALCULATE_CACHED_VALUE, ON_CREATE_LAYOUT
            })
        .setMidStepsUpdate1(
            new LifecycleStep[] {
              // TODO (T133075661) duplicate OCTPs
              ON_CREATE_TREE_PROP, ON_CALCULATE_CACHED_VALUE, ON_CREATE_LAYOUT, ON_CREATE_TREE_PROP
            })
        .setBotStepsUpdate1(botStepsForUpdate1)
        .setMountSpecStepsUpdate1(
            new LifecycleStep[] {
              ON_CREATE_TREE_PROP,
              ON_PREPARE,
              ON_MEASURE,
              ON_BOUNDS_DEFINED,
              SHOULD_UPDATE,
              ON_UNBIND,
              ON_UNMOUNT,
              ON_MOUNT,
              ON_BIND
            })
        // TODO (T133075661) expected empty, no OCTP
        .setRootStepsUpdate2(new LifecycleStep[] {ON_CREATE_TREE_PROP})
        .setMidStepsUpdate2(
            new LifecycleStep[] {
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE, // TODO (T133075661) unexpected OCCV
              ON_CREATE_LAYOUT,
              ON_DETACHED,
              ON_ATTACHED
            })
        .setBotStepsUpdate2(botStepsForUpdate2)
        .setMountSpecStepsUpdate2(
            new LifecycleStep[] {
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_PREPARE,
              ON_MEASURE,
              ON_BOUNDS_DEFINED,
              ON_DETACHED,
              ON_ATTACHED,
              ON_UNBIND,
              ON_UNMOUNT,
              ON_MOUNT,
              ON_BIND
            })
        .setRootStepsUpdate3(new LifecycleStep[] {ON_CREATE_TREE_PROP})
        .setMidStepsUpdate3(new LifecycleStep[] {}) // empty
        .setBotStepsUpdate3(
            new LifecycleStep[] {
              ON_CREATE_TREE_PROP,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
              ON_DETACHED,
              ON_ATTACHED
            })
        .setMountSpecStepsUpdate3(
            new LifecycleStep[] {
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_PREPARE,
              SHOULD_UPDATE,
              ON_MEASURE,
              ON_BOUNDS_DEFINED,
              ON_DETACHED,
              ON_ATTACHED,
              ON_UNBIND,
              ON_UNMOUNT,
              ON_MOUNT,
              ON_BIND
            })
        .test();
  }

  @Test
  public void test_OCLWSS_OCLWSS_OCL() {
    final LifecycleStep[] rootStepsPreUpdate =
        ComponentsConfiguration.shouldAlwaysResolveNestedTreeInMeasure
            ? new LifecycleStep[] {
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CREATE_TREE_PROP, // TODO (T133075661) duplicate OCTPs
              ON_CALCULATE_CACHED_VALUE,
              ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
              ON_ATTACHED
            }
            : new LifecycleStep[] {
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
              ON_ATTACHED
            };

    final LifecycleStep[] midStepsPreUpdate =
        ComponentsConfiguration.shouldAlwaysResolveNestedTreeInMeasure
            ? new LifecycleStep[] {
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
              ON_ATTACHED
            }
            : new LifecycleStep[] {
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
              ON_ATTACHED
            };

    final LifecycleStep[] botStepsPreUpdate =
        ComponentsConfiguration.shouldAlwaysResolveNestedTreeInMeasure
            ? new LifecycleStep[] {
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_CREATE_LAYOUT,
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_ATTACHED
            }
            : new LifecycleStep[] {
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_CREATE_LAYOUT,
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_CREATE_LAYOUT,
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_ATTACHED
            };

    final LifecycleStep[] mountSpecStepsPreUpdate =
        ComponentsConfiguration.shouldAlwaysResolveNestedTreeInMeasure
            ? new LifecycleStep[] {
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_PREPARE,
              ON_MEASURE,
              ON_BOUNDS_DEFINED,
              ON_ATTACHED,
              ON_CREATE_MOUNT_CONTENT,
              ON_MOUNT,
              ON_BIND
            }
            : new LifecycleStep[] {
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_PREPARE,
              ON_MEASURE,
              ON_CREATE_INITIAL_STATE,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_PREPARE,
              ON_MEASURE,
              ON_BOUNDS_DEFINED,
              ON_ATTACHED,
              ON_CREATE_MOUNT_CONTENT,
              ON_MOUNT,
              ON_BIND
            };

    // Root steps will be the same for all updates
    final LifecycleStep[] expectedStepsForRootAllUpdates =
        new LifecycleStep[] {
          ON_CREATE_TREE_PROP,
          ON_CREATE_TREE_PROP,
          ON_CREATE_TREE_PROP, // TODO (T133075661) duplicate OCTPs
          ON_CALCULATE_CACHED_VALUE,
          ON_CREATE_LAYOUT_WITH_SIZE_SPEC
        };

    // Mid steps will be the same for all updates
    final LifecycleStep[] expectedStepsForMidAllUpdates =
        ComponentsConfiguration.shouldAlwaysResolveNestedTreeInMeasure
            ? new LifecycleStep[] {
              ON_CREATE_TREE_PROP,
              ON_CREATE_TREE_PROP,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_CREATE_LAYOUT_WITH_SIZE_SPEC
            }
            : new LifecycleStep[] {
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_CREATE_LAYOUT_WITH_SIZE_SPEC,
              ON_CREATE_TREE_PROP,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_CREATE_LAYOUT_WITH_SIZE_SPEC
            };

    // Bot steps will be the same for all updates
    final LifecycleStep[] expectedStepsForBotAllUpdates =
        ComponentsConfiguration.shouldAlwaysResolveNestedTreeInMeasure
            ? new LifecycleStep[] {
              ON_CREATE_TREE_PROP, ON_CALCULATE_CACHED_VALUE, ON_CREATE_LAYOUT, ON_CREATE_TREE_PROP
            }
            : new LifecycleStep[] {
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_CREATE_LAYOUT,
              ON_CREATE_TREE_PROP,
              ON_CREATE_TREE_PROP,
              ON_CALCULATE_CACHED_VALUE,
              ON_CREATE_LAYOUT,
              ON_CREATE_TREE_PROP
            };

    // MountSpec steps will be the same for all updates
    final LifecycleStep[] expectedStepsForMountSpecAllUpdates =
        ComponentsConfiguration.shouldAlwaysResolveNestedTreeInMeasure
            ? new LifecycleStep[] {
              ON_CREATE_TREE_PROP,
              ON_PREPARE,
              ON_MEASURE,
              ON_BOUNDS_DEFINED,
              SHOULD_UPDATE,
              ON_UNBIND,
              ON_UNMOUNT,
              ON_MOUNT,
              ON_BIND
            }
            : new LifecycleStep[] {
              ON_CREATE_TREE_PROP,
              ON_PREPARE,
              ON_MEASURE,
              ON_CREATE_TREE_PROP,
              ON_PREPARE,
              ON_MEASURE,
              ON_BOUNDS_DEFINED,
              SHOULD_UPDATE,
              ON_UNBIND,
              ON_UNMOUNT,
              ON_MOUNT,
              ON_BIND
            };

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
        .test();
  }

  private void testSpecificSetup(
      final boolean isRootOCL,
      final boolean isMidOCL,
      final boolean isBotOCL,
      final LifecycleStep[] rootStepsPreStateUpdate,
      final LifecycleStep[] midStepsPreStateUpdate,
      final LifecycleStep[] botStepsPreStateUpdate,
      final LifecycleStep[] mountSpecStepsPreStateUpdate,
      final LifecycleStep[] rootStepsStateUpdate1,
      final LifecycleStep[] midStepsStateUpdate1,
      final LifecycleStep[] botStepsStateUpdate1,
      final LifecycleStep[] mountSpecStateUpdate1,
      final LifecycleStep[] rootStepsStateUpdate2,
      final LifecycleStep[] midStepsStateUpdate2,
      final LifecycleStep[] botStepsStateUpdate2,
      final LifecycleStep[] mountSpecStateUpdate2,
      final LifecycleStep[] rootStepsStateUpdate3,
      final LifecycleStep[] midStepsStateUpdate3,
      final LifecycleStep[] botStepsStateUpdate3,
      final LifecycleStep[] mountSpecStateUpdate3) {
    final ComponentContext c = mLegacyLithoViewRule.getContext();

    final int widthSpec = SizeSpec.makeSizeSpec(500, SizeSpec.EXACTLY);
    final int heightSpec = SizeSpec.makeSizeSpec(500, SizeSpec.EXACTLY);

    // Generate a component + state-update-callers for requested setup.
    final StateUpdateComponentHolder holder =
        createComponentHierarchySetup(c, widthSpec, heightSpec, isRootOCL, isMidOCL, isBotOCL);

    // Set the root and layout
    mLegacyLithoViewRule.setRoot(holder.component);
    mLegacyLithoViewRule.setSizeSpecs(widthSpec, heightSpec);
    mLegacyLithoViewRule.attachToWindow().measure().layout();

    // Ensure the root node is not null. We'll need this to extract scoped contexts to simulate
    // state updates.
    assertThat(mLegacyLithoViewRule.getCurrentRootNode()).isNotNull();

    // Ensure the lifecycle steps are as expected before any state updates.
    List<LifecycleStep> rootSteps = getSteps(holder.rootLayoutSpecSteps);
    List<LifecycleStep> midSteps = getSteps(holder.midLayoutSpecSteps);
    List<LifecycleStep> botSteps = getSteps(holder.botLayoutSpecSteps);
    List<LifecycleStep> mountableSteps = holder.mountableLifecycleTracker.getSteps();

    assertThat(rootSteps)
        .describedAs("Root steps pre update")
        .containsExactly(rootStepsPreStateUpdate);
    assertThat(midSteps)
        .describedAs("Mid steps pre update")
        .containsExactly(midStepsPreStateUpdate);
    assertThat(botSteps)
        .describedAs("Bot steps pre update")
        .containsExactly(botStepsPreStateUpdate);
    assertThat(mountableSteps)
        .describedAs("MountSpec steps pre update")
        .containsExactly(mountSpecStepsPreStateUpdate);

    // Ensure all texts showing initial states are as expected
    assertThat(mLegacyLithoViewRule.findViewWithText("root 0")).isNotNull();
    assertThat(mLegacyLithoViewRule.findViewWithText("mid 0")).isNotNull();
    assertThat(mLegacyLithoViewRule.findViewWithText("bot 0")).isNotNull();

    // Test state update 1/3
    // Reset the lifecycle steps
    holder.clearAllSteps();

    // Extract the root component's context to simulate state updates
    final ComponentContext rootScopedContext = getRootComponentContext();

    // Simulate the state update for the root.
    holder.rootStateCaller.increment(rootScopedContext);

    // Ensure lifecycle steps are as expected after the state update
    rootSteps = getSteps(holder.rootLayoutSpecSteps);
    midSteps = getSteps(holder.midLayoutSpecSteps);
    botSteps = getSteps(holder.botLayoutSpecSteps);
    mountableSteps = holder.mountableLifecycleTracker.getSteps();

    assertThat(rootSteps)
        .describedAs("Root steps after update 1")
        .containsExactly(rootStepsStateUpdate1);
    assertThat(midSteps)
        .describedAs("Mid steps after update 1")
        .containsExactly(midStepsStateUpdate1);
    assertThat(botSteps)
        .describedAs("Bot steps after update 1")
        .containsExactly(botStepsStateUpdate1);
    assertThat(mountableSteps)
        .describedAs("MountSpec steps after update 1")
        .containsExactly(mountSpecStateUpdate1);

    // Ensure the texts properly reflect the correct values after the state update
    assertThat(mLegacyLithoViewRule.findViewWithText("root 1")).isNotNull(); // Updated!
    assertThat(mLegacyLithoViewRule.findViewWithText("mid 0")).isNotNull();
    assertThat(mLegacyLithoViewRule.findViewWithText("bot 0")).isNotNull();

    // Test state update 2/3
    // Reset the lifecycle steps
    holder.clearAllSteps();

    // Extract the mid component's context to simulate state updates
    final ComponentContext midScopedContext = getMidComponentContext();

    // Simulate the state update for the mid component.
    holder.midStateCaller.increment(midScopedContext);

    // Ensure lifecycle steps are as expected after the state update
    rootSteps = getSteps(holder.rootLayoutSpecSteps);
    midSteps = getSteps(holder.midLayoutSpecSteps);
    botSteps = getSteps(holder.botLayoutSpecSteps);
    mountableSteps = holder.mountableLifecycleTracker.getSteps();

    assertThat(rootSteps)
        .describedAs("Root steps after update 2")
        .containsExactly(rootStepsStateUpdate2);
    assertThat(midSteps)
        .describedAs("Mid steps after update 2")
        .containsExactly(midStepsStateUpdate2);
    assertThat(botSteps)
        .describedAs("Bot steps after update 2")
        .containsExactly(botStepsStateUpdate2);
    assertThat(mountableSteps)
        .describedAs("MountSpec steps after update 2")
        .containsExactly(mountSpecStateUpdate2);

    // Ensure the texts properly reflect the correct values after the state update
    assertThat(mLegacyLithoViewRule.findViewWithText("root 1")).isNotNull();
    assertThat(mLegacyLithoViewRule.findViewWithText("mid 1")).isNotNull(); // Updated!
    assertThat(mLegacyLithoViewRule.findViewWithText("bot 0")).isNotNull();

    // Test state update 3/3
    // Reset the lifecycle steps
    holder.clearAllSteps();

    // Extract the bottom component's context to simulate state updates
    final ComponentContext botScopedContext = getBotComponentContext();

    // Simulate the state update for the bottom component.
    holder.botStateCaller.increment(botScopedContext);

    // Ensure lifecycle steps are as expected after the state update
    rootSteps = getSteps(holder.rootLayoutSpecSteps);
    midSteps = getSteps(holder.midLayoutSpecSteps);
    botSteps = getSteps(holder.botLayoutSpecSteps);
    mountableSteps = holder.mountableLifecycleTracker.getSteps();

    assertThat(rootSteps)
        .describedAs("Root steps after update 3")
        .containsExactly(rootStepsStateUpdate3);
    assertThat(midSteps)
        .describedAs("Mid steps after update 3")
        .containsExactly(midStepsStateUpdate3);
    assertThat(botSteps)
        .describedAs("Bot steps after update 3")
        .containsExactly(botStepsStateUpdate3);
    assertThat(mountableSteps)
        .describedAs("MountSpec steps after update 3")
        .containsExactly(mountSpecStateUpdate3);

    // Ensure the texts properly reflect the correct values after the state update
    assertThat(mLegacyLithoViewRule.findViewWithText("root 1")).isNotNull();
    assertThat(mLegacyLithoViewRule.findViewWithText("mid 1")).isNotNull();
    assertThat(mLegacyLithoViewRule.findViewWithText("bot 1")).isNotNull(); // Updated!
  }

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
   *     lifecycle step arrays for each component including the MountSpec held by the bot comp.
   */
  private static StateUpdateComponentHolder createComponentHierarchySetup(
      final ComponentContext c,
      final int widthSpec,
      final int heightSpec,
      final boolean isRootOCL,
      final boolean isMidOCL,
      final boolean isBottomOCL) {

    final List<LifecycleStep.StepInfo> rootStepsInfo = new ArrayList<>();
    final List<LifecycleStep.StepInfo> midStepsInfo = new ArrayList<>();
    final List<LifecycleStep.StepInfo> botStepsInfo = new ArrayList<>();
    final LifecycleTracker mountableLifecycleTracker = new LifecycleTracker();

    final Component mountable =
        MountSpecPureRenderLifecycleTester.create(c)
            .lifecycleTracker(mountableLifecycleTracker)
            .build();

    final BaseIncrementStateCaller rootCaller;
    final BaseIncrementStateCaller midCaller;
    final BaseIncrementStateCaller bottomCaller;

    final Component rootComponent;
    final Component midComponent;
    final Component bottomComponent;

    // bottom comp will set half height to make room for text
    if (isBottomOCL) {
      bottomCaller = new ComponentWithMeasureCallAndStateSpec.Caller();
      bottomComponent =
          ComponentWithMeasureCallAndState.create(c)
              .steps(botStepsInfo)
              .shouldCacheResult(true)
              .widthPx(SizeSpec.getSize(widthSpec) / 2)
              .heightPx(SizeSpec.getSize(heightSpec) / 2)
              .prefix("bot")
              .mountSpec(mountable)
              .build();
    } else {
      bottomCaller = new ComponentWithSizeAndMeasureCallAndStateSpec.Caller();
      bottomComponent =
          ComponentWithSizeAndMeasureCallAndState.create(c)
              .steps(botStepsInfo)
              .shouldCacheResult(true)
              .widthPx(SizeSpec.getSize(widthSpec) / 2)
              .heightPx(SizeSpec.getSize(heightSpec) / 2)
              .prefix("bot")
              .mountSpec(mountable)
              .build();
    }

    if (isMidOCL) {
      midCaller = new ComponentWithMeasureCallAndStateSpec.Caller();
      midComponent =
          ComponentWithMeasureCallAndState.create(c)
              .steps(midStepsInfo)
              .shouldCacheResult(true)
              .component(bottomComponent)
              .widthSpec(widthSpec)
              .heightSpec(heightSpec)
              .prefix("mid")
              .build();
    } else {
      midCaller = new ComponentWithSizeAndMeasureCallAndStateSpec.Caller();
      midComponent =
          ComponentWithSizeAndMeasureCallAndState.create(c)
              .steps(midStepsInfo)
              .shouldCacheResult(true)
              .component(bottomComponent)
              .prefix("mid")
              .build();
    }

    if (isRootOCL) {
      rootCaller = new ComponentWithMeasureCallAndStateSpec.Caller();
      rootComponent =
          ComponentWithMeasureCallAndState.create(c)
              .steps(rootStepsInfo)
              .shouldCacheResult(true)
              .component(midComponent)
              .widthSpec(widthSpec)
              .heightSpec(heightSpec)
              .prefix("root")
              .build();
    } else {
      rootCaller = new ComponentWithSizeAndMeasureCallAndStateSpec.Caller();
      rootComponent =
          ComponentWithSizeAndMeasureCallAndState.create(c)
              .steps(rootStepsInfo)
              .shouldCacheResult(true)
              .component(midComponent)
              .prefix("root")
              .build();
    }

    return new StateUpdateComponentHolder(
        rootComponent,
        rootCaller,
        midCaller,
        bottomCaller,
        rootStepsInfo,
        midStepsInfo,
        botStepsInfo,
        mountableLifecycleTracker);
  }

  private ComponentContext getRootComponentContext() {
    assertThat(mLegacyLithoViewRule.getCurrentRootNode()).isNotNull();
    return getCorrectLayoutResult(mLegacyLithoViewRule.getCurrentRootNode())
        .mNode
        .getComponentContextAt(1);
  }

  private ComponentContext getMidComponentContext() {
    final LithoLayoutResult rootLayoutResult = mLegacyLithoViewRule.getCurrentRootNode();
    assertThat(rootLayoutResult).isNotNull();

    if (rootLayoutResult instanceof NestedTreeHolderResult) {
      final LithoLayoutResult nestedResult = getCorrectLayoutResult(rootLayoutResult);
      final LithoLayoutResult nestedMidResult = getCorrectLayoutResult(nestedResult.getChildAt(1));
      assertThat(nestedMidResult).isNotNull();

      return nestedMidResult.mNode.getComponentContextAt(1);
    } else {
      return rootLayoutResult.mNode.getChildAt(1).getComponentContextAt(0);
    }
  }

  private ComponentContext getBotComponentContext() {
    final LithoLayoutResult rootLayoutResult = mLegacyLithoViewRule.getCurrentRootNode();
    assertThat(rootLayoutResult).isNotNull();

    if (rootLayoutResult instanceof NestedTreeHolderResult) {
      final LithoLayoutResult nestedResult = getCorrectLayoutResult(rootLayoutResult);
      final LithoLayoutResult nestedMidResult = getCorrectLayoutResult(nestedResult.getChildAt(1));
      assertThat(nestedMidResult).isNotNull();
      final LithoLayoutResult nestedBotResult =
          getCorrectLayoutResult(nestedMidResult.getChildAt(1));

      return nestedBotResult.mNode.getComponentContextAt(1);
    } else {
      final LithoLayoutResult midResult = rootLayoutResult.getChildAt(1);

      if (midResult instanceof NestedTreeHolderResult) {
        final LithoLayoutResult nestedResult = getCorrectLayoutResult(midResult);
        final LithoLayoutResult nestedBotResult =
            getCorrectLayoutResult(nestedResult.getChildAt(1));
        return nestedBotResult.mNode.getComponentContextAt(1);
      } else {
        return rootLayoutResult.mNode.getChildAt(1).getChildAt(1).getComponentContextAt(0);
      }
    }
  }

  private static LithoLayoutResult getCorrectLayoutResult(final LithoLayoutResult from) {
    if (from instanceof NestedTreeHolderResult) {
      final LithoLayoutResult nestedResult = ((NestedTreeHolderResult) from).getNestedResult();
      assertThat(nestedResult).isNotNull();
      return nestedResult;
    } else {
      return from;
    }
  }

  /**
   * Holder class for a component hierarchy described above. Holds the root component, state update
   * callers for each 3 components, and step info arrays for each component including the leaf
   * MountSpec.
   */
  public static class StateUpdateComponentHolder {
    public final Component component;
    public final BaseIncrementStateCaller rootStateCaller;
    public final BaseIncrementStateCaller midStateCaller;
    public final BaseIncrementStateCaller botStateCaller;
    public final List<LifecycleStep.StepInfo> rootLayoutSpecSteps;
    public final List<LifecycleStep.StepInfo> midLayoutSpecSteps;
    public final List<LifecycleStep.StepInfo> botLayoutSpecSteps;
    public final LifecycleTracker mountableLifecycleTracker;

    public StateUpdateComponentHolder(
        final Component component,
        final BaseIncrementStateCaller rootStateCaller,
        final BaseIncrementStateCaller midStateCaller,
        final BaseIncrementStateCaller botStateCaller,
        final List<LifecycleStep.StepInfo> rootLayoutSpecSteps,
        final List<LifecycleStep.StepInfo> midLayoutSpecSteps,
        final List<LifecycleStep.StepInfo> botLayoutSpecSteps,
        final LifecycleTracker mountableLifecycleTracker) {
      this.component = component;
      this.rootStateCaller = rootStateCaller;
      this.midStateCaller = midStateCaller;
      this.botStateCaller = botStateCaller;
      this.rootLayoutSpecSteps = rootLayoutSpecSteps;
      this.midLayoutSpecSteps = midLayoutSpecSteps;
      this.botLayoutSpecSteps = botLayoutSpecSteps;
      this.mountableLifecycleTracker = mountableLifecycleTracker;
    }

    /** Resets all lifecycle steps for all components. */
    void clearAllSteps() {
      this.rootLayoutSpecSteps.clear();
      this.midLayoutSpecSteps.clear();
      this.botLayoutSpecSteps.clear();
      this.mountableLifecycleTracker.reset();
    }
  }

  private static class TestHierarchyBuilder {
    private NestedTreeResolutionWithStateTest testRunner;
    private boolean isRootOCL;
    private boolean isMidOCL;
    private boolean isBotOCL;
    private LifecycleStep[] rootStepsPreStateUpdate;
    private LifecycleStep[] midStepsPreStateUpdate;
    private LifecycleStep[] botStepsPreStateUpdate;
    private LifecycleStep[] mountSpecStepsPreStateUpdate;
    private LifecycleStep[] rootStepsStateUpdate1;
    private LifecycleStep[] midStepsStateUpdate1;
    private LifecycleStep[] botStepsStateUpdate1;
    private LifecycleStep[] mountSpecStateUpdate1;
    private LifecycleStep[] rootStepsStateUpdate2;
    private LifecycleStep[] midStepsStateUpdate2;
    private LifecycleStep[] botStepsStateUpdate2;
    private LifecycleStep[] mountSpecStateUpdate2;
    private LifecycleStep[] rootStepsStateUpdate3;
    private LifecycleStep[] midStepsStateUpdate3;
    private LifecycleStep[] botStepsStateUpdate3;
    private LifecycleStep[] mountSpecStateUpdate3;

    static TestHierarchyBuilder create(
        NestedTreeResolutionWithStateTest testRunner,
        final boolean isRootOCL,
        final boolean isMidOCL,
        final boolean isBotOCL) {
      final TestHierarchyBuilder builder = new TestHierarchyBuilder();
      builder.isRootOCL = isRootOCL;
      builder.isMidOCL = isMidOCL;
      builder.isBotOCL = isBotOCL;
      builder.testRunner = testRunner;

      return builder;
    }

    TestHierarchyBuilder setRootStepsPreUpdate(LifecycleStep[] steps) {
      this.rootStepsPreStateUpdate = steps;
      return this;
    }

    TestHierarchyBuilder setMidStepsPreUpdate(LifecycleStep[] steps) {
      this.midStepsPreStateUpdate = steps;
      return this;
    }

    TestHierarchyBuilder setBotStepsPreUpdate(LifecycleStep[] steps) {
      this.botStepsPreStateUpdate = steps;
      return this;
    }

    TestHierarchyBuilder setMountSpecStepsPreUpdate(LifecycleStep[] steps) {
      this.mountSpecStepsPreStateUpdate = steps;
      return this;
    }

    TestHierarchyBuilder setRootStepsUpdate1(LifecycleStep[] steps) {
      this.rootStepsStateUpdate1 = steps;
      return this;
    }

    TestHierarchyBuilder setMidStepsUpdate1(LifecycleStep[] steps) {
      this.midStepsStateUpdate1 = steps;
      return this;
    }

    TestHierarchyBuilder setBotStepsUpdate1(LifecycleStep[] steps) {
      this.botStepsStateUpdate1 = steps;
      return this;
    }

    TestHierarchyBuilder setMountSpecStepsUpdate1(LifecycleStep[] steps) {
      this.mountSpecStateUpdate1 = steps;
      return this;
    }

    TestHierarchyBuilder setRootStepsUpdate2(LifecycleStep[] steps) {
      this.rootStepsStateUpdate2 = steps;
      return this;
    }

    TestHierarchyBuilder setMidStepsUpdate2(LifecycleStep[] steps) {
      this.midStepsStateUpdate2 = steps;
      return this;
    }

    TestHierarchyBuilder setBotStepsUpdate2(LifecycleStep[] steps) {
      this.botStepsStateUpdate2 = steps;
      return this;
    }

    TestHierarchyBuilder setMountSpecStepsUpdate2(LifecycleStep[] steps) {
      this.mountSpecStateUpdate2 = steps;
      return this;
    }

    TestHierarchyBuilder setRootStepsUpdate3(LifecycleStep[] steps) {
      this.rootStepsStateUpdate3 = steps;
      return this;
    }

    TestHierarchyBuilder setMidStepsUpdate3(LifecycleStep[] steps) {
      this.midStepsStateUpdate3 = steps;
      return this;
    }

    TestHierarchyBuilder setBotStepsUpdate3(LifecycleStep[] steps) {
      this.botStepsStateUpdate3 = steps;
      return this;
    }

    TestHierarchyBuilder setMountSpecStepsUpdate3(LifecycleStep[] steps) {
      this.mountSpecStateUpdate3 = steps;
      return this;
    }

    void test() {
      testRunner.testSpecificSetup(
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
          mountSpecStateUpdate3);
    }
  }
}
