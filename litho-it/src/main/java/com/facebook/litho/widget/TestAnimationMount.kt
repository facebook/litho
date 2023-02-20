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

import android.content.Context
import android.graphics.Color
import com.facebook.litho.Column
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentTree
import com.facebook.litho.LithoView
import com.facebook.litho.MeasureScope
import com.facebook.litho.MountableComponent
import com.facebook.litho.MountableComponentScope
import com.facebook.litho.MountableRenderResult
import com.facebook.litho.Row
import com.facebook.litho.SimpleMountable
import com.facebook.litho.SizeSpec
import com.facebook.litho.StateCaller
import com.facebook.litho.Style
import com.facebook.litho.Transition
import com.facebook.litho.animation.AnimatedProperties
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.dp
import com.facebook.litho.transition.transitionKey
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.view.viewTag
import com.facebook.rendercore.MeasureResult

class TestAnimationMount(
    private val stateCaller: StateCaller,
) : MountableComponent() {
  override fun MountableComponentScope.render(): MountableRenderResult {
    return MountableRenderResult(TestAnimationMountable(stateCaller), null)
  }
}

private class TestAnimationMountable(private val stateCaller: StateCaller) :
    SimpleMountable<LithoView>(RenderType.VIEW) {
  override fun createContent(context: Context): LithoView = LithoView(context)

  override fun MeasureScope.measure(widthSpec: Int, heightSpec: Int): MeasureResult {
    // If width is undefined, set default size.
    val width =
        if (SizeSpec.getMode(widthSpec) == SizeSpec.UNSPECIFIED) {
          50
        } else {
          SizeSpec.getSize(widthSpec)
        }

    // If height is undefined, use 1.5 aspect ratio.
    val height =
        if (SizeSpec.getMode(heightSpec) == SizeSpec.UNSPECIFIED) {
          50
        } else {
          SizeSpec.getSize(heightSpec)
        }

    return MeasureResult(width, height)
  }

  override fun mount(c: Context, content: LithoView, layoutData: Any?) {
    val transitionKey = "TRANSITION_KEY"
    val component =
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
    content.componentTree = ComponentTree.create(ComponentContext(c), component).build()
  }

  override fun unmount(c: Context, content: LithoView, layoutData: Any?) {}
}
