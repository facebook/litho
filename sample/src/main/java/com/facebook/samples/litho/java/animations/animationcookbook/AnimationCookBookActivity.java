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

package com.facebook.samples.litho.java.animations.animationcookbook;

import android.os.Bundle;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.widget.RecyclerCollectionComponent;
import com.facebook.samples.litho.NavigatableDemoActivity;
import javax.annotation.Nullable;

public class AnimationCookBookActivity extends NavigatableDemoActivity {

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    final ComponentContext componentContext = new ComponentContext(this);
    Component component =
        RecyclerCollectionComponent.create(componentContext)
            .disablePTR(true)
            .section(
                AnimationCookBookListSection.create(new SectionContext(componentContext)).build())
            .build();
    LithoView lithoView = LithoView.create(this, component);
    setContentView(lithoView);
  }
}
