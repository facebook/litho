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
import com.facebook.litho.ComponentLayout
import com.facebook.litho.ComponentTree
import com.facebook.litho.LithoView
import com.facebook.litho.Output
import com.facebook.litho.Row
import com.facebook.litho.Size
import com.facebook.litho.SizeSpec
import com.facebook.litho.StateCaller
import com.facebook.litho.Transition
import com.facebook.litho.animation.AnimatedProperties
import com.facebook.litho.annotations.FromPrepare
import com.facebook.litho.annotations.MountSpec
import com.facebook.litho.annotations.OnCreateMountContent
import com.facebook.litho.annotations.OnMeasure
import com.facebook.litho.annotations.OnMount
import com.facebook.litho.annotations.OnPrepare
import com.facebook.litho.annotations.Prop

@MountSpec
object TestAnimationMountSpec {

  @JvmStatic
  @OnPrepare
  fun onPrepare(c: ComponentContext, @Prop stateCaller: StateCaller, state: Output<StateCaller>) {
    state.set(stateCaller)
  }

  @JvmStatic
  @OnCreateMountContent
  fun onCreateMountContent(context: Context?): LithoView = LithoView(context)

  @JvmStatic
  @OnMount
  fun onMount(c: ComponentContext, view: LithoView, @FromPrepare state: StateCaller) {
    val viewComponentContext = ComponentContext(view.context)
    val transitionKey = "TRANSITION_KEY"
    val component =
        TestAnimationsComponent.create(viewComponentContext)
            .stateCaller(state)
            .transition(
                Transition.create(transitionKey)
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.ALPHA)
                    .disappearTo(0f))
            .testComponent { componentContext, state ->

              // This could be a lambda but it fails ci.
              Column.create(componentContext)
                  .child(
                      Row.create(componentContext)
                          .heightDip(50f)
                          .widthDip(50f)
                          .backgroundColor(Color.YELLOW))
                  .child(
                      if (!state)
                          Row.create(componentContext)
                              .heightDip(50f)
                              .widthDip(50f)
                              .backgroundColor(Color.RED)
                              .transitionKey(transitionKey)
                              .key(transitionKey)
                              .viewTag("TestAnimationMount")
                      else null)
                  .build()
            }
            .build()
    view.componentTree = ComponentTree.create(viewComponentContext, component).build()
  }

  @JvmStatic
  @OnMeasure
  fun onMeasure(
      c: ComponentContext,
      layout: ComponentLayout,
      widthSpec: Int,
      heightSpec: Int,
      size: Size
  ) {
    // If width is undefined, set default size.
    if (SizeSpec.getMode(widthSpec) == SizeSpec.UNSPECIFIED) {
      size.width = 50
    } else {
      size.width = SizeSpec.getSize(widthSpec)
    }

    // If height is undefined, use 1.5 aspect ratio.
    if (SizeSpec.getMode(heightSpec) == SizeSpec.UNSPECIFIED) {
      size.height = 50
    } else {
      size.height = SizeSpec.getSize(heightSpec)
    }
  }
}
