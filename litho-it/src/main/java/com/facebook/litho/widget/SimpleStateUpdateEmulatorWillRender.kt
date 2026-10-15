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

import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.LifecycleStep.StepInfo
import com.facebook.litho.State
import com.facebook.litho.Style
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.onCleanup
import com.facebook.litho.useEffect
import com.facebook.litho.useState

class SimpleStateUpdateEmulatorWillRender(
    private val caller: Caller,
    private val prefix: String? = null,
    private val style: Style = Style,
) : KComponent() {

  override fun ComponentScope.render(): Component {
    val countState = useState<Int> { 1 }
    useEffect(Unit) {
      caller.state = countState
      onCleanup { caller.state = null }
    }

    val info = ArrayList<StepInfo>()
    return Column(style = style) {
      child(Text((prefix ?: "Text: ") + countState.value))
      child(LayoutSpecWillRenderReuseTester.create(context).steps(info).build())
    }
  }

  class Caller {
    var state: State<Int>? = null

    fun incrementAsync() {
      val counter = state?.value ?: throw RuntimeException("state value is null.")
      state?.updateSync { counter + 1 }
    }
  }
}
