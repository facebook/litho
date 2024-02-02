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

class MountItem(renderTreeNode: RenderTreeNode, val content: Any) {

  private var _renderTreeNode: RenderTreeNode = renderTreeNode

  var host: Host? = null

  var isBound: Boolean = false

  @Deprecated("Use BindData API instead") var mountData: Any? = null

  val bindData: BindData = BindData()

  val renderUnit: RenderUnit<*>
    get() = _renderTreeNode.renderUnit

  val renderTreeNode: RenderTreeNode
    get() = _renderTreeNode

  fun update(renderTreeNode: RenderTreeNode) {
    _renderTreeNode = renderTreeNode
  }

  fun releaseMountContent(context: Context) {
    MountItemsPool.release(context, renderUnit.contentAllocator, content)
  }

  companion object {
    @JvmStatic fun getId(item: MountItem): Long = item._renderTreeNode.renderUnit.getId()
  }
}
