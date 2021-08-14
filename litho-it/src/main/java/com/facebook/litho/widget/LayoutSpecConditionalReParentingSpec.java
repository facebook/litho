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
    if (reParent) {
      return Column.create(c)
          .child(
              Column.create(c)
                  .clickHandler(LayoutSpecConditionalReParenting.onClickEvent3(c)) // 3
                  .child(Text.create(c).widthPx(100).heightPx(100).text("test"))
                  .child(
                      Column.create(c)
                          .clickHandler(LayoutSpecConditionalReParenting.onClickEvent1(c)) // 1
                          .child(firstComponent)
                          .child(
                              SolidColor.create(c).widthPx(100).heightPx(100).color(Color.GREEN))))
          .child(
              Column.create(c)
                  .clickHandler(LayoutSpecConditionalReParenting.onClickEvent2(c)) // 2
                  .child(Text.create(c).widthPx(100).heightPx(100).text("test2")))
          .build();
    } else {
      return Column.create(c)
          .child(
              Column.create(c)
                  .clickHandler(LayoutSpecConditionalReParenting.onClickEvent3(c)) // 3
                  .child(Text.create(c).widthPx(100).heightPx(100).text("test")))
          .child(
              Column.create(c)
                  .clickHandler(LayoutSpecConditionalReParenting.onClickEvent2(c)) // 2
                  .child(Text.create(c).widthPx(100).heightPx(100).text("test2"))
                  .child(
                      Column.create(c)
                          .clickHandler(LayoutSpecConditionalReParenting.onClickEvent1(c)) // 1
                          .child(firstComponent)
                          .child(
                              SolidColor.create(c).widthPx(100).heightPx(100).color(Color.GREEN))))
          .build();
    }
  }

  @OnEvent(ClickEvent.class)
  static void onClickEvent1(ComponentContext c, @FromEvent View view) {}

  @OnEvent(ClickEvent.class)
  static void onClickEvent2(ComponentContext c, @FromEvent View view) {}

  @OnEvent(ClickEvent.class)
  static void onClickEvent3(ComponentContext c, @FromEvent View view) {}
}
