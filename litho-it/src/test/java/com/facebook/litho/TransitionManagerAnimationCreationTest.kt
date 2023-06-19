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

import android.graphics.Rect
import com.facebook.litho.Transition.TransitionAnimator
import com.facebook.litho.TransitionManager.OnAnimationCompleteListener
import com.facebook.litho.animation.AnimatedProperties
import com.facebook.litho.animation.AnimatedProperty
import com.facebook.litho.animation.PropertyAnimation
import com.facebook.litho.animation.PropertyHandle
import com.facebook.litho.animation.SpringTransition
import com.facebook.litho.testing.testrunner.LithoTestRunner
import java.util.ArrayList
import java.util.LinkedHashMap
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/** Tests for the creation of animations using the targeting API in [Transition]. */
@RunWith(LithoTestRunner::class)
class TransitionManagerAnimationCreationTest {

  private var transitionManager: TransitionManager? = null
  private lateinit var testVerificationAnimator: TransitionAnimator
  private val createdAnimations = ArrayList<PropertyAnimation>()

  @Before
  fun setup() {
    transitionManager =
        TransitionManager(
            object : OnAnimationCompleteListener<Any?> {
              override fun onAnimationComplete(transitionId: TransitionId?) = Unit

              override fun onAnimationUnitComplete(propertyHandle: PropertyHandle, data: Any?) =
                  Unit
            },
            if (AnimationsDebug.ENABLED) AnimationsDebug.TAG else null,
            ComponentsSystrace.systrace)
    testVerificationAnimator = TransitionAnimator { propertyAnimation ->
      createdAnimations.add(propertyAnimation)
      SpringTransition(propertyAnimation)
    }
  }

  @After
  fun tearDown() {
    transitionManager = null
    createdAnimations.clear()
  }

  @Test
  fun testCreateSingleAnimation() {
    val current =
        createMockLayoutState(
            Transition.parallel<Transition>(), createMockLayoutOutput("test", 0, 0))
    val next =
        createMockLayoutState(
            Transition.parallel(
                Transition.create(Transition.TransitionKeyType.GLOBAL, "test")
                    .animate(AnimatedProperties.X)
                    .animator(testVerificationAnimator)),
            createMockLayoutOutput("test", 10, 10))
    transitionManager?.setupTransitions(
        current, next, TransitionManager.getRootTransition(next.transitions))
    assertThat(createdAnimations)
        .containsExactlyInAnyOrder(createPropertyAnimation("test", AnimatedProperties.X, 10f))
  }

  @Test
  fun testCreateMultiPropAnimation() {
    val current =
        createMockLayoutState(
            Transition.parallel<Transition>(), createMockLayoutOutput("test", 0, 0))
    val next =
        createMockLayoutState(
            Transition.parallel(
                Transition.create(Transition.TransitionKeyType.GLOBAL, "test")
                    .animate(AnimatedProperties.X, AnimatedProperties.Y)
                    .animator(testVerificationAnimator)),
            createMockLayoutOutput("test", 10, 10))
    transitionManager?.setupTransitions(
        current, next, TransitionManager.getRootTransition(next.transitions))
    assertThat(createdAnimations)
        .containsExactlyInAnyOrder(
            createPropertyAnimation("test", AnimatedProperties.X, 10f),
            createPropertyAnimation("test", AnimatedProperties.Y, 10f))
  }

  @Test
  fun testCreateMultiPropAnimationWithNonChangingProp() {
    val current =
        createMockLayoutState(
            Transition.parallel<Transition>(), createMockLayoutOutput("test", 0, 0))
    val next =
        createMockLayoutState(
            Transition.parallel(
                Transition.create(Transition.TransitionKeyType.GLOBAL, "test")
                    .animate(AnimatedProperties.X, AnimatedProperties.Y)
                    .animator(testVerificationAnimator)),
            createMockLayoutOutput("test", 10, 0))
    transitionManager?.setupTransitions(
        current, next, TransitionManager.getRootTransition(next.transitions))
    assertThat(createdAnimations)
        .containsExactlyInAnyOrder(createPropertyAnimation("test", AnimatedProperties.X, 10f))
  }

  @Test
  fun testCreateMultiComponentAnimation() {
    val current =
        createMockLayoutState(
            Transition.parallel<Transition>(),
            createMockLayoutOutput("test1", 0, 0),
            createMockLayoutOutput("test2", 0, 0))
    val next =
        createMockLayoutState(
            Transition.parallel(
                Transition.create(Transition.TransitionKeyType.GLOBAL, "test1", "test2")
                    .animate(AnimatedProperties.X)
                    .animator(testVerificationAnimator)),
            createMockLayoutOutput("test1", 10, 10),
            createMockLayoutOutput("test2", -10, -10))
    transitionManager?.setupTransitions(
        current, next, TransitionManager.getRootTransition(next.transitions))
    assertThat(createdAnimations)
        .containsExactlyInAnyOrder(
            createPropertyAnimation("test1", AnimatedProperties.X, 10f),
            createPropertyAnimation("test2", AnimatedProperties.X, -10f))
  }

  @Test
  fun testSetsOfComponentsAndPropertiesAnimation() {
    val current =
        createMockLayoutState(
            Transition.parallel<Transition>(),
            createMockLayoutOutput("test1", 0, 0),
            createMockLayoutOutput("test2", 0, 0))
    val next =
        createMockLayoutState(
            Transition.parallel(
                Transition.create(Transition.TransitionKeyType.GLOBAL, "test1", "test2")
                    .animate(AnimatedProperties.X, AnimatedProperties.Y)
                    .animator(testVerificationAnimator)),
            createMockLayoutOutput("test1", 10, 10),
            createMockLayoutOutput("test2", -10, -10))
    transitionManager?.setupTransitions(
        current, next, TransitionManager.getRootTransition(next.transitions))
    assertThat(createdAnimations)
        .containsExactlyInAnyOrder(
            createPropertyAnimation("test1", AnimatedProperties.X, 10f),
            createPropertyAnimation("test1", AnimatedProperties.Y, 10f),
            createPropertyAnimation("test2", AnimatedProperties.X, -10f),
            createPropertyAnimation("test2", AnimatedProperties.Y, -10f))
  }

  @Test
  fun testAutoLayoutAnimation() {
    val current =
        createMockLayoutState(
            Transition.parallel<Transition>(),
            createMockLayoutOutput("test1", 0, 0),
            createMockLayoutOutput("test2", 0, 0))
    val next =
        createMockLayoutState(
            Transition.parallel(Transition.allLayout().animator(testVerificationAnimator)),
            createMockLayoutOutput("test1", 10, 20, 200, 150),
            createMockLayoutOutput("test2", -10, -20, 50, 80))
    transitionManager?.setupTransitions(
        current, next, TransitionManager.getRootTransition(next.transitions))
    assertThat(createdAnimations)
        .containsExactlyInAnyOrder(
            createPropertyAnimation("test1", AnimatedProperties.X, 10f),
            createPropertyAnimation("test1", AnimatedProperties.Y, 20f),
            createPropertyAnimation("test1", AnimatedProperties.WIDTH, 200f),
            createPropertyAnimation("test1", AnimatedProperties.HEIGHT, 150f),
            createPropertyAnimation("test2", AnimatedProperties.X, -10f),
            createPropertyAnimation("test2", AnimatedProperties.Y, -20f),
            createPropertyAnimation("test2", AnimatedProperties.WIDTH, 50f),
            createPropertyAnimation("test2", AnimatedProperties.HEIGHT, 80f))
  }

  @Test
  fun testKeyDoesntExist() {
    val current =
        createMockLayoutState(
            Transition.parallel<Transition>(), createMockLayoutOutput("test", 0, 0))
    val next =
        createMockLayoutState(
            Transition.parallel(
                Transition.create(Transition.TransitionKeyType.GLOBAL, "test", "keydoesntexist")
                    .animate(AnimatedProperties.X, AnimatedProperties.Y)
                    .animator(testVerificationAnimator)),
            createMockLayoutOutput("test", 10, 0))
    transitionManager?.setupTransitions(
        current, next, TransitionManager.getRootTransition(next.transitions))
    assertThat(createdAnimations)
        .containsExactlyInAnyOrder(createPropertyAnimation("test", AnimatedProperties.X, 10f))
  }

  @Test
  fun testNoPreviousLayoutState() {
    val next =
        createMockLayoutState(
            Transition.parallel(
                Transition.create(Transition.TransitionKeyType.GLOBAL, "test", "keydoesntexist")
                    .animate(AnimatedProperties.X, AnimatedProperties.Y)
                    .animator(testVerificationAnimator),
                Transition.create(Transition.TransitionKeyType.GLOBAL, "appearing")
                    .animate(AnimatedProperties.X)
                    .appearFrom(0f)
                    .animator(testVerificationAnimator)),
            createMockLayoutOutput("test", 10, 0),
            createMockLayoutOutput("appearing", 20, 0))
    transitionManager?.setupTransitions(
        null, next, TransitionManager.getRootTransition(next.transitions))
    assertThat(createdAnimations)
        .containsExactlyInAnyOrder(createPropertyAnimation("appearing", AnimatedProperties.X, 20f))
  }

  @Test
  fun testWithMountTimeAnimations() {
    val current =
        createMockLayoutState(
            Transition.parallel<Transition>(),
            createMockLayoutOutput("test", 0, 0),
            createMockLayoutOutput("test2", 0, 0))
    val next =
        createMockLayoutState(
            Transition.parallel(
                Transition.create(Transition.TransitionKeyType.GLOBAL, "test", "keydoesntexist")
                    .animate(AnimatedProperties.X, AnimatedProperties.Y)
                    .animator(testVerificationAnimator)),
            createMockLayoutOutput("test", 10, 0),
            createMockLayoutOutput("test2", 20, 0))
    val mountTimeTransitions = ArrayList<Transition>()
    mountTimeTransitions.add(
        Transition.create(Transition.TransitionKeyType.GLOBAL, "test2")
            .animate(AnimatedProperties.X)
            .appearFrom(0f)
            .animator(testVerificationAnimator))
    val allTransitions: MutableList<Transition> = ArrayList()
    next.transitions?.let { allTransitions.addAll(it) }
    allTransitions.addAll(mountTimeTransitions)
    transitionManager?.setupTransitions(
        current, next, TransitionManager.getRootTransition(allTransitions))
    assertThat(createdAnimations)
        .containsExactlyInAnyOrder(
            createPropertyAnimation("test", AnimatedProperties.X, 10f),
            createPropertyAnimation("test2", AnimatedProperties.X, 20f))
  }

  @Test
  fun testWithOnlyMountTimeAnimations() {
    val current =
        createMockLayoutState(
            Transition.parallel<Transition>(),
            createMockLayoutOutput("test", 0, 0),
            createMockLayoutOutput("test2", 0, 0))
    val next =
        createMockLayoutState(
            null, createMockLayoutOutput("test", 10, 0), createMockLayoutOutput("test2", 20, 0))
    val mountTimeTransitions = ArrayList<Transition>()
    mountTimeTransitions.add(
        Transition.create(Transition.TransitionKeyType.GLOBAL, "test2")
            .animate(AnimatedProperties.X)
            .appearFrom(0f)
            .animator(testVerificationAnimator))
    mountTimeTransitions.add(
        Transition.create(Transition.TransitionKeyType.GLOBAL, "test", "keydoesntexist")
            .animate(AnimatedProperties.X, AnimatedProperties.Y)
            .animator(testVerificationAnimator))
    transitionManager?.setupTransitions(
        current, next, TransitionManager.getRootTransition(mountTimeTransitions))
    assertThat(createdAnimations)
        .containsExactlyInAnyOrder(
            createPropertyAnimation("test", AnimatedProperties.X, 10f),
            createPropertyAnimation("test2", AnimatedProperties.X, 20f))
  }

  @Test
  fun testAnimationFromStateUpdate() {
    val current =
        createMockLayoutState(
            Transition.parallel<Transition>(),
            createMockLayoutOutput("test1", 0, 0),
            createMockLayoutOutput("test2", 0, 0))
    val animationsCreatedFromStateUpdate = ArrayList<PropertyAnimation>()
    val animatorForStateUpdate = TransitionAnimator { propertyAnimation ->
      animationsCreatedFromStateUpdate.add(propertyAnimation)
      SpringTransition(propertyAnimation)
    }
    val transitionsFromStateUpdate = ArrayList<Transition>()
    transitionsFromStateUpdate.add(
        Transition.create(Transition.TransitionKeyType.GLOBAL, "test1", "test2")
            .animate(AnimatedProperties.Y)
            .animator(animatorForStateUpdate))
    val next =
        createMockLayoutState(
            Transition.parallel(
                Transition.create(Transition.TransitionKeyType.GLOBAL, "test1", "test2")
                    .animate(AnimatedProperties.X)
                    .animator(testVerificationAnimator)),
            createMockLayoutOutput("test1", 10, 20),
            createMockLayoutOutput("test2", -10, -20))
    val allTransitions: MutableList<Transition> = ArrayList()
    next.transitions?.let { allTransitions.addAll(it) }
    allTransitions.addAll(transitionsFromStateUpdate)
    transitionManager?.setupTransitions(
        current, next, TransitionManager.getRootTransition(allTransitions))
    assertThat(createdAnimations.size).isEqualTo(2)
    assertThat(animationsCreatedFromStateUpdate)
        .containsExactlyInAnyOrder(
            createPropertyAnimation("test1", AnimatedProperties.Y, 20f),
            createPropertyAnimation("test2", AnimatedProperties.Y, -20f))
  }

  private fun createPropertyAnimation(
      key: String,
      property: AnimatedProperty,
      endValue: Float
  ): PropertyAnimation {
    val transitionId = TransitionId(TransitionId.Type.GLOBAL, key, null)
    return PropertyAnimation(PropertyHandle(transitionId, property), endValue)
  }

  /** @return a mock LayoutState that only has a transition key -> LayoutOutput mapping. */
  private fun createMockLayoutState(
      transitions: TransitionSet?,
      vararg animatableItems: AnimatableItem
  ): LayoutState {
    val transitionIdMapping: MutableMap<TransitionId, OutputUnitsAffinityGroup<AnimatableItem>> =
        LinkedHashMap()
    animatableItems.forEach { animatableItem ->
      val transitionId = animatableItem.transitionId ?: return@forEach
      var group = transitionIdMapping[transitionId]
      if (group == null) {
        group = OutputUnitsAffinityGroup()
        transitionIdMapping[transitionId] = group
      }
      @OutputUnitType val type = LayoutState.getTypeFromId(animatableItem.id)
      group.add(type, animatableItem)
    }
    val layoutState = mock<LayoutState>()
    whenever(layoutState.transitions).thenReturn(transitions?.children)
    whenever(layoutState.transitionIdMapping).thenReturn(transitionIdMapping)
    whenever(layoutState.getAnimatableItemForTransitionId(anyOrNull())).then { invocation ->
      val transitionId = invocation.arguments[0] as TransitionId
      requireNotNull(transitionIdMapping[transitionId])
    }
    return layoutState
  }

  /** @return a mock LayoutOutput with a transition key and dummy bounds. */
  private fun createMockLayoutOutput(
      transitionKey: String,
      x: Int,
      y: Int,
      width: Int = 100,
      height: Int = 100
  ): AnimatableItem {
    val transitionId = TransitionId(TransitionId.Type.GLOBAL, transitionKey, null)
    val bounds = Rect(x, y, x + width, y + height)
    return LithoAnimtableItem(0, bounds, OutputUnitType.CONTENT, null, transitionId)
  }
}
