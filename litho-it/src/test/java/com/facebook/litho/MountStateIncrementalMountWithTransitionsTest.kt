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
import com.facebook.litho.animation.AnimatedProperties
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.common.TestSingleComponentListSection
import com.facebook.litho.sections.widget.ListRecyclerConfiguration
import com.facebook.litho.sections.widget.RecyclerBinderConfiguration
import com.facebook.litho.sections.widget.RecyclerCollectionComponent
import com.facebook.litho.testing.LegacyLithoViewRule
import com.facebook.litho.testing.TransitionTestRule
import com.facebook.litho.testing.exactly
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.widget.LithoViewFactory
import com.facebook.litho.widget.MountSpecLifecycleTester
import com.facebook.litho.widget.SectionsRecyclerView
import com.facebook.litho.widget.TestAnimationsComponent
import com.facebook.litho.widget.TestComponentProvider
import com.facebook.yoga.YogaAlign
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(LithoTestRunner::class)
class MountStateIncrementalMountWithTransitionsTest {

  @JvmField @Rule val legacyLithoViewRule: LegacyLithoViewRule = LegacyLithoViewRule()
  @JvmField @Rule val transitionTestRule: TransitionTestRule = TransitionTestRule()

  private val stateCaller = StateCaller()

  @Test
  fun incrementalMount_componentOffScreen_mountIfAnimating() {
    val lifecycleTracker1 = LifecycleTracker()
    val lifecycleTracker2 = LifecycleTracker()
    val lifecycleTracker3 = LifecycleTracker()
    val trackers = ArrayList<LifecycleTracker>()
    trackers.add(lifecycleTracker1)
    trackers.add(lifecycleTracker2)
    trackers.add(lifecycleTracker3)
    val root = getPartiallyVisibleRootWithAnimatingComponents(trackers)
    legacyLithoViewRule.setRoot(root).setSizeSpecs(exactly(1_040), exactly(60))
    legacyLithoViewRule.attachToWindow().measure().layout()
    val lithoViews: List<LithoView> = ArrayList()
    val sectionsRecyclerView = legacyLithoViewRule.lithoView.getChildAt(0) as SectionsRecyclerView
    sectionsRecyclerView.obtainLithoViewChildren(lithoViews)

    // grab the 2nd litho-view in the list (0 = sticky header, 1 = 1st litho view)
    val animatingLithoView = lithoViews[2]
    animatingLithoView.onAttachedToWindowForTest()
    stateCaller.update()
    legacyLithoViewRule.idle()
    assertThat(lifecycleTracker1.steps).contains(LifecycleStep.ON_MOUNT)
    assertThat(lifecycleTracker2.steps).contains(LifecycleStep.ON_MOUNT)
    assertThat(lifecycleTracker3.steps).contains(LifecycleStep.ON_MOUNT)
  }

  @Test
  fun incrementalMount_animatingComponentWithChildrenLithoView_mountLithoViewsOffScreen() {

    /*
      TODO: Fix the broken test for RCMS

      Root cause is litho MountState applyMountBinders()

      LMS calls TE onMountItem after bind, and after bounds are set.
      RCMS also calls it but before bind and before bounds are set (by design).

      So, in the test:

      LMS, after the Recycler is mounted, bound, and its size set the TE calls
      notify on it which causes the internal LV's to mount.

      RCMS, after the Recycler is mounted, but before being bound, and setting
      size, TE calls notify on it; this means that the size of the view is zero,
      and when the AnimatedRootHost.notifyVisibleBoundsChanged() nothing is mounted
      in the LV
    */
    val lifecycleTracker0 = LifecycleTracker()
    val lifecycleTracker1 = LifecycleTracker()
    val component0 =
        MountSpecLifecycleTester.create(legacyLithoViewRule.context)
            .lifecycleTracker(lifecycleTracker0)
            .intrinsicSize(Size(10, 40))
            .build()
    val animatingComponents: MutableList<Component> = ArrayList()
    animatingComponents.add(
        MountSpecLifecycleTester.create(legacyLithoViewRule.context)
            .lifecycleTracker(lifecycleTracker1)
            .intrinsicSize(Size(40, 40))
            .widthPx(40)
            .heightPx(40)
            .build())
    val sectionContext = SectionContext(legacyLithoViewRule.context)
    val binderConfig =
        RecyclerBinderConfiguration.create().lithoViewFactory(lithoViewFactory).build()
    val config =
        ListRecyclerConfiguration.create()
            .orientation(0)
            .recyclerBinderConfiguration(binderConfig)
            .build()
    val childOfAnimatingComponent =
        RecyclerCollectionComponent.create(legacyLithoViewRule.context)
            .widthPx(40)
            .heightPx(40)
            .section(
                TestSingleComponentListSection.create(sectionContext)
                    .data(animatingComponents)
                    .build())
            .recyclerConfiguration(config)
            .build()
    val partiallyVisibleAnimatingComponent: TestComponentProvider =
        object : TestComponentProvider {
          override fun getComponent(componentContext: ComponentContext, state: Boolean): Component =
              if (!state) {
                Column.create(componentContext)
                    .alignItems(YogaAlign.FLEX_END)
                    .child(
                        Column.create(componentContext)
                            .flexGrow(1f)
                            .child(component0)
                            .transitionKey("transitionkey_root")
                            .build())
                    .build()
              } else {
                Column.create(componentContext)
                    .alignItems(YogaAlign.FLEX_START)
                    .child(
                        Column.create(componentContext)
                            .flexGrow(1f)
                            .child(component0)
                            .child(childOfAnimatingComponent)
                            .transitionKey("transitionkey_root")
                            .build())
                    .build()
              }
        }
    val root =
        TestAnimationsComponent(
            stateCaller,
            Transition.sequence(
                Transition.create("transitionkey_root")
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.X))) { state ->
              requireNotNull(partiallyVisibleAnimatingComponent.getComponent(context, state))
            }
    legacyLithoViewRule.setRoot(root).setSizeSpecs(exactly(40), exactly(80))
    legacyLithoViewRule.attachToWindow().measure().layout()
    assertThat(lifecycleTracker0.isMounted).isTrue
    assertThat(lifecycleTracker1.isMounted).isFalse
    stateCaller.update()
    legacyLithoViewRule.idle()
    assertThat(lifecycleTracker1.isMounted).isTrue
  }

  fun getPartiallyVisibleRootWithAnimatingComponents(
      animatingComponentTrackers: List<LifecycleTracker>
  ): Component {
    val animatingComponents: MutableList<Component> = ArrayList()
    for (tracker in animatingComponentTrackers) {
      animatingComponents.add(
          MountSpecLifecycleTester.create(legacyLithoViewRule.context)
              .lifecycleTracker(tracker)
              .intrinsicSize(Size(40, 40))
              .build())
    }
    val partiallyVisibleAnimatingComponent: TestComponentProvider =
        object : TestComponentProvider {
          override fun getComponent(componentContext: ComponentContext, state: Boolean): Component {
            val builder =
                Column.create(componentContext)
                    .alignItems(if (state) YogaAlign.FLEX_START else YogaAlign.FLEX_END)
            val size = animatingComponents.size
            for (i in 0 until size) {
              builder.child(
                  Row.create(componentContext)
                      .child(animatingComponents[i])
                      .heightDip(40f)
                      .widthDip(40f)
                      .backgroundColor(Color.parseColor("#ee1111"))
                      .transitionKey("transitionkey_$i")
                      .viewTag(RED_TRANSITION_KEY)
                      .build())
            }
            return builder.build()
          }
        }
    val transitions = arrayOfNulls<Transition>(animatingComponents.size)
    val size = animatingComponents.size
    for (i in 0 until size) {
      transitions[i] =
          Transition.create("transitionkey_$i")
              .animator(Transition.timing(144))
              .animate(AnimatedProperties.X)
    }
    val component =
        TestAnimationsComponent(stateCaller, Transition.sequence(*transitions)) { state ->
          requireNotNull(partiallyVisibleAnimatingComponent.getComponent(context, state))
        }
    val data: MutableList<Component> = ArrayList()
    data.add(
        Row.create(legacyLithoViewRule.context)
            .heightDip(40f)
            .widthDip(40f)
            .backgroundColor(Color.parseColor("#ee1111"))
            .build())
    data.add(component)
    val sectionContext = SectionContext(legacyLithoViewRule.context)
    val binderConfig =
        RecyclerBinderConfiguration.create().lithoViewFactory(lithoViewFactory).build()
    val config =
        ListRecyclerConfiguration.create().recyclerBinderConfiguration(binderConfig).build()
    val root =
        RecyclerCollectionComponent.create(legacyLithoViewRule.context)
            .section(TestSingleComponentListSection.create(sectionContext).data(data).build())
            .recyclerConfiguration(config)
            .build()
    return root
  }

  private val lithoViewFactory: LithoViewFactory
    get() = LithoViewFactory { context -> LithoView(context) }

  companion object {
    const val RED_TRANSITION_KEY = "red"
    const val GREEN_TRANSITION_KEY = "green"
    const val BLUE_TRANSITION_KEY = "blue"
  }
}
