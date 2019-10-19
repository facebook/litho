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
 * The method annotated with this annotation will be called when the Component is created for the
 * first time and should create the Service it wishes to use.
 *
 * <p>The method will take as parameters any needed <code>{@literal @}Prop</code> and will have as
 * return type the Service it will create.
 *
 * <p>It will be possible to access the service from other callbacks using: {@code SomeService
 * someService}.
 *
 * <p>For example:
 *
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
public @interface OnCreateService {}
