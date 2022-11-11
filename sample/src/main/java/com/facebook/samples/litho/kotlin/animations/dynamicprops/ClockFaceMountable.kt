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

package com.facebook.samples.litho.kotlin.animations.dynamicprops

import android.content.Context
import com.facebook.litho.DynamicValue
import com.facebook.litho.MeasureScope
import com.facebook.litho.MountableComponent
import com.facebook.litho.MountableComponentScope
import com.facebook.litho.MountableRenderResult
import com.facebook.litho.SimpleMountable
import com.facebook.litho.Style
import com.facebook.rendercore.MeasureResult

class ClockFaceMountable(private val time: DynamicValue<Long>, private val style: Style? = null) :
    MountableComponent() {

  override fun MountableComponentScope.render(): MountableRenderResult {
    time.bindTo(0L) { view: SimpleClockView, value -> view.time = value }

    return MountableRenderResult(Mountable(), style)
  }

  private class Mountable : SimpleMountable<SimpleClockView>(RenderType.VIEW) {

    override fun createContent(context: Context): SimpleClockView = SimpleClockView(context)

    override fun mount(c: Context, content: SimpleClockView, layoutData: Any?) = Unit

    override fun unmount(c: Context, content: SimpleClockView, layoutData: Any?) = Unit

    override fun MeasureScope.measure(widthSpec: Int, heightSpec: Int): MeasureResult =
        fromSpecs(widthSpec, heightSpec)
  }
}
