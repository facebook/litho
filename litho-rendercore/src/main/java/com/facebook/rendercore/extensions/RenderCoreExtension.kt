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

package com.facebook.rendercore.extensions

import android.graphics.Rect
import android.util.Pair
import android.view.ViewGroup
import com.facebook.rendercore.Host
import com.facebook.rendercore.LayoutResult
import com.facebook.rendercore.MountDelegate
import com.facebook.rendercore.MountDelegateTarget
import com.facebook.rendercore.RenderCoreExtensionHost
import com.facebook.rendercore.RenderCoreSystrace
import com.facebook.rendercore.Systracer
import java.util.ArrayDeque

/**
 * The base class for all RenderCore Extensions.
 *
 * @param Input the state the extension operates on.
 */
open class RenderCoreExtension<Input, State> {

  /**
   * The extension can optionally return a [LayoutResultVisitor] for every layout pass which will
   * visit every [LayoutResult]. The visitor should be functional and immutable.
   *
   * @return a [LayoutResultVisitor].
   */
  open fun getLayoutVisitor(): LayoutResultVisitor<out Input>? = null

  /**
   * The extension can optionally return a [MountExtension] which can be used to augment the
   * RenderCore's mounting phase. The [Input] collected in the latest layout pass will be passed to
   * the extension before mount.
   *
   * @return a [MountExtension].
   */
  open fun getMountExtension(): MountExtension<out Input, State>? = null

  /**
   * Should return a new [Input] to which the [LayoutResultVisitor] can write into.
   *
   * @return A new [Input] for [LayoutResultVisitor] to write into.
   */
  open fun createInput(): Input? = null

  companion object {
    /**
     * Calls [MountExtension.beforeMount] for each [RenderCoreExtension] that has a mount phase.
     *
     * @param host The [Host] of the extensions.
     * @param mountDelegate The [MountDelegate].
     * @param results A map of [RenderCoreExtension] to their results from the layout phase.
     */
    @JvmStatic
    fun beforeMount(
        host: Host,
        mountDelegate: MountDelegate?,
        results: List<Pair<RenderCoreExtension<*, *>, Any>>?
    ) {
      if (mountDelegate != null && results != null) {
        val visibleRect = Rect()
        host.getLocalVisibleRect(visibleRect)
        mountDelegate.beforeMount(results, visibleRect)
      }
    }

    /**
     * Calls [MountExtension.afterMount] for each [RenderCoreExtension] that has a mount phase.
     *
     * @param mountDelegate The [MountDelegate].
     */
    @JvmStatic
    fun afterMount(mountDelegate: MountDelegate?) {
      mountDelegate?.afterMount()
    }

    /**
     * Calls [VisibleBoundsCallbacks.onVisibleBoundsChanged] for each [RenderCoreExtension] that has
     * a mount phase.
     *
     * @param host The [Host] of the extensions
     */
    @JvmStatic
    fun notifyVisibleBoundsChanged(target: MountDelegateTarget, host: Host) {
      val delegate = target.getMountDelegate()
      if (delegate != null) {
        val rect = Rect()
        host.getLocalVisibleRect(rect)
        delegate.notifyVisibleBoundsChanged(rect)
      }
    }

    @JvmStatic
    fun onRegisterForPremount(target: MountDelegateTarget, frameTimeMs: Long?) {
      val delegate = target.getMountDelegate()
      delegate?.onRegisterForPremount(frameTimeMs)
    }

    @JvmStatic
    fun onUnregisterForPremount(target: MountDelegateTarget) {
      val delegate = target.getMountDelegate()
      delegate?.onUnregisterForPremount()
    }

    /** returns `false` iff the results have the same [RenderCoreExtension]s. */
    @JvmStatic
    fun shouldUpdate(
        current: List<Pair<RenderCoreExtension<*, *>, Any>>?,
        next: List<Pair<RenderCoreExtension<*, *>, Any>>?
    ): Boolean {
      if (current === next) {
        return false
      }
      if (current == null || next == null) {
        return true
      }
      if (current.size != next.size) {
        return true
      }
      for (i in current.indices) {
        if (current[i].first != next[i].first) {
          return true
        }
      }
      return false
    }

    @JvmStatic
    @JvmOverloads
    fun recursivelyNotifyVisibleBoundsChanged(content: Any?, tracer: Systracer? = null) {
      val systracer = tracer ?: RenderCoreSystrace.getInstance()
      systracer.beginSection("recursivelyNotifyVisibleBoundsChanged")

      if (content != null) {
        val contentStack = ArrayDeque<Any>()
        contentStack.add(content)

        while (!contentStack.isEmpty()) {
          val currentContent = contentStack.pop()
          if (currentContent is RenderCoreExtensionHost) {
            currentContent.notifyVisibleBoundsChanged()
          } else if (currentContent is ViewGroup) {
            for (i in currentContent.childCount - 1 downTo 0) {
              contentStack.push(currentContent.getChildAt(i))
            }
          }
        }
      }

      systracer.endSection()
    }
  }
}
