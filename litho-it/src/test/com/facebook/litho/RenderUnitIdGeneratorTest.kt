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

import com.facebook.litho.RenderUnitIdGenerator.Companion.decodeType
import com.facebook.litho.RenderUnitIdGenerator.Companion.encodeId
import com.facebook.litho.testing.testrunner.LithoTestRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests the bit-packing layout used by [RenderUnitIdGenerator.encodeId] and
 * [RenderUnitIdGenerator.decodeType]. The decode side previously used `(id shr 32).toInt()` which
 * leaked componentTreeId bits into the type — fixed by masking with `0x7L`. These tests pin both
 * the round-trip and the mask boundary so that regression cannot recur silently.
 */
@RunWith(LithoTestRunner::class)
class RenderUnitIdGeneratorTest {

  private val allTypes = intArrayOf(
      OutputUnitType.CONTENT,
      OutputUnitType.BACKGROUND,
      OutputUnitType.FOREGROUND,
      OutputUnitType.HOST,
      OutputUnitType.BORDER,
  )

  @Test
  fun `encodeId then decodeType returns original type for componentTreeId zero`() {
    for (type in allTypes) {
      val id = encodeId(keyId = 7, type = type, componentTreeId = 0)
      assertThat(decodeType(id)).describedAs("type=%d, componentTreeId=0", type).isEqualTo(type)
    }
  }

  @Test
  fun `encodeId then decodeType returns original type for non-zero componentTreeId`() {
    // This is the regression case for the original bug: with componentTreeId != 0,
    // the unfixed decoder returned (componentTreeId shl 3) or type instead of just type.
    for (componentTreeId in intArrayOf(1, 2, 3, 17, 1_000)) {
      for (type in allTypes) {
        val id = encodeId(keyId = 7, type = type, componentTreeId = componentTreeId)
        assertThat(decodeType(id))
            .describedAs("type=%d, componentTreeId=%d", type, componentTreeId)
            .isEqualTo(type)
      }
    }
  }

  @Test
  fun `decodeType ignores keyId and componentTreeId bits`() {
    val type = OutputUnitType.FOREGROUND
    val keyIds = intArrayOf(0, 1, 0xFFFF, Int.MAX_VALUE)
    val componentTreeIds = intArrayOf(0, 1, Int.MAX_VALUE)
    for (keyId in keyIds) {
      for (componentTreeId in componentTreeIds) {
        val id = encodeId(keyId = keyId, type = type, componentTreeId = componentTreeId)
        assertThat(decodeType(id))
            .describedAs("keyId=%d, componentTreeId=%d", keyId, componentTreeId)
            .isEqualTo(type)
      }
    }
  }

  @Test
  fun `encodeId produces the documented bit layout`() {
    // bits 0-31 keyId | bits 32-34 type | bits 35+ componentTreeId
    val id = encodeId(keyId = 0x05, type = OutputUnitType.FOREGROUND, componentTreeId = 1)
    assertThat(id and 0xFFFFFFFFL).describedAs("keyId in low 32 bits").isEqualTo(0x05L)
    assertThat((id ushr 32) and 0x7L)
        .describedAs("type in bits 32-34")
        .isEqualTo(OutputUnitType.FOREGROUND.toLong())
    assertThat(id ushr 35).describedAs("componentTreeId in bits 35+").isEqualTo(1L)
  }

  @Test
  fun `encodeId yields distinct ids for distinct inputs`() {
    val ids = mutableSetOf<Long>()
    for (keyId in intArrayOf(1, 2)) {
      for (type in allTypes) {
        for (componentTreeId in intArrayOf(0, 1, 2)) {
          ids += encodeId(keyId = keyId, type = type, componentTreeId = componentTreeId)
        }
      }
    }
    assertThat(ids).hasSize(2 * allTypes.size * 3)
  }

  @Test
  fun `calculateLayoutOutputId integrates with decodeType`() {
    val generator = RenderUnitIdGenerator(componentTreeId = 42)
    val key: GlobalKey = StringGlobalKey("scope")
    for (type in allTypes) {
      val id = generator.calculateLayoutOutputId(key, type)
      assertThat(decodeType(id)).describedAs("type=%d", type).isEqualTo(type)
    }
  }
}
