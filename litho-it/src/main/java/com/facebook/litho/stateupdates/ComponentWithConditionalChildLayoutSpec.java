/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.litho.stateupdates;

import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.widget.Text;

@LayoutSpec
public class ComponentWithConditionalChildLayoutSpec {

  public static final int INITIAL_COUNT_VALUE = 0;

  @OnCreateInitialState
  static void OnCreateInitialState(ComponentContext c, StateValue<Boolean> showChild) {
    showChild.set(true);
  }

  @OnCreateLayout
  public static Component onCreateLayout(
      ComponentContext c,
      @Prop(optional = true) Caller caller,
      @Prop(optional = true) ComponentWithCounterStateLayoutSpec.Caller childCaller,
      @State Boolean showChild) {
    if (caller != null) {
      caller.set(c);
    }
    return Column.create(c)
        .child(
            showChild
                ? ComponentWithCounterStateLayout.create(c).caller(childCaller).key("testKey")
                : Text.create(c).text("Empty").key("testKey"))
        .build();
  }

  @OnUpdateState
  static void updateChild(StateValue<Boolean> showChild, @Param boolean showChildValue) {
    showChild.set(showChildValue);
  }

  public static class Caller {

    private ComponentContext c;

    private void set(ComponentContext c) {
      this.c = c;
    }

    public void hideChildComponent() {
      ComponentWithConditionalChildLayout.updateChildSync(c, false);
    }

    public void showChildComponent() {
      ComponentWithConditionalChildLayout.updateChildSync(c, true);
    }
  }
}
