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

import android.view.View
import com.facebook.litho.viewcompat.ViewBinder
import com.facebook.litho.viewcompat.ViewCreator

/** [RenderInfo] that can render views. */
class ViewRenderInfo private constructor(builder: Builder) : BaseRenderInfo(builder) {
  override val viewBinder: ViewBinder<View>
  override val viewCreator: ViewCreator<View>

  private val hasCustomViewType: Boolean

  override var viewType: Int = 0
    set(viewType) {
      if (hasCustomViewType) {
        throw UnsupportedOperationException("Cannot override custom view type.")
      }
      field = viewType
    }

  init {
    viewBinder = checkNotNull(builder.viewBinder) { "viewBinder must be provided" }
    viewCreator = checkNotNull(builder.viewCreator) { "viewCreator must be provided" }
    if (builder.hasCustomViewType) {
      viewType = builder.viewType
    }
    hasCustomViewType = builder.hasCustomViewType
  }

  override fun rendersView(): Boolean = true

  override fun hasCustomViewType(): Boolean = hasCustomViewType

  override val name: String = "View (viewType=$viewType)"

  class Builder : BaseRenderInfo.Builder<Builder>() {
    internal var viewBinder: ViewBinder<View>? = null
    internal var viewCreator: ViewCreator<View>? = null
    internal var hasCustomViewType: Boolean = false
    internal var viewType: Int = 0

    /**
     * Specify [ViewCreator] implementation that can be used to create a new view if such view is
     * absent in recycling cache. For the same type of views same [ViewCreator] instance should be
     * provided.
     */
    @Suppress("UNCHECKED_CAST")
    fun viewCreator(viewCreator: ViewCreator<*>): Builder = also {
      this.viewCreator = viewCreator as ViewCreator<View>
    }

    /**
     * Specify [ViewBinder] implementation that can bind model to the view provided from
     * [.viewCreator].
     */
    @Suppress("UNCHECKED_CAST")
    fun viewBinder(viewBinder: ViewBinder<*>): Builder = also {
      this.viewBinder = viewBinder as ViewBinder<View>
    }

    /**
     * Specify a custom ViewType identifier for this View. This will be used instead of being
     * generated from the [ViewCreator] instance.
     */
    fun customViewType(viewType: Int): Builder = also {
      this.hasCustomViewType = true
      this.viewType = viewType
    }

    fun build(): ViewRenderInfo {
      check(this.viewCreator != null && this.viewBinder != null) {
        "Both viewCreator and viewBinder must be provided."
      }

      return ViewRenderInfo(this)
    }

    override fun isFullSpan(isFullSpan: Boolean): Builder {
      throw UnsupportedOperationException("ViewRenderInfo does not support isFullSpan.")
    }
  }

  companion object {
    @JvmStatic fun create(): Builder = Builder()
  }
}
