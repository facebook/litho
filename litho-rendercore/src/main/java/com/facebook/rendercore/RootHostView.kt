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

open class RootHostView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    HostView(context, attrs), RootHost {

  val rootHostDelegate: RootHostDelegate = RootHostDelegate(this)

  override fun setRenderState(renderState: RenderState<*, *, *>?) {
    rootHostDelegate.setRenderState(renderState)
  }

  override fun notifyVisibleBoundsChanged() {
    rootHostDelegate.notifyVisibleBoundsChanged()
  }

  override fun onRegisterForPremount(frameTimeMs: Long?) {
    rootHostDelegate.onRegisterForPremount(frameTimeMs)
  }

  override fun onUnregisterForPremount() {
    rootHostDelegate.onUnregisterForPremount()
  }

  override fun setRenderTreeUpdateListener(listener: RenderTreeUpdateListener?) {
    rootHostDelegate.setRenderTreeUpdateListener(listener)
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    if (rootHostDelegate.onMeasure(
        SizeConstraints.fromMeasureSpecs(widthMeasureSpec, heightMeasureSpec), MEASURE_OUTPUTS)) {
      setMeasuredDimension(MEASURE_OUTPUTS[0], MEASURE_OUTPUTS[1])
    } else {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
  }

  override fun performLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    rootHostDelegate.onLayout(changed, l, t, r, b)
    performLayoutOnChildrenIfNecessary(this)
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
  public override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    rootHostDelegate.detach()
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
  public override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    rootHostDelegate.attach()
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

  fun findMountContentById(id: Long): Any? {
    return rootHostDelegate.findMountContentById(id)
  }

  companion object {
    private val MEASURE_OUTPUTS = IntArray(2)
  }
}
