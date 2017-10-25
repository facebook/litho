/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
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
