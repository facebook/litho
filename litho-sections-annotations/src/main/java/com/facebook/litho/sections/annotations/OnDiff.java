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

package com.facebook.litho.sections.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A class annotated with {@link DiffSectionSpec} requires a method with this annotation. This
 * method is responsible for generating a <code>ChangeSet</code>.
 *
 * <pre>
 * {@literal @}OnDiff
 *  public static void onCreateChangeSet(
 *      SectionContext context,
 *      ChangeSet changeSet,
 *     {@literal @}Prop{@code Diff<Component>} component) {
 *    if (component.getNext() == null) {
 *      changeSet.delete(0);
 *    } else if (component.getPrevious() == null) {
 *      changeSet.insert(0, component.getNext());
 *    }
 *  }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface OnDiff {}
