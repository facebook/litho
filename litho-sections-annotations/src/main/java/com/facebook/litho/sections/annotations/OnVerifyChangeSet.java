/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.litho.sections.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A class annotated with {@link DiffSectionSpec} might need a method with this annotation to verify
 * whether its ChangeSet is valid. Returns useful debug info if duplicate items are found, returns
 * NULL otherwise.
 *
 * <pre>
 * {@literal @}OnVerifyChangeSet
 *  public static <T> String verifyChangeSet(SectionContext context, @Prop List<? extends T> data) {
 *    return detectDuplicates(data);
 *  }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface OnVerifyChangeSet {}
