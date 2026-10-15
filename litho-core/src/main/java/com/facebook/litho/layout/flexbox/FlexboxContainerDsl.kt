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

package com.facebook.litho.layout.flexbox

/**
 * Marker for the context-free flexbox container DSL ([Row] and [Column] in this package). This
 * makes sure that scope-specific functions like [FlexboxScope.child] can only apply to the closest
 * receiver annotated with [FlexboxContainerDsl].
 *
 * Distinct from `com.facebook.litho.ContainerDsl` so the two DSLs do not interfere: the legacy
 * `ResourcesScope.Row { }` / `Column { }` DSL uses `@ContainerDsl` (which subtypes
 * `ResourcesScope`), while this DSL applies to [FlexboxScope] (which does NOT extend
 * `ResourcesScope`) so that outer `ResourcesScope` extensions remain reachable inside the children
 * block.
 *
 * See https://kotlinlang.org/docs/type-safe-builders.html#scope-control-dslmarker
 */
@DslMarker annotation class FlexboxContainerDsl
