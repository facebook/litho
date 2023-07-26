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

package com.facebook.litho.widget.collection

import android.graphics.Color
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.core.padding
import com.facebook.litho.flexbox.position
import com.facebook.litho.flexbox.positionType
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.useEffect
import com.facebook.litho.useRef
import com.facebook.litho.view.alpha
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.widget.Text
import com.facebook.rendercore.dp
import com.facebook.rendercore.px
import com.facebook.rendercore.sp
import com.facebook.yoga.YogaPositionType

/** A component that overlays a render count on top of its children. */
class OverlayRenderCount(val component: Component) : KComponent() {
  override fun ComponentScope.render(): Component {
    val renderCount = useRef { 1 }
    useEffect(Any()) {
      renderCount.value++
      null
    }
    return Column {
      child(component)
      child(
          Column(
              style =
                  Style.positionType(YogaPositionType.ABSOLUTE)
                      .position(bottom = 0.px, end = 0.px)) {
                child(
                    Text(
                        text = "renderCount = ${renderCount.value}",
                        textSize = 8.sp,
                        textColor = Color.WHITE,
                        backgroundColor = Color.DKGRAY,
                        style = Style.alpha(.8f).padding(horizontal = 8.dp)))
              })
    }
  }
}

val Component?.overlayRenderCount: Component?
  get() = this?.let { OverlayRenderCount(it) }
