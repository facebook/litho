/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.litho.sections.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The method annotated with this annotation will be called when the Component is created for
 * the first time and should create the Service it wishes to use.
 *
 *
 * The method will take as parameters any needed <code>{@literal @}Prop</code>
 * and will have as return type the Service it will create.
 *
 * It will be possible to access the service from other callbacks using:
 * {@code SomeService someService}.
 *
 * <p>For example:
 * <pre>
 *  {@literal @}OnCreateService
 *   protected SomeService onCreateService(
 *       SectionContext c,
 *      {@literal @}Prop SomeProp prop) {
 *     return new SomeService(c, prop);
 *   }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface OnCreateService {

}
