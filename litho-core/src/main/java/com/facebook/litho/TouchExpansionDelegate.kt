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

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.MotionEvent
import android.view.TouchDelegate
import android.view.View
import android.view.ViewConfiguration
import androidx.annotation.VisibleForTesting
import androidx.collection.SparseArrayCompat
import com.facebook.rendercore.MountItem

/** Compound touch delegate that forward touch events to recyclable inner touch delegates. */
@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
class TouchExpansionDelegate(host: ComponentHost?) : TouchDelegate(IGNORED_RECT, host) {

  private val delegates = SparseArrayCompat<InnerTouchDelegate?>()
  private var scrapDelegates: SparseArrayCompat<InnerTouchDelegate?>? = null

  /**
   * Registers an inner touch delegate for the given view with the specified expansion. It assumes
   * the given view has its final bounds set.
   *
   * @param index The drawing order index of the given view.
   * @param view The view to which touch expansion should be applied.
   * @param item The mount item which requires touch expansion.
   */
  fun registerTouchExpansion(index: Int, view: View, item: MountItem) {
    delegates.put(index, InnerTouchDelegate(view, item))
  }

  /**
   * Unregisters an inner touch delegate with the given index.
   *
   * @param index The drawing order index of the given view.
   */
  fun unregisterTouchExpansion(index: Int) {
    if (maybeUnregisterFromScrap(index)) {
      return
    }
    delegates.remove(index)
  }

  private fun maybeUnregisterFromScrap(index: Int): Boolean {
    val touchDelegate = scrapDelegates?.get(index)
    if (touchDelegate != null) {
      scrapDelegates?.remove(index)
      return true
    }
    return false
  }

  fun draw(canvas: Canvas, paint: Paint) {
    for (i in delegates.size() - 1 downTo 0) {
      val delegate = delegates.valueAt(i)
      if (delegate != null) {
        val bounds = delegate.getDelegateBounds()
        if (bounds != null) {
          canvas.drawRect(bounds, paint)
        }
      }
    }
  }

  override fun onTouchEvent(event: MotionEvent): Boolean {
    for (i in delegates.size() - 1 downTo 0) {
      val touchDelegate = delegates.valueAt(i)
      if (touchDelegate?.onTouchEvent(event) == true) {
        return true
      }
    }
    return false
  }

  /**
   * Called when the MountItem this Delegate is referred to is moved to another position to also
   * update the indexes of the TouchExpansionDelegate.
   */
  fun moveTouchExpansionIndexes(oldIndex: Int, newIndex: Int) {
    if (delegates[newIndex] != null) {
      ensureScrapDelegates()
      ComponentHostUtils.scrapItemAt(newIndex, delegates, scrapDelegates)
    }
    ComponentHostUtils.moveItem(oldIndex, newIndex, delegates, scrapDelegates)
    releaseScrapDelegatesIfNeeded()
  }

  private fun ensureScrapDelegates() {
    if (scrapDelegates == null) {
      scrapDelegates = SparseArrayCompat(4)
    }
  }

  @VisibleForTesting fun size(): Int = delegates.size()

  private fun releaseScrapDelegatesIfNeeded() {
    if (CollectionsUtils.isEmpty(scrapDelegates)) {
      scrapDelegates = null
    }
  }

  private class InnerTouchDelegate(private val delegateView: View, private val item: MountItem) {
    private var isHandlingTouch = false

    fun getDelegateBounds(): Rect? {
      val expansion =
          LithoLayoutData.getExpandedTouchBounds(item.renderTreeNode.layoutData) ?: return null
      val bounds = item.renderTreeNode.bounds
      return Rect(
          bounds.left - expansion.left,
          bounds.top - expansion.top,
          bounds.right + expansion.right,
          bounds.bottom + expansion.bottom)
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
      val x = event.x.toInt()
      val y = event.y.toInt()
      val delegateBounds = getDelegateBounds() ?: return false
      val slop = ViewConfiguration.get(delegateView.context).scaledTouchSlop
      val delegateSlopBounds = Rect()
      delegateSlopBounds.set(delegateBounds)
      delegateSlopBounds.inset(-slop, -slop)
      var shouldDelegateTouchEvent = false
      var touchWithinViewBounds = true
      var handled = false
      when (event.action) {
        MotionEvent.ACTION_DOWN -> {
          isHandlingTouch = delegateBounds.contains(x, y)
          shouldDelegateTouchEvent = isHandlingTouch
        }
        MotionEvent.ACTION_UP,
        MotionEvent.ACTION_MOVE -> {
          shouldDelegateTouchEvent = isHandlingTouch
          if (isHandlingTouch) {
            if (!delegateSlopBounds.contains(x, y)) {
              touchWithinViewBounds = false
            }
          }
          if (event.action == MotionEvent.ACTION_UP) {
            isHandlingTouch = false
          }
        }
        MotionEvent.ACTION_CANCEL -> {
          shouldDelegateTouchEvent = isHandlingTouch
          isHandlingTouch = false
        }
        else -> {}
      }
      if (shouldDelegateTouchEvent) {
        if (touchWithinViewBounds) {
          // Offset event coordinates to be inside the target view.
          event.setLocation((delegateView.width / 2).toFloat(), (delegateView.height / 2).toFloat())
        } else {
          // Offset event coordinates to be outside the target view (in case it does
          // something like tracking pressed state).
          event.setLocation(-(slop * 2).toFloat(), -(slop * 2).toFloat())
        }
        handled = delegateView.dispatchTouchEvent(event)
      }
      return handled
    }
  }

  companion object {
    private val IGNORED_RECT = Rect()
  }
}
