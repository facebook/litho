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

import com.facebook.litho.config.LithoDebugConfigurations
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.rendercore.px
import com.meta.systrace.Systracer
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * In debug mode, a component's overrides are looked up by its global key's string, which is built
 * only when an override is registered.
 */
@RunWith(LithoTestRunner::class)
class DebugComponentOverridesTest {

  @JvmField @Rule val lithoTestRule = LithoTestRule()

  private val keyBuilds = KeyBuildCounter()
  private lateinit var systraceWas: Systracer
  private var debugModeWas = false

  @Before
  fun countKeyBuilds() {
    debugModeWas = LithoDebugConfigurations.isDebugModeEnabled
    LithoDebugConfigurations.isDebugModeEnabled = true
    systraceWas = ComponentsSystrace.systrace
    ComponentsSystrace.provide(keyBuilds)
  }

  @After
  fun restore() {
    ComponentsSystrace.provide(systraceWas)
    LithoDebugConfigurations.isDebugModeEnabled = debugModeWas
  }

  /**
   * A render unit takes its component's key as its debug key, so only components that mount nothing
   * are counted: the keyed Columns and the one inside the second.
   */
  @Test
  fun `with no override registered, no key string of a component that mounts nothing is built`() {
    lithoTestRule.render(widthPx = 100, heightPx = 100) {
      Column {
        child(key("first") { Column(style = Style.width(10.px).height(10.px)) })
        child(key("second") { Column { child(Column(style = Style.height(10.px))) } })
      }
    }

    assertThat(keyBuilds.count).isEqualTo(0)
  }
}

/** Traces nothing: counts the sections that build a manual global key's string. */
private class KeyBuildCounter : Systracer {

  var count = 0
    private set

  override fun beginSection(name: String) {
    if (name == "ManualGlobalKey.buildKeyString") {
      count++
    }
  }

  override fun beginAsyncSection(name: String) = Unit

  override fun beginAsyncSection(name: String, cookie: Int) = Unit

  override fun beginSectionWithArgs(name: String): Systracer.ArgsBuilder {
    beginSection(name)
    return NoArgs
  }

  override fun endSection() = Unit

  override fun endAsyncSection(name: String) = Unit

  override fun endAsyncSection(name: String, cookie: Int) = Unit

  override fun isTracing(): Boolean = true
}

private object NoArgs : Systracer.ArgsBuilder {
  override fun flush() = Unit

  override fun arg(key: String, value: Any): Systracer.ArgsBuilder = this

  override fun arg(key: String, value: Int): Systracer.ArgsBuilder = this

  override fun arg(key: String, value: Long): Systracer.ArgsBuilder = this

  override fun arg(key: String, value: Double): Systracer.ArgsBuilder = this
}
