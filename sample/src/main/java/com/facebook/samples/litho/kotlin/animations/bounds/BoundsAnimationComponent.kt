/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.samples.litho.kotlin.animations.bounds

import android.graphics.Color
import android.graphics.Typeface
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentScope
import com.facebook.litho.KComponent
import com.facebook.litho.ResourcesScope
import com.facebook.litho.Row
import com.facebook.litho.State
import com.facebook.litho.Style
import com.facebook.litho.Transition
import com.facebook.litho.animation.AnimatedProperties
import com.facebook.litho.core.height
import com.facebook.litho.core.margin
import com.facebook.litho.core.padding
import com.facebook.litho.core.width
import com.facebook.litho.dp
import com.facebook.litho.flexbox.flex
import com.facebook.litho.kotlin.widget.Text
import com.facebook.litho.sp
import com.facebook.litho.transition.transitionKey
import com.facebook.litho.transition.useTransition
import com.facebook.litho.useState
import com.facebook.litho.view.backgroundColor
import com.facebook.litho.view.onClick
import com.facebook.yoga.YogaAlign
import com.facebook.yoga.YogaJustify

private const val ANIMATION_TIMING = 1000
private const val TRANSITION_KEY_CONTAINER_AFFECTED_CHILDREN = "container_affected_children"
private const val TRANSITION_KEY_CHILD_AFFECTED_CHILDREN_START = "child_affected_children_start"
private const val TRANSITION_KEY_CHILD_AFFECTED_CHILDREN_MIDDLE = "child_affected_children_middle"
private const val TRANSITION_KEY_CHILD_AFFECTED_CHILDREN_END = "child_affected_children_end"
private const val TRANSITION_KEY_CONTAINER_AFFECTED_SIBLINGS = "container_affected_siblings"
private const val TRANSITION_KEY_CHILD_AFFECTED_SIBLINGS_START = "child_affected_siblings_start"
private const val TRANSITION_KEY_CHILD_AFFECTED_SIBLINGS_END = "child_affected_siblings_end"
private const val TRANSITION_KEY_CONTAINER_AFFECTED_PARENT = "container_affected_parent"
private const val TRANSITION_KEY_CHILD_AFFECTED_PARENT = "child_affected_parent"
private const val TRANSITION_KEY_CONTAINER_ALL_TOGETHER_GLOBAL = "container_all_together_global"
private const val TRANSITION_KEY_CONTAINER_ALL_TOGETHER_TOP = "container_all_together_top"
private const val TRANSITION_KEY_CONTAINER_ALL_TOGETHER_BOTTOM = "container_all_together_bottom"
private const val TRANSITION_KEY_CHILD_ALL_TOGETHER_TOP_START = "child_all_together_top_start"
private const val TRANSITION_KEY_CHILD_ALL_TOGETHER_TOP_END = "child_all_together_top_end"
private const val TRANSITION_KEY_CHILD_ALL_TOGETHER_BOTTOM_START = "child_all_together_bottom_start"
private const val TRANSITION_KEY_CHILD_ALL_TOGETHER_BOTTOM_END = "child_all_together_bottom_end"

class BoundsAnimationComponent : KComponent() {
  override fun ComponentScope.render(): Component {
    val autoBoundsTransitionEnabled = useState { false }
    val isAffectedChildrenExpanded = useState { true }
    val isAffectedSiblingsExpanded = useState { true }
    val isAffectedParentExpanded = useState { true }
    val isAllTogetherExpanded = useState { false }

    val transitionKeys =
        if (autoBoundsTransitionEnabled.value) {
          arrayOf(
              TRANSITION_KEY_CONTAINER_AFFECTED_CHILDREN,
              TRANSITION_KEY_CHILD_AFFECTED_CHILDREN_START,
              TRANSITION_KEY_CHILD_AFFECTED_CHILDREN_MIDDLE,
              TRANSITION_KEY_CHILD_AFFECTED_CHILDREN_END,
              TRANSITION_KEY_CONTAINER_AFFECTED_SIBLINGS,
              TRANSITION_KEY_CHILD_AFFECTED_SIBLINGS_START,
              TRANSITION_KEY_CHILD_AFFECTED_SIBLINGS_END,
              TRANSITION_KEY_CONTAINER_AFFECTED_PARENT,
              TRANSITION_KEY_CHILD_AFFECTED_PARENT,
              TRANSITION_KEY_CONTAINER_ALL_TOGETHER_GLOBAL,
              TRANSITION_KEY_CONTAINER_ALL_TOGETHER_TOP,
              TRANSITION_KEY_CONTAINER_ALL_TOGETHER_BOTTOM,
              TRANSITION_KEY_CHILD_ALL_TOGETHER_TOP_START,
              TRANSITION_KEY_CHILD_ALL_TOGETHER_TOP_END,
              TRANSITION_KEY_CHILD_ALL_TOGETHER_BOTTOM_START,
              TRANSITION_KEY_CHILD_ALL_TOGETHER_BOTTOM_END)
        } else {
          arrayOf(
              TRANSITION_KEY_CONTAINER_AFFECTED_CHILDREN,
              TRANSITION_KEY_CHILD_AFFECTED_SIBLINGS_END,
              TRANSITION_KEY_CHILD_AFFECTED_PARENT,
              TRANSITION_KEY_CHILD_ALL_TOGETHER_BOTTOM_END)
        }

    useTransition(
        Transition.parallel<Transition.BaseTransitionUnitsBuilder>(
            Transition.create(Transition.TransitionKeyType.GLOBAL, *transitionKeys)
                .animate(AnimatedProperties.WIDTH, AnimatedProperties.X)
                .animator(Transition.timing(ANIMATION_TIMING))))

    return Column(
        alignItems = YogaAlign.CENTER,
        style = Style.backgroundColor(Color.WHITE).padding(vertical = 8.dp)) {
      child(
          Text(
              text = "ABT " + if (autoBoundsTransitionEnabled.value) "enabled" else "disabled",
              textSize = 20.sp,
              textStyle = Typeface.BOLD,
              textColor = Color.BLACK,
              style =
                  Style.onClick {
                    autoBoundsTransitionEnabled.update(autoBoundsTransitionEnabled.value.not())
                  }))
      child(
          Text(text = "Affected Children", textSize = 20.sp, style = Style.margin(vertical = 8.dp)))
      child(getAffectedChildren(isAffectedChildrenExpanded))
      child(
          Text(text = "Affected Siblings", textSize = 20.sp, style = Style.margin(vertical = 8.dp)))
      child(getAffectedSiblings(isAffectedSiblingsExpanded))
      child(Text(text = "Affected Parent", textSize = 20.sp, style = Style.margin(vertical = 8.dp)))
      child(getAffectedParents(isAffectedParentExpanded))
      child(getAllTogether(isAllTogetherExpanded))
    }
  }

  private fun ResourcesScope.getAffectedChildren(isExpanded: State<Boolean>): Component =
      Row(
          style =
              Style.transitionKey(
                      context,
                      TRANSITION_KEY_CONTAINER_AFFECTED_CHILDREN,
                      Transition.TransitionKeyType.GLOBAL)
                  .height((60 + 2 * 8).dp)
                  .width((3 * 60 / (if (isExpanded.value) 1 else 2) + 4 * 8).dp)
                  .padding(all = 8.dp)
                  .backgroundColor(Color.YELLOW)
                  .onClick { isExpanded.update(isExpanded.value.not()) }) {
        child(
            Column(
                style =
                    Style.transitionKey(
                            context,
                            TRANSITION_KEY_CHILD_AFFECTED_CHILDREN_START,
                            Transition.TransitionKeyType.GLOBAL)
                        .flex(1f)
                        .backgroundColor(Color.RED)))
        child(
            Column(
                style =
                    Style.transitionKey(
                            context,
                            TRANSITION_KEY_CHILD_AFFECTED_CHILDREN_MIDDLE,
                            Transition.TransitionKeyType.GLOBAL)
                        .flex(1f)
                        .backgroundColor(Color.RED)
                        .margin(horizontal = 8.dp)))
        child(
            Column(
                style =
                    Style.transitionKey(
                            context,
                            TRANSITION_KEY_CHILD_AFFECTED_CHILDREN_END,
                            Transition.TransitionKeyType.GLOBAL)
                        .flex(1f)
                        .backgroundColor(Color.RED)))
      }

  private fun ResourcesScope.getAffectedSiblings(isExpanded: State<Boolean>): Component =
      Row(
          style =
              Style.transitionKey(
                      context,
                      TRANSITION_KEY_CONTAINER_AFFECTED_SIBLINGS,
                      Transition.TransitionKeyType.GLOBAL)
                  .height((60 + 2 * 8).dp)
                  .width((3 * 60 + 4 * 8).dp)
                  .padding(all = 8.dp)
                  .backgroundColor(Color.LTGRAY)
                  .onClick { isExpanded.update(isExpanded.value.not()) }) {
        child(
            Column(
                style =
                    Style.transitionKey(
                            context,
                            TRANSITION_KEY_CHILD_AFFECTED_SIBLINGS_START,
                            Transition.TransitionKeyType.GLOBAL)
                        .flex(1f)
                        .backgroundColor(Color.RED)))
        child(
            Column(
                style =
                    Style.transitionKey(
                            context,
                            TRANSITION_KEY_CHILD_AFFECTED_SIBLINGS_END,
                            Transition.TransitionKeyType.GLOBAL)
                        .flex(if (isExpanded.value) 2f else 1f)
                        .backgroundColor(Color.YELLOW)
                        .margin(start = 8.dp)))
      }

  private fun ResourcesScope.getAffectedParents(isExpanded: State<Boolean>): Component =
      Row(
          justifyContent = YogaJustify.CENTER,
          style =
              Style.transitionKey(
                      context,
                      TRANSITION_KEY_CONTAINER_AFFECTED_PARENT,
                      Transition.TransitionKeyType.GLOBAL)
                  .height((60 + 2 * 8).dp)
                  .padding(all = 8.dp)
                  .backgroundColor(Color.LTGRAY)
                  .onClick { isExpanded.update(isExpanded.value.not()) }) {
        child(
            Column(
                style =
                    Style.transitionKey(
                            context,
                            TRANSITION_KEY_CHILD_AFFECTED_PARENT,
                            Transition.TransitionKeyType.GLOBAL)
                        .width((60 * if (isExpanded.value) 2 else 1).dp)
                        .backgroundColor(Color.YELLOW)))
      }

  private fun ResourcesScope.getAllTogether(isExpanded: State<Boolean>): Component =
      Column(
          style =
              Style.transitionKey(
                      context,
                      TRANSITION_KEY_CONTAINER_ALL_TOGETHER_GLOBAL,
                      Transition.TransitionKeyType.GLOBAL)
                  .margin(top = 24.dp)
                  .height((60 * 2 + 3 * 8).dp)
                  .padding(all = 8.dp)
                  .backgroundColor(Color.LTGRAY)
                  .onClick { isExpanded.update(isExpanded.value.not()) }) {
        child(
            Row(
                style =
                    Style.transitionKey(
                            context,
                            TRANSITION_KEY_CONTAINER_ALL_TOGETHER_TOP,
                            Transition.TransitionKeyType.GLOBAL)
                        .height(60.dp)
                        .padding(all = 6.dp)
                        .backgroundColor(Color.GRAY)) {
              child(
                  Column(
                      style =
                          Style.transitionKey(
                                  context,
                                  TRANSITION_KEY_CHILD_ALL_TOGETHER_TOP_START,
                                  Transition.TransitionKeyType.GLOBAL)
                              .margin(end = 6.dp)
                              .flex(1f)
                              .backgroundColor(Color.RED)))
              child(
                  Column(
                      style =
                          Style.transitionKey(
                                  context,
                                  TRANSITION_KEY_CHILD_ALL_TOGETHER_TOP_END,
                                  Transition.TransitionKeyType.GLOBAL)
                              .flex(1f)
                              .backgroundColor(Color.RED)))
            })
        child(
            Row(
                style =
                    Style.transitionKey(
                            context,
                            TRANSITION_KEY_CONTAINER_ALL_TOGETHER_BOTTOM,
                            Transition.TransitionKeyType.GLOBAL)
                        .height(60.dp)
                        .padding(all = 6.dp)
                        .margin(top = 8.dp)
                        .backgroundColor(Color.GRAY)) {
              child(
                  Column(
                      style =
                          Style.transitionKey(
                                  context,
                                  TRANSITION_KEY_CHILD_ALL_TOGETHER_BOTTOM_START,
                                  Transition.TransitionKeyType.GLOBAL)
                              .margin(end = 6.dp)
                              .width(100.dp)
                              .backgroundColor(Color.RED)))
              child(
                  Column(
                      style =
                          Style.transitionKey(
                                  context,
                                  TRANSITION_KEY_CHILD_ALL_TOGETHER_BOTTOM_END,
                                  Transition.TransitionKeyType.GLOBAL)
                              .flex(1f)
                              .width((if (isExpanded.value) 200 else 100).dp)
                              .backgroundColor(Color.YELLOW)))
            })
      }
}
