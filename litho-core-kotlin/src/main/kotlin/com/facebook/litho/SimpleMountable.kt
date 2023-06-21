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
import com.facebook.rendercore.ContentAllocator
import com.facebook.rendercore.LayoutContext
import com.facebook.rendercore.MeasureResult
import com.facebook.rendercore.Mountable
import com.facebook.rendercore.RenderUnit
import com.facebook.rendercore.primitives.utils.isEqualOrEquivalentTo

/**
 * This is a simplified implementation of a [Mountable] which requires only one [Binder]. Must be
 * immutable, and not cause side effects.
 */
@Deprecated(
    "Mountable API is deprecated. Use Primitive API instead. Docs: https://fburl.com/staticdocs/j7w6qqyz; example component: https://fburl.com/code/9kigeb7a")
abstract class SimpleMountable<ContentT : Any>(renderType: RenderType) :
    Mountable<ContentT>(renderType), ContentAllocator<ContentT> {

  init {
    addOptionalMountBinder(
        DelegateBinder.createDelegateBinder(
            this, BINDER as Binder<SimpleMountable<ContentT>, ContentT, Any?>))
  }

  final override fun measure(
      context: LayoutContext<*>,
      widthSpec: Int,
      heightSpec: Int,
      previousLayoutData: Any?
  ): MeasureResult {
    val measureScope = MeasureScope(context, previousLayoutData)
    return measureScope.measure(widthSpec, heightSpec)
  }

  abstract fun MeasureScope.measure(widthSpec: Int, heightSpec: Int): MeasureResult

  /**
   * Called just before mounting the content. Use it to set properties on the content. This method
   * is always called from the main thread.
   */
  abstract fun mount(c: Context, content: ContentT, layoutData: Any?)

  /**
   * Called just after unmounting the content. Use it to unset properties on the content. This
   * method is always called from the main thread.
   */
  abstract fun unmount(c: Context, content: ContentT, layoutData: Any?)

  /**
   * Called to check if properties need to be reset. This is expected to be done by invoking
   * [unmount] and then [mount] if this function returns true.
   */
  fun shouldUpdate(
      currentMountable: SimpleMountable<ContentT>,
      newMountable: SimpleMountable<ContentT>,
      currentLayoutData: Any?,
      nextLayoutData: Any?
  ): Boolean = shouldUpdate(newMountable, currentLayoutData, nextLayoutData)

  open fun shouldUpdate(
      newMountable: SimpleMountable<ContentT>,
      currentLayoutData: Any?,
      nextLayoutData: Any?
  ): Boolean = currentLayoutData != nextLayoutData || !isEqualOrEquivalentTo(this, newMountable)

  override fun getContentAllocator(): ContentAllocator<ContentT> {
    return this
  }
}

private val BINDER: RenderUnit.Binder<*, *, *> =
    object : RenderUnit.Binder<SimpleMountable<Any>, Any, Any?> {
      override fun shouldUpdate(
          currentMountable: SimpleMountable<Any>,
          newMountable: SimpleMountable<Any>,
          currentLayoutData: Any?,
          nextLayoutData: Any?
      ): Boolean {
        return currentMountable.shouldUpdate(
            currentMountable,
            newMountable,
            currentLayoutData,
            nextLayoutData,
        )
      }

      override fun bind(
          context: Context,
          content: Any,
          mountable: SimpleMountable<Any>,
          layoutData: Any?
      ): Any? {
        mountable.mount(context, content, layoutData)
        return null
      }

      override fun unbind(
          context: Context,
          content: Any,
          mountable: SimpleMountable<Any>,
          layoutData: Any?,
          bindData: Any?
      ) {
        mountable.unmount(context, content, layoutData)
      }
    }
