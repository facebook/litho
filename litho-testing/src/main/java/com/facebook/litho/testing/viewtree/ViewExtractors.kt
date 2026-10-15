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

package com.facebook.litho.testing.viewtree

import android.os.Build
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.facebook.litho.ComponentHost
import com.facebook.litho.testing.viewtree.ComponentQueries.ownTextContentText
import com.facebook.rendercore.text.RCTextView
import com.google.common.base.Function
import com.google.common.base.Joiner

/** Function objects used for extracting specific information out of Android classes */
object ViewExtractors {

  @JvmField
  val GET_TEXT_FUNCTION: Function<View, String> = Function { input ->
    var text: CharSequence? = null
    if (input is ComponentHost) {
      val strings = input.ownTextContentText
      if (strings.isNotEmpty()) {
        text = Joiner.on("\", and \"").join(strings)
      }
    } else if (input is TextView) {
      text = input.text
    } else if (input is RCTextView) {
      text = input.text
    }
    if (text == null) {
      "No text found, view is ${getVisibilityString(checkNotNull(input).visibility)}"
    } else {
      String.format(
          "Found text: \"%s\", view is %s",
          text.toString(),
          getVisibilityString(checkNotNull(input).visibility),
      )
    }
  }

  @JvmField
  val GET_DRAWABLE_FUNCTION: Function<View, String> = Function { input ->
    when (input) {
      is ImageView -> {
        String.format(
            "Found drawable: \"%s\", view is %s",
            input.drawable?.toString(),
            getVisibilityString(input.visibility),
        )
      }
      is ComponentHost -> {
        val drawables = ArrayList<String>()
        for (d in input.drawables) {
          drawables.add(d.toString())
        }
        for (d in input.imageContent.imageItems) {
          drawables.add(d.toString())
        }
        input.background?.let { drawables.add(it.toString()) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          input.foreground?.let { drawables.add(it.toString()) }
        }
        String.format(
            "Found drawables: \"%s\", view is %s",
            TextUtils.join("\", \"", drawables),
            getVisibilityString(input.visibility),
        )
      }
      else -> "No drawable found"
    }
  }

  @JvmField
  val GET_CONTENT_DESCRIPTION_FUNCTION: Function<View, String> = Function { input ->
    if (input.contentDescription == null) {
      "No content description found, view is ${getVisibilityString(input.visibility)}"
    } else {
      String.format(
          "Found content description: \"%s\", view is %s",
          input.contentDescription,
          getVisibilityString(input.visibility),
      )
    }
  }

  /**
   * Generates a function that extracts information about view tags from the given view.
   *
   * @param key key that identifies the tag
   * @return function that extracts information about view tags
   */
  @JvmStatic
  fun generateGetViewTagFunction(key: Int): Function<View, String> {
    return Function { input ->
      if (input.getTag(key) == null) {
        "No view tag found, view is ${getVisibilityString(input.visibility)}"
      } else {
        String.format(
            "Found view tag: \"%s\", view is %s",
            input.getTag(key),
            getVisibilityString(input.visibility),
        )
      }
    }
  }

  /** A function that inputs a view and outputs the view's id and visibility. */
  @JvmField
  val GET_VIEW_ID_FUNCTION: Function<View, String> = Function { input ->
    val id = input.id
    String.format(
        "View with id \"%s=%d\" is %s.",
        ViewTreeUtil.getResourceName(id),
        id,
        getVisibilityString(input.visibility),
    )
  }

  private fun getVisibilityString(visibility: Int): String {
    return if (visibility == View.VISIBLE) "visible" else "not visible"
  }
}
