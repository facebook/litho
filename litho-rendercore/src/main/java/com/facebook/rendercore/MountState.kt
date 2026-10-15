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

@file:Suppress("UNCHECKED_CAST")

package com.facebook.rendercore

import android.util.Pair
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.collection.LongObjectMap
import androidx.collection.LongSet
import androidx.collection.MutableLongObjectMap
import androidx.collection.MutableLongSet
import androidx.collection.MutableScatterSet
import androidx.collection.ScatterSet
import androidx.collection.emptyScatterSet
import androidx.collection.mutableLongObjectMapOf
import androidx.collection.mutableLongSetOf
import androidx.collection.mutableScatterSetOf
import com.facebook.rendercore.debug.DebugEvent
import com.facebook.rendercore.debug.DebugEventAttribute.Bounds
import com.facebook.rendercore.debug.DebugEventAttribute.Description
import com.facebook.rendercore.debug.DebugEventAttribute.HashCode
import com.facebook.rendercore.debug.DebugEventAttribute.IsGapWorkerPremount
import com.facebook.rendercore.debug.DebugEventAttribute.Key
import com.facebook.rendercore.debug.DebugEventAttribute.Name
import com.facebook.rendercore.debug.DebugEventAttribute.NumMountableOutputs
import com.facebook.rendercore.debug.DebugEventAttribute.RenderUnitId
import com.facebook.rendercore.debug.DebugEventAttribute.RootHostHashCode
import com.facebook.rendercore.debug.DebugEventDispatcher
import com.facebook.rendercore.debug.formatMountStatsSummary
import com.facebook.rendercore.extensions.ExtensionState
import com.facebook.rendercore.extensions.MountExtension
import com.facebook.rendercore.extensions.RenderCoreExtension
import com.facebook.rendercore.extensions.RenderCoreExtension.Companion.shouldUpdate
import com.facebook.rendercore.extensions.VisibilityChangeReason
import com.facebook.rendercore.utils.BoundsUtils
import com.facebook.rendercore.utils.CommonUtils
import com.facebook.rendercore.utils.isEqualOrEquivalentTo
import com.meta.systrace.Systracer
import com.meta.systrace.trace

/**
 * Tracks mount operation statistics for debug logging, including per-component breakdown. This
 * enables detailed analysis of which components are being mounted, updated, reused, or remounted.
 */
@ExposedCopyVisibility
data class MountStats
internal constructor(
    var reusedCount: Int = 0,
    var updatedCount: Int = 0,
    var mountedCount: Int = 0, // Initial mounts (first time)
    var unmountedCount: Int = 0, // Tracked here for convenience
    var remountedCount: Int = 0, // Remounts (content destroyed and recreated)
    val summaryByComponent: MutableMap<String, ComponentMountStats> = mutableMapOf(),
) {
  @ExposedCopyVisibility
  data class ComponentMountStats
  internal constructor(
      var reused: Int = 0,
      var updated: Int = 0,
      var mounted: Int = 0, // Initial mounts
      var remounted: Int = 0, // Remounts (destroyed and recreated)
  )

  fun reset() {
    reusedCount = 0
    updatedCount = 0
    mountedCount = 0
    unmountedCount = 0
    remountedCount = 0
    summaryByComponent.clear()
  }

  fun recordReuse(componentName: String) {
    reusedCount++
    summaryByComponent.getOrPut(componentName) { ComponentMountStats() }.reused++
  }

  fun recordUpdate(componentName: String) {
    updatedCount++
    summaryByComponent.getOrPut(componentName) { ComponentMountStats() }.updated++
  }

  fun recordMount(componentName: String, isInitialMount: Boolean) {
    if (isInitialMount) {
      mountedCount++
      summaryByComponent.getOrPut(componentName) { ComponentMountStats() }.mounted++
    } else {
      remountedCount++
      summaryByComponent.getOrPut(componentName) { ComponentMountStats() }.remounted++
    }
  }

  fun recordUnmount() {
    unmountedCount++
  }
}

open class MountState
@JvmOverloads
constructor(
    private val _rootHost: Host,
    private val tracer: Systracer = RenderCoreSystrace.getInstance(),
    binderObserver: BinderObserver? = null,
    private val mountStateRestartPolicyProvider: () -> MountStateRestartPolicy = {
      MountStateRestartPolicy.Default
    },
    continuationValueReader: ContinuationValueReader = ContinuationValueReader { null },
) : MountDelegateTarget {

  protected val idToMountedItemMap: MutableLongObjectMap<MountItem> = mutableLongObjectMapOf()
  private val idToDirtyBindersMap: MutableLongObjectMap<MutableScatterSet<BinderId>> =
      mutableLongObjectMapOf()
  /** Ids of mount items with continuations that need to be restarted when mount skips */
  private val mountItemsWithContinuations: MutableLongSet = mutableLongSetOf()
  protected val mountContext: MountContext =
      MountContext(_rootHost.context, tracer, binderObserver, continuationValueReader)
  private val mountStateRestartPolicy: MountStateRestartPolicy
    get() = mountStateRestartPolicyProvider()

  private var isMounting = false
  private var _needsRemount = false
  var renderTree: RenderTree? = null
    private set

  // Mount statistics for debug logging (controlled by RenderCoreConfig)
  val mountStats: MountStats? by lazy {
    if (RenderCoreConfig.isDebugModeEnabled) MountStats() else null
  }

  /**
   * Reference to the render tree node currently being mounted or updated – this node's render unit
   * binders are being bound, updated, or its continuations are being restarted.
   */
  protected var currentUpdatingNode: RenderTreeNode? = null
    private set

  private var _mountDelegate: MountDelegate? = null
  private var unmountDelegateExtension: UnmountDelegateExtension<Any>? = null
  private var restartAttachResult: MountStateRestartRegistry.AttachResult? = null
  private var parentRestartRegistry: MountStateRestartRegistry? = null

  protected open fun createMountItem(renderTreeNode: RenderTreeNode, content: Any): MountItem {
    return MountItem(renderTreeNode, content)
  }

  /**
   * True if we have manually unmounted content (e.g. via unmountAllItems), which means the mounted
   * content may no longer match this mount state's render tree, or if registered
   * continuation/nested-mount-state work is pending this pass (see [hasPendingRestartWork]). A host
   * pinned only by clean restart registrations returns false, so callers do not have to force a
   * full mount pass just because restartable items exist.
   */
  override fun needsRemount(): Boolean =
      tracer.trace({ "MountState.needsRemount" }) { _needsRemount || hasPendingRestartWork() }

  /**
   * Whether this mount state needs to be woken through the restart registry this pass. For plain
   * RenderCore this means a render unit has a new continuation value or a nested mount state has
   * pending work. Subclasses with extra dirtiness info override to also account for it.
   */
  open fun hasPendingRestartWork(): Boolean {
    if (mountItemsWithContinuations.isEmpty()) {
      return false
    }
    return tracer.trace({ "MountState.hasPendingRestartWork" }) {
      mountItemsWithContinuations.forEach { id ->
        val mountItem = idToMountedItemMap[id]
        if (
            mountItem != null && mountItem.hasPendingRestartWork(mountContext.continuationContext)
        ) {
          return@trace true
        }
      }
      false
    }
  }

  override fun notifyMount(id: Long) {
    if (idToMountedItemMap[id] != null) {
      return
    }
    val position = checkNotNull(renderTree).getRenderTreeNodeIndex(id)
    val node = checkNotNull(renderTree).getRenderTreeNodeAtIndex(position)
    currentUpdatingNode = node
    mountRenderUnit(node)
    currentUpdatingNode = null
  }

  override fun notifyUnmount(id: Long) {
    unmountItemRecursively(id)
  }

  /**
   * Mount the layoutState on the pre-set HostView.
   *
   * @param renderTree a new [RenderTree] to mount
   */
  override fun mount(renderTree: RenderTree, extraBindersToUpdate: ScatterSet<BinderId>) {
    mount(renderTree, extraBindersToUpdate, VisibilityChangeReason.MOUNT)
  }

  /**
   * Mount the layoutState on the pre-set HostView with a visibility change reason.
   *
   * @param renderTree a new [RenderTree] to mount
   * @param reason the visibility change reason to propagate to visibility-aware extensions
   */
  fun mount(
      renderTree: RenderTree,
      extraBindersToUpdate: ScatterSet<BinderId>,
      reason: VisibilityChangeReason,
  ) {

    val traceIdentifier = DebugEventDispatcher.generateTraceIdentifier(DebugEvent.RenderTreeMounted)
    if (traceIdentifier != null) {
      val attributes = HashMap<String, Any?>()
      attributes[RootHostHashCode] = _rootHost.hashCode()
      attributes[NumMountableOutputs] = renderTree.mountableOutputCount

      DebugEventDispatcher.beginTrace(
          traceIdentifier,
          DebugEvent.RenderTreeMounted,
          renderTree.renderStateId.toString(),
          attributes,
      )
    }

    DebugEventDispatcher.dispatch(
        DebugEvent.RenderTreeMountStart,
        { renderTree.renderStateId.toString() },
        { attrs: MutableMap<String, Any?> ->
          attrs[RootHostHashCode] = _rootHost.hashCode()
          attrs[NumMountableOutputs] = renderTree.mountableOutputCount
        },
    )

    val hadContinuations = mountItemsWithContinuations.isNotEmpty()
    val isTracing = tracer.isTracing()
    try {
      check(!isMounting) { "Trying to mount while already mounting!" }

      val previousRenderTree = this.renderTree
      if (!updateRenderTree(renderTree) && extraBindersToUpdate.isEmpty()) {
        runAllContinuationsIfNeeded()
        updateHostContinuationRegistration(hadContinuations)
        return
      }

      isMounting = true
      // Reset mount statistics if enabled
      mountStats?.reset()
      idToDirtyBindersMap.clear()
      extraBindersToUpdate.forEach { binderId ->
        idToDirtyBindersMap.getOrPut(binderId.renderUnitId) { mutableScatterSetOf() }.add(binderId)
      }

      val hostHierarchyIdentifier = _rootHost.hostHierarchyMountStateIdentifier
      if (isTracing) {
        tracer.beginSection(
            "MountState.mount${if(hostHierarchyIdentifier.isNullOrBlank()) "" else "[$hostHierarchyIdentifier]"}",
        )
        tracer.beginSection("RenderCoreExtension.beforeMount")
      }

      RenderCoreExtension.beforeMount(_rootHost, _mountDelegate, renderTree.extensionResults)
      if (isTracing) {
        tracer.endSection()
      }

      prepareMount(previousRenderTree)

      // Starting from 1 as the RenderTreeNode in position 0 always represents the root which
      // is handled in prepareMount()
      mountItemsInternal(renderTree)

      _needsRemount = false

      if (isTracing) {
        tracer.endSection()
        tracer.beginSection("RenderCoreExtension.afterMount")
      }

      RenderCoreExtension.afterMount(_mountDelegate, reason)
      if (isTracing) {
        tracer.endSection()
      }

      updateHostContinuationRegistration(hadContinuations)

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
      )
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
            // Mount statistics for phase logging (only if enabled)
            mountStats?.let { stats ->
              attrs["mounted_count"] = stats.mountedCount
              attrs["unmounted_count"] = stats.unmountedCount
              attrs["updated_count"] = stats.updatedCount
              attrs["reused_count"] = stats.reusedCount
              attrs["remounted_count"] = stats.remountedCount
              attrs["summary_by_component"] = formatMountStatsSummary(stats.summaryByComponent)
              // Pool stats snapshot for observability
              val poolStats = MountContentPools.getAllPoolStats()
              if (poolStats.isNotEmpty()) {
                attrs["pool_stats"] = poolStats
              }
            }
          },
      )
    }
  }

  protected open fun mountItemsInternal(renderTree: RenderTree) {
    val mountDelegate = _mountDelegate
    val stats = mountStats
    val hasContinuations = mountItemsWithContinuations.isNotEmpty()
    if (RenderCoreConfig.processFromLeafNode) {
      for (i in (renderTree.mountableOutputCount - 1) downTo 1) {
        mountOrSkip(renderTree, i, mountDelegate, stats, hasContinuations)
      }
    } else {
      for (i in 1 until renderTree.mountableOutputCount) {
        mountOrSkip(renderTree, i, mountDelegate, stats, hasContinuations)
      }
    }
  }

  /**
   * Per-item mount entry point. When [RenderTreeNode.isFastPathEligible] is set (by the
   * subtree-cache splice path), skips [updateMountItemIfNeeded] for items where [canSkipUpdate]
   * returns true. Falls through to the slow path ([updateMountItem]) otherwise.
   *
   * `canSkipUpdate` is checked before `isMountable` to avoid double-dispatching `beforeMountItem`
   * on items that fail the predicate (since `updateMountItem` calls `isMountable` internally). When
   * the predicate passes but `isMountable` returns false, the item is unmounted directly to avoid
   * the same double-dispatch issue.
   */
  private fun mountOrSkip(
      renderTree: RenderTree,
      index: Int,
      mountDelegate: MountDelegate?,
      stats: MountStats?,
      hasContinuations: Boolean,
  ) {
    val node = renderTree.getRenderTreeNodeAtIndex(index)
    if (node.isFastPathEligible) {
      val renderUnitId = node.renderUnit.id
      val item = idToMountedItemMap[renderUnitId]
      if (item != null && canSkipUpdate(item, node)) {
        if (mountDelegate == null || mountDelegate.maybeLockForMount(node, index)) {
          currentUpdatingNode = node
          // Safe to skip onStartUpdateRenderUnit/onEndUpdateRenderUnit because we also skip
          // the shouldUpdate call they protect.
          item.update(node)
          // Clean up stale continuations entry (predicate guarantees none are active).
          if (hasContinuations && mountItemsWithContinuations.isNotEmpty()) {
            mountItemsWithContinuations.remove(renderUnitId)
          }
          // Bounds are value-equal per predicate, so changed=false.
          mountDelegate?.onBoundsAppliedToItem(node, item.content, false, tracer)
          stats?.recordReuse(node.renderUnit.description)
          currentUpdatingNode = null
          return
        } else {
          // Unmount directly — do not fall through to updateMountItem which would call
          // isMountable again.
          unmountItemRecursively(item.renderTreeNode.renderUnit.id)
          return
        }
      }
    }

    updateMountItem(renderTree, index)
  }

  /**
   * Returns `true` when [updateMountItemIfNeeded] would be a no-op for this item, meaning the fast
   * path can safely skip it. Subclasses may override to add additional guards.
   *
   * Checks (short-circuits on first failure):
   * - Item is bound and has no pending continuations
   * - No dirty binders and [shouldUpdateMountItem] returns false
   * - Bounds and padding are value-equal
   * - View content has no pending layout request
   *
   * Delegates RU/layoutData comparison to [shouldUpdateMountItem] so that subclass overrides (e.g.
   * BloksMountState) are automatically respected.
   */
  protected open fun canSkipUpdate(item: MountItem, node: RenderTreeNode): Boolean {
    if (!item.isBound) return false
    if (item.hasAnyContinuations()) return false
    val prev = item.renderTreeNode
    if (
        idToDirtyBindersMap.containsKey(node.renderUnit.id) ||
            shouldUpdateMountItem(
                prev.renderUnit,
                node.renderUnit,
                prev.layoutData,
                node.layoutData,
            )
    )
        return false
    if (prev !== node) {
      if (prev.bounds != node.bounds) return false
      if (prev.resolvedPadding != node.resolvedPadding) return false
    }
    val content = item.content
    if (content is View && content.isLayoutRequested) return false
    return true
  }

  private fun updateHostContinuationRegistration(hadContinuations: Boolean) {
    when (mountStateRestartPolicy) {
      // In the Always case, we always attach to the current host continuation registry
      MountStateRestartPolicy.Always -> attachToRestartRegistryIfNeeded()
      // In the Never case, we never attach to the host continuation registry. We unregister if we
      // are attached to a registry
      MountStateRestartPolicy.Never -> detachFromRestartRegistry()
      // In the Default case, if after performing mount, we find that we no longer have any mount
      // items with continuations, we detach from the host continuation registry. Otherwise, if
      // we gain continuations, we must register to the latest host continuation registry.
      MountStateRestartPolicy.Default -> {
        if (hadContinuations && mountItemsWithContinuations.isEmpty()) {
          detachFromRestartRegistry()
        } else if (mountItemsWithContinuations.isNotEmpty()) {
          attachToRestartRegistryIfNeeded()
        }
      }
    }
  }

  private fun attachToRestartRegistryIfNeeded() {
    val registry = currentMountStateRestartRegistry
    if (registry != null && parentRestartRegistry != registry) {
      restartAttachResult?.onDetached()
      restartAttachResult = registry.register(this)
      parentRestartRegistry = registry
    }
  }

  private fun detachFromRestartRegistry() {
    restartAttachResult?.onDetached()
    restartAttachResult = null
    parentRestartRegistry = null
  }

  private fun runAllContinuationsIfNeeded() {
    if (mountItemsWithContinuations.isEmpty()) {
      return
    }

    val isTracing = tracer.isTracing()
    if (isTracing) {
      tracer.beginSection("MountState.restartContinuations")
    }
    restartAllMountItemContinuations()
    if (isTracing) {
      tracer.endSection()
    }
  }

  protected open fun restartAllMountItemContinuations() {
    mountItemsWithContinuations.forEach { id ->
      val mountItem = idToMountedItemMap[id] ?: return@forEach
      // Skip the restart (and the forceLayout/applyBounds it triggers) for items whose
      // continuations/nested mount states have nothing to update this pass.
      if (!mountItem.hasPendingRestartWork(mountContext.continuationContext)) {
        return@forEach
      }
      currentUpdatingNode = mountItem.renderTreeNode
      mountItem.restartContinuations(mountContext.continuationContext)
      // Restarting continuations may result in the mount item's content requesting layout, so we
      // need to perform traversal to complete the update if necessary
      val content = mountItem.content
      var forceTraversal = false
      if (content is View) {
        if (content is RenderTreeHost) {
          content.forceLayout()
        }
        forceTraversal = content.isLayoutRequested
      }
      if (forceTraversal) {
        applyBoundsToMountItem(mountItem, force = true)
      }
      currentUpdatingNode = null
    }
  }

  override fun unmountAllItems() {
    try {
      _rootHost.setInLayout()
      _mountDelegate?.onBeforeUnmountAllItems()
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
      detachFromRestartRegistry()

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

  override fun getContentAt(position: Int): Any? = renderTree?.let { renderTree ->
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
      mountExtension: MountExtension<Input, State>,
  ): ExtensionState<State> {
    val mountDelegate = _mountDelegate ?: MountDelegate(this, tracer).also { _mountDelegate = it }
    return mountDelegate.registerMountExtension(mountExtension) as ExtensionState<State>
  }

  override fun getHosts(): ArrayList<Host> {
    val hosts = ArrayList<Host>()
    idToMountedItemMap.forEachValue { item ->
      val content = item.content
      if (content is Host) {
        hosts.add(content)
      }
    }
    return hosts
  }

  fun invalidateHosts(hostIds: LongSet) {
    tracer.trace({ "MountState.invalidateHosts" }) {
      hostIds.forEach { id ->
        val content = getContentById(id)
        if (content is Host) {
          content.invalidate()
        } else {
          ErrorReporter.report(
              LogLevel.DEBUG,
              "MountState.invalidateHosts",
              "Expecting content with id=$id to be a Host but got ${content?.javaClass?.simpleName}",
          )
        }
      }
    }
  }

  @Suppress("UNCHECKED_CAST")
  fun rebindBinders(binders: LongObjectMap<ScatterSet<BinderId>>) {
    tracer.trace({ "MountState.rebindBinders" }) {
      binders.forEach { renderUnitId, binders ->
        val mountItem = idToMountedItemMap[renderUnitId]
        if (mountItem != null) {
          val renderUnit = mountItem.renderUnit as RenderUnit<Any>
          // Bypasses updateMountItemIfNeeded, so dispatch RenderUnitUpdated to drain the tracer.
          val traceIdentifier =
              DebugEventDispatcher.generateTraceIdentifier(DebugEvent.RenderUnitUpdated)
          if (traceIdentifier != null) {
            val attributes = HashMap<String, Any?>()
            attributes[RenderUnitId] = renderUnitId
            attributes[Description] = renderUnit.description
            attributes[Bounds] = mountItem.renderTreeNode.bounds
            attributes[RootHostHashCode] = _rootHost.hashCode()
            attributes[Key] = renderUnit.debugKey
            DebugEventDispatcher.beginTrace(
                traceIdentifier,
                DebugEvent.RenderUnitUpdated,
                checkNotNull(renderTree).renderStateId.toString(),
                attributes,
            )
          }
          // finally: a throwing binder must still drain the entries the rebind wrote.
          try {
            renderUnit.rebindBinders(
                mountContext,
                binders,
                mountItem.content,
                mountItem.renderTreeNode.layoutData,
                mountItem.bindData,
            )
          } finally {
            traceIdentifier?.let { DebugEventDispatcher.endTrace(it) }
          }
        } else {
          ErrorReporter.report(
              LogLevel.DEBUG,
              "MountState.rebindBinders",
              "RenderUnit with id=$renderUnitId not found",
          )
        }
      }
    }
  }

  override fun getMountItemAt(position: Int): MountItem? = renderTree?.let {
    idToMountedItemMap[it.getRenderTreeNodeAtIndex(position).renderUnit.id]
  }

  override fun getMountItemCount(): Int = idToMountedItemMap.size

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
          applyBoundsToMountItem(mountItem, force = true)
        }
      }
      if (isTracing) {
        tracer.endSection()
      }
    }
  }

  /** Unbinds all the MountItems currently mounted on this MountState. */
  override fun detach() {
    detachFromRestartRegistry()
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
      mountDelegate: MountDelegate?,
  ) {
    // MountState should never update the bounds of the top-level host as this
    // should be done by the ViewGroup containing the LithoView.
    if (renderTreeNode.renderUnit.id == ROOT_HOST_ID) {
      return
    }
    val content = item.content
    val forceTraversal = content is View && content.isLayoutRequested
    val changed = applyBoundsToMountItem(item, forceTraversal)
    mountDelegate?.onBoundsAppliedToItem(
        renderTreeNode,
        item.content,
        changed,
        tracer,
    )
  }

  /** Mount, unmount, or update each [MountItem] in the render tree. */
  protected open fun updateMountItem(renderTree: RenderTree, index: Int) {
    updateMountItemCore(renderTree.getRenderTreeNodeAtIndex(index), index)
  }

  private fun updateMountItemCore(renderTreeNode: RenderTreeNode, index: Int) {
    val isMountable = isMountable(renderTreeNode, index)
    val currentMountItem = idToMountedItemMap[renderTreeNode.renderUnit.id]

    if (currentMountItem != null) {
      if (isMountable) {
        currentUpdatingNode = renderTreeNode
        updateMountItemIfNeeded(renderTreeNode, currentMountItem)
        currentUpdatingNode = null
      } else {
        unmountItemRecursively(currentMountItem.renderTreeNode.renderUnit.id)
      }
    } else {
      if (isMountable) {
        currentUpdatingNode = renderTreeNode
        mountRenderUnit(renderTreeNode)
        currentUpdatingNode = null
      }
    }
  }

  fun willRemountWith(renderTree: RenderTree): Boolean {
    return this.renderTree != renderTree || needsRemount()
  }

  /** Updates the extensions of this [MountState] from the new [RenderTree]. */
  private fun updateRenderTree(renderTree: RenderTree): Boolean {
    // If the trees are same or if this mount state has not been forced to remount, then no update
    // is required.
    if (this.renderTree == renderTree && !_needsRemount) {
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
    if (previousRenderTree == renderTree) {
      return
    }

    val isTracing = tracer.isTracing()
    if (isTracing) {
      tracer.beginSection("MountState.prepareMount")
    }
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
    if (isTracing) {
      tracer.endSection()
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
              checkNotNull(checkNotNull(_mountDelegate).unmountDelegateExtensionState),
              oldItem,
          ) ?: false
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
                  oldItem,
                  oldItem.renderTreeNode.positionInParent,
                  renderTreeNode.positionInParent,
              )
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
          attributes[IsGapWorkerPremount] = GapWorkerPremountScope.isActive
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

      // ensure that the parent is mounted before we try to mount the current node
      if (!isMounted(parentRenderUnit.id)) {
        val savedNode = currentUpdatingNode
        currentUpdatingNode = hostTreeNode
        mountRenderUnit(hostTreeNode)
        currentUpdatingNode = savedNode
      }
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
      val content =
          renderUnit.contentAllocator.acquireContent(
              mountContext.androidContext,
              renderTreeNode.poolScope,
          )
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
      val changed = applyBoundsToMountItem(item, force = true)

      if (item.hasAnyContinuations()) {
        mountItemsWithContinuations.add(renderUnit.id)
      }

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
      mountStats?.recordMount(renderUnit.description, isInitialMount = true)

      if (isTracing) {
        tracer.endSection()
        tracer.endSection()
      }
    }
  }

  protected fun unmountItemRecursively(id: Long) {
    val item = idToMountedItemMap[id] ?: return // Already has been unmounted.
    mountItemsWithContinuations.remove(id)
    item.clearContinuations()

    // When unmounting use the render unit from the MountItem
    val isTracing = tracer.isTracing()
    val node = item.renderTreeNode
    val unit = item.renderUnit as RenderUnit<Any>
    val content = item.content
    val hasUnmountDelegate =
        unmountDelegateExtension?.shouldDelegateUnmount(
            checkNotNull(checkNotNull(_mountDelegate).unmountDelegateExtensionState),
            item,
        ) ?: false
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
          attributes,
      )
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
      if (!hasUnmountDelegate) {
        val host = content as Host
        val leftoverCount = host.mountItemCount
        if (leftoverCount > 0) {
          val diagnostic =
              try {
                buildLeftoverItemsDiagnostic(node, host, leftoverCount)
              } catch (t: Throwable) {
                "<diagnostic gathering failed: ${t.javaClass.simpleName}: ${t.message}>"
              }

          DebugEventDispatcher.dispatch(
              type = "MountState:UnmountLeftoverItems",
              renderStateId = { checkNotNull(renderTree).renderStateId.toString() },
              logLevel = LogLevel.ERROR,
          ) { attrs ->
            attrs["message"] =
                "Recursively unmounting items from a ComponentHost, found $leftoverCount " +
                    "untracked items. Attempting defensive cleanup. $diagnostic"
          }

          var cleanupFailure: Exception? = null
          try {
            host.safelyUnmountAll()
          } catch (e: Exception) {
            cleanupFailure = e
            DebugEventDispatcher.dispatch(
                type = "MountState:UnmountCleanupFailed",
                renderStateId = { checkNotNull(renderTree).renderStateId.toString() },
                logLevel = LogLevel.ERROR,
            ) { attrs ->
              attrs["message"] = "Failed to safely unmount leftover items: ${e.message}"
            }
          }

          val remainingCount = host.mountItemCount
          if (remainingCount > 0) {
            throw IllegalStateException(
                "Recursively unmounting items from a ComponentHost, left $remainingCount " +
                    "items behind after defensive cleanup (original leftover: $leftoverCount). " +
                    diagnostic,
                cleanupFailure,
            )
          }
        }
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
              checkNotNull(checkNotNull(_mountDelegate).unmountDelegateExtensionState),
              item,
              host,
          )
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
      item.releaseMountContent(mountContext.androidContext)
    }
    if (isTracing) {
      tracer.endSection()
    }
    traceIdentifier?.let { DebugEventDispatcher.endTrace(it) }
    mountStats?.recordUnmount()
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
          item.bindData,
      )
    }
  }

  private fun mountRootItem(rootNode: RenderTreeNode) {
    // Create root mount item.
    val item = createMountItem(rootNode, _rootHost)

    // Run mount callbacks.
    mountRenderUnitToContent(
        rootNode,
        rootNode.renderUnit as RenderUnit<Any>,
        _rootHost,
        item.bindData,
    )

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
        mountItem.bindData,
    )
    mountItem.releaseMountContent(mountContext.androidContext)
  }

  fun setRenderTreeUpdateListener(listener: RenderTreeUpdateListener?) {
    _mountDelegate =
        (_mountDelegate ?: MountDelegate(this, tracer)).also { it.setMountStateListener(listener) }
  }

  @VisibleForTesting
  fun addExtensions(extensions: List<Pair<RenderCoreExtension<*, *>, Any>>?) {
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
      bindData: BindData,
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
          attributes,
      )
    }
    unit.mountBinders(mountContext, content, node.layoutData, bindData)
    _mountDelegate?.onMountItem(unit, content, node.layoutData, tracer)
    traceIdentifier?.let { DebugEventDispatcher.endTrace(it) }
  }

  private fun unmountRenderUnitFromContent(
      node: RenderTreeNode,
      unit: RenderUnit<Any>,
      content: Any,
      bindData: BindData,
  ) {
    _mountDelegate?.onUnmountItem(unit, content, node.layoutData, tracer)
    unit.unmountBinders(mountContext, content, node.layoutData, bindData)
  }

  protected fun bindRenderUnitToContent(item: MountItem) {
    val renderUnit = item.renderUnit as RenderUnit<Any>
    val content = item.content
    val layoutData = item.renderTreeNode.layoutData
    renderUnit.attachBinders(mountContext, content, layoutData, item.bindData)
    _mountDelegate?.onBindItem(renderUnit, content, layoutData, tracer)
    item.isBound = true
  }

  private fun unbindRenderUnitFromContent(item: MountItem) {
    val renderUnit = item.renderUnit as RenderUnit<Any>
    val content = item.content
    val layoutData = item.renderTreeNode.layoutData
    _mountDelegate?.onUnbindItem(renderUnit, content, layoutData, tracer)
    renderUnit.detachBinders(mountContext, content, layoutData, item.bindData)
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
    val extraBindersToUpdate = idToDirtyBindersMap[renderUnit.id] ?: emptyScatterSet()
    val hasBindersToUpdate = extraBindersToUpdate.isNotEmpty()

    // Re initialize the MountItem internal state with the new attributes from RenderTreeNode
    val hadContinuations = mountItemsWithContinuations.contains(currentMountItem.renderUnit.id)
    currentMountItem.update(renderTreeNode)
    currentRenderUnit.onStartUpdateRenderUnit()
    mountDelegate?.startNotifyVisibleBoundsChangedSection()

    if (
        hasBindersToUpdate ||
            shouldUpdateMountItem(currentRenderUnit, renderUnit, currentLayoutData, newLayoutData)
    ) {
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
            attributes,
        )
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
          currentMountItem,
          extraBindersToUpdate,
      )
      if (isTracing) {
        tracer.endSection()
      }
      traceIdentifier?.let { DebugEventDispatcher.endTrace(it) }
      mountStats?.recordUpdate(renderUnit.description)
    } else if (!currentMountItem.isBound) {
      bindRenderUnitToContent(currentMountItem)
      mountStats?.recordReuse(renderUnit.description)
    } else {
      currentMountItem.renderUnit.restartContinuations(mountContext.continuationContext)
      mountStats?.recordReuse(renderUnit.description)
    }
    currentMountItem.registry.restartMountStates()

    currentMountItem.isBound = true

    // Update the bounds of the mounted content. This needs to be done regardless of whether
    // the RenderUnit has been updated or not since the mounted item might might have the same
    // size and content but a different position.
    if (isTracing) {
      tracer.beginSection("UpdateBounds: ${renderUnit.description}")
    }
    updateBoundsForMountedRenderTreeNode(renderTreeNode, currentMountItem, mountDelegate)
    if (hadContinuations && !currentMountItem.hasAnyContinuations()) {
      mountItemsWithContinuations.remove(renderUnit.id)
    } else if (currentMountItem.hasAnyContinuations()) {
      mountItemsWithContinuations.add(renderUnit.id)
    }

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
      extraBindersToUpdate: ScatterSet<BinderId>,
  ) {
    try {
      renderUnit.updateBinders(
          mountContext,
          content,
          currentRenderUnit,
          currentLayoutData,
          newLayoutData,
          _mountDelegate,
          currentMountItem.bindData,
          currentMountItem.isBound,
          extraBindersToUpdate,
      )
    } catch (e: Exception) {
      throw RenderUnitUpdateException(
          renderUnit,
          """
            Unable to update RenderUnit for content: '${content.javaClass.simpleName}'.
            RenderUnit: id=${renderUnit.id}; poolKey='${renderUnit.contentAllocator.getPoolKey()}'.
            Current RenderUnit: id=${currentRenderUnit.id}; poolKey='${currentRenderUnit.contentAllocator.getPoolKey()}'.
            """
              .trimIndent(),
          e,
      )
    }
  }

  @VisibleForTesting
  val restartableMountItems: LongSet
    get() = mountItemsWithContinuations

  @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
  fun getMountItem(id: Long): MountItem? = idToMountedItemMap[id]

  fun needsRemount(needsRemount: Boolean) {
    _needsRemount = needsRemount
  }

  private fun applyBoundsToMountItem(mountItem: MountItem, force: Boolean): Boolean {
    val renderTreeNode = mountItem.renderTreeNode
    val bounds = renderTreeNode.bounds
    val padding = renderTreeNode.resolvedPadding
    return withHostContinuationRegistry(mountItem.registry) {
      BoundsUtils.applyBoundsToMountContent(
          bounds.left,
          bounds.top,
          bounds.right,
          bounds.bottom,
          padding,
          mountItem.content,
          force,
          tracer,
      )
    }
  }

  /**
   * Build a multi-line description of what the renderTree thought lived under [hostNode] vs what
   * actually remains in the [host] view after the recursive unmount. Called from the unmount
   * assertion so the production crash report contains enough to localize the splice/baseline
   * emission that produced the inconsistency. Intentionally bounds-limited to keep crash messages
   * tractable.
   */
  private fun buildLeftoverItemsDiagnostic(
      hostNode: RenderTreeNode,
      host: Host,
      leftoverCount: Int,
  ): String {
    val expectedCount = hostNode.childrenCount
    val expected = StringBuilder()
    expected.append("expected=").append(expectedCount).append(" [")
    for (i in 0 until minOf(expectedCount, MAX_DIAGNOSTIC_ITEMS)) {
      val child = hostNode.getChildAt(i)
      val childItem = idToMountedItemMap[child.renderUnit.id]
      if (i > 0) expected.append(", ")
      expected
          .append("id=")
          .append(child.renderUnit.id)
          .append("/")
          .append(child.renderUnit.description)
          .append("[stillInMap=")
          .append(childItem != null)
      if (childItem != null) {
        expected
            .append(", itemHostMatchesParent=")
            .append(childItem.host === host)
            .append(", itemHostClass=")
            .append(childItem.host?.javaClass?.simpleName ?: "null")
            .append(", itemRtnSameAsExpectedRtn=")
            .append(childItem.renderTreeNode === child)
      }
      expected.append("]")
    }
    if (expectedCount > MAX_DIAGNOSTIC_ITEMS) {
      expected.append(", … +").append(expectedCount - MAX_DIAGNOSTIC_ITEMS).append(" more")
    }
    expected.append("]")

    // [Host.mountItems] is sparse — items live at their own [positionInParent] index, not
    // packed from 0. Scan upward, skipping null slots, until we've collected up to the cap or
    // exhausted [leftoverCount]. Bounded scan window protects against pathological cases.
    val leftover = StringBuilder()
    leftover.append("leftover=").append(leftoverCount).append(" [")
    val cap = minOf(leftoverCount, MAX_DIAGNOSTIC_ITEMS)
    var found = 0
    var scanIdx = 0
    while (found < cap && scanIdx < MAX_LEFTOVER_SCAN_POSITIONS) {
      val mountItem =
          try {
            host.getMountItemAt(scanIdx)
          } catch (_: Exception) {
            null
          }
      scanIdx++
      if (mountItem == null) continue
      if (found > 0) leftover.append(", ")
      found++
      val unit = mountItem.renderUnit
      val mountedRtn = mountItem.renderTreeNode
      val currentRtn = renderTreeNodeForId(unit.id)
      leftover
          .append("id=")
          .append(unit.id)
          .append("/")
          .append(unit.description)
          .append("[posInParent=")
          .append(mountedRtn.positionInParent)
          .append(", stillInMap=")
          .append(isMounted(unit.id))
          .append(", itemHostMatchesParent=")
          .append(mountItem.host === host)
          .append(", itemHostClass=")
          .append(mountItem.host?.javaClass?.simpleName ?: "null")
          .append(", rtnIdentityMatchesCurrent=")
          .append(currentRtn != null && currentRtn === mountedRtn)
          .append(", rtnParentMatchesHost=")
          .append(mountedRtn.parent === hostNode)
          .append(", rtnParentId=")
          .append(mountedRtn.parent?.renderUnit?.id ?: "null")
          .append("]")
    }
    if (found < leftoverCount) {
      leftover.append(", … +").append(leftoverCount - found)
      // If the loop exited because we hit the cap, the remainder is "more" beyond the reporting
      // cap. If it exited because scanIdx reached the scan window before finding cap items, the
      // remainder is unaccounted items beyond the scan window.
      if (found < cap) {
        leftover.append(" beyond scan window of ").append(MAX_LEFTOVER_SCAN_POSITIONS)
      } else {
        leftover.append(" more")
      }
    }
    leftover.append("]")

    val hostRtnMatchesCurrent = renderTreeNodeForId(hostNode.renderUnit.id) === hostNode
    return "host(id=${hostNode.renderUnit.id}, description=${hostNode.renderUnit.description}, " +
        "positionInParent=${hostNode.positionInParent}, hostClass=${host.javaClass.simpleName}, " +
        "hostRtnMatchesCurrent=$hostRtnMatchesCurrent); $expected; $leftover"
  }

  /**
   * Look up the RTN instance that the *current* renderTree carries for [id]. Returns null if the
   * renderTree is null or doesn't have an entry for the id. Used to detect stale
   * MountItem.renderTreeNode references — when a MountItem captured an RTN at mount time but the
   * current renderTree has a different RTN object for the same id, splice/reuse produced
   * tree-divergence that mount never reconciled.
   */
  private fun renderTreeNodeForId(id: Long): RenderTreeNode? {
    val tree = renderTree ?: return null
    val index = tree.getRenderTreeNodeIndex(id)
    if (index < 0) return null
    return tree.getRenderTreeNodeAtIndex(index)
  }

  companion object {
    const val ROOT_HOST_ID: Long = 0L

    /** Cap on items reported in the unmount-leaks-items assertion to keep messages tractable. */
    private const val MAX_DIAGNOSTIC_ITEMS = 16

    /**
     * Upper bound on the [Host.getMountItemAt] index scan when collecting leftovers. The host's
     * underlying array is sparse (indexed by [RenderTreeNode.positionInParent]); items can sit at
     * arbitrary indices with null gaps between them. 256 covers ~32× the largest realistic host
     * (Litho hosts are usually <20 children, wide containers ~100), and serves as a runaway stop if
     * [Host.mountItemCount] is inconsistent with the actual array contents. Items past the cap are
     * reported as `+N beyond scan window`.
     */
    private const val MAX_LEFTOVER_SCAN_POSITIONS = 256

    private fun assertParentContentType(
        parentContent: Any,
        renderUnit: RenderUnit<*>,
        parentRenderUnit: RenderUnit<*>,
    ) {
      if (parentContent !is Host) {
        throw RuntimeException(
            """
            Trying to mount a RenderTreeNode, its parent should be a Host, but was '${parentContent.javaClass.simpleName}'.
            Parent RenderUnit: id=${parentRenderUnit.id}; poolKey='${parentRenderUnit.contentAllocator.getPoolKey()}'.
            Child RenderUnit: id=${renderUnit.id}; poolKey='${renderUnit.contentAllocator.getPoolKey()}'.
            """
                .trimIndent(),
        )
      }
    }
  }
}
