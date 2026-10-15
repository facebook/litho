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

package com.facebook.litho.rangemanager

import com.meta.rangemanager.PreparationFailureType
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LithoPreparationHealthEventTest {

  @Test
  fun `Late has LATE failure type`() {
    assertThat(LithoPreparationHealthEvent.Late.failureType).isEqualTo(PreparationFailureType.LATE)
  }

  @Test
  fun `Late has NO_LAYOUT_STATE details`() {
    assertThat(LithoPreparationHealthEvent.Late.details).isEqualTo("NO_LAYOUT_STATE")
  }

  @Test
  fun `Wrong has WRONG failure type`() {
    val event = LithoPreparationHealthEvent.Wrong(sizeSpecsMatch = false, idMatch = true)
    assertThat(event.failureType).isEqualTo(PreparationFailureType.WRONG)
  }

  @Test
  fun `Wrong with idMatch false reports ID_MISMATCH`() {
    val event = LithoPreparationHealthEvent.Wrong(sizeSpecsMatch = true, idMatch = false)
    assertThat(event.details).isEqualTo("ID_MISMATCH")
  }

  @Test
  fun `Wrong with sizeSpecsMatch false reports SIZE_SPEC_MISMATCH`() {
    val event = LithoPreparationHealthEvent.Wrong(sizeSpecsMatch = false, idMatch = true)
    assertThat(event.details).contains("SIZE_SPEC_MISMATCH")
  }

  @Test
  fun `Wrong with sizeSpecsMatch false and constraints includes specs in details`() {
    val event =
        LithoPreparationHealthEvent.Wrong(
            sizeSpecsMatch = false,
            idMatch = true,
            currentSizeConstraint = "100x200",
            incomingSizeConstraint = "300x400",
        )
    assertThat(event.details).isEqualTo("SIZE_SPEC_MISMATCH(current=[100x200], incoming=[300x400])")
  }

  @Test
  fun `Wrong with both idMatch and sizeSpecsMatch true reports UNKNOWN`() {
    val event = LithoPreparationHealthEvent.Wrong(sizeSpecsMatch = true, idMatch = true)
    assertThat(event.details).isEqualTo("UNKNOWN")
  }

  @Test
  fun `Wrong id mismatch takes priority over size spec mismatch`() {
    val event = LithoPreparationHealthEvent.Wrong(sizeSpecsMatch = false, idMatch = false)
    assertThat(event.details).isEqualTo("ID_MISMATCH")
  }
}
