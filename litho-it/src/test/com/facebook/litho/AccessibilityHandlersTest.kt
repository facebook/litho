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
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.test.core.app.ApplicationProvider
import com.facebook.litho.accessibility.contentDescription
import com.facebook.litho.accessibility.onInitializeAccessibilityNodeInfo
import com.facebook.litho.accessibility.onPopulateAccessibilityNode
import com.facebook.litho.config.ComponentsConfiguration
import com.facebook.litho.core.height
import com.facebook.litho.core.width
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.layout.flexbox.Column
import com.facebook.litho.layout.flexbox.Row
import com.facebook.litho.testing.LithoTestRule
import com.facebook.litho.testing.accessibilityProbe
import com.facebook.litho.testing.testrunner.LithoTestRunner
import com.facebook.litho.view.onClick
import com.facebook.rendercore.px
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowAccessibilityManager

/** How the accessibility event handlers set through [Style] are dispatched to a host's node. */
@RunWith(LithoTestRunner::class)
class AccessibilityHandlersTest {

  @get:Rule val lithoTestRule = LithoTestRule(attachToRealWindow = true)

  private lateinit var shadowManager: ShadowAccessibilityManager
  private var previousPopulateOnAllHosts = false

  @Before
  fun setUp() {
    previousPopulateOnAllHosts = ComponentsConfiguration.enablePopulateAccessibilityNodeOnAllHosts
    ComponentsConfiguration.enablePopulateAccessibilityNodeOnAllHosts = true
    val manager =
        ApplicationProvider.getApplicationContext<Context>()
            .getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    shadowManager = Shadows.shadowOf(manager)
    shadowManager.setEnabled(true)
    shadowManager.setTouchExplorationEnabled(true)
    AccessibilityUtils.invalidateCachedIsAccessibilityEnabled()
  }

  @After
  fun tearDown() {
    ComponentsConfiguration.enablePopulateAccessibilityNodeOnAllHosts = previousPopulateOnAllHosts
    shadowManager.setEnabled(false)
    shadowManager.setTouchExplorationEnabled(false)
    AccessibilityUtils.invalidateCachedIsAccessibilityEnabled()
  }

  private fun labelled(label: String): (AccessibilityNodeInfo) -> Boolean = {
    it.contentDescription == label
  }

  @Test
  fun `populate-node handler runs on a host with no content`() {
    val calls = mutableListOf<CharSequence?>()
    val probe =
        lithoTestRule
            .render {
              Column {
                child(
                    Row(
                        style =
                            Style.width(SIZE.px)
                                .height(SIZE.px)
                                .contentDescription(LABEL)
                                .onClick {}
                                .onPopulateAccessibilityNode { event ->
                                  calls.add(event.accessibilityNode.contentDescription)
                                  event.accessibilityNode.isCheckable = true
                                  event.accessibilityNode.isChecked = true
                                },
                    ),
                )
              }
            }
            .accessibilityProbe()

    val target = probe.require(labelled(LABEL))
    calls.clear()
    val node = AccessibilityNodeInfoCompat.wrap(target.info())

    assertThat(node.isCheckable).isTrue()
    assertThat(node.isChecked).isTrue()
    // The handler adds to the default population, which has already copied the label.
    assertThat(calls).containsExactly(LABEL)
  }

  @Test
  fun `populate-node handler does not run on a host with no content while the flag is off`() {
    ComponentsConfiguration.enablePopulateAccessibilityNodeOnAllHosts = false
    val calls = mutableListOf<CharSequence?>()
    val probe =
        lithoTestRule
            .render {
              Column {
                child(
                    Row(
                        style =
                            Style.width(SIZE.px)
                                .height(SIZE.px)
                                .contentDescription(LABEL)
                                .onClick {}
                                .onPopulateAccessibilityNode { event ->
                                  calls.add(event.accessibilityNode.contentDescription)
                                },
                    ),
                )
              }
            }
            .accessibilityProbe()

    probe.require(labelled(LABEL)).info()

    assertThat(calls).isEmpty()
  }

  @Test
  fun `populate-node handler runs on a host whose text is hosted by its own view`() {
    // With accessibility on, the Text gets a host of its own, so the Row's host has no accessible
    // mount item.
    val calls = mutableListOf<CharSequence?>()
    val probe =
        lithoTestRule
            .render {
              Column {
                child(
                    Row(
                        style =
                            Style.contentDescription(LABEL)
                                .onClick {}
                                .onPopulateAccessibilityNode { event ->
                                  calls.add(event.accessibilityNode.contentDescription)
                                  event.accessibilityNode.isCheckable = true
                                },
                    ) {
                      child(Text(text = "a"))
                    },
                )
              }
            }
            .accessibilityProbe()

    val target = probe.require(labelled(LABEL))
    calls.clear()
    val node = AccessibilityNodeInfoCompat.wrap(target.info())

    assertThat(node.isCheckable).isTrue()
    assertThat(calls).containsExactly(LABEL)
  }

  @Test
  fun `populate-node handler runs once on a host with an accessible mount item`() {
    val calls = mutableListOf<CharSequence?>()
    val probe =
        lithoTestRule
            .render {
              Column {
                child(
                    Text(
                        text = "a",
                        style =
                            Style.contentDescription(LABEL).onPopulateAccessibilityNode { event ->
                              calls.add(event.accessibilityNode.contentDescription)
                            },
                    ),
                )
              }
            }
            .accessibilityProbe()

    val target = probe.require(labelled(LABEL))
    calls.clear()
    target.info()

    assertThat(calls).containsExactly(LABEL)
  }

  @Test
  fun `populate-node handler runs once after a node-info handler`() {
    val order = mutableListOf<String>()
    val probe =
        lithoTestRule
            .render {
              Column {
                child(
                    Row(
                        style =
                            Style.width(SIZE.px)
                                .height(SIZE.px)
                                .contentDescription(LABEL)
                                .onClick {}
                                .onInitializeAccessibilityNodeInfo { event ->
                                  event.superDelegate.onInitializeAccessibilityNodeInfo(
                                      event.host,
                                      event.info,
                                  )
                                  order.add("init")
                                }
                                .onPopulateAccessibilityNode { order.add("populate") },
                    ),
                )
              }
            }
            .accessibilityProbe()

    val target = probe.require(labelled(LABEL))
    order.clear()
    target.info()

    assertThat(order).containsExactly("init", "populate")
  }

  private companion object {
    const val SIZE = 100
    const val LABEL = "label"
  }
}
