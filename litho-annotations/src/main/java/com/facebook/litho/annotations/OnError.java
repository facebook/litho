/*
 * Copyright (c) 2017-present, Facebook, Inc.
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
 *  static Component onError(ComponentContext c,
 *    Exception e,
 *   {@literal @}Prop final SomeProp prop) {
 *       MyComponent.updateErrorAsync(c, String.format("Error for %s: %s", prop, e.getMessage()));
 *  }
 * </code>
 * </pre>
 */
@Retention(RetentionPolicy.CLASS)
public @interface OnError {}
