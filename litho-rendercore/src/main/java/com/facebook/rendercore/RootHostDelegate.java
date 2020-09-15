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

import android.graphics.Rect;
import android.view.View.MeasureSpec;
import androidx.annotation.Nullable;
import com.facebook.infer.annotation.ThreadConfined;
import com.facebook.rendercore.extensions.MountExtension;
import com.facebook.rendercore.extensions.RenderCoreExtension;
import java.util.Map;

public class RootHostDelegate implements RenderState.HostListener, RootHost {

  private static final Rect sVisibleRect = new Rect();

  private final Host mHost;
  private final MountState mMountState;
  private @Nullable RenderState mRenderState;
  private @Nullable RenderTree mCurrentRenderTree;
  private boolean mDoMeasureInLayout;

  public RootHostDelegate(Host host) {
    mHost = host;
    mMountState = new MountState(mHost);
  }

  @ThreadConfined(ThreadConfined.UI)
  @Override
  public void setRenderState(RenderState renderState) {
    if (mRenderState == renderState) {
      return;
    }

    if (mRenderState != null) {
      mRenderState.detach();
    }

    mRenderState = renderState;

    if (renderState != null) {
      renderState.attach(this);
      onUIRenderTreeUpdated(renderState.getUIRenderTree());
    } else {
      onUIRenderTreeUpdated(null);
    }
  }

  @Override
  public void onUIRenderTreeUpdated(RenderTree newRenderTree) {
    if (mCurrentRenderTree == newRenderTree) {
      return;
    }

    if (newRenderTree == null) {
      mMountState.unmountAllItems();
    }

    mCurrentRenderTree = newRenderTree;

    mHost.requestLayout();
  }

  /**
   * Returns true if the delegate has defined a size and filled the measureOutput array, returns
   * false if not in which case the hosting view should call super.onMeasure.
   */
  public boolean onMeasure(int widthMeasureSpec, int heightMeasureSpec, int[] measureOutput) {
    int width = MeasureSpec.getSize(widthMeasureSpec);
    int height = MeasureSpec.getSize(heightMeasureSpec);
    if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY
        && MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY) {
      // If the measurements are exact, postpone LayoutState calculation from measure to layout.
      // This is part of the fix for android's double measure bug. Doing this means that if we get
      // remeasured with different exact measurements, we don't compute two layouts.
      mDoMeasureInLayout = true;
      measureOutput[0] = width;
      measureOutput[1] = height;
      return true;
    }

    if (mRenderState != null) {
      mRenderState.measure(widthMeasureSpec, heightMeasureSpec, measureOutput);
      mDoMeasureInLayout = false;
      return true;
    }

    return false;
  }

  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    if (mDoMeasureInLayout && mRenderState != null) {
      mRenderState.measure(
          MeasureSpec.makeMeasureSpec(right - left, MeasureSpec.EXACTLY),
          MeasureSpec.makeMeasureSpec(bottom - top, MeasureSpec.EXACTLY),
          null);
      mDoMeasureInLayout = false;
    }

    if (mCurrentRenderTree != null) {
      beforeMount();
      mMountState.mount(mCurrentRenderTree);
      afterMount();
    }
  }

  public @Nullable Object findMountContentById(long id) {
    return mMountState.findMountContentById(id);
  }

  private void beforeMount() {
    Map<RenderCoreExtension<?>, Object> results = mCurrentRenderTree.getExtensionResults();
    RenderCoreExtension<?>[] extensions = mRenderState.getExtensions();

    // Update the state of all the extensions that have a mount phase.
    if (extensions != null) {
      mHost.getLocalVisibleRect(sVisibleRect);
      for (RenderCoreExtension<?> e : extensions) {
        final Object state = results != null ? results.get(e) : null;
        final MountExtension extension = e.getMountExtension();
        if (extension != null) {
          extension.beforeMount(state, sVisibleRect);
        }
      }
    }
  }

  private void afterMount() {
    RenderCoreExtension<?>[] extensions = mRenderState.getExtensions();
    if (extensions != null) {
      for (RenderCoreExtension<?> e : extensions) {
        final MountExtension extension = e.getMountExtension();
        if (extension != null) {
          extension.afterMount();
        }
      }
    }
  }
}
