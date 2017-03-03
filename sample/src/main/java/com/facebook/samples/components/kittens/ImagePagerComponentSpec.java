// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.samples.components.kittens;

import android.content.Context;

import com.facebook.components.Component;
import com.facebook.components.ComponentLayout;
import com.facebook.components.ComponentContext;
import com.facebook.components.annotations.LayoutSpec;
import com.facebook.components.annotations.OnCreateLayout;
import com.facebook.components.annotations.Prop;
import com.facebook.components.widget.Pager;
import com.facebook.components.widget.PagerBinder;

@LayoutSpec
public class ImagePagerComponentSpec {

  @OnCreateLayout
  static ComponentLayout onCreateLayout(
      ComponentContext c,
      @Prop String[] images) {
    return Pager.create(c)
        .binder(new Binder(c, images))
        .buildWithLayout();
  }

  private static class Binder extends PagerBinder {
    private final String[] mImages;

    public Binder(Context context, String[] images) {
      super(context);
      mImages = images;
      notifyDataSetChanged();
    }

    @Override
    protected int getCount() {
      return mImages.length;
    }

    @Override
    public Component<?> createComponent(ComponentContext c, int position) {
      return SingleImageComponent.create(c)
          .image(mImages[position])
          .build();
    }

    @Override
    public boolean isAsyncLayoutEnabled() {
      return true;
    }
  }
}
