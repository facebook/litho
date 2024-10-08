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

@file:JvmName("TreeProps")

package com.facebook.litho

import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate

interface TreeProp<T> {
  val defaultValue: T
}

@DataClassGenerate
private data class ClassBasedTreeProp<T : Any>(val clazz: Class<T>) : TreeProp<T?> {
  override val defaultValue: T? = null
}

private class ObjectBasedTreeProp<T>(private val defaultValueProducer: () -> T) : TreeProp<T> {
  override val defaultValue: T by lazy { defaultValueProducer() }
}

fun <T : Any> legacyTreePropOf(clazz: Class<T>): TreeProp<T?> {
  return ClassBasedTreeProp(clazz)
}

inline fun <reified T : Any> legacyTreePropOf(): TreeProp<T?> {
  return legacyTreePropOf(T::class.java)
}

fun <T> treePropOf(defaultValueProducer: () -> T): TreeProp<T> {
  return ObjectBasedTreeProp(defaultValueProducer)
}
