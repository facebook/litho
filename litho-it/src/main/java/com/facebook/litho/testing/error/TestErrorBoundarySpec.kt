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

package com.facebook.litho.testing.error

import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.StateValue
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateInitialState
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnError
import com.facebook.litho.annotations.OnUpdateState
import com.facebook.litho.annotations.Param
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.State
import com.facebook.litho.widget.Text
import com.facebook.yoga.YogaEdge
import java.util.Optional

@LayoutSpec
object TestErrorBoundarySpec {

  @JvmStatic
  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @Prop child: Component,
      @State error: Optional<String>
  ): Component =
      if (error.isPresent) {
        Text.create(c)
            .marginDip(YogaEdge.ALL, 8f)
            .textSizeSp(24f)
            .text(String.format("A WILD ERROR APPEARS:\n%s", error.get()))
            .build()
      } else {
        child
      }

  @JvmStatic
  @OnCreateInitialState
  fun createInitialState(c: ComponentContext, error: StateValue<Optional<String>>) {
    error.set(Optional.empty())
  }

  @JvmStatic
  @OnUpdateState
  fun updateError(error: StateValue<Optional<String>?>, @Param errorMsg: String) {
    error.set(Optional.of(errorMsg))
  }

  @JvmStatic
  @OnError
  fun onError(
      c: ComponentContext,
      e: Exception,
      @Prop(optional = true) errorOutput: MutableList<Exception?>?
  ) {
    errorOutput?.add(e)
        ?: TestErrorBoundary.updateErrorAsync(
            c, String.format("Error caught from boundary: %s", e.message))
  }
}
