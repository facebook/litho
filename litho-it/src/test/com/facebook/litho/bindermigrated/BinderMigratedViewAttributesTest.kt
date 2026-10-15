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

import android.graphics.Color
import android.graphics.Paint
import android.util.SparseArray
import android.view.View
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BinderMigratedViewAttributesTest {

  @Test
  fun `copyInto leaves unset guarded attributes unchanged on target`() {
    val source = BinderMigratedViewAttributes()
    val target =
        BinderMigratedViewAttributes().apply {
          scale = 2f
          alpha = 0.5f
          rotation = 45f
          rotationX = 15f
          rotationY = 30f
          clipChildren = false
          defaultFocusHighlightEnabled = false
          shadowElevation = 6f
          ambientShadowColor = Color.RED
          spotShadowColor = Color.BLUE
          isFocusable = true
          isClickable = true
          isEnabled = true
          isSelected = true
          isKeyboardNavigationCluster = true
        }

    source.copyInto(target)

    assertThat(target.scale).isEqualTo(2f)
    assertThat(target.alpha).isEqualTo(0.5f)
    assertThat(target.rotation).isEqualTo(45f)
    assertThat(target.rotationX).isEqualTo(15f)
    assertThat(target.rotationY).isEqualTo(30f)
    assertThat(target.clipChildren).isFalse()
    assertThat(target.defaultFocusHighlightEnabled).isFalse()
    assertThat(target.shadowElevation).isEqualTo(6f)
    assertThat(target.ambientShadowColor).isEqualTo(Color.RED)
    assertThat(target.spotShadowColor).isEqualTo(Color.BLUE)
    assertThat(target.isFocusable).isTrue()
    assertThat(target.isClickable).isTrue()
    assertThat(target.isEnabled).isTrue()
    assertThat(target.isSelected).isTrue()
    assertThat(target.isKeyboardNavigationCluster).isTrue()
  }

  @Test
  fun `copyInto copies guarded attributes explicitly set to default values`() {
    val source =
        BinderMigratedViewAttributes().apply {
          rotationX = 0f
          rotationY = 0f
          clipChildren = true
          defaultFocusHighlightEnabled = true
          shadowElevation = 0f
          ambientShadowColor = Color.BLACK
          spotShadowColor = Color.BLACK
          isFocusable = false
          isClickable = false
          isEnabled = false
          isSelected = false
          isKeyboardNavigationCluster = false
        }
    val target =
        BinderMigratedViewAttributes().apply {
          rotationX = 15f
          rotationY = 30f
          clipChildren = false
          defaultFocusHighlightEnabled = false
          shadowElevation = 6f
          ambientShadowColor = Color.RED
          spotShadowColor = Color.BLUE
          isFocusable = true
          isClickable = true
          isEnabled = true
          isSelected = true
          isKeyboardNavigationCluster = true
        }

    source.copyInto(target)

    assertThat(target.rotationX).isZero()
    assertThat(target.rotationY).isZero()
    assertThat(target.clipChildren).isTrue()
    assertThat(target.defaultFocusHighlightEnabled).isTrue()
    assertThat(target.shadowElevation).isZero()
    assertThat(target.ambientShadowColor).isEqualTo(Color.BLACK)
    assertThat(target.spotShadowColor).isEqualTo(Color.BLACK)
    assertThat(target.isFocusable).isFalse()
    assertThat(target.isClickable).isFalse()
    assertThat(target.isEnabled).isFalse()
    assertThat(target.isSelected).isFalse()
    assertThat(target.isKeyboardNavigationCluster).isFalse()
  }

  @Test
  fun `copyInto does not copy scale alpha and rotation after reset to default`() {
    val source =
        BinderMigratedViewAttributes().apply {
          scale = 2f
          scale = 1f
          alpha = 0.5f
          alpha = 1f
          rotation = 45f
          rotation = 0f
        }
    val target =
        BinderMigratedViewAttributes().apply {
          scale = 3f
          alpha = 0.25f
          rotation = 90f
        }

    source.copyInto(target)

    assertThat(source.isScaleSet).isFalse()
    assertThat(source.isAlphaSet).isFalse()
    assertThat(source.isRotationSet).isFalse()
    assertThat(target.scale).isEqualTo(3f)
    assertThat(target.alpha).isEqualTo(0.25f)
    assertThat(target.rotation).isEqualTo(90f)
  }

  @Test
  fun `copyInto preserves nullable target attributes when source values are null`() {
    val paint = Paint().apply { color = Color.GREEN }
    val target =
        BinderMigratedViewAttributes().apply {
          tooltipText = "tooltip"
          viewTag = "tag"
          transitionName = "transition"
          layoutPaint = paint
        }

    BinderMigratedViewAttributes().copyInto(target)

    assertThat(target.tooltipText).isEqualTo("tooltip")
    assertThat(target.viewTag).isEqualTo("tag")
    assertThat(target.transitionName).isEqualTo("transition")
    assertThat(target.layoutPaint).isSameAs(paint)
  }

  @Test
  fun `copyInto always copies view id clip to outline and layer type`() {
    val source =
        BinderMigratedViewAttributes().apply {
          viewId = View.NO_ID
          clipToOutline = false
          layerType = LayerType.LAYER_TYPE_SOFTWARE
        }
    val target =
        BinderMigratedViewAttributes().apply {
          viewId = 23
          clipToOutline = true
          layerType = LayerType.LAYER_TYPE_HARDWARE
        }

    source.copyInto(target)

    assertThat(target.viewId).isEqualTo(View.NO_ID)
    assertThat(target.clipToOutline).isFalse()
    assertThat(target.layerType).isEqualTo(LayerType.LAYER_TYPE_SOFTWARE)
  }

  @Test
  fun `addViewTags stores first sparse array and merges later tags`() {
    val attributes = BinderMigratedViewAttributes()
    val firstTags = sparseArrayOf(1 to "first")
    val secondTags = sparseArrayOf(2 to "second")

    attributes.addViewTags(firstTags)
    attributes.addViewTags(secondTags)

    assertThat(attributes.viewTags?.get(1)).isEqualTo("first")
    assertThat(attributes.viewTags?.get(2)).isEqualTo("second")
  }

  @Test
  fun `equals distinguishes explicit default flags from unset defaults`() {
    val unset = BinderMigratedViewAttributes()
    val explicitlySetDefault = BinderMigratedViewAttributes().apply { rotationX = 0f }

    assertThat(explicitlySetDefault).isNotEqualTo(unset)
  }

  @Test
  fun `equals treats reset scale alpha and rotation as unset`() {
    val reset =
        BinderMigratedViewAttributes().apply {
          scale = 2f
          scale = 1f
          alpha = 0.5f
          alpha = 1f
          rotation = 45f
          rotation = 0f
        }

    assertThat(reset).isEqualTo(BinderMigratedViewAttributes())
  }

  private fun sparseArrayOf(vararg entries: Pair<Int, Any>): SparseArray<Any> {
    val sparseArray = SparseArray<Any>()
    entries.forEach { sparseArray.put(it.first, it.second) }
    return sparseArray
  }
}
