/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.common;

import com.facebook.litho.Component;
import com.facebook.litho.Diff;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.sections.ChangeSet;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.annotations.DiffSectionSpec;
import com.facebook.litho.sections.annotations.OnDiff;
import com.facebook.litho.widget.ComponentRenderInfo;

@DiffSectionSpec
public class SingleComponentSectionSpec {

  @OnDiff
  public static void onCreateChangeSet(
      SectionContext context,
      ChangeSet changeSet,
      @Prop Diff<Component> component,
      @Prop(optional = true) Diff<Boolean> sticky,
      @Prop(optional = true) Diff<Integer> spanSize,
      @Prop(optional = true) Diff<Boolean> isFullSpan) {

    if (component.getNext() == null) {
      changeSet.delete(0);
      return;
    }

    boolean isNextSticky = false;
    if (sticky != null && sticky.getNext() != null) {
      isNextSticky = sticky.getNext();
    }

    int nextSpanSize = 1;
    if (spanSize != null && spanSize.getNext() != null) {
      nextSpanSize = spanSize.getNext();
    }

    boolean isNextFullSpan = false;
    if (isFullSpan != null && isFullSpan.getNext() != null) {
      isNextFullSpan = isFullSpan.getNext();
    }

    if (component.getPrevious() == null) {
      changeSet.insert(
          0,
          ComponentRenderInfo.create()
              .component(component.getNext())
              .isSticky(isNextSticky)
              .spanSize(nextSpanSize)
              .isFullSpan(isNextFullSpan)
              .build());
      return;
    }

    // Check if update is required.
    boolean isPrevSticky = false;
    if (sticky != null && sticky.getPrevious() != null) {
      isPrevSticky = sticky.getPrevious();
    }

    int prevSpanSize = 1;
    if (spanSize != null && spanSize.getPrevious() != null) {
      prevSpanSize = spanSize.getPrevious();
    }

    boolean isPrevFullSpan = false;
    if (isFullSpan != null && isFullSpan.getPrevious() != null) {
      isPrevFullSpan = isFullSpan.getPrevious();
    }

    if (isPrevSticky != isNextSticky
        || prevSpanSize != nextSpanSize
        || isPrevFullSpan != isNextFullSpan
        || !component.getPrevious().isEquivalentTo(component.getNext())) {
      changeSet.update(
          0,
          ComponentRenderInfo.create()
              .component(component.getNext())
              .isSticky(isNextSticky)
              .spanSize(nextSpanSize)
              .isFullSpan(isNextFullSpan)
              .build());
    }
  }
}
