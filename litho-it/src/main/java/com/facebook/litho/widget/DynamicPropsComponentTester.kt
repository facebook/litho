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
import com.facebook.litho.DynamicValue
import com.facebook.litho.LithoPrimitive
import com.facebook.litho.PrimitiveComponent
import com.facebook.litho.PrimitiveComponentScope
import com.facebook.litho.Style
import com.facebook.rendercore.primitives.ExactSizeConstraintsLayoutBehavior
import com.facebook.rendercore.primitives.ViewAllocator

class DynamicPropsComponentTester(
    private val dynamicPropValue: DynamicValue<Long>? = null,
    private val style: Style = Style,
) : PrimitiveComponent() {

  override fun PrimitiveComponentScope.render(): LithoPrimitive {
    return LithoPrimitive(
        layoutBehavior = ExactSizeConstraintsLayoutBehavior,
        mountBehavior =
            MountBehavior(ViewAllocator { context -> View(context) }) {
              bindDynamic(dynamicPropValue) { view, _ ->
                // Binding logic for dynamicPropValue (empty as in original spec)
                onUnbindDynamic {
                  // No-op unbind
                }
              }
            },
        style = style,
    )
  }
}
