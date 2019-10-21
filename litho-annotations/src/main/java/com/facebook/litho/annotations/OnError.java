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
 * Annotate a method inside your component with {@literal @}OnError to receive a callback when an
 * exception inside supported delegate methods of a child component happens. You then get a chance
 * to either trigger a state update or reraise the exception using <code>dispatchErrorEvent</code>.
 *
 * <p>The method will receive a ComponentContext, and an {@link Exception}.
 *
 * <p>An example use may look like this:
 *
 * <pre>
 * <code>
 * {@literal @}OnError
 *  static Component onError(
 *      ComponentContext c,
 *      Exception e,
 *     {@literal @}Prop SomeProp prop) {
 *    MyComponent.updateErrorAsync(c, String.format("Error for %s: %s", prop, e.getMessage()));
 *  }
 * </code>
 * </pre>
 */
@Retention(RetentionPolicy.CLASS)
public @interface OnError {}
