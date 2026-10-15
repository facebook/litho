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

package com.facebook.litho.animation

import android.graphics.drawable.Drawable
import com.facebook.litho.OutputUnitsAffinityGroup
import com.facebook.litho.dataflow.ValueNode
import java.lang.ref.WeakReference

/**
 * A ValueNode that allows getting and/or setting the value of a specific property (x, y, scale,
 * text color, etc) on a given mount content (View or Drawable).
 *
 * If there is no input hooked up to this node, it will output the current value of this property on
 * the current mount content. Otherwise, this node will set the property of the given mount content
 * to that input value and pass on that value as an output.
 */
class AnimatedPropertyNode(
    mountContentGroup: OutputUnitsAffinityGroup<Any>?,
    private val animatedProperty: AnimatedProperty,
) : ValueNode() {

  private val mountContentGroup: OutputUnitsAffinityGroup<WeakReference<Any?>> =
      OutputUnitsAffinityGroup()
  private var usingRenderThread: Boolean = false

  init {
    setMountContentGroupInner(mountContentGroup)
  }

  /** Sets the mount content that this [AnimatedPropertyNode] updates a value on. */
  fun setMountContentGroup(mountContentGroup: OutputUnitsAffinityGroup<Any>?) {
    setMountContentGroupInner(mountContentGroup)
    setValueInner(value)
  }

  override var value: Float
    get() = super.value
    set(value) {
      super.value = value
      setValueInner(value)
    }

  override fun calculateValue(frameTimeNanos: Long): Float {
    val hasInput = hasInput()
    val mountContent = resolveReference(mountContentGroup.getMostSignificantUnit())

    if (mountContent == null) {
      if (hasInput) {
        return getInput().value
      }

      // If we have no input and have lost our mount content, just return our last known value.
      return value
    }

    if (!hasInput) {
      return animatedProperty.get(mountContent)
    }

    val inputValue = getInput().value
    setValueInner(inputValue)

    return inputValue
  }

  /**
   * Sets if the content is being animated on the Render thread, which means that the further passed
   * are not be applied to the content, but just to be recorded
   */
  internal fun setUsingRenderThread(usingRenderThread: Boolean) {
    this.usingRenderThread = usingRenderThread
  }

  private fun setMountContentGroupInner(mountContentGroup: OutputUnitsAffinityGroup<Any>?) {
    this.mountContentGroup.clean()
    if (mountContentGroup == null) {
      return
    }
    for (i in 0 until mountContentGroup.size()) {
      val mountContentRef = WeakReference(mountContentGroup.getAt(i))
      this.mountContentGroup.add(mountContentGroup.typeAt(i), mountContentRef)
    }
  }

  private fun setValueInner(value: Float) {
    if (usingRenderThread) {
      return
    }

    for (i in 0 until mountContentGroup.size()) {
      val mountContent = resolveReference(mountContentGroup.getAt(i))
      if (mountContent != null) {
        animatedProperty.set(mountContent, value)
      }
    }
  }

  companion object {
    private fun resolveReference(mountContentRef: WeakReference<Any?>?): Any? {
      val mountContent = mountContentRef?.get() ?: return null
      if (mountContent is Drawable && mountContent.callback == null) {
        // MountContent have been already unmounted but we still hold reference to it, so let's
        // clear up the reference and return null. Actual unmounting of a view can be deferred,
        // so don't check it here
        mountContentRef.clear()
        return null
      }
      return mountContent
    }
  }
}
