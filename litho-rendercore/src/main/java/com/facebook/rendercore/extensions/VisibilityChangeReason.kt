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

/**
 * Indicates the reason why a visibility change event was triggered.
 *
 * Understanding the reason for a visibility change can help consumers make more informed decisions
 * about how to respond. For example, a video player might want to pause on [LIFECYCLE] when the app
 * goes to background but continue playing during [SCROLL] if still partially visible.
 */
enum class VisibilityChangeReason {
  /**
   * Visibility changed due to user scrolling, flinging, or ViewPager swiping.
   *
   * This is the most common reason for visibility changes. The component's visible rect changed
   * because the user scrolled the content, causing components to move in or out of the viewport.
   * This includes ViewPager page swipes where child views move in/out of the visible area.
   */
  SCROLL,

  /**
   * Visibility changed due to component mounting or unmounting.
   *
   * This occurs when:
   * - A component is first mounted to the screen
   * - A component is unmounted (removed from the screen)
   * - A component is re-mounted after a layout change
   * - The layout tree is recalculated and visibility is reprocessed
   *
   * This is distinct from [SCROLL] because the visibility change is not due to user scrolling but
   * rather the component system computing and applying a new layout. Use the event's
   * percentVisibleHeight/Width to determine if the component is becoming visible or invisible.
   */
  MOUNT,

  /**
   * Visibility changed due to app or fragment lifecycle state change.
   *
   * This happens when the host Fragment or Activity changes lifecycle state:
   * - App going to background or returning to foreground
   * - Activity being paused or resumed
   * - Fragment becoming visible or hidden due to navigation
   *
   * Use the event's percentVisibleHeight/Width to determine if the component is becoming visible
   * (lifecycle resumed) or invisible (lifecycle paused). Components may want to pause expensive
   * operations when becoming invisible and resume them when becoming visible again.
   */
  LIFECYCLE,

  /**
   * Visibility changed due to a visibility hint being set programmatically.
   *
   * This occurs when [BaseMountingView.setVisibilityHint] is called to manually indicate that the
   * view should be considered visible or invisible. This is typically used when:
   * - A new Fragment/Activity is added on top, obscuring the view
   * - Custom visibility management is needed beyond what the system provides
   *
   * Unlike [LIFECYCLE], this does not necessarily mean the Activity/Fragment lifecycle changed—it's
   * a manual override of visibility state.
   */
  VISIBILITY_HINT,

  /**
   * Visibility changed due to an animation ending or transient state change.
   *
   * This occurs when:
   * - An animation completes and visibility needs to be recalculated
   * - View transient state changes (e.g., via setHasTransientState)
   * - Scale/alpha animations that affect visible bounds finish
   *
   * This is distinct from [SCROLL] because the visibility change is triggered by animation
   * completion rather than user-initiated scrolling.
   */
  ANIMATION,

  /**
   * Visibility changed but the specific reason could not be determined.
   *
   * This is a fallback value used when the visibility change source is ambiguous or when called
   * from code paths that don't have visibility reason tracking. New code should avoid using this
   * and instead propagate the appropriate reason.
   */
  UNKNOWN,
}
