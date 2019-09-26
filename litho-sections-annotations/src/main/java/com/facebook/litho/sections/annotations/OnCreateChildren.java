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

package com.facebook.litho.sections.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Every class that is annotated with {@link GroupSectionSpec} requires a method that is annotated
 * with {@link OnCreateChildren}.
 *
 * <p>The method onCreateChildren is responsible for generating the children of a {@link
 * GroupSectionSpec}. Both {@link GroupSectionSpec} and {@link DiffSectionSpec} are valid children.
 * OnCreateChildren has access to both <code>{@literal @}Prop</code> and/or <code>{@literal @}State
 * </code> annotations.
 *
 * <pre>
 * {@literal @}OnCreateChildren
 *  static Children onCreateChildren(
 *      final SectionContext c,
 *     {@literal @}State boolean isLoading,
 *     {@literal @}Prop int prop) {
 *    return Children.create()
 *       .child(SingleComponentSection.create(c).component(SomeComponent.create(c).build())
 *       .build();
 *  }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface OnCreateChildren {}
