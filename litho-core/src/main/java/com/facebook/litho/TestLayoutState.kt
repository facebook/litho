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

@file:Suppress("DEPRECATION") // special-cases the legacy Spec-generated Column / Row classes

package com.facebook.litho

import com.facebook.litho.ComponentsSystrace.trace
import com.facebook.litho.layout.LayoutDirection
import com.facebook.litho.layout.flexbox.FlexboxContainerComponent
import com.facebook.rendercore.LayoutCache
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaFlexDirection
import com.facebook.yoga.YogaJustify
import com.facebook.yoga.YogaWrap
import java.lang.reflect.Field

/**
 * This should be only used with the deprecated DiffNode based testing infrastructure. This class
 * hosts a test implementation of create and resolve layout. The implementation only resolves the
 * immediate subcomponents of the component. The implementation details of this class should be kept
 * in sync with [LayoutState] if the existing tests written with the deprecated testing
 * infrastructure are relevant.
 */
@Deprecated("Only to be used with the deprecated ComponentTestHelper")
object TestLayoutState {

  @JvmStatic
  fun createAndMeasureTreeForComponent(
      resolveContext: ResolveContext,
      context: ComponentContext,
      component: Component,
      widthSpec: Int,
      heightSpec: Int,
  ): LithoNode? {
    val c =
        Resolver.createScopedContext(
            resolveContext,
            context,
            component,
            ComponentKeyUtils.generateGlobalKey(context, context.componentScope, component),
        )
    val root = createImmediateLayout(resolveContext, c, widthSpec, heightSpec, component)
    c.clearCalculationStateContext()
    if (root == null || resolveContext.isResolveInterrupted) {
      return root
    }
    val lsc = LithoLayoutContext(
        resolveContext.treeId,
        resolveContext.cache,
        c,
        resolveContext.treeState,
        resolveContext.layoutVersion,
        resolveContext.rootComponentId,
        resolveContext.isAccessibilityEnabled,
        LayoutCache(),
        null,
        null,
    )
    c.setLithoLayoutContext(lsc)
    root.applyParentDependentCommonProps(
        resolveContext,
        LayoutDirection.fromContext(c.androidContext),
    )
    LayoutUtils.measureTree(lsc, c.androidContext, root, widthSpec, heightSpec)
    c.clearCalculationStateContext()
    return root
  }

  @JvmStatic
  fun newImmediateLayoutBuilder(
      resolveContext: ResolveContext,
      c: ComponentContext,
      widthSpec: Int,
      heightSpec: Int,
      component: Component,
  ): LithoNode? {
    // this can be false for a mocked component
    if (component.mountType.toString() == null) {
      return null
    }
    if (component is SpecGeneratedComponent && canResolve(component)) {
      return if (component is Wrapper) {
        createImmediateLayout(resolveContext, c, widthSpec, heightSpec, component)
      } else {
        create(resolveContext, c, widthSpec, heightSpec, component)
      }
    }
    // After useKFlexboxContainerComponent default flipped to true (D106294343), KColumn/KRow
    // replace the legacy SpecGen Column/Row at every DSL call site. Walk their children the same
    // way the Spec branch above does so `ComponentTestHelper.getSubComponents` can find deeper
    // sub-components for tests that pre-date the flip.
    if (component is FlexboxContainerComponent) {
      return create(resolveContext, c, widthSpec, heightSpec, component)
    }
    // A Mockito deep-stub / smart-null test double has a null getKey(), which would NPE in
    // generateGlobalKey below (a real component reaching this leaf branch always has a non-null
    // key).
    // This surfaced once the legacy Row/Column build() return type widened to Component: a mocked
    // `provider.foo(): Row.Builder?.build()` now yields a bare mock Component (this leaf branch)
    // instead of a mock Row (the tolerant SpecGeneratedComponent branch above). Skip it, matching
    // the
    // pre-migration tolerance of the mock Row.
    if (component.getKey() == null) {
      return null
    }
    val node = createInternalNode()
    val scopedContext =
        ComponentContext.withComponentScope(
            c,
            component,
            ComponentKeyUtils.generateGlobalKey(c, c.componentScope, component),
        )
    val testComponent = TestComponent(component)
    val scopedComponentInfo = ScopedComponentInfo(testComponent, scopedContext, null)
    scopedComponentInfo.commonProps = testComponent.commonProps
    node.appendComponent(scopedComponentInfo)
    return node
  }

  /**
   * Mimics implementation of Column.resolve or Row.resolve but uses a test InternalNode for shallow
   * child resolution. Also handles the Kotlin replacements [FlexboxContainerComponent] (KColumn /
   * KRow), which carry a precomputed `flexDirection` instead of a `reverse` flag.
   */
  private fun resolve(
      resolveContext: ResolveContext,
      c: ComponentContext,
      widthSpec: Int,
      heightSpec: Int,
      component: Component,
  ): LithoNode? {
    // this can be false for a mocked component
    if (component.mountType.toString() == null) {
      return null
    }
    val flexDirection: YogaFlexDirection =
        if (component is FlexboxContainerComponent) {
          getInternalState<YogaFlexDirection>(component, "flexDirection")
        } else {
          val reverse = getInternalState<Boolean>(component, "reverse")
          if (reverse) YogaFlexDirection.COLUMN_REVERSE else YogaFlexDirection.COLUMN
        }
    val alignItems = getInternalState<YogaAlign?>(component, "alignItems")
    val alignContent = getInternalState<YogaAlign?>(component, "alignContent")
    val justifyContent = getInternalState<YogaJustify?>(component, "justifyContent")
    val wrap = getInternalState<YogaWrap?>(component, "wrap")
    val children = getInternalState<List<Component>?>(component, "children")
    val node = createInternalNode()
    node.flexDirection(flexDirection)
    if (alignItems != null) {
      node.alignItems(alignItems)
    }
    if (alignContent != null) {
      node.alignContent(alignContent)
    }
    if (justifyContent != null) {
      node.justifyContent(justifyContent)
    }
    if (wrap != null) {
      node.wrap(wrap)
    }
    if (children != null) {
      for (child in children) {
        if (resolveContext.isFutureReleased) {
          return null
        }
        if (resolveContext.isResolveInterrupted) {
          node.appendUnresolvedComponent(child)
        } else {
          if (child != null) {
            node.child(newImmediateLayoutBuilder(resolveContext, c, widthSpec, heightSpec, child))
          }
        }
      }
    }
    return node
  }

  private fun Component.render(
      resolveContext: ResolveContext,
      c: ComponentContext,
      widthSpec: Int,
      heightSpec: Int,
  ): RenderResult<Component?> {
    return when (this) {
      is KComponent -> {
        val scope = ComponentScope(c)
        val root = scope.withResolveContext(resolveContext) { render() }
        RenderResult(root, scope.transitionData, scope.useEffectEntries)
      }
      is SpecGeneratedComponent -> render(resolveContext, c, widthSpec, heightSpec)
      else -> error("Invalid component: $this")
    }
  }

  private fun createImmediateLayout(
      resolveContext: ResolveContext,
      c: ComponentContext,
      widthSpec: Int,
      heightSpec: Int,
      component: Component,
  ): LithoNode? {
    // this can be false for a mocked component
    if (component.mountType.toString() == null) {
      return null
    }
    val node: LithoNode?
    val layoutCreatedInWillRender = component.consumeLayoutCreatedInWillRender(resolveContext, c)
    if (layoutCreatedInWillRender != null) {
      return layoutCreatedInWillRender
    }
    if (component is SpecGeneratedComponent) {
      val treePropContainer = c.treePropContainer
      c.treePropContainer = component.getTreePropContainerForChildren(c, treePropContainer)
    }
    when {
      component is Wrapper -> {
        val delegate = component.delegate
        return if (delegate == null) {
          null
        } else {
          newImmediateLayoutBuilder(resolveContext, c, widthSpec, heightSpec, delegate)
        }
      }
      component is SpecGeneratedComponent && canResolve(component) -> {
        c.treePropContainer = c.treePropContainerCopy
        node =
            if (component is Column || component is Row) {
              resolve(resolveContext, c, widthSpec, heightSpec, component)
            } else {
              component
                  .resolve(resolveContext, ScopedComponentInfo(component, c, null), 0, 0)
                  .lithoNode
            }
      }
      component is FlexboxContainerComponent -> {
        node = resolve(resolveContext, c, widthSpec, heightSpec, component)
      }
      Component.isMountSpec(component) -> node = createInternalNode()
      else -> {
        val renderResult = component.render(resolveContext, c, widthSpec, heightSpec)
        val root = renderResult.value
        node =
            if (root == null || root.instanceId <= 0) {
              null
            } else {
              resolveImmediateSubTree(resolveContext, c, widthSpec, heightSpec, root)
            }
      }
    }
    if (node == null) {
      return null
    }
    val commonProps = if (component is SpecGeneratedComponent) component.commonProps else null
    if (commonProps != null && !Component.isLayoutSpecWithSizeSpec(component)) {
      commonProps.copyInto(c, node)
    }
    if (node.childCount == 0) {
      val isMountSpecWithMeasure = component.canMeasure() && Component.isMountSpec(component)
      if (isMountSpecWithMeasure) {
        node.setMeasureFunction(Component.sMeasureFunction)
        node.setMinContentMeasureFunction(Component.sMinContentMeasureFunction)
      }
    }
    val scopedComponentInfo = c.scopedComponentInfo
    scopedComponentInfo.commonProps = commonProps
    node.appendComponent(scopedComponentInfo)
    if (component is SpecGeneratedComponent) {
      component.onPrepare(c)
    }
    return node
  }

  @JvmStatic
  fun resolveImmediateSubTree(
      resolveContext: ResolveContext,
      c: ComponentContext,
      widthSpec: Int,
      heightSpec: Int,
      component: Component,
  ): LithoNode? {

    // this can be false for a mocked component
    val node =
        when {
          component.mountType.toString() == null -> null
          component is Wrapper -> {
            val delegate = component.delegate
            if (delegate == null) {
              null
            } else {
              newImmediateLayoutBuilder(resolveContext, c, widthSpec, heightSpec, delegate)
            }
          }
          component is SpecGeneratedComponent && canResolve(component) ->
              create(resolveContext, c, widthSpec, heightSpec, component)
          component is FlexboxContainerComponent ->
              create(resolveContext, c, widthSpec, heightSpec, component)
          else -> createInternalNode()
        }
    if (node != null) {
      val scopedContext =
          ComponentContext.withComponentScope(
              c,
              component,
              ComponentKeyUtils.generateGlobalKey(c, c.componentScope, component),
          )
      val testComponent = TestComponent(component)
      val scopedComponentInfo = ScopedComponentInfo(testComponent, scopedContext, null)
      scopedComponentInfo.commonProps = testComponent.commonProps
      node.appendComponent(scopedComponentInfo)
    }
    return node
  }

  private fun createInternalNode(): LithoNode = LithoNode()

  private fun createNestedTreeHolder(treePropContainer: TreePropContainer?): LithoNode =
      DeferredLithoNode(treePropContainer)

  // Mimics implementation of Layout.create but uses a custom InternalNode for shallow child
  // resolution.
  private fun create(
      resolveContext: ResolveContext,
      parent: ComponentContext,
      widthSpec: Int,
      heightSpec: Int,
      component: Component,
  ): LithoNode? =
      trace({ "createLayout:${component.simpleName}" }) {
        val node: LithoNode?
        val c: ComponentContext
        val globalKey: GlobalKey
        val scopedComponentInfo: ScopedComponentInfo
        try {

          // 1. Consume the layout created in `willrender`.
          val cached = component.consumeLayoutCreatedInWillRender(resolveContext, parent)

          // 2. Return immediately if cached layout is available.
          if (cached != null) {
            return cached
          }

          // 4. Update the component.
          // 5. Get the scoped context of the updated component.
          c =
              Resolver.createScopedContext(
                  resolveContext,
                  parent,
                  component,
                  null as GlobalKey?,
              )
          globalKey = c.globalKey
          scopedComponentInfo = c.scopedComponentInfo
          // 6. Resolve the component into an InternalNode tree.
          val shouldDeferNestedTreeResolution = Component.willDeferResolution(component)

          // If nested tree resolution is deferred, then create an nested tree holder.
          node =
              when {
                shouldDeferNestedTreeResolution -> createNestedTreeHolder(c.treePropContainer)
                component is SpecGeneratedComponent && canResolve(component) -> {

                  // Resolve the component into an InternalNode.
                  if (component is Column || component is Row) {
                    resolve(resolveContext, c, widthSpec, heightSpec, component)
                  } else {
                    component
                        .resolve(resolveContext, ScopedComponentInfo(component, c, null), 0, 0)
                        .lithoNode
                  }
                }
                component is FlexboxContainerComponent -> {
                  resolve(resolveContext, c, widthSpec, heightSpec, component)
                }
                Component.isMountSpec(component) -> {

                  // Create a blank InternalNode for MountSpecs and set the default flex direction.
                  createInternalNode().also { it.flexDirection(YogaFlexDirection.COLUMN) }
                }
                Component.isLayoutSpec(component) -> {
                  val renderResult = component.render(resolveContext, c, widthSpec, heightSpec)
                  val root = renderResult.value
                  val n =
                      if (root != null) {
                        create(resolveContext, c, widthSpec, heightSpec, root)
                      } else {
                        null
                      }
                  if (n != null) {
                    Resolver.applyTransitionsAndUseEffectEntriesToNode(
                        renderResult.transitionData,
                        renderResult.useEffectEntries,
                        n,
                    )
                  }
                  n
                }
                else -> throw IllegalArgumentException("component:${component.simpleName}")
              }
        } catch (e: Exception) {
          ComponentUtils.handleWithHierarchy(parent, component, e)
          return null
        }
        // 7. If the layout is null then return immediately.
        if (node == null) {
          return null
        }
        trace({ "afterCreateLayout:${component.simpleName}" }) {

          // 8. Set the measure function
          // Set measure func on the root node of the generated tree so that the mount calls use
          // those (see Controller.mountNodeTree()). Handle the case where the component simply
          // delegates its layout creation to another component, i.e. the root node belongs to
          // another component.
          if (node.childCount == 0) {
            val isMountSpecWithMeasure = component.canMeasure() && Component.isMountSpec(component)
            if (isMountSpecWithMeasure || Component.willDeferResolution(component)) {
              node.setMeasureFunction(Component.sMeasureFunction)
              node.setMinContentMeasureFunction(Component.sMinContentMeasureFunction)
            }
          }

          // 10. Add the component to the InternalNode.
          node.appendComponent(scopedComponentInfo)

          // 11. Create and add transition to this component's InternalNode.
          if (c.areTransitionsEnabled()) {
            if (component is SpecGeneratedComponent && component.needsPreviousRenderData()) {
              node.addComponentNeedingPreviousRenderData(scopedComponentInfo)
            } else {
              try {
                // Calls onCreateTransition on the Spec.
                val transition =
                    if (component is SpecGeneratedComponent) component.createTransition(c) else null
                if (transition != null) {
                  node.addTransition(transition)
                }
              } catch (e: Exception) {
                ComponentUtils.handleWithHierarchy(parent, component, e)
              }
            }
          }

          // 12. Add attachable components
          if (
              component is SpecGeneratedComponent && component.hasAttachDetachCallback()
          ) { // needs ComponentUtils.getGlobalKey?
            node.addAttachable(LayoutSpecAttachable(globalKey, component, scopedComponentInfo))
          }

          // 13. Call onPrepare for MountSpecs.
          if (Component.isMountSpec(component)) {
            try {
              (component as SpecGeneratedComponent).onPrepare(c)
            } catch (e: Exception) {
              ComponentUtils.handleWithHierarchy(parent, component, e)
            }
          }

          // 14. Add working ranges to the InternalNode.
          scopedComponentInfo.addWorkingRangeToNode(node)
        }
        node
      }

  private fun <T> getInternalState(obj: Any, fieldName: String): T {
    val foundField = findFieldInHierarchy(getType(obj), fieldName)
    return try {
      foundField[obj] as T
    } catch (e: IllegalAccessException) {
      throw RuntimeException("Internal error: Failed to get field in method getInternalState.", e)
    }
  }

  private fun findFieldInHierarchy(startClass: Class<*>, fieldName: String): Field {
    var foundField: Field? = null
    var currentClass: Class<*>? = startClass
    while (currentClass != null) {
      val declaredFields = currentClass.declaredFields
      for (field in declaredFields) {
        if (field.name == fieldName) {
          require(foundField == null) { "Two or more fields matching $fieldName in $startClass." }
          foundField = field
        }
      }
      if (foundField != null) {
        break
      }
      currentClass = currentClass.superclass
    }
    requireNotNull(foundField) { "No fields matching $fieldName in $startClass." }
    foundField.isAccessible = true
    return foundField
  }

  private fun getType(obj: Any): Class<*> = if (obj is Class<*>) obj else obj.javaClass

  @JvmStatic
  fun canResolve(component: SpecGeneratedComponent?): Boolean =
      // for legacy reasons this method enumerates components which returned true from canResolve()
      // until TestLayoutState is further refactored
      component is Row || component is Column || component is Wrapper
}
