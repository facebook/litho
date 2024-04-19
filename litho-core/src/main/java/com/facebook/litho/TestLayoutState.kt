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

import com.facebook.litho.ComponentsSystrace.isTracing
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
      heightSpec: Int
  ): LithoNode? {
    val c =
        Resolver.createScopedContext(
            resolveContext,
            context,
            component,
            ComponentKeyUtils.generateGlobalKey(context, context.componentScope, component),
            null)
    val root = createImmediateLayout(resolveContext, c, widthSpec, heightSpec, component)
    c.clearCalculationStateContext()
    if (root == null || resolveContext.isLayoutInterrupted) {
      return root
    }
    val lsc =
        LithoLayoutContext(
            resolveContext.treeId,
            resolveContext.cache,
            c,
            resolveContext.treeState,
            resolveContext.layoutVersion,
            resolveContext.rootComponentId,
            resolveContext.isAccessibilityEnabled,
            LayoutCache(),
            null,
            null)
    c.setLithoLayoutContext(lsc)
    Layout.measureTree(lsc, c.androidContext, root, widthSpec, heightSpec, null)
    c.clearCalculationStateContext()
    return root
  }

  @JvmStatic
  fun newImmediateLayoutBuilder(
      resolveContext: ResolveContext,
      c: ComponentContext,
      widthSpec: Int,
      heightSpec: Int,
      component: Component
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
    val node = createInternalNode()
    val scopedContext =
        ComponentContext.withComponentScope(
            c, component, ComponentKeyUtils.generateGlobalKey(c, c.componentScope, component))
    val testComponent = TestComponent(component)
    val scopedComponentInfo = ScopedComponentInfo(testComponent, scopedContext, null)
    scopedComponentInfo.commonProps = testComponent.commonProps
    node.appendComponent(scopedComponentInfo)
    return node
  }

  /**
   * Mimics implementation of Column.resolve or Row.resolve but uses a test InternalNode for shallow
   * child resolution.
   */
  private fun resolve(
      resolveContext: ResolveContext,
      c: ComponentContext,
      widthSpec: Int,
      heightSpec: Int,
      component: Component
  ): LithoNode? {
    // this can be false for a mocked component
    if (component.mountType.toString() == null) {
      return null
    }
    val reverse = getInternalState<Boolean>(component, "reverse")
    val alignItems = getInternalState<YogaAlign?>(component, "alignItems")
    val alignContent = getInternalState<YogaAlign?>(component, "alignContent")
    val justifyContent = getInternalState<YogaJustify?>(component, "justifyContent")
    val wrap = getInternalState<YogaWrap?>(component, "wrap")
    val children = getInternalState<List<Component>?>(component, "children")
    val node = createInternalNode()
    node.flexDirection(if (reverse) YogaFlexDirection.COLUMN_REVERSE else YogaFlexDirection.COLUMN)
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
        if (resolveContext.isLayoutInterrupted) {
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

  private fun createImmediateLayout(
      resolveContext: ResolveContext,
      c: ComponentContext,
      widthSpec: Int,
      heightSpec: Int,
      component: Component
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
                  .resolve(resolveContext, ScopedComponentInfo(component, c, null), 0, 0, null)
                  .lithoNode
            }
      }
      Component.isMountSpec(component) -> node = createInternalNode()
      else -> {
        val renderResult = component.render(resolveContext, c, widthSpec, heightSpec)
        val root = renderResult.component
        node =
            if (root == null || root.id <= 0) {
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
      component: Component
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
          else -> createInternalNode()
        }
    if (node != null) {
      val testComponent = TestComponent(component)
      val scopedComponentInfo = ScopedComponentInfo(testComponent, c, null)
      val commonProps = testComponent.commonProps
      scopedComponentInfo.commonProps = commonProps
      node.appendComponent(scopedComponentInfo)
    }
    return node
  }

  private fun createInternalNode(): LithoNode = LithoNode()

  private fun createNestedTreeHolder(treePropContainer: TreePropContainer?): LithoNode =
      NestedTreeHolder(treePropContainer)

  // Mimics implementation of Layout.create but uses a custom InternalNode for shallow child
  // resolution.
  private fun create(
      resolveContext: ResolveContext,
      parent: ComponentContext,
      widthSpec: Int,
      heightSpec: Int,
      component: Component
  ): LithoNode? {
    val isTracing = isTracing
    if (isTracing) {
      ComponentsSystrace.beginSection("createLayout:${component.simpleName}")
    }
    val node: LithoNode?
    val c: ComponentContext
    val globalKey: String
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
      c = Resolver.createScopedContext(resolveContext, parent, component, null, null)
      globalKey = c.globalKey
      scopedComponentInfo = c.scopedComponentInfo
      // 6. Resolve the component into an InternalNode tree.
      val shouldDeferNestedTreeResolution = Component.isNestedTree(component)

      // If nested tree resolution is deferred, then create an nested tree holder.
      when {
        shouldDeferNestedTreeResolution -> node = createNestedTreeHolder(c.treePropContainer)
        component is SpecGeneratedComponent && canResolve(component) -> {

          // Resolve the component into an InternalNode.
          node =
              if (component is Column || component is Row) {
                resolve(resolveContext, c, widthSpec, heightSpec, component)
              } else {
                component
                    .resolve(resolveContext, ScopedComponentInfo(component, c, null), 0, 0, null)
                    .lithoNode
              }
        }
        Component.isMountSpec(component) -> {

          // Create a blank InternalNode for MountSpecs and set the default flex direction.
          node = createInternalNode()
          node.flexDirection(YogaFlexDirection.COLUMN)
        }
        Component.isLayoutSpec(component) -> {
          val renderResult = component.render(resolveContext, c, widthSpec, heightSpec)
          val root = renderResult.component
          node =
              if (root != null) {
                create(resolveContext, c, widthSpec, heightSpec, root)
              } else {
                null
              }
          if (node != null) {
            Resolver.applyTransitionsAndUseEffectEntriesToNode(
                renderResult.transitions, renderResult.useEffectEntries, node)
          }
        }
        else -> throw IllegalArgumentException("component:${component.simpleName}")
      }
    } catch (e: Exception) {
      ComponentUtils.handleWithHierarchy(parent, component, e)
      return null
    } finally {
      if (isTracing) {
        ComponentsSystrace.endSection()
      }
    }
    // 7. If the layout is null then return immediately.
    if (node == null) {
      return null
    }
    if (isTracing) {
      ComponentsSystrace.beginSection("afterCreateLayout:${component.simpleName}")
    }

    // 8. Set the measure function
    // Set measure func on the root node of the generated tree so that the mount calls use
    // those (see Controller.mountNodeTree()). Handle the case where the component simply
    // delegates its layout creation to another component, i.e. the root node belongs to
    // another component.
    if (node.childCount == 0) {
      val isMountSpecWithMeasure = component.canMeasure() && Component.isMountSpec(component)
      if (isMountSpecWithMeasure || Component.isNestedTree(component)) {
        node.setMeasureFunction(Component.sMeasureFunction)
      }
    }

    // 10. Add the component to the InternalNode.
    node.appendComponent(scopedComponentInfo)

    // 11. Create and add transition to this component's InternalNode.
    if (c.areTransitionsEnabled()) {
      if (component is SpecGeneratedComponent && component.needsPreviousRenderData()) {
        node.addComponentNeedingPreviousRenderData(globalKey, scopedComponentInfo)
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
    if (component is SpecGeneratedComponent &&
        component.hasAttachDetachCallback()) { // needs ComponentUtils.getGlobalKey?
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
    if (isTracing) {
      ComponentsSystrace.endSection()
    }
    return node
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
