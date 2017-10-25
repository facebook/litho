/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.common;

import android.graphics.drawable.ColorDrawable;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.sections.Children;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.annotations.GroupSectionSpec;
import com.facebook.litho.sections.annotations.OnCreateChildren;
import com.facebook.litho.sections.annotations.OnDataBound;
import com.facebook.litho.widget.Image;
import com.facebook.litho.widget.Text;

/**
 * Dummy {@link GroupSectionSpec} to illustrate how to test sections.
 */
@GroupSectionSpec
public class VerySimpleGroupSectionSpec {

  @OnCreateInitialState
  protected static void onCreateInitialState(
      SectionContext c,
      StateValue<Integer> extra) {
    extra.set(0);
  }

  @OnCreateChildren
  protected static Children onCreateChildren(
      SectionContext c, @State(canUpdateLazily = true) int extra, @Prop int numberOfDummy) {
    Children.Builder builder = Children.create();

    if (extra > 0) {
      builder.child(SingleComponentSection.create(c)
          .component(Image.create(c).drawable(new ColorDrawable()).build()));
    }

    for (int i = 0; i < numberOfDummy+extra; i++) {
      builder.child(SingleComponentSection.create(c)
          .component(Text.create(c).text("Lol hi " + i).build())
          .key("key" + i)
          .build());
    }
    return builder.build();
  }

  @OnDataBound
  static void onDataBound(
      SectionContext c, @Prop int numberOfDummy, @State(canUpdateLazily = true) int extra) {
    VerySimpleGroupSection.lazyUpdateExtra(c, extra - numberOfDummy);
  }

  @OnUpdateState
  static void onUpdateState(
      StateValue<Integer> extra,
      @Param int newExtra) {
    extra.set(newExtra);
  }

  @OnEvent(ClickEvent.class)
  static void onImageClick(SectionContext c) {
    VerySimpleGroupSection.onUpdateState(c, 3);
  }
}
