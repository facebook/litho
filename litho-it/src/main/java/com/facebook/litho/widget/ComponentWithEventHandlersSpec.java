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

package com.facebook.litho.widget;

import android.util.Log;
import com.facebook.litho.Column;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.FromEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.Param;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.State;
import com.facebook.litho.widget.events.EventWithGenericTypes;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@LayoutSpec
class ComponentWithEventHandlersSpec {

  @OnCreateInitialState
  static void onCreateInitialState(ComponentContext c, StateValue<Integer> count) {
    count.set(1);
  }

  @OnCreateLayout
  static Component onCreateLayout(ComponentContext c, @Prop String name, @State int count) {
    return Column.create(c)
        .child(
            ComponentWhichDispatchesEvents.<List, Long, String, Object, Object>create(c)
                // event handler which uses ALL generic @FromEvent fields
                .handler(ComponentWithEventHandlers.onEventWithGenericParamUsage(c))
                // event handler which uses ONLY SOME generic @FromEvent fields
                .handler(
                    ComponentWithEventHandlers
                        .<List, Long, String, Object>onEventWithSomeGenericParamUsage(c))
                // event handler which uses NO generic @FromEvent fields
                .handler(
                    ComponentWithEventHandlers
                        .<List, Long, String, Object>onEventWithNoGenericParamUsage(c))
                // event handler which uses EXTRA type variables, even for generic @FromEvent fields
                .handler(
                    ComponentWithEventHandlers
                        .<Object, Set, Long, List, String, Object>
                            onEventWithSomeGenericParamUsageAndExtraTypeVariables(c))
                // event handler which uses EXTRA type variables, even for generic @FromEvent fields
                .handler(
                    ComponentWithEventHandlers.onEventWithAllGenericParamUsageAndExtraTypeVariables(
                        c, new HashSet<>()))
                .eventWithGenericTypesHandler(
                    ComponentWithEventHandlers.onEventWithGenericParamUsage(c)))
        .build();
  }

  @OnEvent(EventWithGenericTypes.class)
  static void onEventWithGenericParamUsage(
      final ComponentContext c,
      final @Prop String name,
      final @State int count,
      final @FromEvent String chars,
      final @FromEvent List object,
      final @FromEvent Long number) {
    Log.d("EventHandler", "onEventWithGeneric");
  }

  @OnEvent(EventWithGenericTypes.class)
  static void onEventWithSomeGenericParamUsage(
      final ComponentContext c,
      final @Prop String name,
      final @FromEvent List object,
      final @FromEvent Long number) {
    Log.d("EventHandler", "onEventWithGeneric");
  }

  @OnEvent(EventWithGenericTypes.class)
  static void onEventWithNoGenericParamUsage(final ComponentContext c, final @Prop String name) {
    Log.d("EventHandler", "onEventWithGeneric");
  }

  @OnEvent(EventWithGenericTypes.class)
  static <T, P extends Set, L extends Long>
      void onEventWithSomeGenericParamUsageAndExtraTypeVariables(
          final ComponentContext c,
          final @Prop String name,
          final @FromEvent List object,
          final @FromEvent List another,
          final @FromEvent L number) {
    Log.d("EventHandler", "onEventWithGeneric");
  }

  @OnEvent(EventWithGenericTypes.class)
  static <T, P extends Set, L extends Long, O extends List, S extends String>
      void onEventWithAllGenericParamUsageAndExtraTypeVariables(
          final ComponentContext c,
          final @Prop String name,
          final @FromEvent O object,
          final @FromEvent O another,
          final @FromEvent L number,
          final @FromEvent S chars,
          final @Param P set) {
    Log.d("EventHandler", "onEventWithGeneric");
  }
}
