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

package com.facebook.litho.widget;

import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.DynamicValue;
import com.facebook.litho.Row;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;

@LayoutSpec
public class DynamicPropsResetValueTesterSpec {

  public static final float ALPHA_OPAQUE = 1f;
  public static final float ALPHA_TRANSPARENT = 0f;

  @OnCreateInitialState
  static void onCreateInitialState(
      ComponentContext c,
      StateValue<Boolean> displayConditionalChild,
      StateValue<DynamicValue<Float>> dynamicAlpha) {
    displayConditionalChild.set(false);
    dynamicAlpha.set(new DynamicValue<>(ALPHA_TRANSPARENT));
  }

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c,
      @Prop Caller caller,
      @State boolean displayConditionalChild,
      @State DynamicValue<Float> dynamicAlpha) {
    caller.set(c);

    Row.Builder builder = Row.create(c);
    if (!displayConditionalChild) {
      builder.child(
          Column.create(c)
              .child(Text.create(c).text("Child 1").alpha(dynamicAlpha).viewTag("vt1")));
    }

    builder.child(Row.create(c).child(Text.create(c).text("Child 2").viewTag("vt2")));

    return builder.build();
  }

  @OnUpdateState
  static void toggleShowChild(StateValue<Boolean> displayConditionalChild) {
    displayConditionalChild.set(!displayConditionalChild.get());
  }

  public static class Caller {

    ComponentContext c;

    void set(ComponentContext c) {
      this.c = c;
    }

    public void toggleShowChild() {
      DynamicPropsResetValueTester.toggleShowChildSync(c);
    }
  }
}
