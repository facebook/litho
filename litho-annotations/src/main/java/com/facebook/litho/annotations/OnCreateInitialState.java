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
 * This annotation is used in a {@link LayoutSpec}; the framework calls the method annotated with
 * {@code OnCreateInitialState} before resolving its layout, i.e. before calling {@link
 * OnCreateLayout}. This lifecycle method should be used to set the initial values of the state of
 * the component. The annotate method can receive {@code StateValue} containers for arguments
 * annotated with {@link State}.
 *
 * <p>The framework can call {@code OnCreateInitialState} from any thread. The method is invoked
 * only once during the lifecycle of the Component inside a ComponentTree - when it is first added
 * to the layout hierarchy. Any updates, such as state updates or prop changes, will not invoke this
 * method again if its global key did not change. Removing a component and adding it back to the
 * hierarchy will invoke this method again.
 *
 * <p><em>Required:</em><br>
 *
 * <ol>
 *   <li>{@code ComponentContext}
 * </ol>
 *
 * <p><em>Optional annotated arguments:</em><br>
 *
 * <ul>
 *   <li>{@link Prop}
 *   <li>{@link TreeProp}
 *   <li>{@link InjectProp}
 *   <li>{@code StateValue}
 * </ul>
 *
 * <pre><code>{@literal @LayoutSpec}
 * public class CounterSpec {
 *
 *  {@literal @OnCreateInitialState}
 *   static void onCreateInitialState(
 *       ComponentContext c,
 *       StateValue&lt;Integer&gt; count) {
 *     count.set(0);
 *   }
 *
 *  {@literal @OnCreateLayout}
 *   static Component onCreateLayout(
 *     ComponentContext c,
 *    {@literal @State} int count) {
 *
 *     return Row.create(c)
 *       .child(
 *         Text.create(c)
 *           .text(String.valueOf(count))
 *       )
 *       .build();
 *   }
 * }</code></pre>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface OnCreateInitialState {}
