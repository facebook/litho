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

package com.facebook.litho.widget

import com.facebook.litho.ComponentContext

/**
 * An implementation of [RecyclerBinderUpdateCallback.OperationExecutor] that uses [RecyclerBinder].
 */
class RecyclerBinderOperationExecutor(private val recyclerBinder: RecyclerBinder) :
    RecyclerBinderUpdateCallback.OperationExecutor {

  override fun executeOperations(
      context: ComponentContext,
      operations: List<RecyclerBinderUpdateCallback.Operation>
  ) {

    for (operation in operations) {
      val components: List<RecyclerBinderUpdateCallback.ComponentContainer>? =
          operation.componentContainers
      var renderInfos: MutableList<RenderInfo>? = null
      if (components != null && components.size > 1) {
        renderInfos = ArrayList()
        for (component in components) {
          renderInfos.add(component.renderInfo)
        }
      }

      when (operation.type) {
        RecyclerBinderUpdateCallback.Operation.INSERT ->
            if (renderInfos != null) {
              recyclerBinder.insertRangeAt(operation.index, renderInfos)
            } else {
              recyclerBinder.insertItemAt(
                  operation.index, operation.componentContainers[0].renderInfo)
            }
        RecyclerBinderUpdateCallback.Operation.DELETE ->
            recyclerBinder.removeRangeAt(operation.index, operation.toIndex)
        RecyclerBinderUpdateCallback.Operation.MOVE ->
            recyclerBinder.moveItem(operation.index, operation.toIndex)
        RecyclerBinderUpdateCallback.Operation.UPDATE ->
            if (renderInfos != null) {
              recyclerBinder.updateRangeAt(operation.index, renderInfos)
            } else {
              recyclerBinder.updateItemAt(
                  operation.index, operation.componentContainers[0].renderInfo)
            }
      }
    }

    recyclerBinder.notifyChangeSetComplete(
        true,
        object : ChangeSetCompleteCallback {
          override fun onDataBound() {
            // Do nothing.
          }

          override fun onDataRendered(isMounted: Boolean, uptimeMillis: Long) {
            // Do nothing.
          }
        })
  }
}
