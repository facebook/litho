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
      @Prop(optional = true) Diff<Integer> spanSize) {

    boolean isSticky = false;
    if (sticky != null && sticky.getNext() != null) {
      isSticky = sticky.getNext();
    }

    int spanSizeVal = 1;
    if (spanSize != null && spanSize.getNext() != null) {
      spanSizeVal = spanSize.getNext();
    }

    if (component.getNext() == null) {
      changeSet.delete(0);
    } else if (component.getPrevious() == null) {
      changeSet.insert(
          0,
          ComponentRenderInfo.create()
              .component(component.getNext())
              .isSticky(isSticky)
              .spanSize(spanSizeVal)
              .build());
    } else {
      changeSet.update(
          0,
          ComponentRenderInfo.create()
              .component(component.getNext())
              .isSticky(isSticky)
              .spanSize(spanSizeVal)
              .build());
    }
  }
}
