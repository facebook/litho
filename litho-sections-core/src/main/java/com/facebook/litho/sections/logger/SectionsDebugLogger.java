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

package com.facebook.litho.sections.logger;

import com.facebook.litho.widget.RenderInfo;

/**
 * Sections logger interface. Used for debugging issues inside {@link
 * com.facebook.litho.sections.SectionTree} if they occur.
 */
public interface SectionsDebugLogger {

  /**
   * fired when a component is being inserted
   *
   * @param tag tag defining component's section tree
   * @param index position in the recyclerview
   * @param renderInfo component type that is being operated on
   * @param thread name of thread that wanted to make this change
   */
  void logInsert(String tag, int index, RenderInfo renderInfo, String thread);

  /**
   * fired when a component is being updated
   *
   * @param tag tag defining component's section tree
   * @param index position in the reyclerview
   * @param renderInfo component type that is being operated on
   * @param thread name of thread that wanted to make this change
   */
  void logUpdate(String tag, int index, RenderInfo renderInfo, String thread);

  /**
   * fired when a component is being deleted
   *
   * @param tag tag defining component's section tree
   * @param index position in the reyclerview
   * @param thread name of thread that wanted to make this change
   */
  void logDelete(String tag, int index, String thread);

  /**
   * fired when a component is being focused
   *
   * @param tag tag defining component's section tree
   * @param index position in the reyclerview
   * @param renderInfo component type that is being operated on
   * @param thread name of thread that wanted to make this change
   */
  void logRequestFocus(String tag, int index, RenderInfo renderInfo, String thread);

  /**
   * fired when a component is being focused
   *
   * @param tag tag defining component's section tree
   * @param index position in the reyclerview
   * @param offset offset from index to scroll to
   * @param renderInfo component type that is being operated on
   * @param thread name of thread that wanted to make this change
   */
  void logRequestFocusWithOffset(
      String tag, int index, int offset, RenderInfo renderInfo, String thread);

  /**
   * fired when a component is being moved
   *
   * @param tag tag defining component's section tree
   * @param fromPosition source index
   * @param toPosition destination index
   * @param thread name of thread that wanted to make this change
   */
  void logMove(String tag, int fromPosition, int toPosition, String thread);

  /**
   * fired when determining if a section should re-compute changesets
   *
   * @param tag tag defining component's section tree
   * @param previous previous section that's already made
   * @param next next section being built
   * @param previousPrefix string of component's prefix
   * @param nextPrefix string of component's prefix
   * @param shouldUpdate boolean value describing if previous & next are the same
   * @param thread name of thread that wanted to make this change
   */
  void logShouldUpdate(
      String tag,
      Object previous,
      Object next,
      String previousPrefix,
      String nextPrefix,
      Boolean shouldUpdate,
      String thread);
}
