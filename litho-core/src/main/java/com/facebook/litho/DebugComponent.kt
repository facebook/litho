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

import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.TextView
import com.facebook.litho.LithoRenderUnit.Companion.getRenderUnit
import com.facebook.rendercore.RenderUnit
import com.facebook.rendercore.visibility.VisibilityMountExtension
import com.facebook.rendercore.visibility.VisibilityOutput

/**
 * A DebugComponent represents a node in Litho's component hierarchy. DebugComponent removes the
 * need to worry about implementation details of whether a node is represented by a [ ] or a
 * [ComponentLayout]. The purpose of this class is for tools such as Flipper's UI inspector to be
 * able to easily visualize a component hierarchy without worrying about implementation details of
 * Litho.
 */
class DebugComponent
private constructor(
    val globalKey: String,
    private val result: LithoLayoutResult,
    private val node: LithoNode,
    private val componentIndex: Int,
    private val xOffset: Int,
    private val yOffset: Int,
    val componentTreeTimeMachine: ComponentTreeTimeMachine?,
) {

  interface Overrider {
    fun applyComponentOverrides(key: String, component: Component)

    fun applyStateOverrides(key: String, state: StateContainer)

    fun applyLayoutOverrides(key: String, node: DebugLayoutNodeEditor)
  }

  var isRoot = false
    get() = componentIndex == 0 && field
    private set

  /** @return the [ComponentContext] for this component. */
  val context: ComponentContext
    get() = result.context

  /** @return The litho view hosting this component. */
  val lithoView: LithoView?
    get() = result.context?.mountedView as LithoView?

  /** @return If this debug component represents a layout node, return it. */
  val layoutNode: DebugLayoutNode?
    get() = if (isLayoutNode) DebugLayoutNode(result) else null

  /** @return True if this not has layout information attached to it (backed by a Yoga node) */
  val isLayoutNode: Boolean
    get() = componentIndex == 0

  /** @return This component's testKey or null if none is set. */
  val testKey: String?
    get() = if (isLayoutNode) node.testKey else null

  /**
   * Returns this component's testKey or null if none is set.
   *
   * Unlike [testKey], this function can return a test key set on any Component, including container
   * Components which resolve into LayoutNodes.
   *
   * Unlike [testKey], this function can also return test keys set on individual Components even
   * when they are all resolved into a single InternalNode.
   */
  val componentTestKey: String?
    get() = node.getCommonPropsAt(componentIndex)?.testKey

  /**
   * @return This component's componentTag or null if none is set. Unlike [testKey], this will
   *   return tags for any Component, including Components which are not LayoutNodes.
   */
  val componentTag: Any?
    get() = node.getCommonPropsAt(componentIndex)?.componentTag

  /** @return This component's key or null if none is set. */
  val key: String?
    get() = component.run { if (hasManualKey()) key else null }

  /** @return The Component instance this debug component wraps. */
  val component: Component
    get() = node.getComponentAt(componentIndex)

  private val componentGlobalKey: String
    get() = node.getComponentContextAt(componentIndex).globalKey

  val stateContainer: StateContainer?
    get() = node.getComponentInfoAt(componentIndex).stateContainer

  private val xFromRoot: Int
    get() = result.x + xOffset

  private val yFromRoot: Int
    get() = result.y + yOffset

  private val isNotTailComponent: Boolean
    get() = componentIndex != 0

  fun setOverrider(overrider: Overrider) {
    overriders[globalKey] = overrider
  }

  /**
   * Get the list of components composed by this component. This will not include any [View]s that
   * are mounted by this component as those are not components. Use [this.getMountedView] for that.
   *
   * @return A list of child components.
   */
  val childComponents: List<DebugComponent>
    get() {
      return when {
        isNotTailComponent -> getImmediateDescendantAsChild()
        result is NestedTreeHolderResult -> {
          val nestedResult = result.nestedResult ?: return emptyList()
          if (nestedResult.mNode.componentCount == 1) {
            when (nestedResult.childCount) {
              0 -> return emptyList()
              else -> getChildren(nestedResult, xFromRoot, yFromRoot)
            }
          }
          val index = (nestedResult.node.componentCount - 2).coerceAtLeast(0)
          val component = getInstance(nestedResult, index, xFromRoot, yFromRoot, null)
          listOfNotNull(component)
        }
        else -> getChildren(result, xFromRoot, yFromRoot)
      }
    }

  private fun getImmediateDescendantAsChild(): List<DebugComponent> {
    val index = componentIndex - 1
    if (index < 0) {
      return emptyList()
    }
    val component = getInstance(result, index, xOffset, yOffset, null)
    return listOfNotNull(component)
  }

  /** @return A mounted view or null if this component does not mount a view. */
  val mountedView: View?
    get() = mountedContent as? View

  /** @return A mounted drawable or null if this component does not mount a drawable. */
  val mountedDrawable: Drawable?
    get() = mountedContent as? Drawable

  /** @return The bounds of this component relative to its hosting [LithoView]. */
  val boundsInLithoView: Rect
    get() {
      if (this.isRoot) {
        return Rect(0, 0, result.width, result.height)
      }
      val x = xFromRoot
      val y = yFromRoot
      return Rect(x, y, x + result.width, y + result.height)
    }

  /** @return The bounds of this component relative to its parent. */
  val bounds: Rect
    get() {
      val x = result.x
      val y = result.y
      return Rect(x, y, x + result.width, y + result.height)
    }
  /**
   * Many components can result in a single Lithonode/layout resut (from delegatation / custom
   * components) We want only the first component to 'take' the offset of the underlying layout node
   * other wise each delegating custom component will appear to multiply the offset.
   *
   * NestedTreeHolder nodes have to be handled differently. The head component on the
   * NestedTreeHolderResult will create a separate DebugComponent node but any margin that needs to
   * be added to its bounds will be applied on the nested result node, which is hosted on another
   * DebugComponent instance.
   */
  /**
   * The bounds of this component relative to its parent componen
   *
   * @return
   */
  val boundsInParentDebugComponent: Rect
    get() {
      /**
       * Many components can result in a single Lithonode/layout resut (from delegatation / custom
       * components) We want only the first component to 'take' the offset of the underlying layout
       * node other wise each delegating custom component will appear to multiply the offset.
       *
       * NestedTreeHolder nodes have to be handled differently. The head component on the
       * NestedTreeHolderResult will create a separate DebugComponent node but any margin that needs
       * to be added to its bounds will be applied on the nested result node, which is hosted on
       * another DebugComponent instance.
       */
      val isHeadComponent = componentIndex == node.componentCount - 1
      val nestedResult = (result as? NestedTreeHolderResult)?.nestedResult
      val xFromNestedResult = nestedResult?.x ?: 0
      val yFromNestedResult = nestedResult?.y ?: 0
      val x = if (isHeadComponent) result.x + xFromNestedResult else 0
      val y = if (isHeadComponent) result.y + yFromNestedResult else 0
      return Rect(x, y, x + result.width, y + result.height)
    }

  /** @return True if this and given debug components share the same internal node */
  fun isSameNode(other: DebugComponent): Boolean = node === other.node

  /**
   * @return A concatenated string of all text content within the underlying LithoView. Null if the
   *   node doesn't have an associated LithoView.
   */
  val allTextContent: String?
    get() = buildString {
      val mountDelegateTarget = lithoView?.mountDelegateTarget ?: return null
      for (i in 0 until mountDelegateTarget.mountItemCount) {
        val mountItem = mountDelegateTarget.getMountItemAt(i)
        val mountItemComponent = mountItem?.let { getRenderUnit(it).component }
        if (mountItemComponent != null) {
          val content = mountItem.content
          if (content is TextContent) {
            for (charSequence in content.textList) append(charSequence)
          } else if (content is TextView) {
            append(content.text)
          }
        }
      }
    }

  /**
   * @return The text content of the component wrapped by the debug component, or null if no
   *   TextContent/TextView are found.
   */
  val textContent: String?
    get() {
      val mountDelegateTarget = lithoView?.mountDelegateTarget ?: return null
      for (i in 0 until mountDelegateTarget.mountItemCount) {
        val mountItem = mountDelegateTarget.getMountItemAt(i)
        val mountItemComponent = mountItem?.let { getRenderUnit(it).component }
        if (mountItemComponent?.id == component.id) {
          val content = mountItem.content
          val sb = StringBuilder()
          if (content is TextContent) {
            for (charSequence in content.textList) sb.append(charSequence)
          } else if (content is TextView) {
            sb.append(content.text)
          }
          if (sb.isNotEmpty()) {
            return sb.toString()
          }
        }
      }
      return null
    }

  /** @return The [ComponentHost] that wraps this component or null if one cannot be found. */
  val componentHost: ComponentHost?
    get() {
      val mountDelegateTarget = lithoView?.mountDelegateTarget ?: return null
      for (i in 0 until mountDelegateTarget.mountItemCount) {
        val mountItem = mountDelegateTarget.getMountItemAt(i)
        val mountItemComponent = mountItem?.let { getRenderUnit(it).component }
        if (mountItemComponent?.isEquivalentTo(component) == true) {
          return mountItem.host as ComponentHost?
        }
      }
      return null
    }

  fun rerender() {
    lithoView?.forceRelayout()
  }

  fun canResolve(): Boolean {
    return component is SpecGeneratedComponent && (component as SpecGeneratedComponent).canResolve()
  }

  val mountedContent: Any?
    get() {
      if (!isLayoutNode) {
        return null
      }
      val mountDelegateTarget = lithoView?.mountDelegateTarget
      if (mountDelegateTarget != null) {
        for (i in 0 until mountDelegateTarget.mountItemCount) {
          val mountItem = mountDelegateTarget.getMountItemAt(i)
          val component = mountItem?.let { getRenderUnit(it).component }
          if (component != null && component === node.tailComponent) {
            return mountItem.content
          }
        }
      }
      return null
    }

  companion object {
    private val overriders: MutableMap<String, Overrider> = HashMap()

    @JvmStatic
    @Synchronized
    fun getInstance(
        result: LithoLayoutResult,
        componentIndex: Int,
        xOffset: Int,
        yOffset: Int,
        componentTree: ComponentTree?
    ): DebugComponent? {
      val node = result.node
      val context = result.context
      if (componentIndex >= node.componentCount) {
        return null
      }
      val componentKey = node.getGlobalKeyAt(componentIndex)
      return DebugComponent(
              componentTreeTimeMachine = componentTree?.timeMachine,
              globalKey = generateGlobalKey(context, componentKey),
              result = result,
              node = result.node,
              componentIndex = componentIndex,
              xOffset = xOffset,
              yOffset = yOffset,
          )
          .also { node.registerDebugComponent(it) }
    }

    @JvmStatic
    fun getRootInstance(view: LithoView): DebugComponent? = getRootInstance(view.componentTree)

    @JvmStatic
    fun getRootInstance(componentTree: ComponentTree?): DebugComponent? {
      val layoutState = componentTree?.mainThreadLayoutState
      val root = layoutState?.rootLayoutResult ?: return null
      val node = root.node
      val outerWrapperComponentIndex = (node.componentCount - 1).coerceAtLeast(0)
      return getInstance(root, outerWrapperComponentIndex, 0, 0, componentTree)?.apply {
        isRoot = true
      }
    }

    @JvmStatic
    fun getInstance(result: LithoLayoutResult): DebugComponent? {
      val rootNode = result.node
      val outerWrapperComponentIndex = (rootNode.componentCount - 1).coerceAtLeast(0)
      return getInstance(result, outerWrapperComponentIndex, 0, 0, null)
    }

    @JvmStatic
    fun getRenderUnit(
        debugComponent: DebugComponent,
        componentTree: ComponentTree
    ): RenderUnit<*>? {
      val component = debugComponent.component
      val layoutState = componentTree.mainThreadLayoutState ?: return null
      for (i in 0 until layoutState.mountableOutputCount) {
        val renderTreeNode = layoutState.getMountableOutputAt(i)
        val lithoRenderUnit = renderTreeNode.renderUnit as LithoRenderUnit
        if (lithoRenderUnit.componentContext?.componentScope === component) {
          return lithoRenderUnit
        }
      }
      return null
    }

    @JvmStatic
    fun getVisibilityOutput(
        debugComponent: DebugComponent,
        componentTree: ComponentTree
    ): VisibilityOutput? {
      val componentGlobalKey = debugComponent.componentGlobalKey
      val layoutState = componentTree.mainThreadLayoutState ?: return null
      for (i in 0 until layoutState.visibilityOutputCount) {
        val visibilityOutput = layoutState.getVisibilityOutputAt(i)
        if (visibilityOutput.id == componentGlobalKey) {
          return visibilityOutput
        }
      }
      return null
    }

    @JvmStatic
    fun isVisible(debugComponent: DebugComponent, lithoView: LithoView): Boolean {
      val componentGlobalKey = debugComponent.componentGlobalKey
      val visibilityState = lithoView.visibilityExtensionState
      return VisibilityMountExtension.isVisible(visibilityState, componentGlobalKey)
    }

    private fun generateGlobalKey(context: ComponentContext, componentKey: String): String =
        generateGlobalKey(context.lithoTree?.id, componentKey)

    @JvmStatic
    fun generateGlobalKey(treeId: Int?, globalKey: String): String =
        "${treeId?: "notree"}:${globalKey}"

    @JvmStatic
    fun applyOverrides(context: ComponentContext, component: Component, componentKey: String) {
      val key = generateGlobalKey(context, componentKey)
      val overrider = overriders[key]
      if (overrider != null) {
        overrider.applyComponentOverrides(key, component)
        val stateContainer = context.scopedComponentInfo.stateContainer
        if (stateContainer != null) {
          overrider.applyStateOverrides(key, stateContainer)
        }
      }
    }

    @JvmStatic
    fun applyOverrides(context: ComponentContext, node: LithoNode) {
      if (node.componentCount == 0) {
        return
      }
      val componentKey = node.getGlobalKeyAt(0)
      val key = generateGlobalKey(context, componentKey)
      val overrider = overriders[key]
      overrider?.applyLayoutOverrides(key, DebugLayoutNodeEditor(node))
    }

    private fun getChildren(result: LithoLayoutResult, x: Int, y: Int) = buildList {
      for (i in 0 until result.childCount) {
        val childNode = result.getChildAt(i)
        val index = (childNode.node.componentCount - 1).coerceAtLeast(0)
        getInstance(childNode, index, x, y, null)?.let { add(it) }
      }
    }
  }
}
