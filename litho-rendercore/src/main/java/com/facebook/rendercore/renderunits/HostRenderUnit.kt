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

package com.facebook.rendercore.renderunits

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.view.View.OnFocusChangeListener
import androidx.annotation.IntDef
import com.facebook.rendercore.ContentAllocator
import com.facebook.rendercore.HostView
import com.facebook.rendercore.RenderType
import com.facebook.rendercore.RenderUnit
import com.facebook.rendercore.RenderUnit.DelegateBinder.Companion.createDelegateBinder

open class HostRenderUnit(override val id: Long) :
    RenderUnit<HostView>(), ContentAllocator<HostView> {

  override val renderType: RenderType = RenderType.VIEW

  @IntDef(value = [UNSET, SET_FALSE, SET_TRUE])
  @Retention(AnnotationRetention.SOURCE)
  internal annotation class TriState

  var background: Drawable? = null
  var foreground: Drawable? = null
  var layerType: Int = View.LAYER_TYPE_NONE

  @get:TriState
  @TriState
  var clickable: Int = UNSET
    private set

  var isEnabled: Boolean = true
  var isFocusable: Boolean = false
  var isFocusableInTouchMode: Boolean = false
  var onFocusChangeListener: OnFocusChangeListener? = null
  var onClickListener: View.OnClickListener? = null

  override fun createContent(context: Context): HostView {
    return HostView(context)
  }

  override val contentAllocator: ContentAllocator<HostView>
    get() = this

  fun setClickable(clickable: Boolean) {
    this.clickable = if (clickable) SET_TRUE else SET_FALSE
  }

  init {
    addOptionalMountBinders(
        createDelegateBinder(this, backgroundBindFunction),
        createDelegateBinder(this, foregroundBindFunction),
        createDelegateBinder(this, touchHandlersBindFunction),
        createDelegateBinder(this, focusabilityBindFunction),
        createDelegateBinder(this, layerTypeBindFunction),
    )
  }

  companion object {
    private const val UNSET = -1
    private const val SET_FALSE = 0
    private const val SET_TRUE = 1
    val backgroundBindFunction: Binder<HostRenderUnit, HostView, Any> =
        object : Binder<HostRenderUnit, HostView, Any> {
          override fun shouldUpdate(
              currentModel: HostRenderUnit,
              newModel: HostRenderUnit,
              currentLayoutData: Any?,
              nextLayoutData: Any?,
          ): Boolean {
            val currentBackground = currentModel.background
            val newBackground = newModel.background
            return if (currentBackground == null) {
              newBackground != null
            } else {
              currentBackground != newBackground
            }
          }

          override fun bind(
              context: Context,
              content: HostView,
              model: HostRenderUnit,
              layoutData: Any?,
          ): Any? {
            content.background = model.background
            return null
          }

          override fun unbind(
              context: Context,
              content: HostView,
              model: HostRenderUnit,
              layoutData: Any?,
              bindData: Any?,
          ) {
            content.background = null
          }
        }
    val foregroundBindFunction: Binder<HostRenderUnit, HostView, Any> =
        object : Binder<HostRenderUnit, HostView, Any> {
          override fun shouldUpdate(
              currentModel: HostRenderUnit,
              newModel: HostRenderUnit,
              currentLayoutData: Any?,
              nextLayoutData: Any?,
          ): Boolean {
            val currentForeground = currentModel.foreground
            val newForeground = newModel.foreground
            return if (currentForeground == null) {
              newForeground != null
            } else {
              newForeground != null && currentForeground != newForeground
            }
          }

          override fun bind(
              context: Context,
              content: HostView,
              model: HostRenderUnit,
              layoutData: Any?,
          ): Any? {
            content.setForegroundCompat(model.foreground)
            return null
          }

          override fun unbind(
              context: Context,
              content: HostView,
              model: HostRenderUnit,
              layoutData: Any?,
              bindData: Any?,
          ) {
            content.setForegroundCompat(null)
          }
        }
    val layerTypeBindFunction: Binder<HostRenderUnit, HostView, Any> =
        object : Binder<HostRenderUnit, HostView, Any> {
          override fun shouldUpdate(
              currentModel: HostRenderUnit,
              newModel: HostRenderUnit,
              currentLayoutData: Any?,
              nextLayoutData: Any?,
          ): Boolean {
            return currentModel.layerType != newModel.layerType
          }

          override fun bind(
              context: Context,
              content: HostView,
              model: HostRenderUnit,
              layoutData: Any?,
          ): Any? {
            content.setLayerType(model.layerType, null)
            return null
          }

          override fun unbind(
              context: Context,
              content: HostView,
              model: HostRenderUnit,
              layoutData: Any?,
              bindData: Any?,
          ) {
            content.setLayerType(View.LAYER_TYPE_NONE, null)
          }
        }
    val touchHandlersBindFunction: Binder<HostRenderUnit, HostView, Any> =
        object : Binder<HostRenderUnit, HostView, Any> {
          override fun shouldUpdate(
              currentModel: HostRenderUnit,
              newModel: HostRenderUnit,
              currentLayoutData: Any?,
              nextLayoutData: Any?,
          ): Boolean {
            // Updating touch and click listeners is not an expensive operation.
            return true
          }

          override fun bind(
              context: Context,
              content: HostView,
              model: HostRenderUnit,
              layoutData: Any?,
          ): Any? {
            val onClickListener = model.onClickListener
            if (onClickListener != null) {
              content.setOnClickListener(onClickListener)
            }
            val onFocusChangeListener = model.onFocusChangeListener
            content.onFocusChangeListener = onFocusChangeListener
            content.isEnabled = model.isEnabled
            @TriState val clickable = model.clickable
            if (clickable != UNSET) {
              content.isClickable = clickable == SET_TRUE
            }
            return null
          }

          override fun unbind(
              context: Context,
              content: HostView,
              model: HostRenderUnit,
              layoutData: Any?,
              bindData: Any?,
          ) {
            content.setOnClickListener(null)
            content.isClickable = false
            content.onFocusChangeListener = null
          }
        }
    val focusabilityBindFunction: Binder<HostRenderUnit, HostView, Any> =
        object : Binder<HostRenderUnit, HostView, Any> {
          override fun shouldUpdate(
              currentModel: HostRenderUnit,
              newModel: HostRenderUnit,
              currentLayoutData: Any?,
              nextLayoutData: Any?,
          ): Boolean {
            return currentModel.isFocusable != newModel.isFocusable ||
                currentModel.isFocusableInTouchMode != newModel.isFocusableInTouchMode
          }

          override fun bind(
              context: Context,
              content: HostView,
              model: HostRenderUnit,
              layoutData: Any?,
          ): Any? {
            content.isFocusable = model.isFocusable
            content.isFocusableInTouchMode = model.isFocusableInTouchMode
            return null
          }

          override fun unbind(
              context: Context,
              content: HostView,
              model: HostRenderUnit,
              layoutData: Any?,
              bindData: Any?,
          ) {
            content.isFocusable = false
            content.isFocusableInTouchMode = false
          }
        }
  }
}
