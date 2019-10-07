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

package com.facebook.litho.sections.treeprops;

import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnCreateTreeProp;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.widget.RecyclerCollectionComponent;

/** Variation of {@link RootComponentSpec} exercising a DiffSection. */
@LayoutSpec
public class RootDiffingComponentSpec {
  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c) {
    return Column.create(c)
        .child(
            RecyclerCollectionComponent.create(c)
                .section(TopDiffingGroupSection.create(new SectionContext(c)).build())
                .flexGrow(1f)
                .build())
        .build();
  }

  @OnCreateTreeProp
  static LogContext onCreateTestTreeProp(ComponentContext c) {
    return new LogContext("rootdiff");
  }
}
