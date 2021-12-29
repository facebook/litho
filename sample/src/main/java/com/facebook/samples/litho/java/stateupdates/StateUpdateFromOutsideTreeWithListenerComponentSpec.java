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

package com.facebook.samples.litho.java.stateupdates;

import android.graphics.Typeface;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaEdge;

@LayoutSpec
public class StateUpdateFromOutsideTreeWithListenerComponentSpec {

  // start_implement_observer
  @OnCreateInitialState
  static void onCreateInitialState(
      final ComponentContext c,
      StateValue<Integer> counter,
      @Prop ExternalEventObserver eventObserver) {
    counter.set(0);
    eventObserver.setListener(
        new ExternalEventObserver.Listener() {
          @Override
          public void onMyEvent() {
            // Note: you should not capture any props/state besides the ComponentContext here
            // because they will not be updated for this callback if they change!
            StateUpdateFromOutsideTreeWithListenerComponent.incrementCounter(c);
          }
        });
  }
  // end_implement_observer

  @OnUpdateState
  static void incrementCounter(StateValue<Integer> counter) {
    counter.set(counter.get() + 1);
  }

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @State int counter) {
    return Column.create(c)
        .paddingDip(YogaEdge.ALL, 8)
        .child(
            Text.create(c)
                .text("This event is dispatched to the component via a vanilla listener:")
                .textStyle(Typeface.BOLD))
        .child(
            Text.create(c)
                .marginDip(YogaEdge.ALL, 8)
                .text("Event 1 has been dispatched " + counter + " times."))
        .build();
  }
}
