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

import com.facebook.litho.Resolver.resolveDeferred
import com.facebook.litho.annotations.ExperimentalLithoApi
import com.facebook.litho.layout.LayoutFunction
import com.facebook.litho.layout.LayoutFunction1
import com.facebook.litho.layout.LayoutFunction2
import com.facebook.litho.layout.LayoutFunction3
import com.facebook.litho.layout.LayoutFunctionN
import com.facebook.rendercore.utils.areObjectsEquivalent

/**
 * A container component that owns the children it is given.
 *
 * A `Row` or a `Column` hands its children to flexbox. A `Layout` positions them itself: [layout]
 * measures the children, gives the container a size and places each child. Use it for layouts
 * flexbox cannot express, such as a pager whose pages sit at a computed stride, a split pane sized
 * by a dragged divider, or a header that collapses as another child scrolls.
 *
 * It is an ordinary component, returned from `render`:
 * ```kotlin
 * class MyRow(private val header: Component, private val body: Component) : KComponent() {
 *   override fun ComponentScope.render(): Component =
 *       Layout(header, body) { header, body, c -> … }
 * }
 * ```
 *
 * Use [components] for children that come from data. The one-, two- and three-child constructors
 * build the same container and let a fixed set of children be named at the call site:
 * ```kotlin
 * Layout(items.map { Page(it).key(it.id) }) { measurables, c -> … }   // N children
 * Layout(avatar, name, timestamp) { avatar, name, timestamp, c -> … } // three, named
 * ```
 *
 * **A container is unchanged only when its layout function is the same one.** It is compared with
 * `equals`, never by class. An `object`, or a lambda that captures nothing, is the same instance on
 * every render and lets an unchanged container skip its layout. A lambda that captures values is a
 * new instance each render, so the container lays out again every time, which is always correct and
 * costs only the layout.
 *
 * A null child, or one that renders nothing, keeps its slot as an empty child that measures 0x0, so
 * every child has the index it was declared at and a function naming its children always gets them
 * all.
 *
 * Give children a stable `key` when they come from data. Without one, a child's state is keyed by
 * its position among its same-type siblings, so inserting at the front hands the second child the
 * first child's state. That shows up as a video resuming at the wrong timestamp rather than as
 * something that looks like a bug.
 *
 * Children resolve into the parent's Litho tree rather than a nested one, so hook identity, prop
 * reconciliation, incremental mount and visibility keep working across them without forwarding
 * anything by hand.
 *
 * A `Layout` mounts no content, so it gets a host of its own only when a [style] needs one.
 * `onClick`, `viewTag`, `focusable`, a shadow and a transition name each create a host, as on any
 * other component. Intercepting touch, cooperating with a nested scroll, or transforming children
 * per frame still needs whatever mechanism you use today.
 *
 * @param components the children, in the order they are laid out
 * @param style what this container tells its own parent, and the props the framework applies to any
 *   node: size, padding, margin, background, and so on. Its padding and border are the one way it
 *   reaches the children: the layout function works inside them, so they narrow the constraints it
 *   receives. Anything else that configures the arrangement — spacing, alignment — the layout
 *   function captures, or the component returning this `Layout` takes as a prop.
 * @param layout measures and places the children
 */
@ExperimentalLithoApi
class Layout
private constructor(
    private val layoutFunction: LayoutFunction,
    private val components: List<Component?>,
    private val style: Style,
) : Component() {

  /**
   * A container over a list of children, for a count that is not known until runtime.
   *
   * @param components the children, in the order they are laid out
   * @param style applied to the container itself
   * @param layout measures and places the children, receiving them as a list
   */
  constructor(
      components: List<Component?>,
      style: Style = Style,
      layout: LayoutFunctionN,
  ) : this(layout, components, style)

  /**
   * A container over one child, which [layout] receives by name rather than by index.
   *
   * @param component1 the only child
   * @param style applied to the container itself
   * @param layout measures and places the child
   */
  constructor(
      component1: Component?,
      style: Style = Style,
      layout: LayoutFunction1,
  ) : this(layout, listOf(component1), style)

  /**
   * A container over two children, which [layout] receives by name rather than by index.
   *
   * @param component1 the first child
   * @param component2 the second child
   * @param style applied to the container itself
   * @param layout measures and places the children
   */
  constructor(
      component1: Component?,
      component2: Component?,
      style: Style = Style,
      layout: LayoutFunction2,
  ) : this(layout, listOf(component1, component2), style)

  /**
   * A container over three children, which [layout] receives by name rather than by index. Past
   * three, use the [components] list.
   *
   * @param component1 the first child
   * @param component2 the second child
   * @param component3 the third child
   * @param style applied to the container itself
   * @param layout measures and places the children
   */
  constructor(
      component1: Component?,
      component2: Component?,
      component3: Component?,
      style: Style = Style,
      layout: LayoutFunction3,
  ) : this(layout, listOf(component1, component2, component3), style)

  override fun resolve(
      resolveContext: ResolveContext,
      scopedComponentInfo: ScopedComponentInfo,
      parentWidthSpec: Int,
      parentHeightSpec: Int,
  ): ComponentResolveResult {
    val c = scopedComponentInfo.context
    val node = LayoutNode()
    for (child in components) {
      // Resolve can be abandoned or handed back to the main thread between any two children, and a
      // container that keeps descending defeats both: a released future's tree is thrown away, and
      // an interrupted one is finished by `Resolver.resumeResolvingTree`, which can only resume the
      // children stashed here. A null is stashed as an empty child, so resuming keeps every slot.
      if (resolveContext.isFutureReleased) {
        return ComponentResolveResult(null, null)
      }
      if (resolveContext.isResolveInterrupted) {
        node.appendUnresolvedComponent(child ?: EmptyComponent())
      } else {
        node.child(resolveContext, c, child)
      }
    }
    node.layoutFunction = layoutFunction
    return ComponentResolveResult(node, createCommonProps(c))
  }

  override fun resolveDeferred(
      calculationContext: CalculationContext,
      componentContext: ComponentContext,
      parentContext: ComponentContext,
  ): ComponentResolveResult =
      this.resolveDeferred(
          calculationContext,
          componentContext,
          parentContext,
          createCommonProps(componentContext),
      )

  private fun createCommonProps(c: ComponentContext): CommonProps? =
      if (style !== Style) CommonProps().apply { style.applyCommonProps(c, this) } else null

  /**
   * Compares the children, the style and the layout function, by `equals`.
   *
   * Inheriting the default would compare nothing. [Component.isEquivalentProps] defers to
   * `ComponentUtils.hasEquivalentFields`, which only looks at fields annotated `@Comparable`, and
   * the Spec API's processor is what generates those onto `@Prop`s. A Kotlin component has none, so
   * every container would be equivalent to every other and a changed child would never reach the
   * screen. `KComponent` overrides this for the same reason.
   *
   * The layout function is never compared by class. Two renders of one call site share a class even
   * when what the lambda captured changed, so class identity would keep a container laid out
   * against a stale value. Comparing the instance is always correct: a lambda that captures values
   * is a new instance each render, so its container lays out again.
   */
  override fun isEquivalentProps(other: Component?): Boolean {
    if (this === other) {
      return true
    }
    if (other !is Layout || javaClass != other.javaClass) {
      return false
    }
    if (instanceId == other.instanceId) {
      return true
    }
    return layoutFunction == other.layoutFunction &&
        areObjectsEquivalent(components, other.components) &&
        areObjectsEquivalent(style, other.style)
  }

  override fun isPureRender(): Boolean = true
}

/**
 * The node of a `Layout`, whose children keep their declared slots: one that is null, or resolves
 * to no node, holds its slot as an empty node, which lays out at 0x0.
 */
internal class LayoutNode : LithoNode() {

  override fun child(resolveContext: ResolveContext, c: ComponentContext, child: Component?) {
    child(
        child?.let { Resolver.resolve(resolveContext, c, it) }
            ?: Resolver.resolve(resolveContext, c, EmptyComponent()),
    )
  }
}
