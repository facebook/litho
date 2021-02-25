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
import java.util.List;
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
  static void onError(
      ComponentContext c, Exception e, @Prop(optional = true) List<Exception> errorOutput) {
    if (errorOutput != null) {
      errorOutput.add(e);
    } else {
      TestErrorBoundary.updateErrorAsync(
          c, String.format("Error caught from boundary: %s", e.getMessage()));
    }
  }
}
