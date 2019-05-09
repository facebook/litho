/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * States for a given Component are the union of all arguments annotated with {@code State} in the
 * spec. While both {@link Prop} and {@link State} hold information that influences the output of
 * the component, they are different in one important way: props get passed to the component from
 * it's parent whereas states are managed within the component.
 *
 * <p>The initial values of states can be set using the {@link OnCreateInitialState} method and
 * states can be updated in {@link OnUpdateState} methods. Updating states in the {@link
 * OnUpdateState} methods will cause the component to invoke its {@link OnCreateLayout} method.
 * States <em>should</em> be immutable since the layout can be calculated on multiple threads.
 * Immutability of the states ensures that no thread safety issues can occur in the component
 * hierarchy.
 *
 * <p><b>Using State:</b> <br>
 *
 * <pre><code>{@literal @LayoutSpec}
 * public class CounterSpec {
 *
 *  {@literal @OnCreateLayout}
 *   static Component onCreateLayout(
 *     ComponentContext c,
 *    {@literal @State} int count) {
 *
 *     return Row.create(c)
 *       .backgroundColor(Color.WHITE)
 *       .heightDip(64)
 *       .paddingDip(YogaEdge.ALL, 8)
 *       .child(
 *         Text.create(c)
 *           .text(" + ")
 *           .clickHandler(Counter.onClick(c))
 *       )
 *       .child(
 *         Text.create(c)
 *           .text(String.valueOf(count))
 *       )
 *       .build();
 *   }
 *
 *  {@literal @OnCreateInitialState}
 *   static void onCreateInitialState(
 *       ComponentContext c,
 *       StateValue&lt;Integer&gt; count) {
 *     count.set(0);
 *   }
 *
 *  {@literal @OnEvent(ClickEvent.class)}
 *   static void onClick(ComponentContext c) {
 *     Counter.increment(c);
 *   }
 *
 *  {@literal @OnUpdateState}
 *   static void increment(StateValue&lt;Integer&gt; count) {
 *     count.set(count.get() + 1);
 *   }
 * }</code></pre>
 *
 * @see Param
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface State {

  /**
   * Declares that this state will not trigger a new layout calculations on update. After a lazy
   * state update the component will continue to host the older value until the next layout
   * calculation is triggered. This is useful for updating internal Component information and
   * persisting it between re-layouts when an immediate layout calculation is not needed.
   *
   * @return {@code true} iff this state should update lazily.
   */
  boolean canUpdateLazily() default false;
}
