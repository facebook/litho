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

import kotlin.reflect.KProperty

/**
 * Create a CachedValue variable within a Component. The [calculator] will provide the calculated
 * value if it hasn't already been calculated or if the inputs have changed since the previous
 * calculation.
 */
fun <T> DslScope.useCached(calculator: () -> T): CachedDelegate<T> =
    CachedDelegate(context, calculator = calculator)

fun <T> DslScope.useCached(input1: Any, calculator: () -> T): CachedDelegate<T> =
    CachedDelegate(context, input1, calculator = calculator)

fun <T> DslScope.useCached(input1: Any, input2: Any, calculator: () -> T): CachedDelegate<T> =
    CachedDelegate(context, input1, input2, calculator = calculator)

fun <T> DslScope.useCached(vararg inputs: Any, calculator: () -> T): CachedDelegate<T> =
    CachedDelegate(context, inputs = inputs, calculator = calculator)

class CachedDelegate<T>
    internal constructor(
        private val c: ComponentContext,
        private val input1: Any? = null,
        private val input2: Any? = null,
        private val inputs: Array<out Any>? = null,
        private val calculator: () -> T
    ) {
  operator fun getValue(nothing: Nothing?, property: KProperty<*>): T {
    val cacheInputs =
        CachedInputs(c.componentScope.javaClass, property.name, input1, input2, inputs)
    val result =
        c.getCachedValue(cacheInputs) ?: calculator().also { c.putCachedValue(cacheInputs, it) }

    @Suppress("UNCHECKED_CAST")
    return result as T
  }
}

internal class CachedInputs(
    val componentClz: Class<out Component>,
    val propertyName: String,
    val input1: Any? = null,
    val input2: Any? = null,
    val inputs: Array<out Any>? = null
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as CachedInputs

    if (componentClz != other.componentClz) return false
    if (propertyName != other.propertyName) return false
    if (input1 != other.input1) return false
    if (input2 != other.input2) return false
    if (inputs != null) {
      if (other.inputs == null) return false
      if (!inputs.contentEquals(other.inputs)) return false
    } else if (other.inputs != null) return false

    return true
  }

  override fun hashCode(): Int {
    var result = componentClz.hashCode()
    result = 31 * result + propertyName.hashCode()
    result = 31 * result + (input1?.hashCode() ?: 0)
    result = 31 * result + (input2?.hashCode() ?: 0)
    result = 31 * result + (inputs?.contentHashCode() ?: 0)
    return result
  }
}
