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

package com.facebook.rendercore

import android.content.Context
import android.util.Pair
import android.view.View
import androidx.collection.LongSparseArray
import com.facebook.rendercore.debug.DebugEvent
import com.facebook.rendercore.debug.DebugEventAttribute.Bounds
import com.facebook.rendercore.debug.DebugEventAttribute.Description
import com.facebook.rendercore.debug.DebugEventAttribute.HashCode
import com.facebook.rendercore.debug.DebugEventAttribute.Key
import com.facebook.rendercore.debug.DebugEventAttribute.Name
import com.facebook.rendercore.debug.DebugEventAttribute.NumMountableOutputs
import com.facebook.rendercore.debug.DebugEventAttribute.RenderUnitId
import com.facebook.rendercore.debug.DebugEventAttribute.RootHostHashCode
import com.facebook.rendercore.debug.DebugEventDispatcher
import com.facebook.rendercore.extensions.ExtensionState
import com.facebook.rendercore.extensions.MountExtension
import com.facebook.rendercore.extensions.RenderCoreExtension
import com.facebook.rendercore.extensions.RenderCoreExtension.Companion.shouldUpdate
import com.facebook.rendercore.utils.BoundsUtils
import com.facebook.rendercore.utils.CommonUtils
import com.facebook.rendercore.utils.isEqualOrEquivalentTo
import java.util.ArrayList
import java.util.HashMap

open class MountState
@JvmOverloads
constructor(
    private val _rootHost: Host,
    private val tracer: Systracer = RenderCoreSystrace.getInstance()
) : MountDelegateTarget {

  protected val idToMountedItemMap: LongSparseArray<MountItem?> = LongSparseArray()
  private val context: Context = _rootHost.context
  private var isMounting = false
  private var _needsRemount = false
  var renderTree: RenderTree? = null
    private set

  private var _mountDelegate: MountDelegate? = null
  private var unmountDelegateExtension: UnmountDelegateExtension<Any>? = null
  private var ensureParentMounted = true

  fun setEnsureParentMounted(ensureParentMounted: Boolean) {
    this.ensureParentMounted = ensureParentMounted
  }

  protected open fun createMountItem(renderTreeNode: RenderTreeNode, content: Any): MountItem {
    return MountItem(renderTreeNode, content)
  }

  /**
   * True if we have manually unmounted content (e.g. via unmountAllItems) which means that while we
   * may not have a new RenderTree, the mounted content does not match what the viewport for the
   * LithoView may be.
   */
  override fun needsRemount(): Boolean = _needsRemount

  override fun notifyMount(id: Long) {
    if (idToMountedItemMap[id] != null) {
      return
    }
    val position = checkNotNull(renderTree).getRenderTreeNodeIndex(id)
    val node = checkNotNull(renderTree).getRenderTreeNodeAtIndex(position)
    mountRenderUnit(node)
  }

  override fun notifyUnmount(id: Long) {
    unmountItemRecursively(id)
  }

  /**
   * Mount the layoutState on the pre-set HostView.
   *
   * @param renderTree a new [RenderTree] to mount
   */
  override fun mount(renderTree: RenderTree?) {
    checkNotNull(renderTree) { "Trying to mount a null RenderTreeNode" }

    val traceIdentifier = DebugEventDispatcher.generateTraceIdentifier(DebugEvent.RenderTreeMounted)
    if (traceIdentifier != null) {
      val attributes = HashMap<String, Any?>()
      attributes[RootHostHashCode] = _rootHost.hashCode()
      attributes[NumMountableOutputs] = renderTree.mountableOutputCount

      DebugEventDispatcher.beginTrace(
          traceIdentifier,
          DebugEvent.RenderTreeMounted,
          renderTree.renderStateId.toString(),
          attributes)
    }

    DebugEventDispatcher.dispatch(
        DebugEvent.RenderTreeMountStart,
        { renderTree.renderStateId.toString() },
        { attrs: MutableMap<String, Any?> ->
          attrs[RootHostHashCode] = _rootHost.hashCode()
          attrs[NumMountableOutputs] = renderTree.mountableOutputCount
        })

    try {
      check(!isMounting) { "Trying to mount while already mounting!" }

      isMounting = true
      val previousRenderTree = this.renderTree
      if (!updateRenderTree(renderTree)) {
        return
      }

      checkNotNull(this.renderTree)

      val isTracing = tracer.isTracing()
      val hostHierarchyIdentifier = _rootHost.hostHierarchyMountStateIdentifier
      if (isTracing) {
        tracer.beginSection(
            "MountState.mount${if(hostHierarchyIdentifier.isNullOrBlank()) "" else "[$hostHierarchyIdentifier]"}")
        tracer.beginSection("RenderCoreExtension.beforeMount")
      }

      RenderCoreExtension.beforeMount(_rootHost, _mountDelegate, renderTree.extensionResults)
      if (isTracing) {
        tracer.endSection()
        tracer.beginSection("MountState.prepareMount")
      }

      prepareMount(previousRenderTree)
      if (isTracing) {
        tracer.endSection()
      }

      // Starting from 1 as the RenderTreeNode in position 0 always represents the root which
      // is handled in prepareMount()
      mountItemsInternal(renderTree)

      _needsRemount = false

      if (isTracing) {
        tracer.endSection()
        tracer.beginSection("RenderCoreExtension.afterMount")
      }

      RenderCoreExtension.afterMount(_mountDelegate)
      if (isTracing) {
        tracer.endSection()
      }

      isMounting = false

      _mountDelegate?.let { mountDelegate ->
        if (isTracing) {
          tracer.beginSection("MountState.onRenderTreeUpdated")
        }
        mountDelegate.renderTreeUpdated((_rootHost as RenderCoreExtensionHost))
        if (isTracing) {
          tracer.endSection()
        }
      }
    } catch (e: Exception) {
      ErrorReporter.report(
          LogLevel.ERROR,
          "MountState:Exception",
          "Exception while mounting: ${e.message}",
          e,
          0,
          null)
      CommonUtils.rethrow(e)
    } finally {
      traceIdentifier?.let { DebugEventDispatcher.endTrace(it) }
      isMounting = false
      DebugEventDispatcher.dispatch(
          DebugEvent.RenderTreeMountEnd,
          { renderTree.renderStateId.toString() },
          { attrs: MutableMap<String, Any?> ->
            attrs[RootHostHashCode] = _rootHost.hashCode()
            attrs[NumMountableOutputs] = renderTree.mountableOutputCount
          })
    }
  }

  protected open fun mountItemsInternal(renderTree: RenderTree) {
    if (RenderCoreConfig.processFromLeafNode) {
      for (i in (renderTree.mountableOutputCount - 1) downTo 1) {
        updateMountItem(renderTree, i)
      }
    } else {
      for (i in 1 until renderTree.mountableOutputCount) {
        updateMountItem(renderTree, i)
      }
    }
  }

  override fun unmountAllItems() {
    try {
      _rootHost.setInLayout()
      if (renderTree == null) {
        unregisterAllExtensions()
        return
      }
      val isTracing = tracer.isTracing()
      if (isTracing) {
        tracer.beginSection("MountState.unmountAllItems")
      }

      // unmount all the content from the Root node
      unmountItemRecursively(ROOT_HOST_ID)

      unregisterAllExtensions()

      if (isTracing) {
        tracer.endSection()
      }

      _needsRemount = true
      renderTree = null
    } finally {
      _rootHost.unsetInLayout()
    }
  }

  override fun isRootItem(position: Int): Boolean =
      renderTree?.let { renderTree ->
        if (position >= renderTree.mountableOutputCount) {
          return false
        }
        val renderUnit = renderTree.getRenderTreeNodeAtIndex(position).renderUnit
        val mountItem = idToMountedItemMap[renderUnit.id] ?: return false
        return mountItem === idToMountedItemMap[ROOT_HOST_ID]
      } ?: false

  override fun getRootHost(): Host = _rootHost

  override fun getContentAt(position: Int): Any? =
      renderTree?.let { renderTree ->
        if (position >= renderTree.mountableOutputCount) {
          return null
        }
        val mountItem =
            idToMountedItemMap[renderTree.getRenderTreeNodeAtIndex(position).renderUnit.id]
                ?: return null
        return mountItem.content
      }

  override fun getContentById(id: Long): Any? = idToMountedItemMap[id]?.content

  @Deprecated("Only used for Litho's integration. Marked for removal.")
  override fun <Input, State> registerMountExtension(
      mountExtension: MountExtension<Input, State>
  ): ExtensionState<State> {
    val mountDelegate = _mountDelegate ?: MountDelegate(this, tracer).also { _mountDelegate = it }
    return mountDelegate.registerMountExtension(mountExtension) as ExtensionState<State>
  }

  override fun getHosts(): ArrayList<Host> {
    val hosts = ArrayList<Host>()
    for (i in 0 until idToMountedItemMap.size()) {
      val item = checkNotNull(idToMountedItemMap.valueAt(i))
      val content = item.content
      if (content is Host) {
        hosts.add(content)
      }
    }
    return hosts
  }

  override fun getMountItemAt(position: Int): MountItem? =
      renderTree?.let { idToMountedItemMap[it.getRenderTreeNodeAtIndex(position).renderUnit.id] }

  override fun getMountItemCount(): Int = idToMountedItemMap.size()

  override fun getRenderUnitCount(): Int = renderTree?.mountableOutputCount ?: 0

  override fun setUnmountDelegateExtension(unmountDelegateExtension: UnmountDelegateExtension<*>) {
    this.unmountDelegateExtension = unmountDelegateExtension as UnmountDelegateExtension<Any>
  }

  override fun removeUnmountDelegateExtension() {
    unmountDelegateExtension = null
  }

  override fun getMountDelegate(): MountDelegate? = _mountDelegate

  override fun getRenderStateId(): Int = renderTree?.renderStateId ?: -1

  /**
   * This is called when the [MountItem]s mounted on this [MountState] need to be re-bound with the
   * same RenderUnit. This happens when a detach/attach happens on the root [Host] that owns the
   * MountState.
   */
  override fun attach() {
    renderTree?.let { renderTree ->
      val isTracing = tracer.isTracing()
      if (isTracing) {
        tracer.beginSection("MountState.bind")
      }
      for (i in 0 until renderTree.mountableOutputCount) {
        val renderUnit = renderTree.getRenderTreeNodeAtIndex(i).renderUnit
        val mountItem = idToMountedItemMap[renderUnit.id]
        if (mountItem == null || mountItem.isBound) {
          continue
        }
        val content = mountItem.content
        bindRenderUnitToContent(mountItem)
        if (content is View && content !is Host && content.isLayoutRequested) {
          BoundsUtils.applyBoundsToMountContent(mountItem.renderTreeNode, content, true, tracer)
        }
      }
      if (isTracing) {
        tracer.endSection()
      }
    }
  }

  /** Unbinds all the MountItems currently mounted on this MountState. */
  override fun detach() {
    renderTree?.let { renderTree ->
      val isTracing = tracer.isTracing()
      if (isTracing) {
        tracer.beginSection("MountState.unbind")
        tracer.beginSection("MountState.unbindAllContent")
      }
      for (i in 0 until renderTree.mountableOutputCount) {
        val renderUnit = renderTree.getRenderTreeNodeAtIndex(i).renderUnit
        val mountItem = idToMountedItemMap[renderUnit.id]
        if (mountItem == null || !mountItem.isBound) {
          continue
        }
        unbindRenderUnitFromContent(mountItem)
      }
      if (isTracing) {
        tracer.endSection()
        tracer.beginSection("MountState.unbindExtensions")
      }
      _mountDelegate?.unBind()
      if (isTracing) {
        tracer.endSection()
        tracer.endSection()
      }
    }
  }

  protected fun isMountable(renderTreeNode: RenderTreeNode, index: Int): Boolean =
      _mountDelegate?.maybeLockForMount(renderTreeNode, index) ?: true

  protected fun updateBoundsForMountedRenderTreeNode(
      renderTreeNode: RenderTreeNode,
      item: MountItem,
      mountDelegate: MountDelegate?
  ) {
    // MountState should never update the bounds of the top-level host as this
    // should be done by the ViewGroup containing the LithoView.
    if (renderTreeNode.renderUnit.id == ROOT_HOST_ID) {
      return
    }
    val content = item.content
    val forceTraversal = content is View && content.isLayoutRequested
    val changed =
        BoundsUtils.applyBoundsToMountContent(
            item.renderTreeNode,
            item.content,
            forceTraversal /* force */,
            tracer,
        )
    mountDelegate?.onBoundsAppliedToItem(
        renderTreeNode,
        item.content,
        changed,
        tracer,
    )
  }

  /** Mount, unmount, or update each [MountItem] in the render tree. */
  private fun updateMountItem(renderTree: RenderTree, index: Int) {
    val renderTreeNode = renderTree.getRenderTreeNodeAtIndex(index)
    val isMountable = isMountable(renderTreeNode, index)
    val currentMountItem = idToMountedItemMap[renderTreeNode.renderUnit.id]

    if (currentMountItem != null) {
      if (isMountable) {
        updateMountItemIfNeeded(renderTreeNode, currentMountItem)
      } else {
        unmountItemRecursively(currentMountItem.renderTreeNode.renderUnit.id)
      }
    } else {
      if (isMountable) {
        mountRenderUnit(renderTreeNode)
      }
    }
  }

  fun willRemountWith(renderTree: RenderTree): Boolean {
    return this.renderTree != renderTree || _needsRemount
  }

  /** Updates the extensions of this [MountState] from the new [RenderTree]. */
  private fun updateRenderTree(renderTree: RenderTree): Boolean {
    // If the trees are same or if no remount is required, then no update is required.
    if (!willRemountWith(renderTree)) {
      return false
    }

    val currentRenderTree = this.renderTree
    // If the extensions have changed, un-register the current and register the new extensions.
    if (currentRenderTree == null || _needsRemount) {
      addExtensions(renderTree.extensionResults)
    } else if (shouldUpdate(currentRenderTree.extensionResults, renderTree.extensionResults)) {
      unregisterAllExtensions()
      addExtensions(renderTree.extensionResults)
    }

    // Update the current render tree.
    this.renderTree = renderTree
    return true
  }

  /**
   * Prepare the [MountState] to mount a new [RenderTree].
   *
   * @param previousRenderTree
   */
  private fun prepareMount(previousRenderTree: RenderTree?) {
    unmountOrMoveOldItems(previousRenderTree)

    val rootItem = idToMountedItemMap[ROOT_HOST_ID]
    val rootNode = checkNotNull(renderTree).getRenderTreeNodeAtIndex(0)

    // If root mount item is null then mounting root node for the first time.
    if (rootItem == null) {
      mountRootItem(rootNode)
    } else {
      // If root mount item is present then update it.
      updateMountItemIfNeeded(rootNode, rootItem)
    }
  }

  /**
   * Go over all the mounted items from the leaves to the root and unmount only the items that are
   * not present in the new LayoutOutputs. If an item is still present but in a new position move
   * the item inside its host. The condition where an item changed host doesn't need any special
   * treatment here since we mark them as removed and re-added when calculating the new
   * LayoutOutputs
   */
  private fun unmountOrMoveOldItems(previousRenderTree: RenderTree?) {
    val currentRenderTree = renderTree
    if (currentRenderTree == null || previousRenderTree == null) {
      return
    }
    val isTracing = tracer.isTracing()
    if (isTracing) {
      tracer.beginSection("unmountOrMoveOldItems")
    }

    // Traversing from the beginning since mRenderUnitIds unmounting won't remove entries there
    // but only from mIndexToMountedItemMap. If an host changes we're going to unmount it and
    // recursively all its mounted children.
    for (i in 1 until previousRenderTree.mountableOutputCount) {
      val previousRenderUnit = previousRenderTree.getRenderTreeNodeAtIndex(i).renderUnit
      val newPosition = currentRenderTree.getRenderTreeNodeIndex(previousRenderUnit.id)
      val oldItem = idToMountedItemMap[previousRenderUnit.id]

      // if oldItem is null it was previously unmounted so there is nothing we need to do.
      if (oldItem == null) {
        continue
      }

      val hasUnmountDelegate =
          unmountDelegateExtension?.shouldDelegateUnmount(
              checkNotNull(checkNotNull(_mountDelegate).unmountDelegateExtensionState), oldItem)
              ?: false
      if (hasUnmountDelegate) {
        continue
      }
      if (newPosition < 0) {
        unmountItemRecursively(oldItem.renderTreeNode.renderUnit.id)
      } else {
        val renderTreeNode = checkNotNull(currentRenderTree.getRenderTreeNodeAtIndex(newPosition))
        val newHostMarker = checkNotNull(renderTreeNode.parent).renderUnit.id
        val hostItem = idToMountedItemMap[newHostMarker]
        val newHost = if (hostItem != null) hostItem.content as Host else null
        if (oldItem.host == null || oldItem.host !== newHost) {
          // If the id is the same but the parent host is different we simply unmount the item and
          // re-mount it later. If the item to unmount is a ComponentHost, all the children will be
          // recursively unmounted.
          unmountItemRecursively(oldItem.renderTreeNode.renderUnit.id)
        } else if (oldItem.renderTreeNode.positionInParent != renderTreeNode.positionInParent) {
          // If a MountItem for this id exists and its Host has not changed but its position
          // in the Host has changed we need to update the position in the Host to ensure
          // the z-ordering.
          checkNotNull(oldItem.host)
              .moveItem(
                  oldItem, oldItem.renderTreeNode.positionInParent, renderTreeNode.positionInParent)
        }
      }
    }
    if (isTracing) {
      tracer.endSection()
    }
  }

  // The content might be null because it's the LayoutSpec for the root host
  // (the very first RenderTreeNode).
  private fun mountContentInHost(item: MountItem, host: Host, node: RenderTreeNode) {
    // Create and keep a MountItem even for the layoutSpec with null content
    // that sets the root host interactions.
    idToMountedItemMap.put(node.renderUnit.id, item)
    host.mount(node.positionInParent, item)
  }

  private fun isMounted(id: Long): Boolean = idToMountedItemMap[id] != null

  protected open fun mountRenderUnit(renderTreeNode: RenderTreeNode) {
    if (renderTreeNode.renderUnit.id == ROOT_HOST_ID) {
      mountRootItem(renderTreeNode)
      return
    }

    DebugEventDispatcher.trace(
        type = DebugEvent.RenderUnitMounted,
        renderStateId = { checkNotNull(renderTree).renderStateId.toString() },
        attributesAccumulator = { attributes ->
          attributes[RenderUnitId] = renderTreeNode.renderUnit.id
          attributes[Name] = renderTreeNode.renderUnit.description
          attributes[Bounds] = renderTreeNode.bounds
          attributes[RootHostHashCode] = _rootHost.hashCode()
          attributes[Key] = renderTreeNode.renderUnit.debugKey
        },
    ) {
      val isTracing = tracer.isTracing()
      if (isTracing) {
        tracer.beginSection("MountItem: ${renderTreeNode.renderUnit.description}")
      }

      // 1. Resolve the correct host to mount our content to.
      val hostTreeNode = checkNotNull(renderTreeNode.parent)
      val parentRenderUnit = hostTreeNode.renderUnit
      val renderUnit: RenderUnit<Any> = renderTreeNode.renderUnit as RenderUnit<Any>

      // 2. Ensure render tree node's parent is mounted or throw exception depending on the
      // ensure-parent-mounted flag.
      if (isTracing) {
        tracer.beginSection("MountItem:mount-parent ${parentRenderUnit.description}")
      }
      maybeEnsureParentIsMounted(hostTreeNode, parentRenderUnit)
      if (isTracing) {
        tracer.endSection()
      }
      val mountItem = checkNotNull(idToMountedItemMap[parentRenderUnit.id])
      val parentContent = mountItem.content
      assertParentContentType(parentContent, renderUnit, parentRenderUnit)
      val host = parentContent as Host

      // 3. call the RenderUnit's Mount bindings.
      if (isTracing) {
        tracer.beginSection("MountItem:acquire-content ${renderUnit.description}")
      }
      val content = renderUnit.contentAllocator.acquireContent(context, renderTreeNode.poolScope)
      if (isTracing) {
        tracer.endSection()
      }
      _mountDelegate?.startNotifyVisibleBoundsChangedSection()
      if (isTracing) {
        tracer.beginSection("MountItem:mount ${renderTreeNode.renderUnit.description}")
      }
      val item = createMountItem(renderTreeNode, content)
      mountRenderUnitToContent(renderTreeNode, renderUnit, content, item.bindData)

      // 4. Mount the content into the selected host.
      mountContentInHost(item, host, renderTreeNode)
      if (isTracing) {
        tracer.endSection()
        tracer.beginSection("MountItem:bind ${renderTreeNode.renderUnit.description}")
      }

      // 5. Call attach binding functions
      bindRenderUnitToContent(item)
      if (isTracing) {
        tracer.endSection()
        tracer.beginSection("MountItem:applyBounds ${renderTreeNode.renderUnit.description}")
      }

      // 6. Apply the bounds to the Mount content now. It's important to do so after bind as calling
      // bind might have triggered a layout request within a View.
      val changed =
          BoundsUtils.applyBoundsToMountContent(
              renderTreeNode,
              item.content,
              true /* force */,
              tracer,
          )
      if (isTracing) {
        tracer.endSection()
        tracer.beginSection("MountItem:after ${renderTreeNode.renderUnit.description}")
      }

      _mountDelegate?.onBoundsAppliedToItem(
          renderTreeNode,
          item.content,
          changed,
          tracer,
      )
      _mountDelegate?.endNotifyVisibleBoundsChangedSection()

      if (isTracing) {
        tracer.endSection()
        tracer.endSection()
      }
    }
  }

  protected fun unmountItemRecursively(id: Long) {
    val item = idToMountedItemMap[id] ?: return // Already has been unmounted.

    // When unmounting use the render unit from the MountItem
    val isTracing = tracer.isTracing()
    val node = item.renderTreeNode
    val unit = item.renderUnit as RenderUnit<Any>
    val content = item.content
    val hasUnmountDelegate =
        unmountDelegateExtension?.shouldDelegateUnmount(
            checkNotNull(checkNotNull(_mountDelegate).unmountDelegateExtensionState), item) ?: false
    val traceIdentifier =
        DebugEventDispatcher.generateTraceIdentifier(DebugEvent.RenderUnitUnmounted)
    if (traceIdentifier != null) {
      val attributes = HashMap<String, Any?>()
      attributes[RenderUnitId] = id
      attributes[Description] = unit.description
      attributes[Bounds] = node.bounds
      attributes[RootHostHashCode] = _rootHost.hashCode()
      attributes[Key] = unit.debugKey
      DebugEventDispatcher.beginTrace(
          traceIdentifier,
          DebugEvent.RenderUnitUnmounted,
          checkNotNull(renderTree).renderStateId.toString(),
          attributes)
    }
    if (isTracing) {
      tracer.beginSection("UnmountItem: ${unit.description}")
    }

    /* Recursively unmount mounted children items.
     * This is the case when mountDiffing is enabled and unmountOrMoveOldItems() has a matching
     * sub tree. However, traversing the tree bottom-up, it needs to unmount a node holding that
     * sub tree, that will still have mounted items. (Different sequence number on RenderTreeNode id)
     */
    if (node.childrenCount > 0) {

      // unmount all children
      for (i in node.childrenCount - 1 downTo 0) {
        unmountItemRecursively(node.getChildAt(i).renderUnit.id)
      }

      // check if all items are unmount from the host
      check(!(!hasUnmountDelegate && (content as Host).mountItemCount > 0)) {
        ("Recursively unmounting items from a ComponentHost, left some items behind maybe because not tracked by its MountState")
      }
    }

    // The root host item cannot be unmounted as it's a reference
    // to the top-level Host, and it is not mounted in a host.
    if (unit.id == ROOT_HOST_ID) {
      unmountRootItem()
      if (isTracing) {
        tracer.endSection()
      }
      traceIdentifier?.let { DebugEventDispatcher.endTrace(it) }
      return
    } else {
      idToMountedItemMap.remove(unit.id)
    }

    val host = item.host
    if (hasUnmountDelegate) {
      checkNotNull(unmountDelegateExtension)
          .unmount(
              checkNotNull(checkNotNull(_mountDelegate).unmountDelegateExtensionState), item, host)
    } else {
      if (isTracing) {
        tracer.beginSection("UnmountItem:remove: ${unit.description}")
      }
      // We don't expect Host to really be null but we observe cases where this
      // is actually happening
      host?.unmount(item)
      if (isTracing) {
        tracer.endSection()
      }

      if (item.isBound) {
        if (isTracing) {
          tracer.beginSection("UnmountItem:unbind: ${unit.description}")
        }
        unbindRenderUnitFromContent(item)
        if (isTracing) {
          tracer.endSection()
        }
      }

      if (content is View) {
        content.setPadding(0, 0, 0, 0)
      }

      if (isTracing) {
        tracer.beginSection("UnmountItem:unmount: ${unit.description}")
      }
      unmountRenderUnitFromContent(node, unit, content, item.bindData)
      if (isTracing) {
        tracer.endSection()
      }
      item.releaseMountContent(context)
    }
    if (isTracing) {
      tracer.endSection()
    }
    traceIdentifier?.let { DebugEventDispatcher.endTrace(it) }
  }

  /**
   * Since the root item is not itself mounted on a host, its unmount method is encapsulated into a
   * different method.
   */
  private fun unmountRootItem() {
    val item = idToMountedItemMap[ROOT_HOST_ID]
    if (item != null) {
      if (item.isBound) {
        unbindRenderUnitFromContent(item)
      }
      idToMountedItemMap.remove(ROOT_HOST_ID)
      val rootRenderTreeNode = checkNotNull(renderTree).root
      unmountRenderUnitFromContent(
          rootRenderTreeNode,
          rootRenderTreeNode.renderUnit as RenderUnit<Any>,
          item.content,
          item.bindData)
    }
  }

  private fun mountRootItem(rootNode: RenderTreeNode) {
    // Create root mount item.
    val item = createMountItem(rootNode, _rootHost)

    // Run mount callbacks.
    mountRenderUnitToContent(
        rootNode, rootNode.renderUnit as RenderUnit<Any>, _rootHost, item.bindData)

    // Adds root mount item to map.
    idToMountedItemMap.put(ROOT_HOST_ID, item)

    // Run binder callbacks
    bindRenderUnitToContent(item)
  }

  override fun unbindMountItem(mountItem: MountItem) {
    if (mountItem.isBound) {
      unbindRenderUnitFromContent(mountItem)
    }
    val content = mountItem.content
    if (content is View) {
      content.setPadding(0, 0, 0, 0)
    }
    unmountRenderUnitFromContent(
        mountItem.renderTreeNode,
        mountItem.renderTreeNode.renderUnit as RenderUnit<Any>,
        content,
        mountItem.bindData)
    mountItem.releaseMountContent(context)
  }

  fun setRenderTreeUpdateListener(listener: RenderTreeUpdateListener?) {
    _mountDelegate =
        (_mountDelegate ?: MountDelegate(this, tracer)).also { it.setMountStateListener(listener) }
  }

  private fun addExtensions(extensions: List<Pair<RenderCoreExtension<*, *>, Any>>?) {
    if (extensions != null) {
      _mountDelegate =
          (_mountDelegate ?: MountDelegate(this, tracer)).also { it.registerExtensions(extensions) }
    }
  }

  override fun unregisterAllExtensions() {
    _mountDelegate?.let { mountDelegate ->
      mountDelegate.unBind()
      mountDelegate.unMount()
      mountDelegate.unregisterAllExtensions()
      mountDelegate.releaseAllAcquiredReferences()
    }
  }

  private fun mountRenderUnitToContent(
      node: RenderTreeNode,
      unit: RenderUnit<Any>,
      content: Any,
      bindData: BindData
  ) {
    val traceIdentifier = DebugEventDispatcher.generateTraceIdentifier(DebugEvent.MountItemMount)
    if (traceIdentifier != null) {
      val attributes = HashMap<String, Any?>()
      attributes[RenderUnitId] = unit.id
      attributes[Description] = unit.description
      attributes[HashCode] = content.hashCode()
      attributes[Bounds] = node.bounds
      attributes[Key] = unit.debugKey
      DebugEventDispatcher.beginTrace(
          traceIdentifier,
          DebugEvent.MountItemMount,
          checkNotNull(renderTree).renderStateId.toString(),
          attributes)
    }
    unit.mountBinders(context, content, node.layoutData, bindData, tracer)
    _mountDelegate?.onMountItem(unit, content, node.layoutData, tracer)
    traceIdentifier?.let { DebugEventDispatcher.endTrace(it) }
  }

  private fun unmountRenderUnitFromContent(
      node: RenderTreeNode,
      unit: RenderUnit<Any>,
      content: Any,
      bindData: BindData
  ) {
    _mountDelegate?.onUnmountItem(unit, content, node.layoutData, tracer)
    unit.unmountBinders(context, content, node.layoutData, bindData, tracer)
  }

  protected fun bindRenderUnitToContent(item: MountItem) {
    val renderUnit = item.renderUnit as RenderUnit<Any>
    val content = item.content
    val layoutData = item.renderTreeNode.layoutData
    renderUnit.attachBinders(context, content, layoutData, item.bindData, tracer)
    _mountDelegate?.onBindItem(renderUnit, content, layoutData, tracer)
    item.isBound = true
  }

  private fun unbindRenderUnitFromContent(item: MountItem) {
    val renderUnit = item.renderUnit as RenderUnit<Any>
    val content = item.content
    val layoutData = item.renderTreeNode.layoutData
    _mountDelegate?.onUnbindItem(renderUnit, content, layoutData, tracer)
    renderUnit.detachBinders(context, content, layoutData, item.bindData, tracer)
    item.isBound = false
  }

  private fun updateMountItemIfNeeded(renderTreeNode: RenderTreeNode, currentMountItem: MountItem) {
    val mountDelegate = _mountDelegate
    val isTracing = tracer.isTracing()
    val renderUnit = renderTreeNode.renderUnit as RenderUnit<Any>
    val newLayoutData = renderTreeNode.layoutData
    val currentNode = currentMountItem.renderTreeNode
    val currentRenderUnit = currentNode.renderUnit as RenderUnit<Any>
    val currentLayoutData = currentNode.layoutData
    val content = currentMountItem.content

    // Re initialize the MountItem internal state with the new attributes from RenderTreeNode
    currentMountItem.update(renderTreeNode)
    currentRenderUnit.onStartUpdateRenderUnit()
    mountDelegate?.startNotifyVisibleBoundsChangedSection()
    if (shouldUpdateMountItem(currentRenderUnit, renderUnit, currentLayoutData, newLayoutData)) {
      val traceIdentifier =
          DebugEventDispatcher.generateTraceIdentifier(DebugEvent.RenderUnitUpdated)
      if (traceIdentifier != null) {
        val attributes = HashMap<String, Any?>()
        attributes[RenderUnitId] = renderTreeNode.renderUnit.id
        attributes[Description] = renderTreeNode.renderUnit.description
        attributes[Bounds] = renderTreeNode.bounds
        attributes[RootHostHashCode] = _rootHost.hashCode()
        attributes[Key] = renderTreeNode.renderUnit.debugKey
        DebugEventDispatcher.beginTrace(
            traceIdentifier,
            DebugEvent.RenderUnitUpdated,
            checkNotNull(renderTree).renderStateId.toString(),
            attributes)
      }
      if (isTracing) {
        tracer.beginSection("UpdateItem: ${renderUnit.description}")
      }
      updateRenderUnitBinders(
          content,
          renderUnit,
          currentRenderUnit,
          currentLayoutData,
          newLayoutData,
          currentMountItem)
      if (isTracing) {
        tracer.endSection()
      }
      traceIdentifier?.let { DebugEventDispatcher.endTrace(it) }
    } else if (!currentMountItem.isBound) {
      bindRenderUnitToContent(currentMountItem)
    }
    currentMountItem.isBound = true

    // Update the bounds of the mounted content. This needs to be done regardless of whether
    // the RenderUnit has been updated or not since the mounted item might might have the same
    // size and content but a different position.
    if (isTracing) {
      tracer.beginSection("UpdateBounds: ${renderUnit.description}")
    }
    updateBoundsForMountedRenderTreeNode(renderTreeNode, currentMountItem, mountDelegate)
    if (isTracing) {
      tracer.endSection()
    }
    mountDelegate?.endNotifyVisibleBoundsChangedSection()
    currentRenderUnit.onEndUpdateRenderUnit()
  }

  protected open fun shouldUpdateMountItem(
      currentRenderUnit: RenderUnit<*>,
      renderUnit: RenderUnit<*>,
      currentLayoutData: Any?,
      newLayoutData: Any?,
  ): Boolean {
    return currentRenderUnit !== renderUnit ||
        !isEqualOrEquivalentTo(currentLayoutData, newLayoutData)
  }

  protected open fun updateRenderUnitBinders(
      content: Any,
      renderUnit: RenderUnit<Any>,
      currentRenderUnit: RenderUnit<Any>,
      currentLayoutData: Any?,
      newLayoutData: Any?,
      currentMountItem: MountItem,
  ) {
    renderUnit.updateBinders(
        context,
        content,
        currentRenderUnit,
        currentLayoutData,
        newLayoutData,
        _mountDelegate,
        currentMountItem.bindData,
        currentMountItem.isBound,
        tracer)
  }

  private fun maybeEnsureParentIsMounted(node: RenderTreeNode, parent: RenderUnit<*>) {
    if (ensureParentMounted && !isMounted(parent.id)) {
      mountRenderUnit(node)
    }
  }

  fun needsRemount(needsRemount: Boolean) {
    _needsRemount = needsRemount
  }

  companion object {
    const val ROOT_HOST_ID: Long = 0L
    private const val TAG: String = "MountState"

    private fun assertParentContentType(
        parentContent: Any,
        renderUnit: RenderUnit<*>,
        parentRenderUnit: RenderUnit<*>
    ) {
      if (parentContent !is Host) {
        throw RuntimeException(
            """
            Trying to mount a RenderTreeNode, its parent should be a Host, but was '${parentContent.javaClass.simpleName}'.
            Parent RenderUnit: id=${parentRenderUnit.id}; poolKey='${parentRenderUnit.contentAllocator.getPoolKey()}'.
            Child RenderUnit: id=${renderUnit.id}; poolKey='${renderUnit.contentAllocator.getPoolKey()}'.
            """
                .trimIndent())
      }
    }
  }
}
