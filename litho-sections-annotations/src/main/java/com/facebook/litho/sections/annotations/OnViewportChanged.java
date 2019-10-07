/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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

package com.facebook.litho.sections.annotations;

/**
 * The method annotated with this annotation will be called when the UI rendering the <code>Section
 * </code> scrolls to a new position.
 *
 * <p>See <a
 * href="https://fblitho.com/docs/communicating-with-the-ui#onviewportchanged">communicating-with-the-ui</a>
 * for details.
 *
 * <p>Example:
 *
 * <pre><code>
 * {@literal @}DiffSectionSpec
 *  public class MyChangeSetSpec {
 *
 *  {@literal @}OnViewportChanged
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
 * </code></pre>
 *
 * <p>This lifecycle method will only be called if either firstVisiblePosition or
 * lastVisiblePosition have changed in response to a scroll event.
 *
 * @see GroupSectionSpec
 * @see DiffSectionSpec
 */
public @interface OnViewportChanged {}
