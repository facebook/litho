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

package com.facebook.samples.litho.animations.transitions;

import com.facebook.litho.ClickEvent;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.Row;
import com.facebook.litho.StateValue;
import com.facebook.litho.Transition;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnCreateTransition;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.State;
import com.facebook.litho.widget.Text;
import com.facebook.yoga.YogaEdge;

@LayoutSpec
class MultipleComponentMovesTransitionSpec {

  enum Deck {
    ORDERED(new String[] {"\uD83C\uDCA1", "\uD83C\uDCB2", "\uD83C\uDCC3", "\uD83C\uDCD4"}),
    REVERSED(new String[] {"\uD83C\uDCD4", "\uD83C\uDCC3", "\uD83C\uDCB2", "\uD83C\uDCA1"}),
    SHUFFLED(new String[] {"\uD83C\uDCC3", "\uD83C\uDCD4", "\uD83C\uDCA1", "\uD83C\uDCB2"}),
    ;

    String[] cards;

    Deck(String[] cards) {
      this.cards = cards;
    }
  }

  @OnCreateInitialState
  static void onCreateInitialState(ComponentContext c, StateValue<Deck> deck) {
    deck.set(Deck.ORDERED);
  }

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @State Deck deck) {
    Row.Builder builder =
        Row.create(c)
            .paddingDip(YogaEdge.ALL, 20)
            .clickHandler(MultipleComponentMovesTransition.onClick(c));

    String[] cards = deck.cards;
    for (String card : cards) {
      builder.child(Text.create(c).text(card).textSizeSp(72).transitionKey(card));
    }

    return builder.build();
  }

  @OnEvent(ClickEvent.class)
  static void onClick(ComponentContext c) {
    MultipleComponentMovesTransition.updateState(c);
  }

  @OnUpdateState
  static void updateState(StateValue<Deck> deck) {
    deck.set(Deck.values()[(deck.get().ordinal() + 1) % Deck.values().length]);
  }

  @OnCreateTransition
  static Transition onCreateTransition(ComponentContext c) {
    return Transition.allLayout();
  }
}
