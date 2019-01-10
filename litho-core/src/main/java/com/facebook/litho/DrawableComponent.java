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

import android.content.Context;
import android.graphics.drawable.Drawable;
import com.facebook.litho.reference.Reference;

class DrawableComponent<T extends Drawable> extends Component {

  Reference<T> mDrawable;
  int mDrawableWidth;
  int mDrawableHeight;

  private DrawableComponent(Reference drawable) {
    super("DrawableComponent");
    mDrawable = drawable;
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
  protected void onMount(
      ComponentContext context,
      Object content) {
    MatrixDrawable drawable = (MatrixDrawable) content;

    drawable.mount(Reference.acquire(context.getAndroidContext(), getDrawable()));
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
    final T innerMountedDrawable = matrixDrawable.getMountedDrawable();

    matrixDrawable.unmount();
    Reference.release(context.getAndroidContext(), innerMountedDrawable, getDrawable());
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
