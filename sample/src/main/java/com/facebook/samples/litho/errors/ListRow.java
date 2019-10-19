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

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;

public class ListRow {
  public final String title;
  public final String subtitle;

  public ListRow(String title, String subtitle) {
    this.title = title;
    this.subtitle = subtitle;
  }

  public Component createComponent(ComponentContext c) {
    return ErrorBoundary.create(c).child(ListRowComponent.create(c).row(this).build()).build();
  }
}
