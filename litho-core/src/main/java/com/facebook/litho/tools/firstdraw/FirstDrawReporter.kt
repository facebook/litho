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

package com.facebook.litho.tools.firstdraw

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewTreeObserver
import com.facebook.litho.ComponentScope
import com.facebook.litho.Style
import com.facebook.litho.annotations.Hook
import com.facebook.litho.binders.viewBinder
import com.facebook.litho.useCallback
import com.facebook.litho.useState
import com.facebook.rendercore.RenderUnit

/**
 * Provides a persistent [FirstDrawReporter] instance to trigger [onDrawn] callback immediately
 * after the first draw for a given component. To bind returned reporter with a component, pass it
 * via the [Style.reportFirstContentDraw] prop.
 *
 * Example:
 * ```
 * class MainScreen : KComponent() {
 *   override fun ComponentScope.render(): Component {
 *     val uiState = useFlow(...)
 *     val reporter = useFirstDrawReporter {
 *       // mark TTRC end
 *     }
 *
 *     return if (uiState.isLoading) {
 *       LoadingUi()
 *     } else {
 *       ContentUi(
 *           data = uiState.items,
 *           style = Style.reportFirstContentDraw(reporter)
 *       )
 *     }
 *   }
 * }
 * ```
 */
@Hook
fun ComponentScope.useFirstDrawReporter(onDrawn: () -> Unit): FirstDrawReporter {
  val onDrawnCallback = useCallback(onDrawn)
  return useState { FirstDrawReporter(onDrawnCallback) }.value
}

/**
 * Style prop that provides [FirstDrawReporter] to report the first draw after [isContentReady] is
 * first set to `true`.
 *
 * @param isContentReady flag to set to true to report the next draw pass.
 */
fun Style.reportFirstContentDraw(
    isContentReady: Boolean = true,
    reporter: FirstDrawReporter
): Style = if (isContentReady) this.viewBinder(reporter) else this

class FirstDrawReporter internal constructor(private val onDrawn: () -> Unit) :
    RenderUnit.Binder<Unit, View, ViewTreeObserver.OnDrawListener> {

  private var isReported = false

  override fun shouldUpdate(
      currentModel: Unit,
      newModel: Unit,
      currentLayoutData: Any?,
      nextLayoutData: Any?
  ): Boolean {
    return false
  }

  override fun bind(
      context: Context,
      view: View,
      model: Unit,
      layoutData: Any?
  ): ViewTreeObserver.OnDrawListener? {
    if (isReported) {
      return null
    }
    val listener = OnDrawListener(view, onDrawn)
    view.viewTreeObserver.addOnDrawListener(listener)
    return listener
  }

  override fun unbind(
      context: Context,
      view: View,
      model: Unit,
      layoutData: Any?,
      bindData: ViewTreeObserver.OnDrawListener?
  ) {
    if (bindData != null && view.viewTreeObserver.isAlive) {
      view.viewTreeObserver.removeOnDrawListener(bindData)
    }
  }

  // This implementation should be in sync with [com.meta.foa.ttrc.OnDrawListener].
  inner class OnDrawListener(
      private val view: View,
      private val onDrawComplete: () -> Unit,
  ) : ViewTreeObserver.OnDrawListener {
    override fun onDraw() {
      if (!isReported) {
        isReported = true

        Handler(checkNotNull(Looper.myLooper())).postAtFrontOfQueue {
          onDrawComplete()
          removeListener()
        }
      } else {
        view.post { removeListener() }
      }
    }

    private fun removeListener() {
      if (view.viewTreeObserver.isAlive) {
        view.viewTreeObserver.removeOnDrawListener(this)
      }
    }
  }
}
