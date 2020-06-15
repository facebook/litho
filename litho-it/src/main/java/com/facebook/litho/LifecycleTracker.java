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

import android.graphics.Rect;
import java.util.ArrayList;
import java.util.List;

/**
 * Can be used to track the sequence of MountSpec callbacks and optionally additional info related
 * to each callback.
 */
public class LifecycleTracker {
  private boolean isMounted = false;
  private boolean isBound = false;
  private boolean isAttached = false;
  private boolean isMeasured = false;

  private final List<LifecycleStep.StepInfo> steps = new ArrayList<>();

  public void addStep(LifecycleStep step, Object... args) {
    steps.add(new LifecycleStep.StepInfo(step, args));
    switch (step) {
      case ON_MEASURE:
        isMeasured = true;
        break;
      case ON_MOUNT:
        isMounted = true;
        break;
      case ON_UNMOUNT:
        isMounted = false;
        break;
      case ON_BIND:
        isBound = true;
        break;
      case ON_UNBIND:
        isBound = false;
        break;
      case ON_ATTACHED:
        isAttached = true;
        break;
      case ON_DETACHED:
        isAttached = false;
        break;
    }
  }

  public void reset() {
    steps.clear();
    isMounted = false;
    isBound = false;
    isAttached = false;
    isMeasured = false;
  }

  public List<LifecycleStep> getSteps() {
    return LifecycleStep.getSteps(steps);
  }

  public boolean isMounted() {
    return isMounted;
  }

  public boolean isBound() {
    return isBound;
  }

  public boolean isAttached() {
    return isAttached;
  }

  public boolean isMeasured() {
    return isMeasured;
  }

  public Size getIntrinsicSize() {
    return (Size) getInfo(LifecycleStep.ON_MEASURE).args[0];
  }

  private LifecycleStep.StepInfo getInfo(LifecycleStep step) {
    for (LifecycleStep.StepInfo stepInfo : steps) {
      if (stepInfo.step == step) {
        return stepInfo;
      }
    }
    throw new IllegalStateException("'" + step + "' was not called or lifecycle steps were reset.");
  }

  public int getWidth() {
    return ((Rect) getInfo(LifecycleStep.ON_BOUNDS_DEFINED).args[0]).width();
  }

  public int getHeight() {
    return ((Rect) getInfo(LifecycleStep.ON_BOUNDS_DEFINED).args[0]).height();
  }
}
