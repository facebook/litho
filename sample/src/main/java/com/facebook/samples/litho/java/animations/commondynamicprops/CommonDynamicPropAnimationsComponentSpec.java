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

package com.facebook.samples.litho.java.animations.commondynamicprops;

import android.graphics.Color;
import android.os.Build;
import android.view.ViewOutlineProvider;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.DynamicValue;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.common.DataDiffSection;
import com.facebook.litho.sections.common.RenderEvent;
import com.facebook.litho.sections.widget.RecyclerCollectionComponent;
import com.facebook.litho.widget.ComponentRenderInfo;
import com.facebook.litho.widget.RenderInfo;
import com.facebook.yoga.YogaAlign;
import com.facebook.yoga.YogaEdge;
import java.util.Arrays;

@LayoutSpec
class CommonDynamicPropAnimationsComponentSpec {

  enum CommonDynamicPropsExample {
    ALPHA,
    SCALE,
    TRANSLATION,
    BACKGROUND_COLOR,
    ROTATION,
    ELEVATION,
    ;
  }

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c) {
    return RecyclerCollectionComponent.create(c)
        .disablePTR(true)
        .section(
            DataDiffSection.<CommonDynamicPropsExample>create(new SectionContext(c))
                .data(Arrays.asList(CommonDynamicPropsExample.values()))
                .renderEventHandler(CommonDynamicPropAnimationsComponent.onRender(c))
                .build())
        .build();
  }

  @OnEvent(RenderEvent.class)
  static RenderInfo onRender(
      ComponentContext c,
      @Prop DynamicValue<Float> dynamicAlpha,
      @Prop DynamicValue<Float> dynamicScale,
      @Prop DynamicValue<Float> dynamicTranslation,
      @Prop DynamicValue<Integer> dynamicBgColor,
      @Prop DynamicValue<Float> dynamicRotation,
      @Prop DynamicValue<Float> dynamicElevation,
      @FromEvent CommonDynamicPropsExample model) {
    Component.Builder builder = Column.create(c).widthDip(100).heightDip(100);

    boolean shouldApplyBgColor = true;

    switch (model) {
      case ALPHA:
        builder.alpha(dynamicAlpha);
        break;
      case SCALE:
        builder.scaleX(dynamicScale).scaleY(dynamicScale);
        break;
      case TRANSLATION:
        builder.translationX(dynamicTranslation).translationY(dynamicTranslation);
        break;
      case BACKGROUND_COLOR:
        builder.backgroundColor(dynamicBgColor);
        shouldApplyBgColor = false;
        break;
      case ROTATION:
        builder.rotation(dynamicRotation);
        break;
      case ELEVATION:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          // By default the shadow is cast by the View's background
          // We need to override this behaviour as the LithoView's background is unset and drawn
          // by a child component.
          builder.outlineProvider(ViewOutlineProvider.PADDED_BOUNDS);
        }
        builder.shadowElevation(dynamicElevation);
        break;
    }

    if (shouldApplyBgColor) {
      builder.backgroundColor(Color.BLUE);
    }

    return ComponentRenderInfo.create()
        .component(
            Column.create(c)
                .alignItems(YogaAlign.CENTER)
                .paddingDip(YogaEdge.ALL, 20)
                .child(builder))
        .build();
  }
}
