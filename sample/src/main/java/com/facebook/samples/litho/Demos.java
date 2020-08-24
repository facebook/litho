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
import com.facebook.samples.litho.errors.ErrorHandlingActivity;
import com.facebook.samples.litho.fastscroll.FastScrollHandleActivity;
import com.facebook.samples.litho.hscroll.HorizontalScrollWithSnapActivity;
import com.facebook.samples.litho.incrementalmount.IncrementalMountWithCustomViewContainerActivity;
import com.facebook.samples.litho.lifecycle.LifecycleDelegateActivity;
import com.facebook.samples.litho.lithography.LithographyActivity;
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

  public static final List<NavigableDemoItem> DEMOS =
      Arrays.asList(
          new SingleDemo(
              "Playground",
              new ComponentCreator() {
                @Override
                public Component create(ComponentContext c) {
                  return PlaygroundComponent.create(c).build();
                }
              }),

          // Please keep this alphabetical with consistent naming and casing!
          new DemoList(
              "API Demos",
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
                          new SingleDemo("Transitions", TransitionsActivity.class))),
                  new DemoGrouping(
                      "Collections",
                      Arrays.asList(
                          new SingleDemo(
                              "HorizontalScroll (non-recycling)", HorizontalScrollActivity.class),
                          new SingleDemo("Sections Demo: Lithography", LithographyActivity.class),
                          new SingleDemo("Snapping", HorizontalScrollWithSnapActivity.class))),
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
                          new SingleDemo("Lifecycle Callbacks", LifecycleDelegateActivity.class))),
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
                          new SingleDemo(
                              "Tooltip Trigger", TooltipTriggerExampleActivity.class))))),
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
                              "List scrolls to bottom", ScrollingToBottomActivity.class))))));

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
