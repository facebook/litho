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

package com.facebook.samples.litho.java.communicating;

import android.widget.Toast;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.FromTrigger;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnTrigger;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.widget.Text;

@LayoutSpec
class ChildComponentReceivesEventFromParentSpec {

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @Prop String textFromParent) {
    return Column.create(c)
        .child(Text.create(c).text("ChildComponent").textSizeDip(20))
        .child(Text.create(c).text("Text received from parent: " + textFromParent).textSizeDip(15))
        .build();
  }

  // start_define_trigger
  @OnTrigger(ShowToastEvent.class)
  static void triggerOnShowToastEvent(ComponentContext c, @FromTrigger String message) {
    Toast.makeText(c.getAndroidContext(), message, Toast.LENGTH_SHORT).show();
  }
  // end_define_trigger
}
