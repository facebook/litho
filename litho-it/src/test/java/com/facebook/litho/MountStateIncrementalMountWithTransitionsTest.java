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

import static com.facebook.litho.SizeSpec.EXACTLY;
import static org.assertj.core.api.Java6Assertions.assertThat;

import android.graphics.Color;
import com.facebook.litho.animation.AnimatedProperties;
import com.facebook.litho.config.ComponentsConfiguration;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.common.TestSingleComponentListSection;
import com.facebook.litho.sections.widget.ListRecyclerConfiguration;
import com.facebook.litho.sections.widget.RecyclerBinderConfiguration;
import com.facebook.litho.sections.widget.RecyclerCollectionComponent;
import com.facebook.litho.sections.widget.RecyclerConfiguration;
import com.facebook.litho.testing.LithoViewRule;
import com.facebook.litho.testing.TransitionTestRule;
import com.facebook.litho.widget.LithoViewFactory;
import com.facebook.litho.widget.MountSpecLifecycleTester;
import com.facebook.litho.widget.SectionsRecyclerView;
import com.facebook.litho.widget.TestAnimationsComponent;
import com.facebook.litho.widget.TestAnimationsComponentSpec;
import com.facebook.yoga.YogaAlign;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;

@RunWith(ParameterizedRobolectricTestRunner.class)
public class MountStateIncrementalMountWithTransitionsTest {

  private final boolean mDelegateToRenderCore;
  private ComponentContext mContext;
  final boolean mUseMountDelegateTarget;
  final boolean mUseIncrementalMountExtensionInMountState;

  public final @Rule LithoViewRule mLithoViewRule = new LithoViewRule();
  public final @Rule TransitionTestRule mTransitionTestRule = new TransitionTestRule();
  private final StateCaller mStateCaller = new StateCaller();
  public static final String RED_TRANSITION_KEY = "red";
  public static final String GREEN_TRANSITION_KEY = "green";
  public static final String BLUE_TRANSITION_KEY = "blue";
  private boolean configUseIncrementalMountExtension;

  @ParameterizedRobolectricTestRunner.Parameters(
      name = "useMountDelegateTarget={0}, useIncrementalMountExtensionInMountState={1}")
  public static Collection data() {
    return Arrays.asList(
        new Object[][] {
          {false, false},
          {true, false},
          {false, true}
        });
  }

  public MountStateIncrementalMountWithTransitionsTest(
      boolean useMountDelegateTarget, boolean useIncrementalMountExtensionInMountState) {
    mUseMountDelegateTarget = useMountDelegateTarget;
    mDelegateToRenderCore = false;
    mUseIncrementalMountExtensionInMountState = useIncrementalMountExtensionInMountState;
  }

  @Before
  public void setup() {
    configUseIncrementalMountExtension = ComponentsConfiguration.useIncrementalMountExtension;
    ComponentsConfiguration.useIncrementalMountExtension =
        mUseIncrementalMountExtensionInMountState;
    mContext = mLithoViewRule.getContext();
    mLithoViewRule.useLithoView(
        new LithoView(mContext, mUseMountDelegateTarget, mDelegateToRenderCore));
  }

  @After
  public void cleanup() {
    ComponentsConfiguration.useIncrementalMountExtension = configUseIncrementalMountExtension;
  }

  @Test
  public void incrementalMount_componentOffScreen_mountIfAnimating() {
    final LifecycleTracker lifecycleTracker1 = new LifecycleTracker();
    final LifecycleTracker lifecycleTracker2 = new LifecycleTracker();
    final LifecycleTracker lifecycleTracker3 = new LifecycleTracker();
    final ArrayList<LifecycleTracker> trackers = new ArrayList<>();
    trackers.add(lifecycleTracker1);
    trackers.add(lifecycleTracker2);
    trackers.add(lifecycleTracker3);

    final Component root = getPartiallyVisibleRootWithAnimatingComponents(trackers);

    mLithoViewRule
        .setRoot(root)
        .setSizeSpecs(SizeSpec.makeSizeSpec(1040, EXACTLY), SizeSpec.makeSizeSpec(60, EXACTLY));
    mLithoViewRule.attachToWindow().measure().layout();

    final List<LithoView> lithoViews = new ArrayList<>();
    final SectionsRecyclerView sectionsRecyclerView =
        (SectionsRecyclerView) mLithoViewRule.getLithoView().getChildAt(0);

    sectionsRecyclerView.obtainLithoViewChildren(lithoViews);

    final LithoView animatingLithoView = lithoViews.get(1);
    animatingLithoView.onAttachedToWindowForTest();

    mStateCaller.update();

    assertThat(lifecycleTracker1.getSteps()).contains(LifecycleStep.ON_MOUNT);
    assertThat(lifecycleTracker2.getSteps()).contains(LifecycleStep.ON_MOUNT);
    assertThat(lifecycleTracker3.getSteps()).contains(LifecycleStep.ON_MOUNT);
  }

  @Test
  public void incrementalMount_animatingComponentWithChildrenLithoView_mountLithoViewsOffScreen() {
    final LifecycleTracker lifecycleTracker0 = new LifecycleTracker();
    final LifecycleTracker lifecycleTracker1 = new LifecycleTracker();

    final MountSpecLifecycleTester component0 =
        MountSpecLifecycleTester.create(mLithoViewRule.getContext())
            .lifecycleTracker(lifecycleTracker0)
            .intrinsicSize(new Size(10, 40))
            .build();

    List<Component> animatingComponents = new ArrayList<>();
    animatingComponents.add(
        MountSpecLifecycleTester.create(mLithoViewRule.getContext())
            .lifecycleTracker(lifecycleTracker1)
            .intrinsicSize(new Size(40, 40))
            .widthPx(40)
            .heightPx(40)
            .build());

    final SectionContext sectionContext = new SectionContext(mLithoViewRule.getContext());
    final RecyclerBinderConfiguration binderConfig =
        RecyclerBinderConfiguration.create().lithoViewFactory(getLithoViewFactory()).build();
    RecyclerConfiguration config =
        ListRecyclerConfiguration.create()
            .orientation(0)
            .recyclerBinderConfiguration(binderConfig)
            .build();

    final Component childOfAnimatingComponent =
        RecyclerCollectionComponent.create(mLithoViewRule.getContext())
            .heightPx(40)
            .topPaddingPx(40)
            .section(
                TestSingleComponentListSection.create(sectionContext)
                    .data(animatingComponents)
                    .build())
            .recyclerConfiguration(config)
            .build();

    TestAnimationsComponentSpec.TestComponent partiallyVisibleAnimatingComponent =
        new TestAnimationsComponentSpec.TestComponent() {
          @Override
          public Component getComponent(ComponentContext componentContext, boolean state) {
            if (!state) {
              return Column.create(componentContext)
                  .alignItems(state ? YogaAlign.FLEX_START : YogaAlign.FLEX_END)
                  .child(
                      Column.create(componentContext)
                          .flexGrow(1)
                          .child(component0)
                          .transitionKey("transitionkey_root")
                          .build())
                  .build();
            }

            return Column.create(componentContext)
                .alignItems(state ? YogaAlign.FLEX_START : YogaAlign.FLEX_END)
                .child(
                    Column.create(componentContext)
                        .flexGrow(1)
                        .child(component0)
                        .child(childOfAnimatingComponent)
                        .transitionKey("transitionkey_root")
                        .build())
                .build();
          }
        };

    final TestAnimationsComponent root =
        TestAnimationsComponent.create(mLithoViewRule.getContext())
            .stateCaller(mStateCaller)
            .transition(
                Transition.sequence(
                    Transition.create("transitionkey_root")
                        .animator(Transition.timing(144))
                        .animate(AnimatedProperties.X)))
            .testComponent(partiallyVisibleAnimatingComponent)
            .build();

    mLithoViewRule
        .setRoot(root)
        .setSizeSpecs(SizeSpec.makeSizeSpec(40, EXACTLY), SizeSpec.makeSizeSpec(80, EXACTLY));
    mLithoViewRule.attachToWindow().measure().layout();

    assertThat(lifecycleTracker0.isMounted()).isTrue();
    assertThat(lifecycleTracker1.isMounted()).isFalse();
    mStateCaller.update();

    assertThat(lifecycleTracker1.isMounted()).isTrue();
  }

  final Component getPartiallyVisibleRootWithAnimatingComponents(
      List<LifecycleTracker> animatingComponentTrackers) {

    List<Component> animatingComponents = new ArrayList<>();
    for (LifecycleTracker tracker : animatingComponentTrackers) {
      animatingComponents.add(
          MountSpecLifecycleTester.create(mLithoViewRule.getContext())
              .lifecycleTracker(tracker)
              .intrinsicSize(new Size(40, 40))
              .build());
    }

    TestAnimationsComponentSpec.TestComponent partiallyVisibleAnimatingComponent =
        new TestAnimationsComponentSpec.TestComponent() {
          @Override
          public Component getComponent(ComponentContext componentContext, boolean state) {
            Column.Builder builder =
                Column.create(componentContext)
                    .alignItems(state ? YogaAlign.FLEX_START : YogaAlign.FLEX_END);

            for (int i = 0, size = animatingComponents.size(); i < size; i++) {
              builder.child(
                  Row.create(componentContext)
                      .child(animatingComponents.get(i))
                      .heightDip(40)
                      .widthDip(40)
                      .backgroundColor(Color.parseColor("#ee1111"))
                      .transitionKey("transitionkey_" + i)
                      .viewTag(RED_TRANSITION_KEY)
                      .build());
            }

            return builder.build();
          }
        };

    final Transition[] transitions = new Transition[animatingComponents.size()];
    for (int i = 0, size = animatingComponents.size(); i < size; i++) {
      transitions[i] =
          Transition.create("transitionkey_" + i)
              .animator(Transition.timing(144))
              .animate(AnimatedProperties.X);
    }

    final TestAnimationsComponent component =
        TestAnimationsComponent.create(mLithoViewRule.getContext())
            .stateCaller(mStateCaller)
            .transition(Transition.sequence(transitions))
            .testComponent(partiallyVisibleAnimatingComponent)
            .build();

    final List<Component> data = new ArrayList<>();

    data.add(
        Row.create(mLithoViewRule.getContext())
            .heightDip(40)
            .widthDip(40)
            .backgroundColor(Color.parseColor("#ee1111"))
            .build());

    data.add(component);

    final SectionContext sectionContext = new SectionContext(mLithoViewRule.getContext());
    final RecyclerBinderConfiguration binderConfig =
        RecyclerBinderConfiguration.create().lithoViewFactory(getLithoViewFactory()).build();
    RecyclerConfiguration config =
        ListRecyclerConfiguration.create().recyclerBinderConfiguration(binderConfig).build();

    final Component root =
        RecyclerCollectionComponent.create(mLithoViewRule.getContext())
            .section(TestSingleComponentListSection.create(sectionContext).data(data).build())
            .recyclerConfiguration(config)
            .build();

    return root;
  }

  private LithoViewFactory getLithoViewFactory() {
    return new LithoViewFactory() {
      @Override
      public LithoView createLithoView(ComponentContext context) {
        return new LithoView(context, mUseMountDelegateTarget, mDelegateToRenderCore);
      }
    };
  }
}
