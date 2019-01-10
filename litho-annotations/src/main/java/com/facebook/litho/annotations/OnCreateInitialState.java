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
 * A method annotated with this annotation is responsible for creating the initial value for
 * params annotated with the {@link State} annotation in this spec. This method will take as
 * parameters a context and an com.facebook.litho.Output for every State variable that it will
 * initialize.
 *
 * <p>For example:
 * <code>
 *
 * {@literal @}LayoutSpec
 * public class MyChangeSetSpec {
 *
 *   {@literal @}OnCreateInitialState
 *   void onCreateInitialState(
 *     ComponentContext c,
 *     Output{@literal <}SomeState{@literal >} someState) {
       someState.set(new SomeState());
 *   }
 * }
 * </code>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface OnCreateInitialState {

}
