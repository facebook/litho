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

import android.content.Context;
import android.graphics.Color;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.ComponentTree;
import com.facebook.litho.LithoView;
import com.facebook.litho.Output;
import com.facebook.litho.Row;
import com.facebook.litho.Size;
import com.facebook.litho.SizeSpec;
import com.facebook.litho.StateCaller;
import com.facebook.litho.Transition;
import com.facebook.litho.animation.AnimatedProperties;
import com.facebook.litho.annotations.FromPrepare;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnMeasure;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.OnPrepare;
import com.facebook.litho.annotations.Prop;

@MountSpec
public class TestAnimationMountSpec {

  @OnPrepare
  static void onPrepare(
      ComponentContext c, @Prop StateCaller stateCaller, Output<StateCaller> state) {
    state.set(stateCaller);
  }

  @OnCreateMountContent
  static LithoView onCreateMountContent(Context context) {
    return new LithoView(context);
  }

  @OnMount
  static void onMount(ComponentContext c, LithoView view, @FromPrepare StateCaller state) {
    final ComponentContext viewComponentContext = new ComponentContext(view.getContext());
    final String transitionKey = "TRANSITION_KEY";
    final TestAnimationsComponent component =
        TestAnimationsComponent.create(viewComponentContext)
            .stateCaller(state)
            .transition(
                Transition.create(transitionKey)
                    .animator(Transition.timing(144))
                    .animate(AnimatedProperties.ALPHA)
                    .disappearTo(0))
            .testComponent(
                new TestAnimationsComponentSpec
                    .TestComponent() { // This could be a lambda but it fails ci.
                  @Override
                  public Component getComponent(ComponentContext componentContext, boolean state) {
                    return Column.create(componentContext)
                        .child(
                            Row.create(componentContext)
                                .heightDip(50)
                                .widthDip(50)
                                .backgroundColor(Color.YELLOW))
                        .child(
                            !state
                                ? Row.create(componentContext)
                                    .heightDip(50)
                                    .widthDip(50)
                                    .backgroundColor(Color.RED)
                                    .transitionKey(transitionKey)
                                    .key(transitionKey)
                                    .viewTag("TestAnimationMount")
                                : null)
                        .build();
                  }
                })
            .build();
    view.setComponentTree(ComponentTree.create(viewComponentContext, component).build());
  }

  @OnMeasure
  static void onMeasure(
      ComponentContext c, ComponentLayout layout, int widthSpec, int heightSpec, Size size) {
    // If width is undefined, set default size.
    if (SizeSpec.getMode(widthSpec) == SizeSpec.UNSPECIFIED) {
      size.width = 50;
    } else {
      size.width = SizeSpec.getSize(widthSpec);
    }

    // If height is undefined, use 1.5 aspect ratio.
    if (SizeSpec.getMode(heightSpec) == SizeSpec.UNSPECIFIED) {
      size.height = 50;
    } else {
      size.height = SizeSpec.getSize(heightSpec);
    }
  }
}
