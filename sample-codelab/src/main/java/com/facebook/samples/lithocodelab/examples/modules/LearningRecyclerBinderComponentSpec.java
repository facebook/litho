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

package com.facebook.samples.lithocodelab.examples.modules;

import androidx.recyclerview.widget.OrientationHelper;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.widget.LinearLayoutInfo;
import com.facebook.litho.widget.Recycler;
import com.facebook.litho.widget.RecyclerBinder;

/**
 * Learn how to render a list of Components without having to go through Views and other Android
 * primitives.
 */
@LayoutSpec
public class LearningRecyclerBinderComponentSpec {
  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c) {
    final RecyclerBinder recyclerBinder =
        new RecyclerBinder.Builder()
            .layoutInfo(new LinearLayoutInfo(c, OrientationHelper.VERTICAL, false))
            .build(c);

    for (int i = 0; i < 32; i++) {
      recyclerBinder.insertItemAt(
          i, LearningPropsComponent.create(c).text1("Item: " + i).text2("Item: " + i).build());
    }

    return Recycler.create(c).binder(recyclerBinder).build();
  }
}
