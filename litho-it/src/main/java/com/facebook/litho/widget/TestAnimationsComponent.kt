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
import com.facebook.litho.StateCaller
import com.facebook.litho.Transition
import com.facebook.litho.onCleanup
import com.facebook.litho.transition.useTransition
import com.facebook.litho.useEffect
import com.facebook.litho.useState

class TestAnimationsComponent(
    private val stateCaller: StateCaller,
    private val transition: Transition?,
    private val componentCall: ComponentScope.(Boolean) -> Component,
) : KComponent() {
  override fun ComponentScope.render(): Component {
    useTransition(transition)
    val state = useState { false }
    useEffect(stateCaller) {
      stateCaller.setStateUpdateListener { state.update { !it } }
      onCleanup { stateCaller.setStateUpdateListener(null) }
    }
    return componentCall(state.value)
  }
}
