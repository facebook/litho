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

package com.facebook.litho

import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/** Unit tests [Style]. */
@RunWith(JUnit4::class)
class StyleTest {

  private class TestStyleItem(val name: String) : StyleItem {
    override fun applyToProps(resourceResolver: ResourceResolver, commonProps: CommonProps) = Unit
  }

  @Test
  fun style_styleItemAdded_getNewInstanceWithOldAndNewItems() {
    val style1 = Style + TestStyleItem("first")
    val style2 = style1 + TestStyleItem("second")

    assertThat(style1).isNotEqualTo(style2)
    assertThat(style1.toStringList()).containsExactly("first")
    assertThat(style2.toStringList()).containsExactly("first", "second")
  }

  @Test
  fun style_stylesAdded_getNewInstanceInRightOrder() {
    val style1 = Style + TestStyleItem("first1") + TestStyleItem("first2")
    val style2 = Style + TestStyleItem("second1") + TestStyleItem("second2")
    val combined = style1 + style2

    assertThat(style1.toStringList()).containsExactly("first1", "first2")
    assertThat(style2.toStringList()).containsExactly("second1", "second2")
    assertThat(combined.toStringList()).containsExactly("first1", "first2", "second1", "second2")
  }

  @Test
  fun style_stylesAndStyleItemsAdded_getNewStyleInRightOrder() {
    val style1 = Style + TestStyleItem("first")
    val style2 = Style + TestStyleItem("second")
    val combined = style1 + style2 + TestStyleItem("third")

    assertThat(combined.toStringList()).containsExactly("first", "second", "third")
  }

  private fun Style.toStringList(): List<String> {
    val list = mutableListOf<String>()
    forEach { list.add((it as TestStyleItem).name) }
    return list
  }
}
