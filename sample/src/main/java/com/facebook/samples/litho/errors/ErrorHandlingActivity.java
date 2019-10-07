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

package com.facebook.samples.litho.errors;

import android.os.Bundle;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.samples.litho.NavigatableDemoActivity;
import java.util.Arrays;

public class ErrorHandlingActivity extends NavigatableDemoActivity {

  private static final ListRow[] DATA =
      new ListRow[] {
        new ListRow("First Title", "First Subtitle"),
        new ListRow("Second Title", "Second Subtitle"),
        new ListRow("Third Title", "Third Subtitle"),
        new ListRow("Fourth Title", "Fourth Subtitle"),
        new ListRow("Fifth Title", "Fifth Subtitle"),
        new ListRow("Sixth Title", "Sixth Subtitle"),
        new ListRow("Seventh Title", "Seventh Subtitle"),
      };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // This feature is currently experimental and not enabled by default.
    ComponentsConfiguration.enableOnErrorHandling = true;

    setContentView(
        LithoView.create(
            this,
            ErrorRootComponent.create(new ComponentContext(this))
                .dataModels(Arrays.asList(DATA))
                .build()));
  }
}
