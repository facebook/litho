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

package com.facebook.samples.litho

import android.content.Context
import android.content.Intent
import com.facebook.litho.Component
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.widget.RecyclerCollectionComponent
import com.facebook.litho.widget.ComponentCreator
import com.facebook.samples.litho.java.animations.animatedbadge.AnimatedBadge
import com.facebook.samples.litho.java.animations.animationcallbacks.AnimationCallbacksActivity
import com.facebook.samples.litho.java.animations.animationcookbook.AnimationCookBookActivity
import com.facebook.samples.litho.java.animations.commondynamicprops.CommonDynamicPropsAnimationActivity
import com.facebook.samples.litho.java.animations.docs.AppearTransitionComponent
import com.facebook.samples.litho.java.animations.docs.ParallelTransitionWithAnimatorsComponent
import com.facebook.samples.litho.java.animations.docs.SequenceTransitionLoopComponent
import com.facebook.samples.litho.java.animations.docs.SimpleAllLayoutTransitionComponent
import com.facebook.samples.litho.java.animations.docs.StaggerTransitionComponent
import com.facebook.samples.litho.java.animations.docs.StaggerTransitionSameComponent
import com.facebook.samples.litho.java.animations.docs.StaggerTransitionWithDelayComponent
import com.facebook.samples.litho.java.animations.docs.keyscope.GlobalKeyParentComponent
import com.facebook.samples.litho.java.animations.expandableelement.ExpandableElementActivity
import com.facebook.samples.litho.java.animations.pageindicators.PageIndicatorsRootComponent
import com.facebook.samples.litho.java.animations.sharedelements.SharedElementsComponent
import com.facebook.samples.litho.java.animations.sharedelements.SharedElementsFragmentActivity
import com.facebook.samples.litho.java.animations.transitions.TransitionsActivity
import com.facebook.samples.litho.java.changesetdebug.ItemsRerenderingActivity
import com.facebook.samples.litho.java.changesetdebug.PropUpdatingActivity
import com.facebook.samples.litho.java.changesetdebug.ScrollingToBottomActivity
import com.facebook.samples.litho.java.changesetdebug.StateResettingActivity
import com.facebook.samples.litho.java.duplicatestate.DuplicateState
import com.facebook.samples.litho.java.dynamicprops.DynamicPropsActivity
import com.facebook.samples.litho.java.editor.SimpleEditorExampleActivity
import com.facebook.samples.litho.java.errors.ErrorHandlingActivity
import com.facebook.samples.litho.java.fastscroll.FastScrollHandleComponent
import com.facebook.samples.litho.java.horizontalscroll.HorizontalScrollRootComponent
import com.facebook.samples.litho.java.hscroll.HorizontalScrollWithDynamicItemHeight
import com.facebook.samples.litho.java.hscroll.HorizontalScrollWithSnapActivity
import com.facebook.samples.litho.java.incrementalmount.IncrementalMountWithCustomViewContainerActivity
import com.facebook.samples.litho.java.lifecycle.LifecycleDelegateActivity
import com.facebook.samples.litho.java.lifecycle.LifecycleFragmentActivity
import com.facebook.samples.litho.java.lifecycle.ViewPagerLifecycleActivity
import com.facebook.samples.litho.java.lithography.LithographyActivity
import com.facebook.samples.litho.java.onboarding.AlphaTransitionComponent
import com.facebook.samples.litho.java.onboarding.FirstComponentSpecActivity
import com.facebook.samples.litho.java.onboarding.HelloWorldActivity
import com.facebook.samples.litho.java.onboarding.IntroducingLayoutComponent
import com.facebook.samples.litho.java.onboarding.LayoutWithImageComponent
import com.facebook.samples.litho.java.playground.PlaygroundComponent
import com.facebook.samples.litho.java.stateupdates.SectionStateUpdateFromComponentSection
import com.facebook.samples.litho.java.stateupdates.StateUpdateFromOutsideTreeActivity
import com.facebook.samples.litho.java.stats.Stats
import com.facebook.samples.litho.java.textinput.TextInputRequestAndClearFocus
import com.facebook.samples.litho.java.textinput.TextInputWithKeyboardAndFocusDemo
import com.facebook.samples.litho.java.triggers.ClearTextTriggerExampleComponent
import com.facebook.samples.litho.java.triggers.CustomEventTriggerExampleComponent
import com.facebook.samples.litho.java.triggers.TooltipTriggerExampleActivity
import com.facebook.samples.litho.java.viewpager.ViewPagerDemoComponent
import com.facebook.samples.litho.kotlin.animations.animatedapi.AnimatedComponent
import com.facebook.samples.litho.kotlin.animations.animatedbadge.AnimatedBadgeKotlin
import com.facebook.samples.litho.kotlin.animations.animatedcounter.AnimatingCounterRootComponent
import com.facebook.samples.litho.kotlin.animations.animationcomposition.ComposedAnimationsComponent
import com.facebook.samples.litho.kotlin.animations.bounds.BoundsAnimationComponent
import com.facebook.samples.litho.kotlin.animations.expandableelement.ExpandableElementRootKotlinComponent
import com.facebook.samples.litho.kotlin.animations.messages.Message
import com.facebook.samples.litho.kotlin.animations.transitions.TransitionsComponent
import com.facebook.samples.litho.kotlin.bordereffects.BorderEffectsComponent
import com.facebook.samples.litho.kotlin.collection.ChangeableItemsCollectionKComponent
import com.facebook.samples.litho.kotlin.collection.CollectionKComponent
import com.facebook.samples.litho.kotlin.collection.PullToRefreshCollectionKComponent
import com.facebook.samples.litho.kotlin.collection.ScrollToCollectionKComponent
import com.facebook.samples.litho.kotlin.errors.ErrorHandlingKotlinActivity
import com.facebook.samples.litho.kotlin.lithography.LithographyKotlinActivity
import com.facebook.samples.litho.kotlin.logging.LoggingActivity
import com.facebook.samples.litho.kotlin.playground.PlaygroundKComponent

class Demos {
  companion object {
    @kotlin.jvm.JvmField
    val DEMOS =
        listOf(
            DemoList(
                name = "Playground",
                listOf(
                    DemoGrouping(
                        name = "Playground",
                        listOf(
                            SingleDemo(name = "Java API Playground") { context ->
                              PlaygroundComponent.create(context).build()
                            },
                            SingleDemo(
                                name = "Kotlin API Playground",
                                component = PlaygroundKComponent()))))),
            // Please keep this alphabetical with consistent naming and casing!
            DemoList(
                name = "Kotlin API Demos",
                listOf(
                    DemoGrouping(
                        name = "Animations",
                        listOf(
                            SingleDemo(name = "Animated API Demo", component = AnimatedComponent()),
                            SingleDemo(name = "Animated Badge") { context ->
                              AnimatedBadgeKotlin.create(context).build()
                            },
                            SingleDemo(
                                name = "Animated Counter",
                                component = AnimatingCounterRootComponent()),
                            SingleDemo(name = "Animations Composition") { context ->
                              ComposedAnimationsComponent.create(context).build()
                            },
                            SingleDemo(name = "Expandable Element") { context ->
                              ExpandableElementRootKotlinComponent.create(context)
                                  .initialMessages(Message.MESSAGES)
                                  .build()
                            },
                            SingleDemo(name = "Transitions", component = TransitionsComponent()))),
                    DemoGrouping(
                        name = "Collections",
                        listOf(
                            SingleDemo(name = "Fixed Items", component = CollectionKComponent()),
                            SingleDemo(
                                name = "Changeable items",
                                component = ChangeableItemsCollectionKComponent()),
                            SingleDemo(
                                name = "Scroll to items",
                                component = ScrollToCollectionKComponent()),
                            SingleDemo(
                                name = "Pull to refresh",
                                component = PullToRefreshCollectionKComponent()),
                            SingleDemo(
                                name = "Sections Demo: Lithography",
                                klass = LithographyKotlinActivity::class.java))),
                    DemoGrouping(
                        name = "Errors",
                        listOf(
                            SingleDemo(
                                name = "Error Boundaries",
                                klass = ErrorHandlingKotlinActivity::class.java))),
                    DemoGrouping(
                        name = "Common Props",
                        listOf(
                            SingleDemo(name = "Border Effects") { context ->
                              BorderEffectsComponent.create(context).build()
                            })),
                    DemoGrouping(
                        name = "Logging",
                        listOf(
                            SingleDemo(name = " Logging", klass = LoggingActivity::class.java))))),
            DemoList(
                name = "Java API Demos",
                listOf(
                    DemoGrouping(
                        name = "Animations",
                        listOf(
                            SingleDemo(name = "Animations Composition") { context ->
                              ComposedAnimationsComponent.create(context).build()
                            },
                            SingleDemo(
                                name = "Expandable Element", ExpandableElementActivity::class.java),
                            SingleDemo(name = "Animated Badge") { context ->
                              AnimatedBadge.create(context).build()
                            },
                            SingleDemo(name = "Bounds Animation") { context ->
                              BoundsAnimationComponent.create(context).build()
                            },
                            SingleDemo(name = "Page Indicators") { context ->
                              PageIndicatorsRootComponent.create(context).build()
                            },
                            SingleDemo(
                                name = "Common Dynamic Props Animations",
                                klass = CommonDynamicPropsAnimationActivity::class.java),
                            SingleDemo(
                                name = "Animation Cookbook",
                                klass = AnimationCookBookActivity::class.java),
                            SingleDemo(
                                name = "Animation Callbacks",
                                klass = AnimationCallbacksActivity::class.java),
                            SingleDemo(name = "Activity Transition with Shared elements") { context
                              ->
                              SharedElementsComponent.create(context).build()
                            },
                            SingleDemo(
                                name = "Fragments Transition with Shared elements",
                                klass = SharedElementsFragmentActivity::class.java),
                            SingleDemo(
                                name = "Transitions", klass = TransitionsActivity::class.java),
                            SingleDemo(name = "All Layout Transition") { context ->
                              SimpleAllLayoutTransitionComponent.create(context).build()
                            },
                            SingleDemo(name = "Alpha Transition") { context ->
                              AlphaTransitionComponent.create(context).build()
                            },
                            SingleDemo(name = "Appear Transition") { context ->
                              AppearTransitionComponent.create(context).build()
                            },
                            SingleDemo(name = "Stagger Transition") { context ->
                              StaggerTransitionComponent.create(context).build()
                            },
                            SingleDemo(name = "Stagger Transition on same Component") { context ->
                              StaggerTransitionSameComponent.create(context).build()
                            },
                            SingleDemo(name = "Stagger Transition with Delay") { context ->
                              StaggerTransitionWithDelayComponent.create(context).build()
                            },
                            SingleDemo(name = "Parallel Transition with Animators") { context ->
                              ParallelTransitionWithAnimatorsComponent.create(context).build()
                            },
                            SingleDemo(name = "Sequence Transition loop") { context ->
                              SequenceTransitionLoopComponent.create(context).build()
                            },
                            SingleDemo(name = "Global key Transition") { context ->
                              GlobalKeyParentComponent.create(context).build()
                            })),
                    DemoGrouping(
                        name = "Collections",
                        listOf(
                            SingleDemo(name = "HorizontalScroll (non-recycling)") { context ->
                              HorizontalScrollRootComponent.create(context).build()
                            },
                            SingleDemo(
                                name = "Sections Demo: Lithography",
                                klass = LithographyActivity::class.java),
                            SingleDemo(
                                name = "Snapping",
                                klass = HorizontalScrollWithSnapActivity::class.java),
                            SingleDemo(name = "Dynamic Item Height") { context ->
                              HorizontalScrollWithDynamicItemHeight.create(context).build()
                            })),
                    DemoGrouping(
                        name = "Common Props",
                        listOf(
                            SingleDemo(name = "Border Effects") { context ->
                              BorderEffectsComponent.create(context).build()
                            },
                            SingleDemo(name = "Duplicate Parent/Child State") { context ->
                              DuplicateState.create(context).build()
                            })),
                    DemoGrouping(
                        name = "Dynamic Props",
                        listOf(
                            SingleDemo(
                                name = "Dynamic Props Demo",
                                klass = DynamicPropsActivity::class.java),
                            SingleDemo(name = "Fast Scroll Handle") { context ->
                              FastScrollHandleComponent.create(context).build()
                            })),
                    DemoGrouping(
                        name = "Incremental Mount",
                        listOf(
                            SingleDemo(
                                name = "With Custom Animating Container",
                                klass =
                                    IncrementalMountWithCustomViewContainerActivity::class.java))),
                    DemoGrouping(
                        name = "Lifecycle",
                        listOf(
                            SingleDemo(
                                name = "Error Boundaries",
                                klass = ErrorHandlingActivity::class.java),
                            SingleDemo(
                                name = "Lifecycle Callbacks",
                                klass = LifecycleDelegateActivity::class.java),
                            SingleDemo(
                                name = "ViewPager Lifecycle Callbacks",
                                klass = ViewPagerLifecycleActivity::class.java),
                            SingleDemo(
                                name = "Fragment transactions lifecycle",
                                klass = LifecycleFragmentActivity::class.java))),
                    DemoGrouping(
                        name = "Other Widgets",
                        listOf(
                            SingleDemo(name = "ViewPager") { context ->
                              ViewPagerDemoComponent.create(context).build()
                            })),
                    DemoGrouping(
                        name = "State Updates",
                        listOf(
                            SingleDemo(
                                name = "State Update from Outside Litho",
                                klass = StateUpdateFromOutsideTreeActivity::class.java),
                            SingleDemo(name = "State Update in Section from Child Component") {
                                context ->
                              RecyclerCollectionComponent.create(context)
                                  .disablePTR(true)
                                  .section(
                                      SectionStateUpdateFromComponentSection.create(
                                              SectionContext(context))
                                          .build())
                                  .build()
                            })),
                    DemoGrouping(
                        name = "TextInput",
                        listOf(
                            SingleDemo(name = "Focus and Show Soft Keyboard on Appear") { context ->
                              TextInputWithKeyboardAndFocusDemo.create(context).build()
                            },
                            SingleDemo(name = "Request and Clear Focus with Keyboard") { context ->
                              TextInputRequestAndClearFocus.create(context).build()
                            })),
                    DemoGrouping(
                        name = "Triggers",
                        listOf(
                            SingleDemo(name = "Clear Text Trigger") { context ->
                              ClearTextTriggerExampleComponent.create(context).build()
                            },
                            SingleDemo(name = "Custom Event Trigger") { context ->
                              CustomEventTriggerExampleComponent.create(context).build()
                            },
                            SingleDemo(
                                name = "Tooltip Trigger",
                                klass = TooltipTriggerExampleActivity::class.java))),
                    DemoGrouping(
                        name = "Editor",
                        listOf(
                            SingleDemo(
                                name = "SimpleEditor for Props and State",
                                klass = SimpleEditorExampleActivity::class.java))))),
            DemoList(
                name = "Internal Debugging Samples",
                listOf(
                    DemoGrouping(
                        name = "Litho Stats",
                        listOf(
                            SingleDemo(name = "LithoStats") { context ->
                              Stats.create(context).build()
                            })),
                    DemoGrouping(
                        name = "Sections Changesets",
                        listOf(
                            SingleDemo(
                                name = "Resetting state",
                                klass = StateResettingActivity::class.java),
                            SingleDemo(
                                name = "Items re-rendering",
                                klass = ItemsRerenderingActivity::class.java),
                            SingleDemo(
                                name = "Not updating with new props",
                                klass = PropUpdatingActivity::class.java),
                            SingleDemo(
                                name = "List scrolls to bottom",
                                klass = ScrollingToBottomActivity::class.java))))),
            DemoList(
                name = "Tutorial",
                listOf(
                    DemoGrouping(
                        name = "Onboarding",
                        listOf(
                            SingleDemo(
                                name = "1. Hello World", klass = HelloWorldActivity::class.java),
                            SingleDemo(
                                name = "2. First Litho Component",
                                klass = FirstComponentSpecActivity::class.java),
                            SingleDemo(name = "3. Introducing Layout") { context ->
                              IntroducingLayoutComponent.create(context).name("Linda").build()
                            },
                            SingleDemo(name = "3.1. More with Layout") { context ->
                              LayoutWithImageComponent.create(context).name("Linda").build()
                            })))))
  }

  interface DemoItem {
    val name: String
  }

  /**
   * The reasons indices are used is so we have something parcelable to pass to the Activity (a
   * ComponentCreator is not parcelable).
   */
  interface NavigableDemoItem : DemoItem {
    fun getIntent(context: Context?, currentIndices: IntArray?): Intent
  }

  interface HasChildrenDemos {
    val demos: List<DemoItem>?
  }

  /** A DemoList has groupings of SingleDemos or DemoLists to navigate to. */
  class DemoList(override val name: String, val datamodels: List<DemoGrouping>) :
      NavigableDemoItem, HasChildrenDemos {
    override fun getIntent(context: Context?, currentIndices: IntArray?): Intent {
      val intent = Intent(context, DemoListActivity::class.java)
      intent.putExtra(DemoListActivity.INDICES, currentIndices)
      return intent
    }

    override val demos: List<DemoGrouping>?
      get() = datamodels
  }

  /** A DemoGrouping is a list of demo items that show under a single heading. */
  class DemoGrouping
  internal constructor(override val name: String, val datamodels: List<NavigableDemoItem>?) :
      DemoItem, HasChildrenDemos {
    override val demos: List<DemoItem>?
      get() = datamodels
  }

  class SingleDemo : NavigableDemoItem {
    override val name: String
    val klass: Class<*>?
    val componentCreator: ComponentCreator?
    val component: Component?

    internal constructor(
        name: String,
        klass: Class<*>? = null,
        component: Component? = null,
        componentCreator: ComponentCreator? = null
    ) {
      this.name = name
      this.klass = klass
      this.componentCreator = componentCreator
      this.component = component
    }

    private val activityClass: Class<*>
      private get() = klass ?: ComponentDemoActivity::class.java

    override fun getIntent(context: Context?, currentIndices: IntArray?): Intent {
      val intent = Intent(context, activityClass)
      intent.putExtra(DemoListActivity.INDICES, currentIndices)
      return intent
    }
  }
}
