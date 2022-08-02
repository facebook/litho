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

package com.facebook.samples.litho.kotlin.documentation

import android.util.Log
import android.view.View
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Row
import com.facebook.litho.Style
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.visibility.onInvisible
import com.facebook.litho.visibility.onVisibilityChanged
import com.facebook.litho.visibility.onVisible

// start_example
class VisibilityHandlingExampleComponent : KComponent() {

  override fun ComponentScope.render(): Component {
    return Column(
        style =
            Style.onVisible { event ->
                  // If the handler was set on a component which mounts content then the
                  // event.content will be a reference to the mounted content.
                  if (event.content is View) {
                    log("Visible", "View")
                  } else {
                    log("Visible", "Drawable")
                  }
                }
                .onInvisible { log("Invisible", "null") }) {
          child(Text("hello world"))
          child(
              Row(
                  style =
                      Style.onVisibilityChanged { event ->
                        if (event.percentVisibleHeight > 50) {
                          Log.d(
                              "visibility-changed",
                              "View is mostly visible now. With: " +
                                  "\ntop: ${event.visibleTop}" +
                                  "\nleft: ${event.visibleLeft}" +
                                  "\nvisible width: ${event.visibleWidth}" +
                                  "\nvisible height: ${event.visibleHeight}" +
                                  "\npercentage visible height: ${event.percentVisibleHeight}" +
                                  "\npercentage visible width: ${event.percentVisibleWidth}")
                        }
                      }) {
                    child(Text("This is an example."))
                  })
        }
  }

  fun log(type: String, content: String) {
    Log.d("visibility", "Visibility callback: $type content: $content")
  }
}
// end_example
