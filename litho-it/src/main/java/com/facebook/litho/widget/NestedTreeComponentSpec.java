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

package com.facebook.litho.widget;

import androidx.annotation.Nullable;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LifecycleStep;
import com.facebook.litho.Row;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayoutWithSizeSpec;
import com.facebook.litho.annotations.OnCreateTreeProp;
import com.facebook.litho.annotations.OnShouldCreateLayoutWithNewSizeSpec;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.TreeProp;
import com.facebook.yoga.YogaDirection;
import java.util.List;

@LayoutSpec
public class NestedTreeComponentSpec {

  @OnCreateTreeProp
  public static C onCreateTreeProp(ComponentContext c) {
    return new C();
  }

  @OnCreateLayoutWithSizeSpec
  static Component onCreateLayout(
      ComponentContext c,
      int widthSpec,
      int heightSpec,
      final @Nullable @TreeProp ExtraProps props) {

    // Added to swallow the lint error.
    String s = String.format("w:%d,h:%d", widthSpec, heightSpec);

    if (props != null && props.steps != null) {
      props.steps.add(LifecycleStep.ON_CREATE_LAYOUT_WITH_SIZE_SPEC);
    }

    Row.Builder row =
        Row.create(c)
            .key("Row")
            .child(NestedTreeChildComponent.create(c).key("NestedTreeChildComponent"));

    if (props != null && props.mDirection != null) {
      row.layoutDirection(props.mDirection);
    }

    return row.build();
  }

  @OnShouldCreateLayoutWithNewSizeSpec
  static boolean shouldCreateNewLayout(
      final ComponentContext c,
      final int widthSpec,
      final int heightSpec,
      final @Prop(optional = true) LifecycleStep crashFromStep,
      final @Nullable @TreeProp ExtraProps props) {
    if (crashFromStep == LifecycleStep.ON_SHOULD_CREATE_LAYOUT_WITH_NEW_SIZE_SPEC) {
      throw new RuntimeException("onShouldCreateLayoutWithSizeSpec crash");
    }
    return props == null || props.shouldCreateNewLayout;
  }

  public static class C {}

  public static class ExtraProps {
    public boolean shouldCreateNewLayout;
    public @Nullable List<LifecycleStep> steps;
    public @Nullable YogaDirection mDirection;
  }
}
