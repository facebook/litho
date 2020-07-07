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

package com.facebook.samples.litho.animations.transitions;

import android.animation.Animator;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ItemDecoration;
import com.facebook.litho.Animations;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.DynamicValue;
import com.facebook.litho.Row;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.State;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.widget.ListRecyclerConfiguration;
import com.facebook.litho.sections.widget.RecyclerCollectionComponent;
import com.facebook.yoga.YogaEdge;
import com.facebook.yoga.YogaPositionType;
import java.util.concurrent.atomic.AtomicReference;

@LayoutSpec
public class ExpandingPickerComponentSpec {

  private static final boolean START_EXPANDED = false;
  private static final long EXPAND_COLLAPSE_DURATION_MS = 300;

  @OnCreateInitialState
  static void createInitialState(
      ComponentContext c,
      StateValue<AtomicReference<Animator>> animatorRef,
      StateValue<DynamicValue<Float>> animatedValue,
      StateValue<DynamicValue<Float>> expandedAmount,
      StateValue<AtomicReference<Boolean>> isExpandedRef,
      StateValue<DynamicValue<Float>> contractButtonRotation,
      StateValue<DynamicValue<Float>> contractButtonScale,
      StateValue<DynamicValue<Float>> contractButtonAlpha,
      StateValue<DynamicValue<Float>> expandButtonScale,
      StateValue<DynamicValue<Float>> expandButtonAlpha,
      StateValue<DynamicValue<Float>> menuButtonAlpha) {
    animatorRef.set(new AtomicReference<Animator>(null));
    animatedValue.set(new DynamicValue<>(START_EXPANDED ? 1f : 0f));
    isExpandedRef.set(new AtomicReference<>(START_EXPANDED));

    final Interpolator interpolator = new AccelerateDecelerateInterpolator();

    Animations.bind(animatedValue).with(interpolator).to(expandedAmount);

    DynamicValue<Float> contractButtonAnimationProgress =
        Animations.bind(animatedValue).inputRange(0.1f, 1f).with(interpolator).create();
    Animations.bind(contractButtonAnimationProgress).outputRange(-90, 0).to(contractButtonRotation);
    Animations.bind(contractButtonAnimationProgress).outputRange(.7f, 1).to(contractButtonScale);
    Animations.bind(contractButtonAnimationProgress).to(contractButtonAlpha);

    DynamicValue<Float> expandButtonAnimationProgress =
        Animations.bind(animatedValue).inputRange(0f, .9f).with(interpolator).create();
    Animations.bind(expandButtonAnimationProgress).outputRange(1, .5f).to(expandButtonScale);
    Animations.bind(expandButtonAnimationProgress).outputRange(1, 0).to(expandButtonAlpha);

    Animations.bind(animatedValue).inputRange(0.5f, 1f).with(interpolator).to(menuButtonAlpha);
  }

  @OnCreateLayout
  static Component onCreateLayout(
      final ComponentContext c,
      @State DynamicValue<Float> expandedAmount,
      @State DynamicValue<Float> contractButtonRotation,
      @State DynamicValue<Float> contractButtonScale,
      @State DynamicValue<Float> contractButtonAlpha,
      @State DynamicValue<Float> expandButtonScale,
      @State DynamicValue<Float> expandButtonAlpha,
      @State DynamicValue<Float> menuButtonAlpha) {
    final SectionContext s = new SectionContext(c);
    return Row.create(c)
        .paddingDip(YogaEdge.ALL, 20)
        .clickHandler(ExpandingPickerComponent.onClickEvent(c))
        .child(
            Row.create(c)
                .widthPercent(100)
                .child(
                    TileComponent.create(c)
                        .scaleX(expandButtonScale)
                        .scaleY(expandButtonScale)
                        .alpha(expandButtonAlpha)
                        .bgColor(Color.RED)
                        .text("Aa"))
                .child(
                    TileComponent.create(c)
                        .positionType(YogaPositionType.ABSOLUTE)
                        .rotation(contractButtonRotation)
                        .scaleX(contractButtonScale)
                        .scaleY(contractButtonScale)
                        .alpha(contractButtonAlpha)
                        .bgColor(Color.LTGRAY)
                        .text("<"))
                .child(
                    RecyclerCollectionComponent.create(s)
                        .flexGrow(1)
                        .itemDecoration(
                            new ItemDecoration() {
                              @Override
                              public void getItemOffsets(
                                  Rect outRect,
                                  View view,
                                  RecyclerView parent,
                                  RecyclerView.State state) {
                                outRect.left += c.getResourceResolver().dipsToPixels(5);
                              }
                            })
                        .recyclerConfiguration(
                            ListRecyclerConfiguration.create()
                                .orientation(LinearLayoutManager.HORIZONTAL)
                                .build())
                        .section(MenuItemsSection.create(s).expandedAmount(expandedAmount).build())
                        .build())
                .child(
                    TileComponent.create(c).alpha(menuButtonAlpha).bgColor(Color.LTGRAY).text("#")))
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onClickEvent(
      ComponentContext c,
      @State AtomicReference<Animator> animatorRef,
      @State AtomicReference<Boolean> isExpandedRef,
      @State DynamicValue<Float> animatedValue) {
    final boolean isExpanded = Boolean.TRUE.equals(isExpandedRef.get());
    isExpandedRef.set(!isExpanded);

    // Account for the progress of the previous animation in the duration
    final float animationProgress = animatedValue.get();
    final long animationDuration =
        (long)
            (EXPAND_COLLAPSE_DURATION_MS
                * (isExpanded ? animationProgress : 1 - animationProgress));

    Animations.animate(animatedValue)
        .to(isExpanded ? 0 : 1)
        .duration(animationDuration)
        .interpolator(new LinearInterpolator())
        .startAndCancelPrevious(animatorRef);
  }
}
