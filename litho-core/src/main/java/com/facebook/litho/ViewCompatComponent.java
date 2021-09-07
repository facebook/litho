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

package com.facebook.litho;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import com.facebook.litho.viewcompat.ViewBinder;
import com.facebook.litho.viewcompat.ViewCreator;

/**
 * A component that can wrap a view using a {@link ViewBinder} class to bind the view and a {@link
 * ViewCreator} to create the mount contents. This component will have a different recycle pool per
 * {@link ViewCreator} instance.
 *
 * @deprecated ViewCompatComponent is not efficient as it will do measurement of views twice.
 *     Recommended way now is to use either ViewRenderInfo (which utilizes same interfaces as this
 *     class: ViewCreator and ViewBinder) if the view is used with sections API or create a custom
 *     MountSpec.
 */
@Deprecated
public class ViewCompatComponent<V extends View> extends Component {

  private static final int UNSPECIFIED_POOL_SIZE = -1;

  private final ViewCreator mViewCreator;
  private final String mComponentName;
  private ViewBinder<V> mViewBinder;

  private int mPoolSize = UNSPECIFIED_POOL_SIZE;

  public static <V extends View> ViewCompatComponent<V> get(
      ViewCreator<V> viewCreator, String componentName) {
    return new ViewCompatComponent<>(viewCreator, componentName);
  }

  public Builder<V> create(ComponentContext componentContext) {
    return new Builder<>(componentContext, this);
  }

  private ViewCompatComponent(ViewCreator viewCreator, String componentName) {
    super(System.identityHashCode(viewCreator));
    mViewCreator = viewCreator;
    mComponentName = componentName;
  }

  @Override
  public boolean isEquivalentTo(Component other) {
    return this == other;
  }

  @Override
  protected boolean canMeasure() {
    return true;
  }

  @Override
  protected void onMeasure(
      ComponentContext c, ComponentLayout layout, int widthSpec, int heightSpec, Size size) {
    final V toMeasure =
        (V) ComponentsPools.acquireMountContent(c.getAndroidContext(), this, c.getRecyclingMode());
    final ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(size.width, size.height);

    toMeasure.setLayoutParams(layoutParams);
    mViewBinder.bind(toMeasure);

    if (toMeasure.getVisibility() == View.GONE) {
      // No need to measure the view if binding it caused its visibility to become GONE.
      size.width = 0;
      size.height = 0;
    } else {
      toMeasure.measure(widthSpec, heightSpec);
      size.width = toMeasure.getMeasuredWidth();
      size.height = toMeasure.getMeasuredHeight();
    }

    mViewBinder.unbind(toMeasure);

    ComponentsPools.release(c.getAndroidContext(), this, toMeasure, c.getRecyclingMode());
  }

  @Override
  protected void onPrepare(ComponentContext c) {
    mViewBinder.prepare();
  }

  @Override
  protected void onBind(ComponentContext c, Object mountedContent) {
    mViewBinder.bind((V) mountedContent);
  }

  @Override
  protected void onUnbind(ComponentContext c, Object mountedContent) {
    mViewBinder.unbind((V) mountedContent);
  }

  @Override
  public MountType getMountType() {
    return MountType.VIEW;
  }

  @Override
  public V onCreateMountContent(Context c) {
    return (V) mViewCreator.createView(c, null);
  }

  public static final class Builder<V extends View> extends Component.Builder<Builder<V>> {

    private ViewCompatComponent mViewCompatComponent;

    private Builder(ComponentContext context, ViewCompatComponent component) {
      super(context, 0, 0, component);
      mViewCompatComponent = component;
    }

    public Builder<V> viewBinder(ViewBinder<V> viewBinder) {
      mViewCompatComponent.mViewBinder = viewBinder;
      return this;
    }

    public Builder<V> contentPoolSize(int size) {
      mViewCompatComponent.mPoolSize = size;
      return this;
    }

    @Override
    protected void setComponent(Component component) {
      mViewCompatComponent = (ViewCompatComponent) component;
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
      return mViewCompatComponent;
    }
  }

  @Override
  protected int poolSize() {
    return mPoolSize == UNSPECIFIED_POOL_SIZE ? super.poolSize() : mPoolSize;
  }

  @Override
  public String getSimpleName() {
    return "ViewCompatComponent_" + mComponentName;
  }
}
