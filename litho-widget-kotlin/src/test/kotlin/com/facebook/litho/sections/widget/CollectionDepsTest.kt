/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.litho.sections.widget

import android.os.Looper
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.Style
import com.facebook.litho.testing.BackgroundLayoutLooperRule
import com.facebook.litho.testing.LithoViewRule
import com.facebook.litho.view.alpha
import com.facebook.litho.view.rotation
import com.facebook.litho.view.scale
import com.facebook.litho.view.viewTag
import com.facebook.litho.widget.Text
import org.assertj.core.api.Assertions.anyOf
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Condition
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.LooperMode

/** Tests for [Collection]'s deps prop */
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(AndroidJUnit4::class)
class CollectionDepsTest {

  @Rule @JvmField val lithoViewRule = LithoViewRule()
  @Rule @JvmField val backgroundLayoutLooperRule = BackgroundLayoutLooperRule()

  private val updatedAlpha =
      object : Condition<View>() {
        override fun matches(value: View?): Boolean {
          return value?.alpha == 1f
        }
      } as
          Condition<in Any>

  private val updatedRotation =
      object : Condition<View>() {
        override fun matches(value: View?): Boolean {
          return value?.rotation == 0f
        }
      } as
          Condition<in Any>

  private val updatedScale =
      object : Condition<View>() {
        override fun matches(value: View?): Boolean {
          return value?.scaleX != 0.75f
        }
      } as
          Condition<in Any>

  @Test
  fun collection_propUpdate_appliedToDependantChildren() {
    class CollectionWithSelectedRows(val alpha: Float, val rotation: Float, val scale: Float) :
        KComponent() {
      override fun ComponentScope.render(): Component? {
        return Collection {
          child {
            Text(
                "deps_null",
                style = Style.viewTag("deps_null").alpha(alpha).rotation(rotation).scale(scale))
          }
          child(deps = arrayOf(alpha)) {
            Text(
                "deps_alpha",
                style = Style.viewTag("deps_alpha").alpha(alpha).rotation(rotation).scale(scale))
          }
          child(deps = arrayOf(scale)) {
            Text(
                "deps_scale",
                style = Style.viewTag("deps_scale").alpha(alpha).rotation(rotation).scale(scale))
          }
          child(deps = arrayOf()) {
            Text(
                "deps_empty",
                style = Style.viewTag("deps_empty").alpha(alpha).rotation(rotation).scale(scale))
          }
        }
      }
    }

    lithoViewRule
        .setRoot(CollectionWithSelectedRows(.5f, 180f, 0.75f))
        .setSizePx(1000, 1000)
        .measure()
        .layout()
        .attachToWindow()

    // Update alpha and rotation. Do not update scale.
    layoutCollectionWithNewStatesOrProps { CollectionWithSelectedRows(1f, 0f, 0.75f) }

    // Verify an update is triggered for default/null deps
    val childWithNullDeps = lithoViewRule.findViewWithTag("deps_null")
    assertThat(childWithNullDeps).has(updatedAlpha).has(updatedRotation).doesNotHave(updatedScale)

    // Verify an update is triggered due to dependency on alpha. Note that rotation will also be
    // updated.
    val childWithAlphaDeps = lithoViewRule.findViewWithTag("deps_alpha")
    assertThat(childWithAlphaDeps).has(updatedAlpha).has(updatedRotation).doesNotHave(updatedScale)

    // Verify no update triggered when deps does not include any updated props
    val childWithScaleDeps = lithoViewRule.findViewWithTag("deps_scale")
    assertThat(childWithScaleDeps).doesNotHave(anyOf(updatedAlpha, updatedRotation, updatedScale))

    // Verify no update triggered when deps does not include any updated props
    val childWithEmptyDeps = lithoViewRule.findViewWithTag("deps_empty")
    assertThat(childWithEmptyDeps).doesNotHave(anyOf(updatedAlpha, updatedRotation, updatedScale))
  }

  private fun layoutCollectionWithNewStatesOrProps(component: () -> Component) {
    // We need to do slightly more work to simulate a Collection re-render as various background
    // tasks need to run to completion.
    lithoViewRule.setRoot(component()).measure().layout().attachToWindow()
    backgroundLayoutLooperRule.runToEndOfTasksSync()
    Shadows.shadowOf(Looper.getMainLooper()).idle()
    lithoViewRule.setRoot(component()).measure().layout().attachToWindow()
  }
}
