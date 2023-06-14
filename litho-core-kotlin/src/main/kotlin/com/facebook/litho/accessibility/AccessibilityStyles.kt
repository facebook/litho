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

package com.facebook.litho.accessibility

import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
import com.facebook.litho.AccessibilityRole.AccessibilityRoleType
import com.facebook.litho.CommonProps
import com.facebook.litho.ComponentContext
import com.facebook.litho.OnInitializeAccessibilityEventEvent
import com.facebook.litho.OnInitializeAccessibilityNodeInfoEvent
import com.facebook.litho.OnPopulateAccessibilityEventEvent
import com.facebook.litho.OnPopulateAccessibilityNodeEvent
import com.facebook.litho.OnRequestSendAccessibilityEventEvent
import com.facebook.litho.PerformAccessibilityActionEvent
import com.facebook.litho.SendAccessibilityEventEvent
import com.facebook.litho.SendAccessibilityEventUncheckedEvent
import com.facebook.litho.Style
import com.facebook.litho.StyleItem
import com.facebook.litho.StyleItemField
import com.facebook.litho.annotations.ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_AUTO
import com.facebook.litho.annotations.ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_NO
import com.facebook.litho.annotations.ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
import com.facebook.litho.annotations.ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_YES
import com.facebook.litho.eventHandler

/** Enums for [AccessibilityStyleItem]. */
@PublishedApi
internal enum class AccessibilityField : StyleItemField {
  ACCESSIBILITY_HEADING,
  ACCESSIBILITY_ROLE,
  ACCESSIBILITY_ROLE_DESCRIPTION,
  CONTENT_DESCRIPTION,
  IMPORTANT_FOR_ACCESSIBILITY,
  ON_INITIALIZE_ACCESSIBILITY_EVENT,
  ON_INITIALIZE_ACCESSIBILITY_NODE_INFO,
  ON_POPULATE_ACCESSIBILITY_EVENT,
  ON_POPULATE_ACCESSIBILITY_NODE,
  ON_REQUEST_SEND_ACCESSIBILITY_EVENT,
  PERFORM_ACCESSIBILITY_ACTION,
  SEND_ACCESSIBILITY_EVENT,
  SEND_ACCESSIBILITY_EVENT_UNCHECKED,
}

@PublishedApi
@DataClassGenerate
internal data class AccessibilityStyleItem(
    override val field: AccessibilityField,
    override val value: Any?
) : StyleItem<Any?> {
  override fun applyCommonProps(context: ComponentContext, commonProps: CommonProps) {
    when (field) {
      AccessibilityField.ACCESSIBILITY_HEADING -> commonProps.accessibilityHeading(value as Boolean)
      AccessibilityField.ACCESSIBILITY_ROLE -> commonProps.accessibilityRole(value as String)
      AccessibilityField.ACCESSIBILITY_ROLE_DESCRIPTION ->
          commonProps.accessibilityRoleDescription(value as CharSequence)
      AccessibilityField.CONTENT_DESCRIPTION ->
          commonProps.contentDescription(value as CharSequence)
      AccessibilityField.IMPORTANT_FOR_ACCESSIBILITY ->
          commonProps.importantForAccessibility(value as Int)
      AccessibilityField.ON_INITIALIZE_ACCESSIBILITY_EVENT ->
          commonProps.onInitializeAccessibilityEventHandler(
              eventHandler(value as (OnInitializeAccessibilityEventEvent) -> Unit))
      AccessibilityField.ON_INITIALIZE_ACCESSIBILITY_NODE_INFO ->
          commonProps.onInitializeAccessibilityNodeInfoHandler(
              eventHandler(value as (OnInitializeAccessibilityNodeInfoEvent) -> Unit))
      AccessibilityField.ON_POPULATE_ACCESSIBILITY_EVENT ->
          commonProps.onPopulateAccessibilityEventHandler(
              eventHandler(value as (OnPopulateAccessibilityEventEvent) -> Unit))
      AccessibilityField.ON_POPULATE_ACCESSIBILITY_NODE ->
          commonProps.onPopulateAccessibilityNodeHandler(
              eventHandler(value as (OnPopulateAccessibilityNodeEvent) -> Unit))
      AccessibilityField.ON_REQUEST_SEND_ACCESSIBILITY_EVENT ->
          commonProps.onRequestSendAccessibilityEventHandler(
              eventHandler(value as (OnRequestSendAccessibilityEventEvent) -> Unit))
      AccessibilityField.PERFORM_ACCESSIBILITY_ACTION ->
          commonProps.performAccessibilityActionHandler(
              eventHandler(value as (PerformAccessibilityActionEvent) -> Unit))
      AccessibilityField.SEND_ACCESSIBILITY_EVENT ->
          commonProps.sendAccessibilityEventHandler(
              eventHandler(value as (SendAccessibilityEventEvent) -> Unit))
      AccessibilityField.SEND_ACCESSIBILITY_EVENT_UNCHECKED ->
          commonProps.sendAccessibilityEventUncheckedHandler(
              eventHandler(value as (SendAccessibilityEventUncheckedEvent) -> Unit))
    }
  }
}

/**
 * Whether the view is a heading for a section of content for accessibility purposes.
 *
 * Note: Since this attribute is available only on API 19 and above, calling this method on lower
 * APIs will have no effect.
 *
 * See [android.view.View.setAccessibilityHeading].
 */
inline fun Style.accessibilityHeading(isAccessibilityHeading: Boolean): Style =
    this + AccessibilityStyleItem(AccessibilityField.ACCESSIBILITY_HEADING, isAccessibilityHeading)

/**
 * The Android Talkback "role" this component has. This will be read out when the view is visited in
 * Talkback mode. See [AccessibilityRoleType] for possible roles.
 */
inline fun Style.accessibilityRole(@AccessibilityRoleType accessibilityRole: String?): Style =
    this +
        accessibilityRole?.let { AccessibilityStyleItem(AccessibilityField.ACCESSIBILITY_ROLE, it) }

/**
 * The description for this Component's [accessibilityRole]. This will be read out when the view is
 * visited in Talkback mode.
 */
inline fun Style.accessibilityRoleDescription(accessibilityRoleDescription: CharSequence?): Style =
    this +
        accessibilityRoleDescription?.let {
          AccessibilityStyleItem(AccessibilityField.ACCESSIBILITY_ROLE_DESCRIPTION, it)
        }

/**
 * A description of the contents of this Component for accessibility.
 *
 * See [android.view.View.setContentDescription].
 */
inline fun Style.contentDescription(contentDescription: CharSequence?): Style =
    this +
        contentDescription?.let {
          AccessibilityStyleItem(AccessibilityField.CONTENT_DESCRIPTION, it)
        }

/**
 * Sets whether this Component is "important for accessibility". If it is, it fires accessibility
 * events and is reported to accessibility services that query the screen. The value for this
 * property can be one of the values in [ImportantForAccessibility].
 *
 * See [android.view.View.setImportantForAccessibility].
 */
inline fun Style.importantForAccessibility(
    importantForAccessibility: ImportantForAccessibility
): Style =
    this +
        AccessibilityStyleItem(
            AccessibilityField.IMPORTANT_FOR_ACCESSIBILITY, importantForAccessibility.asInt)

/**
 * Initializes an [AccessibilityEvent] with information about the the host View which dispatched the
 * event.
 *
 * See [android.view.View.AccessibilityDelegateCompat#onInitializeAccessibilityEvent].
 */
inline fun Style.onInitializeAccessibilityEvent(
    noinline onInitializeAccessibilityEventHandler: (OnInitializeAccessibilityEventEvent) -> Unit
): Style =
    this +
        AccessibilityStyleItem(
            AccessibilityField.ON_INITIALIZE_ACCESSIBILITY_EVENT,
            onInitializeAccessibilityEventHandler)

/**
 * Gives a chance to the host View to populate the accessibility event with its text content.
 *
 * See [android.view.View.AccessibilityDelegateCompat#onPopulateAccessibilityEvent].
 */
inline fun Style.onPopulateAccessibilityEvent(
    noinline onPopulateAccessibilityEventHandler: (OnPopulateAccessibilityEventEvent) -> Unit
): Style =
    this +
        AccessibilityStyleItem(
            AccessibilityField.ON_POPULATE_ACCESSIBILITY_EVENT, onPopulateAccessibilityEventHandler)

/** Gives a chance to the component to implement its own accessibility support. */
inline fun Style.onPopulateAccessibilityNode(
    noinline onPopulateAccessibilityNodeHandler: (OnPopulateAccessibilityNodeEvent) -> Unit
): Style =
    this +
        AccessibilityStyleItem(
            AccessibilityField.ON_POPULATE_ACCESSIBILITY_NODE, onPopulateAccessibilityNodeHandler)

/**
 * Called when a child of the host View has requested sending an [AccessibilityEvent] and gives an
 * opportunity to the parent (the host) to augment the event.
 *
 * See [android.view.View.AccessibilityDelegateCompat#onRequestSendAccessibilityEvent].
 */
inline fun Style.onRequestSendAccessibilityEvent(
    noinline onRequestSendAccessibilityEventHandler: (OnRequestSendAccessibilityEventEvent) -> Unit
): Style =
    this +
        AccessibilityStyleItem(
            AccessibilityField.ON_REQUEST_SEND_ACCESSIBILITY_EVENT,
            onRequestSendAccessibilityEventHandler)

/**
 * Performs the specified accessibility action on the view.
 *
 * See [android.view.View.AccessibilityDelegateCompat#performAccessibilityAction].
 */
inline fun Style.performAccessibilityAction(
    noinline performAccessibilityActionHandler: (PerformAccessibilityActionEvent) -> Unit
): Style =
    this +
        AccessibilityStyleItem(
            AccessibilityField.PERFORM_ACCESSIBILITY_ACTION, performAccessibilityActionHandler)

/**
 * Sends an accessibility event of the given type. If accessibility is not enabled this method has
 * no effect.
 *
 * See [android.view.View.AccessibilityDelegateCompat#sendAccessibilityEvent].
 */
inline fun Style.sendAccessibilityEvent(
    noinline sendAccessibilityEventHandler: (SendAccessibilityEventEvent) -> Unit
): Style =
    this +
        AccessibilityStyleItem(
            AccessibilityField.SEND_ACCESSIBILITY_EVENT, sendAccessibilityEventHandler)

/**
 * Sends an accessibility event. This method behaves exactly as sendAccessibilityEvent() but takes
 * as an argument an empty [AccessibilityEvent] and does not perform a check whether accessibility
 * is enabled.
 *
 * See [android.view.View.AccessibilityDelegateCompat#sendAccessibilityEventUnchecked].
 */
inline fun Style.sendAccessibilityEventUnchecked(
    noinline sendAccessibilityEventUncheckedHandler: (SendAccessibilityEventUncheckedEvent) -> Unit
): Style =
    this +
        AccessibilityStyleItem(
            AccessibilityField.SEND_ACCESSIBILITY_EVENT_UNCHECKED,
            sendAccessibilityEventUncheckedHandler)

/**
 * Initializes an [AccessibilityNodeInfoCompat] with information about the host view.
 *
 * See [android.view.View.AccessibilityDelegateCompat#onInitializeAccessibilityNodeInfo].
 */
inline fun Style.onInitializeAccessibilityNodeInfo(
    noinline onInitializeAccessibilityNodeInfoHandler:
        (OnInitializeAccessibilityNodeInfoEvent) -> Unit
): Style =
    this +
        AccessibilityStyleItem(
            AccessibilityField.ON_INITIALIZE_ACCESSIBILITY_NODE_INFO,
            onInitializeAccessibilityNodeInfoHandler)

/**
 * Enum values for [importantForAccessibility].
 *
 * Note: if you are looking for YES_HIDE_DESCENDANTS, it has been deprecated: prefer to add an
 * intermediate child with `NO_HIDE_DESCENDANTS` instead.
 */
enum class ImportantForAccessibility(val asInt: Int) {
  /** Automatically determine whether a view is important for accessibility. */
  AUTO(IMPORTANT_FOR_ACCESSIBILITY_AUTO),

  /** The view is important for accessibility. */
  YES(IMPORTANT_FOR_ACCESSIBILITY_YES),

  /** The view is not important for accessibility. */
  NO(IMPORTANT_FOR_ACCESSIBILITY_NO),

  /** The view is not important for accessibility, nor are any of its descendant views. */
  NO_HIDE_DESCENDANTS(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS),
}
