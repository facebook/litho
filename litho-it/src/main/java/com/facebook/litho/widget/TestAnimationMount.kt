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
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentTree
import com.facebook.litho.LithoPrimitive
import com.facebook.litho.LithoView
import com.facebook.litho.PrimitiveComponent
import com.facebook.litho.PrimitiveComponentScope
import com.facebook.litho.Row
import com.facebook.litho.StateCaller
import com.facebook.litho.Style
import com.facebook.litho.Transition
import com.facebook.litho.animation.AnimatedProperties
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.transition.transitionKey
import com.facebook.litho.useCached
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.view.viewTag
import com.facebook.rendercore.SizeConstraints
import com.facebook.rendercore.dp
import com.facebook.rendercore.primitives.LayoutBehavior
import com.facebook.rendercore.primitives.LayoutScope
import com.facebook.rendercore.primitives.PrimitiveLayoutResult
import com.facebook.rendercore.primitives.ViewAllocator

class TestAnimationMount(
    private val stateCaller: StateCaller,
) : PrimitiveComponent() {
  override fun PrimitiveComponentScope.render(): LithoPrimitive {
    val transitionKey = "TRANSITION_KEY"
    val component =
        useCached(stateCaller) {
          TestAnimationsComponent(
              stateCaller,
              Transition.create(transitionKey)
                  .animator(Transition.timing(144))
                  .animate(AnimatedProperties.ALPHA)
                  .disappearTo(0f)) { state ->
                Column {
                  child(Row(style = Style.height(50.dp).width(50.dp).backgroundColor(Color.YELLOW)))
                  child(
                      if (!state)
                          Row(
                              style =
                                  Style.height(50.dp)
                                      .width(50.dp)
                                      .backgroundColor(Color.RED)
                                      .transitionKey(context, transitionKey)
                                      .viewTag("TestAnimationMount"))
                      else null)
                }
              }
        }

    return LithoPrimitive(
        layoutBehavior = TestAnimationMountLayoutBehavior,
        mountBehavior =
            MountBehavior(ViewAllocator { context -> LithoView(context) }) {
              bind(component) { content ->
                content.componentTree =
                    ComponentTree.create(ComponentContext(androidContext), component).build()
                onUnbind { content.componentTree = null }
              }
            },
        style = null)
  }
}

private object TestAnimationMountLayoutBehavior : LayoutBehavior {
  override fun LayoutScope.layout(sizeConstraints: SizeConstraints): PrimitiveLayoutResult {
    // If width is undefined, set default size.
    val width =
        if (!sizeConstraints.hasBoundedWidth) {
          50
        } else {
          sizeConstraints.maxWidth
        }

    // If height is undefined, set default size.
    val height =
        if (!sizeConstraints.hasBoundedHeight) {
          50
        } else {
          sizeConstraints.maxHeight
        }

    return PrimitiveLayoutResult(width, height)
  }
}
