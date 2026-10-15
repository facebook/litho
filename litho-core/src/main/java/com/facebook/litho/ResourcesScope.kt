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
import androidx.annotation.StyleableRes
import com.facebook.rendercore.BaseResourcesScope
import com.facebook.rendercore.ResourceResolver

/**
 * The receiver for [KComponent] methods which need to access resources and widgets like [Row],
 * [Column], and [Text]. This class exposes the ability to access functions defined in [Resources]
 * like [stringRes]/[drawableRes] etc.
 */
interface ResourcesScope : BaseResourcesScope {
  val context: ComponentContext
  override val androidContext: Context
    get() = context.androidContext

  override val resourceResolver: ResourceResolver
    get() = context.resourceResolver

  /** Retrieves a styled attribute value for provided {@param id}. */
  fun getIntAttrValue(
      componentContext: ComponentContext,
      @StyleableRes id: Int,
      @StyleableRes attrs: IntArray,
      defaultValue: Int,
  ): Int {
    val typedArray = componentContext.obtainStyledAttributes(attrs, 0)
    for (i in 0 until typedArray.indexCount) {
      val attributeIndex = typedArray.getIndex(i)
      if (attributeIndex == id) {
        return typedArray.getInt(attributeIndex, defaultValue)
      }
    }
    typedArray.recycle()
    return defaultValue
  }

  val <T> TreeProp<T>.value: T
    get() = context.getTreeProp(this)
}

/**
 * Creates a standalone [ResourcesScope] from a plain Android [Context], for building a detached
 * component tree outside a [KComponent.render] / [ComponentScope] — e.g. a dialog or bottom sheet
 * constructed from a click handler. Prefer the ambient [ComponentScope] whenever one is available;
 * this is a compatibility escape hatch, hence the `Compat` suffix.
 *
 * Internally wraps [androidContext] in a fresh [ComponentContext]. A [ResourcesScope] (rather than
 * a bare [com.facebook.rendercore.BaseResourcesScope]) is required because the DSL it unlocks —
 * [Row], [Column], and widgets such as Card/Text — builds Litho components and therefore needs a
 * [ComponentContext], not just an Android [Context].
 *
 * This is the supported way to obtain a [ResourcesScope] from a plain Android [Context].
 */
@Suppress("FunctionName")
fun ResourcesScopeCompat(androidContext: Context): ResourcesScope =
    object : ResourcesScope {
      override val context: ComponentContext = ComponentContext(androidContext)
    }

/**
 * Wraps this existing [ComponentContext] in a [ResourcesScope], for calling the `ResourcesScope`
 * DSL ([Row], [Column], widgets such as Card) from code that already holds a [ComponentContext] but
 * has no ambient [ComponentScope] — e.g. a `@LayoutSpec` helper or a builder-style function.
 *
 * Unlike [ResourcesScopeCompat], this does NOT create a new [ComponentContext]; it reuses the
 * receiver, preserving the live context's tree props, state, and component linkage. Prefer the
 * ambient [ComponentScope] whenever one is available.
 */
fun ComponentContext.asResourcesScope(): ResourcesScope =
    object : ResourcesScope {
      override val context: ComponentContext = this@asResourcesScope
    }
