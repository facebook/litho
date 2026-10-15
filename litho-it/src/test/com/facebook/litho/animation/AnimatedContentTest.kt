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

package com.facebook.litho.animation

import android.view.View
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.flexbox.alignSelf
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.testing.LithoAnimationTestRule
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.TestLithoView
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.viewTag
import com.facebook.rendercore.ms
import com.facebook.rendercore.px
import com.facebook.yoga.YogaAlign
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.LooperMode

/** Tests for the public [AnimatedContent] keyed content-swap API. */
@OptIn(ExperimentalLithoApi::class)
@LooperMode(LooperMode.Mode.PAUSED)
@RunWith(LithoTestRunner::class)
class AnimatedContentTest {

  @Rule
  @JvmField
  val lithoTestRule = LithoTestRule(
      ComponentsConfiguration.defaultInstance.copy(enableStateReadTracking = true),
  )

  @Rule @JvmField val animationRule = LithoAnimationTestRule { lithoTestRule.idle() }

  private val linearFade = tween<Float>(duration = 1_000.ms, easing = Easing.Linear)

  private fun slotCount(testView: TestLithoView): Int =
      testView.findAllComponents(Marker::class).size

  @Test
  fun switchByKey_retainsOutgoingThenReleasesIt() {
    val testView = lithoTestRule.render { content(target = "A") }
    lithoTestRule.idle()
    assertThat(slotCount(testView)).isEqualTo(1)

    testView.setRoot(content(target = "B"))
    lithoTestRule.idle()
    // The outgoing "A" is retained alongside the new target "B" while it animates out.
    assertThat(slotCount(testView)).`as`("outgoing retained on swap").isEqualTo(2)

    // After "A" exit settles, it is released; only "B" remains.
    animationRule.advanceBy(2_000.ms)
    lithoTestRule.idle()
    assertThat(slotCount(testView)).`as`("released after settle").isEqualTo(1)
  }

  @Test
  fun reuseSameKey_doesNotAddSlot() {
    val testView = lithoTestRule.render { content(target = "A") }
    lithoTestRule.idle()
    assertThat(slotCount(testView)).isEqualTo(1)

    testView.setRoot(content(target = "A"))
    animationRule.advanceBy(2_000.ms)
    lithoTestRule.idle()
    assertThat(slotCount(testView)).isEqualTo(1)
  }

  @Test
  fun rapidSwitch_keepsSlotsBoundedThenReleases() {
    val testView = lithoTestRule.render { content(target = "A") }
    lithoTestRule.idle()

    testView.setRoot(content(target = "B"))
    testView.setRoot(content(target = "C"))

    animationRule.advanceBy(500.ms)
    assertThat(slotCount(testView)).isBetween(1, 3)

    animationRule.advanceBy(2_000.ms)
    lithoTestRule.idle()
    assertThat(slotCount(testView)).isEqualTo(1)
  }

  @Test
  fun reorder_returnsToEarlierTarget_settlesWithoutLeak() {
    val testView = lithoTestRule.render { content(target = "A") }
    lithoTestRule.idle()

    testView.setRoot(content(target = "B"))
    lithoTestRule.idle()
    // "A" is exiting at index 0; "B" is the target at index 1.
    assertThat(slotCount(testView)).isEqualTo(2)

    // Return to "A": it reorders from outgoing back to target while "B" now exits. Stable keys keep
    // each slot's identity across the reorder, so no slot is duplicated or stranded.
    testView.setRoot(content(target = "A"))
    animationRule.advanceBy(2_000.ms)
    lithoTestRule.idle()
    assertThat(slotCount(testView)).`as`("settles back to the single retargeted slot").isEqualTo(1)
  }

  @Test
  fun container_sizesToTarget_notOutgoing() {
    // "A" is 80 wide, "B" is 120 wide. The container must size to whichever is the *target*.
    val testView = lithoTestRule.render { sizedContent(target = "A") }
    lithoTestRule.idle()
    assertThat(containerWidth(testView)).isEqualTo(80)

    // Mid-transition (outgoing "A" still present), the container already sizes to the target "B"
    // because the target is the in-flow child, not the oldest outgoing slot.
    testView.setRoot(sizedContent(target = "B"))
    lithoTestRule.idle()
    assertThat(containerWidth(testView))
        .`as`("container follows the target, not the outgoing slot")
        .isEqualTo(120)
  }

  @Test
  fun outgoingSlot_exitIsTimeDriven_spansFadeDurationThenReleases() {
    // The exit fade spec is 1_000ms (linearFade). A time-driven exit keeps the outgoing slot
    // mounted across that window and releases it only once it elapses — not an instant snap (the
    // never-shown path) nor a premature release. The alpha ramp of the shared engine is covered
    // by AnimatedVisibilityTest.fadeOut_appliesDecreasingAlpha (same applyEnterExitContent).
    val testView = lithoTestRule.render { content(target = "A") }
    lithoTestRule.idle()

    testView.setRoot(content(target = "B"))

    animationRule.advanceBy(400.ms)
    assertThat(slotCount(testView)).`as`("outgoing still fading early in the window").isEqualTo(2)

    animationRule.advanceBy(400.ms)
    assertThat(slotCount(testView))
        .`as`("outgoing still fading near the end of the window")
        .isEqualTo(2)

    animationRule.advanceBy(400.ms)
    lithoTestRule.idle()
    assertThat(slotCount(testView)).`as`("released once the fade duration elapses").isEqualTo(1)
  }

  @Test
  fun transitionSpec_initialStateStaysStableAfterOutgoingSettles() {
    val seen = java.util.Collections.synchronizedList(mutableListOf<Pair<String, String>>())
    fun recording(target: String): Component = AnimatedContent(
        targetState = target,
        transitionSpec = {
          seen.add(initialState to targetState)
          enter = fadeIn(linearFade)
          exit = fadeOut(linearFade)
        },
    ) {
      Marker()
    }

    val testView = lithoTestRule.render { recording("A") }
    lithoTestRule.idle()

    testView.setRoot(recording("B"))
    // Let "A" finish exiting and be released, collapsing the container to just "B".
    animationRule.advanceBy(2_000.ms)
    lithoTestRule.idle()

    // The B transition's initialState must remain "A" for B's whole lifetime — even after "A" is
    // gone. If it collapsed to "B" (initialState == targetState) the apparent direction would flip.
    val forTargetB = seen.filter { it.second == "B" }
    assertThat(forTargetB).isNotEmpty
    assertThat(forTargetB).allMatch { it.first == "A" }
  }

  @Test
  fun exitNone_releasesOutgoingImmediately_noStacking() {
    val testView = lithoTestRule.render { noTransitionContent(target = "A") }
    lithoTestRule.idle()
    assertThat(slotCount(testView)).isEqualTo(1)

    testView.setRoot(noTransitionContent(target = "B"))
    lithoTestRule.idle()
    // With no exit animation, the outgoing "A" must be released immediately, not left mounted on
    // top
    // of "B". (An exit with no animated values never fires onSettled, so without an explicit guard
    // it
    // would stay stacked forever.)
    assertThat(slotCount(testView))
        .`as`("no-exit-animation outgoing is released immediately, not stacked")
        .isEqualTo(1)
  }

  @Test
  fun slideIntoContainer_isContainerRelativeWithDirectionalSigns() {
    val scope = AnimatedContentTransitionScope("A", "B")
    with(scope) {
      // Left: enters from the right edge → starts at +full (positive X), measured on the container.
      val left = checkNotNull(slideIntoContainer(SlideDirection.Left).data.slide)
      assertThat(left.relativeToContainer).isTrue
      assertThat(left.offsetX?.invoke(100)).isEqualTo(100)
      assertThat(left.offsetY).isNull()
      // Right: enters from the left → starts at -full.
      assertThat(slideIntoContainer(SlideDirection.Right).data.slide?.offsetX?.invoke(100))
          .isEqualTo(-100)
      // Up: enters from below → +full on Y; X unused.
      val up = checkNotNull(slideIntoContainer(SlideDirection.Up).data.slide)
      assertThat(up.offsetX).isNull()
      assertThat(up.offsetY?.invoke(100)).isEqualTo(100)
      // The offset lambda is applied to the container extent.
      assertThat(
          slideIntoContainer(SlideDirection.Left) { it / 2 }.data.slide?.offsetX?.invoke(100),
      )
          .isEqualTo(50)
    }
  }

  @Test
  fun slideOutOfContainer_endsOnTheTowardsEdge() {
    val scope = AnimatedContentTransitionScope("A", "B")
    with(scope) {
      // Left: exits moving left → ends at -full.
      val left = checkNotNull(slideOutOfContainer(SlideDirection.Left).data.slide)
      assertThat(left.relativeToContainer).isTrue
      assertThat(left.offsetX?.invoke(100)).isEqualTo(-100)
      // Down: exits moving down → +full on Y.
      assertThat(slideOutOfContainer(SlideDirection.Down).data.slide?.offsetY?.invoke(100))
          .isEqualTo(100)
    }
  }

  @Test
  fun containerSlide_swapsAndSettlesWithoutLeak() {
    val testView = lithoTestRule.render { containerSlideContent("A") }
    lithoTestRule.idle()
    assertThat(slotCount(testView)).isEqualTo(1)

    testView.setRoot(containerSlideContent("B"))
    lithoTestRule.idle()
    assertThat(slotCount(testView)).`as`("outgoing retained while it slides out").isEqualTo(2)

    animationRule.advanceBy(2_000.ms)
    lithoTestRule.idle()
    assertThat(slotCount(testView)).`as`("slides settle, outgoing released").isEqualTo(1)
  }

  // --- Helpers ---

  private fun containerSlideContent(target: String): Component = AnimatedContent(
      targetState = target,
      transitionSpec = {
        enter = slideIntoContainer(SlideDirection.Left, linearFade)
        exit = slideOutOfContainer(SlideDirection.Left, linearFade)
      },
  ) {
    Marker()
  }

  private fun noTransitionContent(target: String): Component = AnimatedContent(
      targetState = target,
      // No enter/exit set: both default to None, so the swap is instant with no animation.
      transitionSpec = {},
  ) {
    Marker()
  }

  /**
   * The enter/exit binders are applied to a wrapper around the slot's content, so the tagged view's
   * parent is the one carrying the animated alpha.
   */
  private fun slotAlpha(testView: TestLithoView, state: String): Float? =
      (testView.findViewWithTagOrNull("slot-$state")?.parent as? View)?.alpha

  private fun taggedContent(target: String): Component = AnimatedContent(
      targetState = target,
      transitionSpec = {
        enter = fadeIn(linearFade)
        exit = fadeOut(linearFade)
      },
  ) { state ->
    Row(style = Style.width(50.px).height(50.px).viewTag("slot-$state"))
  }

  @Test
  fun reverseBeforeSettle_returningContentAnimatesInsteadOfSnapping() {
    val testView = lithoTestRule.render { taggedContent("A") }
    lithoTestRule.idle()

    testView.setRoot(taggedContent("B"))
    lithoTestRule.idle()
    animationRule.advanceBy(200.ms)
    val midExit = checkNotNull(slotAlpha(testView, "A"))
    assertThat(midExit).isGreaterThan(0f).isLessThan(1f)

    // Reverse before "A" finishes exiting. Reported from the last settled state the segment
    // collapses to (Visible, Visible), no branch matches, and the enter falls through to snap() —
    // "A" would jump straight back to fully opaque.
    testView.setRoot(taggedContent("A"))
    lithoTestRule.idle()
    animationRule.advanceBy(16.ms)

    assertThat(checkNotNull(slotAlpha(testView, "A"))).isGreaterThan(midExit).isLessThan(1f)
  }

  private fun content(target: String): Component = AnimatedContent(
      targetState = target,
      transitionSpec = {
        enter = fadeIn(linearFade)
        exit = fadeOut(linearFade)
      },
  ) {
    Marker()
  }

  private fun containerWidth(testView: TestLithoView): Int? =
      testView.findViewWithTagOrNull(ROOT_TAG)?.width

  /**
   * An [AnimatedContent] with per-state content widths, wrapped in a [Column] so the container
   * sizes to its in-flow child rather than to the root size spec. The container is tagged for
   * measurement.
   */
  private fun sizedContent(target: String): Component = Column {
    child(
        AnimatedContent(
            targetState = target,
            transitionSpec = {
              enter = fadeIn(linearFade)
              exit = fadeOut(linearFade)
            },
            style = Style.alignSelf(YogaAlign.FLEX_START).viewTag(ROOT_TAG),
        ) { state ->
          Row(style = Style.width(widthForState(state).px).height(50.px))
        },
    )
  }

  private fun widthForState(state: String): Int =
      when (state) {
        "A" -> 80
        "B" -> 120
        else -> 60
      }

  private class Marker : KComponent() {
    override fun ComponentScope.render(): Component = Row(style = Style.width(50.px).height(50.px))
  }

  companion object {
    private const val ROOT_TAG = "ac_root"
  }
}
