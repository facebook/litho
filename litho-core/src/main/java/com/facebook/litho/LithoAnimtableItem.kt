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

import android.graphics.Rect

/**
 * Litho's implementation of the [AnimatableItem] required by the [TransitionsExtension] to power
 * animations. This object should NOT be used to inform the should update during mounting, therefore
 * it should NOT be used to host any such information.
 */
class LithoAnimtableItem(
    private val id: Long,
    private val absoluteBounds: Rect,
    @field:OutputUnitType @param:OutputUnitType private val outputType: Int,
    private val nodeInfo: NodeInfo?,
    private val transitionId: TransitionId?
) : AnimatableItem {

  override fun getId(): Long {
    return id
  }

  override fun getAbsoluteBounds(): Rect {
    return absoluteBounds
  }

  override fun getOutputType(): Int {
    return outputType
  }

  override fun getTransitionId(): TransitionId? {
    return transitionId
  }

  override fun getScale(): Float {
    return nodeInfo?.scale ?: 1.0f
  }

  override fun getAlpha(): Float {
    return nodeInfo?.alpha ?: 1.0f
  }

  override fun getRotation(): Float {
    return nodeInfo?.rotation ?: 0.0f
  }

  override fun getRotationX(): Float {
    return nodeInfo?.rotationX ?: 0.0f
  }

  override fun getRotationY(): Float {
    return nodeInfo?.rotationY ?: 0.0f
  }

  override fun isScaleSet(): Boolean {
    return nodeInfo != null && nodeInfo.isScaleSet
  }

  override fun isAlphaSet(): Boolean {
    return nodeInfo != null && nodeInfo.isAlphaSet
  }

  override fun isRotationSet(): Boolean {
    return nodeInfo != null && nodeInfo.isRotationSet
  }

  override fun isRotationXSet(): Boolean {
    return nodeInfo != null && nodeInfo.isRotationXSet
  }

  override fun isRotationYSet(): Boolean {
    return nodeInfo != null && nodeInfo.isRotationYSet
  }
}
