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

package com.facebook.litho.animation;

import android.support.annotation.VisibleForTesting;
import com.facebook.litho.dataflow.BindingListener;
import com.facebook.litho.dataflow.GraphBinding;
import com.facebook.litho.dataflow.ValueNode;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Base class for defining animations for transitions between states of the component hierarchy.
 * Subclasses should define their animation by creating a {@link GraphBinding} in
 * {@link #setupBinding}.
 */
public abstract class TransitionAnimationBinding implements AnimationBinding {

  private final GraphBinding mGraphBinding;
  private final CopyOnWriteArrayList<AnimationBindingListener> mListeners =
      new CopyOnWriteArrayList<>();

  public TransitionAnimationBinding() {
    this(GraphBinding.create());
  }

  @VisibleForTesting
  TransitionAnimationBinding(GraphBinding graphBinding) {
    mGraphBinding = graphBinding;
    mGraphBinding.setListener(new BindingListener() {
      @Override
      public void onAllNodesFinished(GraphBinding binding) {
        TransitionAnimationBinding.this.onAllNodesFinished();
      }
    });
  }

  /**
   * @see GraphBinding#addBinding(ValueNode, ValueNode, String)
   */
  public void addBinding(ValueNode fromNode, ValueNode toNode, String inputName) {
    mGraphBinding.addBinding(fromNode, toNode, inputName);
  }

  /**
   * @see GraphBinding#addBinding(ValueNode, ValueNode)
   */
  public void addBinding(ValueNode fromNode, ValueNode toNode) {
    mGraphBinding.addBinding(fromNode, toNode);
  }

  @Override
  public void start(Resolver resolver) {
    for (AnimationBindingListener listener : mListeners) {
      if (!listener.shouldStart(this)) {
        notifyCanceledBeforeStart();
        return;
      }
    }
    for (AnimationBindingListener listener : mListeners) {
      listener.onWillStart(this);
    }
    setupBinding(resolver);
    mGraphBinding.activate();
  }

  /**
   * Subclasses should set up their animation by creating a graph that defines how data will flow
   * to relevant {@link AnimatedPropertyNode}s.
   */
  protected abstract void setupBinding(Resolver resolver);

  @Override
  public void stop() {
    if (!isActive()) {
      return;
    }
    mGraphBinding.deactivate();
  }

  @Override
  public boolean isActive() {
    return mGraphBinding.isActive();
  }

  private void onAllNodesFinished() {
    for (AnimationBindingListener listener : mListeners) {
      listener.onFinish(this);
    }
    stop();
  }

  private void notifyCanceledBeforeStart() {
    for (AnimationBindingListener listener : mListeners) {
      listener.onCanceledBeforeStart(this);
    }
  }

  @Override
  public void addListener(AnimationBindingListener bindingListener) {
    mListeners.add(bindingListener);
  }

  @Override
  public void removeListener(AnimationBindingListener animationBindingListener) {
    mListeners.remove(animationBindingListener);
  }
}
