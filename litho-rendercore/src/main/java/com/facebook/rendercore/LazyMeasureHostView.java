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

package com.facebook.rendercore;

import static com.facebook.rendercore.RootHostDelegate.MAX_REMOUNT_RETRIES;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.rendercore.extensions.RenderCoreExtension;
import com.facebook.rendercore.utils.MeasureSpecUtils;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class LazyMeasureHostView extends HostView implements RenderCoreExtensionHost {

  private static final String TAG = "LazyMeasureHostView";
  private @Nullable LazyRenderTreeProvider mLazyRenderTreeProvider;
  private @Nullable RenderResult mCurrentRenderResult;

  public interface LazyRenderTreeProvider {
    RenderResult getRenderTreeForSize(
        int widthSpec, int heightSpec, @Nullable RenderResult previousRenderResult);
  }

  private final MountState mMountState;

  public LazyMeasureHostView(Context context) {
    super(context);
    mMountState = new MountState(this);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    if (mLazyRenderTreeProvider == null) {
      setMeasuredDimension(0, 0);
      mCurrentRenderResult = null;
      return;
    }

    mCurrentRenderResult =
        mLazyRenderTreeProvider.getRenderTreeForSize(
            widthMeasureSpec, heightMeasureSpec, mCurrentRenderResult);
    setMeasuredDimension(
        mCurrentRenderResult.getRenderTree().getWidth(),
        mCurrentRenderResult.getRenderTree().getHeight());
  }

  @Override
  void performLayout(boolean changed, int l, int t, int r, int b) {
    if (mLazyRenderTreeProvider != null) {
      LazyRenderTreeProvider lazyRenderTreeProvider = mLazyRenderTreeProvider;

      if (mCurrentRenderResult != null) {
        mMountState.mount(mCurrentRenderResult.getRenderTree());
      }
      // We could run into the case that mounting a tree ends up requesting another mount.
      // We need to keep re-mounting untile the mounted renderTree matches the mCurrentRenderResult.
      int retries = 0;
      while ((lazyRenderTreeProvider != mLazyRenderTreeProvider) || mCurrentRenderResult == null) {
        if (retries > MAX_REMOUNT_RETRIES) {
          ErrorReporter.report(
              LogLevel.ERROR,
              TAG,
              "More than "
                  + MAX_REMOUNT_RETRIES
                  + " recursive mount attempts. Skipping mounting the latest version.");

          return;
        }

        lazyRenderTreeProvider = mLazyRenderTreeProvider;
        mCurrentRenderResult =
            lazyRenderTreeProvider.getRenderTreeForSize(
                MeasureSpecUtils.exactly(r - l),
                MeasureSpecUtils.exactly(b - t),
                mCurrentRenderResult);
        mMountState.mount(mCurrentRenderResult.getRenderTree());
        retries++;
      }
    }

    performLayoutOnChildrenIfNecessary(this);
  }

  /**
   * Sets render lazyNavBarRenderResult and requests layout
   *
   * @param lazyRenderTreeProvider if null, it unmounts all items
   */
  public void setLazyRenderTreeProvider(@Nullable LazyRenderTreeProvider lazyRenderTreeProvider) {
    if (mLazyRenderTreeProvider == lazyRenderTreeProvider) {
      return;
    }

    if (lazyRenderTreeProvider == null) {
      mCurrentRenderResult = null;
      mMountState.unmountAllItems();
    }

    mLazyRenderTreeProvider = lazyRenderTreeProvider;
    this.requestLayout();
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
  @Override
  public void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    mMountState.detach();
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
  @Override
  public void onAttachedToWindow() {
    super.onAttachedToWindow();
    mMountState.attach();
  }

  @Override
  public void notifyVisibleBoundsChanged() {
    final RenderTree tree = mMountState.getRenderTree();
    RenderCoreExtension.notifyVisibleBoundsChanged(
        mMountState, this, tree != null ? tree.getExtensionResults() : null);
  }

  @Override
  public void offsetTopAndBottom(int offset) {
    super.offsetTopAndBottom(offset);
    notifyVisibleBoundsChanged();
  }

  @Override
  public void offsetLeftAndRight(int offset) {
    super.offsetLeftAndRight(offset);
    notifyVisibleBoundsChanged();
  }

  @Override
  public void setTranslationX(float translationX) {
    super.setTranslationX(translationX);
    notifyVisibleBoundsChanged();
  }

  @Override
  public void setTranslationY(float translationY) {
    super.setTranslationY(translationY);
    notifyVisibleBoundsChanged();
  }
}
