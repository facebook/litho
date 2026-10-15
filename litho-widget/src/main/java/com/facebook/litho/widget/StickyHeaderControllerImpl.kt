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
import android.view.ViewGroup.LayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentTree
import com.facebook.litho.ComponentsReporter
import com.facebook.litho.LithoView
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
    setupStickyHeaderView(sectionsRecyclerView)
    hideStickyHeader()
    layoutManager = sectionsRecyclerView.recyclerView.layoutManager
    if (layoutManager == null) {
      throw RuntimeException(LAYOUTMANAGER_NOT_INITIALIZED)
    }

    sectionsRecyclerView.recyclerView.addOnScrollListener(this)
  }

  override fun reset() {
    val sectionsRecyclerView = checkNotNull(sectionsRecyclerView) { RECYCLER_NOT_INITIALIZED }

    sectionsRecyclerView.recyclerView.removeOnScrollListener(this)
    sectionsRecyclerView.stickyHeaderView = null
    layoutManager = null
    this.sectionsRecyclerView = null
  }

  override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
    val sectionsRecyclerView = checkNotNull(sectionsRecyclerView) { RECYCLER_NOT_INITIALIZED }
    val stickyHeaderView =
        checkNotNull(sectionsRecyclerView.stickyHeaderView) { STICKY_HEADER_NOT_INITIALIZED }
            as LithoView
    val layoutManager = checkNotNull(layoutManager) { LAYOUTMANAGER_NOT_INITIALIZED }

    // Find the first visible item position in the RecyclerView
    val firstVisiblePosition = hasStickyHeader.findFirstVisibleItemPosition()
    if (firstVisiblePosition == RecyclerView.NO_POSITION) {
      return // Exit early if no visible items
    }

    // Find the position of the sticky header that should be displayed (searching backwards from
    // first visible)
    val stickyHeaderPosition = findStickyHeaderPositionBefore(firstVisiblePosition)
    // Get the component tree for the first visible item
    val firstVisibleItemComponentTree =
        hasStickyHeader.getComponentForStickyHeaderAt(firstVisiblePosition)
    // Check if we need to reset the translation of the previously translated view
    val shouldResetLastTranslatedView =
        lastTranslatedView != null &&
            firstVisibleItemComponentTree != null &&
            lastTranslatedView !== firstVisibleItemComponentTree.getLithoView()

    if (shouldResetLastTranslatedView) {
      // Reset previously modified view's translation back to original position
      lastTranslatedView?.translationY = 0f
      lastTranslatedView = null
    }

    val noStickyHeader =
        stickyHeaderPosition == RecyclerView.NO_POSITION || (firstVisibleItemComponentTree == null)
    if (noStickyHeader) {
      // No sticky header above first visible position, reset the state
      hideStickyHeader()
      maybeRestoreDetachedComponentTree(
          recyclerView,
          stickyHeaderView,
          previousStickyHeaderPosition,
      )
      previousStickyHeaderPosition = RecyclerView.NO_POSITION
      return
    }

    if (firstVisiblePosition == stickyHeaderPosition) {
      // Case 1: The first visible item is a sticky header

      val firstVisibleView = checkNotNull(firstVisibleItemComponentTree).getLithoView()

      if (firstVisibleView == null) {
        // When RV has pending updates adapter position and layout position might not match
        // and firstVisibleView might be null.
        ComponentsReporter.emitMessage(
            ComponentsReporter.LogLevel.ERROR,
            FIRST_VISIBLE_STICKY_HEADER_NULL,
            """First visible sticky header item is null,
            |RV.hasPendingAdapterUpdates: ${sectionsRecyclerView.recyclerView.hasPendingAdapterUpdates()},
            |first visible component: ${firstVisibleItemComponentTree.getSimpleName()},
            |hasMounted: ${firstVisibleItemComponentTree.hasMounted()},
            |isReleased: ${firstVisibleItemComponentTree.isReleased}
            |"""
                .trimMargin(),
        )
      } else {
        // Translate first child, no need for sticky header.
        //
        // NOTE: Translate only if the next item is not also sticky header. If two sticky items are
        // stacked we don't want to translate the first one, as it would hide the second one under
        // the first one which is undesirable.

        if (
            !hasStickyHeader.isValidPosition(stickyHeaderPosition + 1) ||
                !hasStickyHeader.isSticky(stickyHeaderPosition + 1)
        ) {
          firstVisibleView.translationY = -firstVisibleView.top.toFloat()
        }
      }

      // Track the translated view and hide the separate sticky header
      lastTranslatedView = firstVisibleView
      hideStickyHeader()
      maybeRestoreDetachedComponentTree(recyclerView, stickyHeaderView, stickyHeaderPosition)
      previousStickyHeaderPosition = RecyclerView.NO_POSITION
    } else {
      // Case 2: The sticky header is above the first visible item, use separate sticky header view

      if (
          sectionsRecyclerView.isStickyHeaderHidden ||
              stickyHeaderPosition != previousStickyHeaderPosition ||
              ( // RecyclerView can cause a relayout without any scroll changes, check for that here
              ComponentsConfiguration.initStickyHeaderInLayoutWhenComponentTreeIsNull &&
                  stickyHeaderView.componentTree == null &&
                  dx == 0 &&
                  dy == 0 &&
                  (sectionsRecyclerView.recyclerView.scrollState == RecyclerView.SCROLL_STATE_IDLE))
      ) {
        initStickyHeader(sectionsRecyclerView, stickyHeaderView, stickyHeaderPosition)
        showStickyHeader()
      }

      // Calculate translation for sticky header to handle multiple sticky headers stacking
      val lastVisiblePosition = hasStickyHeader.findLastVisibleItemPosition()
      var translationY = 0
      // Look for the next sticky header in visible range
      for (i in firstVisiblePosition..lastVisiblePosition) {
        if (hasStickyHeader.isSticky(i)) {
          val nextStickyHeader = checkNotNull(layoutManager.findViewByPosition(i))
          // Calculate offset to push current sticky header up when next one approaches
          val offsetBetweenStickyHeaders =
              nextStickyHeader.top - stickyHeaderView.bottom + sectionsRecyclerView.paddingTop
          translationY = min(offsetBetweenStickyHeaders, 0) // Only negative translation (upward)
          break
        }
      }
      // Apply the calculated translation to the sticky header
      setStickyHeaderVerticalOffset(stickyHeaderView, translationY)
      previousStickyHeaderPosition = stickyHeaderPosition
    }
  }

  private fun initStickyHeader(
      sectionsRecyclerView: SectionsRecyclerView,
      stickyHeaderView: LithoView,
      stickyHeaderPosition: Int,
  ) {
    val tree = hasStickyHeader.getComponentForStickyHeaderAt(stickyHeaderPosition)
    if (tree?.isReleased == false) {
      bindStickHeader(sectionsRecyclerView, stickyHeaderView, tree)
    }
  }

  fun findStickyHeaderPositionBefore(position: Int): Int {
    for (i in position downTo 0) {
      if (hasStickyHeader.isSticky(i)) return i
    }
    return RecyclerView.NO_POSITION
  }

  private fun setupStickyHeaderView(sectionsRecyclerView: SectionsRecyclerView) {
    if (sectionsRecyclerView.stickyHeaderView != null) return

    sectionsRecyclerView.stickyHeaderView =
        LithoView(ComponentContext(sectionsRecyclerView.context), null).apply {
          layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
          id = R.id.sticky_header_id
        }
  }

  fun maybeRestoreDetachedComponentTree(
      recyclerView: RecyclerView,
      stickyHeaderView: LithoView,
      stickyHeaderPosition: Int,
  ) {
    if (
        stickyHeaderPosition <= -1 ||
            !ComponentsConfiguration.initStickyHeaderInLayoutWhenComponentTreeIsNull ||
            stickyHeaderView.componentTree == null
    ) {
      return
    }

    val viewHolder = recyclerView.findViewHolderForAdapterPosition(stickyHeaderPosition)
    val recyclerBinderViewHolder = viewHolder as? RecyclerBinderViewHolder
    val lithoView = recyclerBinderViewHolder?.lithoView
    if (
        lithoView != null &&
            lithoView.componentTree == null &&
            lithoView.hasTemporaryDetachedComponentTree()
    ) {
      lithoView.requestLayout()
    }
  }

  fun showStickyHeader() {
    sectionsRecyclerView?.stickyHeaderView?.visibility = View.VISIBLE
    sectionsRecyclerView?.layoutStickyHeader()
  }

  fun hideStickyHeader() {
    (sectionsRecyclerView?.stickyHeaderView as? LithoView)?.unmountAllItems()
    sectionsRecyclerView?.stickyHeaderView?.visibility = View.GONE
  }

  fun bindStickHeader(
      sectionRecyclerView: SectionsRecyclerView,
      stickyHeaderView: LithoView,
      componentTree: ComponentTree,
  ) {
    val existingLithoView = componentTree.getLithoView()
    stickyHeaderView.componentTree = componentTree

    // Set this after because setComponentTree clears temporary detached component tree
    if (existingLithoView != null && stickyHeaderView !== existingLithoView) {
      existingLithoView.setTemporaryDetachedComponentTree(componentTree)
    }
    sectionRecyclerView.measureStickyHeader(sectionRecyclerView.width)
  }

  fun setStickyHeaderVerticalOffset(stickyHeaderView: LithoView, verticalOffset: Int) {
    stickyHeaderView.translationY = verticalOffset.toFloat()
  }

  companion object {
    private const val FIRST_VISIBLE_STICKY_HEADER_NULL =
        "StickyHeaderControllerImpl:FirstVisibleStickyHeaderNull"
    const val RECYCLER_ARGUMENT_NULL: String = "Cannot initialize with null SectionsRecyclerView."
    const val RECYCLER_ALREADY_INITIALIZED: String =
        "SectionsRecyclerView has already been initialized but never reset."
    const val RECYCLER_NOT_INITIALIZED: String = "SectionsRecyclerView has not been set yet."
    const val STICKY_HEADER_NOT_INITIALIZED: String = "Sticky header view has not been set yet."
    const val LAYOUTMANAGER_NOT_INITIALIZED: String =
        "LayoutManager of RecyclerView is not initialized yet."
  }
}
