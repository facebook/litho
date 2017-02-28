// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.components.widget;

import android.support.v4.view.ViewPager;

import com.facebook.components.ComponentContext;
import com.facebook.components.ComponentLayout;
import com.facebook.components.Diff;
import com.facebook.components.Size;
import com.facebook.components.annotations.MountSpec;
import com.facebook.components.annotations.OnBind;
import com.facebook.components.annotations.OnBoundsDefined;
import com.facebook.components.annotations.OnCreateMountContent;
import com.facebook.components.annotations.OnMeasure;
import com.facebook.components.annotations.OnMount;
import com.facebook.components.annotations.OnUnbind;
import com.facebook.components.annotations.OnUnmount;
import com.facebook.components.annotations.Prop;
import com.facebook.components.annotations.ShouldUpdate;

/**
 * A component that takes a list of component inputs to render them as items
 * in a {@link ViewPager}.
 */
@MountSpec(canMountIncrementally = true, isPureRender = true)
class PagerSpec {

  @OnMeasure
  static void onMeasure(
      ComponentContext context,
      ComponentLayout layout,
      int widthSpec,
      int heightSpec,
      Size size) {
    // TODO: t9066805
    throw new IllegalStateException("Pager must have sizes spec set");
  }

  @OnBoundsDefined
  static void onBoundsDefined(
      ComponentContext context,
      ComponentLayout layout,
      @Prop PagerBinder binder) {
    binder.setSize(
        layout.getWidth(),
        layout.getHeight());
  }

  @OnCreateMountContent
  static ViewPager onCreateMountContent(ComponentContext c) {
    return new ViewPager(c);
  }

  @OnMount
  static void onMount(
      ComponentContext context,
      ViewPager viewPager,
      @Prop PagerBinder binder) {
    binder.mount(viewPager);
  }

  @ShouldUpdate(onMount = true)
  protected static boolean shouldUpdate(
      Diff<PagerBinder> binder) {
    return binder.getNext() != binder.getPrevious();
  }

  @OnBind
  static void onBind(
      ComponentContext context,
      ViewPager mountedView,
      @Prop PagerBinder binder) {
    binder.bind(mountedView);
  }

  @OnUnbind
  static void onUnbind(
      ComponentContext context,
      ViewPager mountedView,
      @Prop PagerBinder binder) {
    binder.unbind(mountedView);
  }

  @OnUnmount
  static void onUnmount(
      ComponentContext context,
      ViewPager mountedView,
      @Prop PagerBinder binder) {
    binder.unmount(mountedView);
  }
}
