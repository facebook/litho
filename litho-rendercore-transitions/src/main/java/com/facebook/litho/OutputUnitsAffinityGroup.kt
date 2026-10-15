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
 * A family of output units ([LayoutOutput]s or [MountItem]s) generated for the same [Component].
 * Used by [LayoutState], [MountState] and [TransitionManager] to group items subjected to same
 * [Transition] set to their originative [Component]
 */
class OutputUnitsAffinityGroup<T> {

  private val content: Array<Any?> = arrayOfNulls(5)
  private var _size: Int = 0

  constructor()

  constructor(group: OutputUnitsAffinityGroup<T>) {
    for (index in content.indices) {
      content[index] = group.content[index]
    }
    _size = group._size
  }

  fun add(@OutputUnitType type: Int, value: T) {
    requireNotNull(value) { "value should not be null" }

    if (content[type] != null) {
      throw RuntimeException("Already contains unit for type ${typeToString(type)}")
    }

    if (content[OutputUnitType.HOST] != null || (type == OutputUnitType.HOST && _size > 0)) {
      throw RuntimeException(ERROR_HOST_UNIT_VALIDATION)
    }

    content[type] = value
    _size++
  }

  fun replace(@OutputUnitType type: Int, value: T?) {
    if (value != null && content[type] != null) {
      content[type] = value
    } else if (value != null && content[type] == null) {
      add(type, value)
    } else if (value == null && content[type] != null) {
      content[type] = null
      _size--
    }
  }

  @Suppress("UNCHECKED_CAST") operator fun get(@OutputUnitType type: Int): T? = content[type] as T?

  fun size(): Int = _size

  val isEmpty: Boolean
    get() = _size == 0

  @OutputUnitType
  fun typeAt(index: Int): Int {
    if (index < 0 || index >= _size) {
      throw IndexOutOfBoundsException("index=$index, size=$_size")
    }
    var i = 0
    var j = 0
    while (j <= index) {
      if (content[i] != null) {
        j++
      }
      i++
    }
    return i - 1
  }

  fun getAt(index: Int): T? = get(typeAt(index))

  fun getMostSignificantUnit(): T? =
      when {
        content[OutputUnitType.HOST] != null -> get(OutputUnitType.HOST)
        content[OutputUnitType.CONTENT] != null -> get(OutputUnitType.CONTENT)
        content[OutputUnitType.BACKGROUND] != null -> get(OutputUnitType.BACKGROUND)
        content[OutputUnitType.FOREGROUND] != null -> get(OutputUnitType.FOREGROUND)
        else -> get(OutputUnitType.BORDER)
      }

  fun clean() {
    for (i in content.indices) {
      content[i] = null
    }
    _size = 0
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) {
      return false
    }
    val that = other as OutputUnitsAffinityGroup<*>
    if (_size != that._size) {
      return false
    }
    for (i in content.indices) {
      if (content[i] != that.content[i]) {
        return false
      }
    }
    return true
  }

  override fun toString(): String {
    val sb = StringBuilder(super.toString())
    for (index in 0 until size()) {
      @OutputUnitType val type = typeAt(index)
      val unit = getAt(index)
      sb.append("\n\t").append(typeToString(type)).append(": ").append(unit.toString())
    }
    return sb.toString()
  }

  companion object {
    @JvmField
    val ERROR_HOST_UNIT_VALIDATION: String =
        "OutputUnitsAffinityGroup can contain only one content of type HOST and it must be the first one."

    private fun typeToString(@OutputUnitType type: Int): String? =
        when (type) {
          OutputUnitType.CONTENT -> "CONTENT"
          OutputUnitType.BACKGROUND -> "BACKGROUND"
          OutputUnitType.FOREGROUND -> "FOREGROUND"
          OutputUnitType.HOST -> "HOST"
          OutputUnitType.BORDER -> "BORDER"
          else -> null
        }
  }
}
