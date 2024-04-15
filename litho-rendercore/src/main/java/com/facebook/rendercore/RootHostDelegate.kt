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

import com.facebook.infer.annotation.ThreadConfined
import com.facebook.rendercore.extensions.RenderCoreExtension

class RootHostDelegate(private val host: Host) : RenderState.HostListener, RootHost {

  private val mountState: MountState = MountState(host)

  private var currentRenderTree: RenderTree? = null
  private var doMeasureInLayout = false

  private var _renderState: RenderState<*, *, *>? = null

  val renderState: RenderState<*, *, *>?
    get() = _renderState

  @ThreadConfined(ThreadConfined.UI)
  override fun setRenderState(renderState: RenderState<*, *, *>?) {
    if (_renderState == renderState) {
      return
    }
    _renderState?.detach()
    _renderState = renderState
    if (renderState != null) {
      renderState.attach(this)
      onUIRenderTreeUpdated(renderState.uiRenderTree)
    } else {
      onUIRenderTreeUpdated(null)
    }
  }

  override fun onUIRenderTreeUpdated(newRenderTree: RenderTree?) {
    if (currentRenderTree == newRenderTree) {
      return
    }
    if (newRenderTree == null) {
      mountState.unmountAllItems()
    }
    currentRenderTree = newRenderTree
    host.requestLayout()
  }

  override fun notifyVisibleBoundsChanged() {
    RenderCoreExtension.notifyVisibleBoundsChanged(mountState, host)
  }

  override fun onRegisterForPremount(frameTimeMs: Long?) {
    RenderCoreExtension.onRegisterForPremount(mountState, frameTimeMs)
  }

  override fun onUnregisterForPremount() {
    RenderCoreExtension.onUnregisterForPremount(mountState)
  }

  override fun setRenderTreeUpdateListener(listener: RenderTreeUpdateListener?) {
    mountState.setRenderTreeUpdateListener(listener)
  }

  /**
   * Returns true if the delegate has defined a size and filled the measureOutput array, returns
   * false if not in which case the hosting view should call super.onMeasure.
   */
  fun onMeasure(sizeConstraints: SizeConstraints, measureOutput: IntArray): Boolean {
    if (sizeConstraints.hasExactWidth && sizeConstraints.hasExactHeight) {
      // If the measurements are exact, postpone LayoutState calculation from measure to layout.
      // This is part of the fix for android's double measure bug. Doing this means that if we get
      // remeasured with different exact measurements, we don't compute two layouts.
      doMeasureInLayout = true
      measureOutput[0] = sizeConstraints.maxWidth
      measureOutput[1] = sizeConstraints.maxHeight
      return true
    }
    val renderState = _renderState
    if (renderState != null) {
      renderState.measure(sizeConstraints, measureOutput)
      doMeasureInLayout = false
      return true
    }
    return false
  }

  fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
    val renderState = _renderState
    if (doMeasureInLayout && renderState != null) {
      renderState.measure(SizeConstraints.exact(right - left, bottom - top), null)
      doMeasureInLayout = false
    }

    if (currentRenderTree != null) {
      var renderTree: RenderTree? = currentRenderTree
      mountState.mount(renderTree)
      // We could run into the case that mounting a tree ends up requesting another mount.
      // We need to keep re-mounting untile the mounted renderTree matches the mCurrentRenderTree.
      var retries = 0
      while (renderTree != currentRenderTree) {
        if (retries > MAX_REMOUNT_RETRIES) {
          ErrorReporter.report(
              LogLevel.ERROR,
              TAG,
              "More than " +
                  MAX_REMOUNT_RETRIES +
                  " recursive mount attempts. Skipping mounting the latest version.")
          return
        }
        renderTree = currentRenderTree
        mountState.mount(renderTree)
        retries++
      }
    }
  }

  fun findMountContentById(id: Long): Any? {
    return mountState.getContentById(id)
  }

  fun detach() {
    mountState.detach()
  }

  fun attach() {
    mountState.attach()
  }

  companion object {
    const val MAX_REMOUNT_RETRIES: Int = 4
    private const val TAG = "RootHostDelegate"
  }
}
