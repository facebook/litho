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

package com.facebook.litho.widget

import android.graphics.Color
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.Diff
import com.facebook.litho.Row
import com.facebook.litho.StateValue
import com.facebook.litho.Transition
import com.facebook.litho.TransitionEndEvent
import com.facebook.litho.animation.AnimatedProperties
import com.facebook.litho.animation.AnimatedProperty
import com.facebook.litho.annotations.FromEvent
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnCreateTransition
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.OnUpdateState
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.PropDefault
import com.facebook.litho.annotations.State
import com.facebook.yoga.YogaAlign
import java.lang.IllegalStateException

@LayoutSpec
object TransitionEndCallbackTestComponentSpec {

  private const val TRANSITION_KEY = "TRANSITION_KEY"
  const val ANIM_X = "ANIM_X"
  const val ANIM_ALPHA = "ANIM_ALPHA"
  const val ANIM_DISAPPEAR = "ANIM_DISAPPEAR"

  @PropDefault const val testPropForRenderData: String = "default"

  @JvmStatic
  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @Prop caller: Caller,
      @Prop(optional = true) testPropForRenderData: String,
      @State state: Boolean
  ): Component? {
    caller.set(c)
    return when (caller.testType) {
      TestType.SAME_KEY -> getSameKeyTestComponent(c, state)
      TestType.DISAPPEAR -> getDisappearComponent(c, state)
      else -> null
    }
  }

  private fun getDisappearComponent(c: ComponentContext, state: Boolean): Component =
      Column.create(c)
          .child(Row.create(c).heightDip(50f).widthDip(50f).backgroundColor(Color.YELLOW))
          .child(
              if (!state)
                  Row.create(c)
                      .heightDip(50f)
                      .widthDip(50f)
                      .backgroundColor(Color.RED)
                      .transitionKey(TRANSITION_KEY)
                      .key(TRANSITION_KEY)
              else null)
          .build()

  private fun getSameKeyTestComponent(c: ComponentContext, state: Boolean): Component =
      Column.create(c)
          .child(
              Column.create(c)
                  .alignItems(if (!state) YogaAlign.FLEX_START else YogaAlign.FLEX_END)
                  .child(
                      Row.create(c)
                          .heightDip(50f)
                          .widthDip(50f)
                          .backgroundColor(Color.RED)
                          .alpha(if (!state) 1.0f else 0.2f)
                          .transitionKey(TRANSITION_KEY)))
          .build()

  @JvmStatic
  @OnEvent(TransitionEndEvent::class)
  fun onTransitionEnd(
      c: ComponentContext,
      @FromEvent transitionKey: String,
      @FromEvent property: AnimatedProperty,
      @State state: Boolean,
      @Prop caller: Caller
  ) {
    when (caller.testType) {
      TestType.SAME_KEY ->
          if (property === AnimatedProperties.X) {
            caller.transitionEndMessage = ANIM_X
          } else {
            caller.transitionEndMessage = ANIM_ALPHA
          }
      TestType.DISAPPEAR -> caller.transitionEndMessage = if (!state) ANIM_DISAPPEAR else ""
      null -> Unit
    }
  }

  @JvmStatic
  @OnUpdateState
  fun updateState(state: StateValue<Boolean>) {
    state.set(!state.get()!!)
  }

  @JvmStatic
  @OnCreateTransition
  fun onCreateTransition(
      c: ComponentContext,
      @Prop caller: Caller,
      @Prop(optional = true) testPropForRenderData: Diff<String>,
      @State state: Boolean,
  ): Transition? {
    if (state && testPropForRenderData.previous == null) {
      throw IllegalStateException("previous render must be set for subsequent layouts but was null")
    }
    val transition =
        when (caller.testType) {
          TestType.SAME_KEY ->
              Transition.parallel(
                  Transition.create(TRANSITION_KEY)
                      .animate(AnimatedProperties.ALPHA)
                      .animator(Transition.timing(100))
                      .transitionEndHandler(TransitionEndCallbackTestComponent.onTransitionEnd(c)),
                  Transition.create(TRANSITION_KEY)
                      .animate(AnimatedProperties.X)
                      .animator(Transition.timing(200))
                      .transitionEndHandler(TransitionEndCallbackTestComponent.onTransitionEnd(c)))
          TestType.DISAPPEAR ->
              Transition.create(Transition.TransitionKeyType.LOCAL, TRANSITION_KEY)
                  .animate(AnimatedProperties.SCALE)
                  .appearFrom(0f)
                  .disappearTo(0f)
                  .transitionEndHandler(TransitionEndCallbackTestComponent.onTransitionEnd(c))
          else -> null
        }
    return transition
  }

  enum class TestType {
    SAME_KEY,
    DISAPPEAR
  }

  class Caller {
    var c: ComponentContext? = null
    var transitionEndMessage: String = ""
    var testType: TestType? = null

    fun set(c: ComponentContext?) {
      this.c = c
    }

    fun toggle() {
      TransitionEndCallbackTestComponent.updateStateSync(c)
    }
  }
}
