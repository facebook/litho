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

package com.facebook.samples.litho.animations.animationcookbook;

import com.facebook.litho.sections.Children;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.annotations.GroupSectionSpec;
import com.facebook.litho.sections.annotations.OnCreateChildren;
import com.facebook.litho.sections.common.SingleComponentSection;

@GroupSectionSpec
class AnimationCookBookListSectionSpec {

  @OnCreateChildren
  static Children onCreateChildren(SectionContext c) {
    return Children.create()
        .child(SingleComponentSection.create(c).component(BounceExampleComponent.create(c)))
        .child(SingleComponentSection.create(c).component(CallbackExampleComponent.create(c)))
        .child(SingleComponentSection.create(c).component(ContinuousExampleComponent.create(c)))
        .build();
  }
}
