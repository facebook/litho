/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The method annotated with this annotation will be called when the data corresponding to this
 * Section props is now visible to the {@link com.facebook.litho.list.SectionTree.Target} of the
 * {@link com.facebook.litho.list.SectionTree}.
 * In the classic case where the Ui for the Section is represented by a RecyclerView, this means
 * that by the time OnDataBound is called the RecyclerView has visibility over the data contained
 * in this section.
 *
 * <p>For example:
 * <pre>
 * {@code
 *
 * @GroupSectionSpec
 * public class MyGroupSectionSpec {
 *
 *   @OnDataBound
 *   protected void onDataBound(
 *     SectionContext c,
 *     Service service, // If any
 *     @Prop List<? extends Edge> edges,
 *     @Prop RecyclerCollectionEventsController recyclerController,
 *     @State(canUpdateLazily = true) boolean didScrollOnce) {
 *       if (!didScrollOnce) {
 *         recyclerController.requestScrollToPosition(10, true);
 *         MyGroupSectionSpec.lazyUpdateDidScrollOnce(c, true);
 *       }
 *       if (shouldTakeAction(edges)) {
 *         service.doSomething();
 *       }
 *   }
 * }
 * </pre>
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface OnDataBound {

}
