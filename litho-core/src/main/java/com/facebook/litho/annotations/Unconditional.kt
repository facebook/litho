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

package com.facebook.litho.annotations

/**
 * Marker for methods whose identity is their call order, so callers must not vary that order.
 *
 * Two rules, both enforced by `KComponentUnconditionalMisuseDetector`:
 * - They must be called unconditionally: not inside `if`, `when`, `for`, `while` or `forEach`.
 * - They may only be called from another `@Unconditional` method, which is what keeps the first
 *   rule from being escaped one frame up.
 *
 * `ComponentScope.diffOf` is the case this exists for. It appends to a per-render list and reads
 * the previous render's value back by that position, so a call that runs on some renders and not
 * others silently pairs a value with the wrong predecessor.
 *
 * Hooks used to work this way and no longer do: the compiler plugin gives each hook call site a
 * structural identity, so [Hook] does not carry this annotation and hooks may be called
 * conditionally. Do not reach for `@Unconditional` on anything hook-shaped.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
annotation class Unconditional
