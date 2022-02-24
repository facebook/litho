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

import android.content.Context
import com.facebook.rendercore.RenderUnit

/** Experimental. Currently for Litho team internal use only. */
abstract class SimpleMountable<ContentT> : Mountable<ContentT> {

  abstract fun mount(c: Context?, content: ContentT, layoutData: Any?)

  abstract fun unmount(c: Context?, content: ContentT, layoutData: Any?)

  abstract fun shouldUpdate(
      currentMountable: SimpleMountable<ContentT>,
      newMountable: SimpleMountable<ContentT>,
      currentLayoutData: Any?,
      nextLayoutData: Any?
  ): Boolean

  override fun getBinders(): List<RenderUnit.Binder<*, ContentT>> =
      BINDER_LIST as List<RenderUnit.Binder<*, ContentT>>
}

private val BINDER_LIST: List<RenderUnit.Binder<*, *>> =
    listOf(
        object : RenderUnit.Binder<SimpleMountable<Any>, Any> {
          override fun shouldUpdate(
              currentMountable: SimpleMountable<Any>,
              newMountable: SimpleMountable<Any>,
              currentLayoutData: Any?,
              nextLayoutData: Any?
          ): Boolean {
            currentLayoutData as LithoLayoutData
            nextLayoutData as LithoLayoutData
            return newMountable.shouldUpdate(
                currentMountable,
                newMountable,
                currentLayoutData.mLayoutData,
                nextLayoutData.mLayoutData)
          }

          override fun bind(
              context: Context?,
              content: Any,
              mountable: SimpleMountable<Any>,
              layoutData: Any?
          ) {
            layoutData as LithoLayoutData
            mountable.mount(context, content, layoutData.mLayoutData)
          }

          override fun unbind(
              context: Context,
              content: Any,
              mountable: SimpleMountable<Any>,
              layoutData: Any?
          ) {
            layoutData as LithoLayoutData
            mountable.unmount(context, content, layoutData.mLayoutData)
          }
        })
