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

package com.facebook.litho.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * States for a given Component are the union of all arguments annotated with {@code State} in the
 * spec. While both {@link Prop} and {@link State} hold information that influences the output of
 * the component, they are different in one important way: props get passed to the component from
 * its parent whereas states are managed within the component.
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
   * Declares that this state can be updated lazily and that will not trigger a new layout
   * calculations. After a lazy state update the component will continue to host the older value
   * until the next layout calculation is preformed. This is useful for updating internal Component
   * information and persisting it between re-layouts when an immediate layout calculation is not
   * needed or when this state is not involved into layout calculation at all.
   *
   * <p><b>Note:</b> Such state can still participate in normal state update via {@link
   * OnUpdateState} methods, but for lazy state updates an additional {@code lazyUpdate*StateName*}
   * method will be generated.
   *
   * <p><b>Warning:</b> For now, lazily updated values will be available only in {@link OnEvent}
   * methods (or after a normal state update). If you need support of other lifecycle methods, feel
   * free to file an issue.
   *
   * <p><b>Using State:</b> <br>
   *
   * <pre><code>{@literal @LayoutSpec}
   * public class MyComponentSpec {
   *  {@literal @OnCreateLayout}
   *   static Component onCreateLayout(ComponentContext c) {
   *     return Column.create(c)
   *       .child(
   *         Text.create(c)
   *           .backgroundRes(R.drawable.button_background)
   *           .textSizeSp(20)
   *           .text("Submit")
   *           .clickHandler(MyComponent.onClick(c)))
   *       .build();
   *   }
   *
   *  {@literal @OnEvent(ClickEvent.class)}
   *   static void onClick(ComponentContext c, @State(canUpdateLazily = true) boolean wasAlreadyClicked) {
   *     if (!wasAlreadyClicked) {
   *       logFirstButtonClick();
   *     }
   *     MyComponent.lazyUpdateWasAlreadyClicked(c, true);
   *   }
   * }</code></pre>
   *
   * @return {@code true} if this state can be updated lazily.
   */
  boolean canUpdateLazily() default false;
}
