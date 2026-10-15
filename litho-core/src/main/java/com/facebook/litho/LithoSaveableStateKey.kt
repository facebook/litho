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

/**
 * Encodes Litho's structural component and hook identities for Android saved state.
 *
 * [GlobalKey] is the correct in-memory identity, but its component type ids are allocated by a
 * process-local counter. The persistent form replaces those ids with their component class names
 * while preserving the parent path, sibling positions, manual keys, and hook call-site path.
 */
internal fun saveableStateKey(
    globalKey: GlobalKey,
    hookLocalKey: HookKey,
    explicitKey: String?,
    componentName: String,
): String = buildString {
  appendPart(KEY_FORMAT_VERSION)
  appendGlobalKeyPath(globalKey, componentName)
  appendHookPath(hookLocalKey)
  if (explicitKey == null) {
    appendPart(NO_EXPLICIT_KEY)
  } else {
    appendPart(EXPLICIT_KEY)
    appendPart(explicitKey)
  }
}

private fun StringBuilder.appendGlobalKeyPath(globalKey: GlobalKey, componentName: String) {
  val chain = ArrayList<GlobalKey>(DEFAULT_COMPONENT_DEPTH)
  var current: GlobalKey? = globalKey
  while (current != null) {
    chain.add(current)
    current = current.parent
  }

  appendPart(COMPONENT_PATH)
  for (index in chain.indices.reversed()) {
    when (val segment = chain[index]) {
      is DefaultGlobalKey -> appendDefaultComponent(segment.key, segment.position, componentName)
      is ManualGlobalKey -> appendManualComponent(segment.key, segment.position)
      is StringGlobalKey -> appendLegacyComponentPath(segment.key, componentName)
    }
  }
}

private fun StringBuilder.appendDefaultComponent(
    typeId: Int,
    position: Int,
    componentName: String,
) {
  val typeName =
      Component.typeNameForId(typeId)
          ?: error(
              "useSaveableState cannot derive a persistent identity for $componentName because " +
                  "component type id $typeId in its component path has no registered class " +
                  "identity. Give the component represented by that unresolved path segment an " +
                  "explicit Litho component key.",
          )
  appendDefaultComponent(typeName, position)
}

private fun StringBuilder.appendDefaultComponent(typeName: String, position: Int) {
  appendPart(DEFAULT_COMPONENT)
  appendPart(typeName)
  appendPart(position.toString())
}

private fun StringBuilder.appendManualComponent(key: String, position: Int) {
  appendPart(MANUAL_COMPONENT)
  appendPart(key)
  appendPart(position.toString())
}

/** Converts the legacy flattened GlobalKey without retaining its process-local type ids. */
private fun StringBuilder.appendLegacyComponentPath(key: String, componentName: String) {
  val segments = key.split(LEGACY_COMPONENT_SEPARATOR)
  for (segment in segments) {
    val positionSeparator = segment.lastIndexOf(COMPONENT_POSITION_SEPARATOR)
    val position =
        if (positionSeparator > 0) {
          segment.substring(positionSeparator + 1).toIntOrNull()
        } else {
          null
        }
    val body = if (position != null) segment.substring(0, positionSeparator) else segment
    val typeId = body.toIntOrNull()
    when {
      body.startsWith(MANUAL_KEY_PREFIX) -> appendManualComponent(body, position ?: 0)
      typeId != null -> appendDefaultComponent(typeId, position ?: 0, componentName)
      else -> {
        appendPart(STABLE_LITERAL_COMPONENT)
        appendPart(body)
        appendPart((position ?: 0).toString())
      }
    }
  }
}

private fun StringBuilder.appendHookPath(hookLocalKey: HookKey) {
  appendPart(HOOK_PATH)
  val chain = ArrayList<HookKey>(DEFAULT_HOOK_DEPTH)
  var current: HookKey? = hookLocalKey
  while (current != null) {
    chain.add(current)
    current = current.parent
  }
  appendPart(STRUCTURAL_HOOK)
  appendPart(chain.size.toString())
  for (index in chain.indices.reversed()) {
    val packedId = chain[index].id
    appendPart((packedId shr Int.SIZE_BITS).toInt().toString())
    appendPart(packedId.toInt().toString())
  }
}

private fun StringBuilder.appendPart(part: String) {
  append(part.length).append(':').append(part)
}

private const val KEY_FORMAT_VERSION = "litho-saveable-state-v1"
private const val COMPONENT_PATH = "component-path"
private const val DEFAULT_COMPONENT = "component"
private const val MANUAL_COMPONENT = "manual-key"
private const val STABLE_LITERAL_COMPONENT = "stable-literal"
private const val HOOK_PATH = "hook-path"
private const val STRUCTURAL_HOOK = "structural-hook"
private const val EXPLICIT_KEY = "explicit-key"
private const val NO_EXPLICIT_KEY = "no-explicit-key"
private const val MANUAL_KEY_PREFIX = '$'
private const val LEGACY_COMPONENT_SEPARATOR = ','
private const val COMPONENT_POSITION_SEPARATOR = '!'
private const val DEFAULT_COMPONENT_DEPTH = 8
private const val DEFAULT_HOOK_DEPTH = 4
