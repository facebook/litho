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

import com.facebook.litho.Component
import com.facebook.litho.ComponentTreeDebugEventListener
import com.facebook.litho.ComponentsLogger
import com.facebook.litho.EmptyComponent
import com.facebook.litho.EventHandler
import com.facebook.litho.RenderCompleteEvent

/** [RenderInfo] that can render components. */
class ComponentRenderInfo private constructor(builder: Builder) : BaseRenderInfo(builder) {

  override val component: Component =
      checkNotNull(builder.component) { "Component must be provided." }
  override val renderCompleteEventHandler: EventHandler<RenderCompleteEvent>? =
      builder.renderCompleteEventEventHandler
  override val componentsLogger: ComponentsLogger? = builder.componentsLogger
  override val logTag: String? = builder.logTag
  override val debugEventListener: ComponentTreeDebugEventListener? = builder.debugEventListener

  override fun rendersComponent(): Boolean = true

  override val name: String
    get() = component.simpleName

  class Builder : BaseRenderInfo.Builder<Builder>() {
    var component: Component? = null
    var renderCompleteEventEventHandler: EventHandler<RenderCompleteEvent>? = null
    var componentsLogger: ComponentsLogger? = null
    var logTag: String? = null
    var debugEventListener: ComponentTreeDebugEventListener? = null

    /** Specify [Component] that will be rendered as an item of the list. */
    fun component(component: Component?): Builder = also { this.component = component }

    fun renderCompleteHandler(
        renderCompleteEventHandler: EventHandler<RenderCompleteEvent>?
    ): Builder = also { this.renderCompleteEventEventHandler = renderCompleteEventHandler }

    fun component(builder: Component.Builder<*>): Builder {
      return component(builder.build())
    }

    fun componentsLogger(componentsLogger: ComponentsLogger?): Builder = also {
      this.componentsLogger = componentsLogger
    }

    fun logTag(logTag: String?): Builder = also { this.logTag = logTag }

    fun debugEventListener(debugEventListener: ComponentTreeDebugEventListener?): Builder = also {
      this.debugEventListener = debugEventListener
    }

    fun build(): ComponentRenderInfo {
      return ComponentRenderInfo(this)
    }
  }

  companion object {
    @JvmStatic
    fun create(): Builder {
      return Builder()
    }

    /** Create empty [ComponentRenderInfo]. */
    @JvmStatic
    fun createEmpty(): RenderInfo {
      return create().component(EmptyComponent()).build()
    }
  }
}
