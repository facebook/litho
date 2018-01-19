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
 * exception inside supported delegate methods happens. You then get a chance to either provide a
 * fallback component that is displayed instead or re-raise the error.
 *
 * <p>The method will receive a ComponentContext, an {@link Exception} and a LifecycleMethod enum.
 *
 * <p>The currently supported lifecycle methods are defined in {@code
 * com.facebook.litho.LifecyclePhase}.
 *
 * <ul>
 *   <li>onCreateLayout
 *   <li>onCreateLayoutWithSizeSpec
 * </ul>
 *
 * An example use may look like this:
 *
 * <pre>{@code
 * {@literal @}OnError
 *  static Component onError(ComponentContext c,
 *    Exception e,
 *    LifecyclePhase l,
 *  {@literal @}Prop final SomeProp prop) {
 *    switch (l) {
 *      case ON_CREATE_LAYOUT:
 *        return Text.create(c).text(
 *            String.format("Prop Name=%s, Error=%s", prop.name, e)
 *        ).textSizeSp(36).build();
 *      default:
 *        throw new RuntimeException(e);
 *    }
 *  }
 * }</pre>
 */
@Retention(RetentionPolicy.SOURCE)
public @interface OnError {}
