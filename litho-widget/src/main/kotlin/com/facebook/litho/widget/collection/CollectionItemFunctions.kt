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

package com.facebook.litho.widget.collection

/**
 * Predicate for collection item metadata. Unlike `(T) -> Boolean`, this compiles to a primitive
 * `boolean` return and avoids boxing when invoked for every item.
 *
 * The generic [T] parameter still erases to `Object`, so this interface should only be used when
 * [T] is a reference type. That is true for LazyGrid and LazyList data models, so the input
 * parameter does not box here; only the primitive return type needed specialization.
 */
fun interface Predicate<in T> {
  fun test(item: T): Boolean
}

/**
 * Maps collection item metadata to an Int. Unlike `(T) -> Int`, this compiles to a primitive `int`
 * return and avoids boxing when invoked for every item.
 *
 * The generic [T] parameter still erases to `Object`, so this interface should only be used when
 * [T] is a reference type. That is true for LazyGrid and LazyList data models, so the input
 * parameter does not box here; only the primitive return type needed specialization.
 */
fun interface IntMapper<in T> {
  fun map(item: T): Int
}

/**
 * Maps collection item metadata to a Float. Unlike `(T) -> Float`, this compiles to a primitive
 * `float` return and avoids boxing when invoked for every item.
 *
 * The generic [T] parameter still erases to `Object`, so this interface should only be used when
 * [T] is a reference type. That is true for LazyGrid and LazyList data models, so the input
 * parameter does not box here; only the primitive return type needed specialization.
 */
fun interface FloatMapper<in T> {
  fun map(item: T): Float
}

/**
 * Predicate for collection item metadata with index. Unlike `(Int, T) -> Boolean`, this compiles to
 * a primitive `boolean` return and avoids boxing when invoked for every item.
 *
 * Used by [childrenIndexed] APIs to allow index-aware predicates without manual forEachIndexed
 * loops — see R27.
 */
fun interface IndexedPredicate<in T> {
  fun test(index: Int, item: T): Boolean
}

/**
 * Maps collection item metadata with index to an Int. Unlike `(Int, T) -> Int`, this compiles to a
 * primitive `int` return and avoids boxing when invoked for every item.
 */
fun interface IndexedIntMapper<in T> {
  fun map(index: Int, item: T): Int
}

/**
 * Maps collection item metadata with index to a Float. Unlike `(Int, T) -> Float`, this compiles to
 * a primitive `float` return and avoids boxing when invoked for every item.
 */
fun interface IndexedFloatMapper<in T> {
  fun map(index: Int, item: T): Float
}
