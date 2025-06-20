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

import android.graphics.PathEffect
import kotlin.jvm.JvmField

/**
 * This class is a placeholder for the unresolved layout and result of a [Component]s which
 * implement the [com.facebook.litho.annotations.OnCreateLayoutWithSizeSpec] and
 * [com.facebook.litho.layout.RenderWithConstraints].The [TreePropContainer], padding and border
 * width properties and held separately so that they can be copied into the actual deferred node
 * result before measuring it.
 */
class DeferredLithoNode
@JvmOverloads
constructor(
    propContainer: TreePropContainer? = null,
    /**
     * When a node is measured during Component.measure and a layout-result is cached, it is cached
     * using that node as the key. Later, this layout may resolve a deffered node, and so in order
     * to be able to access this cache, this node is used.
     */
    val cachedNode: LithoNode? = null,
    @JvmField var parentContext: ComponentContext? = null
) : LithoNode() {

  val pendingTreePropContainer: TreePropContainer? = TreePropContainer.copy(propContainer)

  @JvmField var deferredBorders: IntArray? = null
  @JvmField var deferredPaddings: Edges? = null
  @JvmField var deferredIsPaddingPercentages: BooleanArray? = null

  override fun border(widths: IntArray, colors: IntArray, radii: FloatArray, effect: PathEffect?) {
    val deferredBorders = IntArray(Border.EDGE_COUNT)
    System.arraycopy(widths, 0, deferredBorders, 0, deferredBorders.size)
    System.arraycopy(colors, 0, borderColors, 0, borderColors.size)
    System.arraycopy(radii, 0, borderRadius, 0, borderRadius.size)
    borderPathEffect = effect
    this.deferredBorders = deferredBorders
  }

  override fun createLayoutResult(layoutOutput: YogaLayoutOutput): DeferredLithoLayoutResult =
      DeferredLithoLayoutResult(
          c = tailComponentContext, deferredNode = this, layoutOutput = layoutOutput)

  fun copyInto(target: LithoNode) {
    // Defer copying, and set this DeferredNode on the target. The props will be
    // transferred to the deferred result during layout calculation.
    target.setDeferredNode(this)
  }

  fun transferInto(target: LithoNode) {
    if (nodeInfo != null) {
      target.applyNodeInfo(nodeInfo)
    }
    if (target.isImportantForAccessibilityIsSet) {
      target.importantForAccessibility(importantForAccessibility)
    }
    target.duplicateParentState(isDuplicateParentStateEnabled)
    if (privateFlags and PFLAG_DUPLICATE_CHILDREN_STATES_IS_SET != 0L) {
      target.duplicateChildrenStates(isDuplicateChildrenStatesEnabled)
    }
    if (privateFlags and PFLAG_BACKGROUND_IS_SET != 0L) {
      target.background(background)
      target.paddingFromBackground = paddingFromBackground
    }
    if (privateFlags and PFLAG_FOREGROUND_IS_SET != 0L) {
      target.foreground(foreground)
    }
    if (isForceViewWrapping) {
      target.wrapInView()
    }
    if (privateFlags and PFLAG_VISIBLE_HANDLER_IS_SET != 0L) {
      target.visibleHandler(visibleHandler)
    }
    if (privateFlags and PFLAG_FOCUSED_HANDLER_IS_SET != 0L) {
      target.focusedHandler(focusedHandler)
    }
    if (privateFlags and PFLAG_FULL_IMPRESSION_HANDLER_IS_SET != 0L) {
      target.fullImpressionHandler(fullImpressionHandler)
    }
    if (privateFlags and PFLAG_INVISIBLE_HANDLER_IS_SET != 0L) {
      target.invisibleHandler(invisibleHandler)
    }
    if (privateFlags and PFLAG_UNFOCUSED_HANDLER_IS_SET != 0L) {
      target.unfocusedHandler(unfocusedHandler)
    }
    if (privateFlags and PFLAG_VISIBLE_RECT_CHANGED_HANDLER_IS_SET != 0L) {
      target.visibilityChangedHandler(visibilityChangedHandler)
    }
    if (testKey != null) {
      target.testKey(testKey)
    }
    deferredBorders?.let { target.border(it, borderColors, borderRadius, borderPathEffect) }
    if (privateFlags and PFLAG_TRANSITION_KEY_IS_SET != 0L) {
      target.transitionKey(transitionKey, transitionOwnerKey)
    }
    if (privateFlags and PFLAG_TRANSITION_KEY_TYPE_IS_SET != 0L) {
      target.transitionKeyType(transitionKeyType)
    }
    if (visibleHeightRatio != 0f) {
      target.visibleHeightRatio(visibleHeightRatio)
    }
    if (visibleWidthRatio != 0f) {
      target.visibleWidthRatio(visibleWidthRatio)
    }
    if (privateFlags and PFLAG_STATE_LIST_ANIMATOR_SET != 0L) {
      target.stateListAnimator(stateListAnimator)
    }
    if (privateFlags and PFLAG_STATE_LIST_ANIMATOR_RES_SET != 0L) {
      target.stateListAnimatorRes(stateListAnimatorRes)
    }
    if (layerType != LayerType.LAYER_TYPE_NOT_SET) {
      target.layerType(layerType, layerPaint)
    }
    target.setDeferredPadding(deferredPaddings, deferredIsPaddingPercentages)
  }
}
