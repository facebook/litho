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

import android.graphics.Rect
import android.util.Pair
import androidx.annotation.VisibleForTesting
import androidx.collection.LongSparseArray
import com.facebook.rendercore.extensions.ExtensionState
import com.facebook.rendercore.extensions.GapWorkerCallbacks
import com.facebook.rendercore.extensions.InformsMountCallback
import com.facebook.rendercore.extensions.MountExtension
import com.facebook.rendercore.extensions.OnItemCallbacks
import com.facebook.rendercore.extensions.RenderCoreExtension
import com.facebook.rendercore.extensions.VisibleBoundsCallbacks
import java.util.ArrayList
import java.util.HashSet

/**
 * Can be passed to a MountState to override default mounting behaviour and control which items get
 * mounted or unmounted.
 */
class MountDelegate(val mountDelegateTarget: MountDelegateTarget, val tracer: Systracer) {

  private val referenceCountMap = LongSparseArray<Int>()
  private val _extensionStates: MutableList<ExtensionState<Any>> = ArrayList()
  var unmountDelegateExtensionState: ExtensionState<Any>? = null
    private set

  private var referenceCountingEnabled: Boolean = false
  private var collectVisibleBoundsChangedCalls: Boolean = false
  private var notifyVisibleBoundsChangedNestCount: Int = 0
  private val notifyVisibleBoundsChangedItems: MutableSet<Any?> = HashSet()
  private var mountStateListener: RenderTreeUpdateListener? = null

  fun setCollectVisibleBoundsChangedCalls(value: Boolean) {
    collectVisibleBoundsChangedCalls = value
  }

  fun registerExtensions(extensions: List<Pair<RenderCoreExtension<*, *>, Any>>?) {
    _extensionStates.clear()
    if (extensions != null) {
      for (e in extensions) {
        val extension = e.first.getMountExtension()
        if (extension != null) {
          val extensionState = extension.createExtensionState(this) as ExtensionState<Any>
          if (extension is UnmountDelegateExtension<*>) {
            mountDelegateTarget.setUnmountDelegateExtension(
                (extension as UnmountDelegateExtension<*>))
            unmountDelegateExtensionState = extensionState
          }
          referenceCountingEnabled =
              (referenceCountingEnabled ||
                  (extension is InformsMountCallback &&
                      (extension as InformsMountCallback).canPreventMount()))
          _extensionStates.add(extensionState)
        }
      }
    }
  }

  /** @param extension */
  @Deprecated("Only used for Litho's integration. Marked for removal.")
  fun registerMountExtension(extension: MountExtension<*, *>): ExtensionState<*> {
    val extensionState = extension.createExtensionState(this) as ExtensionState<Any>
    if (extension is UnmountDelegateExtension<*>) {
      mountDelegateTarget.setUnmountDelegateExtension((extension as UnmountDelegateExtension<*>))
      unmountDelegateExtensionState = extensionState
    }
    referenceCountingEnabled =
        (referenceCountingEnabled ||
            (extension is InformsMountCallback &&
                (extension as InformsMountCallback).canPreventMount()))
    _extensionStates.add(extensionState)
    return extensionState
  }

  /** @param toRemove [MountExtension] to remove. */
  @Deprecated("Only used for Litho's integration. Marked for removal.")
  fun unregisterMountExtension(toRemove: MountExtension<*, *>) {
    var mountExtension: MountExtension<*, *>? = null
    val iter = _extensionStates.iterator()
    while (iter.hasNext()) {
      val extension = iter.next().extension
      if (extension === toRemove) {
        mountExtension = extension
        iter.remove()
        break
      }
    }
    if (mountExtension is UnmountDelegateExtension<*>) {
      mountDelegateTarget.removeUnmountDelegateExtension()
      unmountDelegateExtensionState = null
    }
    checkNotNull(mountExtension) { { "Could not find the extension $toRemove" } }
    if (mountExtension is InformsMountCallback &&
        (mountExtension as InformsMountCallback).canPreventMount()) {
      updateRefCountEnabled()
    }
  }

  fun unregisterAllExtensions() {
    mountDelegateTarget.removeUnmountDelegateExtension()
    unmountDelegateExtensionState = null
    _extensionStates.clear()
    referenceCountingEnabled = false
  }

  fun onRegisterForPremount(frameTimeMs: Long?) {
    for (i in 0 until _extensionStates.size) {
      val extension = _extensionStates[i].extension
      if (extension is GapWorkerCallbacks<*>) {
        (extension as GapWorkerCallbacks<Any>).onRegisterForPremount(
            _extensionStates[i] as ExtensionState<Any>, frameTimeMs)
      }
    }
  }

  fun onUnregisterForPremount() {
    for (i in 0 until _extensionStates.size) {
      val extension = _extensionStates[i].extension
      if (extension is GapWorkerCallbacks<*>) {
        (extension as GapWorkerCallbacks<Any>).onUnregisterForPremount(
            _extensionStates[i] as ExtensionState<Any>)
      }
    }
  }

  /**
   * Calls [MountExtension#beforeMount(ExtensionState, Object, Rect)] for each [RenderCoreExtension]
   * that has a mount phase.
   *
   * @param results A map of [RenderCoreExtension] to their results from the layout phase.
   */
  fun beforeMount(results: List<Pair<RenderCoreExtension<*, *>, Any>>, rect: Rect?) {
    var i = 0 // Use an int to get the index of the extensions state.
    for (entry in results) {
      val input = entry.second
      if (entry.first.getMountExtension() != null) {
        val extension = entry.first.getMountExtension() as MountExtension<Any, Any>
        val current = _extensionStates[i] as ExtensionState<Any>
        check(current.extension === extension) {
          String.format(
              "state for %s was not found at expected index %d. Found %s at index instead.",
              entry.first,
              i,
              current.extension)
        }
        extension.beforeMount(current, input, rect)
        i++
      }
    }
  }

  fun afterMount() {
    startNotifyVisibleBoundsChangedSection()
    for (i in 0 until _extensionStates.size) {
      _extensionStates[i].afterMount()
    }
    endNotifyVisibleBoundsChangedSection()
  }

  fun notifyVisibleBoundsChanged(rect: Rect?) {
    startNotifyVisibleBoundsChangedSection()
    for (i in 0 until _extensionStates.size) {
      val state = _extensionStates[i] as ExtensionState<Any>
      val extension = state.extension
      if (extension is VisibleBoundsCallbacks<*>) {
        (extension as VisibleBoundsCallbacks<Any>).onVisibleBoundsChanged(state, rect)
      }
    }
    endNotifyVisibleBoundsChangedSection()
  }

  fun notifyVisibleBoundsChangedForItem(item: Any?) {
    if (!collectVisibleBoundsChangedCalls) {
      RenderCoreExtension.recursivelyNotifyVisibleBoundsChanged(item, tracer)
      return
    }
    notifyVisibleBoundsChangedItems.add(item)
  }

  fun startNotifyVisibleBoundsChangedSection() {
    if (!collectVisibleBoundsChangedCalls) {
      return
    }
    notifyVisibleBoundsChangedNestCount++
  }

  fun endNotifyVisibleBoundsChangedSection() {
    if (!collectVisibleBoundsChangedCalls) {
      return
    }
    notifyVisibleBoundsChangedNestCount--
    if (notifyVisibleBoundsChangedNestCount < 0) {
      throw RuntimeException(
          "notifyVisibleBoundsChangedNestCount should not be decremented below zero!")
    }
    if (notifyVisibleBoundsChangedNestCount == 0) {
      for (item in notifyVisibleBoundsChangedItems) {
        RenderCoreExtension.recursivelyNotifyVisibleBoundsChanged(item, tracer)
      }
      notifyVisibleBoundsChangedItems.clear()
    }
  }

  private fun updateRefCountEnabled() {
    referenceCountingEnabled = false
    for (i in 0 until _extensionStates.size) {
      val extension = _extensionStates[i].extension
      if (extension is InformsMountCallback) {
        referenceCountingEnabled = (extension as InformsMountCallback).canPreventMount()
      }
      if (referenceCountingEnabled) {
        return
      }
    }
  }

  fun unBind() {
    startNotifyVisibleBoundsChangedSection()
    for (i in 0 until _extensionStates.size) {
      _extensionStates[i].onUnbind()
    }
    endNotifyVisibleBoundsChangedSection()
  }

  fun unMount() {
    startNotifyVisibleBoundsChangedSection()
    for (i in 0 until _extensionStates.size) {
      _extensionStates[i].onUnmount()
    }
    endNotifyVisibleBoundsChangedSection()
  }

  fun onBindItem(renderUnit: RenderUnit<*>, content: Any, layoutData: Any?, tracer: Systracer) {
    startNotifyVisibleBoundsChangedSection()
    for (i in 0 until _extensionStates.size) {
      val state = _extensionStates[i] as ExtensionState<Any>
      val extension = state.extension
      if (extension is OnItemCallbacks<*>) {
        val isTracing = tracer.isTracing()
        if (isTracing) {
          tracer.beginSection("Extension:onBindItem ${extension.name}")
        }
        (extension as OnItemCallbacks<Any>).onBindItem(state, renderUnit, content, layoutData)
        if (isTracing) {
          tracer.endSection()
        }
      }
    }
    endNotifyVisibleBoundsChangedSection()
  }

  fun onUnbindItem(renderUnit: RenderUnit<*>, content: Any, layoutData: Any?, tracer: Systracer) {
    startNotifyVisibleBoundsChangedSection()
    for (i in 0 until _extensionStates.size) {
      val state = _extensionStates[i] as ExtensionState<Any>
      val extension = state.extension
      if (extension is OnItemCallbacks<*>) {
        val isTracing = tracer.isTracing()
        if (isTracing) {
          tracer.beginSection("Extension:onUnbindItem ${extension.name}")
        }
        (extension as OnItemCallbacks<Any>).onUnbindItem(state, renderUnit, content, layoutData)
        if (isTracing) {
          tracer.endSection()
        }
      }
    }
    endNotifyVisibleBoundsChangedSection()
  }

  /**
   * Collects all the [MountExtension] which need a callback to their mount and bind item methods
   * for {@param nextRenderUnit}. This method returns the list of those extensions.
   */
  fun collateExtensionsToUpdate(
      previousRenderUnit: RenderUnit<*>,
      previousLayoutData: Any?,
      nextRenderUnit: RenderUnit<*>,
      nextLayoutData: Any?,
      tracer: Systracer
  ): List<ExtensionState<*>>? {
    var extensionStatesToUpdate: MutableList<ExtensionState<*>>? = null
    for (i in 0 until _extensionStates.size) {
      val state = _extensionStates[i] as ExtensionState<Any>
      val extension = state.extension
      if (extension is OnItemCallbacks<*>) {
        val isTracing = tracer.isTracing()
        if (isTracing) {
          tracer.beginSection("Extension:shouldUpdateItem ${extension.name}")
        }
        val shouldUpdate: Boolean =
            (extension as OnItemCallbacks<Any>).shouldUpdateItem(
                state, previousRenderUnit, previousLayoutData, nextRenderUnit, nextLayoutData)
        if (isTracing) {
          tracer.endSection()
        }
        if (shouldUpdate) {
          if (extensionStatesToUpdate == null) {
            extensionStatesToUpdate = ArrayList(_extensionStates.size)
          }
          extensionStatesToUpdate.add(state)
        }
      }
    }
    return extensionStatesToUpdate
  }

  fun onMountItem(renderUnit: RenderUnit<*>, content: Any, layoutData: Any?, tracer: Systracer) {
    startNotifyVisibleBoundsChangedSection()
    for (i in 0 until _extensionStates.size) {
      val state = _extensionStates[i] as ExtensionState<Any>
      val extension = state.extension
      if (extension is OnItemCallbacks<*>) {
        val isTracing = tracer.isTracing()
        if (isTracing) {
          tracer.beginSection("Extension:onMountItem ${extension.name}")
        }
        (extension as OnItemCallbacks<Any>).onMountItem(state, renderUnit, content, layoutData)
        if (isTracing) {
          tracer.endSection()
        }
      }
    }
    endNotifyVisibleBoundsChangedSection()
  }

  fun onUnmountItem(renderUnit: RenderUnit<*>, content: Any, layoutData: Any?, tracer: Systracer) {
    startNotifyVisibleBoundsChangedSection()
    for (i in 0 until _extensionStates.size) {
      val state = _extensionStates[i] as ExtensionState<Any>
      val extension = state.extension
      if (extension is OnItemCallbacks<*>) {
        val isTracing = tracer.isTracing()
        if (isTracing) {
          tracer.beginSection("Extension:onUnmountItem ${extension.name}")
        }
        (extension as OnItemCallbacks<Any>).onUnmountItem(state, renderUnit, content, layoutData)
        if (isTracing) {
          tracer.endSection()
        }
      }
    }
    endNotifyVisibleBoundsChangedSection()
  }

  fun onBoundsAppliedToItem(
      node: RenderTreeNode,
      content: Any,
      changed: Boolean,
      tracer: Systracer
  ) {
    startNotifyVisibleBoundsChangedSection()
    for (i in 0 until _extensionStates.size) {
      val state = _extensionStates[i] as ExtensionState<Any>
      val extension = state.extension
      if (extension is OnItemCallbacks<*>) {
        val isTracing = tracer.isTracing()
        if (isTracing) {
          tracer.beginSection("Extension:onBoundsAppliedToItem ${extension.name}")
        }
        (extension as OnItemCallbacks<Any>).onBoundsAppliedToItem(
            state, node.renderUnit, content, node.layoutData, changed)
        if (isTracing) {
          tracer.endSection()
        }
      }
    }
    endNotifyVisibleBoundsChangedSection()
  }

  fun getContentAt(position: Int): Any? = mountDelegateTarget.getContentAt(position)

  fun getContentById(id: Long): Any? = mountDelegateTarget.getContentById(id)

  fun isRootItem(position: Int): Boolean = mountDelegateTarget.isRootItem(position)

  /** @return true if this item needs to be mounted. */
  fun maybeLockForMount(renderTreeNode: RenderTreeNode, index: Int): Boolean {
    if (!referenceCountingEnabled) {
      return true
    }
    startNotifyVisibleBoundsChangedSection()
    for (i in 0 until _extensionStates.size) {
      val extension = _extensionStates[i].extension
      if (extension is OnItemCallbacks<*>) {
        (extension as OnItemCallbacks<Any>).beforeMountItem(
            _extensionStates[i] as ExtensionState<Any>, renderTreeNode, index)
      }
    }
    endNotifyVisibleBoundsChangedSection()
    return hasAcquiredRef(renderTreeNode.renderUnit.id)
  }

  fun isLockedForMount(renderTreeNode: RenderTreeNode): Boolean =
      isLockedForMount(renderTreeNode.renderUnit.id)

  fun isLockedForMount(id: Long): Boolean =
      if (!referenceCountingEnabled) {
        true
      } else {
        hasAcquiredRef(id)
      }

  private fun hasAcquiredRef(renderUnitId: Long): Boolean {
    val refCount = referenceCountMap[renderUnitId]
    return refCount != null && refCount > 0
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  fun acquireMountRef(node: RenderTreeNode) {
    acquireMountRef(node.renderUnit.id)
  }

  fun acquireMountRef(id: Long) {
    incrementExtensionRefCount(id)
  }

  fun acquireAndMountRef(node: RenderTreeNode) {
    acquireAndMountRef(node.renderUnit.id)
  }

  fun acquireAndMountRef(id: Long) {
    acquireMountRef(id)

    // Only mount if we're during a mounting phase, otherwise the mounting phase will take care of
    // that.
    mountDelegateTarget.notifyMount(id)
  }

  fun hasItemToMount(): Boolean {
    for (i in 0 until _extensionStates.size) {
      val extension = _extensionStates[i].extension
      if (extension is GapWorkerCallbacks<*> &&
          (extension as GapWorkerCallbacks<Any>).hasItemToMount(
              _extensionStates[i] as ExtensionState<Any>)) {
        return true
      }
    }
    return false
  }

  fun premountNext() {
    startNotifyVisibleBoundsChangedSection()
    for (i in 0 until _extensionStates.size) {
      val extension = _extensionStates[i].extension
      if (extension is GapWorkerCallbacks<*>) {
        (extension as GapWorkerCallbacks<Any>).premountNext(
            _extensionStates[i] as ExtensionState<Any>)
      }
    }
    endNotifyVisibleBoundsChangedSection()
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  fun releaseMountRef(renderTreeNode: RenderTreeNode) {
    releaseMountRef(renderTreeNode.renderUnit.id)
  }

  fun releaseMountRef(id: Long) {
    decrementExtensionRefCount(id)
  }

  fun releaseAndUnmountRef(renderTreeNode: RenderTreeNode) {
    releaseAndUnmountRef(renderTreeNode.renderUnit.id)
  }

  fun releaseAndUnmountRef(id: Long) {
    val wasLockedForMount = isLockedForMount(id)
    releaseMountRef(id)
    if (wasLockedForMount && !isLockedForMount(id)) {
      mountDelegateTarget.notifyUnmount(id)
    }
  }

  fun releaseAllAcquiredReferences() {
    if (!referenceCountingEnabled) {
      return
    }
    for (extension in _extensionStates) {
      extension.releaseAllAcquiredReferences()
    }
    referenceCountMap.clear()
  }

  private fun incrementExtensionRefCount(renderUnitId: Long) {
    if (!referenceCountingEnabled) {
      return
    }
    var refCount = referenceCountMap[renderUnitId]
    if (refCount == null) {
      refCount = 0
    }
    referenceCountMap.put(renderUnitId, refCount + 1)
  }

  private fun decrementExtensionRefCount(renderUnitId: Long) {
    if (!referenceCountingEnabled) {
      return
    }
    val refCount = referenceCountMap[renderUnitId]
    check(!(refCount == null || refCount == 0)) {
      "Trying to decrement reference count for an item you don't own."
    }
    referenceCountMap.put(renderUnitId, refCount - 1)
  }

  @VisibleForTesting fun getRefCount(id: Long): Int = checkNotNull(referenceCountMap[id])

  @get:VisibleForTesting
  val extensionStates: List<ExtensionState<*>>
    get() = _extensionStates

  fun setMountStateListener(listener: RenderTreeUpdateListener?) {
    mountStateListener = listener
  }

  fun renderTreeUpdated(host: RenderCoreExtensionHost) {
    mountStateListener?.onRenderTreeUpdated(host)
  }

  companion object {
    @JvmStatic
    fun onUnbindItemWhichRequiresUpdate(
        extensionStatesToUpdate: List<ExtensionState<*>>,
        previousRenderUnit: RenderUnit<*>,
        previousLayoutData: Any?,
        nextRenderUnit: RenderUnit<*>?,
        nextLayoutData: Any?,
        content: Any,
        tracer: Systracer
    ) {
      if (!extensionStatesToUpdate.isEmpty()) {
        val size = extensionStatesToUpdate.size
        for (i in 0 until size) {
          val state = extensionStatesToUpdate[i] as ExtensionState<Any>
          val extension = state.extension
          if (extension is OnItemCallbacks<*>) {
            val isTracing = tracer.isTracing()
            if (isTracing) {
              tracer.beginSection("Extension:onUnbindItem ${extension.name}")
            }
            (extension as OnItemCallbacks<Any>).onUnbindItem(
                state, previousRenderUnit, content, previousLayoutData)
            if (isTracing) {
              tracer.endSection()
            }
          }
        }
      }
    }

    @JvmStatic
    fun onUnmountItemWhichRequiresUpdate(
        extensionStatesToUpdate: List<ExtensionState<*>>,
        previousRenderUnit: RenderUnit<*>,
        previousLayoutData: Any?,
        nextRenderUnit: RenderUnit<*>?,
        nextLayoutData: Any?,
        content: Any,
        tracer: Systracer
    ) {
      if (!extensionStatesToUpdate.isEmpty()) {
        val size = extensionStatesToUpdate.size
        for (i in 0 until size) {
          val state = extensionStatesToUpdate[i] as ExtensionState<Any>
          val extension = state.extension
          if (extension is OnItemCallbacks<*>) {
            val isTracing = tracer.isTracing()
            if (isTracing) {
              tracer.beginSection("Extension:onUnmountItem ${extension.name}")
            }
            (extension as OnItemCallbacks<Any>).onUnmountItem(
                state, previousRenderUnit, content, previousLayoutData)
            if (isTracing) {
              tracer.endSection()
            }
          }
        }
      }
    }

    @JvmStatic
    fun onMountItemWhichRequiresUpdate(
        extensionStatesToUpdate: List<ExtensionState<*>>,
        previousRenderUnit: RenderUnit<*>?,
        previousLayoutData: Any?,
        nextRenderUnit: RenderUnit<*>,
        nextLayoutData: Any?,
        content: Any,
        tracer: Systracer
    ) {
      if (!extensionStatesToUpdate.isEmpty()) {
        val size = extensionStatesToUpdate.size
        for (i in 0 until size) {
          val state = extensionStatesToUpdate[i] as ExtensionState<Any>
          val extension = state.extension
          if (extension is OnItemCallbacks<*>) {
            val isTracing = tracer.isTracing()
            if (isTracing) {
              tracer.beginSection("Extension:onMountItem ${extension.name}")
            }
            (extension as OnItemCallbacks<Any>).onMountItem(
                state, nextRenderUnit, content, nextLayoutData)
            if (isTracing) {
              tracer.endSection()
            }
          }
        }
      }
    }

    @JvmStatic
    fun onBindItemWhichRequiresUpdate(
        extensionStatesToUpdate: List<ExtensionState<*>>,
        previousRenderUnit: RenderUnit<*>?,
        previousLayoutData: Any?,
        nextRenderUnit: RenderUnit<*>,
        nextLayoutData: Any?,
        content: Any,
        tracer: Systracer
    ) {
      if (!extensionStatesToUpdate.isEmpty()) {
        val size = extensionStatesToUpdate.size
        for (i in 0 until size) {
          val state = extensionStatesToUpdate[i] as ExtensionState<Any>
          val extension = state.extension
          if (extension is OnItemCallbacks<*>) {
            val isTracing = tracer.isTracing()
            if (isTracing) {
              tracer.beginSection("Extension:onBindItem ${extension.name}")
            }
            (extension as OnItemCallbacks<Any>).onBindItem(
                state, nextRenderUnit, content, nextLayoutData)
            if (isTracing) {
              tracer.endSection()
            }
          }
        }
      }
    }
  }
}
