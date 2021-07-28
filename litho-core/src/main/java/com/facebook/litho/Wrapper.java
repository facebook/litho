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

import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.RequiredProp;
import java.util.BitSet;
import javax.annotation.Nullable;

/**
 * Utility class for wrapping an existing {@link Component}. This is useful for adding further
 * {@link CommonPropsHolder} to an already created component.
 */
public final class Wrapper extends Component {

  @Nullable @Prop Component delegate;

  private Wrapper() {
    super("Wrapper");
  }

  @Override
  protected boolean canResolve() {
    return true;
  }

  public static Builder create(ComponentContext context) {
    return create(context, 0, 0);
  }

  public static Builder create(ComponentContext context, int defStyleAttr, int defStyleRes) {
    final Builder builder = new Builder();
    builder.init(context, defStyleAttr, defStyleRes, new Wrapper());
    return builder;
  }

  @Override
  protected Component onCreateLayout(ComponentContext c) {
    return this;
  }

  @Override
  protected InternalNode resolve(ComponentContext c) {
    if (delegate == null) {
      return ComponentContext.NULL_LAYOUT;
    }

    return Layout.create(c.getLayoutStateContext(), c, delegate);
  }

  @Override
  public boolean isEquivalentTo(Component other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    Wrapper wrapper = (Wrapper) other;
    if (this.getId() == wrapper.getId()) {
      return true;
    }
    if (delegate != null ? !delegate.isEquivalentTo(wrapper.delegate) : wrapper.delegate != null) {
      return false;
    }
    return true;
  }

  @Override
  protected Component getSimpleNameDelegate() {
    return delegate;
  }

  public static class Builder extends Component.Builder<Builder> {
    private static final String[] REQUIRED_PROPS_NAMES = new String[] {"delegate"};
    private static final int REQUIRED_PROPS_COUNT = 1;

    private final BitSet mRequired = new BitSet(REQUIRED_PROPS_COUNT);
    private Wrapper mWrapper;

    private void init(
        ComponentContext context, int defStyleAttr, int defStyleRes, Wrapper wrapper) {
      super.init(context, defStyleAttr, defStyleRes, wrapper);
      mWrapper = wrapper;
    }

    @RequiredProp("delegate")
    public Builder delegate(@Nullable Component delegate) {
      mRequired.set(0);
      this.mWrapper.delegate = delegate;

      return this;
    }

    @Override
    protected void setComponent(Component component) {
      mWrapper = (Wrapper) component;
    }

    @Override
    public Builder getThis() {
      return this;
    }

    @Override
    public Wrapper build() {
      checkArgs(REQUIRED_PROPS_COUNT, mRequired, REQUIRED_PROPS_NAMES);
      return mWrapper;
    }
  }
}
