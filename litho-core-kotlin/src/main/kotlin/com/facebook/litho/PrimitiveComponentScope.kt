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

package com.facebook.litho

import com.facebook.rendercore.ContentAllocator
import com.facebook.rendercore.primitives.MountBehavior as PrimitiveMountBehavior
import com.facebook.rendercore.primitives.MountConfigurationScope

/** The implicit receiver for [PrimitiveComponent.render] call. */
class PrimitiveComponentScope
internal constructor(context: ComponentContext, resolveStateContext: ResolveStateContext) :
    ComponentScope(context, resolveStateContext) {

  /**
   * Indicates whether the component skips Incremental Mount. If this is true then the Component
   * will not be involved in Incremental Mount.
   */
  var shouldExcludeFromIncrementalMount: Boolean = false

  /** Creates a [com.facebook.rendercore.primitives.MountBehavior1] with a unique id. */
  @Suppress("FunctionName", "NOTHING_TO_INLINE")
  inline fun <ContentType : Any> MountBehavior(
      contentAllocator: ContentAllocator<ContentType>,
      noinline mountConfigurationCall: MountConfigurationScope<ContentType>.() -> Unit
  ): PrimitiveMountBehavior<ContentType> {
    return PrimitiveMountBehavior(id = createId(), contentAllocator, mountConfigurationCall)
  }

  /** Creates a unique ID for a given component. */
  @PublishedApi
  internal fun createId(): Long {
    // TODO(zielinskim): calculateLayoutOutputId is mutated during resolve/layout and it may race.
    // Ideally, we'd like to replace this hacky solution with something else.
    return context.renderUnitIdGenerator?.calculateLayoutOutputId(
        context.globalKey, OutputUnitType.CONTENT)
        ?: throw IllegalStateException("Attempt to use a released RenderStateContext")
  }
}
