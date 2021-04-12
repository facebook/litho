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

package com.facebook.litho.testing;

import androidx.annotation.AttrRes;
import androidx.annotation.StyleRes;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Wrapper;

/**
 * @deprecated Component should not be directly subclassed, write a layout spec or mount spec
 *     instead
 */
@Deprecated
public class TestLayoutComponent extends TestComponent {

  private final boolean mIsPureRender;
  private final boolean mHasMountSpecChild;
  private final boolean mIsDelegate;

  private TestLayoutComponent(boolean isPureRender, boolean hasMountSpecChild, boolean isDelegate) {
    super();

    mIsPureRender = isPureRender;
    mHasMountSpecChild = hasMountSpecChild;
    mIsDelegate = isDelegate;
  }

  @Override
  protected boolean isPureRender() {
    return mIsPureRender;
  }

  @Override
  protected Component onCreateLayout(ComponentContext c) {
    super.onCreateLayout(c);
    final Component mountSpecComponent = TestDrawableComponent.create(c, true, true, false).build();

    if (mIsDelegate) {
      return Wrapper.create(c).delegate(mountSpecComponent).build();
    }

    Component.ContainerBuilder<?> containerBuilder = Column.create(c);

    if (mHasMountSpecChild) {
      containerBuilder.child(mountSpecComponent);
    }

    return containerBuilder.build();
  }

  @Override
  public MountType getMountType() {
    return MountType.NONE;
  }

  public static Builder create(
      ComponentContext context, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
    return create(context, defStyleAttr, defStyleRes, true, false, false);
  }

  public static Builder create(
      ComponentContext context,
      @AttrRes int defStyleAttr,
      @StyleRes int defStyleRes,
      boolean isPureRender,
      boolean hasMountSpecChild,
      boolean isDelegate) {
    return newBuilder(
        context,
        defStyleAttr,
        defStyleRes,
        new TestLayoutComponent(isPureRender, hasMountSpecChild, isDelegate));
  }

  public static Builder create(ComponentContext context) {
    return create(context, 0, 0, true, false, false);
  }

  public static Builder create(ComponentContext context, boolean isPureRender) {
    return create(context, 0, 0, isPureRender, false, false);
  }

  private static Builder newBuilder(
      ComponentContext context,
      @AttrRes int defStyleAttr,
      @StyleRes int defStyleRes,
      TestLayoutComponent state) {
    final Builder builder = new Builder();
    builder.init(context, defStyleAttr, defStyleRes, state);
    return builder;
  }

  public static class Builder extends com.facebook.litho.Component.Builder<Builder> {
    TestLayoutComponent mState;

    private void init(
        ComponentContext context,
        @AttrRes int defStyleAttr,
        @StyleRes int defStyleRes,
        TestLayoutComponent state) {
      super.init(context, defStyleAttr, defStyleRes, state);
      mState = state;
    }

    @Override
    protected void setComponent(Component component) {
      mState = (TestLayoutComponent) component;
    }

    @Override
    public Builder getThis() {
      return this;
    }

    @Override
    public TestComponent build() {
      return mState;
    }
  }
}
