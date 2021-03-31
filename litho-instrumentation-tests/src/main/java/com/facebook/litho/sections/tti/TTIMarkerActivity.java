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

package com.facebook.litho.sections.tti;

import android.app.Activity;
import android.os.Bundle;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.widget.RecyclerCollectionComponent;
import java.util.ArrayList;
import java.util.List;

public class TTIMarkerActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final ComponentContext c = new ComponentContext(this);
    final SectionContext sc = new SectionContext(this);

    final List<String> data = new ArrayList<>();
    data.add("Hello World");

    final Component component =
        RecyclerCollectionComponent.create(c)
            .section(TTIMarkerSection.create(sc).data(data))
            .build();
    final LithoView lithoView = LithoView.create(c, component);
    setContentView(lithoView);
  }
}
