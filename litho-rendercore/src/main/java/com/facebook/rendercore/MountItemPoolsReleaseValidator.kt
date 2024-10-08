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

import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.view.children
import com.facebook.kotlin.compilerplugins.dataclassgenerate.annotation.DataClassGenerate
import java.util.WeakHashMap

/**
 * Inspects a [View] to understand if it has any view properties that were not cleaned up before the
 * item is sent back to the [MountContentPools].
 *
 * This should only be used for debugging purposes.
 */
class MountItemPoolsReleaseValidator
internal constructor(
    private val failOnDetection: Boolean = false,
    private val excludedPatterns: Set<Regex> = emptySet(),
    private val onInvalidRelease: ((exception: InvalidReleaseToMountPoolException) -> Unit)? = null,
    /**
     * These are fields the client can add for custom fields they want to inspect. If you have a
     * custom view you can define a specific extraction that verifies if a listener was properly
     * cleaned up for example
     */
    extraFields: List<FieldExtractionDefinition> = emptyList()
) {

  private val pooledViewsToInitialState = WeakHashMap<View, Set<FieldState>>()

  private val fields: Map<String, FieldExtractionDefinition> =
      (setOf(
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
              FieldExtractionDefinition("background") { it.background },
              FieldExtractionDefinition("foreground") {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                  it.foreground
                } else {
                  null
                }
              },
              FieldExtractionDefinition("tag") { it.tag },
              FieldExtractionDefinition("seekBarListener") {
                if (it is SeekBar) {
                  getFieldFromSeekBar(it)
                } else {
                  null
                }
              }) + extraFields)
          .associateBy { it.id }

  fun registerAcquiredViewState(view: View) {
    pooledViewsToInitialState[view] =
        fields.values.map { field -> FieldState(field.id, field.extractor(view)) }.toSet()

    if (view is ViewGroup) {
      view.children.forEach { child -> registerAcquiredViewState(child) }
    }
  }

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

    val beforeReleaseFieldsState =
        fields.values.map { field -> FieldState(field.id, field.extractor(view)) }.toSet()

    val afterPoolFieldsState = pooledViewsToInitialState.remove(view)
    if (beforeReleaseFieldsState != afterPoolFieldsState) {
      val differentFieldsState =
          if (afterPoolFieldsState == null) beforeReleaseFieldsState
          else beforeReleaseFieldsState.minus(afterPoolFieldsState)

      val unreleasedFields = differentFieldsState.filter { field -> field.value != null }
      if (unreleasedFields.isNotEmpty()) {
        val result = buildString {
          append("Improper release detected: ${currentHierarchyIdentifier}\n")
          unreleasedFields.forEach { field -> append("- ${field.id} | ${field.value}\n") }

          if (view is TextView) {
            append("- text=${view.text}\n")
          }
          append("\n")
        }

        onInvalidRelease?.invoke(InvalidReleaseToMountPoolException(result))

        if (failOnDetection) {
          assert(false) { result }
        } else {
          Log.d(TAG, result)
        }
      }
    }
  }

  private fun getListenerFieldFromViewListenerInfo(view: View, fieldName: String): Any? =
      view.safeAccessViewField<View>("mListenerInfo")?.safeAccessObjectField(fieldName)

  private fun getFieldFromSeekBar(view: SeekBar): SeekBar.OnSeekBarChangeListener? =
      view.safeAccessViewField<SeekBar>("mOnSeekBarChangeListener")
          as? SeekBar.OnSeekBarChangeListener

  private fun getReadableResourceName(view: View): String? {
    return try {
      view.context.resources.getResourceEntryName(view.id)
    } catch (e: Exception) {
      null
    }
  }

  private inline fun <reified T> T.safeAccessViewField(fieldName: String): Any? {
    return try {
      val field = T::class.java.getDeclaredField(fieldName)
      field.isAccessible = true
      field.get(this)
    } catch (e: Exception) {
      // ignore
      null
    }
  }

  private fun Any.safeAccessObjectField(fieldName: String): Any? {
    return try {
      val field = this::class.java.getDeclaredField(fieldName)
      field.isAccessible = true
      field.get(this)
    } catch (e: Exception) {
      // ignore
      null
    }
  }

  @DataClassGenerate
  data class FieldExtractionDefinition(val id: String, val extractor: (View) -> Any?)

  @DataClassGenerate data class FieldState(val id: String, val value: Any?)
}

private const val TAG = "MountReleaseValidator"

/**
 * This exception is thrown when a view is released to the pool but it has a listener or any other
 * view property that was not cleaned up.
 */
class InvalidReleaseToMountPoolException(message: String) : RuntimeException(message)
