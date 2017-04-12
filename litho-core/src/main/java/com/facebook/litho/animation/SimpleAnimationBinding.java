/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.animation;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import android.support.annotation.VisibleForTesting;

import com.facebook.litho.dataflow.BadGraphSetupException;
import com.facebook.litho.dataflow.BindingListener;
import com.facebook.litho.dataflow.GraphBinding;
import com.facebook.litho.dataflow.ValueNode;

import static com.facebook.litho.dataflow.ValueNode.DEFAULT_INPUT;

/**
 * An {@link AnimationBinding} corresponding to a single {@link GraphBinding}. It adds the ability
 * to add bindings to {@link PendingNode}s.
 */
public class SimpleAnimationBinding implements AnimationBinding {

  private final GraphBinding mGraphBinding;
  private final ArrayList<PendingBinding> mPendingBindings = new ArrayList<>();
  private final ArrayList<PendingNode> mPendingNodes = new ArrayList<>();
  private final CopyOnWriteArrayList<AnimationBindingListener> mListeners =
      new CopyOnWriteArrayList<>();

  public SimpleAnimationBinding() {
    this(GraphBinding.create());
  }

  @VisibleForTesting
  SimpleAnimationBinding(GraphBinding graphBinding) {
    mGraphBinding = graphBinding;
    mGraphBinding.setListener(new BindingListener() {
      @Override
      public void onAllNodesFinished(GraphBinding binding) {
        SimpleAnimationBinding.this.onAllNodesFinished();
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

  /**
   * Adds a PendingBinding from a concrete ValueNode to a PendingNode. The binding will actually be
   * added at runtime when the PendingNode can be turned into a concrete ValueNode. If
   * BindingSpecs are added, {@link #resolvePendingNodes} must be called before this binding is
   * activated.
   */
  public void addPendingBinding(ValueNode fromNode, PendingNode toNode, String inputName) {
    mPendingNodes.add(toNode);
    mPendingBindings.add(PendingBinding.create(fromNode, toNode, inputName));
  }

  /**
   * Adds a PendingBinding from a ValueNodeSpec to a concrete ValueNode.
   * @see #addPendingBinding(PendingNode, ValueNode, String)
   */
  public void addPendingBinding(PendingNode fromNode, ValueNode toNode, String inputName) {
    mPendingNodes.add(fromNode);
    mPendingBindings.add(PendingBinding.create(fromNode, toNode, inputName));
  }

  /**
   * Adds a PendingBinding from a concrete ValueNode as the default input to a ValueNodeSpec.
   * @see #addPendingBinding(PendingNode, ValueNode, String)
   */
  public void addPendingBinding(ValueNode fromNode, PendingNode toNode) {
    mPendingNodes.add(toNode);
    mPendingBindings.add(PendingBinding.create(fromNode, toNode, DEFAULT_INPUT));
  }

  /**
   * Adds a PendingBinding from a ValueNodeSpec as the default input to a concrete ValueNode.
   * @see #addPendingBinding(PendingNode, ValueNode, String)
   */
  public void addPendingBinding(PendingNode fromNode, ValueNode toNode) {
    mPendingNodes.add(fromNode);
    mPendingBindings.add(PendingBinding.create(fromNode, toNode, DEFAULT_INPUT));
  }

  @Override
  public void start() {
    if (mPendingBindings.size() > 0) {
      throw new BadGraphSetupException(
          "Tried to activate a GraphBinding that has unresolved ValueNodeSpecs!");
    }
    mGraphBinding.activate();
  }

  @Override
  public void stop() {
    mGraphBinding.deactivate();
  }

  @Override
  public boolean isActive() {
    return mGraphBinding.isActive();
  }

  @Override
  public void resolvePendingNodes(PendingNodeResolver resolver) {
    for (int i = 0; i < mPendingBindings.size(); i++) {
      final PendingBinding pendingBinding = mPendingBindings.get(i);
      final ValueNode resolvedSpecNode = resolver.resolve(pendingBinding.pendingNode);
      if (pendingBinding.isFromResolvedToPending) {
        mGraphBinding.addBinding(
            pendingBinding.resolvedNode,
            resolvedSpecNode,
            pendingBinding.inputName);
      } else {
        mGraphBinding.addBinding(
            resolvedSpecNode,
            pendingBinding.resolvedNode,
            pendingBinding.inputName);
      }
    }
    mPendingBindings.clear();
    mPendingNodes.clear();
  }

  private void onAllNodesFinished() {
    for (AnimationBindingListener listener : mListeners) {
      listener.onFinish(this);
    }
    stop();
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
