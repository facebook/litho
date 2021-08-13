// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package com.facebook.litho.widget;

import android.graphics.Color;
import android.view.View;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.Prop;

@LayoutSpec
public class LayoutSpecConditionalReParentingSpec {

  @OnCreateLayout
  static Component onCreateLayout(
      ComponentContext c, @Prop boolean reParent, @Prop Component firstComponent) {

    Column.Builder builder =
        Column.create(c).clickHandler(LayoutSpecConditionalReParenting.onClickEvent3(c));

    if (reParent) {
      builder
          .child(
              Column.create(c) // Column added before Text(test1)
                  .clickHandler(LayoutSpecConditionalReParenting.onClickEvent1(c))
                  .child(firstComponent)
                  .child(Text.create(c).text("test2")) // Text move up 1 position
                  .child(SolidColor.create(c).widthPx(100).heightPx(100).color(Color.GREEN)))
          .child(
              Text.create(c)
                  .widthPx(100)
                  .heightPx(100)
                  .text("test1")
                  .clickHandler(LayoutSpecConditionalReParenting.onClickEvent2(c)));
    } else {
      builder
          .child(
              Text.create(c)
                  .widthPx(100)
                  .heightPx(100)
                  .text("test1")
                  .clickHandler(LayoutSpecConditionalReParenting.onClickEvent2(c)))
          .child(
              Column.create(c)
                  .clickHandler(LayoutSpecConditionalReParenting.onClickEvent1(c))
                  .child(firstComponent)
                  .child(SolidColor.create(c).widthPx(100).heightPx(100).color(Color.GREEN))
                  .child(Text.create(c).text("test2")));
    }

    return Column.create(c).child(builder).build();
  }

  @OnEvent(ClickEvent.class)
  static void onClickEvent1(ComponentContext c, @FromEvent View view) {}

  @OnEvent(ClickEvent.class)
  static void onClickEvent2(ComponentContext c, @FromEvent View view) {}

  @OnEvent(ClickEvent.class)
  static void onClickEvent3(ComponentContext c, @FromEvent View view) {}
}
