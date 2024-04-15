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
import android.util.AttributeSet
import androidx.annotation.VisibleForTesting
import com.facebook.rendercore.RootHostDelegate.Companion.MAX_REMOUNT_RETRIES
import com.facebook.rendercore.extensions.RenderCoreExtension

private const val TAG = "RenderTreeHostView"

open class RenderTreeHostView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) :
    HostView(context, attrs), RenderTreeHost {

  private val mountState = MountState(this)
  private var currentRenderTree: RenderTree? = null

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val renderTree = currentRenderTree
    if (renderTree == null) {
      setMeasuredDimension(0, 0)
    } else {
      setMeasuredDimension(renderTree.width, renderTree.height)
    }
  }

  override fun performLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    if (currentRenderTree != null) {
      var renderTree = currentRenderTree
      mountState.mount(renderTree)
      // We could run into the case that mounting a tree ends up requesting another mount.
      // We need to keep re-mounting until the mounted renderTree matches the mCurrentRenderTree.
      var retries = 0
      while (renderTree !== currentRenderTree) {
        if (retries > MAX_REMOUNT_RETRIES) {
          ErrorReporter.report(
              LogLevel.ERROR,
              TAG,
              "More than $MAX_REMOUNT_RETRIES recursive mount attempts. Skipping mounting the latest version.")
          return
        }

        renderTree = currentRenderTree
        mountState.mount(renderTree)
        retries++
      }
    }

    performLayoutOnChildrenIfNecessary(this)
  }

  /**
   * Sets render tree and requests layout
   *
   * @param tree if null, it unmounts all items
   */
  override fun setRenderTree(tree: RenderTree?) {
    if (currentRenderTree === tree) {
      return
    }

    if (tree == null) {
      mountState.unmountAllItems()
    }

    currentRenderTree = tree
    this.requestLayout()
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
  public override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    mountState.detach()
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
  public override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    mountState.attach()
  }

  override fun notifyVisibleBoundsChanged() {
    RenderCoreExtension.notifyVisibleBoundsChanged(mountState, this)
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

  override fun offsetTopAndBottom(offset: Int) {
    if (offset != 0) {
      super.offsetTopAndBottom(offset)
      notifyVisibleBoundsChanged()
    }
  }

  override fun offsetLeftAndRight(offset: Int) {
    if (offset != 0) {
      super.offsetLeftAndRight(offset)
      notifyVisibleBoundsChanged()
    }
  }

  override fun setTranslationX(translationX: Float) {
    if (translationX != getTranslationX()) {
      super.setTranslationX(translationX)
      notifyVisibleBoundsChanged()
    }
  }

  override fun setTranslationY(translationY: Float) {
    if (translationY != getTranslationY()) {
      super.setTranslationY(translationY)
      notifyVisibleBoundsChanged()
    }
  }

  fun getCurrentRenderTree(): RenderTree? {
    return currentRenderTree
  }

  fun findMountContentById(id: Long): Any? {
    return mountState.getContentById(id)
  }
}
