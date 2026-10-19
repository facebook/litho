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

package com.facebook.rendercore.utils

val NoDeps = emptyArray<Any?>()

object CommonUtils {

  private val sectionNames = SectionNameCache(Class<*>::getSimpleName)

  /**
   * This API is used for tracing, and the section names have a char limit of 127. If the class name
   * exceeds that it will be replace by the simple name. In a release build the class name will be
   * minified, so it is unlikely to hit the limit. Computed once per class and held in a bounded
   * cache.
   */
  @JvmStatic fun getSectionNameForTracing(kclass: Class<*>): String = sectionNames[kclass]

  /**
   * This API is used for tracing. It's the same as getSectionNameForTracing(Class), but it uses
   * [poolKey] instead of the class name.
   */
  @JvmStatic
  fun getSectionNameForTracing(poolKey: Any): String {
    return "poolKey:$poolKey".take(127)
  }

  /** Utility to re-throw exceptions. */
  @JvmStatic
  fun rethrow(e: Exception) {
    if (e is RuntimeException) {
      throw e
    } else {
      throw RuntimeException(e)
    }
  }
}

/**
 * Section names by class, each computed once from [simpleNameOf]: `Class.getSimpleName` walks the
 * class's annotations and allocates on Android on every call.
 *
 * Only the [maxSize] most recently named classes are kept, so a long tail of one-off classes
 * (anonymous classes, lambdas) cannot grow the cache without bound.
 */
internal class SectionNameCache(
    private val simpleNameOf: (Class<*>) -> String,
    private val maxSize: Int = 512,
) {

  private val names =
      object : LinkedHashMap<Class<*>, String>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<Class<*>, String>): Boolean =
            size > maxSize
      }

  operator fun get(kclass: Class<*>): String =
      synchronized(names) {
        names.getOrPut(kclass) {
          val simpleName = simpleNameOf(kclass)
          if (simpleName.length < 5) "<cls>${kclass.name}</cls>" else simpleName
        }
      }
}
