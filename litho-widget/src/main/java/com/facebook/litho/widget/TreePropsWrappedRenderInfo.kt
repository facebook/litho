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
import com.facebook.litho.EventHandler
import com.facebook.litho.RenderCompleteEvent
import com.facebook.litho.TreePropContainer
import com.facebook.litho.viewcompat.ViewBinder
import com.facebook.litho.viewcompat.ViewCreator
import com.facebook.litho.widget.ComponentRenderInfo.Companion.createEmpty

/** A wrapper around [RenderInfo] that also stores TreeProps used for rendering the component. */
class TreePropsWrappedRenderInfo(
    renderInfo: RenderInfo?,
    val treePropContainer: TreePropContainer?
) : RenderInfo {

  private val renderInfo = renderInfo ?: createEmpty()

  override val component: Component
    get() = renderInfo.component

  override val renderCompleteEventHandler: EventHandler<RenderCompleteEvent>?
    get() = renderInfo.renderCompleteEventHandler

  override fun rendersComponent(): Boolean {
    return renderInfo.rendersComponent()
  }

  override val componentsLogger: ComponentsLogger?
    get() = renderInfo.componentsLogger

  override val debugEventListener: ComponentTreeDebugEventListener?
    get() = renderInfo.debugEventListener

  override val logTag: String?
    get() = renderInfo.logTag

  override val name: String
    get() = renderInfo.name

  override val isSticky: Boolean
    get() = renderInfo.isSticky

  override val spanSize: Int
    get() = renderInfo.spanSize

  override val isFullSpan: Boolean
    get() = renderInfo.isFullSpan

  override val parentWidthPercent: Float
    get() = renderInfo.parentWidthPercent

  override val parentHeightPercent: Float
    get() = renderInfo.parentHeightPercent

  override fun getCustomAttribute(key: String): Any? {
    return renderInfo.getCustomAttribute(key)
  }

  override fun addCustomAttribute(key: String, value: Any?) {
    renderInfo.addCustomAttribute(key, value)
  }

  /**
   * @return true, if [RenderInfo] was created through [ViewRenderInfo.create], or false otherwise.
   *   This should be queried before accessing view related methods, such as [viewBinder],
   *   [viewCreator], [viewType] and [viewType] from [RenderInfo] type.
   */
  override fun rendersView(): Boolean {
    return renderInfo.rendersView()
  }

  override val viewBinder: ViewBinder<*>
    /**
     * @return Valid [ViewBinder] if [RenderInfo] was created through [ViewRenderInfo.create], or
     *   otherwise it will throw [UnsupportedOperationException]. If this method is accessed from
     *   [RenderInfo] type, [rendersView] should be queried first before accessing.
     */
    get() = renderInfo.viewBinder

  override val viewCreator: ViewCreator<*>
    /**
     * @return Valid [ViewCreator] if [RenderInfo] was created through [ViewRenderInfo.create], or
     *   otherwise it will throw [UnsupportedOperationException]. If this method is accessed from
     *   [RenderInfo] type, [rendersView] should be queried first before accessing.
     */
    get() = renderInfo.viewCreator

  /**
   * @return true, if a custom viewType was set for this [RenderInfo] and it was created through
   *   [ViewRenderInfo.create], or false otherwise.
   */
  override fun hasCustomViewType(): Boolean {
    return renderInfo.hasCustomViewType()
  }

  override var viewType: Int
    /**
     * @return viewType of current [RenderInfo] if it was created through [ViewRenderInfo.create] or
     *   otherwise it will throw [UnsupportedOperationException]. If this method is accessed from
     *   [RenderInfo] type, [rendersView] should be queried first before accessing.
     */
    get() = renderInfo.viewType
    /**
     * Set viewType of current [RenderInfo] if it was created through [ViewRenderInfo.create] and a
     * custom viewType was not set, or otherwise it will throw [UnsupportedOperationException].
     */
    set(viewType) {
      renderInfo.viewType = viewType
    }

  override fun addDebugInfo(key: String, value: Any?) {
    renderInfo.addDebugInfo(key, value)
  }

  override fun getDebugInfo(key: String): Any? {
    return renderInfo.getDebugInfo(key)
  }
}
