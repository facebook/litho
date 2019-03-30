/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.widget;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Selects the first Component that will render by calling {@link
 * Component#willRender(ComponentContext, Component)} on the Components provided.
 *
 * <p>The Component creation is made lazy by taking a {@link ComponentCreator} instead of a {@link
 * Component}.
 *
 * <p>This is useful when a single Component is to be rendered amongst a large number of candidate
 * Components or when multiple Components can potentially render some content using the same props
 * and the first one capable of rendering the content needs to be used.
 */
@LayoutSpec
public class LazySelectorComponentSpec {

  @OnCreateLayout
  static @Nullable Component onCreateLayout(
      final ComponentContext c,
      final @Prop(varArg = "component", optional = true) List<ComponentCreator> components) {
    if (components == null) {
      return null;
    }

    for (int i = 0; i < components.size(); i++) {
      final Component component = components.get(i).create();
      if (Component.willRender(c, component)) {
        return component;
      }
    }

    return null;
  }
}
