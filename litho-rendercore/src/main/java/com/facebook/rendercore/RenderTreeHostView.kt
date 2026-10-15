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
import com.facebook.rendercore.extensions.VisibilityChangeReason

private const val TAG = "RenderTreeHostView"

open class RenderTreeHostView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) :
    HostView(context, attrs), RenderTreeHost {

  protected open val mountState = MountState(this)

  private var currentMountInput: MountInput? = null

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val renderTree = currentMountInput?.renderTree
    if (renderTree == null) {
      setMeasuredDimension(0, 0)
    } else {
      setMeasuredDimension(renderTree.width, renderTree.height)
    }
  }

  override fun performLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    val initialMountInput = this.currentMountInput
    if (initialMountInput != null) {
      var mountInput = initialMountInput
      performMount(mountInput)
      // We could run into the case that mounting a tree ends up requesting another mount.
      // We need to keep re-mounting until the mounted renderTree matches the mCurrentRenderTree.
      var retries = 0
      while (mountInput !== this.currentMountInput) {
        if (retries > MAX_REMOUNT_RETRIES) {
          ErrorReporter.report(
              LogLevel.ERROR,
              TAG,
              "More than $MAX_REMOUNT_RETRIES recursive mount attempts. Skipping mounting the latest version.",
          )
          return
        }

        mountInput = this.currentMountInput
        if (mountInput == null) {
          break
        }
        performMount(mountInput)
        retries++
      }
    }

    performLayoutOnChildrenIfNecessary(this)
  }

  protected open fun performMount(mountInput: MountInput) {
    mountState.mount(mountInput.renderTree)
  }

  /**
   * Sets render tree and requests layout
   *
   * @param tree if null, it unmounts all items
   */
  override fun setMountInput(mountInput: MountInput?) {
    if (currentMountInput == mountInput) {
      return
    }

    if (mountInput == null) {
      mountState.unmountAllItems()
    }

    currentMountInput = mountInput
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

  override fun notifyVisibleBoundsChanged(reason: VisibilityChangeReason) {
    RenderCoreExtension.notifyVisibleBoundsChanged(mountState, this, reason)
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
      notifyVisibleBoundsChanged(VisibilityChangeReason.SCROLL)
    }
  }

  override fun offsetLeftAndRight(offset: Int) {
    if (offset != 0) {
      super.offsetLeftAndRight(offset)
      notifyVisibleBoundsChanged(VisibilityChangeReason.SCROLL)
    }
  }

  override fun setTranslationX(translationX: Float) {
    if (translationX != getTranslationX()) {
      super.setTranslationX(translationX)
      notifyVisibleBoundsChanged(VisibilityChangeReason.SCROLL)
    }
  }

  override fun setTranslationY(translationY: Float) {
    if (translationY != getTranslationY()) {
      super.setTranslationY(translationY)
      notifyVisibleBoundsChanged(VisibilityChangeReason.SCROLL)
    }
  }

  fun getCurrentRenderTree(): RenderTree? {
    return currentMountInput?.renderTree
  }

  fun getCurrentMountInput(): MountInput? = currentMountInput

  fun findMountContentById(id: Long): Any? {
    return mountState.getContentById(id)
  }
}
