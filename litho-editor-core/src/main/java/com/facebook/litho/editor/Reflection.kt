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

package com.facebook.litho.editor

import java.lang.reflect.AccessibleObject
import java.lang.reflect.Field
import java.lang.reflect.Method

object Reflection {
  private val cache = mutableMapOf<String, AccessibleObject>()

  private inline fun <reified T> fromCache(key: String): T? = cache[key] as? T

  private fun getCacheKey(obj: Any, name: String, vararg types: Class<*>) =
      StringBuilder()
          .apply {
            val klass = obj as? Class<*> ?: obj::class.java
            append(klass.name).append('#').append(name)
            if (types.isNotEmpty()) {
              append('(')
              var separator = ""
              types.forEach {
                append(separator).append(it.name)
                separator = ","
              }
              append(')')
            }
          }
          .toString()

  fun getMethod(obj: Any?, name: String, vararg types: Class<*>): Method? {
    if (obj == null) return null
    val key = getCacheKey(obj, name, *types)
    return fromCache(key)
        ?: run {
          var klass: Class<*>? = obj as? Class<*> ?: obj.javaClass
          while (klass != null) {
            try {
              klass.getDeclaredMethod(name, *types).let { method ->
                if (!method.isAccessible) method.isAccessible = true
                cache[key] = method
                return method
              }
            } catch (ignored: NoSuchMethodException) {}
            klass = klass.superclass
          }
          null
        }
  }

  inline fun <reified T> invoke(
      obj: Any?,
      name: String,
      types: Array<Class<*>> = emptyArray(),
      vararg args: Any
  ): T? =
      try {
        getMethod(obj, name, *types)?.run { invoke(obj, *args) as? T }
      } catch (e: Exception) {
        null
      }

  fun <T> getValueUNSAFE(f: Field, node: Any?): T? {
    return try {
      val oldAccessibility = f.isAccessible
      f.isAccessible = true
      var value: T? = null
      f[node]?.let { v -> value = v as T }

      f.isAccessible = oldAccessibility
      value
    } catch (e: IllegalArgumentException) {
      throw RuntimeException(e)
    } catch (e: IllegalAccessException) {
      throw RuntimeException(e)
    }
  }

  fun <T> setValueUNSAFE(f: Field, node: Any?, value: T) {
    try {
      val oldAccessibility = f.isAccessible
      f.isAccessible = true
      f[node] = value
      f.isAccessible = oldAccessibility
    } catch (e: IllegalArgumentException) {
      throw RuntimeException(e)
    } catch (e: IllegalAccessException) {
      throw RuntimeException(e)
    } catch (e: SecurityException) {
      throw RuntimeException(e)
    }
  }

  fun geFieldUNSAFE(clazz: Class<*>, field: String): Field? {
    return try {
      clazz.getDeclaredField(field)
    } catch (e: SecurityException) {
      throw RuntimeException(e)
    } catch (e: NoSuchFieldException) {
      throw RuntimeException(e)
    }
  }
}
