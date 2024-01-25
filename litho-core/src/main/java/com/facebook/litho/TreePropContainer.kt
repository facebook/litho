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

import com.facebook.infer.annotation.ThreadConfined
import com.facebook.infer.annotation.ThreadSafe
import java.util.Collections
import java.util.HashMap
import java.util.Objects

/**
 * A data structure to store tree props.
 *
 * @see TreeProp
 */
@ThreadConfined(ThreadConfined.ANY)
class TreePropContainer {

  private val map = Collections.synchronizedMap(HashMap<TreeProp<*>, Any?>())

  fun put(key: Class<*>, value: Any?) {
    val treeProp = legacyTreePropOf(key)
    map[treeProp] = value
  }

  fun <T> put(treeProp: TreeProp<out T>, value: T) {
    map[treeProp] = value
  }

  operator fun <T : Any> get(key: Class<T>): T? {
    val treeProp = legacyTreePropOf(key)
    return map[treeProp] as T?
  }

  operator fun <T> get(prop: TreeProp<T>): T {
    if (map.containsKey(prop)) {
      return map[prop] as T
    }
    return prop.defaultValue
  }

  operator fun set(key: Class<*>, value: Any?) = put(key, value)

  fun reset() {
    map.clear()
  }

  override fun equals(o: Any?): Boolean {
    if (this === o) {
      return true
    }

    if (o !is TreePropContainer) return false

    return map == o.map
  }

  override fun hashCode(): Int = Objects.hash(map)

  companion object {
    /** @return a copy of the provided TreeProps instance; returns null if source is null */
    @JvmStatic
    @ThreadSafe(enableChecks = false)
    fun copy(source: TreePropContainer?): TreePropContainer? =
        if (source == null) {
          null
        } else {
          acquire(source)
        }

    /**
     * Whenever a Spec sets tree props, the TreeProps map from the parent is copied. If parent
     * TreeProps are null, a new TreeProps instance is created to copy the current tree props.
     *
     * Infer knows that newProps is owned but doesn't know that newProps.mMap is owned.
     */
    @JvmStatic
    @ThreadSafe(enableChecks = false)
    fun acquire(source: TreePropContainer?): TreePropContainer {
      val newProps = TreePropContainer()
      if (source != null) {
        synchronized(source.map) { newProps.map.putAll(source.map) }
      }
      return newProps
    }
  }
}
