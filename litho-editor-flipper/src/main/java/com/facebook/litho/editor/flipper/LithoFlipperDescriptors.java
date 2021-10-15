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

package com.facebook.litho.editor.flipper;

import com.facebook.flipper.plugins.inspector.DescriptorMapping;
import com.facebook.litho.DebugComponent;
import com.facebook.litho.LithoView;
import com.facebook.litho.sections.debug.DebugSection;
import com.facebook.litho.widget.LithoRecyclerView;

public final class LithoFlipperDescriptors {

  public static void add(DescriptorMapping descriptorMapping) {
    descriptorMapping.register(LithoView.class, new LithoViewDescriptor());
    descriptorMapping.register(DebugComponent.class, new DebugComponentDescriptor());
  }

  public static void addWithSections(DescriptorMapping descriptorMapping) {
    add(descriptorMapping);
    descriptorMapping.register(LithoRecyclerView.class, new LithoRecyclerViewDescriptor());
    descriptorMapping.register(DebugSection.class, new DebugSectionDescriptor());
  }
}
