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
import java.util.List;

@GroupSectionSpec
class TestSingleComponentListSectionSpec {

  @OnCreateChildren
  static Children onCreateChildren(SectionContext c, @Prop List<Component> data) {
    final Children.Builder builder = Children.create();
    for (Component child : data) {
      builder.child(SingleComponentSection.create(c).component(child).build());
    }

    return builder.build();
  }
}
