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

import com.facebook.litho.animation.AnimationBinding;
import java.util.ArrayList;
import java.util.List;

/** A set of {@link Transition}s. */
public abstract class TransitionSet extends Transition {

  private final ArrayList<Transition> mChildren = new ArrayList<>();

  <T extends Transition> TransitionSet(T... children) {
    for (int i = 0; i < children.length; i++) {
      addChild(children[i]);
    }
  }

  <T extends Transition> TransitionSet(List<T> children) {
    for (int i = 0; i < children.size(); i++) {
      addChild(children.get(i));
    }
  }

  private void addChild(Transition child) {
    if (child instanceof Transition.BaseTransitionUnitsBuilder) {
      final ArrayList<? extends Transition> transitions =
          ((Transition.BaseTransitionUnitsBuilder) child).getTransitionUnits();
      if (transitions.size() > 1) {
        mChildren.add(new ParallelTransitionSet(transitions));
      } else {
        mChildren.add(transitions.get(0));
      }
    } else if (child != null) {
      mChildren.add(child);
    } else {
      throw new IllegalStateException("Null element is not allowed in transition set");
    }
  }

  public ArrayList<Transition> getChildren() {
    return mChildren;
  }

  abstract AnimationBinding createAnimation(List<AnimationBinding> children);
}
