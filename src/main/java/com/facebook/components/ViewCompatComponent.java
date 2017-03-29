/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import java.util.HashMap;
import java.util.Map;

import android.support.v4.util.Pools;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.litho.viewcompatcreator.ViewCompatCreator;

/**
 * A component that can wrap a view using a {@link ViewBinder} class to bind the view
 * and a {@link ViewCompatCreator} to create the mount contents.
 * This component will have a different recycle pool per {@link ViewCompatCreator}.
 */
public class ViewCompatComponent<V extends View> extends ComponentLifecycle {

  /**
   * Binds data to a view.
   * @param <V> the type of View.
   */
  public interface ViewBinder<V extends View> {

    /**
     * Prepares the binder to be bound to a view.
     *
     * Use this method to perform calculations ahead of time and save them.
     */
    void prepare();

    /**
     * Binds data to the given view so it can be rendered on screen. This will always be called
     * after prepare so that you can use stored output from prepare here if needed.
     *
     * @param view the view to bind.
     */
    void bind(V view);

    /**
     * Cleans up a view that goes off screen after it has already been bound.
     *
     * @param view the view to unbind.
     */
    void unbind(V view);
  }

  private static final Pools.SynchronizedPool<Builder> sBuilderPool =
      new Pools.SynchronizedPool<>(2);

  private static final Map<ViewCompatCreator, ViewCompatComponent> sInstances = new HashMap<>();

  private final ViewCompatCreator mViewCompatCreator;
  private final String mComponentName;

  public static <V extends View> ViewCompatComponent<V> get(
      ViewCompatCreator<V> viewCreator,
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

  private ViewCompatComponent(ViewCompatCreator viewCreator, String componentName) {
    super();
    mViewCompatCreator = viewCreator;
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

    View toMeasure = (View) ComponentsPools.acquireMountContent(c, getId());
    if (toMeasure == null) {
      toMeasure = mViewCompatCreator.createView(c);
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
    return (V) mViewCompatCreator.createView(c);
  }

  public static final class Builder<V extends View>
      extends Component.Builder<ViewCompatComponent<V>> {

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
    public Builder<V> key(String key) {
      super.setKey(key);
      return this;
    }

    @Override
