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

package com.facebook.litho;

import android.support.v4.util.Pools;
import com.facebook.litho.annotations.Prop;
import java.util.BitSet;
import javax.annotation.Nullable;

/**
 * Utility class for wrapping an existing {@link Component}. This is useful for adding further
 * {@link CommonPropsHolder} to an already created component.
 */
public final class Wrapper extends Component {

  @Nullable @Prop Component delegate;

  private static final Pools.SynchronizedPool<Builder> sBuilderPool =
      new Pools.SynchronizedPool<Builder>(2);
  private static final String MEASURED_DELEGATE_ERROR =
      "The purpose of Wrapper is to add other common props on the delegate component. The delegate has already computed a layout but it needs to be discarded because it could change after changing its common props. Use Wrapper#delegateAllowRemeasure if you are sure you need to set a delegate that has already been measured.";

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
    Builder builder = sBuilderPool.acquire();
    if (builder == null) {
      builder = new Builder();
    }
    builder.init(context, defStyleAttr, defStyleRes, new Wrapper());
    return builder;
  }

  @Override
  protected Component onCreateLayout(ComponentContext c) {
    return this;
  }

  @Override
  protected ComponentLayout resolve(ComponentContext c) {
    if (delegate == null) {
      return ComponentContext.NULL_LAYOUT;
    }

    return c.newLayoutBuilder(delegate, 0, 0);
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
    if (delegate != null ? !delegate.equals(wrapper.delegate) : wrapper.delegate != null) {
      return false;
    }
    return true;
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

    public Builder delegate(@Nullable Component delegate) {
      mRequired.set(0);
      this.mWrapper.delegate = delegate;
      // The purpose of Wrapper is to add other common props on the delegate component, which means
      // that we can't reuse the cached layout because it could change.
      if (delegate != null && delegate.mLastMeasuredLayoutThreadLocal != null) {
        ComponentsReporter.emitMessage(ComponentsReporter.LogLevel.ERROR, MEASURED_DELEGATE_ERROR);
        delegate.mLastMeasuredLayoutThreadLocal = null;
      }

      return this;
    }

    /**
     * YOU PROBABLY DON'T WANT TO USE THIS. If delegate has already been measured, delegate will be
     * remeasured with the new common props. Only use this if you are sure this is what you want:
     * measuring a component and then passing it to theWrapper as delegate to change its common
     * props after it's been measured. This will create a new copy of delegate, throw away the
     * information about previous measurement and measure again. In most cases this is probably not
     * what you need and you can refactor to avoid double measurement. See {@link
     * #delegate(Component)} for the more performant version.
     */
    @Deprecated
    public Builder delegateAllowRemeasure(@Nullable Component delegate) {
      mRequired.set(0);

      if (delegate == null) {
        return this;
      }

      if (delegate.mLastMeasuredLayoutThreadLocal == null
          || delegate.mLastMeasuredLayoutThreadLocal.get() == null) {
        return delegate(delegate);
      }

      this.mWrapper.delegate = delegate.makeShallowCopyAndClearLastMeasuredLayout();

      return this;
    }

    @Override
    public Builder getThis() {
      return this;
    }

    @Override
    public Wrapper build() {
      checkArgs(REQUIRED_PROPS_COUNT, mRequired, REQUIRED_PROPS_NAMES);
      Wrapper wrapper = mWrapper;
      release();
      return wrapper;
    }

    @Override
    protected void release() {
      super.release();
      mRequired.clear();
      mWrapper = null;
      sBuilderPool.release(this);
    }
  }
}
