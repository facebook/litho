// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.samples.litho;

import android.content.Intent;
import android.view.View;

import com.facebook.components.ClickEvent;
import com.facebook.components.ComponentContext;
import com.facebook.components.ComponentLayout;
import com.facebook.components.Container;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.widget.Text;

import static com.facebook.yoga.YogaEdge.ALL;

@LayoutSpec
public class DemoListItemComponentSpec {

  @OnCreateLayout
  static ComponentLayout onCreateLayout(
      ComponentContext c,
      @Prop final String name) {
    return Container.create(c)
        .paddingDip(ALL, 16)
        .child(
            Text.create(c)
                .text(name)
                .textSizeSp(18)
                .build())
        .clickHandler(DemoListItemComponent.onClick(c))
        .build();
  }

  @OnEvent(ClickEvent.class)
  static void onClick(
      ComponentContext c,
      @FromEvent View view,
      @Prop final String name) {
    final Intent intent = new Intent(c, DemoActivity.class);
    intent.putExtra("demoName", name);
    c.startActivity(intent);
  }
}
