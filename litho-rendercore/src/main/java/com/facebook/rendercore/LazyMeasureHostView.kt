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
import androidx.annotation.VisibleForTesting
import com.facebook.rendercore.extensions.RenderCoreExtension

class LazyMeasureHostView(context: Context) : HostView(context), RenderCoreExtensionHost {

  private val mountState: MountState = MountState(this)

  private var lazyRenderTreeProvider: LazyRenderTreeProvider<Any?>? = null
  private var currentRenderResult: RenderResult<*, Any?>? = null

  fun interface LazyRenderTreeProvider<RenderContext> {
    fun getRenderTreeForSize(
        sizeConstraints: SizeConstraints,
        previousRenderResult: RenderResult<*, RenderContext>?
    ): RenderResult<*, RenderContext>
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val renderTreeProvider = lazyRenderTreeProvider
    if (renderTreeProvider == null) {
      setMeasuredDimension(0, 0)
      currentRenderResult = null
      return
    }
    val sizeConstraints = SizeConstraints.fromMeasureSpecs(widthMeasureSpec, heightMeasureSpec)
    val renderResult = renderTreeProvider.getRenderTreeForSize(sizeConstraints, currentRenderResult)
    setMeasuredDimension(renderResult.renderTree.width, renderResult.renderTree.height)
    currentRenderResult = renderResult
  }

  override fun performLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    val renderTreeProvider = lazyRenderTreeProvider
    if (renderTreeProvider != null) {
      var lazyRenderTreeProvider: LazyRenderTreeProvider<Any?> = renderTreeProvider
      currentRenderResult?.let { mountState.mount(it.renderTree) }
      // We could run into the case that mounting a tree ends up requesting another mount.
      // We need to keep re-mounting until the mounted renderTree matches the currentRenderResult.
      var retries = 0
      while (lazyRenderTreeProvider != this.lazyRenderTreeProvider || currentRenderResult == null) {
        if (retries > RootHostDelegate.MAX_REMOUNT_RETRIES) {
          ErrorReporter.report(
              LogLevel.ERROR,
              TAG,
              "More than " +
                  RootHostDelegate.MAX_REMOUNT_RETRIES +
                  " recursive mount attempts. Skipping mounting the latest version.")
          return
        }
        lazyRenderTreeProvider = renderTreeProvider
        val renderResult =
            lazyRenderTreeProvider.getRenderTreeForSize(
                SizeConstraints.exact(r - l, b - t), currentRenderResult)
        mountState.mount(renderResult.renderTree)
        currentRenderResult = renderResult
        retries++
      }
    }
    performLayoutOnChildrenIfNecessary(this)
  }

  /**
   * Sets render lazyNavBarRenderResult and requests layout
   *
   * @param lazyRenderTreeProvider if null, it unmounts all items
   */
  @Suppress("UNCHECKED_CAST")
  fun setLazyRenderTreeProvider(lazyRenderTreeProvider: LazyRenderTreeProvider<*>?) {
    if (this.lazyRenderTreeProvider == lazyRenderTreeProvider) {
      return
    }
    if (lazyRenderTreeProvider == null) {
      currentRenderResult = null
      mountState.unmountAllItems()
    }
    this.lazyRenderTreeProvider = lazyRenderTreeProvider as LazyRenderTreeProvider<Any?>?
    requestLayout()
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
    super.offsetTopAndBottom(offset)
    notifyVisibleBoundsChanged()
  }

  override fun offsetLeftAndRight(offset: Int) {
    super.offsetLeftAndRight(offset)
    notifyVisibleBoundsChanged()
  }

  override fun setTranslationX(translationX: Float) {
    super.setTranslationX(translationX)
    notifyVisibleBoundsChanged()
  }

  override fun setTranslationY(translationY: Float) {
    super.setTranslationY(translationY)
    notifyVisibleBoundsChanged()
  }

  companion object {
    private const val TAG = "LazyMeasureHostView"
  }
}
