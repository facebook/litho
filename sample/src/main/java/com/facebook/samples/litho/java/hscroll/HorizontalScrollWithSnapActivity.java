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

package com.facebook.samples.litho.java.hscroll;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.litho.sections.widget.RecyclerCollectionEventsController;
import com.facebook.samples.litho.NavigatableDemoActivity;

public class HorizontalScrollWithSnapActivity extends NavigatableDemoActivity {
  static Integer[] colors =
      new Integer[] {
        Color.BLACK, Color.BLUE, Color.GREEN, Color.YELLOW, Color.RED, Color.MAGENTA, Color.GRAY
      };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final ComponentContext componentContext = new ComponentContext(this);
    final LinearLayout container = new LinearLayout(this);
    container.setOrientation(LinearLayout.VERTICAL);
    final RecyclerCollectionEventsController eventsController =
        new RecyclerCollectionEventsController();
    container.addView(
        LithoView.create(
            this,
            HorizontalScrollWithSnapComponent.create(componentContext)
                .colors(colors)
                .eventsController(eventsController)
                .build()));
    container.addView(
        LithoView.create(
            this,
            HorizontalScrollScrollerComponent.create(componentContext)
                .colors(colors)
                .eventsController(eventsController)
                .build()));
    setContentView(container);
  }
}
