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

public class RootHostView extends HostView implements RootHost {

  private static final int[] MEASURE_OUTPUTS = new int[2];

  private final RootHostDelegate mRootHostDelegate;

  public RootHostView(Context context) {
    this(context, null);
  }

  public RootHostView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    mRootHostDelegate = new RootHostDelegate(this);
  }

  @Override
  public void setRenderState(@Nullable RenderState renderState) {
    mRootHostDelegate.setRenderState(renderState);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    if (mRootHostDelegate.onMeasure(widthMeasureSpec, heightMeasureSpec, MEASURE_OUTPUTS)) {
      setMeasuredDimension(MEASURE_OUTPUTS[0], MEASURE_OUTPUTS[1]);
    } else {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
  }

  @Override
  void performLayout(boolean changed, int l, int t, int r, int b) {
    mRootHostDelegate.onLayout(changed, l, t, r, b);
    performLayoutOnChildrenIfNecessary(this);
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

  public @Nullable Object findMountContentById(long id) {
    return mRootHostDelegate.findMountContentById(id);
  }

  private void notifyVisibleBoundsChanged() {
    mRootHostDelegate.notifyVisibleBoundsChanged();
  }
}
