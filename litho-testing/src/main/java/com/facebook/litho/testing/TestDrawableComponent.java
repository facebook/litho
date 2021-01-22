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
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import androidx.annotation.AttrRes;
import androidx.annotation.StyleRes;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.Size;
import com.facebook.litho.SizeSpec;

/**
 * @deprecated Use MountSpecLifecycleTester if lifecycle assertions are needed or
 *     SimpleMountSpecTester if not.
 */
public class TestDrawableComponent extends TestComponent {

  public interface TestComponentListener {
    void onPrepare();
  }

  private static final long CALLS_SHOULD_UPDATE_ON_MOUNT = 1L << 0;
  private static final long IS_PURE_RENDER = 1L << 1;
  private static final long CAN_MEASURE = 1L << 2;
  private static final long IMPLEMENTS_ACCESSIBILITY = 1L << 4;
  private static final long IS_MOUNT_SIZE_DEPENDENT = 1L << 5;

  private final long mProperties;

  private int color = Color.BLACK;
  private int measuredWidth = -1;
  private int measuredHeight = -1;
  private boolean mReturnSelfInMakeShallowCopy;
  private TestComponentListener mTestComponentListener;

  private TestDrawableComponent(long properties) {
    super("TestDrawableComponent");
    mProperties = properties;
  }

  @Override
  public boolean shouldUpdate(
      ComponentContext previousScopedContext,
      Component previous,
      ComponentContext nextScopedContext,
      Component next) {
    return !next.equals(previous);
  }

  @Override
  protected boolean callsShouldUpdateOnMount() {
    return (mProperties & CALLS_SHOULD_UPDATE_ON_MOUNT) != 0;
  }

  @Override
  protected boolean isPureRender() {
    return (mProperties & IS_PURE_RENDER) != 0;
  }

  @Override
  protected boolean implementsAccessibility() {
    return (mProperties & IMPLEMENTS_ACCESSIBILITY) != 0;
  }

  @Override
  public boolean isMountSizeDependent() {
    return (mProperties & IS_MOUNT_SIZE_DEPENDENT) != 0;
  }

  @Override
  protected Object onCreateMountContent(Context c) {
    return new ColorDrawable();
  }

  @Override
  protected void onMount(ComponentContext c, Object convertDrawable) {
    ((ColorDrawable) convertDrawable).setColor(color);

    onMountCalled();
  }

  @Override
  protected void onUnmount(ComponentContext c, Object mountedContent) {
    onUnmountCalled();
  }

  @Override
  protected boolean canMeasure() {
    return (mProperties & CAN_MEASURE) != 0;
  }

  @Override
  protected void onMeasure(
      ComponentContext c, ComponentLayout layout, int widthSpec, int heightSpec, Size size) {
    int width = SizeSpec.getSize(widthSpec);
    int height = SizeSpec.getSize(heightSpec);
    onMeasureCalled();

    size.width = measuredWidth != -1 ? SizeSpec.resolveSize(widthSpec, measuredWidth) : width;
    size.height = measuredHeight != -1 ? SizeSpec.resolveSize(heightSpec, measuredHeight) : height;
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
    return MountType.DRAWABLE;
  }

  @Override
  public Component makeShallowCopy() {
    if (mReturnSelfInMakeShallowCopy) {
      return this;
    }

    return super.makeShallowCopy();
  }

  @Override
  protected void onPrepare(ComponentContext c) {
    if (mTestComponentListener != null) {
      mTestComponentListener.onPrepare();
    }

    super.onPrepare(c);
  }

  public void setTestComponentListener(TestComponentListener listener) {
    mTestComponentListener = listener;
  }

  public static Builder create(ComponentContext context) {
    return create(context, 0, 0, true, true, true, false, false);
  }

  public static Builder create(
      ComponentContext context, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
    return create(context, defStyleAttr, defStyleRes, true, true, true, false);
  }

  public static Builder create(
      ComponentContext context,
      boolean callsShouldUpdateOnMount,
      boolean isPureRender,
      boolean canMeasure,
      boolean implementsAccessibility) {
    return create(
        context, 0, 0, callsShouldUpdateOnMount, isPureRender, canMeasure, implementsAccessibility);
  }

  public static Builder create(
      ComponentContext context,
      @AttrRes int defStyleAttr,
      @StyleRes int defStyleRes,
      boolean callsShouldUpdateOnMount,
      boolean isPureRender,
      boolean canMeasure,
      boolean implementsAccessibility) {
    return create(
        context,
        defStyleAttr,
        defStyleRes,
        callsShouldUpdateOnMount,
        isPureRender,
        canMeasure,
        implementsAccessibility,
        false);
  }

  public static Builder create(
      ComponentContext context,
      @AttrRes int defStyleAttr,
      @StyleRes int defStyleRes,
      boolean callsShouldUpdateOnMount,
      boolean isPureRender,
      boolean canMeasure,
      boolean implementsAccessibility,
      boolean isMountSizeDependent) {

    long properties = 0;

    if (callsShouldUpdateOnMount) {
      properties |= CALLS_SHOULD_UPDATE_ON_MOUNT;
    }
    if (isPureRender) {
      properties |= IS_PURE_RENDER;
    }
    if (canMeasure) {
      properties |= CAN_MEASURE;
    }
    if (implementsAccessibility) {
      properties |= IMPLEMENTS_ACCESSIBILITY;
    }
    if (isMountSizeDependent) {
      properties |= IS_MOUNT_SIZE_DEPENDENT;
    }

    return newBuilder(context, defStyleAttr, defStyleRes, new TestDrawableComponent(properties));
  }

  private static Builder newBuilder(
      ComponentContext context,
      @AttrRes int defStyleAttr,
      @StyleRes int defStyleRes,
      TestDrawableComponent state) {
    final Builder builder = new Builder();
    builder.init(context, defStyleAttr, defStyleRes, state);
    return builder;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    TestDrawableComponent that = (TestDrawableComponent) o;

    if (color != that.color) return false;
    if (measuredWidth != that.measuredWidth) return false;
    if (measuredHeight != that.measuredHeight) return false;
    return mReturnSelfInMakeShallowCopy == that.mReturnSelfInMakeShallowCopy;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + color;
    result = 31 * result + measuredWidth;
    result = 31 * result + measuredHeight;
    result = 31 * result + (mReturnSelfInMakeShallowCopy ? 1 : 0);
    return result;
  }

  public static class Builder extends com.facebook.litho.Component.Builder<Builder> {
    TestDrawableComponent mComponent;

    private void init(
        ComponentContext context,
        @AttrRes int defStyleAttr,
        @StyleRes int defStyleRes,
        TestDrawableComponent component) {
      super.init(context, defStyleAttr, defStyleRes, component);
      mComponent = component;
    }

    @Override
    protected void setComponent(Component component) {
      mComponent = (TestDrawableComponent) component;
    }

    public Builder measuredWidth(int width) {
      mComponent.measuredWidth = width;
      return this;
    }

    public Builder measuredHeight(int height) {
      mComponent.measuredHeight = height;
      return this;
    }

    public Builder color(int color) {
      mComponent.color = color;
      return this;
    }

    public Builder unique() {
      mComponent.mIsUnique = true;
      return this;
    }

    public Builder returnSelfInMakeShallowCopy() {
      mComponent.mReturnSelfInMakeShallowCopy = true;
      return this;
    }

    @Override
    public TestDrawableComponent build() {
      return mComponent;
    }

    @Override
    public Builder getThis() {
      return this;
    }
  }

  /**
   * A listener that will block in prepare until allowPrepareToComplete is called from another
   * thread.
   */
  public static class BlockInPrepareComponentListener implements TestComponentListener {

    private final ThreadLocal<Boolean> mDoNotBlockOnThisThread = new ThreadLocal<>();
    private final TimeOutSemaphore mOnAsyncPrepareStartSemaphore = new TimeOutSemaphore(0);
    private final TimeOutSemaphore mAllowPrepareToCompleteSemaphore = new TimeOutSemaphore(0);

    @Override
    public void onPrepare() {
      if (mDoNotBlockOnThisThread.get() != null && mDoNotBlockOnThisThread.get()) {
        return;
      }

      mOnAsyncPrepareStartSemaphore.release();
      mAllowPrepareToCompleteSemaphore.acquire();

      mOnAsyncPrepareStartSemaphore.drainPermits();
      mAllowPrepareToCompleteSemaphore.drainPermits();
    }

    /** Blocks from another thread until prepare() is called. */
    public void awaitPrepareStart() {
      mOnAsyncPrepareStartSemaphore.acquire();
    }

    /** Allows prepare to complete. */
    public void allowPrepareToComplete() {
      mAllowPrepareToCompleteSemaphore.release();
    }

    public void setDoNotBlockOnThisThread() {
      mDoNotBlockOnThisThread.set(true);
    }
  }
}
