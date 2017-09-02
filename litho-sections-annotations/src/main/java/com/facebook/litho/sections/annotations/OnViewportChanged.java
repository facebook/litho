/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.annotations;

/**
 * The method annotated with this annotation will be called when the Ui rendering the
 * {@link com.facebook.litho.sections.Section} scrolled to a new position.
 *
 * <p>For example:
 * <pre>
 * {@code
 *
 * @DiffSectionSpec
 * public class MyChangeSetSpec {
 *
 *   @OnViewportChanged
 *   protected void onViewportChanged(
 *     SectionContext c,
 *     int firstVisiblePosition,
 *     int lastVisiblePosition,
 *     int totalCount, // the total count of items this ListComponent is displaying in the list
 *     int firstFullyVisibleIndex,
 *     int lastFullyVisibleIndex,
 *     Service service) { // If any
 *     if (something(firstVisiblePosition, lastVisiblePosition, totalCount) {
 *       service.doSomething();
 *     }
 *   }
 * }
 * </pre>
 *
 * <p> This lifecycle method will only be called if either firstVisiblePosition or
 * lastVisiblePosition have changed in response to a scroll event.
 */
public @interface OnViewportChanged {

}
