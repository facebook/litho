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

package com.facebook.samples.litho.java.onboarding;

import com.facebook.litho.ClickEvent;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.State;
import com.facebook.litho.widget.Image;

// start_example
@LayoutSpec
public class CheckboxComponentSpec {

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @State boolean isChecked) {

    return Column.create(c)
        .child(
            Image.create(c)
                .drawableRes(
                    isChecked
                        ? android.R.drawable.checkbox_on_background
                        : android.R.drawable.checkbox_off_background))
        .clickHandler(CheckboxComponent.onCheckboxClicked(c))
        .build();
  }

  @OnUpdateState
  static void updateCheckbox(StateValue<Boolean> isChecked) {
    isChecked.set(!isChecked.get());
  }

  @OnEvent(ClickEvent.class)
  static void onCheckboxClicked(ComponentContext c) {
    CheckboxComponent.updateCheckbox(c);
  }
}
// end_example
