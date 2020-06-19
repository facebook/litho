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
import android.animation.FloatEvaluator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ItemDecoration;
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
      StateValue<DynamicValue<Float>> expandedAmount,
      StateValue<AtomicReference<Boolean>> isExpanded,
      StateValue<DynamicValue<Float>> contractButtonRotation,
      StateValue<DynamicValue<Float>> contractButtonScale,
      StateValue<DynamicValue<Float>> contractButtonAlpha,
      StateValue<DynamicValue<Float>> expandButtonScale,
      StateValue<DynamicValue<Float>> expandButtonAlpha,
      StateValue<DynamicValue<Float>> menuButtonAlpha) {
    animatorRef.set(new AtomicReference<Animator>(null));
    expandedAmount.set(new DynamicValue<>(START_EXPANDED ? 1f : 0f));
    isExpanded.set(new AtomicReference<>(START_EXPANDED));

    contractButtonRotation.set(new DynamicValue<>(START_EXPANDED ? 0f : -90f));
    contractButtonScale.set(new DynamicValue<>(START_EXPANDED ? 1f : 0.5f));
    contractButtonAlpha.set(new DynamicValue<>(START_EXPANDED ? 1f : 0f));

    expandButtonScale.set(new DynamicValue<>(START_EXPANDED ? 0.5f : 1f));
    expandButtonAlpha.set(new DynamicValue<>(START_EXPANDED ? 0f : 1f));

    menuButtonAlpha.set(new DynamicValue<>(START_EXPANDED ? 1f : 0f));
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
                                outRect.left = c.getResourceResolver().dipsToPixels(5);
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
      @State AtomicReference<Boolean> isExpanded,
      @State DynamicValue<Float> expandedAmount,
      @State DynamicValue<Float> contractButtonRotation,
      @State DynamicValue<Float> contractButtonScale,
      @State DynamicValue<Float> contractButtonAlpha,
      @State DynamicValue<Float> expandButtonScale,
      @State DynamicValue<Float> expandButtonAlpha,
      @State DynamicValue<Float> menuButtonAlpha) {

    Animator oldAnimator = animatorRef.get();
    if (oldAnimator != null) {
      oldAnimator.cancel();
    }

    Animator newAnimator =
        createExpandCollapseAnimator(
            !Boolean.TRUE.equals(isExpanded.get()),
            expandedAmount,
            contractButtonRotation,
            contractButtonScale,
            contractButtonAlpha,
            expandButtonScale,
            expandButtonAlpha,
            menuButtonAlpha);
    isExpanded.set(!Boolean.TRUE.equals(isExpanded.get()));
    animatorRef.set(newAnimator);
    newAnimator.start();
  }

  private static final Animator createExpandCollapseAnimator(
      final boolean expand,
      final DynamicValue<Float> expandedAmount,
      final DynamicValue<Float> contractButtonRotation,
      final DynamicValue<Float> contractButtonScale,
      final DynamicValue<Float> contractButtonAlpha,
      final DynamicValue<Float> expandButtonScale,
      final DynamicValue<Float> expandButtonAlpha,
      final DynamicValue<Float> menuButtonAlpha) {
    final float from = expandedAmount.get();
    final float to = expand ? 1 : 0;
    final ValueAnimator expandCollapseAnimator = ValueAnimator.ofFloat(from, to);
    expandCollapseAnimator.setDuration(EXPAND_COLLAPSE_DURATION_MS);
    final FloatEvaluator floatEvaluator = new FloatEvaluator();
    expandCollapseAnimator.addUpdateListener(
        new ValueAnimator.AnimatorUpdateListener() {
          @Override
          public void onAnimationUpdate(ValueAnimator animation) {
            float animatedValue = (Float) animation.getAnimatedValue();
            expandedAmount.set(animatedValue);

            contractButtonRotation.set(floatEvaluator.evaluate(animatedValue, -90, 0));
            contractButtonScale.set(floatEvaluator.evaluate(animatedValue, .7, 1));
            contractButtonAlpha.set(floatEvaluator.evaluate(animatedValue, 0, 1));

            expandButtonScale.set(floatEvaluator.evaluate(animatedValue, 1, 0.7));
            expandButtonAlpha.set(floatEvaluator.evaluate(animatedValue, 1, 0));

            menuButtonAlpha.set(floatEvaluator.evaluate(animatedValue, 0, 1));
          }
        });
    return expandCollapseAnimator;
  }
}
