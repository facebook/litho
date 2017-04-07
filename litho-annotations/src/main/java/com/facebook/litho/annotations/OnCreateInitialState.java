/**
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
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
