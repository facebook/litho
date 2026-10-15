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

import android.content.Context

open class MountItem(renderTreeNode: RenderTreeNode, val content: Any) {

  private var _renderTreeNode: RenderTreeNode = renderTreeNode

  internal val registry: MountStateRestartRegistry = MountStateRestartRegistry()

  var host: Host? = null

  var isBound: Boolean = false

  val bindData: BindData = BindData()

  val renderUnit: RenderUnit<*>
    get() = _renderTreeNode.renderUnit

  val renderTreeNode: RenderTreeNode
    get() = _renderTreeNode

  fun update(renderTreeNode: RenderTreeNode) {
    _renderTreeNode = renderTreeNode
  }

  fun releaseMountContent(context: Context) {
    renderUnit.contentAllocator.recycleContent(context, content, renderTreeNode.poolScope)
  }

  internal fun restartContinuations(context: MountContinuationContext) {
    if (!hasAnyContinuations()) {
      return
    }
    RenderCoreSystrace.beginSection(
        "restartContinuations [${renderUnit.description} / ${renderUnit.id}]",
    )
    renderUnit.restartContinuations(context)
    registry.restartMountStates()
    RenderCoreSystrace.endSection()
  }

  internal fun hasAnyContinuations(): Boolean {
    return renderUnit.hasAnyContinuations() || registry.hasAnyMountStates()
  }

  /**
   * Like [hasAnyContinuations] but consults whether this render unit's continuations have a new
   * value to re-run against ([RenderUnit.hasNewContinuationValues]) or any nested mount state has
   * pending restart work, so clean restart registrations can be skipped.
   */
  internal fun hasPendingRestartWork(context: MountContinuationContext): Boolean {
    return renderUnit.hasNewContinuationValues(context) ||
        registry.anyMountStateHasPendingRestartWork()
  }

  internal fun clearContinuations() {
    registry.clear()
  }

  companion object {
    @JvmStatic fun getId(item: MountItem): Long = item._renderTreeNode.renderUnit.id
  }
}
