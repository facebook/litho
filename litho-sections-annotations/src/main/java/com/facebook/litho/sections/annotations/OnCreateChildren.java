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
 * Every class that is annotated with {@link GroupSectionSpec} requires a method that is annotated
 * with {@link OnCreateChildren}.
 *
 * The method onCreateChildren is responsible for generating the children of a
 * {@link GroupSectionSpec}. Both {@link GroupSectionSpec} and {@link DiffSectionSpec} are valid
 * children. OnCreateChildren has access to both  * {@link com.facebook.litho.annotations.Prop}
 * and/or {@link com.facebook.litho.annotations.State}
 *
 * @code
 *
 * @OnCreateChildren
 * static Children onCreateChildren(
 *     final SectionContext c,
 *     @State boolean isLoading,
 *     @Prop int prop) {
 *   return Children.create()
 *      .child(SingleComponentSection.create(c).component(SomeComponent.create(c).build())
 *      .build();
 * }
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface OnCreateChildren {

}
