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

import com.facebook.litho.testing.testrunner.LithoTestRunner
import java.lang.reflect.Modifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Every accessibility property of [NodeInfo] is handled in several places by hand:
 * [NodeInfo.needsAccessibilityDelegate], [NodeInfo.isEquivalentTo], [NodeInfo.copyInto] and the
 * delegate that applies it. One left out of the first is silently dropped from a container; one
 * left out of equivalence stays stale on a mounted host after it changes. This table holds each
 * property once, checks every place against it, and fails when a new field is added without an
 * entry.
 */
@RunWith(LithoTestRunner::class)
class NodeInfoAccessibilityPropertiesTest {

  private class Property(
      val name: String,
      /** The fields of [NodeInfo] that hold it, for the coverage check. */
      val fields: List<String>,
      /**
       * Whether a value a service would read makes the node need [ComponentAccessibilityDelegate].
       */
      val needsDelegate: Boolean,
      val set: NodeInfo.() -> Unit,
      val setDifferently: NodeInfo.() -> Unit,
  )

  private fun <E> handler(id: Int): EventHandler<E> = EventHandlerTestUtil.create(id, null)

  private val properties: List<Property> = listOf(
      Property(
          "contentDescription",
          listOf("_contentDescription"),
          needsDelegate = false,
          { contentDescription = "a" },
          { contentDescription = "b" },
      ),
      Property(
          "stateDescription",
          listOf("_stateDescription"),
          needsDelegate = true,
          { stateDescription = "a" },
          { stateDescription = "b" },
      ),
      Property(
          "containerTitle",
          listOf("_containerTitle"),
          needsDelegate = true,
          { containerTitle = "a" },
          { containerTitle = "b" },
      ),
      Property(
          "accessibilityRole",
          listOf("_accessibilityRole"),
          needsDelegate = true,
          { accessibilityRole = AccessibilityRole.BUTTON },
          { accessibilityRole = AccessibilityRole.IMAGE },
      ),
      Property(
          "accessibilityRoleDescription",
          listOf("_accessibilityRoleDescription"),
          needsDelegate = true,
          { accessibilityRoleDescription = "a" },
          { accessibilityRoleDescription = "b" },
      ),
      Property(
          "accessibilityPaneTitle",
          listOf("_accessibilityPaneTitle"),
          needsDelegate = true,
          { accessibilityPaneTitle = "a" },
          { accessibilityPaneTitle = "b" },
      ),
      Property(
          "liveRegionMode",
          listOf("_liveRegionMode"),
          needsDelegate = true,
          { liveRegionMode = 1 },
          { liveRegionMode = 2 },
      ),
      Property(
          "labeledBy",
          listOf("_labeledBy"),
          needsDelegate = true,
          { labeledBy = "a" },
          { labeledBy = "b" },
      ),
      Property(
          "focusOrder",
          listOf("_focusOrder"),
          needsDelegate = true,
          { focusOrder = FocusOrderModel("a") },
          { focusOrder = FocusOrderModel("b") },
      ),
      Property(
          "minDurationBetweenContentChangesMillis",
          listOf("_minDurationBetweenContentChangesMillis"),
          needsDelegate = true,
          { minDurationBetweenContentChangesMillis = 1L },
          { minDurationBetweenContentChangesMillis = 2L },
      ),
      Property(
          "accessibilityHeading",
          listOf("accessibilityHeadingState"),
          needsDelegate = true,
          { setAccessibilityHeading(true) },
          { setAccessibilityHeading(false) },
      ),
      Property(
          "screenReaderFocusable",
          listOf("screenReaderFocusState"),
          needsDelegate = true,
          { setScreenReaderFocusable(true) },
          { setScreenReaderFocusable(false) },
      ),
      Property(
          "initialFocus",
          listOf("initialFocusState"),
          needsDelegate = true,
          { setInitialFocusState(true) },
          { setInitialFocusState(false) },
      ),
      Property(
          "dispatchPopulateAccessibilityEventHandler",
          listOf("_dispatchPopulateAccessibilityEventHandler"),
          needsDelegate = true,
          { dispatchPopulateAccessibilityEventHandler = handler(1) },
          { dispatchPopulateAccessibilityEventHandler = handler(2) },
      ),
      Property(
          "onInitializeAccessibilityEventHandler",
          listOf("_onInitializeAccessibilityEventHandler"),
          needsDelegate = true,
          { onInitializeAccessibilityEventHandler = handler(1) },
          { onInitializeAccessibilityEventHandler = handler(2) },
      ),
      Property(
          "onInitializeAccessibilityNodeInfoHandler",
          listOf("_onInitializeAccessibilityNodeInfoHandler"),
          needsDelegate = true,
          { onInitializeAccessibilityNodeInfoHandler = handler(1) },
          { onInitializeAccessibilityNodeInfoHandler = handler(2) },
      ),
      Property(
          "onPopulateAccessibilityEventHandler",
          listOf("_onPopulateAccessibilityEventHandler"),
          needsDelegate = true,
          { onPopulateAccessibilityEventHandler = handler(1) },
          { onPopulateAccessibilityEventHandler = handler(2) },
      ),
      Property(
          "onPopulateAccessibilityNodeHandler",
          listOf("_onPopulateAccessibilityNodeHandler"),
          needsDelegate = true,
          { onPopulateAccessibilityNodeHandler = handler(1) },
          { onPopulateAccessibilityNodeHandler = handler(2) },
      ),
      Property(
          "onVirtualViewKeyboardFocusChangedHandler",
          listOf("_onVirtualViewKeyboardFocusChangedHandler"),
          needsDelegate = true,
          { onVirtualViewKeyboardFocusChangedHandler = handler(1) },
          { onVirtualViewKeyboardFocusChangedHandler = handler(2) },
      ),
      Property(
          "onPerformActionForVirtualViewHandler",
          listOf("_onPerformActionForVirtualViewHandler"),
          needsDelegate = true,
          { onPerformActionForVirtualViewHandler = handler(1) },
          { onPerformActionForVirtualViewHandler = handler(2) },
      ),
      Property(
          "onRequestSendAccessibilityEventHandler",
          listOf("_onRequestSendAccessibilityEventHandler"),
          needsDelegate = true,
          { onRequestSendAccessibilityEventHandler = handler(1) },
          { onRequestSendAccessibilityEventHandler = handler(2) },
      ),
      Property(
          "performAccessibilityActionHandler",
          listOf("_performAccessibilityActionHandler"),
          needsDelegate = true,
          { performAccessibilityActionHandler = handler(1) },
          { performAccessibilityActionHandler = handler(2) },
      ),
      Property(
          "sendAccessibilityEventHandler",
          listOf("_sendAccessibilityEventHandler"),
          needsDelegate = true,
          { sendAccessibilityEventHandler = handler(1) },
          { sendAccessibilityEventHandler = handler(2) },
      ),
      Property(
          "sendAccessibilityEventUncheckedHandler",
          listOf("_sendAccessibilityEventUncheckedHandler"),
          needsDelegate = true,
          { sendAccessibilityEventUncheckedHandler = handler(1) },
          { sendAccessibilityEventUncheckedHandler = handler(2) },
      ),
  )

  /**
   * Fields of [NodeInfo] that are not accessibility properties of their own: bookkeeping, and the
   * binder-migrated half, which carries its own equality and copy.
   */
  private val notAccessibilityProperties = setOf("flags", "binderMigratedNodeInfo", "viewTags")

  @Test
  fun `every field of NodeInfo is an entry in this table or is known not to be one`() {
    val declared =
        NodeInfo::class
            .java
            .declaredFields
            .filterNot { Modifier.isStatic(it.modifiers) || it.isSynthetic }
            .map { it.name }
            .toSet()
    val covered = properties.flatMap { it.fields }.toSet() + notAccessibilityProperties

    assertThat(declared - covered)
        .describedAs("fields with no entry: add each to the table, with its handling everywhere")
        .isEmpty()
    assertThat(covered - declared).describedAs("entries naming no field").isEmpty()
  }

  @Test
  fun `a value a service would read makes the node need the delegate`() {
    val missing =
        properties
            .filter { it.needsDelegate }
            .filterNot { property -> NodeInfo().apply(property.set).needsAccessibilityDelegate() }
            .map { it.name }

    assertThat(missing).describedAs("set, but the node does not need the delegate").isEmpty()
  }

  @Test
  fun `setting or changing a property breaks equivalence`() {
    val unnoticedSet =
        properties
            .filter { property -> NodeInfo().apply(property.set).isEquivalentTo(NodeInfo()) }
            .map { it.name }
    val unnoticedChange =
        properties
            .filter { property ->
              NodeInfo()
                  .apply(property.set)
                  .isEquivalentTo(NodeInfo().apply(property.setDifferently))
            }
            .map { it.name }

    assertThat(mapOf("set" to unnoticedSet, "changed" to unnoticedChange))
        .describedAs("properties equivalence does not notice")
        .isEqualTo(mapOf("set" to emptyList<String>(), "changed" to emptyList<String>()))
  }

  @Test
  fun `the same focus order rebuilt on the next render stays equivalent`() {
    // useFocusOrder builds new models every render. Equivalence also decides whether a host is
    // rebound, so comparing them by identity would rebind every host with a focus order each pass.
    val first = NodeInfo().apply { focusOrder = FocusOrderModel("row:0") }
    val next = NodeInfo().apply { focusOrder = FocusOrderModel("row:0") }

    assertThat(first.isEquivalentTo(next)).isTrue()
  }

  @Test
  fun `copyInto carries every property`() {
    val lost =
        properties
            .filterNot { property ->
              val source = NodeInfo().apply(property.set)
              val target = NodeInfo()
              source.copyInto(target)
              target.isEquivalentTo(source)
            }
            .map { it.name }

    assertThat(lost).describedAs("not carried by copyInto").isEmpty()
  }
}
