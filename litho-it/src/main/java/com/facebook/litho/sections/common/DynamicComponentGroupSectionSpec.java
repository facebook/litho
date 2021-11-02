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

package com.facebook.litho.sections.common;

import com.facebook.litho.Component;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.sections.Children;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.annotations.GroupSectionSpec;
import com.facebook.litho.sections.annotations.OnCreateChildren;

/** Dynamic group section spec that accepts a component to populate as the child. */
@GroupSectionSpec
class DynamicComponentGroupSectionSpec {

  @OnCreateChildren
  static Children onCreateChildren(
      SectionContext c, @Prop Component component, @Prop int totalItems) {
    Children.Builder builder = Children.create();

    for (int i = 0; i < totalItems; i++) {
      builder.child(SingleComponentSection.create(c).component(component).key("key" + i).build());
    }

    return builder.build();
  }
}
