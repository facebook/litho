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

package com.facebook.samples.litho;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.Nullable;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.widget.RecyclerCollectionComponent;
import com.facebook.litho.widget.ComponentCreator;
import com.facebook.samples.litho.animations.animatedbadge.AnimatedBadgeActivity;
import com.facebook.samples.litho.animations.animationcallbacks.AnimationCallbacksActivity;
import com.facebook.samples.litho.animations.animationcomposition.ComposedAnimationsActivity;
import com.facebook.samples.litho.animations.animationcookbook.AnimationCookBookActivity;
import com.facebook.samples.litho.animations.bounds.BoundsAnimationActivity;
import com.facebook.samples.litho.animations.commondynamicprops.CommonDynamicPropsAnimationActivity;
import com.facebook.samples.litho.animations.docs.AppearTransitionComponent;
import com.facebook.samples.litho.animations.docs.ParallelTransitionWithAnimatorsComponent;
import com.facebook.samples.litho.animations.docs.SequenceTransitionLoopComponent;
import com.facebook.samples.litho.animations.docs.SimpleAllLayoutTransitionComponent;
import com.facebook.samples.litho.animations.docs.StaggerTransitionComponent;
import com.facebook.samples.litho.animations.docs.StaggerTransitionSameComponent;
import com.facebook.samples.litho.animations.docs.StaggerTransitionWithDelayComponent;
import com.facebook.samples.litho.animations.docs.keyscope.GlobalKeyParentComponent;
import com.facebook.samples.litho.animations.expandableelement.ExpandableElementActivity;
import com.facebook.samples.litho.animations.pageindicators.PageIndicatorsActivity;
import com.facebook.samples.litho.animations.sharedelements.SharedElementsActivity;
import com.facebook.samples.litho.animations.sharedelements.SharedElementsFragmentActivity;
import com.facebook.samples.litho.animations.transitions.TransitionsActivity;
import com.facebook.samples.litho.bordereffects.BorderEffectsActivity;
import com.facebook.samples.litho.changesetdebug.ItemsRerenderingActivity;
import com.facebook.samples.litho.changesetdebug.PropUpdatingActivity;
import com.facebook.samples.litho.changesetdebug.ScrollingToBottomActivity;
import com.facebook.samples.litho.changesetdebug.StateResettingActivity;
import com.facebook.samples.litho.duplicatestate.DuplicateStateActivity;
import com.facebook.samples.litho.dynamicprops.DynamicPropsActivity;
import com.facebook.samples.litho.editor.SimpleEditorExampleActivity;
import com.facebook.samples.litho.errors.ErrorHandlingActivity;
import com.facebook.samples.litho.fastscroll.FastScrollHandleActivity;
import com.facebook.samples.litho.hscroll.HorizontalScrollWithDynamicItemHeight;
import com.facebook.samples.litho.hscroll.HorizontalScrollWithSnapActivity;
import com.facebook.samples.litho.incrementalmount.IncrementalMountWithCustomViewContainerActivity;
import com.facebook.samples.litho.kotlin.animations.animatedapi.AnimatedActivity;
import com.facebook.samples.litho.kotlin.animations.animatedcounter.AnimatedCounterActivity;
import com.facebook.samples.litho.kotlin.collection.CollectionKComponent;
import com.facebook.samples.litho.kotlin.logging.LoggingActivity;
import com.facebook.samples.litho.kotlin.playground.PlaygroundActivity;
import com.facebook.samples.litho.lifecycle.LifecycleDelegateActivity;
import com.facebook.samples.litho.lifecycle.LifecycleFragmentActivity;
import com.facebook.samples.litho.lifecycle.ViewPagerLifecycleActivity;
import com.facebook.samples.litho.lithography.LithographyActivity;
import com.facebook.samples.litho.onboarding.AlphaTransitionComponent;
import com.facebook.samples.litho.onboarding.FirstComponentSpecActivity;
import com.facebook.samples.litho.onboarding.HelloWorldActivity;
import com.facebook.samples.litho.onboarding.IntroducingLayoutComponent;
import com.facebook.samples.litho.onboarding.LayoutWithImageComponent;
import com.facebook.samples.litho.playground.PlaygroundComponent;
import com.facebook.samples.litho.stateupdates.SectionStateUpdateFromComponentSection;
import com.facebook.samples.litho.stateupdates.StateUpdateFromOutsideTreeActivity;
import com.facebook.samples.litho.staticscroll.horizontalscroll.HorizontalScrollActivity;
import com.facebook.samples.litho.stats.StatsActivity;
import com.facebook.samples.litho.textinput.TextInputRequestAndClearFocus;
import com.facebook.samples.litho.textinput.TextInputWithKeyboardAndFocusDemo;
import com.facebook.samples.litho.triggers.ClearTextTriggerExampleActivity;
import com.facebook.samples.litho.triggers.CustomEventTriggerExampleActivity;
import com.facebook.samples.litho.triggers.TooltipTriggerExampleActivity;
import com.facebook.samples.litho.viewpager.ViewPagerDemoComponent;
import java.util.Arrays;
import java.util.List;

public class Demos {

  public static final List<DemoList> DEMOS =
      Arrays.asList(
          new DemoList(
              "Playground",
              Arrays.asList(
                  new DemoGrouping(
                      "Playground",
                      Arrays.asList(
                          new SingleDemo(
                              "Java API Playground",
                              new ComponentCreator() {
                                @Override
                                public Component create(ComponentContext c) {
                                  return PlaygroundComponent.create(c).build();
                                }
                              }),
                          new SingleDemo("Kotlin API Playground", PlaygroundActivity.class))))),
          // Please keep this alphabetical with consistent naming and casing!
          new DemoList(
              "Kotlin API Demos",
              Arrays.asList(
                  new DemoGrouping(
                      "Animations",
                      Arrays.asList(
                          new SingleDemo("Animated API Demo", AnimatedActivity.class),
                          new SingleDemo(
                              "Animated Badge",
                              com.facebook.samples.litho.kotlin.animations.animatedbadge
                                  .AnimatedBadgeActivity.class),
                          new SingleDemo("Animated Counter", AnimatedCounterActivity.class),
                          new SingleDemo(
                              "Animations Composition",
                              com.facebook.samples.litho.kotlin.animations.animationcomposition
                                  .ComposedAnimationsActivity.class),
                          new SingleDemo(
                              "Expandable Element",
                              com.facebook.samples.litho.kotlin.animations.expandableelement
                                  .ExpandableElementActivity.class),
                          new SingleDemo(
                              "Transitions",
                              com.facebook.samples.litho.kotlin.animations.transitions
                                  .TransitionsActivity.class))),
                  new DemoGrouping(
                      "Collections",
                      Arrays.asList(
                          new SingleDemo(
                              "Collections Demo",
                              new ComponentCreator() {
                                @Override
                                public Component create(ComponentContext c) {
                                  return new CollectionKComponent();
                                }
                              }),
                          new SingleDemo(
                              "Sections Demo: Lithography",
                              com.facebook.samples.litho.kotlin.lithography.LithographyActivity
                                  .class))),
                  new DemoGrouping(
                      "Errors",
                      Arrays.asList(
                          new SingleDemo(
                              "Error Boundaries",
                              com.facebook.samples.litho.kotlin.errors.ErrorHandlingActivity
                                  .class))),
                  new DemoGrouping(
                      "Common Props",
                      Arrays.asList(
                          new SingleDemo(
                              "Border Effects",
                              com.facebook.samples.litho.kotlin.bordereffects.BorderEffectsActivity
                                  .class))),
                  new DemoGrouping(
                      "Logging",
                      Arrays.asList(new SingleDemo(" Logging", LoggingActivity.class))))),
          new DemoList(
              "Java API Demos",
              Arrays.asList(
                  new DemoGrouping(
                      "Animations",
                      Arrays.asList(
                          new SingleDemo(
                              "Animations Composition", ComposedAnimationsActivity.class),
                          new SingleDemo("Expandable Element", ExpandableElementActivity.class),
                          new SingleDemo("Animated Badge", AnimatedBadgeActivity.class),
                          new SingleDemo("Bounds Animation", BoundsAnimationActivity.class),
                          new SingleDemo("Page Indicators", PageIndicatorsActivity.class),
                          new SingleDemo(
                              "Common Dynamic Props Animations",
                              CommonDynamicPropsAnimationActivity.class),
                          new SingleDemo("Animation Cookbook", AnimationCookBookActivity.class),
                          new SingleDemo("Animation Callbacks", AnimationCallbacksActivity.class),
                          new SingleDemo(
                              "Activity Transition with Shared elements",
                              SharedElementsActivity.class),
                          new SingleDemo(
                              "Fragments Transition with Shared elements",
                              SharedElementsFragmentActivity.class),
                          new SingleDemo("Transitions", TransitionsActivity.class),
                          new SingleDemo(
                              "All Layout Transition",
                              new ComponentCreator() {
                                @Override
                                public Component create(ComponentContext c) {
                                  return SimpleAllLayoutTransitionComponent.create(c).build();
                                }
                              }),
                          new SingleDemo(
                              "Alpha Transition",
                              new ComponentCreator() {
                                @Override
                                public Component create(ComponentContext c) {
                                  return AlphaTransitionComponent.create(c).build();
                                }
                              }),
                          new SingleDemo(
                              "Appear Transition",
                              new ComponentCreator() {
                                @Override
                                public Component create(ComponentContext c) {
                                  return AppearTransitionComponent.create(c).build();
                                }
                              }),
                          new SingleDemo(
                              "Stagger Transition",
                              new ComponentCreator() {
                                @Override
                                public Component create(ComponentContext c) {
                                  return StaggerTransitionComponent.create(c).build();
                                }
                              }),
                          new SingleDemo(
                              "Stagger Transition on same Component",
                              new ComponentCreator() {
                                @Override
                                public Component create(ComponentContext c) {
                                  return StaggerTransitionSameComponent.create(c).build();
                                }
                              }),
                          new SingleDemo(
                              "Stagger Transition with Delay",
                              new ComponentCreator() {
                                @Override
                                public Component create(ComponentContext c) {
                                  return StaggerTransitionWithDelayComponent.create(c).build();
                                }
                              }),
                          new SingleDemo(
                              "Parallel Transition with Animators",
                              new ComponentCreator() {
                                @Override
                                public Component create(ComponentContext c) {
                                  return ParallelTransitionWithAnimatorsComponent.create(c).build();
                                }
                              }),
                          new SingleDemo(
                              "Sequence Transition loop",
                              new ComponentCreator() {
                                @Override
                                public Component create(ComponentContext c) {
                                  return SequenceTransitionLoopComponent.create(c).build();
                                }
                              }),
                          new SingleDemo(
                              "Global key Transition",
                              new ComponentCreator() {
                                @Override
                                public Component create(ComponentContext c) {
                                  return GlobalKeyParentComponent.create(c).build();
                                }
                              }))),
                  new DemoGrouping(
                      "Collections",
                      Arrays.asList(
                          new SingleDemo(
                              "HorizontalScroll (non-recycling)", HorizontalScrollActivity.class),
                          new SingleDemo("Sections Demo: Lithography", LithographyActivity.class),
                          new SingleDemo("Snapping", HorizontalScrollWithSnapActivity.class),
                          new SingleDemo(
                              "Dynamic Item Height",
                              new ComponentCreator() {
                                @Override
                                public Component create(ComponentContext c) {
                                  return HorizontalScrollWithDynamicItemHeight.create(c).build();
                                }
                              }))),
                  new DemoGrouping(
                      "Common Props",
                      Arrays.asList(
                          new SingleDemo("Border Effects", BorderEffectsActivity.class),
                          new SingleDemo(
                              "Duplicate Parent/Child State", DuplicateStateActivity.class))),
                  new DemoGrouping(
                      "Dynamic Props",
                      Arrays.asList(
                          new SingleDemo("Dynamic Props Demo", DynamicPropsActivity.class),
                          new SingleDemo("Fast Scroll Handle", FastScrollHandleActivity.class))),
                  new DemoGrouping(
                      "Incremental Mount",
                      Arrays.asList(
                          new SingleDemo(
                              "With Custom Animating Container",
                              IncrementalMountWithCustomViewContainerActivity.class))),
                  new DemoGrouping(
                      "Lifecycle",
                      Arrays.asList(
                          new SingleDemo("Error Boundaries", ErrorHandlingActivity.class),
                          new SingleDemo("Lifecycle Callbacks", LifecycleDelegateActivity.class),
                          new SingleDemo(
                              "ViewPager Lifecycle Callbacks", ViewPagerLifecycleActivity.class),
                          new SingleDemo(
                              "Fragment transactions lifecycle", LifecycleFragmentActivity.class))),
                  new DemoGrouping(
                      "Other Widgets",
                      Arrays.asList(
                          new SingleDemo(
                              "ViewPager",
                              new ComponentCreator() {
                                @Override
                                public Component create(ComponentContext c) {
                                  return ViewPagerDemoComponent.create(c).build();
                                }
                              }))),
                  new DemoGrouping(
                      "State Updates",
                      Arrays.asList(
                          new SingleDemo(
                              "State Update from Outside Litho",
                              StateUpdateFromOutsideTreeActivity.class),
                          new SingleDemo(
                              "State Update in Section from Child Component",
                              new ComponentCreator() {
                                @Override
                                public Component create(ComponentContext c) {
                                  return RecyclerCollectionComponent.create(c)
                                      .disablePTR(true)
                                      .section(
                                          SectionStateUpdateFromComponentSection.create(
                                                  new SectionContext(c))
                                              .build())
                                      .build();
                                }
                              }))),
                  new DemoGrouping(
                      "TextInput",
                      Arrays.asList(
                          new SingleDemo(
                              "Focus and Show Soft Keyboard on Appear",
                              new ComponentCreator() {
                                @Override
                                public Component create(ComponentContext c) {
                                  return TextInputWithKeyboardAndFocusDemo.create(c).build();
                                }
                              }),
                          new SingleDemo(
                              "Request and Clear Focus with Keyboard",
                              new ComponentCreator() {
                                @Override
                                public Component create(ComponentContext c) {
                                  return TextInputRequestAndClearFocus.create(c).build();
                                }
                              }))),
                  new DemoGrouping(
                      "Triggers",
                      Arrays.asList(
                          new SingleDemo(
                              "Clear Text Trigger", ClearTextTriggerExampleActivity.class),
                          new SingleDemo(
                              "Custom Event Trigger", CustomEventTriggerExampleActivity.class),
                          new SingleDemo("Tooltip Trigger", TooltipTriggerExampleActivity.class))),
                  new DemoGrouping(
                      "Editor",
                      Arrays.asList(
                          new SingleDemo(
                              "SimpleEditor for Props and State",
                              SimpleEditorExampleActivity.class))))),
          new DemoList(
              "Internal Debugging Samples",
              Arrays.asList(
                  new DemoGrouping(
                      "Litho Stats",
                      Arrays.asList(new SingleDemo("LithoStats", StatsActivity.class))),
                  new DemoGrouping(
                      "Sections Changesets",
                      Arrays.asList(
                          new SingleDemo("Resetting state", StateResettingActivity.class),
                          new SingleDemo("Items re-rendering", ItemsRerenderingActivity.class),
                          new SingleDemo("Not updating with new props", PropUpdatingActivity.class),
                          new SingleDemo(
                              "List scrolls to bottom", ScrollingToBottomActivity.class))))),
          new DemoList(
              "Tutorial",
              Arrays.asList(
                  new DemoGrouping(
                      "Onboarding",
                      Arrays.asList(
                          new SingleDemo("1. Hello World", HelloWorldActivity.class),
                          new SingleDemo(
                              "2. First Litho Component", FirstComponentSpecActivity.class),
                          new SingleDemo(
                              "3. Introducing Layout",
                              new ComponentCreator() {
                                @Override
                                public Component create(ComponentContext c) {
                                  return IntroducingLayoutComponent.create(c).name("Linda").build();
                                }
                              }),
                          new SingleDemo(
                              "3.1. More with Layout",
                              new ComponentCreator() {
                                @Override
                                public Component create(ComponentContext c) {
                                  return LayoutWithImageComponent.create(c).name("Linda").build();
                                }
                              }))))));

  public interface DemoItem {
    String getName();
  }

  /**
   * The reasons indices are used is so we have something parcelable to pass to the Activity (a
   * ComponentCreator is not parcelable).
   */
  public interface NavigableDemoItem extends DemoItem {
    Intent getIntent(Context context, int[] currentIndices);
  }

  public interface HasChildrenDemos {
    List<? extends DemoItem> getDemos();
  }

  /** A DemoList has groupings of SingleDemos or DemoLists to navigate to. */
  static final class DemoList implements NavigableDemoItem, HasChildrenDemos {
    final String name;
    final List<DemoGrouping> datamodels;

    DemoList(String name, List<DemoGrouping> datamodels) {
      this.name = name;
      this.datamodels = datamodels;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public Intent getIntent(Context context, int[] currentIndices) {
      final Intent intent = new Intent(context, DemoListActivity.class);
      intent.putExtra(DemoListActivity.INDICES, currentIndices);
      return intent;
    }

    @Override
    public List<DemoGrouping> getDemos() {
      return datamodels;
    }
  }

  /** A DemoGrouping is a list of demo items that show under a single heading. */
  public static class DemoGrouping implements DemoItem, HasChildrenDemos {
    final String name;
    @Nullable final List<? extends NavigableDemoItem> datamodels;

    DemoGrouping(String name, List<? extends NavigableDemoItem> datamodels) {
      this.name = name;
      this.datamodels = datamodels;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public List<? extends DemoItem> getDemos() {
      return datamodels;
    }
  }

  public static class SingleDemo implements NavigableDemoItem {
    final String name;
    @Nullable final Class klass;
    @Nullable final ComponentCreator componentCreator;

    SingleDemo(String name, Class klass) {
      this.name = name;
      this.klass = klass;
      this.componentCreator = null;
    }

    SingleDemo(String name, ComponentCreator componentCreator) {
      this.name = name;
      this.klass = null;
      this.componentCreator = componentCreator;
    }

    @Override
    public String getName() {
      return name;
    }

    private Class getActivityClass() {
      if (klass != null) {
        return klass;
      } else {
        return ComponentDemoActivity.class;
      }
    }

    @Override
    public Intent getIntent(Context context, int[] currentIndices) {
      final Intent intent = new Intent(context, getActivityClass());
      intent.putExtra(DemoListActivity.INDICES, currentIndices);
      return intent;
    }
  }
}
