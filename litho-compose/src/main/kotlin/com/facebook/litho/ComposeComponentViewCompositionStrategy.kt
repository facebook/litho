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

import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnDetachedFromWindow
import androidx.core.view.ancestors
import com.facebook.compose.view.AbstractMetaComposeView
import com.facebook.compose.view.MetaViewCompositionStrategy

/**
 * The composition will be disposed automatically when the view is detached from a window, unless it
 * is pooled by Litho.
 *
 * When not pooled by Litho, this behaves exactly the same as [DisposeOnDetachedFromWindow].
 */
object ComposeComponentViewCompositionStrategy : MetaViewCompositionStrategy {
  override fun installFor(view: AbstractMetaComposeView): () -> Unit {
    val listener =
        object : View.OnAttachStateChangeListener {
          override fun onViewAttachedToWindow(v: View) = Unit

          override fun onViewDetachedFromWindow(v: View) {
            if (!view.isWithinLithoPoolingContainer) {
              view.disposeComposition()
            }
          }
        }
    view.addOnAttachStateChangeListener(listener)

    return { view.removeOnAttachStateChangeListener(listener) }
  }
}

/** Whether one of this View's ancestors has R.id.litho_pooling_container tag set to true. */
val View.isWithinLithoPoolingContainer: Boolean
  get() {
    ancestors.forEach {
      if (it is View && it.getTag(R.id.litho_pooling_container) == true) {
        return true
      }
    }
    return false
  }
