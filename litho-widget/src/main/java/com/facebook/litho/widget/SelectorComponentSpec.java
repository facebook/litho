/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.widget;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.Wrapper;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import java.util.List;

/**
 * Selects the first Component that will render by calling {@link
 * Component#willRender(ComponentContext, ComponentLayout)} on the Components provided.
 *
 * <p>This is useful when a single Component is to be rendered amongst a large number of candidate
 * Components or when multiple Components can potentially render some content using the same props
 * and the first one capable of rendering the content needs to be used.
 */
@LayoutSpec
class SelectorComponentSpec {

  @OnCreateLayout
  static ComponentLayout onCreateLayout(
      final ComponentContext c,
      final @Prop(varArg = "component", optional = true) List<Component> components) {
    if (components == null) {
      return null;
    }

    for (int i = 0; i < components.size(); i++) {
      final ComponentLayout layout = Wrapper.create(c).delegate(components.get(i)).build();
      if (Component.willRender(c, layout)) {
        return layout;
      }
    }

    return null;
  }
}
