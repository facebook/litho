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
 * Annotates a parameter to a component's event handler callback method indicating that it will be
 * supplied by the event object.
 */
@Retention(RetentionPolicy.SOURCE)
public @interface FromEvent {

  /**
   * The base type for an annotated parameter as it's defined in the event object. This is useful if
   * an event callback wants to receive a more specific type for a param than what's declared in the
   * Event object. The generated code will implicitly cast parameters before it delegates to the
   * spec method.
   *
   * <p>Either the parameter type as declared or the type specified here must match the type in the
   * Event object.
   */
  Class baseClass() default Object.class;
}
