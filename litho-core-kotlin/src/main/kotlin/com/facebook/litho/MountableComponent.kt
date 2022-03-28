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

package com.facebook.litho

/**
 * <p>Base class for Kotlin mountable components. This class encapsulates some of the Mount Spec
 * APIs. All Kotlin mountable components must extend this class.</p>
 *
 * <p>Experimental. Currently for Litho team internal use only.</p>
 */
abstract class MountableComponent(open val style: Style? = null) : Component() {

  final override fun prepare(c: ComponentContext): PrepareResult {
    val componentScope = ComponentScope(c)
    val mountable = componentScope.render()
    style?.applyToComponent(c.resourceResolver, this)
    return PrepareResult(mountable, componentScope.transitions, componentScope.useEffectEntries)
  }

  /** This function must return [Mountable] which are immutable. */
  abstract fun ComponentScope.render(): Mountable<*>

  final override fun getMountType(): MountType = MountType.MOUNTABLE

  final override fun canMeasure(): Boolean {
    return true
  }
}
