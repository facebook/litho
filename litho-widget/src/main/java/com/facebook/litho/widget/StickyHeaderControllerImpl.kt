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

package com.facebook.litho.widget

import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.recyclerview.widget.RecyclerView
import com.facebook.litho.ComponentsReporter
import com.facebook.litho.config.ComponentsConfiguration
import kotlin.math.min

/**
 * Controller that handles sticky header logic. Depending on where the sticky item is located in the
 * list, we might either use first child as sticky header or use [SectionsRecyclerView]'s sticky
 * header.
 */
internal class StickyHeaderControllerImpl(private val hasStickyHeader: HasStickyHeader) :
    RecyclerView.OnScrollListener(), StickyHeaderController {

  private var sectionsRecyclerView: SectionsRecyclerView? = null
  private var layoutManager: RecyclerView.LayoutManager? = null
  private var lastTranslatedView: View? = null
  private var previousStickyHeaderPosition = RecyclerView.NO_POSITION

  override fun init(sectionsRecyclerView: SectionsRecyclerView) {
    if (this.sectionsRecyclerView != null) {
      throw RuntimeException(RECYCLER_ALREADY_INITIALIZED)
    }

    this.sectionsRecyclerView = sectionsRecyclerView
    sectionsRecyclerView.hideStickyHeader()
    layoutManager = sectionsRecyclerView.recyclerView.layoutManager
    if (layoutManager == null) {
      throw RuntimeException(LAYOUTMANAGER_NOT_INITIALIZED)
    }

    sectionsRecyclerView.recyclerView.addOnScrollListener(this)
  }

  override fun reset() {
    val sectionsRecyclerView = checkNotNull(sectionsRecyclerView) { RECYCLER_NOT_INITIALIZED }

    sectionsRecyclerView.recyclerView.removeOnScrollListener(this)
    layoutManager = null
    this.sectionsRecyclerView = null
  }

  override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
    val sectionsRecyclerView = checkNotNull(sectionsRecyclerView) { RECYCLER_NOT_INITIALIZED }
    val layoutManager = checkNotNull(layoutManager) { LAYOUTMANAGER_NOT_INITIALIZED }

    val firstVisiblePosition = hasStickyHeader.findFirstVisibleItemPosition()
    if (firstVisiblePosition == RecyclerView.NO_POSITION) {
      return
    }

    val stickyHeaderPosition = findStickyHeaderPosition(firstVisiblePosition)
    val firstVisibleItemComponentTree =
        hasStickyHeader.getComponentForStickyHeaderAt(firstVisiblePosition)
    val shouldResetLastTranslatedView =
        lastTranslatedView != null &&
            firstVisibleItemComponentTree != null &&
            lastTranslatedView !== firstVisibleItemComponentTree.lithoView

    if (shouldResetLastTranslatedView) {
      // Reset previously modified view
      lastTranslatedView?.translationY = 0f
      lastTranslatedView = null
    }

    val noStickyHeader =
        stickyHeaderPosition == RecyclerView.NO_POSITION || (firstVisibleItemComponentTree == null)
    if (noStickyHeader) {
      // no sticky header above first visible position, reset the state
      sectionsRecyclerView.hideStickyHeader()
      sectionsRecyclerView.maybeRestoreDetachedComponentTree(previousStickyHeaderPosition)
      previousStickyHeaderPosition = RecyclerView.NO_POSITION
      return
    }

    if (firstVisiblePosition == stickyHeaderPosition) {
      val firstVisibleView = checkNotNull(firstVisibleItemComponentTree).lithoView

      if (firstVisibleView == null) {
        // When RV has pending updates adapter position and layout position might not match
        // and firstVisibleView might be null.
        ComponentsReporter.emitMessage(
            ComponentsReporter.LogLevel.ERROR,
            FIRST_VISIBLE_STICKY_HEADER_NULL,
            """First visible sticky header item is null, 
            |RV.hasPendingAdapterUpdates: ${sectionsRecyclerView.recyclerView.hasPendingAdapterUpdates()}, 
            |first visible component: ${firstVisibleItemComponentTree.simpleName}, 
            |hasMounted: ${firstVisibleItemComponentTree.hasMounted()}, 
            |isReleased: ${firstVisibleItemComponentTree.isReleased}
            |"""
                .trimMargin())
      } else {
        // Translate first child, no need for sticky header.
        //
        // NOTE: Translate only if the next item is not also sticky header. If two sticky items are
        // stacked we don't want to translate the first one, as it would hide the second one under
        // the first one which is undesirable.

        if (!hasStickyHeader.isValidPosition(stickyHeaderPosition + 1) ||
            !hasStickyHeader.isSticky(stickyHeaderPosition + 1)) {
          firstVisibleView.translationY = -firstVisibleView.top.toFloat()
        }
      }

      lastTranslatedView = firstVisibleView
      sectionsRecyclerView.hideStickyHeader()
      sectionsRecyclerView.maybeRestoreDetachedComponentTree(stickyHeaderPosition)
      previousStickyHeaderPosition = RecyclerView.NO_POSITION
    } else {
      if (sectionsRecyclerView.isStickyHeaderHidden ||
          stickyHeaderPosition != previousStickyHeaderPosition ||
          ( // RecyclerView can cause a relayout without any scroll changes, check for that here
          ComponentsConfiguration.initStickyHeaderInLayoutWhenComponentTreeIsNull &&
              sectionsRecyclerView.stickyHeader.componentTree == null &&
              dx == 0 &&
              dy == 0 &&
              (sectionsRecyclerView.recyclerView.scrollState == RecyclerView.SCROLL_STATE_IDLE))) {
        initStickyHeader(stickyHeaderPosition)
        sectionsRecyclerView.showStickyHeader()
      }

      // Translate sticky header
      val lastVisiblePosition = hasStickyHeader.findLastVisibleItemPosition()
      var translationY = 0
      for (i in firstVisiblePosition..lastVisiblePosition) {
        if (hasStickyHeader.isSticky(i)) {
          val nextStickyHeader = checkNotNull(layoutManager.findViewByPosition(i))
          val offsetBetweenStickyHeaders =
              nextStickyHeader.top - sectionsRecyclerView.stickyHeader.bottom +
                  sectionsRecyclerView.paddingTop
          translationY = min(offsetBetweenStickyHeaders, 0)
          break
        }
      }
      sectionsRecyclerView.setStickyHeaderVerticalOffset(translationY)
      previousStickyHeaderPosition = stickyHeaderPosition
    }
  }

  private fun initStickyHeader(stickyHeaderPosition: Int) {
    val tree = hasStickyHeader.getComponentForStickyHeaderAt(stickyHeaderPosition)
    if (tree?.isReleased == false) {
      sectionsRecyclerView?.setStickyComponent(tree)
    }
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  fun findStickyHeaderPosition(currentFirstVisiblePosition: Int): Int {
    for (i in currentFirstVisiblePosition downTo 0) if (hasStickyHeader.isSticky(i)) return i
    return RecyclerView.NO_POSITION
  }

  companion object {
    private const val FIRST_VISIBLE_STICKY_HEADER_NULL =
        "StickyHeaderControllerImpl:FirstVisibleStickyHeaderNull"
    const val RECYCLER_ARGUMENT_NULL: String = "Cannot initialize with null SectionsRecyclerView."
    const val RECYCLER_ALREADY_INITIALIZED: String =
        "SectionsRecyclerView has already been initialized but never reset."
    const val RECYCLER_NOT_INITIALIZED: String = "SectionsRecyclerView has not been set yet."
    const val LAYOUTMANAGER_NOT_INITIALIZED: String =
        "LayoutManager of RecyclerView is not initialized yet."
  }
}
