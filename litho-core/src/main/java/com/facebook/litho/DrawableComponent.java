/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import android.graphics.drawable.Drawable;
import com.facebook.litho.reference.Reference;

class DrawableComponent<T extends Drawable> extends Component {

  Reference<T> mDrawable;
  int mDrawableWidth;
  int mDrawableHeight;

  private DrawableComponent(Reference drawable) {
    super();
    mDrawable = drawable;
  }

  @Override
  protected void onBoundsDefined(ComponentContext c, ComponentLayout layout) {
    setDrawableWidth(layout.getWidth());
    setDrawableHeight(layout.getHeight());
  }

  @Override
  protected Object onCreateMountContent(ComponentContext c) {
    return new MatrixDrawable();
  }

  @Override
  protected void onMount(
      ComponentContext context,
      Object content) {
    MatrixDrawable drawable = (MatrixDrawable) content;

    drawable.mount(Reference.acquire(context, getDrawable()));
  }

  @Override
  protected void onBind(
      ComponentContext c,
      Object mountedContent) {
    final MatrixDrawable mountedDrawable = (MatrixDrawable) mountedContent;

    mountedDrawable.bind(getDrawableWidth(), getDrawableHeight());
  }

  @Override
  protected void onUnmount(
      ComponentContext context,
      Object mountedContent) {
    final MatrixDrawable<T> matrixDrawable = (MatrixDrawable<T>) mountedContent;
    Reference.release(context, matrixDrawable.getMountedDrawable(), getDrawable());
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

  public static DrawableComponent create(Reference<? extends Drawable> drawable) {
    return new DrawableComponent<>(drawable);
  }

  @Override
  protected boolean shouldUpdate(Component previous, Component next) {
    final Reference previousReference = ((DrawableComponent) previous).getDrawable();
    final Reference nextReference = ((DrawableComponent) next).getDrawable();

    return Reference.shouldUpdate(previousReference, nextReference);
  }

  @Override
  public String getSimpleName() {
    return mDrawable.getSimpleName();
  }

  private Reference<T> getDrawable() {
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
