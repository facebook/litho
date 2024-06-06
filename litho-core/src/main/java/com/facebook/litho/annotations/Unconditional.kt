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
 * Marker annotation for methods that should be called unconditionally.
 *
 * This annotation can be applied directly on methods. It can also serve as a meta annotation for
 * other annotations to indicate that they should be treated as unconditional.
 *
 * Unconditional methods follow 2 clear rules:
 * - They must be called unconditionally. i.e it is not permitted to call them inside if, when, for
 *   or forEach expressions
 * - They may only be called within other Unconditional annotated methods.
 *
 * The reason for these rules is to preserve the identity of such methods and as such, ensure
 * correctness and consistency across multiple invocations.
 *
 * The motivation here is very similar to that of [Hook]s. In fact, hooks are a kind of
 * Unconditional, though the vice versa is not necessarily true.
 *
 * **Note**: At the moment, it is not possible to call Unconditional methods directly from a
 * render(), hence, they can **only** be called from other unconditional methods (including Hooks,
 * even though hooks don't actually have such restriction). This restriction may be lifted in the
 * future.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
annotation class Unconditional
