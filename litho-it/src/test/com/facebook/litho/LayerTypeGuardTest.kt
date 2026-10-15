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
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(LithoTestRunner::class)
class LayerTypeGuardTest {

  private var previousMax: Int = -1

  @Before
  fun setUp() {
    previousMax = LayerTypeGuard.maxHardwareLayerSizePx
  }

  @After
  fun tearDown() {
    LayerTypeGuard.maxHardwareLayerSizePx = previousMax
  }

  private fun viewOfSize(width: Int, height: Int): View =
      View(ApplicationProvider.getApplicationContext<Context>()).apply {
        layout(0, 0, width, height)
      }

  @Test
  fun hardwareLayerWithinCapIsUnchanged() {
    LayerTypeGuard.maxHardwareLayerSizePx = 16383
    assertThat(
        LayerTypeGuard.guardHardwareLayerForSize(
            viewOfSize(1080, 1920),
            LayerType.LAYER_TYPE_HARDWARE,
        ),
    )
        .isEqualTo(LayerType.LAYER_TYPE_HARDWARE)
  }

  @Test
  fun oversizedHeightHardwareLayerIsDowngradedToNone() {
    LayerTypeGuard.maxHardwareLayerSizePx = 16383
    assertThat(
        LayerTypeGuard.guardHardwareLayerForSize(
            viewOfSize(1088, 16384),
            LayerType.LAYER_TYPE_HARDWARE,
        ),
    )
        .isEqualTo(LayerType.LAYER_TYPE_NONE)
  }

  @Test
  fun oversizedWidthHardwareLayerIsDowngradedToNone() {
    LayerTypeGuard.maxHardwareLayerSizePx = 16383
    assertThat(
        LayerTypeGuard.guardHardwareLayerForSize(
            viewOfSize(16384, 1080),
            LayerType.LAYER_TYPE_HARDWARE,
        ),
    )
        .isEqualTo(LayerType.LAYER_TYPE_NONE)
  }

  @Test
  fun disabledCapLeavesOversizedHardwareLayerUnchanged() {
    LayerTypeGuard.maxHardwareLayerSizePx = -1
    assertThat(
        LayerTypeGuard.guardHardwareLayerForSize(
            viewOfSize(1088, 16384),
            LayerType.LAYER_TYPE_HARDWARE,
        ),
    )
        .isEqualTo(LayerType.LAYER_TYPE_HARDWARE)
  }

  @Test
  fun nonHardwareLayerTypesAreNeverDowngraded() {
    LayerTypeGuard.maxHardwareLayerSizePx = 16383
    val oversized = viewOfSize(1088, 16384)
    assertThat(LayerTypeGuard.guardHardwareLayerForSize(oversized, LayerType.LAYER_TYPE_SOFTWARE))
        .isEqualTo(LayerType.LAYER_TYPE_SOFTWARE)
    assertThat(LayerTypeGuard.guardHardwareLayerForSize(oversized, LayerType.LAYER_TYPE_NONE))
        .isEqualTo(LayerType.LAYER_TYPE_NONE)
  }
}
