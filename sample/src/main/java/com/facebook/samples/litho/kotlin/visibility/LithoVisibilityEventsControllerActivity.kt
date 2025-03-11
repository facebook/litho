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

package com.facebook.samples.litho.kotlin.visibility

import android.os.Bundle
import android.util.Log
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.LithoView
import com.facebook.litho.LithoVisibilityEventsController
import com.facebook.litho.LithoVisibilityEventsController.LithoVisibilityState
import com.facebook.litho.LithoVisibilityEventsControllerDelegate
import com.facebook.litho.Style
import com.facebook.litho.core.width
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.visibility.onInvisible
import com.facebook.litho.visibility.onVisible
import com.facebook.rendercore.dp
import com.facebook.samples.litho.NavigatableDemoActivity

class LithoVisibilityEventsControllerActivity : NavigatableDemoActivity() {
  val delegate: LithoVisibilityEventsController = LithoVisibilityEventsControllerDelegate()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val lithoView = LithoView.create(ComponentContext(this), VisibilityComponent(), delegate)
    setContentView(lithoView)
  }

  override fun onResume() {
    super.onResume()
    delegate.moveToVisibilityState(LithoVisibilityState.HINT_VISIBLE)
  }

  override fun onPause() {
    super.onPause()
    delegate.moveToVisibilityState(LithoVisibilityState.HINT_INVISIBLE)
  }

  override fun onDestroy() {
    super.onDestroy()
    delegate.moveToVisibilityState(LithoVisibilityState.DESTROYED)
  }

  private class VisibilityComponent : KComponent() {
    override fun ComponentScope.render(): Component? {
      return Column(
          style =
              Style.width(200.dp)
                  .width(200.dp)
                  .onVisible { Log.d("VisibilityComponent", "onVisible") }
                  .onInvisible { Log.d("VisibilityComponent", "onInvisible") }) {
            child(Text("Hello World"))
          }
    }
  }
}
