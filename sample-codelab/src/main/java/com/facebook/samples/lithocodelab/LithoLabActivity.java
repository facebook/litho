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

package com.facebook.samples.lithocodelab;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import javax.annotation.Nullable;

/**
 * <b>*************** START THE LAB HERE ***************</b>
 *
 * <p>
 *
 * <p>This is a simple "Hello, world." activity that renders using Views. The goal of the lab is to
 * build this into something that resembles LithoLabApproximateEndActivity using Litho.
 *
 * <p>
 *
 * <p>Build a header. Then leverage {@link StoryCardComponent} to render the rest of the story card.
 * Then add some statefulness and click handling to the save button in the story card.
 */
public class LithoLabActivity extends AppCompatActivity {

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.hello_world);
  }
}
