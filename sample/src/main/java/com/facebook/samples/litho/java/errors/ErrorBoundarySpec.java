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

package com.facebook.samples.litho.java.errors;

import androidx.annotation.Nullable;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnError;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;

@LayoutSpec
public class ErrorBoundarySpec {

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c, @Prop Component child, @State @Nullable Exception error) {

    if (error != null) {
      return DebugErrorComponent.create(c).message("Error Boundary").throwable(error).build();
    }

    return child;
  }

  @OnUpdateState
  static void updateError(StateValue<Exception> error, @Param Exception e) {
    error.set(e);
  }

  @OnError
  static void onError(ComponentContext c, Exception error) {
    ErrorBoundary.updateErrorSync(c, error);
  }
}
