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

package com.facebook.samples.litho.lithography;

import com.facebook.litho.ComponentContext;
import com.facebook.litho.widget.ComponentRenderInfo;
import com.facebook.litho.widget.RenderInfo;

public class Artist implements Datum {

  public final String name;
  public final String biography;
  public final String[] images;
  public final int year;

  public Artist(String name, String biography, int year, String... images) {
    this.name = name;
    this.biography = biography;
    this.year = year;
    this.images = images;
  }

  @Override
  public RenderInfo createComponent(ComponentContext c) {
    return ComponentRenderInfo.create()
        .component(FeedItemCard.create(c).artist(this).build())
        .build();
  }
}
