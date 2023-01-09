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

package com.facebook.litho.widget

import android.util.Log
import com.facebook.litho.Column
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.StateValue
import com.facebook.litho.annotations.FromEvent
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateInitialState
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.Param
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.State
import com.facebook.litho.widget.events.EventWithGenericTypes
import java.util.HashSet

@LayoutSpec
internal object ComponentWithEventHandlersSpec {

  @JvmStatic
  @OnCreateInitialState
  fun onCreateInitialState(c: ComponentContext, count: StateValue<Int>) {
    count.set(1)
  }

  @JvmStatic
  @OnCreateLayout
  fun onCreateLayout(c: ComponentContext, @Prop name: String, @State count: Int): Component =
      Column.create(c)
          .child(
              ComponentWhichDispatchesEvents.create<List<*>, Long, String, Any, Any>(
                      c) // event handler which uses ALL generic @FromEvent fields
                  .handler(
                      ComponentWithEventHandlers.onEventWithGenericParamUsage(
                          c)) // event handler which uses ONLY SOME generic @FromEvent fields
                  .handler(
                      ComponentWithEventHandlers.onEventWithSomeGenericParamUsage(
                          c)) // event handler which uses NO generic @FromEvent fields
                  .handler(
                      ComponentWithEventHandlers.onEventWithNoGenericParamUsage(
                          c)) // event handler which uses EXTRA type variables, even for generic
                  // @FromEvent fields
                  .handler(
                      ComponentWithEventHandlers
                          .onEventWithSomeGenericParamUsageAndExtraTypeVariables<
                              Any, Set<*>, Long, List<*>, String, Any>(
                              c)) // event handler which uses EXTRA type variables, even for generic
                  // @FromEvent fields
                  .handler(
                      ComponentWithEventHandlers
                          .onEventWithAllGenericParamUsageAndExtraTypeVariables<
                              Any, HashSet<Any>, Long, List<*>, String, Any>(
                              c, HashSet()))
                  .eventWithGenericTypesHandler(
                      ComponentWithEventHandlers.onEventWithGenericParamUsage(c)))
          .build()

  @JvmStatic
  @OnEvent(EventWithGenericTypes::class)
  fun onEventWithGenericParamUsage(
      c: ComponentContext,
      @Prop name: String?,
      @State count: Int,
      @FromEvent chars: String?,
      @FromEvent `object`: List<*>?,
      @FromEvent number: Long?
  ) {
    Log.d("EventHandler", "onEventWithGeneric")
  }

  @JvmStatic
  @OnEvent(EventWithGenericTypes::class)
  fun onEventWithSomeGenericParamUsage(
      c: ComponentContext,
      @Prop name: String,
      @FromEvent `object`: List<*>,
      @FromEvent number: Long
  ) {
    Log.d("EventHandler", "onEventWithGeneric")
  }

  @JvmStatic
  @OnEvent(EventWithGenericTypes::class)
  fun onEventWithNoGenericParamUsage(c: ComponentContext, @Prop name: String) {
    Log.d("EventHandler", "onEventWithGeneric")
  }

  @JvmStatic
  @OnEvent(EventWithGenericTypes::class)
  fun <T, P : Set<*>?, L : Long?> onEventWithSomeGenericParamUsageAndExtraTypeVariables(
      c: ComponentContext,
      @Prop name: String?,
      @FromEvent `object`: List<*>?,
      @FromEvent another: List<*>?,
      @FromEvent number: L
  ) {
    Log.d("EventHandler", "onEventWithGeneric")
  }

  @JvmStatic
  @OnEvent(EventWithGenericTypes::class)
  fun <
      T,
      P : Set<*>,
      L : Long,
      O : List<*>,
      S : String> onEventWithAllGenericParamUsageAndExtraTypeVariables(
      c: ComponentContext,
      @Prop name: String,
      @FromEvent `object`: O,
      @FromEvent another: O,
      @FromEvent number: L,
      @FromEvent chars: S,
      @Param set: P
  ) {
    Log.d("EventHandler", "onEventWithGeneric")
  }
}
