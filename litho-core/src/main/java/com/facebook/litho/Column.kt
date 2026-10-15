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

@file:Suppress("DEPRECATION")

package com.facebook.litho

import android.annotation.SuppressLint
import com.facebook.litho.annotations.Prop
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaFlexDirection
import com.facebook.yoga.YogaGutter
import com.facebook.yoga.YogaJustify
import com.facebook.yoga.YogaWrap

/** A [Component] that renders its children in a column. */
@SuppressLint("DeprecatedClass", "DeprecatedMethod")
@Deprecated(
    "The legacy Spec-generated Column component is deprecated and will be retired. " +
        "Use Column from com.facebook.litho.layout.flexbox instead.",
)
class Column
private constructor(
    customSimpleName: String?,
    @field:Prop(optional = true) private var alignContent: YogaAlign?,
    @field:Prop(optional = true) private var alignItems: YogaAlign?,
    @field:Prop(optional = true) private var justifyContent: YogaJustify?,
    @field:Prop(optional = true) private var wrap: YogaWrap?,
    @field:Prop(optional = true) private var reverse: Boolean,
    @field:Prop(optional = true) private var children: MutableList<Component>?,
    @field:Prop(optional = true) private var gapPx: Int? = null,
    @field:Prop(optional = true) private var mainAxisGapPx: Int? = null,
    @field:Prop(optional = true) private var crossAxisGapPx: Int? = null,
) : SpecGeneratedComponent(customSimpleName ?: "Column") {

  constructor(
      customSimpleName: String?,
  ) : this(customSimpleName, null, null, null, null, false, null)

  @JvmOverloads
  constructor(
      alignContent: YogaAlign?,
      alignItems: YogaAlign?,
      justifyContent: YogaJustify?,
      wrap: YogaWrap?,
      reverse: Boolean,
      children: MutableList<Component>? = null,
      gapPx: Int? = null,
      mainAxisGapPx: Int? = null,
      crossAxisGapPx: Int? = null,
  ) : this(
      null,
      alignContent,
      alignItems,
      justifyContent,
      wrap,
      reverse,
      children,
      gapPx,
      mainAxisGapPx,
      crossAxisGapPx,
  )

  private fun resolve(resolveContext: ResolveContext, c: ComponentContext): LithoNode? {
    val node = LithoNode()
    node.flexDirection(if (reverse) YogaFlexDirection.COLUMN_REVERSE else YogaFlexDirection.COLUMN)
    alignItems?.let { node.alignItems(it) }
    alignContent?.let { node.alignContent(it) }
    justifyContent?.let { node.justifyContent(it) }
    wrap?.let { node.wrap(it) }
    // For Column: mainAxis = vertical (ROW), crossAxis = horizontal (COLUMN)
    if (mainAxisGapPx != null || crossAxisGapPx != null) {
      mainAxisGapPx?.let { node.setGap(YogaGutter.ROW, it) }
      crossAxisGapPx?.let { node.setGap(YogaGutter.COLUMN, it) }
    } else {
      gapPx?.let { node.setGap(YogaGutter.ALL, it) }
    }
    val children = children ?: return node
    val size = children.size
    if (size == 0) return node
    for (i in 0 until size) {
      val child = children[i]
      if (resolveContext.isFutureReleased) {
        return null
      }
      if (resolveContext.isResolveInterrupted) {
        node.appendUnresolvedComponent(child)
      } else {
        val resolved = Resolver.resolve(resolveContext, c, child)
        if (resolved != null) {
          node.children.add(resolved)
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
  ): ComponentResolveResult {
    val lithoNode = resolve(resolveContext, scopedComponentInfo.context)
    return ComponentResolveResult(lithoNode, commonProps)
  }

  override fun isEquivalentProps(other: Component?): Boolean {
    if (this === other) {
      return true
    }
    if (other !is Column) {
      return false
    }
    if (this.instanceId == other.instanceId) {
      return true
    }
    val children = children
    val otherChildren = other.children
    if (children != null) {
      if (otherChildren == null || children.size != otherChildren.size) {
        return false
      }
      children.forEachIndexed { index, child ->
        if (!ComponentUtils.isEquivalent(child, otherChildren[index])) {
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
    if (gapPx != other.gapPx) {
      return false
    }
    if (mainAxisGapPx != other.mainAxisGapPx) {
      return false
    }
    if (crossAxisGapPx != other.crossAxisGapPx) {
      return false
    }
    return reverse == other.reverse
  }

  override fun usesLocalStateContainer(): Boolean = true

  @Deprecated(
      "The legacy Column.Builder API is deprecated and will be retired. " +
          "Use Column from com.facebook.litho.layout.flexbox instead.",
  )
  class Builder
  internal constructor(
      context: ComponentContext,
      defStyleAttr: Int,
      defStyleRes: Int,
      customSimpleName: String?,
  ) :
      FlexboxLegacyContainerBuilder<Builder>(
          context,
          defStyleAttr,
          defStyleRes,
          Column(customSimpleName),
          customSimpleName,
      ) {
    override fun getThis(): Builder = this

    override fun build(): Component = if (isKComponentEnabled()) buildKColumn() else buildLegacy()

    // Leaf-owned, mirroring the pre-refactor base shape: child(Builder) must feed build() straight
    // into the child(Component) sink. Redex's RemoveBuilderPatternPass strips the passed-in builder
    // only if it can inline child() to expose the nested build(); a direct
    // `children.add(child.build())` body compiles to getChildren()/checkNotNull/List.add, which the
    // pass's restricted inliner refuses — the builder then escapes and is globally excluded.
    override fun child(child: Component.Builder<*>?): Builder =
        if (child == null) this else child(child.build())

    override fun child(child: Component?): Builder {
      if (child != null) children.add(child)
      return this
    }

    override fun applyContainerProps(component: SpecGeneratedComponent) {
      val column = component as Column
      column.alignContent = alignContentValue
      column.alignItems = alignItemsValue
      column.justifyContent = justifyContentValue
      column.wrap = wrapValue
      column.reverse = reverseValue
      column.children = children
      column.gapPx = gapPxValue
    }
  }

  companion object {
    @Deprecated(
        "The legacy Column.create() builder API is deprecated and will be retired. " +
            "Use Column from com.facebook.litho.layout.flexbox instead.",
    )
    @JvmStatic
    fun create(context: ComponentContext?, simpleName: String?): Builder =
        Builder(requireNotNull(context), 0, 0, simpleName)

    @Deprecated(
        "The legacy Column.create() builder API is deprecated and will be retired. " +
            "Use Column from com.facebook.litho.layout.flexbox instead.",
    )
    @JvmStatic
    @JvmOverloads
    fun create(
        context: ComponentContext?,
        defStyleAttr: Int = 0,
        defStyleRes: Int = 0,
        simpleName: String? = null,
    ): Builder = Builder(requireNotNull(context), defStyleAttr, defStyleRes, simpleName)
  }
}
