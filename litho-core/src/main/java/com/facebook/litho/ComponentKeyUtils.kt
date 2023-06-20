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

object ComponentKeyUtils {

  private const val DUPLICATE_MANUAL_KEY = "ComponentKeyUtils:DuplicateManualKey"
  private const val NULL_PARENT_KEY = "ComponentKeyUtils:NullParentKey"
  private val TYPE_ID_PATTERN = "(\\d+)".toRegex()
  private const val PREFIX_FOR_MANUAL_KEY = "$"
  private const val SEPARATOR = ","
  private const val SEPARATOR_FOR_MANUAL_KEY = ",$"

  /**
   * @param keyParts a list of objects that will be concatenated to form another component's key
   * @return a key formed by concatenating the key parts delimited by a separator.
   */
  @JvmStatic
  fun getKeyWithSeparator(vararg keyParts: Any): String = buildString {
    append(PREFIX_FOR_MANUAL_KEY).append(keyParts[0])
    for (i in 1..keyParts.lastIndex) append(SEPARATOR_FOR_MANUAL_KEY).append(keyParts[i])
  }

  @JvmStatic
  fun getKeyWithSeparator(parentGlobalKey: String, key: String): String {
    return "$parentGlobalKey$SEPARATOR$key"
  }

  @JvmStatic
  fun getKeyForChildPosition(currentKey: String, index: Int): String {
    if (index == 0) {
      return currentKey
    }
    return "$currentKey!$index"
  }

  /**
   * Generate a global key for the given component that is unique among all of this component's
   * children of the same type. If a manual key has been set on the child component using the .key()
   * method, return the manual key.
   *
   * @param parentComponent parent component within the layout context
   * @param childComponent child component with the parent context
   * @return a unique global key for this component relative to its siblings.
   *
   * TODO: (T38237241) remove the usage of the key handler post the nested tree experiment
   */
  @JvmStatic
  fun generateGlobalKey(
      parentContext: ComponentContext,
      parentComponent: Component?,
      childComponent: Component
  ): String {
    val hasManualKey = childComponent.hasManualKey()
    val key = if (hasManualKey) "$${childComponent.key}" else childComponent.key
    val globalKey: String
    if (parentComponent == null) {
      globalKey = key
    } else {
      val parentGlobalKey = parentContext.globalKey
      if (parentGlobalKey == null) {
        logParentHasNullGlobalKey(parentComponent, childComponent)
        globalKey = "null$key"
      } else {
        val childKey = getKeyWithSeparator(parentGlobalKey, key)
        val index: Int
        if (hasManualKey) {
          index = parentContext.scopedComponentInfo.getManualKeyUsagesCountAndIncrement(key)
          if (index != 0) {
            logDuplicateManualKeyWarning(childComponent, key.substring(1))
          }
        } else {
          index = parentContext.scopedComponentInfo.getChildCountAndIncrement(childComponent)
        }
        globalKey = getKeyForChildPosition(childKey, index)
      }
    }
    return globalKey
  }

  private fun logParentHasNullGlobalKey(parentComponent: Component, childComponent: Component) {
    ComponentsReporter.emitMessage(
        ComponentsReporter.LogLevel.ERROR,
        NULL_PARENT_KEY,
        "Trying to generate parent-based key for component ${childComponent.simpleName}" +
            " , but parent ${parentComponent.simpleName} has a null global key \".")
  }

  private fun logDuplicateManualKeyWarning(component: Component, key: String) {
    ComponentsReporter.emitMessage(
        ComponentsReporter.LogLevel.WARNING,
        DUPLICATE_MANUAL_KEY,
        "The manual key $key you are setting on this ${component.simpleName} " +
            "is a duplicate and will be changed into a unique one. " +
            "This will result in unexpected behavior if you don't change it.")
  }

  @JvmStatic
  fun getKeyWithSeparatorForTest(vararg keyParts: Any): String = buildString {
    append(keyParts[0])
    for (i in 1..keyParts.lastIndex) append(SEPARATOR).append(keyParts[i])
  }

  @JvmStatic
  fun mapToSimpleName(key: String, typeToIdMap: Map<Any, Int>): String? {
    // manual keys cannot be resolved.
    if (key.startsWith(PREFIX_FOR_MANUAL_KEY)) {
      return key
    }

    // default to return in case of failure
    val unresolved = "id($key)"

    // find the type id
    val idPart = TYPE_ID_PATTERN.find(key)?.value ?: return unresolved
    val id =
        idPart.toIntOrNull()
            ?: return unresolved // if parsing failed just return the unresolved key.
    for ((type, value) in typeToIdMap) {
      if (value == id) {
        return when (type) {
          is Class<*> -> "<cls>${type.name}</cls>"
          else -> type.toString()
        }
      }
    }
    return unresolved
  }
}
