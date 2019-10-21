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

import static android.os.Build.VERSION.SDK_INT;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build.VERSION_CODES;
import com.facebook.litho.drawable.ComparableDrawable;
import com.facebook.litho.drawable.DefaultComparableDrawable;

class DrawableComponent<T extends Drawable> extends Component {

  private final  ComparableDrawable mDrawable;
  int mDrawableWidth;
  int mDrawableHeight;
  private boolean isRippleBackground;

  private DrawableComponent(ComparableDrawable drawable) {
    super("DrawableComponent");
    mDrawable = drawable;
    isRippleBackground = false;
  }

  @Override
  protected void onBoundsDefined(ComponentContext c, ComponentLayout layout) {
    setDrawableWidth(layout.getWidth());
    setDrawableHeight(layout.getHeight());
  }

  @Override
  protected Object onCreateMountContent(Context c) {
    return new MatrixDrawable();
  }

  @Override
  protected void onMount(ComponentContext context, Object content) {
    MatrixDrawable drawable = (MatrixDrawable) content;

    drawable.mount(getDrawable());
  }

  @Override
  protected void onBind(ComponentContext c, Object mountedContent) {
    final MatrixDrawable mountedDrawable = (MatrixDrawable) mountedContent;

    mountedDrawable.bind(getDrawableWidth(), getDrawableHeight());
  }

  @Override
  protected void onUnmount(ComponentContext context, Object mountedContent) {
    final MatrixDrawable<T> matrixDrawable = (MatrixDrawable<T>) mountedContent;
    matrixDrawable.unmount();
  }

  @Override
  protected boolean isPureRender() {
    return true;
  }

  @Override
  public MountType getMountType() {
    return MountType.DRAWABLE;
  }

  public static DrawableComponent create(ComparableDrawable drawable) {
    return new DrawableComponent<>(drawable);
  }

  @Override
  protected boolean shouldUpdate(Component previous, Component next) {
    final ComparableDrawable previousDrawable = ((DrawableComponent) previous).getDrawable();
    final ComparableDrawable nextDrawable = ((DrawableComponent) next).getDrawable();

    return !previousDrawable.isEquivalentTo(nextDrawable);
  }

  public ComparableDrawable getDrawable() {
    return mDrawable;
  }

  @Override
  public boolean isEquivalentTo(Component o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    DrawableComponent drawableComponent = (DrawableComponent) o;

    return mDrawable.equals(drawableComponent.mDrawable);
  }

  private void setDrawableWidth(int drawableWidth) {
    mDrawableWidth = drawableWidth;
  }


  /**
   * Indicates if this component is a ripple drawable background for a ComponentHost.
   *
   * <p>This will help to check if the background is a ripple drawable. We have to set
   * RippleDrawables to the view's background so ripple can be projected outside the view bounds.
   */
  public void mayBeSetIsRippleBackground() {
    if (SDK_INT < VERSION_CODES.LOLLIPOP) {
      return;
    }

    if (!(mDrawable instanceof DefaultComparableDrawable)) {
      return;
    }
    // We need to unwrap the RippleDrawable from DefaultComparableDrawable since
    // DefaultComparableDrawable does not override all the methods necessary for Ripple drawable to
    // be projected outside the bounds.
    // We need to get the wrapped drawable to identify if it's a ripple drawable or not.
    Drawable unwrappedDrawable = ((DefaultComparableDrawable) mDrawable).getWrappedDrawable();
    // We will set the view's background only if it's RippleDrawable
    if (unwrappedDrawable instanceof RippleDrawable) {
      isRippleBackground = true;
    }
  }

  /**
   * Returns true if this component contains a RippleDrawable background for a ComponentHost or else
   * returns false.
   *
   * <p>This will help to check if the background is a ripple drawable. We have to set
   * RippleDrawables to the view's background so ripple can be projected outside the view bounds.
   */
  public boolean isRippleBackground() {
    return isRippleBackground;
  }

  private int getDrawableWidth() {
    return mDrawableWidth;
  }

  private void setDrawableHeight(int drawableHeight) {
    mDrawableHeight = drawableHeight;
  }

  private int getDrawableHeight() {
    return mDrawableHeight;
  }
}
