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

package com.facebook.rendercore

import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
import java.util.concurrent.ConcurrentHashMap

/**
 * Unique key for a [RenderUnit.Binder]. This must implement [Object.equals] and [Object.hashCode]
 * to ensure that it can be differentiated from other [BinderKey]s
 */
interface BinderKey {
  override fun equals(other: Any?): Boolean

  override fun hashCode(): Int
}

/** Default [BinderKey] backed by the class of the [RenderUnit.Binder] */
@DataClassGenerate
data class ClassBinderKey(private val clazz: Class<*>) : BinderKey {
  companion object {
    @JvmStatic var cache: ConcurrentHashMap<Class<*>, ClassBinderKey>? = null

    @JvmStatic
    fun of(clazz: Class<*>): ClassBinderKey {
      val c = cache ?: return ClassBinderKey(clazz)
      return c[clazz] ?: ClassBinderKey(clazz).also { c.putIfAbsent(clazz, it) }
    }
  }
}

@DataClassGenerate
/** [BinderKey] backed by an index typically used by fixed [RenderUnit.Binder]s */
internal data class IndexedBinderKey(val index: Int) : BinderKey
