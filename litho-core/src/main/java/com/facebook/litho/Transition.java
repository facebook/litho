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

import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import androidx.annotation.Nullable;
import com.facebook.infer.annotation.ThreadSafe;
import com.facebook.litho.animation.AnimatedProperties;
import com.facebook.litho.animation.AnimatedProperty;
import com.facebook.litho.animation.AnimatedPropertyNode;
import com.facebook.litho.animation.AnimationBinding;
import com.facebook.litho.animation.DimensionValue;
import com.facebook.litho.animation.FloatValue;
import com.facebook.litho.animation.PropertyAnimation;
import com.facebook.litho.animation.PropertyHandle;
import com.facebook.litho.animation.Resolver;
import com.facebook.litho.animation.RuntimeValue;
import com.facebook.litho.animation.SpringTransition;
import com.facebook.litho.animation.TimingTransition;
import com.facebook.litho.animation.TransitionAnimationBinding;
import com.facebook.litho.dataflow.springs.SpringConfig;
import com.facebook.rendercore.Function;
import java.util.ArrayList;

/**
 * Defines how a property on a component should animate as it changes, allowing you to optionally
 * define appear-from values for appear animations and disappear-to values for disappear animations.
 *
 * <p>TODO(t20719329): Better documentation for Transition class
 *
 * <p>Note: This abstract class has no instance methods, abstract or otherwise. It's a marker class
 * so that {@link TransitionSet}s and {@link TransitionUnit}s can be composed with each other. It's
 * abstract because before Java 8, static methods on interfaces are not allowed.
 */
public abstract class Transition {

  /**
   * Spring-driven animator with values of {@link SpringConfig#DEFAULT_TENSION} and {@link
   * SpringConfig#DEFAULT_FRICTION} that has overshooting behavior. Spring physics implementation is
   * taken from Rebound library and we recommend to use demo provided at <a
   * href="http://facebook.github.io/rebound/">http://facebook.github.io/rebound</a> to have a
   * better sense of how friction and tension values work together.
   */
  public static final TransitionAnimator SPRING_WITH_OVERSHOOT =
      new SpringTransitionAnimator(SpringConfig.defaultConfig);

  /**
   * Spring-driven animator that can be used as alternative to {@link #SPRING_WITH_OVERSHOOT} if
   * overshoot is not desired. Spring physics implementation is taken from Rebound library and we
   * recommend to use demo provided at <a
   * href="http://facebook.github.io/rebound/">http://facebook.github.io/rebound</a> to have a
   * better sense of how friction and tension values work together.
   */
  public static final TransitionAnimator SPRING_WITHOUT_OVERSHOOT =
      new SpringTransitionAnimator(SpringConfig.noOvershootConfig);

  /**
   * The default TransitionKeyType, assigned to component and transitions when user does not specify
   * what TransitionKeyType to use
   */
  static final Transition.TransitionKeyType DEFAULT_TRANSITION_KEY_TYPE = TransitionKeyType.LOCAL;

  public enum TransitionKeyType {
    GLOBAL,
    LOCAL
  }

  /** The type of a {@link ComponentTarget}. */
  enum ComponentTargetType {

    /**
     * Targets all components with transition keys that are changing in this transition. Expected
     * extra data: none.
     */
    ALL,

    /** Targets one local transition key. Expected extra data: String, a transition key. */
    LOCAL_KEY,

    /** Targets a set of local transition keys. Expected extra data: String[] of transition keys. */
    LOCAL_KEY_SET,

    /** Targets one global transition key. Expected extra data: String, a transition key. */
    GLOBAL_KEY,

    /**
     * Targets a set of global transition keys. Expected extra data: String[] of transition keys.
     */
    GLOBAL_KEY_SET,

    /**
     * Used for automatic bounds transition. Targets all components in the Component tree which
     * bounds are changing in this transition. Expected extra data: none.
     */
    AUTO_LAYOUT
  }

  /** The type of a {@link PropertyTarget}. */
  enum PropertyTargetType {

    /** Targets a set of properties. Expected extra data: AnimatedProperty[] of properties. */
    SET,

    /** Targets a single property. Expected extra data: AnimatedProperty, a single property. */
    SINGLE,

    /**
     * Used for automatic bounds transition. Targets properties related to bounds which are defined
     * in {@link AnimatedProperties#AUTO_LAYOUT_PROPERTIES}. Expected extra data: none.
     */
    AUTO_LAYOUT,
  }

  /** Specifies what components and properties a Transition should target. */
  public static class AnimationTarget {

    public final ComponentTarget componentTarget;
    public final PropertyTarget propertyTarget;

    AnimationTarget(ComponentTarget componentTarget, PropertyTarget propertyTarget) {
      this.componentTarget = componentTarget;
      this.propertyTarget = propertyTarget;
    }
  }

  /** Specifies the component(s) a Transition should target. */
  public static class ComponentTarget {

    public final ComponentTargetType componentTargetType;
    public final Object componentTargetExtraData;

    ComponentTarget(ComponentTargetType componentTargetType, Object componentTargetExtraData) {
      this.componentTargetType = componentTargetType;
      this.componentTargetExtraData = componentTargetExtraData;
    }
  }

  /** Specifies the property(s) a Transition should target. */
  public static class PropertyTarget {

    public final PropertyTargetType propertyTargetType;
    public final Object propertyTargetExtraData;

    PropertyTarget(PropertyTargetType propertyTargetType, Object propertyTargetExtraData) {
      this.propertyTargetType = propertyTargetType;
      this.propertyTargetExtraData = propertyTargetExtraData;
    }
  }

  private static final TransitionAnimator DEFAULT_ANIMATOR = SPRING_WITH_OVERSHOOT;
  private static final Interpolator DEFAULT_INTERPOLATOR = new AccelerateDecelerateInterpolator();
  private static final int DEFAULT_DURATION = 300;

  /**
   * Class that knows how to create a {@link TransitionAnimationBinding} given a {@link
   * PropertyAnimation}. This can be used to customize the type of animation using {@link
   * TransitionUnitsBuilder#animator}.
   */
  public interface TransitionAnimator {

    /**
     * @return a {@link TransitionAnimationBinding} for the given {@link PropertyAnimation} that
     *     will animate the change in value on this property.
     */
    TransitionAnimationBinding createAnimation(PropertyAnimation propertyAnimation);
  }

  /** Creates a Transition for the component with the given transition key. */
  public static TransitionUnitsBuilder create(String key) {
    return create(DEFAULT_TRANSITION_KEY_TYPE, key);
  }

  /** Creates a Transition for the components with the given transition keys. */
  public static TransitionUnitsBuilder create(String... keys) {
    return create(DEFAULT_TRANSITION_KEY_TYPE, keys);
  }

  /** Creates a Transition for the component with the given transition key of the given type. */
  public static TransitionUnitsBuilder create(TransitionKeyType type, String key) {
    return new TransitionUnitsBuilder(getComponentTargetTypeForTransitionKeyType(type, true), key);
  }

  /** Creates a Transition for the components with the given transition keys of the given type. */
  public static TransitionUnitsBuilder create(TransitionKeyType type, String... keys) {
    return new TransitionUnitsBuilder(
        getComponentTargetTypeForTransitionKeyType(type, false), keys);
  }

  private static ComponentTargetType getComponentTargetTypeForTransitionKeyType(
      TransitionKeyType transitionKeyType, boolean isSingleKey) {
    if (transitionKeyType == TransitionKeyType.GLOBAL) {
      return isSingleKey ? ComponentTargetType.GLOBAL_KEY : ComponentTargetType.GLOBAL_KEY_SET;
    } else if (transitionKeyType == TransitionKeyType.LOCAL) {
      return isSingleKey ? ComponentTargetType.LOCAL_KEY : ComponentTargetType.LOCAL_KEY_SET;
    } else {
      throw new RuntimeException("Unhandled TransitionKeyType " + transitionKeyType);
    }
  }

  /** Creates a Transition for the components targeted by the given {@link ComponentTarget}. */
  public static TransitionUnitsBuilder create(ComponentTarget target) {
    return new TransitionUnitsBuilder(target.componentTargetType, target.componentTargetExtraData);
  }

  /**
   * Creates an Automatic Bounds Transition that targets every component in the component tree whose
   * bounds have been changed.
   */
  public static AutoBoundsTransitionBuilder allLayout() {
    return new AutoBoundsTransitionBuilder();
  }

  /** Creates a set of {@link Transition}s that will run in parallel. */
  @ThreadSafe(enableChecks = false)
  public static <T extends Transition> TransitionSet parallel(T... transitions) {
    return new ParallelTransitionSet(transitions);
  }

  /** Creates a set of {@link Transition}s that will run in parallel but starting on a stagger. */
  public static <T extends Transition> TransitionSet stagger(int staggerMs, T... transitions) {
    return new ParallelTransitionSet(staggerMs, transitions);
  }

  /** Creates a sequence of {@link Transition}s that will run one after another. */
  public static <T extends Transition> TransitionSet sequence(T... transitions) {
    return new SequenceTransitionSet(transitions);
  }

  /** Creates a delayed {@link Transition}s that will run after a delay. */
  public static <T extends Transition> Transition delay(int delayMs, T transition) {
    return new DelayTransitionSet(delayMs, transition);
  }

  /**
   * Creates a {@link SpringTransition} with the given tension and friction. Spring physics
   * implementation is taken from Rebound library and we recommend to use demo provided at <a
   * href="http://facebook.github.io/rebound/">http://facebook.github.io/rebound</a> to have a
   * better sense of how friction and tension values work together.
   */
  public static TransitionAnimator springWithConfig(final double tension, final double friction) {
    return new SpringTransitionAnimator(tension, friction);
  }

  /** Creates a {@link TimingTransition} with the given duration. */
  public static TransitionAnimator timing(final int durationMs) {
    return new TimingTransitionAnimator(durationMs);
  }

  /** Creates a {@link TimingTransition} with the given duration and {@link Interpolator}. */
  public static TransitionAnimator timing(final int durationMs, Interpolator interpolator) {
    return new TimingTransitionAnimator(durationMs, interpolator);
  }

  public static class TransitionUnit extends Transition {

    private final AnimationTarget mAnimationTarget;
    private final TransitionAnimator mTransitionAnimator;
    private final RuntimeValue mAppearFrom;
    private final RuntimeValue mDisappearTo;
    @Nullable private final String mTraceName;
    @Nullable private String mOwnerKey;
    @Nullable private Function<Void> mTransitionEndHandler;

    TransitionUnit(
        AnimationTarget animationTarget,
        TransitionAnimator transitionAnimator,
        RuntimeValue appearFrom,
        RuntimeValue disappearTo,
        @Nullable String traceName,
        @Nullable Function<Void> transitionEndHandler) {
      mAnimationTarget = animationTarget;
      mTransitionAnimator = transitionAnimator;
      mAppearFrom = appearFrom;
      mDisappearTo = disappearTo;
      mTraceName = traceName;
      mTransitionEndHandler = transitionEndHandler;
    }

    AnimationTarget getAnimationTarget() {
      return mAnimationTarget;
    }

    boolean hasAppearAnimation() {
      return mAppearFrom != null;
    }

    boolean hasDisappearAnimation() {
      return mDisappearTo != null;
    }

    RuntimeValue getAppearFrom() {
      return mAppearFrom;
    }

    RuntimeValue getDisappearTo() {
      return mDisappearTo;
    }

    @Nullable
    Function<Void> getTransitionEndHandler() {
      return mTransitionEndHandler;
    }

    AnimationBinding createAnimation(PropertyHandle propertyHandle, float targetValue) {
      final PropertyAnimation propertyAnimation =
          new PropertyAnimation(propertyHandle, targetValue);
      return mTransitionAnimator.createAnimation(propertyAnimation);
    }

    @Nullable
    String getTraceName() {
      return mTraceName;
    }

    void setOwnerKey(@Nullable String ownerKey) {
      mOwnerKey = ownerKey;
    }

    @Nullable
    String getOwnerKey() {
      return mOwnerKey;
    }

    boolean targets(TransitionId transitionId) {
      switch (mAnimationTarget.componentTarget.componentTargetType) {
        case ALL:
        case AUTO_LAYOUT:
          return true;

        case LOCAL_KEY:
          if (!equals(mOwnerKey, transitionId.mExtraData)) {
            return false;
          }
        case GLOBAL_KEY:
          return transitionId.mReference.equals(
              mAnimationTarget.componentTarget.componentTargetExtraData);

        case LOCAL_KEY_SET:
          if (!equals(mOwnerKey, transitionId.mExtraData)) {
            return false;
          }
        case GLOBAL_KEY_SET:
          return arrayContains(
              (String[]) mAnimationTarget.componentTarget.componentTargetExtraData,
              transitionId.mReference);

        default:
          throw new RuntimeException(
              "Didn't handle type: " + mAnimationTarget.componentTarget.componentTargetType);
      }
    }

    boolean targetsProperty(AnimatedProperty property) {
      switch (mAnimationTarget.propertyTarget.propertyTargetType) {
        case AUTO_LAYOUT:
          return arrayContains(AnimatedProperties.AUTO_LAYOUT_PROPERTIES, property);
        case SET:
          return arrayContains(
              (AnimatedProperty[]) mAnimationTarget.propertyTarget.propertyTargetExtraData,
              property);
        case SINGLE:
          return property.equals(mAnimationTarget.propertyTarget.propertyTargetExtraData);
        default:
          throw new RuntimeException(
              "Didn't handle type: " + mAnimationTarget.propertyTarget.propertyTargetExtraData);
      }
    }
  }

  /**
   * Transition builder that allows targeting arbitrary keys and properties. By default CHANGE
   * animation is supported and it is possible to add support for APPEARING/DISAPPEARING animations
   * by providing start/end values for given property with {@link #appearFrom(float)}/{@link
   * #disappearTo(float)}. Default animator is {@link SpringTransition} but that can be customized
   * by providing other animator with {@link #animator(TransitionAnimator)}.
   */
  public static class TransitionUnitsBuilder extends BaseTransitionUnitsBuilder {

    TransitionUnitsBuilder(ComponentTarget componentTarget) {
      mComponentTarget = componentTarget;
    }

    TransitionUnitsBuilder(
        ComponentTargetType componentTargetType, Object componentTargetExtraData) {
      mComponentTarget = new ComponentTarget(componentTargetType, componentTargetExtraData);
    }

    /**
     * Adds a given property to animate. This also puts the Builder in a state to configure the
     * animation of this property using {@link #animator}, {@link #appearFrom}, and {@link
     * #disappearTo}.
     *
     * @param property the property to animate
     */
    public TransitionUnitsBuilder animate(AnimatedProperty property) {
      maybeCommitCurrentBuilder();
      mPropertyTarget = new PropertyTarget(PropertyTargetType.SINGLE, property);
      return this;
    }

    /**
     * Adds a set of properties to animate. This also puts the Builder in a state to configure the
     * animation of these properties using {@link #animator}. To specify appearFrom/disappearTo
     * values, address properties individually using {@link #animate(AnimatedProperty)}.
     *
     * @param properties the properties to animate
     */
    public TransitionUnitsBuilder animate(AnimatedProperty... properties) {
      maybeCommitCurrentBuilder();
      mPropertyTarget = new PropertyTarget(PropertyTargetType.SET, properties);
      return this;
    }

    /**
     * Adds a target of properties to animate. This also puts the Builder in a state to configure
     * the animation of these properties using {@link #animator}. To specify appearFrom/disappearTo
     * values, address properties individually using {@link #animate(AnimatedProperty)}.
     *
     * @param propertyTarget the target properties to animate
     */
    public TransitionUnitsBuilder animate(PropertyTarget propertyTarget) {
      maybeCommitCurrentBuilder();
      mPropertyTarget = propertyTarget;
      return this;
    }

    /**
     * Use to define the {@link TransitionAnimator} that drives the animation. The default is a
     * spring.
     */
    public TransitionUnitsBuilder animator(TransitionAnimator animator) {
      mTransitionAnimator = animator;
      return this;
    }

    /**
     * Add a transition end handler that would get a callback whenever the transition finishes.
     *
     * @param transitionEndHandler
     * @return
     */
    public TransitionUnitsBuilder transitionEndHandler(Function<Void> transitionEndHandler) {
      mTransitionEndHandler = transitionEndHandler;
      return this;
    }

    /**
     * Define where appear animations should start from.
     *
     * @see FloatValue
     * @see DimensionValue
     */
    public TransitionUnitsBuilder appearFrom(RuntimeValue value) {
      if (mPropertyTarget == null
          || mPropertyTarget.propertyTargetType != PropertyTargetType.SINGLE) {
        throw new RuntimeException(
            "Must specify a single property using #animate() before specifying an appearFrom "
                + "value!");
      }
      mAppearFrom = value;
      return this;
    }

    public TransitionUnitsBuilder traceName(String name) {
      mTraceName = name;
      return this;
    }

    /**
     * Define where disappear animations should end at.
     *
     * @see FloatValue
     * @see DimensionValue
     */
    public TransitionUnitsBuilder disappearTo(RuntimeValue value) {
      if (mPropertyTarget == null
          || mPropertyTarget.propertyTargetType != PropertyTargetType.SINGLE) {
        throw new RuntimeException(
            "Must specify a single property using #animate() before specifying an disappearTo "
                + "value!");
      }
      mDisappearTo = value;
      return this;
    }

    /** Define a constant value where appear animations should start from. */
    public TransitionUnitsBuilder appearFrom(float value) {
      return appearFrom(new FloatValue(value));
    }

    /** Define a constant value where disappear animations should end at. */
    public TransitionUnitsBuilder disappearTo(float value) {
      return disappearTo(new FloatValue(value));
    }
  }

  /**
   * Transition builder that targets every component in the component tree whose bounds have been
   * changed. Default animator is {@link SpringTransition} but that can be customized by providing
   * other animator with {@link #animator(TransitionAnimator)}.
   */
  public static class AutoBoundsTransitionBuilder extends BaseTransitionUnitsBuilder {

    AutoBoundsTransitionBuilder() {
      mComponentTarget = new ComponentTarget(ComponentTargetType.AUTO_LAYOUT, null);
      mPropertyTarget = new PropertyTarget(PropertyTargetType.AUTO_LAYOUT, null);
    }

    /**
     * Use to define the {@link TransitionAnimator} that drives the animation. The default is a
     * spring.
     */
    public AutoBoundsTransitionBuilder animator(TransitionAnimator animator) {
      mTransitionAnimator = animator;
      return this;
    }

    /**
     * Add a transition end handler that would get a callback whenever the transition finishes.
     *
     * @param transitionEndHandler
     * @return
     */
    public AutoBoundsTransitionBuilder transitionEndHandler(Function<Void> transitionEndHandler) {
      mTransitionEndHandler = transitionEndHandler;
      return this;
    }
  }

  public abstract static class BaseTransitionUnitsBuilder extends Transition {

    ArrayList<TransitionUnit> mBuiltTransitions = new ArrayList<>();
    ComponentTarget mComponentTarget;
    PropertyTarget mPropertyTarget;
    TransitionAnimator mTransitionAnimator = DEFAULT_ANIMATOR;
    RuntimeValue mAppearFrom;
    RuntimeValue mDisappearTo;
    String mTraceName;
    @Nullable Function<Void> mTransitionEndHandler;

    void maybeCommitCurrentBuilder() {
      if (mPropertyTarget == null) {
        return;
      }
      mBuiltTransitions.add(
          new TransitionUnit(
              new AnimationTarget(mComponentTarget, mPropertyTarget),
              mTransitionAnimator,
              mAppearFrom,
              mDisappearTo,
              mTraceName,
              mTransitionEndHandler));
      mPropertyTarget = null;
      mTransitionAnimator = DEFAULT_ANIMATOR;
      mAppearFrom = null;
      mDisappearTo = null;
      mTraceName = null;
      mTransitionEndHandler = null;
    }

    ArrayList<TransitionUnit> getTransitionUnits() {
      maybeCommitCurrentBuilder();
      return mBuiltTransitions;
    }
  }

  private static <T> boolean arrayContains(T[] array, T value) {
    for (int i = 0, size = array.length; i < size; i++) {
      if (array[i] == value) {
        return true;
      }
    }

    return false;
  }

  /** Creates spring-driven animations. */
  public static class SpringTransitionAnimator implements TransitionAnimator {

    final SpringConfig mSpringConfig;

    /**
     * Create spring-driven animator with given tension and friction values. Spring physics
     * implementation is taken from Rebound library and we recommend to use demo provided at <a
     * href="http://facebook.github.io/rebound/">http://facebook.github.io/rebound</a> to have a
     * better sense of how friction and tension values work together.
     */
    public SpringTransitionAnimator(final double tension, final double friction) {
      mSpringConfig = new SpringConfig(tension, friction);
    }

    /**
     * Create spring-driven animator with given {@link SpringConfig}. Spring physics implementation
     * is taken from Rebound library and we recommend to use demo provided at <a
     * href="http://facebook.github.io/rebound/">http://facebook.github.io/rebound</a> to have a
     * better sense of how friction and tension values work together.
     */
    public SpringTransitionAnimator(SpringConfig springConfig) {
      mSpringConfig = springConfig;
    }

    @Override
    public TransitionAnimationBinding createAnimation(PropertyAnimation propertyAnimation) {
      return new SpringTransition(propertyAnimation, mSpringConfig);
    }
  }

  /** Creates timing-driven animations with the given duration. */
  public static class TimingTransitionAnimator implements Transition.TransitionAnimator {

    final int mDurationMs;
    final Interpolator mInterpolator;

    /** Create timing animator with accelerate decelerate interpolation. */
    public TimingTransitionAnimator(int durationMs) {
      this(durationMs, DEFAULT_INTERPOLATOR);
    }

    /** Create timing animator with custom Android interpolator. */
    public TimingTransitionAnimator(int durationMs, Interpolator interpolator) {
      mDurationMs = durationMs;
      mInterpolator = interpolator;
    }

    @Override
    public TransitionAnimationBinding createAnimation(PropertyAnimation propertyAnimation) {
      return new TimingTransition(mDurationMs, propertyAnimation, mInterpolator);
    }
  }

  /**
   * Separate resolver for root component to extract the start value of appear animation of its *
   * width/height that we will set in root host onMeasure.
   */
  private static class RootItemResolver implements Resolver {

    private final TransitionsExtensionInput mTransitionsExtensionInput;
    private final AnimatedProperty mAnimatedProperty;

    private RootItemResolver(
        TransitionsExtensionInput transitionsExtensionInput, AnimatedProperty animatedProperty) {
      mTransitionsExtensionInput = transitionsExtensionInput;
      mAnimatedProperty = animatedProperty;
    }

    @Override
    public float getCurrentState(PropertyHandle propertyHandle) {
      return mAnimatedProperty.get(mTransitionsExtensionInput.getAnimatableRootItem());
    }

    @Override
    public AnimatedPropertyNode getAnimatedPropertyNode(PropertyHandle propertyHandle) {
      throw new UnsupportedOperationException();
    }
  }

  static float getRootAppearFromValue(
      TransitionUnit transition,
      TransitionsExtensionInput transitionsExtensionInput,
      AnimatedProperty property) {
    final RootItemResolver resolver = new RootItemResolver(transitionsExtensionInput, property);
    final TransitionId rootTransitionId = transitionsExtensionInput.getRootTransitionId();
    return transition
        .getAppearFrom()
        .resolve(resolver, new PropertyHandle(rootTransitionId, property));
  }

  /** @return {@code true} iff a and b are equal. */
  static boolean equals(@Nullable Object a, @Nullable Object b) {
    if (a == b) {
      return true;
    }

    if (a == null || b == null) {
      return false;
    }

    return a.equals(b);
  }

  /**
   * Contains information about whether root component has bounds transition and if so whether it
   * defines appear animation as well. The latter is useful to extract from which value we should
   * animate from so that in root Host on measure we can set initial value.
   */
  static class RootBoundsTransition {
    boolean hasTransition;
    TransitionUnit appearTransition;
  }
}
