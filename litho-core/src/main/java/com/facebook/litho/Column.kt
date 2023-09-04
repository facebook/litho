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

import com.facebook.litho.annotations.Prop
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaFlexDirection
import com.facebook.yoga.YogaJustify
import com.facebook.yoga.YogaWrap

/** A [Component] that renders its children in a column. */
class Column
private constructor(
    customSimpleName: String?,
    @field:Prop(optional = true) private var alignContent: YogaAlign?,
    @field:Prop(optional = true) private var alignItems: YogaAlign?,
    @field:Prop(optional = true) private var justifyContent: YogaJustify?,
    @field:Prop(optional = true) private var wrap: YogaWrap?,
    @field:Prop(optional = true) private var reverse: Boolean,
    @field:Prop(optional = true) private var children: MutableList<Component>?,
) : SpecGeneratedComponent(customSimpleName ?: "Column") {

  constructor(
      customSimpleName: String?
  ) : this(customSimpleName, null, null, null, null, false, null)

  @JvmOverloads
  constructor(
      alignContent: YogaAlign?,
      alignItems: YogaAlign?,
      justifyContent: YogaJustify?,
      wrap: YogaWrap?,
      reverse: Boolean,
      children: MutableList<Component>? = null
  ) : this(null, alignContent, alignItems, justifyContent, wrap, reverse, children)

  override fun canResolve(): Boolean = true

  public override fun resolve(resolveContext: ResolveContext, c: ComponentContext): LithoNode? {
    val node = LithoNode()
    node.flexDirection(if (reverse) YogaFlexDirection.COLUMN_REVERSE else YogaFlexDirection.COLUMN)
    alignItems?.let { node.alignItems(it) }
    alignContent?.let { node.alignContent(it) }
    justifyContent?.let { node.justifyContent(it) }
    wrap?.let { node.wrap(it) }
    children?.let { children ->
      for (child in children) {
        if (resolveContext.isFutureReleased) {
          return null
        }
        if (resolveContext.isLayoutInterrupted) {
          node.appendUnresolvedComponent(child)
        } else {
          node.child(resolveContext, c, child)
        }
      }
    }
    return node
  }

  public override fun resolve(
      resolveContext: ResolveContext,
      scopedComponentInfo: ScopedComponentInfo,
      parentWidthSpec: Int,
      parentHeightSpec: Int,
      componentsLogger: ComponentsLogger?
  ): ComponentResolveResult {
    val lithoNode = resolve(resolveContext, scopedComponentInfo.context)
    return ComponentResolveResult(lithoNode, commonProps)
  }

  override fun isEquivalentProps(other: Component?, shouldCompareCommonProps: Boolean): Boolean {
    if (this === other) {
      return true
    }
    if (other !is Column) {
      return false
    }
    if (this.id == other.id) {
      return true
    }
    val children = children
    val otherChildren = other.children
    if (children != null) {
      if (otherChildren == null || children.size != otherChildren.size) {
        return false
      }
      children.forEachIndexed { index, child ->
        if (!child.isEquivalentTo(otherChildren[index], shouldCompareCommonProps)) {
          return false
        }
      }
    } else if (otherChildren != null) {
      return false
    }
    if (alignItems != other.alignItems) {
      return false
    }
    if (alignContent != other.alignContent) {
      return false
    }
    if (justifyContent != other.justifyContent) {
      return false
    }
    return reverse == other.reverse
  }

  override fun usesLocalStateContainer(): Boolean = true

  class Builder
  internal constructor(
      context: ComponentContext,
      defStyleAttr: Int,
      defStyleRes: Int,
      @JvmField var column: Column
  ) : ContainerBuilder<Builder>(context, defStyleAttr, defStyleRes, column) {
    override fun setComponent(component: Component) {
      column = component as Column
    }

    override fun child(child: Component?): Builder {
      if (child == null) {
        return this
      }
      val children = column.children ?: ArrayList<Component>().also { column.children = it }
      children.add(child)
      return this
    }

    override fun child(child: Component.Builder<*>?): Builder =
        if (child == null) {
          this
        } else {
          child(child.build())
        }

    override fun alignContent(alignContent: YogaAlign?): Builder = apply {
      column.alignContent = alignContent
    }

    override fun alignItems(alignItems: YogaAlign?): Builder = apply {
      column.alignItems = alignItems
    }

    override fun justifyContent(justifyContent: YogaJustify?): Builder = apply {
      column.justifyContent = justifyContent
    }

    override fun wrap(wrap: YogaWrap?): Builder = apply { column.wrap = wrap }

    override fun reverse(reverse: Boolean): Builder = apply { column.reverse = reverse }

    override fun getThis(): Builder = this

    override fun build(): Column = column
  }

  companion object {
    @JvmStatic
    fun create(context: ComponentContext?, simpleName: String?): Builder =
        create(context, 0, 0, simpleName)

    @JvmStatic
    @JvmOverloads
    fun create(
        context: ComponentContext?,
        defStyleAttr: Int = 0,
        defStyleRes: Int = 0,
        simpleName: String? = null
    ): Builder = Builder(requireNotNull(context), defStyleAttr, defStyleRes, Column(simpleName))
  }
}
