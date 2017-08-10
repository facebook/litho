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
import android.support.v4.util.SimpleArrayMap;
import android.view.View;
import android.view.ViewGroup;
import com.facebook.litho.viewcompat.ViewCreator;
import com.facebook.litho.viewcompat.ViewBinder;

/**
 * A component that can wrap a view using a {@link ViewBinder} class to bind the view
 * and a {@link ViewCreator} to create the mount contents.
 * This component will have a different recycle pool per {@link ViewCreator}.
 */
public class ViewCompatComponent<V extends View> extends ComponentLifecycle {

  private static final Pools.SynchronizedPool<Builder> sBuilderPool =
      new Pools.SynchronizedPool<>(2);

  private static final SimpleArrayMap<ViewCreator, ViewCompatComponent> sInstances =
      new SimpleArrayMap<>();

  private final ViewCreator mViewCreator;
  private final String mComponentName;

  public static <V extends View> ViewCompatComponent<V> get(
      ViewCreator<V> viewCreator,
      String componentName) {
    ViewCompatComponent<V> componentLifecycle;

    synchronized (sInstances) {
      componentLifecycle = sInstances.get(viewCreator);
      if (componentLifecycle == null) {
        componentLifecycle = new ViewCompatComponent<>(viewCreator, componentName);
        sInstances.put(viewCreator, componentLifecycle);
      }
    }

    return componentLifecycle;
  }

  public Builder<V> create(ComponentContext componentContext) {
    ViewCompatComponentImpl impl = new ViewCompatComponentImpl(this);
    Builder<V> builder = sBuilderPool.acquire();
    if (builder == null) {
      builder = new Builder<>();
    }
    builder.init(componentContext, impl);

    return builder;
  }

  private ViewCompatComponent(ViewCreator viewCreator, String componentName) {
    super();
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
      ComponentLayout layout,
      int widthSpec,
      int heightSpec,
      Size size,
      Component<?> component) {
    final ViewCompatComponentImpl impl = (ViewCompatComponentImpl) component;
    final ViewBinder viewBinder = impl.mViewBinder;

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
  protected void onPrepare(ComponentContext c, Component<?> component) {
    ViewCompatComponentImpl impl = ((ViewCompatComponentImpl) component);
    impl.mViewBinder.prepare();
  }

  @Override
  void bind(ComponentContext c, Object mountedContent, Component<?> component) {
    ViewCompatComponentImpl impl = ((ViewCompatComponentImpl) component);
    impl.mViewBinder.bind((View) mountedContent);
  }

  @Override
  void unbind(
      ComponentContext c, Object mountedContent, Component<?> component) {
    ViewCompatComponentImpl impl = ((ViewCompatComponentImpl) component);
    impl.mViewBinder.unbind((View) mountedContent);
  }

  @Override
  public MountType getMountType() {
    return MountType.VIEW;
  }

  private static final class ViewCompatComponentImpl<V extends View>
      extends Component<ViewCompatComponent<V>> implements Cloneable {

    private ViewBinder<V> mViewBinder;

    protected ViewCompatComponentImpl(ViewCompatComponent lifecycle) {
      super(lifecycle);
    }

    @Override
    public String getSimpleName() {
      return getLifecycle().getSimpleName();
    }
  }

  private String getSimpleName() {
    return mComponentName;
  }

  @Override
  V createMountContent(ComponentContext c) {
    return (V) mViewCreator.createView(c);
  }

  public static final class Builder<V extends View>
      extends Component.Builder<ViewCompatComponent<V>, Builder<V>> {

    private ViewCompatComponentImpl mImpl;

    private void init(ComponentContext context, ViewCompatComponentImpl impl) {
      super.init(context, 0, 0, impl);
      mImpl = impl;
    }

    public Builder<V> viewBinder(ViewBinder<V> viewBinder) {
      mImpl.mViewBinder = viewBinder;
      return this;
    }

    @Override
    public Builder<V> getThis() {
      return this;
    }

    @Override
    public Component<ViewCompatComponent<V>> build() {
      if (mImpl.mViewBinder == null) {
        throw new IllegalStateException(
            "To create a ViewCompatComponent you must provide a ViewBinder.");
      }
      ViewCompatComponentImpl impl = mImpl;
      release();
      return impl;
    }

    @Override
    protected void release() {
      super.release();
      mImpl = null;
      sBuilderPool.release(this);
    }
  }
}
