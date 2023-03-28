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

internal fun LithoLayoutResult.getMountedContent(): Any? {
  val view = context.mountedView as? LithoView
  val mountDelegateTarget = view?.mountDelegateTarget
  if (mountDelegateTarget != null) {
    val count = mountDelegateTarget.mountItemCount
    for (i in 0 until count) {
      val mountItem = mountDelegateTarget.getMountItemAt(i)
      val component = mountItem?.let { LithoRenderUnit.getRenderUnit(it).component } ?: continue
      if (component === node.tailComponent) return mountItem.content
    }
  }
  return null
}
