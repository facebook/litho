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

import com.facebook.litho.animation.AnimatedProperties
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.testing.inlinelayoutspec.InlineLayoutSpec
import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class CollectTransitionsTest {
  @JvmField @Rule val lithoViewRule: LithoViewRule = LithoViewRule()
  private val wrappingContentWithTransition: InlineLayoutSpec =
      object : InlineLayoutSpec() {
        protected override fun onCreateLayout(c: ComponentContext): Component {
          return Wrapper.create(c).delegate(componentWithTransition).build()
        }

        protected override fun onCreateTransition(c: ComponentContext): Transition? {
          return Transition.create(Transition.TransitionKeyType.GLOBAL, "test")
              .animate(AnimatedProperties.Y)
        }
      }
  private val componentWithTransition: InlineLayoutSpec =
      object : InlineLayoutSpec() {
        protected override fun onCreateLayout(c: ComponentContext): Component {
          return Row.create(c)
              .child(Row.create(c).transitionKey("test"))
              .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
              .child(Row.create(c).transitionKey("test2"))
              .transitionKeyType(Transition.TransitionKeyType.GLOBAL)
              .build()
        }

        protected override fun onCreateTransition(c: ComponentContext): Transition? {
          return Transition.create(Transition.TransitionKeyType.GLOBAL, "test")
              .animate(AnimatedProperties.X)
        }
      }

  @Test
  fun testCollectsWrappingTransitions() {
    val originalValue: Boolean = ComponentsConfiguration.isAnimationDisabled
    try {
      ComponentsConfiguration.isAnimationDisabled = false
      val testLithoView =
          lithoViewRule.render(widthPx = 100, heightPx = 100) { wrappingContentWithTransition }
      Assertions.assertThat(testLithoView.componentTree.mainThreadLayoutState!!.transitions)
          .hasSize(2)
    } finally {
      ComponentsConfiguration.isAnimationDisabled = originalValue
    }
  }
}
