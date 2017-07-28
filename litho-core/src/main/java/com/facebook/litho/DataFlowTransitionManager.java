/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v4.util.SimpleArrayMap;
import android.util.Log;
import android.view.View;
import android.view.ViewParent;

import com.facebook.litho.animation.AnimatedProperty;
import com.facebook.litho.animation.AnimatedPropertyNode;
import com.facebook.litho.animation.AnimationBinding;
import com.facebook.litho.animation.AnimationBindingListener;
import com.facebook.litho.animation.ComponentProperty;
import com.facebook.litho.animation.Resolver;
import com.facebook.litho.animation.RuntimeValue;
import com.facebook.litho.internal.ArraySet;

import static com.facebook.litho.AnimationsDebug.TAG;

/**
 * Unique per MountState instance. Called from MountState on mount calls to process the transition
 * keys and handles which transitions to run and when.
 *
 * This class is tightly coupled to MountState. When creating new animations, the expected usage of
 * this class is:
 * 1. {@link #setupTransitions} is called with the current and next LayoutStates.
 * 2. {@link #isKeyAnimating} and {@link #isKeyDisappearing} can be called to determine what is/will
 *    be animating
 * 3. MountState updates the mount content for changing content.
 * 4. {@link #runTransitions} is called to restore initial states for the transition and run the new
 *    animations.
 *
 * Additionally, any time the {@link MountState} is re-used for a different component tree (e.g.
 * because it was recycled in a RecyclerView), {@link #reset} should be called to stop running
 * all existing animations.
 *
 * SOME TECHNICAL DETAILS:
 *
 * First, we need to figure out which keys are appearing and disappearing. Based on this, we create
 * different animations (e.g. a key could have no animation for appear, but an animation for
 * change).
 *
 * These animations can reference different properties, so it's only at that point that we can
 * actually record before/after values (in {@link #recordAllTransitioningProperties}). This is also
 * why we keep around LayoutOutputs beyond the actual {@link #recordLayoutOutputDiff} call.
 *
 * Finally, in {@link #runTransitions}, the animations can configure themselves with arbitrary
 * start/end properties via the {@link TransitionsResolver}. The animations run and we can finally
 * release the saved LayoutOutputs and cleanup any state for animations that didn't need to run.
 */
public class DataFlowTransitionManager {

  @IntDef({KeyStatus.APPEARED, KeyStatus.CHANGED, KeyStatus.DISAPPEARED, KeyStatus.UNSET})
  @Retention(RetentionPolicy.SOURCE)
  @interface KeyStatus {
    int UNSET = -1;
    int APPEARED = 0;
    int CHANGED = 1;
    int DISAPPEARED = 2;
  }

  /**
   * A listener that will be invoked when a mount content has stopped animating.
   */
  public interface OnAnimationCompleteListener {
    void onAnimationComplete(String transitionKey);
  }

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
   * Animation state of a given mount content. Holds everything we currently know about an animating
   * transition key, as well as information about its most recent changes in property values and
   * whether it's appearing, disappearing, or changing.
   */
  private static class AnimationState {

    public final ArraySet<AnimationBinding> activeAnimations = new ArraySet<>();
    public final SimpleArrayMap<AnimatedProperty, AnimatedPropertyNode> animatedPropertyNodes =
        new SimpleArrayMap<>();
    public ArraySet<AnimatedProperty> animatingProperties = new ArraySet<>();
    public Object mountContent;
    public TransitionDiff currentDiff = new TransitionDiff();
    public int changeType = KeyStatus.UNSET;
    public @Nullable LayoutOutput currentLayoutOutput;
    public @Nullable LayoutOutput nextLayoutOutput;
  }

  private final ArrayList<AnimationBinding> mAnimationBindings = new ArrayList<>();
  private final SimpleArrayMap<AnimationBinding, ArraySet<String>> mAnimationsToKeys =
      new SimpleArrayMap<>();
  private final SimpleArrayMap<String, AnimationState> mAnimationStates = new SimpleArrayMap<>();
  private final TransitionsAnimationBindingListener mAnimationBindingListener =
      new TransitionsAnimationBindingListener();
  private final TransitionsResolver mResolver = new TransitionsResolver();
  private final ArrayList<Transition> mTransitions = new ArrayList<>();
  private final OnAnimationCompleteListener mOnAnimationCompleteListener;

  public DataFlowTransitionManager(OnAnimationCompleteListener onAnimationCompleteListener) {
    mOnAnimationCompleteListener = onAnimationCompleteListener;
  }

  /**
   * Creates (but doesn't start) the animations for the next transition based on the current and
   * next layout states. After this is called, MountState can commit the layout changes and then
   * call {@link #runTransitions} to restore the initial states and run the animations.
   */
  void setupTransitions(LayoutState currentLayoutState, LayoutState nextLayoutState) {
    prepareTransitions(nextLayoutState.getTransitionContext());

    final SimpleArrayMap<String, LayoutOutput> currentTransitionKeys =
        currentLayoutState.getTransitionKeyMapping();
    final SimpleArrayMap<String, LayoutOutput> nextTransitionKeys =
        nextLayoutState.getTransitionKeyMapping();
    final boolean[] seenIndicesInNewLayout = new boolean[currentTransitionKeys.size()];
    for (int i = 0, size = nextTransitionKeys.size(); i < size; i++) {
      final String transitionKey = nextTransitionKeys.keyAt(i);
      final LayoutOutput nextLayoutOutput = nextTransitionKeys.valueAt(i);

      final int currentIndex = currentTransitionKeys.indexOfKey(transitionKey);

      LayoutOutput currentLayoutOutput = null;
      if (currentIndex >= 0) {
        currentLayoutOutput = currentTransitionKeys.valueAt(currentIndex);
        seenIndicesInNewLayout[currentIndex] = true;
      }

      recordLayoutOutputDiff(transitionKey, currentLayoutOutput, nextLayoutOutput);
    }

    for (int i = 0, size = currentTransitionKeys.size(); i < size; i++) {
      if (seenIndicesInNewLayout[i]) {
        continue;
      }
      recordLayoutOutputDiff(
          currentTransitionKeys.keyAt(i),
          currentTransitionKeys.valueAt(i),
          null);
    }

    commitLayoutOutputDiffs();
  }

  /**
   * Called after {@link #commitLayoutOutputDiffs} has been called and the new layout has been
   * mounted. This restores the state of the previous layout for content that will animate and then
   * starts the corresponding animations.
   */
  void runTransitions() {
    restoreInitialStates();

    if (AnimationsDebug.ENABLED) {
      debugLogStartingAnimations();
    }

    for (int i = 0, size = mAnimationBindings.size(); i < size; i++) {
      final AnimationBinding binding = mAnimationBindings.get(i);
      binding.addListener(mAnimationBindingListener);
      binding.start(mResolver);
    }

    cleanupLayoutOutputs();
  }

  /**
   * Sets the mount content for a given key. This is used to initially set mount content, but also
   * to set content when content is incrementally mounted/unmounted during an animation.
   */
  void setMountContentInner(String transitionKey, Object mountContent) {
    final AnimationState animationState = mAnimationStates.get(transitionKey);
    if (animationState != null) {
      setMountContentInner(transitionKey, animationState, mountContent);
    }
  }

  /**
   * After mount content diffs have been committed with {@link #commitLayoutOutputDiffs}, returns
   * whether the given key will be/is animating.
   */
  boolean isKeyAnimating(String key) {
    return mAnimationStates.containsKey(key);
  }

  /**
   * After mount content diffs have been committed with {@link #commitLayoutOutputDiffs}, returns
   * whether the given key will be disappearing **in this layout diff**.
   */
  boolean isKeyDisappearing(String key) {
    final AnimationState animationState = mAnimationStates.get(key);
    if (animationState == null) {
      return false;
    }
    return animationState.changeType == KeyStatus.DISAPPEARED;
  }

  /**
   * To be called when a MountState is recycled for a new component tree. Clears all animations.
   */
  void reset() {
    // Clear/reset all animations
    for (int i = 0, size = mAnimationStates.size(); i < size; i++) {
      final String key = mAnimationStates.keyAt(i);
      final AnimationState animationState = mAnimationStates.valueAt(i);
      setMountContentInner(key, animationState, null);
      clearLayoutOutputs(animationState);
    }
    
    mAnimationStates.clear();
    mAnimationsToKeys.clear();
    mTransitions.clear();
    mAnimationBindings.clear();
  }

  /**
   * Called to signal that a new layout is being mounted that may require transition animations: the
   * specification for these animations is provided on the given {@link TransitionContext}.
   */
  private void prepareTransitions(TransitionContext transitionContext) {
    mAnimationBindings.clear();
    mTransitions.clear();
    mTransitions.addAll(transitionContext.getTransitionSet().getTransitions());

    if (AnimationsDebug.ENABLED) {
      Log.d(
          AnimationsDebug.TAG,
          "Got new TransitionContext with " + mTransitions.size() + " transitions");
    }

    for (int i = 0, size = mAnimationStates.size(); i < size; i++) {
      final AnimationState animationState = mAnimationStates.valueAt(i);
      animationState.currentDiff.reset();
    }
  }

  /**
   * Called to record the current/next content for a transition key.
   *
   * @param currentLayoutOutput the current LayoutOutput for this key, or null if the key is
   * appearing
   * @param nextLayoutOutput the new LayoutOutput for this key, or null if the key is disappearing
   */
  private void recordLayoutOutputDiff(
      String transitionKey,
      LayoutOutput currentLayoutOutput,
      LayoutOutput nextLayoutOutput) {
    AnimationState animationState = mAnimationStates.get(transitionKey);
    if (animationState == null) {
      animationState = new AnimationState();
      mAnimationStates.put(transitionKey, animationState);
    }

    if (currentLayoutOutput == null && nextLayoutOutput == null) {
      throw new RuntimeException("Both current and next LayoutOutputs were null!");
    }

    if (currentLayoutOutput == null && nextLayoutOutput != null) {
      animationState.changeType = KeyStatus.APPEARED;
    } else if (currentLayoutOutput != null && nextLayoutOutput != null) {
      animationState.changeType = KeyStatus.CHANGED;
    } else {
      animationState.changeType = KeyStatus.DISAPPEARED;
    }

    animationState.currentLayoutOutput = currentLayoutOutput;
    animationState.nextLayoutOutput = nextLayoutOutput;

    if (animationState.currentLayoutOutput != null) {
      animationState.currentLayoutOutput.incrementRefCount();
    }
    if (animationState.nextLayoutOutput != null) {
      animationState.nextLayoutOutput.incrementRefCount();
    }

    if (AnimationsDebug.ENABLED) {
      Log.d(AnimationsDebug.TAG, "Saw key " + transitionKey + " which is " +
          keyStatusToString(animationState.changeType));
    }
  }

  /**
   * Called after all mount content diffs have been recorded for this transition. Creates all
   * animations that will actually run after the new layout is mounted, but does not yet run the
   * transitions.
   *
   * After this point, MountState can use {@link #isKeyAnimating} and {@link #isKeyDisappearing} to
   * check whether certain mount content will animate.
   */
  private void commitLayoutOutputDiffs() {
    createTransitionAnimations();

    // Fill the before and after values of the diff for each animating property.
    //
    // The before value comes from the "before" LayoutOutput for this transition key, unless the key
    // is appearing in which case an "appearFrom" value is resolved. Similarly, the after value
    // comes from the "after" LayoutOutput, unless the key is disappearing in which case a
    // "disappearTo" value is resolved.
    recordAllTransitioningProperties();
    fillAppearFromValues();
    fillDisappearToValues();

    // If we recorded any mount content diffs that didn't result in an animation being created for
    // that transition key, clean them up now.
    cleanupNonAnimatingAnimationStates();
  }

  private void createTransitionAnimations() {
    for (int i = 0, size = mTransitions.size(); i < size; i++) {
      final Transition transition = mTransitions.get(i);
      final String key = transition.getTransitionKey();
      final AnimationState animationState = mAnimationStates.get(key);

      if (AnimationsDebug.ENABLED) {
        final String propName = transition.getAnimatedProperty().getName();
        Log.d(
            AnimationsDebug.TAG,
            "Creating animation for key " + key + "#" + propName + ":");
      }

      if (animationState == null) {
        if (AnimationsDebug.ENABLED) {
          Log.d(
              AnimationsDebug.TAG,
              " - this key was not seen in the before/after layout state");
        }
        continue;
      }

      AnimationBinding animation = null;
      if (animationState.changeType == KeyStatus.CHANGED) {
        animation = transition.createChangeAnimation();
      } else if (animationState.changeType == KeyStatus.DISAPPEARED) {
        if (transition.hasDisappearAnimation()) {
          animation = transition.createDisappearAnimation();
        }
      } else if (animationState.changeType == KeyStatus.APPEARED) {
        if (transition.hasAppearAnimation()) {
          animation = transition.createAppearAnimation();
        }
      }

      if (animation != null) {
        mAnimationBindings.add(animation);
      }

      if (AnimationsDebug.ENABLED) {
        final String changeType = keyStatusToString(animationState.changeType);
        if (animation != null) {
          Log.d(AnimationsDebug.TAG, " - created " + changeType + " animation");
        } else {
          Log.d(
              AnimationsDebug.TAG,
              " - did not find matching transition for status " + changeType);
        }
      }
    }
  }

  /**
   * This method uses the set of mount content that is appearing/changing/disappearing and the
   * corresponding calculated animations from {@link #createTransitionAnimations} to fill the proper
   * before and after values for all the properties that will be animating.
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

        final AnimationState animationState = mAnimationStates.get(key);
        if (animationState == null) {
          continue;
        }
        animationState.animatingProperties.add(animatedProperty);
        animationState.activeAnimations.add(binding);

        if (animationState.changeType != KeyStatus.APPEARED) {
          final float currentValue = getCurrentValue(animationState, animatedProperty);

          if (animationState.currentDiff.beforeValues.put(animatedProperty, currentValue) != null) {
            throw new RuntimeException("TransitionDiff wasn't cleared properly!");
          }
        }

        if (animationState.changeType != KeyStatus.DISAPPEARED) {
          final Float previousValue = animationState.currentDiff.afterValues.put(
              animatedProperty,
              animatedProperty.get(animationState.nextLayoutOutput));
          if (previousValue != null) {
            throw new RuntimeException("TransitionDiff wasn't cleared properly!");
          }
        }
      }
      transitioningProperties.clear();
    }
    ComponentsPools.release(transitioningProperties);
  }

  /**
   * Resolves the values that appearing mount content properties will appear from and sets those
   * values in the currentDiff for the corresponding transition key.
   */
  private void fillAppearFromValues() {
    SimpleArrayMap<ComponentProperty, RuntimeValue> appearFromValues = new SimpleArrayMap<>();
    for (int i = 0, size = mAnimationBindings.size(); i < size; i++) {
      final AnimationBinding binding = mAnimationBindings.get(i);
      binding.collectAppearFromValues(appearFromValues);
    }

    for (int i = 0, size = appearFromValues.size(); i < size; i++) {
      final ComponentProperty property = appearFromValues.keyAt(i);
      final RuntimeValue runtimeValue = appearFromValues.valueAt(i);
      final AnimationState animationState = mAnimationStates.get(property.getTransitionKey());
      final AnimatedPropertyNode node = animationState.animatedPropertyNodes.get(
          property.getProperty());

      // If this key is already animating, use its current value, otherwise resolve the appearFrom
      // value
      float value;
      if (node != null) {
        value = node.getValue();
      } else {
        value = runtimeValue.resolve(mResolver, property);
      }

      if (animationState.changeType != KeyStatus.APPEARED) {
        throw new RuntimeException(
            "Wrong transition type for appear of key " + property.getTransitionKey() + ": " +
                keyStatusToString(animationState.changeType));
      }
      animationState.currentDiff.beforeValues.put(property.getProperty(), value);
    }
  }

  /**
   * Resolves the values that disappearing mount content properties will disappear to and sets those
   * values in the currentDiff for the corresponding transition key.
   */
  private void fillDisappearToValues() {
    SimpleArrayMap<ComponentProperty, RuntimeValue> disappearToValues = new SimpleArrayMap<>();
    for (int i = 0, size = mAnimationBindings.size(); i < size; i++) {
      final AnimationBinding binding = mAnimationBindings.get(i);
      binding.collectDisappearToValues(disappearToValues);
    }

    for (int i = 0, size = disappearToValues.size(); i < size; i++) {
      final ComponentProperty property = disappearToValues.keyAt(i);
      final RuntimeValue runtimeValue = disappearToValues.valueAt(i);
      final AnimationState animationState = mAnimationStates.get(property.getTransitionKey());
      if (animationState.changeType != KeyStatus.DISAPPEARED) {
        throw new RuntimeException(
            "Wrong transition type for disappear of key " + property.getTransitionKey() + ": " +
                keyStatusToString(animationState.changeType));
      }
      final float value = runtimeValue.resolve(mResolver, property);
      if (animationState.currentDiff.afterValues.put(property.getProperty(), value) != null) {
        throw new RuntimeException("TransitionDiff wasn't cleared properly!");
      }
    }
  }

  private void restoreInitialStates() {
    for (int i = 0, size = mAnimationStates.size(); i < size; i++) {
      final String transitionKey = mAnimationStates.keyAt(i);
      final AnimationState animationState = mAnimationStates.valueAt(i);
      // If the component is appearing, we will instead restore the initial value in
      // fillAppearFromValues. This is necessary since appearFrom values can be written in terms of
      // the end state (e.g. appear from an offset of -10dp)
      if (animationState.changeType != KeyStatus.APPEARED) {
        for (int j = 0; j < animationState.currentDiff.beforeValues.size(); j++) {
          final AnimatedProperty property = animationState.currentDiff.beforeValues.keyAt(j);
          property.set(
              animationState.mountContent,
              animationState.currentDiff.beforeValues.valueAt(j));
        }
      }
    }
  }

  private float getCurrentValue(AnimationState animationState, AnimatedProperty animatedProperty) {
    final AnimatedPropertyNode animatedNode = animationState.animatedPropertyNodes.get(
        animatedProperty);
    if (animatedNode != null) {
      return animatedNode.getValue();
    }

    // Try to use the before value, but for appearing animations, use the end state since there is
    // no before state.
    final LayoutOutput layoutOutputToCheck = animationState.changeType == KeyStatus.APPEARED ?
        animationState.nextLayoutOutput :
        animationState.currentLayoutOutput;
    if (layoutOutputToCheck == null) {
      throw new RuntimeException("Both LayoutOutputs were null!");
    }

    return animatedProperty.get(layoutOutputToCheck);
  }

  private AnimatedPropertyNode getOrCreateAnimatedPropertyNode(
      String key,
      AnimatedProperty animatedProperty) {
    final AnimationState state = mAnimationStates.get(key);
    AnimatedPropertyNode node = state.animatedPropertyNodes.get(animatedProperty);
    if (node == null) {
      node = new AnimatedPropertyNode(state.mountContent, animatedProperty);
      state.animatedPropertyNodes.put(animatedProperty, node);
    }
    return node;
  }

  private void setMountContentInner(
      String key,
      AnimationState animationState,
      Object mountContent) {
    // If the mount content changes, this means this transition key will be rendered with a
    // different mount content (View or Drawable) than it was during the last mount, so we need to
    // migrate animation state from the old mount content to the new one.

    if (animationState.mountContent == mountContent) {
      return;
    }

    if (AnimationsDebug.ENABLED) {
      Log.d(AnimationsDebug.TAG, "Setting mount content for " + key + " to " + mountContent);
    }

    if (animationState.mountContent != null) {
      final ArraySet<AnimatedProperty> animatingProperties = animationState.animatingProperties;
      for (int i = 0, size = animatingProperties.size(); i < size; i++) {
        animatingProperties.valueAt(i).reset(animationState.mountContent);
      }
      recursivelySetChildClipping(animationState.mountContent, true);
    }
    for (int i = 0, size = animationState.animatedPropertyNodes.size(); i < size; i++) {
      animationState.animatedPropertyNodes.valueAt(i).setMountContent(mountContent);
    }
    recursivelySetChildClipping(mountContent, false);
    animationState.mountContent = mountContent;
  }

  /**
   * Set the clipChildren properties to all Views in the same tree branch from the given one, up to
   * the top LithoView.
   *
   * TODO(17934271): Handle the case where two+ animations with different lifespans share the same
   * parent, in which case we shouldn't unset clipping until the last item is done animating.
   */
  private void recursivelySetChildClipping(Object mountContent, boolean clipChildren) {
    if (!(mountContent instanceof View)) {
      return;
    }

    recursivelySetChildClippingForView((View) mountContent, clipChildren);
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

  /**
   * Removes any AnimationStates that were created in {@link #recordLayoutOutputDiff} but never
   * resulted in an animation being created.
   */
  private void cleanupNonAnimatingAnimationStates() {
    for (int i = mAnimationStates.size() - 1; i >= 0; i--) {
      final AnimationState animationState = mAnimationStates.valueAt(i);
      if (animationState.activeAnimations.isEmpty()) {
        setMountContentInner(mAnimationStates.keyAt(i), animationState, null);
        clearLayoutOutputs(mAnimationStates.removeAt(i));
      }
    }
  }

  private void cleanupLayoutOutputs() {
    for (int i = 0; i < mAnimationStates.size(); i++) {
      final AnimationState animationState = mAnimationStates.valueAt(i);
      clearLayoutOutputs(animationState);
    }
  }

  private void debugLogStartingAnimations() {
    if (!AnimationsDebug.ENABLED) {
      throw new RuntimeException("Trying to debug log animations without debug flag set!");
    }

    Log.d(TAG, "Starting animations:");

    final ArraySet<ComponentProperty> transitioningProperties = new ArraySet<>();
    for (int i = 0, size = mAnimationBindings.size(); i < size; i++) {
      final AnimationBinding binding = mAnimationBindings.get(i);

      binding.collectTransitioningProperties(transitioningProperties);

      for (int j = 0, propSize = transitioningProperties.size(); j < propSize; j++) {
        final ComponentProperty property = transitioningProperties.valueAt(j);
        final String key = property.getTransitionKey();
        final AnimatedProperty animatedProperty = property.getProperty();
        final AnimationState animationState = mAnimationStates.get(key);
        final float beforeValue = animationState.currentDiff.beforeValues.get(animatedProperty);
        final float afterValue = animationState.currentDiff.afterValues.get(animatedProperty);
        final String changeType = keyStatusToString(animationState.changeType);

        Log.d(
            TAG,
            " - " + key + "." + animatedProperty.getName() + " will animate from " + beforeValue +
                " to " + afterValue + " (" + changeType + ")");
      }
      transitioningProperties.clear();
    }
  }

  private static String keyStatusToString(int keyStatus) {
    switch (keyStatus) {
      case KeyStatus.APPEARED:
        return "APPEARED";
      case KeyStatus.CHANGED:
        return "CHANGED";
      case KeyStatus.DISAPPEARED:
        return "DISAPPEARED";
      case KeyStatus.UNSET:
        return "UNSET";
      default:
        throw new RuntimeException("Unknown keyStatus: " + keyStatus);
    }
  }

  private static void clearLayoutOutputs(AnimationState animationState) {
    if (animationState.currentLayoutOutput != null) {
      animationState.currentLayoutOutput.decrementRefCount();
      animationState.currentLayoutOutput = null;
    }
    if (animationState.nextLayoutOutput != null) {
      animationState.nextLayoutOutput.decrementRefCount();
      animationState.nextLayoutOutput = null;
    }
  }

  private class TransitionsAnimationBindingListener implements AnimationBindingListener {

    @Override
    public void onWillStart(AnimationBinding binding) {
    }

    @Override
    public void onFinish(AnimationBinding binding) {
      final ArraySet<String> transitioningKeys = mAnimationsToKeys.remove(binding);
      if (transitioningKeys == null) {
        return;
      }

      // When an animation finishes, we want to go through all the mount contents it was animating
      // and see if it was the last active animation. If it was, we know that item is no longer
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
          if (animationState.changeType == KeyStatus.DISAPPEARED &&
              animationState.mountContent != null) {
            for (int j = 0; j < animationState.animatingProperties.size(); j++) {
              animationState.animatingProperties.valueAt(j).reset(animationState.mountContent);
            }
          }
          recursivelySetChildClipping(animationState.mountContent, true);
          if (mOnAnimationCompleteListener != null) {
            mOnAnimationCompleteListener.onAnimationComplete(key);
          }
          mAnimationStates.remove(key);
          clearLayoutOutputs(animationState);
        }
      }
      ComponentsPools.release(transitioningKeys);
    }
  }

  private class TransitionsResolver implements Resolver {

    @Override
    public float getCurrentState(ComponentProperty property) {
      final AnimationState animationState = mAnimationStates.get(property.getTransitionKey());

      // We may already have an explicit beginning state for this property from
      // recordAllTransitioningProperties or setAppearFromValues, in which case we should return it.
      //
      // Otherwise, it may be a property that isn't animating, but is used to calculate an appear-
      // from value (e.g. the width for DimensionValue#widthPercentageOffset) in which case we
      // should just grab it off the LayoutOutput.
      Float explicitValue = animationState.currentDiff.beforeValues.get(property.getProperty());
      if (explicitValue != null) {
        return explicitValue;
      }

      return getCurrentValue(animationState, property.getProperty());
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
