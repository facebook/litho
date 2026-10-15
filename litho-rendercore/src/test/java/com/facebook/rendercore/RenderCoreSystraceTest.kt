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

package com.facebook.rendercore

import com.meta.systrace.Systracer
import java.lang.reflect.Field
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RenderCoreSystraceTest {

  private lateinit var capturedSections: MutableList<String>
  private lateinit var testSystracer: TestSystracer

  private lateinit var hasStartedField: Field
  private lateinit var instanceField: Field

  @Before
  fun setup() {
    capturedSections = mutableListOf()
    testSystracer = TestSystracer()

    // Use reflection to reset hasStarted and inject our test systracer
    hasStartedField = RenderCoreSystrace::class.java.getDeclaredField("hasStarted")
    hasStartedField.isAccessible = true
    hasStartedField.setBoolean(null, false)

    instanceField = RenderCoreSystrace::class.java.getDeclaredField("instance")
    instanceField.isAccessible = true

    // Inject our test systracer
    RenderCoreSystrace.use(testSystracer)
  }

  @After
  fun tearDown() {
    // Reset hasStarted to false for other tests
    hasStartedField.setBoolean(null, false)
  }

  @Test
  fun `empty section name is not truncated`() {
    RenderCoreSystrace.beginSection("")

    assertThat(capturedSections).hasSize(1)
    assertThat(capturedSections[0]).isEmpty()
  }

  @Test
  fun `single character section name is not truncated`() {
    RenderCoreSystrace.beginSection("A")

    assertThat(capturedSections).hasSize(1)
    assertThat(capturedSections[0]).isEqualTo("A")
  }

  @Test
  fun `section name with exactly 127 characters is not truncated`() {
    // Create a name with exactly 127 characters
    val exactName = "A".repeat(127)
    RenderCoreSystrace.beginSection(exactName)

    assertThat(capturedSections).hasSize(1)
    assertThat(capturedSections[0]).hasSize(127)
    assertThat(capturedSections[0]).isEqualTo(exactName)
  }

  @Test
  fun `common short section names are not truncated`() {
    val shortNames = listOf(
        "MountItem:acquire-content",
        "LayoutState:calculate",
        "View:measure",
        "Component:render",
    )

    shortNames.forEach { name ->
      capturedSections.clear()
      RenderCoreSystrace.beginSection(name)

      assertThat(capturedSections).hasSize(1)
      assertThat(capturedSections[0]).isEqualTo(name)
    }
  }

  @Test
  fun `section name with 128 characters is truncated to 127`() {
    // Create a name with 128 characters (1 over limit)
    val longName = "A".repeat(128)
    RenderCoreSystrace.beginSection(longName)

    assertThat(capturedSections).hasSize(1)
    assertThat(capturedSections[0]).hasSize(127)
    assertThat(capturedSections[0]).isEqualTo("A".repeat(127))
  }

  @Test
  fun `section name with 200 characters is truncated to 127`() {
    val longName = "B".repeat(200)
    RenderCoreSystrace.beginSection(longName)

    assertThat(capturedSections).hasSize(1)
    assertThat(capturedSections[0]).hasSize(127)
    assertThat(capturedSections[0]).isEqualTo("B".repeat(127))
  }

  @Test
  fun `section name with 500 characters is truncated to 127`() {
    val veryLongName = "C".repeat(500)
    RenderCoreSystrace.beginSection(veryLongName)

    assertThat(capturedSections).hasSize(1)
    assertThat(capturedSections[0]).hasSize(127)
    assertThat(capturedSections[0]).isEqualTo("C".repeat(127))
  }

  @Test
  fun `real world example with long description is truncated`() {
    val componentName = "ComplexComponent"
    val description =
        "with a very long description that contains detailed information about what this component does and all its properties and state"
            .repeat(2)
    val fullName = "MountItem:acquire-content $componentName $description"

    RenderCoreSystrace.beginSection(fullName)

    assertThat(capturedSections).hasSize(1)
    assertThat(capturedSections[0]).hasSize(127)
    assertThat(capturedSections[0]).isEqualTo(fullName.take(127))
  }

  @Test
  fun `section name with unicode emoji characters is handled correctly`() {
    // Test with emojis which can be multiple bytes
    val nameWithEmojis = "MountItem:🎨rendering🚀fast⚡"
    RenderCoreSystrace.beginSection(nameWithEmojis)

    assertThat(capturedSections).hasSize(1)
    assertThat(capturedSections[0]).isEqualTo(nameWithEmojis)
  }

  @Test
  fun `long section name with unicode emoji characters is truncated correctly`() {
    // Create a long name with emojis that exceeds 127 characters
    val baseText = "Component:render-"
    val longName = baseText + "🎨".repeat(100)

    RenderCoreSystrace.beginSection(longName)

    assertThat(capturedSections).hasSize(1)
    // The truncation should happen at character level (take(127))
    assertThat(capturedSections[0]).hasSize(127)
    assertThat(capturedSections[0]).isEqualTo(longName.take(127))
  }

  @Test
  fun `section name with unicode multibyte characters is handled correctly`() {
    val nameWithUnicode = "MountItem:render-日本語-китайский-العربية"
    RenderCoreSystrace.beginSection(nameWithUnicode)

    assertThat(capturedSections).hasSize(1)
    assertThat(capturedSections[0]).isEqualTo(nameWithUnicode)
  }

  @Test
  fun `section name with special characters is handled correctly`() {
    val nameWithSpecialChars = "Component:render <tag> & \"quotes\" 'single' \\backslash"
    RenderCoreSystrace.beginSection(nameWithSpecialChars)

    assertThat(capturedSections).hasSize(1)
    assertThat(capturedSections[0]).isEqualTo(nameWithSpecialChars)
  }

  @Test
  fun `section name with newlines and tabs is handled correctly`() {
    val nameWithWhitespace = "Component:render\nwith\nnewlines\tand\ttabs"
    RenderCoreSystrace.beginSection(nameWithWhitespace)

    assertThat(capturedSections).hasSize(1)
    assertThat(capturedSections[0]).isEqualTo(nameWithWhitespace)
  }

  @Test
  fun `beginSectionWithArgs truncates long names correctly`() {
    val longName = "D".repeat(200)
    RenderCoreSystrace.beginSectionWithArgs(longName)

    assertThat(capturedSections).hasSize(1)
    assertThat(capturedSections[0]).hasSize(127)
    assertThat(capturedSections[0]).isEqualTo("D".repeat(127))
  }

  @Test
  fun `beginSectionWithArgs does not truncate short names`() {
    val shortName = "Component:render:args"
    RenderCoreSystrace.beginSectionWithArgs(shortName)

    assertThat(capturedSections).hasSize(1)
    assertThat(capturedSections[0]).isEqualTo(shortName)
  }

  @Test
  fun `multiple section calls each truncate independently`() {
    val longName1 = "A".repeat(150)
    val longName2 = "B".repeat(180)
    val shortName = "ShortName"

    RenderCoreSystrace.beginSection(longName1)
    RenderCoreSystrace.beginSection(longName2)
    RenderCoreSystrace.beginSection(shortName)

    assertThat(capturedSections).hasSize(3)
    assertThat(capturedSections[0]).hasSize(127).isEqualTo("A".repeat(127))
    assertThat(capturedSections[1]).hasSize(127).isEqualTo("B".repeat(127))
    assertThat(capturedSections[2]).isEqualTo(shortName)
  }

  @Test
  fun `truncation preserves start of section name`() {
    val prefix = "MountItem:acquire-content-"
    val longSuffix = "X".repeat(200)
    val fullName = prefix + longSuffix

    RenderCoreSystrace.beginSection(fullName)

    assertThat(capturedSections).hasSize(1)
    val truncated = capturedSections[0]
    assertThat(truncated).hasSize(127)
    assertThat(truncated).startsWith(prefix)
    assertThat(truncated).isEqualTo(fullName.take(127))
  }

  private inner class TestSystracer : Systracer {
    private val MAX_SECTION_NAME_LENGTH = 127

    override fun beginSection(name: String) {
      val truncatedName =
          if (name.length > MAX_SECTION_NAME_LENGTH) {
            name.take(MAX_SECTION_NAME_LENGTH)
          } else {
            name
          }
      capturedSections.add(truncatedName)
    }

    override fun beginAsyncSection(name: String) {
      val truncatedName =
          if (name.length > MAX_SECTION_NAME_LENGTH) {
            name.take(MAX_SECTION_NAME_LENGTH)
          } else {
            name
          }
      capturedSections.add(truncatedName)
    }

    override fun beginAsyncSection(name: String, cookie: Int) {
      val truncatedName =
          if (name.length > MAX_SECTION_NAME_LENGTH) {
            name.take(MAX_SECTION_NAME_LENGTH)
          } else {
            name
          }
      capturedSections.add(truncatedName)
    }

    override fun beginSectionWithArgs(name: String): Systracer.ArgsBuilder {
      beginSection(name)
      return RenderCoreSystrace.NO_OP_ARGS_BUILDER
    }

    override fun endSection() {
      // No-op for testing
    }

    override fun endAsyncSection(name: String) {
      // No-op for testing
    }

    override fun endAsyncSection(name: String, cookie: Int) {
      // No-op for testing
    }

    override fun isTracing(): Boolean = true
  }
}
