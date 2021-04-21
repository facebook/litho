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

package com.facebook.litho.transition

import com.facebook.litho.ComponentScope
import com.facebook.litho.Transition
import com.facebook.rendercore.transitions.TransitionUtils

/** Defines single or multiple [Transition] animations for the given component */
fun ComponentScope.useTransition(transition: Transition?) {
  transition ?: return
  TransitionUtils.setOwnerKey(transition, context.globalKey)
  val transitionsList = transitions ?: ArrayList<Transition>()
  transitionsList.add(transition)
  transitions = transitionsList
}
