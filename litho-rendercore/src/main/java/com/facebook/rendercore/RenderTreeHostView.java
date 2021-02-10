/*
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
import android.util.AttributeSet;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.facebook.rendercore.extensions.RenderCoreExtension;

public class RenderTreeHostView extends HostView implements RenderTreeHost {
  private static final String TAG = "RenderTreeHostView";

  private final MountState mMountState;
  private @Nullable RenderTree mCurrentRenderTree;

  public RenderTreeHostView(Context context) {
    this(context, null);
  }

  public RenderTreeHostView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    mMountState = new MountState(this);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    if (mCurrentRenderTree == null) {
      setMeasuredDimension(0, 0);
    } else {
      setMeasuredDimension(mCurrentRenderTree.getWidth(), mCurrentRenderTree.getHeight());
    }
  }

  @Override
  void performLayout(boolean changed, int l, int t, int r, int b) {
    if (mCurrentRenderTree != null) {
      RenderTree renderTree = mCurrentRenderTree;
      mMountState.mount(renderTree);
      // We could run into the case that mounting a tree ends up requesting another mount.
      // We need to keep re-mounting untile the mounted renderTree matches the mCurrentRenderTree.
      int retries = 0;
      while (renderTree != mCurrentRenderTree) {
        if (retries > MAX_REMOUNT_RETRIES) {
          ErrorReporter.report(
              LogLevel.ERROR,
              TAG,
              "More than "
                  + MAX_REMOUNT_RETRIES
                  + " recursive mount attempts. Skipping mounting the latest version.");

          return;
        }

        renderTree = mCurrentRenderTree;
        mMountState.mount(renderTree);
        retries++;
      }
    }

    performLayoutOnChildrenIfNecessary(this);
  }

  @Override
  public void setRenderTree(@Nullable RenderTree tree) {
    if (mCurrentRenderTree == tree) {
      return;
    }

    if (tree == null) {
      mMountState.unmountAllItems();
    }

    mCurrentRenderTree = tree;
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
