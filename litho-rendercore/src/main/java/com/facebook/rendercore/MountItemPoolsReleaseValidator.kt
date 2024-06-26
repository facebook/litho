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

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.view.children
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
import java.lang.reflect.Field

/**
 * Inspects a [View] to understand if it has any view properties that were not cleaned up before the
 * item is sent back to the [MountItemsPool].
 *
 * This should only be used for debugging purposes.
 */
internal class MountItemPoolsReleaseValidator(
    private val failOnDetection: Boolean = false,
    private val excludedPatterns: Set<Regex> = emptySet(),
    /**
     * These are fields the client can add for custom fields they want to inspect. If you have a
     * custom view you can define a specific extraction that verifies if a listener was properly
     * cleaned up for example
     */
    extraFields: List<FieldExtractionDefinition> = emptyList()
) {

  private val fields =
      setOf(
          FieldExtractionDefinition("touchListener") {
            getListenerFieldFromViewListenerInfo(it, "mOnTouchListener")
          },
          FieldExtractionDefinition("clickListener") {
            getListenerFieldFromViewListenerInfo(it, "mOnClickListener")
          },
          FieldExtractionDefinition("longClickListener") {
            getListenerFieldFromViewListenerInfo(it, "mOnLongClickListener")
          },
          FieldExtractionDefinition("focusChangeListener") {
            getListenerFieldFromViewListenerInfo(it, "mOnFocusChangeListener")
          },
          FieldExtractionDefinition("scrollChangeListener") {
            getListenerFieldFromViewListenerInfo(it, "mOnScrollChangeListener")
          },
          FieldExtractionDefinition("layoutChangeListeners") {
            getListenerFieldFromViewListenerInfo(it, "mOnLayoutChangeListeners")
          },
          FieldExtractionDefinition("attachStateChangeListeners") {
            getListenerFieldFromViewListenerInfo(it, "mOnAttachStateChangeListeners")
          },
          FieldExtractionDefinition("dragListener") {
            getListenerFieldFromViewListenerInfo(it, "mOnDragListener")
          },
          FieldExtractionDefinition("keyListener") {
            getListenerFieldFromViewListenerInfo(it, "mOnKeyListener")
          },
          FieldExtractionDefinition("contextClickListener") {
            getListenerFieldFromViewListenerInfo(it, "mOnContextClickListener")
          },
          FieldExtractionDefinition("applyWindowInsetsListener") {
            getListenerFieldFromViewListenerInfo(it, "mOnApplyWindowInsetsListener")
          },
          FieldExtractionDefinition("tag") { it.tag },
          FieldExtractionDefinition("seekBarListener") {
            if (it is SeekBar) {
              getFieldFromSeekBar(it)
            } else {
              null
            }
          }) + extraFields

  fun assertValidRelease(view: View, hierarchyIdentifiers: List<String>) {
    if (!BuildConfig.DEBUG) {
      return
    }

    val viewIdResourceName = getReadableResourceName(view)
    val viewIdentifier =
        "${view.javaClass.simpleName}${if(viewIdResourceName.isNullOrBlank()) "" else "@id/$viewIdResourceName"}"
    val currentHierarchy = hierarchyIdentifiers + listOf(viewIdentifier)
    if (view is ViewGroup) {
      view.children.forEach { child -> assertValidRelease(child, currentHierarchy) }
    }

    val currentHierarchyIdentifier = currentHierarchy.joinToString("->")

    if (excludedPatterns.any {
      it.containsMatchIn(currentHierarchyIdentifier) || it.matches(currentHierarchyIdentifier)
    }) {
      return
    }

    val unreleasedFields = fields.filter { field -> field.extractor(view) != null }
    if (unreleasedFields.isNotEmpty()) {
      val result = buildString {
        append("Improper release detected: ${currentHierarchyIdentifier}\n")
        unreleasedFields.forEach { field -> append("- ${field.id} | ${field.extractor(view)}\n") }

        if (view is TextView) {
          append("- text=${view.text}\n")
        }
        append("\n")
      }

      if (failOnDetection) {
        assert(false) { result }
      } else {
        Log.d(TAG, currentHierarchyIdentifier)
        Log.d(TAG, result)
      }
    }
  }

  private fun getListenerFieldFromViewListenerInfo(view: View, fieldName: String): Any? {
    return try {
      val listenerInfoField: Field = View::class.java.getDeclaredField("mListenerInfo")
      listenerInfoField.isAccessible = true
      val listenerInfo = listenerInfoField.get(view) ?: return null

      val listenerInfoGivenField: Field = listenerInfo.javaClass.getDeclaredField(fieldName)
      listenerInfoGivenField.isAccessible = true
      listenerInfoGivenField.get(listenerInfo)
    } catch (e: NoSuchFieldException) {
      null
    } catch (e: IllegalAccessException) {
      null
    }
  }

  private fun getFieldFromSeekBar(view: SeekBar): Any? {
    return try {
      val listenerInfoField = SeekBar::class.java.getDeclaredField("mOnSeekBarChangeListener")
      listenerInfoField.isAccessible = true
      val listener = listenerInfoField.get(view) as? SeekBar.OnSeekBarChangeListener
      listener
    } catch (e: NoSuchFieldException) {
      null
    } catch (e: IllegalAccessException) {
      null
    }
  }

  private fun getReadableResourceName(view: View): String? {
    return try {
      view.context.resources.getResourceEntryName(view.id)
    } catch (e: Exception) {
      null
    }
  }

  @DataClassGenerate
  data class FieldExtractionDefinition(val id: String, val extractor: (View) -> Any?)
}

private const val TAG = "MountReleaseValidator"
