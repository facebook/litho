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

/**
 * A unique identifier for a [RenderUnit.Binder] within a [RenderTree].
 *
 * @param renderUnitId The id of the [RenderUnit] that owns the binder
 * @param type The type of the [RenderUnit.Binder]. Note that both fixed and optional binders will
 *   have the same [BinderType.MOUNT].
 * @param key The key of the [RenderUnit.Binder] within a [RenderUnit]. The combination of [key] and
 *   [type] for any given binder is always unique within its [RenderUnit].
 */
@DataClassGenerate
data class BinderId(val renderUnitId: Long, val type: BinderType, val key: BinderKey)

/**
 * The type of a [RenderUnit.Binder]. This is used to distinguish between different kinds of
 * binders.
 */
enum class BinderType {
  MOUNT,
  ATTACH
}
