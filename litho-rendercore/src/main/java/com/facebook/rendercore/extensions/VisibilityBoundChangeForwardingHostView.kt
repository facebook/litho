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

package com.facebook.rendercore.extensions

import android.content.Context
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import com.facebook.rendercore.R
import com.facebook.rendercore.RenderCoreSystrace

/**
 * A [FrameLayout] that detects changes to its visible bounds (scroll, translation, visibility) and
 * forwards notifications to a [VisibilityBoundsChangeNotifier].
 *
 * This view achieves correct visibility tracking for nested component views by two behaviors:
 * - It detects scroll changes and translation changes, and propagates a notification that the
 *   visible bounds have changed.
 * - It observes visibility changes (VISIBLE, INVISIBLE, GONE) and propagates this information so
 *   that nested component views can mount/unmount and fire visibility events accordingly.
 *
 * @param context The Android context.
 * @param notifier The [VisibilityBoundsChangeNotifier] to forward visibility events to.
 * @param isHostResumed Optional callback that returns whether the host (e.g. Fragment) is in a
 *   resumed state. When provided, the view will post a deferred visible-bounds notification on
 *   attach if the host is not resumed. This works around cases where onDestroyView may not be
 *   called when a Fragment is replaced, causing incorrect mount state on return.
 * @param forwardScrollChanges When `true` (default), this view registers a
 *   [ViewTreeObserver.OnScrollChangedListener] on attach and forwards every scroll callback as a
 *   visible-bounds-change notification to [notifier]. Pass `false` when an ancestor already
 *   dispatches `notifyVisibleBoundsChanged` to nested
 *   [com.facebook.rendercore.RenderCoreExtensionHost] instances inside this view (e.g. a Litho
 *   recycler whose IM extension recursively walks `mountedOutputIdsWithNestedContent`) — the
 *   registered listener would then be a duplicate firing once per scroll event for every attached
 *   `VBCFHV` in the window. Translation forwarding ([setTranslationX] / [setTranslationY]
 *   overrides) and visibility forwarding ([setVisibility] override) are unaffected by this flag.
 */
open class VisibilityBoundChangeForwardingHostView(
    context: Context,
    private val notifier: VisibilityBoundsChangeNotifier,
    private val isHostResumed: (() -> Boolean)? = null,
    private val forwardScrollChanges: Boolean = true,
) : FrameLayout(context) {

  private val scrollChangeListener = ViewTreeObserver.OnScrollChangedListener {
    notifyVisibleBoundsChange()
  }

  init {
    setTag(VISIBILITY_BOUNDS_CHANGE_NOTIFIER_TAG_KEY, notifier)
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    if (forwardScrollChanges) {
      viewTreeObserver.addOnScrollChangedListener(scrollChangeListener)
    }
    val resumed = isHostResumed
    if (resumed != null && !resumed()) {
      post { notifyVisibleBoundsChange() }
    }
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    if (forwardScrollChanges) {
      viewTreeObserver.removeOnScrollChangedListener(scrollChangeListener)
    }
  }

  override fun setTranslationX(translationX: Float) {
    super.setTranslationX(translationX)
    notifyVisibleBoundsChange()

    if (translationX == 0f) {
      post { notifyVisibleBoundsChange() }
    }
  }

  override fun setTranslationY(translationY: Float) {
    super.setTranslationY(translationY)
    notifyVisibleBoundsChange()
  }

  protected open fun notifyVisibleBoundsChange() {
    if (RenderCoreSystrace.isTracing()) {
      RenderCoreSystrace.beginSection(
          "VisibilityBoundChangeForwardingHostView#notifyVisibleBoundsChange",
      )
      try {
        notifier.notifyVisibleBoundsChanged(this, VisibilityChangeReason.SCROLL)
      } finally {
        RenderCoreSystrace.endSection()
      }
    } else {
      notifier.notifyVisibleBoundsChanged(this, VisibilityChangeReason.SCROLL)
    }
  }

  override fun setVisibility(visibility: Int) {
    super.setVisibility(visibility)
    notifyVisibilityChange(visibility)
  }

  protected open fun notifyVisibilityChange(visibility: Int) {
    if (RenderCoreSystrace.isTracing()) {
      val visibilityString =
          when (visibility) {
            View.VISIBLE -> "VISIBLE"
            View.INVISIBLE -> "INVISIBLE"
            View.GONE -> "GONE"
            else -> "Unknown"
          }
      RenderCoreSystrace.beginSection(
          "VisibilityBoundChangeForwardingHostView#setVisibility#$visibilityString",
      )
      try {
        notifier.notifyVisibilityChanged(this, visibility, VisibilityChangeReason.SCROLL)
      } finally {
        RenderCoreSystrace.endSection()
      }
    } else {
      notifier.notifyVisibilityChanged(this, visibility, VisibilityChangeReason.SCROLL)
    }
  }

  companion object {
    @JvmField
    val VISIBILITY_BOUNDS_CHANGE_NOTIFIER_TAG_KEY: Int = R.id.visibility_bounds_change_notifier_tag
  }
}
