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

package com.facebook.rendercore.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CommonUtilsTest {

  /** What a section name is, from the class's simple name: its full name if that is short. */
  private fun expected(kclass: Class<*>): String {
    val simpleName = kclass.simpleName
    return if (simpleName.length < 5) "<cls>${kclass.name}</cls>" else simpleName
  }

  @Test
  fun `a top-level class's section name is its simple name`() {
    assertThat(CommonUtils.getSectionNameForTracing(TopLevelTracedClass::class.java))
        .isEqualTo("TopLevelTracedClass")
  }

  @Test
  fun `a nested class's section name is its simple name`() {
    assertThat(CommonUtils.getSectionNameForTracing(NestedLongName::class.java))
        .isEqualTo("NestedLongName")
  }

  @Test
  fun `an inner class's section name is its simple name`() {
    assertThat(CommonUtils.getSectionNameForTracing(InnerLongName::class.java))
        .isEqualTo("InnerLongName")
  }

  @Test
  fun `a Kotlin object's section name is its simple name`() {
    assertThat(CommonUtils.getSectionNameForTracing(TracedObject::class.java))
        .isEqualTo("TracedObject")
  }

  @Test
  fun `a class with a short simple name is named by its full name`() {
    assertThat(CommonUtils.getSectionNameForTracing(Abc::class.java))
        .isEqualTo("<cls>com.facebook.rendercore.utils.CommonUtilsTest\$Abc</cls>")
  }

  @Test
  fun `an anonymous class, whose simple name is empty, is named by its full name`() {
    val anonymous =
        object : Runnable {
          override fun run() = Unit
        }
    val kclass = anonymous.javaClass

    assertThat(kclass.simpleName).isEmpty()
    assertThat(CommonUtils.getSectionNameForTracing(kclass)).isEqualTo("<cls>${kclass.name}</cls>")
  }

  @Test
  fun `a lambda's section name is derived from its simple name`() {
    val lambda: () -> Unit = {}
    val kclass = lambda.javaClass

    assertThat(CommonUtils.getSectionNameForTracing(kclass)).isEqualTo(expected(kclass))
  }

  @Test
  fun `a repeat call returns the cached name`() {
    val first = CommonUtils.getSectionNameForTracing(Xyz::class.java)

    assertThat(CommonUtils.getSectionNameForTracing(Xyz::class.java)).isSameAs(first)
  }

  @Test
  fun `a class's simple name is read once across many calls`() {
    val reads = HashMap<Class<*>, Int>()
    val cache = SectionNameCache(
        { kclass ->
          reads[kclass] = (reads[kclass] ?: 0) + 1
          kclass.simpleName
        },
    )

    repeat(10) {
      assertThat(cache[NestedLongName::class.java]).isEqualTo("NestedLongName")
      assertThat(cache[Abc::class.java]).isEqualTo(expected(Abc::class.java))
    }

    assertThat(reads)
        .containsOnly(
            java.util.Map.entry(NestedLongName::class.java, 1),
            java.util.Map.entry(Abc::class.java, 1),
        )
  }

  @Test
  fun `an evicted class's section name is recomputed`() {
    val reads = HashMap<Class<*>, Int>()
    val cache = SectionNameCache(
        { tracedClass ->
          reads[tracedClass] = (reads[tracedClass] ?: 0) + 1
          tracedClass.simpleName
        },
        maxSize = 2,
    )

    assertThat(cache[TopLevelTracedClass::class.java]).isEqualTo("TopLevelTracedClass")
    assertThat(cache[NestedLongName::class.java]).isEqualTo("NestedLongName")
    assertThat(cache[InnerLongName::class.java]).isEqualTo("InnerLongName")

    assertThat(cache[TopLevelTracedClass::class.java]).isEqualTo("TopLevelTracedClass")
    assertThat(reads[TopLevelTracedClass::class.java]).isEqualTo(2)
    assertThat(reads[NestedLongName::class.java]).isEqualTo(1)
  }

  private class NestedLongName

  private inner class InnerLongName

  private object TracedObject

  private class Abc

  private class Xyz
}

private class TopLevelTracedClass
