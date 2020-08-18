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

import android.os.Bundle;
import androidx.annotation.Nullable;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
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

public class DemoListActivity extends NavigatableDemoActivity {

  static final String INDICES = "INDICES";

  static final List<DemoListDataModel> DATA_MODELS =
      Arrays.asList(
          new DemoListDataModel(
              "Playground",
              new ComponentCreator() {
                @Override
                public Component create(ComponentContext c) {
                  return PlaygroundComponent.create(c).build();
                }
              }),

          // Please keep this alphabetical with consistent naming and casing!
          new DemoListDataModel(
              "API Demos",
              Arrays.asList(
                  new DemoListDataModel(
                      "Animations",
                      Arrays.asList(
                          new DemoListDataModel(
                              "Animations Composition", ComposedAnimationsActivity.class),
                          new DemoListDataModel(
                              "Expandable Element", ExpandableElementActivity.class),
                          new DemoListDataModel("Animated Badge", AnimatedBadgeActivity.class),
                          new DemoListDataModel("Bounds Animation", BoundsAnimationActivity.class),
                          new DemoListDataModel("Page Indicators", PageIndicatorsActivity.class),
                          new DemoListDataModel(
                              "Common Dynamic Props Animations",
                              CommonDynamicPropsAnimationActivity.class),
                          new DemoListDataModel(
                              "Animation Cookbook", AnimationCookBookActivity.class),
                          new DemoListDataModel(
                              "Animation Callbacks", AnimationCallbacksActivity.class),
                          new DemoListDataModel("Shared elements", SharedElementsActivity.class),
                          new DemoListDataModel("Transitions", TransitionsActivity.class))),
                  new DemoListDataModel("Border Effects", BorderEffectsActivity.class),
                  new DemoListDataModel(
                      "Duplicate Parent/Child State", DuplicateStateActivity.class),
                  new DemoListDataModel("Dynamic Props", DynamicPropsActivity.class),
                  new DemoListDataModel("Error Boundaries", ErrorHandlingActivity.class),
                  new DemoListDataModel("Fast Scroll Handle", FastScrollHandleActivity.class),
                  new DemoListDataModel(
                      "HorizontalScroll (non-recycling)", HorizontalScrollActivity.class),
                  new DemoListDataModel(
                      "Incremental Mount",
                      Arrays.asList(
                          new DemoListDataModel(
                              "With Custom Animating Container",
                              IncrementalMountWithCustomViewContainerActivity.class))),
                  new DemoListDataModel("Lifecycle Callbacks", LifecycleDelegateActivity.class),
                  new DemoListDataModel("LithoStats", StatsActivity.class),
                  new DemoListDataModel("Snapping", HorizontalScrollWithSnapActivity.class),
                  new DemoListDataModel(
                      "State Updates",
                      Arrays.asList(
                          new DemoListDataModel(
                              "State Update from Outside Litho",
                              StateUpdateFromOutsideTreeActivity.class))),
                  new DemoListDataModel(
                      "TextInput",
                      Arrays.asList(
                          new DemoListDataModel(
                              "Focus and Show Soft Keyboard on Appear",
                              new ComponentCreator() {
                                @Override
                                public Component create(ComponentContext c) {
                                  return TextInputWithKeyboardAndFocusDemo.create(c).build();
                                }
                              }),
                          new DemoListDataModel(
                              "Request and Clear Focus with Keyboard",
                              new ComponentCreator() {
                                @Override
                                public Component create(ComponentContext c) {
                                  return TextInputRequestAndClearFocus.create(c).build();
                                }
                              }))),
                  new DemoListDataModel(
                      "Triggers",
                      Arrays.asList(
                          new DemoListDataModel(
                              "Clear Text Trigger", ClearTextTriggerExampleActivity.class),
                          new DemoListDataModel(
                              "Custom Event Trigger", CustomEventTriggerExampleActivity.class),
                          new DemoListDataModel(
                              "Tooltip Trigger", TooltipTriggerExampleActivity.class))),
                  new DemoListDataModel(
                      "ViewPager",
                      new ComponentCreator() {
                        @Override
                        public Component create(ComponentContext c) {
                          return ViewPagerDemoComponent.create(c).build();
                        }
                      }))),
          new DemoListDataModel(
              "Internal Debugging Samples",
              Arrays.asList(
                  new DemoListDataModel("Lithography", LithographyActivity.class),
                  new DemoListDataModel(
                      "Sections Changesets",
                      Arrays.asList(
                          new DemoListDataModel("Resetting state", StateResettingActivity.class),
                          new DemoListDataModel(
                              "Items re-rendering", ItemsRerenderingActivity.class),
                          new DemoListDataModel(
                              "Not updating with new props", PropUpdatingActivity.class),
                          new DemoListDataModel(
                              "List scrolls to bottom", ScrollingToBottomActivity.class))))));

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final int[] indices = getIntent().getIntArrayExtra(INDICES);
    final List<DemoListDataModel> dataModels = getDataModels(indices);

    final ComponentContext componentContext = new ComponentContext(this);
    setContentView(
        LithoView.create(
            this,
            DemoListComponent.create(componentContext)
                .dataModels(dataModels)
                .parentIndices(indices)
                .build()));
  }

  private List<DemoListDataModel> getDataModels(@Nullable int[] indices) {
    List<DemoListDataModel> dataModels = DATA_MODELS;
    if (indices == null) {
      return dataModels;
    }

    for (int i = 0; i < indices.length; i++) {
      dataModels = dataModels.get(indices[i]).datamodels;
    }
    return dataModels;
  }

  static final class DemoListDataModel {
    final String name;
    @Nullable final Class klass;
    @Nullable final List<DemoListDataModel> datamodels;
    @Nullable final ComponentCreator componentCreator;

    DemoListDataModel(String name, Class klass) {
      this.name = name;
      this.klass = klass;
      this.datamodels = null;
      this.componentCreator = null;
    }

    DemoListDataModel(String name, List<DemoListDataModel> datamodels) {
      this.name = name;
      this.datamodels = datamodels;
      this.klass = null;
      this.componentCreator = null;
    }

    DemoListDataModel(String name, ComponentCreator componentCreator) {
      this.name = name;
      this.datamodels = null;
      this.klass = null;
      this.componentCreator = componentCreator;
    }

    public Class getActivityClass() {
      if (klass != null) {
        return klass;
      } else if (datamodels != null) {
        return DemoListActivity.class;
      } else {
        return ComponentDemoActivity.class;
      }
    }
  }
}
