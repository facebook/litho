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

package com.facebook.litho;

import static com.facebook.litho.AnimationsDebug.TAG;

import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v4.util.SparseArrayCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewParent;
import com.facebook.litho.Transition.TransitionUnit;
import com.facebook.litho.animation.AnimatedProperties;
import com.facebook.litho.animation.AnimatedProperty;
import com.facebook.litho.animation.AnimatedPropertyNode;
import com.facebook.litho.animation.AnimationBinding;
import com.facebook.litho.animation.AnimationBindingListener;
import com.facebook.litho.animation.ParallelBinding;
import com.facebook.litho.animation.PropertyAnimation;
import com.facebook.litho.animation.PropertyHandle;
import com.facebook.litho.animation.Resolver;
import com.facebook.litho.internal.ArraySet;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Handles animating transitions defined by ComponentSpec's onCreateTransition code.
 *
 * USAGE FROM MOUNTSTATE
 *
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
 * TECHNICAL DETAILS
 *
 * - Transition keys are 1-1 mapped to AnimationState
 * - An {@link AnimationState} has many {@link PropertyState}s (one for each property)
 * - A {@link PropertyState} can have up to one animation.
 *
 * An {@link AnimationState} keeps track of the current mount content object, as well as the state
 * of all animating properties ({@link PropertyState}s). A {@link PropertyState} keeps track of a
 * {@link AnimatedPropertyNode}, which has the current value of that property in the animation, and
 * up to one animation and end value. A reverse mapping from animation to property(s) being animated
 * is tracked in {@link #mAnimationsToPropertyHandles}.
 *
 * Combined, these mean that at any point in time, we're able to tell what animation is animating
 * what property(s). Knowing this, we can properly resolve conflicting animations (animations on the
 * same property of the same mount content).
 *
 * Another important note: sometimes we need to keep values set on properties before or after
 * animations.
 *
 * Examples include an appearFrom value for an animation that starts later in a sequence of
 * animations (in that case, the appearFrom value must be immediately applied even if the animation
 * isn't starting until later), and keeping disappearTo values even after an animation has completed
 * (e.g., consider animating alpha and X position: if the alpha animation finishes first, we still
 * need to keep the final value until we can remove the animating content).
 *
 * As such, our rule is that we should have a {@link PropertyState} on the corresponding
 * {@link AnimationState} for any property that has a value no necessarily reflected by the most up
 * to date {@link LayoutOutput} for that transition key in the most recent {@link LayoutState}. Put
 * another way, animation doesn't always imply movement, but a temporary change from a canonical
 * {@link LayoutOutput}.
 */
public class TransitionManager {

  /**
   * Whether a piece of content identified by a transition key is appearing, disappearing, or just
   * possibly changing some properties.
   */
  @IntDef({ChangeType.APPEARED, ChangeType.CHANGED, ChangeType.DISAPPEARED, ChangeType.UNSET})
  @Retention(RetentionPolicy.SOURCE)
  @interface ChangeType {
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
   * The animation state of a single property (e.g. X, Y, ALPHA) on a piece of mount content.
   */
  private static class PropertyState {

    /**
     * The {@link AnimatedPropertyNode} for this property: it contains the current animated value
     * and a way to set a new value.
     */
    public AnimatedPropertyNode animatedPropertyNode;

    /**
     * The animation, if any, that is currently running on this property.
     */
    public AnimationBinding animation;

    /**
     * If there's an {@link #animation}, the target value it's animating to.
     */
    public Float targetValue;

    /**
     * The last mounted value of this property.
     */
    public Float lastMountedValue;

    /**
     * How many animations are waiting to finish for this property.
     */
    public int numPendingAnimations;
  }

  /**
   * Animation state of a given mount content. Holds everything we currently know about an animating
   * transition key, such as whether it's appearing, disappearing, or changing, as well as
   * information about any animating properties on this mount content.
   */
  private static class AnimationState {

    /**
     * The states for all the properties of this mount content that have an animated value (e.g. a
     * value that isn't necessarily their mounted value).
     */
    public final Map<AnimatedProperty, PropertyState> propertyStates = new HashMap<>();

    /**
     * The current mount content for this animation state, if it's mounted, null otherwise. This
     * mount content can change over time.
     */
    public @Nullable OutputUnitsAffinityGroup<Object> mountContentGroup;

    /**
     * Whether the last LayoutState diff that had this content in it showed the content appearing,
     * disappearing or changing.
     */
    public int changeType = ChangeType.UNSET;

    /** While calculating animations, the current (before) LayoutOutput. */
    public @Nullable OutputUnitsAffinityGroup<LayoutOutput> currentLayoutOutputsGroup;

    /** While calculating animations, the next (after) LayoutOutput. */
    public @Nullable OutputUnitsAffinityGroup<LayoutOutput> nextLayoutOutputsGroup;

    /**
     * Whether this transition key was seen in the last transition, either in the current or next
     * {@link LayoutState}s.
     */
    public boolean seenInLastTransition = false;
  }

  private final Map<AnimationBinding, ArraySet<PropertyHandle>> mAnimationsToPropertyHandles =
      new HashMap<>();
  private final Map<String, AnimationState> mAnimationStates = new HashMap<>();
  private final SparseArrayCompat<String> mTraceNames = new SparseArrayCompat<>();
  private final Map<PropertyHandle, Float> mInitialStatesToRestore = new HashMap<>();
  private final ArraySet<AnimationBinding> mRunningRootAnimations = new ArraySet<>();
  private final TransitionsAnimationBindingListener mAnimationBindingListener =
      new TransitionsAnimationBindingListener();
  private final RootAnimationListener mRootAnimationListener = new RootAnimationListener();
  private final TransitionsResolver mResolver = new TransitionsResolver();
  private final OnAnimationCompleteListener mOnAnimationCompleteListener;
  private final MountState mMountState;
  private AnimationBinding mRootAnimationToRun;

  public TransitionManager(
      OnAnimationCompleteListener onAnimationCompleteListener, MountState mountState) {
    mOnAnimationCompleteListener = onAnimationCompleteListener;
    mMountState = mountState;
  }

  /**
   * Creates (but doesn't start) the animations for the next transition based on the current and
   * next layout states.
   *
   * <p>After this is called, MountState can use {@link #isKeyAnimating} and {@link
   * #isKeyDisappearing} to check whether certain mount content will animate, commit the layout
   * changes, and then call {@link #runTransitions} to restore the initial states and run the
   * animations.
   */
  void setupTransitions(
      LayoutState currentLayoutState, LayoutState nextLayoutState, Transition rootTransition) {
    final boolean isTracing = ComponentsSystrace.isTracing();
    if (isTracing) {
      ComponentsSystrace.beginSection("TransitionManager.setupTransition");
    }

    for (AnimationState animationState : mAnimationStates.values()) {
      animationState.seenInLastTransition = false;
    }

    final Map<String, OutputUnitsAffinityGroup<LayoutOutput>> nextTransitionKeys =
        nextLayoutState.getTransitionKeyMapping();
    if (currentLayoutState == null) {
      for (String transitionKey : nextTransitionKeys.keySet()) {
        final OutputUnitsAffinityGroup<LayoutOutput> nextLayoutOutputsGroup =
            nextTransitionKeys.get(transitionKey);
        recordLayoutOutputsGroupDiff(transitionKey, null, nextLayoutOutputsGroup);
      }
    } else {
      final Map<String, OutputUnitsAffinityGroup<LayoutOutput>> currentTransitionKeys =
          currentLayoutState.getTransitionKeyMapping();
      final HashSet<String> seenKeysInNewLayout = new HashSet<>();
      for (String transitionKey : nextTransitionKeys.keySet()) {
        final OutputUnitsAffinityGroup<LayoutOutput> nextLayoutOutputsGroup =
            nextTransitionKeys.get(transitionKey);

        final OutputUnitsAffinityGroup<LayoutOutput> currentLayoutOutputsGroup =
            currentTransitionKeys.get(transitionKey);
        if (currentLayoutOutputsGroup != null) {
          seenKeysInNewLayout.add(transitionKey);
        }

        recordLayoutOutputsGroupDiff(
            transitionKey, currentLayoutOutputsGroup, nextLayoutOutputsGroup);
      }

      for (String transitionKey : currentTransitionKeys.keySet()) {
        if (seenKeysInNewLayout.contains(transitionKey)) {
          continue;
        }
        recordLayoutOutputsGroupDiff(transitionKey, currentTransitionKeys.get(transitionKey), null);
      }
    }

    createTransitionAnimations(rootTransition);

    // If we recorded any mount content diffs that didn't result in an animation being created for
    // that transition key, clean them up now.
    cleanupNonAnimatingAnimationStates();

    if (isTracing) {
      ComponentsSystrace.endSection();
    }
  }

  /**
   * Called after {@link #setupTransitions} has been called and the new layout has been mounted.
   * This restores the state of the previous layout for content that will animate and then starts
   * the corresponding animations.
   */
  void runTransitions() {
    restoreInitialStates();

    if (AnimationsDebug.ENABLED) {
      debugLogStartingAnimations();
    }

    if (mRootAnimationToRun != null) {
      mRootAnimationToRun.addListener(mRootAnimationListener);
      mRootAnimationToRun.start(mResolver);
      mRootAnimationToRun = null;
    }
  }

  void removeMountContent(String transitionKey, @OutputUnitType int type) {
    final AnimationState animationState = mAnimationStates.get(transitionKey);
    if (animationState == null) {
      return;
    }
    final OutputUnitsAffinityGroup<Object> mountContentGroup = animationState.mountContentGroup;
    if (mountContentGroup == null || mountContentGroup.get(type) == null) {
      return;
    }

    OutputUnitsAffinityGroup<Object> updatedMountContentGroup;
    if (mountContentGroup.size() > 1) {
      updatedMountContentGroup = new OutputUnitsAffinityGroup<>(mountContentGroup);
      updatedMountContentGroup.replace(type, null);
    } else {
      // The group is empty now, so just pass null
      updatedMountContentGroup = null;
    }
    setMountContentInner(transitionKey, animationState, updatedMountContentGroup);
  }

  /**
   * Sets the mount content for a given key. This is used to initially set mount content, but also
   * to set content when content is incrementally mounted during an animation.
   */
  void setMountContent(String transitionKey, OutputUnitsAffinityGroup<Object> mountContentGroup) {
    final AnimationState animationState = mAnimationStates.get(transitionKey);
    if (animationState != null) {
      setMountContentInner(transitionKey, animationState, mountContentGroup);
    }
  }

  /**
   * After transitions have been setup with {@link #setupTransitions}, returns whether the given key
   * will be/is animating.
   */
  boolean isKeyAnimating(String key) {
    return mAnimationStates.containsKey(key);
  }

  /**
   * After transitions have been setup with {@link #setupTransitions}, returns whether the given key
   * whether the given key is disappearing.
   */
  boolean isKeyDisappearing(String key) {
    final AnimationState animationState = mAnimationStates.get(key);
    if (animationState == null) {
      return false;
    }
    return animationState.changeType == ChangeType.DISAPPEARED;
  }

  /**
   * To be called when a MountState is recycled for a new component tree. Clears all animations.
   */
  void reset() {
    for (String key : mAnimationStates.keySet()) {
      final AnimationState animationState = mAnimationStates.get(key);
      setMountContentInner(key, animationState, null);
      clearLayoutOutputs(animationState);
    }
    mAnimationStates.clear();
    mTraceNames.clear();

    // Clear these so that stopping animations below doesn't cause us to trigger any useless
    // cleanup.
    mAnimationsToPropertyHandles.clear();

    // Calling stop will cause the animation to be removed from the set, so iterate in reverse
    // order.
    for (int i = mRunningRootAnimations.size() - 1; i >= 0; i--) {
      mRunningRootAnimations.valueAt(i).stop();
    }
    mRunningRootAnimations.clear();

    mRootAnimationToRun = null;
  }

  /**
   * Called to record the current/next content for a transition key.
   *
   * @param currentLayoutOutputsGroup the current group of LayoutOutputs for this key, or null if
   *     the key is appearing
   * @param nextLayoutOutputsGroup the new group of LayoutOutput for this key, or null if the key is
   *     disappearing
   */
  private void recordLayoutOutputsGroupDiff(
      String transitionKey,
      OutputUnitsAffinityGroup<LayoutOutput> currentLayoutOutputsGroup,
      OutputUnitsAffinityGroup<LayoutOutput> nextLayoutOutputsGroup) {
    AnimationState animationState = mAnimationStates.get(transitionKey);
    if (animationState == null) {
      animationState = new AnimationState();
      mAnimationStates.put(transitionKey, animationState);
    }

    if (currentLayoutOutputsGroup == null && nextLayoutOutputsGroup == null) {
      throw new RuntimeException("Both current and next LayoutOutput groups were null!");
    }

    if (currentLayoutOutputsGroup == null && nextLayoutOutputsGroup != null) {
      animationState.changeType = ChangeType.APPEARED;
    } else if (currentLayoutOutputsGroup != null && nextLayoutOutputsGroup != null) {
      animationState.changeType = ChangeType.CHANGED;
    } else {
      animationState.changeType = ChangeType.DISAPPEARED;
    }

    if (currentLayoutOutputsGroup != null) {
      acquireRef(currentLayoutOutputsGroup);
    }
    animationState.currentLayoutOutputsGroup = currentLayoutOutputsGroup;

    if (nextLayoutOutputsGroup != null) {
      acquireRef(nextLayoutOutputsGroup);
    }
    animationState.nextLayoutOutputsGroup = nextLayoutOutputsGroup;

    recordLastMountedValues(animationState);

    animationState.seenInLastTransition = true;

    if (AnimationsDebug.ENABLED) {
      Log.d(
          AnimationsDebug.TAG,
          "Saw key " + transitionKey + " which is " +
              changeTypeToString(animationState.changeType));
    }
  }

  private static void acquireRef(OutputUnitsAffinityGroup<LayoutOutput> group) {
    for (int i = 0, size = group.size(); i < size; i++) {
      group.getAt(i).acquireRef();
    }
  }

  private void recordLastMountedValues(AnimationState animationState) {
    final LayoutOutput layoutOutput =
        animationState.nextLayoutOutputsGroup != null
            ? animationState.nextLayoutOutputsGroup.getMostSignificantUnit()
            : null;
    // The values for all members of the group should be the same, thus we'll be collected from the
    // most significant one
    for (AnimatedProperty property : animationState.propertyStates.keySet()) {
      final PropertyState propertyState = animationState.propertyStates.get(property);
      if (layoutOutput == null) {
        propertyState.lastMountedValue = null;
      } else {
        propertyState.lastMountedValue = property.get(layoutOutput);
      }
    }
  }

  @Nullable
  static Transition getRootTransition(List<Transition> allTransitions) {
    if (allTransitions.isEmpty()) {
      return null;
    }

    if (allTransitions.size() == 1) {
      return allTransitions.get(0);
    }

    return new ParallelTransitionSet(allTransitions);
  }

  private void createTransitionAnimations(Transition rootTransition) {
    mRootAnimationToRun = createAnimationsForTransition(rootTransition);
  }

  private AnimationBinding createAnimationsForTransition(Transition transition) {
    if (transition instanceof TransitionUnit) {
      return createAnimationsForTransitionUnit((TransitionUnit) transition);
    } else if (transition instanceof TransitionSet) {
      return createAnimationsForTransitionSet((TransitionSet) transition);
    } else {
      throw new RuntimeException("Unhandled Transition type: " + transition);
    }
  }

  private AnimationBinding createAnimationsForTransitionSet(TransitionSet transitionSet) {
    final ArrayList<Transition> children = transitionSet.getChildren();
    final ArrayList<AnimationBinding> createdAnimations = new ArrayList<>();
    for (int i = 0, size = children.size(); i < size; i++) {
      final AnimationBinding animation = createAnimationsForTransition(children.get(i));
      if (animation != null) {
        createdAnimations.add(animation);
      }
    }

    if (createdAnimations.isEmpty()) {
      return null;
    }

    return transitionSet.createAnimation(createdAnimations);
  }

  private AnimationBinding createAnimationsForTransitionUnit(TransitionUnit transition) {
    final Transition.AnimationTarget animationTarget = transition.getAnimationTarget();
    final ArrayList<AnimationBinding> createdAnimations = new ArrayList<>();
    switch (animationTarget.componentTarget.componentTargetType) {
      case ALL:
        createAnimationsForTransitionUnitAllKeys(transition, createdAnimations);
        break;
      case AUTO_LAYOUT:
        // TODO T25723456. As of now targeting components which have transition keys. Later we'll
        // move on to remove that constraint (Step 2).
        createAnimationsForTransitionUnitAllKeys(transition, createdAnimations);
        break;
      case SET:
        final String[] keys = (String[]) animationTarget.componentTarget.componentTargetExtraData;
        for (int j = 0; j < keys.length; j++) {
          createAnimationsForTransitionUnit(transition, keys[j], createdAnimations);
        }
        break;
      case SINGLE:
        createAnimationsForTransitionUnit(
            transition,
            (String) animationTarget.componentTarget.componentTargetExtraData,
            createdAnimations);
        break;
    }

    if (createdAnimations.isEmpty()) {
      return null;
    }

    if (createdAnimations.size() == 1) {
      return createdAnimations.get(0);
    }

    return new ParallelBinding(0, createdAnimations);
  }

  private void createAnimationsForTransitionUnitAllKeys(
      TransitionUnit transition,
      ArrayList<AnimationBinding> outList) {
    for (String transitionKey : mAnimationStates.keySet()) {
      final AnimationState animationState = mAnimationStates.get(transitionKey);
      if (!animationState.seenInLastTransition) {
        continue;
      }
      createAnimationsForTransitionUnit(transition, transitionKey, outList);
    }
  }

  private void createAnimationsForTransitionUnit(
      TransitionUnit transition, String key,
      ArrayList<AnimationBinding> outList) {
    final Transition.AnimationTarget animationTarget = transition.getAnimationTarget();
    switch (animationTarget.propertyTarget.propertyTargetType) {
      case AUTO_LAYOUT:
        for (int i = 0; i < AnimatedProperties.AUTO_LAYOUT_PROPERTIES.length; i++) {
          final AnimationBinding createdAnimation =
              maybeCreateAnimation(transition, key, AnimatedProperties.AUTO_LAYOUT_PROPERTIES[i]);
          if (createdAnimation != null) {
            outList.add(createdAnimation);
          }
        }
        break;
      case SET:
        final AnimatedProperty[] properties =
            (AnimatedProperty[]) animationTarget.propertyTarget.propertyTargetExtraData;
        for (int i = 0; i < properties.length; i++) {
          final AnimationBinding createdAnimation =
              maybeCreateAnimation(transition, key, properties[i]);
          if (createdAnimation != null) {
            outList.add(createdAnimation);
          }
        }
        break;
      case SINGLE:
        final AnimationBinding createdAnimation =
            maybeCreateAnimation(
                transition,
                key,
                (AnimatedProperty) animationTarget.propertyTarget.propertyTargetExtraData);
        if (createdAnimation != null) {
          outList.add(createdAnimation);
        }
        break;
    }
  }

  private @Nullable AnimationBinding maybeCreateAnimation(
      TransitionUnit transition, String key, AnimatedProperty property) {
    final AnimationState animationState = mAnimationStates.get(key);

    if (AnimationsDebug.ENABLED) {
      Log.d(
          AnimationsDebug.TAG,
          "Calculating transitions for " + key + "#" + property.getName() + ":");
    }

    if (animationState == null
        || (animationState.currentLayoutOutputsGroup == null
            && animationState.nextLayoutOutputsGroup == null)) {
      if (AnimationsDebug.ENABLED) {
        Log.d(AnimationsDebug.TAG, " - this key was not seen in the before/after layout state");
      }
      return null;
    }

    final int changeType = animationState.changeType;
    final String changeTypeString = changeTypeToString(animationState.changeType);
    if ((changeType == ChangeType.APPEARED && !transition.hasAppearAnimation()) ||
        (changeType == ChangeType.DISAPPEARED && !transition.hasDisappearAnimation())) {
      if (AnimationsDebug.ENABLED) {
        Log.d(
            AnimationsDebug.TAG,
            " - did not find matching transition for change type " + changeTypeString);
      }
      return null;
    }

    final PropertyState existingState = animationState.propertyStates.get(property);
    final PropertyHandle propertyHandle = new PropertyHandle(key, property);
    final float startValue;
    if (existingState != null) {
      startValue = existingState.animatedPropertyNode.getValue();
    } else {
      if (animationState.changeType != ChangeType.APPEARED) {
        startValue =
            property.get(animationState.currentLayoutOutputsGroup.getMostSignificantUnit());
      } else {
        startValue = transition.getAppearFrom().resolve(mResolver, propertyHandle);
      }
    }

    final float endValue;
    if (animationState.changeType != ChangeType.DISAPPEARED) {
      endValue = property.get(animationState.nextLayoutOutputsGroup.getMostSignificantUnit());
    } else {
      endValue = transition.getDisappearTo().resolve(mResolver, propertyHandle);
    }

    // Don't replace new animations in two cases: 1) we're already animating that property to
    // the same end value or 2) the start and end values are already the same
    if (existingState != null && existingState.targetValue != null) {
      if (endValue == existingState.targetValue) {
        if (AnimationsDebug.ENABLED) {
          Log.d(
              AnimationsDebug.TAG,
              " - property is already animating to this end value: " + endValue);
        }
        return null;
      }
    } else if (startValue == endValue) {
      if (AnimationsDebug.ENABLED) {
        Log.d(
            AnimationsDebug.TAG,
            " - the start and end values were the same: " + startValue + " = " + endValue);
      }
      return null;
    }

    if (AnimationsDebug.ENABLED) {
      Log.d(AnimationsDebug.TAG, " - created animation");
    }

    final AnimationBinding animation = transition.createAnimation(propertyHandle, endValue);
    animation.addListener(mAnimationBindingListener);

    PropertyState propertyState = existingState;
    if (propertyState == null) {
      propertyState = new PropertyState();

      propertyState.animatedPropertyNode =
          new AnimatedPropertyNode(animationState.mountContentGroup, property);
      animationState.propertyStates.put(property, propertyState);
    }
    propertyState.animatedPropertyNode.setValue(startValue);
    propertyState.numPendingAnimations++;

    // Currently, all supported animations can only animate one property at a time, but we think
    // this will change in the future so we maintain a set here.
    final ArraySet<PropertyHandle> animatedPropertyHandles = new ArraySet<>();
    animatedPropertyHandles.add(propertyHandle);
    mAnimationsToPropertyHandles.put(animation, animatedPropertyHandles);

    mInitialStatesToRestore.put(propertyHandle, startValue);

    if (!TextUtils.isEmpty(transition.getTraceName())) {
      mTraceNames.put(animation.hashCode(), transition.getTraceName());
    }

    return animation;
  }

  private void restoreInitialStates() {
    for (PropertyHandle propertyHandle : mInitialStatesToRestore.keySet()) {
      final float value = mInitialStatesToRestore.get(propertyHandle);
      final AnimationState animationState = mAnimationStates.get(propertyHandle.getTransitionKey());
      if (animationState.mountContentGroup != null) {
        setPropertyValue(propertyHandle.getProperty(), value, animationState.mountContentGroup);
      }
    }

    mInitialStatesToRestore.clear();
  }

  private void setMountContentInner(
      String key,
      AnimationState animationState,
      OutputUnitsAffinityGroup<Object> newMountContentGroup) {
    // If the mount content changes, this means this transition key will be rendered with a
    // different mount content (View or Drawable) than it was during the last mount, so we need to
    // migrate animation state from the old mount content to the new one.
    final OutputUnitsAffinityGroup<Object> mountContentGroup = animationState.mountContentGroup;
    if ((mountContentGroup == null && newMountContentGroup == null)
        || (mountContentGroup != null && mountContentGroup.equals(newMountContentGroup))) {
      return;
    }

    if (AnimationsDebug.ENABLED) {
      Log.d(
          AnimationsDebug.TAG, "Setting mount content for " + key + " to " + newMountContentGroup);
    }

    final Map<AnimatedProperty, PropertyState> animatingProperties = animationState.propertyStates;
    if (animationState.mountContentGroup != null) {
      for (AnimatedProperty animatedProperty : animatingProperties.keySet()) {
        resetProperty(animatedProperty, animationState.mountContentGroup);
      }
      recursivelySetChildClippingForGroup(animationState.mountContentGroup, true);
    }

    for (PropertyState propertyState : animatingProperties.values()) {
      propertyState.animatedPropertyNode.setMountContentGroup(newMountContentGroup);
    }
    if (newMountContentGroup != null) {
      recursivelySetChildClippingForGroup(newMountContentGroup, false);
    }
    animationState.mountContentGroup = newMountContentGroup;
  }

  private void recursivelySetChildClippingForGroup(
      OutputUnitsAffinityGroup<Object> mountContentGroup, boolean clipChildren) {
    // We only need to set clipping to view containers (OutputUnitType.HOST)
    recursivelySetChildClipping(mountContentGroup.get(OutputUnitType.HOST), clipChildren);
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
   * Removes any AnimationStates that were created in {@link #recordLayoutOutputsGroupDiff} but
   * never resulted in an animation being created.
   */
  private void cleanupNonAnimatingAnimationStates() {
    final Iterator<String> transitionKeyIterator = mAnimationStates.keySet().iterator();
    while (transitionKeyIterator.hasNext()) {
      final String transitionKey = transitionKeyIterator.next();
      final AnimationState animationState = mAnimationStates.get(transitionKey);
      if (animationState.propertyStates.isEmpty()) {
        setMountContentInner(transitionKey, animationState, null);
        transitionKeyIterator.remove();
        clearLayoutOutputs(animationState);
      }
    }
  }

  private void debugLogStartingAnimations() {
    if (!AnimationsDebug.ENABLED) {
      throw new RuntimeException("Trying to debug log animations without debug flag set!");
    }

    Log.d(TAG, "Starting animations:");

    // TODO(t20726089): Restore introspection of animations
  }

  private static String changeTypeToString(int changeType) {
    switch (changeType) {
      case ChangeType.APPEARED:
        return "APPEARED";
      case ChangeType.CHANGED:
        return "CHANGED";
      case ChangeType.DISAPPEARED:
        return "DISAPPEARED";
      case ChangeType.UNSET:
        return "UNSET";
      default:
        throw new RuntimeException("Unknown changeType: " + changeType);
    }
  }

  private static void clearLayoutOutputs(AnimationState animationState) {
    if (animationState.currentLayoutOutputsGroup != null) {
      release(animationState.currentLayoutOutputsGroup);
      animationState.currentLayoutOutputsGroup = null;
    }
    if (animationState.nextLayoutOutputsGroup != null) {
      release(animationState.nextLayoutOutputsGroup);
      animationState.nextLayoutOutputsGroup = null;
    }
  }

  private static void release(OutputUnitsAffinityGroup<LayoutOutput> group) {
    for (int i = 0, size = group.size(); i < size; i++) {
      group.getAt(i).release();
    }
  }

  private static float getPropertyValue(
      AnimatedProperty property, OutputUnitsAffinityGroup<LayoutOutput> mountContentGroup) {
    return property.get(mountContentGroup.getMostSignificantUnit());
  }

  private static void setPropertyValue(
      AnimatedProperty property, float value, OutputUnitsAffinityGroup<Object> mountContentGroup) {
    for (int i = 0, size = mountContentGroup.size(); i < size; i++) {
      property.set(mountContentGroup.getAt(i), value);
    }
  }

  private static void resetProperty(
      AnimatedProperty property, OutputUnitsAffinityGroup<Object> mountContentGroup) {
    for (int i = 0, size = mountContentGroup.size(); i < size; i++) {
      property.reset(mountContentGroup.getAt(i));
    }
  }

  private class TransitionsAnimationBindingListener implements AnimationBindingListener {

    private final ArrayList<PropertyAnimation> mTempPropertyAnimations = new ArrayList<>();

    @Override
    public void onScheduledToStartLater(AnimationBinding binding) {
      updateAnimationStates(binding);
    }

    @Override
    public void onWillStart(AnimationBinding binding) {
      updateAnimationStates(binding);

      final String traceName = mTraceNames.get(binding.hashCode());
      if (!TextUtils.isEmpty(traceName)) {
        ComponentsSystrace.beginSectionAsync(traceName, binding.hashCode());
      }
    }

    @Override
    public void onFinish(AnimationBinding binding) {
      finishAnimation(binding);
    }

    @Override
    public void onCanceledBeforeStart(AnimationBinding binding) {
      finishAnimation(binding);
    }

    @Override
    public boolean shouldStart(AnimationBinding binding) {
      binding.collectTransitioningProperties(mTempPropertyAnimations);

      boolean shouldStart = true;

      // Make sure that all animating properties will animate to a valid position
      for (int i = 0, size = mTempPropertyAnimations.size(); i < size; i++) {
        final PropertyAnimation propertyAnimation = mTempPropertyAnimations.get(i);
        final String key = propertyAnimation.getTransitionKey();
        final AnimationState animationState = mAnimationStates.get(key);
        final PropertyState propertyState =
            animationState.propertyStates.get(propertyAnimation.getProperty());

        if (AnimationsDebug.ENABLED) {
          Log.d(
              AnimationsDebug.TAG,
              "Trying to start animation on " + key + "#" +
                  propertyAnimation.getProperty().getName() + " to " +
                  propertyAnimation.getTargetValue() + ":");
        }

        if (propertyState.lastMountedValue != null &&
            propertyState.lastMountedValue != propertyAnimation.getTargetValue()) {
          if (AnimationsDebug.ENABLED) {
            Log.d(
                AnimationsDebug.TAG,
                " - Canceling animation, last mounted value does not equal animation target: " +
                    propertyState.lastMountedValue + " != " + propertyAnimation.getTargetValue());

          }

          shouldStart = false;
        }
      }

      mTempPropertyAnimations.clear();
      return shouldStart;
    }

    private void updateAnimationStates(AnimationBinding binding) {
      binding.collectTransitioningProperties(mTempPropertyAnimations);

      for (int i = 0, size = mTempPropertyAnimations.size(); i < size; i++) {
        final PropertyAnimation propertyAnimation = mTempPropertyAnimations.get(i);
        final String key = propertyAnimation.getTransitionKey();
        final AnimationState animationState = mAnimationStates.get(key);
        final PropertyState propertyState =
            animationState.propertyStates.get(propertyAnimation.getProperty());

        propertyState.targetValue = propertyAnimation.getTargetValue();
        propertyState.animation = binding;
      }

      mTempPropertyAnimations.clear();
    }

    private void finishAnimation(AnimationBinding binding) {
      final ArraySet<PropertyHandle> keys = mAnimationsToPropertyHandles.remove(binding);
      if (keys == null) {
        return;
      }

      // When an animation finishes, we want to go through all the mount contents it was animating
      // and see if it was the last active animation. If it was, we know that item is no longer
      // animating and we can release the animation state.
      for (int i = 0, size = keys.size(); i < size; i++) {
        final PropertyHandle propertyHandle = keys.valueAt(i);
        final String key = propertyHandle.getTransitionKey();
        final AnimatedProperty property = propertyHandle.getProperty();
        final AnimationState animationState = mAnimationStates.get(key);
        final boolean isDisappearAnimation = animationState.changeType == ChangeType.DISAPPEARED;

        // Disappearing animations are treated differently because we want to keep their animated
        // value up until the point that all animations have finished and we can remove the
        // disappearing content (disappearing items disappear to a value that is based on a provided
        // disappearTo value and not a LayoutOutput, so we can't regenerate it).
        //
        // For non-disappearing content, we know the end value is already reflected by the
        // LayoutOutput we transitioned to, so we don't need to persist an animated value.
        final boolean didFinish;
        if (isDisappearAnimation) {
          final PropertyState propertyState = animationState.propertyStates.get(property);
          if (propertyState == null) {
            throw new RuntimeException(
                "Some animation bookkeeping is wrong: tried to remove an animation from the list " +
                    "of active animations, but it wasn't there.");
          }

          propertyState.numPendingAnimations--;
          didFinish = areAllDisappearingAnimationsFinished(animationState);
          if (didFinish && animationState.mountContentGroup != null) {
            for (AnimatedProperty animatedProperty : animationState.propertyStates.keySet()) {
              resetProperty(animatedProperty, animationState.mountContentGroup);
            }
          }
        } else {
          final PropertyState propertyState = animationState.propertyStates.get(property);
          if (propertyState == null) {
            throw new RuntimeException(
                "Some animation bookkeeping is wrong: tried to remove an animation from the list " +
                    "of active animations, but it wasn't there.");
          }

          propertyState.numPendingAnimations--;
          if (propertyState.numPendingAnimations > 0) {
            didFinish = false;
          } else {
            animationState.propertyStates.remove(property);
            didFinish = animationState.propertyStates.isEmpty();

            if (animationState.mountContentGroup != null) {
              final float value = getPropertyValue(property, animationState.nextLayoutOutputsGroup);
              setPropertyValue(property, value, animationState.mountContentGroup);
            }
          }
        }

        if (didFinish) {
          if (AnimationsDebug.ENABLED) {
            Log.d(AnimationsDebug.TAG, "Finished all animations for key " + key);
          }
          if (animationState.mountContentGroup != null) {
            recursivelySetChildClippingForGroup(animationState.mountContentGroup, true);
          }
          if (mOnAnimationCompleteListener != null) {
            mOnAnimationCompleteListener.onAnimationComplete(key);
          }
          mAnimationStates.remove(key);
          clearLayoutOutputs(animationState);
        }
      }

      final String traceName = mTraceNames.get(binding.hashCode());
      if (!TextUtils.isEmpty(traceName)) {
        ComponentsSystrace.endSectionAsync(traceName, binding.hashCode());
        mTraceNames.delete(binding.hashCode());
      }
    }

    private boolean areAllDisappearingAnimationsFinished(AnimationState animationState) {
      if (animationState.changeType != ChangeType.DISAPPEARED) {
        throw new RuntimeException("This should only be checked for disappearing animations");
      }
      for (PropertyState propertyState : animationState.propertyStates.values()) {
        if (propertyState.numPendingAnimations > 0) {
          return false;
        }
      }
      return true;
    }
  }

  private class TransitionsResolver implements Resolver {

    @Override
    public float getCurrentState(PropertyHandle propertyHandle) {
      final AnimatedProperty animatedProperty = propertyHandle.getProperty();
      final AnimationState animationState = mAnimationStates.get(propertyHandle.getTransitionKey());
      final PropertyState propertyState = animationState.propertyStates.get(animatedProperty);

      // Use the current animating value if it exists...
      if (propertyState != null) {
        return propertyState.animatedPropertyNode.getValue();
      }

      // ...otherwise, if it's a property not being animated (e.g., the width when content appears
      // from a width offset), get the property from the LayoutOutput.
      final OutputUnitsAffinityGroup<LayoutOutput> layoutOutputGroupToCheck =
          animationState.changeType == ChangeType.APPEARED
              ? animationState.nextLayoutOutputsGroup
              : animationState.currentLayoutOutputsGroup;
      if (layoutOutputGroupToCheck == null) {
        throw new RuntimeException("Both LayoutOutputs were null!");
      }

      return animatedProperty.get(layoutOutputGroupToCheck.getMostSignificantUnit());
    }

    @Override
    public AnimatedPropertyNode getAnimatedPropertyNode(PropertyHandle propertyHandle) {
      final AnimationState state = mAnimationStates.get(propertyHandle.getTransitionKey());
      final PropertyState propertyState = state.propertyStates.get(propertyHandle.getProperty());
      return propertyState.animatedPropertyNode;
    }
  }

  private class RootAnimationListener implements AnimationBindingListener {

    @Override
    public void onScheduledToStartLater(AnimationBinding binding) {}

    @Override
    public void onWillStart(AnimationBinding binding) {
      mRunningRootAnimations.add(binding);
    }

    @Override
    public void onFinish(AnimationBinding binding) {
      mRunningRootAnimations.remove(binding);
    }

    @Override
    public void onCanceledBeforeStart(AnimationBinding binding) {
      mRunningRootAnimations.remove(binding);
    }

    @Override
    public boolean shouldStart(AnimationBinding binding) {
      return true;
    }
  }
}
