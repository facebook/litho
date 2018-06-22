/*
 * Copyright 2018-present Facebook, Inc.
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
package com.facebook.litho.processor.integration.resources;

import android.content.Context;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.ComponentLifecycle;
import com.facebook.litho.LithoView;
import com.facebook.litho.Size;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.ResType;
import java.util.BitSet;

/**
 * @prop-required content com.facebook.litho.Component
 * @prop-required ratio double
 * @see com.facebook.litho.processor.integration.resources.SimpleMountSpec
 */
public final class SimpleMount extends Component {
  @Prop(resType = ResType.NONE, optional = false)
  Component content;

  @Prop(resType = ResType.NONE, optional = false)
  double ratio;

  private SimpleMount() {
    super("SimpleMount");
  }

  @Override
  public boolean isEquivalentTo(Component other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    SimpleMount simpleMountRef = (SimpleMount) other;
    if (this.getId() == simpleMountRef.getId()) {
      return true;
    }
    if (content != null
        ? !content.isEquivalentTo(simpleMountRef.content)
        : simpleMountRef.content != null) {
      return false;
    }
    if (Double.compare(ratio, simpleMountRef.ratio) != 0) {
      return false;
    }
    return true;
  }

  @Override
  public SimpleMount makeShallowCopy() {
    SimpleMount component = (SimpleMount) super.makeShallowCopy();
    component.content = component.content != null ? component.content.makeShallowCopy() : null;
    return component;
  }

  @Override
  protected void onMeasure(
      ComponentContext c, ComponentLayout layout, int widthSpec, int heightSpec, Size size) {
    SimpleMountSpec.onMeasure(
        (ComponentContext) c,
        (ComponentLayout) layout,
        (int) widthSpec,
        (int) heightSpec,
        (Size) size,
        (double) ratio);
  }

  @Override
  protected boolean canMeasure() {
    return true;
  }

  @Override
  protected Object onCreateMountContent(Context c) {
    Object _result = (Object) SimpleMountSpec.onCreateMountContent((Context) c);
    return _result;
  }

  @Override
  protected void onMount(ComponentContext c, Object lithoView) {
    SimpleMountSpec.onMount((ComponentContext) c, (LithoView) lithoView, (Component) content);
  }

  @Override
  protected void onUnmount(ComponentContext c, Object mountedView) {
    SimpleMountSpec.onUnmount((ComponentContext) c, (LithoView) mountedView);
  }

  @Override
  public ComponentLifecycle.MountType getMountType() {
    return ComponentLifecycle.MountType.VIEW;
  }

  @Override
  protected int poolSize() {
    return 3;
  }

  @Override
  protected boolean canPreallocate() {
    return false;
  }

  public static Builder create(ComponentContext context) {
    return create(context, 0, 0);
  }

  public static Builder create(ComponentContext context, int defStyleAttr, int defStyleRes) {
    final Builder builder = new Builder();
    SimpleMount instance = new SimpleMount();
    builder.init(context, defStyleAttr, defStyleRes, instance);
    return builder;
  }

  public static class Builder extends Component.Builder<Builder> {
    private static final String[] REQUIRED_PROPS_NAMES = new String[] {"content", "ratio"};

    private static final int REQUIRED_PROPS_COUNT = 2;

    SimpleMount mSimpleMount;

    ComponentContext mContext;

    private final BitSet mRequired = new BitSet(REQUIRED_PROPS_COUNT);

    private void init(ComponentContext context, int defStyleAttr, int defStyleRes,
        SimpleMount simpleMountRef) {
      super.init(context, defStyleAttr, defStyleRes, simpleMountRef);
      mSimpleMount = simpleMountRef;
      mContext = context;
      mRequired.clear();
    }

    public Builder content(Component content) {
      this.mSimpleMount.content = content == null ? null : content.makeShallowCopy();
      mRequired.set(0);
      return this;
    }

    public Builder content(Component.Builder<?> contentBuilder) {
      this.mSimpleMount.content = contentBuilder == null ? null : contentBuilder.build();
      mRequired.set(0);
      return this;
    }

    public Builder ratio(double ratio) {
      this.mSimpleMount.ratio = ratio;
      mRequired.set(1);
      return this;
    }

    @Override
    public Builder getThis() {
      return this;
    }

    @Override
    public SimpleMount build() {
      checkArgs(REQUIRED_PROPS_COUNT, mRequired, REQUIRED_PROPS_NAMES);
      SimpleMount simpleMountRef = mSimpleMount;
      release();
      return simpleMountRef;
    }

    @Override
    protected void release() {
      super.release();
      mSimpleMount = null;
      mContext = null;
    }
  }
}
