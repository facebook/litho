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

package com.facebook.litho.accessibility

import com.facebook.litho.AccessibilityRole.AccessibilityRoleType
import com.facebook.litho.Component
import com.facebook.litho.OnInitializeAccessibilityNodeInfoEvent
import com.facebook.litho.ResourceResolver
import com.facebook.litho.Style
import com.facebook.litho.StyleItem
import com.facebook.litho.annotations.ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_AUTO
import com.facebook.litho.annotations.ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_NO
import com.facebook.litho.annotations.ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
import com.facebook.litho.annotations.ImportantForAccessibility.IMPORTANT_FOR_ACCESSIBILITY_YES
import com.facebook.litho.eventHandler
import com.facebook.litho.exhaustive
import com.facebook.litho.getCommonPropsHolder

/** Enums for [AccessibilityStyleItem]. */
@PublishedApi
internal enum class AccessibilityField {
  ACCESSIBILITY_HEADING,
  ACCESSIBILITY_ROLE,
  ACCESSIBILITY_ROLE_DESCRIPTION,
  CONTENT_DESCRIPTION,
  IMPORTANT_FOR_ACCESSIBILITY,
  ON_INITIALIZE_ACCESSIBILITY_NODE_INFO,
}

@PublishedApi
internal class AccessibilityStyleItem(val field: AccessibilityField, val value: Any?) : StyleItem {
  override fun applyToComponent(resourceResolver: ResourceResolver, component: Component) {
    val commonProps = component.getCommonPropsHolder()
    when (field) {
      AccessibilityField.ACCESSIBILITY_HEADING -> commonProps.accessibilityHeading(value as Boolean)
      AccessibilityField.ACCESSIBILITY_ROLE -> commonProps.accessibilityRole(value as String)
      AccessibilityField.ACCESSIBILITY_ROLE_DESCRIPTION ->
          commonProps.accessibilityRoleDescription(value as CharSequence)
      AccessibilityField.CONTENT_DESCRIPTION ->
          commonProps.contentDescription(value as CharSequence)
      AccessibilityField.IMPORTANT_FOR_ACCESSIBILITY ->
          commonProps.importantForAccessibility(value as Int)
      AccessibilityField.ON_INITIALIZE_ACCESSIBILITY_NODE_INFO ->
          commonProps.onInitializeAccessibilityNodeInfoHandler(
              eventHandler(value as (OnInitializeAccessibilityNodeInfoEvent) -> Unit))
    }.exhaustive
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
inline fun Style.accessibilityRole(@AccessibilityRoleType accessibilityRole: String): Style =
    this + AccessibilityStyleItem(AccessibilityField.ACCESSIBILITY_ROLE, accessibilityRole)

/**
 * The description for this Component's [accessibilityRole]. This will be read out when the view is
 * visited in Talkback mode.
 */
inline fun Style.accessibilityRoleDescription(accessibilityRoleDescription: CharSequence): Style =
    this +
        AccessibilityStyleItem(
            AccessibilityField.ACCESSIBILITY_ROLE_DESCRIPTION, accessibilityRoleDescription)

/**
 * A description of the contents of this Component for accessibility.
 *
 * See [android.view.View.setContentDescription].
 */
inline fun Style.contentDescription(contentDescription: CharSequence): Style =
    this + AccessibilityStyleItem(AccessibilityField.CONTENT_DESCRIPTION, contentDescription)

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
