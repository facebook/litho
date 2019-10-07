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

package com.facebook.litho.testing;

import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Row;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.Prop;
import java.util.List;

@LayoutSpec
public class TestWrappedComponentPropSpec {

  public static class ComponentWrapper {
    Component mComponent;

    public ComponentWrapper(Component component) {
      mComponent = component;
    }
  }

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c,
      @Prop(optional = true) ComponentWrapper wrapper,
      @Prop(optional = true) List<Component> componentList) {
    if (wrapper != null) {
      return Row.create(c).child(wrapper.mComponent).build();
    }

    final Row.Builder builder = Row.create(c);

    for (Component component : componentList) {
      builder.child(component);
    }

    return builder.build();
  }
}
