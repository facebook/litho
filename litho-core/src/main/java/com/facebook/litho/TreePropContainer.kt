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
import com.facebook.litho.config.ComponentsConfiguration
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

  private val map = Collections.synchronizedMap(HashMap<Class<*>, Any?>())
  private val objectMap = Collections.synchronizedMap(HashMap<TreeProp<*>, Any?>())
  private val isObjectTreePropEnabled: Boolean = ComponentsConfiguration.isObjectTreePropEnabled

  fun put(key: Class<*>, value: Any?) {
    if (isObjectTreePropEnabled) {
      val treeProp = createLegacyTreeProp(key)
      objectMap[treeProp] = value
    } else {
      map[key] = value
    }
  }

  operator fun <T> get(key: Class<T>): T? {
    return if (isObjectTreePropEnabled) {
      val treeProp = createLegacyTreeProp(key)
      objectMap[treeProp] as T?
    } else {
      map[key] as T?
    }
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

  /**
   * This returns a copy of the content stored by the [TreePropContainer] in the raw format to which
   * it is stored.
   *
   * This is only meant to be used by the internal APIs.
   */
  @JvmName("asRawMap") internal fun asRawMap(): Map<Class<*>, Any?> = map.toMap()

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
