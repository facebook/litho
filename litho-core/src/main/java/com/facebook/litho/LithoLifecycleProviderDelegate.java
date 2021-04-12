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

import androidx.annotation.IntDef;
import java.util.ArrayList;
import java.util.List;

/**
 * Default {@link LithoLifecycleProvider} implementation. Defines the standard state changes for a
 * Litho ComponentTree. A custom LithoLifecycleProvider implementation can change the lifecycle
 * state but delegate to this to handle the effects of the state change. See an example of how this
 * facilitates a custom lifecycle implementation in {@link AOSPLithoLifecycleProvider}.
 */
public class LithoLifecycleProviderDelegate implements LithoLifecycleProvider {

  private final List<LithoLifecycleListener> mLithoLifecycleListeners = new ArrayList<>(4);
  private LithoLifecycle mCurrentState = LithoLifecycle.HINT_VISIBLE;

  @IntDef({
    LifecycleTransitionStatus.VALID,
    LifecycleTransitionStatus.NO_OP,
    LifecycleTransitionStatus.INVALID,
  })
  public @interface LifecycleTransitionStatus {
    int VALID = 0;
    int NO_OP = 1;
    int INVALID = 2;
  }

  @Override
  public void moveToLifecycle(LithoLifecycle newLifecycleState) {
    if (newLifecycleState == LithoLifecycle.DESTROYED
        && mCurrentState == LithoLifecycle.HINT_VISIBLE) {
      moveToLifecycle(LithoLifecycle.HINT_INVISIBLE);
    }

    final @LifecycleTransitionStatus int transitionStatus =
        getTransitionStatus(mCurrentState, newLifecycleState);

    if (transitionStatus == LifecycleTransitionStatus.INVALID) {
      throw new IllegalStateException(
          "Cannot move from state " + mCurrentState + " to state " + newLifecycleState);
    }

    if (transitionStatus == LifecycleTransitionStatus.VALID) {
      mCurrentState = newLifecycleState;
      switch (newLifecycleState) {
        case HINT_VISIBLE:
          notifyOnResumeVisible();
          return;
        case HINT_INVISIBLE:
          notifyOnPauseVisible();
          return;
        case DESTROYED:
          notifyOnDestroy();
          return;
        default:
          throw new IllegalStateException("State not known");
      }
    }
  }

  @Override
  public LithoLifecycle getLifecycleStatus() {
    return mCurrentState;
  }

  private static @LifecycleTransitionStatus int getTransitionStatus(
      LithoLifecycle currentState, LithoLifecycle nextState) {
    if (currentState == LithoLifecycle.DESTROYED) {
      return LifecycleTransitionStatus.INVALID;
    }

    if (nextState == LithoLifecycle.DESTROYED) {
      // You have to move through HINT_INVISIBLE before moving to DESTROYED.
      return currentState == LithoLifecycle.HINT_INVISIBLE
          ? LifecycleTransitionStatus.VALID
          : LifecycleTransitionStatus.INVALID;
    }

    if (nextState == LithoLifecycle.HINT_VISIBLE) {
      if (currentState == LithoLifecycle.HINT_VISIBLE) {
        return LifecycleTransitionStatus.NO_OP;
      }

      if (currentState == LithoLifecycle.HINT_INVISIBLE) {
        return LifecycleTransitionStatus.VALID;
      }

      return LifecycleTransitionStatus.INVALID;
    }

    if (nextState == LithoLifecycle.HINT_INVISIBLE) {
      if (currentState == LithoLifecycle.HINT_INVISIBLE) {
        return LifecycleTransitionStatus.NO_OP;
      }

      if (currentState == LithoLifecycle.HINT_VISIBLE) {
        return LifecycleTransitionStatus.VALID;
      }

      return LifecycleTransitionStatus.INVALID;
    }

    return LifecycleTransitionStatus.INVALID;
  }

  @Override
  public synchronized void addListener(LithoLifecycleListener listener) {
    mLithoLifecycleListeners.add(listener);
  }

  @Override
  public synchronized void removeListener(LithoLifecycleListener listener) {
    mLithoLifecycleListeners.remove(listener);
  }

  private void notifyOnResumeVisible() {
    final List<LithoLifecycleListener> lithoLifecycleListeners;
    synchronized (this) {
      lithoLifecycleListeners = new ArrayList<>(mLithoLifecycleListeners);
    }

    for (LithoLifecycleListener lithoLifecycleListener : lithoLifecycleListeners) {
      lithoLifecycleListener.onMovedToState(LithoLifecycle.HINT_VISIBLE);
    }
  }

  private void notifyOnPauseVisible() {
    final List<LithoLifecycleListener> lithoLifecycleListeners;
    synchronized (this) {
      lithoLifecycleListeners = new ArrayList<>(mLithoLifecycleListeners);
    }

    for (LithoLifecycleListener lithoLifecycleListener : lithoLifecycleListeners) {
      lithoLifecycleListener.onMovedToState(LithoLifecycle.HINT_INVISIBLE);
    }
  }

  private void notifyOnDestroy() {
    final List<LithoLifecycleListener> lithoLifecycleListeners;
    synchronized (this) {
      lithoLifecycleListeners = new ArrayList<>(mLithoLifecycleListeners);
    }

    for (LithoLifecycleListener lithoLifecycleListener : lithoLifecycleListeners) {
      lithoLifecycleListener.onMovedToState(LithoLifecycle.DESTROYED);
    }
  }
}
