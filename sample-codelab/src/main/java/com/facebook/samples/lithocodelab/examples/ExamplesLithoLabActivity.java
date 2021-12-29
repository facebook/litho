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

package com.facebook.samples.lithocodelab.examples;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import javax.annotation.Nullable;

/**
 * Renders {@link ExamplesActivityComponentSpec} initially, and then handles all navigation to
 * example module Components and back button presses. This was hackily thrown together since it's
 * just for demo purposes.
 */
public class ExamplesLithoLabActivity extends AppCompatActivity {

  private LabExampleController mLabExampleController;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final ComponentContext c = new ComponentContext(this);

    mLabExampleController = new LabExampleController(c);
    mLabExampleController.goToMain();
  }

  @Override
  public void onBackPressed() {
    if (mLabExampleController.isMain()) {
      super.onBackPressed();
    }

    mLabExampleController.goToMain();
  }

  public class LabExampleController {
    private final ComponentContext c;
    private final Component examplesActivityComponent;

    private boolean isMain = false;

    private LabExampleController(ComponentContext c) {
      this.c = c;
      examplesActivityComponent =
          ExamplesActivityComponent.create(c).labExampleController(this).build();
    }

    private boolean isMain() {
      return isMain;
    }

    public void goToMain() {
      setContentComponent(examplesActivityComponent);
      isMain = true;
    }

    public void setContentComponent(Component component) {
      isMain = false;
      ExamplesLithoLabActivity.this.setContentView(
          LithoView.create(ExamplesLithoLabActivity.this /* context */, component));
    }
  }
}
