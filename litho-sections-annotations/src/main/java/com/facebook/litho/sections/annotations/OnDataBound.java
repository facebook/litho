/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.sections.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The method annotated with this annotation will be called when the data corresponding to this
 * Section props is now visible to the <code>SectionTree.Target</code> of the <code>SectionTree
 * </code>. In the classic case where the Ui for the Section is represented by a RecyclerView, this
 * means that by the time OnDataBound is called the RecyclerView has visibility over the data
 * contained in this section.
 *
 * <p>For example:
 *
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
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface OnDataBound {}
