/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import static com.facebook.litho.ContextUtils.getValidActivityForContext;

import android.support.v4.util.Pools;
import android.view.View;
import android.view.ViewGroup;
import com.facebook.litho.viewcompat.ViewBinder;
import com.facebook.litho.viewcompat.ViewCreator;

/**
 * A component that can wrap a view using a {@link ViewBinder} class to bind the view
 * and a {@link ViewCreator} to create the mount contents.
 * This component will have a different recycle pool per {@link ViewCreator}.
 */
public class ViewCompatComponent<V extends View> extends Component {

  private static final Pools.SynchronizedPool<Builder> sBuilderPool =
      new Pools.SynchronizedPool<>(2);

  private final ViewCreator mViewCreator;
  private final String mComponentName;
  private ViewBinder<V> mViewBinder;

  public static <V extends View> ViewCompatComponent<V> get(
      ViewCreator<V> viewCreator,
      String componentName) {
    return new ViewCompatComponent<>(viewCreator, componentName);
  }

  public Builder<V> create(ComponentContext componentContext) {
    Builder<V> builder = sBuilderPool.acquire();
    if (builder == null) {
      builder = new Builder<>();
    }
    builder.init(componentContext, this);

    return builder;
  }

  private ViewCompatComponent(ViewCreator viewCreator, String componentName) {
    super(viewCreator.getClass());
    mViewCreator = viewCreator;
    mComponentName = "ViewCompatComponent_" + componentName;
  }

  @Override
  protected boolean canMeasure() {
    return true;
  }

  @Override
  protected void onMeasure(
      ComponentContext c,
      ActualComponentLayout layout,
      int widthSpec,
      int heightSpec,
      Size size,
      Component component) {
    final ViewCompatComponent viewCompatComponent = (ViewCompatComponent) component;
    final ViewBinder viewBinder = viewCompatComponent.mViewBinder;

    final boolean isSafeToAllocatePool = getValidActivityForContext(c) != null;

    View toMeasure =
        (View) ComponentsPools.acquireMountContent(c, getTypeId(), isSafeToAllocatePool);
    if (toMeasure == null) {
      toMeasure = mViewCreator.createView(c);
    }

    ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(size.width, size.height);
    toMeasure.setLayoutParams(layoutParams);

    viewBinder.bind(toMeasure);

    if (toMeasure.getVisibility() == View.GONE) {
      // No need to measure the view if binding it caused its visibility to become GONE.
      size.width = 0;
      size.height = 0;
    } else {
      toMeasure.measure(widthSpec, heightSpec);
      size.width = toMeasure.getMeasuredWidth();
      size.height = toMeasure.getMeasuredHeight();
    }

    viewBinder.unbind(toMeasure);

    ComponentsPools.release(c, this, toMeasure);
  }

  @Override
  protected void onPrepare(ComponentContext c, Component component) {
    mViewBinder.prepare();
  }

  @Override
  void bind(ComponentContext c, Object mountedContent, Component component) {
    mViewBinder.bind((V) mountedContent);
  }

  @Override
  void unbind(
      ComponentContext c, Object mountedContent, Component component) {
    mViewBinder.unbind((V) mountedContent);
  }

  @Override
  public MountType getMountType() {
    return MountType.VIEW;
  }

  @Override
  public String getSimpleName() {
    return mComponentName;
  }

  @Override
  public V createMountContent(ComponentContext c) {
    return (V) mViewCreator.createView(c);
  }

  public static final class Builder<V extends View> extends Component.Builder<Builder<V>> {

    private ViewCompatComponent mViewCompatComponent;

    private void init(ComponentContext context, ViewCompatComponent component) {
      super.init(context, 0, 0, component);
      mViewCompatComponent = component;
    }

    public Builder<V> viewBinder(ViewBinder<V> viewBinder) {
      mViewCompatComponent.mViewBinder = viewBinder;
      return this;
    }

    @Override
    public Builder<V> getThis() {
      return this;
    }

    @Override
    public ViewCompatComponent<V> build() {
      if (mViewCompatComponent.mViewBinder == null) {
        throw new IllegalStateException(
            "To create a ViewCompatComponent you must provide a ViewBinder.");
      }
      ViewCompatComponent viewCompatComponent = mViewCompatComponent;
      release();
      return viewCompatComponent;
    }

    @Override
    protected void release() {
      super.release();
      mViewCompatComponent = null;
      sBuilderPool.release(this);
    }
  }
}
