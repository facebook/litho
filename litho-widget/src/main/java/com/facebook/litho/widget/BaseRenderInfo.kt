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
import com.facebook.litho.viewcompat.ViewBinder
import com.facebook.litho.viewcompat.ViewCreator
import java.util.Collections

/**
 * Keeps the list item information that will allow the framework to understand how to render it.
 *
 * SpanSize will be defaulted to 1. It is the information that is required to calculate how much of
 * the SpanCount the component should occupy in a Grid layout.
 *
 * IsSticky will be defaulted to false. It determines if the component should be a sticky header or
 * not
 *
 * IsFullSpan will be defaulted to false. It is the information that determines if the component
 * should occupy all of the SpanCount in a StaggeredGrid layout.
 *
 * ParentWidthPercent determines how much space of the parent container in width the component would
 * take to fill.
 *
 * ParentHeightPercent determines how much space of the parent container in height the component
 * would take to fill.
 */
abstract class BaseRenderInfo protected constructor(builder: Builder<*>) : RenderInfo {

  private var customAttributes: MutableMap<String, Any?>? = builder.customAttributes
  private var debugInfo: MutableMap<String, Any?>? = builder.debugInfo

  override val isSticky: Boolean
    get() {
      val attributes = customAttributes
      if (attributes == null || !attributes.containsKey(IS_STICKY)) {
        return false
      }

      return attributes[IS_STICKY] as Boolean
    }

  override val spanSize: Int
    get() {
      val attributes = customAttributes
      if (attributes == null || !attributes.containsKey(SPAN_SIZE)) {
        return 1
      }

      return attributes[SPAN_SIZE] as Int
    }

  override val isFullSpan: Boolean
    get() {
      val attributes = customAttributes
      if (attributes == null || !attributes.containsKey(IS_FULL_SPAN)) {
        return false
      }

      return attributes[IS_FULL_SPAN] as Boolean
    }

  override fun getCustomAttribute(key: String): Any? {
    return customAttributes?.get(key)
  }

  override fun addCustomAttribute(key: String, value: Any?) {
    if (customAttributes == null) {
      customAttributes = Collections.synchronizedMap(HashMap())
    }
    requireNotNull(customAttributes)[key] = value
  }

  override val parentWidthPercent: Float
    get() {
      val attributes = customAttributes
      if (attributes == null || !attributes.containsKey(PARENT_WIDTH_PERCENT)) {
        return -1f
      }
      return attributes[PARENT_WIDTH_PERCENT] as Float
    }

  override val parentHeightPercent: Float
    get() {
      val attributes = customAttributes
      if (attributes == null || !attributes.containsKey(PARENT_HEIGHT_PERCENT)) {
        return -1f
      }
      return attributes[PARENT_HEIGHT_PERCENT] as Float
    }

  /**
   * @return true, if [RenderInfo] was created through [ComponentRenderInfo.create], or false
   *   otherwise. This should be queried before accessing [.getComponent] from [RenderInfo] type.
   */
  override fun rendersComponent(): Boolean {
    return false
  }

  override val component: Component
    /**
     * @return Valid [Component] if [RenderInfo] was created through [ComponentRenderInfo.create],
     *   otherwise it will throw [UnsupportedOperationException]. If this method is accessed from
     *   [RenderInfo] type, [rendersComponent] should be queried first before accessing.
     */
    get() {
      throw UnsupportedOperationException()
    }

  override val renderCompleteEventHandler: EventHandler<RenderCompleteEvent>?
    /**
     * @return Valid [EventHandler<RenderCompleteEvent>] if [RenderInfo] was created through
     *   [ComponentRenderInfo.create], otherwise it will throw [UnsupportedOperationException].
     */
    get() {
      // TODO(T28620590): Support RenderCompleteEvent handler for ViewRenderInfo
      throw UnsupportedOperationException()
    }

  override val componentsLogger: ComponentsLogger?
    /**
     * @return Optional [ComponentsLogger] if [RenderInfo] was created through
     *   [ComponentRenderInfo.create], null otherwise
     */
    get() = null

  override val debugEventListener: ComponentTreeDebugEventListener?
    /**
     * @return Optional [ComponentTreeDebugEventListener] if [RenderInfo] was created through
     *   [ComponentRenderInfo.create], null otherwise
     */
    get() = null

  override val logTag: String?
    /**
     * @return Optional identifier for logging if [RenderInfo] was created through
     *   [ComponentRenderInfo.create], null otherwise
     */
    get() = null

  /**
   * @return true, if [RenderInfo] was created through [ViewRenderInfo.create], or false otherwise.
   *   This should be queried before accessing view related methods, such as [getViewBinder],
   *   [getViewCreator], [getViewType] and [setViewType] from [RenderInfo] type.
   */
  override fun rendersView(): Boolean {
    return false
  }

  override val viewBinder: ViewBinder<*>
    /**
     * @return Valid [ViewBinder] if [RenderInfo] was created through [ViewRenderInfo.create], or
     *   otherwise it will throw [UnsupportedOperationException]. If this method is accessed from
     *   [RenderInfo] type, [rendersView] should be queried first before accessing.
     */
    get() {
      throw UnsupportedOperationException()
    }

  override val viewCreator: ViewCreator<*>
    /**
     * @return Valid [ViewCreator] if [RenderInfo] was created through [ViewRenderInfo.create], or
     *   otherwise it will throw [UnsupportedOperationException]. If this method is accessed from
     *   [RenderInfo] type, [rendersView] should be queried first before accessing.
     */
    get() {
      throw UnsupportedOperationException()
    }

  /**
   * @return true, if a custom viewType was set for this [RenderInfo] and it was created through
   *   [ViewRenderInfo.create], or false otherwise.
   */
  override fun hasCustomViewType(): Boolean {
    return false
  }

  override var viewType: Int
    /**
     * @return viewType of current [RenderInfo] if it was created through [ViewRenderInfo.create] or
     *   otherwise it will throw [UnsupportedOperationException]. If this method is accessed from
     *   [RenderInfo] type, [rendersView] should be queried first before accessing.
     */
    get() {
      throw UnsupportedOperationException()
    }
    /**
     * Set viewType of current [RenderInfo] if it was created through [ViewRenderInfo.create] and a
     * custom viewType was not set, or otherwise it will throw [UnsupportedOperationException].
     */
    set(viewType) {
      throw UnsupportedOperationException()
    }

  override fun addDebugInfo(key: String, value: Any?) {
    if (debugInfo == null) {
      debugInfo = Collections.synchronizedMap(HashMap())
    }

    requireNotNull(debugInfo)[key] = value
  }

  override fun getDebugInfo(key: String): Any? {
    val info = debugInfo
    if (info == null) {
      return null
    }

    return info[key]
  }

  abstract class Builder<T> {
    var customAttributes: MutableMap<String, Any?>? = null
    var debugInfo: MutableMap<String, Any?>? = null

    open fun isSticky(isSticky: Boolean): T {
      return customAttribute(IS_STICKY, isSticky)
    }

    open fun spanSize(spanSize: Int): T {
      return customAttribute(SPAN_SIZE, spanSize)
    }

    open fun isFullSpan(isFullSpan: Boolean): T {
      return customAttribute(IS_FULL_SPAN, isFullSpan)
    }

    open fun parentWidthPercent(widthPercent: Float): T {
      return customAttribute(PARENT_WIDTH_PERCENT, widthPercent)
    }

    open fun parentHeightPercent(heightPercent: Float): T {
      return customAttribute(PARENT_HEIGHT_PERCENT, heightPercent)
    }

    open fun customAttribute(key: String, value: Any?): T {
      if (customAttributes == null) {
        customAttributes = Collections.synchronizedMap(HashMap())
      }
      requireNotNull(customAttributes)[key] = value

      return this as T
    }

    open fun debugInfo(key: String, value: Any?): T {
      if (debugInfo == null) {
        debugInfo = Collections.synchronizedMap(HashMap())
      }

      requireNotNull(debugInfo)[key] = value

      return this as T
    }
  }

  companion object {
    private const val IS_STICKY = "is_sticky"
    private const val SPAN_SIZE = "span_size"
    private const val IS_FULL_SPAN = "is_full_span"
    private const val PARENT_WIDTH_PERCENT = "parent_width_percent"
    private const val PARENT_HEIGHT_PERCENT = "parent_height_percent"
  }
}
