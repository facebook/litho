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

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Handle;
import com.facebook.litho.LithoView;
import com.facebook.samples.litho.NavigatableDemoActivity;

public class StateUpdateFromOutsideTreeActivity extends NavigatableDemoActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final ComponentContext componentContext = new ComponentContext(this);
    final LinearLayout container = new LinearLayout(this);
    container.setOrientation(LinearLayout.VERTICAL);

    final ExternalEventObserver observer1 = new ExternalEventObserver();

    // start_external_observer
    container.addView(
        LithoView.create(
            componentContext,
            StateUpdateFromOutsideTreeWithListenerComponent.create(componentContext)
                .eventObserver(observer1)
                .build()));

    final Button button1 = new Button(this);
    button1.setText("Dispatch Event 1");
    button1.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            observer1.notifyExternalEventOccurred();
          }
        });

    // end_external_observer
    container.addView(button1);

    // start_external_handle
    final Handle componentHandle = new Handle();
    final LithoView lithoViewWithTrigger =
        LithoView.create(
            componentContext,
            StateUpdateFromOutsideTreeWithTriggerComponent.create(componentContext)
                .handle(componentHandle)
                .build());

    final Button button2 = new Button(this);
    button2.setText("Dispatch Event 2");
    button2.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            StateUpdateFromOutsideTreeWithTriggerComponent.notifyExternalEvent(
                // This is a bit of a gotcha right now: you need to use the ComponentContext from
                // the ComponentTree to dispatch the trigger from outside a Component.
                lithoViewWithTrigger.getComponentTree().getContext(),
                componentHandle,
                1 /* pass through the increment to show you can pass arbitrary data */);
          }
        });
    // end_external_handle
    container.addView(lithoViewWithTrigger);
    container.addView(button2);

    setContentView(container);
  }
}
