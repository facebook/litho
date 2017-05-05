/**
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import java.util.ArrayList;
import java.util.List;

import android.support.v4.util.Pools;
import android.support.v4.util.SimpleArrayMap;
import android.view.View;
import android.view.ViewParent;

import com.facebook.litho.animation.AnimatedPropertyNode;
import com.facebook.litho.animation.AnimationBinding;
import com.facebook.litho.animation.AnimationBindingListener;
import com.facebook.litho.animation.LazyValue;
import com.facebook.litho.animation.AnimatedProperty;
import com.facebook.litho.animation.Resolver;
import com.facebook.litho.animation.ComponentProperty;
import com.facebook.litho.internal.ArraySet;

/**
 * Implementation of transitions in Litho via a dataflow graph.
 */
public class DataFlowTransitionManager {

  /**
   * A listener that will be invoked when a mount item has stopped animating.
   */
  public interface OnMountItemAnimationComplete {
    void onMountItemAnimationComplete(Object mountItem);
  }

  private static final Pools.SimplePool<AnimationState> sAnimationStatePool =
      new Pools.SimplePool<>(20);

  /**
   * The before and after values of single component undergoing a transition.
   */
  private static class TransitionDiff {

    public final SimpleArrayMap<AnimatedProperty, Float> beforeValues = new SimpleArrayMap<>();
    public final SimpleArrayMap<AnimatedProperty, Float> afterValues = new SimpleArrayMap<>();

    public void reset() {
      beforeValues.clear();
      afterValues.clear();
    }
  }

  /**
   * Animation state of a MountItem.
   */
  private static class AnimationState {

    public final ArraySet<AnimationBinding> activeAnimations = new ArraySet<>();
    public final SimpleArrayMap<AnimatedProperty, AnimatedPropertyNode> animatedPropertyNodes =
        new SimpleArrayMap<>();
    public ArraySet<AnimatedProperty> animatingProperties = new ArraySet<>();
    public Object mountItem;
    public ArrayList<OnMountItemAnimationComplete> mAnimationCompleteListeners = new ArrayList<>();
    public TransitionDiff currentDiff = new TransitionDiff();
    public int changeType = TransitionManager.KeyStatus.UNSET;
    public boolean sawInPreMount = false;

    public void reset() {
      activeAnimations.clear();
      animatedPropertyNodes.clear();
      animatingProperties.clear();
      mountItem = null;
      mAnimationCompleteListeners.clear();
      currentDiff.reset();
      changeType = TransitionManager.KeyStatus.UNSET;
      sawInPreMount = false;
    }
  }

  private final ArrayList<AnimationBinding> mAnimationBindings = new ArrayList<>();
  private final SimpleArrayMap<AnimationBinding, ArraySet<String>> mAnimationsToKeys =
      new SimpleArrayMap<>();
  private final SimpleArrayMap<String, AnimationState> mAnimationStates = new SimpleArrayMap<>();
  private final TransitionsAnimationBindingListener mAnimationBindingListener =
      new TransitionsAnimationBindingListener();
  private final TransitionsResolver mResolver = new TransitionsResolver();

  void onNewTransitionContext(TransitionContext transitionContext) {
    mAnimationBindings.clear();
    mAnimationBindings.addAll(transitionContext.getTransitionAnimationBindings());

    for (int i = 0, size = mAnimationStates.size(); i < size; i++) {
      final AnimationState animationState = mAnimationStates.valueAt(i);
      animationState.sawInPreMount = false;
      animationState.currentDiff.reset();
    }

    recordAllTransitioningProperties();
  }

  void onPreMountItem(String transitionKey, Object mountItem) {

    final AnimationState animationState = mAnimationStates.get(transitionKey);
    if (animationState != null) {
      for (int i = 0; i < animationState.animatingProperties.size(); i++) {
        final AnimatedProperty prop = animationState.animatingProperties.valueAt(i);
        if (animationState.currentDiff.beforeValues.put(prop, prop.get(mountItem)) != null) {
          throw new RuntimeException("TransitionDiff wasn't cleared properly!");
        }

        // Unfortunately, we have no guarantee that this mountItem won't be re-used for another
        // different component during the coming mount, so we need to reset it before the actual
        // mount happens. The proper before-values will be set again before any animations start.
        prop.reset(mountItem);
      }
      setMountItem(animationState, mountItem);

      // We set the change type to disappeared for now: if we see it again in onPostMountItem we'll
      // update it there
      animationState.changeType = TransitionManager.KeyStatus.DISAPPEARED;
      animationState.sawInPreMount = true;
    }
  }

  void onPostMountItem(String transitionKey, Object mountItem) {
    final AnimationState animationState = mAnimationStates.get(transitionKey);
    if (animationState != null) {
      if (!animationState.sawInPreMount) {
        animationState.changeType = TransitionManager.KeyStatus.APPEARED;
      } else {
        animationState.changeType = TransitionManager.KeyStatus.UNCHANGED;
      }

      setMountItem(animationState, mountItem);

      for (int i = 0; i < animationState.animatingProperties.size(); i++) {
        final AnimatedProperty prop = animationState.animatingProperties.valueAt(i);
        if (animationState.currentDiff.afterValues.put(prop, prop.get(mountItem)) != null) {
          throw new RuntimeException("TransitionDiff wasn't cleared properly!");
        }
      }
    }
  }

  void activateBindings() {
    restoreInitialStates();
    setDisappearToValues();
    for (int i = 0, size = mAnimationBindings.size(); i < size; i++) {
      final AnimationBinding binding = mAnimationBindings.get(i);
      binding.addListener(mAnimationBindingListener);
      binding.start(mResolver);
    }
  }

  void addMountItemAnimationCompleteListener(String key, OnMountItemAnimationComplete listener) {
    final AnimationState state = mAnimationStates.get(key);
    state.mAnimationCompleteListeners.add(listener);
  }

  boolean isKeyAnimating(String key) {
    return mAnimationStates.containsKey(key);
  }

  void cleanupDisappearingTransitions(List<String> transitionKeys) {
    for (int i = 0, size = transitionKeys.size(); i < size; i++) {
      final String key = transitionKeys.get(i);
      final AnimationState animationState = mAnimationStates.get(key);
      if (animationState == null) {
        continue;
      }

      setMountItem(animationState, null);
    }
  }

  private void restoreInitialStates() {
    for (int i = 0, size = mAnimationStates.size(); i < size; i++) {
      final String transitionKey = mAnimationStates.keyAt(i);
      final AnimationState animationState = mAnimationStates.valueAt(i);
      // If the component is appearing, we will instead restore the initial value in
      // setAppearFromValues. This is necessary since appearFrom values can be written in terms of
      // the end state (e.g. appear from an offset of -10dp)
      if (animationState.changeType != TransitionManager.KeyStatus.APPEARED) {
        for (int j = 0; j < animationState.currentDiff.beforeValues.size(); j++) {
          final AnimatedProperty property = animationState.currentDiff.beforeValues.keyAt(j);
          property.set(
              animationState.mountItem,
              animationState.currentDiff.beforeValues.valueAt(j));
        }
      }
    }
    setAppearFromValues();
  }

  private void setAppearFromValues() {
    SimpleArrayMap<ComponentProperty, LazyValue> appearFromValues = new SimpleArrayMap<>();
    for (int i = 0, size = mAnimationBindings.size(); i < size; i++) {
      final AnimationBinding binding = mAnimationBindings.get(i);
      binding.collectAppearFromValues(appearFromValues);
    }

    for (int i = 0, size = appearFromValues.size(); i < size; i++) {
      final ComponentProperty property = appearFromValues.keyAt(i);
      final LazyValue lazyValue = appearFromValues.valueAt(i);
      final AnimationState animationState = mAnimationStates.get(property.getTransitionKey());
      final float value = lazyValue.resolve(mResolver, property);
      property.getProperty().set(animationState.mountItem, value);

      if (animationState.changeType != TransitionManager.KeyStatus.APPEARED) {
        throw new RuntimeException(
            "Wrong transition type for appear: " + animationState.changeType);
      }
      animationState.currentDiff.beforeValues.put(property.getProperty(), value);
    }
  }

  private void setDisappearToValues() {
    SimpleArrayMap<ComponentProperty, LazyValue> disappearToValues = new SimpleArrayMap<>();
    for (int i = 0, size = mAnimationBindings.size(); i < size; i++) {
      final AnimationBinding binding = mAnimationBindings.get(i);
      binding.collectDisappearToValues(disappearToValues);
    }

    for (int i = 0, size = disappearToValues.size(); i < size; i++) {
      final ComponentProperty property = disappearToValues.keyAt(i);
      final LazyValue lazyValue = disappearToValues.valueAt(i);
      final AnimationState animationState = mAnimationStates.get(property.getTransitionKey());
      if (animationState.changeType != TransitionManager.KeyStatus.DISAPPEARED) {
        throw new RuntimeException(
            "Wrong transition type for disappear: " + animationState.changeType);
      }
      final float value = lazyValue.resolve(mResolver, property);
      animationState.currentDiff.afterValues.put(property.getProperty(), value);
    }
  }

  /**
   * This method should record the transition key and animated properties of all animating mount
   * items so that we know whether to record them in onPre/PostMountItem
   */
  private void recordAllTransitioningProperties() {
    final ArraySet<ComponentProperty> transitioningProperties = ComponentsPools.acquireArraySet();
    for (int i = 0, size = mAnimationBindings.size(); i < size; i++) {
      final AnimationBinding binding = mAnimationBindings.get(i);
      final ArraySet<String> animatedKeys = ComponentsPools.acquireArraySet();
      mAnimationsToKeys.put(binding, animatedKeys);

      binding.collectTransitioningProperties(transitioningProperties);

      for (int j = 0, propSize = transitioningProperties.size(); j < propSize; j++) {
        final ComponentProperty property = transitioningProperties.valueAt(j);
        final String key = property.getTransitionKey();
        final AnimatedProperty animatedProperty = property.getProperty();
        animatedKeys.add(key);

        // This key will be animating - make sure it has an AnimationState
        AnimationState animationState = mAnimationStates.get(key);
        if (animationState == null) {
          animationState = acquireAnimationState();
          mAnimationStates.put(key, animationState);
        }
        animationState.animatingProperties.add(animatedProperty);
        animationState.activeAnimations.add(binding);
      }
      transitioningProperties.clear();
    }
    ComponentsPools.release(transitioningProperties);
  }

  private AnimatedPropertyNode getOrCreateAnimatedPropertyNode(
      String key,
      AnimatedProperty animatedProperty) {
    final AnimationState state = mAnimationStates.get(key);
    AnimatedPropertyNode node = state.animatedPropertyNodes.get(animatedProperty);
    if (node == null) {
      node = new AnimatedPropertyNode(state.mountItem, animatedProperty);
      state.animatedPropertyNodes.put(animatedProperty, node);
    }
    return node;
  }

  private void setMountItem(AnimationState animationState, Object newMountItem) {
    // If the mount item changes, this means this transition key will be rendered with a different
    // mount item (View or Drawable) than it was during the last mount, so we need to migrate
    // animation state from the old mount item to the new one.

    if (animationState.mountItem == newMountItem) {
      return;
    }

    if (animationState.mountItem != null) {
      final ArraySet<AnimatedProperty> animatingProperties = animationState.animatingProperties;
      for (int i = 0, size = animatingProperties.size(); i < size; i++) {
        animatingProperties.valueAt(i).reset(animationState.mountItem);
      }
      onMountItemAnimationComplete(animationState);
    }
    for (int i = 0, size = animationState.animatedPropertyNodes.size(); i < size; i++) {
      animationState.animatedPropertyNodes.valueAt(i).setMountItem(newMountItem);
    }
    recursivelySetChildClipping(newMountItem, false);
    animationState.mountItem = newMountItem;
  }

  private void onMountItemAnimationComplete(AnimationState animationState) {
    recursivelySetChildClipping(animationState.mountItem, true);
    fireMountItemAnimationCompleteListeners(animationState);
  }

  private void fireMountItemAnimationCompleteListeners(AnimationState animationState) {
    if (animationState.mountItem == null) {
      return;
    }

    final ArrayList<OnMountItemAnimationComplete> listeners =
        animationState.mAnimationCompleteListeners;
    for (int i = 0, listenerSize = listeners.size(); i < listenerSize; i++) {
      listeners.get(i).onMountItemAnimationComplete(animationState.mountItem);
    }
    listeners.clear();
  }

  private static AnimationState acquireAnimationState() {
    AnimationState animationState = sAnimationStatePool.acquire();
    if (animationState == null) {
      animationState = new AnimationState();
    }
    return animationState;
  }

  private static void releaseAnimationState(AnimationState animationState) {
    animationState.reset();
    sAnimationStatePool.release(animationState);
  }

  private class TransitionsAnimationBindingListener implements AnimationBindingListener {

    @Override
    public void onStart(AnimationBinding binding) {
    }

    @Override
    public void onFinish(AnimationBinding binding) {
      final ArraySet<String> transitioningKeys = mAnimationsToKeys.remove(binding);

      // When an animation finishes, we want to go through all the mount items it was animating and
      // see if it was the last active animation. If it was, we know that item is no longer
      // animating and we can release the animation state.
      
      for (int i = 0, size = transitioningKeys.size(); i < size; i++) {
        final String key = transitioningKeys.valueAt(i);
        final AnimationState animationState = mAnimationStates.get(key);
        if (!animationState.activeAnimations.remove(binding)) {
          throw new RuntimeException(
              "Some animation bookkeeping is wrong: tried to remove an animation from the list " +
                  "of active animations, but it wasn't there.");
        }
        if (animationState.activeAnimations.size() == 0) {
          if (animationState.changeType == TransitionManager.KeyStatus.DISAPPEARED) {
            for (int j = 0; j < animationState.animatingProperties.size(); j++) {
              animationState.animatingProperties.valueAt(j).reset(animationState.mountItem);
            }
          }
          onMountItemAnimationComplete(animationState);
          mAnimationStates.remove(key);
          releaseAnimationState(animationState);
        }
      }
      ComponentsPools.release(transitioningKeys);
    }
  }

  /**
   * Set the clipChildren properties to all Views in the same tree branch from the given one, up to
   * the top LithoView.
   *
   * TODO(17934271): Handle the case where two+ animations with different lifespans share the same
   * parent, in which case we shouldn't unset clipping until the last item is done animating.
   */
  private void recursivelySetChildClipping(Object mountItem, boolean clipChildren) {
    if (!(mountItem instanceof View)) {
      return;
    }

    recursivelySetChildClippingForView((View) mountItem, clipChildren);
  }

  private void recursivelySetChildClippingForView(View view, boolean clipChildren) {
    if (view instanceof ComponentHost) {
      ((ComponentHost) view).setClipChildren(clipChildren);
    }

    final ViewParent parent = view.getParent();
    if (parent instanceof ComponentHost) {
      recursivelySetChildClippingForView((View) parent, clipChildren);
    }
  }

  private class TransitionsResolver implements Resolver {

    @Override
    public float getCurrentState(ComponentProperty property) {
      final AnimationState animationState = mAnimationStates.get(property.getTransitionKey());
      return property.getProperty().get(animationState.mountItem);
    }

    @Override
    public float getEndState(ComponentProperty property) {
      final AnimationState animationState = mAnimationStates.get(property.getTransitionKey());
      return animationState.currentDiff.afterValues.get(property.getProperty());
    }

    @Override
    public AnimatedPropertyNode getAnimatedPropertyNode(ComponentProperty property) {
      return getOrCreateAnimatedPropertyNode(property.getTransitionKey(), property.getProperty());
    }
  }
}
