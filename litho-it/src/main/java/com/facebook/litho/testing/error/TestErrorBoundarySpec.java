/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.litho.testing.error;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnError;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaEdge;
import java.util.Optional;

@LayoutSpec
public class TestErrorBoundarySpec {

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c, @Prop Component child, @State Optional<String> error) {

    if (error.isPresent()) {
      return Text.create(c)
          .marginDip(YogaEdge.ALL, 8)
          .textSizeSp(24)
          .text(String.format("A WILD ERROR APPEARS:\n%s", error.get()))
          .build();
    }

    return child;
  }

  @OnCreateInitialState
  static void createInitialState(ComponentContext c, StateValue<Optional<String>> error) {

    error.set(Optional.<String>empty());
  }

  @OnUpdateState
  static void updateError(StateValue<Optional<String>> error, @Param String errorMsg) {
    error.set(Optional.of(errorMsg));
  }

  @OnError
  static void onError(ComponentContext c, Exception e) {
    TestErrorBoundary.updateError(
        c, String.format("Error caught from boundary: %s", e.getMessage()));
  }
}
