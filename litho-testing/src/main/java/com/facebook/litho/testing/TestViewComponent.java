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

import android.content.Context;
import android.view.View;
import androidx.annotation.AttrRes;
import androidx.annotation.StyleRes;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.Size;
import com.facebook.litho.SizeSpec;
import javax.annotation.Nullable;

/**
 * @deprecated Component should not be directly subclassed, write a layout spec or mount spec
 *     instead
 */
@Deprecated
public class TestViewComponent extends TestComponent {

  private final boolean mIsPureRender;
  private final boolean mCanMeasure;
  private final boolean mHasChildLithoViews;
  @Nullable private View mTestView;

  private TestViewComponent(boolean isPureRender, boolean canMeasure, boolean hasChildLithoViews) {
    super("TestViewComponent");

    mIsPureRender = isPureRender;
    mCanMeasure = canMeasure;
    mHasChildLithoViews = hasChildLithoViews;
  }

  @Override
  protected boolean isPureRender() {
    return mIsPureRender;
  }

  @Override
  protected boolean hasChildLithoViews() {
    return mHasChildLithoViews;
  }

  @Override
  protected Object onCreateMountContent(Context c) {
    return mTestView != null ? mTestView : new View(c);
  }

  @Override
  protected void onMount(ComponentContext c, Object convertView) {
    onMountCalled();
  }

  @Override
  protected void onUnmount(ComponentContext c, Object mountedContent) {
    onUnmountCalled();
  }

  @Override
  protected boolean canMeasure() {
    return mCanMeasure;
  }

  @Override
  protected void onMeasure(
      ComponentContext c, ComponentLayout layout, int widthSpec, int heightSpec, Size size) {
    int width = SizeSpec.getSize(widthSpec);
    int height = SizeSpec.getSize(heightSpec);

    size.height = height;
    size.width = width;

    onMeasureCalled();
  }

  @Override
  protected void onBoundsDefined(ComponentContext c, ComponentLayout layout) {
    onDefineBoundsCalled();
  }

  @Override
  protected void onBind(ComponentContext c, Object mountedContent) {
    onBindCalled();
  }

  @Override
  protected void onUnbind(ComponentContext c, Object mountedContent) {
    onUnbindCalled();
  }

  @Override
  public MountType getMountType() {
    return MountType.VIEW;
  }

  public static Builder create(
      ComponentContext context, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
    return create(context, defStyleAttr, defStyleRes, true, true, true);
  }

  public static Builder create(
      ComponentContext context,
      @AttrRes int defStyleAttr,
      @StyleRes int defStyleRes,
      boolean isPureRender,
      boolean canMeasure,
      boolean canMountIncrementally) {
    return newBuilder(
        context,
        defStyleAttr,
        defStyleRes,
        new TestViewComponent(isPureRender, canMeasure, canMountIncrementally));
  }

  public static Builder create(ComponentContext context) {
    return create(context, 0, 0, true, true, true);
  }

  public static Builder create(
      ComponentContext context,
      boolean isPureRender,
      boolean canMeasure,
      boolean canMountIncrementally) {
    return create(context, 0, 0, isPureRender, canMeasure, canMountIncrementally);
  }

  private static Builder newBuilder(
      ComponentContext context,
      @AttrRes int defStyleAttr,
      @StyleRes int defStyleRes,
      TestViewComponent state) {
    final Builder builder = new Builder();
    builder.init(context, defStyleAttr, defStyleRes, state);
    return builder;
  }

  public static class Builder extends com.facebook.litho.Component.Builder<Builder> {
    TestViewComponent mState;

    private void init(
        ComponentContext context,
        @AttrRes int defStyleAttr,
        @StyleRes int defStyleRes,
        TestViewComponent state) {
      super.init(context, defStyleAttr, defStyleRes, state);
      mState = state;
    }

    @Override
    protected void setComponent(Component component) {
      mState = (TestViewComponent) component;
    }

    public Builder testView(View testView) {
      mState.mTestView = testView;
      return this;
    }

    @Override
    public TestViewComponent build() {
      return mState;
    }

    @Override
    public Builder getThis() {
      return this;
    }
  }
}
