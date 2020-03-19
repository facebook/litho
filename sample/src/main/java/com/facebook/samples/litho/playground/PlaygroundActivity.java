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

package com.facebook.samples.litho.playground;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import com.facebook.litho.Column;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.litho.widget.Text;
import com.facebook.samples.litho.NavigatableDemoActivity;

import static com.facebook.yoga.YogaEdge.ALL;

public class PlaygroundActivity extends NavigatableDemoActivity implements View.OnClickListener {

  LithoView lithoView;
  Text helloWorld;
  Text lithoTutorial;
  PlaygroundComponent playgroundComponent;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final ComponentContext c = new ComponentContext(this);
    lithoView = new LithoView(c);

    playgroundComponent = PlaygroundComponent.create(c).clickListener(this).build();
    helloWorld = Text.create(c)
        .text("Hello world")
        .textSizeSp(40).build();
    lithoTutorial = Text.create(c)
        .text("Litho tutorial")
        .textSizeSp(20).build();

    Column column = Column.create(c)
        .paddingDip(ALL, 16)
        .backgroundColor(Color.WHITE)
        .child(helloWorld)
        .child(playgroundComponent)
        .build();

    lithoView.setComponent(column);
    setContentView(lithoView);
  }

  @Override
  public void onClick(View v) {
    Column column = Column.create(lithoView.getComponentContext())
        .paddingDip(ALL, 16)
        .backgroundColor(Color.WHITE)
        .child(helloWorld)
        .child(lithoTutorial)
        .child(playgroundComponent)
        .build();

    lithoView.setComponent(column);
  }
}
