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

import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.Nullable;

public class RenderTreeHostView extends HostView implements RenderTreeHost {

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
      mMountState.mount(mCurrentRenderTree);
    }
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
}
