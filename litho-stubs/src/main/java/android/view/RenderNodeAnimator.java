/*
 * Copyright 2018-present Facebook, Inc.
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
package android.view;

import android.animation.Animator;
import android.animation.TimeInterpolator;
import android.graphics.Canvas;
import android.graphics.CanvasProperty;
import android.graphics.Paint;

public class RenderNodeAnimator extends Animator {

  public static int PAINT_ALPHA;
  public static int PAINT_STROKE_WIDTH;

  public RenderNodeAnimator(int property, float finalValue) {
    throw new RuntimeException("Stub!");
  }

  public RenderNodeAnimator(CanvasProperty<Float> property, float finalValue) {
    throw new RuntimeException("Stub!");
  }

  public RenderNodeAnimator(CanvasProperty<Paint> property, int paintField, float finalValue) {
    throw new RuntimeException("Stub!");
  }

  public RenderNodeAnimator(int x, int y, float startRadius, float endRadius) {
    throw new RuntimeException("Stub!");
  }

  @Override
  public long getStartDelay() {
    throw new RuntimeException("Stub!");
  }

  @Override
  public void setStartDelay(long startDelay) {
    throw new RuntimeException("Stub!");
  }

  @Override
  public Animator setDuration(long duration) {
    throw new RuntimeException("Stub!");
  }

  @Override
  public long getDuration() {
    throw new RuntimeException("Stub!");
  }

  @Override
  public void setInterpolator(TimeInterpolator value) {
    throw new RuntimeException("Stub!");
  }

  @Override
  public boolean isRunning() {
    throw new RuntimeException("Stub!");
  }

  public void setTarget(Canvas canvas) {
    throw new RuntimeException("Stub!");
  }

  public void setTarget(View view) {
    throw new RuntimeException("Stub!");
  }

  @Override
  public void setTarget(Object view) {
    throw new RuntimeException("Stub!");
  }
}
