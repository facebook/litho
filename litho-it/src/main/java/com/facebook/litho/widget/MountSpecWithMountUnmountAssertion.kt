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

import android.view.View
import com.facebook.litho.LithoPrimitive
import com.facebook.litho.PrimitiveComponent
import com.facebook.litho.PrimitiveComponentScope
import com.facebook.litho.Style
import com.facebook.litho.useState
import com.facebook.rendercore.primitives.FixedSizeLayoutBehavior
import com.facebook.rendercore.primitives.ViewAllocator
import com.facebook.rendercore.px

/**
 * This component throws an exception if unmount is invoked on the component before mount was
 * invoked for it. This allows tests to assert that unmount is always invoked on a component for
 * which mount was invoked.
 */
class MountSpecWithMountUnmountAssertion(
    private val container: Container,
    private val hasTagSet: Boolean = false,
    private val style: Style? = null,
) : PrimitiveComponent() {

  override fun PrimitiveComponentScope.render(): LithoPrimitive {
    val holder = useState { container }

    return LithoPrimitive(
        layoutBehavior = FixedSizeLayoutBehavior(100.px, 100.px),
        mountBehavior =
            MountBehavior(ViewAllocator { context -> View(context) }) {
              bind(holder, hasTagSet) { content ->
                holder.value.value = "mounted"
                check(content.parent == null) { "The view must be detached when mount is called" }
                check(!(hasTagSet && content.tag != null)) {
                  "The tag must be set after mount is called."
                }

                onUnbind {
                  checkNotNull(holder.value.value) {
                    "The value was never set in mount. Which means that mount was not invoked for this instance of the component or unmount was called without mount."
                  }
                  check(content.parent == null) {
                    "The view must be detached before unmount is called"
                  }
                  check(!(hasTagSet && content.tag != null)) {
                    "The tag must be unset before unmount is called."
                  }
                }
              }
            },
        style = style,
    )
  }

  class Container {
    var value: String? = null
  }
}
