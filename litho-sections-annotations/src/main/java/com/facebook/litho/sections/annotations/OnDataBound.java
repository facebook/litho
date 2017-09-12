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
 * Section props is now visible to the <code>SectionTree.Target</code> of the
 * <code>SectionTree</code>.
 * In the classic case where the Ui for the Section is represented by a RecyclerView, this means
 * that by the time OnDataBound is called the RecyclerView has visibility over the data contained
 * in this section.
 *
 * <p>For example:
 * <pre>
 *  {@literal @GroupSectionSpec}
 *   public class MyGroupSectionSpec {
 *
 *    {@literal OnDataBound}
 *     protected void onDataBound(
 *       SectionContext c,
 *       Service service, // If any
 *      {@literal @}Prop {@code List<? extends Edge>} edges,
 *      {@literal @}Prop RecyclerCollectionEventsController recyclerController,
 *      {@literal @}State(canUpdateLazily = true) boolean didScrollOnce) {
 *         if (!didScrollOnce) {
 *           recyclerController.requestScrollToPosition(10, true);
 *           MyGroupSectionSpec.lazyUpdateDidScrollOnce(c, true);
 *         } if (shouldTakeAction(edges)) {
 *           service.doSomething();
 *         }
 *     }
 * }
 * </pre>
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface OnDataBound {

}
