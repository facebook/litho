/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

/**
 * Marker for the Litho container component DSL. This makes sure that when we have scope-specific
 * functions like `child()` to add children, they can only apply to the closest receiver annotated
 * with ContainerDsl.
 *
 * See https://kotlinlang.org/docs/type-safe-builders.html#scope-control-dslmarker
 */
@DslMarker annotation class ContainerDsl
