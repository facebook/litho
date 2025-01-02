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

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.UiThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.facebook.litho.config.ComponentsConfiguration

/**
 * An implementation of BaseMountingView that can mount a LayoutState without the need for a
 * ComponentTree
 */
class LithoRenderTreeView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    BaseMountingView(context, attrs), LifecycleEventObserver {

  private var hasNewTree = false

  // Set together
  private var layoutState: LayoutState? = null
  private var _treeState: TreeState? = null

  private var currentLifecycleOwner: LifecycleOwner? = null

  private val requireTreeState: TreeState
    get() {
      return requireNotNull(treeState) { "TreeState not available." }
    }

  private val requireLayoutState: LayoutState
    get() {
      return requireNotNull(layoutState) { "LayoutState not available." }
    }

  var onClean: (() -> Unit)? = null

  override fun onLifecycleOwnerChanged(
      previousLifecycleOwner: LifecycleOwner?,
      currentLifecycleOwner: LifecycleOwner?
  ) {
    previousLifecycleOwner?.lifecycle?.removeObserver(this)
    currentLifecycleOwner?.lifecycle?.addObserver(this)
  }

  override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
    if (event == Lifecycle.Event.ON_DESTROY) {
      clean()
    }
  }

  override val configuration: ComponentsConfiguration?
    get() = layoutState?.componentContext?.lithoConfiguration?.componentsConfig

  override val isIncrementalMountEnabled: Boolean
    get() {
      val componentContext = layoutState?.componentContext ?: return false
      return ComponentContext.isIncrementalMountEnabled(componentContext)
    }

  override val currentLayoutState: LayoutState?
    get() = layoutState

  override val isVisibilityProcessingEnabled: Boolean
    get() {
      val componentContext = layoutState?.componentContext ?: return false
      return ComponentContext.isVisibilityProcessingEnabled(componentContext)
    }

  override val treeState: TreeState?
    get() = _treeState

  override val hasTree: Boolean
    get() = layoutState != null

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    // mAnimatedWidth/mAnimatedHeight >= 0 if something is driving a width/height animation.
    val isAnimating = animatedWidth != -1 || animatedHeight != -1
    // up to date view sizes, taking into account running animations
    val upToDateWidth = if (animatedWidth != -1) animatedWidth else width
    val upToDateHeight = if (animatedHeight != -1) animatedHeight else height
    animatedWidth = -1
    animatedHeight = -1
    if (isAnimating) {
      // If the mount state is dirty, we want to ignore the current animation and calculate the
      // new LayoutState as normal below. That LayoutState has the opportunity to define its own
      // transition to a new width/height from the current height of the LithoRenderTreeView, or if
      // not we will jump straight to that width/height.
      if (!isMountStateDirty) {
        setMeasuredDimension(upToDateWidth, upToDateHeight)
        return
      }
    }
    if (layoutState == null) {
      setMeasuredDimension(
          if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY)
              MeasureSpec.getSize(widthMeasureSpec)
          else 0,
          if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY)
              MeasureSpec.getSize(heightMeasureSpec)
          else 0)
      return
    }
    val hasMounted = requireTreeState.mountInfo.hasMounted

    val canAnimateRootBounds = !hasNewTree || !hasMounted
    var width = requireLayoutState.width
    var height = requireLayoutState.height
    if (canAnimateRootBounds) {
      // We might need to collect transitions before mount to know whether this LithoRenderTreeView
      // width or height is animated.
      maybeCollectAllTransitions()
      val initialAnimatedWidth = getInitialAnimatedMountingViewWidth(upToDateWidth, hasNewTree)
      if (initialAnimatedWidth != -1) {
        width = initialAnimatedWidth
      }
      val initialAnimatedHeight = getInitialAnimatedMountingViewHeight(upToDateHeight, hasNewTree)
      if (initialAnimatedHeight != -1) {
        height = initialAnimatedHeight
      }
    }
    setMeasuredDimension(width, height)
    hasNewTree = false
  }

  @UiThread
  fun setLayoutState(layoutState: LayoutState, treeState: TreeState) {
    ThreadUtils.assertMainThread()
    if (this.layoutState === layoutState) {
      require(this.treeState === treeState)
      if (isAttached) {
        rebind()
      }
      return
    }

    hasNewTree =
        this.layoutState == null ||
            (requireLayoutState.componentTreeId != layoutState.componentTreeId)
    setMountStateDirty()
    if (this.layoutState != null && hasNewTree) {
      onBeforeSettingNewTree()
    }
    this.layoutState = layoutState
    this._treeState = treeState

    if (this.layoutState == null) {
      clearDebugOverlay(this)
    }

    setupMountExtensions()
    requestLayout()
  }

  fun clean() {
    unmountAllItems()
    onClean?.invoke()
    onClean = null
    currentLifecycleOwner?.lifecycle?.removeObserver(this)
    currentLifecycleOwner = null
    layoutState = null
    _treeState = null
    hasNewTree = true
    requestLayout()
  }
}
