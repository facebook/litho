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

package com.facebook.litho

import java.lang.reflect.Modifier

/** Base class for Kotlin Components. */
abstract class KComponent : Component() {

  final override fun onCreateLayout(c: ComponentContext): Component? = DslScope(c).render()

  abstract fun DslScope.render(): Component?

  /**
   * Compare this component to a different one to check if they are equivalent. This is used to be
   * able to skip rendering a component again.
   */
  override fun isEquivalentTo(other: Component): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    if (id == other.id) {
      return true
    }
    if (!hasEquivalentFields(other as KComponent)) {
      return false
    }

    return true
  }

  /** Compare all private final fields in the components. */
  private fun hasEquivalentFields(other: KComponent): Boolean {
    for (field in javaClass.declaredFields) {
      if (Modifier.isPrivate(field.modifiers) && Modifier.isFinal(field.modifiers)) {
        field.isAccessible = true
        val field1 = field.get(this)
        val field2 = field.get(other)
        field.isAccessible = false

        if (!EquivalenceUtils.areObjectsEquivalent(field1, field2)) {
          return false
        }
      }
    }

    return true
  }
}
