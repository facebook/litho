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

package com.facebook.samples.litho.editor;

import android.os.Bundle;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.samples.litho.NavigatableDemoActivity;

public class SimpleEditorExampleActivity extends NavigatableDemoActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final ComponentContext componentContext = new ComponentContext(this);
    setContentView(
        LithoView.create(
            this,
            SimpleEditorComponent.create(componentContext)
                .bool(false)
                .string("Hello")
                .number(999)
                .immutableProp(
                    new SimpleEditorComponentSpec.ImmutableClass(
                        "Ana", 20, SimpleEditorComponentSpec.Emotions.HAPPY))
                .mutableProp(
                    new SimpleEditorComponentSpec.MutableClass(
                        "Beatrice", 40, SimpleEditorComponentSpec.Emotions.SAD))
                .build()));
  }
}
