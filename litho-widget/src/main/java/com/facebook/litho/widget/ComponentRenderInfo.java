/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.widget;

import android.support.v4.util.Pools;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;

/** {@link RenderInfo} that can render components. */
public class ComponentRenderInfo extends RenderInfo {

  private static final Pools.Pool<Builder> sBuilderPool = new Pools.SynchronizedPool<>(2);

  private final Component mComponent;

  public static Builder create() {
    Builder builder = sBuilderPool.acquire();
    if (builder == null) {
      builder = new Builder();
    }

    return builder;
  }

  private ComponentRenderInfo(Builder builder) {
    super(builder);

    if (builder.component == null) {
      throw new IllegalStateException("Component must be provided.");
    }

    mComponent = builder.component;
  }

  /** Create empty {@link ComponentRenderInfo}. */
  public static RenderInfo createEmpty() {
    return create().component(new EmptyComponent()).build();
  }

  @Override
  public Component getComponent() {
    return mComponent;
  }

  @Override
  public boolean rendersComponent() {
    return true;
  }

  @Override
  public String getName() {
    return mComponent.getSimpleName();
  }

  public static class Builder extends RenderInfo.Builder<Builder> {
    private Component component;

    /** Specify {@link Component} that will be rendered as an item of the list. */
    public Builder component(Component component) {
      this.component = component;
      return this;
    }

    public Builder component(Component.Builder builder) {
      return component(builder.build());
    }

    public ComponentRenderInfo build() {
      final ComponentRenderInfo renderInfo = new ComponentRenderInfo(this);
      release();

      return renderInfo;
    }

    @Override
    void release() {
      super.release();
      component = null;
      sBuilderPool.release(this);
    }
  }

  private static class EmptyComponent extends Component {

    protected EmptyComponent() {
      super("EmptyComponent");
    }

    @Override
    protected Component onCreateLayout(ComponentContext c) {
      return Column.create(c).build();
    }

    @Override
    public boolean isEquivalentTo(Component other) {
      return EmptyComponent.this == other
          || (other != null && EmptyComponent.this.getClass() == other.getClass());
    }
  }
}
