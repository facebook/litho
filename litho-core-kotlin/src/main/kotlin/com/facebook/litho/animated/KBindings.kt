/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.litho.animated

import com.facebook.litho.ComponentScope
import com.facebook.litho.DerivedDynamicValue
import com.facebook.litho.DynamicValue
import com.facebook.litho.annotations.Hook
import com.facebook.litho.useState

/**
 * Creates a [DynamicValue] with a static initial value. This DynamicValue can be passed to the
 * variants of [Style] properties that take DynamicValue, like [Style.backgroundColor] or
 * [Style.translationY]. You can then imperatively set a new value on the main thread to update that
 * property without a state update, or use the collection of [Animated] APIs to drive animations on
 * this DynamicValue, which will in turn animate the property.
 *
 * Note: You can only change or animate a DynamicValue on the main thread.
 */
@Hook
fun <T> ComponentScope.useBinding(initialValue: T) = useState { DynamicValue(initialValue) }.value

/**
 * Creates a [DynamicValue] deriving from an existing binding param, with modifications applied by
 * transform function.
 *
 * For example:
 * ```
 * val bgColor = useBinding(colorProgress) { progress -> Color.HSVToColor(floatArrayOf(100f *
 * progress, 100f, 255f)) }
 * ```
 */
@Hook
fun <T, S> ComponentScope.useBinding(
    binding: DynamicValue<T>,
    transform: (T) -> S
): DynamicValue<S> {
  return useState { DerivedDynamicValue(binding, transform) }.value
}
