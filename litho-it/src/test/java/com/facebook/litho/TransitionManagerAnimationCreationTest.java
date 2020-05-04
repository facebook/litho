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

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.graphics.Rect;
import com.facebook.litho.animation.AnimatedProperties;
import com.facebook.litho.animation.AnimatedProperty;
import com.facebook.litho.animation.PropertyAnimation;
import com.facebook.litho.animation.PropertyHandle;
import com.facebook.litho.animation.SpringTransition;
import com.facebook.litho.animation.TransitionAnimationBinding;
import com.facebook.litho.testing.testrunner.LithoTestRunner;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/** Tests for the creation of animations using the targeting API in {@link Transition}. */
@RunWith(LithoTestRunner.class)
public class TransitionManagerAnimationCreationTest {

  private TransitionManager mTransitionManager;
  private Transition.TransitionAnimator mTestVerificationAnimator;
  private ArrayList<PropertyAnimation> mCreatedAnimations = new ArrayList<>();

  @Before
  public void setup() {
    mTransitionManager =
        new TransitionManager(
            new TransitionManager.OnAnimationCompleteListener() {
              @Override
              public void onAnimationComplete(TransitionId transitionId) {}
            },
            mock(MountState.class));
    mTestVerificationAnimator =
        new Transition.TransitionAnimator() {
          @Override
          public TransitionAnimationBinding createAnimation(PropertyAnimation propertyAnimation) {
            mCreatedAnimations.add(propertyAnimation);
            return new SpringTransition(propertyAnimation);
          }
        };
  }

  @After
  public void tearDown() {
    mTransitionManager = null;
    mCreatedAnimations.clear();
  }

  @Test
  public void testCreateSingleAnimation() {
    final LayoutState current =
        createMockLayoutState(Transition.parallel(), createMockLayoutOutput("test", 0, 0));
    final LayoutState next =
        createMockLayoutState(
            Transition.parallel(
                Transition.create(Transition.TransitionKeyType.GLOBAL, "test")
                    .animate(AnimatedProperties.X)
                    .animator(mTestVerificationAnimator)),
            createMockLayoutOutput("test", 10, 10));

    mTransitionManager.setupTransitions(
        current, next, TransitionManager.getRootTransition(next.getTransitions()));

    assertThat(mCreatedAnimations)
        .containsExactlyInAnyOrder(createPropertyAnimation("test", AnimatedProperties.X, 10));
  }

  @Test
  public void testCreateMultiPropAnimation() {
    final LayoutState current =
        createMockLayoutState(Transition.parallel(), createMockLayoutOutput("test", 0, 0));
    final LayoutState next =
        createMockLayoutState(
            Transition.parallel(
                Transition.create(Transition.TransitionKeyType.GLOBAL, "test")
                    .animate(AnimatedProperties.X, AnimatedProperties.Y)
                    .animator(mTestVerificationAnimator)),
            createMockLayoutOutput("test", 10, 10));

    mTransitionManager.setupTransitions(
        current, next, TransitionManager.getRootTransition(next.getTransitions()));

    assertThat(mCreatedAnimations)
        .containsExactlyInAnyOrder(
            createPropertyAnimation("test", AnimatedProperties.X, 10),
            createPropertyAnimation("test", AnimatedProperties.Y, 10));
  }

  @Test
  public void testCreateMultiPropAnimationWithNonChangingProp() {
    final LayoutState current =
        createMockLayoutState(Transition.parallel(), createMockLayoutOutput("test", 0, 0));
    final LayoutState next =
        createMockLayoutState(
            Transition.parallel(
                Transition.create(Transition.TransitionKeyType.GLOBAL, "test")
                    .animate(AnimatedProperties.X, AnimatedProperties.Y)
                    .animator(mTestVerificationAnimator)),
            createMockLayoutOutput("test", 10, 0));

    mTransitionManager.setupTransitions(
        current, next, TransitionManager.getRootTransition(next.getTransitions()));

    assertThat(mCreatedAnimations)
        .containsExactlyInAnyOrder(createPropertyAnimation("test", AnimatedProperties.X, 10));
  }

  @Test
  public void testCreateMultiComponentAnimation() {
    final LayoutState current =
        createMockLayoutState(
            Transition.parallel(),
            createMockLayoutOutput("test1", 0, 0),
            createMockLayoutOutput("test2", 0, 0));
    final LayoutState next =
        createMockLayoutState(
            Transition.parallel(
                Transition.create(Transition.TransitionKeyType.GLOBAL, "test1", "test2")
                    .animate(AnimatedProperties.X)
                    .animator(mTestVerificationAnimator)),
            createMockLayoutOutput("test1", 10, 10),
            createMockLayoutOutput("test2", -10, -10));

    mTransitionManager.setupTransitions(
        current, next, TransitionManager.getRootTransition(next.getTransitions()));

    assertThat(mCreatedAnimations)
        .containsExactlyInAnyOrder(
            createPropertyAnimation("test1", AnimatedProperties.X, 10),
            createPropertyAnimation("test2", AnimatedProperties.X, -10));
  }

  @Test
  public void testSetsOfComponentsAndPropertiesAnimation() {
    final LayoutState current =
        createMockLayoutState(
            Transition.parallel(),
            createMockLayoutOutput("test1", 0, 0),
            createMockLayoutOutput("test2", 0, 0));
    final LayoutState next =
        createMockLayoutState(
            Transition.parallel(
                Transition.create(Transition.TransitionKeyType.GLOBAL, "test1", "test2")
                    .animate(AnimatedProperties.X, AnimatedProperties.Y)
                    .animator(mTestVerificationAnimator)),
            createMockLayoutOutput("test1", 10, 10),
            createMockLayoutOutput("test2", -10, -10));

    mTransitionManager.setupTransitions(
        current, next, TransitionManager.getRootTransition(next.getTransitions()));

    assertThat(mCreatedAnimations)
        .containsExactlyInAnyOrder(
            createPropertyAnimation("test1", AnimatedProperties.X, 10),
            createPropertyAnimation("test1", AnimatedProperties.Y, 10),
            createPropertyAnimation("test2", AnimatedProperties.X, -10),
            createPropertyAnimation("test2", AnimatedProperties.Y, -10));
  }

  @Test
  public void testAutoLayoutAnimation() {
    final LayoutState current =
        createMockLayoutState(
            Transition.parallel(),
            createMockLayoutOutput("test1", 0, 0),
            createMockLayoutOutput("test2", 0, 0));

    final LayoutState next =
        createMockLayoutState(
            Transition.parallel(Transition.allLayout().animator(mTestVerificationAnimator)),
            createMockLayoutOutput("test1", 10, 20, 200, 150),
            createMockLayoutOutput("test2", -10, -20, 50, 80));

    mTransitionManager.setupTransitions(
        current, next, TransitionManager.getRootTransition(next.getTransitions()));

    assertThat(mCreatedAnimations)
        .containsExactlyInAnyOrder(
            createPropertyAnimation("test1", AnimatedProperties.X, 10),
            createPropertyAnimation("test1", AnimatedProperties.Y, 20),
            createPropertyAnimation("test1", AnimatedProperties.WIDTH, 200),
            createPropertyAnimation("test1", AnimatedProperties.HEIGHT, 150),
            createPropertyAnimation("test2", AnimatedProperties.X, -10),
            createPropertyAnimation("test2", AnimatedProperties.Y, -20),
            createPropertyAnimation("test2", AnimatedProperties.WIDTH, 50),
            createPropertyAnimation("test2", AnimatedProperties.HEIGHT, 80));
  }

  @Test
  public void testKeyDoesntExist() {
    final LayoutState current =
        createMockLayoutState(Transition.parallel(), createMockLayoutOutput("test", 0, 0));
    final LayoutState next =
        createMockLayoutState(
            Transition.parallel(
                Transition.create(Transition.TransitionKeyType.GLOBAL, "test", "keydoesntexist")
                    .animate(AnimatedProperties.X, AnimatedProperties.Y)
                    .animator(mTestVerificationAnimator)),
            createMockLayoutOutput("test", 10, 0));

    mTransitionManager.setupTransitions(
        current, next, TransitionManager.getRootTransition(next.getTransitions()));

    assertThat(mCreatedAnimations)
        .containsExactlyInAnyOrder(createPropertyAnimation("test", AnimatedProperties.X, 10));
  }

  @Test
  public void testNoPreviousLayoutState() {
    final LayoutState next =
        createMockLayoutState(
            Transition.parallel(
                Transition.create(Transition.TransitionKeyType.GLOBAL, "test", "keydoesntexist")
                    .animate(AnimatedProperties.X, AnimatedProperties.Y)
                    .animator(mTestVerificationAnimator),
                Transition.create(Transition.TransitionKeyType.GLOBAL, "appearing")
                    .animate(AnimatedProperties.X)
                    .appearFrom(0)
                    .animator(mTestVerificationAnimator)),
            createMockLayoutOutput("test", 10, 0),
            createMockLayoutOutput("appearing", 20, 0));

    mTransitionManager.setupTransitions(
        null, next, TransitionManager.getRootTransition(next.getTransitions()));

    assertThat(mCreatedAnimations)
        .containsExactlyInAnyOrder(createPropertyAnimation("appearing", AnimatedProperties.X, 20));
  }

  @Test
  public void testWithMountTimeAnimations() {
    final LayoutState current =
        createMockLayoutState(
            Transition.parallel(),
            createMockLayoutOutput("test", 0, 0),
            createMockLayoutOutput("test2", 0, 0));
    final LayoutState next =
        createMockLayoutState(
            Transition.parallel(
                Transition.create(Transition.TransitionKeyType.GLOBAL, "test", "keydoesntexist")
                    .animate(AnimatedProperties.X, AnimatedProperties.Y)
                    .animator(mTestVerificationAnimator)),
            createMockLayoutOutput("test", 10, 0),
            createMockLayoutOutput("test2", 20, 0));
    final ArrayList<Transition> mountTimeTransitions = new ArrayList<>();
    mountTimeTransitions.add(
        Transition.create(Transition.TransitionKeyType.GLOBAL, "test2")
            .animate(AnimatedProperties.X)
            .appearFrom(0)
            .animator(mTestVerificationAnimator));

    final List<Transition> allTransitions = new ArrayList<>();
    allTransitions.addAll(next.getTransitions());
    allTransitions.addAll(mountTimeTransitions);
    mTransitionManager.setupTransitions(
        current, next, TransitionManager.getRootTransition(allTransitions));

    assertThat(mCreatedAnimations)
        .containsExactlyInAnyOrder(
            createPropertyAnimation("test", AnimatedProperties.X, 10),
            createPropertyAnimation("test2", AnimatedProperties.X, 20));
  }

  @Test
  public void testWithOnlyMountTimeAnimations() {
    final LayoutState current =
        createMockLayoutState(
            Transition.parallel(),
            createMockLayoutOutput("test", 0, 0),
            createMockLayoutOutput("test2", 0, 0));
    final LayoutState next =
        createMockLayoutState(
            null, createMockLayoutOutput("test", 10, 0), createMockLayoutOutput("test2", 20, 0));
    final ArrayList<Transition> mountTimeTransitions = new ArrayList<>();
    mountTimeTransitions.add(
        Transition.create(Transition.TransitionKeyType.GLOBAL, "test2")
            .animate(AnimatedProperties.X)
            .appearFrom(0)
            .animator(mTestVerificationAnimator));
    mountTimeTransitions.add(
        Transition.create(Transition.TransitionKeyType.GLOBAL, "test", "keydoesntexist")
            .animate(AnimatedProperties.X, AnimatedProperties.Y)
            .animator(mTestVerificationAnimator));

    mTransitionManager.setupTransitions(
        current, next, TransitionManager.getRootTransition(mountTimeTransitions));

    assertThat(mCreatedAnimations)
        .containsExactlyInAnyOrder(
            createPropertyAnimation("test", AnimatedProperties.X, 10),
            createPropertyAnimation("test2", AnimatedProperties.X, 20));
  }

  @Test
  public void testAnimationFromStateUpdate() {
    final LayoutState current =
        createMockLayoutState(
            Transition.parallel(),
            createMockLayoutOutput("test1", 0, 0),
            createMockLayoutOutput("test2", 0, 0));

    final ArrayList<PropertyAnimation> animationsCreatedFromStateUpdate = new ArrayList<>();

    Transition.TransitionAnimator animatorForStateUpdate =
        new Transition.TransitionAnimator() {
          @Override
          public TransitionAnimationBinding createAnimation(PropertyAnimation propertyAnimation) {
            animationsCreatedFromStateUpdate.add(propertyAnimation);
            return new SpringTransition(propertyAnimation);
          }
        };
    final ArrayList<Transition> transitionsFromStateUpdate = new ArrayList<>();
    transitionsFromStateUpdate.add(
        Transition.create(Transition.TransitionKeyType.GLOBAL, "test1", "test2")
            .animate(AnimatedProperties.Y)
            .animator(animatorForStateUpdate));

    final LayoutState next =
        createMockLayoutState(
            Transition.parallel(
                Transition.create(Transition.TransitionKeyType.GLOBAL, "test1", "test2")
                    .animate(AnimatedProperties.X)
                    .animator(mTestVerificationAnimator)),
            createMockLayoutOutput("test1", 10, 20),
            createMockLayoutOutput("test2", -10, -20));

    final List<Transition> allTransitions = new ArrayList<>();
    allTransitions.addAll(next.getTransitions());
    allTransitions.addAll(transitionsFromStateUpdate);

    mTransitionManager.setupTransitions(
        current, next, TransitionManager.getRootTransition(allTransitions));

    assertThat(mCreatedAnimations.size()).isEqualTo(2);

    assertThat(animationsCreatedFromStateUpdate)
        .containsExactlyInAnyOrder(
            createPropertyAnimation("test1", AnimatedProperties.Y, 20),
            createPropertyAnimation("test2", AnimatedProperties.Y, -20));
  }

  private PropertyAnimation createPropertyAnimation(
      String key, AnimatedProperty property, float endValue) {
    final TransitionId transitionId = new TransitionId(TransitionId.Type.GLOBAL, key, null);
    return new PropertyAnimation(new PropertyHandle(transitionId, property), endValue);
  }

  /** @return a mock LayoutState that only has a transition key -> LayoutOutput mapping. */
  private LayoutState createMockLayoutState(
      TransitionSet transitions, LayoutOutput... layoutOutputs) {
    final Map<TransitionId, OutputUnitsAffinityGroup<LayoutOutput>> transitionIdMapping =
        new LinkedHashMap<>();
    for (int i = 0; i < layoutOutputs.length; i++) {
      final LayoutOutput layoutOutput = layoutOutputs[i];
      final TransitionId transitionId = layoutOutput.getTransitionId();
      if (transitionId == null) {
        continue;
      }
      OutputUnitsAffinityGroup<LayoutOutput> group = transitionIdMapping.get(transitionId);
      if (group == null) {
        group = new OutputUnitsAffinityGroup<>();
        transitionIdMapping.put(transitionId, group);
      }
      final @OutputUnitType int type =
          LayoutStateOutputIdCalculator.getLevelFromId(layoutOutput.getId());
      group.add(type, layoutOutput);
    }

    final LayoutState layoutState = mock(LayoutState.class);
    when(layoutState.getTransitions())
        .thenReturn(transitions != null ? transitions.getChildren() : null);
    when(layoutState.getTransitionIdMapping()).thenReturn(transitionIdMapping);
    when(layoutState.getLayoutOutputsForTransitionId(any()))
        .then(
            new Answer<OutputUnitsAffinityGroup<LayoutOutput>>() {
              @Override
              public OutputUnitsAffinityGroup<LayoutOutput> answer(InvocationOnMock invocation)
                  throws Throwable {
                final TransitionId transitionId = (TransitionId) invocation.getArguments()[0];
                return transitionIdMapping.get(transitionId);
              }
            });
    return layoutState;
  }

  /** @return a mock LayoutOutput with a transition key and dummy bounds. */
  private static LayoutOutput createMockLayoutOutput(String transitionKey, int x, int y) {
    return createMockLayoutOutput(transitionKey, x, y, 100, 100);
  }

  /** @return a mock LayoutOutput with a transition key and bounds. */
  private static LayoutOutput createMockLayoutOutput(
      String transitionKey, int x, int y, int width, int height) {
    final LayoutOutput layoutOutput = mock(LayoutOutput.class);
    final TransitionId transitionId =
        new TransitionId(TransitionId.Type.GLOBAL, transitionKey, null);
    when(layoutOutput.getTransitionId()).thenReturn(transitionId);
    when(layoutOutput.getBounds()).thenReturn(new Rect(x, y, x + width, y + height));
    return layoutOutput;
  }
}
